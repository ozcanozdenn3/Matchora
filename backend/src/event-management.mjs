import { ensure, keys, json } from './errors.mjs';
import { ownedEvent, member, row, assertMutableEventRoster, ACTIVE_EVENTS, release, offerWaitlist, notify, audit } from './model.mjs';

async function manageable(ctx, p) {
  const event = await ownedEvent(ctx, p.eventId);
  assertMutableEventRoster(event);
  ensure(p.expectedVersion === event.version, 409, 'VERSION_CONFLICT', 'Kadro değişti. Yenileyip tekrar dene.');
  ensure(ACTIVE_EVENTS.includes(event.status) && Number(event.starts_at) > ctx.now, 409, 'EVENT_CLOSED', 'Kadro yalnız maç başlamadan değiştirilebilir.');
  ensure(p.userId !== event.host_id, 409, 'HOST_CANNOT_REMOVE', 'Organizatör kendisini çıkaramaz; maçı iptal edebilir.');
  return event;
}

export const eventManagementHandlers = {
  'event.member.remove': async (ctx, p) => {
    keys(p, ['eventId', 'userId', 'expectedVersion']);
    const event = await manageable(ctx, p);
    await member(ctx, event.id, p.userId);
    await ctx.db.run("UPDATE event_members SET status='removed' WHERE event_id=? AND user_id=?", [event.id, p.userId]);
    await ctx.db.run("UPDATE join_requests SET status='cancelled' WHERE scope='event' AND scope_id=? AND user_id=? AND status='pending'", [event.id, p.userId]);
    await ctx.db.run("UPDATE waitlist SET status='cancelled',offered_until=NULL WHERE event_id=? AND user_id=?", [event.id, p.userId]);
    await ctx.db.run('DELETE FROM thread_members WHERE user_id=? AND thread_id IN (SELECT id FROM threads WHERE event_id=?)', [p.userId, event.id]);
    // Organizer removal is never treated as the athlete abandoning the match.
    await release(ctx, p.userId, `event:${event.id}`);
    await ctx.db.run("UPDATE events SET status='FILLING',version=version+1 WHERE id=?", [event.id]);
    await offerWaitlist(ctx, event.id);
    await notify(ctx, p.userId, 'matches', 'Maç kadron güncellendi', `${json(event.payload).title} · Organizatör katılımını sonlandırdı. Varsa taahhüdün iade edildi.`, 'EVENT', event.id, `event-remove:${event.id}:${p.userId}:${ctx.requestKey}`);
    await audit(ctx, 'event.member.remove', event.id, { userId: p.userId });
    return { removed: true };
  },
  'event.member.restore': async (ctx, p) => {
    keys(p, ['eventId', 'userId', 'expectedVersion']);
    const event = await manageable(ctx, p), user = await row(ctx, 'users', 'id', p.userId);
    ensure(user.status === 'active', 409, 'ACCOUNT_DISABLED', 'Bu hesap yeniden katılamıyor.');
    const old = await ctx.db.get('SELECT status FROM event_members WHERE event_id=? AND user_id=?', [event.id, p.userId]);
    ensure(old?.status === 'removed', 409, 'INVALID_STATE', 'Bu sporcu çıkarılmış katılımcılar arasında değil.');
    await ctx.db.run("UPDATE event_members SET status='left',side=NULL,checked_in_at=NULL WHERE event_id=? AND user_id=?", [event.id, p.userId]);
    await ctx.db.run('UPDATE events SET version=version+1 WHERE id=?', [event.id]);
    await notify(ctx, p.userId, 'matches', 'Yeniden katılabilirsin', `${json(event.payload).title} · Organizatör yeniden başvurmana izin verdi. Katılmak senin seçimin.`, 'EVENT', event.id, `event-restore:${event.id}:${p.userId}:${ctx.requestKey}`);
    await audit(ctx, 'event.member.restore', event.id, { userId: p.userId });
    return { restored: true };
  }
};
