-- Apply catalog corrections after the original V2 so existing databases retain
-- their Flyway checksums, skill IDs and references from evidence/scores.
UPDATE skills SET code = 'CLEAN_CODE', name = 'Clean code', skill_group = 'Foundations'
WHERE code = 'TYPESCRIPT';

UPDATE skills SET code = 'AGILE', name = 'Agile/Scrum', skill_group = 'Soft skills'
WHERE code = 'NEXTJS';

UPDATE skills SET code = 'DSA', name = 'Cấu trúc dữ liệu và giải thuật', skill_group = 'Foundations'
WHERE code = 'DATA_STRUCTURES_ALGORITHMS';

UPDATE skills SET code = 'TESTING', name = 'Kiểm thử phần mềm', skill_group = 'Foundations'
WHERE code = 'UNIT_TESTING';

UPDATE skills SET code = 'SECURITY', name = 'Bảo mật', skill_group = 'DevOps'
WHERE code = 'CI_CD';

UPDATE skills SET code = 'CLOUD', name = 'Điện toán đám mây', skill_group = 'DevOps'
WHERE code = 'PROBLEM_SOLVING';
