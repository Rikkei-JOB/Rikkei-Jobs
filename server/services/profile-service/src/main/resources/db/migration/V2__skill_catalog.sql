-- Repeatable catalog: stable team skill codes; group labels and mappings are sample data.
INSERT INTO skills (id, code, name, skill_group)
SELECT CAST(seed.id AS UUID), seed.code, seed.name, seed.skill_group
FROM (VALUES
    ('00000000-0000-0000-0001-000000000001', 'JAVA', 'Java', 'Backend'),
    ('00000000-0000-0000-0001-000000000002', 'SPRING_BOOT', 'Spring Boot', 'Backend'),
    ('00000000-0000-0000-0001-000000000003', 'SQL', 'SQL', 'Backend'),
    ('00000000-0000-0000-0001-000000000004', 'GIT', 'Git', 'DevOps'),
    ('00000000-0000-0000-0001-000000000005', 'REST_API', 'REST API', 'Backend'),
    ('00000000-0000-0000-0001-000000000006', 'HTML_CSS', 'HTML/CSS', 'Frontend'),
    ('00000000-0000-0000-0001-000000000007', 'JAVASCRIPT', 'JavaScript', 'Frontend'),
    ('00000000-0000-0000-0001-000000000008', 'TYPESCRIPT', 'TypeScript', 'Frontend'),
    ('00000000-0000-0000-0001-000000000009', 'REACTJS', 'ReactJS', 'Frontend'),
    ('00000000-0000-0000-0001-000000000010', 'NEXTJS', 'Next.js', 'Frontend'),
    ('00000000-0000-0000-0001-000000000011', 'DOCKER', 'Docker', 'DevOps'),
    ('00000000-0000-0000-0001-000000000012', 'LINUX', 'Linux', 'DevOps'),
    ('00000000-0000-0000-0001-000000000013', 'OOP', 'Lập trình hướng đối tượng', 'Foundations'),
    ('00000000-0000-0000-0001-000000000014', 'DATA_STRUCTURES_ALGORITHMS', 'Cấu trúc dữ liệu và giải thuật', 'Foundations'),
    ('00000000-0000-0000-0001-000000000015', 'UNIT_TESTING', 'Kiểm thử đơn vị', 'Foundations'),
    ('00000000-0000-0000-0001-000000000016', 'CI_CD', 'CI/CD', 'DevOps'),
    ('00000000-0000-0000-0001-000000000017', 'ENGLISH', 'Tiếng Anh', 'Soft skills'),
    ('00000000-0000-0000-0001-000000000018', 'COMMUNICATION', 'Giao tiếp', 'Soft skills'),
    ('00000000-0000-0000-0001-000000000019', 'TEAMWORK', 'Làm việc nhóm', 'Soft skills'),
    ('00000000-0000-0000-0001-000000000020', 'PROBLEM_SOLVING', 'Giải quyết vấn đề', 'Soft skills')
) AS seed(id, code, name, skill_group)
WHERE NOT EXISTS (SELECT 1 FROM skills existing WHERE existing.code = seed.code);

INSERT INTO skill_mappings (id, source, source_code, skill_id, weight)
SELECT CAST(seed.id AS UUID), seed.source, seed.source_code, skill.id, seed.weight
FROM (VALUES
    ('00000000-0000-0000-0002-000000000001', 'LMS', 'JAVA01', 'JAVA', 0.7),
    ('00000000-0000-0000-0002-000000000002', 'LMS', 'JAVA01', 'SQL', 0.3),
    ('00000000-0000-0000-0002-000000000003', 'LMS', 'SQL01', 'SQL', 1.0),
    ('00000000-0000-0000-0002-000000000004', 'LMS', 'SPRING01', 'SPRING_BOOT', 0.7),
    ('00000000-0000-0000-0002-000000000005', 'LMS', 'SPRING01', 'REST_API', 0.3),
    ('00000000-0000-0000-0002-000000000006', 'LMS', 'WEB01', 'HTML_CSS', 0.5),
    ('00000000-0000-0000-0002-000000000007', 'LMS', 'WEB01', 'JAVASCRIPT', 0.5),
    ('00000000-0000-0000-0002-000000000008', 'LMS', 'GIT01', 'GIT', 0.7),
    ('00000000-0000-0000-0002-000000000009', 'LMS', 'GIT01', 'TEAMWORK', 0.3),
    ('00000000-0000-0000-0002-000000000010', 'LMS', 'REACT01', 'REACTJS', 0.7),
    ('00000000-0000-0000-0002-000000000011', 'LMS', 'REACT01', 'JAVASCRIPT', 0.3),
    ('00000000-0000-0000-0002-000000000012', 'EXAM', 'JAVA01', 'JAVA', 1.0),
    ('00000000-0000-0000-0002-000000000013', 'EXAM', 'REACT01', 'REACTJS', 1.0),
    ('00000000-0000-0000-0002-000000000014', 'PM', 'API_BACKEND', 'REST_API', 0.6),
    ('00000000-0000-0000-0002-000000000015', 'PM', 'API_BACKEND', 'SPRING_BOOT', 0.4)
) AS seed(id, source, source_code, skill_code, weight)
JOIN skills skill ON skill.code = seed.skill_code
WHERE NOT EXISTS (SELECT 1 FROM skill_mappings existing
    WHERE existing.source = seed.source AND existing.source_code = seed.source_code
        AND existing.skill_id = skill.id);
