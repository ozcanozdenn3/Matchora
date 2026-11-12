import { json } from './errors.mjs';
export function distanceKm(a,b){
 const rad=Math.PI/180,dLat=(b.latitude-a.latitude)*rad,dLon=(b.longitude-a.longitude)*rad;
 const h=Math.sin(dLat/2)**2+Math.cos(a.latitude*rad)*Math.cos(b.latitude*rad)*Math.sin(dLon/2)**2;
 return 6371*2*Math.asin(Math.sqrt(Math.min(1,Math.max(0,h))));
}
export async function ownDiscovery(ctx){
 if(ctx.ownDiscovery===undefined){const row=ctx.actor?await ctx.db.get('SELECT payload FROM discovery_preferences WHERE user_id=?',[ctx.actor.id]):null;ctx.ownDiscovery=row?json(row.payload):{};}
 return ctx.ownDiscovery;
}
export const activePoint=(discovery,now)=>discovery?.location?.expiresAt>now?discovery.location:null;
