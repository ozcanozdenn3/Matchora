CREATE TABLE IF NOT EXISTS job_checkpoints (
 name TEXT PRIMARY KEY, ran_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS venue_reviews (
 id TEXT PRIMARY KEY, booking_id TEXT NOT NULL UNIQUE REFERENCES bookings(id),
 venue_id TEXT NOT NULL REFERENCES venues(id), user_id TEXT NOT NULL REFERENCES users(id),
 rating INTEGER NOT NULL CHECK(rating BETWEEN 1 AND 5), comment TEXT NOT NULL,
 status TEXT NOT NULL, version INTEGER NOT NULL DEFAULT 1,
 created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS venue_reviews_public ON venue_reviews(venue_id,status,created_at);
INSERT INTO schema_migrations(version,applied_at) VALUES(5,0) ON CONFLICT(version) DO NOTHING;
