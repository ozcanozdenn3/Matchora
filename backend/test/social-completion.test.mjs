import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { fixture } from './helpers.mjs';
const fails=code=>error=>error.code===code;
async function direct(f){await f.cmd(f.guest,'preferences.save',{privacy:{dmPolicy:'Herkes'}});return(await f.cmd(f.host,'chat.direct',{userId:f.guest.id})).result.id;}
async function send(f,thread,text,actor=f.host,extra={}){return(await f.cmd(actor,'chat.send',{threadId:thread,text,...extra})).result.id;}
async function messages(f,thread,actor=f.host){return(await f.service.messages(actor,thread)).items;}
async function photo(f,{owner=f.host,status='approved',city='İstanbul',area='Kadıköy',point=null,age=0,caption='Saha anısı'}={}){
 const upload=randomUUID(),id=randomUUID();
 await f.store.run('INSERT INTO uploads(id,owner_id,mime,size,object_key,status,created_at) VALUES(?,?,?,?,?,?,?)',[upload,owner.id,'image/jpeg',1,upload,'ready',f.clock()]);
 await f.store.run('INSERT INTO memories(id,author_id,upload_id,status,payload,created_at) VALUES(?,?,?,?,?,?)',[id,owner.id,upload,status,JSON.stringify({version:1,caption,city,area,sport:'FOOTBALL',venue:'Saha',sceneTheme:'NIGHT_TURF',approximateLocation:point}),f.clock()-age]);
 return{id,upload};
}

test('conversation deletion is personal and new same-millisecond messages return without old history',async t=>{
 const f=await fixture(t),thread=await direct(f),first=await send(f,thread,'Eski mesaj');
 await f.cmd(f.host,'chat.delete',{threadId:thread});
 assert.ok(!(await f.service.bootstrap(f.host)).threads.some(v=>v.id===thread));
 assert.equal((await messages(f,thread,f.guest))[0].id,first);
 const next=await send(f,thread,'Yeni mesaj',f.guest);
 const own=await f.service.bootstrap(f.host),chat=own.threads.find(v=>v.id===thread);
 assert.deepEqual(chat.messages.map(v=>v.id),[next]);assert.equal(chat.unread,1);
 assert.deepEqual((await messages(f,thread,f.guest)).map(v=>v.id),[first,next]);
});

test('clear is not a false read receipt and sequence receipts distinguish messages in the same millisecond',async t=>{
 const f=await fixture(t),thread=await direct(f),first=await send(f,thread,'Bir');
 await f.cmd(f.guest,'chat.clear',{threadId:thread});
 assert.equal((await messages(f,thread))[0].readBy,0);
 const second=await send(f,thread,'İki');
 assert.equal((await f.service.bootstrap(f.guest)).threads.find(v=>v.id===thread).unread,1);
 await f.cmd(f.guest,'chat.read',{threadId:thread});
 assert.equal((await messages(f,thread)).find(v=>v.id===second).readBy,1);
 await send(f,thread,'Üç');
 assert.equal((await f.service.bootstrap(f.guest)).threads.find(v=>v.id===thread).unread,1);
 assert.ok(!(await messages(f,thread,f.guest)).some(v=>v.id===first));
});

test('deleting and blocking is atomic and cleanup remains available when the peer blocked first',async t=>{
 const f=await fixture(t),thread=await direct(f);await send(f,thread,'Merhaba');
 await f.cmd(f.guest,'user.block',{userId:f.host.id,blocked:true});
 await f.cmd(f.host,'chat.delete',{threadId:thread,blockUser:true});
 assert.ok((await f.service.bootstrap(f.host)).blockedUserIds.includes(f.guest.id));
 await assert.rejects(send(f,thread,'Ulaşamaz',f.guest),fails('BLOCKED'));
 await assert.rejects(f.cmd(f.other,'chat.delete',{threadId:thread}),fails('FORBIDDEN'));
});

test('message hide is personal, edits are versioned and deleting removes content and attachments for everyone',async t=>{
 const f=await fixture(t),thread=await direct(f),id=await send(f,thread,'İlk metin');
 await assert.rejects(f.cmd(f.guest,'message.edit',{messageId:id,expectedVersion:1,text:'Başkasının metni'}),fails('FORBIDDEN'));
 await f.cmd(f.host,'message.edit',{messageId:id,expectedVersion:1,text:'Düzeltilen metin'});
 await assert.rejects(f.cmd(f.host,'message.edit',{messageId:id,expectedVersion:1,text:'Eski sürüm'}),fails('VERSION_CONFLICT'));
 assert.equal((await messages(f,thread,f.guest))[0].text,'Düzeltilen metin');
 assert.equal((await messages(f,thread,f.guest))[0].edited,true);
 await assert.rejects(f.cmd(f.guest,'message.delete',{messageId:id,expectedVersion:2}),fails('FORBIDDEN'));
 await f.cmd(f.host,'message.delete',{messageId:id,expectedVersion:2});
 const deleted=(await messages(f,thread,f.guest))[0];assert.equal(deleted.deleted,true);assert.equal(deleted.text,'Bu mesaj silindi.');assert.equal(deleted.uploadId,null);
 await f.cmd(f.guest,'message.hide',{messageId:id});
 assert.equal((await messages(f,thread,f.guest)).length,0);assert.equal((await messages(f,thread)).length,1);
});

test('message edits close after fifteen minutes and replies cannot target another thread or cleared history',async t=>{
 const f=await fixture(t),thread=await direct(f),id=await send(f,thread,'Yanıt hedefi');
 const reply=await send(f,thread,'Yanıt',f.guest,{replyTo:id});
 assert.equal((await messages(f,thread)).find(v=>v.id===reply).reply.text,'Yanıt hedefi');
 const event=(await f.cmd(f.host,'event.create',f.event())).result.id,other=(await f.store.get('SELECT id FROM threads WHERE event_id=?',[event])).id;
 await assert.rejects(send(f,other,'Yanlış oda',f.host,{replyTo:id}),fails('INVALID_REPLY'));
 f.advance(16*60000);
 await assert.rejects(f.cmd(f.host,'message.edit',{messageId:id,expectedVersion:1,text:'Geç'}),fails('EDIT_WINDOW_CLOSED'));
 await f.cmd(f.guest,'chat.clear',{threadId:thread});
 await assert.rejects(send(f,thread,'Temizlenmiş',f.guest,{replyTo:id}),fails('NOT_FOUND'));
});

test('forwarding cannot revive a cleared or deleted private image',async t=>{
 const f=await fixture(t),thread=await direct(f),{upload}=await photo(f,{status:'pending'});
 const message=await send(f,thread,'Fotoğraf',f.host,{uploadId:upload});
 await f.cmd(f.guest,'chat.clear',{threadId:thread});
 await assert.rejects(send(f,thread,'Geri getir',f.guest,{uploadId:upload,forwardId:message}),fails('NOT_FOUND'));
 await f.cmd(f.host,'message.delete',{messageId:message,expectedVersion:1});
 assert.equal((await f.store.get('SELECT upload_id FROM messages WHERE id=?',[message])).upload_id,null);
});

test('global top-liked includes older posts beyond bootstrap fifty and unique votes never multiply',async t=>{
 const f=await fixture(t),old=await photo(f,{age:100000});
 for(let i=0;i<55;i++)await photo(f,{age:i});
 assert.ok(!(await f.service.bootstrap(f.host)).memories.some(v=>v.id===old.id));
 await f.cmd(f.host,'memory.like',{memoryId:old.id,liked:true});await f.cmd(f.host,'memory.like',{memoryId:old.id,liked:true});
 await f.cmd(f.guest,'memory.like',{memoryId:old.id,liked:true});
 let feed=await f.service.memories(f.host,{mode:'top',limit:10});assert.equal(feed.items[0].id,old.id);assert.equal(feed.items[0].likes,2);assert.equal(feed.items[0].likedByMe,true);
 await f.cmd(f.host,'memory.like',{memoryId:old.id,liked:false});feed=await f.service.memories(f.host,{mode:'top'});assert.equal(feed.items[0].likes,1);
});

test('random feeds keep a stable seed across pages, change on refresh and validate scoped encrypted cursors',async t=>{
 const f=await fixture(t);for(let i=0;i<30;i++)await photo(f,{age:i});
 const one=await f.service.memories(f.host,{mode:'random',seed:'first',limit:10});
 assert.deepEqual(one.items.map(v=>v.id),(await f.service.memories(f.host,{mode:'random',seed:'first',limit:10})).items.map(v=>v.id));
 const two=await f.service.memories(f.host,{mode:'random',seed:'first',limit:10,after:one.nextCursor});
 assert.equal(new Set([...one.items,...two.items].map(v=>v.id)).size,20);
 assert.notDeepEqual(one.items.map(v=>v.id),(await f.service.memories(f.host,{mode:'random',seed:'second',limit:10})).items.map(v=>v.id));
 await assert.rejects(f.service.memories(f.host,{mode:'top',seed:'first',after:one.nextCursor}),fails('INVALID_CURSOR'));
 await assert.rejects(f.service.memories(f.guest,{mode:'random',seed:'first',after:one.nextCursor}),fails('INVALID_CURSOR'));
 await assert.rejects(f.service.memories(f.host,{mode:'random',seed:'first',after:'tampered'}),fails('INVALID_CURSOR'));
 f.advance(3600001);await assert.rejects(f.service.memories(f.host,{mode:'random',seed:'first',after:one.nextCursor}),fails('INVALID_CURSOR'));
});

test('nearby uses actual great-circle coordinates and region-only posts never acquire a fake distance',async t=>{
 const f=await fixture(t),near=await photo(f,{point:{latitude:41,longitude:29},age:100}),far=await photo(f,{point:{latitude:39.93,longitude:32.85}}),unknown=await photo(f,{city:'İstanbul',area:'Kadıköy'});
 const feed=await f.service.memories(f.host,{mode:'nearby',city:'İstanbul',district:'Kadıköy',latitude:41,longitude:29});
 assert.equal(feed.items[0].id,near.id);assert.equal(feed.items[0].distanceKm,0);assert.ok(feed.items.find(v=>v.id===far.id).distanceKm>300);assert.equal(feed.items.at(-1).id,unknown.id);assert.equal(feed.items.at(-1).distanceKm,null);
 assert.equal((await f.service.memories(f.host,{mode:'nearby',city:'İstanbul',district:'Kadıköy'})).locationBasis,'region');
 const western=await f.service.memories(f.host,{mode:'nearby',latitude:-33.9,longitude:-151.2});assert.equal(western.items.length,3);
 await assert.rejects(f.service.memories(f.host,{mode:'nearby',latitude:100,longitude:29}),fails('INVALID_LOCATION'));
});

test('feed eligibility respects moderation, blocks, inactive authors and own pending/rejected management',async t=>{
 const f=await fixture(t),mine=await photo(f,{status:'pending'}),rejected=await photo(f,{status:'rejected'}),other=await photo(f,{owner:f.guest}),inactive=await photo(f,{owner:f.other});
 await f.cmd(f.admin,'admin.user.status',{userId:f.other.id,status:'suspended',reason:'Test için hesap geçici kapatılıyor.'});
 assert.deepEqual((await f.service.memories(null,{mode:'top'})).items.map(v=>v.id),[other.id]);
 assert.deepEqual(new Set((await f.service.memories(f.host,{mode:'mine'})).items.map(v=>v.id)),new Set([mine.id,rejected.id]));
 await assert.rejects(f.service.memories(null,{mode:'mine'}),fails('UNAUTHENTICATED'));
 await f.cmd(f.host,'user.block',{userId:f.guest.id,blocked:true});assert.equal((await f.service.memories(f.host,{mode:'top'})).items.length,0);
 assert.ok(!(await f.service.memories(null,{mode:'top'})).items.some(v=>v.id===inactive.id));
});

test('editing own memories preserves identity, changes the region index and returns to moderation',async t=>{
 const f=await fixture(t),memory=await photo(f);
 const payload={memoryId:memory.id,expectedVersion:1,caption:'Yeni not',venue:'Yeni saha',city:'Ankara',area:'Çankaya',approximateLocation:{latitude:39.93,longitude:32.85}};
 await assert.rejects(f.cmd(f.guest,'memory.update',payload),fails('FORBIDDEN'));
 await f.cmd(f.host,'memory.update',payload);
 assert.equal((await f.service.memories(null,{mode:'newest'})).items.length,0);
 const own=(await f.service.memories(f.host,{mode:'mine'})).items[0];assert.equal(own.id,memory.id);assert.equal(own.version,2);assert.equal(own.status,'pending');assert.equal(own.city,'Ankara');
 assert.equal((await f.store.get('SELECT city FROM memory_locations WHERE memory_id=?',[memory.id])).city,'ankara');
 await assert.rejects(f.cmd(f.host,'memory.update',payload),fails('VERSION_CONFLICT'));
});

test('memory create and edit validate the raw point and persist only coarse public coordinates',async t=>{
 const f=await fixture(t),source=await photo(f);
 const payload={caption:'Yeni anı',sport:'FOOTBALL',venue:'Saha',area:'Kadıköy',city:'İstanbul',uploadId:source.upload,approximateLocation:{latitude:90.004,longitude:29}};
 await assert.rejects(f.cmd(f.host,'memory.create',payload),fails('INVALID_LOCATION'));
 const id=(await f.cmd(f.host,'memory.create',{...payload,approximateLocation:{latitude:40.987654,longitude:29.012345}})).result.id;
 assert.deepEqual(JSON.parse((await f.store.get('SELECT payload FROM memories WHERE id=?',[id])).payload).approximateLocation,{latitude:40.99,longitude:29.01});
 await f.cmd(f.host,'memory.update',{memoryId:id,expectedVersion:1,approximateLocation:{latitude:39.934567,longitude:32.856789}});
 const point={latitude:39.93,longitude:32.86};
 assert.deepEqual((await f.service.memories(f.host,{mode:'mine'})).items.find(m=>m.id===id).approximateLocation,point);
 assert.deepEqual((await f.service.bootstrap(f.host)).memories.find(m=>m.id===id).approximateLocation,point);
});

test('legacy precise memory payloads do not leak and changing region without a new point clears old distance data',async t=>{
 const f=await fixture(t),memory=await photo(f,{point:{latitude:40.987654,longitude:29.012345}});
 const point={latitude:40.99,longitude:29.01};
 assert.deepEqual((await f.service.memories(null,{mode:'newest'})).items[0].approximateLocation,point);
 assert.deepEqual((await f.service.bootstrap()).memories[0].approximateLocation,point);
 await f.cmd(f.host,'memory.update',{memoryId:memory.id,expectedVersion:1,city:'Ankara',area:'Çankaya'});
 assert.equal((await f.service.memories(f.host,{mode:'mine'})).items[0].approximateLocation,null);
 const index=await f.store.get('SELECT latitude,longitude FROM memory_locations WHERE memory_id=?',[memory.id]);
 assert.equal(index.latitude,null);assert.equal(index.longitude,null);
});
