const slotTimeFormat = new Intl.DateTimeFormat('en-GB',{timeZone:'Europe/Istanbul',hour:'2-digit',minute:'2-digit',hourCycle:'h23'});
import { ensure, keys, integer, text, json } from './errors.mjs';
import { row, audit } from './model.mjs';
import { assertVenueOpen } from './operations.mjs';
import { networkHandlers } from './network.mjs';

const weekday = new Intl.DateTimeFormat('en-US',{timeZone:'Europe/Istanbul',weekday:'short'});
const weekdays=['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
export const venueScheduleHandlers={
 'venue.slot.bulk.create':async(ctx,p)=>{
  keys(p,['venueId','court','startsAtEpochMillis','days','weekdays','slotsPerDay','durationMin','priceMinor']);
  const venue=await row(ctx,'venues','id',p.venueId);assertVenueOpen(venue);
  ensure(venue.owner_id===ctx.actor.id||ctx.actor.role==='admin',403,'FORBIDDEN','Tesis yetkisi gerekiyor.');
  const start=integer(p.startsAtEpochMillis,'Başlangıç',ctx.now+60000,ctx.now+300*86400000),days=integer(p.days,'Gün sayısı',1,60),perDay=integer(p.slotsPerDay,'Günlük saat',1,12),duration=integer(p.durationMin,'Süre',15,240),price=integer(p.priceMinor,'Kuruş',100,100000000),court=text(p.court,'Saha',1,60);
  ensure(Array.isArray(p.weekdays)&&p.weekdays.length>0&&p.weekdays.length<=7&&new Set(p.weekdays).size===p.weekdays.length&&p.weekdays.every(d=>Number.isSafeInteger(d)&&d>=1&&d<=7),400,'INVALID_INPUT','Haftanın günlerini seç.');
  const local=slotTimeFormat.format(start).split(':').map(Number);
  ensure(local[0]*60+local[1]+perDay*duration<=1440,400,'INVALID_INPUT','Günlük saat aralığı gece yarısını geçemez.');
  const starts=[];for(let day=0;day<days;day++){
   const base=start+day*86400000;if(!p.weekdays.includes(weekdays.indexOf(weekday.format(base))+1))continue;
   for(let slot=0;slot<perDay;slot++)starts.push(base+slot*duration*60000);
  }
  ensure(starts.length>0&&starts.length<=120,400,'INVALID_INPUT','Plan 1–120 saat oluşturmalı; tarih veya gün seçimini daralt.');
  const ids=[];for(const startsAtEpochMillis of starts)ids.push((await networkHandlers['venue.slot.create'](ctx,{venueId:venue.id,court,startsAtEpochMillis,durationMin:duration,priceMinor:price})).id);
  await audit(ctx,'venue.slot.bulk.create',venue.id,{court,days,weekdays:p.weekdays,count:ids.length});
  return{ids,created:ids.length};
 }
};
