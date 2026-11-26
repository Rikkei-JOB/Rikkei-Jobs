-- Disposable local demo DB only. Requires identity/company dev seed and service Flyway migrations.
-- Reruns preserve existing requests and decisions. No credentials or contacts are copied into audit logs.
BEGIN;
DO $$ BEGIN
  IF (SELECT count(*) FROM identity.users WHERE email IN ('candidate1@rikkeijobs.test','candidate2@rikkeijobs.test','candidate3@rikkeijobs.test')) <> 3
  THEN RAISE EXCEPTION 'Run identity dev seed first'; END IF;
  IF NOT EXISTS (SELECT 1 FROM company.jobs WHERE company_id='00000000-0000-0000-0000-0000000000a1')
  THEN RAISE EXCEPTION 'Run company dev seed first'; END IF;
END $$;
INSERT INTO profile.candidate_profiles(user_id,full_name,email,phone,program,class_code,course_name,visibility,updated_at)
SELECT id,full_name,email,'0901234567','Lập trình','UT03-DEMO','Java 2026','PUBLIC',now()
FROM identity.users WHERE email IN ('candidate1@rikkeijobs.test','candidate2@rikkeijobs.test','candidate3@rikkeijobs.test')
ON CONFLICT(user_id) DO NOTHING;
INSERT INTO recruitment.applications(id,job_id,user_id,company_id,job_title,company_name,cv_id,cv_snapshot_url,cover_letter,status,created_at,updated_at)
SELECT '03000000-0000-4000-8000-000000000001',j.id,u.id,j.company_id,j.title,c.name,
'03000000-0000-4000-8000-000000000099','/demo/ut03-no-cv','Fixture lịch sử ứng tuyển; không có tệp CV.','WITHDRAWN',now(),now()
FROM identity.users u CROSS JOIN LATERAL (SELECT * FROM company.jobs WHERE company_id='00000000-0000-0000-0000-0000000000a1' ORDER BY id LIMIT 1) j
JOIN company.companies c ON c.id=j.company_id WHERE u.email='candidate1@rikkeijobs.test'
ON CONFLICT(id) DO NOTHING;
WITH fixtures(id,email,status,created_at) AS (VALUES
('03000000-0000-4000-8000-000000000011'::uuid,'candidate2@rikkeijobs.test','PENDING',now()),
('03000000-0000-4000-8000-000000000012'::uuid,'candidate3@rikkeijobs.test','APPROVED',now()-interval '1 day'),
('03000000-0000-4000-8000-000000000013'::uuid,'candidate2@rikkeijobs.test','REJECTED',now()-interval '2 days'))
INSERT INTO recruitment.contact_requests(id,company_id,user_id,job_id,reason,status,created_by,created_at,reviewed_by,reviewed_at,rejection_reason)
SELECT f.id,j.company_id,u.id,j.id,'Cần trao đổi vị trí Java.',f.status,'00000000-0000-0000-0001-000000000001',f.created_at,
CASE WHEN f.status<>'PENDING' THEN '00000000-0000-0000-0001-000000000005'::uuid END,
CASE WHEN f.status<>'PENDING' THEN f.created_at+interval '1 hour' END,
CASE WHEN f.status='REJECTED' THEN 'Cần bổ sung mục đích tuyển dụng.' END
FROM fixtures f JOIN identity.users u ON u.email=f.email
CROSS JOIN LATERAL (SELECT id,company_id FROM company.jobs WHERE company_id='00000000-0000-0000-0000-0000000000a1' ORDER BY id LIMIT 1) j
ON CONFLICT DO NOTHING;
COMMIT;
-- If a sample already had a PRIVATE profile, publish it via HS-05 before demoing a new request.
SELECT u.id,u.email,p.visibility FROM identity.users u LEFT JOIN profile.candidate_profiles p ON p.user_id=u.id
WHERE u.email LIKE 'candidate%@rikkeijobs.test' ORDER BY u.email;
