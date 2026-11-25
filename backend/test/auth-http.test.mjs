import test from 'node:test';
import assert from 'node:assert/strict';
import { createSign, generateKeyPairSync } from 'node:crypto';
import { fixture } from './helpers.mjs';
import { createApi } from '../src/server.mjs';
import { configFromEnv } from '../src/config.mjs';
import { sanitizeImage } from '../src/media.mjs';

function signFirebaseJwt(privateKey, kid, payload) {
 const header = Buffer.from(JSON.stringify({ alg: 'RS256', typ: 'JWT', kid })).toString('base64url');
 const claims = Buffer.from(JSON.stringify(payload)).toString('base64url');
 const body = `${header}.${claims}`;
 const signer = createSign('RSA-SHA256');
 signer.update(body);
 signer.end();
 return `${body}.${signer.sign(privateKey).toString('base64url')}`;
}

test('OTP attempts persist and challenge is single use',async t=>{const f=await fixture(t),challenge=await f.auth.startOtp({phone:'5321234567'},'test');for(let i=0;i<5;i++)await assert.rejects(f.auth.verifyOtp({challengeId:challenge.challengeId,code:challenge.developmentCode==='000000'?'000001':'000000'}));await assert.rejects(f.auth.verifyOtp({challengeId:challenge.challengeId,code:challenge.developmentCode}));const next=await f.auth.startOtp({phone:'5321234567'},'other');const session=await f.auth.verifyOtp({challengeId:next.challengeId,code:next.developmentCode});assert.ok(session.accessToken);await assert.rejects(f.auth.verifyOtp({challengeId:next.challengeId,code:next.developmentCode}));});
test('rotating refresh token detects reuse and revokes the whole family',async t=>{const f=await fixture(t),old=f.sessions.host;const fresh=await f.auth.refresh({refreshToken:old.refreshToken});await assert.rejects(f.auth.actor(old.accessToken));assert.ok(await f.auth.actor(fresh.accessToken));await assert.rejects(f.auth.refresh({refreshToken:old.refreshToken}));await assert.rejects(f.auth.actor(fresh.accessToken));});
test('production configuration fails closed without external credentials and URLs',()=>{assert.throws(()=>configFromEnv({MATCHORA_ENV:'production',DEV_AUTH_ENABLED:'true'}),error=>error.code==='CONFIGURATION_REQUIRED');});
test('non-images and oversized payloads are rejected before storage',()=>{assert.throws(()=>sanitizeImage(Buffer.from('<html>'+ 'x'.repeat(100)),'image/jpeg'));assert.throws(()=>sanitizeImage(Buffer.alloc(6*1024*1024),'image/png'),error=>error.code==='IMAGE_SIZE');});
test('HTTP endpoints enforce bearer auth, media type, CORS and structured errors',async t=>{const f=await fixture(t),server=createApi(f.service,f.auth);await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));t.after(()=>new Promise(resolve=>server.close(resolve)));const base=`http://127.0.0.1:${server.address().port}`;assert.equal((await fetch(base+'/health')).status,200);assert.equal((await fetch(base+'/v1/bootstrap')).status,200);assert.equal((await fetch(base+'/v1/commands/event.create',{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'})).status,401);assert.equal((await fetch(base+'/health',{headers:{Origin:'https://attacker.invalid'}})).status,403);const response=await fetch(base+'/v1/commands/event.create',{method:'POST',headers:{Authorization:`Bearer ${f.sessions.host.accessToken}`,'Content-Type':'application/json'},body:'{}'});assert.equal(response.status,400);const error=await response.json();assert.equal(error.error.code,'IDEMPOTENCY_REQUIRED');assert.ok(error.error.requestId);const contract=await(await fetch(base+'/openapi.json')).json();assert.ok(contract.paths['/v1/commands/result.dispute']);});

test('contact OTP is bound to the signed-in user and cannot authenticate another account',async t=>{const f=await fixture(t),code=await f.auth.startOtp({phone:'5329876543'},'contact',f.host);await assert.rejects(f.auth.verifyContact(f.guest,{challengeId:code.challengeId,code:code.developmentCode}));await assert.rejects(f.auth.verifyOtp({challengeId:code.challengeId,code:code.developmentCode}));await f.auth.verifyContact(f.host,{challengeId:code.challengeId,code:code.developmentCode});assert.ok((await f.service.bootstrap(f.host)).profile.verifications.includes('BASIC'));assert.equal((await f.service.bootstrap(f.host)).profile.phone,'+905329876543');});
test('production URLs reject embedded credentials and support contact must be valid',()=>{const base={MATCHORA_ENV:'production',DATABASE_URL:'postgresql://db.example.invalid/matchora',AUTH_USERINFO_URL:'https://auth.example.invalid/user',CHECKIN_SECRET:'a'.repeat(32),BILLING_WEBHOOK_SECRET:'b'.repeat(32),PUBLIC_API_URL:'https://api.example.invalid',PRIVACY_URL:'https://example.invalid/privacy',TERMS_URL:'https://example.invalid/terms',PRIVACY_VERSION:'1',TERMS_VERSION:'1',SUPPORT_EMAIL:'support@example.invalid'};assert.equal(configFromEnv(base).devAuth,false);assert.throws(()=>configFromEnv({...base,PRIVACY_URL:'https://private:secret@example.invalid/privacy'}),error=>error.code==='HTTPS_REQUIRED');assert.throws(()=>configFromEnv({...base,SUPPORT_EMAIL:'invalid'}),error=>error.code==='INVALID_CONFIGURATION');assert.throws(()=>configFromEnv({...base,PUBLIC_API_URL:'https://localhost'}),error=>error.code==='HTTPS_REQUIRED');});

test('email OTP login routes are disabled and Firebase/phone sign-in allows any user while restricting admin role', async t => {
 const f = await fixture(t);
 f.config.admins = new Set(['phone:+905350705672']);
 const { publicKey, privateKey } = generateKeyPairSync('rsa', { modulusLength: 2048 });
 const certPem = publicKey.export({ type: 'spki', format: 'pem' }).toString();
 f.auth.request = async () => Response.json({ kid1: certPem }, { headers: { 'cache-control': 'max-age=3600' } });

 const server = createApi(f.service, f.auth);
 await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
 t.after(() => new Promise(resolve => server.close(resolve)));
 const base = `http://127.0.0.1:${server.address().port}`;
 const post = (path, payload) => fetch(base + path, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });

 assert.equal((await post('/v1/auth/email/start', { email: 'user@example.invalid' })).status, 404);
 assert.equal((await post('/v1/auth/email/verify', { email: 'user@example.invalid', code: '123456' })).status, 404);
 assert.equal((await f.service.configuration()).auth.email, false);

 const nowSec = Math.floor(f.clock() / 1000);
 const userJwt = signFirebaseJwt(privateKey, 'kid1', {
  aud: 'matchora-c5ddc',
  iss: 'https://securetoken.google.com/matchora-c5ddc',
  sub: 'firebase-normal-user',
  phone_number: '+905551112233',
  iat: nowSec,
  exp: nowSec + 3600
 });
 const userRes = await post('/v1/auth/id-token', { provider: 'firebase', idToken: userJwt });
 assert.equal(userRes.status, 200);
 const userSession = await userRes.json();
 assert.equal(userSession.profileHints.phone, '+905551112233');
 const userBoot = await (await fetch(base + '/v1/bootstrap', { headers: { Authorization: `Bearer ${userSession.accessToken}` } })).json();
 assert.equal(userBoot.role, 'user');
 assert.equal(userBoot.profile, null);

 const adminJwt = signFirebaseJwt(privateKey, 'kid1', {
  aud: 'matchora-c5ddc',
  iss: 'https://securetoken.google.com/matchora-c5ddc',
  sub: 'firebase-admin-user',
  phone_number: '+905350705672',
  iat: nowSec,
  exp: nowSec + 3600
 });
 const adminRes = await post('/v1/auth/id-token', { provider: 'firebase', idToken: adminJwt });
 assert.equal(adminRes.status, 200);
 const adminSession = await adminRes.json();
 const adminBoot = await (await fetch(base + '/v1/bootstrap', { headers: { Authorization: `Bearer ${adminSession.accessToken}` } })).json();
 assert.equal(adminBoot.role, 'admin');

 const contactJwt = signFirebaseJwt(privateKey, 'kid1', {
  aud: 'matchora-c5ddc',
  iss: 'https://securetoken.google.com/matchora-c5ddc',
  sub: 'firebase-contact-user',
  phone_number: '+447911123456',
  iat: nowSec,
  exp: nowSec + 3600
 });
 const contactRes = await fetch(base + '/v1/auth/contact/verify-id-token', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${f.sessions.host.accessToken}` },
  body: JSON.stringify({ idToken: contactJwt })
 });
 assert.equal(contactRes.status, 200);
 const contactBody = await contactRes.json();
 assert.equal(contactBody.snapshot.profile.phone, '+447911123456');

 assert.throws(
  () => configFromEnv({ REVENUECAT_PRODUCT_MAP: '{invalid-json' }),
  err => err.code === 'INVALID_CONFIGURATION'
 );
});
