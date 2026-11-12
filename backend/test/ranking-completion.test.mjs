import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { fixture } from './helpers.mjs';

test('ranking filters apply before page limits and a low ranked district athlete is not lost behind a global top five hundred',async t=>{
 const f=await fixture(t);
 await f.store.transaction(async db=>{
  for(let n=0;n<520;n++){
   const id=randomUUID();await db.run("INSERT INTO users(id,subject,role,status,created_at) VALUES(?,?,'user','active',?)",[id,`ranking:${n}`,f.clock()]);
   await db.run('INSERT INTO profiles(user_id,payload,version,updated_at) VALUES(?,?,1,?)',[id,JSON.stringify({...f.passport(`Sporcu ${n}`),city:n===519?'Ankara':'İstanbul',area:n===519?'Çankaya':'Kadıköy',skills:[{sport:'TENNIS',rating:4000-n,verifiedMatches:10}]}),f.clock()]);
   await db.run('INSERT INTO preferences(user_id,privacy,notifications) VALUES(?,?,?)',[id,'{}','{}']);
  }
 });
 const district=await f.service.rankings(null,{sport:'TENNIS',scope:'district',city:'Ankara',district:'Çankaya'});
 assert.equal(district.items.length,1);assert.equal(district.items[0].name,'Sporcu 519');assert.equal(district.items[0].rank,1);
 const national=await f.service.rankings(f.host,{sport:'TENNIS',scope:'national',limit:17});assert.equal(national.total,520);
 const ids=new Set();let page=national;
 do{for(const row of page.items){assert.ok(!ids.has(row.userId));ids.add(row.userId);}page=page.nextCursor?await f.service.rankings(f.host,{sport:'TENNIS',scope:'national',limit:100,after:page.nextCursor}):null;}while(page);
 assert.equal(ids.size,520);
 await assert.rejects(f.service.rankings(f.guest,{sport:'TENNIS',scope:'national',after:national.nextCursor}),e=>e.code==='INVALID_CURSOR');
 await assert.rejects(f.service.rankings(f.host,{sport:'FOOTBALL',scope:'national',after:national.nextCursor}),e=>e.code==='INVALID_CURSOR');
});

test('ranking own position respects privacy and suspended accounts while preserving ranks beyond the current page',async t=>{
 const f=await fixture(t);
 const p=JSON.parse((await f.store.get('SELECT payload FROM profiles WHERE user_id=?',[f.host.id])).payload);p.skills[0].rating=1;await f.store.run('UPDATE profiles SET payload=? WHERE user_id=?',[JSON.stringify(p),f.host.id]);
 const first=await f.service.rankings(f.host,{sport:'FOOTBALL',scope:'national',limit:1});assert.equal(first.items.length,1);assert.equal(first.own.rank,4);assert.equal(first.own.rating,1);
 await f.cmd(f.host,'preferences.save',{privacy:{showInRankings:false}});assert.equal((await f.service.rankings(f.host,{sport:'FOOTBALL',scope:'national'})).own,null);
 await f.store.run("UPDATE users SET status='suspended' WHERE id=?",[f.guest.id]);assert.equal((await f.service.rankings(null,{sport:'FOOTBALL',scope:'national'})).total,2);
});
