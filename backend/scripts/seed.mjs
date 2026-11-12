import { SqliteStore,migrate } from '../src/store.mjs';
import { configFromEnv } from '../src/config.mjs';
import { Auth } from '../src/auth.mjs';
import { MatchoraService } from '../src/service.mjs';
import { LocalMediaStore } from '../src/media.mjs';
import { ledger } from '../src/model.mjs';
import { randomUUID } from 'node:crypto';
const config=configFromEnv();if(config.production||config.databaseUrl)throw new Error('Seed is restricted to a local SQLite development database.');
config.devAuth=true;config.admins=new Set(['dev:seed-admin']);const store=await SqliteStore.open(config.databasePath);await migrate(store);
try{
 const auth=new Auth(store,config),service=new MatchoraService(store,config,new LocalMediaStore(config.storagePath));
 const users=[];
 for(const identity of ['seed-admin','seed-player-a','seed-player-b']){
  const session=await auth.devLogin({identity}),actor=await auth.actor(session.accessToken);users.push(actor);
  if(!(await service.bootstrap(actor)).profile)await service.command(actor,'profile.save',{name:identity==='seed-admin'?'Moda Spor Organizatörü':identity.endsWith('a')?'Deniz Sporcu':'Ece Sporcu',city:'İstanbul',area:'Kadıköy',bio:'Yerel geliştirme verisi',birthYear:1997,skills:[{sport:'FOOTBALL',level:'INTERMEDIATE',position:'Orta saha'}],availability:['Pzt 19:00–23:00'],dominantSide:'Sağ ayak'},randomUUID());
  await store.transaction(db=>ledger({db,now:Date.now()},actor.id,'BONUS',1000,0,'development-seed',`seed:${actor.id}`,'Geliştirme bakiyesi'));
 }
 const [host,player]=users;
 if(!(await store.get('SELECT id FROM events LIMIT 1'))){const event=(await service.command(host,'event.create',{title:'Moda Akşam Halı Saha',sport:'FOOTBALL',venue:'Moda Spor Parkı',city:'İstanbul',area:'Kadıköy',capacity:14,startsAtEpochMillis:Date.now()+4*3600000,durationMin:60,mode:'SOCIAL',format:'7v7',approximateLocation:{latitude:40.98,longitude:29.03},description:'Yerel API üzerinden kalıcı olarak oluşturuldu.'},randomUUID())).result.id;await service.command(player,'event.join',{eventId:event},randomUUID());}
 if(!(await store.get('SELECT id FROM venues LIMIT 1'))){const venue=(await service.command(host,'venue.create',{name:'Moda Spor Tesisi',area:'Kadıköy',sports:['FOOTBALL','TENNIS'],courts:2,cancellationHours:3,amenities:['Duş','Otopark','Aydınlatma']},randomUUID())).result.id;await service.command(host,'venue.slot.create',{venueId:venue,court:'Saha 1',startsAtEpochMillis:Date.now()+24*3600000,durationMin:60,priceMinor:200000},randomUUID());}
 if(!(await store.get('SELECT id FROM communities LIMIT 1')))await service.command(host,'community.create',{name:'Kadıköy Spor Çevresi',area:'Kadıköy',access:'OPEN',description:'Yerel API geliştirme topluluğu'},randomUUID());
 console.log('Local development seed completed. No credentials or tokens are printed.');
}finally{await store.close();}
