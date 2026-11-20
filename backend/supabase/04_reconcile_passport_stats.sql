-- Matchora passport statistics repair. Run the whole file in Supabase SQL Editor.
-- The first result explains completed matches that have not reached verification.
-- Counts use verified results and recorded check-ins. This does not create attendance,
-- approve a score, award tokens, or change MMR/reliability.
SELECT e.id AS event_id,
       e.status AS event_status,
       COALESCE(r.status, 'missing') AS result_status,
       COUNT(m.user_id) AS joined_players,
       COUNT(m.checked_in_at) AS checked_in_players,
       CASE
         WHEN COUNT(m.checked_in_at) = 0 THEN 'CHECKIN_REQUIRED'
         WHEN r.event_id IS NULL THEN 'RESULT_REQUIRED'
         WHEN r.status = 'disputed' THEN 'DISPUTE_REVIEW_REQUIRED'
         WHEN r.status = 'pending' THEN 'OPPONENT_CONFIRMATION_AND_WAITING_PERIOD'
         ELSE 'RESULT_NOT_VERIFIED'
       END AS pending_step
FROM public.events e
LEFT JOIN public.results r ON r.event_id = e.id
LEFT JOIN public.event_members m ON m.event_id = e.id AND m.status = 'joined'
WHERE e.status IN ('COMPLETED', 'DISPUTED') AND (r.status IS NULL OR r.status <> 'verified')
GROUP BY e.id, e.status, r.event_id, r.status
ORDER BY e.id;

-- Self-contained statement: the editor may execute statements on separate connections.
DO $matchora_passport_repair$
DECLARE
  repaired_profiles bigint;
BEGIN
  -- Acquire the business-write lock before the next statement takes its snapshot.
  PERFORM pg_advisory_xact_lock(617834921);

WITH verified_scores AS (
  SELECT event_id,
         CASE WHEN payload::jsonb ->> 'homeScore' ~ '^[0-9]{1,3}$'
              THEN (payload::jsonb ->> 'homeScore')::integer END AS home_score,
         CASE WHEN payload::jsonb ->> 'awayScore' ~ '^[0-9]{1,3}$'
              THEN (payload::jsonb ->> 'awayScore')::integer END AS away_score
  FROM public.results WHERE status = 'verified'
), verified_attendance AS (
  SELECT m.user_id, e.sport, e.ends_at,
         FLOOR((e.ends_at::numeric + 3 * 3600000 + 3 * 86400000) / (7 * 86400000))::bigint AS played_week,
         CASE
           WHEN m.side = 'home' AND r.home_score > r.away_score THEN 1
           WHEN m.side = 'away' AND r.away_score > r.home_score THEN 1
           ELSE 0
         END AS won
  FROM public.event_members m
  JOIN public.events e ON e.id = m.event_id
  JOIN verified_scores r ON r.event_id = e.id
  WHERE m.status = 'joined' AND m.checked_in_at IS NOT NULL
), totals AS (
  SELECT user_id, COUNT(*)::integer AS matches FROM verified_attendance GROUP BY user_id
), sport_totals AS (
  SELECT user_id, sport, COUNT(*)::integer AS matches, SUM(won)::integer AS wins
  FROM verified_attendance GROUP BY user_id, sport
), current_week AS (
  -- Must match progression.mjs weekNumber(), including Istanbul's UTC+3 offset.
  SELECT FLOOR((EXTRACT(EPOCH FROM transaction_timestamp()) * 1000 + 3 * 3600000 + 3 * 86400000) / (7 * 86400000))::bigint AS week
), played_weeks AS (
  SELECT DISTINCT user_id, played_week FROM verified_attendance
), streak_anchor AS (
  SELECT p.user_id,
         CASE WHEN EXISTS (SELECT 1 FROM played_weeks w WHERE w.user_id = p.user_id AND w.played_week = c.week)
              THEN c.week ELSE c.week - 1 END AS anchor
  FROM public.profiles p CROSS JOIN current_week c
), ordered_weeks AS (
  SELECT w.user_id, w.played_week, a.anchor,
         ROW_NUMBER() OVER (PARTITION BY w.user_id ORDER BY w.played_week DESC) AS position
  FROM played_weeks w JOIN streak_anchor a ON a.user_id = w.user_id
  WHERE w.played_week <= a.anchor
), streaks AS (
  SELECT user_id, COUNT(*)::integer AS weeks
  FROM ordered_weeks WHERE played_week = anchor - position + 1 GROUP BY user_id
), profiles AS (
  SELECT p.user_id, p.payload::jsonb AS previous,
         COALESCE(t.matches, 0) AS matches, COALESCE(s.weeks, 0) AS weeks
  FROM public.profiles p
  LEFT JOIN totals t ON t.user_id = p.user_id
  LEFT JOIN streaks s ON s.user_id = p.user_id
), updated AS (
  SELECT p.user_id, p.previous,
         p.previous || jsonb_build_object(
           'verifiedMatches', p.matches,
           'streakWeeks', p.weeks,
           'skills', COALESCE((
             SELECT jsonb_agg(
               CASE WHEN jsonb_typeof(skill.value) = 'object' THEN
                 skill.value || jsonb_build_object(
                   'verifiedMatches', COALESCE(t.matches, 0),
                   'wonMatches', COALESCE(t.wins, 0),
                   'winRate', CASE WHEN COALESCE(t.matches, 0) > 0 THEN ROUND(100.0 * t.wins / t.matches)::integer ELSE 0 END
                 )
               ELSE skill.value END ORDER BY skill.ordinality)
             FROM jsonb_array_elements(CASE WHEN jsonb_typeof(p.previous -> 'skills') = 'array' THEN p.previous -> 'skills' ELSE '[]'::jsonb END)
                  WITH ORDINALITY AS skill(value, ordinality)
             LEFT JOIN sport_totals t ON t.user_id = p.user_id AND t.sport = skill.value ->> 'sport'
           ), '[]'::jsonb)
         ) || CASE WHEN jsonb_typeof(p.previous -> 'skillArchive') = 'object' THEN jsonb_build_object(
           'skillArchive', COALESCE((
             SELECT jsonb_object_agg(skill.key,
               CASE WHEN jsonb_typeof(skill.value) = 'object' THEN
                 skill.value || jsonb_build_object(
                   'verifiedMatches', COALESCE(t.matches, 0),
                   'wonMatches', COALESCE(t.wins, 0),
                   'winRate', CASE WHEN COALESCE(t.matches, 0) > 0 THEN ROUND(100.0 * t.wins / t.matches)::integer ELSE 0 END
                 )
               ELSE skill.value END)
             FROM jsonb_each(p.previous -> 'skillArchive') skill
             LEFT JOIN sport_totals t ON t.user_id = p.user_id AND t.sport = skill.key
           ), '{}'::jsonb)
         ) ELSE '{}'::jsonb END AS next
  FROM profiles p
)
, repaired AS (
  UPDATE public.profiles p
  SET payload = repair.next::text,
      version = p.version + 1,
      updated_at = FLOOR(EXTRACT(EPOCH FROM transaction_timestamp()) * 1000)::bigint
  FROM updated repair
  WHERE p.user_id = repair.user_id AND repair.previous IS DISTINCT FROM repair.next
  RETURNING p.user_id
)
SELECT COUNT(*) INTO repaired_profiles FROM repaired;

RAISE NOTICE 'Matchora: % passport profiles reconciled.', repaired_profiles;
END
$matchora_passport_repair$;

-- Current values, available even when the editor executes this query separately.
SELECT user_id,
       payload::jsonb ->> 'verifiedMatches' AS verified_matches,
       payload::jsonb ->> 'streakWeeks' AS streak_weeks
FROM public.profiles ORDER BY user_id;
