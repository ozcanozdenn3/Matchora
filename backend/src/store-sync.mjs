import { ensure,keys } from './errors.mjs';
import { rateLimit,sign,digest } from './auth.mjs';
import { PRODUCTS,billingWebhook } from './billing.mjs';

export function storeConfiguration(config){
 const keys={ios:config.revenueCatAppleKey,android:config.revenueCatGoogleKey};
 for(const platform of Object.keys(keys))if(!/^(appl_|goog_)/.test(keys[platform]??'')&&!( !config.production&&keys[platform]?.startsWith('test_')))keys[platform]=null;
 return{keys,products:{ios:config.revenueCatAppleProducts??{},android:config.revenueCatGoogleProducts??{}},sync:!!config.revenueCatSecretKey,enabled:!!(config.billingSecret&&config.revenueCatAuthorization)};
}
export async function synchronizeStore(ctx,request=fetch){
 ensure(ctx.config.revenueCatSecretKey&&ctx.config.billingSecret,503,'STORE_SYNC_UNAVAILABLE','Mağaza doğrulama servisi henüz yapılandırılmadı.');
 await rateLimit(ctx.db,`billing-sync:${ctx.actor.id}`,ctx.now,10,60000);
 let response;try{response=await request(`https://api.revenuecat.com/v1/subscribers/${encodeURIComponent(ctx.actor.id)}`,{headers:{Authorization:`Bearer ${ctx.config.revenueCatSecretKey}`,Accept:'application/json'},signal:AbortSignal.timeout(8000),redirect:'error'});}catch{ensure(false,503,'STORE_SYNC_UNAVAILABLE','Mağaza hakları alınamadı. Tekrar dene.');}
 ensure(response.ok,503,'STORE_SYNC_UNAVAILABLE','Mağaza hakları alınamadı. Tekrar dene.');const data=await response.json(),subscriber=data.subscriber;
 ensure(subscriber&&subscriber.original_app_user_id===ctx.actor.id,409,'PURCHASE_OWNERSHIP','Mağaza hesabı başka bir Matchora hesabına ait. Destekle iletişime geç.');
 const normalized=[];
 for(const [storeId,entries]of Object.entries(subscriber.non_subscriptions??{}))for(const purchase of entries){
  const productId=ctx.config.revenueCatProducts?.[storeId]??storeId,product=PRODUCTS[productId];if(!product||product.subscription)continue;
  const transaction=String(purchase.store_transaction_id??'');if(!transaction||purchase.is_sandbox&&ctx.config.production)continue;
  normalized.push({type:purchase.refunded_at?'refund':'purchase',transactionId:transaction,productId,platform:purchase.store==='app_store'?'apple':purchase.store==='play_store'?'google':'unsupported'});
 }
 let active=false;
 for(const [storeId,purchase]of Object.entries(subscriber.subscriptions??{})){
  const productId=ctx.config.revenueCatProducts?.[storeId]??storeId,product=PRODUCTS[productId],expiresAt=Date.parse(purchase.expires_date),purchasedAt=Date.parse(purchase.purchase_date??purchase.original_purchase_date);
  if(!product?.subscription||!Number.isSafeInteger(expiresAt)||expiresAt<=ctx.now||purchase.is_sandbox&&ctx.config.production)continue;
  const transaction=String(purchase.store_transaction_id??'');if(!transaction)continue;active=true;
  normalized.push({type:'purchase',transactionId:transaction,originalTransactionId:purchase.original_transaction_id??transaction,productId,platform:purchase.store==='app_store'?'apple':purchase.store==='play_store'?'google':'unsupported',expiresAt,purchasedAt:Number.isSafeInteger(purchasedAt)?purchasedAt:ctx.now});
 }
 for(const item of normalized){
  if(item.platform==='unsupported')continue;
  // Revoked consumables can only reverse an existing grant, never invent a negative purchase.
  if(item.type==='refund'&&!await ctx.db.get('SELECT transaction_id FROM store_transactions WHERE transaction_id=?',[item.transactionId]))continue;
  const value={...item,userId:ctx.actor.id};const raw=Buffer.from(JSON.stringify({...value,eventId:`rc-sync:${digest(JSON.stringify(value))}`}));
  await billingWebhook(ctx,raw,{'x-matchora-timestamp':String(ctx.now),'x-matchora-signature':sign(ctx.config.billingSecret,`${ctx.now}.${raw}`)});
 }
 if(!active)await ctx.db.run("UPDATE subscriptions SET status='expired' WHERE user_id=? AND expires_at<=?",[ctx.actor.id,ctx.now]);
 return{synchronized:true};
}
export const storeHandlers={'billing.sync':async(ctx,p)=>{keys(p,[]);return synchronizeStore(ctx);}};
