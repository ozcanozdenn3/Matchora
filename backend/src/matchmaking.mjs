import { activePoint,distanceKm,ownDiscovery } from './geo.mjs';
import { json } from './errors.mjs';
export async function matchScore(ctx,event,user){
 if(!user)return{time:50,distance:50,level:50,reliability:50,preference:50,reasons:['Tercihlerin için sporcu pasaportunu tamamla.'],confidence:0};
 if(ctx.actor){
  if(ctx.matchSignalsEnabled===undefined){const prefs=await ctx.db.get('SELECT privacy FROM preferences WHERE user_id=?',[ctx.actor.id]);ctx.matchSignalsEnabled=!prefs||json(prefs.privacy).chemistrySignals!==false;}
  if(!ctx.matchSignalsEnabled)return{time:50,distance:50,level:50,reliability:50,preference:50,reasons:['Kişisel eşleşme sinyalleri tercihlerinden kapalı.'],confidence:0};
 }
 const discovery=await ownDiscovery(ctx),point=activePoint(discovery,ctx.now);
 const settings=json(event.payload),skill=user.skills.find(s=>s.sport===event.sport),sameCity=event.city===(ctx.discoveryCity??discovery.city??user.city),sameArea=sameCity&&event.district===(discovery.district??user.area);
 const actualDistance=point&&event.latitude!=null&&event.longitude!=null?distanceKm(point,{latitude:Number(event.latitude),longitude:Number(event.longitude)}):null;
 const hour=new Intl.DateTimeFormat('en-GB',{timeZone:'Europe/Istanbul',hour:'2-digit',minute:'2-digit',hour12:false}).format(Number(event.starts_at));
 const day=new Intl.DateTimeFormat('tr-TR',{timeZone:'Europe/Istanbul',weekday:'short'}).format(Number(event.starts_at)).slice(0,3);
 const time=user.availability.length?user.availability.some(a=>{const times=a.match(/\d{2}:\d{2}/g);return times?.length>=2&&a.toLocaleLowerCase('tr').includes(day.toLocaleLowerCase('tr'))&&hour>=times[0]&&hour<=times[1];})?100:35:65;
 const distance=actualDistance==null?(sameArea?100:sameCity?70:15):actualDistance<=3?100:actualDistance<=10?85:actualDistance<=25?70:actualDistance<=50?40:10,level=skill?85:30,reliability=Math.max(0,Math.min(100,user.reliability)),preference=skill?100:20;
 const reasons=[];if(actualDistance!=null)reasons.push('Yaklaşık konumuna göre uzaklık');else if(sameArea)reasons.push('Tercih ettiğin ilçe');else if(sameCity)reasons.push('Aynı şehir');if(skill)reasons.push('Seçtiğin branş');if(time===100)reasons.push('Müsaitlik saatlerine uygun');if(settings.verifiedOnly)reasons.push('İletişim doğrulaması gerekli');
 return{time,distance,level,reliability,preference,reasons,confidence:Math.min(100,user.verifiedMatches*10)};
}
export async function chemistry(ctx,first,second){const ratings=await ctx.db.all("SELECT f.play_again FROM feedback f JOIN results r ON r.event_id=f.event_id WHERE r.status='verified' AND ((f.from_id=? AND f.to_id=?) OR (f.from_id=? AND f.to_id=?))",[first,second,second,first]);if(!ratings.length)return{score:null,confidence:0,sampleCount:0};return{score:Math.round(ratings.reduce((n,r)=>n+r.play_again,0)*100/ratings.length),confidence:Math.min(100,ratings.length*20),sampleCount:ratings.length};}
