import { saveMeetingLocation } from './event-locations.mjs';
import { ensure,keys,text,integer,choice,json } from './errors.mjs';
import { row,ownedEvent,audit,encode,notify,release,ACTIVE_EVENTS,offerWaitlist } from './model.mjs';
import { canonicalTurkishCity } from './turkish-locale.mjs';

export const managementHandlers={
 'event.update':async(ctx,p)=>{
  keys(p,['eventId','expectedVersion','title','venue','description','startsAtEpochMillis','capacity','approximateLocation','durationMin','pricePerPersonTry','levelRange','minReliability','verifiedOnly','neededRoles','emergency','approvalRequired','mode','format','gamePlatform','gameServer','voiceChannel','hostGameTag']);const event=await ownedEvent(ctx,p.eventId);
  ensure(ACTIVE_EVENTS.includes(event.status)&&Number(event.starts_at)>ctx.now,409,'EVENT_CLOSED','Başlamış veya kapalı maç düzenlenemez.');
  ensure(p.expectedVersion===event.version,409,'VERSION_CONFLICT','Maç güncellendi. Yenile ve tekrar dene.');
  ensure(!await ctx.db.get('SELECT id FROM tournament_fixtures WHERE event_id=?',[event.id])&&!await ctx.db.get('SELECT id FROM challenges WHERE event_id=?',[event.id]),409,'OFFICIAL_EVENT','Resmi maçın planı organizasyon üzerinden yönetilmeli.');
  const count=await ctx.db.get("SELECT COUNT(*) AS n FROM event_members WHERE event_id=? AND status='joined'",[event.id]);
  const start=p.startsAtEpochMillis==null?Number(event.starts_at):integer(p.startsAtEpochMillis,'Başlangıç',ctx.now+60000,ctx.now+365*86400000),capacity=p.capacity==null?event.capacity:integer(p.capacity,'Kontenjan',2,100);
  const currentDuration=Math.max(15,Math.round((Number(event.ends_at)-Number(event.starts_at))/60000)),duration=p.durationMin==null?currentDuration:integer(p.durationMin,'Süre',15,720);
  const reserved=await ctx.db.get("SELECT COUNT(*) AS n FROM waitlist WHERE event_id=? AND status='offered' AND offered_until>?",[event.id,ctx.now]);ensure(capacity>=count.n+reserved.n,409,'CAPACITY_BELOW_ROSTER','Kontenjan kadrodan ve ayrılmış yerlerden küçük olamaz.');
  if(p.neededRoles!==undefined)ensure(Array.isArray(p.neededRoles)&&p.neededRoles.length<=10,400,'INVALID_INPUT','Aranan roller geçersiz.');
  const data=json(event.payload),next={...data,durationMin:duration,
   ...(p.title!=null?{title:text(p.title,'Başlık',3,100)}:{}),
   ...(p.venue!=null?{venue:text(p.venue,'Saha',2,160)}:{}),
   ...(p.description!=null?{description:text(p.description,'Açıklama',0,2000)}:{}),
   ...(p.pricePerPersonTry!=null?{pricePerPersonTry:integer(p.pricePerPersonTry,'Saha ücreti',0,100000)}:{}),
   ...(p.levelRange!=null?{levelRange:text(p.levelRange,'Seviye',1,80)}:{}),
   ...(p.minReliability!=null?{minReliability:integer(p.minReliability,'Güvenilirlik',0,100)}:{}),
   ...(p.verifiedOnly!=null?{verifiedOnly:Boolean(p.verifiedOnly)}:{}),
   ...(p.neededRoles!=null?{neededRoles:p.neededRoles.map(v=>text(v,'Rol',1,80))}:{}),
   ...(p.emergency!=null?{emergency:Boolean(p.emergency)}:{}),
   ...(p.approvalRequired!=null?{approvalRequired:Boolean(p.approvalRequired)}:{}),
   ...(p.mode!=null?{mode:choice(p.mode,['SOCIAL','COMPETITIVE'],'Oyun modu')}:{}),
   ...(p.format!=null?{format:text(p.format,'Format',1,80)}:{}),
   ...(p.gamePlatform!=null?{gamePlatform:text(p.gamePlatform,'Platform',0,80)}:{}),
   ...(p.gameServer!=null?{gameServer:text(p.gameServer,'Sunucu',0,80)}:{}),
   ...(p.voiceChannel!=null?{voiceChannel:text(p.voiceChannel,'Ses kanalı',0,200)}:{}),
   ...(p.hostGameTag!=null?{hostGameTag:text(p.hostGameTag,'Oyuncu etiketi',0,80)}:{})
  };
  await ctx.db.run('UPDATE events SET payload=?,capacity=?,starts_at=?,ends_at=?,status=?,version=version+1 WHERE id=?',[encode(next),capacity,start,start+duration*60000,count.n>=capacity?'FULL':'FILLING',event.id]);
  if(next.title!==data.title)await ctx.db.run('UPDATE threads SET title=? WHERE event_id=?',[next.title,event.id]);
  if(p.approximateLocation!=null){await saveMeetingLocation(ctx,event.id,p.approximateLocation);await ctx.db.run('UPDATE events SET latitude=?,longitude=? WHERE id=?',[Math.round(p.approximateLocation.latitude*100)/100,Math.round(p.approximateLocation.longitude*100)/100,event.id]);}
  else if(next.venue!==data.venue){await saveMeetingLocation(ctx,event.id,null);await ctx.db.run('UPDATE events SET latitude=NULL,longitude=NULL WHERE id=?',[event.id]);}
  for(const member of await ctx.db.all("SELECT user_id FROM event_members WHERE event_id=? AND status='joined' AND user_id<>?",[event.id,ctx.actor.id]))await notify(ctx,member.user_id,'matches','Maç planı güncellendi',next.title,'EVENT',event.id,`event-updated:${event.id}:${event.version}:${member.user_id}`);
  if(capacity>event.capacity)await offerWaitlist(ctx,event.id);
  await audit(ctx,'event.update',event.id);return{updated:true};
 },
 'challenge.cancel':async(ctx,p)=>{keys(p,['challengeId']);const item=await row(ctx,'challenges','id',p.challengeId),team=await row(ctx,'teams','id',item.from_team);ensure(team.captain_id===ctx.actor.id,403,'FORBIDDEN','Gönderen takım kaptanı olmalısın.');ensure(item.status==='PENDING',409,'INVALID_STATE','Yalnız bekleyen teklif geri çekilebilir.');await ctx.db.run("UPDATE challenges SET status='DECLINED' WHERE id=?",[item.id]);await release(ctx,item.created_by,`challenge:${item.id}`);await audit(ctx,'challenge.cancel',item.id);return{cancelled:true};},
 'team.request.cancel':async(ctx,p)=>{keys(p,['requestId']);const request=await row(ctx,'join_requests','id',p.requestId);ensure(request.scope==='team'&&request.user_id===ctx.actor.id,403,'FORBIDDEN','Başvuru sana ait değil.');ensure(request.status==='pending',409,'INVALID_STATE','Başvuru beklemede değil.');await ctx.db.run("UPDATE join_requests SET status='cancelled' WHERE id=?",[request.id]);return{cancelled:true};},
 'team.update':async(ctx,p)=>{
  const fields=['name','badge','colorHex','city','area','requiredMmr','availability','description','recruitingOpen','homeVenue','networkScope','coCaptainId','substituteIds','achievements'];keys(p,['teamId','expectedVersion',...fields]);
  const team=await row(ctx,'teams','id',p.teamId),data=json(team.payload);ensure(!data.archived,409,'TEAM_ARCHIVED','Bu takım kapatıldı.');ensure(team.captain_id===ctx.actor.id,403,'FORBIDDEN','Kaptan yetkisi gerekiyor.');
  if(p.expectedVersion!=null)ensure(integer(p.expectedVersion,'Sürüm',1)===(data.version??1),409,'VERSION_CONFLICT','Takım güncellendi. Yenile ve tekrar dene.');
  ensure(fields.some(field=>p[field]!==undefined),400,'INVALID_INPUT','Güncellenecek takım bilgisini seç.');const next={...data,version:(data.version??1)+1};
  for(const [field,label,min,max] of [['name','Takım adı',3,60],['badge','Amblem',1,12],['city','İl',2,60],['area','Bölge',2,60],['availability','Müsaitlik',0,200],['description','Açıklama',0,1000],['homeVenue','Ev sahası',0,160]])if(p[field]!==undefined)next[field]=field==='city'?canonicalTurkishCity(text(p[field],label,min,max)):text(p[field],label,min,max);
  if(p.colorHex!==undefined){ensure(typeof p.colorHex==='string'&&/^#[a-fA-F0-9]{8}$/.test(p.colorHex),400,'INVALID_INPUT','Takım rengi geçersiz.');next.colorHex=p.colorHex;}
  if(p.requiredMmr!==undefined)next.requiredMmr=integer(p.requiredMmr,'MMR',0,3000);
  if(p.recruitingOpen!==undefined){ensure(typeof p.recruitingOpen==='boolean');next.recruitingOpen=p.recruitingOpen;}
  if(p.networkScope!==undefined)next.networkScope=choice(p.networkScope,['CITY','CAMPUS','COMPANY','TRAVEL'],'Kapsam');
  if(p.coCaptainId!==undefined)next.coCaptainId=p.coCaptainId?text(p.coCaptainId,'İkinci kaptan',2,80):null;
  if(p.substituteIds!==undefined){ensure(Array.isArray(p.substituteIds)&&p.substituteIds.length<=50);next.substituteIds=p.substituteIds.map(v=>text(v,'Yedek',2,80));}
  if(p.achievements!==undefined){ensure(Array.isArray(p.achievements)&&p.achievements.length<=50);next.achievements=p.achievements.map(v=>text(v,'Başarı',1,120));}
  await ctx.db.run('UPDATE teams SET payload=? WHERE id=?',[encode(next),team.id]);if(p.name!==undefined)await ctx.db.run('UPDATE threads SET title=? WHERE team_id=?',[next.name,team.id]);
  await audit(ctx,'team.update',team.id);return{updated:true,version:next.version};
 },
 'tournament.update':async(ctx,p)=>{
  const fields=['name','dates','requirement','feeNote','venue','city','area','networkScope','networkName','communityId','gamePlatform','serverRegion','minRating','verifiedOnly','prizeToken','prizeCash','format','organizerName'];
  keys(p,['tournamentId','expectedVersion',...fields]);
  const t=await row(ctx,'tournaments','id',p.tournamentId),data=json(t.payload);
  ensure(t.creator_id===ctx.actor.id,403,'FORBIDDEN','Organizatör yetkisi gerekiyor.');
  ensure(['open','running'].includes(t.status),409,'INVALID_STATE','Tamamlanmış veya iptal edilmiş turnuva düzenlenemez.');
  if(p.expectedVersion!=null)ensure(integer(p.expectedVersion,'Sürüm',1)===(data.version??1),409,'VERSION_CONFLICT','Turnuva güncellendi. Yenile ve tekrar dene.');
  const next={...data,version:(data.version??1)+1};
  for(const [field,label,min,max] of [['name','Turnuva adı',3,100],['dates','Tarihler',2,160],['requirement','Koşullar',0,500],['feeNote','Ücret notu',0,300],['venue','Saha',0,160],['city','İl',2,60],['area','İlçe',2,60],['networkName','Ağ adı',0,120],['gamePlatform','Platform',0,100],['serverRegion','Sunucu / lobi',0,160],['prizeToken','Token ödülü',0,80],['prizeCash','Ödül',0,120],['format','Format',2,80],['organizerName','Organizatör adı',0,100]])if(p[field]!==undefined)next[field]=field==='city'?canonicalTurkishCity(text(p[field],label,min,max)):text(p[field],label,min,max);
  if(p.networkScope!==undefined)next.networkScope=choice(p.networkScope,['CITY','CAMPUS','COMPANY','TRAVEL'],'Kapsam');
  if(p.communityId!==undefined)next.communityId=p.communityId?text(p.communityId,'Topluluk',2,80):null;
  if(p.minRating!==undefined)next.minRating=integer(p.minRating,'En az MMR',0,3000);
  if(p.verifiedOnly!==undefined)next.verifiedOnly=Boolean(p.verifiedOnly);
  await ctx.db.run('UPDATE tournaments SET payload=? WHERE id=?',[encode(next),t.id]);
  await audit(ctx,'tournament.update',t.id);return{updated:true,version:next.version};
 },
 'community.transfer':async(ctx,p)=>{keys(p,['communityId','userId']);const community=await row(ctx,'communities','id',p.communityId);ensure(!json(community.payload).archived,409,'COMMUNITY_ARCHIVED','Bu topluluk kapatıldı.');ensure(community.owner_id===ctx.actor.id,403,'FORBIDDEN','Yönetici yetkisi gerekiyor.');const user=await row(ctx,'users','id',p.userId);ensure(user.status==='active'&&await ctx.db.get("SELECT user_id FROM community_members WHERE community_id=? AND user_id=? AND status='joined'",[community.id,p.userId]),409,'MEMBERSHIP_REQUIRED','Yeni yönetici aktif üye olmalı.');await ctx.db.run('UPDATE communities SET owner_id=? WHERE id=?',[p.userId,community.id]);await notify(ctx,p.userId,'social','Topluluk yöneticiliği sana devredildi',json(community.payload).name,'NETWORK',community.id,`community-transfer:${community.id}:${ctx.requestKey}`);await audit(ctx,'community.transfer',community.id,{to:p.userId});return{transferred:true};},
 'community.invite.decline':async(ctx,p)=>{keys(p,['communityId']);await ctx.db.run("UPDATE community_members SET status='left' WHERE community_id=? AND user_id=? AND status='invited'",[p.communityId,ctx.actor.id]);return{declined:true};},
 'venue.slot.cancel':async(ctx,p)=>{keys(p,['slotId','expectedVersion']);const slot=await row(ctx,'venue_slots','id',p.slotId),venue=await row(ctx,'venues','id',slot.venue_id),data=json(slot.payload);ensure(venue.owner_id===ctx.actor.id||ctx.actor.role==='admin',403,'FORBIDDEN','Tesis yetkisi gerekiyor.');if(p.expectedVersion!=null)ensure(p.expectedVersion===(data.version??1),409,'VERSION_CONFLICT','Saat değişti. Yenile ve tekrar dene.');ensure(slot.status==='open',409,'SLOT_CLOSED','Bu saat zaten kapatılmış.');ensure(!await ctx.db.get("SELECT id FROM bookings WHERE slot_id=? AND status IN ('pending','confirmed')",[slot.id]),409,'ACTIVE_BOOKING','Önce mevcut rezervasyonu sonuçlandırmalısın.');const next={...data,version:(data.version??1)+1};await ctx.db.run("UPDATE venue_slots SET status='cancelled',payload=? WHERE id=?",[encode(next),slot.id]);await audit(ctx,'venue.slot.cancel',slot.id);return{cancelled:true,version:next.version};},
 'booking.complete':async(ctx,p)=>{keys(p,['bookingId']);const booking=await row(ctx,'bookings','id',p.bookingId),slot=await row(ctx,'venue_slots','id',booking.slot_id),venue=await row(ctx,'venues','id',slot.venue_id);ensure(venue.owner_id===ctx.actor.id||ctx.actor.role==='admin',403,'FORBIDDEN','Tesis yetkisi gerekiyor.');ensure(booking.status==='confirmed'&&Number(slot.ends_at)<=ctx.now,409,'INVALID_STATE','Onaylı rezervasyonun saati tamamlanmış olmalı.');const payment=await ctx.db.get('SELECT status FROM venue_payments WHERE booking_id=?',[booking.id]);ensure(payment?.status==='paid',409,'PAYMENT_PENDING','Ödeme kaydı tamamlanmalı.');await ctx.db.run("UPDATE bookings SET status='completed' WHERE id=?",[booking.id]);await notify(ctx,booking.user_id,'matches','Tesis kullanımın tamamlandı',json(venue.payload).name,'RESERVATIONS',booking.id,`booking-complete:${booking.id}`);await audit(ctx,'booking.complete',booking.id);return{completed:true};}
};
