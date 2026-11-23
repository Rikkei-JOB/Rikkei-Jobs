-- Run in a disposable local dev database after Flyway migrations.
-- Re-running keeps the decisions already made by STAFF.
BEGIN;
SET LOCAL search_path TO profile;
WITH sample(user_id, full_name, class_code, course_name, visibility, submitted) AS (
    VALUES
        ('06000000-0000-4000-8000-000000000001'::uuid, 'Nguyễn Thu Hà', 'HS06-A', 'Java 2026', 'PENDING', '2026-11-18 02:00Z'::timestamptz),
        ('06000000-0000-4000-8000-000000000002'::uuid, 'Trần Minh An', 'HS06-A', 'Java 2026', 'PENDING', '2026-11-19 03:00Z'::timestamptz),
        ('06000000-0000-4000-8000-000000000003'::uuid, 'Lê Bảo Linh', 'HS06-B', 'Frontend 2026', 'PRIVATE', NULL::timestamptz)
), added AS (
    INSERT INTO candidate_profiles
        (user_id, full_name, email, phone, program, class_code, course_name, headline, summary, visibility, consent_at, updated_at)
    SELECT user_id, full_name, 'demo-' || right(user_id::text, 1) || '@example.test', '0900000000',
           'Lập trình', class_code, course_name, 'Lập trình viên Java',
           'Mong muốn phát triển ứng dụng quản lý tuyển dụng.', visibility, submitted, coalesce(submitted, now())
    FROM sample ON CONFLICT (user_id) DO NOTHING
    RETURNING user_id, visibility, consent_at
), states AS (
    INSERT INTO skill_profile_states(user_id, consented, approved)
    SELECT user_id, visibility = 'PENDING', false FROM added
    ON CONFLICT DO NOTHING
)
INSERT INTO profile_reviews(user_id, action, actor_id, reason, created_at)
SELECT a.user_id, event.action,
       CASE WHEN event.action = 'REJECT' THEN '06000000-0000-4000-8000-000000000099'::uuid ELSE a.user_id END,
       CASE WHEN event.action = 'REJECT' THEN 'Vui lòng bổ sung mục tiêu nghề nghiệp.' END,
       a.consent_at - event.ago
FROM added a CROSS JOIN (VALUES ('SUBMIT', interval '2 days'), ('REJECT', interval '1 day'), ('SUBMIT', interval '0')) event(action, ago)
WHERE a.visibility = 'PENDING' AND (event.ago = interval '0' OR a.user_id = '06000000-0000-4000-8000-000000000001'::uuid)
ORDER BY a.user_id, event.ago DESC;
COMMIT;
