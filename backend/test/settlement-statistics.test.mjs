import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { settleResult } from '../src/rating.mjs';

async function confirmedMatch(f) {
  const eventId = (await f.cmd(f.host, 'event.create', f.event({ startsAtEpochMillis: f.clock() + 600000, capacity: 2 }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId });
  const code = (await f.cmd(f.host, 'event.checkin.code', { eventId })).result.code;
  for (const actor of [f.host, f.guest]) await f.cmd(actor, 'event.checkin', { eventId, code });
  const event = await f.store.get('SELECT version FROM events WHERE id=?', [eventId]);
  await f.cmd(f.host, 'event.transition', { eventId, status: 'IN_PROGRESS', expectedVersion: event.version });
  await f.cmd(f.host, 'result.submit', { eventId, homeScore: 2, awayScore: 1, sides: { home: [f.host.id], away: [f.guest.id] } });
  await f.cmd(f.guest, 'result.confirm', { eventId });
  return f.store.get('SELECT * FROM results WHERE event_id=?', [eventId]);
}

async function rawProfile(f, actor = f.host) {
  return JSON.parse((await f.store.get('SELECT payload FROM profiles WHERE user_id=?', [actor.id])).payload);
}

async function settle(f, result) {
  await f.store.transaction(db => settleResult(f.service.ctx(db), result));
}

test('legacy missing and null match statistics settle to finite counters and preserve the earned rating', async t => {
  const f = await fixture(t);
  const result = await confirmedMatch(f);
  const profile = await rawProfile(f);
  delete profile.verifiedMatches;
  delete profile.reliability;
  delete profile.skills[0].rating;
  profile.skills[0].verifiedMatches = null;
  profile.skills[0].wonMatches = null;
  profile.skills[0].winRate = null;
  await f.store.run('UPDATE profiles SET payload=? WHERE user_id=?', [JSON.stringify(profile), f.host.id]);
  f.advance(25 * 3600000);
  await settle(f, result);
  const saved = await rawProfile(f);
  assert.equal(saved.verifiedMatches, 1);
  assert.equal(saved.reliability, 100);
  assert.deepEqual([saved.skills[0].rating, saved.skills[0].verifiedMatches, saved.skills[0].wonMatches, saved.skills[0].winRate], [1012, 1, 1, 100]);
  assert.deepEqual(saved.skillArchive.FOOTBALL, saved.skills[0]);
  assert.equal((await f.store.get('SELECT delta FROM reliability_history WHERE user_id=? AND event_id=?', [f.host.id, result.event_id])).delta, 0);
});

test('an archived sport receives real attendance credit without resetting its earned rating or granting ineligible MMR', async t => {
  const f = await fixture(t);
  const result = await confirmedMatch(f);
  const profile = await rawProfile(f);
  profile.skillArchive = { FOOTBALL: { ...profile.skills[0], rating: 1175, verifiedMatches: 2, wonMatches: 1, winRate: 50 } };
  profile.skills = [{ ...profile.skills[0], sport: 'TENNIS' }];
  await f.store.run('UPDATE profiles SET payload=? WHERE user_id=?', [JSON.stringify(profile), f.host.id]);
  f.advance(25 * 3600000);
  await settle(f, result);
  const saved = await rawProfile(f);
  assert.equal(saved.verifiedMatches, 1);
  assert.equal(saved.skills[0].verifiedMatches, 0);
  assert.deepEqual([saved.skillArchive.FOOTBALL.rating, saved.skillArchive.FOOTBALL.verifiedMatches, saved.skillArchive.FOOTBALL.wonMatches, saved.skillArchive.FOOTBALL.winRate], [1175, 3, 2, 67]);
  const changes = JSON.parse((await f.store.get('SELECT payload FROM results WHERE event_id=?', [result.event_id])).payload).ratingChanges;
  assert.deepEqual(changes.find(change => change.userId === f.host.id), { userId: f.host.id, before: 1175, after: 1175, delta: 0, reason: 'PROFILE_SPORT_REQUIRED' });
});

test('settlement rechecks its deadline and a stale replay cannot overwrite the finalized evidence or award twice', async t => {
  const f = await fixture(t);
  const result = await confirmedMatch(f);
  await settle(f, result);
  assert.equal((await f.store.get('SELECT status FROM results WHERE event_id=?', [result.event_id])).status, 'pending');
  assert.equal((await rawProfile(f)).verifiedMatches, 0);
  f.advance(25 * 3600000);
  await settle(f, result);
  const savedResult = await f.store.get('SELECT * FROM results WHERE event_id=?', [result.event_id]);
  const savedEvent = await f.store.get('SELECT version FROM events WHERE id=?', [result.event_id]);
  const savedProfile = await f.store.get('SELECT * FROM profiles WHERE user_id=?', [f.host.id]);
  await settle(f, result);
  assert.deepEqual(await f.store.get('SELECT * FROM results WHERE event_id=?', [result.event_id]), savedResult);
  assert.deepEqual(await f.store.get('SELECT version FROM events WHERE id=?', [result.event_id]), savedEvent);
  assert.deepEqual(await f.store.get('SELECT * FROM profiles WHERE user_id=?', [f.host.id]), savedProfile);
  assert.equal(JSON.parse(savedResult.payload).ratingChanges.length, 2);
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM rating_history WHERE event_id=?', [result.event_id])).n, 2);
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM audit_log WHERE action='result.finalize' AND target_id=?", [result.event_id])).n, 1);
});
