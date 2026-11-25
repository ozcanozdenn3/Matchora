-- Matchora: yönetim paneli kuyruk, denetim ve moderasyon indeksleri.
-- Mevcut veritabanında bu dosyanın tamamını tek seferde çalıştırabilirsiniz (tekrar çalıştırılabilir).
BEGIN;
SET LOCAL search_path = public;

CREATE INDEX IF NOT EXISTS support_requests_open_queue ON support_requests(status, category, created_at);
CREATE INDEX IF NOT EXISTS reports_open_queue ON reports(status, created_at);
CREATE INDEX IF NOT EXISTS reports_target_status ON reports(target_type, target_id, status);
CREATE INDEX IF NOT EXISTS venue_applications_pending_queue ON venue_applications(status, created_at);
CREATE INDEX IF NOT EXISTS venue_reviews_pending_queue ON venue_reviews(status, created_at);
CREATE INDEX IF NOT EXISTS results_moderation_queue ON results(status, dispute_until, created_at);
CREATE INDEX IF NOT EXISTS deletion_requests_pending_queue ON deletion_requests(status, execute_after);
CREATE INDEX IF NOT EXISTS outbox_status_created ON outbox(status, created_at DESC);
CREATE INDEX IF NOT EXISTS users_role_status ON users(role, status, created_at DESC);
CREATE INDEX IF NOT EXISTS audit_log_recent ON audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS memories_upload_lookup ON memories(upload_id);
CREATE INDEX IF NOT EXISTS messages_upload_lookup ON messages(upload_id);
INSERT INTO schema_migrations(version, applied_at) VALUES(15, (extract(epoch from now()) * 1000)::bigint) ON CONFLICT(version) DO NOTHING;

INSERT INTO feature_flags(name, enabled, updated_at) VALUES
 ('billing', 1, (extract(epoch from now()) * 1000)::bigint),
 ('chat', 1, (extract(epoch from now()) * 1000)::bigint),
 ('memories', 1, (extract(epoch from now()) * 1000)::bigint),
 ('tournaments', 1, (extract(epoch from now()) * 1000)::bigint),
 ('venues', 1, (extract(epoch from now()) * 1000)::bigint)
ON CONFLICT(name) DO NOTHING;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron')
     AND EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_net') THEN
    PERFORM cron.unschedule('matchora-jobs-heartbeat')
      WHERE EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'matchora-jobs-heartbeat');
    PERFORM cron.schedule(
      'matchora-jobs-heartbeat',
      '* * * * *',
      $cron$SELECT net.http_get(url := 'https://rqtppytkoetwnjyfxulw.supabase.co/functions/v1/matchora-api/health');$cron$
    );
  END IF;
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

COMMIT;

SELECT version FROM public.schema_migrations WHERE version = 15;
SELECT name, enabled FROM public.feature_flags ORDER BY name;
