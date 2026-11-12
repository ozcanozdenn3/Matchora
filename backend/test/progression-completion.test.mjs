import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { progression } from '../src/progression.mjs';

async function verifiedRecord(f,actors,{emergency=false,noShow=[]}={}){
 const id=(await f.cmd(f.host,'event.create',f.event({capacity:Math.max(2,actors.length),emergency}))).result.id;
 for(const actor of actors.filter(a=>a.id!==f.host.id))await f.cmd(actor,'event.join',{eventId:id});
 f.advance(3*3600000);
 for(const actor of actors)await f.store.run('UPDATE event_members SET side=?,checked_in_at=? WHERE event_id=? AND user_id=?',[actor.id===f.host.id?'home':'away',noShow.includes(actor.id)?null:f.clock()-2*3600000,id,actor.id]);
 await f.store.run("UPDATE events SET status='VERIFIED' WHERE id=?",[id]);
 await f.store.run("INSERT INTO results(event_id,submitted_by,payload,status,dispute_until,created_at) VALUES(?,?,?,'verified',?,?)",[id,f.host.id,JSON.stringify({homeScore:2,awayScore:1}),f.clock(),f.clock()]);
 return id;
}

test('all eight passport badges have real rules; first-time community participation is unique and emergency fillers must attend',async t=>{
 const f=await fixture(t);await f.grant(f.host);await f.grant(f.guest);
 const newcomers=[];for(let n=0;n<5;n++){const user=await f.user(`newcomer-${n}`);await f.cmd(user,'profile.save',f.passport(`Yeni sporcu ${n}`));newcomers.push(user);}
 await verifiedRecord(f,[f.host,...newcomers]);
 let host=await progression(f.service.ctx(f.store),f.host.id);assert.deepEqual(new Set(host.badges.map(b=>b.id)),new Set(['b1','b2','b3','b4','b5','b6','b7','b8']));assert.ok(host.badges.find(b=>b.id==='b3').earned);
 for(let n=0;n<2;n++)await verifiedRecord(f,[f.host,f.guest],{emergency:true});
 await verifiedRecord(f,[f.host,f.guest],{emergency:true,noShow:[f.guest.id]});
 let guest=await progression(f.service.ctx(f.store),f.guest.id);assert.equal(guest.badges.find(b=>b.id==='b8').earned,false);
 await verifiedRecord(f,[f.host,f.guest],{emergency:true});guest=await progression(f.service.ctx(f.store),f.guest.id);assert.ok(guest.badges.find(b=>b.id==='b8').earned);
 host=await progression(f.service.ctx(f.store),f.host.id);assert.equal(host.badges.find(b=>b.id==='b8').earned,false);
});

test('tournament winner badge follows verified championship participation, not later team membership',async t=>{
 const f=await fixture(t);for(const actor of [f.host,f.guest]){await f.grant(actor);const version=(await f.service.bootstrap(actor)).profile.version;await f.cmd(actor,'profile.save',{...f.passport(),expectedVersion:version,skills:[{sport:'CHESS',level:'BEGINNER',position:''}]});}
 const home=(await f.cmd(f.host,'team.create',{name:'Satranç A',sport:'CHESS',area:'Kadıköy'})).result.id;
 const away=(await f.cmd(f.guest,'team.create',{name:'Satranç B',sport:'CHESS',area:'Kadıköy'})).result.id;
 const cup=(await f.cmd(f.host,'tournament.create',{name:'Satranç kupası',sport:'CHESS',capacity:2,dates:'Kasım',formatCode:'KNOCKOUT',isLeague:false})).result.id;
 await f.cmd(f.host,'tournament.register',{tournamentId:cup,teamId:home});await f.cmd(f.guest,'tournament.register',{tournamentId:cup,teamId:away});
 await f.cmd(f.host,'tournament.schedule',{tournamentId:cup,startsAtEpochMillis:f.clock()+3600000,venue:'Kulüp'});
 const final=await f.store.get('SELECT * FROM tournament_fixtures WHERE tournament_id=?',[cup]);
 const champion=final.home_team,championUser=champion===home?f.host:f.guest,loser=champion===home?f.guest:f.host;
 f.advance(3*3600000);await f.store.run('UPDATE event_members SET checked_in_at=? WHERE event_id=?',[f.clock()-2*3600000,final.event_id]);
 await f.store.run("UPDATE events SET status='VERIFIED' WHERE id=?",[final.event_id]);await f.store.run("INSERT INTO results(event_id,submitted_by,payload,status,dispute_until,created_at) VALUES(?,?,?,'verified',?,?)",[final.event_id,championUser.id,JSON.stringify({homeScore:1,awayScore:0}),f.clock(),f.clock()]);
 assert.equal((await progression(f.service.ctx(f.store),championUser.id)).badges.find(b=>b.id==='b5').earned,false);
 await f.cmd(f.host,'tournament.schedule',{tournamentId:cup,startsAtEpochMillis:f.clock()+3600000,venue:'Kulüp'});
 assert.ok((await progression(f.service.ctx(f.store),championUser.id)).badges.find(b=>b.id==='b5').earned);
 assert.equal((await progression(f.service.ctx(f.store),loser.id)).badges.find(b=>b.id==='b5').earned,false);
 assert.equal((await f.service.bootstrap(championUser)).matchHistory[0].won,true);
 assert.equal((await f.service.bootstrap(loser)).matchHistory[0].won,false);
});
