import { ensure } from './errors.mjs';

/** Private objects are proxied through readUpload so membership/block checks still apply. */
export class SupabaseMediaStore {
 constructor(url,key,bucket='matchora-private',request=fetch){
  const endpoint=new URL(url);
  ensure(endpoint.protocol==='https:'&&!endpoint.username&&!endpoint.password,503,'STORAGE_CONFIGURATION','Dosya deposu HTTPS adresi kullanmalı.');
  ensure(/^[a-z0-9-]{1,63}$/.test(bucket)&&key,503,'STORAGE_CONFIGURATION','Özel dosya deposu yapılandırılmalı.');
  this.base=endpoint.origin+'/storage/v1';this.key=key;this.bucket=bucket;this.request=request;
 }
 object(key){ensure(/^[a-f0-9-]{36}$/.test(key));return `${this.bucket}/${key}`;}
 async call(path,options={}){
  let response;
  try{response=await this.request(this.base+path,{...options,headers:{apikey:this.key,Authorization:`Bearer ${this.key}`,...options.headers},signal:AbortSignal.timeout(10000),redirect:'error'});}
  catch{ensure(false,503,'STORAGE_UNAVAILABLE','Fotoğraf deposuna ulaşılamadı. Tekrar dene.');}
  ensure(response.ok,503,'STORAGE_UNAVAILABLE','Fotoğraf işlemi tamamlanamadı. Tekrar dene.');return response;
 }
 async put(key,bytes){await this.call(`/object/${this.object(key)}`,{method:'POST',headers:{'Content-Type':bytes[0]===0x89?'image/png':'image/jpeg','x-upsert':'false'},body:bytes});}
 async get(key){const response=await this.call(`/object/authenticated/${this.object(key)}`);const length=Number(response.headers.get('content-length'));ensure(!length||length<=5*1024*1024,413,'IMAGE_SIZE','Fotoğraf çok büyük.');const bytes=Buffer.from(await response.arrayBuffer());ensure(bytes.length<=5*1024*1024,413,'IMAGE_SIZE','Fotoğraf çok büyük.');return bytes;}
 async remove(key){this.object(key);await this.call(`/object/${this.bucket}`,{method:'DELETE',headers:{'Content-Type':'application/json'},body:JSON.stringify({prefixes:[key]})});}
}

export async function queueStorageDeletion(ctx,key){await ctx.db.run('INSERT INTO storage_deletions(object_key,next_at,created_at) VALUES(?,?,?) ON CONFLICT(object_key) DO NOTHING',[key,ctx.now,ctx.now]);}
export async function cleanMedia(service){
 const now=service.clock();
 await service.store.transaction(async db=>{
  // Unattached uploads are kept for 24h to allow retries; verification evidence stays attached.
  for(const item of await db.all("SELECT u.* FROM uploads u WHERE u.status='ready' AND u.created_at<? AND NOT EXISTS(SELECT 1 FROM messages m WHERE m.upload_id=u.id) AND NOT EXISTS(SELECT 1 FROM memories m WHERE m.upload_id=u.id AND m.status<>'deleted')",[now-86400000])){
   if(await db.get('SELECT user_id FROM profiles WHERE payload LIKE ? LIMIT 1',[`%${item.id}%`]))continue;
   if(await db.get('SELECT id FROM venues WHERE payload LIKE ? LIMIT 1',[`%${item.id}%`]))continue;
   if(await db.get("SELECT id FROM venue_applications WHERE status='pending' AND payload LIKE ? LIMIT 1",[`%${item.id}%`]))continue;
   if(await db.get("SELECT id FROM support_requests WHERE category='Fotoğraf doğrulaması' AND status='open' AND message LIKE ? LIMIT 1",[`%${item.id}%`]))continue;
   await db.run("UPDATE uploads SET status='deleted' WHERE id=?",[item.id]);await queueStorageDeletion({db,now},item.object_key);
  }
 });
 for(const item of await service.store.all('SELECT * FROM storage_deletions WHERE next_at<=? ORDER BY created_at LIMIT 50',[now])){
  try{await service.media.remove(item.object_key);await service.store.run('DELETE FROM storage_deletions WHERE object_key=?',[item.object_key]);}
  catch{await service.store.run('UPDATE storage_deletions SET attempts=attempts+1,next_at=? WHERE object_key=?',[now+Math.min(86400000,2**Math.min(item.attempts,12)*60000),item.object_key]);}
 }
}
