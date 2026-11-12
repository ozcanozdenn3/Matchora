import { smsConfigured } from '../src/sms.mjs';
import { oauthConfigured } from '../src/oauth.mjs';
import { storeConfiguration } from '../src/store-sync.mjs';
import { configFromEnv } from '../src/config.mjs';
const env=process.env,checks=[];
const add=(id,passed,detail)=>checks.push({id,status:passed?'ready':'missing',detail});
const required=['DATABASE_URL','AUTH_USERINFO_URL','CHECKIN_SECRET','BILLING_WEBHOOK_SECRET','PUBLIC_API_URL','PRIVACY_URL','TERMS_URL','PRIVACY_VERSION','TERMS_VERSION','SUPPORT_EMAIL'];
for(const key of required)add(key,!!env[key],`${key} sunucu ortamında tanımlanmalı.`);
try{configFromEnv({...env,MATCHORA_ENV:'production'});add('production-config',true,'Üretim sunucu yapılandırması doğrulandı.');}
catch(error){add('production-config',false,error.message);}
const config=configFromEnv({...env,MATCHORA_ENV:'development'});
add('sms',smsConfigured(config),'Twilio Messaging Service veya SMS köprüsünün kimlik bilgileri gerekiyor.');
add('oauth',oauthConfigured(config),'Supabase Auth /auth/v1/user ve public auth anahtarı gerekiyor.');
add('private-storage',!!(config.supabaseUrl&&config.storageServiceKey),'Özel Supabase Storage URL ve yalnız sunucuda tutulan service-role anahtarı gerekiyor.');
const store=storeConfiguration(config);
add('store-sdk-keys',!!(store.keys.ios&&store.keys.android),'Apple ve Android RevenueCat public SDK anahtarları gerekiyor.');
add('store-product-ids',Object.keys(store.products.ios).length===6&&Object.keys(store.products.android).length===6,'Dört token paketi ve iki Plus ürününün platform ID eşlemeleri gerekiyor.');
add('store-sync',store.sync,'Sunucu RevenueCat secret API anahtarı gerekiyor.');
add('billing-webhook',!!env.REVENUECAT_WEBHOOK_AUTHORIZATION,'RevenueCat webhook özel yetkisi ve mağaza ürün eşlemesi gerekiyor.');
add('ios-push',!!(env.APNS_KEY_ID&&env.APNS_TEAM_ID&&env.APNS_PRIVATE_KEY&&env.APNS_BUNDLE_ID||env.PUSH_GATEWAY_URL&&env.PUSH_GATEWAY_TOKEN),'APNs sunucu anahtarı veya push köprüsü gerekiyor.');
add('android-push',!!(env.FCM_PROJECT_ID&&env.FCM_CLIENT_EMAIL&&env.FCM_PRIVATE_KEY||env.PUSH_GATEWAY_URL&&env.PUSH_GATEWAY_TOKEN),'FCM sunucu anahtarı veya push köprüsü gerekiyor.');
const ready=checks.every(item=>item.status==='ready');
console.log(JSON.stringify({serverConfigurationReady:ready,scope:'Yalnız yapılandırma varlığı ve sunucu doğrulaması. Canlı bağlantı, mobil SDK, mağaza kabulü veya güvenlik denetimi yerine geçmez.',checks},null,2));
process.exitCode=ready?0:1;
