import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { notify } from '../src/model.mjs';
import { notificationPage } from '../src/notification-feed.mjs';
test('notifications page beyond the first hundred, filter before limiting and report complete unread counts',async t=>{
 const f=await fixture(t);
 await f.store.transaction(async db=>{for(let n=0;n<130;n++)await notify(f.service.ctx(db),f.host.id,n===0?'wallet':'matches',`Hareket ${n}`,'Açıklama','PASSPORT','',`page-${n}`);});
 const page=p=>f.store.transaction(db=>notificationPage(f.service.ctx(db,f.host),p));
 const wallet=await page({filter:'wallet',limit:1});assert.equal(wallet.items.length,1);assert.equal(wallet.items[0].title,'Hareket 0');assert.equal(wallet.unreadCount,130);
 const ids=new Set();let result=await page({limit:17});const firstCursor=result.nextCursor;
 do{for(const n of result.items){assert.ok(!ids.has(n.id));ids.add(n.id);}result=result.nextCursor?await page({after:result.nextCursor,limit:17}):null;}while(result);
 assert.equal(ids.size,130);
 await assert.rejects(page({filter:'wallet',after:firstCursor}),e=>e.code==='INVALID_CURSOR');
 await assert.rejects(f.store.transaction(db=>notificationPage(f.service.ctx(db,f.guest),{after:firstCursor})),e=>e.code==='INVALID_CURSOR');
 await f.cmd(f.host,'notification.read');assert.equal((await page({})).unreadCount,0);
 await f.cmd(f.host,'notification.clear',{notificationId:wallet.items[0].id});assert.equal((await page({filter:'wallet'})).items.length,0);
 await f.cmd(f.host,'notification.clear');assert.equal((await page({})).items.length,0);
});
