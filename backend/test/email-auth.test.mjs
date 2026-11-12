import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { startEmailOtp, verifyEmailOtp } from '../src/email-auth.mjs';
import { createApi } from '../src/server.mjs';
const fails=code=>error=>error.code===code;
function configure(f){f.config.userInfoUrl='https://project.supabase.co/auth/v1/user';f.config.authPublicKey='public-publishable-key';}
const providerToken='supabase-email-access-token-0123456789';

test('email login needs a configured real provider and rejects malformed addresses without sending',async t=>{
 const f=await fixture(t);let calls=0;f.auth.request=async()=>{calls++;throw new Error('unexpected');};
 await assert.rejects(startEmailOtp(f.auth,{email:'person@example.invalid'},'ip'),fails('AUTH_NOT_CONFIGURED'));
 configure(f);for(const email of ['5321234567','person@','person\u0000@example.invalid'])await assert.rejects(startEmailOtp(f.auth,{email},'ip'),fails('INVALID_EMAIL'));
 assert.equal(calls,0);
});

test('email code delivery uses Supabase signup OTP, never returns a demo code and persists resend throttling',async t=>{
 const f=await fixture(t);configure(f);const calls=[];f.auth.request=async(url,options)=>{calls.push({url,options});return Response.json({});};
 const result=await startEmailOtp(f.auth,{email:' Person@Example.invalid '},'ip');
 assert.deepEqual(result,{sent:true,resendAfterSeconds:60});assert.equal(calls[0].url,'https://project.supabase.co/auth/v1/otp');
 assert.deepEqual(JSON.parse(calls[0].options.body),{email:'person@example.invalid',create_user:true});assert.equal(calls[0].options.headers.apikey,'public-publishable-key');
 await assert.rejects(startEmailOtp(f.auth,{email:'person@example.invalid'},'another-ip'),fails('RATE_LIMITED'));assert.equal(calls.length,1);
 f.advance(60001);await startEmailOtp(f.auth,{email:'person@example.invalid'},'ip');assert.equal(calls.length,2);
});

test('verified email creates a Matchora session only from the trusted confirmed identity and the same code cannot replay',async t=>{
 const f=await fixture(t);configure(f);let consumed=false;f.auth.request=async(url,options)=>{
  if(url.endsWith('/verify')){assert.deepEqual(JSON.parse(options.body),{email:'person@example.invalid',token:'123456',type:'email'});if(consumed)return Response.json({}, {status:400});consumed=true;return Response.json({access_token:providerToken});}
  assert.equal(options.headers.Authorization,`Bearer ${providerToken}`);return Response.json({id:'email-provider-person',email:'person@example.invalid',email_confirmed_at:'confirmed'});
 };
 const session=await verifyEmailOtp(f.auth,{email:'PERSON@example.invalid',code:'123456'},'ip');assert.equal((await f.auth.actor(session.accessToken)).id,session.userId);assert.equal(session.profileHints.email,'person@example.invalid');
 const before=(await f.store.get('SELECT COUNT(*) AS n FROM sessions')).n;
 await assert.rejects(verifyEmailOtp(f.auth,{email:'person@example.invalid',code:'123456'},'ip'),fails('EMAIL_CODE_REJECTED'));
 assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM sessions')).n,before);
});

test('a mismatched or unconfirmed upstream email cannot authenticate the requested address',async t=>{
 const f=await fixture(t);configure(f);let identity={id:'wrong-identity',email:'other@example.invalid',email_confirmed_at:'confirmed'};
 f.auth.request=async url=>url.endsWith('/verify')?Response.json({access_token:providerToken}):Response.json(identity);
 const before=(await f.store.get('SELECT COUNT(*) AS n FROM users')).n;
 await assert.rejects(verifyEmailOtp(f.auth,{email:'person@example.invalid',code:'123456'},'ip'),fails('PROVIDER_REJECTED'));
 identity={id:'unconfirmed-identity',email:'person@example.invalid'};
 await assert.rejects(verifyEmailOtp(f.auth,{email:'person@example.invalid',code:'123456'},'ip'),fails('PROVIDER_REJECTED'));
 assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM users')).n,before);
});

test('rejected email-code attempts commit their rate limits and never create a session',async t=>{
 const f=await fixture(t);configure(f);let calls=0;f.auth.request=async()=>{calls++;return Response.json({}, {status:400});};
 for(let attempt=0;attempt<5;attempt++)await assert.rejects(verifyEmailOtp(f.auth,{email:'person@example.invalid',code:'000000'},'ip'),fails('EMAIL_CODE_REJECTED'));
 await assert.rejects(verifyEmailOtp(f.auth,{email:'person@example.invalid',code:'000000'},'ip'),fails('RATE_LIMITED'));assert.equal(calls,5);
});

test('email provider outages stay errors instead of development account fallbacks',async t=>{
 const f=await fixture(t);configure(f);f.auth.request=async()=>{throw new Error('network down');};
 await assert.rejects(startEmailOtp(f.auth,{email:'person@example.invalid'},'ip'),fails('AUTH_UNAVAILABLE'));
 f.advance(60001);f.auth.request=async()=>Response.json({}, {status:503});
 await assert.rejects(startEmailOtp(f.auth,{email:'person@example.invalid'},'ip'),fails('EMAIL_UNAVAILABLE'));
});

test('HTTP email login and signup return only a server session after OTP verification',async t=>{
 const f=await fixture(t);configure(f);f.auth.request=async url=>url.endsWith('/otp')?Response.json({}):url.endsWith('/verify')?Response.json({access_token:providerToken}):Response.json({id:'http-email-user',email:'http@example.invalid',email_confirmed_at:'confirmed'});
 const server=createApi(f.service,f.auth);await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));t.after(()=>new Promise(resolve=>server.close(resolve)));
 const base=`http://127.0.0.1:${server.address().port}`,post=(path,payload)=>fetch(base+path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
 assert.equal((await post('/v1/auth/email/start',{email:'invalid'})).status,400);
 const started=await(await post('/v1/auth/email/start',{email:'http@example.invalid'})).json();assert.equal(started.sent,true);assert.equal(started.accessToken,undefined);
 const verified=await post('/v1/auth/email/verify',{email:'http@example.invalid',code:'123456'});assert.equal(verified.status,200);const session=await verified.json();assert.equal(session.profileHints.email,'http@example.invalid');
 const snapshot=await(await fetch(base+'/v1/bootstrap',{headers:{Authorization:`Bearer ${session.accessToken}`}})).json();assert.equal(snapshot.profile,null);assert.equal(snapshot.userId,session.userId);
});
