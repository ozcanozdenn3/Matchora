import { RealtimeHub } from './realtime.mjs';
import { startEmailOtp, verifyEmailOtp } from './email-auth.mjs';
import { startOAuth,finishOAuth } from './oauth.mjs';
import { SupabaseMediaStore } from './storage.mjs';
import { gzip } from 'node:zlib';
import { createServer } from 'node:http';
import { pathToFileURL } from 'node:url';
import { randomUUID } from 'node:crypto';
import { SqliteStore,PostgresStore,migrate } from './store.mjs';
import { configFromEnv } from './config.mjs';
import { Auth,rateLimit } from './auth.mjs';
import { ApiError,ensure,keys } from './errors.mjs';
import { LocalMediaStore } from './media.mjs';
import { MatchoraService,handlers } from './service.mjs';
import { PRODUCTS } from './billing.mjs';
import { runJobs } from './jobs.mjs';
import { privacyHtml, termsHtml, guidelinesHtml, deletionHtml } from './legal-pages.mjs';
export async function readBody(req,limit=1024*1024){const chunks=[];let size=0;for await(const chunk of req){size+=chunk.length;ensure(size<=limit,413,'BODY_TOO_LARGE','İstek çok büyük.');chunks.push(chunk);}return Buffer.concat(chunks);}
export function createApi(service,auth){
 const realtime=new RealtimeHub(service,auth);
 const server=createServer(async(req,res)=>{
  console.log(`[API] ${req.method} ${req.url} from ${req.socket.remoteAddress}`);
  const requestId=randomUUID();res.setHeader('X-Request-Id',requestId);res.setHeader('X-Content-Type-Options','nosniff');res.setHeader('Cache-Control','no-store');res.setHeader('Content-Security-Policy',"default-src 'none'; frame-ancestors 'none'");
  const send = (status, value) => {
   if (res.destroyed) return;
   const body = JSON.stringify(value);
   res.setHeader('Content-Type', 'application/json; charset=utf-8');
   res.statusCode = status;
   const acceptsGzip = (req.headers['accept-encoding'] ?? '').split(',').some(entry =>
     /^gzip(?:\s*;\s*q=(?:1(?:\.0*)?|0\.\d*[1-9]\d*))?$/i.test(entry.trim()));
   if (body.length < 4096 || !acceptsGzip) { res.end(body); return; }
   res.appendHeader('Vary', 'Accept-Encoding');
   gzip(body, {level:1}, (error, compressed) => {
     if (res.destroyed) return;
     if (error) { res.end(body); return; }
     res.setHeader('Content-Encoding','gzip');
     res.end(compressed);
   });
  };
  const sendHtml=(status,html)=>{if(res.destroyed)return;res.setHeader('Content-Security-Policy',"default-src 'self' 'unsafe-inline'; style-src 'unsafe-inline'; frame-ancestors 'none'");res.writeHead(status,{'Content-Type':'text/html; charset=utf-8'});res.end(html);};
  try{
   const url=new URL(req.url,'http://localhost');const origin=req.headers.origin;if(origin){ensure(service.config.corsOrigins.has(origin),403,'ORIGIN_DENIED','Bu kaynaktan erişilemiyor.');res.setHeader('Access-Control-Allow-Origin',origin);res.setHeader('Vary','Origin');res.setHeader('Access-Control-Allow-Headers','Authorization,Content-Type,Idempotency-Key,Prefer');res.setHeader('Access-Control-Allow-Methods','GET,POST,OPTIONS');}
   if(req.method==='OPTIONS'){res.writeHead(204);res.end();return;}
   const bucket = req.socket.remoteAddress ?? 'unknown';
   const countRequest = db => rateLimit(db,`http:${bucket}`,service.clock(),240,60000);
   // PostgreSQL serializes this atomic upsert by bucket. Taking the global
   // business transaction lock here would queue even GETs behind unrelated writes.
   if (service.store instanceof PostgresStore) await countRequest(service.store);
   else await service.store.transaction(countRequest);
   if(req.method==='GET'&&(url.pathname==='/privacy'||url.pathname==='/v1/legal/privacy')){sendHtml(200,privacyHtml(service.config));return;}
   if(req.method==='GET'&&(url.pathname==='/terms'||url.pathname==='/v1/legal/terms')){sendHtml(200,termsHtml(service.config));return;}
   if(req.method==='GET'&&(url.pathname==='/guidelines'||url.pathname==='/v1/legal/guidelines')){sendHtml(200,guidelinesHtml(service.config));return;}
   if(req.method==='GET'&&(url.pathname==='/deletion'||url.pathname==='/v1/legal/deletion')){sendHtml(200,deletionHtml(service.config));return;}
   if(req.method==='GET'&&url.pathname==='/health'){await service.store.get('SELECT 1 AS ok');send(200,{status:'ok',version:1});return;}
   if(req.method==='GET'&&url.pathname==='/v1/config'){send(200,{...await service.configuration(),products:PRODUCTS});return;}
   if(req.method==='GET'&&url.pathname==='/v1/passports/verify'){send(200,await service.verifyPassport(url.searchParams.get('code')));return;}
   if(req.method==='GET'&&url.pathname==='/openapi.json'){send(200,{openapi:'3.1.0',info:{title:'Matchora API',version:'1.0.0'},paths:Object.fromEntries(Object.keys(handlers).map(operation=>[`/v1/commands/${operation}`,{post:{operationId:operation,security:[{bearerAuth:[]}],parameters:[{name:'Idempotency-Key',in:'header',required:true,schema:{type:'string',minLength:8}}],requestBody:{required:true,content:{'application/json':{schema:{type:'object'}}}},responses:{200:{description:'Atomic command result and current user-scoped snapshot'},400:{description:'Validation error'},401:{description:'Authentication required'},403:{description:'Forbidden'},409:{description:'Conflict'}}}}])),components:{securitySchemes:{bearerAuth:{type:'http',scheme:'bearer'}}}});return;}
   if(req.method==='POST'&&url.pathname==='/v1/billing/revenuecat'){send(200,await service.revenuecat(await readBody(req),req.headers));return;}
   if(req.method==='POST'&&url.pathname==='/v1/billing/webhook'){const raw=await readBody(req);send(200,await service.webhook(raw,req.headers));return;}
   const bearer=req.headers.authorization?.startsWith('Bearer ')?req.headers.authorization.slice(7):null;
   const optionalAuth=(['/v1/bootstrap','/v1/events','/v1/memories','/v1/rankings'].includes(url.pathname)||url.pathname.startsWith('/v1/events/')||url.pathname.startsWith('/v1/players/')||url.pathname.startsWith('/v1/venues/')||url.pathname.startsWith('/v1/catalog/'))||(url.pathname.startsWith('/v1/media/')&&req.method==='GET');
   let actor=null;if(bearer)actor=await auth.actor(bearer);else if(!optionalAuth&&!url.pathname.startsWith('/v1/auth/'))ensure(false,401,'UNAUTHENTICATED','Giriş yapmalısın.');
   if(req.method==='GET'&&url.pathname==='/v1/realtime'){await realtime.connect(req,res,actor,bearer);return;}
   if(req.method==='GET'&&url.pathname==='/v1/players/search'){send(200,await service.searchPlayers(actor,Object.fromEntries(url.searchParams)));return;}
   if(req.method==='GET'&&url.pathname.startsWith('/v1/history/')){send(200,await service.personalHistory(actor,url.pathname.split('/').at(-1),Object.fromEntries(url.searchParams)));return;}
   if(req.method==='GET'&&url.pathname==='/v1/bootstrap'){send(200,await service.bootstrap(actor,{city:url.searchParams.get('city')}));return;}
   if(req.method==='GET'&&/^\/v1\/players\/[a-fA-F0-9-]{36}$/.test(url.pathname)){send(200,await service.player(actor,url.pathname.split('/').at(-1),url.searchParams.get('sport')??'FOOTBALL'));return;}
   if(req.method==='GET'&&url.pathname==='/v1/rankings'){send(200,await service.rankings(actor,Object.fromEntries(url.searchParams)));return;}
   if(req.method==='GET'&&/^\/v1\/catalog\/[^/]+\/[a-fA-F0-9-]{36}$/.test(url.pathname)){const parts=url.pathname.split('/');send(200,await service.catalogItem(actor,parts[3],parts[4]));return;}
   if(req.method==='GET'&&/^\/v1\/catalog\/[^/]+$/.test(url.pathname)){send(200,await service.catalog(actor,url.pathname.split('/').at(-1),Object.fromEntries(url.searchParams)));return;}
   if(req.method==='GET'&&url.pathname==='/v1/notifications'){send(200,await service.notifications(actor,Object.fromEntries(url.searchParams)));return;}
   if(req.method==='GET'&&url.pathname==='/v1/memories'){send(200,await service.memories(actor,Object.fromEntries(url.searchParams)));return;}
   if(req.method==='GET'&&url.pathname==='/v1/events'){send(200,await service.events(actor,Object.fromEntries(url.searchParams)));return;}
   if(req.method==='GET'&&/^\/v1\/events\/[a-fA-F0-9-]{36}$/.test(url.pathname)){send(200,await service.event(actor,url.pathname.split('/').at(-1)));return;}
   if(req.method==='GET'&&url.pathname==='/v1/threads'){send(200,await service.threads(actor,Object.fromEntries(url.searchParams)));return;}
   if(req.method==='GET'&&/^\/v1\/threads\/[a-fA-F0-9-]{36}$/.test(url.pathname)){send(200,await service.thread(actor,url.pathname.split('/').at(-1)));return;}
   if(req.method==='GET'&&url.pathname.startsWith('/v1/threads/')){const parts=url.pathname.split('/');ensure(parts.length===5&&parts[4]==='messages',404,'NOT_FOUND','Bulunamadı.');send(200,await service.messages(actor,parts[3],url.searchParams.get('before'),url.searchParams.get('limit')));return;}
   if(req.method==='GET'&&url.pathname==='/v1/account/export'){res.setHeader('Content-Disposition','attachment; filename=matchora-account.json');send(200,await service.exportAccount(actor));return;}
   if(req.method==='GET'&&url.pathname==='/v1/account/sessions'){send(200,await service.sessions(actor));return;}
   if(req.method==='GET'&&url.pathname==='/v1/operator/venues'){send(200,await service.operatorVenues(actor));return;}
   if(req.method==='GET'&&/^\/v1\/venues\/[a-fA-F0-9-]{36}\/reviews$/.test(url.pathname)){send(200,await service.venueReviews(actor,url.pathname.split('/')[3],Object.fromEntries(url.searchParams)));return;}
   if(req.method==='GET'&&url.pathname==='/v1/admin/inbox'){send(200,await service.adminInbox(actor));return;}
   if(req.method==='GET'&&url.pathname.startsWith('/v1/media/')){const item=await service.mediaFile(actor,url.pathname.split('/').at(-1));res.writeHead(200,{'Content-Type':item.mime,'Content-Length':item.bytes.length,'Content-Disposition':'attachment; filename="photo"','Cache-Control':'private, max-age=60'});res.end(item.bytes);return;}
   if(req.method==='POST'&&url.pathname==='/v1/media'){const mime=req.headers['content-type']?.split(';')[0];ensure(['image/jpeg','image/png'].includes(mime),415,'INVALID_IMAGE','JPEG veya PNG gerekiyor.');send(201,await service.upload(actor,await readBody(req,5*1024*1024),mime,req.headers['idempotency-key']??null));return;}
   ensure(req.method==='POST',405,'METHOD_NOT_ALLOWED','İstek yöntemi desteklenmiyor.');ensure(req.headers['content-type']?.split(';')[0]==='application/json',415,'JSON_REQUIRED','JSON gövde gerekiyor.');let payload;try{payload=JSON.parse((await readBody(req)).toString('utf8'));}catch(error){if(error instanceof ApiError)throw error;throw new ApiError(400,'INVALID_JSON','JSON geçersiz.');}ensure(payload&&typeof payload==='object'&&!Array.isArray(payload));
   if(url.pathname==='/v1/command-receipts/resolve'){send(200,await service.commandReceipt(actor,payload));return;}
   if(/^\/v1\/threads\/[a-fA-F0-9-]{36}\/typing$/.test(url.pathname)){send(200,await realtime.typing(actor,url.pathname.split('/')[3],payload));return;}
   if(url.pathname==='/v1/auth/oauth/start'){send(200,await startOAuth(auth,payload,bucket));return;}
   if(url.pathname==='/v1/auth/oauth/finish'){await service.store.transaction(db=>rateLimit(db,`oauth-finish:${bucket}`,service.clock(),30,60000));send(200,await finishOAuth(auth,payload));return;}
   if(url.pathname==='/v1/auth/dev'){send(200,await auth.devLogin(payload));return;}
   if(url.pathname==='/v1/auth/exchange'){await service.store.transaction(db=>rateLimit(db,`exchange:${bucket}`,service.clock(),20,60000));send(200,await auth.exchange(payload));return;}
   if(url.pathname==='/v1/auth/id-token'){await service.store.transaction(db=>rateLimit(db,`id-token:${bucket}`,service.clock(),30,60000));send(200,await auth.exchangeIdToken(payload));return;}
   if(url.pathname==='/v1/auth/contact/start'){ensure(actor,401,'UNAUTHENTICATED','Giriş gerekiyor.');send(200,await auth.startOtp(payload,bucket,actor));return;}
   if(url.pathname==='/v1/auth/contact/verify'){ensure(actor,401,'UNAUTHENTICATED','Giriş gerekiyor.');await auth.verifyContact(actor,payload);send(200,{snapshot:await service.bootstrap(actor,{city:url.searchParams.get('city')})});return;}
   if(url.pathname==='/v1/auth/email/start'){send(200,await startEmailOtp(auth,payload,bucket));return;}
   if(url.pathname==='/v1/auth/email/verify'){send(200,await verifyEmailOtp(auth,payload,bucket));return;}
   if(url.pathname==='/v1/auth/otp/start'){send(200,await auth.startOtp(payload,bucket));return;}
   if(url.pathname==='/v1/auth/otp/verify'){send(200,await auth.verifyOtp(payload));return;}
   if(url.pathname==='/v1/auth/refresh'){send(200,await auth.refresh(payload));return;}
   if(url.pathname==='/v1/auth/logout'){ensure(actor,401,'UNAUTHENTICATED','Giriş gerekiyor.');keys(payload,['allDevices']);send(200,await auth.logout(actor,payload.allDevices===true));return;}
   const operation=url.pathname.startsWith('/v1/commands/')?url.pathname.slice('/v1/commands/'.length):null;ensure(operation,404,'NOT_FOUND','Bulunamadı.');const response=await service.command(actor,operation,payload,req.headers['idempotency-key'],{compact:req.headers.prefer==='return=changes',city:url.searchParams.get('city')});
   // Publishing a live hint must not turn an already committed command into a failed write.
   void realtime.command(actor,operation,payload,response.result).catch(()=>{});
   send(200,response);
  }catch(error){const known=error instanceof ApiError;send(known?error.status:500,{error:{code:known?error.code:'INTERNAL_ERROR',message:known?error.message:'İşlem tamamlanamadı. Tekrar dene.',requestId}});if(!known)console.error(JSON.stringify({level:'error',requestId,type:error.name,code:error.code??'internal'}));}
 });server.realtime=realtime;server.once('close',()=>realtime.close());server.requestTimeout=45000;server.headersTimeout=15000;server.keepAliveTimeout=5000;server.maxHeadersCount=50;server.maxRequestsPerSocket=100;return server;
}
export async function start(config=configFromEnv()){
 const store=config.databaseUrl?await PostgresStore.open(config.databaseUrl):await SqliteStore.open(config.databasePath);await migrate(store);const media=config.supabaseUrl&&config.storageServiceKey?new SupabaseMediaStore(config.supabaseUrl,config.storageServiceKey,config.storageBucket):new LocalMediaStore(config.storagePath),service=new MatchoraService(store,config,media),auth=new Auth(store,config),server=createApi(service,auth);let running=false;const timer=setInterval(async()=>{if(running)return;running=true;try{await runJobs(service);}catch(error){console.error(JSON.stringify({level:'error',task:'jobs',type:error.name}));}finally{running=false;}},15000);timer.unref();await new Promise(resolve=>server.listen(config.port,config.host,resolve));console.log(`Matchora API listening on ${config.host}:${config.port}`);
 const close=async()=>{clearInterval(timer);server.realtime.close();await new Promise(resolve=>server.close(resolve));await store.close();};return{server,service,auth,close};
}
if(process.argv[1]&&import.meta.url===pathToFileURL(process.argv[1]).href){process.on('uncaughtException',err=>console.error(JSON.stringify({level:'error',type:'uncaughtException',message:err?.message})));process.on('unhandledRejection',reason=>console.error(JSON.stringify({level:'error',type:'unhandledRejection',message:reason?.message??String(reason)})));const app=await start();for(const signal of ['SIGTERM','SIGINT'])process.once(signal,async()=>{await app.close();process.exit(0);});}
