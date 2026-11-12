CREATE TABLE IF NOT EXISTS event_locations (
 event_id TEXT PRIMARY KEY REFERENCES events(id),
 latitude DOUBLE PRECISION NOT NULL,
 longitude DOUBLE PRECISION NOT NULL
);
INSERT INTO schema_migrations(version,applied_at) VALUES(10,0) ON CONFLICT(version) DO NOTHING;
