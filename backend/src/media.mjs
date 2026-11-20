import { visibleMessage } from './chat-state.mjs';
import sharp from 'sharp';
import { mkdir,writeFile,readFile,unlink } from 'node:fs/promises';
import { join } from 'node:path';
import { crc32 } from 'node:zlib';
import { id } from './auth.mjs';
import { ensure,json } from './errors.mjs';
import { assertThread,row } from './model.mjs';
export function sanitizeImage(bytes,mime){
 ensure(Buffer.isBuffer(bytes)&&bytes.length>20&&bytes.length<=5*1024*1024,413,'IMAGE_SIZE','Fotoğraf en fazla 5 MB olabilir.');
 if(mime==='image/jpeg'){
  ensure(bytes[0]===0xff&&bytes[1]===0xd8,415,'INVALID_IMAGE','JPEG dosyası geçersiz.');const chunks=[bytes.subarray(0,2)];let offset=2,dimensions=false;
  while(offset<bytes.length){ensure(bytes[offset]===0xff,415,'INVALID_IMAGE','JPEG bölümü geçersiz.');const marker=bytes[offset+1];if(marker===0xda){ensure(dimensions&&bytes.at(-2)===0xff&&bytes.at(-1)===0xd9,415,'INVALID_IMAGE','JPEG tamamlanmamış.');chunks.push(bytes.subarray(offset));return Buffer.concat(chunks);}ensure(marker!==0xd9&&marker!==0x00&&offset+4<=bytes.length,415,'INVALID_IMAGE','JPEG geçersiz.');const length=bytes.readUInt16BE(offset+2);ensure(length>=2&&offset+2+length<=bytes.length,415,'INVALID_IMAGE','JPEG bölümü geçersiz.');if([0xc0,0xc1,0xc2].includes(marker)){ensure(length>=8);const h=bytes.readUInt16BE(offset+5),w=bytes.readUInt16BE(offset+7);ensure(w>0&&h>0&&w<=8192&&h<=8192&&w*h<=40000000,413,'IMAGE_DIMENSIONS','Fotoğraf boyutları çok büyük.');dimensions=true;}if(!(marker>=0xe1&&marker<=0xef)&&marker!==0xfe)chunks.push(bytes.subarray(offset,offset+length+2));offset+=length+2;}
 }else if(mime==='image/png'){
  ensure(bytes.subarray(0,8).equals(Buffer.from([137,80,78,71,13,10,26,10])),415,'INVALID_IMAGE','PNG geçersiz.');const chunks=[bytes.subarray(0,8)];let offset=8,hasData=false,hasHeader=false;
  const allowed=new Set(['IHDR','PLTE','IDAT','IEND','tRNS','sRGB','gAMA','cHRM']);
  while(offset+12<=bytes.length){const length=bytes.readUInt32BE(offset),end=offset+length+12;ensure(end<=bytes.length,415,'INVALID_IMAGE','PNG bölümü geçersiz.');const type=bytes.toString('ascii',offset+4,offset+8);ensure(crc32(bytes.subarray(offset+4,end-4))===bytes.readUInt32BE(end-4),415,'INVALID_IMAGE','PNG sağlama değeri geçersiz.');if(type==='IHDR'){ensure(!hasHeader&&offset===8&&length===13);const w=bytes.readUInt32BE(offset+8),h=bytes.readUInt32BE(offset+12);ensure(w>0&&h>0&&w<=8192&&h<=8192&&w*h<=40000000,413,'IMAGE_DIMENSIONS','Fotoğraf boyutları çok büyük.');hasHeader=true;}if(type==='IDAT')hasData=true;if(allowed.has(type))chunks.push(bytes.subarray(offset,end));if(type==='IEND'){ensure(hasHeader&&hasData&&length===0);return Buffer.concat(chunks);}offset=end;}
 }
 ensure(false,415,'INVALID_IMAGE','Yalnızca geçerli JPEG veya PNG yükleyebilirsin.');
}
export class LocalMediaStore{
 constructor(path){this.path=path;}
 async put(key,bytes){await mkdir(this.path,{recursive:true,mode:0o700});await writeFile(join(this.path,key),bytes,{flag:'wx',mode:0o600});}
 async get(key){ensure(/^[a-f0-9-]{36}$/.test(key));return readFile(join(this.path,key));}
 async remove(key){ensure(/^[a-f0-9-]{36}$/.test(key));await unlink(join(this.path,key)).catch(e=>{if(e.code!=='ENOENT')throw e;});}
}
export async function prepareUpload(bytes,mime){
 const checked=sanitizeImage(bytes,mime);let clean;
 try{clean=await sharp(checked,{limitInputPixels:40000000,failOn:'warning',animated:false}).rotate().resize({width:2048,height:2048,fit:'inside',withoutEnlargement:true}).jpeg({quality:85,mozjpeg:false}).toBuffer();}
 catch{ensure(false,415,'INVALID_IMAGE','Fotoğraf çözümlenemedi. Başka bir fotoğraf seç.');}
 mime='image/jpeg';ensure(clean.length<=5*1024*1024,413,'IMAGE_SIZE','Fotoğraf çok büyük.');
 return{uploadId:id(),bytes:clean,mime};
}
export async function recordUpload(ctx,uploadId,size,mime='image/jpeg'){
 await ctx.db.run('INSERT INTO uploads(id,owner_id,mime,size,object_key,status,created_at) VALUES(?,?,?,?,?,?,?)',[uploadId,ctx.actor.id,mime,size,uploadId,'ready',ctx.now]);
 return{id:uploadId,mime,size};
}
export async function upload(ctx,storage,bytes,mime){
 const prepared=await prepareUpload(bytes,mime);
 await storage.put(prepared.uploadId,prepared.bytes);
 try{return await recordUpload(ctx,prepared.uploadId,prepared.bytes.length,prepared.mime);}
 catch(error){await storage.remove(prepared.uploadId);throw error;}
}
export async function authorizeUploadRead(ctx,uploadId){
 const item=await row(ctx,'uploads','id',uploadId);ensure(item.status==='ready',404,'NOT_FOUND','Fotoğraf bulunamadı.');let allowed=item.owner_id===ctx.actor?.id;
 if(!allowed&&ctx.actor?.role==='admin')allowed=!!await ctx.db.get("SELECT id FROM memories WHERE upload_id=? AND status='pending'",[item.id]);
 if(!allowed&&ctx.actor?.role==='admin')allowed=!!await ctx.db.get("SELECT id FROM support_requests WHERE category='Fotoğraf doğrulaması' AND status='open' AND message LIKE ?",[`%${item.id}%`]);
 if(!allowed&&ctx.actor?.role==='admin'){const apps=await ctx.db.all("SELECT payload FROM venue_applications WHERE status='pending' AND payload LIKE ?",[`%${item.id}%`]);allowed=apps.some(a=>(json(a.payload).photoUploadIds??[]).includes(item.id));}
 if(!allowed){const memory=await ctx.db.get("SELECT id,author_id FROM memories WHERE upload_id=? AND status='approved'",[item.id]);allowed=!!memory;if(memory&&ctx.actor&&await ctx.db.get('SELECT user_id FROM blocks WHERE (user_id=? AND target_id=?) OR (user_id=? AND target_id=?)',[ctx.actor.id,memory.author_id,memory.author_id,ctx.actor.id]))allowed=false;}
 if(!allowed){const profiles=await ctx.db.all("SELECT p.payload FROM profiles p JOIN users u ON u.id=p.user_id WHERE u.status='active' AND p.payload LIKE ?",[`%${item.id}%`]);allowed=profiles.some(p=>json(p.payload).avatarUploadId===item.id);}
 if(!allowed){const venues=await ctx.db.all("SELECT v.payload FROM venues v JOIN users u ON u.id=v.owner_id WHERE u.status='active' AND u.role IN ('admin','venue_owner') AND v.payload LIKE ?",[`%${item.id}%`]);allowed=venues.some(v=>{const d=json(v.payload);return (!d.archived||ctx.actor?.role==='admin')&&(d.photoUploadIds??[]).includes(item.id);});}
 if(!allowed&&ctx.actor){for(const m of await ctx.db.all('SELECT id,thread_id FROM messages WHERE upload_id=?',[item.id])){try{const message=await visibleMessage(ctx,m.id);if(message.deleted_at)continue;allowed=true;break;}catch{}}}
 ensure(allowed,404,'NOT_FOUND','Fotoğraf bulunamadı.');return{objectKey:item.object_key,mime:item.mime};
}
export async function readUpload(ctx,storage,uploadId){
 const file=await authorizeUploadRead(ctx,uploadId);
 return{bytes:await storage.get(file.objectKey),mime:file.mime};
}
