import { createHash,randomBytes } from 'node:crypto';
import { digest,id,rateLimit,secureEqual } from './auth.mjs';
import { ensure,keys,choice,text } from './errors.mjs';

export function oauthConfigured(config){return !!(config.authPublicKey&&config.userInfoUrl?.endsWith('/auth/v1/user'));}
export async function startOAuth(auth,p,bucket){
 keys(p,['provider']);const provider=choice(p.provider,['apple','google'],'Giriş sağlayıcısı');
 ensure(oauthConfigured(auth.config),503,'AUTH_NOT_CONFIGURED','Sosyal giriş servisi henüz bağlanmadı.');
 const flowId=id(),clientSecret=randomBytes(32).toString('hex'),verifier=randomBytes(48).toString('base64url'),now=auth.clock();
 await auth.store.transaction(async db=>{
  await rateLimit(db,`oauth:${bucket}`,now,15,15*60000);
  await db.run('INSERT INTO oauth_flows(id,provider,client_secret_hash,verifier,expires_at,created_at) VALUES(?,?,?,?,?,?)',[flowId,provider,digest(clientSecret),verifier,now+5*60000,now]);
 });
 const url=new URL(auth.config.userInfoUrl.replace(/user$/,'authorize'));
 url.searchParams.set('provider',provider);
 url.searchParams.set('redirect_to',`matchora://auth/callback?flow=${flowId}`);
 url.searchParams.set('code_challenge',createHash('sha256').update(verifier).digest('base64url'));
 url.searchParams.set('code_challenge_method','s256');
 let targetUrl = url.toString();
 const testHost = url.hostname === 'project.supabase.co' || url.hostname.endsWith('.invalid') || url.hostname.endsWith('.test');
 if (provider === 'google' && typeof auth.request === 'function' && (!testHost || auth.request !== fetch)) {
  try {
   const response = await auth.request(targetUrl, { method: 'GET', redirect: 'manual', signal: AbortSignal.timeout(6000) });
   const location = response.headers.get('location');
   if (location && location.startsWith('https://')) targetUrl = location;
  } catch {}
 }
 // The client secret is returned only over the API, never in the browser URL.
 return{flowId,clientSecret,url:targetUrl,expiresAt:now+5*60000};
}
export async function finishOAuth(auth,p,request=auth.request){
 keys(p,['flowId','clientSecret','code']);text(p.flowId,'Giriş isteği',36,36);text(p.clientSecret,'Giriş doğrulaması',64,64);text(p.code,'Giriş kodu',8,2048);
 const flow=await auth.store.transaction(async db=>{
  const row=await db.get('SELECT * FROM oauth_flows WHERE id=?',[p.flowId]);
  ensure(row&&!row.consumed&&Number(row.expires_at)>auth.clock()&&secureEqual(row.client_secret_hash,digest(p.clientSecret)),401,'OAUTH_EXPIRED','Giriş isteği geçersiz veya süresi dolmuş. Tekrar giriş yap.');
  await db.run('UPDATE oauth_flows SET consumed=1 WHERE id=?',[row.id]);return row;
 });
 let response;
 try{response=await request(auth.config.userInfoUrl.replace(/user$/,'token?grant_type=pkce'),{method:'POST',headers:{apikey:auth.config.authPublicKey,'Content-Type':'application/json'},body:JSON.stringify({auth_code:p.code,code_verifier:flow.verifier}),signal:AbortSignal.timeout(10000),redirect:'error'});}
 catch{ensure(false,503,'AUTH_UNAVAILABLE','Giriş servisine ulaşılamadı. Yeniden giriş yap.');}
 ensure(response.ok,401,'PROVIDER_REJECTED','Giriş doğrulanamadı. Yeniden giriş yap.');
 const data=await response.json();
 // exchange verifies the returned access token using the trusted /user endpoint.
 return auth.exchange({providerToken:data.access_token});
}
