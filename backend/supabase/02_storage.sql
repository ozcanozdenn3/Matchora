-- Supabase SQL Editor only. The API holds the service key; mobile clients never do.
-- No public bucket and no anon/authenticated storage policies are created.
INSERT INTO storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
VALUES('matchora-private','matchora-private',false,5242880,ARRAY['image/jpeg','image/png'])
ON CONFLICT(id) DO UPDATE SET public=false,file_size_limit=5242880,
 allowed_mime_types=ARRAY['image/jpeg','image/png'];
INSERT INTO public.schema_migrations(version,applied_at) VALUES(4,0) ON CONFLICT(version) DO NOTHING;
