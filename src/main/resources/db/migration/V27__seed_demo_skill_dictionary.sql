-- Dữ liệu mẫu master data: nhóm kỹ năng + kỹ năng (demo Admin → Master data Skills).
-- ON CONFLICT (name): bỏ qua nếu tên đã tồn tại.

INSERT INTO skill_categories (id, name, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'Lập trình', NOW(), NOW()),
    (gen_random_uuid(), 'Frontend', NOW(), NOW()),
    (gen_random_uuid(), 'Backend & DevOps', NOW(), NOW()),
    (gen_random_uuid(), 'Cơ sở dữ liệu', NOW(), NOW()),
    (gen_random_uuid(), 'Mobile', NOW(), NOW()),
    (gen_random_uuid(), 'Kỹ năng mềm & công cụ', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Gắn skill theo tên category hiện có trong DB (kể cả vừa insert hoặc đã có từ trước).
INSERT INTO skills (id, category_id, name, created_at, updated_at)
SELECT gen_random_uuid(), c.id, v.skill_name, NOW(), NOW()
FROM (VALUES
    ('Lập trình', 'Java'),
    ('Lập trình', 'Kotlin'),
    ('Lập trình', 'Python'),
    ('Lập trình', 'JavaScript'),
    ('Lập trình', 'TypeScript'),
    ('Lập trình', 'Go'),
    ('Lập trình', 'C#'),
    ('Frontend', 'React'),
    ('Frontend', 'Vue.js'),
    ('Frontend', 'Angular'),
    ('Frontend', 'HTML & CSS'),
    ('Frontend', 'Tailwind CSS'),
    ('Backend & DevOps', 'Spring Boot'),
    ('Backend & DevOps', 'Node.js'),
    ('Backend & DevOps', 'Docker'),
    ('Backend & DevOps', 'Kubernetes'),
    ('Backend & DevOps', 'REST API'),
    ('Backend & DevOps', 'Git'),
    ('Cơ sở dữ liệu', 'PostgreSQL'),
    ('Cơ sở dữ liệu', 'MySQL'),
    ('Cơ sở dữ liệu', 'Redis'),
    ('Cơ sở dữ liệu', 'MongoDB'),
    ('Mobile', 'Android'),
    ('Mobile', 'iOS'),
    ('Mobile', 'Flutter'),
    ('Mobile', 'React Native'),
    ('Kỹ năng mềm & công cụ', 'Agile / Scrum'),
    ('Kỹ năng mềm & công cụ', 'Jira'),
    ('Kỹ năng mềm & công cụ', 'Tiếng Anh giao tiếp'),
    ('Kỹ năng mềm & công cụ', 'Làm việc nhóm')
) AS v(category_name, skill_name)
JOIN skill_categories c ON c.name = v.category_name
ON CONFLICT (name) DO NOTHING;
