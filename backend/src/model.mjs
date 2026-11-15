import { id } from './auth.mjs';
import { ensure, fail, json, text, integer, choice, keys } from './errors.mjs';
import { SPORTS } from './sports.mjs';
import { eventNetworkMembership } from './event-network.mjs';
export const encode = JSON.stringify;
export const ACTIVE_EVENTS = ['PUBLISHED', 'FILLING', 'FULL'];
export const actorRequired = ctx => ensure(ctx.actor?.id, 401, 'UNAUTHENTICATED', 'Giriş yapmalısın.');
export const adminRequired = ctx => ensure(ctx.actor?.role === 'admin', 403, 'FORBIDDEN', 'Bu işlem yetki gerektirir.');
export async function row(ctx, table, key, value) {
  const result = await ctx.db.get(`SELECT * FROM ${table} WHERE ${key}=?`, [value]);
  ensure(result, 404, 'NOT_FOUND', 'Kayıt bulunamadı.'); return result;
}
export async function profile(ctx, userId = ctx.actor.id) { const result = await row(ctx, 'profiles', 'user_id', userId); return json(result.payload); }
export async function ownedEvent(ctx, eventId) { const e = await row(ctx, 'events', 'id', eventId); ensure(e.host_id === ctx.actor.id || ctx.actor.role === 'admin', 403, 'FORBIDDEN', 'Yalnızca organizatör işlem yapabilir.'); return e; }
export async function member(ctx, eventId, userId = ctx.actor.id) { const m = await ctx.db.get("SELECT * FROM event_members WHERE event_id=? AND user_id=? AND status='joined'", [eventId, userId]); ensure(m, 403, 'PARTICIPANT_REQUIRED', 'Bu işlem katılımcılara açık.'); return m; }
export async function captain(ctx, teamId) { const team = await row(ctx, 'teams', 'id', teamId); ensure(team.captain_id === ctx.actor.id || ctx.actor.role === 'admin', 403, 'CAPTAIN_REQUIRED', 'Takım kaptanı işlem yapabilir.'); return team; }
export async function audit(ctx, action, target, metadata = {}) { await ctx.db.run('INSERT INTO audit_log(id,actor_id,action,target_id,metadata,created_at) VALUES(?,?,?,?,?,?)', [id(), ctx.actor?.id ?? null, action, target, encode(metadata), ctx.now]); }
export async function wallet(ctx, userId = ctx.actor.id) {
  const r = await ctx.db.get('SELECT COALESCE(SUM(amount),0) AS balance,COALESCE(SUM(locked_delta),0) AS locked FROM wallet_entries WHERE user_id=?', [userId]);
  return { balance: Number(r.balance), locked: Number(r.locked) };
}
export async function ledger(ctx, userId, kind, amount, lockedDelta, ref, dedupe, description) {
  integer(amount, 'Token', -1000000, 1000000); integer(lockedDelta, 'Kilit', -1000000, 1000000);
  const exists = await ctx.db.get('SELECT id FROM wallet_entries WHERE dedupe=?', [dedupe]); if (exists) return;
  const current = await wallet(ctx, userId);
  ensure(current.balance + amount >= 0 && current.locked + lockedDelta >= 0, 409, 'INSUFFICIENT_TOKENS', 'Yeterli token bakiyesi yok.');
  await ctx.db.run('INSERT INTO wallet_entries(id,user_id,kind,amount,locked_delta,ref,dedupe,description,created_at) VALUES(?,?,?,?,?,?,?,?,?)', [id(), userId, kind, amount, lockedDelta, ref, dedupe, description, ctx.now]);
}
export async function hold(ctx, userId, scope, amount) {
  integer(amount, 'Taahhüt', 0, 1000); if (!amount) return;
  const old = await ctx.db.get('SELECT * FROM holds WHERE user_id=? AND scope=?', [userId, scope]);
  if (old?.status === 'held') return;
  const generation = id();
  await ledger(ctx, userId, 'HOLD', -amount, amount, scope, `hold:${generation}`, 'Katılım taahhüdü');
  await ctx.db.run('INSERT INTO holds(id,user_id,scope,amount,status) VALUES(?,?,?,?,?) ON CONFLICT(user_id,scope) DO UPDATE SET id=?,amount=?,status=?', [generation, userId, scope, amount, 'held', generation, amount, 'held']);
}
export async function release(ctx, userId, scope, consume = false) {
  const old = await ctx.db.get("SELECT * FROM holds WHERE user_id=? AND scope=? AND status='held'", [userId, scope]); if (!old) return 0;
  await ledger(ctx, userId, consume ? 'SPEND' : 'RELEASE', consume ? 0 : old.amount, -old.amount, scope, `release:${old.id}`, consume ? 'Katılım taahhüdü kullanıldı' : 'Katılım taahhüdü iadesi');
  await ctx.db.run('UPDATE holds SET status=? WHERE id=?', [consume ? 'consumed' : 'released', old.id]); return old.amount;
}
export async function notify(ctx, userId, category, title, body, destination, targetId, dedupe, extra = {}) {
  const exists = await ctx.db.get('SELECT id FROM notifications WHERE dedupe=?', [dedupe]); if (exists) return;
  const payload = { title, body, destination, targetId, emoji: '🔔', priority: category === 'wallet' ? 'P0' : ['matches','emergencyFill'].includes(category)?'P1':['challenges','waitlist','teams'].includes(category)?'P2':category==='marketing'?'P4':'P3', ...extra }, notificationId = id();
  await ctx.db.run('INSERT INTO notifications(id,user_id,dedupe,category,payload,created_at) VALUES(?,?,?,?,?,?)', [notificationId, userId, dedupe, category, encode(payload), ctx.now]);
  await ctx.db.run('INSERT INTO outbox(id,user_id,kind,payload,status,next_at,created_at) VALUES(?,?,?,?,?,?,?)', [id(), userId, 'push', encode({ ...payload, notificationId }), 'pending', ctx.now, ctx.now]);
}
export async function reward(ctx, userId, key, amount) {
  const old = await ctx.db.get('SELECT reward_key FROM rewards WHERE user_id=? AND reward_key=?', [userId, key]); if (old) return false;
  await ctx.db.run('INSERT INTO rewards(user_id,reward_key,created_at) VALUES(?,?,?)', [userId, key, ctx.now]);
  await ledger(ctx, userId, 'BONUS', amount, 0, key, `reward:${userId}:${key}`, key === 'welcome' ? 'Pasaport hoş geldin hediyesi' : key.startsWith('subscription:') ? 'Plus aylık token' : 'Sporcu görevi ödülü'); return true;
}
export function assertMutableEventRoster(event) {
  const settings = json(event.payload);
  ensure(!settings.isTeamEvent && !settings.challengeId && !settings.tournamentId, 409, 'FIXED_ROSTER', 'Resmi maç kadrosu takım veya turnuva kaydından belirlenir.');
}
export async function eventEligibility(ctx, event) {
  assertMutableEventRoster(event);
  const previous = await ctx.db.get('SELECT status FROM event_members WHERE event_id=? AND user_id=?', [event.id,ctx.actor.id]);
  ensure(previous?.status !== 'removed', 409, 'EVENT_REMOVED', 'Organizatör katılımını sonlandırdı. Yeniden katılım için organizatörün izni gerekiyor.');
  const p = await profile(ctx), settings = json(event.payload);
  if(settings.mode==='COMPETITIVE')ensure(p.skills.some(s=>s.sport===event.sport),403,'PROFILE_SPORT_REQUIRED','Rekabetçi maç için önce bu branşı pasaportuna ekle.');
  ensure(ACTIVE_EVENTS.includes(event.status) && Number(event.starts_at) > ctx.now, 409, 'EVENT_CLOSED', 'Bu maç artık katılım almıyor.');
  ensure(!settings.verifiedOnly || p.verifications.includes('BASIC'), 403, 'VERIFICATION_REQUIRED', 'Bu maç iletişim doğrulaması istiyor.');
  ensure(p.reliability >= (settings.minReliability ?? 0), 403, 'RELIABILITY_REQUIRED', 'Bu maçın güvenilirlik koşulunu karşılamıyorsun.');
  if (settings.networkScope === 'CAMPUS' || settings.networkScope === 'COMPANY') {
    const membership = await eventNetworkMembership(ctx,settings);
    ensure(membership, 403, 'COMMUNITY_REQUIRED', 'Bu ağın onaylı üyeliği gerekiyor.');
  }
}
export async function joinEvent(ctx, event, userId = ctx.actor.id) {
  assertMutableEventRoster(event);
  const old = await ctx.db.get('SELECT * FROM event_members WHERE event_id=? AND user_id=?', [event.id, userId]);
  ensure(old?.status !== 'removed',409,'EVENT_REMOVED','Yeniden katılım için organizatörün izni gerekiyor.');
  if (old?.status === 'joined') return;
  const count = await ctx.db.get("SELECT COUNT(*) AS n FROM event_members WHERE event_id=? AND status='joined'", [event.id]);
  const offers = await ctx.db.get("SELECT COUNT(*) AS n FROM waitlist WHERE event_id=? AND status='offered' AND offered_until>? AND user_id<>?", [event.id, ctx.now, userId]);
  ensure(count.n + offers.n < event.capacity, 409, 'EVENT_FULL', 'Son kontenjan doldu.');
  await hold(ctx, userId, `event:${event.id}`, event.commitment);
  await ctx.db.run('INSERT INTO event_members(event_id,user_id,status,joined_at) VALUES(?,?,?,?) ON CONFLICT(event_id,user_id) DO UPDATE SET status=?,side=NULL,checked_in_at=NULL,joined_at=?', [event.id, userId, 'joined', ctx.now, 'joined', ctx.now]);
  await ctx.db.run('UPDATE events SET status=?,version=version+1 WHERE id=?', [count.n + 1 >= event.capacity ? 'FULL' : 'FILLING', event.id]);
  const thread = await ctx.db.get('SELECT id FROM threads WHERE event_id=?', [event.id]);
  if (thread) await ctx.db.run('INSERT INTO thread_members(thread_id,user_id) VALUES(?,?) ON CONFLICT(thread_id,user_id) DO NOTHING', [thread.id, userId]);
  await ctx.db.run("UPDATE waitlist SET status='joined',offered_until=NULL WHERE event_id=? AND user_id=?", [event.id, userId]);
  await ctx.db.run("UPDATE join_requests SET status='approved' WHERE scope='event' AND scope_id=? AND user_id=? AND status='pending'", [event.id,userId]);
  await notify(ctx, userId, 'matches', 'Maç kadrosundasın', json(event.payload).title, 'EVENT', event.id, `joined:${event.id}:${userId}:${ctx.requestKey}`);
}
export async function offerWaitlist(ctx, eventId) {
  const event = await row(ctx, 'events', 'id', eventId);
  if (!ACTIVE_EVENTS.includes(event.status) || Number(event.starts_at) <= ctx.now) return;
  const count = await ctx.db.get("SELECT COUNT(*) AS n FROM event_members WHERE event_id=? AND status='joined'", [eventId]);
  const offers = await ctx.db.get("SELECT COUNT(*) AS n FROM waitlist WHERE event_id=? AND status='offered' AND offered_until>?", [eventId, ctx.now]);
  let seats = event.capacity - count.n - offers.n;
  const entries = await ctx.db.all("SELECT * FROM waitlist WHERE event_id=? AND status='waiting' ORDER BY created_at,user_id", [eventId]);
  for (const entry of entries) { if (seats-- <= 0) break;
    await ctx.db.run("UPDATE waitlist SET status='offered',offered_until=? WHERE event_id=? AND user_id=?", [ctx.now + 10 * 60000, eventId, entry.user_id]);
    await notify(ctx, entry.user_id, 'waitlist', 'Sıran geldi', '10 dakika içinde yerini ayır.', 'EVENT', eventId, `offer:${eventId}:${entry.user_id}:${ctx.now}`);
  }
}
export async function cancelEvent(ctx, event) {
  await ctx.db.run("UPDATE events SET status='CANCELLED',version=version+1 WHERE id=?", [event.id]);
  const held = await ctx.db.all("SELECT user_id FROM holds WHERE scope=? AND status='held'", [`event:${event.id}`]);
  for (const entry of held) await release(ctx, entry.user_id, `event:${event.id}`);
  await ctx.db.run("UPDATE join_requests SET status='rejected' WHERE scope='event' AND scope_id=? AND status='pending'", [event.id]);
  await ctx.db.run("UPDATE waitlist SET status='cancelled' WHERE event_id=?", [event.id]);
  const members = await ctx.db.all("SELECT user_id FROM event_members WHERE event_id=? AND status='joined'", [event.id]);
  for (const m of members) await notify(ctx, m.user_id, 'matches', 'Maç iptal edildi', json(event.payload).title, 'EVENT', event.id, `cancel:${event.id}:${m.user_id}`);
}
export async function assertThread(ctx, threadId, write = false) {
  const thread = await row(ctx, 'threads', 'id', threadId);
  const membership = await ctx.db.get('SELECT * FROM thread_members WHERE thread_id=? AND user_id=?', [threadId, ctx.actor.id]);
  ensure(membership, 403, 'FORBIDDEN', 'Bu sohbetin katılımcısı değilsin.');
  if (thread.event_id) await member(ctx, thread.event_id);
  if (thread.team_id) ensure(await ctx.db.get("SELECT user_id FROM team_members WHERE team_id=? AND user_id=? AND status='joined'", [thread.team_id, ctx.actor.id]), 403, 'FORBIDDEN', 'Takım üyeliğin sona ermiş.');
  if (thread.pair_key?.startsWith('community:')) {
    const communityId = thread.pair_key.slice('community:'.length);
    const community = await ctx.db.get('SELECT payload FROM communities WHERE id=?', [communityId]);
    ensure(community && !json(community.payload).archived, 403, 'FORBIDDEN', 'Bu topluluk kapatılmış.');
    ensure(await ctx.db.get("SELECT user_id FROM community_members WHERE community_id=? AND user_id=? AND status='joined'", [communityId, ctx.actor.id]), 403, 'FORBIDDEN', 'Topluluk üyeliğin sona ermiş.');
  }
  if (thread.kind === 'DIRECT') {
    const others = await ctx.db.all('SELECT user_id FROM thread_members WHERE thread_id=? AND user_id<>?', [threadId, ctx.actor.id]);
    for (const other of others) ensure(!await ctx.db.get('SELECT user_id FROM blocks WHERE (user_id=? AND target_id=?) OR (user_id=? AND target_id=?)', [ctx.actor.id, other.user_id, other.user_id, ctx.actor.id]), 403, 'BLOCKED', 'Bu sohbete erişilemiyor.');
  }
  return thread;
}
