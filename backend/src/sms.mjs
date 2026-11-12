import { ensure } from './errors.mjs';
export const smsConfigured=config=>!!(config.smsUrl&&config.smsToken||config.twilioAccountSid&&config.twilioAuthToken&&(config.twilioFrom||config.twilioMessagingServiceSid));
export async function sendOtpSms(config,to,code,requestId,request=fetch){
 let response;
 try{
  if(config.smsUrl){response=await request(config.smsUrl,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${config.smsToken}`},body:JSON.stringify({to,code,requestId}),signal:AbortSignal.timeout(8000),redirect:'error'});}
  else{
   ensure(/^AC[a-fA-F0-9]{32}$/.test(config.twilioAccountSid??'')&&config.twilioAuthToken&&(config.twilioFrom||config.twilioMessagingServiceSid),503,'SMS_NOT_CONFIGURED','SMS servisi henüz bağlanmadı.');
   const body=new URLSearchParams({To:to,Body:`Matchora doğrulama kodun: ${code}. Kod 5 dakika geçerli. Kodu kimseyle paylaşma.`});
   if(config.twilioMessagingServiceSid)body.set('MessagingServiceSid',config.twilioMessagingServiceSid);else body.set('From',config.twilioFrom);
   response=await request(`https://api.twilio.com/2010-04-01/Accounts/${config.twilioAccountSid}/Messages.json`,{method:'POST',headers:{Authorization:`Basic ${Buffer.from(`${config.twilioAccountSid}:${config.twilioAuthToken}`).toString('base64')}`,'Content-Type':'application/x-www-form-urlencoded'},body,signal:AbortSignal.timeout(8000),redirect:'error'});
  }
 }catch(error){if(error.code==='SMS_NOT_CONFIGURED')throw error;ensure(false,503,'SMS_UNAVAILABLE','Kod gönderilemedi. Tekrar dene.');}
 ensure(response.ok,503,'SMS_UNAVAILABLE','Kod gönderilemedi. Tekrar dene.');
}
