import test from 'node:test';
import assert from 'node:assert/strict';
import { readdir, readFile } from 'node:fs/promises';
import { join } from 'node:path';
import sharp from 'sharp';
import { PGlite } from '@electric-sql/pglite';
import { fixture } from './helpers.mjs';

const image = color => sharp({create:{width:8,height:8,channels:3,background:color}}).jpeg().toBuffer();

test('replaying an acknowledged upload preserves the object, receipt and quota; keys cannot change photo', async t => {
  const f = await fixture(t), bytes = await image('white'), key = 'durable-photo-request';
  const first = await f.service.upload(f.host, bytes, 'image/jpeg', key);
  const repeated = await f.service.upload(f.host, bytes, 'image/jpeg', key);
  assert.deepEqual(repeated, first);
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM uploads WHERE owner_id=?', [f.host.id])).n, 1);
  assert.equal((await f.store.get('SELECT count FROM rate_limits WHERE bucket=?', [`media:${f.host.id}`])).count, 1);
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM upload_receipts')).n, 1);
  await assert.rejects(f.service.upload(f.host, await image('black'), 'image/jpeg', key), error => error.code === 'IDEMPOTENCY_CONFLICT');
  const other = await f.service.upload(f.other, bytes, 'image/jpeg', key);
  assert.notEqual(other.id, first.id);
  await assert.rejects(f.service.upload(f.host, bytes, 'image/jpeg', 'bad'), error => error.code === 'IDEMPOTENCY_REQUIRED');
});

test('simultaneous retry produces one receipt, one stored photo and one quota charge', async t => {
  const f = await fixture(t), bytes = await image('red');
  const results = await Promise.all(Array.from({length:4}, () => f.service.upload(f.host, bytes, 'image/jpeg', 'concurrent-photo-request')));
  assert.equal(new Set(results.map(result => result.id)).size, 1);
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM uploads')).n, 1);
  assert.equal((await f.store.get('SELECT COUNT(*) AS n FROM upload_receipts')).n, 1);
  assert.equal((await readdir(join(f.directory, 'uploads'))).length, 1);
});

test('supplemental Supabase script is repeatable and denies mobile access to upload receipts', async t => {
  const pg = new PGlite();
  t.after(() => pg.close());
  await pg.exec('CREATE ROLE anon; CREATE ROLE authenticated;');
  await pg.exec(await readFile(new URL('../migrations/001_core.sql', import.meta.url), 'utf8'));
  const sql = await readFile(new URL('../supabase/06_feature_completion.sql', import.meta.url), 'utf8');
  await pg.exec(sql);
  await pg.exec(sql);
  const versions = await pg.query('SELECT version FROM schema_migrations WHERE version IN (12,13) ORDER BY version');
  assert.deepEqual(versions.rows.map(row => row.version), [12,13]);
  const security = await pg.query("SELECT relrowsecurity FROM pg_class WHERE oid='public.upload_receipts'::regclass");
  assert.equal(security.rows[0].relrowsecurity, true);
  const grants = await pg.query("SELECT count(*)::int AS n FROM information_schema.table_privileges WHERE table_schema='public' AND table_name='upload_receipts' AND grantee IN ('anon','authenticated','PUBLIC')");
  assert.equal(grants.rows[0].n, 0);
});
