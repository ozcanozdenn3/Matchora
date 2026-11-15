import { tournamentDetails } from './tournament-details.mjs';
import { json } from './errors.mjs';
import { reward } from './model.mjs';
import { canonicalTurkish } from './turkish-locale.mjs';
export const weekNumber=stamp=>Math.floor((Number(stamp)+3*3600000+3*86400000)/(7*86400000));
export async function progression(ctx,userId,targetWeek=weekNumber(ctx.now)){
 const records=await ctx.db.all("SELECT e.id,e.host_id,e.city,e.ends_at,e.payload,m.side,m.checked_in_at FROM event_members m JOIN events e ON e.id=m.event_id JOIN results r ON r.event_id=e.id WHERE m.user_id=? AND m.status='joined' AND r.status='verified' ORDER BY e.ends_at DESC,e.id",[userId]);
 const matches=records.filter(m=>m.checked_in_at);
 let attendance=0;for(const match of records){if(!match.checked_in_at)break;attendance++;}
 const fair=await ctx.db.get("SELECT AVG(f.fair_play) AS average,COUNT(DISTINCT f.event_id) AS events FROM feedback f JOIN results r ON r.event_id=f.event_id WHERE f.to_id=? AND r.status='verified'",[userId]);
 const weeks=new Set(matches.filter(m=>m.checked_in_at).map(m=>weekNumber(m.ends_at))),week=targetWeek;
 let cursor=weeks.has(week)?week:week-1,streak=0;while(weeks.has(cursor--))streak++;
 const cities=new Set(matches.filter(m=>m.checked_in_at).map(m=>canonicalTurkish(m.city)).filter(Boolean)),weekly=matches.filter(m=>weekNumber(m.ends_at)===week),opponents=new Set();let repeats=0;
 for(const match of weekly){if(!['home','away'].includes(match.side))continue;for(const peer of await ctx.db.all("SELECT user_id FROM event_members WHERE event_id=? AND status='joined' AND checked_in_at IS NOT NULL AND side=? AND user_id<>?",[match.id,match.side==='home'?'away':'home',userId])){
  opponents.add(peer.user_id);
  if(await ctx.db.get("SELECT a.event_id FROM event_members a JOIN event_members b ON b.event_id=a.event_id JOIN events e ON e.id=a.event_id JOIN results r ON r.event_id=e.id WHERE a.user_id=? AND b.user_id=? AND a.status='joined' AND b.status='joined' AND a.checked_in_at IS NOT NULL AND b.checked_in_at IS NOT NULL AND a.side IN ('home','away') AND b.side IN ('home','away') AND a.side<>b.side AND r.status='verified' AND e.ends_at<? LIMIT 1",[userId,peer.user_id,match.ends_at]))repeats++;
 }}
 const badge=(id,title,emoji,description,value,goal)=>({id,title,emoji,description,earned:value>=goal,progress:Math.min(1,value/goal)});
 const newcomers=new Set();
 for(const first of await ctx.db.all("SELECT e.id,e.ends_at,m.user_id FROM events e JOIN results r ON r.event_id=e.id JOIN event_members m ON m.event_id=e.id JOIN event_members host ON host.event_id=e.id AND host.user_id=e.host_id WHERE e.host_id=? AND r.status='verified' AND m.status='joined' AND m.checked_in_at IS NOT NULL AND m.user_id<>? AND host.checked_in_at IS NOT NULL",[userId,userId])){
  if(!await ctx.db.get("SELECT e.id FROM event_members m JOIN events e ON e.id=m.event_id JOIN results r ON r.event_id=e.id WHERE m.user_id=? AND m.status='joined' AND m.checked_in_at IS NOT NULL AND r.status='verified' AND (e.ends_at<? OR (e.ends_at=? AND e.id<?)) LIMIT 1",[first.user_id,first.ends_at,first.ends_at,first.id]))newcomers.add(first.user_id);
 }
 let titles=0;
 for(const cup of await ctx.db.all("SELECT DISTINCT t.* FROM tournaments t JOIN tournament_fixtures f ON f.tournament_id=t.id JOIN event_members m ON m.event_id=f.event_id WHERE t.status='completed' AND m.user_id=? AND m.checked_in_at IS NOT NULL",[userId])){
  const details=await tournamentDetails(ctx,cup),finalRound=Math.max(0,...details.fixtures.map(f=>Number(f.round))),finals=details.fixtures.filter(f=>Number(f.round)===finalRound&&f.result_status==='verified');
  const champion=json(cup.payload).isLeague?details.standings[0]?.teamId:finals.length===1?(finals[0].homeScore>finals[0].awayScore?finals[0].home_team:finals[0].homeScore<finals[0].awayScore?finals[0].away_team:null):null;
  if(champion&&await ctx.db.get("SELECT m.user_id FROM tournament_fixtures f JOIN event_members m ON m.event_id=f.event_id JOIN results r ON r.event_id=f.event_id WHERE f.tournament_id=? AND m.user_id=? AND m.status='joined' AND m.checked_in_at IS NOT NULL AND r.status='verified' AND ((f.home_team=? AND m.side='home') OR (f.away_team=? AND m.side='away')) LIMIT 1",[cup.id,userId,champion,champion]))titles++;
 }
 const urgent=matches.filter(m=>json(m.payload).emergency===true&&m.host_id!==userId).length;
 const badges=[badge('b1','Perfect Attendance','🎯','Art arda 10 doğrulanmış maçta check-in',attendance,10),badge('b2','Fair Play','🤝','20 maçta 4.5+ fair play',Number(fair.average)>=4.5?Number(fair.events):0,20),badge('b3','Community Builder','🌱','Düzenlediğin maçlarda 5 yeni sporcunun ilk doğrulanmış katılımını sağla',newcomers.size,5),badge('b5','Tournament Winner','🏆','Şampiyon takımın doğrulanmış maç kadrosunda yer al',titles,1),badge('b8','Clutch Filler','🦸','3 acil oyuncu ilanında doğrulanmış katılım',urgent,3),badge('b4','100 Matches','💯','100 doğrulanmış maç',matches.length,100),badge('b6','Traveler','✈️','3 farklı şehirde doğrulanmış katılım',cities.size,3),badge('b7','Iron Streak','🔥','12 hafta kesintisiz doğrulanmış katılım',streak,12)];
 const missions=[{id:'opponents',title:'3 farklı rakiple oyna',description:'Bu hafta doğrulanmış maçlarda yeni sporcularla tanış',progress:Math.min(3,opponents.size),goal:3,rewardTokens:15,emoji:'🧩'},{id:'rematch',title:'Bir rakiple tekrar oyna',description:'Daha önce karşılaştığın biriyle yeniden sahaya çık',progress:Math.min(1,repeats),goal:1,rewardTokens:10,emoji:'🔁'},{id:'checkin',title:'Zamanında check-in',description:'Bu hafta 2 doğrulanmış maçta check-in yap',progress:Math.min(2,weekly.filter(m=>m.checked_in_at).length),goal:2,rewardTokens:10,emoji:'📍'}];
 const claimedKeys=new Set((await ctx.db.all('SELECT reward_key FROM rewards WHERE user_id=?',[userId])).map(r=>r.reward_key));
 for(const mission of missions)mission.claimed=claimedKeys.has(`weekly:${week}:${mission.id}`);
 return{week,streakWeeks:streak,badges,missions};
}
export async function grantProgressRewards(ctx,userId,targetWeek=weekNumber(ctx.now)){
 const progress=await progression(ctx,userId,targetWeek);
 for(const mission of progress.missions)if(mission.progress>=mission.goal)await reward(ctx,userId,`weekly:${progress.week}:${mission.id}`,mission.rewardTokens);
}
