import test from 'node:test';
import assert from 'node:assert/strict';
import { generateKeyPairSync, createSign } from 'node:crypto';
import { fixture } from './helpers.mjs';
import { runJobs } from '../src/jobs.mjs';
import { eloDelta, confirmationState } from '../src/rating.mjs';
import { Auth, id } from '../src/auth.mjs';

async function reportedMatch(f, { mode = 'COMPETITIVE', checked = ['host', 'guest'], homeScore = 3, awayScore = 1 } = {}) {
  const event = (await f.cmd(f.host, 'event.create', f.event({ startsAtEpochMillis: f.clock() + 600000, capacity: 2, mode }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId: event });
  const code = (await f.cmd(f.host, 'event.checkin.code', { eventId: event })).result.code;
  for (const name of checked) await f.cmd(f[name], 'event.checkin', { eventId: event, code });
  const saved = await f.store.get('SELECT version FROM events WHERE id=?', [event]);
  await f.cmd(f.host, 'event.transition', { eventId: event, status: 'IN_PROGRESS', expectedVersion: saved.version });
  await f.cmd(f.host, 'result.submit', { eventId: event, homeScore, awayScore, sides: { home: [f.host.id], away: [f.guest.id] } });
  return event;
}
async function verifiedMatch(f, options = {}) {
  const event = await reportedMatch(f, options);
  await f.cmd(f.guest, 'result.confirm', { eventId: event });
  f.advance(25 * 3600000); await runJobs(f.service); return event;
}
async function physicalSlot(f, priceMinor = 100001) {
  const venue = (await f.cmd(f.admin, 'venue.create', { name: 'Gerçek saha', city: 'İstanbul', area: 'Kadıköy', sports: ['FOOTBALL'], courts: 1, cancellationHours: 0 })).result.id;
  const slot = (await f.cmd(f.admin, 'venue.slot.create', { venueId: venue, court: 'Saha 1', startsAtEpochMillis: f.clock() + 3600000, durationMin: 60, priceMinor })).result.id;
  const booking = (await f.cmd(f.host, 'booking.create', { slotId: slot, splitCount: 4 })).result.id;
  return { venue, slot, booking };
}

test('MMR Elo rewards an upset, handles draws and uses an independent opponent majority', () => {
  assert.equal(eloDelta(1000, 1000, 1), 12); assert.equal(eloDelta(1000, 1000, 0), -12); assert.equal(eloDelta(1000, 1000, 0.5), 0);
  assert.ok(eloDelta(1000, 1400, 1) > 12); assert.ok(eloDelta(1400, 1000, 1) < 12);
  const members = Array.from({ length: 14 }, (_, i) => ({ user_id: String(i), side: i < 7 ? 'home' : 'away', checked_in_at: 1 }));
  const result = { submitted_by: '0' };
  assert.deepEqual(confirmationState(members, result, ['1', '2', '3', '7', '8', '9', '10'].map(user_id => ({ user_id }))), { required: 4, confirmed: 4 });
});

test('no-show cannot confirm, gain MMR, gain match credit or earn participation missions', async t => {
  const f = await fixture(t), event = await reportedMatch(f, { checked: ['host'] });
  await assert.rejects(f.cmd(f.guest, 'result.confirm', { eventId: event }), e => e.code === 'CHECKIN_REQUIRED');
  f.advance(25 * 3600000); await runJobs(f.service);
  assert.equal((await f.service.bootstrap(f.host)).results[0].status, 'pending');
  await f.cmd(f.admin, 'admin.result.resolve', { eventId: event, approve: true, reason: 'Katılım kayıtları operatörce incelendi.' });
  await runJobs(f.service);
  const own = await f.service.bootstrap(f.host), absent = await f.service.bootstrap(f.guest);
  assert.equal(absent.profile.skills[0].rating, 1000); assert.equal(absent.profile.verifiedMatches, 0);
  assert.equal(absent.profile.reliability, 90); assert.equal(absent.wallet.locked, 0); assert.equal(absent.wallet.balance, 10);
  assert.ok(absent.progression.missions.every(m => m.progress === 0));
  assert.equal(own.profile.skills[0].rating, 1000); assert.equal(own.results[0].rating.reason, 'OPPONENT_CHECKIN_REQUIRED');
});

test('social matches count real attendance but never change MMR; pending state explains the wait', async t => {
  const f = await fixture(t), event = await reportedMatch(f, { mode: 'SOCIAL' });
  let snapshot = await f.service.bootstrap(f.host);
  assert.equal(snapshot.results[0].rating.reason, 'SOCIAL_MATCH');
  await f.cmd(f.guest, 'result.confirm', { eventId: event });
  f.advance(25 * 3600000); await runJobs(f.service);
  snapshot = await f.service.bootstrap(f.host);
  assert.equal(snapshot.profile.skills[0].rating, 1000); assert.equal(snapshot.profile.verifiedMatches, 1);
  assert.equal(snapshot.results[0].rating.delta, 0);
});

test('earned MMR survives removing and restoring a sport and active matches protect their sport', async t => {
  const f = await fixture(t), event = await reportedMatch(f);
  let own = await f.service.bootstrap(f.host);
  const tennis = { sport: 'TENNIS', level: 'INTERMEDIATE', position: '' };
  await assert.rejects(f.cmd(f.host, 'profile.save', { ...f.passport(), expectedVersion: own.profile.version, skills: [tennis] }), e => e.code === 'SPORT_IN_USE');
  await f.cmd(f.guest, 'result.confirm', { eventId: event }); f.advance(25 * 3600000); await runJobs(f.service);
  own = await f.service.bootstrap(f.host); assert.equal(own.profile.skills[0].rating, 1012);
  await f.cmd(f.host, 'profile.save', { ...f.passport(), expectedVersion: own.profile.version, skills: [tennis] });
  own = await f.service.bootstrap(f.host);
  await f.cmd(f.host, 'profile.save', { ...f.passport(), expectedVersion: own.profile.version, skills: [tennis, f.passport().skills[0]] });
  own = await f.service.bootstrap(f.host);
  assert.equal(own.profile.skills.find(s => s.sport === 'FOOTBALL').rating, 1012);
  assert.equal(own.profile.skills.find(s => s.sport === 'FOOTBALL').verifiedMatches, 1);
});

test('rating history records the actual change at the zero floor and feedback updates fair play immediately', async t => {
  const f = await fixture(t);
  const saved = await f.store.get('SELECT payload FROM profiles WHERE user_id=?', [f.host.id]), profile = JSON.parse(saved.payload);
  profile.skills[0].rating = 5; await f.store.run('UPDATE profiles SET payload=? WHERE user_id=?', [JSON.stringify(profile), f.host.id]);
  const other = JSON.parse((await f.store.get('SELECT payload FROM profiles WHERE user_id=?', [f.guest.id])).payload);other.skills[0].rating=5;await f.store.run('UPDATE profiles SET payload=? WHERE user_id=?',[JSON.stringify(other),f.guest.id]);
  const event = await verifiedMatch(f, { homeScore: 0, awayScore: 1 });
  let own = await f.service.bootstrap(f.host);
  assert.equal(own.profile.skills[0].rating, 0); assert.equal(own.ratingHistory[0].delta, -5);
  await f.cmd(f.guest, 'feedback.submit', { eventId: event, targetId: f.host.id, fairPlay: 4, playAgain: true });
  own = await f.service.bootstrap(f.host); assert.equal(own.profile.fairPlay, 4);
});

test('tournament capacity, format, roster MMR and phone requirements are enforced', async t => {
  const f = await fixture(t); await f.grant(f.host);
  const team = (await f.cmd(f.host, 'team.create', { name: 'Koşullu takım', sport: 'FOOTBALL', area: 'Kadıköy' })).result.id;
  const cup = { name: 'Koşullu kupa', sport: 'FOOTBALL', format: 'Eleme', formatCode: 'KNOCKOUT', capacity: 2, dates: '10 Kasım' };
  await assert.rejects(f.cmd(f.host, 'tournament.create', { ...cup, capacity: 3 }), e => e.code === 'INVALID_CAPACITY');
  await assert.rejects(f.cmd(f.host, 'tournament.create', { ...cup, formatCode: 'SWISS' }), e => e.code === 'INVALID_INPUT');
  const high = (await f.cmd(f.host, 'tournament.create', { ...cup, minRating: 1200 })).result.id;
  await assert.rejects(f.cmd(f.host, 'tournament.register', { tournamentId: high, teamId: team }), e => e.code === 'TOURNAMENT_RATING_REQUIRED');
  const verified = (await f.cmd(f.host, 'tournament.create', { ...cup, verifiedOnly: true })).result.id;
  await assert.rejects(f.cmd(f.host, 'tournament.register', { tournamentId: verified, teamId: team }), e => e.code === 'TOURNAMENT_VERIFICATION_REQUIRED');
  const open = (await f.cmd(f.host, 'tournament.create', { ...cup, formatCode: 'ROUND_ROBIN', capacity: 6, minRating: 1000 })).result.id;
  const registration = await f.cmd(f.host, 'tournament.register', { tournamentId: open, teamId: team });
  const saved = registration.snapshot.tournaments.find(c => c.id === open);
  assert.equal(saved.capacity, 6); assert.equal(saved.isLeague, true); assert.equal(saved.minRating, 1000);
});

test('community editing detects stale versions and removing a member closes future private participation', async t => {
  const f = await fixture(t);
  const community = (await f.cmd(f.host, 'community.create', { name: 'Özel kampüs', type: 'CAMPUS', access: 'OPEN', city: 'İstanbul', area: 'Kadıköy' })).result.id;
  await f.cmd(f.guest, 'community.join', { communityId: community });
  const event = (await f.cmd(f.host, 'event.create', f.event({ networkScope: 'CAMPUS', communityId: community }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId: event });
  const update = { communityId: community, expectedVersion: 1, name: 'Güncel kulüp', description: 'Yeni açıklama', access: 'INVITE' };
  await assert.rejects(f.cmd(f.guest, 'community.update', update), e => e.code === 'FORBIDDEN');
  await f.cmd(f.host, 'community.update', update);
  await assert.rejects(f.cmd(f.host, 'community.update', update), e => e.code === 'VERSION_CONFLICT');
  await f.cmd(f.host, 'community.member.remove', { communityId: community, userId: f.guest.id });
  const guest = await f.service.bootstrap(f.guest);
  assert.equal(guest.wallet.locked, 0); assert.equal(guest.events.some(e => e.id === event), false);
  await assert.rejects(f.service.event(f.guest, event), e => e.code === 'NOT_FOUND');
});

test('reviews require completed own bookings, remain private until moderated and aggregate real ratings', async t => {
  const f = await fixture(t), { venue, booking } = await physicalSlot(f);
  await assert.rejects(f.cmd(f.host, 'venue.review.save', { bookingId: booking, rating: 4, comment: 'İyi saha' }), e => e.code === 'COMPLETED_BOOKING_REQUIRED');
  await f.cmd(f.admin, 'booking.confirm', { bookingId: booking });
  await f.cmd(f.admin, 'booking.payment.record', { bookingId: booking, reference: 'makbuz-001' });
  f.advance(3 * 3600000); await f.cmd(f.admin, 'booking.complete', { bookingId: booking });
  await assert.rejects(f.cmd(f.guest, 'venue.review.save', { bookingId: booking, rating: 4 }), e => e.code === 'COMPLETED_BOOKING_REQUIRED');
  const review = (await f.cmd(f.host, 'venue.review.save', { bookingId: booking, rating: 4, comment: 'Temiz ve düzenli saha.' })).result.id;
  assert.equal((await f.service.venueReviews(null, venue)).items.length, 0);
  assert.equal((await f.service.venueReviews(f.host, venue)).items[0].status, 'pending');
  await f.cmd(f.admin, 'admin.venue.review', { reviewId: review, approved: true, reason: 'Doğrulanmış rezervasyon yorumu incelendi.' });
  const snapshot = await f.service.bootstrap(f.host), saved = snapshot.venues.find(v => v.id === venue);
  assert.equal(saved.rating, 4); assert.equal(saved.reviews, 1); assert.equal(snapshot.venuePayments[0].amountMinor, 100001);
  await assert.rejects(f.cmd(f.host, 'venue.review.save', { bookingId: booking, rating: 2, expectedVersion: 1 }), e => e.code === 'VERSION_CONFLICT');
  await f.cmd(f.admin, 'admin.feature.set', { name: 'venues', enabled: false });
  await f.cmd(f.host, 'venue.review.delete', { reviewId: review });
  assert.equal((await f.service.venueReviews(null, venue)).items.length, 0);
});

test('offline refund is authorized, idempotent, frees the slot and never pretends to send money', async t => {
  const f = await fixture(t), { slot, booking } = await physicalSlot(f);
  await f.cmd(f.admin, 'booking.confirm', { bookingId: booking });
  await f.cmd(f.admin, 'booking.payment.record', { bookingId: booking, reference: 'tesiste-odendi-1' });
  await assert.rejects(f.cmd(f.guest, 'booking.refund.record', { bookingId: booking, reference: 'fake-refund' }), e => e.code === 'FORBIDDEN');
  const payload = { bookingId: booking, reference: 'tesiste-iade-1' };
  const first = await f.cmd(f.admin, 'booking.refund.record', payload, 'refund-record-key-1');
  assert.equal(first.result.method, 'offline_refund_record');
  assert.equal((await f.cmd(f.admin, 'booking.refund.record', payload, 'refund-record-key-1')).replayed, true);
  assert.equal((await f.service.bootstrap(f.host)).venuePayments[0].status, 'REFUNDED');
  await f.cmd(f.guest, 'booking.create', { slotId: slot, splitCount: 1 });
});

test('operator data is tenant scoped and revoked operator role cannot read or write venue management', async t => {
  const f = await fixture(t);
  await assert.rejects(f.service.operatorVenues(f.host), e => e.code === 'FORBIDDEN');
  for (const actor of [f.host, f.other]) await f.cmd(f.admin, 'admin.operator.assign', { userId: actor.id, role: 'venue_owner', reason: 'İşletmeci hesabı doğrulanarak atandı.' });
  f.host = await f.auth.actor(f.sessions.host.accessToken); f.other = await f.auth.actor(f.sessions.other.accessToken);
  const venue = (await f.cmd(f.host, 'venue.create', { name: 'Yalnız benim tesisim', area: 'Kadıköy', sports: ['FOOTBALL'], courts: 1 })).result.id;
  assert.equal((await f.service.operatorVenues(f.host)).items.length, 1);
  assert.equal((await f.service.operatorVenues(f.other)).items.length, 0);
  await f.cmd(f.admin, 'admin.operator.assign', { userId: f.host.id, role: 'user', reason: 'İşletmeci erişimi kapatılıyor.' });
  await assert.rejects(f.service.operatorVenues(f.host), e => e.code === 'FORBIDDEN');
  await assert.rejects(f.cmd(f.host, 'venue.slot.create', { venueId: venue, court: 'Saha', startsAtEpochMillis: f.clock() + 3600000, durationMin: 60, priceMinor: 10000 }), e => e.code === 'FORBIDDEN');
});

test('emergency producer honors district, opt-out, blocks, membership and event deduplication', async t => {
  const f = await fixture(t), event = (await f.cmd(f.host, 'event.create', f.event({ emergency: true, listingType: 'SEEK_PLAYERS' }))).result.id;
  await f.cmd(f.guest, 'preferences.save', { notifications: { emergencyFill: false } });
  await f.cmd(f.other, 'user.block', { userId: f.host.id, blocked: true });
  await runJobs(f.service); f.advance(6 * 60000); await runJobs(f.service);
  const rows = await f.store.all("SELECT user_id FROM notifications WHERE category='emergencyFill'");
  assert.deepEqual(rows.map(r => r.user_id), [f.admin.id]);
  assert.ok((await f.service.bootstrap(f.admin)).notifications.some(n => n.targetId === event));
});

test('newly published match sends push to nearby athletes with matching sport and honors opt-out', async t => {
  const f = await fixture(t);
  const event = (await f.cmd(f.host, 'event.create', f.event({ emergency: false, listingType: 'SEEK_PLAYERS' }))).result.id;
  await f.cmd(f.guest, 'preferences.save', { notifications: { upcomingMatch: false } });
  await f.cmd(f.other, 'user.block', { userId: f.host.id, blocked: true });
  await runJobs(f.service);
  const rows = await f.store.all("SELECT user_id FROM notifications WHERE category='matches' AND dedupe LIKE 'new-match:%'");
  assert.deepEqual(rows.map(r => r.user_id), [f.admin.id]);
  assert.ok((await f.service.bootstrap(f.admin)).notifications.some(n => n.targetId === event && n.title === 'İlçende oyuncu aranıyor'));
});

test('weekly digest and mutual rematch suggestions are once only and late feedback affects fair play', async t => {
  const f = await fixture(t), event = await verifiedMatch(f);
  await f.cmd(f.host, 'feedback.submit', { eventId: event, targetId: f.guest.id, fairPlay: 5, playAgain: true });
  await f.cmd(f.guest, 'feedback.submit', { eventId: event, targetId: f.host.id, fairPlay: 4, playAgain: true });
  f.advance(2 * 86400000); await runJobs(f.service); f.advance(6 * 60000); await runJobs(f.service);
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM notifications WHERE category='rematch'")).n, 2);
  await f.cmd(f.guest, 'preferences.save', { notifications: { weeklyDigest: false } });
  f.advance(5 * 86400000); await runJobs(f.service); f.advance(6 * 60000); await runJobs(f.service);
  const digest = await f.store.all("SELECT user_id FROM notifications WHERE category='weeklyDigest'");
  assert.deepEqual(digest.map(r => r.user_id), [f.host.id]);
});

test('expired delivery leases recover and feature shutdown still permits reservation cancellation', async t => {
  const f = await fixture(t), { booking } = await physicalSlot(f);
  const job = id();
  await f.store.run('INSERT INTO outbox(id,user_id,kind,payload,status,next_at,created_at) VALUES(?,?,?,?,?,?,?)', [job, f.host.id, 'push', '{}', 'processing', f.clock() - 1, f.clock()]);
  await runJobs(f.service);
  assert.equal((await f.store.get('SELECT status FROM outbox WHERE id=?', [job])).status, 'pending');
  await f.cmd(f.admin, 'admin.feature.set', { name: 'venues', enabled: false });
  await f.cmd(f.host, 'booking.cancel', { bookingId: booking });
});


test('weekly digest waits for Sunday dispute windows and attributes MMR to the match week', async t => {
  const f = await fixture(t);
  f.advance(Date.UTC(2026, 10, 8, 12) - f.clock());
  await verifiedMatch(f);
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM notifications WHERE category='weeklyDigest'")).n, 0);
  f.advance(86400000); await runJobs(f.service);
  const digest = await f.store.get("SELECT payload FROM notifications WHERE user_id=? AND category='weeklyDigest'", [f.host.id]);
  assert.ok(digest);
  assert.match(JSON.parse(digest.payload).body, /1 doğrulanmış katılım.*\+12 MMR/);
});

test('firebase id token verifies RS256 signature, issuer, audience and normalizes phone', async t => {
  const f = await fixture(t);
  const { privateKey, publicKey } = generateKeyPairSync('rsa', { modulusLength: 2048 });
  const publicPem = publicKey.export({ type: 'spki', format: 'pem' });
  const signJwt = (header, claims, key = privateKey) => {
    const unsigned = `${Buffer.from(JSON.stringify(header)).toString('base64url')}.${Buffer.from(JSON.stringify(claims)).toString('base64url')}`;
    const sig = createSign('RSA-SHA256').update(unsigned).sign(key).toString('base64url');
    return `${unsigned}.${sig}`;
  };
  const auth = new Auth(f.store, { ...f.config, fcmProjectId: 'matchora-c5ddc' }, f.clock, async url => {
    assert.match(url, /securetoken@system\.gserviceaccount\.com/);
    return new Response(JSON.stringify({ kid1: publicPem }), { status: 200, headers: { 'cache-control': 'public, max-age=3600' } });
  });
  const nowSec = Math.floor(f.clock() / 1000);
  const validToken = signJwt(
    { alg: 'RS256', kid: 'kid1', typ: 'JWT' },
    { aud: 'matchora-c5ddc', iss: 'https://securetoken.google.com/matchora-c5ddc', sub: 'fb-uid-1', phone_number: '+905350705672', iat: nowSec - 10, auth_time: nowSec - 10, exp: nowSec + 3600 }
  );
  const session = await auth.exchangeIdToken({ provider: 'firebase', idToken: validToken });
  assert.ok(session.accessToken);
  assert.equal(session.profileHints.phone, '+905350705672');
  const otherKey = generateKeyPairSync('rsa', { modulusLength: 2048 }).privateKey;
  const forged = signJwt({ alg: 'RS256', kid: 'kid1' }, { aud: 'matchora-c5ddc', iss: 'https://securetoken.google.com/matchora-c5ddc', sub: 'fb-uid-1', exp: nowSec + 3600, iat: nowSec }, otherKey);
  await assert.rejects(auth.exchangeIdToken({ provider: 'firebase', idToken: forged }), e => e.code === 'INVALID_TOKEN');
  const expired = signJwt({ alg: 'RS256', kid: 'kid1' }, { aud: 'matchora-c5ddc', iss: 'https://securetoken.google.com/matchora-c5ddc', sub: 'fb-uid-1', exp: nowSec - 10, iat: nowSec - 20 });
  await assert.rejects(auth.exchangeIdToken({ provider: 'firebase', idToken: expired }), e => e.code === 'INVALID_TOKEN');
});
