import { json } from './errors.mjs';
import { id } from './auth.mjs';
import { row, release, audit, encode, notify, reward } from './model.mjs';
import { grantProgressRewards, weekNumber } from './progression.mjs';

export const RATING_POLICY = Object.freeze({ initial: 1000, kFactor: 24, disputeHours: 24,
  requiresCheckIn: true, requiresOpponentMajority: true,
  description: 'MMR yalnız doğrulanmış rekabetçi maçta, iki tarafta katılım kaydı varsa değişir. Token, görev ve abonelik MMR satın almaz.' });

export function eloDelta(rating, opponentRating, outcome) {
  const expected = 1 / (1 + 10 ** ((opponentRating - rating) / 400));
  return Math.round(RATING_POLICY.kFactor * (outcome - expected));
}

export function confirmationState(members, result, confirmations) {
  const submitter = members.find(m => m.user_id === result.submitted_by);
  const opponents = members.filter(m => m.user_id !== result.submitted_by && (!submitter?.side || m.side !== submitter.side));
  const required = Math.max(1, Math.floor(opponents.length / 2) + 1);
  const valid = new Set(opponents.filter(m => m.checked_in_at).map(m => m.user_id));
  return { required, confirmed: confirmations.filter(c => valid.has(c.user_id)).length };
}

function eligibility(event, members, userId, profiles) {
  if (json(event.payload).mode !== 'COMPETITIVE') return 'SOCIAL_MATCH';
  const own = members.find(m => m.user_id === userId);
  if (!own?.checked_in_at) return 'CHECKIN_REQUIRED';
  if (!own.side || !members.some(m => m.side === 'home') || !members.some(m => m.side === 'away')) return 'SIDES_REQUIRED';
  if (!members.some(m => m.side !== own.side && m.checked_in_at)) return 'OPPONENT_CHECKIN_REQUIRED';
  if (!profiles.get(userId)?.skills?.some(s => s.sport === event.sport)) return 'PROFILE_SPORT_REQUIRED';
  return null;
}

export async function ratingStatus(ctx, result) {
  const event = await row(ctx, 'events', 'id', result.event_id);
  const members = await ctx.db.all("SELECT * FROM event_members WHERE event_id=? AND status='joined'", [event.id]);
  const confirmations = await ctx.db.all('SELECT user_id FROM result_confirmations WHERE event_id=?', [event.id]);
  const state = confirmationState(members, result, confirmations), payload = json(result.payload);
  const settled = payload.ratingChanges?.find(c => c.userId === ctx.actor.id);
  const ownProfile = await ctx.db.get('SELECT payload FROM profiles WHERE user_id=?', [ctx.actor.id]);
  const reason = eligibility(event, members, ctx.actor.id, new Map(ownProfile ? [[ctx.actor.id, json(ownProfile.payload)]] : []));
  return { ...state, status: result.status, finalizesAfter: Number(result.dispute_until), delta: settled?.delta ?? null,
    reason: settled?.reason ?? reason ?? (result.status === 'disputed' ? 'DISPUTED' : state.confirmed < state.required ? 'PENDING_CONFIRMATION' : 'WAITING_PERIOD') };
}

/** Runs inside the same transaction as finalizing a result and its wallet commitments. */
export async function settleResult(ctx, result) {
  const event = await row(ctx, 'events', 'id', result.event_id);
  if (Number(event.ends_at) > ctx.now) return;
  const members = await ctx.db.all("SELECT * FROM event_members WHERE event_id=? AND status='joined'", [event.id]);
  const confirmations = await ctx.db.all('SELECT user_id FROM result_confirmations WHERE event_id=?', [event.id]);
  const payload = json(result.payload), confirmation = confirmationState(members, result, confirmations);
  if (!payload.adminApproved && confirmation.confirmed < confirmation.required) return;
  const profiles = new Map();
  for (const member of members) {
    const saved = await ctx.db.get('SELECT payload FROM profiles WHERE user_id=?', [member.user_id]);
    if (saved) profiles.set(member.user_id, json(saved.payload));
  }
  const rating = m => profiles.get(m.user_id)?.skills?.find(s => s.sport === event.sport)?.rating ?? 1000;
  const home = members.filter(m => m.side === 'home' && m.checked_in_at && profiles.has(m.user_id));
  const away = members.filter(m => m.side === 'away' && m.checked_in_at && profiles.has(m.user_id));
  const mean = side => side.reduce((sum, m) => sum + rating(m), 0) / Math.max(1, side.length);
  const homeOutcome = payload.homeScore === payload.awayScore ? 0.5 : payload.homeScore > payload.awayScore ? 1 : 0;
  const changes = [];
  await ctx.db.run("UPDATE events SET status='VERIFIED',version=version+1 WHERE id=?", [event.id]);
  for (const member of members) {
    const userId = member.user_id, data = profiles.get(userId);
    if (!data || await ctx.db.get('SELECT id FROM reliability_history WHERE user_id=? AND event_id=?', [userId, event.id])) continue;
    const attended = !!member.checked_in_at, reason = eligibility(event, members, userId, profiles);
    const before = data.skills.find(s => s.sport === event.sport)?.rating ?? 1000;
    const outcome = member.side === 'home' ? homeOutcome : 1 - homeOutcome;
    const desired = reason ? 0 : eloDelta(member.side === 'home' ? mean(home) : mean(away), member.side === 'home' ? mean(away) : mean(home), outcome);
    const after = Math.max(0, before + desired), delta = after - before;
    const skills = data.skills.map(s => {
      if (s.sport !== event.sport) return s;
      const played = s.verifiedMatches + (attended ? 1 : 0);
      const won = (s.wonMatches ?? Math.round((s.winRate ?? 0) * s.verifiedMatches / 100)) + (attended && member.side && outcome === 1 ? 1 : 0);
      return { ...s, rating: after, verifiedMatches: played, wonMatches: won, winRate: played ? Math.round(100 * won / played) : 0 };
    });
    const reliability = Math.max(0, Math.min(100, data.reliability + (attended ? 2 : -10)));
    const fair = await ctx.db.get("SELECT AVG(f.fair_play) AS score FROM feedback f JOIN results r ON r.event_id=f.event_id WHERE f.to_id=? AND (r.status='verified' OR r.event_id=?)", [userId, event.id]);
    const next = { ...data, skills, verifiedMatches: data.verifiedMatches + (attended ? 1 : 0), reliability,
      fairPlay: fair.score ? Math.round(fair.score * 10) / 10 : data.fairPlay };
    await ctx.db.run('UPDATE profiles SET payload=?,version=version+1,updated_at=? WHERE user_id=?', [encode(next), ctx.now, userId]);
    await ctx.db.run('INSERT INTO rating_history(id,user_id,event_id,sport,delta,rating,created_at) VALUES(?,?,?,?,?,?,?)', [id(), userId, event.id, event.sport, delta, after, ctx.now]);
    await ctx.db.run('INSERT INTO reliability_history(id,user_id,event_id,delta,reason,created_at) VALUES(?,?,?,?,?,?)', [id(), userId, event.id, reliability - data.reliability, attended ? 'Doğrulanmış katılım' : 'Check-in kaydı yok', ctx.now]);
    await release(ctx, userId, `event:${event.id}`, !attended);
    if (next.verifiedMatches >= 10) await reward(ctx, userId, 'ten_verified_matches', 20);
    changes.push({ userId, before, after, delta, reason: reason ?? 'VERIFIED' });
    await notify(ctx, userId, 'matches', 'Maç sonucu doğrulandı', `${json(event.payload).title} · ${delta > 0 ? '+' : ''}${delta} MMR`, 'EVENT', event.id, `verified:${event.id}:${userId}`);
  }
  await ctx.db.run("UPDATE results SET status='verified',payload=?,finalized_at=? WHERE event_id=?", [encode({ ...payload, ratingChanges: changes }), ctx.now, event.id]);
  await audit(ctx, 'result.finalize', event.id);
  for (const userId of profiles.keys()) await grantProgressRewards(ctx, userId, weekNumber(event.ends_at));
}
