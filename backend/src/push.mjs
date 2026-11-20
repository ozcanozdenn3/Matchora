import { createPrivateKey,sign } from 'node:crypto';
const b64=value=>Buffer.from(JSON.stringify(value)).toString('base64url');
export function apnsToken(config,now=Date.now()){
 const unsigned=`${b64({alg:'ES256',kid:config.apnsKeyId})}.${b64({iss:config.apnsTeamId,iat:Math.floor(now/1000)})}`;
 return `${unsigned}.${sign('sha256',Buffer.from(unsigned),{key:createPrivateKey(config.apnsPrivateKey.replace(/\\n/g,'\n')),dsaEncoding:'ieee-p1363'}).toString('base64url')}`;
}
export function apnsPayload(notification){return{aps:{alert:{title:notification.title,body:notification.body},sound:'default'},destination:notification.destination,targetId:notification.targetId,notificationId:notification.notificationId};}
export async function sendApns(config,device,notification,jobId){
 if(!/^[a-fA-F0-9]{64,200}$/.test(device.token))return{invalid:true};
 const h2Spec=['node','http2'].join(':');
 const {connect}=await import(h2Spec);
 const session=connect(config.apnsSandbox?'https://api.sandbox.push.apple.com':'https://api.push.apple.com');
 try{return await new Promise((resolve,reject)=>{
  let request;const timer=setTimeout(()=>{request?.close();reject(new Error('APNS_TIMEOUT'));},8000);
  session.once('error',error=>{clearTimeout(timer);reject(error);});
  request=session.request({':method':'POST',':path':`/3/device/${device.token}`,authorization:`bearer ${apnsToken(config)}`,'apns-topic':config.apnsBundleId,'apns-push-type':'alert','apns-priority':'10','apns-id':jobId});
  let status=0,body='';request.on('response',headers=>{status=Number(headers[':status']);});request.setEncoding('utf8');request.on('data',chunk=>{if(body.length<2048)body+=chunk;});request.once('error',error=>{clearTimeout(timer);reject(error);});
  request.on('end',()=>{clearTimeout(timer);if(status===200)resolve({sent:true});else if(status===410)resolve({invalid:true});else reject(new Error(`APNS_${status}`));});request.end(JSON.stringify(apnsPayload(notification)));
 });}finally{session.close();}
}
let cachedGoogleToken=null;
async function googleToken(config,fetcher){
 if(cachedGoogleToken?.email===config.fcmClientEmail&&cachedGoogleToken.expires>Date.now()+60000)return cachedGoogleToken.token;
 const now=Math.floor(Date.now()/1000),unsigned=`${b64({alg:'RS256',typ:'JWT'})}.${b64({iss:config.fcmClientEmail,scope:'https://www.googleapis.com/auth/firebase.messaging',aud:'https://oauth2.googleapis.com/token',iat:now,exp:now+3600})}`;
 const assertion=`${unsigned}.${sign('RSA-SHA256',Buffer.from(unsigned),config.fcmPrivateKey.replace(/\\n/g,'\n')).toString('base64url')}`;
 const response=await fetcher('https://oauth2.googleapis.com/token',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer',assertion}),signal:AbortSignal.timeout(8000),redirect:'error'});
 if(!response.ok)throw new Error('FCM_AUTH_REJECTED');const value=await response.json();if(typeof value.access_token!=='string')throw new Error('FCM_AUTH_INVALID');
 cachedGoogleToken={email:config.fcmClientEmail,token:value.access_token,expires:Date.now()+Math.min(Number(value.expires_in)||3000,3600)*1000};return value.access_token;
}
export async function sendFcm(config,device,notification,fetcher=fetch){
 const token=await googleToken(config,fetcher);
 const response=await fetcher(`https://fcm.googleapis.com/v1/projects/${encodeURIComponent(config.fcmProjectId)}/messages:send`,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({message:{token:device.token,notification:{title:notification.title,body:notification.body},data:{destination:notification.destination??'ACCOUNT',targetId:notification.targetId??'',notificationId:notification.notificationId??''},android:{notification:{channel_id:'matchora_activity'}}}}),signal:AbortSignal.timeout(8000),redirect:'error'});
 if(response.ok)return{sent:true};const error=await response.json().catch(()=>({}));if(error.error?.details?.some(d=>d.errorCode==='UNREGISTERED'))return{invalid:true};throw new Error(`FCM_${response.status}`);
}
export const nativePushConfigured=config=>!!(config.apnsKeyId&&config.apnsTeamId&&config.apnsPrivateKey&&config.apnsBundleId||config.fcmProjectId&&config.fcmClientEmail&&config.fcmPrivateKey);
