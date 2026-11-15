import { ensure,choice,text,json } from './errors.mjs';
import { SPORTS } from './sports.mjs';
import { openCursor,sealCursor } from './opaque-cursor.mjs';
import { canonicalTurkishCity, sameTurkishPlace } from './turkish-locale.mjs';

export async function rankingRows(ctx){
 const deltas=new Map((await ctx.db.all('SELECT user_id,sport,SUM(delta) AS total FROM rating_history WHERE created_at>=? GROUP BY user_id,sport',[ctx.now-7*86400000])).map(d=>[`${d.user_id}:${d.sport}`,Number(d.total)]));
 const blocked=ctx.actor?new Set((await ctx.db.all('SELECT target_id AS id FROM blocks WHERE user_id=? UNION SELECT user_id AS id FROM blocks WHERE target_id=?',[ctx.actor.id,ctx.actor.id])).map(r=>r.id)):null;
 const rows=[];
 for(const user of await ctx.db.all("SELECT p.user_id,p.payload,n.privacy FROM profiles p JOIN users u ON u.id=p.user_id JOIN preferences n ON n.user_id=p.user_id WHERE u.status='active'")){
  if(blocked?.has(user.user_id))continue;
  const privacy=json(user.privacy),p=json(user.payload);if(privacy.showInRankings===false)continue;
  for(const skill of p.skills)rows.push({userId:user.user_id,name:p.name,avatarUploadId:p.avatarUploadId??null,city:p.city,area:p.area,sport:skill.sport,rating:skill.rating,verifiedMatches:skill.verifiedMatches,delta:privacy.showRatingHistory===false?0:deltas.get(`${user.user_id}:${skill.sport}`)??0});
 }
 return rows.sort((a,b)=>b.rating-a.rating||a.userId.localeCompare(b.userId,'en'));
}
export async function rankingPage(ctx,p={}){
 const sport=choice(p.sport??'FOOTBALL',SPORTS,'Branş'),scope=choice(p.scope??'district',['district','city','national'],'Bölge'),city=canonicalTurkishCity(text(p.city??'','İl',0,60)),district=text(p.district??'','İlçe',0,60);
 ensure(scope==='national'||city.length>=2,400,'CITY_REQUIRED','Sıralama için il seç.');ensure(scope!=='district'||district.length>=2,400,'DISTRICT_REQUIRED','Sıralama için ilçe seç.');
 const binding=JSON.stringify([sport,scope,city,district]),cursor=p.after?openCursor(ctx,'rankings',p.after):null;
 if(cursor)ensure(cursor.scope===binding&&Number.isFinite(cursor.rating)&&typeof cursor.userId==='string',400,'INVALID_CURSOR','Sıralama filtreleri değişti. Yenile.');
 const all=(await rankingRows(ctx)).filter(r=>r.sport===sport&&(scope==='national'||sameTurkishPlace(r.city,city))&&(scope!=='district'||sameTurkishPlace(r.area,district))).map((r,i)=>({...r,rank:i+1}));
 const available=cursor?all.filter(r=>r.rating<cursor.rating||r.rating===cursor.rating&&r.userId.localeCompare(cursor.userId,'en')>0):all;
 const size=Math.min(100,Math.max(1,Math.floor(Number(p.limit)||50))),items=available.slice(0,size),last=items.at(-1);
 return{items,total:all.length,own:all.find(r=>r.userId===ctx.actor?.id)??null,serverTime:ctx.now,nextCursor:available.length>size?sealCursor(ctx,'rankings',{scope:binding,rating:last.rating,userId:last.userId}):null};
}
