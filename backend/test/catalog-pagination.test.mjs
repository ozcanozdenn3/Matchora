import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { fixture } from './helpers.mjs';
import { createApi } from '../src/server.mjs';

const kinds=['teams','tournaments','communities','venues'];
const fails=code=>error=>error.code===code;
async function seedCatalog(f,count=125){
  const ids=Object.fromEntries(kinds.map(kind=>[kind,[]]));
  await f.store.transaction(async db=>{
    for(let i=0;i<count;i++){
      const city=i%2?'Ankara':'İstanbul',sport=i%2?'TENNIS':'FOOTBALL',stamp=f.clock()-i;
      const payload={name:`Katalog ${i}`,city,area:'Merkez',sport,sports:[sport],archived:i<5};
      for(const kind of kinds){
        const id=randomUUID();if(!payload.archived)ids[kind].push(id);
        if(kind==='teams')await db.run('INSERT INTO teams(id,captain_id,sport,payload,created_at) VALUES(?,?,?,?,?)',[id,f.admin.id,sport,JSON.stringify(payload),stamp]);
        else if(kind==='tournaments')await db.run('INSERT INTO tournaments(id,creator_id,sport,status,capacity,payload,created_at) VALUES(?,?,?,?,?,?,?)',[id,f.admin.id,sport,'open',2,JSON.stringify(payload),stamp]);
        else if(kind==='communities')await db.run('INSERT INTO communities(id,owner_id,access,payload,created_at) VALUES(?,?,?,?,?)',[id,f.admin.id,'INVITE',JSON.stringify(payload),stamp]);
        else await db.run('INSERT INTO venues(id,owner_id,payload,created_at) VALUES(?,?,?,?)',[id,f.admin.id,JSON.stringify(payload),stamp]);
      }
    }
  });
  return ids;
}
async function allPages(f,kind,filters={},actor=null){
  const items=[];let after=null,pages=0;
  do{const page=await f.service.catalog(actor,kind,{...filters,limit:17,...(after?{after}:{})});items.push(...page.items);after=page.nextCursor;assert.ok(++pages<50);}while(after);
  return items;
}

test('all four public catalogs continue beyond bootstrap one hundred without archived rows, duplicates or shape changes',async t=>{
  const f=await fixture(t),expected=await seedCatalog(f),bootstrap=await f.service.bootstrap();
  for(const kind of kinds){
    assert.equal(bootstrap[kind].length,100);assert.deepEqual(bootstrap[kind].map(item=>item.id),expected[kind].slice(0,100));
    const next=await f.service.catalog(null,kind,{after:bootstrap.nextCatalogCursors[kind],limit:100});
    assert.deepEqual(next.items.map(item=>item.id),expected[kind].slice(100));assert.equal(next.nextCursor,null);
    const all=await allPages(f,kind);assert.deepEqual(all.map(item=>item.id),expected[kind]);assert.equal(new Set(all.map(item=>item.id)).size,120);
    assert.deepEqual((await f.service.catalog(null,kind,{limit:100})).items,bootstrap[kind]);
  }
});

test('catalog filters and encrypted cursors bind actor, kind, city, sport and expiry',async t=>{
  const f=await fixture(t);await seedCatalog(f,30);
  const first=await f.service.catalog(f.host,'teams',{city:' İstanbul ',sport:'FOOTBALL',limit:3});
  assert.ok(first.items.every(item=>item.city==='İstanbul'&&item.sport==='FOOTBALL'));
  await f.service.catalog(f.host,'teams',{city:'İSTANBUL',sport:'FOOTBALL',limit:3,after:first.nextCursor});
  for(const [actor,kind,filters] of [[f.guest,'teams',{city:'İstanbul',sport:'FOOTBALL'}],[f.host,'communities',{city:'İstanbul',sport:'FOOTBALL'}],[f.host,'teams',{city:'Ankara',sport:'FOOTBALL'}],[f.host,'teams',{city:'İstanbul',sport:'TENNIS'}]])
    await assert.rejects(f.service.catalog(actor,kind,{...filters,after:first.nextCursor}),fails('INVALID_CURSOR'));
  await assert.rejects(f.service.catalog(f.host,'teams',{after:'tampered'}),fails('INVALID_CURSOR'));
  await assert.rejects(f.service.catalog(null,'__proto__'),fails('INVALID_INPUT'));
  await assert.rejects(f.service.catalog(null,'teams',{limit:101}),fails('INVALID_INPUT'));
  f.advance(3600001);await assert.rejects(f.service.catalog(f.host,'teams',{city:'İstanbul',sport:'FOOTBALL',after:first.nextCursor}),fails('INVALID_CURSOR'));
});

test('filtered catalogs scan past unrelated rows, retain old owned records and carry team public player references',async t=>{
  const f=await fixture(t),expected=await seedCatalog(f,125);
  const oldTeam=randomUUID(),oldCommunity=randomUUID();
  await f.store.run('INSERT INTO teams(id,captain_id,sport,payload,created_at) VALUES(?,?,?,?,?)',[oldTeam,f.host.id,'FOOTBALL','{"name":"Eski takım","area":"Kadıköy"}',f.clock()-10000]);
  await f.store.run('INSERT INTO team_members(team_id,user_id,role,status) VALUES(?,?,?,?)',[oldTeam,f.host.id,'captain','joined']);
  await f.store.run('INSERT INTO communities(id,owner_id,access,payload,created_at) VALUES(?,?,?,?,?)',[oldCommunity,f.host.id,'OPEN','{"name":"Eski ağ","city":"İstanbul","sport":"FOOTBALL"}',f.clock()-10000]);
  const own=await f.service.bootstrap(f.host);
  assert.ok(own.teams.some(item=>item.id===oldTeam));assert.ok(own.communities.some(item=>item.id===oldCommunity));
  const cityTeams=await allPages(f,'teams',{city:'İstanbul',sport:'FOOTBALL'});
  assert.ok(cityTeams.some(item=>item.id===oldTeam),'Legacy teams use their captain passport city');
  assert.equal(cityTeams.length,61);
  const first=await f.service.catalog(f.host,'teams',{limit:10});assert.equal(first.players[0].id,f.admin.id);
  assert.equal(first.players[0].email,undefined);assert.equal(first.players[0].phone,undefined);
  const tail=await f.service.catalog(f.host,'teams',{after:own.nextCatalogCursors.teams,limit:100});
  assert.ok(tail.items.some(item=>item.id===oldTeam));assert.equal(tail.items.length,expected.teams.length-100+1);
});

test('venue catalog excludes inactive or demoted owners and rechecks lifecycle state on later pages',async t=>{
  const f=await fixture(t),expected=await seedCatalog(f,12);
  const revoked=randomUUID();
  await f.store.run('INSERT INTO venues(id,owner_id,payload,created_at) VALUES(?,?,?,?)',[revoked,f.guest.id,'{"name":"Yetkisiz tesis"}',f.clock()]);
  const first=await f.service.catalog(null,'venues',{limit:2});assert.ok(!first.items.some(item=>item.id===revoked));
  const hidden=expected.venues[2],saved=await f.store.get('SELECT payload FROM venues WHERE id=?',[hidden]);
  await f.store.run('UPDATE venues SET payload=? WHERE id=?',[JSON.stringify({...JSON.parse(saved.payload),archived:true}),hidden]);
  const next=await f.service.catalog(null,'venues',{after:first.nextCursor,limit:100});
  assert.ok(!next.items.some(item=>item.id===hidden));
  await f.store.run("UPDATE users SET status='suspended' WHERE id=?",[f.admin.id]);
  assert.equal((await f.service.catalog(null,'venues')).items.length,0);
});

test('catalog HTTP routes allow guest discovery and reject supplied invalid sessions and unknown catalogs',async t=>{
  const f=await fixture(t);await seedCatalog(f,8);
  const server=createApi(f.service,f.auth);await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));t.after(()=>new Promise(resolve=>server.close(resolve)));
  const base=`http://127.0.0.1:${server.address().port}`;
  const response=await fetch(base+'/v1/catalog/teams?limit=2');assert.equal(response.status,200);assert.equal((await response.json()).items.length,2);
  assert.equal((await fetch(base+'/v1/catalog/unknown')).status,400);
  assert.equal((await fetch(base+'/v1/catalog/teams',{headers:{Authorization:'Bearer invalid'}})).status,401);
  assert.equal((await fetch(base+'/v1/notifications')).status,401);
});
