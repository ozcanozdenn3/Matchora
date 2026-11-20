import { json } from './errors.mjs';
export async function tournamentDetails(ctx,tournament){
 const rawFixtures=ctx.tournamentFixturesById?.has(tournament.id)
  ? ctx.tournamentFixturesById.get(tournament.id)
  : await ctx.db.all("SELECT f.*,r.status AS result_status,r.payload AS result_payload,e.starts_at,e.status AS event_status,e.payload AS event_payload,h.payload AS home_payload,a.payload AS away_payload FROM tournament_fixtures f JOIN events e ON e.id=f.event_id JOIN teams h ON h.id=f.home_team JOIN teams a ON a.id=f.away_team LEFT JOIN results r ON r.event_id=f.event_id WHERE f.tournament_id=? ORDER BY f.round,e.starts_at,f.id",[tournament.id]);
 const fixtures=rawFixtures.map(row=>({...row}));
 const table=new Map();
 const actorId=ctx.actor?.id??'';
 const registrations=ctx.tournamentRegsById?.has(tournament.id)
  ? ctx.tournamentRegsById.get(tournament.id).filter(r=>r.status==='joined'||(ctx.actor&&(r.captain_id===actorId||actorId===tournament.creator_id)))
  : await ctx.db.all("SELECT tm.team_id,tm.roster,tm.status,t.captain_id,t.payload FROM tournament_members tm JOIN teams t ON t.id=tm.team_id WHERE tm.tournament_id=? AND (tm.status='joined' OR t.captain_id=? OR ?=?) ORDER BY tm.created_at,tm.team_id",[tournament.id,actorId,actorId,tournament.creator_id]);
 for(const item of registrations)
  if(item.status==='joined')table.set(item.team_id,{teamId:item.team_id,team:json(item.payload).name,played:0,won:0,drawn:0,lost:0,goalDiff:0,points:0});
 for(const f of fixtures){
  if(f.result_status!=='verified')continue;
  const result=json(f.result_payload),home=table.get(f.home_team),away=table.get(f.away_team);
  f.homeScore=result.homeScore;f.awayScore=result.awayScore;
  if(!home||!away)continue;home.played++;away.played++;home.goalDiff+=result.homeScore-result.awayScore;away.goalDiff+=result.awayScore-result.homeScore;
  if(result.homeScore===result.awayScore){home.drawn++;away.drawn++;home.points++;away.points++;}
  else {const winner=result.homeScore>result.awayScore?home:away,loser=winner===home?away:home;winner.won++;winner.points+=3;loser.lost++;}
 }
 return{registrations:registrations.map(r=>({teamId:r.team_id,name:json(r.payload).name,captainId:r.captain_id,status:r.status,rosterIds:json(r.roster)})),fixtures:fixtures.map(({result_payload,event_payload,home_payload,away_payload,...rest})=>({...rest,startsAtEpochMillis:Number(rest.starts_at),venue:json(event_payload).venue,homeName:json(home_payload).name,awayName:json(away_payload).name})),standings:[...table.values()].sort((a,b)=>b.points-a.points||b.goalDiff-a.goalDiff||a.teamId.localeCompare(b.teamId))};
}
