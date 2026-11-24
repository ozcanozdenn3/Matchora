import { sign,secureEqual } from './auth.mjs';
import { ensure,keys,text } from './errors.mjs';
import { profile } from './model.mjs';
import { publicPlayer } from './queries.mjs';
export const passportHandlers={
 'passport.code':async(ctx,p)=>{keys(p,[]);await profile(ctx);const expiresAt=ctx.now+15*60000,value=`${ctx.actor.id}.${expiresAt}`;return{code:`${value}.${sign(ctx.config.secret,`passport:${value}`)}`,expiresAt};}
};
export async function verifyPassport(ctx,value){
 const parts=text(value,'Sporcu kartı kodu',20,250).split('.'),expiresAt=Number(parts[1]);
 ensure(parts.length===3&&/^[a-f0-9-]{36}$/.test(parts[0])&&Number.isSafeInteger(expiresAt)&&expiresAt>ctx.now&&expiresAt<=ctx.now+15*60000&&secureEqual(parts[2],sign(ctx.config.secret,`passport:${parts[0]}.${parts[1]}`)),403,'PASSPORT_CODE_INVALID','Sporcu kartı geçersiz veya süresi dolmuş.');
 const user=await ctx.db.get("SELECT id FROM users WHERE id=? AND status='active'",[parts[0]]);ensure(user,404,'NOT_FOUND','Sporcu bulunamadı.');
 const player=await publicPlayer(ctx,user.id);return{userId:user.id,name:player.name,city:player.city,area:player.area,reliability:player.reliability,verifiedMatches:player.verifiedMatches,verifications:player.verifications,expiresAt};
}
