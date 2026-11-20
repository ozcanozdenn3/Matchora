-- Private server-owned schema. Mobile clients use the authenticated API, never direct table writes.
DO $$ DECLARE t RECORD; BEGIN
  FOR t IN SELECT tablename FROM pg_tables WHERE schemaname=current_schema() AND tablename IN
    ('users','profiles','preferences','sessions','auth_challenges','rate_limits','commands','wallet_entries','holds','rewards','store_transactions','subscriptions','webhook_events','events','event_members','join_requests','waitlist','results','result_confirmations','feedback','rating_history','reliability_history','teams','team_members','team_invites','challenges','tournaments','tournament_members','tournament_fixtures','communities','community_members','venues','venue_slots','bookings','venue_payments','threads','thread_members','blocks','uploads','messages','memories','memory_likes','notifications','outbox','push_devices','support_requests','reports','deletion_requests','audit_log','feature_flags','legal_documents','legal_acceptances','discovery_preferences','oauth_flows','storage_deletions','subscription_windows','job_checkpoints','venue_reviews','message_metadata','thread_member_state','thread_revisions','message_hides','memory_locations','notification_hides','venue_applications','event_locations','upload_receipts','identity_deletions')
  LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t.tablename);
    IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname='anon') THEN EXECUTE format('REVOKE ALL ON TABLE %I FROM anon',t.tablename); END IF;
    IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN EXECUTE format('REVOKE ALL ON TABLE %I FROM authenticated',t.tablename); END IF;
  END LOOP;
END $$;
CREATE OR REPLACE FUNCTION matchora_immutable_row() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'append-only record'; END $$;
DROP TRIGGER IF EXISTS wallet_immutable ON wallet_entries;
CREATE TRIGGER wallet_immutable BEFORE UPDATE OR DELETE ON wallet_entries FOR EACH ROW EXECUTE FUNCTION matchora_immutable_row();
DROP TRIGGER IF EXISTS audit_immutable ON audit_log;
CREATE TRIGGER audit_immutable BEFORE UPDATE OR DELETE ON audit_log FOR EACH ROW EXECUTE FUNCTION matchora_immutable_row();
INSERT INTO schema_migrations(version,applied_at) VALUES(2,0) ON CONFLICT(version) DO NOTHING;
