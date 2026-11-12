import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { meetingLocation } from '../src/event-locations.mjs';
import { processDeletions } from '../src/jobs.mjs';
const exact={latitude:40.987654321,longitude:29.034567891};
async function saved(f,id,actor){const event=await f.store.get('SELECT * FROM events WHERE id=?',[id]);return meetingLocation(f.service.ctx(f.store,actor),event);}
test('exact meeting points are private to host and current members while guests and applicants only see the rounded pin',async t=>{
 const f=await fixture(t);for(const user of [f.host,f.guest,f.other])await f.grant(user);
 const id=(await f.cmd(f.host,'event.create',f.event({approvalRequired:true,approximateLocation:exact}))).result.id;
 assert.deepEqual(await saved(f,id,f.host),exact);assert.equal(await saved(f,id,null),null);assert.equal(await saved(f,id,f.guest),null);
 const publicEvent=await f.service.event(null,id);assert.deepEqual(publicEvent.approximateLocation,{latitude:40.99,longitude:29.03});assert.equal(publicEvent.meetingLocation,null);
 const request=(await f.cmd(f.guest,'event.request',{eventId:id})).result.id;assert.equal(await saved(f,id,f.guest),null);
 await f.cmd(f.host,'event.request.review',{requestId:request,approve:true});assert.deepEqual(await saved(f,id,f.guest),exact);
 assert.deepEqual((await f.service.event(f.guest,id)).meetingLocation,exact);
 const version=(await f.service.event(f.host,id)).version;await f.cmd(f.host,'event.member.remove',{eventId:id,userId:f.guest.id,expectedVersion:version});assert.equal(await saved(f,id,f.guest),null);
});
test('changing only the venue text removes stale coordinates and coordinate edits keep private precision',async t=>{
 const f=await fixture(t);await f.grant(f.host);
 const id=(await f.cmd(f.host,'event.create',f.event({approximateLocation:exact}))).result.id;
 let version=(await f.service.event(f.host,id)).version;
 await f.cmd(f.host,'event.update',{eventId:id,expectedVersion:version,venue:'Başka kort'});assert.equal(await saved(f,id,f.host),null);assert.equal((await f.service.event(null,id)).approximateLocation,null);
 version=(await f.service.event(f.host,id)).version;await f.cmd(f.host,'event.update',{eventId:id,expectedVersion:version,approximateLocation:exact});assert.deepEqual(await saved(f,id,f.host),exact);
 await assert.rejects(f.cmd(f.host,'event.update',{eventId:id,expectedVersion:(await f.service.event(f.host,id)).version,approximateLocation:{latitude:NaN,longitude:29}}),e=>e.code==='INVALID_LOCATION');
 assert.deepEqual(await saved(f,id,f.host),exact);
});
test('former participants lose precise points after the travel window and account deletion redacts hosted points',async t=>{
 const f=await fixture(t);for(const user of [f.host,f.guest])await f.grant(user);
 const id=(await f.cmd(f.host,'event.create',f.event({approximateLocation:exact}))).result.id;await f.cmd(f.guest,'event.join',{eventId:id});
 f.advance(27*3600000);assert.equal(await saved(f,id,f.guest),null);assert.deepEqual(await saved(f,id,f.host),exact);
 await f.cmd(f.host,'account.delete');f.advance(2000);await f.store.transaction(db=>processDeletions(f.service.ctx(db)));assert.equal(await saved(f,id,f.host),null);
});
