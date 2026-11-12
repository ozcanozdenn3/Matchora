import { mkdtemp,rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { randomUUID } from 'node:crypto';
import { SqliteStore,migrate } from '../src/store.mjs';
import { configFromEnv } from '../src/config.mjs';
import { Auth } from '../src/auth.mjs';
import { MatchoraService } from '../src/service.mjs';
import { LocalMediaStore } from '../src/media.mjs';
import { ledger } from '../src/model.mjs';
export async function fixture(t,{file=false}={}){
 const directory=await mkdtemp(join(tmpdir(),'matchora-test-'));const path=file?join(directory,'test.sqlite'):':memory:';const store=await SqliteStore.open(path);await migrate(store);
 let now=Date.UTC(2026,10,3,9);const clock=()=>now;
 const config={...configFromEnv({MATCHORA_ENV:'development',DEV_AUTH_ENABLED:'true',CHECKIN_SECRET:'test-secret-012345678901234567890',BILLING_WEBHOOK_SECRET:'billing-test-secret-0123456789012345'}),admins:new Set(['dev:admin']),deletionDelay:1000};
 const auth=new Auth(store,config,clock),media=new LocalMediaStore(join(directory,'uploads')),service=new MatchoraService(store,config,media,clock);
 const sessions={};async function user(name){const session=await auth.devLogin({identity:name});sessions[name]=session;return auth.actor(session.accessToken);}
 const host=await user('host'),guest=await user('guest'),other=await user('other'),admin=await user('admin');
 const cmd=(actor,operation,payload={},key=randomUUID())=>service.command(actor,operation,payload,key);
 const passport=(name='Sporcu')=>({name,city:'İstanbul',area:'Kadıköy',bio:'',birthYear:1997,phone:'',email:'',skills:[{sport:'FOOTBALL',level:'INTERMEDIATE',position:'Orta saha'}],availability:[],dominantSide:'',campus:null,company:null,jerseyNumber:null});
 for(const [actor,name]of [[host,'Ev sahibi'],[guest,'Konuk sporcu'],[other,'Diğer sporcu'],[admin,'Operatör']])await cmd(actor,'profile.save',passport(name));
 const event=(extra={})=>({title:'Akşam maçı',sport:'FOOTBALL',venue:'Kadıköy saha',city:'İstanbul',area:'Kadıköy',capacity:4,startsAtEpochMillis:now+3600000,durationMin:60,mode:'COMPETITIVE',...extra});
 async function grant(actor,amount=1000){await store.transaction(db=>ledger({db,now:clock()},actor.id,'BONUS',amount,0,'test',randomUUID(),'Test bakiyesi'));}
 t.after(async()=>{await store.close();await rm(directory,{recursive:true,force:true});});
 return{directory,path,store,config,auth,service,host,guest,other,admin,sessions,user,cmd,passport,event,clock,advance:delta=>{now+=delta;},grant};
}
