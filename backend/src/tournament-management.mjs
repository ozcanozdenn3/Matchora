import { ensure, keys, text, json } from './errors.mjs';
import { row, notify, audit } from './model.mjs';

export const tournamentManagementHandlers = {
 'tournament.registration.remove': async(ctx,p)=>{
  keys(p,['tournamentId','teamId','reason']);
  const tournament=await row(ctx,'tournaments','id',p.tournamentId);
  ensure(tournament.creator_id===ctx.actor.id,403,'FORBIDDEN','Organizatör yetkisi gerekiyor.');
  ensure(tournament.status==='open',409,'TOURNAMENT_STARTED','Fikstür başladıktan sonra kadro kaydı bu işlemle kaldırılamaz.');
  const registration=await ctx.db.get("SELECT * FROM tournament_members WHERE tournament_id=? AND team_id=? AND status='joined'",[tournament.id,p.teamId]);
  ensure(registration,409,'NOT_REGISTERED','Aktif takım kaydı bulunamadı.');const reason=text(p.reason,'Gerekçe',10,500),team=await row(ctx,'teams','id',p.teamId);
  await ctx.db.run("UPDATE tournament_members SET status='removed' WHERE tournament_id=? AND team_id=?",[tournament.id,team.id]);
  for(const userId of new Set([team.captain_id,...json(registration.roster)]))await notify(ctx,userId,'challenges','Turnuva kaydı kaldırıldı',`${json(tournament.payload).name} · ${reason}`,'TOURNAMENT',tournament.id,`tournament-remove:${tournament.id}:${team.id}:${userId}:${ctx.requestKey}`);
  await audit(ctx,'tournament.registration.remove',tournament.id,{teamId:team.id,reason});return{removed:true};
 },
 'tournament.registration.restore': async(ctx,p)=>{
  keys(p,['tournamentId','teamId']);const tournament=await row(ctx,'tournaments','id',p.tournamentId);
  ensure(tournament.creator_id===ctx.actor.id,403,'FORBIDDEN','Organizatör yetkisi gerekiyor.');ensure(tournament.status==='open',409,'TOURNAMENT_STARTED','Turnuva kayıt dönemi kapalı.');
  const registration=await ctx.db.get("SELECT * FROM tournament_members WHERE tournament_id=? AND team_id=? AND status='removed'",[tournament.id,p.teamId]);
  ensure(registration,409,'INVALID_STATE','Bu takım çıkarılmış kayıtlar arasında değil.');
  await ctx.db.run("UPDATE tournament_members SET status='left' WHERE tournament_id=? AND team_id=?",[tournament.id,p.teamId]);const team=await row(ctx,'teams','id',p.teamId);
  await notify(ctx,team.captain_id,'challenges','Turnuvaya yeniden başvurabilirsin',json(tournament.payload).name,'TOURNAMENT',tournament.id,`tournament-restore:${tournament.id}:${team.id}:${ctx.requestKey}`);
  await audit(ctx,'tournament.registration.restore',tournament.id,{teamId:team.id});return{restored:true};
 }
};
