import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';

const venue = (extra = {}) => ({ name: 'Moda spor tesisi', city: 'İstanbul', area: 'Kadıköy', sports: ['FOOTBALL'], amenities: [], courts: 1, cancellationHours: 0, ...extra });
const fails = code => error => error.code === code;
async function ownerAccount(f) {
  await f.cmd(f.admin, 'admin.operator.assign', { userId: f.host.id, role: 'venue_owner', reason: 'Tesis işletmeci bilgileri doğrulandı.' });
  return f.auth.actor(f.sessions.host.accessToken);
}

test('venue owner creates, reads, edits and clears an optional address while legacy updates preserve it', async t => {
  const f = await fixture(t), owner = await ownerAccount(f);
  const id = (await f.cmd(owner, 'venue.create', venue())).result.id;
  assert.equal((await f.service.bootstrap()).venues.find(v => v.id === id).address, '');
  const address = 'Moda Caddesi 30, Kadıköy İstanbul';
  await f.cmd(owner, 'venue.update', { ...venue({ address }), venueId: id, expectedVersion: 1 });
  assert.equal((await f.service.bootstrap()).venues.find(v => v.id === id).address, address);
  assert.equal((await f.service.operatorVenues(owner)).items.find(v => v.id === id).address, address);
  await f.cmd(owner, 'venue.update', { ...venue({ name: 'Moda yeni tesis adı' }), venueId: id, expectedVersion: 2 });
  assert.equal((await f.service.bootstrap()).venues.find(v => v.id === id).address, address);
  await f.cmd(owner, 'venue.update', { ...venue({ address: '' }), venueId: id, expectedVersion: 3 });
  assert.equal((await f.service.bootstrap()).venues.find(v => v.id === id).address, '');
});

test('venue address validation accepts empty legacy values and rejects short or oversized direct and application values', async t => {
  const f = await fixture(t), owner = await ownerAccount(f);
  await assert.rejects(f.cmd(owner, 'venue.create', venue({ address: 'Kısa' })), fails('INVALID_INPUT'));
  await assert.rejects(f.cmd(owner, 'venue.create', venue({ address: 'x'.repeat(301) })), fails('INVALID_INPUT'));
  const id = (await f.cmd(owner, 'venue.create', venue({ address: '  Moda Caddesi 30  ' }))).result.id;
  assert.equal((await f.service.bootstrap()).venues.find(v => v.id === id).address, 'Moda Caddesi 30');
  await assert.rejects(f.cmd(owner, 'venue.update', { ...venue({ address: 'Kısa' }), venueId: id, expectedVersion: 1 }), fails('INVALID_INPUT'));
  await assert.rejects(f.cmd(f.other, 'venue.update', { ...venue({ address: 'Başkasının adresi' }), venueId: id, expectedVersion: 1 }), fails('FORBIDDEN'));
  await assert.rejects(f.cmd(f.guest, 'venue.application.create', venue({ address: '' })), fails('INVALID_INPUT'));
  await assert.rejects(f.cmd(f.guest, 'venue.application.create', venue({ address: '123456789' })), fails('INVALID_INPUT'));
});

test('pending and confirmed reservations protect venue address changes including clearing', async t => {
  const f = await fixture(t), owner = await ownerAccount(f), address = 'Moda Caddesi 30, Kadıköy İstanbul';
  const id = (await f.cmd(owner, 'venue.create', venue({ address }))).result.id;
  const slotId = (await f.cmd(owner, 'venue.slot.create', { venueId: id, court: 'Saha 1', startsAtEpochMillis: f.clock() + 3600000, durationMin: 60, priceMinor: 10000 })).result.id;
  const bookingId = (await f.cmd(f.guest, 'booking.create', { slotId, splitCount: 1 })).result.id;
  const changed = 'Bahariye Caddesi 40, Kadıköy İstanbul';
  await assert.rejects(f.cmd(owner, 'venue.update', { ...venue({ address: changed }), venueId: id, expectedVersion: 1 }), fails('ACTIVE_BOOKING'));
  await f.cmd(owner, 'venue.update', { ...venue({ name: 'Moda yeni tesis adı', address }), venueId: id, expectedVersion: 1 });
  await f.cmd(owner, 'venue.update', { ...venue(), venueId: id, expectedVersion: 2 });
  await f.cmd(owner, 'booking.confirm', { bookingId });
  await assert.rejects(f.cmd(owner, 'venue.update', { ...venue({ address: '' }), venueId: id, expectedVersion: 3 }), fails('ACTIVE_BOOKING'));
  await f.cmd(f.guest, 'booking.cancel', { bookingId });
  await f.cmd(owner, 'venue.update', { ...venue({ address: changed }), venueId: id, expectedVersion: 3 });
  assert.equal((await f.service.bootstrap()).venues.find(v => v.id === id).address, changed);
});
