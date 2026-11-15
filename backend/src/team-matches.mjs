import { id } from './auth.mjs';
import { ensure,json } from './errors.mjs';
import { row,encode,notify } from './model.mjs';
import { coarseMemoryLocation } from './memory-feed.mjs';
import { canonicalTurkishCity, sameTurkishPlace } from './turkish-locale.mjs';

export function assertTeamOpen(team){ensure(!json(team.payload).archived,409,'TEAM_ARCHIVED','Bu takım kapatıldı; yeni işlem yapılamaz.');}

export async function scheduleChallenge(ctx,challenge,data){
 const home=await row(ctx,'teams','id',challenge.from_team),away=await row(ctx,'teams','id',challenge.to_team);
 assertTeamOpen(home);assertTeamOpen(away);
 const roster=async team=>(await ctx.db.all("SELECT m.user_id FROM team_members m JOIN users u ON u.id=m.user_id WHERE m.team_id=? AND m.status='joined' AND u.status='active' ORDER BY m.user_id",[team.id])).map(m=>m.user_id);
 const homeIds=await roster(home),awayIds=await roster(away),all=[...homeIds,...awayIds];
 ensure(homeIds.length&&awayIds.length&&all.length<=100,409,'INVALID_ROSTER','İki takımın da aktif kadrosu bulunmalı.');
 ensure(new Set(all).size===all.length,409,'DUPLICATE_PLAYER','Aynı sporcu iki takımın kadrosunda olamaz.');
 for(const userId of all){const saved=await ctx.db.get('SELECT payload FROM profiles WHERE user_id=?',[userId]);ensure(saved&&json(saved.payload).skills.some(s=>s.sport===home.sport),409,'PROFILE_SPORT_REQUIRED','Resmi kadrodaki her sporcu maç branşını pasaportuna eklemeli.');}
 const hp=await ctx.db.get('SELECT payload FROM profiles WHERE user_id=?',[home.captain_id]),place=hp?json(hp.payload):{},homeData=json(home.payload);
 const fallbackCity=canonicalTurkishCity(homeData.city??place.city??'İstanbul');
 const venueRows=await ctx.db.all('SELECT id,payload FROM venues');
 const matchingVenues=venueRows.map(v=>({id:v.id,data:json(v.payload)})).filter(v=>!v.data.archived&&sameTurkishPlace(v.data.name,data.venue));
 const matchedVenue=matchingVenues.find(v=>sameTurkishPlace(v.data.city,fallbackCity))??matchingVenues[0]??null;
 const venuePoint=matchedVenue?coarseMemoryLocation(matchedVenue.data.approximateLocation,false):null;
 const city=canonicalTurkishCity(matchedVenue?.data.city??fallbackCity);
 const area=matchedVenue?.data.area??homeData.area??place.area??city;
 const eventId=id(),threadId=id(),title=`${homeData.name} – ${json(away.payload).name}`;
 const payload={title,sport:home.sport,venue:matchedVenue?.data.name??data.venue,city,area,mode:'COMPETITIVE',levelRange:'Takım maçı',minReliability:0,verifiedOnly:false,description:data.note,isTeamEvent:true,challengeId:challenge.id,homeTeamId:home.id,awayTeamId:away.id,networkScope:'CITY',...(matchedVenue?{venueId:matchedVenue.id}:{}),...(venuePoint?{approximateLocation:venuePoint,saveMeetingLocation:true}:{})};
 await ctx.db.run('INSERT INTO events(id,host_id,sport,status,capacity,starts_at,ends_at,commitment,city,district,latitude,longitude,payload,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)',[eventId,home.captain_id,home.sport,'FULL',all.length,data.startsAtEpochMillis,data.startsAtEpochMillis+3600000,0,payload.city,payload.area,venuePoint?.latitude??null,venuePoint?.longitude??null,encode(payload),ctx.now]);
 await ctx.db.run('INSERT INTO threads(id,kind,event_id,title,created_at) VALUES(?,?,?,?,?)',[threadId,'EVENT',eventId,title,ctx.now]);
 for(const userId of all){
  await ctx.db.run('INSERT INTO event_members(event_id,user_id,status,side,joined_at) VALUES(?,?,?,?,?)',[eventId,userId,'joined',homeIds.includes(userId)?'home':'away',ctx.now]);
  await ctx.db.run('INSERT INTO thread_members(thread_id,user_id) VALUES(?,?)',[threadId,userId]);
  await notify(ctx,userId,'matches','Takım maçın planlandı',title,'EVENT',eventId,`challenge-match:${eventId}:${userId}`);
 }
 return eventId;
}

export async function teamStatistics(ctx,team,members){
 let rating=0,reliability=0,count=0;
 ctx.teamProfileCache??=new Map();
 const missing=members.map(m=>m.user_id).filter(id=>!ctx.teamProfileCache.has(id));
 if(missing.length){
  const q=missing.map(()=>'?').join(',');
  for(const p of await ctx.db.all(`SELECT user_id,payload FROM profiles WHERE user_id IN (${q})`,missing))ctx.teamProfileCache.set(p.user_id,json(p.payload));
  for(const id of missing)if(!ctx.teamProfileCache.has(id))ctx.teamProfileCache.set(id,null);
 }
 for(const member of members){const profile=ctx.teamProfileCache.get(member.user_id);if(!profile)continue;const skill=profile.skills.find(s=>s.sport===team.sport);if(skill){rating+=skill.rating;reliability+=profile.reliability;count++;}}
 ctx.verifiedResultsBySport??=new Map();
 if(!ctx.verifiedResultsBySport.has(team.sport)){
  ctx.verifiedResultsBySport.set(team.sport,await ctx.db.all("SELECT e.payload AS event_payload,r.payload AS result_payload FROM results r JOIN events e ON e.id=r.event_id WHERE r.status='verified' AND e.sport=?",[team.sport]));
 }
 let won=0,lost=0,drawn=0;
 for(const row of ctx.verifiedResultsBySport.get(team.sport)){
  const event=json(row.event_payload);if(event.homeTeamId!==team.id&&event.awayTeamId!==team.id)continue;
  const result=json(row.result_payload),ours=event.homeTeamId===team.id?result.homeScore:result.awayScore,theirs=event.homeTeamId===team.id?result.awayScore:result.homeScore;
  if(ours>theirs)won++;else if(ours<theirs)lost++;else drawn++;
 }
 return{rating:count?Math.round(rating/count):1000,reliability:count?Math.round(reliability/count):0,won,lost,drawn,played:won+lost+drawn,winRate:won+lost+drawn?Math.round(100*won/(won+lost+drawn)):0};
}
