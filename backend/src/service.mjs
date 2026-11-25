import { searchPlayers } from './player-search.mjs';
import { identityDeletionHandlers } from './identity-deletion.mjs';
import { personalHistory } from './personal-history.mjs';
import { catalogPage, catalogDetail } from './catalog.mjs';
import { notificationPage } from './notification-feed.mjs';
import { rankingPage } from './rankings.mjs';
import { venueApplicationHandlers } from './venue-applications.mjs';
import { memoryManagementHandlers } from './memory-management.mjs';
import { memoryFeed } from './memory-feed.mjs';
import { chatManagementHandlers } from './chat-management.mjs';
import { venueScheduleHandlers } from './venue-schedule.mjs';
import { operationHandlers, operatorVenues, venueReviews } from './operations.mjs';
import { communityManagementHandlers } from './community-management.mjs';
import { RATING_POLICY } from './rating.mjs';
import { playerDetails } from './player-details.mjs';
import { passportHandlers,verifyPassport } from './passport.mjs';
import { storeConfiguration,storeHandlers } from './store-sync.mjs';
import { managementHandlers } from './management.mjs';
import { smsConfigured } from './sms.mjs';
import { oauthConfigured } from './oauth.mjs';
import { accountHandlers,exportAccount } from './account.mjs';
import { ApiError,ensure,keys,text,json } from './errors.mjs';
import { digest,rateLimit } from './auth.mjs';
import { legalHandlers,legalConfiguration } from './legal.mjs';
import { identityHandlers } from './identity.mjs';
import { eventManagementHandlers } from './event-management.mjs';
import { eventHandlers } from './events.mjs';
import { tournamentManagementHandlers } from './tournament-management.mjs';
import { competitionHandlers } from './competition.mjs';
import { networkHandlers } from './network.mjs';
import { socialHandlers } from './social.mjs';
import { adminHandlers } from './admin.mjs';
import { audit,profile,encode,actorRequired,row } from './model.mjs';
import { commandChanges,snapshot,eventList,messagesPage,eventView,canSeeEvent,publicPlayer,chatInbox,chatDetail } from './queries.mjs';
import { revenueCatWebhook } from './revenuecat.mjs';
import { billingWebhook } from './billing.mjs';
import { prepareUpload,recordUpload,authorizeUploadRead } from './media.mjs';
export const handlers={...identityDeletionHandlers,...venueApplicationHandlers,...passportHandlers,...storeHandlers,...managementHandlers,...accountHandlers,...legalHandlers,...identityHandlers,...eventHandlers,...eventManagementHandlers,...competitionHandlers,...tournamentManagementHandlers,...networkHandlers,...communityManagementHandlers,...operationHandlers,...venueScheduleHandlers,...socialHandlers,...memoryManagementHandlers,...chatManagementHandlers,...adminHandlers};
export function canonical(value){if(Array.isArray(value))return `[${value.map(canonical).join(',')}]`;if(value&&typeof value==='object')return `{${Object.keys(value).sort().map(k=>`${JSON.stringify(k)}:${canonical(value[k])}`).join(',')}}`;return JSON.stringify(value);}
export class MatchoraService{
 constructor(store,config,media,clock=Date.now){this.store=store;this.config=config;this.media=media;this.clock=clock;}
 ctx(db,actor=null,scope={}){return{db,actor,now:this.clock(),config:this.config,discoveryCity:scope.city?text(scope.city,'İl',2,60):undefined};}
 async commandReceipt(actor,payload){
  ensure(actor?.id,401,'UNAUTHENTICATED','Giriş yapmalısın.');
  keys(payload,['operation','payload','requestKey']);
  const {operation,requestKey}=payload;
  ensure(typeof operation==='string'&&Object.hasOwn(handlers,operation),400,'INVALID_INPUT','İşlem türü geçersiz.');
  ensure(typeof requestKey==='string'&&/^[A-Za-z0-9_.:-]{8,120}$/.test(requestKey),400,'IDEMPOTENCY_REQUIRED','İşlem anahtarı geçersiz.');
  ensure(payload.payload&&typeof payload.payload==='object'&&!Array.isArray(payload.payload),400,'INVALID_INPUT','İşlem bilgisi geçersiz.');
  return this.store.transaction(async db=>{
  const user=await db.get('SELECT status FROM users WHERE id=?',[actor.id]);
  ensure(user?.status==='active',403,'ACCOUNT_DISABLED','Bu hesap kullanılamıyor.');
  const saved=await db.get('SELECT operation,request_hash,response FROM commands WHERE user_id=? AND request_key=?',[actor.id,requestKey]);
  if(!saved)return{found:false};
  ensure(saved.operation===operation&&saved.request_hash===digest(canonical({operation,payload:payload.payload})),409,'IDEMPOTENCY_CONFLICT','Bu işlem anahtarı farklı içerikle kullanılmış.');
  return{found:true,result:JSON.parse(saved.response)};
  });
 }
 async command(actor,operation,payload,key,scope={}){
  ensure(Object.hasOwn(handlers,operation),404,'UNKNOWN_OPERATION','İşlem bulunamadı.');ensure(actor?.id,401,'UNAUTHENTICATED','Giriş yapmalısın.');ensure(typeof key==='string'&&/^[A-Za-z0-9_.:-]{8,120}$/.test(key),400,'IDEMPOTENCY_REQUIRED','Tekrarsız işlem anahtarı gerekiyor.');
  const hash=digest(canonical({operation,payload}));
  try{return await this.store.transaction(async db=>{const ctx={...this.ctx(db,actor,scope),requestKey:key};const user=await row(ctx,'users','id',actor.id);ensure(user.status==='active',403,'ACCOUNT_DISABLED','Bu hesap kullanılamıyor.');ctx.actor={...actor,role:user.role};const old=await db.get('SELECT * FROM commands WHERE user_id=? AND request_key=?',[actor.id,key]);if(old){ensure(old.operation===operation&&old.request_hash===hash,409,'IDEMPOTENCY_CONFLICT','İstek anahtarı farklı içerikle kullanıldı.');return{result:JSON.parse(old.response),snapshot:await snapshot(ctx),replayed:true};}
   const exitOperations=new Set(['legal.accept','account.delete','account.delete.cancel','account.session.revoke','support.create','report.create','preferences.save','device.remove','device.register','notification.read','notification.clear','chat.read','chat.delete','chat.clear','message.hide','message.delete','event.leave','event.member.remove','event.request.cancel','booking.cancel','team.leave','team.archive','community.archive','venue.archive','venue.application.withdraw','team.request.cancel','community.leave','tournament.withdraw','challenge.cancel','memory.delete','venue.review.delete','user.block','booking.refund.record','booking.complete','tournament.cancel','community.invite.decline']);
   if(ctx.config.production&&!operation.startsWith('admin.')&&!exitOperations.has(operation)&&!(operation==='event.transition'&&payload.status==='CANCELLED')){
    const legal=await legalConfiguration(ctx);ensure(legal.configured,503,'LEGAL_DOCUMENTS_PENDING','Belgeler henüz yayınlanmadı.');ensure(await db.get('SELECT id FROM legal_acceptances WHERE user_id=? AND terms_version=? AND privacy_version=?',[actor.id,legal.documents.terms.version,legal.documents.privacy.version]),403,'LEGAL_ACCEPTANCE_REQUIRED','Güncel kullanım koşulları ve gizlilik politikasını incelemelisin.');
   }
   const operatorOperations=new Set(['venue.create','venue.update','venue.archive','venue.slot.create','venue.slot.bulk.create','venue.slot.update','venue.slot.reopen','venue.slot.cancel','booking.confirm','booking.payment.record','booking.complete','booking.refund.record']);
   if(operatorOperations.has(operation))ensure(['admin','venue_owner'].includes(ctx.actor.role),403,'FORBIDDEN','Güncel tesis yetkisi gerekiyor.');
   const feature=operation.split('.')[0],flagMap={memory:'memories',tournament:'tournaments',venue:'venues',booking:'venues',chat:'chat',message:'chat'};if(flagMap[feature]&&!exitOperations.has(operation)){const flag=await db.get('SELECT enabled FROM feature_flags WHERE name=?',[flagMap[feature]]);ensure(!flag||flag.enabled,503,'FEATURE_UNAVAILABLE','Bu özellik geçici olarak kullanılamıyor.');}
   const result=await handlers[operation](ctx,payload);await db.run('INSERT INTO commands(user_id,operation,request_key,request_hash,response,created_at) VALUES(?,?,?,?,?,?)',[actor.id,operation,key,hash,encode(result),ctx.now]);if (scope.compact) {
     const changes = await commandChanges(ctx,operation,payload,result);
     if (changes) return {result,changes,replayed:false};
   }
   return{result,snapshot:await snapshot(ctx),replayed:false};
  });}catch(error){if(error instanceof ApiError)throw error;if(error.code?.startsWith('SQLITE_CONSTRAINT')||error.code==='23505')throw new ApiError(409,'CONFLICT','Bu kayıt zaten var veya işlem mevcut duruma uygun değil.');throw error;}
 }
 async configuration(){return this.store.read(async db=>{const flag=await db.get('SELECT enabled FROM feature_flags WHERE name=?',['billing']),store=storeConfiguration(this.config);return{fees:{tournament:20,listing:10},ratingPolicy:RATING_POLICY,...await legalConfiguration(this.ctx(db)),auth:{social:oauthConfigured(this.config),email:false,phone:smsConfigured(this.config),development:this.config.devAuth},billing:!!this.config.billingSecret&&flag?.enabled!==0,store:{...store,enabled:store.enabled&&flag?.enabled!==0},supportEmail:this.config.supportEmail??null};});}
 async searchPlayers(actor,filters){return this.store.read(db=>searchPlayers(this.ctx(db,actor),filters));}
 async personalHistory(actor,kind,filters){return this.store.read(db=>personalHistory(this.ctx(db,actor),kind,filters));}
 async player(actor,userId,sport){return this.store.read(db=>playerDetails(this.ctx(db,actor),userId,sport));}
 async bootstrap(actor=null,scope={}){return this.store.read(async db=>{if(actor){const current=await db.get('SELECT role,status FROM users WHERE id=?',[actor.id]);ensure(current?.status==='active',401,'ACCOUNT_DISABLED','Hesap kullanılamıyor.');actor={...actor,role:current.role};}return snapshot(this.ctx(db,actor,scope));});}
 async verifyPassport(code){return this.store.read(db=>verifyPassport(this.ctx(db),code));}
 async exportAccount(actor){return this.store.read(db=>exportAccount(this.ctx(db,actor)));}
 async sessions(actor){return this.store.read(async db=>({items:(await db.all("SELECT id,created_at,expires_at FROM sessions WHERE user_id=? AND revoked=0 AND refresh_expires_at>? ORDER BY created_at DESC",[actor.id,this.clock()])).map(s=>({...s,current:s.id===actor.sessionId}))}));}
 async event(actor,eventId){ensure(typeof eventId==='string'&&/^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$/.test(eventId),400,'INVALID_EVENT_ID','Geçerli bir ilan ID’si gir.');return this.store.read(async db=>{const ctx=this.ctx(db,actor),event=await row(ctx,'events','id',eventId.toLowerCase());ensure(!['DRAFT','ARCHIVED'].includes(event.status)&&await canSeeEvent(ctx,event),404,'NOT_FOUND','İlan bulunamadı veya bu ağa erişim iznin yok.');const view=await eventView(ctx,event);return{...view,players:await Promise.all([...new Set([view.hostId,...view.participantIds])].map(id=>publicPlayer(ctx,id)))};});}
 async threads(actor,filters){return this.store.read(db=>chatInbox(this.ctx(db,actor),filters));}
 async thread(actor,id){return this.store.read(db=>chatDetail(this.ctx(db,actor),id));}
 async events(actor,filters){return this.store.read(db=>eventList(this.ctx(db,actor),filters));}
 async rankings(actor,filters){return this.store.read(db=>rankingPage(this.ctx(db,actor),filters));}
 async catalogItem(actor,kind,id){return this.store.read(db=>catalogDetail(this.ctx(db,actor),kind,id));}
 async catalog(actor,kind,filters){return this.store.read(db=>catalogPage(this.ctx(db,actor),kind,filters));}
 async notifications(actor,filters){return this.store.read(db=>notificationPage(this.ctx(db,actor),filters));}
 async memories(actor,filters){return this.store.read(db=>memoryFeed(this.ctx(db,actor),filters));}
 async messages(actor,threadId,before,limit){return this.store.read(db=>messagesPage(this.ctx(db,actor),threadId,before,limit));}
 async webhook(raw,headers){return this.store.transaction(db=>billingWebhook(this.ctx(db),raw,headers));}
 async revenuecat(raw,headers){return this.store.transaction(db=>revenueCatWebhook(this.ctx(db),raw,headers));}
 async upload(actor,bytes,mime,requestKey=null){
  ensure(actor,401,'UNAUTHENTICATED','Giriş gerekiyor.');
  if(requestKey!=null)ensure(typeof requestKey==='string'&&/^[A-Za-z0-9_.:-]{8,120}$/.test(requestKey),400,'IDEMPOTENCY_REQUIRED','Yükleme anahtarı geçersiz.');
  const requestHash=requestKey?digest(Buffer.concat([Buffer.from(mime),bytes])):null;
  const previous=async db=>{
   if(!requestKey)return null;
   const receipt=await db.get('SELECT * FROM upload_receipts WHERE owner_id=? AND request_key=?',[actor.id,requestKey]);
   if(!receipt)return null;
   ensure(receipt.request_hash===requestHash,409,'IDEMPOTENCY_CONFLICT','Yükleme anahtarı farklı bir fotoğrafa ait.');
   const saved=await db.get("SELECT id,mime,size FROM uploads WHERE id=? AND owner_id=? AND status='ready'",[receipt.upload_id,actor.id]);
   ensure(saved,409,'UPLOAD_EXPIRED','Önceki yükleme artık kullanılamıyor. Yeniden fotoğraf seç.');
   return {...saved};
  };
  const replay=await previous(this.store);if(replay)return replay;
  const prepared=await prepareUpload(bytes,mime),now=this.clock();
  await this.store.read(async db=>{
   const limit=await db.get('SELECT count,resets_at FROM rate_limits WHERE bucket=?',[`media:${actor.id}`]);
   ensure(!limit||Number(limit.resets_at)<=now||limit.count<20,429,'RATE_LIMITED','Çok fazla deneme. Biraz sonra tekrar dene.');
   const total=await db.get('SELECT COALESCE(SUM(size),0) AS n FROM uploads WHERE owner_id=? AND created_at>?',[actor.id,now-86400000]);
   ensure(Number(total.n)+prepared.bytes.length<=100*1024*1024,429,'UPLOAD_QUOTA','Bugünkü fotoğraf yükleme sınırına ulaştın.');
  });
  await this.media.put(prepared.uploadId,prepared.bytes);
  try{
   const result=await this.store.transaction(async db=>{
    const replay=await previous(db);if(replay)return replay;
    await rateLimit(db,`media:${actor.id}`,this.clock(),20,3600000);
    const total=await db.get('SELECT COALESCE(SUM(size),0) AS n FROM uploads WHERE owner_id=? AND created_at>?',[actor.id,this.clock()-86400000]);
    ensure(Number(total.n)+prepared.bytes.length<=100*1024*1024,429,'UPLOAD_QUOTA','Bugünkü fotoğraf yükleme sınırına ulaştın.');
    const result=await recordUpload(this.ctx(db,actor),prepared.uploadId,prepared.bytes.length,prepared.mime);
    if(requestKey)await db.run('INSERT INTO upload_receipts(owner_id,request_key,request_hash,upload_id,created_at) VALUES(?,?,?,?,?)',[actor.id,requestKey,requestHash,prepared.uploadId,this.clock()]);
    return result;
   });
   if(result.id!==prepared.uploadId)await this.media.remove(prepared.uploadId).catch(()=>{});
   return result;
  }catch(error){await this.media.remove(prepared.uploadId).catch(()=>{});throw error;}
 }
 async mediaFile(actor,uploadId){
  const file=await this.store.read(db=>authorizeUploadRead(this.ctx(db,actor),uploadId));
  return{bytes:await this.media.get(file.objectKey),mime:file.mime};
 }
 async operatorVenues(actor){return this.store.read(async db=>{const current=actor?.id?await db.get('SELECT role,status FROM users WHERE id=?',[actor.id]):null;ensure(current?.status==='active'&&['admin','venue_owner'].includes(current.role),403,'FORBIDDEN','Güncel tesis yetkisi gerekiyor.');return operatorVenues(this.ctx(db,{...actor,role:current.role}));});}
 async venueReviews(actor,venueId,filters={}){return this.store.read(db=>venueReviews(this.ctx(db,actor),venueId,filters));}
 async adminInbox(actor,filters={}){return this.store.read(async db=>{
  const current=actor?.id?await db.get('SELECT role,status FROM users WHERE id=?',[actor.id]):null;ensure(current?.role==='admin'&&current.status==='active',403,'FORBIDDEN','Yönetici yetkisi gerekiyor.');
  const photoRows=await db.all("SELECT s.*,p.payload AS profile_payload,u.phone AS user_phone FROM support_requests s LEFT JOIN profiles p ON p.user_id=s.user_id JOIN users u ON u.id=s.user_id WHERE s.category='Fotoğraf doğrulaması' AND s.status='open' ORDER BY s.created_at LIMIT 100");
  const photos=photoRows.map(request=>{const photoData=request.profile_payload?json(request.profile_payload):null;const upload=request.message.match(/[a-f0-9-]{36}/)?.[0];const {profile_payload,user_phone,...base}=request;return{...base,uploadId:upload??'',stale:!photoData||photoData.avatarUploadId!==upload,requesterName:photoData?.name??'Sporcu',requesterCity:photoData?.city??'',requesterArea:photoData?.area??'',requesterPhone:user_phone??'',currentAvatarUploadId:photoData?.avatarUploadId??null,verifications:photoData?.verifications??[]};});
  const supportRows=await db.all("SELECT s.*,p.payload AS profile_payload,u.phone AS user_phone,u.email AS user_email FROM support_requests s LEFT JOIN profiles p ON p.user_id=s.user_id JOIN users u ON u.id=s.user_id WHERE s.category<>'Fotoğraf doğrulaması' AND s.status='open' ORDER BY s.created_at LIMIT 100");
  const support=supportRows.map(request=>{const data=request.profile_payload?json(request.profile_payload):null;const {profile_payload,user_phone,user_email,...base}=request;return{...base,requesterName:data?.name??'Sporcu',requesterPhone:user_phone??'',requesterEmail:user_email??''};});
  const reports=await db.all("SELECT r.*,p.payload AS reporter_payload FROM reports r LEFT JOIN profiles p ON p.user_id=r.user_id WHERE r.status='open' ORDER BY r.created_at LIMIT 100");
  const targets={event:'events',memory:'memories',message:'messages',team:'teams',venue:'venues',venue_review:'venue_reviews',thread:'threads',user:'users'};
  for(const report of reports){
   report.reporterName=report.reporter_payload?json(report.reporter_payload).name??'Sporcu':'Sporcu';
   delete report.reporter_payload;
   const table=targets[report.target_type];
   if(table){
    const target=await db.get(`SELECT * FROM ${table} WHERE id=?`,[report.target_id]);
    if(report.target_type==='user'){
     const targetProfile=await db.get('SELECT payload FROM profiles WHERE user_id=?',[report.target_id]);
     const pData=targetProfile?json(targetProfile.payload):null;
     report.context=target?{title:pData?.name??'Kullanıcı',description:[pData?.city,pData?.area,target.status].filter(Boolean).join(' · '),status:target.status??''}:null;
     report.targetOwnerId=report.target_id;
     report.targetUploadId=pData?.avatarUploadId??null;
    }else{
     const data=target?.payload?json(target.payload):target;
     report.context=data?{title:data.title??data.name??data.subject??'Bildirilen kayıt',description:data.description??data.caption??data.comment??data.body??'',status:target?.status??''}:null;
     report.targetOwnerId=target?.author_id??target?.sender_id??target?.host_id??target?.captain_id??target?.owner_id??target?.user_id??null;
     report.targetUploadId=target?.upload_id??(Array.isArray(data?.photoUploadIds)?data.photoUploadIds[0]??null:null);
     report.targetVersion=Number(target?.version??data?.version??1);
     report.targetStatus=target?.status??'';
     if(report.target_type==='message'&&target){
      const msgMeta=await db.get('SELECT version,deleted_at FROM message_metadata WHERE message_id=?',[target.id]);
      report.targetVersion=msgMeta?.version??1;
      report.targetDeleted=!!msgMeta?.deleted_at;
     }
    }
   }
  }
  const venueApplications=(await db.all("SELECT a.*,p.payload AS applicant_payload,u.phone AS applicant_phone FROM venue_applications a LEFT JOIN profiles p ON p.user_id=a.user_id JOIN users u ON u.id=a.user_id WHERE a.status='pending' ORDER BY a.created_at LIMIT 100")).map(a=>{const {applicant_payload,applicant_phone,...base}=a;const pData=applicant_payload?json(applicant_payload):null;return{...base,...json(a.payload),applicantName:pData?.name??'Başvuran',applicantPhone:applicant_phone??''};});
  const memories=(await db.all("SELECT m.*,p.payload AS author_payload FROM memories m LEFT JOIN profiles p ON p.user_id=m.author_id WHERE m.status='pending' ORDER BY m.created_at LIMIT 100")).map(m=>{const d=json(m.payload),ap=m.author_payload?json(m.author_payload):null;const {author_payload,...base}=m;return{...base,authorName:ap?.name??'Sporcu',caption:d.caption??'',sport:d.sport??'',venue:d.venue??'',area:d.area??'',city:d.city??'',sceneTheme:d.sceneTheme??'NIGHT_TURF'};});
  const results=[];
  for(const r of await db.all("SELECT r.*,e.payload AS event_payload,e.status AS event_status FROM results r JOIN events e ON e.id=r.event_id WHERE r.status='disputed' OR (r.status='pending' AND r.dispute_until<=?) ORDER BY r.created_at LIMIT 100",[this.clock()])){
   const ep=json(r.event_payload),rp=json(r.payload),dispute=await db.get("SELECT rep.details,p.payload AS rep_payload FROM reports rep LEFT JOIN profiles p ON p.user_id=rep.user_id WHERE rep.target_type='event' AND rep.target_id=? AND rep.reason='Skor itirazı' ORDER BY rep.created_at DESC LIMIT 1",[r.event_id]);
   const subP=await db.get('SELECT payload FROM profiles WHERE user_id=?',[r.submitted_by]);
   results.push({...r,eventTitle:ep.title??'Maç',homeScore:rp.homeScore??0,awayScore:rp.awayScore??0,submitterName:subP?json(subP.payload).name??'Sporcu':'Sporcu',disputeReason:dispute?.details??'',disputedByName:dispute?.rep_payload?json(dispute.rep_payload).name??'':'',eventStatus:r.event_status});
  }
  const reviews=(await db.all("SELECT r.*,v.payload AS venue_payload,p.payload AS reviewer_payload FROM venue_reviews r JOIN venues v ON v.id=r.venue_id LEFT JOIN profiles p ON p.user_id=r.user_id WHERE r.status='pending' ORDER BY r.created_at LIMIT 100")).map(r=>{const {reviewer_payload,...base}=r;return{...base,venueName:json(r.venue_payload).name??'Tesis',reviewerName:reviewer_payload?json(reviewer_payload).name??'Sporcu':'Sporcu'};});
  const users=(await db.all("SELECT u.id,u.role,u.status,p.payload FROM users u JOIN profiles p ON p.user_id=u.id WHERE u.status IN ('active','suspended') ORDER BY u.created_at DESC LIMIT 200")).map(u=>({id:u.id,role:u.role,status:u.status,name:json(u.payload).name}));
  const searchQuery=typeof filters.q==='string'?filters.q.trim().toLocaleLowerCase('tr'):'';
  let userSql="SELECT u.id,u.role,u.status,u.phone,u.email,u.created_at,p.payload FROM users u LEFT JOIN profiles p ON p.user_id=u.id WHERE u.status IN ('active','suspended')";
  const userParams=[];
  if(searchQuery){const pat=`%${searchQuery}%`;userSql+=" AND (LOWER(COALESCE(p.payload,'')) LIKE ? OR LOWER(COALESCE(u.phone,'')) LIKE ? OR LOWER(COALESCE(u.email,'')) LIKE ? OR LOWER(u.id) LIKE ?)";userParams.push(pat,pat,pat,pat);}
  userSql+=' ORDER BY u.created_at DESC LIMIT ?';userParams.push(searchQuery?100:200);
  const userDirectory=(await db.all(userSql,userParams)).map(u=>{const p=u.payload?json(u.payload):{};return{id:u.id,role:u.role,status:u.status,name:p.name??'Yeni hesap',phone:u.phone??'',email:u.email??'',city:p.city??'',area:p.area??'',verifications:p.verifications??[],createdAt:Number(u.created_at)};});
  const storedFlags=new Map((await db.all('SELECT name,enabled,updated_at FROM feature_flags')).map(f=>[f.name,f]));
  const featureFlags=['billing','chat','memories','tournaments','venues'].map(name=>({name,enabled:storedFlags.get(name)?storedFlags.get(name).enabled!==0:true,updatedAt:Number(storedFlags.get(name)?.updated_at??0)}));
  const legal=await legalConfiguration({db,config:this.config});
  const deletionRequests=(await db.all("SELECT d.*,p.payload AS profile_payload,u.phone FROM deletion_requests d LEFT JOIN profiles p ON p.user_id=d.user_id JOIN users u ON u.id=d.user_id WHERE d.status='pending' ORDER BY d.execute_after LIMIT 100")).map(d=>({userId:d.user_id,name:d.profile_payload?json(d.profile_payload).name??'Sporcu':'Sporcu',phone:d.phone??'',executeAfter:Number(d.execute_after),createdAt:Number(d.created_at)}));
  const outboxFailures=(await db.all("SELECT o.id,o.user_id,o.kind,o.attempts,o.payload,o.created_at,p.payload AS recipient_payload FROM outbox o LEFT JOIN profiles p ON p.user_id=o.user_id WHERE o.status='failed' ORDER BY o.created_at DESC LIMIT 100")).map(o=>{const op=o.payload?json(o.payload):{};const rp=o.recipient_payload?json(o.recipient_payload):null;return{id:o.id,user_id:o.user_id,kind:o.kind,attempts:o.attempts,title:op.title??'Bildirim',destination:op.destination??'',recipientName:rp?.name??o.user_id,createdAt:Number(o.created_at)};});
  const identityDeletions=await db.all("SELECT user_id AS id,status,attempts,next_at,last_error,created_at FROM identity_deletions WHERE status<>'completed' ORDER BY created_at LIMIT 100");
  const announcements=(await db.all("SELECT metadata,created_at FROM audit_log WHERE action='admin.announcement.send' ORDER BY created_at DESC LIMIT 10")).map(row=>({...json(row.metadata),createdAt:Number(row.created_at)}));
  const recentAudit=(await db.all("SELECT a.id,a.actor_id,a.action,a.target_id,a.metadata,a.created_at,p.payload AS actor_payload FROM audit_log a LEFT JOIN profiles p ON p.user_id=a.actor_id WHERE a.action LIKE 'admin.%' OR a.action IN ('report.create','result.dispute','account.deletion.request','venue.application.create') ORDER BY a.created_at DESC LIMIT 40")).map(a=>({id:a.id,actorId:a.actor_id??'',actorName:a.actor_payload?json(a.actor_payload).name??'Sistem':'Sistem',action:a.action,targetId:a.target_id,createdAt:Number(a.created_at),summary:json(a.metadata)?.reason??json(a.metadata)?.response??json(a.metadata)?.status??''}));
  return{identityDeletions,venueApplications,reports,support,photos,memories,results,reviews,users,userDirectory,featureFlags,legalDocuments:legal,deletionRequests,recentAudit,announcements,outboxFailures,
   counts:{photos:photos.length,reports:reports.length,venueApplications:venueApplications.length,memories:memories.length,results:results.length,reviews:reviews.length,support:support.length,outboxFailures:outboxFailures.length,identityDeletions:identityDeletions.length,deletionRequests:deletionRequests.length}};
 });}

}
