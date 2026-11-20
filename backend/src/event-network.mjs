import { ensure, json } from './errors.mjs';

/** A typed, active community membership is the authority for private rosters. */
export async function eventNetworkMembership(ctx, settings) {
  if (!ctx.actor || !settings.communityId) return null;
  ctx.communityMembershipCache ??= new Map();
  const key = `${ctx.actor.id}:${settings.communityId}`;
  if (!ctx.communityMembershipCache.has(key)) {
    const community = await ctx.db.get("SELECT c.* FROM communities c JOIN community_members m ON m.community_id=c.id WHERE c.id=? AND m.user_id=? AND m.status='joined'", [settings.communityId, ctx.actor.id]);
    ctx.communityMembershipCache.set(key, community ? json(community.payload) : null);
  }
  const data = ctx.communityMembershipCache.get(key);
  return data && !data.archived && data.type === settings.networkScope ? data : null;
}

export async function canonicalEventNetwork(ctx, settings) {
  if (['CAMPUS', 'COMPANY'].includes(settings.networkScope)) {
    const community = await eventNetworkMembership(ctx, settings);
    ensure(community, 403, 'COMMUNITY_REQUIRED', 'Seçtiğin türdeki aktif topluluğun onaylı üyesi olmalısın.');
    return { communityId: settings.communityId, networkName: community.name };
  }
  // Travel is published at the real destination; unrelated community IDs confer no access.
  return { communityId: null, networkName: settings.networkScope === 'TRAVEL' ? settings.city : null };
}
