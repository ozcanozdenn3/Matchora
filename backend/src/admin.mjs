import { id } from './auth.mjs';
import { ensure,text,choice,keys,json } from './errors.mjs';
import { adminRequired,row,profile,audit,encode,notify,release,cancelEvent,ACTIVE_EVENTS } from './model.mjs';
import { ensureChatMetadata,bumpChatRevision } from './chat-state.mjs';
import { settleResult } from './rating.mjs';
import { processDeletions } from './jobs.mjs';
export const adminHandlers={
 'admin.report.resolve':async(ctx,p)=>{
  adminRequired(ctx);keys(p,['reportId','status','reason']);
  const report=await row(ctx,'reports','id',p.reportId);
  choice(p.status,['resolved','dismissed'],'Karar');
  const reason=text(p.reason,'Karar gerekçesi',10,1000);
  ensure(report.status==='open',409,'INVALID_STATE','Bildirim kapandı.');
  await ctx.db.run('UPDATE reports SET status=? WHERE id=?',[p.status,report.id]);
  if(report.user_id!==ctx.actor.id)await notify(ctx,report.user_id,'social',p.status==='resolved'?'Şikâyetin sonuçlandırıldı':'Şikâyet bildirimin incelendi',reason,'ACCOUNT',report.id,`report-resolve:${report.id}:${ctx.requestKey}`);
  await audit(ctx,'admin.report.resolve',report.id,{status:p.status,reason});
  return{status:p.status};
 },
 'admin.support.resolve':async(ctx,p)=>{
  adminRequired(ctx);keys(p,['requestId','status','response']);
  const request=await row(ctx,'support_requests','id',p.requestId);
  ensure(request.status==='open',409,'INVALID_STATE','Destek talebi daha önce yanıtlandı.');
  choice(p.status,['resolved','rejected'],'Durum');
  const response=text(p.response,'Yanıt',3,1000);
  await ctx.db.run('UPDATE support_requests SET status=? WHERE id=?',[p.status,request.id]);
  await notify(ctx,request.user_id,'social','Destek talebin yanıtlandı',response,'ACCOUNT',request.id,`support:${request.id}:${ctx.requestKey}`);
  await audit(ctx,'admin.support.resolve',request.id,{status:p.status,response});
  return{status:p.status};
 },
 'admin.memory.review':async(ctx,p)=>{
  adminRequired(ctx);keys(p,['memoryId','approved','reason']);
  ensure(typeof p.approved==='boolean');
  const reason=text(p.reason,'Gerekçe',3,500);
  const m=await row(ctx,'memories','id',p.memoryId);
  const hasOpenReport=!p.approved&&m.status==='approved'?!!await ctx.db.get("SELECT id FROM reports WHERE target_type='memory' AND target_id=? AND status='open' LIMIT 1",[m.id]):false;
  ensure(m.status==='pending'||hasOpenReport,409,'INVALID_STATE','İçerik işlendi.');
  await ctx.db.run('UPDATE memories SET status=? WHERE id=?',[p.approved?'approved':'rejected',m.id]);
  if(!p.approved)await ctx.db.run("UPDATE reports SET status='resolved' WHERE target_type='memory' AND target_id=? AND status='open'",[m.id]);
  await notify(ctx,m.author_id,'social',p.approved?'Anı paylaşımın onaylandı ✓':'Anı paylaşımın yayından kaldırıldı',reason,'MEMORIES',m.id,`memory-review:${m.id}:${ctx.requestKey}`);
  await audit(ctx,'admin.memory.review',m.id,{approved:p.approved,reason});
  return{approved:p.approved};
 },
 'admin.verification.photo':async(ctx,p)=>{
  adminRequired(ctx);keys(p,['userId','uploadId','approved','reason','requestId']);
  ensure(typeof p.approved==='boolean');
  const reason=text(p.reason,'Gerekçe',3,500);
  const user=await profile(ctx,p.userId),upload=await row(ctx,'uploads','id',p.uploadId);
  if(p.approved)ensure(upload.owner_id===p.userId&&user.avatarUploadId===upload.id,409,'INVALID_PHOTO','İncelenen fotoğraf güncel profil fotoğrafı olmalı.');
  else ensure(upload.owner_id===p.userId,409,'INVALID_PHOTO','İncelenen fotoğraf kullanıcıya ait değil.');
  if(p.requestId){
   const request=await row(ctx,'support_requests','id',p.requestId);
   ensure(request.user_id===p.userId&&request.category==='Fotoğraf doğrulaması'&&request.status==='open'&&request.message===`İnceleme için fotoğraf: ${upload.id}`,409,'INVALID_STATE','Fotoğraf talebi güncel değil.');
   await ctx.db.run('UPDATE support_requests SET status=? WHERE id=?',[p.approved?'resolved':'rejected',p.requestId]);
  }
  const v=new Set(user.verifications??[]);
  if(p.approved)v.add('PHOTO');
  else if(user.avatarUploadId===upload.id)v.delete('PHOTO');
  await ctx.db.run('UPDATE profiles SET payload=?,version=version+1,updated_at=? WHERE user_id=?',[encode({...user,verifications:[...v]}),ctx.now,p.userId]);
  await notify(ctx,p.userId,'social',p.approved?'Fotoğraf doğrulaman onaylandı ✓':'Fotoğraf doğrulaman reddedildi',reason,'PASSPORT',p.requestId??p.uploadId,`photo-verify:${p.requestId??p.uploadId}:${ctx.requestKey}`);
  await audit(ctx,'admin.verification.photo',p.userId,{uploadId:p.uploadId,approved:p.approved,reason,...(p.requestId?{requestId:p.requestId}:{})});
  return{approved:p.approved};
 },
 'admin.result.resolve':async(ctx,p)=>{
  adminRequired(ctx);keys(p,['eventId','approve','reason']);
  ensure(typeof p.approve==='boolean');
  const reason=text(p.reason,'Gerekçe',10,1000);
  const r=await row(ctx,'results','event_id',p.eventId),event=await row(ctx,'events','id',p.eventId);
  ensure(r.status==='disputed'||r.status==='pending'&&Number(r.dispute_until)<=ctx.now,409,'INVALID_STATE','Sonuç henüz incelemeye açık değil.');
  await ctx.db.run('UPDATE results SET status=?,dispute_until=? WHERE event_id=?',[p.approve?'pending':'rejected',ctx.now,p.eventId]);
  await ctx.db.run('UPDATE events SET status=?,version=version+1 WHERE id=?',[p.approve?'COMPLETED':'DISPUTED',p.eventId]);
  if(p.approve){
   await ctx.db.run('UPDATE results SET payload=? WHERE event_id=?',[encode({...json(r.payload),adminApproved:true}),p.eventId]);
   await ctx.db.run('INSERT INTO result_confirmations(event_id,user_id,created_at) VALUES(?,?,?) ON CONFLICT(event_id,user_id) DO NOTHING',[p.eventId,ctx.actor.id,ctx.now]);
   await settleResult(ctx,{event_id:p.eventId});
   const fresh=await ctx.db.get('SELECT status FROM results WHERE event_id=?',[p.eventId]);
   if(fresh?.status!=='verified'){
    for(const m of await ctx.db.all("SELECT user_id FROM event_members WHERE event_id=? AND status='joined'",[p.eventId])){
     await notify(ctx,m.user_id,'matches','Maç skoru operatör onayını aldı',`${json(event.payload).title} · ${reason}`,'EVENT',p.eventId,`result-approved:${p.eventId}:${m.user_id}:${ctx.now}`);
    }
   }
  }else{
   for(const h of await ctx.db.all("SELECT user_id FROM holds WHERE scope=? AND status='held'",[`event:${p.eventId}`]))await release(ctx,h.user_id,`event:${p.eventId}`);
   for(const m of await ctx.db.all("SELECT user_id FROM event_members WHERE event_id=? AND status='joined'",[p.eventId])){
    await notify(ctx,m.user_id,'matches','Maç skoru reddedildi',`${json(event.payload).title} · ${reason}`,'EVENT',p.eventId,`result-rejected:${p.eventId}:${m.user_id}:${ctx.now}`);
   }
  }
  await ctx.db.run("UPDATE reports SET status=? WHERE target_type='event' AND target_id=? AND reason='Skor itirazı' AND status='open'",[p.approve?'dismissed':'resolved',p.eventId]);
  await audit(ctx,'admin.result.resolve',p.eventId,{approve:p.approve,reason});
  return{approved:p.approve};
 },
 'admin.user.status':async(ctx,p)=>{
  adminRequired(ctx);keys(p,['userId','status','reason']);
  ensure(p.userId!==ctx.actor.id,409,'SELF_MODERATION','Kendi hesabına bu işlemi uygulayamazsın.');
  await row(ctx,'users','id',p.userId);choice(p.status,['active','suspended'],'Durum');
  const reason=text(p.reason,'Gerekçe',10,1000);
  await ctx.db.run('UPDATE users SET status=? WHERE id=?',[p.status,p.userId]);
  if(p.status==='suspended')await ctx.db.run('UPDATE sessions SET revoked=1 WHERE user_id=?',[p.userId]);
  else await notify(ctx,p.userId,'social','Hesabın yeniden aktif edildi',reason,'ACCOUNT',p.userId,`user-reactivated:${p.userId}:${ctx.requestKey}`);
  await audit(ctx,'admin.user.status',p.userId,{status:p.status,reason});
  return{status:p.status};
 },
 'admin.feature.set':async(ctx,p)=>{
  adminRequired(ctx);keys(p,['name','enabled']);
  const name=choice(p.name,['billing','memories','tournaments','venues','chat'],'Özellik');
  ensure(typeof p.enabled==='boolean');
  await ctx.db.run('INSERT INTO feature_flags(name,enabled,updated_at) VALUES(?,?,?) ON CONFLICT(name) DO UPDATE SET enabled=?,updated_at=?',[name,p.enabled?1:0,ctx.now,p.enabled?1:0,ctx.now]);
  await audit(ctx,'admin.feature.set',name,{enabled:p.enabled});
  return{enabled:p.enabled};
 },
 'admin.content.remove':async(ctx,p)=>{
  adminRequired(ctx);keys(p,['reportId','reason']);
  const report=await row(ctx,'reports','id',p.reportId);
  ensure(report.status==='open',409,'INVALID_STATE','Bildirim zaten sonuçlandırıldı.');
  const reason=text(p.reason,'Gerekçe',10,1000);
  let targetOwnerId=null;
  let destination='ACCOUNT';
  let removedTitle='İçerik moderasyon tarafından kaldırıldı';
  if(report.target_type==='memory'){
   const m=await row(ctx,'memories','id',report.target_id);
   targetOwnerId=m.author_id;destination='MEMORIES';removedTitle='Anı paylaşımın yayından kaldırıldı';
   await ctx.db.run("UPDATE memories SET status='deleted',payload='{}' WHERE id=?",[m.id]);
   await ctx.db.run('DELETE FROM memory_locations WHERE memory_id=?',[m.id]);
  }else if(report.target_type==='venue_review'){
   const r=await row(ctx,'venue_reviews','id',report.target_id);
   targetOwnerId=r.user_id;destination='VENUES';removedTitle='Tesis değerlendirmen yayından kaldırıldı';
   await ctx.db.run("UPDATE venue_reviews SET comment='',status='deleted',version=version+1,updated_at=? WHERE id=?",[ctx.now,r.id]);
  }else if(report.target_type==='message'){
   const raw=await row(ctx,'messages','id',report.target_id);
   targetOwnerId=raw.sender_id;destination='CHAT';removedTitle='Mesajın moderasyon tarafından kaldırıldı';
   await ensureChatMetadata(ctx,raw.thread_id);
   await ctx.db.run("UPDATE messages SET body='',upload_id=NULL WHERE id=?",[raw.id]);
   await ctx.db.run('UPDATE message_metadata SET deleted_at=?,version=version+1 WHERE message_id=?',[ctx.now,raw.id]);
   await bumpChatRevision(ctx,raw.thread_id);
  }else if(report.target_type==='thread'){
   const t=await row(ctx,'threads','id',report.target_id);
   destination='CHAT';
   await ensureChatMetadata(ctx,t.id);
   const msgs=await ctx.db.all('SELECT id FROM messages WHERE thread_id=?',[t.id]);
   await ctx.db.run("UPDATE messages SET body='',upload_id=NULL WHERE thread_id=?",[t.id]);
   for(const msg of msgs)await ctx.db.run('UPDATE message_metadata SET deleted_at=?,version=version+1 WHERE message_id=?',[ctx.now,msg.id]);
   await bumpChatRevision(ctx,t.id);
  }else if(report.target_type==='event'){
   const e=await row(ctx,'events','id',report.target_id);
   targetOwnerId=e.host_id;destination='EVENT';removedTitle='Etkinliğin moderasyon tarafından iptal edildi';
   if(ACTIVE_EVENTS.includes(e.status)||e.status==='IN_PROGRESS')await cancelEvent(ctx,e);
   else await ctx.db.run("UPDATE events SET status='CANCELLED',version=version+1 WHERE id=?",[e.id]);
  }else if(report.target_type==='team'){
   const t=await row(ctx,'teams','id',report.target_id);
   targetOwnerId=t.captain_id;destination='TEAM';removedTitle='Takım kaydın moderasyon tarafından arşivlendi';
   for(const ch of await ctx.db.all("SELECT * FROM challenges WHERE (from_team=? OR to_team=?) AND status='PENDING'",[t.id,t.id])){
    await ctx.db.run("UPDATE challenges SET status='DECLINED' WHERE id=?",[ch.id]);
    await release(ctx,ch.created_by,`challenge:${ch.id}`);
   }
   await ctx.db.run('UPDATE teams SET payload=? WHERE id=?',[encode({...json(t.payload),archived:true,recruitingOpen:false}),t.id]);
   await ctx.db.run("UPDATE team_members SET status='left' WHERE team_id=?",[t.id]);
   await ctx.db.run("UPDATE team_invites SET status='declined' WHERE team_id=? AND status='pending'",[t.id]);
   await ctx.db.run("UPDATE join_requests SET status='cancelled' WHERE scope='team' AND scope_id=? AND status='pending'",[t.id]);
  }else if(report.target_type==='venue'){
   const v=await row(ctx,'venues','id',report.target_id);
   targetOwnerId=v.owner_id;destination='VENUES';removedTitle='Tesis kaydın moderasyon tarafından yayından kaldırıldı';
   const vData=json(v.payload);
   await ctx.db.run('UPDATE venues SET payload=? WHERE id=?',[encode({...vData,archived:true,version:(vData.version??1)+1}),v.id]);
   for(const slot of await ctx.db.all("SELECT id,payload FROM venue_slots WHERE venue_id=? AND starts_at>? AND status='open'",[v.id,ctx.now])){
    const hasActiveBooking=await ctx.db.get("SELECT id FROM bookings WHERE slot_id=? AND status IN ('pending','confirmed')",[slot.id]);
    if(!hasActiveBooking){
     const sData=json(slot.payload);
     await ctx.db.run("UPDATE venue_slots SET status='cancelled',payload=? WHERE id=?",[encode({...sData,version:(sData.version??1)+1}),slot.id]);
    }
   }
  }else if(report.target_type==='user'){
   const u=await row(ctx,'users','id',report.target_id);
   ensure(u.id!==ctx.actor.id,409,'SELF_MODERATION','Kendi hesabına bu işlemi uygulayamazsın.');
   await ctx.db.run("UPDATE users SET status='suspended' WHERE id=?",[u.id]);
   await ctx.db.run('UPDATE sessions SET revoked=1 WHERE user_id=?',[u.id]);
  }
  await ctx.db.run("UPDATE reports SET status='resolved' WHERE target_type=? AND target_id=? AND status='open'",[report.target_type,report.target_id]);
  if(targetOwnerId&&targetOwnerId!==ctx.actor.id&&report.target_type!=='user'){
   await notify(ctx,targetOwnerId,'social',removedTitle,reason,destination,report.target_id,`content-removed:${report.id}:${ctx.requestKey}`);
  }
  if(report.user_id!==ctx.actor.id){
   await notify(ctx,report.user_id,'social','Şikâyetin sonuçlandırıldı',reason,'ACCOUNT',report.id,`report-resolve:${report.id}:${ctx.requestKey}`);
  }
  await audit(ctx,'admin.content.remove',report.target_id,{reportId:report.id,targetType:report.target_type,reason});
  return{removed:true,targetType:report.target_type,targetId:report.target_id};
 },
 'admin.deletion.resolve':async(ctx,p)=>{
  adminRequired(ctx);keys(p,['userId','approve','reason']);
  ensure(typeof p.approve==='boolean');
  const userId=text(p.userId,'Hesap',1,200);
  ensure(userId!==ctx.actor.id,409,'SELF_MODERATION','Kendi hesabına bu işlemi uygulayamazsın.');
  const reason=text(p.reason,'Gerekçe',3,500);
  const req=await ctx.db.get("SELECT * FROM deletion_requests WHERE user_id=? AND status='pending'",[userId]);
  ensure(req,404,'NOT_FOUND','Bekleyen hesap silme talebi bulunamadı.');
  if(p.approve){
   await ctx.db.run("UPDATE deletion_requests SET execute_after=? WHERE user_id=? AND status='pending'",[ctx.now,userId]);
   await processDeletions(ctx);
  }else{
   await ctx.db.run("UPDATE deletion_requests SET status='cancelled' WHERE user_id=? AND status='pending'",[userId]);
   await notify(ctx,userId,'social','Hesap silme talebin iptal edildi',reason,'ACCOUNT',userId,`deletion-resolve:${userId}:${ctx.requestKey}`);
  }
  await audit(ctx,'admin.deletion.resolve',userId,{approve:p.approve,reason});
  return{resolved:true,approved:p.approve};
 }
};
