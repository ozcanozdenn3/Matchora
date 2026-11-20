CREATE INDEX IF NOT EXISTS profiles_search_order ON profiles(updated_at DESC,user_id DESC);
CREATE INDEX IF NOT EXISTS blocks_target_actor ON blocks(target_id,user_id);
CREATE INDEX IF NOT EXISTS rating_history_user_page ON rating_history(user_id,created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS reliability_history_user_page ON reliability_history(user_id,created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS wallet_entries_user_page ON wallet_entries(user_id,created_at DESC,id DESC);
INSERT INTO schema_migrations(version,applied_at) VALUES(12,0) ON CONFLICT(version) DO NOTHING;
