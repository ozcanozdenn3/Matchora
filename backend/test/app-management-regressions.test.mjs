import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { randomUUID } from 'node:crypto';
import { runJobs } from '../src/jobs.mjs';

const fails = code => error => error.code === code;
async function team(f, owner=f.host, name='Takım yönetimi') {
  return (await f.cmd(owner,'team.create',{name,sport:'FOOTBALL',area:'Kadıköy'})).result.id;
}
async function add(f, teamId, user=f.guest) {
  await f.cmd(f.host,'team.invite',{teamId,userId:user.id});
  await f.cmd(user,'team.invite.respond',{teamId,accept:true});
}
async function community(f, extra={}) {
  return (await f.cmd(f.host,'community.create',{name:'Kampüs spor ağı',type:'CAMPUS',access:'OPEN',area:'Kadıköy',sport:'FOOTBALL',...extra})).result.id;
}
async function cup(f, home, away) {
  await f.grant(f.host);
  const id=(await f.cmd(f.host,'tournament.create',{name:'Yönetim kupası',sport:'FOOTBALL',capacity:2,dates:'Kasım'})).result.id;
  await f.cmd(f.host,'tournament.register',{tournamentId:id,teamId:home});
  if(away)await f.cmd(f.other,'tournament.register',{tournamentId:id,teamId:away});
  return id;
}

test('captain removes an athlete, closes chat access and explicitly invites them back', async t=>{
  const f=await fixture(t),id=await team(f);await add(f,id);
  const thread=(await f.store.get('SELECT id FROM threads WHERE team_id=?',[id])).id;
  await assert.rejects(f.cmd(f.other,'team.member.remove',{teamId:id,userId:f.guest.id}),fails('CAPTAIN_REQUIRED'));
  await assert.rejects(f.cmd(f.host,'team.member.remove',{teamId:id,userId:f.host.id}),fails('CAPTAIN_CANNOT_REMOVE'));
  await f.cmd(f.host,'team.member.remove',{teamId:id,userId:f.guest.id});
  await assert.rejects(f.service.messages(f.guest,thread),e=>e.status===403);
  await assert.rejects(f.cmd(f.guest,'team.request',{teamId:id}),fails('TEAM_REMOVED'));
  assert.equal((await f.service.bootstrap(f.host)).removedTeamMembers[0].user_id,f.guest.id);
  await add(f,id);
  assert.equal((await f.service.bootstrap(f.guest)).teams.find(v=>v.id===id).isMine,true);
  assert.ok(Array.isArray((await f.service.messages(f.guest,thread)).items));
});

test('captain transfer requires another active member and voluntary leave requires membership',async t=>{
  const f=await fixture(t),id=await team(f);await add(f,id);
  await assert.rejects(f.cmd(f.other,'team.leave',{teamId:id}),fails('MEMBERSHIP_REQUIRED'));
  await assert.rejects(f.cmd(f.host,'team.transfer',{teamId:id,userId:f.host.id}),fails('MEMBERSHIP_REQUIRED'));
  await f.cmd(f.admin,'admin.user.status',{userId:f.guest.id,status:'suspended',reason:'Test için hesap durumu değiştiriliyor.'});
  await assert.rejects(f.cmd(f.host,'team.transfer',{teamId:id,userId:f.guest.id}),fails('MEMBERSHIP_REQUIRED'));
  await f.cmd(f.admin,'admin.user.status',{userId:f.guest.id,status:'active',reason:'Test için hesap tekrar açılıyor.'});
  await f.cmd(f.host,'team.transfer',{teamId:id,userId:f.guest.id});
  await f.cmd(f.host,'team.leave',{teamId:id});
  assert.equal((await f.store.get('SELECT captain_id FROM teams WHERE id=?',[id])).captain_id,f.guest.id);
});

test('closing a team cancels pending offers and refunds their original owner',async t=>{
  const f=await fixture(t),home=await team(f),away=await team(f,f.other,'Rakip takım');
  await f.cmd(f.host,'challenge.create',{fromTeamId:home,toTeamId:away,proposedTime:'Yarın',venue:'Final sahası',stakeTokens:5,startsAtEpochMillis:f.clock()+7200000});
  await f.cmd(f.host,'team.archive',{teamId:home});
  assert.deepEqual((await f.service.bootstrap(f.host)).wallet,{balance:20,locked:0});
  assert.ok(!(await f.service.bootstrap(f.host)).teams.some(v=>v.id===home));
  await assert.rejects(f.cmd(f.guest,'team.request',{teamId:home}),fails('TEAM_ARCHIVED'));
  await assert.rejects(f.cmd(f.host,'team.invite',{teamId:home,userId:f.guest.id}),fails('TEAM_ARCHIVED'));
  await assert.rejects(f.cmd(f.host,'team.update',{teamId:home,recruitingOpen:true}),fails('TEAM_ARCHIVED'));
  await assert.rejects(f.cmd(f.other,'challenge.create',{fromTeamId:away,toTeamId:home,proposedTime:'Yarın',venue:'Saha',startsAtEpochMillis:f.clock()+7200000}),fails('TEAM_ARCHIVED'));
});

test('registered tournament rosters prevent removal or leave until their exact registration is withdrawn',async t=>{
  const f=await fixture(t),home=await team(f);await add(f,home);const otherTeam=await team(f),id=await cup(f,home);
  await assert.rejects(f.cmd(f.guest,'team.leave',{teamId:home}),fails('ROSTER_COMMITTED'));
  await assert.rejects(f.cmd(f.host,'team.member.remove',{teamId:home,userId:f.guest.id}),fails('ROSTER_COMMITTED'));
  await assert.rejects(f.cmd(f.host,'team.archive',{teamId:home}),fails('ROSTER_COMMITTED'));
  await assert.rejects(f.cmd(f.host,'tournament.withdraw',{tournamentId:id,teamId:otherTeam}),fails('NOT_REGISTERED'));
  await f.cmd(f.host,'tournament.withdraw',{tournamentId:id,teamId:home});
  await f.cmd(f.guest,'team.leave',{teamId:home});
});

test('future scheduled fixtures can be cancelled together but begun tournaments cannot be silently erased',async t=>{
  const f=await fixture(t),home=await team(f),away=await team(f,f.other,'Turnuva rakibi'),id=await cup(f,home,away);
  await f.cmd(f.host,'tournament.schedule',{tournamentId:id,startsAtEpochMillis:f.clock()+7200000,venue:'Final sahası'});
  await f.cmd(f.host,'tournament.cancel',{tournamentId:id});
  assert.equal((await f.store.get('SELECT status FROM tournaments WHERE id=?',[id])).status,'cancelled');
  assert.ok((await f.store.all('SELECT e.status FROM tournament_fixtures f JOIN events e ON e.id=f.event_id WHERE f.tournament_id=?',[id])).every(v=>v.status==='CANCELLED'));
  const another=await cup(f,home,away);
  await f.cmd(f.host,'tournament.schedule',{tournamentId:another,startsAtEpochMillis:f.clock()+600000,venue:'Final sahası'});
  f.advance(11*60000);
  await assert.rejects(f.cmd(f.host,'tournament.cancel',{tournamentId:another}),fails('TOURNAMENT_STARTED'));
});

test('knockout draws cannot consume the result record and block tournament progress',async t=>{
  const f=await fixture(t),home=await team(f),away=await team(f,f.other,'Eleme rakibi'),id=await cup(f,home,away);
  await f.cmd(f.host,'tournament.schedule',{tournamentId:id,startsAtEpochMillis:f.clock()+600000,venue:'Final sahası'});
  const event=(await f.store.get('SELECT e.* FROM tournament_fixtures f JOIN events e ON e.id=f.event_id WHERE f.tournament_id=?',[id]));
  const code=(await f.cmd(f.host,'event.checkin.code',{eventId:event.id})).result.code;
  await f.cmd(f.host,'event.checkin',{eventId:event.id,code});
  await f.cmd(f.host,'event.transition',{eventId:event.id,status:'IN_PROGRESS',expectedVersion:event.version});
  await assert.rejects(f.cmd(f.host,'result.submit',{eventId:event.id,homeScore:1,awayScore:1}),fails('TIE_BREAK_REQUIRED'));
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM results WHERE event_id=?',[event.id])).n,0);
  await f.cmd(f.host,'result.submit',{eventId:event.id,homeScore:2,awayScore:1});
});

test('leaving a private community releases participation and leaving cannot undo moderator removal',async t=>{
  const f=await fixture(t),id=await community(f);await f.cmd(f.guest,'community.join',{communityId:id});
  const event=(await f.cmd(f.host,'event.create',f.event({networkScope:'CAMPUS',communityId:id}))).result.id;
  await f.cmd(f.guest,'event.join',{eventId:event});
  await f.cmd(f.guest,'community.leave',{communityId:id});
  assert.deepEqual((await f.service.bootstrap(f.guest)).wallet,{balance:20,locked:0});
  assert.equal((await f.store.get('SELECT status FROM event_members WHERE event_id=? AND user_id=?',[event,f.guest.id])).status,'left');
  await f.cmd(f.guest,'community.join',{communityId:id});
  await f.cmd(f.host,'community.member.remove',{communityId:id,userId:f.guest.id});
  await assert.rejects(f.cmd(f.guest,'community.leave',{communityId:id}),fails('MEMBERSHIP_REQUIRED'));
  await assert.rejects(f.cmd(f.guest,'community.join',{communityId:id}),fails('COMMUNITY_REMOVED'));
  await f.cmd(f.host,'community.invite',{communityId:id,userId:f.guest.id});
  await f.cmd(f.guest,'community.join',{communityId:id});
});

test('community invitations explicitly approve membership and closing refunds future private events',async t=>{
  const f=await fixture(t),id=await community(f,{access:'APPROVAL'});
  await f.cmd(f.host,'community.invite',{communityId:id,userId:f.guest.id});
  assert.equal((await f.cmd(f.guest,'community.join',{communityId:id})).result.status,'joined');
  const event=(await f.cmd(f.host,'event.create',f.event({networkScope:'CAMPUS',communityId:id}))).result.id;
  await f.cmd(f.guest,'event.join',{eventId:event});
  await f.cmd(f.host,'community.archive',{communityId:id});
  assert.equal((await f.store.get('SELECT status FROM events WHERE id=?',[event])).status,'CANCELLED');
  assert.deepEqual((await f.service.bootstrap(f.guest)).wallet,{balance:20,locked:0});
  await assert.rejects(f.cmd(f.guest,'community.join',{communityId:id}),fails('COMMUNITY_ARCHIVED'));
  await assert.rejects(f.cmd(f.host,'community.invite',{communityId:id,userId:f.guest.id}),fails('COMMUNITY_ARCHIVED'));
});

test('older owned teams, invitations, communities and tournaments remain manageable beyond catalog limits',async t=>{
  const f=await fixture(t),home=await team(f),invited=await team(f,f.other,'Eski davet takımı'),network=await community(f),tournament=await cup(f,home);
  await f.cmd(f.other,'team.invite',{teamId:invited,userId:f.guest.id});
  for(let i=0;i<101;i++){
    const stamp=f.clock()+i+1;
    await f.store.run('INSERT INTO teams(id,captain_id,sport,payload,created_at) VALUES(?,?,?,?,?)',[randomUUID(),f.admin.id,'FOOTBALL','{"name":"Yeni takım"}',stamp]);
    await f.store.run('INSERT INTO communities(id,owner_id,access,payload,created_at) VALUES(?,?,?,?,?)',[randomUUID(),f.admin.id,'OPEN','{"name":"Yeni topluluk"}',stamp]);
    await f.store.run('INSERT INTO tournaments(id,creator_id,sport,status,capacity,payload,created_at) VALUES(?,?,?,?,?,?,?)',[randomUUID(),f.admin.id,'FOOTBALL','open',2,'{"name":"Yeni turnuva"}',stamp]);
  }
  const own=await f.service.bootstrap(f.host);
  assert.ok(own.teams.some(t=>t.id===home));assert.ok(own.communities.some(c=>c.id===network));assert.ok(own.tournaments.some(t=>t.id===tournament));
  assert.ok((await f.service.bootstrap(f.guest)).teams.some(t=>t.id===invited));
});

test('cross-city pending applications stay in activity even outside discovery pages',async t=>{
  const f=await fixture(t),event=(await f.cmd(f.other,'event.create',f.event({city:'Ankara',area:'Çankaya'}))).result.id;
  await f.cmd(f.guest,'event.request',{eventId:event});
  const own=await f.service.bootstrap(f.guest,{city:'İstanbul'});
  assert.ok(own.events.some(e=>e.id===event));
  assert.ok(!(await f.service.bootstrap(null,{city:'İstanbul'})).events.some(e=>e.id===event));
});

test('departed team members receive no new chat notices and queued private pushes are revoked',async t=>{
  const f=await fixture(t),id=await team(f);await add(f,id);
  const thread=(await f.store.get('SELECT id FROM threads WHERE team_id=?',[id])).id;
  await f.cmd(f.host,'chat.send',{threadId:thread,text:'Çıkarılmadan önceki mesaj'});
  const before=(await f.store.get("SELECT COUNT(*) AS n FROM notifications WHERE user_id=? AND category='social'",[f.guest.id])).n;
  await f.cmd(f.host,'team.member.remove',{teamId:id,userId:f.guest.id});
  await f.cmd(f.host,'chat.send',{threadId:thread,text:'Artık yalnız mevcut kadroya'});
  assert.equal((await f.store.get("SELECT COUNT(*) AS n FROM notifications WHERE user_id=? AND category='social'",[f.guest.id])).n,before);
  const job=await f.store.get("SELECT o.id FROM outbox o JOIN notifications n ON n.id=json_extract(o.payload,'$.notificationId') WHERE o.user_id=? AND n.category='social'",[f.guest.id]);
  const old=globalThis.fetch;t.after(()=>{globalThis.fetch=old;});
  globalThis.fetch=async()=>({ok:true});f.config.pushUrl='https://push.example.invalid';
  await runJobs(f.service);
  assert.equal((await f.store.get('SELECT status FROM outbox WHERE id=?',[job.id])).status,'skipped');
});
