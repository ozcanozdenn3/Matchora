import { id } from './auth.mjs';
import { ensure,keys,text } from './errors.mjs';
import { assertThread,row,notify,audit,profile } from './model.mjs';
import { ensureChatMetadata,chatMemberState,visibleMessage,bumpChatRevision,markChatRead } from './chat-state.mjs';

export async function sendChatMessage(ctx,p) {
  keys(p,['threadId','text','uploadId','forwardId','replyTo']);
  const thread=await assertThread(ctx,p.threadId,true),body=text(p.text??'','Mesaj',0,4000);
  ensure(body||p.uploadId,400,'EMPTY_MESSAGE','Bir mesaj veya fotoğraf ekle.');
  if(p.replyTo){const reply=await visibleMessage(ctx,p.replyTo);ensure(reply.thread_id===thread.id&&!reply.deleted_at,400,'INVALID_REPLY','Bu mesaja yanıt verilemiyor.');}
  if(p.uploadId){const upload=await row(ctx,'uploads','id',p.uploadId);if(upload.status!=='ready'){ensure(upload.owner_id===ctx.actor.id,403,'FORBIDDEN','Fotoğraf kullanılamıyor.');ensure(false,409,'UPLOAD_EXPIRED','Fotoğrafın yükleme süresi doldu. Yeniden yüklenmeli.');}
    if(upload.owner_id!==ctx.actor.id){
      const memory=p.forwardId?await ctx.db.get("SELECT * FROM memories WHERE id=? AND upload_id=? AND status='approved'",[p.forwardId,p.uploadId]):null;
      if(memory)ensure(!await ctx.db.get('SELECT user_id FROM blocks WHERE (user_id=? AND target_id=?) OR (user_id=? AND target_id=?)',[ctx.actor.id,memory.author_id,memory.author_id,ctx.actor.id]),403,'BLOCKED','Fotoğraf iletilemiyor.');
      else {const source=p.forwardId?await visibleMessage(ctx,p.forwardId):null;ensure(source&&!source.deleted_at&&source.upload_id===p.uploadId,403,'FORBIDDEN','Fotoğraf kullanılamıyor.');}
    }
  }
  await ensureChatMetadata(ctx,thread.id);
  const sequence=Number((await ctx.db.get('SELECT COALESCE(MAX(sequence),0) AS n FROM message_metadata WHERE thread_id=?',[thread.id])).n)+1,messageId=id();
  await ctx.db.run('INSERT INTO messages(id,thread_id,sender_id,body,upload_id,created_at) VALUES(?,?,?,?,?,?)',[messageId,thread.id,ctx.actor.id,body,p.uploadId??null,ctx.now]);
  await ctx.db.run('INSERT INTO message_metadata(message_id,thread_id,sequence,reply_to) VALUES(?,?,?,?)',[messageId,thread.id,sequence,p.replyTo??null]);
  await ctx.db.run('UPDATE thread_member_state SET hidden=0,updated_at=? WHERE thread_id=?',[ctx.now,thread.id]);
  await markChatRead(ctx,thread.id);
  const sender=await profile(ctx);
  const senderName=sender?.name||'Bir sporcu';
  const snippet=body?(body.length>70?body.slice(0,67)+'...':body):'📷 Fotoğraf paylaştı';
  const members=await ctx.db.all("SELECT m.user_id,m.muted FROM thread_members m JOIN users u ON u.id=m.user_id WHERE m.thread_id=? AND m.user_id<>? AND u.status='active'",[thread.id,ctx.actor.id]);
  for(const member of members)if(!member.muted){try{await assertThread({...ctx,actor:{id:member.user_id,role:'user'}},thread.id);}catch{continue;}
    await notify(ctx,member.user_id,'social',senderName,snippet,'CHAT',thread.id,`message:${messageId}:${member.user_id}`);
  }
  return {id:messageId};
}

async function personalThread(ctx,p,hide) {
  keys(p,['threadId','blockUser']);const thread=await row(ctx,'threads','id',p.threadId);
  ensure(await ctx.db.get('SELECT user_id FROM thread_members WHERE thread_id=? AND user_id=?',[thread.id,ctx.actor.id]),403,'FORBIDDEN','Bu sohbet sana ait değil.');
  ensure(p.blockUser==null||typeof p.blockUser==='boolean');
  if(p.blockUser){ensure(thread.kind==='DIRECT',400,'DIRECT_REQUIRED','Engelleme birebir sohbetlerde kullanılabilir.');
    const other=await ctx.db.get('SELECT user_id FROM thread_members WHERE thread_id=? AND user_id<>?',[thread.id,ctx.actor.id]);
    if(other)await ctx.db.run('INSERT INTO blocks(user_id,target_id,created_at) VALUES(?,?,?) ON CONFLICT(user_id,target_id) DO NOTHING',[ctx.actor.id,other.user_id,ctx.now]);
  }
  await chatMemberState(ctx,thread.id);
  const sequence=Number((await ctx.db.get('SELECT COALESCE(MAX(sequence),0) AS n FROM message_metadata WHERE thread_id=?',[thread.id])).n);
  await ctx.db.run('UPDATE thread_member_state SET clear_sequence=?,hidden=?,version=version+1,updated_at=? WHERE thread_id=? AND user_id=?',[sequence,hide?1:0,ctx.now,thread.id,ctx.actor.id]);
  await audit(ctx,hide?'chat.delete':'chat.clear',thread.id,{blockUser:!!p.blockUser});
  return {cleared:true,hidden:hide};
}

export const chatManagementHandlers={
 'chat.clear':async(ctx,p)=>personalThread(ctx,p,false),
 'chat.delete':async(ctx,p)=>personalThread(ctx,p,true),
 'message.hide':async(ctx,p)=>{keys(p,['messageId']);const message=await visibleMessage(ctx,p.messageId);
  await ctx.db.run('INSERT INTO message_hides(message_id,user_id,created_at) VALUES(?,?,?) ON CONFLICT(message_id,user_id) DO NOTHING',[message.id,ctx.actor.id,ctx.now]);
  await chatMemberState(ctx,message.thread_id);await ctx.db.run('UPDATE thread_member_state SET version=version+1 WHERE thread_id=? AND user_id=?',[message.thread_id,ctx.actor.id]);
  return {hidden:true};},
 'message.edit':async(ctx,p)=>{keys(p,['messageId','expectedVersion','text']);const message=await visibleMessage(ctx,p.messageId);
  ensure(message.sender_id===ctx.actor.id&&!message.deleted_at,403,'FORBIDDEN','Yalnız kendi aktif mesajını düzenleyebilirsin.');
  ensure(p.expectedVersion===message.version,409,'VERSION_CONFLICT','Mesaj değişti. Yenileyip tekrar dene.');
  ensure(ctx.now-Number(message.created_at)<=15*60000,409,'EDIT_WINDOW_CLOSED','Düzenleme süresi 15 dakikadır.');
  await ctx.db.run('UPDATE messages SET body=? WHERE id=?',[text(p.text,'Mesaj',1,4000),message.id]);
  await ctx.db.run('UPDATE message_metadata SET edited_at=?,version=version+1 WHERE message_id=?',[ctx.now,message.id]);await bumpChatRevision(ctx,message.thread_id);
  return {edited:true};},
 'message.delete':async(ctx,p)=>{keys(p,['messageId','expectedVersion']);const raw=await row(ctx,'messages','id',p.messageId);await ensureChatMetadata(ctx,raw.thread_id);const metadata=await row(ctx,'message_metadata','message_id',raw.id);const message={...raw,...metadata,id:raw.id};
  ensure(message.sender_id===ctx.actor.id||ctx.actor.role==='admin',403,'FORBIDDEN','Yalnız kendi mesajını herkesten silebilirsin.');
  ensure(p.expectedVersion===message.version,409,'VERSION_CONFLICT','Mesaj değişti. Yenileyip tekrar dene.');
  await ctx.db.run("UPDATE messages SET body='',upload_id=NULL WHERE id=?",[message.id]);
  await ctx.db.run('UPDATE message_metadata SET deleted_at=?,version=version+1 WHERE message_id=?',[ctx.now,message.id]);await bumpChatRevision(ctx,message.thread_id);
  await audit(ctx,'message.delete',message.id);return {deleted:true};}
};
