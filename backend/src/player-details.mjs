import { ensure, json } from './errors.mjs';
import { publicPlayer, canSeeEvent } from './queries.mjs';
import { progression } from './progression.mjs';
import { chemistry } from './matchmaking.mjs';
import { SPORTS } from './sports.mjs';

/** Public sporting evidence; never includes contact details or private event IDs. */
export async function playerDetails(ctx, userId, sport) {
  ensure(SPORTS.includes(sport), 400, 'INVALID_INPUT', 'Branş geçersiz.');
  ensure(await ctx.db.get("SELECT id FROM users WHERE id=? AND status='active'", [userId]), 404, 'NOT_FOUND', 'Sporcu bulunamadı.');
  const saved = await ctx.db.get('SELECT privacy FROM preferences WHERE user_id=?', [userId]);
  const privacy = saved ? json(saved.privacy) : {};
  if (ctx.actor && ctx.actor.id !== userId) {
    ensure(!await ctx.db.get('SELECT user_id FROM blocks WHERE (user_id=? AND target_id=?) OR (user_id=? AND target_id=?)',
      [ctx.actor.id, userId, userId, ctx.actor.id]), 404, 'NOT_FOUND', 'Sporcu bulunamadı.');
  }
  const result = { player: await publicPlayer(ctx, userId), historyVisible: privacy.showRatingHistory !== false,
    recentForm: [], badges: [], chemistry: null, playedTogether: null };
  if (result.historyVisible) {
    const matches = await ctx.db.all(`SELECT e.*,m.side,r.payload AS result_payload,h.delta AS rating_delta
      FROM event_members m JOIN events e ON e.id=m.event_id JOIN results r ON r.event_id=e.id
      LEFT JOIN rating_history h ON h.event_id=e.id AND h.user_id=m.user_id
      WHERE m.user_id=? AND m.status='joined' AND m.checked_in_at IS NOT NULL AND r.status='verified' AND e.sport=?
      ORDER BY e.ends_at DESC,e.id DESC LIMIT 100`, [userId, sport]);
    for (const match of matches) {
      if (!await canSeeEvent(ctx, match)) continue;
      const score = json(match.result_payload);
      const ours = match.side === 'home' ? score.homeScore : score.awayScore;
      const theirs = match.side === 'home' ? score.awayScore : score.homeScore;
      result.recentForm.push({ outcome: !match.side ? null : ours === theirs ? 'D' : ours > theirs ? 'W' : 'L',
        ratingDelta: match.rating_delta == null ? null : Number(match.rating_delta) });
      if (result.recentForm.length === 5) break;
    }
    result.badges = (await progression(ctx, userId)).badges.filter(b => b.earned);
  }
  if (ctx.actor && ctx.actor.id !== userId && privacy.chemistrySignals !== false) {
    const own = await ctx.db.get('SELECT privacy FROM preferences WHERE user_id=?', [ctx.actor.id]);
    if (!own || json(own.privacy).chemistrySignals !== false) {
      result.chemistry = (await chemistry(ctx, ctx.actor.id, userId)).score;
      result.playedTogether = Number((await ctx.db.get(`SELECT COUNT(*) AS n FROM event_members a
        JOIN event_members b ON b.event_id=a.event_id JOIN results r ON r.event_id=a.event_id
        WHERE a.user_id=? AND b.user_id=? AND a.status='joined' AND b.status='joined' AND r.status='verified'`,
      [ctx.actor.id, userId])).n);
    }
  }
  return result;
}
