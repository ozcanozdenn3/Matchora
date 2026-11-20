CREATE TABLE IF NOT EXISTS upload_receipts (
 owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 request_key TEXT NOT NULL,
 request_hash TEXT NOT NULL,
 upload_id TEXT NOT NULL REFERENCES uploads(id) ON DELETE CASCADE,
 created_at BIGINT NOT NULL,
 PRIMARY KEY(owner_id,request_key)
);
CREATE INDEX IF NOT EXISTS upload_receipts_created ON upload_receipts(created_at);
INSERT INTO schema_migrations(version,applied_at) VALUES(13,0) ON CONFLICT(version) DO NOTHING;
