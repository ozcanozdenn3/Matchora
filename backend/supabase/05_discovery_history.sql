-- Run the entire file in Supabase SQL Editor. This adds indexes for directory and history pagination.
-- Existing profile, match, rating and wallet data is preserved.
BEGIN;
CREATE INDEX IF NOT EXISTS profiles_search_order ON public.profiles(updated_at DESC,user_id DESC);
CREATE INDEX IF NOT EXISTS blocks_target_actor ON public.blocks(target_id,user_id);
CREATE INDEX IF NOT EXISTS rating_history_user_page ON public.rating_history(user_id,created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS reliability_history_user_page ON public.reliability_history(user_id,created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS wallet_entries_user_page ON public.wallet_entries(user_id,created_at DESC,id DESC);
INSERT INTO public.schema_migrations(version,applied_at) VALUES(12,0) ON CONFLICT(version) DO NOTHING;
COMMIT;
