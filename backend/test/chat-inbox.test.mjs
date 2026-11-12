import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { fixture } from './helpers.mjs';
import { createApi } from '../src/server.mjs';

async function manyThreads(f, count) {
  const ids=[];
  await f.store.transaction(async db=>{
    for(let i=0;i<count;i++) {
      const id=randomUUID();ids.push(id);
      await db.run('INSERT INTO threads(id,kind,title,created_at) VALUES(?,?,?,?)',[id,'DIRECT',`Sohbet ${i}`,f.clock()-i]);
      for(const actor of [f.host,f.guest])await db.run('INSERT INTO thread_members(thread_id,user_id) VALUES(?,?)',[id,actor.id]);
    }
  });
  return ids;
}

test('chat inbox exposes all conversations after the initial hundred with stable actor-bound cursors',async t=>{
  const f=await fixture(t),ids=await manyThreads(f,107),first=await f.service.bootstrap(f.host);
  assert.equal(first.threads.length,100);assert.ok(first.nextThreadCursor);
  const next=await f.service.threads(f.host,{after:first.nextThreadCursor});
  assert.equal(next.items.length,7);assert.equal(next.nextCursor,null);
  assert.equal(new Set([...first.threads,...next.items].map(t=>t.id)).size,107);
  assert.deepEqual([...first.threads,...next.items].map(t=>t.id),ids);
  await assert.rejects(f.service.threads(f.guest,{after:first.nextThreadCursor}),e=>e.code==='INVALID_CURSOR');
  await assert.rejects(f.service.threads(null,{}),e=>e.code==='UNAUTHENTICATED');
});

test('old notification targets resolve independently, hidden or revoked threads cannot leak, blocking drops message bodies',async t=>{
  const f=await fixture(t),ids=await manyThreads(f,103),id=ids.at(-1);
  assert.ok(!(await f.service.bootstrap(f.host)).threads.some(t=>t.id===id));
  assert.equal((await f.service.thread(f.host,id)).item.id,id);
  await assert.rejects(f.service.thread(f.other,id),e=>e.code==='NOT_FOUND');
  await f.cmd(f.host,'chat.delete',{threadId:ids[0]});
  await assert.rejects(f.service.thread(f.host,ids[0]),e=>e.code==='NOT_FOUND');
  const page=await f.service.threads(f.host,{limit:100});assert.equal(page.items.length,100);assert.ok(!page.items.some(t=>t.id===ids[0]));
  await f.cmd(f.host,'chat.send',{threadId:id,text:'Özel içerik'});
  await f.cmd(f.guest,'user.block',{userId:f.host.id,blocked:true});
  const locked=await f.service.thread(f.host,id);assert.equal(locked.item.locked,true);assert.deepEqual(locked.item.messages,[]);
  await f.store.run('DELETE FROM thread_members WHERE thread_id=? AND user_id=?',[id,f.host.id]);
  await assert.rejects(f.service.thread(f.host,id),e=>e.code==='NOT_FOUND');
});

test('HTTP chat inbox and direct thread endpoints require the signed-in member',async t=>{
  const f=await fixture(t),[id]=await manyThreads(f,1),server=createApi(f.service,f.auth);
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));t.after(()=>new Promise(resolve=>server.close(resolve)));
  const base=`http://127.0.0.1:${server.address().port}`;
  assert.equal((await fetch(`${base}/v1/threads`)).status,401);
  const own={Authorization:`Bearer ${f.sessions.host.accessToken}`},other={Authorization:`Bearer ${f.sessions.other.accessToken}`};
  assert.equal((await fetch(`${base}/v1/threads/${id}`,{headers:other})).status,404);
  assert.equal((await(await fetch(`${base}/v1/threads/${id}`,{headers:own})).json()).item.id,id);
  assert.equal((await(await fetch(`${base}/v1/threads`,{headers:own})).json()).items[0].id,id);
});

test('unread conversation badge counts beyond the first page and respects read, hide and blocking',async t=>{
  const f=await fixture(t),[id]=await manyThreads(f,1);
  await f.cmd(f.guest,'chat.send',{threadId:id,text:'Eski ama okunmadı'});
  f.advance(10000);await manyThreads(f,103);
  let snapshot=await f.service.bootstrap(f.host);
  assert.ok(!snapshot.threads.some(t=>t.id===id));assert.equal(snapshot.unreadThreadCount,1);
  await f.cmd(f.host,'chat.read',{threadId:id});assert.equal((await f.service.bootstrap(f.host)).unreadThreadCount,0);
  await f.cmd(f.guest,'chat.send',{threadId:id,text:'Yeni'});assert.equal((await f.service.bootstrap(f.host)).unreadThreadCount,1);
  await f.cmd(f.host,'chat.delete',{threadId:id});assert.equal((await f.service.bootstrap(f.host)).unreadThreadCount,0);
  await f.cmd(f.guest,'chat.send',{threadId:id,text:'Yeniden açıldı'});
  await f.cmd(f.host,'user.block',{userId:f.guest.id,blocked:true});assert.equal((await f.service.bootstrap(f.host)).unreadThreadCount,0);
});
