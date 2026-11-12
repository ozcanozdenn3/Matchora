import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { fixture } from './helpers.mjs';
async function reviews(f,count){
 const venue=(await f.cmd(f.admin,'venue.create',{name:'Yorum tesisi',city:'İstanbul',area:'Kadıköy',sports:['TENNIS'],courts:1})).result.id;
 const slot=(await f.cmd(f.admin,'venue.slot.create',{venueId:venue,court:'Kort',startsAtEpochMillis:f.clock()+3600000,durationMin:60,priceMinor:10000})).result.id;
 for(let n=0;n<count;n++){
  const booking=randomUUID(),review=randomUUID(),user=n%2===0?f.host:f.guest;
  await f.store.run("INSERT INTO bookings(id,slot_id,user_id,status,amount_minor,split_count,cancellation_deadline,created_at) VALUES(?,?,?,'completed',10000,1,?,?)",[booking,slot,user.id,f.clock(),f.clock()]);
  await f.store.run("INSERT INTO venue_payments(id,booking_id,amount_minor,currency,status,created_at) VALUES(?,?,10000,'TRY','paid',?)",[randomUUID(),booking,f.clock()]);
  await f.store.run("INSERT INTO venue_reviews(id,booking_id,venue_id,user_id,rating,comment,status,created_at,updated_at) VALUES(?,?,?,?,5,?,'published',?,?)",[review,booking,venue,user.id,`Değerlendirme ${n}`,f.clock(),f.clock()]);
 }
 return venue;
}
test('venue reviews page beyond fifty with stable same-time ordering and scoped cursors',async t=>{
 const f=await fixture(t),venue=await reviews(f,115),ids=new Set();
 let page=await f.service.venueReviews(f.other,venue,{limit:13}),firstCursor=page.nextCursor;
 do{for(const item of page.items){assert.ok(!ids.has(item.id));ids.add(item.id);}page=page.nextCursor?await f.service.venueReviews(f.other,venue,{limit:13,after:page.nextCursor}):null;}while(page);
 assert.equal(ids.size,115);
 await assert.rejects(f.service.venueReviews(f.guest,venue,{after:firstCursor}),e=>e.code==='INVALID_CURSOR');
 const second=(await f.cmd(f.admin,'venue.create',{name:'Başka yorum tesisi',city:'İstanbul',area:'Kadıköy',sports:['TENNIS'],courts:1})).result.id;
 await assert.rejects(f.service.venueReviews(f.other,second,{after:firstCursor}),e=>e.code==='INVALID_CURSOR');
});
test('blocked reviews are excluded before the page limit and deleted authors cannot reappear',async t=>{
 const f=await fixture(t),venue=await reviews(f,110);
 await f.cmd(f.other,'user.block',{userId:f.guest.id,blocked:true});
 const first=await f.service.venueReviews(f.other,venue,{limit:50});assert.equal(first.items.length,50);assert.ok(first.items.every(v=>v.authorId===f.host.id));
 const second=await f.service.venueReviews(f.other,venue,{limit:50,after:first.nextCursor});assert.equal(second.items.length,5);
 await f.store.run("UPDATE users SET status='deleted' WHERE id=?",[f.host.id]);assert.equal((await f.service.venueReviews(f.other,venue)).items.length,0);
});
