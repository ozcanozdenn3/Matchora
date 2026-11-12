import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { processDeletions } from '../src/jobs.mjs';
const fails=code=>error=>error.code===code;
const venue={name:'Moda spor tesisi',city:'İstanbul',area:'Kadıköy',address:'Moda Caddesi 30 Kadıköy İstanbul',sports:['FOOTBALL','TENNIS'],amenities:['Duş'],courts:2,cancellationHours:24};

test('a community publishes with its owner and approval requests reach only the manager, then notify the applicant',async t=>{
 const f=await fixture(t);
 const id=(await f.cmd(f.host,'community.create',{name:'Moda sporcuları',city:'İstanbul',area:'Kadıköy',type:'CLUB',access:'APPROVAL',sport:'FOOTBALL'})).result.id;
 assert.equal((await f.service.bootstrap()).communities.find(c=>c.id===id).ownerId,f.host.id);
 assert.equal((await f.cmd(f.guest,'community.join',{communityId:id})).result.status,'pending');
 const host=await f.service.bootstrap(f.host),guest=await f.service.bootstrap(f.guest),other=await f.service.bootstrap(f.other);
 assert.equal(host.communityRequests.length,1);assert.equal(other.communityRequests.length,0);
 assert.ok(host.notifications.some(n=>n.destination==='NETWORK'&&n.targetId===id));assert.ok(!guest.joinedCommunityIds.includes(id));
 await f.cmd(f.guest,'community.join',{communityId:id});
 assert.equal((await f.service.bootstrap(f.host)).notifications.filter(n=>n.title==='Topluluk katılım başvurusu').length,1);
 await assert.rejects(f.cmd(f.other,'community.review',{communityId:id,userId:f.guest.id,approve:true}),fails('FORBIDDEN'));
 await f.cmd(f.host,'community.review',{communityId:id,userId:f.guest.id,approve:true});
 const accepted=await f.service.bootstrap(f.guest);assert.ok(accepted.joinedCommunityIds.includes(id));assert.ok(accepted.notifications.some(n=>n.title==='Topluluk başvurun kabul edildi'&&n.destination==='NETWORK'));
 await f.cmd(f.guest,'community.leave',{communityId:id});assert.ok(!(await f.service.bootstrap(f.guest)).joinedCommunityIds.includes(id));
});

test('a regular athlete applies for a venue; only versioned administrator approval publishes and grants management',async t=>{
 const f=await fixture(t),id=(await f.cmd(f.host,'venue.application.create',venue)).result.id;
 assert.equal((await f.service.bootstrap()).venues.length,0);
 assert.equal((await f.service.bootstrap(f.guest)).venueApplications.length,0);
 assert.equal((await f.service.adminInbox(f.admin)).venueApplications[0].id,id);
 assert.ok((await f.service.bootstrap(f.admin)).notifications.some(n=>n.destination==='MODERATION'));
 await assert.rejects(f.cmd(f.host,'venue.create',venue),fails('FORBIDDEN'));
 await assert.rejects(f.cmd(f.host,'admin.venue.application.review',{applicationId:id,expectedVersion:1,approve:true,response:'Onaylandı'}),fails('FORBIDDEN'));
 await assert.rejects(f.cmd(f.admin,'admin.venue.application.review',{applicationId:id,expectedVersion:2,approve:true,response:'Onaylandı'}),fails('VERSION_CONFLICT'));
 const result=(await f.cmd(f.admin,'admin.venue.application.review',{applicationId:id,expectedVersion:1,approve:true,response:'İşletmeci bilgileri doğrulandı.'})).result;
 const own=await f.service.bootstrap(f.host);assert.equal(own.role,'venue_owner');assert.ok(own.venues.some(v=>v.id===result.venueId));assert.equal(own.venueApplications[0].status,'approved');
 assert.equal((await f.service.operatorVenues(f.host)).items[0].id,result.venueId);
 await assert.rejects(f.cmd(f.admin,'admin.venue.application.review',{applicationId:id,expectedVersion:2,approve:true,response:'Tekrar'}),fails('INVALID_STATE'));
 const slots=[];for(const court of ['Kort 1','Kort 2'])slots.push((await f.cmd(f.host,'venue.slot.create',{venueId:result.venueId,court,startsAtEpochMillis:f.clock()+3600000,durationMin:60,priceMinor:12345})).result.id);
 assert.equal(new Set(slots).size,2);
 await assert.rejects(f.cmd(f.host,'venue.slot.create',{venueId:result.venueId,court:'Kort 3',startsAtEpochMillis:f.clock()+3600000,durationMin:60,priceMinor:12345}),fails('COURT_CAPACITY')); 
 await assert.rejects(f.cmd(f.host,'venue.slot.create',{venueId:result.venueId,court:'KORT   1',startsAtEpochMillis:f.clock()+3600000,durationMin:60,priceMinor:12345}),fails('SLOT_OVERLAP'));
 const booking=(await f.cmd(f.guest,'booking.create',{slotId:slots[0],splitCount:3})).result;assert.equal(booking.perPersonMinor,4115);
 await assert.rejects(f.cmd(f.other,'booking.create',{slotId:slots[0],splitCount:1}),fails('SLOT_BOOKED'));
});

test('venue applications support withdrawal, rejection, ownership isolation, export and personal-data deletion',async t=>{
 const f=await fixture(t),id=(await f.cmd(f.host,'venue.application.create',venue)).result.id;
 await assert.rejects(f.cmd(f.host,'venue.application.create',venue),fails('ALREADY_REQUESTED'));
 await assert.rejects(f.cmd(f.guest,'venue.application.withdraw',{applicationId:id,expectedVersion:1}),fails('FORBIDDEN'));
 await f.cmd(f.host,'venue.application.withdraw',{applicationId:id,expectedVersion:1});
 const retry=(await f.cmd(f.host,'venue.application.create',venue)).result.id;
 await f.cmd(f.admin,'admin.venue.application.review',{applicationId:retry,expectedVersion:1,approve:false,response:'İşletme belgesini doğrulayamadık.'});
 assert.equal((await f.service.bootstrap(f.host)).role,'user');assert.equal((await f.service.exportAccount(f.host)).venueApplications.length,2);
 const pending=(await f.cmd(f.host,'venue.application.create',venue)).result.id;
 await f.cmd(f.host,'account.delete');f.advance(2000);await f.store.transaction(db=>processDeletions(f.service.ctx(db)));
 const cleaned=await f.store.get('SELECT payload,status FROM venue_applications WHERE id=?',[pending]);assert.equal(cleaned.payload,'{}');assert.equal(cleaned.status,'withdrawn');
});

test('approval-required pickup listings cannot be bypassed by map quick join and host acceptance remains authoritative',async t=>{
 const f=await fixture(t);await f.grant(f.host);await f.grant(f.guest);
 const id=(await f.cmd(f.host,'event.create',f.event({listingType:'SEEK_PLAYERS',approvalRequired:true}))).result.id;
 await assert.rejects(f.cmd(f.guest,'event.join',{eventId:id}),fails('APPROVAL_REQUIRED'));
 const request=(await f.cmd(f.guest,'event.request',{eventId:id,position:'Kaleci',note:'Saatinde gelirim'})).result.id;
 assert.ok(!(await f.service.bootstrap(f.guest)).joinedEventIds.includes(id));
 await assert.rejects(f.cmd(f.other,'event.request.review',{requestId:request,approve:true}),fails('FORBIDDEN'));
 await f.cmd(f.host,'event.request.review',{requestId:request,approve:true});
 assert.ok((await f.service.bootstrap(f.guest)).joinedEventIds.includes(id));
 await f.cmd(f.host,'event.member.remove',{eventId:id,userId:f.guest.id,expectedVersion:(await f.service.event(f.host,id)).version});
 await assert.rejects(f.cmd(f.guest,'event.request',{eventId:id}),fails('EVENT_REMOVED'));
});

test('accepting a waitlist offer on an approval listing creates a host request, never automatic membership',async t=>{
 const f=await fixture(t);for(const actor of [f.host,f.guest,f.other])await f.grant(actor);
 const id=(await f.cmd(f.host,'event.create',f.event({capacity:2,approvalRequired:true}))).result.id;
 const req=(await f.cmd(f.guest,'event.request',{eventId:id})).result.id;await f.cmd(f.host,'event.request.review',{requestId:req,approve:true});
 await f.cmd(f.other,'event.waitlist',{eventId:id,action:'join'});await f.cmd(f.guest,'event.leave',{eventId:id});
 const result=(await f.cmd(f.other,'event.waitlist',{eventId:id,action:'accept'})).result;
 assert.equal(result.status,'pending');assert.ok(!(await f.service.bootstrap(f.other)).joinedEventIds.includes(id));
 await f.cmd(f.host,'event.request.review',{requestId:result.requestId,approve:true});assert.ok((await f.service.bootstrap(f.other)).joinedEventIds.includes(id));
});
