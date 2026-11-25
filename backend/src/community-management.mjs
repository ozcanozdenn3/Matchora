import { id } from './auth.mjs';
import { ensure, keys, text, choice, json, integer } from './errors.mjs';
import { row, encode, audit, release, cancelEvent, offerWaitlist, notify } from './model.mjs';
import { SPORTS } from './sports.mjs';
import { canonicalTurkishCity } from './turkish-locale.mjs';

export async function closeCommunityParticipation(ctx, community, userId) {
    const events = await ctx.db.all("SELECT * FROM events WHERE starts_at>? AND status IN ('PUBLISHED','FILLING','FULL')", [ctx.now]);
    for (const event of events) {
      const data=json(event.payload);if (data.communityId !== community.id || !['CAMPUS','COMPANY'].includes(data.networkScope)) continue;
      if (event.host_id === userId) { await cancelEvent(ctx, event); continue; }
      const joined = await ctx.db.get("SELECT user_id FROM event_members WHERE event_id=? AND user_id=? AND status='joined'", [event.id, userId]);
      await ctx.db.run("UPDATE event_members SET status='left' WHERE event_id=? AND user_id=?", [event.id, userId]);
      if (joined) await ctx.db.run("UPDATE events SET status='FILLING',version=version+1 WHERE id=?", [event.id]);
      await ctx.db.run("UPDATE join_requests SET status='cancelled' WHERE scope='event' AND scope_id=? AND user_id=? AND status='pending'", [event.id, userId]);
      await ctx.db.run("UPDATE waitlist SET status='cancelled' WHERE event_id=? AND user_id=?", [event.id, userId]);
      await release(ctx, userId, `event:${event.id}`);
      await offerWaitlist(ctx, event.id);
    }
    const thread = await ctx.db.get('SELECT id FROM threads WHERE pair_key=?', [`community:${community.id}`]);
    if (thread) await ctx.db.run('DELETE FROM thread_members WHERE thread_id=? AND user_id=?', [thread.id, userId]);
}

export const communityManagementHandlers = {
  'community.archive': async (ctx,p) => {
    keys(p,['communityId']);const community=await row(ctx,'communities','id',p.communityId),data=json(community.payload);
    ensure(community.owner_id===ctx.actor.id||ctx.actor.role==='admin',403,'FORBIDDEN','Topluluk yöneticisi olmalısın.');
    ensure(!data.archived,409,'COMMUNITY_ARCHIVED','Bu topluluk zaten kapatıldı.');
    const events=await ctx.db.all("SELECT * FROM events WHERE ends_at>? AND status NOT IN ('CANCELLED','VERIFIED','ARCHIVED')",[ctx.now]);
    const privateEvents=events.filter(e=>{const p=json(e.payload);return p.communityId===community.id&&['CAMPUS','COMPANY'].includes(p.networkScope);});
    ensure(!privateEvents.some(e=>Number(e.starts_at)<=ctx.now||e.status==='IN_PROGRESS'),409,'ACTIVE_EVENT','Devam eden özel maçlar sonuçlanınca topluluğu kapatabilirsin.');
    for(const event of privateEvents)await cancelEvent(ctx,event);
    const members=await ctx.db.all("SELECT user_id FROM community_members WHERE community_id=? AND status='joined'",[community.id]);
    await ctx.db.run('UPDATE communities SET payload=? WHERE id=?',[encode({...data,archived:true,version:(data.version??1)+1}),community.id]);
    await ctx.db.run("UPDATE community_members SET status='left' WHERE community_id=?",[community.id]);
    const communityThread=await ctx.db.get('SELECT id FROM threads WHERE pair_key=?',[`community:${community.id}`]);
    if(communityThread)await ctx.db.run('DELETE FROM thread_members WHERE thread_id=?',[communityThread.id]);
    for(const member of members)if(member.user_id!==ctx.actor.id)await notify(ctx,member.user_id,'social','Topluluk kapatıldı',data.name,'NETWORK',community.id,`community-archive:${community.id}:${member.user_id}`);
    await audit(ctx,'community.archive',community.id);return{archived:true};
  },
  'community.update': async (ctx, p) => {
    keys(p, ['communityId', 'expectedVersion', 'name', 'description', 'access', 'sport', 'sports', 'city', 'area']);
    const community = await row(ctx, 'communities', 'id', p.communityId), data = json(community.payload);
    ensure(!data.archived,409,'COMMUNITY_ARCHIVED','Bu topluluk kapatıldı.');
    ensure(community.owner_id === ctx.actor.id, 403, 'FORBIDDEN', 'Topluluk yöneticisi olmalısın.');
    ensure(integer(p.expectedVersion, 'Sürüm', 1) === (data.version ?? 1), 409, 'VERSION_CONFLICT', 'Topluluk değişti. Yenileyip tekrar dene.');
    const primarySport = p.sport ? choice(p.sport, SPORTS, 'Branş') : (Array.isArray(p.sports) && p.sports[0] ? choice(p.sports[0], SPORTS, 'Branş') : data.sport);
    const sportsList = Array.isArray(p.sports) && p.sports.length ? p.sports.map(s => choice(s, SPORTS, 'Branş')) : (data.sports ?? [primarySport]);
    ensure(sportsList.length <= 20);
    const next = { ...data, ...(p.city !== undefined ? { city: canonicalTurkishCity(text(p.city, 'İl', 2, 60)) } : {}), ...(p.area !== undefined ? { area: text(p.area, 'Bölge', 2, 60) } : {}), name: text(p.name, 'Topluluk adı', 3, 80), description: text(p.description ?? '', 'Açıklama', 0, 1000), sport: primarySport, sports: [...new Set([primarySport, ...sportsList])], version: (data.version ?? 1) + 1 };
    const access = choice(p.access, ['OPEN', 'APPROVAL', 'INVITE'], 'Katılım');
    await ctx.db.run('UPDATE communities SET access=?,payload=? WHERE id=?', [access, encode(next), community.id]);
    await ctx.db.run('UPDATE threads SET title=? WHERE pair_key=?', [next.name, `community:${community.id}`]);
    await audit(ctx, 'community.update', community.id); return { updated: true };
  },
  'community.chat': async (ctx, p) => {
    keys(p, ['communityId']);
    const community = await row(ctx, 'communities', 'id', p.communityId), data = json(community.payload);
    ensure(!data.archived, 409, 'COMMUNITY_ARCHIVED', 'Bu topluluk kapatıldı.');
    ensure(await ctx.db.get("SELECT user_id FROM community_members WHERE community_id=? AND user_id=? AND status='joined'", [community.id, ctx.actor.id]), 403, 'MEMBERSHIP_REQUIRED', 'Topluluk sohbetine katılmak için onaylı üye olmalısın.');
    const pairKey = `community:${community.id}`;
    let thread = await ctx.db.get('SELECT id FROM threads WHERE pair_key=?', [pairKey]);
    if (!thread) {
      thread = { id: id() };
      await ctx.db.run('INSERT INTO threads(id,kind,title,pair_key,created_at) VALUES(?,?,?,?,?)', [thread.id, 'SQUAD', data.name, pairKey, ctx.now]);
    }
    const members = await ctx.db.all("SELECT user_id FROM community_members WHERE community_id=? AND status='joined'", [community.id]);
    for (const m of members) await ctx.db.run('INSERT INTO thread_members(thread_id,user_id) VALUES(?,?) ON CONFLICT(thread_id,user_id) DO NOTHING', [thread.id, m.user_id]);
    return { id: thread.id };
  },
  'community.member.remove': async (ctx, p) => {
    keys(p, ['communityId', 'userId']);
    const community = await row(ctx, 'communities', 'id', p.communityId);
    ensure(community.owner_id === ctx.actor.id && p.userId !== ctx.actor.id, 403, 'FORBIDDEN', 'Yalnız diğer üyeleri çıkarabilirsin.');
    ensure(await ctx.db.get("SELECT user_id FROM community_members WHERE community_id=? AND user_id=? AND status IN ('joined','invited','pending')", [community.id, p.userId]), 404, 'NOT_FOUND', 'Üye bulunamadı.');
    await ctx.db.run("UPDATE community_members SET status='removed' WHERE community_id=? AND user_id=?", [community.id, p.userId]);
    await closeCommunityParticipation(ctx, community, p.userId);
    await notify(ctx,p.userId,'social','Topluluk üyeliğin sonlandırıldı',json(community.payload).name,'NETWORK',community.id,`community-removed:${community.id}:${p.userId}:${ctx.requestKey}`);
    await audit(ctx, 'community.member.remove', community.id, { userId: p.userId }); return { removed: true };
  }
};
