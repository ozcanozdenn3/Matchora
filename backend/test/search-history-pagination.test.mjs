import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { PGlite } from '@electric-sql/pglite';
import { fixture } from './helpers.mjs';
import { searchPlayers } from '../src/player-search.mjs';
import { personalHistory } from '../src/personal-history.mjs';
import { PostgresStore } from '../src/store.mjs';

const rejects = code => error => error.code===code;

test('directory searches all profiles with Turkish filters, literal wildcard names, blocks and public privacy',async t=>{
 const f=await fixture(t);
 const search = (filters={},actor=f.host) => searchPlayers(f.service.ctx(f.store,actor),filters);
 await f.store.transaction(async db=>{
  for(let index=0;index<205;index++) {
   const id=`directory-${String(index).padStart(3,'0')}`;
   const athlete={...f.passport(`Sporcu ${index}`),verifiedMatches:0,reliability:93,skills:[{sport:'TENNIS',rating:1000}]};
   if(index===0)Object.assign(athlete,{name:'ÇAĞRI IŞIK',city:'İZMİR',area:'KARŞIYAKA',email:'private@example.com',phone:'+905551112233'});
   if(index===1)athlete.name='Ayşe %_\\ Özel';
   await db.run('INSERT INTO users(id,subject,created_at) VALUES(?,?,?)',[id,id,f.clock()]);
   await db.run('INSERT INTO profiles(user_id,payload,updated_at) VALUES(?,?,?)',[id,JSON.stringify(athlete),f.clock()-index]);
   await db.run('INSERT INTO preferences(user_id,privacy) VALUES(?,?)',[id,JSON.stringify({showReliability:index!==0})]);
  }
 });
 let result=await search({q:'cagri isik',city:'izmir',district:'karsiyaka',sport:'TENNIS'});
 assert.equal(result.items[0].id,'directory-000');assert.equal(result.items[0].reliability,0);
 assert.equal(result.items[0].reliabilityVisible,false);
 assert.equal(result.items[0].email,undefined);assert.equal(result.items[0].phone,undefined);
 assert.equal((await search({q:'%_\\'})).items[0].id,'directory-001');
 assert.equal((await search({q:'Sporcu 204',sport:'TENNIS'})).items[0].id,'directory-204');
 assert.equal((await search({q:'cagri',sport:'FOOTBALL'})).items.length,0);
 await f.store.run('INSERT INTO blocks(user_id,target_id,created_at) VALUES(?,?,?)',['directory-000',f.host.id,f.clock()]);
 assert.equal((await search({q:'cagri'})).items.length,0);
 await f.store.run("UPDATE users SET status='suspended' WHERE id='directory-001'");
 assert.equal((await search({q:'%_\\'})).items.length,0);
 await assert.rejects(searchPlayers(f.service.ctx(f.store),{}),rejects('UNAUTHENTICATED'));
});

test('directory cursors bind actor and filters, paginate ties once and reject changed access',async t=>{
 const f=await fixture(t);
 const search=(filters={},actor=f.host)=>searchPlayers(f.service.ctx(f.store,actor),filters);
 const first=await search({limit:1});assert.equal(first.items.length,1);assert.ok(first.nextCursor);
 await assert.rejects(search({limit:1,after:first.nextCursor},f.guest),rejects('INVALID_CURSOR'));
 await assert.rejects(search({q:'farklı',after:first.nextCursor}),rejects('INVALID_CURSOR'));
 await assert.rejects(search({after:first.nextCursor+'broken'}),rejects('INVALID_CURSOR'));
 const second=await search({limit:1,after:first.nextCursor});assert.notEqual(second.items[0].id,first.items[0].id);
 await f.store.run('INSERT INTO blocks(user_id,target_id,created_at) VALUES(?,?,?)',[f.host.id,second.items[0].id,f.clock()]);
 const refreshedSecond=await search({limit:1,after:first.nextCursor});assert.ok(refreshedSecond.items.every(p=>p.id!==second.items[0].id));
 f.advance(3600001);await assert.rejects(search({after:first.nextCursor}),rejects('INVALID_CURSOR'));
});

test('personal history loads beyond bootstrap limits without duplicates or another user records',async t=>{
 const f=await fixture(t);
 await f.store.transaction(async db=>{
  for(let index=0;index<205;index++) {
   const id=`history-${String(index).padStart(3,'0')}`,created=f.clock()-Math.floor(index/3)*1000;
   await db.run(`INSERT INTO events(id,host_id,sport,status,capacity,starts_at,ends_at,commitment,city,district,payload,created_at) VALUES(?,?,?,'VERIFIED',2,?,?,0,'İstanbul','Kadıköy',?,?)`,[id,f.host.id,'FOOTBALL',created-3600000,created,JSON.stringify({title:`Maç ${index}`,mode:'COMPETITIVE'}),created]);
   await db.run("INSERT INTO event_members(event_id,user_id,status,side,checked_in_at,joined_at) VALUES(?,?,'joined','home',?,?)",[id,f.host.id,created,created]);
   await db.run("INSERT INTO results(event_id,submitted_by,payload,status,dispute_until,created_at) VALUES(?,?,?,'verified',?,?)",[id,f.host.id,JSON.stringify({homeScore:2,awayScore:1}),created,created]);
   await db.run('INSERT INTO rating_history(id,user_id,event_id,sport,delta,rating,created_at) VALUES(?,?,?,?,1,1001,?)',[`rating-${id}`,f.host.id,id,'FOOTBALL',created]);
   await db.run('INSERT INTO reliability_history(id,user_id,event_id,delta,reason,created_at) VALUES(?,?,?,1,?,?)',[`reliability-${id}`,f.host.id,id,'Katılım',created]);
   await db.run("INSERT INTO wallet_entries(id,user_id,kind,amount,ref,dedupe,description,created_at) VALUES(?,?,'BONUS',1,?,?,?,?)",[`wallet-${id}`,f.host.id,id,id,'Katılım ödülü',created]);
  }
 });
 const history=(kind,filters={},actor=f.host)=>personalHistory(f.service.ctx(f.store,actor),kind,filters);
 for(const kind of ['matches','ratings','reliability','wallet']) {
  let after,items=[];
  do {const page=await history(kind,{limit:37,...(after?{after}:{})});items.push(...page.items);after=page.nextCursor;}while(after);
  assert.equal(items.length,kind==='wallet'?206:205,kind);
  assert.equal(new Set(items.map(item=>item.id)).size,items.length);
  assert.ok(items.every(item=>item.user_id==null||item.user_id===f.host.id));
 }
 const first=await history('matches',{limit:3});assert.ok(first.nextCursor);
 await assert.rejects(history('matches',{after:first.nextCursor},f.guest),rejects('INVALID_CURSOR'));
 await assert.rejects(history('ratings',{after:first.nextCursor}),rejects('INVALID_CURSOR'));
 await assert.rejects(history('matches',{sport:'TENNIS',after:first.nextCursor}),rejects('INVALID_CURSOR'));
 assert.equal((await history('matches',{},f.guest)).items.length,0);
 assert.equal((await history('matches',{sport:'TENNIS'})).items.length,0);
 assert.equal(first.items[0].won,true);assert.equal(first.items[0].mmrDelta,1);
 assert.equal((await history('reliability',{limit:1})).items[0].detail.startsWith('Maç '),true);
 await assert.rejects(personalHistory(f.service.ctx(f.store),'wallet'),rejects('UNAUTHENTICATED'));
});

test('PostgreSQL supports Turkish directory queries, authenticated history and repeatable pagination indexes',async t=>{
 const pg=new PGlite();t.after(()=>pg.close());
 await pg.exec(await readFile(new URL('../migrations/001_core.sql',import.meta.url),'utf8'));
 const migration=await readFile(new URL('../migrations/012_discovery_history.sql',import.meta.url),'utf8');
 await pg.exec(migration);await pg.exec(migration);
 const store=new PostgresStore({query:async(sql,params)=>{const result=await pg.query(sql,params);return{rows:result.rows,rowCount:result.affectedRows};}});
 for(const [id,name] of [['requester','İsteyen'],['found','ÇAĞRI IŞIK']]) {
  await store.run('INSERT INTO users(id,subject,created_at) VALUES(?,?,1)',[id,id]);
  await store.run('INSERT INTO profiles(user_id,payload,updated_at) VALUES(?,?,1)',[id,JSON.stringify({name,city:'İZMİR',area:'KARŞIYAKA',skills:[{sport:'TENNIS',rating:1000}],reliability:100})]);
  await store.run("INSERT INTO preferences(user_id) VALUES(?)",[id]);
 }
 const ctx={db:store,actor:{id:'requester'},config:{secret:'x'.repeat(32)},now:100};
 const result=await searchPlayers(ctx,{q:'çağrı ışık',city:'izmir',sport:'TENNIS'});
 assert.equal(result.items[0].id,'found');
 assert.deepEqual((await personalHistory(ctx,'wallet')).items,[]);
});
