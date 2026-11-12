CREATE TABLE IF NOT EXISTS oauth_flows (
 id TEXT PRIMARY KEY, provider TEXT NOT NULL, client_secret_hash TEXT NOT NULL,
 verifier TEXT NOT NULL, expires_at BIGINT NOT NULL, consumed INTEGER NOT NULL DEFAULT 0,
 created_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS storage_deletions (
 object_key TEXT PRIMARY KEY, attempts INTEGER NOT NULL DEFAULT 0,
 next_at BIGINT NOT NULL, created_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS subscription_windows (
 transaction_id TEXT PRIMARY KEY REFERENCES store_transactions(transaction_id),
 user_id TEXT NOT NULL REFERENCES users(id), product_id TEXT NOT NULL,
 starts_at BIGINT NOT NULL, ends_at BIGINT NOT NULL
);
INSERT INTO schema_migrations(version,applied_at) VALUES(3,0) ON CONFLICT(version) DO NOTHING;
