import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { migrate } from '../src/store.mjs';

test('upgrading the old single-window slot constraint preserves paid booking foreign keys and is repeatable',async t=>{
 const f=await fixture(t);
 const venue=(await f.cmd(f.admin,'venue.create',{name:'Eski şema tesisi',city:'İstanbul',area:'Kadıköy',sports:['TENNIS'],courts:2})).result.id;
 const slot=(await f.cmd(f.admin,'venue.slot.create',{venueId:venue,court:'Kort 1',startsAtEpochMillis:f.clock()+7200000,durationMin:60,priceMinor:10001})).result.id;
 const booking=(await f.cmd(f.host,'booking.create',{slotId:slot})).result.id;
 await f.cmd(f.admin,'booking.confirm',{bookingId:booking});await f.cmd(f.admin,'booking.payment.record',{bookingId:booking,reference:'existing-paid-receipt'});
 // Reproduce the shipped table layout before the multi-court upgrade.
 await f.store.exec(`PRAGMA foreign_keys=OFF;BEGIN;
 CREATE TABLE old_venue_slots(id TEXT PRIMARY KEY,venue_id TEXT NOT NULL REFERENCES venues(id),starts_at BIGINT NOT NULL,ends_at BIGINT NOT NULL,price_minor INTEGER NOT NULL CHECK(price_minor>=0),status TEXT NOT NULL,payload TEXT NOT NULL,UNIQUE(venue_id,starts_at,ends_at));
 INSERT INTO old_venue_slots SELECT * FROM venue_slots;DROP TABLE venue_slots;ALTER TABLE old_venue_slots RENAME TO venue_slots;COMMIT;PRAGMA foreign_keys=ON;`);
 await migrate(f.store);await migrate(f.store);
 assert.deepEqual(await f.store.all('PRAGMA foreign_key_check'),[]);
 assert.equal((await f.store.get('SELECT slot_id FROM bookings WHERE id=?',[booking])).slot_id,slot);
 assert.equal((await f.store.get('SELECT provider_ref FROM venue_payments WHERE booking_id=?',[booking])).provider_ref,'existing-paid-receipt');
 const parallel=(await f.cmd(f.admin,'venue.slot.create',{venueId:venue,court:'Kort 2',startsAtEpochMillis:f.clock()+7200000,durationMin:60,priceMinor:10001})).result.id;assert.notEqual(parallel,slot);
});
