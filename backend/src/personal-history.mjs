import { ensure, choice, keys, json } from './errors.mjs';
import { dateLabel,timeLabel } from './queries.mjs';
import { SPORTS } from './sports.mjs';
import { sealCursor, openCursor } from './opaque-cursor.mjs';

const kinds = ['matches','ratings','reliability','wallet','reservations','support','reports'];

function reservationItem(record) {
  return {id:record.id,venue:json(record.venue_payload).name,
    description:`${json(record.slot_payload).court} · ${dateLabel(record.starts_at)} ${timeLabel(record.starts_at)}`,
    amountMinor:Number(record.amount_minor),amountTry:Math.ceil(Number(record.amount_minor)/100),
    method:'Tesiste ödeme',time:dateLabel(record.created_at),bookingStatus:record.booking_status,
    status:record.payment_status==='refunded'?'REFUNDED':record.booking_status==='cancelled'?'CANCELLED':record.payment_status==='paid'?'PAID':'PENDING',
    bookingKey:`${record.venue_id}:${record.slot_id}`,cancellationDeadline:Number(record.cancellation_deadline),
    createdAt:Number(record.created_at),startsAt:Number(record.starts_at)};
}

function matchItem(record) {
  const score = json(record.score), event = json(record.event_payload);
  const difference = score.homeScore-score.awayScore;
  return {id:record.id,sport:record.sport,title:event.title,date:dateLabel(record.starts_at),
    score:`${score.homeScore} – ${score.awayScore}`,
    won:!['home','away'].includes(record.side) || difference===0 ? null : record.side==='home' ? difference>0 : difference<0,
    mmrDelta:Number(record.delta ?? 0),mode:event.mode,verified:true,createdAt:Number(record.starts_at)};
}

/** Every row is bound to the requesting athlete; cursors cannot cross users or feeds. */
export async function personalHistory(ctx, kind, filters = {}) {
  ensure(ctx.actor?.id,401,'UNAUTHENTICATED','Geçmişini görmek için giriş yap.');
  choice(kind,kinds,'Geçmiş');
  keys(filters,['after','limit','sport']);
  const size = filters.limit == null ? 50 : Number(filters.limit);
  ensure(Number.isInteger(size) && size>=1 && size<=100,400,'INVALID_INPUT','Sayfa boyutu 1 ile 100 arasında olmalı.');
  const sport = filters.sport ? choice(filters.sport,SPORTS,'Branş') : '';
  ensure(!sport || ['matches','ratings'].includes(kind),400,'INVALID_INPUT','Bu geçmiş branşa göre filtrelenmiyor.');
  const scope = JSON.stringify([kind,sport]);
  const cursor = filters.after ? openCursor(ctx,`personal-history:${kind}`,filters.after) : null;
  const watermark = cursor?.watermark ?? ctx.now;
  if (cursor) {
    ensure(cursor.scope===scope && Number.isSafeInteger(watermark) && watermark>=0 &&
      Array.isArray(cursor.last) && cursor.last.length===2 && Number.isSafeInteger(cursor.last[0]) &&
      cursor.last[0]<=watermark && typeof cursor.last[1]==='string' && cursor.last[1].length<=200,
      400,'INVALID_CURSOR','Geçmiş filtreleri değişti. Baştan yenile.');
  }
  const parameters = [ctx.actor.id,watermark];
  let sql, stamp, identifier;
  if (kind==='matches') {
    stamp='e.starts_at';identifier='e.id';
    sql=`SELECT e.id,e.sport,e.payload AS event_payload,e.starts_at,m.side,r.payload AS score,h.delta
      FROM event_members m JOIN events e ON e.id=m.event_id JOIN results r ON r.event_id=e.id
      LEFT JOIN rating_history h ON h.event_id=e.id AND h.user_id=m.user_id
      WHERE m.user_id=? AND e.starts_at<=? AND m.status='joined' AND m.checked_in_at IS NOT NULL AND r.status='verified'`;
  } else if (kind==='ratings') {
    stamp='h.created_at';identifier='h.id';
    sql='SELECT h.* FROM rating_history h WHERE h.user_id=? AND h.created_at<=?';
  } else if (kind==='reliability') {
    stamp='h.created_at';identifier='h.id';
    sql=`SELECT h.*,e.payload AS event_payload FROM reliability_history h JOIN events e ON e.id=h.event_id
      WHERE h.user_id=? AND h.created_at<=?`;
  } else if (kind==='wallet') {
    stamp='w.created_at';identifier='w.id';
    sql='SELECT w.* FROM wallet_entries w WHERE w.user_id=? AND w.created_at<=?';
  } else if (kind==='reservations') {
    stamp='b.created_at';identifier='b.id';
    sql=`SELECT b.*,b.status AS booking_status,s.venue_id,s.payload AS slot_payload,s.starts_at,
      p.status AS payment_status,v.payload AS venue_payload FROM bookings b JOIN venue_slots s ON s.id=b.slot_id
      JOIN venue_payments p ON p.booking_id=b.id JOIN venues v ON v.id=s.venue_id
      WHERE b.user_id=? AND b.created_at<=?`;
  } else if (kind==='support') {
    stamp='s.created_at';identifier='s.id';
    sql=`SELECT s.*,(SELECT a.metadata FROM audit_log a WHERE ((a.target_id=s.id AND a.action='admin.support.resolve') OR (s.category='Fotoğraf doğrulaması' AND a.target_id=s.user_id AND a.action='admin.verification.photo'))
      ORDER BY a.created_at DESC,a.id DESC LIMIT 1) AS reply_payload FROM support_requests s WHERE s.user_id=? AND s.created_at<=?`;
  } else {
    stamp='r.created_at';identifier='r.id';
    sql='SELECT r.* FROM reports r WHERE r.user_id=? AND r.created_at<=?';
  }
  if (sport) { sql+=` AND ${kind==='matches'?'e':'h'}.sport=?`; parameters.push(sport); }
  if (cursor) {
    sql+=` AND (${stamp}<? OR (${stamp}=? AND ${identifier}<?))`;
    parameters.push(cursor.last[0],cursor.last[0],cursor.last[1]);
  }
  sql+=` ORDER BY ${stamp} DESC,${identifier} DESC LIMIT ?`;
  parameters.push(size+1);
  const rows = await ctx.db.all(sql,parameters), selected=rows.slice(0,size);
  const items = selected.map(record => {
    if (kind==='matches') return matchItem(record);
    if (kind==='ratings') return record;
    if (kind==='reliability') return {...record,date:dateLabel(record.created_at),detail:json(record.event_payload).title};
    if (kind==='reservations') return reservationItem(record);
    if (kind==='support') {
      const {reply_payload,...request}=record;
      const replyMeta=reply_payload?json(reply_payload):null;
      return {...request,response:replyMeta?(replyMeta.response??replyMeta.reason??null):null,createdAt:Number(record.created_at)};
    }
    if (kind==='reports') return {...record,createdAt:Number(record.created_at)};
    return {id:record.id,type:record.kind,amount:record.amount,lockedDelta:record.locked_delta,description:record.description,
      time:dateLabel(record.created_at),ref:record.ref,idempotencyKey:record.dedupe,createdAt:Number(record.created_at)};
  });
  const last = selected.at(-1);
  return {items,kind,serverTime:ctx.now,nextCursor:rows.length>size
    ? sealCursor(ctx,`personal-history:${kind}`,{scope,watermark,last:[Number(kind==='matches'?last.starts_at:last.created_at),last.id]}) : null};
}
