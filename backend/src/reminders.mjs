import { json } from './errors.mjs';
import { notify } from './model.mjs';
export async function scheduleReminders(ctx){
 const events=await ctx.db.all("SELECT * FROM events WHERE starts_at>? AND starts_at<=? AND status IN ('PUBLISHED','FILLING','FULL')",[ctx.now,ctx.now+3600000]);
 for(const event of events){
  const data=json(event.payload);
  const members=await ctx.db.all("SELECT m.user_id,p.notifications FROM event_members m JOIN users u ON u.id=m.user_id JOIN preferences p ON p.user_id=m.user_id WHERE m.event_id=? AND m.status='joined' AND u.status='active'",[event.id]);
  for(const member of members)if(json(member.notifications).upcomingMatch!==false)
   await notify(ctx,member.user_id,'matches','Maçın yaklaşıyor',`${data.title} · ${data.venue}`,'EVENT',event.id,`reminder:${event.id}:${event.starts_at}:${member.user_id}`);
 }
}
