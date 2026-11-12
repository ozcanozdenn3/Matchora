import { ensure,keys,text } from './errors.mjs';
import { audit } from './model.mjs';
import { rateLimit } from './auth.mjs';

export async function exportAccount(ctx){
 const userId=ctx.actor.id;
 await rateLimit(ctx.db,`export:${userId}`,ctx.now,3,3600000);
 const one=(sql)=>ctx.db.get(sql,[userId]),many=(sql)=>ctx.db.all(sql,[userId]);
 const account=await one('SELECT id,phone,email,status,created_at FROM users WHERE id=?');
 const output={exportedAt:ctx.now,account,
  profile:await one('SELECT payload,version,updated_at FROM profiles WHERE user_id=?'),
  preferences:await one('SELECT privacy,notifications FROM preferences WHERE user_id=?'),
  discovery:await one('SELECT payload FROM discovery_preferences WHERE user_id=?'),
  wallet:await many('SELECT kind,amount,locked_delta,description,created_at FROM wallet_entries WHERE user_id=? ORDER BY created_at'),
  hostedMeetingLocations:await many('SELECT l.* FROM event_locations l JOIN events e ON e.id=l.event_id WHERE e.host_id=?'),
  memberships:await many('SELECT event_id,status,side,checked_in_at,joined_at FROM event_members WHERE user_id=?'),
  teamMemberships:await many('SELECT team_id,role,status FROM team_members WHERE user_id=?'),
  communityMemberships:await many('SELECT community_id,status,created_at FROM community_members WHERE user_id=?'),
  sentMessages:await many('SELECT id,thread_id,body,upload_id,created_at FROM messages WHERE sender_id=? ORDER BY created_at'),
  memories:await many('SELECT id,upload_id,status,payload,created_at FROM memories WHERE author_id=?'),
  feedbackGiven:await many('SELECT event_id,to_id,fair_play,play_again,created_at FROM feedback WHERE from_id=?'),
  support:await many('SELECT subject,message,category,status,created_at FROM support_requests WHERE user_id=?'),
  venueApplications:await many('SELECT id,payload,status,response,created_at,updated_at FROM venue_applications WHERE user_id=?'),
  bookings:await many('SELECT id,slot_id,status,amount_minor,cancellation_deadline,created_at FROM bookings WHERE user_id=?'),
  legalAcceptances:await many('SELECT terms_version,privacy_version,created_at FROM legal_acceptances WHERE user_id=?')};
 await audit(ctx,'account.export',userId);
 return output;
}
export const accountHandlers={
 'account.session.revoke':async(ctx,p)=>{keys(p,['sessionId']);const session=await ctx.db.get('SELECT id,user_id FROM sessions WHERE id=?',[text(p.sessionId,'Oturum',1,100)]);ensure(session?.user_id===ctx.actor.id,404,'NOT_FOUND','Oturum bulunamadı.');await ctx.db.run('UPDATE sessions SET revoked=1 WHERE id=?',[session.id]);await audit(ctx,'account.session.revoke',session.id);return{revoked:true};}
};
