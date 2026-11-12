import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { fixture } from './helpers.mjs';
import { runJobs } from '../src/jobs.mjs';

const code = value => error => error.code === value;
const conflict = error => error.status === 409;
const version = async (f, eventId) => (await f.store.get('SELECT version FROM events WHERE id=?', [eventId])).version;
const wallet = async (f, actor) => (await f.service.bootstrap(actor)).wallet;
async function remove(f, eventId, userId, actor = f.host, key = randomUUID()) {
  return f.cmd(actor, 'event.member.remove', { eventId, userId, expectedVersion: await version(f, eventId) }, key);
}
async function restore(f, eventId, userId, actor = f.host) {
  return f.cmd(actor, 'event.member.restore', { eventId, userId, expectedVersion: await version(f, eventId) });
}

test('direct admission consumes a pending application without letting cancellation or rejection release the roster hold', async t => {
  const f = await fixture(t), eventId = (await f.cmd(f.host, 'event.create', f.event())).result.id;
  const requestId = (await f.cmd(f.guest, 'event.request', { eventId, position: 'Kaleci' })).result.id;
  await f.cmd(f.guest, 'event.join', { eventId });
  assert.equal((await f.store.get('SELECT status FROM join_requests WHERE id=?', [requestId])).status, 'approved');
  await assert.rejects(f.cmd(f.guest, 'event.request.cancel', { requestId }), code('INVALID_STATE'));
  await assert.rejects(f.cmd(f.host, 'event.request.review', { requestId, approve: false }), code('INVALID_STATE'));
  assert.deepEqual(await wallet(f, f.guest), { balance: 10, locked: 10 });
  assert.equal((await f.store.get('SELECT status FROM event_members WHERE event_id=? AND user_id=?', [eventId, f.guest.id])).status, 'joined');
});

test('a second join at full capacity is idempotent for the already admitted athlete and keeps a single hold', async t => {
  const f = await fixture(t), eventId = (await f.cmd(f.host, 'event.create', f.event({ capacity: 2 }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId });
  const before = await version(f, eventId);
  await f.cmd(f.guest, 'event.join', { eventId });
  assert.equal(await version(f, eventId), before);
  assert.deepEqual(await wallet(f, f.guest), { balance: 10, locked: 10 });
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM wallet_entries WHERE user_id=? AND kind='HOLD' AND ref=?", [f.guest.id, `event:${eventId}`])).n, 1);
});

test('voluntary leave and readmission create fresh commitment generations without duplicate refunds', async t => {
  const f = await fixture(t), eventId = (await f.cmd(f.host, 'event.create', f.event({ startsAtEpochMillis: f.clock() + 10 * 3600000 }))).result.id;
  for (let i = 0; i < 2; i++) {
    await f.cmd(f.guest, 'event.join', { eventId });
    assert.deepEqual(await wallet(f, f.guest), { balance: 10, locked: 10 });
    await f.cmd(f.guest, 'event.leave', { eventId });
    assert.deepEqual(await wallet(f, f.guest), { balance: 20, locked: 0 });
  }
  await f.cmd(f.guest, 'event.join', { eventId });
  assert.deepEqual(await wallet(f, f.guest), { balance: 10, locked: 10 });
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM wallet_entries WHERE user_id=? AND kind='HOLD' AND ref=?", [f.guest.id, `event:${eventId}`])).n, 3);
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM wallet_entries WHERE user_id=? AND kind='RELEASE' AND ref=?", [f.guest.id, `event:${eventId}`])).n, 2);
});

test('scheduled start closes leave even before the host or worker changes the event status', async t => {
  const f = await fixture(t), eventId = (await f.cmd(f.host, 'event.create', f.event({ startsAtEpochMillis: f.clock() + 60000 }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId }); f.advance(60000);
  await assert.rejects(f.cmd(f.guest, 'event.leave', { eventId }), conflict);
  await assert.rejects(f.cmd(f.other, 'event.join', { eventId }), code('EVENT_CLOSED'));
  await assert.rejects(f.cmd(f.other, 'event.request', { eventId }), code('EVENT_CLOSED'));
  assert.deepEqual(await wallet(f, f.guest), { balance: 10, locked: 10 });
  assert.equal((await f.store.get('SELECT status FROM event_members WHERE event_id=? AND user_id=?', [eventId, f.guest.id])).status, 'joined');
});

test('an elapsed event cannot be restarted or have its roster removed before lifecycle cleanup runs', async t => {
  const f = await fixture(t), eventId = (await f.cmd(f.host, 'event.create', f.event({ startsAtEpochMillis: f.clock() + 60000, durationMin: 15 }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId }); f.advance(17 * 60000);
  await assert.rejects(f.cmd(f.host, 'event.transition', { eventId, status: 'IN_PROGRESS', expectedVersion: await version(f, eventId) }), conflict);
  await assert.rejects(remove(f, eventId, f.guest.id), conflict);
  await assert.rejects(f.cmd(f.guest, 'event.leave', { eventId }), conflict);
  assert.equal((await f.store.get('SELECT status FROM events WHERE id=?', [eventId])).status, 'FILLING');
});

test('a no-show cannot consume the only score report and never gains attendance after operator settlement', async t => {
  const f = await fixture(t), eventId = (await f.cmd(f.host, 'event.create', f.event({ startsAtEpochMillis: f.clock() + 600000, capacity: 2 }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId });
  const checkinCode = (await f.cmd(f.host, 'event.checkin.code', { eventId })).result.code;
  await f.cmd(f.host, 'event.checkin', { eventId, code: checkinCode });
  await f.cmd(f.host, 'event.transition', { eventId, status: 'IN_PROGRESS', expectedVersion: await version(f, eventId) });
  const score = { eventId, homeScore: 2, awayScore: 1, sides: { home: [f.host.id], away: [f.guest.id] } };
  await assert.rejects(f.cmd(f.guest, 'result.submit', score), code('CHECKIN_REQUIRED'));
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM results WHERE event_id=?', [eventId])).n, 0);
  assert.equal((await f.store.get('SELECT status FROM events WHERE id=?', [eventId])).status, 'IN_PROGRESS');
  await f.cmd(f.host, 'result.submit', score);
  await assert.rejects(f.cmd(f.guest, 'result.confirm', { eventId }), code('CHECKIN_REQUIRED'));
  f.advance(25 * 3600000); await runJobs(f.service);
  await f.cmd(f.admin, 'admin.result.resolve', { eventId, approve: true, reason: 'Katılım kayıtları ve sonuç operatörce incelendi.' });
  await runJobs(f.service);
  const absent = await f.service.bootstrap(f.guest);
  assert.equal(absent.profile.reliability, 90);
  assert.equal(absent.profile.verifiedMatches, 0);
  assert.equal(absent.profile.skills[0].rating, 1000);
  assert.ok(absent.progression.missions.every(mission => mission.progress === 0));
});

test('organizer removal refunds a late commitment, revokes chat and participation, and offers the freed place', async t => {
  const f = await fixture(t), eventId = (await f.cmd(f.host, 'event.create', f.event({ capacity: 2 }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId });
  const threadId = (await f.store.get('SELECT id FROM threads WHERE event_id=?', [eventId])).id;
  await f.cmd(f.host, 'chat.send', { threadId, text: 'Kadroya özel konuşma' });
  await f.cmd(f.other, 'event.waitlist', { eventId, action: 'join' });
  f.advance(35 * 60000);
  const payload = { eventId, userId: f.guest.id, expectedVersion: await version(f, eventId) }, key = randomUUID();
  await f.cmd(f.host, 'event.member.remove', payload, key);
  assert.equal((await f.cmd(f.host, 'event.member.remove', payload, key)).replayed, true);
  const removed = await f.service.bootstrap(f.guest);
  assert.deepEqual(removed.wallet, { balance: 20, locked: 0 });
  assert.equal(removed.profile.reliability, 100);
  assert.ok(!removed.joinedEventIds.includes(eventId));
  assert.ok(!removed.checkedInEventIds.includes(eventId));
  assert.ok(removed.threads.every(thread => thread.id !== threadId || thread.locked && thread.messages.length === 0));
  assert.equal((await f.store.get('SELECT status FROM event_members WHERE event_id=? AND user_id=?', [eventId, f.guest.id])).status, 'removed');
  assert.equal((await f.store.get('SELECT status FROM waitlist WHERE event_id=? AND user_id=?', [eventId, f.other.id])).status, 'offered');
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM reliability_history WHERE user_id=? AND event_id=?', [f.guest.id, eventId])).n, 0);
  await assert.rejects(f.service.messages(f.guest, threadId), error => error.status === 403);
  await assert.rejects(f.cmd(f.guest, 'chat.send', { threadId, text: 'Artık kadroda değilim' }), error => error.status === 403);
  const checkinCode = (await f.cmd(f.host, 'event.checkin.code', { eventId })).result.code;
  await assert.rejects(f.cmd(f.guest, 'event.checkin', { eventId, code: checkinCode }), code('PARTICIPANT_REQUIRED'));
  assert.ok(removed.notifications.some(notification => notification.targetId === eventId));
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM audit_log WHERE action='event.member.remove' AND target_id=?", [eventId])).n, 1);
});

test('removal and restoration require the organizer role, current version and a non-host participant', async t => {
  const f = await fixture(t), eventId = (await f.cmd(f.host, 'event.create', f.event())).result.id;
  await f.cmd(f.guest, 'event.join', { eventId });
  const expectedVersion = await version(f, eventId);
  await assert.rejects(f.cmd(f.other, 'event.member.remove', { eventId, userId: f.guest.id, expectedVersion }), code('FORBIDDEN'));
  await assert.rejects(f.cmd(f.host, 'event.member.remove', { eventId, userId: f.guest.id, expectedVersion: expectedVersion - 1 }), code('VERSION_CONFLICT'));
  await assert.rejects(f.cmd(f.host, 'event.member.remove', { eventId, userId: f.host.id, expectedVersion }), conflict);
  await remove(f, eventId, f.guest.id, f.admin);
  await assert.rejects(restore(f, eventId, f.guest.id, f.other), code('FORBIDDEN'));
  await assert.rejects(f.cmd(f.host, 'event.member.restore', { eventId, userId: f.guest.id, expectedVersion }), code('VERSION_CONFLICT'));
  await restore(f, eventId, f.guest.id, f.admin);
  assert.deepEqual(await wallet(f, f.guest), { balance: 20, locked: 0 });
  assert.ok(!(await f.service.bootstrap(f.guest)).joinedEventIds.includes(eventId));
});

test('removed athletes cannot bypass readmission via join, application or waitlist; restore never autojoins or charges', async t => {
  const f = await fixture(t), eventId = (await f.cmd(f.host, 'event.create', f.event())).result.id;
  await f.cmd(f.guest, 'event.join', { eventId }); await remove(f, eventId, f.guest.id);
  await assert.rejects(f.cmd(f.guest, 'event.join', { eventId }), conflict);
  await assert.rejects(f.cmd(f.guest, 'event.request', { eventId, position: 'Kaleci' }), conflict);
  await assert.rejects(f.cmd(f.guest, 'event.waitlist', { eventId, action: 'join' }), conflict);
  await assert.rejects(f.cmd(f.guest, 'event.waitlist', { eventId, action: 'accept' }), conflict);
  await restore(f, eventId, f.guest.id);
  assert.deepEqual(await wallet(f, f.guest), { balance: 20, locked: 0 });
  assert.ok(!(await f.service.bootstrap(f.guest)).joinedEventIds.includes(eventId));
  await f.cmd(f.guest, 'event.join', { eventId });
  assert.deepEqual(await wallet(f, f.guest), { balance: 10, locked: 10 });
  assert.ok((await f.service.bootstrap(f.guest)).joinedEventIds.includes(eventId));
});

test('removal cancels legacy pending applications and waitlist entries without refunding another event hold', async t => {
  const f = await fixture(t); await f.grant(f.host);
  const eventId = (await f.cmd(f.host, 'event.create', f.event())).result.id;
  const otherEventId = (await f.cmd(f.host, 'event.create', f.event())).result.id;
  await f.cmd(f.guest, 'event.join', { eventId }); await f.cmd(f.guest, 'event.join', { eventId: otherEventId });
  // Reproduce records persisted by older clients before pending/direct admission was reconciled.
  const requestId = randomUUID();
  await f.store.run('INSERT INTO join_requests(id,scope,scope_id,user_id,status,payload,created_at) VALUES(?,?,?,?,?,?,?)', [requestId, 'event', eventId, f.guest.id, 'pending', '{}', f.clock()]);
  await f.store.run('INSERT INTO waitlist(event_id,user_id,status,created_at) VALUES(?,?,?,?)', [eventId, f.guest.id, 'waiting', f.clock()]);
  await remove(f, eventId, f.guest.id);
  assert.notEqual((await f.store.get('SELECT status FROM join_requests WHERE id=?', [requestId])).status, 'pending');
  assert.ok(!['waiting', 'offered'].includes((await f.store.get('SELECT status FROM waitlist WHERE event_id=? AND user_id=?', [eventId, f.guest.id])).status));
  assert.deepEqual(await wallet(f, f.guest), { balance: 10, locked: 10 });
  assert.equal((await f.store.get('SELECT status FROM holds WHERE user_id=? AND scope=?', [f.guest.id, `event:${otherEventId}`])).status, 'held');
});

for (const kind of ['challenge', 'tournament']) test(`organizer removal cannot alter a frozen ${kind} fixture roster`, async t => {
  const f = await fixture(t); await f.grant(f.host);
  const home = (await f.cmd(f.host, 'team.create', { name: 'Ev sahibi takım', sport: 'FOOTBALL', area: 'Kadıköy' })).result.id;
  const away = (await f.cmd(f.guest, 'team.create', { name: 'Konuk takım', sport: 'FOOTBALL', area: 'Kadıköy' })).result.id;
  let eventId;
  if (kind === 'challenge') {
    const challengeId = (await f.cmd(f.host, 'challenge.create', { fromTeamId: home, toTeamId: away, proposedTime: 'Akşam 21:00', venue: 'Final sahası', startsAtEpochMillis: f.clock() + 7200000 })).result.id;
    eventId = (await f.cmd(f.guest, 'challenge.respond', { challengeId, accept: true })).result.eventId;
  } else {
    const tournamentId = (await f.cmd(f.host, 'tournament.create', { name: 'Resmi kadro kupası', sport: 'FOOTBALL', capacity: 2, dates: '10 Kasım' })).result.id;
    await f.cmd(f.host, 'tournament.register', { tournamentId, teamId: home });
    await f.cmd(f.guest, 'tournament.register', { tournamentId, teamId: away });
    const scheduled = await f.cmd(f.host, 'tournament.schedule', { tournamentId, startsAtEpochMillis: f.clock() + 7200000, venue: 'Final sahası' });
    eventId = (await f.store.get('SELECT event_id FROM tournament_fixtures WHERE id=?', [scheduled.result.fixtures[0]])).event_id;
  }
  const roster = await f.store.all('SELECT user_id,status,side FROM event_members WHERE event_id=? ORDER BY user_id', [eventId]);
  await assert.rejects(remove(f, eventId, f.guest.id), code('FIXED_ROSTER'));
  await assert.rejects(restore(f, eventId, f.guest.id), code('FIXED_ROSTER'));
  assert.deepEqual(await f.store.all('SELECT user_id,status,side FROM event_members WHERE event_id=? ORDER BY user_id', [eventId]), roster);
});
