import { id } from './auth.mjs';
import { coarseMemoryLocation } from './memory-feed.mjs';
import { ensure,keys,text,integer,choice,json } from './errors.mjs';
import { row,profile,encode,audit,notify } from './model.mjs';
import { SPORTS } from './sports.mjs';

export const venueApplicationHandlers={
 'venue.application.create':async(ctx,p)=>{
  keys(p,['name','city','area','sports','amenities','courts','cancellationHours','address','approximateLocation','phone','description']);await profile(ctx);
  ensure(Array.isArray(p.sports)&&p.sports.length>0&&p.sports.length<=20&&new Set(p.sports).size===p.sports.length);
  ensure(Array.isArray(p.amenities??[])&&(p.amenities??[]).length<=30);
  const pending=await ctx.db.get("SELECT COUNT(*) AS n FROM venue_applications WHERE user_id=? AND status='pending'",[ctx.actor.id]);
  ensure(Number(pending.n)<3,409,'APPLICATION_LIMIT','En fazla üç bekleyen tesis başvurusu olabilir.');
  const payload={name:text(p.name,'Tesis adı',3,100),city:text(p.city,'İl',2,60),area:text(p.area,'İlçe',2,80),address:text(p.address,'Tesis adresi',10,300),sports:p.sports.map(s=>choice(s,SPORTS,'Branş')),amenities:(p.amenities??[]).map(a=>text(a,'Olanak',1,60)),courts:integer(p.courts,'Saha sayısı',1,50),cancellationHours:integer(p.cancellationHours??24,'İptal süresi',0,168),approximateLocation:coarseMemoryLocation(p.approximateLocation),phone:text(p.phone??'','Telefon',0,40),description:text(p.description??'','Açıklama',0,1000)};
  const duplicate=await ctx.db.all("SELECT payload FROM venue_applications WHERE user_id=? AND status='pending'",[ctx.actor.id]);
  ensure(!duplicate.some(a=>{const v=json(a.payload);return v.name.toLocaleLowerCase('tr')===payload.name.toLocaleLowerCase('tr')&&v.city===payload.city&&v.area===payload.area;}),409,'ALREADY_REQUESTED','Bu tesisin başvurusu zaten inceleniyor.');
  const applicationId=id();await ctx.db.run("INSERT INTO venue_applications(id,user_id,payload,status,created_at,updated_at) VALUES(?,?,?,'pending',?,?)",[applicationId,ctx.actor.id,encode(payload),ctx.now,ctx.now]);
  for(const admin of await ctx.db.all("SELECT id FROM users WHERE role='admin' AND status='active'"))await notify(ctx,admin.id,'social','Yeni tesis başvurusu',payload.name,'MODERATION',applicationId,`venue-application:${applicationId}:${admin.id}`);
  await audit(ctx,'venue.application.create',applicationId);return{id:applicationId,status:'pending'};
 },
 'venue.application.withdraw':async(ctx,p)=>{
  keys(p,['applicationId','expectedVersion']);const a=await row(ctx,'venue_applications','id',p.applicationId);
  ensure(a.user_id===ctx.actor.id,403,'FORBIDDEN','Başvuru sana ait değil.');ensure(a.status==='pending',409,'INVALID_STATE','Başvuru artık beklemiyor.');ensure(integer(p.expectedVersion,'Sürüm',1)===a.version,409,'VERSION_CONFLICT','Başvuru güncellendi.');
  await ctx.db.run("UPDATE venue_applications SET status='withdrawn',version=version+1,updated_at=? WHERE id=?",[ctx.now,a.id]);await audit(ctx,'venue.application.withdraw',a.id);return{withdrawn:true};
 },
 'admin.venue.application.review':async(ctx,p)=>{
  keys(p,['applicationId','expectedVersion','approve','response']);ensure(ctx.actor.role==='admin',403,'FORBIDDEN','Yönetici yetkisi gerekiyor.');ensure(typeof p.approve==='boolean');
  const a=await row(ctx,'venue_applications','id',p.applicationId),user=await row(ctx,'users','id',a.user_id),payload=json(a.payload);
  ensure(a.status==='pending',409,'INVALID_STATE','Başvuru işlendi.');ensure(integer(p.expectedVersion,'Sürüm',1)===a.version,409,'VERSION_CONFLICT','Başvuru güncellendi.');
  const response=text(p.response,'Başvuru yanıtı',3,1000);let venueId=null;
  if(p.approve){ensure(user.status==='active',409,'ACCOUNT_DISABLED','Başvuran hesap aktif olmalı.');venueId=id();await ctx.db.run('INSERT INTO venues(id,owner_id,payload,created_at) VALUES(?,?,?,?)',[venueId,a.user_id,encode({...payload,version:1,verified:true}),ctx.now]);if(user.role!=='admin')await ctx.db.run("UPDATE users SET role='venue_owner' WHERE id=?",[a.user_id]);}
  await ctx.db.run('UPDATE venue_applications SET status=?,venue_id=?,response=?,version=version+1,updated_at=? WHERE id=?',[p.approve?'approved':'rejected',venueId,response,ctx.now,a.id]);
  await notify(ctx,a.user_id,'social',p.approve?'Tesisin yayına alındı':'Tesis başvurusu yanıtlandı',response,'VENUES',venueId??a.id,`venue-application-review:${a.id}`);await audit(ctx,'admin.venue.application.review',a.id,{approved:p.approve,venueId});return{approved:p.approve,venueId};
 }
};
