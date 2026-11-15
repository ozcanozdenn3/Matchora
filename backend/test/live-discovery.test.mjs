import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { scheduleEngagement } from '../src/engagement.mjs';
import { expireRecords } from '../src/jobs.mjs';

test('fresh approximate location limits emergency calls by chosen radius, expires and never leaks through public players',async t=>{
 const f=await fixture(t);await f.grant(f.host);
 const id=(await f.cmd(f.host,'event.create',f.event({emergency:true,approximateLocation:{latitude:41,longitude:29}}))).result.id;
 await f.cmd(f.guest,'preferences.save',{notifications:{emergencyRadiusKm:1}});
 await f.cmd(f.guest,'discovery.location',{latitude:41.08,longitude:29});
 const saved=JSON.parse((await f.store.get('SELECT payload FROM discovery_preferences WHERE user_id=?',[f.guest.id])).payload);assert.equal(saved.location.latitude,41.08);
 assert.equal(saved.location.expiresAt,f.clock()+15*60000);
 await f.cmd(f.guest,'discovery.save',{city:'İstanbul',district:'Kadıköy',travelMode:false});assert.deepEqual(JSON.parse((await f.store.get('SELECT payload FROM discovery_preferences WHERE user_id=?',[f.guest.id])).payload).location,saved.location);
 await f.store.transaction(db=>scheduleEngagement(f.service.ctx(db)));
 assert.ok(!(await f.service.bootstrap(f.guest)).notifications.some(n=>n.targetId===id));
 await f.cmd(f.guest,'preferences.save',{notifications:{emergencyRadiusKm:25}});f.advance(5*60000+1);
 await f.store.transaction(db=>scheduleEngagement(f.service.ctx(db)));
 assert.ok((await f.service.bootstrap(f.guest)).notifications.some(n=>n.targetId===id&&n.title==='Yakınında oyuncu aranıyor'));
 const publicUser=(await f.service.bootstrap()).players.find(p=>p.id===f.guest.id);assert.equal(publicUser.location,undefined);assert.equal(publicUser.latitude,undefined);
 await f.cmd(f.guest,'discovery.location',{});assert.equal(JSON.parse((await f.store.get('SELECT payload FROM discovery_preferences WHERE user_id=?',[f.guest.id])).payload).location,undefined);
 await f.cmd(f.guest,'discovery.location',{latitude:41.004567,longitude:29.006789});assert.equal(JSON.parse((await f.store.get('SELECT payload FROM discovery_preferences WHERE user_id=?',[f.guest.id])).payload).location.latitude,41);
 f.advance(15*60000+1);await f.store.transaction(db=>expireRecords(f.service.ctx(db)));
 assert.equal(JSON.parse((await f.store.get('SELECT payload FROM discovery_preferences WHERE user_id=?',[f.guest.id])).payload).location,undefined);
});

test('radar matching uses a recent real point and safely falls back to the selected district',async t=>{
 const f=await fixture(t);await f.grant(f.host);
 const id=(await f.cmd(f.host,'event.create',f.event({approximateLocation:{latitude:41,longitude:29}}))).result.id;
 await f.cmd(f.guest,'discovery.location',{latitude:42,longitude:29});
 const distant=await f.service.event(f.guest,id);assert.equal(distant.score.distance,10);assert.ok(distant.score.reasons.includes('Yaklaşık konumuna göre uzaklık'));
 f.advance(16*60000);assert.equal((await f.service.event(f.guest,id)).score.distance,100);
 await f.cmd(f.guest,'preferences.save',{privacy:{chemistrySignals:false}});assert.equal((await f.service.event(f.guest,id)).score.confidence,0);
});

test('regular open match announcement delivers push notifications to nearby athletes based on location coordinates',async t=>{
 const f=await fixture(t);await f.grant(f.host);
 const id=(await f.cmd(f.host,'event.create',f.event({emergency:false,approximateLocation:{latitude:41,longitude:29}}))).result.id;
 await f.cmd(f.guest,'discovery.location',{latitude:41.05,longitude:29.05});
 await f.store.transaction(db=>scheduleEngagement(f.service.ctx(db)));
 const notices=(await f.service.bootstrap(f.guest)).notifications;
 assert.ok(notices.some(n=>n.targetId===id&&n.title==='Yakınında yeni maç ilanı'));
});
