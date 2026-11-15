CREATE INDEX IF NOT EXISTS notifications_user_feed ON notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS memories_status_created ON memories(status, created_at DESC);
CREATE INDEX IF NOT EXISTS memories_author_created ON memories(author_id, created_at DESC);
CREATE INDEX IF NOT EXISTS event_members_user_status ON event_members(user_id, status);
CREATE INDEX IF NOT EXISTS team_members_user_status ON team_members(user_id, status);
CREATE INDEX IF NOT EXISTS community_members_user_status ON community_members(user_id, status);
CREATE INDEX IF NOT EXISTS bookings_user_created ON bookings(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS venues_owner ON venues(owner_id);
INSERT INTO schema_migrations(version,applied_at) VALUES(11,0) ON CONFLICT(version) DO NOTHING;

