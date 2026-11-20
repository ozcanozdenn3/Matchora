-- Matchora: hesap silme bağlantısı ve rezervasyon/destek/şikâyet geçmişleri.
-- Mevcut veritabanında bu dosyanın tamamını tek seferde çalıştır.
-- Tekrar çalıştırılabilir. Mevcut kullanıcı/maç/sohbet/cüzdan verilerini değiştirmez.
-- Auth hesabının silinmesi, yalnız kullanıcının süresi dolan silme talebi için API worker'ında yapılır.
BEGIN;
SET LOCAL search_path = public;

CREATE TABLE IF NOT EXISTS identity_deletions (
 user_id TEXT PRIMARY KEY REFERENCES users(id),
 provider_user_id TEXT,
 auth_origin TEXT NOT NULL,
 status TEXT NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','processing','completed')),
 attempts INTEGER NOT NULL DEFAULT 0,
 next_at BIGINT NOT NULL,
 claim_token TEXT,
 last_error TEXT NOT NULL DEFAULT '',
 created_at BIGINT NOT NULL,
 completed_at BIGINT,
 UNIQUE(auth_origin,provider_user_id)
);
CREATE INDEX IF NOT EXISTS identity_deletions_due ON identity_deletions(status,next_at);
CREATE INDEX IF NOT EXISTS bookings_user_history ON bookings(user_id,created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS support_user_history ON support_requests(user_id,created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS reports_user_history ON reports(user_id,created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS audit_support_reply ON audit_log(target_id,action,created_at DESC,id DESC);
INSERT INTO schema_migrations(version,applied_at) VALUES(14,0) ON CONFLICT(version) DO NOTHING;

ALTER TABLE public.identity_deletions ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.identity_deletions FROM PUBLIC;
DO $$
BEGIN
  IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname='anon') THEN
    REVOKE ALL ON TABLE public.identity_deletions FROM anon;
  END IF;
  IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN
    REVOKE ALL ON TABLE public.identity_deletions FROM authenticated;
  END IF;
END $$;
COMMIT;

-- Beklenen: version=14, identity_cleanup_rls=true.
SELECT version FROM public.schema_migrations WHERE version=14;
SELECT relrowsecurity AS identity_cleanup_rls FROM pg_class WHERE oid='public.identity_deletions'::regclass;
