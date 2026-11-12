import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { fixture } from './helpers.mjs';
import { runJobs } from '../src/jobs.mjs';

const fails = code => error => error.code === code;
async function replaceSport(f, actor, sport) {
  const saved = (await f.service.bootstrap(actor)).profile;
  return f.cmd(actor, 'profile.save', { ...f.passport(), expectedVersion: saved.version,
    skills: [{ sport, level: 'INTERMEDIATE', position: '' }] });
}
async function pendingResult(f) {
  const event = (await f.cmd(f.host, 'event.create', f.event({ startsAtEpochMillis: f.clock() + 600000 }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId: event });
  const code = (await f.cmd(f.host, 'event.checkin.code', { eventId: event })).result.code;
  await f.cmd(f.host, 'event.checkin', { eventId: event, code });
  const saved = await f.store.get('SELECT version FROM events WHERE id=?', [event]);
  await f.cmd(f.host, 'event.transition', { eventId: event, status: 'IN_PROGRESS', expectedVersion: saved.version });
  await f.cmd(f.host, 'result.submit', { eventId: event, homeScore: 2, awayScore: 1, sides: { home: [f.host.id], away: [f.guest.id] } });
  return event;
}

test('rejected scores release the passport sport lock while an old unresolved result still protects it', async t => {
  const f = await fixture(t), event = await pendingResult(f);
  f.advance(80 * 3600000); await runJobs(f.service);
  await assert.rejects(replaceSport(f, f.host, 'TENNIS'), fails('SPORT_IN_USE'));
  await f.cmd(f.admin, 'admin.result.resolve', { eventId: event, approve: false, reason: 'Skor kanıtları sonucu doğrulamaya yeterli değil.' });
  await replaceSport(f, f.host, 'TENNIS');
  assert.equal((await f.service.bootstrap(f.host)).profile.skills[0].sport, 'TENNIS');
});

test('an abandoned resultless match stops locking a sport when its 72 hour commitment expires', async t => {
  const f = await fixture(t);
  await f.cmd(f.host, 'event.create', f.event());
  await assert.rejects(replaceSport(f, f.host, 'TENNIS'), fails('SPORT_IN_USE'));
  f.advance(80 * 3600000); await runJobs(f.service);
  assert.equal((await f.service.bootstrap(f.host)).wallet.locked, 0);
  await replaceSport(f, f.host, 'TENNIS');
  await replaceSport(f, f.host, 'FOOTBALL');
  assert.equal((await f.service.bootstrap(f.host)).profile.skills[0].rating, 1000);
});

test('captain transfer hands future challenge match administration to the new captain without moving tournament ownership', async t => {
  const f = await fixture(t); await f.grant(f.host);
  const home = (await f.cmd(f.host, 'team.create', { name: 'Ev sahibi takım', sport: 'FOOTBALL', area: 'Kadıköy' })).result.id;
  const away = (await f.cmd(f.other, 'team.create', { name: 'Konuk takım', sport: 'FOOTBALL', area: 'Kadıköy' })).result.id;
  await f.cmd(f.host, 'team.invite', { teamId: home, userId: f.guest.id });
  await f.cmd(f.guest, 'team.invite.respond', { teamId: home, accept: true });
  const challenge = (await f.cmd(f.host, 'challenge.create', { fromTeamId: home, toTeamId: away, proposedTime: 'Bugün', venue: 'Final sahası', startsAtEpochMillis: f.clock() + 600000 })).result.id;
  const match = (await f.cmd(f.other, 'challenge.respond', { challengeId: challenge, accept: true })).result.eventId;
  const cup = (await f.cmd(f.host, 'tournament.create', { name: 'Bağımsız organizasyon', sport: 'FOOTBALL', capacity: 2, dates: 'Kasım' })).result.id;
  await f.cmd(f.host, 'tournament.register', { tournamentId: cup, teamId: home });
  await f.cmd(f.other, 'tournament.register', { tournamentId: cup, teamId: away });
  await f.cmd(f.host, 'tournament.schedule', { tournamentId: cup, startsAtEpochMillis: f.clock() + 1200000, venue: 'Kupa sahası' });
  await f.cmd(f.host, 'team.transfer', { teamId: home, userId: f.guest.id });
  await assert.rejects(f.cmd(f.host, 'event.checkin.code', { eventId: match }), fails('FORBIDDEN'));
  const code = (await f.cmd(f.guest, 'event.checkin.code', { eventId: match })).result.code;
  await f.cmd(f.guest, 'event.checkin', { eventId: match, code });
  const event = await f.store.get('SELECT host_id,version FROM events WHERE id=?', [match]);
  assert.equal(event.host_id, f.guest.id);
  await f.cmd(f.guest, 'event.transition', { eventId: match, status: 'IN_PROGRESS', expectedVersion: event.version });
  const tournamentEvent = await f.store.get('SELECT e.host_id FROM events e JOIN tournament_fixtures f ON f.event_id=e.id WHERE f.tournament_id=?', [cup]);
  assert.equal(tournamentEvent.host_id, f.host.id);
  assert.equal((await f.store.get('SELECT creator_id FROM tournaments WHERE id=?', [cup])).creator_id, f.host.id);
});

async function bookingFixture(f) {
  const venue = (await f.cmd(f.admin, 'venue.create', { name: 'İşletilen tesis', city: 'İstanbul', area: 'Kadıköy', sports: ['FOOTBALL'], courts: 1, cancellationHours: 0 })).result.id;
  const slot = (await f.cmd(f.admin, 'venue.slot.create', { venueId: venue, court: 'Saha 1', startsAtEpochMillis: f.clock() + 3600000, durationMin: 60, priceMinor: 100001 })).result.id;
  const booking = (await f.cmd(f.host, 'booking.create', { slotId: slot })).result.id;
  return { venue, slot, booking };
}

test('operator catalog preserves old live bookings and early slots beyond the newest hundred records', async t => {
  const f = await fixture(t), { venue, slot, booking } = await bookingFixture(f);
  await f.store.transaction(async db => {
    for (let i = 0; i < 101; i++) {
      const slotId = randomUUID(), bookingId = randomUUID(), at = f.clock() + (i + 2) * 86400000;
      await db.run('INSERT INTO venue_slots(id,venue_id,starts_at,ends_at,price_minor,status,payload) VALUES(?,?,?,?,?,?,?)', [slotId, venue, at, at + 3600000, 100, 'cancelled', '{}']);
      await db.run('INSERT INTO bookings(id,slot_id,user_id,status,amount_minor,split_count,cancellation_deadline,created_at) VALUES(?,?,?,?,?,?,?,?)', [bookingId, slotId, f.host.id, 'cancelled', 100, 1, at, f.clock() + i + 1]);
      await db.run('INSERT INTO venue_payments(id,booking_id,amount_minor,currency,status,created_at) VALUES(?,?,?,?,?,?)', [randomUUID(), bookingId, 100, 'TRY', 'cancelled', f.clock() + i + 1]);
    }
    for (let i = 0; i < 101; i++) await db.run('INSERT INTO venues(id,owner_id,payload,created_at) VALUES(?,?,?,?)', [randomUUID(), f.admin.id, '{"name":"Yeni tesis"}', f.clock() + i + 1]);
  });
  const item = (await f.service.operatorVenues(f.admin)).items.find(v => v.id === venue);
  assert.ok(item); assert.ok(item.slots.some(s => s.id === slot)); assert.ok(item.bookings.some(b => b.id === booking));
  await f.cmd(f.admin, 'booking.confirm', { bookingId: booking });
  await f.cmd(f.admin, 'booking.cancel', { bookingId: booking });
});

test('offline refund retains the original payment receipt in the immutable audit history', async t => {
  const f = await fixture(t), { booking } = await bookingFixture(f);
  await f.cmd(f.admin, 'booking.confirm', { bookingId: booking });
  await f.cmd(f.admin, 'booking.payment.record', { bookingId: booking, reference: 'original-payment-receipt' });
  await f.cmd(f.admin, 'booking.refund.record', { bookingId: booking, reference: 'separate-refund-receipt' });
  const paymentAudit = await f.store.get("SELECT metadata FROM audit_log WHERE action='venue.payment.record' AND target_id=?", [booking]);
  assert.deepEqual(JSON.parse(paymentAudit.metadata), { reference: 'original-payment-receipt', amountMinor: 100001, currency: 'TRY', method: 'pay_at_venue' });
  const refundAudit = await f.store.get("SELECT metadata FROM audit_log WHERE action='booking.refund.record' AND target_id=?", [booking]);
  assert.equal(JSON.parse(refundAudit.metadata).reference, 'separate-refund-receipt');
  assert.equal(JSON.parse(refundAudit.metadata).paymentReference, 'original-payment-receipt');
  await assert.rejects(f.store.run('UPDATE audit_log SET metadata=? WHERE target_id=?', ['{}', booking]), /immutable audit/);
});

const venueDetails = { name: 'İşletilen tesis', city: 'İstanbul', area: 'Kadıköy', sports: ['FOOTBALL'], courts: 1, cancellationHours: 0 };
test('venue editing checks tenant access and versions without rewriting an existing cancellation agreement', async t => {
  const f = await fixture(t), { venue, booking } = await bookingFixture(f);
  const deadline = (await f.store.get('SELECT cancellation_deadline FROM bookings WHERE id=?', [booking])).cancellation_deadline;
  const edit = { venueId: venue, expectedVersion: 1, ...venueDetails, name: 'Yeni tesis adı', cancellationHours: 24 };
  await assert.rejects(f.cmd(f.other, 'venue.update', edit), fails('FORBIDDEN'));
  await assert.rejects(f.cmd(f.admin, 'venue.update', { ...edit, city: 'Ankara' }), fails('ACTIVE_BOOKING'));
  assert.equal((await f.cmd(f.admin, 'venue.update', edit)).result.version, 2);
  assert.equal((await f.store.get('SELECT cancellation_deadline FROM bookings WHERE id=?', [booking])).cancellation_deadline, deadline);
  await assert.rejects(f.cmd(f.admin, 'venue.update', edit), fails('VERSION_CONFLICT'));
});

test('slot editing preserves booked amounts and reopening checks current versions, overlap and elapsed time', async t => {
  const f = await fixture(t), { venue, slot, booking } = await bookingFixture(f);
  const edit = { slotId: slot, expectedVersion: 1, court: 'Saha 1', startsAtEpochMillis: f.clock() + 7200000, durationMin: 60, priceMinor: 200002 };
  await assert.rejects(f.cmd(f.admin, 'venue.slot.update', edit), fails('ACTIVE_BOOKING'));
  assert.equal((await f.store.get('SELECT amount_minor FROM bookings WHERE id=?', [booking])).amount_minor, 100001);
  assert.equal((await f.store.get('SELECT price_minor FROM venue_slots WHERE id=?', [slot])).price_minor, 100001);
  await f.cmd(f.host, 'booking.cancel', { bookingId: booking });
  assert.equal((await f.cmd(f.admin, 'venue.slot.update', edit)).result.version, 2);
  await assert.rejects(f.cmd(f.admin, 'venue.slot.update', edit), fails('VERSION_CONFLICT'));
  assert.equal((await f.cmd(f.admin, 'venue.slot.cancel', { slotId: slot, expectedVersion: 2 })).result.version, 3);
  const conflict = (await f.cmd(f.admin, 'venue.slot.create', { venueId: venue, court: 'saha   1', startsAtEpochMillis: edit.startsAtEpochMillis, durationMin: 60, priceMinor: 30000 })).result.id;
  await assert.rejects(f.cmd(f.admin, 'venue.slot.reopen', { slotId: slot, expectedVersion: 3 }), fails('SLOT_OVERLAP'));
  await f.cmd(f.admin, 'venue.slot.cancel', { slotId: conflict, expectedVersion: 1 });
  assert.equal((await f.cmd(f.admin, 'venue.slot.reopen', { slotId: slot, expectedVersion: 3 })).result.version, 4);
  await assert.rejects(f.cmd(f.admin, 'venue.slot.create', { venueId: venue, court: 'SAHA 1', startsAtEpochMillis: edit.startsAtEpochMillis, durationMin: 60, priceMinor: 30000 }), fails('SLOT_OVERLAP'));
  await f.cmd(f.admin, 'venue.slot.cancel', { slotId: slot, expectedVersion: 4 });
  f.advance(4 * 3600000);
  await assert.rejects(f.cmd(f.admin, 'venue.slot.reopen', { slotId: slot, expectedVersion: 5 }), fails('SLOT_CLOSED'));
});

test('archiving an unpaid future venue cancels reservations, closes slots and rejects every new booking route', async t => {
  const f = await fixture(t), { venue, slot, booking } = await bookingFixture(f);
  assert.equal((await f.cmd(f.admin, 'venue.archive', { venueId: venue, expectedVersion: 1 })).result.version, 2);
  assert.equal((await f.store.get('SELECT status FROM bookings WHERE id=?', [booking])).status, 'cancelled');
  assert.equal((await f.store.get('SELECT status FROM venue_payments WHERE booking_id=?', [booking])).status, 'cancelled');
  assert.equal((await f.store.get('SELECT status FROM venue_slots WHERE id=?', [slot])).status, 'cancelled');
  assert.ok((await f.service.bootstrap(f.host)).notifications.some(n => n.title === 'Tesis rezervasyonu iptal edildi'));
  assert.ok(!(await f.service.bootstrap()).venues.some(v => v.id === venue));
  await assert.rejects(f.cmd(f.admin, 'venue.slot.create', { venueId: venue, court: 'Yeni saha', startsAtEpochMillis: f.clock() + 7200000, durationMin: 60, priceMinor: 10000 }), fails('VENUE_ARCHIVED'));
  await assert.rejects(f.cmd(f.guest, 'booking.create', { slotId: slot }), fails('VENUE_ARCHIVED'));
  await assert.rejects(f.cmd(f.admin, 'venue.slot.reopen', { slotId: slot, expectedVersion: 2 }), fails('VENUE_ARCHIVED'));
});

test('paid and ongoing venue bookings prevent silent archival until properly resolved', async t => {
  const f = await fixture(t), { venue, booking } = await bookingFixture(f);
  await f.cmd(f.admin, 'booking.confirm', { bookingId: booking });
  await f.cmd(f.admin, 'booking.payment.record', { bookingId: booking, reference: 'paid-before-close' });
  await assert.rejects(f.cmd(f.admin, 'venue.archive', { venueId: venue, expectedVersion: 1 }), fails('PAID_BOOKING'));
  await f.cmd(f.admin, 'booking.refund.record', { bookingId: booking, reference: 'refunded-before-close' });
  const next = (await f.cmd(f.host, 'booking.create', { slotId: (await f.service.operatorVenues(f.admin)).items.find(v => v.id === venue).slots[0].id })).result.id;
  f.advance(70 * 60000);
  await assert.rejects(f.cmd(f.admin, 'venue.archive', { venueId: venue, expectedVersion: 1 }), fails('ACTIVE_BOOKING'));
  await f.cmd(f.admin, 'booking.cancel', { bookingId: next });
  await f.cmd(f.admin, 'venue.archive', { venueId: venue, expectedVersion: 1 });
});

test('team appearance, home city and recruitment rules can be edited without pruning an existing roster',async t=>{
  const f=await fixture(t),team=(await f.cmd(f.host,'team.create',{name:'Eski takım',sport:'FOOTBALL',area:'Kadıköy'})).result.id;
  let card=(await f.service.bootstrap(f.host)).teams.find(item=>item.id===team);assert.equal(card.city,'İstanbul');assert.equal(card.version,1);
  await f.cmd(f.host,'team.invite',{teamId:team,userId:f.guest.id});await f.cmd(f.guest,'team.invite.respond',{teamId:team,accept:true});
  const edit={teamId:team,expectedVersion:1,name:'Yeni takım',badge:'🏆',colorHex:'#FF123456',city:'Ankara',area:'Çankaya',requiredMmr:1500,availability:'Pazartesi akşamı',description:'Güncel takım açıklaması',recruitingOpen:true};
  await assert.rejects(f.cmd(f.guest,'team.update',edit),fails('FORBIDDEN'));
  assert.equal((await f.cmd(f.host,'team.update',edit)).result.version,2);
  card=(await f.service.bootstrap(f.host)).teams.find(item=>item.id===team);
  for(const field of ['name','badge','colorHex','city','area','requiredMmr','availability','description','recruitingOpen'])assert.deepEqual(card[field],edit[field]);
  assert.ok(card.memberIds.includes(f.guest.id));assert.equal((await f.store.get('SELECT title FROM threads WHERE team_id=?',[team])).title,'Yeni takım');
  await assert.rejects(f.cmd(f.host,'team.update',edit),fails('VERSION_CONFLICT'));
  await assert.rejects(f.cmd(f.other,'team.request',{teamId:team}),fails('SKILL_REQUIRED'));
  await assert.rejects(f.cmd(f.host,'team.update',{teamId:team,expectedVersion:2,colorHex:'bad-color'}),fails('INVALID_INPUT'));
  await f.cmd(f.host,'team.update',{teamId:team,description:'Eski istemci çağrısı',recruitingOpen:false});
  assert.equal((await f.service.bootstrap(f.host)).teams.find(item=>item.id===team).version,3);
  await f.cmd(f.host,'team.archive',{teamId:team});await assert.rejects(f.cmd(f.host,'team.update',{...edit,expectedVersion:3}),fails('TEAM_ARCHIVED'));
});
