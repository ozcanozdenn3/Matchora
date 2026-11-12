import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import sharp from 'sharp';
import { fixture } from './helpers.mjs';

const rejectsCode = (promise, code) => assert.rejects(promise, error => error.code === code);

async function allEvents(f, actor, filters = {}) {
  const items = [], cursors = new Set();
  let after;
  do {
    const page = await f.service.events(actor, { ...filters, after });
    items.push(...page.items);
    after = page.nextCursor;
    if (after) {
      assert.ok(!cursors.has(after), 'Pagination must advance even when a private page is empty');
      cursors.add(after);
      assert.ok(cursors.size < 250, 'Pagination must terminate');
    }
  } while (after);
  return items;
}

async function saveProfile(f, actor, changes) {
  const current = (await f.service.bootstrap(actor)).profile;
  return f.cmd(actor, 'profile.save', {
    ...f.passport(current.name), avatarUploadId: current.avatarUploadId,
    ...changes, expectedVersion: current.version
  });
}

async function avatar(f, actor, color = 'red') {
  const bytes = await sharp({ create: { width: 8, height: 8, channels: 3, background: color } }).jpeg().toBuffer();
  const upload = await f.service.upload(actor, bytes, 'image/jpeg');
  await saveProfile(f, actor, { avatarUploadId: upload.id });
  return upload.id;
}

test('event cursor pages retain city boundaries and stable ordering while excluding cancelled and elapsed games', async t => {
  const f = await fixture(t); await f.grant(f.host);
  const expected = [];
  for (let i = 0; i < 3; i++) expected.push((await f.cmd(f.host, 'event.create', f.event({ title: `İstanbul maçı ${i}` }))).result.id);
  const izmir = (await f.cmd(f.host, 'event.create', f.event({ city: 'İzmir', area: 'Karşıyaka' }))).result.id;
  const cancelled = (await f.cmd(f.host, 'event.create', f.event())).result.id;
  await f.cmd(f.host, 'event.transition', { eventId: cancelled, status: 'CANCELLED', expectedVersion: 1 });
  const elapsed = (await f.cmd(f.host, 'event.create', f.event({ startsAtEpochMillis: f.clock() + 60000, durationMin: 15 }))).result.id;
  f.advance(17 * 60000);

  const rows = await allEvents(f, f.guest, { city: 'İstanbul', limit: 1 });
  assert.deepEqual(rows.map(row => row.id), expected.sort());
  assert.equal(new Set(rows.map(row => row.id)).size, 3);
  assert.ok(rows.every(row => row.city === 'İstanbul'));
  assert.ok(!rows.some(row => [izmir, cancelled, elapsed].includes(row.id)));
  assert.deepEqual((await allEvents(f, null, { city: 'İzmir', limit: 1 })).map(row => row.id), [izmir]);
  const browse = await f.service.bootstrap(null, { city: 'İstanbul' });
  assert.deepEqual(browse.events.map(row => row.id).sort(), expected);
});

test('private community events remain hidden across pagination, bootstrap and membership changes', async t => {
  const f = await fixture(t); await f.grant(f.host);
  const communityId = (await f.cmd(f.host, 'community.create', { name: 'Özel kampüs', type: 'CAMPUS', access: 'OPEN', city: 'İstanbul', area: 'Kadıköy' })).result.id;
  const hidden = (await f.cmd(f.host, 'event.create', f.event({ networkScope: 'CAMPUS', communityId, startsAtEpochMillis: f.clock() + 600000 }))).result.id;
  const visible = (await f.cmd(f.host, 'event.create', f.event())).result.id;
  const first = await f.service.events(f.guest, { city: 'İstanbul', limit: 1 });
  assert.deepEqual(first.items, []);
  assert.equal(typeof first.nextCursor, 'string');
  assert.ok(!Buffer.from(first.nextCursor, 'base64url').includes(Buffer.from(hidden)), 'The encrypted cursor must not disclose a hidden event id');
  assert.deepEqual((await allEvents(f, f.guest, { city: 'İstanbul', limit: 1 })).map(row => row.id), [visible]);
  assert.deepEqual((await allEvents(f, null, { city: 'İstanbul', limit: 1 })).map(row => row.id), [visible]);
  assert.ok(!(await f.service.bootstrap(f.guest)).events.some(row => row.id === hidden));
  await f.cmd(f.guest, 'community.join', { communityId });
  assert.deepEqual((await allEvents(f, f.guest, { city: 'İstanbul', limit: 1 })).map(row => row.id), [hidden, visible]);
  await f.cmd(f.guest, 'community.leave', { communityId });
  assert.deepEqual((await allEvents(f, f.guest, { city: 'İstanbul', limit: 1 })).map(row => row.id), [visible]);
});

test('event cursors reject authenticated tampering, malformed tokens and expiration', async t => {
  const f = await fixture(t); await f.grant(f.host);
  for (let i = 0; i < 2; i++) await f.cmd(f.host, 'event.create', f.event({ startsAtEpochMillis: f.clock() + 3 * 3600000 }));
  const { nextCursor } = await f.service.events(f.guest, { city: 'İstanbul', limit: 1 });
  const bytes = Buffer.from(nextCursor, 'base64url'); bytes[30] ^= 1;
  await rejectsCode(f.service.events(f.guest, { after: bytes.toString('base64url') }), 'INVALID_CURSOR');
  await rejectsCode(f.service.events(f.guest, { after: 'not-a-valid-cursor' }), 'INVALID_CURSOR');
  await rejectsCode(f.service.events(f.guest, { after: 'a'.repeat(1025) }), 'INVALID_CURSOR');
  f.advance(3600001);
  await rejectsCode(f.service.events(f.guest, { after: nextCursor }), 'INVALID_CURSOR');
  assert.equal((await f.service.events(f.guest, { limit: 1 })).items.length, 1);
});

test('bootstrap caps discovery at 200 and its cursor reaches the remaining city events without duplicates', async t => {
  const f = await fixture(t), expected = [];
  // Bulk fixture data avoids 205 unrelated command snapshots; other tests exercise real event creation.
  await f.store.transaction(async db => {
    for (let i = 0; i < 207; i++) {
      const id = randomUUID(), city = i < 205 ? 'İstanbul' : 'İzmir';
      const payload = f.event({ title: `Sayfalama maçı ${i}`, city, area: city === 'İzmir' ? 'Karşıyaka' : 'Kadıköy', networkScope: 'CITY' });
      const starts = f.clock() + 3600000;
      await db.run('INSERT INTO events(id,host_id,sport,status,capacity,starts_at,ends_at,commitment,city,district,payload,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)', [id, f.host.id, 'FOOTBALL', 'PUBLISHED', 4, starts, starts + 3600000, 10, city, payload.area, JSON.stringify(payload), f.clock()]);
      if (city === 'İstanbul') expected.push(id);
    }
  });
  expected.sort();
  const initial = await f.service.bootstrap(null, { city: 'İstanbul' });
  assert.equal(initial.events.length, 200);
  assert.deepEqual(initial.events.map(row => row.id), expected.slice(0, 200));
  assert.equal(typeof initial.nextEventCursor, 'string');
  const remainder = await f.service.events(null, { city: 'İstanbul', after: initial.nextEventCursor, limit: 200 });
  assert.deepEqual(remainder.items.map(row => row.id), expected.slice(200));
  assert.equal(remainder.nextCursor, null);
  assert.equal(new Set([...initial.events, ...remainder.items].map(row => row.id)).size, 205);
  assert.equal((await f.service.bootstrap(null, { city: 'İzmir' })).events.length, 2);
});

test('admin announcements honor marketing consent, Turkish city matching, account status and idempotency', async t => {
  const f = await fixture(t);
  for (const actor of [f.host, f.guest, f.other]) await f.cmd(actor, 'preferences.save', { notifications: { marketing: true } });
  await saveProfile(f, f.guest, { city: 'İzmir', area: 'Karşıyaka' });
  await f.cmd(f.admin, 'admin.user.status', { userId: f.other.id, status: 'suspended', reason: 'Duyuru alıcıları için güvenlik denetimi.' });
  const payload = { title: 'Kadıköy turnuvası', body: 'Yeni turnuvanın kayıtları bugün açıldı.', city: 'istanbul' }, key = randomUUID();
  await rejectsCode(f.cmd(f.host, 'admin.announcement.send', payload), 'FORBIDDEN');
  const sent = await f.cmd(f.admin, 'admin.announcement.send', payload, key);
  assert.equal(sent.result.recipients, 1);
  const notifications = await f.store.all("SELECT user_id,payload FROM notifications WHERE category='marketing'");
  assert.deepEqual(notifications.map(row => row.user_id), [f.host.id]);
  assert.partialDeepStrictEqual(JSON.parse(notifications[0].payload), { title: payload.title, body: payload.body, destination: 'PASSPORT', targetId: '' });
  assert.equal((await f.cmd(f.admin, 'admin.announcement.send', payload, key)).replayed, true);
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM notifications WHERE category='marketing'")).n, 1);
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM audit_log WHERE action='admin.announcement.send'")).n, 1);
  await rejectsCode(f.cmd(f.admin, 'admin.announcement.send', { ...payload, title: 'Farklı içerik' }, key), 'IDEMPOTENCY_CONFLICT');
  await rejectsCode(f.cmd(f.admin, 'admin.announcement.send', payload), 'RATE_LIMITED');
  f.advance(86400001);
  assert.equal((await f.cmd(f.admin, 'admin.announcement.send', { ...payload, city: '' })).result.recipients, 2);
});

test('invalid announcement content fails without consuming the admin daily allowance', async t => {
  const f = await fixture(t);
  const payload = { title: 'Duyuru', body: 'Yeni etkinlik kayıtları açıldı.', city: '' };
  await assert.rejects(f.cmd(f.admin, 'admin.announcement.send', { ...payload, title: 'ab' }));
  await assert.rejects(f.cmd(f.admin, 'admin.announcement.send', { ...payload, body: 'kısa' }));
  await assert.rejects(f.cmd(f.admin, 'admin.announcement.send', { ...payload, city: 'a'.repeat(61) }));
  assert.equal((await f.cmd(f.admin, 'admin.announcement.send', payload)).result.recipients, 0);
  assert.equal((await f.service.adminInbox(f.admin)).announcements.length, 1);
});

test('photo verification deduplicates an upload and an admin decision closes its exact request', async t => {
  const f = await fixture(t), uploadId = await avatar(f, f.host);
  const first = await f.cmd(f.host, 'verification.photo', { uploadId });
  const second = await f.cmd(f.host, 'verification.photo', { uploadId });
  assert.equal(first.result.id, second.result.id);
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM support_requests WHERE user_id=? AND category='Fotoğraf doğrulaması'", [f.host.id])).n, 1);
  await rejectsCode(f.cmd(f.other, 'verification.photo', { uploadId }), 'FORBIDDEN');
  const inbox = await f.service.adminInbox(f.admin);
  assert.equal(inbox.photos.find(row => row.id === first.result.id).uploadId, uploadId);
  assert.equal(inbox.photos.find(row => row.id === first.result.id).stale, false);
  assert.ok(!inbox.support.some(row => row.id === first.result.id));
  const decision = { userId: f.host.id, uploadId, approved: true, requestId: first.result.id, reason: 'Profil fotoğrafı doğrulandı.' };
  await rejectsCode(f.cmd(f.guest, 'admin.verification.photo', decision), 'FORBIDDEN');
  await f.cmd(f.admin, 'admin.verification.photo', decision);
  assert.equal((await f.store.get('SELECT status FROM support_requests WHERE id=?', [first.result.id])).status, 'resolved');
  assert.ok((await f.service.bootstrap(f.host)).profile.verifications.includes('PHOTO'));
  assert.ok(!(await f.service.adminInbox(f.admin)).photos.some(row => row.id === first.result.id));
  await rejectsCode(f.cmd(f.admin, 'admin.verification.photo', decision), 'INVALID_STATE');
});

test('changing an avatar invalidates photo approval and prevents review of the stale image', async t => {
  const f = await fixture(t), original = await avatar(f, f.host);
  const requestId = (await f.cmd(f.host, 'verification.photo', { uploadId: original })).result.id;
  await f.cmd(f.admin, 'admin.verification.photo', { userId: f.host.id, uploadId: original, approved: true, requestId, reason: 'İlk fotoğraf doğrulandı.' });
  const replacement = await avatar(f, f.host, 'blue');
  assert.ok(!(await f.service.bootstrap(f.host)).profile.verifications.includes('PHOTO'));
  const oldRequest = (await f.cmd(f.host, 'verification.photo', { uploadId: original })).result.id;
  const inbox = await f.service.adminInbox(f.admin);
  assert.equal(inbox.photos.find(row => row.id === oldRequest).stale, true);
  await rejectsCode(f.cmd(f.admin, 'admin.verification.photo', { userId: f.host.id, uploadId: original, requestId: oldRequest, approved: true, reason: 'Eski fotoğraf tekrar incelendi.' }), 'INVALID_PHOTO');
  const own = (await f.service.bootstrap(f.host)).profile;
  assert.equal(own.avatarUploadId, replacement);
  assert.ok(!own.verifications.includes('PHOTO'));
  assert.equal((await f.store.get('SELECT status FROM support_requests WHERE id=?', [oldRequest])).status, 'open');
});

test('photo review cannot attach a current upload to another upload request from the same user', async t => {
  const f = await fixture(t), original = await avatar(f, f.host);
  const oldRequest = (await f.cmd(f.host, 'verification.photo', { uploadId: original })).result.id;
  const current = await avatar(f, f.host, 'blue');
  const currentRequest = (await f.cmd(f.host, 'verification.photo', { uploadId: current })).result.id;
  await rejectsCode(f.cmd(f.admin, 'admin.verification.photo', { userId: f.host.id, uploadId: current, requestId: oldRequest, approved: true, reason: 'Yanlış talep kimliği eşleştirildi.' }), 'INVALID_STATE');
  assert.equal((await f.store.get('SELECT status FROM support_requests WHERE id=?', [oldRequest])).status, 'open');
  assert.ok(!(await f.service.bootstrap(f.host)).profile.verifications.includes('PHOTO'));
  await f.cmd(f.admin, 'admin.verification.photo', { userId: f.host.id, uploadId: current, requestId: currentRequest, approved: false, reason: 'Fotoğraf incelemesi reddedildi.' });
  assert.equal((await f.store.get('SELECT status FROM support_requests WHERE id=?', [currentRequest])).status, 'rejected');
});

test('admin inbox exposes report context, current user status and announcement audit without private message bodies', async t => {
  const f = await fixture(t);
  const eventId = (await f.cmd(f.host, 'event.create', f.event({ title: 'İncelenecek maç', description: 'Saha organizasyonu açıklaması.' }))).result.id;
  const eventReport = (await f.cmd(f.guest, 'report.create', { targetType: 'event', targetId: eventId, reason: 'İlan bilgisi hatalı', details: 'Kontrol edilmesini istiyorum.' })).result.id;
  await f.cmd(f.guest, 'preferences.save', { privacy: { dmPolicy: 'Herkes' } });
  const threadId = (await f.cmd(f.host, 'chat.direct', { userId: f.guest.id })).result.id;
  await f.cmd(f.host, 'chat.send', { threadId, text: 'Özel konuşmanın yönetim listesine çıkmaması gereken içeriği.' });
  const threadReport = (await f.cmd(f.guest, 'report.create', { targetType: 'thread', targetId: threadId, reason: 'Sohbet incelensin' })).result.id;
  await f.cmd(f.admin, 'admin.user.status', { userId: f.other.id, status: 'suspended', reason: 'Yönetim listesinde askıya alınmış hesap denetimi.' });
  await f.cmd(f.admin, 'admin.announcement.send', { title: 'Denetim duyurusu', body: 'Bu duyuru denetim listesinde görünmeli.', city: '' });

  await rejectsCode(f.service.adminInbox(null), 'FORBIDDEN');
  await rejectsCode(f.service.adminInbox(f.host), 'FORBIDDEN');
  const inbox = await f.service.adminInbox(f.admin);
  assert.deepEqual(inbox.reports.find(row => row.id === eventReport).context, { title: 'İncelenecek maç', description: 'Saha organizasyonu açıklaması.', status: 'PUBLISHED' });
  assert.deepEqual(inbox.reports.find(row => row.id === threadReport).context, { title: 'Direkt sohbet', description: '', status: '' });
  assert.ok(!JSON.stringify(inbox).includes('Özel konuşmanın'));
  assert.deepEqual(inbox.users.find(row => row.id === f.other.id), { id: f.other.id, role: 'user', status: 'suspended', name: 'Diğer sporcu' });
  assert.ok(inbox.users.every(row => Object.keys(row).sort().join(',') === 'id,name,role,status'));
  assert.deepEqual(inbox.announcements, [{ recipients: 0, createdAt: f.clock() }]);
});

test('admin inbox rejects an actor whose admin role was revoked after authentication', async t => {
  const f = await fixture(t);
  await f.store.run("UPDATE users SET role='user' WHERE id=?", [f.admin.id]);
  await rejectsCode(f.service.adminInbox(f.admin), 'FORBIDDEN');
});

test('admin report context covers memories, teams, venues and venue reviews', async t => {
  const f = await fixture(t), uploadId = await avatar(f, f.host);
  const memoryId = (await f.cmd(f.host, 'memory.create', { caption: 'İncelenecek spor anısı', sport: 'FOOTBALL', venue: 'Kadıköy saha', city: 'İstanbul', area: 'Kadıköy', uploadId })).result.id;
  const teamId = (await f.cmd(f.host, 'team.create', { name: 'İncelenecek takım', sport: 'FOOTBALL', area: 'Kadıköy', description: 'Takım açıklaması.' })).result.id;
  const venueId = (await f.cmd(f.admin, 'venue.create', { name: 'İncelenecek tesis', city: 'İstanbul', area: 'Kadıköy', sports: ['FOOTBALL'], courts: 1 })).result.id;
  const slotId = (await f.cmd(f.admin, 'venue.slot.create', { venueId, court: 'Saha 1', startsAtEpochMillis: f.clock() + 60000, durationMin: 15, priceMinor: 10000 })).result.id;
  const bookingId = (await f.cmd(f.host, 'booking.create', { slotId, splitCount: 1 })).result.id;
  await f.cmd(f.admin, 'booking.confirm', { bookingId });
  await f.cmd(f.admin, 'booking.payment.record', { bookingId, reference: 'gercek-tesis-odemesi' });
  f.advance(17 * 60000);
  await f.cmd(f.admin, 'booking.complete', { bookingId });
  const reviewId = (await f.cmd(f.host, 'venue.review.save', { bookingId, rating: 4, comment: 'İncelenecek tesis değerlendirmesi.' })).result.id;

  const targets = [
    ['memory', memoryId, f.guest, { title: 'Bildirilen kayıt', description: 'İncelenecek spor anısı', status: 'pending' }],
    ['team', teamId, f.guest, { title: 'İncelenecek takım', description: 'Takım açıklaması.', status: '' }],
    ['venue', venueId, f.guest, { title: 'İncelenecek tesis', description: '', status: '' }],
    ['venue_review', reviewId, f.host, { title: 'Bildirilen kayıt', description: 'İncelenecek tesis değerlendirmesi.', status: 'pending' }]
  ];
  for (const [targetType, targetId, actor] of targets) await f.cmd(actor, 'report.create', { targetType, targetId, reason: 'İçerik incelensin' });
  const inbox = await f.service.adminInbox(f.admin);
  for (const [targetType, targetId, , expected] of targets) assert.deepEqual(inbox.reports.find(row => row.target_type === targetType && row.target_id === targetId).context, expected);
  await f.cmd(f.admin, 'admin.operator.assign', { userId: f.guest.id, role: 'venue_owner', reason: 'Tesis işletmecisinin erişimi atanıyor.' });
  const operator = await f.user('guest');
  await rejectsCode(f.service.adminInbox(operator), 'FORBIDDEN');
});
