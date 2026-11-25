import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { sign } from '../src/auth.mjs';
import { synchronizeStore,storeConfiguration } from '../src/store-sync.mjs';
import { runJobs } from '../src/jobs.mjs';

test('only a host can edit a future pickup match, with version and roster guards',async t=>{
 const f=await fixture(t);await f.grant(f.host);await f.grant(f.guest);const event=(await f.cmd(f.host,'event.create',f.event())).result.id;
 await f.cmd(f.guest,'event.join',{eventId:event});let version=(await f.service.bootstrap(f.host)).events[0].version;
 await assert.rejects(f.cmd(f.guest,'event.update',{eventId:event,expectedVersion:version,title:'Başka maç'}),e=>e.code==='FORBIDDEN');
 await f.cmd(f.host,'event.update',{eventId:event,expectedVersion:version,title:'Yeni maç planı',venue:'Yeni saha'});
 await assert.rejects(f.cmd(f.host,'event.update',{eventId:event,expectedVersion:version,title:'Eski sürüm'}),e=>e.code==='VERSION_CONFLICT');
 assert.ok((await f.service.bootstrap(f.guest)).notifications.some(n=>n.title==='Maç planı güncellendi'));
 f.advance(3600001);await assert.rejects(f.cmd(f.host,'event.update',{eventId:event,expectedVersion:version+1,title:'Geç düzenleme'}),e=>e.code==='EVENT_CLOSED');
});
test('team application withdrawal and owner transfer recheck ownership and real membership',async t=>{
 const f=await fixture(t),team=(await f.cmd(f.host,'team.create',{name:'Kadıköy Takımı',sport:'FOOTBALL',area:'Kadıköy'})).result.id;
 const request=(await f.cmd(f.guest,'team.request',{teamId:team,position:'Orta saha'})).result.id;
 await assert.rejects(f.cmd(f.other,'team.request.cancel',{requestId:request}),e=>e.code==='FORBIDDEN');await f.cmd(f.guest,'team.request.cancel',{requestId:request});
 await assert.rejects(f.cmd(f.guest,'team.update',{teamId:team,description:'Yetkisiz',recruitingOpen:true}),e=>e.code==='FORBIDDEN');
 await f.cmd(f.host,'team.update',{teamId:team,description:'Yeni ilan',recruitingOpen:false});
 const community=(await f.cmd(f.host,'community.create',{name:'Spor Ağı',area:'Kadıköy'})).result.id;
 await assert.rejects(f.cmd(f.host,'community.transfer',{communityId:community,userId:f.guest.id}),e=>e.code==='MEMBERSHIP_REQUIRED');
 await f.cmd(f.guest,'community.join',{communityId:community});await f.cmd(f.host,'community.transfer',{communityId:community,userId:f.guest.id});
 await f.cmd(f.host,'community.leave',{communityId:community});
});
test('passport QR is short-lived, signed, and contains no private contacts',async t=>{
 const f=await fixture(t),code=(await f.cmd(f.host,'passport.code')).result.code,data=await f.service.verifyPassport(code);
 assert.equal(data.name,'Ev sahibi');assert.equal(data.reliability,100);assert.equal(data.phone,undefined);assert.equal(data.email,undefined);
 await assert.rejects(f.service.verifyPassport(code+'a'),e=>e.code==='PASSPORT_CODE_INVALID');f.advance(900001);await assert.rejects(f.service.verifyPassport(code),e=>e.code==='PASSPORT_CODE_INVALID');
});
test('profile edits cannot replace a verified phone without OTP or falsely retain its verification',async t=>{
 const f=await fixture(t);const challenge=await f.auth.startOtp({phone:'5321234567'},'contact',f.host);await f.auth.verifyContact(f.host,{challengeId:challenge.challengeId,code:challenge.developmentCode});
 const before=(await f.service.bootstrap(f.host)).profile;
 await assert.rejects(f.cmd(f.host,'profile.save',{...f.passport(),phone:'+905399999999',expectedVersion:before.version}),e=>e.code==='PHONE_VERIFICATION_REQUIRED');
 const after=(await f.service.bootstrap(f.host)).profile;assert.equal(after.phone,'+905321234567');assert.ok(after.verifications.includes('BASIC'));
});
test('private photo forwarding requires access to its source; approved public memories can be shared',async t=>{
 const f=await fixture(t);for(const actor of [f.host,f.guest,f.other])await f.cmd(actor,'preferences.save',{privacy:{dmPolicy:'Herkes'}});
 const first=(await f.cmd(f.host,'chat.direct',{userId:f.guest.id})).result.id,second=(await f.cmd(f.other,'chat.direct',{userId:f.guest.id})).result.id;
 const sharp=(await import('sharp')).default,image=await sharp({create:{width:4,height:4,channels:3,background:'white'}}).jpeg().toBuffer(),upload=(await f.service.upload(f.host,image,'image/jpeg')).id;
 const message=(await f.cmd(f.host,'chat.send',{threadId:first,text:'Özel fotoğraf',uploadId:upload})).result.id;
 await assert.rejects(f.cmd(f.other,'chat.send',{threadId:second,text:'İzinsiz',uploadId:upload,forwardId:message}),e=>e.code==='FORBIDDEN');
 await f.cmd(f.guest,'chat.send',{threadId:second,text:'İletilen',uploadId:upload,forwardId:message});
 const memory=(await f.cmd(f.host,'memory.create',{caption:'Anı',sport:'FOOTBALL',venue:'Saha',area:'Kadıköy',city:'İstanbul',uploadId:upload})).result.id;
 await f.store.run("UPDATE memories SET status='pending' WHERE id=?",[memory]);
 await assert.rejects(f.cmd(f.other,'chat.send',{threadId:second,text:'Bekleyen',uploadId:upload,forwardId:memory}),e=>e.code==='NOT_FOUND');
 await f.cmd(f.admin,'admin.memory.review',{memoryId:memory,approved:true,reason:'Uygun fotoğraf'});
 await f.cmd(f.other,'chat.send',{threadId:second,text:'Vitrinden',uploadId:upload,forwardId:memory});
});
test('server store configuration never returns a secret API key in place of an SDK key',()=>{
 const config=storeConfiguration({production:true,revenueCatAppleKey:'sk_PRIVATE',revenueCatGoogleKey:'goog_PUBLIC',revenueCatSecretKey:'sk_SERVER',billingSecret:'secret',revenueCatAuthorization:'private'});
 assert.equal(config.keys.ios,null);assert.equal(config.keys.android,'goog_PUBLIC');assert.ok(!JSON.stringify(config).includes('sk_'));
});
test('verified store sync is replay-safe and never transfers a transaction between users',async t=>{
 const f=await fixture(t);f.config.revenueCatSecretKey='sk_test';
 const subscriber={original_app_user_id:f.host.id,subscriptions:{},non_subscriptions:{tkn_50:[{store_transaction_id:'verified-store-tx-1',store:'app_store',purchase_date:new Date(f.clock()).toISOString()}]}};
 const request=async()=>Response.json({subscriber});
 await f.store.transaction(db=>synchronizeStore({db,actor:f.host,config:f.config,now:f.clock()},request));
 await f.store.transaction(db=>synchronizeStore({db,actor:f.host,config:f.config,now:f.clock()},request));
 assert.equal((await f.service.bootstrap(f.host)).wallet.balance,70);
 await assert.rejects(f.store.transaction(db=>synchronizeStore({db,actor:f.guest,config:f.config,now:f.clock()},request)),e=>e.code==='PURCHASE_OWNERSHIP');
});
test('Plus restores do not duplicate monthly grants and a later renewal cannot earn tokens for an unpaid gap',async t=>{
 const f=await fixture(t);
 async function purchase(eventId,transactionId,origin,at,expires){const raw=Buffer.from(JSON.stringify({eventId,type:'purchase',userId:f.host.id,transactionId,originalTransactionId:origin,productId:'pro_monthly',platform:'apple',purchasedAt:at,expiresAt:expires}));return f.service.webhook(raw,{'x-matchora-timestamp':String(f.clock()),'x-matchora-signature':sign(f.config.billingSecret,`${f.clock()}.${raw}`)});}
 const at=f.clock();await purchase('first-event','first-tx','first-origin',at,at+28*86400000);assert.equal((await f.service.bootstrap(f.host)).wallet.balance,120);
 await purchase('restore-event','first-tx','restore-alias',at,at+28*86400000);assert.equal((await f.service.bootstrap(f.host)).wallet.balance,120);
 f.advance(95*86400000);await runJobs(f.service);await purchase('renew-after-gap','new-tx','new-origin',f.clock(),f.clock()+28*86400000);await runJobs(f.service);
 assert.equal((await f.service.bootstrap(f.host)).wallet.balance,220);
});
test('new legal versions require acceptance again while deletion and support remain usable',async t=>{
 const f=await fixture(t);f.config.production=true;
 await f.cmd(f.admin,'admin.legal.publish',{kind:'privacy',version:'1',url:'https://example.invalid/privacy'});
 await f.cmd(f.admin,'admin.legal.publish',{kind:'terms',version:'1',url:'https://example.invalid/terms'});
 assert.equal((await f.service.bootstrap(f.host)).legalAcceptanceRequired,true);
 await assert.rejects(f.cmd(f.host,'event.create',f.event()),e=>e.code==='LEGAL_ACCEPTANCE_REQUIRED');
 await f.cmd(f.host,'legal.accept',{termsVersion:'1',privacyVersion:'1'});
 assert.equal((await f.service.bootstrap(f.host)).legalAcceptanceRequired,false);
 await f.cmd(f.admin,'admin.legal.publish',{kind:'terms',version:'2',url:'https://example.invalid/terms-v2'});
 assert.equal((await f.service.bootstrap(f.host)).legalAcceptanceRequired,true);
 await f.cmd(f.host,'support.create',{subject:'Hesap sorusu',category:'Hesap',message:'Yeni belgeler hakkında destek istiyorum.'});
 await f.cmd(f.host,'account.delete',{});
});
test('moderators can inspect pending showcase photos without granting access to unrelated private photos',async t=>{
 const f=await fixture(t),sharp=(await import('sharp')).default,bytes=await sharp({create:{width:4,height:4,channels:3,background:'white'}}).jpeg().toBuffer(),upload=(await f.service.upload(f.host,bytes,'image/jpeg')).id;
 await assert.rejects(f.service.mediaFile(f.admin,upload),e=>e.code==='NOT_FOUND');
 await f.cmd(f.host,'memory.create',{caption:'İnceleme',sport:'FOOTBALL',venue:'Saha',area:'Kadıköy',city:'İstanbul',uploadId:upload});
 await f.store.run("UPDATE memories SET status='pending' WHERE upload_id=?",[upload]);
 assert.ok((await f.service.mediaFile(f.admin,upload)).bytes.length>0);
 await assert.rejects(f.service.mediaFile(f.guest,upload),e=>e.code==='NOT_FOUND');
});
test('optional null fields clear on update and billing accepts minor clock skew',async t=>{
 const f=await fixture(t);await f.grant(f.host);
 const ev=(await f.cmd(f.host,'event.create',{...f.event(),sport:'ESPORTS_VALORANT',mode:'SOCIAL',gamePlatform:'PC',gameServer:'TR',voiceChannel:'Discord',hostGameTag:'tag#1'})).result.id;
 await f.cmd(f.host,'event.update',{eventId:ev,expectedVersion:1,description:null,gamePlatform:null,gameServer:null,voiceChannel:null,hostGameTag:null});
 const updatedEv=(await f.service.bootstrap(f.host)).events.find(e=>e.id===ev);
 assert.equal(updatedEv.gamePlatform,'');assert.equal(updatedEv.description,'');

 const team=(await f.cmd(f.host,'team.create',{name:'Yıldız FK',sport:'FOOTBALL',area:'Kadıköy',homeVenue:'Fenerbahçe'})).result.id;
 await f.cmd(f.host,'team.update',{teamId:team,homeVenue:null,description:null});
 assert.equal((await f.service.bootstrap(f.host)).teams.find(x=>x.id===team).homeVenue,'');

 await f.cmd(f.admin,'admin.operator.assign',{userId:f.host.id,role:'venue_owner',reason:'Saha sahibi yetkisi verildi.'});
 const venue=(await f.cmd(f.host,'venue.create',{name:'Arena',city:'İstanbul',area:'Kadıköy',address:'Moda Caddesi No 12',phone:'+905551112233',description:'Test',sports:['FOOTBALL'],amenities:['Duş'],courts:1,cancellationHours:12,photoUploadIds:[]})).result.id;
 await f.cmd(f.host,'venue.update',{venueId:venue,expectedVersion:1,name:'Arena',city:'İstanbul',area:'Kadıköy',sports:['FOOTBALL'],courts:1,cancellationHours:12,phone:null,description:null});
 const updatedVenue=(await f.service.bootstrap(f.host)).venues.find(v=>v.id===venue);
 assert.equal(updatedVenue.phone,'');assert.equal(updatedVenue.description,'');

 const tId=(await f.cmd(f.host,'tournament.create',{name:'Kupam',sport:'FOOTBALL',format:'Eleme',capacity:4,dates:'Hafta sonu',venue:'Saha 1',prizeToken:'10 TKN',prizeCash:'500 TL'})).result.id;
 await f.cmd(f.host,'tournament.update',{tournamentId:tId,venue:null,prizeToken:null,prizeCash:null});
 const updatedT=(await f.service.bootstrap(f.host)).tournaments.find(x=>x.id===tId);
 assert.equal(updatedT.venue,'');assert.equal(updatedT.prizeToken,'');assert.equal(updatedT.prizeCash,'');

 const raw=Buffer.from(JSON.stringify({eventId:'skew-1',type:'purchase',userId:f.host.id,transactionId:'skew-tx',originalTransactionId:'skew-orig',productId:'pro_monthly',platform:'apple',purchasedAt:f.clock()+30000,expiresAt:f.clock()+28*86400000}));
 await f.service.webhook(raw,{'x-matchora-timestamp':String(f.clock()),'x-matchora-signature':sign(f.config.billingSecret,`${f.clock()}.${raw}`)});
});
