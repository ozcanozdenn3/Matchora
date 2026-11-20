import { ensure, keys } from './errors.mjs';
import { assertThread } from './model.mjs';

const UUID = /^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$/;

/** Authenticated SSE invalidations. Message content always comes from authorized GETs. */
export class RealtimeHub {
  constructor(service, auth, { heartbeatMs = 15000, typingTtlMs = 5000, maxPerUser = 5 } = {}) {
    this.service = service;
    this.auth = auth;
    this.heartbeatMs = heartbeatMs;
    this.typingTtlMs = typingTtlMs;
    this.maxPerUser = maxPerUser;
    this.connections = new Map();
    this.lastTyping = new Map();
  }

  async connect(req, res, actor, token) {
    ensure(actor?.id && token, 401, 'UNAUTHENTICATED', 'Giriş yapmalısın.');
    const current = await this.auth.actor(token);
    ensure(current.id === actor.id, 401, 'UNAUTHENTICATED', 'Oturum geçersiz.');
    const clients = this.connections.get(actor.id) ?? new Set();
    ensure(clients.size < this.maxPerUser, 429, 'REALTIME_LIMIT', 'Çok fazla anlık bağlantı var. Tekrar dene.');
    ensure(this.clientCount < 2000, 503, 'REALTIME_BUSY', 'Anlık bağlantı şu anda dolu.');
    res.writeHead(200, {
      'Content-Type': 'text/event-stream; charset=utf-8',
      'Cache-Control': 'no-cache, no-store, no-transform',
      Connection: 'keep-alive',
      'X-Accel-Buffering': 'no'
    });
    res.flushHeaders?.();
    const client = { res, token, actor: current, closed: false, checking: false, timer: null };
    clients.add(client);
    this.connections.set(actor.id, clients);
    const close = () => {
      if (client.closed) return;
      client.closed = true;
      clearInterval(client.timer);
      clients.delete(client);
      if (!clients.size) this.connections.delete(actor.id);
    };
    res.once('close', close);
    req.once('aborted', close);
    client.timer = setInterval(async () => {
      if (client.closed || client.checking) return;
      client.checking = true;
      try {
        await this.auth.actor(token);
        if (!client.closed) res.write(': heartbeat\n\n');
      } catch {
        if (!client.closed) {
          this.send(client, { type: 'sessionExpired' });
          res.end();
          close();
        }
      } finally {
        client.checking = false;
        this.pruneTyping();
      }
    }, this.heartbeatMs);
    client.timer.unref?.();
    // Reconcile after every reconnect, including changes missed while the app slept.
    this.send(client, { type: 'invalidate' });
  }

  get clientCount() {
    let count = 0;
    for (const clients of this.connections.values()) count += clients.size;
    return count;
  }

  send(client, event) {
    if (client.closed || client.res.destroyed || client.res.writableEnded) return;
    if (client.res.writableLength > 64 * 1024) {
      client.res.destroy();
      return;
    }
    client.res.write(`data: ${JSON.stringify(event)}\n\n`);
  }

  invalidate(userIds, sourceSessionId = null, threadId = null) {
    for (const userId of new Set(userIds.filter(Boolean))) {
      for (const client of this.connections.get(userId) ?? []) {
        // The originating session already received the committed command response.
        // Other devices still reconcile, without doubling every local API operation.
        if (sourceSessionId && client.actor.sessionId === sourceSessionId) continue;
        this.send(client, { type: 'invalidate', ...(threadId ? {threadId} : {}) });
      }
    }
  }

  async authorizedMembers(threadId) {
    return this.service.store.read(async db => {
      const candidates = await db.all(`SELECT m.user_id,u.role FROM thread_members m
        JOIN users u ON u.id=m.user_id WHERE m.thread_id=? AND u.status='active'`, [threadId]);
      const members = [];
      for (const user of candidates) {
        try {
          await assertThread(this.service.ctx(db, { id: user.user_id, role: user.role }), threadId);
          const state = await db.get('SELECT hidden FROM thread_member_state WHERE thread_id=? AND user_id=?', [threadId, user.user_id]);
          if (!state?.hidden) members.push(user.user_id);
        } catch { /* Removed, blocked or archived conversations have no live audience. */ }
      }
      return members;
    });
  }

  /** Call only after the domain command transaction committed successfully. */
  async command(actor, operation, payload, result) {
    const recipients = new Set([actor.id]);
    let threadId = payload.threadId ?? payload.shareThreadId;
    if (operation === 'chat.direct') threadId = result.id;
    if (operation.startsWith('message.') && payload.messageId) {
      threadId = (await this.service.store.get('SELECT thread_id FROM messages WHERE id=?', [payload.messageId]))?.thread_id;
    }
    if (payload.eventId) {
      threadId ??= (await this.service.store.get('SELECT id FROM threads WHERE event_id=?', [payload.eventId]))?.id;
    }
    if (threadId) for (const userId of await this.authorizedMembers(threadId)) recipients.add(userId);
    if (operation === 'user.block') recipients.add(payload.userId);
    if (operation === 'event.member.remove' || operation === 'team.member.remove') recipients.add(payload.userId);
    this.invalidate([...recipients], actor.sessionId, threadId);
  }

  async typing(actor, threadId, payload) {
    ensure(actor?.id, 401, 'UNAUTHENTICATED', 'Giriş yapmalısın.');
    ensure(UUID.test(threadId), 400, 'INVALID_THREAD', 'Sohbet kimliği geçersiz.');
    keys(payload, ['typing']);
    ensure(typeof payload.typing === 'boolean', 400, 'INVALID_INPUT', 'Yazma durumu geçersiz.');
    await this.service.store.read(db => assertThread(this.service.ctx(db, actor), threadId, true));
    const now = this.service.clock(), key = `${actor.id}:${threadId}`;
    const previous = this.lastTyping.get(key);
    if (payload.typing && previous != null && now - previous < 800) return { sent: false };
    this.lastTyping.set(key, now);
    this.pruneTyping();
    const event = { type: 'typing', threadId, userId: actor.id, typing: payload.typing, expiresAt: now + this.typingTtlMs };
    for (const userId of await this.authorizedMembers(threadId)) {
      if (userId === actor.id) continue;
      for (const client of this.connections.get(userId) ?? []) this.send(client, event);
    }
    return { sent: true };
  }

  pruneTyping() {
    const threshold = this.service.clock() - this.typingTtlMs;
    for (const [key, at] of this.lastTyping) if (at < threshold) this.lastTyping.delete(key);
  }

  close() {
    for (const clients of this.connections.values()) {
      for (const client of clients) {
        clearInterval(client.timer);
        client.closed = true;
        client.res.end();
      }
    }
    this.connections.clear();
    this.lastTyping.clear();
  }
}
