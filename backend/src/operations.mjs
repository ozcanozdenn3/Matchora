import { sealCursor,openCursor } from './opaque-cursor.mjs';
import { coarseMemoryLocation } from './memory-feed.mjs';
import { id, rateLimit } from './auth.mjs';
import { ensure, keys, text, integer, choice, json } from './errors.mjs';
import { row, audit, notify, adminRequired } from './model.mjs';
import { publicPlayer, dateLabel } from './queries.mjs';
import { SPORTS } from './sports.mjs';

const encode=JSON.stringify;
export function assertVenueOpen(venue){ensure(!json(venue.payload).archived,409,'VENUE_ARCHIVED','Bu tesis kapatıldı; yeni işlem yapılamaz.');}
export function venueAddress(value){const address=text(value??'','Açık tesis adresi',0,300);ensure(!address||address.length>=10,400,'INVALID_INPUT','Açık tesis adresi en az 10 karakter olmalı.');return address;}
function expectedVersion(data,value){ensure(integer(value,'Sürüm',1)===(data.version??1),409,'VERSION_CONFLICT','Kayıt değişti. Yenile ve tekrar dene.');}
async function ownedVenue(ctx,venueId){
  const venue=await row(ctx,'venues','id',venueId);
  ensure(['admin','venue_owner'].includes(ctx.actor.role)&&(venue.owner_id===ctx.actor.id||ctx.actor.role==='admin'),403,'FORBIDDEN','Tesis yetkisi gerekiyor.');
  return venue;
}
const courtKey=value=>value.normalize('NFKC').trim().replace(/\s+/g,' ').toLocaleLowerCase('tr');
export async function assertSlotNoOverlap(ctx,venueId,court,start,end,excludeId=''){
  const others=await ctx.db.all("SELECT payload,starts_at,ends_at FROM venue_slots WHERE venue_id=? AND starts_at<? AND ends_at>? AND id<>? AND status<>'cancelled'",[venueId,end,start,excludeId]);
  ensure(!others.some(s=>courtKey(json(s.payload).court??'')===courtKey(court)),409,'SLOT_OVERLAP','Bu sahanın saatleri çakışıyor.');
  const venue=await row(ctx,'venues','id',venueId),capacity=json(venue.payload).courts??1;
  const points=[{at:start,change:1},{at:end,change:-1},...others.flatMap(s=>[{at:Math.max(start,Number(s.starts_at)),change:1},{at:Math.min(end,Number(s.ends_at)),change:-1}])].sort((a,b)=>a.at-b.at||a.change-b.change);
  let concurrent=0;for(const point of points){concurrent+=point.change;ensure(concurrent<=capacity,409,'COURT_CAPACITY','Bu saat aralığında tesisin saha/kort sayısı aşılıyor. Önce saha sayısını veya saatleri düzenle.');}
}
async function slotHasBooking(ctx,slotId){return ctx.db.get("SELECT id FROM bookings WHERE slot_id=? AND status IN ('pending','confirmed')",[slotId]);}
function slotFields(ctx,p){
  const start=integer(p.startsAtEpochMillis,'Başlangıç',ctx.now+60000,ctx.now+365*86400000),duration=integer(p.durationMin,'Süre',15,720);
  return{start,end:start+duration*60000,duration,court:text(p.court,'Saha',1,60),price:integer(p.priceMinor,'Kuruş',100,100000000)};
}

export async function operatorVenues(ctx) {
  ensure(ctx.actor && ['admin', 'venue_owner'].includes(ctx.actor.role), 403, 'FORBIDDEN', 'Tesis yetkisi gerekiyor.');
  const newest = await ctx.db.all('SELECT * FROM venues WHERE owner_id=? OR ?=1 ORDER BY created_at DESC LIMIT 100', [ctx.actor.id, ctx.actor.role === 'admin' ? 1 : 0]);
  const active = await ctx.db.all(`SELECT v.* FROM venues v WHERE (v.owner_id=? OR ?=1) AND
    (EXISTS(SELECT 1 FROM venue_slots s WHERE s.venue_id=v.id AND s.ends_at>? AND s.status='open') OR
     EXISTS(SELECT 1 FROM bookings b JOIN venue_slots s ON s.id=b.slot_id WHERE s.venue_id=v.id AND b.status IN ('pending','confirmed')))`, [ctx.actor.id, ctx.actor.role === 'admin' ? 1 : 0, ctx.now]);
  const venues = [...new Map([...newest, ...active].map(v => [v.id, v])).values()];
  const items = [];
  for (const venue of venues) {
    const slots = await ctx.db.all(`SELECT * FROM venue_slots WHERE venue_id=? AND (ends_at>? OR
      id IN (SELECT id FROM venue_slots WHERE venue_id=? ORDER BY starts_at DESC,id LIMIT 100)) ORDER BY starts_at,id`, [venue.id, ctx.now, venue.id]);
    const bookings = await ctx.db.all(`SELECT b.*,p.status AS payment_status,s.starts_at,s.ends_at FROM bookings b JOIN venue_slots s ON s.id=b.slot_id
      JOIN venue_payments p ON p.booking_id=b.id WHERE s.venue_id=? AND (b.status IN ('pending','confirmed') OR
      b.id IN (SELECT b2.id FROM bookings b2 JOIN venue_slots s2 ON s2.id=b2.slot_id WHERE s2.venue_id=? ORDER BY b2.created_at DESC,b2.id LIMIT 100)) ORDER BY b.created_at DESC,b.id`, [venue.id, venue.id]);
    for (const booking of bookings) booking.customerName = (await publicPlayer(ctx, booking.user_id)).name;
    const data=json(venue.payload);
    items.push({ ...data, version:data.version??1, id: venue.id, ownerId: venue.owner_id,
      slots: slots.map(s => ({ ...s, ...json(s.payload), version:json(s.payload).version??1, startsAt: Number(s.starts_at), priceMinor: Number(s.price_minor) })), bookings });
  }
  return { items };
}

export async function venueReviews(ctx,venueId,filters={}) {
  await row(ctx,'venues','id',venueId);
  const cursor=filters.after?openCursor(ctx,'venue-reviews',filters.after):null;
  if(cursor)ensure(cursor.venueId===venueId&&Number.isSafeInteger(cursor.createdAt)&&typeof cursor.id==='string',400,'INVALID_CURSOR','Değerlendirme sayfası bu tesise ait değil.');
  const params=[venueId,ctx.actor?.id??''];let where="r.venue_id=? AND (r.status='published' OR (r.user_id=? AND r.status='pending'))";
  if(ctx.actor){where+=' AND NOT EXISTS(SELECT 1 FROM blocks b WHERE (b.user_id=? AND b.target_id=r.user_id) OR (b.user_id=r.user_id AND b.target_id=?))';params.push(ctx.actor.id,ctx.actor.id);}
  if(cursor){where+=' AND (r.created_at<? OR (r.created_at=? AND r.id<?))';params.push(cursor.createdAt,cursor.createdAt,cursor.id);}
  const size=Math.min(100,Math.max(1,Math.floor(Number(filters.limit)||50)));
  const rows=await ctx.db.all(`SELECT r.* FROM venue_reviews r JOIN users u ON u.id=r.user_id WHERE u.status='active' AND ${where} ORDER BY r.created_at DESC,r.id DESC LIMIT ?`,[...params,size+1]),selected=rows.slice(0,size),items=[];
  for(const review of selected)items.push({id:review.id,authorId:review.user_id,authorName:(await publicPlayer(ctx,review.user_id)).name,rating:review.rating,comment:review.comment,status:review.status,createdAt:Number(review.created_at),own:ctx.actor?.id===review.user_id});
  const last=selected.at(-1);
  const reviewableBookings=[];
  if(ctx.actor){const venue=await row(ctx,'venues','id',venueId);for(const booking of await ctx.db.all("SELECT b.*,s.starts_at,s.payload FROM bookings b JOIN venue_slots s ON s.id=b.slot_id JOIN venue_payments p ON p.booking_id=b.id WHERE b.user_id=? AND s.venue_id=? AND b.status='completed' AND p.status='paid' ORDER BY b.created_at DESC,b.id DESC LIMIT 100",[ctx.actor.id,venueId]))reviewableBookings.push({id:booking.id,venue:json(venue.payload).name,description:`${json(booking.payload).court} · ${dateLabel(booking.starts_at)}`,amountMinor:Number(booking.amount_minor),amountTry:Math.ceil(booking.amount_minor/100),method:'Tesiste ödeme',time:dateLabel(booking.created_at),status:'PAID',bookingStatus:'completed',bookingKey:`${venueId}:${booking.slot_id}`});}
  return{items,reviewableBookings,serverTime:ctx.now,nextCursor:rows.length>size?sealCursor(ctx,'venue-reviews',{venueId,createdAt:Number(last.created_at),id:last.id}):null};
}

export const operationHandlers = {
  'venue.update':async(ctx,p)=>{
    keys(p,['venueId','expectedVersion','name','city','area','address','sports','amenities','courts','cancellationHours','approximateLocation','phone','description']);
    const venue=await ownedVenue(ctx,p.venueId),data=json(venue.payload);assertVenueOpen(venue);expectedVersion(data,p.expectedVersion);
    ensure(Array.isArray(p.sports)&&p.sports.length>0&&p.sports.length<=20&&new Set(p.sports).size===p.sports.length);
    const amenities=p.amenities??data.amenities??[];ensure(Array.isArray(amenities)&&amenities.length<=30);
    const next={...data,name:text(p.name,'Tesis adı',3,100),city:text(p.city,'İl',2,60),area:text(p.area,'Bölge',2,80),address:venueAddress(p.address===undefined?data.address:p.address),
      sports:p.sports.map(s=>choice(s,SPORTS,'Branş')),amenities:amenities.map(a=>text(a,'Olanak',1,60)),courts:integer(p.courts,'Saha sayısı',1,50),cancellationHours:integer(p.cancellationHours,'İptal süresi',0,168),
      approximateLocation:p.approximateLocation===undefined?data.approximateLocation:coarseMemoryLocation(p.approximateLocation),
      phone:p.phone===undefined?(data.phone??''):text(p.phone,'Telefon',0,40),
      description:p.description===undefined?(data.description??''):text(p.description,'Açıklama',0,1000),
      version:(data.version??1)+1};
    const changesLocation=next.city!==data.city||next.area!==data.area||next.address!==(data.address??'')||next.courts!==data.courts||JSON.stringify([...next.sports].sort())!==JSON.stringify([...(data.sports??[])].sort());
    if(changesLocation)ensure(!await ctx.db.get("SELECT b.id FROM bookings b JOIN venue_slots s ON s.id=b.slot_id WHERE s.venue_id=? AND b.status IN ('pending','confirmed') LIMIT 1",[venue.id]),409,'ACTIVE_BOOKING','Aktif rezervasyon varken tesisin konumu, branşı veya saha sayısı değiştirilemez.');
    await ctx.db.run('UPDATE venues SET payload=? WHERE id=?',[encode(next),venue.id]);await audit(ctx,'venue.update',venue.id);return{updated:true,version:next.version};
  },
  'venue.archive':async(ctx,p)=>{
    keys(p,['venueId','expectedVersion']);const venue=await ownedVenue(ctx,p.venueId),data=json(venue.payload);assertVenueOpen(venue);expectedVersion(data,p.expectedVersion);
    const bookings=await ctx.db.all("SELECT b.*,s.starts_at,s.ends_at,p.status AS payment_status FROM bookings b JOIN venue_slots s ON s.id=b.slot_id JOIN venue_payments p ON p.booking_id=b.id WHERE s.venue_id=? AND b.status IN ('pending','confirmed')",[venue.id]);
    ensure(!bookings.some(b=>b.payment_status==='paid'),409,'PAID_BOOKING','Ödenmiş aktif rezervasyonları sonuçlandır veya gerçek iadelerini kaydet.');
    ensure(!bookings.some(b=>Number(b.starts_at)<=ctx.now&&Number(b.ends_at)>ctx.now),409,'ACTIVE_BOOKING','Devam eden rezervasyon bitince tesisi kapatabilirsin.');
    for(const booking of bookings.filter(b=>Number(b.starts_at)>ctx.now)){
      await ctx.db.run("UPDATE bookings SET status='cancelled' WHERE id=?",[booking.id]);await ctx.db.run("UPDATE venue_payments SET status='cancelled' WHERE booking_id=?",[booking.id]);
      await notify(ctx,booking.user_id,'matches','Tesis rezervasyonu iptal edildi',`${data.name} yeni rezervasyonlara kapatıldı. Tahsilat yapılmamış rezervasyonun sonlandırıldı.`,'RESERVATIONS',booking.id,`venue-archive:${venue.id}:${booking.id}`);
      await audit(ctx,'booking.cancel',booking.id,{reason:'venue.archive',venueId:venue.id});
    }
    for(const slot of await ctx.db.all("SELECT id,payload FROM venue_slots WHERE venue_id=? AND starts_at>? AND status='open'",[venue.id,ctx.now])){
      const payload=json(slot.payload);await ctx.db.run("UPDATE venue_slots SET status='cancelled',payload=? WHERE id=?",[encode({...payload,version:(payload.version??1)+1}),slot.id]);
    }
    const next={...data,archived:true,version:(data.version??1)+1};await ctx.db.run('UPDATE venues SET payload=? WHERE id=?',[encode(next),venue.id]);
    await audit(ctx,'venue.archive',venue.id);return{archived:true,version:next.version};
  },
  'venue.slot.update':async(ctx,p)=>{
    keys(p,['slotId','expectedVersion','court','startsAtEpochMillis','durationMin','priceMinor']);
    const slot=await row(ctx,'venue_slots','id',p.slotId),venue=await ownedVenue(ctx,slot.venue_id),data=json(slot.payload);assertVenueOpen(venue);expectedVersion(data,p.expectedVersion);
    ensure(slot.status==='open'&&Number(slot.starts_at)>ctx.now,409,'SLOT_CLOSED','Yalnız gelecekteki açık saat düzenlenebilir.');
    ensure(!await slotHasBooking(ctx,slot.id),409,'ACTIVE_BOOKING','Rezervasyonlu saatin zamanı veya bedeli değiştirilemez.');
    const fields=slotFields(ctx,p);await assertSlotNoOverlap(ctx,venue.id,fields.court,fields.start,fields.end,slot.id);
    const next={...data,court:fields.court,durationMin:fields.duration,version:(data.version??1)+1};
    await ctx.db.run('UPDATE venue_slots SET starts_at=?,ends_at=?,price_minor=?,payload=? WHERE id=?',[fields.start,fields.end,fields.price,encode(next),slot.id]);
    await audit(ctx,'venue.slot.update',slot.id);return{updated:true,version:next.version};
  },
  'venue.slot.reopen':async(ctx,p)=>{
    keys(p,['slotId','expectedVersion']);const slot=await row(ctx,'venue_slots','id',p.slotId),venue=await ownedVenue(ctx,slot.venue_id),data=json(slot.payload);assertVenueOpen(venue);expectedVersion(data,p.expectedVersion);
    ensure(slot.status==='cancelled'&&Number(slot.starts_at)>ctx.now,409,'SLOT_CLOSED','Yalnız gelecekteki kapatılmış saat yeniden açılabilir.');
    ensure(!await slotHasBooking(ctx,slot.id),409,'ACTIVE_BOOKING','Bu saatin aktif rezervasyonu var.');
    await assertSlotNoOverlap(ctx,venue.id,data.court,Number(slot.starts_at),Number(slot.ends_at),slot.id);
    const next={...data,version:(data.version??1)+1};await ctx.db.run("UPDATE venue_slots SET status='open',payload=? WHERE id=?",[encode(next),slot.id]);
    await audit(ctx,'venue.slot.reopen',slot.id);return{reopened:true,version:next.version};
  },
  'admin.announcement.send': async (ctx, p) => {
    adminRequired(ctx); keys(p, ['title', 'body', 'city']);
    const title = text(p.title, 'Başlık', 3, 120), body = text(p.body, 'Duyuru', 10, 1000);
    const city = text(p.city ?? '', 'İl', 0, 60);
    await rateLimit(ctx.db, `announcement:${ctx.actor.id}`, ctx.now, 1, 86400000);
    const users = await ctx.db.all("SELECT p.user_id,p.payload,n.notifications FROM profiles p JOIN preferences n ON n.user_id=p.user_id JOIN users u ON u.id=p.user_id WHERE u.status='active'");
    let recipients = 0;
    for (const user of users) {
      if (json(user.notifications).marketing !== true) continue;
      if (city && json(user.payload).city.toLocaleLowerCase('tr') !== city.toLocaleLowerCase('tr')) continue;
      await notify(ctx, user.user_id, 'marketing', title, body, 'PASSPORT', '', `announcement:${ctx.requestKey}:${user.user_id}`);
      recipients++;
    }
    await audit(ctx, 'admin.announcement.send', ctx.requestKey, { recipients });
    return { recipients };
  },
  'venue.review.save': async (ctx, p) => {
    keys(p, ['bookingId', 'rating', 'comment', 'expectedVersion']);
    const booking = await row(ctx, 'bookings', 'id', p.bookingId), slot = await row(ctx, 'venue_slots', 'id', booking.slot_id);
    ensure(booking.user_id === ctx.actor.id && booking.status === 'completed', 403, 'COMPLETED_BOOKING_REQUIRED', 'Yalnız kendi tamamlanmış rezervasyonunu değerlendirebilirsin.');
    const old = await ctx.db.get('SELECT * FROM venue_reviews WHERE booking_id=?', [booking.id]);
    if (old) ensure(p.expectedVersion === old.version, 409, 'VERSION_CONFLICT', 'Değerlendirme güncellendi. Yenileyip tekrar dene.');
    const reviewId = old?.id ?? id(), rating = integer(p.rating, 'Puan', 1, 5), comment = text(p.comment ?? '', 'Yorum', 0, 1000);
    await ctx.db.run(`INSERT INTO venue_reviews(id,booking_id,venue_id,user_id,rating,comment,status,version,created_at,updated_at) VALUES(?,?,?,?,?,?,'pending',1,?,?)
      ON CONFLICT(booking_id) DO UPDATE SET rating=?,comment=?,status='pending',version=venue_reviews.version+1,updated_at=?`,
    [reviewId, booking.id, slot.venue_id, ctx.actor.id, rating, comment, ctx.now, ctx.now, rating, comment, ctx.now]);
    await audit(ctx, 'venue.review.save', reviewId); return { id: reviewId, status: 'pending' };
  },
  'venue.review.delete': async (ctx, p) => {
    keys(p, ['reviewId']); const review = await row(ctx, 'venue_reviews', 'id', p.reviewId);
    ensure(review.user_id === ctx.actor.id || ctx.actor.role === 'admin', 403, 'FORBIDDEN', 'Değerlendirme sana ait değil.');
    await ctx.db.run("UPDATE venue_reviews SET comment='',status='deleted',version=version+1,updated_at=? WHERE id=?", [ctx.now, review.id]);
    await audit(ctx, 'venue.review.delete', review.id); return { deleted: true };
  },
  'admin.venue.review': async (ctx, p) => {
    adminRequired(ctx); keys(p, ['reviewId', 'approved', 'reason']); ensure(typeof p.approved === 'boolean');
    const review = await row(ctx, 'venue_reviews', 'id', p.reviewId); ensure(review.status === 'pending', 409, 'INVALID_STATE', 'Değerlendirme işlendi.');
    await ctx.db.run('UPDATE venue_reviews SET status=?,version=version+1,updated_at=? WHERE id=?', [p.approved ? 'published' : 'rejected', ctx.now, review.id]);
    await audit(ctx, 'admin.venue.review', review.id, { approved: p.approved, reason: text(p.reason, 'Gerekçe', 10, 1000) });
    return { approved: p.approved };
  },
  'booking.refund.record': async (ctx, p) => {
    keys(p, ['bookingId', 'reference']); const booking = await row(ctx, 'bookings', 'id', p.bookingId), slot = await row(ctx, 'venue_slots', 'id', booking.slot_id), venue = await row(ctx, 'venues', 'id', slot.venue_id);
    ensure(venue.owner_id === ctx.actor.id || ctx.actor.role === 'admin', 403, 'FORBIDDEN', 'Tesis yetkisi gerekiyor.');
    const payment = await ctx.db.get('SELECT * FROM venue_payments WHERE booking_id=?', [booking.id]);
    ensure(payment?.status === 'paid' && ['confirmed', 'completed'].includes(booking.status), 409, 'INVALID_STATE', 'Ödenmiş rezervasyon gerekiyor.');
    const reference = text(p.reference, 'İade referansı', 3, 120);
    await ctx.db.run("UPDATE venue_payments SET status='refunded',provider_ref=? WHERE booking_id=?", [reference, booking.id]);
    await ctx.db.run("UPDATE bookings SET status='cancelled' WHERE id=?", [booking.id]);
    await audit(ctx, 'booking.refund.record', booking.id, { reference, paymentReference: payment.provider_ref ?? null, amountMinor: Number(booking.amount_minor) });
    await notify(ctx, booking.user_id, 'wallet', 'Tesis iade kaydı oluşturdu', `${json(venue.payload).name} · Referans ${reference}`, 'ACCOUNT', booking.id, `refund:${booking.id}`);
    return { recorded: true, method: 'offline_refund_record' };
  },
  'admin.operator.assign': async (ctx, p) => {
    adminRequired(ctx); keys(p, ['userId', 'role', 'reason']); const user = await row(ctx, 'users', 'id', p.userId);
    ensure(user.status === 'active' && user.role !== 'admin', 403, 'FORBIDDEN', 'Aktif, yönetici olmayan kullanıcı seç.');
    const role = choice(p.role, ['user', 'venue_owner'], 'Yetki');
    await ctx.db.run('UPDATE users SET role=? WHERE id=?', [role, user.id]);
    await audit(ctx, 'admin.operator.assign', user.id, { role, reason: text(p.reason, 'Gerekçe', 10, 1000) }); return { role };
  },
  'admin.outbox.retry': async (ctx, p) => {
    adminRequired(ctx); keys(p, ['outboxId']); const job = await row(ctx, 'outbox', 'id', p.outboxId);
    ensure(job.status === 'failed', 409, 'INVALID_STATE', 'Yalnız başarısız bildirim tekrar denenebilir.');
    await ctx.db.run("UPDATE outbox SET status='pending',attempts=0,next_at=? WHERE id=?", [ctx.now, job.id]);
    await audit(ctx, 'admin.outbox.retry', job.id); return { queued: true };
  }
};
