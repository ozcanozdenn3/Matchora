import { randomBytes } from 'node:crypto';
import { ensure } from './errors.mjs';
export function configFromEnv(env = process.env) {
  const production = env.MATCHORA_ENV === 'production';
  const config = {
    production, host: env.HOST ?? '127.0.0.1', port: Number(env.PORT ?? 8787),
    databaseUrl: env.DATABASE_URL, databasePath: env.SQLITE_PATH ?? './var/matchora.sqlite',
    supabaseUrl:env.SUPABASE_URL,storageServiceKey:env.SUPABASE_SERVICE_ROLE_KEY,storageBucket:env.SUPABASE_STORAGE_BUCKET??'matchora-private',
    storagePath: env.STORAGE_PATH ?? './var/uploads', publicUrl: env.PUBLIC_API_URL,
    userInfoUrl: env.AUTH_USERINFO_URL, authPublicKey: env.AUTH_PUBLIC_KEY,
    authAdminKey: env.SUPABASE_AUTH_ADMIN_KEY || env.SUPABASE_SERVICE_ROLE_KEY,
    twilioAccountSid:env.TWILIO_ACCOUNT_SID,twilioAuthToken:env.TWILIO_AUTH_TOKEN,twilioFrom:env.TWILIO_FROM,twilioMessagingServiceSid:env.TWILIO_MESSAGING_SERVICE_SID,
    smsUrl: env.SMS_GATEWAY_URL, smsToken: env.SMS_GATEWAY_TOKEN,
    pushUrl: env.PUSH_GATEWAY_URL, pushToken: env.PUSH_GATEWAY_TOKEN,
    apnsKeyId:env.APNS_KEY_ID,apnsTeamId:env.APNS_TEAM_ID,apnsPrivateKey:env.APNS_PRIVATE_KEY,apnsBundleId:env.APNS_BUNDLE_ID,apnsSandbox:env.APNS_SANDBOX==='true',
    fcmProjectId:env.FCM_PROJECT_ID,fcmClientEmail:env.FCM_CLIENT_EMAIL,fcmPrivateKey:env.FCM_PRIVATE_KEY,
    secret: env.CHECKIN_SECRET ?? (production ? '' : randomBytes(32).toString('hex')),
    billingSecret: env.BILLING_WEBHOOK_SECRET,
    revenueCatAppleKey:env.REVENUECAT_APPLE_PUBLIC_KEY,revenueCatGoogleKey:env.REVENUECAT_GOOGLE_PUBLIC_KEY,revenueCatSecretKey:env.REVENUECAT_SECRET_KEY,
    revenueCatAppleProducts:env.REVENUECAT_APPLE_PRODUCT_IDS?JSON.parse(env.REVENUECAT_APPLE_PRODUCT_IDS):{},
    revenueCatGoogleProducts:env.REVENUECAT_GOOGLE_PRODUCT_IDS?JSON.parse(env.REVENUECAT_GOOGLE_PRODUCT_IDS):{},
    revenueCatAuthorization: env.REVENUECAT_WEBHOOK_AUTHORIZATION,
    revenueCatProducts: env.REVENUECAT_PRODUCT_MAP ? JSON.parse(env.REVENUECAT_PRODUCT_MAP) : {},
    devAuth: !production && env.DEV_AUTH_ENABLED === 'true',
    admins: new Set((env.ADMIN_SUBJECTS ?? '').split(',').filter(Boolean)),
    corsOrigins: new Set((env.CORS_ORIGINS ?? '').split(',').filter(Boolean)),
    privacyUrl: env.PRIVACY_URL, termsUrl: env.TERMS_URL, supportEmail: env.SUPPORT_EMAIL,
    termsIosUrl: env.TERMS_IOS_URL, termsAndroidUrl: env.TERMS_ANDROID_URL,
    privacyVersion: env.PRIVACY_VERSION, termsVersion: env.TERMS_VERSION,
    guidelinesUrl: env.COMMUNITY_GUIDELINES_URL, deletionUrl: env.ACCOUNT_DELETION_URL,
    deletionDelay: Number(env.DELETION_DELAY_MS ?? 7 * 86400000)
  };
  ensure(Number.isInteger(config.port) && config.port > 0 && config.port <= 65535);
  ensure(Number.isSafeInteger(config.deletionDelay)&&config.deletionDelay>=0&&config.deletionDelay<=30*86400000,503,'INVALID_CONFIGURATION','Hesap silme bekleme süresi geçersiz.');
  if (production) {
    for (const [name, value] of Object.entries({ DATABASE_URL: config.databaseUrl, AUTH_USERINFO_URL: config.userInfoUrl,
      CHECKIN_SECRET: config.secret?.length >= 32, BILLING_WEBHOOK_SECRET: config.billingSecret?.length >= 32,
      PUBLIC_API_URL: config.publicUrl, PRIVACY_URL: config.privacyUrl, TERMS_URL: config.termsUrl, PRIVACY_VERSION: config.privacyVersion, TERMS_VERSION: config.termsVersion, SUPPORT_EMAIL: config.supportEmail })) {
      ensure(value, 503, 'CONFIGURATION_REQUIRED', `${name} yapılandırılmalı.`);
    }
    ensure(/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(config.supportEmail),503,'INVALID_CONFIGURATION','Destek e-posta adresi geçersiz.');
    for (const value of [config.supabaseUrl, config.userInfoUrl, config.publicUrl, config.privacyUrl, config.termsUrl, config.guidelinesUrl,config.deletionUrl,config.smsUrl, config.pushUrl].filter(Boolean)) {
      const endpoint=new URL(value);
      ensure(endpoint.protocol==='https:'&&!endpoint.username&&!endpoint.password&&!['localhost','127.0.0.1','[::1]','10.0.2.2'].includes(endpoint.hostname),503,'HTTPS_REQUIRED','Üretim servisleri kimlik bilgisi içermeyen genel HTTPS adresleri kullanmalı.');
    }
  }
  return config;
}
