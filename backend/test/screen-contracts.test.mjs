import test from 'node:test';
import assert from 'node:assert/strict';
import sharp from 'sharp';
import { fixture } from './helpers.mjs';
import { runJobs } from '../src/jobs.mjs';
import { sign } from '../src/auth.mjs';
import { createApi } from '../src/server.mjs';

async function verifiedMatch(f, extra = {}) {
  const event = (await f.cmd(f.host, 'event.create', f.event({ startsAtEpochMillis: f.clock() + 600000, ...extra }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId: event });
  const code = (await f.cmd(f.host, 'event.checkin.code', { eventId: event })).result.code;
  for (const actor of [f.host, f.guest]) await f.cmd(actor, 'event.checkin', { eventId: event, code });
  const current = await f.store.get('SELECT version FROM events WHERE id=?', [event]);
  await f.cmd(f.host, 'event.transition', { eventId: event, status: 'IN_PROGRESS', expectedVersion: current.version });
  await f.cmd(f.host, 'result.submit', { eventId: event, homeScore: 2, awayScore: 1, sides: { home: [f.host.id], away: [f.guest.id] } });
  await f.cmd(f.guest, 'result.confirm', { eventId: event });
  f.advance(25 * 3600000); await runJobs(f.service);
  return event;
}

test('player detail shows verified outcomes and consented chemistry, without private contact data', async t => {
  const f = await fixture(t);
  assert.deepEqual((await f.service.player(null, f.host.id, 'FOOTBALL')).recentForm, []);
  const event = await verifiedMatch(f);
  await f.cmd(f.guest, 'feedback.submit', { eventId: event, targetId: f.host.id, fairPlay: 5, playAgain: true });
  let detail = await f.service.player(f.guest, f.host.id, 'FOOTBALL');
  assert.equal(detail.recentForm.length, 1); assert.equal(detail.recentForm[0].outcome, 'W');
  assert.ok(detail.recentForm[0].ratingDelta > 0); assert.equal(detail.playedTogether, 1); assert.equal(detail.chemistry, 100);
  for (const field of ['phone', 'email', 'birthYear']) assert.equal(field in detail.player, false);
  await f.cmd(f.host, 'preferences.save', { privacy: { showRatingHistory: false, chemistrySignals: false, showReliability: false } });
  detail = await f.service.player(f.guest, f.host.id, 'FOOTBALL');
  assert.equal(detail.player.reliabilityVisible, false);
  assert.equal(detail.historyVisible, false); assert.deepEqual(detail.recentForm, []); assert.deepEqual(detail.badges, []);
  assert.equal(detail.chemistry, null); assert.equal(detail.playedTogether, null);
  await f.cmd(f.host, 'user.block', { userId: f.guest.id, blocked: true });
  await assert.rejects(f.service.player(f.guest, f.host.id, 'FOOTBALL'), e => e.code === 'NOT_FOUND');
});

test('public player form does not reveal private community matches', async t => {
  const f = await fixture(t);
  const community = (await f.cmd(f.host, 'community.create', { name: 'Özel kampüs', type: 'CAMPUS', access: 'OPEN', area: 'Kadıköy' })).result.id;
  await f.cmd(f.guest, 'community.join', { communityId: community });
  await verifiedMatch(f, { networkScope: 'CAMPUS', communityId: community });
  assert.equal((await f.service.player(null, f.host.id, 'FOOTBALL')).recentForm.length, 0);
  assert.equal((await f.service.player(f.other, f.host.id, 'FOOTBALL')).recentForm.length, 0);
  assert.equal((await f.service.player(f.guest, f.host.id, 'FOOTBALL')).recentForm.length, 1);
});

test('memory theme and score tag survive persistence and reject unsupported values', async t => {
  const f = await fixture(t);
  const image = await sharp({ create: { width: 8, height: 8, channels: 3, background: 'red' } }).jpeg().toBuffer();
  const upload = await f.service.upload(f.host, image, 'image/jpeg');
  const payload = { caption: 'Güzel maçtı', sport: 'TENNIS', venue: 'Kort', area: 'Kadıköy', city: 'İstanbul', uploadId: upload.id,
    shareThreadId: null, matchScoreTag: '6-3 / 6-2', sceneTheme: 'SUNSET_TENNIS' };
  const result = await f.cmd(f.host, 'memory.create', payload);
  const memory = result.snapshot.memories.find(m => m.id === result.result.id);
  assert.equal(memory.sceneTheme, payload.sceneTheme); assert.equal(memory.matchScoreTag, payload.matchScoreTag);
  await assert.rejects(f.cmd(f.host, 'memory.create', { ...payload, sceneTheme: 'UNKNOWN' }), e => e.code === 'INVALID_INPUT');
  await assert.rejects(f.cmd(f.host, 'memory.create', { ...payload, matchScoreTag: 'x'.repeat(81) }), e => e.code === 'INVALID_INPUT');
});

test('support response and moderation state reach only the request owner', async t => {
  const f = await fixture(t);
  const request = (await f.cmd(f.host, 'support.create', { subject: 'Yardım talebi', message: 'Maç ayrıntısı için yardım istiyorum.', category: 'Hesap' })).result.id;
  await f.cmd(f.admin, 'admin.support.resolve', { requestId: request, status: 'resolved', response: 'Talebin incelendi ve yanıtlandı.' });
  const own = (await f.service.bootstrap(f.host)).supportRequests.find(r => r.id === request);
  assert.equal(own.status, 'resolved'); assert.equal(own.response, 'Talebin incelendi ve yanıtlandı.');
  assert.ok(Number.isSafeInteger(own.respondedAt));
  await assert.rejects(f.cmd(f.admin, 'admin.support.resolve', { requestId: request, status: 'rejected', response: 'İkinci yanıt' }), e => e.code === 'INVALID_STATE');
  assert.equal((await f.service.bootstrap(f.guest)).supportRequests.length, 0);
  assert.equal((await f.service.bootstrap(null)).supportRequests.length, 0);
});

test('sales kill switch disables the catalog but still settles already paid transactions', async t => {
  const f = await fixture(t); f.config.revenueCatAuthorization = 'trusted-delivery'; f.config.revenueCatAppleKey = 'appl_public';
  assert.equal((await f.service.configuration()).store.enabled, true);
  await f.cmd(f.admin, 'admin.feature.set', { name: 'billing', enabled: false });
  assert.equal((await f.service.configuration()).store.enabled, false);
  const raw = Buffer.from(JSON.stringify({ eventId: 'paid-before-sales-stop', type: 'purchase', userId: f.host.id,
    transactionId: 'transaction-before-stop', productId: 'tkn_50', platform: 'apple' }));
  await f.service.webhook(raw, { 'x-matchora-timestamp': String(f.clock()), 'x-matchora-signature': sign(f.config.billingSecret, `${f.clock()}.${raw}`) });
  assert.equal((await f.service.bootstrap(f.host)).wallet.balance, 70);
  await f.cmd(f.admin, 'admin.feature.set', { name: 'billing', enabled: true });
  assert.equal((await f.service.configuration()).store.enabled, true);
});

test('team appearance and tournament descriptive fields survive a fresh snapshot', async t => {
  const f = await fixture(t); await f.grant(f.host);
  const team = (await f.cmd(f.host, 'team.create', { name: 'Mavi takım', sport: 'FOOTBALL', area: 'Kadıköy', colorHex: '#FF123456' })).result.id;
  assert.equal((await f.service.bootstrap(f.host)).teams.find(t => t.id === team).colorHex, '#FF123456');
  await assert.rejects(f.cmd(f.host, 'team.create', { name: 'Geçersiz takım', sport: 'FOOTBALL', area: 'Kadıköy', colorHex: 'invalid' }), e => e.code === 'INVALID_INPUT');
  const tournament = (await f.cmd(f.host, 'tournament.create', { name: 'Özel branş kupası', sport: 'CUSTOM', capacity: 4,
    dates: '10–12 Kasım', format: 'Tek eleme', city: 'İzmir', area: 'Karşıyaka', customSportName: 'Masa oyunu',
    organizerName: 'Spor kulübü', gamePlatform: 'PC', serverRegion: 'TR lobi' })).result.id;
  const saved = (await f.service.bootstrap(f.host)).tournaments.find(t => t.id === tournament);
  for (const [key, value] of Object.entries({ city: 'İzmir', area: 'Karşıyaka', customSportName: 'Masa oyunu', organizerName: 'Spor kulübü', gamePlatform: 'PC', serverRegion: 'TR lobi' })) assert.equal(saved[key], value);
});

test('passport local rank uses the same district and visibility policy as rankings', async t => {
  const f = await fixture(t);
  let snapshot = await f.service.bootstrap(f.host);
  const district = snapshot.rankings.filter(r => r.sport === 'FOOTBALL' && r.city === 'İstanbul' && r.area === 'Kadıköy');
  assert.equal(snapshot.profile.skills[0].localRank, district.findIndex(r => r.userId === f.host.id) + 1);
  await f.cmd(f.host, 'preferences.save', { privacy: { showInRankings: false } });
  snapshot = await f.service.bootstrap(f.host);
  assert.equal(snapshot.profile.skills[0].localRank, null);
});


test('player detail HTTP route supports guest discovery and validates identity and sport', async t => {
  const f = await fixture(t), server = createApi(f.service, f.auth);
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  t.after(() => new Promise(resolve => server.close(resolve)));
  const base = `http://127.0.0.1:${server.address().port}/v1/players/${f.host.id}`;
  const response = await fetch(base + '?sport=ESPORTS_CS2');
  assert.equal(response.status, 200);
  const data = await response.json(); assert.equal(data.player.id, f.host.id); assert.deepEqual(data.recentForm, []);
  assert.equal('phone' in data.player, false); assert.equal('email' in data.player, false);
  assert.equal((await fetch(base + '?sport=UNKNOWN')).status, 400);
  assert.equal((await fetch(base, { headers: { Authorization: 'Bearer invalid-token' } })).status, 401);
});


test('permanent communities have distinct organization types; field and travel belong to event listings', async t => {
  const f = await fixture(t);
  for (const type of ['CAMPUS', 'COMPANY', 'CLUB']) {
    const result = await f.cmd(f.host, 'community.create', { name: `${type} topluluğu`, type, access: 'OPEN', area: 'Kadıköy' });
    assert.equal(result.snapshot.communities.find(c => c.id === result.result.id).type, type);
  }
  for (const type of ['CITY','TRAVEL']) await assert.rejects(f.cmd(f.host,'community.create',{name:'Tek maç ilanı',type,access:'OPEN',area:'Kadıköy'}),error=>error.code==='INVALID_INPUT');
});
