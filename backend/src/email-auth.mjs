import { digest, rateLimit } from './auth.mjs';
import { ensure, keys, text } from './errors.mjs';
import { oauthConfigured } from './oauth.mjs';

function emailAddress(value) {
 const email=text(value,'E-posta',3,254).toLowerCase();
 ensure(!/[\u0000-\u001f\u007f]/.test(email)&&/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email),400,'INVALID_EMAIL','Geçerli bir e-posta adresi gir.');return email;
}
async function supabaseRequest(auth,path,payload) {
 ensure(oauthConfigured(auth.config),503,'AUTH_NOT_CONFIGURED','E-posta giriş servisi henüz bağlanmadı.');
 let response;try{response=await auth.request(auth.config.userInfoUrl.replace(/user$/,path),{
  method:'POST',headers:{apikey:auth.config.authPublicKey,'Content-Type':'application/json'},body:JSON.stringify(payload),signal:AbortSignal.timeout(10000),redirect:'error'
 });}catch{ensure(false,503,'AUTH_UNAVAILABLE','Giriş servisine ulaşılamadı. Tekrar dene.');}
 if(response.status===429)ensure(false,429,'RATE_LIMITED','Yeni kod istemeden önce biraz bekle.');
 ensure(response.ok,path==='verify'?401:503,path==='verify'?'EMAIL_CODE_REJECTED':'EMAIL_UNAVAILABLE',path==='verify'?'Kod geçersiz veya süresi dolmuş. Yeni kod isteyebilirsin.':'E-posta gönderilemedi. Tekrar dene.');
 return response;
}
export async function startEmailOtp(auth,p,bucket) {
 keys(p,['email']);const email=emailAddress(p.email);
 ensure(oauthConfigured(auth.config),503,'AUTH_NOT_CONFIGURED','E-posta giriş servisi henüz bağlanmadı.');
 await auth.store.transaction(async db=>{
  await rateLimit(db,`email-start:${bucket}`,auth.clock(),15,15*60000);
  await rateLimit(db,`email-address:${digest(email)}`,auth.clock(),1,60000);
 });
 await supabaseRequest(auth,'otp',{email,create_user:true});
 // Supabase owns code generation, expiry and one-time consumption. Never send a test code to mobile.
 return{sent:true,resendAfterSeconds:60};
}
export async function verifyEmailOtp(auth,p,bucket) {
 keys(p,['email','code']);const email=emailAddress(p.email),code=text(p.code,'Giriş kodu',6,6);
 ensure(/^\d{6}$/.test(code),400,'INVALID_INPUT','E-postana gelen altı haneli kodu gir.');
 ensure(oauthConfigured(auth.config),503,'AUTH_NOT_CONFIGURED','E-posta giriş servisi henüz bağlanmadı.');
 // Attempts commit before contacting the provider, including rejected verifications.
 await auth.store.transaction(async db=>{
  await rateLimit(db,`email-verify:${bucket}`,auth.clock(),40,15*60000);
  await rateLimit(db,`email-verify-address:${digest(email)}`,auth.clock(),5,10*60000);
 });
 const response=await supabaseRequest(auth,'verify',{email,token:code,type:'email'}),data=await response.json();
 ensure(typeof data.access_token==='string',401,'EMAIL_CODE_REJECTED','E-posta doğrulanamadı.');
 return auth.exchange({providerToken:data.access_token},{email});
}
