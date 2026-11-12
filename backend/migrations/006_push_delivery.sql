ALTER TABLE outbox ADD COLUMN IF NOT EXISTS claim_token TEXT;
ALTER TABLE outbox ADD COLUMN IF NOT EXISTS delivered_at BIGINT;
CREATE INDEX IF NOT EXISTS outbox_user_delivery ON outbox(user_id,status,delivered_at);
INSERT INTO schema_migrations(version,applied_at) VALUES(6,0) ON CONFLICT(version) DO NOTHING;
