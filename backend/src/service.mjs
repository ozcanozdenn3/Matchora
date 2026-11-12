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
import { snapshot,eventList,messagesPage,eventView,canSeeEvent,publicPlayer,chatInbox,chatDetail } from './queries.mjs';
import { revenueCatWebhook } from './revenuecat.mjs';
import { billingWebhook } from './billing.mjs';
import { upload,readUpload } from './media.mjs';
export const handlers={...venueApplicationHandlers,...passportHandlers,...storeHandlers,...managementHandlers,...accountHandlers,...legalHandlers,...identityHandlers,...eventHandlers,...eventManagementHandlers,...competitionHandlers,...tournamentManagementHandlers,...networkHandlers,...communityManagementHandlers,...operationHandlers,...venueScheduleHandlers,...socialHandlers,...memoryManagementHandlers,...chatManagementHandlers,...adminHandlers};
export function canonical(value){if(Array.isArray(value))return `[${value.map(canonical).join(',')}]`;if(value&&typeof value==='object')return `{${Object.keys(value).sort().map(k=>`${JSON.stringify(k)}:${canonical(value[k])}`).join(',')}}`;return JSON.stringify(value);}
export class MatchoraService{
 constructor(store,config,media,clock=Date.now){this.store=store;this.config=config;this.media=media;this.clock=clock;}
 ctx(db,actor=null,scope={}){return{db,actor,now:this.clock(),config:this.config,discoveryCity:scope.city?text(scope.city,'İl',2,60):undefined};}
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
   const feature=operation.split('.')[0],flagMap={memory:'memories',tournament:'tournaments',venue:'venues',booking:'venues',chat:'chat'};if(flagMap[feature]&&!exitOperations.has(operation)){const flag=await db.get('SELECT enabled FROM feature_flags WHERE name=?',[flagMap[feature]]);ensure(!flag||flag.enabled,503,'FEATURE_UNAVAILABLE','Bu özellik geçici olarak kullanılamıyor.');}
   const result=await handlers[operation](ctx,payload);await db.run('INSERT INTO commands(user_id,operation,request_key,request_hash,response,created_at) VALUES(?,?,?,?,?,?)',[actor.id,operation,key,hash,encode(result),ctx.now]);return{result,snapshot:await snapshot(ctx),replayed:false};
  });}catch(error){if(error instanceof ApiError)throw error;if(error.code?.startsWith('SQLITE_CONSTRAINT')||error.code==='23505')throw new ApiError(409,'CONFLICT','Bu kayıt zaten var veya işlem mevcut duruma uygun değil.');throw error;}
 }
 async configuration(){return this.store.transaction(async db=>{const flag=await db.get('SELECT enabled FROM feature_flags WHERE name=?',['billing']),store=storeConfiguration(this.config);return{fees:{tournament:20,listing:10},ratingPolicy:RATING_POLICY,...await legalConfiguration(this.ctx(db)),auth:{social:oauthConfigured(this.config),email:oauthConfigured(this.config),phone:smsConfigured(this.config),development:this.config.devAuth},billing:!!this.config.billingSecret&&flag?.enabled!==0,store:{...store,enabled:store.enabled&&flag?.enabled!==0},supportEmail:this.config.supportEmail??null};});}
 async player(actor,userId,sport){return this.store.transaction(db=>playerDetails(this.ctx(db,actor),userId,sport));}
 async bootstrap(actor=null,scope={}){return this.store.transaction(async db=>{if(actor){const current=await db.get('SELECT role,status FROM users WHERE id=?',[actor.id]);ensure(current?.status==='active',401,'ACCOUNT_DISABLED','Hesap kullanılamıyor.');actor={...actor,role:current.role};}return snapshot(this.ctx(db,actor,scope));});}
 async verifyPassport(code){return this.store.transaction(db=>verifyPassport(this.ctx(db),code));}
 async exportAccount(actor){return this.store.transaction(db=>exportAccount(this.ctx(db,actor)));}
 async sessions(actor){return this.store.transaction(async db=>({items:(await db.all("SELECT id,created_at,expires_at FROM sessions WHERE user_id=? AND revoked=0 AND refresh_expires_at>? ORDER BY created_at DESC",[actor.id,this.clock()])).map(s=>({...s,current:s.id===actor.sessionId}))}));}
 async event(actor,eventId){ensure(typeof eventId==='string'&&/^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$/.test(eventId),400,'INVALID_EVENT_ID','Geçerli bir ilan ID’si gir.');return this.store.transaction(async db=>{const ctx=this.ctx(db,actor),event=await row(ctx,'events','id',eventId.toLowerCase());ensure(!['DRAFT','ARCHIVED'].includes(event.status)&&await canSeeEvent(ctx,event),404,'NOT_FOUND','İlan bulunamadı veya bu ağa erişim iznin yok.');const view=await eventView(ctx,event);return{...view,players:await Promise.all([...new Set([view.hostId,...view.participantIds])].map(id=>publicPlayer(ctx,id)))};});}
 async threads(actor,filters){return this.store.transaction(db=>chatInbox(this.ctx(db,actor),filters));}
 async thread(actor,id){return this.store.transaction(db=>chatDetail(this.ctx(db,actor),id));}
 async events(actor,filters){return this.store.transaction(db=>eventList(this.ctx(db,actor),filters));}
 async rankings(actor,filters){return this.store.transaction(db=>rankingPage(this.ctx(db,actor),filters));}
 async catalogItem(actor,kind,id){return this.store.transaction(db=>catalogDetail(this.ctx(db,actor),kind,id));}
 async catalog(actor,kind,filters){return this.store.transaction(db=>catalogPage(this.ctx(db,actor),kind,filters));}
 async notifications(actor,filters){return this.store.transaction(db=>notificationPage(this.ctx(db,actor),filters));}
 async memories(actor,filters){return this.store.transaction(db=>memoryFeed(this.ctx(db,actor),filters));}
 async messages(actor,threadId,before,limit){return this.store.transaction(db=>messagesPage(this.ctx(db,actor),threadId,before,limit));}
 async webhook(raw,headers){return this.store.transaction(db=>billingWebhook(this.ctx(db),raw,headers));}
 async revenuecat(raw,headers){return this.store.transaction(db=>revenueCatWebhook(this.ctx(db),raw,headers));}
 async upload(actor,bytes,mime){return this.store.transaction(async db=>{await rateLimit(db,`media:${actor.id}`,this.clock(),20,3600000);const total=await db.get('SELECT COALESCE(SUM(size),0) AS n FROM uploads WHERE owner_id=? AND created_at>?',[actor.id,this.clock()-86400000]);ensure(Number(total.n)+bytes.length<=100*1024*1024,429,'UPLOAD_QUOTA','Bugünkü fotoğraf yükleme sınırına ulaştın.');return upload(this.ctx(db,actor),this.media,bytes,mime);});}
 async mediaFile(actor,uploadId){return this.store.transaction(db=>readUpload(this.ctx(db,actor),this.media,uploadId));}
 async operatorVenues(actor){return this.store.transaction(async db=>{const current=actor?.id?await db.get('SELECT role,status FROM users WHERE id=?',[actor.id]):null;ensure(current?.status==='active'&&['admin','venue_owner'].includes(current.role),403,'FORBIDDEN','Güncel tesis yetkisi gerekiyor.');return operatorVenues(this.ctx(db,{...actor,role:current.role}));});}
 async venueReviews(actor,venueId,filters={}){return this.store.transaction(db=>venueReviews(this.ctx(db,actor),venueId,filters));}
 async adminInbox(actor){return this.store.transaction(async db=>{
  const current=actor?.id?await db.get('SELECT role,status FROM users WHERE id=?',[actor.id]):null;ensure(current?.role==='admin'&&current.status==='active',403,'FORBIDDEN','Yönetici yetkisi gerekiyor.');
  const support=await db.all("SELECT * FROM support_requests WHERE status='open' ORDER BY created_at LIMIT 100");
  const photos=[];for(const request of support.filter(r=>r.category==='Fotoğraf doğrulaması')){const photo=await db.get('SELECT payload FROM profiles WHERE user_id=?',[request.user_id]);const upload=request.message.match(/[a-f0-9-]{36}/)?.[0];photos.push({...request,uploadId:upload??'',stale:!photo||json(photo.payload).avatarUploadId!==upload});}
  const reports=await db.all("SELECT * FROM reports WHERE status='open' ORDER BY created_at LIMIT 100");
  const targets={event:'events',memory:'memories',message:'messages',team:'teams',venue:'venues',venue_review:'venue_reviews',thread:'threads'};
  for(const report of reports){const table=targets[report.target_type];if(table){const target=await db.get(`SELECT * FROM ${table} WHERE id=?`,[report.target_id]);const data=target?.payload?json(target.payload):target;report.context=data?{title:data.title??data.name??data.subject??'Bildirilen kayıt',description:data.description??data.caption??data.comment??data.body??'',status:target?.status??''}:null;}}
  return{venueApplications:(await db.all("SELECT a.*,p.payload AS applicant_payload FROM venue_applications a JOIN profiles p ON p.user_id=a.user_id WHERE a.status='pending' ORDER BY a.created_at LIMIT 100")).map(a=>({...a,...json(a.payload),applicantName:json(a.applicant_payload).name})),reports,support:support.filter(r=>r.category!=='Fotoğraf doğrulaması'),photos,
   memories:await db.all("SELECT * FROM memories WHERE status='pending' LIMIT 100"),
   results:await db.all("SELECT r.*,e.payload AS event_payload FROM results r JOIN events e ON e.id=r.event_id WHERE r.status='disputed' OR (r.status='pending' AND r.dispute_until<=?) ORDER BY r.created_at LIMIT 100",[this.clock()]),
   reviews:await db.all("SELECT r.*,v.payload AS venue_payload FROM venue_reviews r JOIN venues v ON v.id=r.venue_id WHERE r.status='pending' ORDER BY r.created_at LIMIT 100"),
   users:(await db.all("SELECT u.id,u.role,u.status,p.payload FROM users u JOIN profiles p ON p.user_id=u.id WHERE u.status IN ('active','suspended') ORDER BY u.created_at DESC LIMIT 200")).map(u=>({id:u.id,role:u.role,status:u.status,name:json(u.payload).name})),
   announcements:(await db.all("SELECT metadata,created_at FROM audit_log WHERE action='admin.announcement.send' ORDER BY created_at DESC LIMIT 10")).map(row=>({...json(row.metadata),createdAt:Number(row.created_at)})),
   outboxFailures:await db.all("SELECT id,user_id,kind,attempts FROM outbox WHERE status='failed' LIMIT 100")};
 });}

}
