import { createHash,randomUUID } from 'node:crypto';
import { ensure,choice,text,json } from './errors.mjs';
import { sealCursor,openCursor } from './opaque-cursor.mjs';
import { canonicalTurkish } from './turkish-locale.mjs';
const radians=Math.PI/180;
const canonical=value=>canonicalTurkish(value);
const digest=value=>createHash('sha256').update(value).digest('hex');

export function coarseMemoryLocation(point, strict=true) {
 if(point==null)return null;
 const valid=Number.isFinite(point.latitude)&&Math.abs(point.latitude)<=90&&Number.isFinite(point.longitude)&&Math.abs(point.longitude)<=180;
 if(strict)ensure(valid,400,'INVALID_LOCATION','Konum noktası geçersiz.');
 return valid?{latitude:Math.round(point.latitude*100)/100,longitude:Math.round(point.longitude*100)/100}:null;
}
export function publicMemoryPayload(payload, blockedIds=null) {
 const data=typeof payload==='string'?json(payload):payload;
 const comments=Array.isArray(data.comments)&&blockedIds?.size?data.comments.filter(c=>!blockedIds.has(c.authorId)):data.comments;
 return{...data,...(comments!==undefined?{comments}:{}),approximateLocation:coarseMemoryLocation(data.approximateLocation,false)};
}

export async function indexMemory(ctx,memoryId,payload) {
 const point=payload.approximateLocation??null;
 if(point)ensure(Number.isFinite(point.latitude)&&Math.abs(point.latitude)<=90&&Number.isFinite(point.longitude)&&Math.abs(point.longitude)<=180,400,'INVALID_LOCATION','Konum noktası geçersiz.');
 await ctx.db.run(`INSERT INTO memory_locations(memory_id,city,district,sport,latitude,longitude,shuffle_key) VALUES(?,?,?,?,?,?,?)
 ON CONFLICT(memory_id) DO UPDATE SET city=excluded.city,district=excluded.district,sport=excluded.sport,latitude=excluded.latitude,longitude=excluded.longitude`,
 [memoryId,canonical(payload.city),canonical(payload.area),payload.sport??'FOOTBALL',point?Math.round(point.latitude*100)/100:null,point?Math.round(point.longitude*100)/100:null,parseInt(digest(memoryId).slice(0,8),16)%2147483647]);
}
export async function ensureMemoryIndex(ctx){
 for(const row of await ctx.db.all("SELECT m.id,m.payload,l.city,l.district FROM memories m LEFT JOIN memory_locations l ON l.memory_id=m.id WHERE m.status<>'deleted'")){
  const payload=json(row.payload);
  if(row.city===null||row.city!==canonical(payload.city)||row.district!==canonical(payload.area))await indexMemory(ctx,row.id,payload);
 }
}

export async function memoryFeed(ctx,filters={}) {
 await ensureMemoryIndex(ctx);
 const mode=choice(filters.mode??'random',['random','nearby','top','newest','mine'],'Akış');
 if(mode==='mine')ensure(ctx.actor,401,'UNAUTHENTICATED','Paylaşımlarını görmek için giriş yap.');
 const city=canonical(text(filters.city??'','İl',0,60)),district=canonical(text(filters.district??'','İlçe',0,60));
 const latitude=filters.latitude==null||filters.latitude===''?null:Number(filters.latitude),longitude=filters.longitude==null||filters.longitude===''?null:Number(filters.longitude);
 ensure((latitude==null&&longitude==null)||(Number.isFinite(latitude)&&Math.abs(latitude)<=90&&Number.isFinite(longitude)&&Math.abs(longitude)<=180),400,'INVALID_LOCATION','Konum geçersiz.');
 const cursor=filters.after?openCursor(ctx,'memory-feed',filters.after):null;
 const seed=text(filters.seed??cursor?.seed??randomUUID(),'Akış anahtarı',1,80),watermark=cursor?.watermark??ctx.now;
 const scope=digest(JSON.stringify([mode,city,district,latitude,longitude,seed,filters.sport??'']));
 if(cursor)ensure(cursor.scope===scope&&Array.isArray(cursor.last),400,'INVALID_CURSOR','Akış filtreleri değişti. Baştan yenile.');
 const factor=1+parseInt(digest(seed).slice(0,6),16)%1000000,shift=parseInt(digest(seed).slice(6,14),16)%2147483647;
 const sphere=latitude==null?'CAST(NULL AS DOUBLE PRECISION)':`sin(l.latitude*${radians})*(${Math.sin(latitude*radians)})+cos(l.latitude*${radians})*(${Math.cos(latitude*radians)})*cos((l.longitude-(${longitude}))*${radians})`;
 const params=[city,district,city,watermark];
 let eligibility=mode==='mine'?"m.author_id=? AND m.status IN ('approved','pending','rejected')":"m.status='approved'";
 if(mode==='mine')params.push(ctx.actor.id);
 if(ctx.actor){eligibility+=' AND NOT EXISTS(SELECT 1 FROM blocks b WHERE (b.user_id=? AND b.target_id=m.author_id) OR (b.user_id=m.author_id AND b.target_id=?))';params.push(ctx.actor.id,ctx.actor.id);}
 if(filters.sport){eligibility+=' AND l.sport=?';params.push(filters.sport);}
 let sql=`WITH scored AS (SELECT m.*,l.latitude,l.longitude,
 (SELECT COUNT(*) FROM memory_likes likes WHERE likes.memory_id=m.id) AS like_count,
 ((l.shuffle_key*${factor}+${shift})%2147483647) AS shuffle_rank,
 CASE WHEN l.city=? AND l.district=? THEN 0 WHEN l.city=? THEN 1 ELSE 2 END AS region_rank,
 ${sphere} AS sphere_dot FROM memories m JOIN memory_locations l ON l.memory_id=m.id JOIN users u ON u.id=m.author_id
 WHERE m.created_at<=? AND u.status='active' AND ${eligibility}),
 ranked AS(SELECT scored.*,CASE WHEN sphere_dot IS NULL THEN 1000000000.0 ELSE 6371.0*acos(CASE WHEN sphere_dot>1 THEN 1 WHEN sphere_dot< -1 THEN -1 ELSE sphere_dot END) END AS distance_rank FROM scored)
 SELECT * FROM ranked`;
 const columns=mode==='random'?[['shuffle_rank','ASC'],['id','ASC']]:mode==='top'?[['like_count','DESC'],['created_at','DESC'],['id','DESC']]:mode==='nearby'?[['distance_rank','ASC'],['region_rank','ASC'],['created_at','DESC'],['id','DESC']]:[['created_at','DESC'],['id','DESC']];
 if(cursor){ensure(cursor.last.length===columns.length&&cursor.last.every((v,i)=>columns[i][0]==='id'?typeof v==='string':Number.isFinite(v)),400,'INVALID_CURSOR','Akış sayfası geçersiz.');
  const branches=[];for(let i=0;i<columns.length;i++){const conditions=[];for(let n=0;n<i;n++){conditions.push(`${columns[n][0]}=?`);params.push(cursor.last[n]);}conditions.push(`${columns[i][0]}${columns[i][1]==='ASC'?'>':'<'}?`);params.push(cursor.last[i]);branches.push(`(${conditions.join(' AND ')})`);}sql+=' WHERE '+branches.join(' OR ');
 }
 const size=Math.min(100,Math.max(1,Math.floor(Number(filters.limit)||24)));sql+=' ORDER BY '+columns.map(([field,direction])=>`${field} ${direction}`).join(',')+' LIMIT ?';params.push(size+1);
 const blockedIds=ctx.actor?new Set((await ctx.db.all('SELECT target_id AS id FROM blocks WHERE user_id=? UNION SELECT user_id AS id FROM blocks WHERE target_id=?',[ctx.actor.id,ctx.actor.id])).map(r=>r.id)):null;
 const rows=await ctx.db.all(sql,params),selected=rows.slice(0,size),items=[];
 const authorIds=[...new Set(selected.map(m=>m.author_id))],profilesById=new Map();
 if(authorIds.length){
  const q=authorIds.map(()=>'?').join(',');
  for(const p of await ctx.db.all(`SELECT user_id,payload FROM profiles WHERE user_id IN (${q})`,authorIds))profilesById.set(p.user_id,json(p.payload));
 }
 const likedSet=new Set();
 if(ctx.actor&&selected.length){
  const mIds=selected.map(m=>m.id),q=mIds.map(()=>'?').join(',');
  for(const l of await ctx.db.all(`SELECT memory_id FROM memory_likes WHERE user_id=? AND memory_id IN (${q})`,[ctx.actor.id,...mIds]))likedSet.add(l.memory_id);
 }
 for(const memory of selected){
  const data=profilesById.get(memory.author_id)??{};
  items.push({...publicMemoryPayload(memory.payload,blockedIds),id:memory.id,authorId:memory.author_id,authorName:data.name??'Sporcu',authorAvatarUploadId:data.avatarUploadId??null,uploadId:memory.upload_id,status:memory.status,likes:Number(memory.like_count),likedByMe:likedSet.has(memory.id),createdAt:Number(memory.created_at),distanceKm:memory.distance_rank<1000000000?Math.round(memory.distance_rank*10)/10:null});
 }
 const last=selected.at(-1);
 return {items,mode,seed,serverTime:ctx.now,locationBasis:latitude!=null?'gps':city?'region':'unknown',nextCursor:rows.length>size?sealCursor(ctx,'memory-feed',{scope,seed,watermark,last:columns.map(([column])=>column==='id'?last[column]:Number(last[column]))}):null};
}
