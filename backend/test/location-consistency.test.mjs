import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { fixture } from './helpers.mjs';

async function venue(f,payload){
  const venueId=randomUUID();
  await f.store.run('INSERT INTO venues(id,owner_id,payload,created_at) VALUES(?,?,?,?)',[venueId,f.admin.id,JSON.stringify(payload),f.clock()]);
  return venueId;
}

test('venue catalog folds Turkish city names, applies radius and exposes owner and distance',async t=>{
  const f=await fixture(t);
  const near=await venue(f,{name:'Yakın Tesis',city:'İstanbul',area:'Kadıköy',sports:['FOOTBALL'],approximateLocation:{latitude:40.99,longitude:29.03}});
  const far=await venue(f,{name:'Uzak Tesis',city:'İstanbul',area:'Beylikdüzü',sports:['FOOTBALL'],approximateLocation:{latitude:41.0,longitude:28.64}});
  await venue(f,{name:'Ankara Tesis',city:'Ankara',area:'Çankaya',sports:['FOOTBALL'],approximateLocation:{latitude:39.92,longitude:32.85}});
  const page=await f.service.catalog(null,'venues',{city:'istanbul',latitude:40.99,longitude:29.03,radiusKm:10});
  const ids=page.items.map(item=>item.id);
  assert.ok(ids.includes(near));assert.ok(!ids.includes(far));
  const item=page.items.find(v=>v.id===near);
  assert.equal(item.ownerId,f.admin.id);assert.equal(typeof item.distanceKm,'number');assert.ok(item.distanceKm<1);
  const all=await f.service.catalog(null,'venues',{city:'İSTANBUL'});
  assert.equal(all.items.length,2);
});

test('accepted challenge attaches matching venue id and coordinates instead of a hardcoded district',async t=>{
  const f=await fixture(t);
  const venueId=await venue(f,{name:'Ataşehir Arena',city:'İstanbul',area:'Ataşehir',sports:['FOOTBALL'],approximateLocation:{latitude:40.98,longitude:29.12}});
  const home=(await f.cmd(f.host,'team.create',{name:'Ev Takımı',sport:'FOOTBALL',city:'İstanbul',area:'Kadıköy'})).result.id;
  const away=(await f.cmd(f.guest,'team.create',{name:'Rakip Takım',sport:'FOOTBALL',city:'İstanbul',area:'Ataşehir'})).result.id;
  const challenge=(await f.cmd(f.host,'challenge.create',{fromTeamId:home,toTeamId:away,proposedTime:'Cumartesi 20:00',venue:'Ataşehir Arena',stakeTokens:0,startsAtEpochMillis:f.clock()+86400000})).result.id;
  const response=(await f.cmd(f.guest,'challenge.respond',{challengeId:challenge,accept:true})).result;
  const event=await f.store.get('SELECT * FROM events WHERE id=?',[response.eventId]);
  const payload=JSON.parse(event.payload);
  assert.equal(payload.venueId,venueId);assert.equal(payload.area,'Ataşehir');assert.equal(event.district,'Ataşehir');
  assert.ok(event.latitude!=null&&event.longitude!=null);
});

