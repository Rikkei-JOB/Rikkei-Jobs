-- Bỏ kỹ năng ENGLISH khỏi danh mục: ngoại ngữ giờ là mục riêng trong hồ sơ (V14, trình độ CEFR),
-- không tính vào độ khớp. Không có ánh xạ LMS/thi nào trỏ tới ENGLISH, nên chỉ mất điểm tự khai và
-- bằng chứng tự khai/mentor của kỹ năng này. company-service xoá yêu cầu ENGLISH khỏi tin tuyển dụng.
DELETE FROM skill_scores WHERE skill_id = '00000000-0000-0000-0001-000000000017';
DELETE FROM skill_evidences WHERE skill_id = '00000000-0000-0000-0001-000000000017';
DELETE FROM skill_mappings WHERE skill_id = '00000000-0000-0000-0001-000000000017';
DELETE FROM skills WHERE id = '00000000-0000-0000-0001-000000000017';
