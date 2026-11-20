import test from 'node:test';
import assert from 'node:assert/strict';
import { createApi } from '../src/server.mjs';
import { fixture } from './helpers.mjs';

async function readEvent(reader) {
  const decoder = new TextDecoder();
  let buffer = '';
  while (!buffer.includes('\n\n')) {
    const {done,value} = await reader.read();
    assert.equal(done, false, 'live connection ended before event');
    buffer += decoder.decode(value, {stream:true});
  }
  return JSON.parse(buffer.split('\n').find(line => line.startsWith('data: ')).slice(6));
}

test('HTTP search, private history, typing and post-commit chat updates use the authenticated routes', async t => {
  const f = await fixture(t), server = createApi(f.service,f.auth);
  await new Promise(resolve => server.listen(0,'127.0.0.1',resolve));
  const abort = new AbortController();
  t.after(async () => {
    abort.abort();
    server.realtime.close();
    server.closeAllConnections();
    await new Promise(resolve => server.close(resolve));
  });
  const base = `http://127.0.0.1:${server.address().port}`;
  const headers = actor => ({Authorization:`Bearer ${f.sessions[actor].accessToken}`});
  assert.equal((await fetch(`${base}/v1/players/search?q=Konuk`)).status,401);
  assert.equal((await fetch(`${base}/v1/history/wallet`)).status,401);
  const players = await (await fetch(`${base}/v1/players/search?q=Konuk&sport=FOOTBALL`,{headers:headers('host')})).json();
  assert.deepEqual(players.items.map(item => item.id),[f.guest.id]);
  await f.grant(f.host,77);
  const history = await (await fetch(`${base}/v1/history/wallet`,{headers:headers('host')})).json();
  assert.ok(history.items.some(item => item.amount===77));
  assert.equal((await fetch(`${base}/v1/realtime`)).status,401);
  await f.cmd(f.guest,'preferences.save',{privacy:{dmPolicy:'Herkes'}});
  const threadId = (await f.cmd(f.host,'chat.direct',{userId:f.guest.id})).result.id;
  const stream = await fetch(`${base}/v1/realtime`,{headers:headers('guest'),signal:AbortSignal.any([abort.signal,AbortSignal.timeout(8000)])});
  const reader = stream.body.getReader();
  assert.deepEqual(await readEvent(reader),{type:'invalidate'});
  const typing = await fetch(`${base}/v1/threads/${threadId}/typing`,{method:'POST',headers:{...headers('host'),'Content-Type':'application/json'},body:JSON.stringify({typing:true})});
  assert.equal(typing.status,200);
  const typingEvent = await readEvent(reader);
  assert.equal(typingEvent.type,'typing');
  assert.equal(typingEvent.threadId,threadId);
  const sent = await fetch(`${base}/v1/commands/chat.send`,{method:'POST',headers:{...headers('host'),'Content-Type':'application/json','Idempotency-Key':'http-realtime-message'},body:JSON.stringify({threadId,text:'Anlık kontrol'})});
  assert.equal(sent.status,200);
  assert.deepEqual(await readEvent(reader),{type:'invalidate',threadId});
  const detail = await (await fetch(`${base}/v1/threads/${threadId}`,{headers:headers('guest')})).json();
  assert.equal(detail.item.messages.at(-1).text,'Anlık kontrol');
  await reader.cancel();
});
