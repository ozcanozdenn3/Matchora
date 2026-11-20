import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { runJobs } from '../src/jobs.mjs';
const fails=code=>e=>e.code===code;
const team=async(f,actor,name)=>(await f.cmd(actor,'team.create',{name,sport:'FOOTBALL',city:'Ankara',area:'Çankaya'})).result.id;
const cup=async(f,extra={})=>(await f.cmd(f.host,'tournament.create',{name:'Kadrolu kupa',sport:'FOOTBALL',capacity:2,formatCode:'KNOCKOUT',dates:'Kasım',rosterSize:1,...extra})).result.id;

test('organizer may register their own selected roster; reserve members do not enter fixtures and fixture targets have real metadata',async t=>{
 const f=await fixture(t);await f.grant(f.host);
 const home=await team(f,f.host,'Başkent takımı'),away=await team(f,f.guest,'Konuk takımı');
 await f.cmd(f.host,'team.invite',{teamId:home,userId:f.other.id});await f.cmd(f.other,'team.invite.respond',{teamId:home,accept:true});
 const id=await cup(f);
 await assert.rejects(f.cmd(f.host,'tournament.register',{tournamentId:id,teamId:home,roster:[f.host.id,f.other.id]}),fails('ROSTER_SIZE_REQUIRED'));
 await assert.rejects(f.cmd(f.host,'tournament.register',{tournamentId:id,teamId:home,roster:[f.guest.id]}),fails('INVALID_ROSTER'));
 await f.cmd(f.host,'tournament.register',{tournamentId:id,teamId:home,roster:[f.host.id]});
 await f.cmd(f.guest,'tournament.register',{tournamentId:id,teamId:away,roster:[f.guest.id]});
 const start=f.clock()+3600000;await f.cmd(f.host,'tournament.schedule',{tournamentId:id,startsAtEpochMillis:start,venue:'Başkent spor salonu'});
 const view=(await f.service.bootstrap(f.host)).tournaments.find(t=>t.id===id);
 assert.equal(view.registrations.length,2);assert.ok(view.registrations.every(t=>t.rosterIds.length===1));
 const matchFixture=view.fixtures[0];assert.equal(matchFixture.startsAtEpochMillis,start);assert.equal(matchFixture.venue,'Başkent spor salonu');assert.ok(matchFixture.homeName&&matchFixture.awayName);
 const event=await f.service.event(f.host,matchFixture.event_id);assert.equal(event.participantIds.length,2);assert.ok(!event.participantIds.includes(f.other.id));
 assert.ok((await f.service.bootstrap(f.host)).notifications.some(n=>n.destination==='TOURNAMENT'&&n.targetId===id));
});

test('organizer registration removal is authorized, notified and cannot be bypassed; restore requires fresh captain consent',async t=>{
 const f=await fixture(t);await f.grant(f.host);const home=await team(f,f.host,'Yerel takım'),away=await team(f,f.guest,'Misafir takım'),id=await cup(f);
 await f.cmd(f.guest,'tournament.register',{tournamentId:id,teamId:away,roster:[f.guest.id]});
 await assert.rejects(f.cmd(f.other,'tournament.registration.remove',{tournamentId:id,teamId:away,reason:'Yanlış kişi denemesi'}),fails('FORBIDDEN'));
 await f.cmd(f.host,'tournament.registration.remove',{tournamentId:id,teamId:away,reason:'Turnuva kayıt koşulu karşılanmadı.'});
 await assert.rejects(f.cmd(f.guest,'tournament.register',{tournamentId:id,teamId:away,roster:[f.guest.id]}),fails('REGISTRATION_REMOVED'));
 assert.equal((await f.service.bootstrap(f.guest)).tournaments.find(t=>t.id===id).registrations[0].status,'removed');
 assert.equal((await f.service.bootstrap()).tournaments.find(t=>t.id===id).registrations.length,0);
 await f.cmd(f.host,'tournament.registration.restore',{tournamentId:id,teamId:away});
 assert.ok(!(await f.service.bootstrap(f.guest)).registeredTournamentIds.includes(id));
 await f.cmd(f.guest,'tournament.register',{tournamentId:id,teamId:away,roster:[f.guest.id]});
 await f.cmd(f.host,'tournament.register',{tournamentId:id,teamId:home,roster:[f.host.id]});
 await f.cmd(f.host,'tournament.schedule',{tournamentId:id,startsAtEpochMillis:f.clock()+3600000,venue:'Spor merkezi'});
 await assert.rejects(f.cmd(f.host,'tournament.registration.remove',{tournamentId:id,teamId:away,reason:'Başlayan turnuvayı değiştirme'}),fails('TOURNAMENT_STARTED'));
});

test('knockout completion persists its champion and notifies the registered roster exactly once',async t=>{
 const f=await fixture(t);await f.grant(f.host);const home=await team(f,f.host,'Final ev sahibi'),away=await team(f,f.guest,'Final konuğu'),id=await cup(f);
 await f.cmd(f.host,'tournament.register',{tournamentId:id,teamId:home,roster:[f.host.id]});await f.cmd(f.guest,'tournament.register',{tournamentId:id,teamId:away,roster:[f.guest.id]});
 await f.cmd(f.host,'tournament.schedule',{tournamentId:id,startsAtEpochMillis:f.clock()+600000,venue:'Final sahası'});
 const matchFixture=(await f.store.get('SELECT * FROM tournament_fixtures WHERE tournament_id=?',[id]));
 const code=(await f.cmd(f.host,'event.checkin.code',{eventId:matchFixture.event_id})).result.code;
 for(const actor of [f.host,f.guest])await f.cmd(actor,'event.checkin',{eventId:matchFixture.event_id,code});
 await f.cmd(f.host,'event.transition',{eventId:matchFixture.event_id,status:'IN_PROGRESS',expectedVersion:(await f.service.event(f.host,matchFixture.event_id)).version});
 await f.cmd(f.host,'result.submit',{eventId:matchFixture.event_id,homeScore:2,awayScore:1});
 await f.cmd(f.guest,'result.confirm',{eventId:matchFixture.event_id});f.advance(25*3600000);await runJobs(f.service);
 assert.equal((await f.store.get('SELECT status FROM results WHERE event_id=?',[matchFixture.event_id])).status,'verified');
 const automatic = await f.service.bootstrap(f.host);
 assert.equal(automatic.tournaments.find(t=>t.id===id).championTeamId,matchFixture.home_team);
 assert.equal(automatic.tournaments.find(t=>t.id===id).status,'completed');
 const winner = matchFixture.home_team===home ? f.host : f.guest;
 assert.equal((await f.service.bootstrap(winner)).progression.badges.find(b=>b.id==='b5').earned,true);
 await runJobs(f.service);

 const result=await f.cmd(f.host,'tournament.schedule',{tournamentId:id,startsAtEpochMillis:f.clock()+7200000,venue:'Final sahası'},'finish-cup-replay');
 assert.equal(result.result.champion,matchFixture.home_team);assert.equal(result.snapshot.tournaments.find(t=>t.id===id).championTeamId,matchFixture.home_team);
 await f.cmd(f.host,'tournament.schedule',{tournamentId:id,startsAtEpochMillis:f.clock()+7200000,venue:'Final sahası'},'finish-cup-replay');
 assert.equal((await f.service.bootstrap(f.guest)).notifications.filter(n=>n.title==='Turnuva tamamlandı').length,1);
});

test('club creation, roster request, captain decision, removal and explicit reinvitation complete one real lifecycle',async t=>{
 const f=await fixture(t),id=await team(f,f.host,'Çankaya yeni kulüp');
 const created=(await f.service.bootstrap(f.host)).teams.find(t=>t.id===id);assert.equal(created.city,'Ankara');assert.equal(created.area,'Çankaya');
 const request=(await f.cmd(f.guest,'team.request',{teamId:id,position:'Kaleci',note:'Hafta içi uygunum.'})).result.id;
 assert.ok((await f.service.bootstrap(f.host)).teamJoinRequests.some(r=>r.id===request));
 await f.cmd(f.host,'team.request.review',{requestId:request,approve:true});
 assert.ok((await f.service.bootstrap(f.guest)).teams.find(t=>t.id===id).memberIds.includes(f.guest.id));
 await f.cmd(f.host,'team.member.remove',{teamId:id,userId:f.guest.id});
 await assert.rejects(f.cmd(f.guest,'team.request',{teamId:id}),fails('TEAM_REMOVED'));
 await f.cmd(f.host,'team.invite',{teamId:id,userId:f.guest.id});await f.cmd(f.guest,'team.invite.respond',{teamId:id,accept:true});
 assert.ok((await f.service.bootstrap(f.guest)).teams.find(t=>t.id===id).memberIds.includes(f.guest.id));
});

test('catalog notification targets resolve outside the first page and archived clubs cannot be revived',async t=>{
 const f=await fixture(t),id=await team(f,f.host,'Eski bildirim kulübü');
 const detail=await f.service.catalogItem(f.guest,'teams',id);assert.equal(detail.item.id,id);assert.equal(detail.players[0].id,f.host.id);
 await assert.rejects(f.service.catalogItem(f.host,'users',id),fails('INVALID_INPUT'));
 await f.cmd(f.host,'team.archive',{teamId:id});await assert.rejects(f.service.catalogItem(f.guest,'teams',id),fails('NOT_FOUND'));
});
