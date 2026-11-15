import test from 'node:test';
import assert from 'node:assert/strict';
import sharp from 'sharp';
import { fixture } from './helpers.mjs';
import { cleanMedia } from '../src/storage.mjs';
import { startOAuth } from '../src/oauth.mjs';

const fails = code => error => error.code === code;
const baseVenue = extra => ({
  name: 'Panorama Spor Tesisleri',
  city: 'İstanbul',
  area: 'Kadıköy',
  address: 'Bağdat Caddesi No 120 Kadıköy',
  sports: ['FOOTBALL', 'TENNIS'],
  amenities: ['Duş', 'Otopark'],
  courts: 2,
  cancellationHours: 24,
  ...extra
});

async function makePng(background = 'green') {
  return sharp({ create: { width: 12, height: 12, channels: 3, background } }).png().toBuffer();
}

test('venue photo uploads enforce up to 4 owned ready unique UUIDs across create, update, application and review', async t => {
  const f = await fixture(t);
  const png = await makePng('blue');
  const uploads = [];
  for (let i = 0; i < 5; i++) {
    uploads.push((await f.service.upload(f.admin, png, 'image/png')).id);
  }

  await assert.rejects(
    f.cmd(f.admin, 'venue.create', baseVenue({ photoUploadIds: uploads })),
    fails('INVALID_INPUT')
  );
  await assert.rejects(
    f.cmd(f.admin, 'venue.create', baseVenue({ photoUploadIds: [uploads[0], uploads[0].toUpperCase()] })),
    fails('INVALID_INPUT')
  );
  await assert.rejects(
    f.cmd(f.admin, 'venue.create', baseVenue({ photoUploadIds: ['not-a-uuid'] })),
    fails('INVALID_INPUT')
  );

  const foreignUpload = (await f.service.upload(f.other, png, 'image/png')).id;
  await assert.rejects(
    f.cmd(f.admin, 'venue.create', baseVenue({ photoUploadIds: [foreignUpload] })),
    fails('FORBIDDEN')
  );

  const created = (await f.cmd(f.admin, 'venue.create', baseVenue({ photoUploadIds: uploads.slice(0, 4) }))).result.id;
  const boot = await f.service.bootstrap();
  const venue = boot.venues.find(v => v.id === created);
  assert.deepEqual(venue.photoUploadIds, uploads.slice(0, 4));

  // Partial update omitting photoUploadIds retains existing photos.
  await f.cmd(f.admin, 'venue.update', {
    venueId: created,
    expectedVersion: 1,
    name: venue.name,
    city: venue.city,
    area: venue.area,
    sports: venue.sports,
    courts: venue.courts,
    cancellationHours: 12
  });
  assert.deepEqual((await f.service.bootstrap()).venues.find(v => v.id === created).photoUploadIds, uploads.slice(0, 4));

  // Venue application carries up to 4 applicant photos through admin approval into the published venue.
  const applicantPhotos = [
    (await f.service.upload(f.host, png, 'image/png')).id,
    (await f.service.upload(f.host, png, 'image/png')).id
  ];
  const appId = (await f.cmd(f.host, 'venue.application.create', baseVenue({
    name: 'Sahil Tenis Kulübü',
    photoUploadIds: applicantPhotos
  }))).result.id;

  const inbox = await f.service.adminInbox(f.admin);
  assert.deepEqual(inbox.venueApplications.find(a => a.id === appId).photoUploadIds, applicantPhotos);

  const review = (await f.cmd(f.admin, 'admin.venue.application.review', {
    applicationId: appId,
    expectedVersion: 1,
    approve: true,
    response: 'Fotoğraflar ve yetki doğrulandı.'
  })).result;
  const approvedVenue = (await f.service.bootstrap(f.host)).venues.find(v => v.id === review.venueId);
  assert.deepEqual(approvedVenue.photoUploadIds, applicantPhotos);
});

test('readUpload and cleanMedia enforce venue, pending application, and photo verification visibility rules', async t => {
  const f = await fixture(t);
  const png = await makePng('red');
  const activeVenuePhoto = (await f.service.upload(f.admin, png, 'image/png')).id;
  const archivedVenuePhoto = (await f.service.upload(f.admin, png, 'image/png')).id;
  const pendingAppPhoto = (await f.service.upload(f.host, png, 'image/png')).id;
  const orphanPhoto = (await f.service.upload(f.other, png, 'image/png')).id;

  const activeVenueId = (await f.cmd(f.admin, 'venue.create', baseVenue({ name: 'Açık Tesis', photoUploadIds: [activeVenuePhoto] }))).result.id;
  const archivedVenueId = (await f.cmd(f.admin, 'venue.create', baseVenue({ name: 'Kapanacak Tesis', photoUploadIds: [archivedVenuePhoto] }))).result.id;
  await f.cmd(f.host, 'venue.application.create', baseVenue({ name: 'Başvuru Tesisi', photoUploadIds: [pendingAppPhoto] }));

  // Guest / public can read active venue photos, while archived venue photos are hidden from non-owners.
  const publicRead = await f.service.mediaFile(null, activeVenuePhoto);
  assert.equal(publicRead.mime, 'image/jpeg');
  assert.ok(Buffer.isBuffer(publicRead.bytes) && publicRead.bytes.length > 0);
  assert.ok(activeVenueId);

  await f.cmd(f.admin, 'venue.archive', { venueId: archivedVenueId, expectedVersion: 1 });
  await assert.rejects(f.service.mediaFile(f.other, archivedVenuePhoto), fails('NOT_FOUND'));
  assert.equal((await f.service.mediaFile(f.admin, archivedVenuePhoto)).mime, 'image/jpeg');

  // Pending venue application photos are visible to admins and applicant, not random users.
  await assert.rejects(f.service.mediaFile(f.other, pendingAppPhoto), fails('NOT_FOUND'));
  assert.equal((await f.service.mediaFile(f.admin, pendingAppPhoto)).mime, 'image/jpeg');
  assert.equal((await f.service.mediaFile(f.host, pendingAppPhoto)).mime, 'image/jpeg');

  // Open photo verification support request upload is readable by admins even after avatar changes, while private to peers once detached from profile.
  const verifyPhoto = (await f.service.upload(f.guest, png, 'image/png')).id;
  await f.cmd(f.guest, 'profile.save', {
    name: 'Doğrulama Sporcu',
    city: 'İstanbul',
    area: 'Kadıköy',
    birthYear: 1996,
    skills: [{ sport: 'FOOTBALL', level: 'INTERMEDIATE', position: 'Orta Saha' }],
    avatarUploadId: verifyPhoto,
    expectedVersion: 1
  });
  await f.cmd(f.guest, 'verification.photo', { uploadId: verifyPhoto });
  assert.equal((await f.service.mediaFile(f.other, verifyPhoto)).mime, 'image/jpeg');
  await f.cmd(f.guest, 'profile.save', {
    name: 'Doğrulama Sporcu',
    city: 'İstanbul',
    area: 'Kadıköy',
    birthYear: 1996,
    skills: [{ sport: 'FOOTBALL', level: 'INTERMEDIATE', position: 'Orta Saha' }],
    avatarUploadId: null,
    expectedVersion: 2
  });
  await assert.rejects(f.service.mediaFile(f.other, verifyPhoto), fails('NOT_FOUND'));
  assert.equal((await f.service.mediaFile(f.admin, verifyPhoto)).mime, 'image/jpeg');

  // cleanMedia deletes unattached stale uploads after 24h while keeping venue, pending application & open verification uploads.
  f.advance(86400001);
  await cleanMedia(f.service);
  assert.equal((await f.store.get('SELECT status FROM uploads WHERE id=?', [orphanPhoto])).status, 'deleted');
  assert.equal((await f.store.get('SELECT status FROM uploads WHERE id=?', [activeVenuePhoto])).status, 'ready');
  assert.equal((await f.store.get('SELECT status FROM uploads WHERE id=?', [pendingAppPhoto])).status, 'ready');
  assert.equal((await f.store.get('SELECT status FROM uploads WHERE id=?', [verifyPhoto])).status, 'ready');
});

test('venue.archive cancels unsettled past unpaid bookings and application duplicate check folds Turkish city/area case', async t => {
  const f = await fixture(t);
  const venueId = (await f.cmd(f.admin, 'venue.create', baseVenue({ name: 'Arşiv Tesisi' }))).result.id;
  const pastUnsettledSlot = (await f.cmd(f.admin, 'venue.slot.create', {
    venueId,
    court: 'Saha 1',
    startsAtEpochMillis: f.clock() + 3600000,
    durationMin: 60,
    priceMinor: 25000
  })).result.id;
  const bookingId = (await f.cmd(f.guest, 'booking.create', { slotId: pastUnsettledSlot, splitCount: 2 })).result.id;

  // Move time past the slot end while the booking remains unpaid/pending.
  f.advance(4 * 3600000);
  await f.cmd(f.admin, 'venue.archive', { venueId, expectedVersion: 1 });
  assert.equal((await f.store.get('SELECT status FROM bookings WHERE id=?', [bookingId])).status, 'cancelled');
  assert.equal((await f.store.get('SELECT status FROM venue_payments WHERE booking_id=?', [bookingId])).status, 'cancelled');

  // Turkish locale case-folding prevents duplicate pending venue application with İSTANBUL / KADIKÖY.
  await f.cmd(f.host, 'venue.application.create', baseVenue({ name: 'Büyükada Kortları', city: 'İstanbul', area: 'Kadıköy' }));
  await assert.rejects(
    f.cmd(f.host, 'venue.application.create', baseVenue({ name: 'BÜYÜKADA KORTLARI', city: 'İSTANBUL', area: 'KADIKÖY' })),
    fails('ALREADY_REQUESTED')
  );
});

test('verifyContact tolerates null profile verifications and startOAuth follows Google redirect with injected request', async t => {
  const f = await fixture(t);
  // Simulate legacy profile payload missing verifications array.
  const raw = JSON.parse((await f.store.get('SELECT payload FROM profiles WHERE user_id=?', [f.host.id])).payload);
  delete raw.verifications;
  await f.store.run('UPDATE profiles SET payload=? WHERE user_id=?', [JSON.stringify(raw), f.host.id]);

  const challenge = await f.auth.startOtp({ phone: '5324445566' }, 'contact', f.host);
  await f.auth.verifyContact(f.host, { challengeId: challenge.challengeId, code: challenge.developmentCode });
  assert.ok((await f.service.bootstrap(f.host)).profile.verifications.includes('BASIC'));

  f.config.userInfoUrl = 'https://project.supabase.co/auth/v1/user';
  f.config.authPublicKey = 'public-key';
  f.auth.request = async () => new Response(null, {
    status: 302,
    headers: { location: 'https://accounts.google.com/o/oauth2/v2/auth?client_id=matchora-test' }
  });
  const flow = await startOAuth(f.auth, { provider: 'google' }, 'oauth-test');
  assert.equal(flow.url, 'https://accounts.google.com/o/oauth2/v2/auth?client_id=matchora-test');
});
