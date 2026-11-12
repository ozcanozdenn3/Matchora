CREATE TABLE IF NOT EXISTS message_metadata (
 message_id TEXT PRIMARY KEY REFERENCES messages(id),
 thread_id TEXT NOT NULL REFERENCES threads(id),
 sequence BIGINT NOT NULL,
 version INTEGER NOT NULL DEFAULT 1,
 edited_at BIGINT,
 deleted_at BIGINT,
 reply_to TEXT REFERENCES messages(id),
 UNIQUE(thread_id,sequence)
);
CREATE TABLE IF NOT EXISTS thread_member_state (
 thread_id TEXT NOT NULL REFERENCES threads(id),
 user_id TEXT NOT NULL REFERENCES users(id),
 hidden INTEGER NOT NULL DEFAULT 0,
 clear_sequence BIGINT NOT NULL DEFAULT 0,
 read_sequence BIGINT NOT NULL DEFAULT 0,
 version INTEGER NOT NULL DEFAULT 1,
 updated_at BIGINT NOT NULL,
 PRIMARY KEY(thread_id,user_id)
);
CREATE TABLE IF NOT EXISTS thread_revisions (
 thread_id TEXT PRIMARY KEY REFERENCES threads(id),
 revision BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS message_hides (
 message_id TEXT NOT NULL REFERENCES messages(id),
 user_id TEXT NOT NULL REFERENCES users(id),
 created_at BIGINT NOT NULL,
 PRIMARY KEY(message_id,user_id)
);
CREATE TABLE IF NOT EXISTS memory_locations (
 memory_id TEXT PRIMARY KEY REFERENCES memories(id),
 city TEXT NOT NULL,
 district TEXT NOT NULL,
 sport TEXT NOT NULL,
 latitude DOUBLE PRECISION,
 longitude DOUBLE PRECISION,
 shuffle_key BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS notification_hides (
 notification_id TEXT NOT NULL REFERENCES notifications(id),
 user_id TEXT NOT NULL REFERENCES users(id),
 created_at BIGINT NOT NULL,
 PRIMARY KEY(notification_id,user_id)
);
CREATE INDEX IF NOT EXISTS message_metadata_thread_sequence ON message_metadata(thread_id,sequence);
CREATE INDEX IF NOT EXISTS message_hides_user ON message_hides(user_id,message_id);
CREATE INDEX IF NOT EXISTS memory_locations_region ON memory_locations(city,district,sport);
INSERT INTO schema_migrations(version,applied_at) VALUES(7,0) ON CONFLICT(version) DO NOTHING;
