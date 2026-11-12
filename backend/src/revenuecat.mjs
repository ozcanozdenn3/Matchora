import { digest,secureEqual,sign } from './auth.mjs';
import { ensure } from './errors.mjs';
import { PRODUCTS,billingWebhook } from './billing.mjs';
import { audit } from './model.mjs';
/** RevenueCat authenticates the delivery using the configured private Authorization header. */
export async function revenueCatWebhook(ctx,raw,headers){
 ensure(ctx.config.revenueCatAuthorization&&secureEqual(headers.authorization,ctx.config.revenueCatAuthorization),401,'WEBHOOK_UNAUTHENTICATED','Webhook doğrulanamadı.');
 let payload;try{payload=JSON.parse(raw);}catch{ensure(false);}
 const e=payload.event;ensure(e&&typeof e.id==='string'&&typeof e.type==='string',400,'INVALID_WEBHOOK','Olay bilgisi eksik.');
 if(e.type==='TEST')return{test:true};
 ensure(!ctx.config.production||e.environment==='PRODUCTION',400,'SANDBOX_EVENT','Üretimde sandbox olayı kabul edilmez.');
 const seen=await ctx.db.get('SELECT payload_hash FROM webhook_events WHERE id=?',[`revenuecat:${e.id}`]);
 const hash=digest(raw);if(seen){ensure(seen.payload_hash===hash,409,'IDEMPOTENCY_CONFLICT','Olay farklı içerikle tekrarlandı.');return{duplicate:true};}
 if(e.type==='TRANSFER'){const targets=Array.isArray(e.transferred_to)?e.transferred_to:[];for(const userId of targets){const user=await ctx.db.get('SELECT id FROM users WHERE id=?',[userId]);if(!user)continue;await ctx.db.run('INSERT INTO support_requests(id,user_id,category,subject,message,status,created_at) VALUES(?,?,?,?,?,?,?)',[`rc-transfer:${e.id}:${userId}`,userId,'Ödeme','Mağaza hesabı transferi incelemesi',JSON.stringify({from:e.transferred_from??[],to:targets}),'open',ctx.now]);}await ctx.db.run('INSERT INTO webhook_events(id,payload_hash,created_at) VALUES(?,?,?)',[`revenuecat:${e.id}`,hash,ctx.now]);return{processed:true,reconciliationRequired:true};}
 const productId=ctx.config.revenueCatProducts?.[e.product_id]??e.product_id,product=PRODUCTS[productId];
 ensure(product,400,'UNKNOWN_PRODUCT','Ürün eşlemesi bulunamadı.');
 const userId=e.app_user_id;ensure(typeof userId==='string'&&!userId.startsWith('$RCAnonymousID'),400,'ACCOUNT_REQUIRED','Mağaza satın alımı Matchora kullanıcı ID ile yapılmalı.');
 let type=null;
 if(['INITIAL_PURCHASE','RENEWAL','NON_RENEWING_PURCHASE'].includes(e.type))type=e.type==='RENEWAL'?'renewal':'purchase';
 if(e.type==='EXPIRATION')type='expire';
 if(e.type==='CANCELLATION'&&!product.subscription)type='refund';
 // A subscription cancellation or refund does not necessarily end access immediately.
 // Entitlements remain active until an authoritative expiry, never until a guessed date.
 if(type){
  const normalized=Buffer.from(JSON.stringify({eventId:`rc-normalized:${e.id}`,type,userId,transactionId:String(e.transaction_id??e.original_transaction_id),originalTransactionId:String(e.original_transaction_id??e.transaction_id),productId,platform:e.store==='APP_STORE'?'apple':e.store==='PLAY_STORE'?'google':'unsupported',expiresAt:e.expiration_at_ms,...(Number.isSafeInteger(e.purchased_at_ms)?{purchasedAt:e.purchased_at_ms}:{})}));
  await billingWebhook(ctx,normalized,{'x-matchora-timestamp':String(ctx.now),'x-matchora-signature':sign(ctx.config.billingSecret,`${ctx.now}.${normalized}`)});
 }else if(['CANCELLATION','UNCANCELLATION','BILLING_ISSUE','SUBSCRIPTION_PAUSED','PRODUCT_CHANGE','SUBSCRIPTION_EXTENDED','REFUND_REVERSED','TRANSFER'].includes(e.type)){
  await audit(ctx,'billing.reconciliation.required',e.id,{type:e.type,userId,productId});
  // Retain a durable operator task; unsupported lifecycle changes are not silently acknowledged and lost.
  await ctx.db.run('INSERT INTO support_requests(id,user_id,category,subject,message,status,created_at) VALUES(?,?,?,?,?,?,?)',[`rc-review:${e.id}`,userId,'Ödeme','Mağaza hakları uzlaştırması',JSON.stringify({type:e.type,productId,transactionId:e.transaction_id,expiresAt:e.expiration_at_ms,...(Number.isSafeInteger(e.purchased_at_ms)?{purchasedAt:e.purchased_at_ms}:{})}),'open',ctx.now]);
 }else ensure(false,400,'UNSUPPORTED_BILLING_EVENT','Olay türü desteklenmiyor.');
 await ctx.db.run('INSERT INTO webhook_events(id,payload_hash,created_at) VALUES(?,?,?)',[`revenuecat:${e.id}`,hash,ctx.now]);return{processed:true,reconciliationRequired:!type};
}
