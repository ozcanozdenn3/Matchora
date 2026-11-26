import { readFile, mkdir } from 'node:fs/promises';
import { dirname } from 'node:path';

export class SqliteStore {
  constructor(db) { this.db = db; this.tail = Promise.resolve(); this.statements = new Map(); }
  static async open(path = ':memory:') {
    const sqliteSpec = ['node', 'sqlite'].join(':');
    const { DatabaseSync } = await import(sqliteSpec);
    if (path !== ':memory:') await mkdir(dirname(path), { recursive: true, mode: 0o700 });
    const db = new DatabaseSync(path);
    db.exec('PRAGMA foreign_keys=ON; PRAGMA journal_mode=WAL; PRAGMA busy_timeout=5000;');
    return new SqliteStore(db);
  }
  statement(sql) {
    let statement = this.statements.get(sql);
    if (!statement) {
      statement = this.db.prepare(sql);
      if (this.statements.size >= 512) this.statements.delete(this.statements.keys().next().value);
      this.statements.set(sql, statement);
    }
    return statement;
  }
  async get(sql, params = []) { return this.statement(sql).get(...params); }
  async all(sql, params = []) { return this.statement(sql).all(...params); }
  async run(sql, params = []) { const r = this.statement(sql).run(...params); return { changes: Number(r.changes) }; }
  async exec(sql) { this.statements.clear(); this.db.exec(sql); }
  async read(fn) { return fn(this); }
  async transaction(fn) {
    const previous = this.tail; let release;
    this.tail = new Promise(resolve => { release = resolve; });
    await previous;
    try { this.db.exec('BEGIN IMMEDIATE'); const result = await fn(this); this.db.exec('COMMIT'); return result; }
    catch (error) { if (this.db.isTransaction) this.db.exec('ROLLBACK'); throw error; }
    finally { release(); }
  }
  async close() { await this.tail; this.statements.clear(); this.db.close(); }
}

// The same parameterized statements and transaction boundaries are used after the final PostgreSQL connection.
export class PostgresStore {
  constructor(pool, client = null) { this.pool = pool; this.client = client; }
  static async open(url, ssl = true) {
    const pgMod = await import('pg');
    const Pool = pgMod.Pool ?? pgMod.default?.Pool;
    const types = pgMod.types ?? pgMod.default?.types;
    types.setTypeParser(20, value => { const n=Number(value); if(!Number.isSafeInteger(n)) throw new Error('Unsafe database integer'); return n; });
    const connection = new URL(url);
    for (const key of ['sslmode','sslcert','sslkey','sslrootcert']) connection.searchParams.delete(key);
    const env = { ...(globalThis.Deno?.env?.toObject?.() ?? {}), ...(globalThis.process?.env ?? {}) };
    const rejectUnauthorized = env.DATABASE_SSL_REJECT_UNAUTHORIZED !== 'false';
    const pool = new Pool({
      connectionString: connection.toString(),
      ssl: ssl ? { rejectUnauthorized } : false,
      max: Number(env.DATABASE_POOL_MAX ?? 10),
      connectionTimeoutMillis: 5000,
      idleTimeoutMillis: 30000,
      statement_timeout: 15000,
      keepAlive: true,
      keepAliveInitialDelayMillis: 10000
    });
    pool.on('error', (err) => {
      console.warn('Postgres pool idle client warning:', err.message);
    });
    pool.on('connect', (client) => {
      client.on('error', (err) => {
        console.warn('Postgres client socket warning:', err.message);
      });
    });
    return new PostgresStore(pool);
  }
  async query(sql, params) {
    let index = 0;
    // Application SQL uses ? only as parameter placeholders, never in literals.
    const statement = sql.replace(/\?/g, () => `$${++index}`);
    const result = await (this.client ?? this.pool).query(statement, params);
    return result;
  }
  async get(sql, params = []) { return (await this.query(sql, params)).rows[0]; }
  async all(sql, params = []) { return (await this.query(sql, params)).rows; }
  async run(sql, params = []) { return { changes: (await this.query(sql, params)).rowCount }; }
  async exec(sql) { await (this.client ?? this.pool).query(sql); }
  async read(fn) { return fn(this); }
  async transaction(fn) {
    const client = await this.pool.connect();
    let hasError = false;
    try {
      await client.query('BEGIN');
      // All writers share this advisory lock; this keeps cross-module invariants identical to SQLite.
      await client.query('SELECT pg_advisory_xact_lock(617834921)');
      const result = await fn(new PostgresStore(this.pool, client));
      await client.query('COMMIT'); return result;
    } catch (error) {
      hasError = true;
      try { await client.query('ROLLBACK'); } catch (_) {}
      throw error;
    }
    finally { client.release(hasError); }
  }
  async close() { await this.pool.end(); }
}

export async function migrate(store) {
  await store.exec(await readFile(new URL('../migrations/001_core.sql', import.meta.url), 'utf8'));
  await store.exec(await readFile(new URL('../migrations/003_integrations.sql', import.meta.url), 'utf8'));
  await store.exec(await readFile(new URL('../migrations/005_product_workflows.sql', import.meta.url), 'utf8'));
  await store.exec(await readFile(new URL('../migrations/007_social_completion.sql', import.meta.url), 'utf8'));
  await store.exec(await readFile(new URL('../migrations/008_venue_applications.sql', import.meta.url), 'utf8'));
  await store.exec(await readFile(new URL('../migrations/010_private_event_locations.sql', import.meta.url), 'utf8'));
  await store.exec(await readFile(new URL('../migrations/014_identity_cleanup_history.sql', import.meta.url), 'utf8'));
  const deliveryMigration = await readFile(new URL('../migrations/006_push_delivery.sql', import.meta.url), 'utf8');
  if (store instanceof SqliteStore) {
    await store.exec(await readFile(new URL('../migrations/013_upload_receipts.sql', import.meta.url), 'utf8'));
    const slotIndexes = await store.all('PRAGMA index_list(venue_slots)');
    for (const index of slotIndexes.filter(i => i.unique && i.origin === 'u')) {
      const columns = await store.all(`PRAGMA index_info('${index.name.replaceAll("'", "''")}')`);
      if (columns.map(c => c.name).join(',') !== 'venue_id,starts_at,ends_at') continue;
      await store.exec('PRAGMA foreign_keys=OFF;');
      try {
        await store.exec(`BEGIN;
          CREATE TABLE venue_slots_migrating (id TEXT PRIMARY KEY,venue_id TEXT NOT NULL REFERENCES venues(id),starts_at BIGINT NOT NULL,ends_at BIGINT NOT NULL,price_minor INTEGER NOT NULL CHECK(price_minor>=0),status TEXT NOT NULL,payload TEXT NOT NULL);
          INSERT INTO venue_slots_migrating SELECT id,venue_id,starts_at,ends_at,price_minor,status,payload FROM venue_slots;
          DROP TABLE venue_slots;
          ALTER TABLE venue_slots_migrating RENAME TO venue_slots;`);
        const violations=await store.all('PRAGMA foreign_key_check');
        if(violations.length)throw new Error('Multi-court migration failed foreign key validation');
        await store.exec('COMMIT;');
      } catch(error) { await store.exec('ROLLBACK;');throw error; }
      finally { await store.exec('PRAGMA foreign_keys=ON;'); }
    }
    await store.exec((await readFile(new URL('../migrations/009_multi_court_slots.sql',import.meta.url),'utf8')).replace(/^ALTER TABLE .*;\r?\n/gm,''));
    const outboxColumns = await store.all('PRAGMA table_info(outbox)');
    for (const [name,type] of [['claim_token','TEXT'],['delivered_at','BIGINT']]) {
      if (!outboxColumns.some(c => c.name === name)) await store.exec(`ALTER TABLE outbox ADD COLUMN ${name} ${type}`);
    }
    // SQLite lacks ADD COLUMN IF NOT EXISTS; apply those two guarded alterations above.
    await store.exec(deliveryMigration.replace(/^ALTER TABLE outbox ADD COLUMN IF NOT EXISTS .*;\r?\n/gm, ''));
    const columns = await store.all('PRAGMA table_info(subscriptions)');
    if (!columns.some(c => c.name === 'anchor_at')) await store.exec('ALTER TABLE subscriptions ADD COLUMN anchor_at BIGINT NOT NULL DEFAULT 0');
    await store.run('UPDATE schema_migrations SET applied_at=? WHERE version=1 AND applied_at=0',[Date.now()]);
    await store.exec(`CREATE TRIGGER IF NOT EXISTS wallet_no_update BEFORE UPDATE ON wallet_entries BEGIN SELECT RAISE(ABORT,'immutable ledger'); END;
      CREATE TRIGGER IF NOT EXISTS wallet_no_delete BEFORE DELETE ON wallet_entries BEGIN SELECT RAISE(ABORT,'immutable ledger'); END;
      CREATE TRIGGER IF NOT EXISTS audit_no_update BEFORE UPDATE ON audit_log BEGIN SELECT RAISE(ABORT,'immutable audit'); END;
      CREATE TRIGGER IF NOT EXISTS audit_no_delete BEFORE DELETE ON audit_log BEGIN SELECT RAISE(ABORT,'immutable audit'); END;`);
  } else {
    await store.exec(deliveryMigration);
    await store.exec(await readFile(new URL('../migrations/009_multi_court_slots.sql',import.meta.url),'utf8'));
    await store.exec(await readFile(new URL('../migrations/013_upload_receipts.sql', import.meta.url), 'utf8'));
    await store.exec(await readFile(new URL('../migrations/002_postgres_security.sql', import.meta.url), 'utf8'));
  }
  await store.exec(await readFile(new URL('../migrations/011_indexes_and_sync.sql', import.meta.url), 'utf8'));
  await store.exec(await readFile(new URL('../migrations/012_discovery_history.sql', import.meta.url), 'utf8'));
  await store.exec(await readFile(new URL('../migrations/015_admin_system.sql', import.meta.url), 'utf8'));
}

export async function ensureProductionRuntimeSchema(store, config = {}) {
  const healthUrl = `${(config.publicUrl || 'https://rqtppytkoetwnjyfxulw.supabase.co/functions/v1/matchora-api').replace(/\/$/, '')}/health`;
  const parsed = new URL(healthUrl);
  if (parsed.protocol !== 'https:' || parsed.username || parsed.password) {
    throw new Error('Invalid public health URL for runtime schema');
  }
  const safeHealthUrl = parsed.toString().replaceAll("'", "''");
  const now = Date.now();
  await store.exec(`
    CREATE INDEX IF NOT EXISTS support_requests_open_queue ON support_requests(status, category, created_at);
    CREATE INDEX IF NOT EXISTS reports_open_queue ON reports(status, created_at);
    CREATE INDEX IF NOT EXISTS reports_target_status ON reports(target_type, target_id, status);
    CREATE INDEX IF NOT EXISTS venue_applications_pending_queue ON venue_applications(status, created_at);
    CREATE INDEX IF NOT EXISTS venue_reviews_pending_queue ON venue_reviews(status, created_at);
    CREATE INDEX IF NOT EXISTS results_moderation_queue ON results(status, dispute_until, created_at);
    CREATE INDEX IF NOT EXISTS deletion_requests_pending_queue ON deletion_requests(status, execute_after);
    CREATE INDEX IF NOT EXISTS outbox_status_created ON outbox(status, created_at DESC);
    CREATE INDEX IF NOT EXISTS users_role_status ON users(role, status, created_at DESC);
    CREATE INDEX IF NOT EXISTS audit_log_recent ON audit_log(created_at DESC);
    CREATE INDEX IF NOT EXISTS memories_upload_lookup ON memories(upload_id);
    CREATE INDEX IF NOT EXISTS messages_upload_lookup ON messages(upload_id);
    INSERT INTO schema_migrations(version, applied_at) VALUES(15, ${now}) ON CONFLICT(version) DO NOTHING;
    INSERT INTO feature_flags(name, enabled, updated_at) VALUES
      ('billing', 1, ${now}),
      ('chat', 1, ${now}),
      ('memories', 1, ${now}),
      ('tournaments', 1, ${now}),
      ('venues', 1, ${now})
    ON CONFLICT(name) DO NOTHING;
    DO $$
    BEGIN
      IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron')
         AND EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_net') THEN
        PERFORM cron.unschedule('matchora-jobs-heartbeat')
          WHERE EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'matchora-jobs-heartbeat');
        PERFORM cron.schedule(
          'matchora-jobs-heartbeat',
          '* * * * *',
          format('SELECT net.http_get(url := %L);', '${safeHealthUrl}')
        );
      END IF;
    EXCEPTION WHEN OTHERS THEN
      NULL;
    END $$;
  `);
}
