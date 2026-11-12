CREATE TABLE IF NOT EXISTS venue_applications (
 id TEXT PRIMARY KEY,
 user_id TEXT NOT NULL REFERENCES users(id),
 payload TEXT NOT NULL,
 status TEXT NOT NULL CHECK(status IN ('pending','approved','rejected','withdrawn')),
 version INTEGER NOT NULL DEFAULT 1,
 venue_id TEXT REFERENCES venues(id),
 response TEXT NOT NULL DEFAULT '',
 created_at BIGINT NOT NULL,
 updated_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS venue_applications_user ON venue_applications(user_id,status,created_at);
INSERT INTO schema_migrations(version,applied_at) VALUES(8,0) ON CONFLICT(version) DO NOTHING;
