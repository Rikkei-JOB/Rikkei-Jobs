-- profile-service V15 bỏ kỹ năng ENGLISH khỏi danh mục (ngoại ngữ thành mục riêng của hồ sơ, không tính độ khớp).
-- Xoá yêu cầu ENGLISH khỏi JD và đánh dấu điểm khớp đã lưu của các JD đó là cũ để tính lại.
UPDATE match_scores SET stale = TRUE
WHERE job_id IN (SELECT job_id FROM job_skills WHERE skill_id = '00000000-0000-0000-0001-000000000017');
DELETE FROM job_skills WHERE skill_id = '00000000-0000-0000-0001-000000000017';
