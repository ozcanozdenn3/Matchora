-- A venue can offer several courts at the same time. Per-court overlap is checked in the serialized command transaction.
ALTER TABLE venue_slots DROP CONSTRAINT IF EXISTS venue_slots_venue_id_starts_at_ends_at_key;
CREATE INDEX IF NOT EXISTS venue_slots_window ON venue_slots(venue_id,starts_at,ends_at);
INSERT INTO schema_migrations(version,applied_at) VALUES(9,0) ON CONFLICT(version) DO NOTHING;
