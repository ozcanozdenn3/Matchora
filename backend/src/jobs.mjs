import { scheduleEngagement } from './engagement.mjs';
import { settleResult } from './rating.mjs';
import { cleanMedia,queueStorageDeletion } from './storage.mjs';
import { nativePushConfigured,sendApns,sendFcm } from './push.mjs';
import { grantProgressRewards } from './progression.mjs';
import { scheduleReminders } from './reminders.mjs';
import { grantSubscriptionMonths } from './billing.mjs';
import { id } from './auth.mjs';
import { json } from './errors.mjs';
import { row,release,audit,encode,offerWaitlist,notify,reward,assertThread } from './model.mjs';
export async function finalizeResults(ctx){
 for(const result of await ctx.db.all("SELECT * FROM results WHERE status='pending' AND dispute_until<=? ORDER BY created_at,event_id",[ctx.now]))await settleResult(ctx,result);
}
export async function expireRecords(ctx){
 for(const row of await ctx.db.all('SELECT user_id,payload FROM discovery_preferences')){const data=json(row.payload);if(data.location&&data.location.expiresAt<=ctx.now){delete data.location;await ctx.db.run('UPDATE discovery_preferences SET payload=? WHERE user_id=?',[encode(data),row.user_id]);}}
 await ctx.db.run("UPDATE outbox SET status='pending',claim_token=NULL WHERE status='processing' AND next_at<=?",[ctx.now]);
 for(const event of await ctx.db.all("SELECT * FROM events WHERE status IN ('PUBLISHED','FILLING','FULL','IN_PROGRESS') AND ends_at<=?",[ctx.now])){
  await ctx.db.run("UPDATE events SET status='COMPLETED',version=version+1 WHERE id=?",[event.id]);
  for(const request of await ctx.db.all("SELECT user_id FROM join_requests WHERE scope='event' AND scope_id=? AND status='pending'",[event.id]))await release(ctx,request.user_id,`event:${event.id}`);
  await ctx.db.run("UPDATE join_requests SET status='expired' WHERE scope='event' AND scope_id=? AND status='pending'",[event.id]);
  await ctx.db.run("UPDATE waitlist SET status='expired' WHERE event_id=? AND status IN ('waiting','offered')",[event.id]);
 }
 for(const event of await ctx.db.all("SELECT e.id FROM events e LEFT JOIN results r ON r.event_id=e.id WHERE e.status='COMPLETED' AND e.ends_at<=? AND (r.status IS NULL OR r.status IN ('pending','rejected'))",[ctx.now-72*3600000])){
  for(const hold of await ctx.db.all("SELECT user_id FROM holds WHERE scope=? AND status='held'",[`event:${event.id}`]))await release(ctx,hold.user_id,`event:${event.id}`);
 }

 for(const entry of await ctx.db.all("SELECT * FROM waitlist WHERE status='offered' AND offered_until<=?",[ctx.now])){await ctx.db.run("UPDATE waitlist SET status='expired' WHERE event_id=? AND user_id=?",[entry.event_id,entry.user_id]);await offerWaitlist(ctx,entry.event_id);}
 for(const c of await ctx.db.all("SELECT * FROM challenges WHERE status='PENDING'")){if(json(c.payload).startsAtEpochMillis<=ctx.now){await release(ctx,c.created_by,`challenge:${c.id}`);await ctx.db.run("UPDATE challenges SET status='DECLINED' WHERE id=?",[c.id]);}}
 await ctx.db.run("UPDATE subscriptions SET status='expired' WHERE expires_at<=? AND status='active'",[ctx.now]);
 await ctx.db.run('DELETE FROM rate_limits WHERE resets_at<?',[ctx.now-86400000]);
 await ctx.db.run('DELETE FROM oauth_flows WHERE expires_at<?',[ctx.now-86400000]);
 await ctx.db.run('DELETE FROM sessions WHERE refresh_expires_at<?',[ctx.now-30*86400000]);
 await ctx.db.run('DELETE FROM auth_challenges WHERE expires_at<?',[ctx.now-86400000]);
}
export async function processDeletions(ctx){const objects=[];
 for(const request of await ctx.db.all("SELECT * FROM deletion_requests WHERE status='pending' AND execute_after<=?",[ctx.now])){
  const user=await row(ctx,'users','id',request.user_id);
  for(const booking of await ctx.db.all("SELECT b.id,s.venue_id FROM bookings b JOIN venue_slots s ON s.id=b.slot_id JOIN venue_payments p ON p.booking_id=b.id WHERE b.user_id=? AND b.status IN ('pending','confirmed') AND s.starts_at>? AND p.status='pending'",[user.id,ctx.now])){
   await ctx.db.run("UPDATE bookings SET status='cancelled' WHERE id=?",[booking.id]);await ctx.db.run("UPDATE venue_payments SET status='cancelled' WHERE booking_id=?",[booking.id]);
   const venue=await row(ctx,'venues','id',booking.venue_id);await notify(ctx,venue.owner_id,'matches','Rezervasyon iptal edildi','Hesap silindiği için tahsilat yapılmamış gelecek rezervasyon iptal edildi.','VENUES',booking.venue_id,`account-booking-cancel:${booking.id}`);
   await audit(ctx,'booking.cancel',booking.id,{reason:'account.delete'});
  }
  const affectedThreads=await ctx.db.all('SELECT DISTINCT thread_id FROM messages WHERE sender_id=? OR upload_id IN (SELECT id FROM uploads WHERE owner_id=?)',[user.id,user.id]);
  for(const thread of affectedThreads)await ctx.db.run('INSERT INTO thread_revisions(thread_id,revision) VALUES(?,1) ON CONFLICT(thread_id) DO UPDATE SET revision=thread_revisions.revision+1',[thread.thread_id]);
  await ctx.db.run('UPDATE message_metadata SET deleted_at=?,version=version+1 WHERE message_id IN (SELECT id FROM messages WHERE sender_id=?)',[ctx.now,user.id]);
  await ctx.db.run("UPDATE messages SET upload_id=NULL WHERE upload_id IN (SELECT id FROM uploads WHERE owner_id=?)",[user.id]);
  // A captain/host transition is resolved by operations; no stranded live matches are silently erased.
  for(const event of await ctx.db.all("SELECT * FROM events WHERE host_id=? AND status IN ('PUBLISHED','FILLING','FULL','IN_PROGRESS')",[user.id])){
   const payloadData=json(event.payload);
   if(payloadData.isTeamEvent){
    const successor=await ctx.db.get("SELECT m.user_id FROM event_members m JOIN users u ON u.id=m.user_id WHERE m.event_id=? AND m.user_id<>? AND m.status='joined' AND u.status='active' ORDER BY m.joined_at,m.user_id LIMIT 1",[event.id,user.id]);
    if(successor){await ctx.db.run('UPDATE events SET host_id=?,version=version+1 WHERE id=?',[successor.user_id,event.id]);continue;}
   }
   const {cancelEvent}=await import('./model.mjs');await cancelEvent(ctx,event);
  }
  for(const team of await ctx.db.all('SELECT * FROM teams WHERE captain_id=?',[user.id])){
   const next=await ctx.db.get("SELECT m.user_id FROM team_members m JOIN users u ON u.id=m.user_id WHERE m.team_id=? AND m.user_id<>? AND m.status='joined' AND u.status='active' ORDER BY u.created_at,m.user_id LIMIT 1",[team.id,user.id]);
   if(next){await ctx.db.run('UPDATE teams SET captain_id=? WHERE id=?',[next.user_id,team.id]);await ctx.db.run("UPDATE team_members SET role='captain' WHERE team_id=? AND user_id=?",[team.id,next.user_id]);await notify(ctx,next.user_id,'challenges','Takım kaptanlığı sana devredildi',json(team.payload).name,'CHALLENGES',team.id,`deletion-transfer:${team.id}:${user.id}`);}
   else await ctx.db.run('UPDATE teams SET payload=? WHERE id=?',[encode({...json(team.payload),archived:true,description:'',recruitingOpen:false}),team.id]);
   await audit(ctx,'account.deletion.team',team.id,{successor:next?.user_id??null});
  }
  await ctx.db.run("UPDATE team_members SET status='left' WHERE user_id=?",[user.id]);
  await ctx.db.run("UPDATE team_invites SET status='declined' WHERE user_id=?",[user.id]);
  await ctx.db.run("UPDATE join_requests SET status='cancelled',payload='{}' WHERE user_id=?",[user.id]);
  for(const community of await ctx.db.all('SELECT * FROM communities WHERE owner_id=?',[user.id])){
   const next=await ctx.db.get("SELECT m.user_id FROM community_members m JOIN users u ON u.id=m.user_id WHERE m.community_id=? AND m.user_id<>? AND m.status='joined' AND u.status='active' ORDER BY m.created_at,m.user_id LIMIT 1",[community.id,user.id]);
   if(next)await ctx.db.run('UPDATE communities SET owner_id=? WHERE id=?',[next.user_id,community.id]);
   else await ctx.db.run('UPDATE communities SET payload=? WHERE id=?',[encode({...json(community.payload),archived:true,description:''}),community.id]);
  }
  await ctx.db.run("UPDATE community_members SET status='left' WHERE user_id=?",[user.id]);
  for(const membership of await ctx.db.all("SELECT e.* FROM events e JOIN event_members m ON m.event_id=e.id WHERE m.user_id=? AND m.status='joined' AND e.status IN ('PUBLISHED','FILLING','FULL') AND e.starts_at>?",[user.id,ctx.now])){
   await ctx.db.run("UPDATE event_members SET status='left' WHERE event_id=? AND user_id=?",[membership.id,user.id]);await release(ctx,user.id,`event:${membership.id}`);await offerWaitlist(ctx,membership.id);
  }
  await ctx.db.run("UPDATE waitlist SET status='cancelled' WHERE user_id=?",[user.id]);
  await ctx.db.run('DELETE FROM oauth_flows WHERE expires_at<?',[ctx.now-86400000]);
 await ctx.db.run('DELETE FROM sessions WHERE refresh_expires_at<?',[ctx.now-30*86400000]);
 await ctx.db.run('DELETE FROM auth_challenges WHERE user_id=? OR identity=?',[user.id,user.phone??'']);
  const uploads=await ctx.db.all('SELECT object_key FROM uploads WHERE owner_id=?',[user.id]);objects.push(...uploads.map(u=>u.object_key));for(const upload of uploads)await queueStorageDeletion(ctx,upload.object_key);
  await ctx.db.run("UPDATE uploads SET status='deleted' WHERE owner_id=?",[user.id]);
  await ctx.db.run('DELETE FROM message_hides WHERE user_id=?',[user.id]);await ctx.db.run('DELETE FROM thread_member_state WHERE user_id=?',[user.id]);await ctx.db.run('DELETE FROM memory_locations WHERE memory_id IN (SELECT id FROM memories WHERE author_id=?)',[user.id]);
  await ctx.db.run("UPDATE messages SET body='Silinen hesap',upload_id=NULL WHERE sender_id=?",[user.id]);await ctx.db.run("UPDATE memories SET status='deleted',payload='{}',upload_id=NULL WHERE author_id=?",[user.id]);
  await ctx.db.run('DELETE FROM memory_likes WHERE user_id=?',[user.id]);await ctx.db.run('DELETE FROM push_devices WHERE user_id=?',[user.id]);await ctx.db.run('DELETE FROM notification_hides WHERE user_id=?',[user.id]);await ctx.db.run('DELETE FROM notifications WHERE user_id=?',[user.id]);await ctx.db.run('DELETE FROM outbox WHERE user_id=?',[user.id]);await ctx.db.run('DELETE FROM commands WHERE user_id=?',[user.id]);
  await ctx.db.run("UPDATE venue_applications SET payload='{}',response='',status=CASE WHEN status='pending' THEN 'withdrawn' ELSE status END,version=version+1,updated_at=? WHERE user_id=?",[ctx.now,user.id]);
  await ctx.db.run("UPDATE support_requests SET subject='Silinen hesap',message='',status='closed' WHERE user_id=?",[user.id]);await ctx.db.run("UPDATE reports SET details='' WHERE user_id=?",[user.id]);
  await ctx.db.run('DELETE FROM event_locations WHERE event_id IN (SELECT id FROM events WHERE host_id=?)',[user.id]);
  for(const event of await ctx.db.all('SELECT id,payload FROM events WHERE host_id=?',[user.id])){const p=json(event.payload);await ctx.db.run('UPDATE events SET payload=?,latitude=NULL,longitude=NULL WHERE id=?',[encode({...p,title:'Silinen organizatörün maçı',description:'',venue:'Konum kaldırıldı',voiceChannel:'',hostGameTag:'',networkName:null}),event.id]);}
  await ctx.db.run("UPDATE venue_reviews SET comment='',status='deleted',version=version+1 WHERE user_id=?",[user.id]);
  await ctx.db.run('DELETE FROM profiles WHERE user_id=?',[user.id]);await ctx.db.run('DELETE FROM preferences WHERE user_id=?',[user.id]);await ctx.db.run('DELETE FROM discovery_preferences WHERE user_id=?',[user.id]);
  await ctx.db.run("UPDATE users SET subject=?,phone=NULL,email=NULL,status='deleted' WHERE id=?",[`deleted:${user.id}`,user.id]);await ctx.db.run('UPDATE sessions SET revoked=1 WHERE user_id=?',[user.id]);await ctx.db.run("UPDATE deletion_requests SET status='completed' WHERE user_id=?",[user.id]);await audit(ctx,'account.deletion.complete',user.id);
 }
 return objects;
}
const PUSH_LEASE_MS=60000;
async function claimPush(service){
 return service.store.transaction(async db=>{
  const now=service.clock();
  await db.run("UPDATE outbox SET status='pending',claim_token=NULL WHERE status='processing' AND next_at<=?",[now]);
  const rows=await db.all("SELECT * FROM outbox WHERE status='pending' AND next_at<=? ORDER BY created_at,id LIMIT 50",[now]);
  for(const job of rows){
   const prefs=await db.get('SELECT notifications FROM preferences WHERE user_id=?',[job.user_id]);
   const settings=prefs?json(prefs.notifications):{},payload=json(job.payload),notification=await db.get('SELECT category,dedupe FROM notifications WHERE id=?',[payload.notificationId]);
   let inaccessible=!!(payload.notificationId&&await db.get('SELECT notification_id FROM notification_hides WHERE notification_id=? AND user_id=?',[payload.notificationId,job.user_id]));
   if(payload.destination==='CHAT') {try{await assertThread({db,actor:{id:job.user_id,role:'user'}},payload.targetId);}catch{inaccessible=true;}}
   if(notification?.dedupe?.startsWith('reminder:')||notification?.dedupe?.startsWith('joined:')||notification?.dedupe?.startsWith('event-updated:')){
    const joined=await db.get("SELECT m.user_id FROM event_members m JOIN events e ON e.id=m.event_id WHERE m.event_id=? AND m.user_id=? AND m.status='joined' AND e.status IN ('PUBLISHED','FILLING','FULL','IN_PROGRESS') AND e.ends_at>?",[payload.targetId,job.user_id,now]);if(!joined)inaccessible=true;
   }
   if(notification?.dedupe?.startsWith('offer:')){
    const offered=await db.get("SELECT w.user_id FROM waitlist w JOIN events e ON e.id=w.event_id WHERE w.event_id=? AND w.user_id=? AND w.status='offered' AND w.offered_until>? AND e.starts_at>? AND e.status IN ('PUBLISHED','FILLING','FULL')",[payload.targetId,job.user_id,now,now]);if(!offered)inaccessible=true;
   }
   if(inaccessible){await db.run("UPDATE outbox SET status='skipped',claim_token=NULL WHERE id=?",[job.id]);continue;}
   const categoryKey={matches:'upcomingMatch',wallet:null,social:'social',teams:'social',challenges:'challenges',waitlist:'waitlist',emergencyFill:'emergencyFill',weeklyDigest:'weeklyDigest',rematch:'rematch',marketing:'marketing'}[notification?.category];
   if(payload.expiresAt&&Number(payload.expiresAt)<=now||categoryKey&&settings[categoryKey]===false){await db.run("UPDATE outbox SET status='skipped',claim_token=NULL WHERE id=?",[job.id]);continue;}
   const hour=new Intl.DateTimeFormat('en-GB',{timeZone:'Europe/Istanbul',hour:'2-digit',minute:'2-digit',hour12:false}).format(now);
   const from=settings.quietFrom??'23:00',to=settings.quietTo??'08:00',quiet=from>to?(hour>=from||hour<to):(hour>=from&&hour<to);
   if(settings.quietHours!==false&&quiet){await db.run('UPDATE outbox SET next_at=? WHERE id=?',[now+30*60000,job.id]);continue;}
   const devices=await db.all('SELECT platform,token FROM push_devices WHERE user_id=?',[job.user_id]);
   if(!devices.length){await db.run("UPDATE outbox SET status='skipped',claim_token=NULL WHERE id=?",[job.id]);continue;}
   const today=new Date(now+3*3600000);today.setUTCHours(0,0,0,0);const dayStart=today.getTime()-3*3600000;
   // Active claims reserve quota so simultaneous workers cannot both take the last daily slot.
   const delivered=await db.get("SELECT COUNT(*) AS n FROM outbox WHERE user_id=? AND ((status='delivered' AND delivered_at>=?) OR (status='processing' AND next_at>?))",[job.user_id,dayStart,now]);
   if(Number(delivered.n)>=(settings.dailyCap??6)&&notification?.category!=='wallet'){await db.run('UPDATE outbox SET next_at=? WHERE id=?',[dayStart+86400000,job.id]);continue;}
   const claimToken=id();
   await db.run("UPDATE outbox SET status='processing',claim_token=?,next_at=? WHERE id=? AND status='pending'",[claimToken,now+PUSH_LEASE_MS,job.id]);
   return{...job,payload,devices,claimToken};
  }
  return null;
 });
}
async function renewPush(service,job){
 const result=await service.store.transaction(db=>db.run("UPDATE outbox SET next_at=? WHERE id=? AND status='processing' AND claim_token=?",[service.clock()+PUSH_LEASE_MS,job.id,job.claimToken]));
 return result.changes===1;
}
async function finishPush(service,job,status,{nextAt=service.clock(),attempt=false}={}){
 await service.store.transaction(db=>db.run("UPDATE outbox SET status=?,claim_token=NULL,next_at=?,attempts=attempts+?,delivered_at=? WHERE id=? AND status='processing' AND claim_token=?",
  [status,nextAt,attempt?1:0,status==='delivered'?service.clock():null,job.id,job.claimToken]));
}
export async function runJobs(service){
 const objects=await service.store.transaction(async db=>{const ctx={db,actor:null,now:service.clock(),config:service.config};await expireRecords(ctx);await scheduleReminders(ctx);await grantSubscriptionMonths(ctx);await finalizeResults(ctx);return processDeletions(ctx);});
 await service.store.transaction(db=>scheduleEngagement({db,actor:null,now:service.clock(),config:service.config}));
 await cleanMedia(service);
 if(!service.config.pushUrl&&!nativePushConfigured(service.config))return;
 for(let index=0;index<50;index++){
  const job=await claimPush(service);if(!job)break;
  try{
   if(!await renewPush(service,job))continue;
   if(service.config.pushUrl){const response=await fetch(service.config.pushUrl,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${service.config.pushToken}`,'Idempotency-Key':job.id},body:JSON.stringify({devices:job.devices,notification:job.payload}),signal:AbortSignal.timeout(8000),redirect:'error'});if(!response.ok)throw new Error('Push delivery rejected');}
   else {let attempted=0,accepted=0,lost=false;for(const device of job.devices){let result;
    if(!await renewPush(service,job)){lost=true;break;}
    if(device.platform==='ios'&&service.config.apnsPrivateKey)result=await sendApns(service.config,device,job.payload,job.id);
    else if(device.platform==='android'&&service.config.fcmPrivateKey)result=await sendFcm(service.config,device,job.payload);
    else continue;
    attempted++;if(result.sent)accepted++;if(result.invalid)await service.store.transaction(db=>db.run('DELETE FROM push_devices WHERE token=? AND user_id=?',[device.token,job.user_id]));
   }if(lost)continue;if(!accepted){await finishPush(service,job,'skipped',{attempt:attempted>0});continue;}}
   await finishPush(service,job,'delivered',{attempt:true});
  }catch{await finishPush(service,job,job.attempts>=7?'failed':'pending',{attempt:true,nextAt:service.clock()+Math.min(3600000,2**job.attempts*10000)});}
 }
}
