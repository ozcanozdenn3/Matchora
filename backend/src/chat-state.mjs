import { ensure } from './errors.mjs';
import { assertThread } from './model.mjs';

// Sequence cursors avoid losing messages sent in the same millisecond as a read/clear.
export async function ensureChatMetadata(ctx, threadId) {
  const missing = await ctx.db.all('SELECT m.id FROM messages m LEFT JOIN message_metadata x ON x.message_id=m.id WHERE m.thread_id=? AND x.message_id IS NULL ORDER BY m.created_at,m.id', [threadId]);
  if (!missing.length) return;
  let sequence = Number((await ctx.db.get('SELECT COALESCE(MAX(sequence),0) AS n FROM message_metadata WHERE thread_id=?', [threadId])).n);
  for (const message of missing) await ctx.db.run('INSERT INTO message_metadata(message_id,thread_id,sequence) VALUES(?,?,?) ON CONFLICT(message_id) DO NOTHING', [message.id, threadId, ++sequence]);
}

export async function chatMemberState(ctx, threadId, userId=ctx.actor.id) {
  await ensureChatMetadata(ctx, threadId);
  let state = await ctx.db.get('SELECT * FROM thread_member_state WHERE thread_id=? AND user_id=?', [threadId,userId]);
  if (!state) {
    const member = await ctx.db.get('SELECT read_at FROM thread_members WHERE thread_id=? AND user_id=?', [threadId,userId]);
    const read = Number((await ctx.db.get('SELECT COALESCE(MAX(x.sequence),0) AS n FROM message_metadata x JOIN messages m ON m.id=x.message_id WHERE x.thread_id=? AND m.created_at<=?', [threadId,Number(member?.read_at??0)])).n);
    await ctx.db.run('INSERT INTO thread_member_state(thread_id,user_id,read_sequence,updated_at) VALUES(?,?,?,?) ON CONFLICT(thread_id,user_id) DO NOTHING',[threadId,userId,read,0]);
    state = await ctx.db.get('SELECT * FROM thread_member_state WHERE thread_id=? AND user_id=?',[threadId,userId]);
  }
  return state;
}

export async function bumpChatRevision(ctx, threadId) {
  await ctx.db.run('INSERT INTO thread_revisions(thread_id,revision) VALUES(?,1) ON CONFLICT(thread_id) DO UPDATE SET revision=thread_revisions.revision+1',[threadId]);
}

export async function visibleMessage(ctx, messageId) {
  const message = await ctx.db.get('SELECT * FROM messages WHERE id=?',[messageId]);
  ensure(message,404,'NOT_FOUND','Mesaj bulunamadı.');
  await assertThread(ctx,message.thread_id);
  const state=await chatMemberState(ctx,message.thread_id), metadata=await ctx.db.get('SELECT * FROM message_metadata WHERE message_id=?',[message.id]);
  ensure(Number(metadata.sequence)>Number(state.clear_sequence)&&!await ctx.db.get('SELECT message_id FROM message_hides WHERE message_id=? AND user_id=?',[message.id,ctx.actor.id]),404,'NOT_FOUND','Mesaj bulunamadı.');
  return {...message,...metadata,id:message.id};
}

export async function chatMessageRows(ctx,threadId,{before=null,limit=100}={}) {
  const state=await chatMemberState(ctx,threadId);
  let sql='SELECT m.*,x.sequence,x.version,x.edited_at,x.deleted_at,x.reply_to FROM messages m JOIN message_metadata x ON x.message_id=m.id WHERE m.thread_id=? AND x.sequence>? AND NOT EXISTS(SELECT 1 FROM message_hides h WHERE h.message_id=m.id AND h.user_id=?)',params=[threadId,Number(state.clear_sequence),ctx.actor.id];
  if(before?.length===3){sql+=' AND x.sequence<?';params.push(before[2]);}else if(before){sql+=' AND (m.created_at<? OR (m.created_at=? AND m.id<?))';params.push(before[0],before[0],before[1]);}
  sql+=' ORDER BY x.sequence DESC LIMIT ?';params.push(limit);
  return ctx.db.all(sql,params);
}

export async function chatUnread(ctx,threadId) {
  const state=await chatMemberState(ctx,threadId);
  return Number((await ctx.db.get('SELECT COUNT(*) AS n FROM messages m JOIN message_metadata x ON x.message_id=m.id WHERE m.thread_id=? AND x.sequence>? AND x.deleted_at IS NULL AND m.sender_id<>? AND NOT EXISTS(SELECT 1 FROM message_hides h WHERE h.message_id=m.id AND h.user_id=?)',[threadId,Math.max(Number(state.clear_sequence),Number(state.read_sequence)),ctx.actor.id,ctx.actor.id])).n);
}

export async function markChatRead(ctx,threadId) {
  await assertThread(ctx,threadId);await chatMemberState(ctx,threadId);
  const sequence=Number((await ctx.db.get('SELECT COALESCE(MAX(sequence),0) AS n FROM message_metadata WHERE thread_id=?',[threadId])).n);
  await ctx.db.run('UPDATE thread_member_state SET read_sequence=?,updated_at=? WHERE thread_id=? AND user_id=?',[sequence,ctx.now,threadId,ctx.actor.id]);
  await ctx.db.run('UPDATE thread_members SET read_at=? WHERE thread_id=? AND user_id=?',[ctx.now,threadId,ctx.actor.id]);
  return {read:true};
}

export async function messageContent(ctx,message) {
  const metadata=message.sequence!=null?message:await ctx.db.get('SELECT * FROM message_metadata WHERE message_id=?',[message.id]);
  let reply=null;
  if(metadata?.reply_to){
    try {const target=await visibleMessage(ctx,metadata.reply_to);reply={id:target.id,senderId:target.sender_id,text:target.deleted_at?'Silinen mesaj':target.body.slice(0,160),deleted:!!target.deleted_at};}catch{reply={id:metadata.reply_to,text:'Önceki mesaj',unavailable:true};}
  }
  ctx.chatReaders??=new Map();if(!ctx.chatReaders.has(message.thread_id))ctx.chatReaders.set(message.thread_id,await ctx.db.all('SELECT s.user_id,s.read_sequence FROM thread_member_state s JOIN thread_members m ON m.thread_id=s.thread_id AND m.user_id=s.user_id WHERE s.thread_id=?',[message.thread_id]));
  const readBy=ctx.chatReaders.get(message.thread_id).filter(r=>r.user_id!==message.sender_id&&Number(r.read_sequence)>=Number(metadata?.sequence??0)).length;
  const uploadId=metadata?.deleted_at?null:message.upload_id;
  let memoryMeta=null;
  if(uploadId){
    const mem=await ctx.db.get("SELECT id,status,payload FROM memories WHERE upload_id=? AND status<>'deleted' ORDER BY created_at DESC LIMIT 1",[uploadId]);
    if(mem){try{const mp=JSON.parse(mem.payload);memoryMeta={id:mem.id,sport:mp.sport??'FOOTBALL',venue:mp.venue??'',area:mp.area??'',city:mp.city??'',matchScoreTag:mp.matchScoreTag??null,sceneTheme:mp.sceneTheme??'NIGHT_TURF',isShowcase:true};}catch{}}
  }
  return {readBy,text:metadata?.deleted_at?'Bu mesaj silindi.':message.body,uploadId,memoryMeta,
    sequence:Number(metadata?.sequence??0),version:Number(metadata?.version??1),edited:!!metadata?.edited_at,deleted:!!metadata?.deleted_at,reply};
}
