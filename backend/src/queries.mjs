import { publicMemoryPayload } from './memory-feed.mjs';
import { meetingLocation } from './event-locations.mjs';
import { catalogPage, catalogItem } from './catalog.mjs';
import { notificationPage } from './notification-feed.mjs';
import { rankingRows } from './rankings.mjs';
import { chatMemberState,chatMessageRows,chatUnread,messageContent } from './chat-state.mjs';
import { eventCursor, readEventCursor } from './event-cursor.mjs';
import { ratingStatus } from './rating.mjs';
import { legalConfiguration } from './legal.mjs';
import { progression } from './progression.mjs';
import { tournamentDetails } from './tournament-details.mjs';
import { teamStatistics } from './team-matches.mjs';
import { ensure, json, text } from './errors.mjs';
import { matchScore } from './matchmaking.mjs';
import { eventNetworkMembership } from './event-network.mjs';
import { sealCursor, openCursor } from './opaque-cursor.mjs';
import { assertThread, profile, wallet, row } from './model.mjs';
export const dateLabel = stamp => new Intl.DateTimeFormat('tr-TR',{timeZone:'Europe/Istanbul',day:'numeric',month:'short'}).format(Number(stamp));
export const timeLabel = stamp => new Intl.DateTimeFormat('tr-TR',{timeZone:'Europe/Istanbul',hour:'2-digit',minute:'2-digit',hour12:false}).format(Number(stamp));
export async function publicPlayer(ctx,userId) {
 ctx.publicPlayers??=new Map();if(ctx.publicPlayers.has(userId))return ctx.publicPlayers.get(userId);
 const row = await ctx.db.get("SELECT u.id, u.status, p.payload AS profile_payload, pr.privacy AS prefs_privacy FROM users u LEFT JOIN profiles p ON p.user_id=u.id LEFT JOIN preferences pr ON pr.user_id=u.id WHERE u.id=?", [userId]);
 if (!row || row.status !== 'active') return {id:userId,name:'Silinen sporcu',skills:[],area:'',reliability:0,verifiedMatches:0,verifications:[]};
 if (!row.profile_payload) return {id:userId,name:'Yeni sporcu',skills:[],area:'',reliability:0,verifiedMatches:0,verifications:[]};
 const data=json(row.profile_payload),privacy=row.prefs_privacy?json(row.prefs_privacy):{};
 const result={id:userId,name:data.name,city:data.city,area:data.area,bio:data.bio??'',dominantSide:data.dominantSide??'',availability:data.availability??[],campus:data.campus??null,company:data.company??null,jerseyNumber:data.jerseyNumber??null,skills:data.skills,verifications:data.verifications,verifiedMatches:data.verifiedMatches,reliability:privacy.showReliability===false?0:data.reliability,reliabilityVisible:privacy.showReliability!==false,fairPlay:data.fairPlay,avatarUploadId:data.avatarUploadId};ctx.publicPlayers.set(userId,result);return result;
}
export async function eventView(ctx,event){const own=ctx.actor?(ctx.ownProfileRow??await ctx.db.get('SELECT payload FROM profiles WHERE user_id=?',[ctx.actor.id])):null;const score=await matchScore(ctx,event,own?json(own.payload):null);const data=json(event.payload),members=await ctx.db.all("SELECT user_id,side,checked_in_at FROM event_members WHERE event_id=? AND status='joined' ORDER BY joined_at,user_id",[event.id]),waiting=await ctx.db.get("SELECT COUNT(*) AS n FROM waitlist WHERE event_id=? AND status IN ('waiting','offered')",[event.id]);return{...data,meetingLocation:await meetingLocation(ctx,event),score,id:event.id,hostId:event.host_id,status:event.status,capacity:event.capacity,startsAtEpochMillis:Number(event.starts_at),startsInMin:Math.round((Number(event.starts_at)-ctx.now)/60000),durationMin:Math.round((Number(event.ends_at)-Number(event.starts_at))/60000),day:dateLabel(event.starts_at),time:timeLabel(event.starts_at),participantIds:members.map(m=>m.user_id),checkedInParticipantIds:members.filter(m=>m.checked_in_at).map(m=>m.user_id),homeParticipantIds:members.filter(m=>m.side==='home').map(m=>m.user_id),awayParticipantIds:members.filter(m=>m.side==='away').map(m=>m.user_id),waitlistCount:waiting.n,commitmentTokens:event.commitment,version:event.version,approximateLocation:event.latitude==null?null:{latitude:Math.round(event.latitude*100)/100,longitude:Math.round(event.longitude*100)/100}};}
export async function canSeeEvent(ctx,event){const p=json(event.payload);if(!['CAMPUS','COMPANY'].includes(p.networkScope))return true;return !!await eventNetworkMembership(ctx,p);}
async function threadView(ctx,t) {
 const memberState=await chatMemberState(ctx,t.id);if(memberState.hidden)return null;
 const revision=await ctx.db.get('SELECT revision FROM thread_revisions WHERE thread_id=?',[t.id]);
 let blocked=false;try{await assertThread(ctx,t.id);}catch{blocked=true;}
 const members=await ctx.db.all('SELECT user_id FROM thread_members WHERE thread_id=?',[t.id]);let title=t.title;
 if(t.kind==='DIRECT'){const other=members.find(m=>m.user_id!==ctx.actor.id);title=other?(await publicPlayer(ctx,other.user_id)).name:title;}
 const messages=blocked?[]:(await chatMessageRows(ctx,t.id)).reverse(),unread=blocked?0:await chatUnread(ctx,t.id);
 return{id:t.id,type:t.kind,title,memberIds:members.map(m=>m.user_id),memberCount:members.length,relatedEventId:t.event_id,activityAt:Number(t.activity_at??t.created_at),historyRevision:`${Number(revision?.revision??0)}:${memberState.version}`,nextMessageCursor:messages.length===100?Buffer.from(JSON.stringify([Number(messages[0].created_at),messages[0].id,Number(messages[0].sequence)])).toString('base64url'):null,messages:await Promise.all(messages.map(m=>messageView(ctx,m))),unread,muted:!!t.muted,locked:blocked};
}
async function unreadThreadCount(ctx) {
 const candidates=await ctx.db.all(`SELECT t.id FROM threads t JOIN thread_members tm ON tm.thread_id=t.id
 LEFT JOIN thread_member_state s ON s.thread_id=t.id AND s.user_id=tm.user_id
 WHERE tm.user_id=? AND COALESCE(s.hidden,0)=0 AND EXISTS(
 SELECT 1 FROM messages m LEFT JOIN message_metadata x ON x.message_id=m.id WHERE m.thread_id=t.id AND m.sender_id<>?
 AND x.deleted_at IS NULL AND ((x.sequence IS NULL AND m.created_at>tm.read_at) OR
 (x.sequence>COALESCE(s.read_sequence,0) AND x.sequence>COALESCE(s.clear_sequence,0)))
 AND NOT EXISTS(SELECT 1 FROM message_hides h WHERE h.message_id=m.id AND h.user_id=?))`,[ctx.actor.id,ctx.actor.id,ctx.actor.id]);
 let count=0;for(const thread of candidates){try{await assertThread(ctx,thread.id);}catch{continue;}if(await chatUnread(ctx,thread.id)>0)count++;}
 return count;
}
export async function chatInbox(ctx,filters={}) {
 ensure(ctx.actor,401,'UNAUTHENTICATED','Giriş yapmalısın.');
 const size=Math.min(100,Math.max(1,Number(filters.limit)||100)),cursor=filters.after?openCursor(ctx,'chat-inbox',filters.after):null;
 if(cursor)ensure(Array.isArray(cursor)&&cursor.length===2&&Number.isSafeInteger(cursor[0])&&typeof cursor[1]==='string',400,'INVALID_CURSOR','Sohbet sayfası geçersiz.');
 let sql="SELECT * FROM (SELECT t.*,m.muted,CASE WHEN COALESCE(s.updated_at,0)>COALESCE((SELECT MAX(created_at) FROM messages z WHERE z.thread_id=t.id),t.created_at) THEN s.updated_at ELSE COALESCE((SELECT MAX(created_at) FROM messages z WHERE z.thread_id=t.id),t.created_at) END AS activity_at FROM threads t JOIN thread_members m ON m.thread_id=t.id LEFT JOIN thread_member_state s ON s.thread_id=t.id AND s.user_id=m.user_id WHERE m.user_id=? AND COALESCE(s.hidden,0)=0) inbox";
 const params=[ctx.actor.id];if(cursor){sql+=' WHERE (activity_at<? OR (activity_at=? AND id<?))';params.push(cursor[0],cursor[0],cursor[1]);}
 sql+=' ORDER BY activity_at DESC,id DESC LIMIT ?';params.push(size+1);
 const rows=await ctx.db.all(sql,params),items=[];for(const t of rows.slice(0,size)){const view=await threadView(ctx,t);if(view)items.push(view);}
 const last=rows[size-1],ids=[...new Set(items.flatMap(t=>t.memberIds))];
 return{items,players:await Promise.all(ids.map(id=>publicPlayer(ctx,id))),unreadThreadCount:await unreadThreadCount(ctx),nextCursor:rows.length>size?sealCursor(ctx,'chat-inbox',[Number(last.activity_at),last.id]):null};
}
export async function chatDetail(ctx,threadId) {
 ensure(ctx.actor,401,'UNAUTHENTICATED','Giriş yapmalısın.');
 const t=await ctx.db.get('SELECT t.*,m.muted FROM threads t JOIN thread_members m ON m.thread_id=t.id WHERE t.id=? AND m.user_id=?',[threadId,ctx.actor.id]);
 ensure(t,404,'NOT_FOUND','Sohbet bulunamadı.');const item=await threadView(ctx,t);ensure(item,404,'NOT_FOUND','Sohbet bulunamadı.');
 return{item,players:await Promise.all(item.memberIds.map(id=>publicPlayer(ctx,id)))};
}
export async function eventList(ctx,filters={}){
 const size=Math.min(200,Math.max(1,Number(filters.limit)||100));
 const cursor=filters.after?readEventCursor(ctx,filters.after):null;
 let sql="SELECT * FROM events WHERE status IN ('PUBLISHED','FILLING','FULL','IN_PROGRESS') AND ends_at>?",params=[ctx.now];
 if(cursor){sql+=' AND (starts_at>? OR (starts_at=? AND id>?))';params.push(cursor[0],cursor[0],cursor[1]);}
 const city=filters.city??ctx.discoveryCity;
 if(city){sql+=' AND city=?';params.push(text(city,'İl',2,60));}
 if(filters.sport){sql+=' AND sport=?';params.push(filters.sport);}
 sql+=' ORDER BY starts_at,id LIMIT ?';params.push(size+1);
 const rows=await ctx.db.all(sql,params),items=[];
 for(const event of rows.slice(0,size))if(await canSeeEvent(ctx,event))items.push(await eventView(ctx,event));
 return{items,nextCursor:rows.length>size?eventCursor(ctx,rows[size-1]):null};
}
async function mergeOwnCatalog(ctx,kind,publicItems,records){
 const merged=new Map(publicItems.map(item=>[item.id,item]));
 for(const record of records)if(!merged.has(record.id)){const item=await catalogItem(ctx,kind,record);if(item)merged.set(item.id,item);}
 return [...merged.values()];
}
export async function snapshot(ctx){
 ctx.publicPlayers=new Map();ctx.ownProfileRow=ctx.actor?await ctx.db.get('SELECT * FROM profiles WHERE user_id=?',[ctx.actor.id]):null;
 const savedDiscovery=ctx.actor?await ctx.db.get('SELECT payload FROM discovery_preferences WHERE user_id=?',[ctx.actor.id]):null;
 const browseCity=ctx.discoveryCity??(savedDiscovery?json(savedDiscovery.payload).city:ctx.ownProfileRow?json(ctx.ownProfileRow.payload).city:null);
 const publicRows=await ctx.db.all("SELECT * FROM events WHERE status IN ('PUBLISHED','FILLING','FULL','IN_PROGRESS') AND ends_at>?"+(browseCity?' AND city=?':'')+" ORDER BY starts_at,id LIMIT 201",browseCity?[ctx.now,browseCity]:[ctx.now]);
 const publicEvents=publicRows.slice(0,200),nextEventCursor=publicRows.length>200?eventCursor(ctx,publicEvents[199]):null;
 const ownEvents=ctx.actor?await ctx.db.all("SELECT DISTINCT e.* FROM events e LEFT JOIN event_members m ON m.event_id=e.id AND m.user_id=? WHERE e.status NOT IN ('DRAFT','ARCHIVED') AND (e.host_id=? OR (m.user_id=? AND m.status='joined')) ORDER BY e.starts_at DESC LIMIT 100",[ctx.actor.id,ctx.actor.id,ctx.actor.id]):[];
 const activeOwnEvents=ctx.actor?await ctx.db.all("SELECT DISTINCT e.* FROM events e WHERE e.status IN ('PUBLISHED','FILLING','FULL','IN_PROGRESS') AND e.ends_at>? AND (e.host_id=? OR e.id IN (SELECT event_id FROM event_members WHERE user_id=? AND status='joined') OR e.id IN (SELECT scope_id FROM join_requests WHERE scope='event' AND user_id=? AND status='pending') OR e.id IN (SELECT event_id FROM waitlist WHERE user_id=? AND status IN ('waiting','offered')))",[ctx.now,ctx.actor.id,ctx.actor.id,ctx.actor.id,ctx.actor.id]):[];
 const eventRows=[...new Map([...publicEvents,...ownEvents,...activeOwnEvents].map(e=>[e.id,e])).values()].sort((a,b)=>Number(a.starts_at)-Number(b.starts_at)),events=[];for(const e of eventRows)if(await canSeeEvent(ctx,e))events.push(await eventView(ctx,e));
 const users=await ctx.db.all("SELECT user_id FROM profiles JOIN users ON users.id=profiles.user_id WHERE users.status='active' ORDER BY updated_at DESC LIMIT 200"),players=[];for(const u of users)players.push(await publicPlayer(ctx,u.user_id));
 const publicTeamPage=await catalogPage(ctx,'teams',{limit:100}),ownTeams=ctx.actor?await ctx.db.all("SELECT DISTINCT t.* FROM teams t LEFT JOIN team_members m ON m.team_id=t.id AND m.user_id=? WHERE t.captain_id=? OR m.status='joined' OR t.id IN (SELECT team_id FROM team_invites WHERE user_id=? AND status='pending')",[ctx.actor.id,ctx.actor.id,ctx.actor.id]):[];
 const teams=await mergeOwnCatalog(ctx,'teams',publicTeamPage.items,ownTeams);
 const publicTournamentPage=await catalogPage(ctx,'tournaments',{limit:100}),ownTournaments=ctx.actor?await ctx.db.all("SELECT DISTINCT t.* FROM tournaments t LEFT JOIN tournament_members tm ON tm.tournament_id=t.id AND tm.status='joined' LEFT JOIN team_members m ON m.team_id=tm.team_id AND m.user_id=? AND m.status='joined' WHERE t.status IN ('open','running') AND (t.creator_id=? OR m.user_id=?)",[ctx.actor.id,ctx.actor.id,ctx.actor.id]):[];
 const tournaments=await mergeOwnCatalog(ctx,'tournaments',publicTournamentPage.items,ownTournaments);
 const publicCommunityPage=await catalogPage(ctx,'communities',{limit:100}),ownCommunities=ctx.actor?await ctx.db.all("SELECT DISTINCT c.* FROM communities c LEFT JOIN community_members m ON m.community_id=c.id AND m.user_id=? WHERE c.owner_id=? OR m.status IN ('joined','invited','pending')",[ctx.actor.id,ctx.actor.id]):[];
 const communities=await mergeOwnCatalog(ctx,'communities',publicCommunityPage.items,ownCommunities);
 const publicVenuePage=await catalogPage(ctx,'venues',{limit:100}),venues=publicVenuePage.items;
 const memories=[];const memoryRows=await ctx.db.all("SELECT * FROM memories WHERE status='approved' OR (author_id=? AND status='pending') ORDER BY created_at DESC LIMIT 50",[ctx.actor?.id??'']);for(const m of memoryRows){if(ctx.actor&&await ctx.db.get('SELECT * FROM blocks WHERE (user_id=? AND target_id=?) OR (user_id=? AND target_id=?)',[ctx.actor.id,m.author_id,m.author_id,ctx.actor.id]))continue;const author=await publicPlayer(ctx,m.author_id),likes=await ctx.db.get('SELECT COUNT(*) AS n FROM memory_likes WHERE memory_id=?',[m.id]),liked=ctx.actor?await ctx.db.get('SELECT user_id FROM memory_likes WHERE memory_id=? AND user_id=?',[m.id,ctx.actor.id]):null;memories.push({...publicMemoryPayload(m.payload),id:m.id,authorId:m.author_id,authorName:author.name,authorAvatarUploadId:author.avatarUploadId??null,uploadId:m.upload_id,status:m.status,likes:likes.n,likedByMe:!!liked,createdAt:Number(m.created_at)});}
 const result={events,nextEventCursor,nextCatalogCursors:{teams:publicTeamPage.nextCursor,tournaments:publicTournamentPage.nextCursor,communities:publicCommunityPage.nextCursor,venues:publicVenuePage.nextCursor},players,teams,tournaments,communities,venues,memories,serverTime:ctx.now,ratingHistory:[],reliabilityHistory:[],rankings:[],featureFlags:await ctx.db.all('SELECT name,enabled FROM feature_flags'),profile:null,userId:null,threads:[],notifications:[],ledger:[],venuePayments:[],joinRequests:[],teamJoinRequests:[],challenges:[],results:[],submittedFeedbackTargets:[],supportRequests:[],reports:[],removedEventIds:[],removedEventMembers:[],removedTeamMembers:[],joinedEventIds:[],checkedInEventIds:[],waitlist:[],invitedPlayerIds:[],registeredTournamentIds:[],joinedCommunityIds:[],requestedCommunityIds:[],blockedUserIds:[],blockedThreadIds:[],wallet:{balance:0,locked:0},privacy:{},notificationPrefs:{}};
 result.rankings=await rankingRows(ctx);
 const localRanks=new Map();if(ctx.actor&&ctx.ownProfileRow){const own=json(ctx.ownProfileRow.payload);for(const skill of own.skills){const rank=result.rankings.filter(r=>r.sport===skill.sport&&r.city===own.city&&r.area===own.area).findIndex(r=>r.userId===ctx.actor.id);localRanks.set(skill.sport,rank<0?null:rank+1);}}
 result.rankings=result.rankings.slice(0,500);
 const publicRefs=new Set([...events.flatMap(e=>[e.hostId,...e.participantIds]),...teams.flatMap(t=>[t.captainId,...t.memberIds])]);
 for(const id of publicRefs)if(!players.some(p=>p.id===id))players.push(await publicPlayer(ctx,id));
 if(!ctx.actor)return result;
 const legal=await legalConfiguration(ctx);
 result.legalAcceptanceRequired=ctx.config.production&&!await ctx.db.get('SELECT id FROM legal_acceptances WHERE user_id=? AND terms_version=? AND privacy_version=?',[ctx.actor.id,legal.documents.terms.version??'',legal.documents.privacy.version??'']);
 const userId=ctx.actor.id,profileRow=ctx.ownProfileRow,prefs=await row(ctx,'preferences','user_id',userId);result.userId=userId;result.role=ctx.actor.role;result.profile=profileRow?{...json(profileRow.payload),version:profileRow.version}:null;if(result.profile)result.profile.skills=result.profile.skills.map(skill=>({...skill,localRank:localRanks.get(skill.sport)??null}));result.wallet=await wallet(ctx);result.privacy=json(prefs.privacy);result.notificationPrefs=json(prefs.notifications);
 result.removedEventIds=(await ctx.db.all("SELECT event_id FROM event_members WHERE user_id=? AND status='removed'",[userId])).map(r=>r.event_id);
 result.removedEventMembers=await ctx.db.all("SELECT m.event_id,m.user_id FROM event_members m JOIN events e ON e.id=m.event_id WHERE e.host_id=? AND m.status='removed' AND e.starts_at>?",[userId,ctx.now]);
 result.removedTeamMembers=await ctx.db.all("SELECT m.team_id,m.user_id FROM team_members m JOIN teams t ON t.id=m.team_id WHERE t.captain_id=? AND m.status='removed'",[userId]);
 result.venueApplications=(await ctx.db.all('SELECT * FROM venue_applications WHERE user_id=? ORDER BY created_at DESC LIMIT 100',[userId])).map(a=>({...json(a.payload),id:a.id,status:a.status,version:a.version,response:a.response,venueId:a.venue_id}));
 result.incomingCommunityInvites=await ctx.db.all("SELECT community_id FROM community_members WHERE user_id=? AND status='invited'",[userId]);
 result.ownedCommunityMembers=await ctx.db.all("SELECT m.community_id,m.user_id FROM community_members m JOIN communities c ON c.id=m.community_id WHERE c.owner_id=? AND m.status='joined'",[userId]);
 result.incomingTeamInvites=await ctx.db.all("SELECT team_id FROM team_invites WHERE user_id=? AND status='pending'",[userId]);
 result.communityRequests=await ctx.db.all("SELECT m.community_id,m.user_id FROM community_members m JOIN communities c ON c.id=m.community_id WHERE c.owner_id=? AND m.status='pending'",[userId]);
 result.ledger=(await ctx.db.all('SELECT * FROM wallet_entries WHERE user_id=? ORDER BY created_at DESC,id DESC LIMIT 200',[userId])).map(r=>({id:r.id,type:r.kind,amount:r.amount,lockedDelta:r.locked_delta,description:r.description,time:dateLabel(r.created_at),ref:r.ref,idempotencyKey:r.dedupe}));
 const membership=await ctx.db.all("SELECT * FROM event_members WHERE user_id=? AND status='joined'",[userId]);result.joinedEventIds=membership.map(m=>m.event_id);result.checkedInEventIds=membership.filter(m=>m.checked_in_at).map(m=>m.event_id);result.waitlist=await ctx.db.all("SELECT * FROM waitlist WHERE user_id=? AND status IN ('waiting','offered')",[userId]);
 result.joinRequests=await ctx.db.all("SELECT * FROM join_requests WHERE scope='event' AND (user_id=? OR scope_id IN (SELECT id FROM events WHERE host_id=?))",[userId,userId]);result.teamJoinRequests=await ctx.db.all("SELECT * FROM join_requests WHERE scope='team' AND (user_id=? OR scope_id IN (SELECT id FROM teams WHERE captain_id=?))",[userId,userId]);
 result.challenges=await ctx.db.all('SELECT * FROM challenges WHERE from_team IN (SELECT team_id FROM team_members WHERE user_id=?) OR to_team IN (SELECT team_id FROM team_members WHERE user_id=?)',[userId,userId]);
 result.results=await ctx.db.all("SELECT r.* FROM results r JOIN event_members m ON m.event_id=r.event_id WHERE m.user_id=? AND m.status='joined'",[userId]);
 for(const record of result.results)record.rating=await ratingStatus(ctx,record);
 result.submittedFeedbackTargets=await ctx.db.all('SELECT event_id,to_id FROM feedback WHERE from_id=?',[userId]);
 const inbox=await chatInbox(ctx);result.threads=inbox.items;result.nextThreadCursor=inbox.nextCursor;result.unreadThreadCount=inbox.unreadThreadCount;result.blockedThreadIds=inbox.items.filter(t=>t.locked&&t.type==='DIRECT').map(t=>t.id);
 result.blockedUserIds=(await ctx.db.all('SELECT target_id FROM blocks WHERE user_id=?',[userId])).map(r=>r.target_id);
 const notificationFeed=await notificationPage(ctx,{limit:100});result.notifications=notificationFeed.items;result.nextNotificationCursor=notificationFeed.nextCursor;result.unreadNotificationCount=notificationFeed.unreadCount;
 for(const b of await ctx.db.all(`SELECT b.*,s.venue_id,s.payload,s.starts_at,p.status AS payment_status FROM bookings b JOIN venue_slots s ON s.id=b.slot_id JOIN venue_payments p ON p.booking_id=b.id WHERE b.user_id=? AND (b.status IN ('pending','confirmed') OR b.id IN (SELECT id FROM bookings WHERE user_id=? ORDER BY created_at DESC,id DESC LIMIT 100)) ORDER BY b.created_at DESC`,[userId,userId])){const venue=await row(ctx,'venues','id',b.venue_id);result.venuePayments.push({id:b.id,venue:json(venue.payload).name,description:`${json(b.payload).court} · ${dateLabel(b.starts_at)} ${timeLabel(b.starts_at)}`,amountMinor:b.amount_minor,amountTry:Math.ceil(b.amount_minor/100),method:'Tesiste ödeme',time:dateLabel(b.created_at),bookingStatus:b.status,status:b.payment_status==='refunded'?'REFUNDED':b.status==='cancelled'?'CANCELLED':b.payment_status==='paid'?'PAID':'PENDING',bookingKey:`${b.venue_id}:${b.slot_id}`,cancellationDeadline:Number(b.cancellation_deadline)});}
 result.venueReviews=await ctx.db.all('SELECT id,booking_id,rating,comment,status,version FROM venue_reviews WHERE user_id=?',[userId]);
 result.supportRequests=await ctx.db.all('SELECT id,subject,message,category,status FROM support_requests WHERE user_id=? ORDER BY created_at DESC LIMIT 100',[userId]);
 const replies=await ctx.db.all("SELECT a.target_id,a.metadata,a.created_at FROM audit_log a JOIN support_requests s ON s.id=a.target_id WHERE s.user_id=? AND a.action='admin.support.resolve' ORDER BY a.created_at DESC,a.id DESC",[userId]);
 for(const request of result.supportRequests){const reply=replies.find(r=>r.target_id===request.id);request.response=reply?json(reply.metadata).response:null;request.respondedAt=reply?Number(reply.created_at):null;}
 result.reports=await ctx.db.all('SELECT id,target_id AS target,reason,details,status FROM reports WHERE user_id=? ORDER BY created_at DESC LIMIT 100',[userId]);
 result.registeredTournamentIds=(await ctx.db.all("SELECT DISTINCT tournament_id FROM tournament_members tm JOIN team_members m ON m.team_id=tm.team_id WHERE m.user_id=? AND m.status='joined' AND tm.status='joined'",[userId])).map(r=>r.tournament_id);result.invitedPlayerIds=(await ctx.db.all("SELECT i.user_id FROM team_invites i JOIN teams t ON t.id=i.team_id WHERE t.captain_id=? AND i.status='pending'",[userId])).map(r=>r.user_id);
 const cm=await ctx.db.all('SELECT * FROM community_members WHERE user_id=?',[userId]);result.joinedCommunityIds=cm.filter(m=>m.status==='joined').map(m=>m.community_id);result.requestedCommunityIds=cm.filter(m=>m.status==='pending').map(m=>m.community_id);
 const referencedPlayers=[...result.blockedUserIds,...result.removedEventMembers.map(m=>m.user_id),...result.removedTeamMembers.map(m=>m.user_id),...result.ownedCommunityMembers.map(m=>m.user_id),...result.communityRequests.map(m=>m.user_id),...result.joinRequests.map(m=>m.user_id),...result.teamJoinRequests.map(m=>m.user_id),...result.threads.flatMap(t=>t.memberIds)];
 for(const id of new Set(referencedPlayers))if(!players.some(p=>p.id===id))players.push(await publicPlayer(ctx,id));
 result.ratingHistory=await ctx.db.all('SELECT * FROM rating_history WHERE user_id=? ORDER BY created_at DESC LIMIT 100',[userId]);
 result.reliabilityHistory=(await ctx.db.all('SELECT h.*,e.payload AS event_payload FROM reliability_history h JOIN events e ON e.id=h.event_id WHERE h.user_id=? ORDER BY h.created_at DESC,h.id DESC LIMIT 100',[userId])).map(h=>({...h,date:dateLabel(h.created_at),detail:json(h.event_payload).title}));
 result.matchHistory=(await ctx.db.all("SELECT e.id,e.sport,e.payload AS event_payload,e.starts_at,m.side,r.payload AS score,h.delta FROM event_members m JOIN events e ON e.id=m.event_id JOIN results r ON r.event_id=e.id LEFT JOIN rating_history h ON h.event_id=e.id AND h.user_id=m.user_id WHERE m.user_id=? AND m.status='joined' AND m.checked_in_at IS NOT NULL AND r.status='verified' ORDER BY e.starts_at DESC,e.id DESC LIMIT 100",[userId])).map(m=>{const score=json(m.score),event=json(m.event_payload),difference=score.homeScore-score.awayScore;return{id:m.id,sport:m.sport,title:event.title,date:dateLabel(m.starts_at),score:`${score.homeScore} – ${score.awayScore}`,won:!['home','away'].includes(m.side)||difference===0?null:m.side==='home'?difference>0:difference<0,mmrDelta:Number(m.delta??0),mode:event.mode,verified:true};});
 const discovery=await ctx.db.get('SELECT payload FROM discovery_preferences WHERE user_id=?',[userId]);result.discovery=discovery?json(discovery.payload):null;
 result.progression=await progression(ctx,userId);if(result.profile)result.profile.streakWeeks=result.progression.streakWeeks;
 result.deletionRequest=await ctx.db.get("SELECT status,execute_after FROM deletion_requests WHERE user_id=? AND status='pending'",[userId]);
 result.subscription=await ctx.db.get('SELECT product_id,status,expires_at FROM subscriptions WHERE user_id=? AND status=? AND expires_at>?',[userId,'active',ctx.now]);return result;
}
export async function messagesPage(ctx,threadId,before,limit=50){await assertThread(ctx,threadId);const size=Math.min(100,Math.max(1,Number(limit)||50));let cursor=null;try{if(before)cursor=JSON.parse(Buffer.from(before,'base64url').toString());}catch{ensure(false,400,'INVALID_CURSOR','Sayfa bilgisi geçersiz.');}if(cursor)ensure(Array.isArray(cursor)&&[2,3].includes(cursor.length)&&(cursor.length===2||Number.isSafeInteger(cursor[2])&&cursor[2]>0)&&Number.isSafeInteger(cursor[0])&&typeof cursor[1]==='string',400,'INVALID_CURSOR','Sayfa bilgisi geçersiz.');const rows=await chatMessageRows(ctx,threadId,{before:cursor,limit:size+1});const items=rows.slice(0,size),last=items.at(-1);return{items:await Promise.all(items.reverse().map(m=>messageView(ctx,m))),nextCursor:rows.length>size?Buffer.from(JSON.stringify([Number(last.created_at),last.id,Number(last.sequence)])).toString('base64url'):null};}

export async function messageView(ctx,m){return{id:m.id,senderId:m.sender_id,senderName:(await publicPlayer(ctx,m.sender_id)).name,...await messageContent(ctx,m),time:timeLabel(m.created_at),day:new Intl.DateTimeFormat('tr-TR',{timeZone:'Europe/Istanbul',year:'numeric',day:'numeric',month:'short'}).format(Number(m.created_at)),createdAt:Number(m.created_at),isMine:m.sender_id===ctx.actor?.id};}
