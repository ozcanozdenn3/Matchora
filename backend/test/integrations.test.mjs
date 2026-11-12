import test from 'node:test';
import assert from 'node:assert/strict';
import sharp from 'sharp';
import { createHash } from 'node:crypto';
import { fixture } from './helpers.mjs';
import { startOAuth,finishOAuth } from '../src/oauth.mjs';
import { SupabaseMediaStore,cleanMedia } from '../src/storage.mjs';
import { sendOtpSms } from '../src/sms.mjs';

test('OAuth binds the callback to a private client secret and PKCE verifier; replay fails',async t=>{
 const f=await fixture(t);f.config.userInfoUrl='https://project.supabase.co/auth/v1/user';f.config.authPublicKey='public-key';
 const calls=[];f.auth.request=async(url,options)=>{calls.push({url,options});if(url.includes('/token?'))return Response.json({access_token:'verified-token-0123456789'});return Response.json({id:'trusted-external-user',email:'user@example.invalid',email_confirmed_at:'today'});};
 const flow=await startOAuth(f.auth,{provider:'apple'},'test'),url=new URL(flow.url);
 assert.equal(url.searchParams.get('provider'),'apple');assert.ok(!flow.url.includes(flow.clientSecret));
 await assert.rejects(finishOAuth(f.auth,{flowId:flow.flowId,clientSecret:'a'.repeat(64),code:'auth-code-123'}),e=>e.code==='OAUTH_EXPIRED');
 const session=await finishOAuth(f.auth,{flowId:flow.flowId,clientSecret:flow.clientSecret,code:'auth-code-123'});
 const body=JSON.parse(calls[0].options.body);assert.equal(body.auth_code,'auth-code-123');
 assert.equal(createHash('sha256').update(body.code_verifier).digest('base64url'),url.searchParams.get('code_challenge'));
 assert.equal((await f.auth.actor(session.accessToken)).id,session.userId);
 await assert.rejects(finishOAuth(f.auth,{flowId:flow.flowId,clientSecret:flow.clientSecret,code:'auth-code-123'}),e=>e.code==='OAUTH_EXPIRED');
});
test('expired OAuth requests and unavailable providers cannot create sessions',async t=>{
 const f=await fixture(t);await assert.rejects(startOAuth(f.auth,{provider:'google'},'test'),e=>e.code==='AUTH_NOT_CONFIGURED');
 f.config.userInfoUrl='https://project.supabase.co/auth/v1/user';f.config.authPublicKey='public';
 const flow=await startOAuth(f.auth,{provider:'google'},'test');f.advance(301000);
 await assert.rejects(finishOAuth(f.auth,{flowId:flow.flowId,clientSecret:flow.clientSecret,code:'code-012345'}),e=>e.code==='OAUTH_EXPIRED');
});
test('private Supabase storage authenticates every operation and never exposes a public object URL',async()=>{
 const requests=[],bytes=Buffer.from([255,216,1,2]);
 const storage=new SupabaseMediaStore('https://project.supabase.co','server-secret','matchora-private',async(url,options)=>{requests.push({url,options});return options.method==='POST'||options.method==='DELETE'?Response.json({}):new Response(bytes);});
 const key='12345678-1234-1234-1234-123456789012';await storage.put(key,bytes);assert.deepEqual(await storage.get(key),bytes);await storage.remove(key);
 assert.ok(requests.every(r=>r.options.headers.Authorization==='Bearer server-secret'&&!r.url.includes('/public/')));
 assert.equal(requests[0].options.headers['x-upsert'],'false');assert.equal(requests[1].url,`https://project.supabase.co/storage/v1/object/authenticated/matchora-private/${key}`);
 assert.deepEqual(JSON.parse(requests[2].options.body),{prefixes:[key]});await assert.rejects(storage.get('../private'));
});
test('image uploads are decoded and re-encoded; unreferenced media cleanup retries storage failures',async t=>{
 const f=await fixture(t),image=await sharp({create:{width:8,height:8,channels:3,background:'red'}}).png().toBuffer();
 const upload=await f.service.upload(f.host,image,'image/png');assert.equal(upload.mime,'image/jpeg');
 const data=await f.service.mediaFile(f.host,upload.id);assert.equal((await sharp(data.bytes).metadata()).format,'jpeg');
 f.advance(86400001);const remove=f.service.media.remove.bind(f.service.media);f.service.media.remove=async()=>{throw new Error('offline');};await cleanMedia(f.service);
 assert.equal((await f.store.get('SELECT status FROM uploads WHERE id=?',[upload.id])).status,'deleted');assert.equal((await f.store.get('SELECT attempts FROM storage_deletions')).attempts,1);
 f.service.media.remove=remove;f.advance(61000);await cleanMedia(f.service);assert.equal(await f.store.get('SELECT * FROM storage_deletions'),undefined);
});
test('direct Twilio SMS uses server credentials and sending failures consume the challenge',async t=>{
 let request;const config={twilioAccountSid:'AC'+'a'.repeat(32),twilioAuthToken:'private',twilioFrom:'+15005550006'};
 await sendOtpSms(config,'+905321234567','123456','id',async(url,options)=>{request={url,options};return Response.json({sid:'SM'});});
 assert.equal(request.options.body.get('To'),'+905321234567');assert.ok(request.options.body.get('Body').includes('123456'));assert.ok(request.options.headers.Authorization.startsWith('Basic '));
 const f=await fixture(t);Object.assign(f.config,config);f.auth.request=async()=>new Response('',{status:503});
 await assert.rejects(f.auth.startOtp({phone:'5321234567'},'test'),e=>e.code==='SMS_UNAVAILABLE');assert.equal((await f.store.get('SELECT consumed FROM auth_challenges')).consumed,1);
});
test('message pagination keeps a stable order for equal timestamps and respects ownership',async t=>{
 const f=await fixture(t);await f.cmd(f.guest,'preferences.save',{privacy:{dmPolicy:'Herkes'}});const thread=(await f.cmd(f.host,'chat.direct',{userId:f.guest.id})).result.id;
 for(let n=0;n<7;n++)await f.cmd(f.host,'chat.send',{threadId:thread,text:`Mesaj ${n}`});
 const ids=[];let before;do{const page=await f.service.messages(f.guest,thread,before,3);assert.ok(page.items.every(m=>m.senderName==='Ev sahibi'&&!m.isMine&&m.createdAt===f.clock()));ids.push(...page.items.map(m=>m.id));before=page.nextCursor;}while(before);
 assert.equal(ids.length,7);assert.equal(new Set(ids).size,7);await assert.rejects(f.service.messages(f.other,thread),e=>e.code==='FORBIDDEN');
});


test('publishing a memory and sharing its photo is atomic and idempotent',async t=>{
 const f=await fixture(t);await f.cmd(f.guest,'preferences.save',{privacy:{dmPolicy:'Herkes'}});
 const threadId=(await f.cmd(f.host,'chat.direct',{userId:f.guest.id})).result.id;
 const image=await sharp({create:{width:8,height:8,channels:3,background:'blue'}}).jpeg().toBuffer();
 const upload=await f.service.upload(f.host,image,'image/jpeg');
 const payload={caption:'Saha anısı',sport:'FOOTBALL',venue:'Kadıköy saha',city:'İstanbul',area:'Kadıköy',uploadId:upload.id,shareThreadId:threadId};
 const first=await f.cmd(f.host,'memory.create',payload,'memory-and-chat-1');
 const repeat=await f.cmd(f.host,'memory.create',payload,'memory-and-chat-1');
 assert.equal(repeat.result.id,first.result.id);assert.equal(repeat.result.messageId,first.result.messageId);
 assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM memories')).n,1);
 const messages=await f.service.messages(f.guest,threadId);assert.equal(messages.items.length,1);assert.equal(messages.items[0].uploadId,upload.id);
 assert.equal((await f.service.bootstrap(f.guest)).memories.length,0,'public feed still requires moderation');
 await f.cmd(f.host,'memory.create',{...payload,shareThreadId:null},'memory-feed-only-1');
 assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM memories')).n,2);
 assert.equal((await f.service.messages(f.guest,threadId)).items.length,1);
});
test('memory plus chat rejects foreign threads and disabled chat without publishing partial data',async t=>{
 const f=await fixture(t);await f.cmd(f.guest,'preferences.save',{privacy:{dmPolicy:'Herkes'}});
 const threadId=(await f.cmd(f.host,'chat.direct',{userId:f.guest.id})).result.id;
 const image=await sharp({create:{width:8,height:8,channels:3,background:'blue'}}).jpeg().toBuffer();
 const upload=await f.service.upload(f.other,image,'image/jpeg');
 const payload={caption:'Özel fotoğraf',sport:'FOOTBALL',venue:'Saha',city:'İstanbul',area:'Kadıköy',uploadId:upload.id,shareThreadId:threadId};
 await assert.rejects(f.cmd(f.other,'memory.create',payload),e=>e.code==='FORBIDDEN');
 assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM memories')).n,0);
 await f.cmd(f.admin,'admin.feature.set',{name:'chat',enabled:false});
 await assert.rejects(f.cmd(f.other,'memory.create',payload),e=>e.code==='FEATURE_UNAVAILABLE');
 assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM memories')).n,0);
 assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM messages')).n,0);
});
