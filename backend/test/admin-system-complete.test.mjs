import test from 'node:test';
import assert from 'node:assert/strict';
import sharp from 'sharp';
import { fixture } from './helpers.mjs';
import { runJobs } from '../src/jobs.mjs';

const fails = code => error => error.code === code;

async function jpeg(color = 'orange') {
  return sharp({ create: { width: 16, height: 16, channels: 3, background: color } }).jpeg().toBuffer();
}

test('admin photo verification alerts, decision notifications, stale rejections and support reasons work end-to-end', async t => {
  const f = await fixture(t);
  const firstUpload = (await f.service.upload(f.host, await jpeg('red'), 'image/jpeg')).id;
  const hostProfile = (await f.service.bootstrap(f.host)).profile;
  await f.cmd(f.host, 'profile.save', { ...f.passport(hostProfile.name), avatarUploadId: firstUpload, expectedVersion: hostProfile.version });
  const req1 = (await f.cmd(f.host, 'verification.photo', { uploadId: firstUpload })).result.id;

  const adminBoot = await f.service.bootstrap(f.admin);
  assert.ok(adminBoot.notifications.some(n => n.destination === 'MODERATION' && n.targetId === req1));

  const secondUpload = (await f.service.upload(f.host, await jpeg('blue'), 'image/jpeg')).id;
  const v2 = (await f.service.bootstrap(f.host)).profile.version;
  await f.cmd(f.host, 'profile.save', { ...f.passport(hostProfile.name), avatarUploadId: secondUpload, expectedVersion: v2 });
  const req2 = (await f.cmd(f.host, 'verification.photo', { uploadId: secondUpload })).result.id;

  const inbox = await f.service.adminInbox(f.admin);
  assert.equal(inbox.photos.find(r => r.id === req1).stale, true);
  assert.equal(inbox.photos.find(r => r.id === req2).stale, false);
  assert.equal(inbox.photos.find(r => r.id === req2).requesterName, 'Ev sahibi');

  // Admin can reject an outdated stale request to clear the queue, and decision reason reaches user.
  await f.cmd(f.admin, 'admin.verification.photo', { userId: f.host.id, uploadId: firstUpload, requestId: req1, approved: false, reason: 'Eski profil fotoğrafı yenisiyle değiştirildi.' });
  await f.cmd(f.admin, 'admin.verification.photo', { userId: f.host.id, uploadId: secondUpload, requestId: req2, approved: true, reason: 'Profil fotoğrafın net ve doğrulandı.' });

  const afterHost = await f.service.bootstrap(f.host);
  assert.ok(afterHost.profile.verifications.includes('PHOTO'));
  assert.ok(afterHost.notifications.some(n => n.destination === 'PASSPORT' && n.body === 'Profil fotoğrafın net ve doğrulandı.'));
  assert.equal(afterHost.supportRequests.find(s => s.id === req2).response, 'Profil fotoğrafın net ve doğrulandı.');
});

test('reported and rejected memories enter admin moderation, preserve upload readability for moderators, and notify owners', async t => {
  const f = await fixture(t);
  const uploadId = (await f.service.upload(f.host, await jpeg('green'), 'image/jpeg')).id;
  const memId = (await f.cmd(f.host, 'memory.create', { caption: 'Sahada harika gol', sport: 'FOOTBALL', venue: 'Kadıköy Park', area: 'Kadıköy', city: 'İstanbul', uploadId })).result.id;

  // Report triggers admin alert & lets admin inspect upload even if author blocked admin.
  await f.cmd(f.host, 'user.block', { userId: f.admin.id, blocked: true });
  const repId = (await f.cmd(f.guest, 'report.create', { targetType: 'memory', targetId: memId, reason: 'Uygunsuz fotoğraf bildirimi' })).result.id;
  assert.ok((await f.service.bootstrap(f.admin)).notifications.some(n => n.destination === 'MODERATION' && n.targetId === repId));

  const inbox1 = await f.service.adminInbox(f.admin);
  const report = inbox1.reports.find(r => r.id === repId);
  assert.equal(report.targetUploadId, uploadId);
  assert.equal((await f.service.mediaFile(f.admin, uploadId)).mime, 'image/jpeg');

  // Admin rejects reported approved memory -> closes report, hides from public feed, notifies author & includes moderationReason in mine/bootstrap.
  await f.cmd(f.admin, 'admin.memory.review', { memoryId: memId, approved: false, reason: 'Başka sporcuların yüzü izinsiz görünüyor.' });
  assert.equal((await f.service.memories(null, { mode: 'newest' })).items.length, 0);
  const mine = (await f.service.memories(f.host, { mode: 'mine' })).items.find(m => m.id === memId);
  assert.equal(mine.status, 'rejected');
  assert.equal(mine.moderationReason, 'Başka sporcuların yüzü izinsiz görünüyor.');
  assert.equal((await f.service.adminInbox(f.admin)).reports.length, 0);
  assert.ok((await f.service.bootstrap(f.host)).notifications.some(n => n.destination === 'MEMORIES' && n.targetId === memId));

  // Author edits rejected memory -> moves to pending & alerts admins & appears in pending memories queue.
  await f.cmd(f.host, 'memory.update', { memoryId: memId, expectedVersion: 1, caption: 'Kırpılmış ve güncellenmiş anı' });
  const inbox2 = await f.service.adminInbox(f.admin);
  assert.equal(inbox2.memories.find(m => m.id === memId).authorName, 'Ev sahibi');
});

test('admin result resolve settles or refunds immediately, closes dispute reports, notifies users, and adminInbox exposes directory & flags', async t => {
  const f = await fixture(t);
  await f.cmd(f.admin, 'admin.operator.assign', { userId: f.host.id, role: 'venue_owner', reason: 'Tesis doğrulandı ve yetki açıldı.' });
  assert.ok((await f.service.bootstrap(f.host)).notifications.some(n => n.destination === 'VENUE_MANAGEMENT'));

  const eventId = (await f.cmd(f.host, 'event.create', f.event({ startsAtEpochMillis: f.clock() + 600000, capacity: 2 }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId });
  const code = (await f.cmd(f.host, 'event.checkin.code', { eventId })).result.code;
  for (const a of [f.host, f.guest]) await f.cmd(a, 'event.checkin', { eventId, code });
  const ver = (await f.store.get('SELECT version FROM events WHERE id=?', [eventId])).version;
  await f.cmd(f.host, 'event.transition', { eventId, status: 'IN_PROGRESS', expectedVersion: ver });
  await f.cmd(f.host, 'result.submit', { eventId, homeScore: 3, awayScore: 2, sides: { home: [f.host.id], away: [f.guest.id] } });
  await f.cmd(f.guest, 'result.dispute', { eventId, reason: 'Maç sonucu sahada 2-2 bitmişti.' });

  const inbox = await f.service.adminInbox(f.admin, { q: 'Konuk' });
  assert.ok(inbox.results.some(r => r.event_id === eventId && r.disputeReason.includes('2-2')));
  assert.ok(inbox.userDirectory.some(u => u.id === f.guest.id));
  assert.ok(inbox.users.every(row => Object.keys(row).sort().join(',') === 'id,name,role,status'));
  assert.equal(inbox.featureFlags.length, 5);
  assert.equal(inbox.counts.results, 1);

  f.advance(2 * 3600000);
  await f.cmd(f.admin, 'admin.result.resolve', { eventId, approve: false, reason: 'Hakem ve oyuncularca skor doğrulanamadı.' });
  const hostBoot = await f.service.bootstrap(f.host);
  assert.equal(hostBoot.wallet.locked, 0);
  assert.ok(hostBoot.notifications.some(n => n.targetId === eventId && n.title === 'Maç skoru reddedildi'));
  assert.equal((await f.service.adminInbox(f.admin)).reports.filter(r => r.reason === 'Skor itirazı').length, 0);
});

test('reported private chat upload is readable by admin only while chat kill switch gates edits', async t => {
  const f = await fixture(t);
  await f.cmd(f.guest, 'preferences.save', { privacy: { dmPolicy: 'Herkes' } });
  const threadId = (await f.cmd(f.host, 'chat.direct', { userId: f.guest.id })).result.id;
  const img = (await f.service.upload(f.host, await jpeg('yellow'), 'image/jpeg')).id;
  const msgId = (await f.cmd(f.host, 'chat.send', { threadId, text: 'Şikâyet edilecek mesaj', uploadId: img })).result.id;

  await assert.rejects(f.service.mediaFile(f.admin, img), fails('NOT_FOUND'));
  await f.cmd(f.guest, 'report.create', { targetType: 'message', targetId: msgId, reason: 'Uygunsuz sohbet görseli' });
  assert.equal((await f.service.mediaFile(f.admin, img)).mime, 'image/jpeg');

  await f.cmd(f.admin, 'admin.feature.set', { name: 'chat', enabled: false });
  await assert.rejects(f.cmd(f.host, 'message.edit', { messageId: msgId, expectedVersion: 1, text: 'Düzenle' }), fails('FEATURE_UNAVAILABLE'));

  // Bulk outbox retry_all
  await f.store.run("UPDATE outbox SET status='failed' WHERE status='pending'");
  const retried = (await f.cmd(f.admin, 'admin.outbox.retry_all', {})).result.queued;
  assert.ok(retried >= 1);
});

test('admin system SQL supplement and bundled schema are repeatable in PostgreSQL', async t => {
  const { PGlite } = await import('@electric-sql/pglite');
  const { readFile } = await import('node:fs/promises');
  const pg = new PGlite();
  t.after(() => pg.close());
  await pg.exec('CREATE ROLE anon; CREATE ROLE authenticated;');
  await pg.exec(await readFile(new URL('../supabase/01_schema.sql', import.meta.url), 'utf8'));
  const adminSql = await readFile(new URL('../supabase/08_admin_system.sql', import.meta.url), 'utf8');
  await pg.exec(adminSql);
  await pg.exec(adminSql);
  assert.equal((await pg.query('SELECT version FROM public.schema_migrations WHERE version=15')).rows[0].version, 15);
  assert.equal((await pg.query('SELECT count(*)::int AS n FROM public.feature_flags')).rows[0].n, 5);
});

test('admin.content.remove cleans reported event and team and resolves open report', async t => {
  const f = await fixture(t);
  await f.grant(f.host);
  const eventId = (await f.cmd(f.host, 'event.create', f.event())).result.id;
  const teamId = (await f.cmd(f.host, 'team.create', { name: 'Kötü İsim FK', sport: 'FOOTBALL', area: 'Kadıköy' })).result.id;
  const repEvent = (await f.cmd(f.guest, 'report.create', { targetType: 'event', targetId: eventId, reason: 'Kural ihlali içeren etkinlik' })).result.id;
  const repTeam = (await f.cmd(f.guest, 'report.create', { targetType: 'team', targetId: teamId, reason: 'Hakaret içeren takım adı' })).result.id;

  await f.cmd(f.admin, 'admin.content.remove', { reportId: repEvent, reason: 'Topluluk kurallarına aykırı etkinlik kaldırıldı.' });
  await f.cmd(f.admin, 'admin.content.remove', { reportId: repTeam, reason: 'Uygunsuz takım adı nedeniyle arşivlendi.' });

  const evRow = await f.store.get('SELECT status FROM events WHERE id=?', [eventId]);
  const teamRow = await f.store.get('SELECT payload FROM teams WHERE id=?', [teamId]);
  assert.equal(evRow.status, 'CANCELLED');
  assert.equal(JSON.parse(teamRow.payload).archived, true);
  assert.equal((await f.service.adminInbox(f.admin)).reports.length, 0);
});

test('admin.deletion.resolve executes or cancels pending deletion requests and legal terms propagate', async t => {
  const f = await fixture(t);
  await f.grant(f.host);
  await f.grant(f.guest);

  await f.cmd(f.guest, 'account.delete', {});
  await f.cmd(f.admin, 'admin.deletion.resolve', { userId: f.guest.id, approve: false, reason: 'Kullanıcı talebi iptal edildi' });
  assert.equal((await f.store.get('SELECT status FROM deletion_requests WHERE user_id=?', [f.guest.id])).status, 'cancelled');

  await f.cmd(f.host, 'account.delete', {});
  await f.cmd(f.admin, 'admin.deletion.resolve', { userId: f.host.id, approve: true, reason: 'Acil silme onaylandı' });
  assert.equal((await f.store.get('SELECT status FROM deletion_requests WHERE user_id=?', [f.host.id])).status, 'completed');
  assert.equal((await f.store.get('SELECT status FROM users WHERE id=?', [f.host.id])).status, 'deleted');

  await f.cmd(f.admin, 'admin.legal.publish', { kind: 'terms', version: '2026.9', url: 'https://example.com/terms-2026-9' });
  const cfg = await f.service.configuration();
  assert.equal(cfg.documents.terms.version, '2026.9');
  assert.equal(cfg.documents.terms_ios.url, 'https://example.com/terms-2026-9');
  assert.equal(cfg.documents.terms_android.url, 'https://example.com/terms-2026-9');
});

test('ensureProductionRuntimeSchema is repeatable in PostgreSQL', async t => {
  const { PGlite } = await import('@electric-sql/pglite');
  const { readFile } = await import('node:fs/promises');
  const { ensureProductionRuntimeSchema } = await import('../src/store.mjs');
  const pg = new PGlite();
  t.after(() => pg.close());
  await pg.exec('CREATE ROLE anon; CREATE ROLE authenticated;');
  await pg.exec(await readFile(new URL('../supabase/01_schema.sql', import.meta.url), 'utf8'));
  await ensureProductionRuntimeSchema({ exec: sql => pg.exec(sql) }, { publicUrl: 'https://rqtppytkoetwnjyfxulw.supabase.co/functions/v1/matchora-api' });
  await ensureProductionRuntimeSchema({ exec: sql => pg.exec(sql) }, { publicUrl: 'https://rqtppytkoetwnjyfxulw.supabase.co/functions/v1/matchora-api' });
  assert.equal((await pg.query('SELECT version FROM public.schema_migrations WHERE version=15')).rows[0].version, 15);
  assert.equal((await pg.query('SELECT count(*)::int AS n FROM public.feature_flags')).rows[0].n, 5);
});

