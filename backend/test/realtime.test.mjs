import test from 'node:test';
import assert from 'node:assert/strict';
import { EventEmitter, once } from 'node:events';
import { fixture } from './helpers.mjs';
import { RealtimeHub } from '../src/realtime.mjs';

class Response extends EventEmitter {
  constructor() { super(); this.frames = []; this.destroyed = false; this.writableEnded = false; this.writableLength = 0; }
  writeHead(status, headers) { this.status = status; this.headers = headers; }
  flushHeaders() { }
  write(frame) { this.frames.push(frame); return true; }
  end() { this.writableEnded = true; this.emit('close'); }
  destroy() { this.destroyed = true; this.emit('close'); }
  events() { return this.frames.filter(frame => frame.startsWith('data: ')).map(frame => JSON.parse(frame.slice(6))); }
}
async function connect(hub, actor, session) {
  const req = new EventEmitter(), res = new Response();
  await hub.connect(req, res, actor, session.accessToken);
  assert.equal(res.status, 200);
  assert.equal(res.headers['Content-Type'], 'text/event-stream; charset=utf-8');
  assert.deepEqual(res.events(), [{type:'invalidate'}]);
  res.frames = [];
  return res;
}

test('real-time delivery invalidates only the conversation audience and never streams message contents', async t => {
  const f = await fixture(t), hub = new RealtimeHub(f.service, f.auth);
  t.after(() => hub.close());
  await f.cmd(f.guest,'preferences.save',{privacy:{dmPolicy:'Herkes'}});
  const threadId = (await f.cmd(f.host, 'chat.direct', {userId:f.guest.id})).result.id;
  const host = await connect(hub,f.host,f.sessions.host), guest = await connect(hub,f.guest,f.sessions.guest), other = await connect(hub,f.other,f.sessions.other);
  const secondSession = await f.auth.devLogin({identity:'host'});
  const secondDevice = await connect(hub,await f.auth.actor(secondSession.accessToken),secondSession);
  const payload = {threadId,text:'Özel mesaj gövdesi'};
  const response = await f.cmd(f.host,'chat.send',payload);
  await hub.command(f.host,'chat.send',payload,response.result);
  assert.deepEqual(host.events(), []);
  assert.deepEqual(secondDevice.events(), [{type:'invalidate',threadId}]);
  assert.deepEqual(guest.events(), [{type:'invalidate',threadId}]);
  assert.deepEqual(other.events(), []);
  assert.ok(!JSON.stringify(guest.frames).includes(payload.text));
  guest.end();
  assert.equal(hub.clientCount,3);
});

test('typing is transient, throttled, scoped to active members and suppressed after blocking or personal deletion', async t => {
  const f = await fixture(t), hub = new RealtimeHub(f.service,f.auth);
  t.after(() => hub.close());
  await f.cmd(f.guest,'preferences.save',{privacy:{dmPolicy:'Herkes'}});
  const threadId = (await f.cmd(f.host,'chat.direct',{userId:f.guest.id})).result.id;
  const guest = await connect(hub,f.guest,f.sessions.guest), other = await connect(hub,f.other,f.sessions.other);
  const started = await hub.typing(f.host,threadId,{typing:true});
  assert.deepEqual(started,{sent:true});
  assert.deepEqual(guest.events(),[{type:'typing',threadId,userId:f.host.id,typing:true,expiresAt:f.clock()+5000}]);
  assert.deepEqual(await hub.typing(f.host,threadId,{typing:true}),{sent:false});
  assert.deepEqual(other.events(),[]);
  await assert.rejects(hub.typing(f.other,threadId,{typing:true}), error => error.code === 'FORBIDDEN');
  await assert.rejects(hub.typing(f.host,threadId,{typing:'yes'}), error => error.code === 'INVALID_INPUT');
  await f.cmd(f.guest,'chat.delete',{threadId});
  guest.frames = [];
  await hub.typing(f.host,threadId,{typing:false});
  assert.deepEqual(guest.events(),[]);
  await f.cmd(f.host,'user.block',{userId:f.guest.id,blocked:true});
  await assert.rejects(hub.typing(f.host,threadId,{typing:false}), error => error.code === 'BLOCKED');
});

test('revoked sessions close their live connection and connection caps cannot be bypassed', async t => {
  const f = await fixture(t), hub = new RealtimeHub(f.service,f.auth,{heartbeatMs:10,maxPerUser:1});
  t.after(() => hub.close());
  const host = await connect(hub,f.host,f.sessions.host);
  await assert.rejects(connect(hub,f.host,f.sessions.host), error => error.code === 'REALTIME_LIMIT');
  const closed = once(host,'close');
  await f.auth.logout(f.host,false);
  await Promise.race([closed,new Promise((_,reject) => {
    const timeout = setTimeout(() => reject(new Error('revoked stream stayed connected')),1000);
    closed.finally(() => clearTimeout(timeout));
  })]);
  assert.deepEqual(host.events(),[{type:'sessionExpired'}]);
  assert.equal(hub.clientCount,0);
  await assert.rejects(connect(hub,f.host,f.sessions.host), error => error.code === 'UNAUTHENTICATED');
});
