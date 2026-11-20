import test from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {fixture} from './helpers.mjs';

test('compact listing and roster changes agree with the full authorized state', async t => {
 const f = await fixture(t);
 const command = (actor, op, body, key = randomUUID()) => f.service.command(actor, op, body, key, {compact:true});
 const key = randomUUID(), payload = f.event();
 const created = await command(f.host, 'event.create', payload, key);
 const id = created.result.id;
 async function compare(actor, response) {
   assert.equal(response.snapshot, undefined);
   assert.equal(response.changes.userId, actor.id);
   const full = await f.service.bootstrap(actor);
   for (const [name,value] of Object.entries(response.changes.fields)) assert.deepEqual(value,full[name],name);
   assert.deepEqual(response.changes.event,full.events.find(event => event.id === id));
   assert.deepEqual(response.changes.thread.item,full.threads.find(thread => thread.id === response.changes.thread.item.id));
 }
 await compare(f.host,created);
 const replay = await command(f.host,'event.create',payload,key);
 assert.equal(replay.replayed,true);
 assert.equal(replay.snapshot.events.length,1);
 await compare(f.guest,await command(f.guest,'event.join',{eventId:id}));
 const threadId = created.changes.thread.item.id;
 f.advance(1000);
 const sent = await command(f.host,'chat.send',{threadId,text:'Merhaba'});
 assert.ok(sent.changes.thread.item.messages.some(message => message.id === sent.result.id));
 assert.deepEqual(sent.changes.thread.item,(await f.service.bootstrap(f.host)).threads.find(t => t.id === threadId));
});
