import { createHash } from 'node:crypto';
import { ensure, choice, integer, text, json } from './errors.mjs';
import { SPORTS } from './sports.mjs';
import { sealCursor, openCursor } from './opaque-cursor.mjs';
import { teamStatistics } from './team-matches.mjs';
import { tournamentDetails } from './tournament-details.mjs';
import { publicPlayer, canSeeEvent, dateLabel, timeLabel } from './queries.mjs';
import { distanceKm } from './geo.mjs';
import { canonicalTurkish } from './turkish-locale.mjs';

const kinds=['teams','tournaments','communities','venues'];
const canonical=value=>canonicalTurkish(value);
const digest=value=>createHash('sha256').update(JSON.stringify(value)).digest('hex');

export async function catalogDetail(ctx,kind,id) {
 choice(kind,kinds,'Katalog');ensure(typeof id==='string'&&/^[a-fA-F0-9-]{36}$/.test(id),400,'INVALID_INPUT','Kayıt kimliği geçersiz.');
 const record=await ctx.db.get(`SELECT * FROM ${kind} WHERE id=?`,[id.toLowerCase()]);ensure(record,404,'NOT_FOUND','Kayıt bulunamadı.');
 const item=await catalogItem(ctx,kind,record);ensure(item,404,'NOT_FOUND','Kayıt artık kullanılamıyor.');
 const refs=kind==='teams'?[item.captainId,...item.memberIds]:[];
 return{kind,item,players:await Promise.all([...new Set(refs)].map(id=>publicPlayer(ctx,id))),serverTime:ctx.now};
}

async function visibleCatalogRecord(ctx,kind,record,filters){
  const data=json(record.payload);
  if(data.archived)return false;
  const sport=kind==='venues'?data.sports??[]:[record.sport??data.sport];
  if(filters.sport&&!sport.includes(filters.sport))return false;
  let city=data.city;
  if(kind==='teams'&&!city){
    ctx.captainCityCache??=new Map();
    if(!ctx.captainCityCache.has(record.captain_id)){const saved=await ctx.db.get('SELECT payload FROM profiles WHERE user_id=?',[record.captain_id]);ctx.captainCityCache.set(record.captain_id,saved?json(saved.payload).city:'');}
    city=ctx.captainCityCache.get(record.captain_id);
  }
  if(filters.city&&canonical(city)!==filters.city)return false;
  if(filters.district&&canonical(data.area)!==filters.district)return false;
  if(kind==='venues'&&filters.radiusKm!=null&&filters.origin){
    const point=data.approximateLocation;
    const hasCoords=point&&Number.isFinite(Number(point.latitude))&&Number.isFinite(Number(point.longitude));
    if(hasCoords){
      if(distanceKm(filters.origin,{latitude:Number(point.latitude),longitude:Number(point.longitude)})>filters.radiusKm)return false;
    }else if(!filters.fallbackDistrict||canonical(data.area)!==filters.fallbackDistrict){
      // Legacy unpinned venues match tight radius queries only within the user's selected district.
      return false;
    }
  }
  return true;
}

/** The same card projection is used for bootstrap and all subsequent catalog pages. */
export async function catalogItem(ctx,kind,record,origin=null){
  const data=json(record.payload);if(data.archived)return null;
  if(kind==='teams'){
    const members=await ctx.db.all("SELECT user_id FROM team_members WHERE team_id=? AND status='joined' ORDER BY user_id",[record.id]);
    return{...data,...await teamStatistics(ctx,record,members),id:record.id,sport:record.sport,captainId:record.captain_id,memberIds:members.map(m=>m.user_id),isMine:members.some(m=>m.user_id===ctx.actor?.id)};
  }
  if(kind==='tournaments'){
    const count=await ctx.db.get("SELECT COUNT(*) AS n FROM tournament_members WHERE tournament_id=? AND status='joined'",[record.id]);
    return{...data,id:record.id,creatorId:record.creator_id,sport:record.sport,status:record.status,capacity:record.capacity,participants:Number(count.n),...await tournamentDetails(ctx,record)};
  }
  if(kind==='communities'){
    const count=await ctx.db.get("SELECT COUNT(*) AS n FROM community_members WHERE community_id=? AND status='joined'",[record.id]);
    if(!ctx.catalogUpcoming){
      ctx.catalogUpcoming=new Map();
      for(const event of await ctx.db.all("SELECT * FROM events WHERE starts_at>? AND status IN ('PUBLISHED','FILLING','FULL')",[ctx.now])){
        const community=json(event.payload).communityId;if(community&&await canSeeEvent(ctx,event))ctx.catalogUpcoming.set(community,(ctx.catalogUpcoming.get(community)??0)+1);
      }
    }
    return{...data,id:record.id,access:record.access,ownerId:record.owner_id,memberCount:Number(count.n),upcomingEvents:ctx.catalogUpcoming.get(record.id)??0};
  }
  const owner=await ctx.db.get('SELECT role,status FROM users WHERE id=?',[record.owner_id]);
  if(owner?.status!=='active'||!['admin','venue_owner'].includes(owner.role))return null;
  const slotRows=await ctx.db.all('SELECT * FROM venue_slots WHERE venue_id=? AND starts_at>? ORDER BY starts_at,id LIMIT 100',[record.id,ctx.now]);
  const bookedSlots=new Set();
  if(slotRows.length){
    const slotIds=slotRows.map(s=>s.id),q=slotIds.map(()=>'?').join(',');
    for(const b of await ctx.db.all(`SELECT slot_id FROM bookings WHERE status IN ('pending','confirmed') AND slot_id IN (${q})`,slotIds))bookedSlots.add(b.slot_id);
  }
  const slots=slotRows.map(slot=>({...json(slot.payload),id:slot.id,day:dateLabel(slot.starts_at),time:timeLabel(slot.starts_at),priceTry:Math.ceil(slot.price_minor/100),priceMinor:Number(slot.price_minor),available:slot.status==='open'&&!bookedSlots.has(slot.id),startsAtEpochMillis:Number(slot.starts_at)}));
  const reviews=await ctx.db.get("SELECT AVG(rating) AS score,COUNT(*) AS n FROM venue_reviews WHERE venue_id=? AND status='published'",[record.id]);
  const point=data.approximateLocation;
  const venueDistanceKm=origin&&point&&Number.isFinite(Number(point.latitude))&&Number.isFinite(Number(point.longitude))
    ?Math.round(distanceKm(origin,{latitude:Number(point.latitude),longitude:Number(point.longitude)})*10)/10
    :null;
  return{...data,id:record.id,ownerId:record.owner_id,slots,rating:Number(reviews.score??0),reviews:Number(reviews.n),...(venueDistanceKm!=null?{distanceKm:venueDistanceKm}:{})};
}

export async function catalogPage(ctx,kind,input={}){
  choice(kind,kinds,'Katalog');
  const size=input.limit==null?24:integer(Number(input.limit),'Sayfa boyutu',1,100);
  const city=canonical(text(input.city??'','İl',0,60)),district=canonical(text(input.district??'','İlçe',0,60)),fallbackDistrict=canonical(text(input.fallbackDistrict??input.district??'','İlçe',0,60)),sport=input.sport?choice(input.sport,SPORTS,'Branş'):'';
  const latitude=input.latitude==null||input.latitude===''?null:Number(input.latitude),longitude=input.longitude==null||input.longitude===''?null:Number(input.longitude);
  ensure((latitude==null&&longitude==null)||(Number.isFinite(latitude)&&Math.abs(latitude)<=90&&Number.isFinite(longitude)&&Math.abs(longitude)<=180),400,'INVALID_LOCATION','Konum geçersiz.');
  const origin=latitude!=null?{latitude,longitude}:null;
  const radiusKm=input.radiusKm==null||input.radiusKm===''?null:Number(input.radiusKm);
  if(radiusKm!=null){ensure(Number.isFinite(radiusKm)&&radiusKm>0&&radiusKm<=500,400,'INVALID_INPUT','Mesafe sınırı geçersiz.');ensure(origin!=null||fallbackDistrict,400,'INVALID_LOCATION','Mesafe filtresi için konum seç.');}
  const scope=digest([kind,city,district,sport,origin?.latitude??null,origin?.longitude??null,radiusKm,fallbackDistrict]),cursor=input.after?openCursor(ctx,'catalog',input.after):null;
  if(cursor)ensure(cursor.scope===scope&&Array.isArray(cursor.last)&&cursor.last.length===2&&Number.isSafeInteger(cursor.last[0])&&typeof cursor.last[1]==='string'&&Number.isSafeInteger(cursor.watermark),400,'INVALID_CURSOR','Katalog filtreleri değişti. Baştan yenile.');
  const watermark=cursor?.watermark??ctx.now,rows=[];let position=cursor?.last??null,done=false;
  while(rows.length<=size&&!done){
    const params=[watermark];let sql=kind==='venues'?"SELECT c.* FROM venues c JOIN users owner ON owner.id=c.owner_id WHERE owner.status='active' AND owner.role IN ('admin','venue_owner') AND c.created_at<=?":`SELECT c.* FROM ${kind} c WHERE c.created_at<=?`;
    if(position){sql+=' AND (c.created_at<? OR (c.created_at=? AND c.id<?))';params.push(position[0],position[0],position[1]);}
    sql+=' ORDER BY c.created_at DESC,c.id DESC LIMIT 200';
    const batch=await ctx.db.all(sql,params);if(!batch.length)break;
    for(const record of batch){position=[Number(record.created_at),record.id];if(await visibleCatalogRecord(ctx,kind,record,{city,district,sport,origin,radiusKm,fallbackDistrict}))rows.push(record);if(rows.length>size)break;}
    done=batch.length<200;
  }
  const selected=rows.slice(0,size),items=[];
  for(const record of selected){const item=await catalogItem(ctx,kind,record,origin);if(item)items.push(item);}
  const refs=new Set(kind==='teams'?items.flatMap(item=>[item.captainId,...item.memberIds]):[]),players=[];
  for(const userId of refs)players.push(await publicPlayer(ctx,userId));
  const last=selected.at(-1);
  return{kind,items,players,serverTime:ctx.now,nextCursor:rows.length>size?sealCursor(ctx,'catalog',{scope,watermark,last:[Number(last.created_at),last.id]}):null};
}
