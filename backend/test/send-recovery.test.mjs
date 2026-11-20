import test from 'node:test';
import assert from 'node:assert/strict';
import sharp from 'sharp';
import { fixture } from './helpers.mjs';
import { cleanMedia } from '../src/storage.mjs';
import { createApi } from '../src/server.mjs';

const photo = () => sharp({create:{width:8,height:8,channels:3,background:'white'}}).jpeg().toBuffer();
const receipt = (f, actor, operation, payload, requestKey) => f.service.commandReceipt(actor,{operation,payload,requestKey});

for (const operation of ['chat.send','memory.create']) test(`${operation}: a photo left unattached for over 24h can be renewed with one committed send`, async t => {
  const f = await fixture(t), bytes = await photo();
  await f.cmd(f.guest,'preferences.save',{privacy:{dmPolicy:'Herkes'}});
  const threadId = (await f.cmd(f.host,'chat.direct',{userId:f.guest.id})).result.id;
  const key = 'delayed-photo-request', original = await f.service.upload(f.host,bytes,'image/jpeg',key);
  const payload = operation === 'chat.send' ? {threadId,text:'Bekleyen fotoğraf',uploadId:original.id} : {caption:'Bekleyen anı',sport:'FOOTBALL',city:'İstanbul',area:'Kadıköy',uploadId:original.id,shareThreadId:threadId};
  f.advance(86400001);
  await cleanMedia(f.service);
  assert.equal((await f.store.get('SELECT status FROM uploads WHERE id=?',[original.id])).status,'deleted');
  await assert.rejects(f.cmd(f.host,operation,payload,key),error => error.code === 'UPLOAD_EXPIRED');
  assert.deepEqual(await receipt(f,f.host,operation,payload,key),{found:false});
  await assert.rejects(f.service.upload(f.host,bytes,'image/jpeg',key),error => error.code === 'UPLOAD_EXPIRED');
  const renewed = await f.service.upload(f.host,bytes,'image/jpeg',`${key}.photo.1`);
  const finalPayload = {...payload,uploadId:renewed.id};
  const first = await f.cmd(f.host,operation,finalPayload,key), repeated = await f.cmd(f.host,operation,finalPayload,key);
  assert.deepEqual(first.result,repeated.result);
  assert.deepEqual(await receipt(f,f.host,operation,finalPayload,key),{found:true,result:first.result});
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM messages WHERE thread_id=?',[threadId])).n,1);
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM commands WHERE user_id=? AND request_key=?',[f.host.id,key])).n,1);
  if (operation === 'memory.create') assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM memories')).n,1);
  await cleanMedia(f.service);
  assert.equal((await f.store.get('SELECT status FROM uploads WHERE id=?',[renewed.id])).status,'ready');
});

test('a lost listing acknowledgement is recoverable after its time passes and free tokens fall to zero', async t => {
  const f = await fixture(t), payload = f.event(), key = 'listing-lost-acknowledgement';
  await f.cmd(f.host,'event.create',f.event({title:'Diğer ilan'}));
  assert.equal((await f.service.bootstrap(f.host)).wallet.balance,10);
  const first = await f.cmd(f.host,'event.create',payload,key);
  assert.equal(first.snapshot.wallet.balance,0);
  f.advance(2*3600000);
  assert.deepEqual(await receipt(f,f.host,'event.create',payload,key),{found:true,result:first.result});
  const replay = await f.cmd(f.host,'event.create',payload,key);
  assert.deepEqual(replay.result,first.result);
  assert.equal(replay.replayed,true);
  assert.equal(replay.snapshot.wallet.balance,0);
  assert.equal(replay.snapshot.wallet.locked,20);
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM events WHERE host_id=?',[f.host.id])).n,2);
  await assert.rejects(receipt(f,f.host,'event.create',{...payload,title:'Değişen ilan'},key),error => error.code === 'IDEMPOTENCY_CONFLICT');
  assert.deepEqual(await receipt(f,f.guest,'event.create',payload,key),{found:false});
});

test('receipt checks wait for the committing command and preserve its result after a conversation closes', async t => {
  const f = await fixture(t);
  await f.cmd(f.guest,'preferences.save',{privacy:{dmPolicy:'Herkes'}});
  const threadId = (await f.cmd(f.host,'chat.direct',{userId:f.guest.id})).result.id;
  const key = 'closing-thread-photo-send', payload = {threadId,text:'Kaydedilen mesaj'};
  const sending = f.cmd(f.host,'chat.send',payload,key);
  const checking = receipt(f,f.host,'chat.send',payload,key);
  const sent = await sending;
  assert.deepEqual(await checking,{found:true,result:sent.result});
  await f.cmd(f.guest,'user.block',{userId:f.host.id,blocked:true});
  assert.deepEqual(await receipt(f,f.host,'chat.send',payload,key),{found:true,result:sent.result});
  assert.deepEqual((await f.cmd(f.host,'chat.send',payload,key)).result,sent.result);
  await assert.rejects(f.cmd(f.host,'chat.send',{threadId,text:'Yeni mesaj'},'blocked-new-message'),error => error.code === 'BLOCKED');
  await assert.rejects(receipt(f,null,'chat.send',payload,key),error => error.status === 401);
});

test('HTTP receipt resolution requires the same account, exact payload and valid original key', async t => {
  const f = await fixture(t), server = createApi(f.service,f.auth);
  await new Promise(resolve => server.listen(0,'127.0.0.1',resolve));
  t.after(async () => {server.realtime.close();server.closeAllConnections();await new Promise(resolve => server.close(resolve));});
  const url = `http://127.0.0.1:${server.address().port}/v1/command-receipts/resolve`;
  const payload = f.event(), requestKey = 'http-listing-receipt', operation = 'event.create';
  const created = await f.cmd(f.host,operation,payload,requestKey);
  const request = (who,body) => fetch(url,{method:'POST',headers:{'Content-Type':'application/json',...(who ? {Authorization:`Bearer ${f.sessions[who].accessToken}`} : {})},body:JSON.stringify(body)});
  assert.equal((await request(null,{operation,payload,requestKey})).status,401);
  const found = await request('host',{operation,payload,requestKey});
  assert.equal(found.status,200);
  assert.deepEqual(await found.json(),{found:true,result:created.result});
  assert.deepEqual(await (await request('guest',{operation,payload,requestKey})).json(),{found:false});
  assert.equal((await request('host',{operation,payload:{...payload,title:'Farklı içerik'},requestKey})).status,409);
  assert.equal((await request('host',{operation,payload,requestKey:'short'})).status,400);
  assert.equal((await request('host',{operation,payload:[],requestKey})).status,400);
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM events')).n,1);
});
