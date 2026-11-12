import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID, generateKeyPairSync } from 'node:crypto';
import { fixture } from './helpers.mjs';
import { progression, weekNumber } from '../src/progression.mjs';
import { runJobs } from '../src/jobs.mjs';
import { notify } from '../src/model.mjs';
import { migrate } from '../src/store.mjs';

async function verifiedAttendance(f, endsAt, checkedIn) {
  const event = randomUUID();
  await f.store.run('INSERT INTO events(id,host_id,sport,status,capacity,starts_at,ends_at,commitment,city,district,payload,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',
    [event, f.host.id, 'FOOTBALL', 'VERIFIED', 2, endsAt - 3600000, endsAt, 0, 'İstanbul', 'Kadıköy', '{}', endsAt - 3600000]);
  await f.store.run('INSERT INTO event_members(event_id,user_id,status,checked_in_at,joined_at) VALUES(?,?,?,?,?)',
    [event, f.host.id, 'joined', checkedIn ? endsAt - 3600000 : null, endsAt - 3600000]);
  await f.store.run('INSERT INTO results(event_id,submitted_by,payload,status,dispute_until,finalized_at,created_at) VALUES(?,?,?,?,?,?,?)',
    [event, f.host.id, '{}', 'verified', endsAt, endsAt, endsAt]);
  return event;
}

test('perfect attendance stops at a verified no-show while total participation keeps checked-in matches', async t => {
  const f = await fixture(t);
  for (let i = 0; i < 11; i++) await verifiedAttendance(f, f.clock() - (i + 1) * 3600000, i !== 5);
  let progress = await progression({ db: f.store, now: f.clock() }, f.host.id);
  assert.equal(progress.badges.find(b => b.id === 'b1').earned, false);
  assert.equal(progress.badges.find(b => b.id === 'b1').progress, 0.5);
  assert.equal(progress.badges.find(b => b.id === 'b4').progress, 0.1);
  for (let i = 0; i < 5; i++) await verifiedAttendance(f, f.clock() - (i + 1) * 60000, true);
  progress = await progression({ db: f.store, now: f.clock() }, f.host.id);
  assert.equal(progress.badges.find(b => b.id === 'b1').earned, true);
});

async function reportCheckedMatch(f) {
  const event = (await f.cmd(f.host, 'event.create', f.event({ startsAtEpochMillis: f.clock() + 600000, durationMin: 15, capacity: 2 }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId: event });
  const code = (await f.cmd(f.host, 'event.checkin.code', { eventId: event })).result.code;
  for (const actor of [f.host, f.guest]) await f.cmd(actor, 'event.checkin', { eventId: event, code });
  const current = await f.store.get('SELECT version FROM events WHERE id=?', [event]);
  await f.cmd(f.host, 'event.transition', { eventId: event, status: 'IN_PROGRESS', expectedVersion: current.version });
  await f.cmd(f.host, 'result.submit', { eventId: event, homeScore: 2, awayScore: 1, sides: { home: [f.host.id], away: [f.guest.id] } });
  await f.cmd(f.guest, 'result.confirm', { eventId: event });
  return event;
}

test('Sunday matches verified on Monday grant their original weekly rewards exactly once', async t => {
  const f = await fixture(t);
  f.advance(Date.UTC(2026, 10, 8, 12) - f.clock());
  const matchWeek = weekNumber(f.clock());
  await reportCheckedMatch(f); f.advance(3600000); await reportCheckedMatch(f);
  f.advance(25 * 3600000);
  assert.equal(weekNumber(f.clock()), matchWeek + 1);
  await runJobs(f.service); await runJobs(f.service);
  const rewards = await f.store.all("SELECT reward_key FROM rewards WHERE user_id=? AND reward_key LIKE 'weekly:%'", [f.host.id]);
  assert.deepEqual(rewards.map(r => r.reward_key).sort(), [`weekly:${matchWeek}:checkin`, `weekly:${matchWeek}:rematch`].sort());
  const own = await f.service.bootstrap(f.host);
  assert.equal(own.wallet.balance, 40);
  assert.ok(own.progression.missions.every(m => m.progress === 0 && m.claimed === false));
});

test('opponent missions exclude teammates and require opposite sides in the earlier rematch too', async t => {
  const f = await fixture(t);
  const previous = await verifiedAttendance(f, f.clock() - 7200000, true);
  const current = await verifiedAttendance(f, f.clock() - 3600000, true);
  for (const event of [previous, current]) {
    await f.store.run("UPDATE event_members SET side='home' WHERE event_id=?", [event]);
    for (const actor of [f.guest, f.other, f.admin]) await f.store.run('INSERT INTO event_members(event_id,user_id,status,side,checked_in_at,joined_at) VALUES(?,?,?,?,?,?)',
      [event, actor.id, 'joined', 'home', f.clock() - 10800000, f.clock() - 10800000]);
  }
  const mission = async name => (await progression({ db: f.store, now: f.clock() }, f.host.id)).missions.find(m => m.id === name).progress;
  assert.equal(await mission('opponents'), 0); assert.equal(await mission('rematch'), 0);
  await f.store.run("UPDATE event_members SET side='away' WHERE event_id=? AND user_id=?", [current, f.other.id]);
  assert.equal(await mission('opponents'), 1); assert.equal(await mission('rematch'), 0);
  await f.store.run("UPDATE event_members SET side='away' WHERE event_id=? AND user_id=?", [previous, f.other.id]);
  assert.equal(await mission('opponents'), 1); assert.equal(await mission('rematch'), 1);
});

for (const kind of ['challenge', 'tournament']) test(`${kind} fixtures reject ordinary roster changes and outsider join routes`, async t => {
  const f = await fixture(t); await f.grant(f.host);
  const home = (await f.cmd(f.host, 'team.create', { name: 'Ev sahibi takım', sport: 'FOOTBALL', area: 'Kadıköy' })).result.id;
  const away = (await f.cmd(f.guest, 'team.create', { name: 'Konuk takım', sport: 'FOOTBALL', area: 'Kadıköy' })).result.id;
  let event;
  if (kind === 'challenge') {
    const challenge = (await f.cmd(f.host, 'challenge.create', { fromTeamId: home, toTeamId: away, proposedTime: 'Akşam 21:00', venue: 'Final sahası', startsAtEpochMillis: f.clock() + 7200000 })).result.id;
    event = (await f.cmd(f.guest, 'challenge.respond', { challengeId: challenge, accept: true })).result.eventId;
  } else {
    const tournament = (await f.cmd(f.host, 'tournament.create', { name: 'Kadro koruma kupası', sport: 'FOOTBALL', capacity: 2, dates: '10 Kasım' })).result.id;
    await f.cmd(f.host, 'tournament.register', { tournamentId: tournament, teamId: home });
    await f.cmd(f.guest, 'tournament.register', { tournamentId: tournament, teamId: away });
    const scheduled = await f.cmd(f.host, 'tournament.schedule', { tournamentId: tournament, startsAtEpochMillis: f.clock() + 7200000, venue: 'Final sahası' });
    event = (await f.store.get('SELECT event_id FROM tournament_fixtures WHERE id=?', [scheduled.result.fixtures[0]])).event_id;
  }
  const originalRoster = await f.store.all("SELECT user_id,side FROM event_members WHERE event_id=? AND status='joined' ORDER BY side", [event]);
  const fixed = error => error.code === 'FIXED_ROSTER';
  await assert.rejects(f.cmd(f.guest, 'event.leave', { eventId: event }), fixed);
  await assert.rejects(f.cmd(f.other, 'event.join', { eventId: event }), fixed);
  await assert.rejects(f.cmd(f.other, 'event.request', { eventId: event }), fixed);
  for (const action of ['join', 'accept', 'leave']) await assert.rejects(f.cmd(f.other, 'event.waitlist', { eventId: event, action }), fixed);
  const roster = await f.store.all("SELECT user_id,side FROM event_members WHERE event_id=? AND status='joined' ORDER BY side", [event]);
  assert.deepEqual(roster, originalRoster);
  assert.deepEqual(roster.map(m => m.user_id).sort(), [f.guest.id, f.host.id].sort());
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM join_requests WHERE scope_id=?', [event])).n, 0);
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM waitlist WHERE event_id=?', [event])).n, 0);
});

async function queuedPushes(f, count, dailyCap = 100) {
  f.config.pushUrl = 'https://push.invalid/send';
  await f.cmd(f.host, 'preferences.save', { notifications: { quietHours: false, dailyCap } });
  await f.cmd(f.host, 'device.register', { platform: 'android', token: 'registered-device-test-token' });
  for (let i = 0; i < count; i++) await notify({ db: f.store, now: f.clock() }, f.host.id, 'social', `Duyuru ${i}`, 'Teslimat testi', 'PASSPORT', '', `critical-push:${i}`);
}

test('push workers claim one row at a time so a long batch cannot expire unsent rows', async t => {
  const f = await fixture(t); await queuedPushes(f, 14);
  const delivered = []; let overlapping = false;
  t.mock.method(globalThis, 'fetch', async (_url, options) => {
    delivered.push(options.headers['Idempotency-Key']); f.advance(7000);
    if (delivered.length === 10 && !overlapping) { overlapping = true; await runJobs(f.service); }
    return { ok: true };
  });
  await runJobs(f.service);
  assert.equal(overlapping, true); assert.equal(delivered.length, 14); assert.equal(new Set(delivered).size, 14);
  const jobs = await f.store.all('SELECT status,attempts,claim_token,delivered_at FROM outbox');
  assert.ok(jobs.every(j => j.status === 'delivered' && j.attempts === 1 && j.claim_token === null && j.delivered_at !== null));
});

test('a stale push worker cannot overwrite a successor claim after a provider response', async t => {
  const f = await fixture(t); await queuedPushes(f, 1);
  t.mock.method(globalThis, 'fetch', async (_url, options) => {
    await f.store.run("UPDATE outbox SET claim_token='successor-claim' WHERE id=?", [options.headers['Idempotency-Key']]);
    return { ok: true };
  });
  await runJobs(f.service);
  const job = await f.store.get('SELECT status,attempts,claim_token,delivered_at FROM outbox');
  assert.equal(job.status, 'processing'); assert.equal(job.claim_token, 'successor-claim');
  assert.equal(job.attempts, 0); assert.equal(job.delivered_at, null);
});

test('daily push quota counts actual delivery dates for yesterday backlog and concurrent claims', async t => {
  const f = await fixture(t); await queuedPushes(f, 3, 1);
  await f.store.run('UPDATE outbox SET created_at=?', [f.clock() - 86400000]);
  let delivered = 0, overlapping = false;
  t.mock.method(globalThis, 'fetch', async () => {
    delivered++;
    if (!overlapping) { overlapping = true; await runJobs(f.service); }
    return { ok: true };
  });
  await runJobs(f.service); await runJobs(f.service);
  assert.equal(delivered, 1);
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM outbox WHERE status='delivered' AND delivered_at=?", [f.clock()])).n, 1);
  f.advance(86400000); await runJobs(f.service);
  assert.equal(delivered, 2);
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM outbox WHERE status='pending'")).n, 1);
});

test('delivery metadata migration can be reapplied without losing queue state', async t => {
  const f = await fixture(t); await queuedPushes(f, 1);
  await f.store.run("UPDATE outbox SET claim_token='kept-claim',delivered_at=?", [f.clock()]);
  await migrate(f.store); await migrate(f.store);
  const job = await f.store.get('SELECT claim_token,delivered_at FROM outbox');
  assert.equal(job.claim_token, 'kept-claim'); assert.equal(job.delivered_at, f.clock());
});

test('native push skips invalid-only device sets without provider acceptance or a delivery timestamp', async t => {
  const f = await fixture(t); await queuedPushes(f, 1, 1);
  f.config.pushUrl = null;
  Object.assign(f.config, { apnsKeyId: 'test-key', apnsTeamId: 'test-team', apnsBundleId: 'app.matchora.test', apnsPrivateKey: 'unused-invalid-token-key' });
  await f.cmd(f.host, 'device.remove', { token: 'registered-device-test-token' });
  for (const suffix of ['one', 'two']) await f.cmd(f.host, 'device.register', { platform: 'ios', token: `invalid-apns-token-${suffix}` });
  t.mock.method(globalThis, 'fetch', async () => { throw new Error('No provider request is expected for invalid APNs tokens'); });
  await runJobs(f.service); await runJobs(f.service);
  const job = await f.store.get('SELECT status,attempts,claim_token,delivered_at FROM outbox');
  assert.equal(job.status, 'skipped'); assert.equal(job.attempts, 1);
  assert.equal(job.claim_token, null); assert.equal(job.delivered_at, null);
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM push_devices WHERE user_id=?', [f.host.id])).n, 0);
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM notifications WHERE user_id=?', [f.host.id])).n, 1);
});

test('native push records provider acceptance when a valid device succeeds alongside an invalid token', async t => {
  const f = await fixture(t); await queuedPushes(f, 1);
  const pair = generateKeyPairSync('rsa', { modulusLength: 2048 });
  f.config.pushUrl = null;
  Object.assign(f.config, { apnsKeyId: 'test-key', apnsTeamId: 'test-team', apnsBundleId: 'app.matchora.test', apnsPrivateKey: 'unused-invalid-token-key',
    fcmProjectId: 'matchora-test', fcmClientEmail: 'critical-native-push@example.test', fcmPrivateKey: pair.privateKey.export({ type: 'pkcs8', format: 'pem' }) });
  await f.cmd(f.host, 'device.register', { platform: 'ios', token: 'invalid-apns-token-test' });
  let accepted = 0;
  t.mock.method(globalThis, 'fetch', async url => {
    if (url.includes('oauth2.googleapis.com')) return { ok: true, json: async () => ({ access_token: 'test-native-provider-token', expires_in: 3600 }) };
    assert.ok(url.includes('fcm.googleapis.com')); accepted++; return { ok: true };
  });
  await runJobs(f.service); await runJobs(f.service);
  const job = await f.store.get('SELECT status,attempts,claim_token,delivered_at FROM outbox');
  assert.equal(accepted, 1); assert.equal(job.status, 'delivered'); assert.equal(job.attempts, 1);
  assert.equal(job.claim_token, null); assert.equal(job.delivered_at, f.clock());
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM push_devices WHERE user_id=?', [f.host.id])).n, 1);
});
