import { ensure } from './errors.mjs';
export async function saveMeetingLocation(ctx,eventId,point){
 if(!point){await ctx.db.run('DELETE FROM event_locations WHERE event_id=?',[eventId]);return;}
 ensure(Number.isFinite(point.latitude)&&Math.abs(point.latitude)<=90&&Number.isFinite(point.longitude)&&Math.abs(point.longitude)<=180,400,'INVALID_LOCATION','Buluşma noktası geçersiz.');
 await ctx.db.run('INSERT INTO event_locations(event_id,latitude,longitude) VALUES(?,?,?) ON CONFLICT(event_id) DO UPDATE SET latitude=excluded.latitude,longitude=excluded.longitude',[eventId,point.latitude,point.longitude]);
}
export async function meetingLocation(ctx,event){
 if(!ctx.actor)return null;
 if(event.host_id!==ctx.actor.id){
  if(ctx.now>Number(event.ends_at)+24*3600000)return null;
  const member=ctx.eventMembersByEventId?.has(event.id)
   ? ctx.eventMembersByEventId.get(event.id).some(m=>m.user_id===ctx.actor.id)
   : !!await ctx.db.get("SELECT user_id FROM event_members WHERE event_id=? AND user_id=? AND status='joined'",[event.id,ctx.actor.id]);
  if(!member)return null;
 }
 const point=ctx.eventLocationsByEventId?.has(event.id)
  ? ctx.eventLocationsByEventId.get(event.id)
  : await ctx.db.get('SELECT latitude,longitude FROM event_locations WHERE event_id=?',[event.id]);
 return point?{latitude:Number(point.latitude),longitude:Number(point.longitude)}:null;
}
