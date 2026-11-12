import { activePoint,distanceKm } from './geo.mjs';
import { json } from './errors.mjs';
import { notify } from './model.mjs';
import { canSeeEvent } from './queries.mjs';
import { weekNumber } from './progression.mjs';

/** Suggestions only: never sends a DM or enrolls a player without their action. */
export async function scheduleEngagement(ctx) {
  const last = await ctx.db.get("SELECT ran_at FROM job_checkpoints WHERE name='engagement'");
  if (last && ctx.now - Number(last.ran_at) < 5 * 60000) return;
  const users = await ctx.db.all("SELECT p.user_id,p.payload,n.privacy,n.notifications,d.payload AS discovery FROM profiles p JOIN users u ON u.id=p.user_id JOIN preferences n ON n.user_id=p.user_id LEFT JOIN discovery_preferences d ON d.user_id=p.user_id WHERE u.status='active'");
  const urgent = await ctx.db.all("SELECT * FROM events WHERE status IN ('PUBLISHED','FILLING') AND starts_at>? AND starts_at<=?", [ctx.now, ctx.now + 3 * 3600000]);
  for (const event of urgent) {
    const details = json(event.payload);
    if (!details.emergency) continue;
    const occupied = await ctx.db.get("SELECT COUNT(*) AS n FROM event_members WHERE event_id=? AND status='joined'", [event.id]);
    if (occupied.n >= event.capacity) continue;
    for (const user of users) {
      const profile = json(user.payload), prefs = json(user.notifications);
      if (prefs.emergencyFill === false || user.user_id === event.host_id) continue;
      const discovery=user.discovery?json(user.discovery):{},point=activePoint(discovery,ctx.now);
      const hasCoordinates=point&&event.latitude!=null&&event.longitude!=null;
      if(hasCoordinates){if(distanceKm(point,{latitude:Number(event.latitude),longitude:Number(event.longitude)})>(prefs.emergencyRadiusKm??3))continue;}
      else if((discovery.city??profile.city)!==event.city||(discovery.district??profile.area)!==event.district)continue;
      if (!profile.skills.some(s => s.sport === event.sport) || profile.reliability < (details.minReliability ?? 0) || details.verifiedOnly && !profile.verifications.includes('BASIC')) continue;
      const actor = { ...ctx, actor: { id: user.user_id, role: 'user' } };
      if (!await canSeeEvent(actor, event)) continue;
      if (await ctx.db.get("SELECT user_id FROM event_members WHERE event_id=? AND user_id=? AND status='joined'", [event.id, user.user_id])) continue;
      if (await ctx.db.get('SELECT user_id FROM blocks WHERE (user_id=? AND target_id=?) OR (user_id=? AND target_id=?)', [user.user_id, event.host_id, event.host_id, user.user_id])) continue;
      await notify(ctx, user.user_id, 'emergencyFill', hasCoordinates?'Yakınında oyuncu aranıyor':'İlçende oyuncu aranıyor', `${details.title} · ${details.venue}`, 'EVENT', event.id,
        `emergency:${event.id}:${event.starts_at}:${user.user_id}`, { expiresAt: Number(event.starts_at) });
    }
  }
  const week = weekNumber(ctx.now), previous = week - 1;
  // Let the 24-hour dispute period of Sunday games close before publishing the summary.
  const digestReady = ctx.now >= week * 7 * 86400000 - 3 * 86400000 - 3 * 3600000 + 86400000;
  for (const user of users) {
    const prefs = json(user.notifications);
    if (digestReady && prefs.weeklyDigest !== false) {
      const matches = await ctx.db.all("SELECT e.ends_at FROM event_members m JOIN events e ON e.id=m.event_id JOIN results r ON r.event_id=e.id WHERE m.user_id=? AND m.checked_in_at IS NOT NULL AND m.status='joined' AND r.status='verified' AND e.ends_at>=?", [user.user_id, ctx.now - 15 * 86400000]);
      const count = matches.filter(m => weekNumber(m.ends_at) === previous).length;
      if (count) {
        const history = await ctx.db.all("SELECT h.delta,e.ends_at FROM rating_history h JOIN events e ON e.id=h.event_id WHERE h.user_id=? AND e.ends_at>=?", [user.user_id, ctx.now - 15 * 86400000]);
        const delta = history.filter(h => weekNumber(h.ends_at) === previous).reduce((n, h) => n + Number(h.delta), 0);
        await notify(ctx, user.user_id, 'weeklyDigest', 'Haftalık spor özetin', `Geçen hafta ${count} doğrulanmış katılım · ${delta > 0 ? '+' : ''}${delta} MMR`, 'PASSPORT', '', `weekly-digest:${week}:${user.user_id}`);
      }
    }
    if (prefs.rematch === false) continue;
    const peers = await ctx.db.all(`SELECT DISTINCT f.to_id FROM feedback f JOIN feedback b ON b.event_id=f.event_id AND b.from_id=f.to_id AND b.to_id=f.from_id
      JOIN results r ON r.event_id=f.event_id JOIN events e ON e.id=f.event_id
      WHERE f.from_id=? AND f.play_again=1 AND b.play_again=1 AND r.status='verified' AND e.ends_at<? AND e.ends_at>=?`, [user.user_id, ctx.now - 24 * 3600000, ctx.now - 14 * 86400000]);
    for (const peer of peers.slice(0, 3)) {
      const other = users.find(u => u.user_id === peer.to_id);
      if (!other || json(other.notifications).rematch === false) continue;
      if (await ctx.db.get('SELECT user_id FROM blocks WHERE (user_id=? AND target_id=?) OR (user_id=? AND target_id=?)', [user.user_id, peer.to_id, peer.to_id, user.user_id])) continue;
      await notify(ctx, user.user_id, 'rematch', 'Birlikte yeniden sahaya çıkın', `${json(other.payload).name} ile yeniden oynamayı ikiniz de istediniz. Uygun maçları keşfet.`, 'PLAY', '', `rematch:${week}:${user.user_id}:${peer.to_id}`);
    }
  }
  await ctx.db.run('INSERT INTO job_checkpoints(name,ran_at) VALUES(?,?) ON CONFLICT(name) DO UPDATE SET ran_at=?', ['engagement', ctx.now, ctx.now]);
}
