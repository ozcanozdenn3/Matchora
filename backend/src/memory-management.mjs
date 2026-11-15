import { keys,ensure,json,text,choice } from './errors.mjs';
import { row,audit,encode } from './model.mjs';
import { SPORTS } from './sports.mjs';
import { indexMemory, coarseMemoryLocation } from './memory-feed.mjs';
export const memoryManagementHandlers={
 'memory.update':async(ctx,p)=>{
  keys(p,['memoryId','expectedVersion','caption','venue','area','city','sport','matchScoreTag','sceneTheme','uploadId','approximateLocation']);
  const old=await row(ctx,'memories','id',p.memoryId),data=json(old.payload);
  ensure(old.author_id===ctx.actor.id&&old.status!=='deleted',403,'FORBIDDEN','Yalnız kendi paylaşımını düzenleyebilirsin.');
  ensure(p.expectedVersion===(data.version??1),409,'VERSION_CONFLICT','Paylaşım değişti. Yenileyip tekrar dene.');
  if(p.uploadId){const upload=await row(ctx,'uploads','id',p.uploadId);ensure(upload.owner_id===ctx.actor.id&&upload.status==='ready',403,'FORBIDDEN','Fotoğraf sana ait olmalı.');}
  const next={...data,version:(data.version??1)+1,caption:text(p.caption??data.caption??'','Açıklama',0,500),venue:text(p.venue??data.venue??'','Tesis',0,160),
   city:text(p.city??data.city,'İl',2,60),area:text(p.area??data.area,'İlçe',2,60),sport:choice(p.sport??data.sport,SPORTS,'Branş'),
   matchScoreTag:p.matchScoreTag===undefined?data.matchScoreTag:text(p.matchScoreTag??'','Skor',0,80),sceneTheme:p.sceneTheme??data.sceneTheme,
   approximateLocation:coarseMemoryLocation(p.approximateLocation===undefined?data.approximateLocation:p.approximateLocation)};
  if(p.approximateLocation===undefined&&(next.city!==data.city||next.area!==data.area||next.venue!==data.venue))next.approximateLocation=null;
  choice(next.sceneTheme,['NIGHT_TURF','SUNSET_TENNIS','ARENA_HOOPS','PADEL_GLASS','BEACH_VOLLEY','TROPHY_MOMENT'],'Tema');
  await indexMemory(ctx,old.id,next);
  const nextStatus=old.status==='rejected'?'pending':'approved';
  await ctx.db.run('UPDATE memories SET payload=?,upload_id=?,status=? WHERE id=?',[encode(next),p.uploadId??old.upload_id,nextStatus,old.id]);
  await audit(ctx,'memory.update',old.id);return{updated:true,status:nextStatus};
 }
};
