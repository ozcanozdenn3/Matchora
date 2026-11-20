import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';

test('all reservations, support replies and reports page past 100 with ownership and cursor isolation', async t => {
  const f=await fixture(t);
  await f.store.transaction(async db=>{
    await db.run('INSERT INTO venues(id,owner_id,payload,created_at) VALUES(?,?,?,?)',['history-venue',f.host.id,JSON.stringify({name:'Geçmiş tesisi'}),f.clock()]);
    for(let i=0;i<205;i++) {
      const suffix=String(i).padStart(3,'0'),id=`old-${suffix}`,created=f.clock()-Math.floor(i/3)*1000;
      await db.run("INSERT INTO venue_slots(id,venue_id,starts_at,ends_at,price_minor,status,payload) VALUES(?,'history-venue',?,?,10001,'booked',?)",[id,created-i*7200000-7200000,created-i*7200000-3600000,JSON.stringify({court:'Kort 1'})]);
      await db.run("INSERT INTO bookings(id,slot_id,user_id,status,amount_minor,split_count,cancellation_deadline,created_at) VALUES(?,?,?,'completed',10001,1,?,?)",[id,id,f.host.id,created,created]);
      await db.run("INSERT INTO venue_payments(id,booking_id,amount_minor,currency,status,created_at) VALUES(?,?,10001,'TRY','paid',?)",[id,id,created]);
      await db.run("INSERT INTO support_requests(id,user_id,category,subject,message,status,created_at) VALUES(?,?,'Teknik sorun',?,'Detaylı açıklama','resolved',?)",[id,f.host.id,`Talep ${i}`,created]);
      await db.run("INSERT INTO audit_log(id,actor_id,action,target_id,metadata,created_at) VALUES(?,?,'admin.support.resolve',?,?,?)",[id,f.admin.id,id,JSON.stringify({response:`Yanıt ${i}`}),created]);
      await db.run("INSERT INTO reports(id,user_id,target_type,target_id,reason,details,status,created_at) VALUES(?,?,'event',?,'İnceleme','Açıklama','resolved',?)",[id,f.host.id,id,created]);
    }
  });
  for(const kind of ['reservations','support','reports']) {
    let cursor,items=[];
    do {
      const page=await f.service.personalHistory(f.host,kind,{limit:29,...(cursor?{after:cursor}:{})});
      items.push(...page.items);cursor=page.nextCursor;
    }while(cursor);
    assert.equal(items.length,205,kind);
    assert.equal(new Set(items.map(item=>item.id)).size,205);
    assert.equal((await f.service.personalHistory(f.guest,kind)).items.length,0);
    const first=await f.service.personalHistory(f.host,kind,{limit:1});
    await assert.rejects(f.service.personalHistory(f.guest,kind,{after:first.nextCursor}),error=>error.code==='INVALID_CURSOR');
    if(kind==='support')assert.ok(items.every(item=>item.response.startsWith('Yanıt ')));
    if(kind==='reservations') {assert.equal(items[0].amountMinor,10001);assert.equal(items[0].bookingStatus,'completed');assert.equal(items[0].status,'PAID');}
  }
  const support=await f.service.personalHistory(f.host,'support',{limit:1});
  await assert.rejects(f.service.personalHistory(f.host,'reports',{after:support.nextCursor}),error=>error.code==='INVALID_CURSOR');
  f.advance(3600001);
  await assert.rejects(f.service.personalHistory(f.host,'support',{after:support.nextCursor}),error=>error.code==='INVALID_CURSOR');
});

test('Supabase supplement is repeatable and PostgreSQL serves extended histories with private cleanup jobs', async t => {
  const { PGlite } = await import('@electric-sql/pglite');
  const { readFile } = await import('node:fs/promises');
  const { PostgresStore } = await import('../src/store.mjs');
  const { personalHistory } = await import('../src/personal-history.mjs');
  const { queueIdentityDeletion,processIdentityDeletions } = await import('../src/identity-deletion.mjs');
  const pg=new PGlite();t.after(()=>pg.close());
  await pg.exec('CREATE ROLE anon; CREATE ROLE authenticated;');
  await pg.exec(await readFile(new URL('../migrations/001_core.sql',import.meta.url),'utf8'));
  const sql=await readFile(new URL('../supabase/07_account_cleanup_history.sql',import.meta.url),'utf8');
  await pg.exec(sql);await pg.exec(sql);
  const query=async(sql,params)=>{const result=await pg.query(sql,params);return {rows:result.rows,rowCount:result.affectedRows};};
  const store=new PostgresStore({query,connect:async()=>({query,release(){}})});
  for(const id of ['owner','other','deleted'])await store.run('INSERT INTO users(id,subject,created_at) VALUES(?,?,0)',[id,id]);
  await store.run('INSERT INTO venues(id,owner_id,payload,created_at) VALUES(?,?,?,0)',['venue','owner',JSON.stringify({name:'Kort'})]);
  for(const [index,id] of ['one','two'].entries()){
    await store.run("INSERT INTO venue_slots(id,venue_id,starts_at,ends_at,price_minor,status,payload) VALUES(?,'venue',?,?,10001,'booked',?)",[id,index*1000,index*1000+100,JSON.stringify({court:id})]);
    await store.run("INSERT INTO bookings(id,slot_id,user_id,status,amount_minor,split_count,cancellation_deadline,created_at) VALUES(?,?,'owner','completed',10001,1,0,10)",[id,id]);
    await store.run("INSERT INTO venue_payments(id,booking_id,amount_minor,currency,status,created_at) VALUES(?,?,10001,'TRY','paid',10)",[id,id]);
    await store.run("INSERT INTO support_requests(id,user_id,category,subject,message,status,created_at) VALUES(?,'owner','Hesap','Talep','Açıklama','resolved',10)",[id]);
    await store.run("INSERT INTO audit_log(id,action,target_id,metadata,created_at) VALUES(?,'admin.support.resolve',?,?,10)",[id,id,JSON.stringify({response:'Yanıt'})]);
    await store.run("INSERT INTO reports(id,user_id,target_type,target_id,reason,details,status,created_at) VALUES(?,'owner','event','event','Gerekçe','Açıklama','open',10)",[id]);
  }
  const ctx={db:store,actor:{id:'owner'},now:1000,config:{secret:'x'.repeat(32)}};
  for(const kind of ['reservations','support','reports']){
    const first=await personalHistory(ctx,kind,{limit:1});
    const second=await personalHistory(ctx,kind,{limit:1,after:first.nextCursor});
    assert.notEqual(first.items[0].id,second.items[0].id);
    assert.equal(second.nextCursor,null);
    assert.equal((await personalHistory({...ctx,actor:{id:'other'}},kind)).items.length,0);
  }
  assert.equal((await personalHistory(ctx,'support')).items[0].response,'Yanıt');
  const config={supabaseUrl:'https://auth.example.invalid',userInfoUrl:'https://auth.example.invalid/auth/v1/user',authAdminKey:'test-key'};
  await store.run("UPDATE users SET status='deleted' WHERE id='deleted'");
  await queueIdentityDeletion({...ctx,config},{id:'deleted',subject:'provider:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'});
  await processIdentityDeletions({store,config,clock:()=>1000},async()=>new Response('{}'));
  assert.equal((await store.get("SELECT status FROM identity_deletions WHERE user_id='deleted'")).status,'completed');
  assert.equal((await pg.query("SELECT relrowsecurity FROM pg_class WHERE oid='public.identity_deletions'::regclass")).rows[0].relrowsecurity,true);
  assert.equal((await pg.query("SELECT count(*)::int AS n FROM information_schema.table_privileges WHERE table_schema='public' AND table_name='identity_deletions' AND grantee IN ('anon','authenticated','PUBLIC')")).rows[0].n,0);
});
