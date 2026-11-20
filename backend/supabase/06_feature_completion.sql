-- Matchora: sporcu araması, tam geçmiş ve tekrar denemede güvenli fotoğraf yükleme.
-- Mevcut veritabanında Supabase SQL Editor ile bu dosyanın tamamını tek seferde çalıştır.
-- Tekrar çalıştırılabilir; mevcut kullanıcı, maç, sohbet veya cüzdan verilerini değiştirmez.
BEGIN;
SET LOCAL search_path = public;

CREATE INDEX IF NOT EXISTS profiles_search_order ON profiles(updated_at DESC,user_id DESC);
CREATE INDEX IF NOT EXISTS blocks_target_actor ON blocks(target_id,user_id);
CREATE INDEX IF NOT EXISTS rating_history_user_page ON rating_history(user_id,created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS reliability_history_user_page ON reliability_history(user_id,created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS wallet_entries_user_page ON wallet_entries(user_id,created_at DESC,id DESC);
INSERT INTO schema_migrations(version,applied_at) VALUES(12,0) ON CONFLICT(version) DO NOTHING;

CREATE TABLE IF NOT EXISTS upload_receipts (
 owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 request_key TEXT NOT NULL,
 request_hash TEXT NOT NULL,
 upload_id TEXT NOT NULL REFERENCES uploads(id) ON DELETE CASCADE,
 created_at BIGINT NOT NULL,
 PRIMARY KEY(owner_id,request_key)
);
CREATE INDEX IF NOT EXISTS upload_receipts_created ON upload_receipts(created_at);
INSERT INTO schema_migrations(version,applied_at) VALUES(13,0) ON CONFLICT(version) DO NOTHING;

ALTER TABLE public.upload_receipts ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.upload_receipts FROM PUBLIC;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
    REVOKE ALL ON TABLE public.upload_receipts FROM anon;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
    REVOKE ALL ON TABLE public.upload_receipts FROM authenticated;
  END IF;
END $$;
COMMIT;

-- Beklenen sonuç: 12 ve 13 sürümleri; receipt_rls = true.
SELECT version FROM public.schema_migrations WHERE version IN (12,13) ORDER BY version;
SELECT relrowsecurity AS receipt_rls FROM pg_class WHERE oid = 'public.upload_receipts'::regclass;
