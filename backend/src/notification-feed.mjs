const notificationDateFormat = new Intl.DateTimeFormat('tr-TR',{timeZone:'Europe/Istanbul',day:'numeric',month:'short'});
import { ensure,choice,json } from './errors.mjs';
import { sealCursor,openCursor } from './opaque-cursor.mjs';
const filters={all:null,matches:['matches','emergencyFill','weeklyDigest','rematch'],squads:['teams','challenges','waitlist','social'],wallet:['wallet']};
export async function notificationPage(ctx,p={}){
 ensure(ctx.actor,401,'UNAUTHENTICATED','Bildirimler için giriş yap.');
 const filter=choice(p.filter??'all',Object.keys(filters),'Bildirim filtresi'),cursor=p.after?openCursor(ctx,'notifications',p.after):null;
 if(cursor)ensure(cursor.filter===filter&&Number.isSafeInteger(cursor.createdAt)&&typeof cursor.id==='string',400,'INVALID_CURSOR','Bildirim filtresi değişti. Baştan yenile.');
 const visible="n.user_id=? AND NOT EXISTS(SELECT 1 FROM notification_hides h WHERE h.notification_id=n.id AND h.user_id=n.user_id)",params=[ctx.actor.id];
 let where=visible;
 if(filters[filter]){where+=` AND n.category IN (${filters[filter].map(()=>'?').join(',')})`;params.push(...filters[filter]);}
 if(cursor){where+=' AND (n.created_at<? OR (n.created_at=? AND n.id<?))';params.push(cursor.createdAt,cursor.createdAt,cursor.id);}
 const size=Math.min(100,Math.max(1,Math.floor(Number(p.limit)||50))),rows=await ctx.db.all(`SELECT n.* FROM notifications n WHERE ${where} ORDER BY n.created_at DESC,n.id DESC LIMIT ?`,[...params,size+1]),selected=rows.slice(0,size),last=selected.at(-1);
 const unread=await ctx.db.get(`SELECT COUNT(*) AS n FROM notifications n WHERE ${visible} AND n.read_at IS NULL`,[ctx.actor.id]);
 return{items:selected.map(n=>({...json(n.payload),id:n.id,category:n.category,read:n.read_at!=null,timeAgo:notificationDateFormat.format(Number(n.created_at)),createdAt:Number(n.created_at)})),unreadCount:Number(unread.n),serverTime:ctx.now,nextCursor:rows.length>size?sealCursor(ctx,'notifications',{filter,createdAt:Number(last.created_at),id:last.id}):null};
}
