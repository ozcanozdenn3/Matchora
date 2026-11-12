import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
const venue=async f=>(await f.cmd(f.admin,'venue.create',{name:'Programlı spor tesisi',city:'Ankara',area:'Çankaya',sports:['FOOTBALL'],courts:1,cancellationHours:3})).result.id;
const plan=(f,id,extra={})=>({venueId:id,court:'Saha 1',startsAtEpochMillis:f.clock()+3600000,days:3,weekdays:[1,2,3,4,5,6,7],slotsPerDay:2,durationMin:60,priceMinor:12345,...extra});

test('weekly availability creates real exact-price slots atomically with stable replay and owner authorization',async t=>{
 const f=await fixture(t),id=await venue(f),payload=plan(f,id);
 await assert.rejects(f.cmd(f.host,'venue.slot.bulk.create',payload),e=>e.code==='FORBIDDEN');
 const first=await f.cmd(f.admin,'venue.slot.bulk.create',payload,'weekly-plan-create');assert.equal(first.result.created,6);
 const replay=await f.cmd(f.admin,'venue.slot.bulk.create',payload,'weekly-plan-create');assert.deepEqual(replay.result,first.result);
 const rows=await f.store.all('SELECT * FROM venue_slots WHERE venue_id=? ORDER BY starts_at',[id]);assert.equal(rows.length,6);assert.ok(rows.every(s=>s.price_minor===12345));assert.equal(Number(rows[1].starts_at)-Number(rows[0].starts_at),3600000);
 await assert.rejects(f.cmd(f.admin,'venue.slot.bulk.create',plan(f,id,{days:60,slotsPerDay:8})),e=>e.code==='INVALID_INPUT');
});

test('a single conflict rolls back every new slot and preserves booked availability',async t=>{
 const f=await fixture(t),id=await venue(f),payload=plan(f,id),conflict=payload.startsAtEpochMillis+3600000;
 const slot=(await f.cmd(f.admin,'venue.slot.create',{venueId:id,court:'Saha 1',startsAtEpochMillis:conflict,durationMin:60,priceMinor:1000})).result.id;
 const booking=(await f.cmd(f.guest,'booking.create',{slotId:slot,splitCount:1})).result.id;
 await assert.rejects(f.cmd(f.admin,'venue.slot.bulk.create',payload),e=>e.code==='SLOT_OVERLAP');
 assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM venue_slots WHERE venue_id=?',[id])).n,1);
 assert.equal((await f.store.get('SELECT status FROM bookings WHERE id=?',[booking])).status,'pending');
});

test('venue review screen exposes only the current customer completed paid reservations for writing',async t=>{
 const f=await fixture(t),id=await venue(f),slot=(await f.cmd(f.admin,'venue.slot.create',{venueId:id,court:'Saha 1',startsAtEpochMillis:f.clock()+3600000,durationMin:60,priceMinor:12345})).result.id;
 const booking=(await f.cmd(f.guest,'booking.create',{slotId:slot,splitCount:1})).result.id;
 assert.deepEqual((await f.service.venueReviews(f.guest,id)).reviewableBookings,[]);
 await f.cmd(f.admin,'booking.confirm',{bookingId:booking});await f.cmd(f.admin,'booking.payment.record',{bookingId:booking,reference:'venue-receipt-123'});f.advance(3*3600000);await f.cmd(f.admin,'booking.complete',{bookingId:booking});
 const own=await f.service.venueReviews(f.guest,id);assert.equal(own.reviewableBookings[0].id,booking);assert.equal(own.reviewableBookings[0].amountMinor,12345);
 assert.deepEqual((await f.service.venueReviews(f.other,id)).reviewableBookings,[]);assert.deepEqual((await f.service.venueReviews(null,id)).reviewableBookings,[]);
 await f.cmd(f.guest,'venue.review.save',{bookingId:booking,rating:5,comment:'Saha deneyimi çok iyiydi.'});
 assert.equal((await f.service.venueReviews(f.guest,id)).items[0].status,'pending');
});
