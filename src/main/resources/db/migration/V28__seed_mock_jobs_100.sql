-- 100 việc làm mock cho demo / load test danh sách (Career site).
-- Idempotent: chỉ chèn khi chưa có tin nào có tiêu đề bắt đầu bằng "MOCK — ".

INSERT INTO jobs (
    id,
    title,
    description,
    department,
    required_skills,
    status,
    expires_at,
    industry,
    level,
    job_type,
    salary_type,
    salary_min,
    salary_max,
    salary_currency,
    requirements,
    benefits,
    tags,
    company_name,
    company_logo,
    address,
    city,
    company_size,
    company_id,
    notification_email,
    number_of_positions,
    approval_status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    'MOCK — ' || t.title,
    '<p>' || t.title || ' — mô tả mock cho demo. Tham gia phát triển sản phẩm, làm việc nhóm, báo cáo tiến độ định kỳ.</p>',
    t.dept,
    t.skills,
    'OPEN',
    NOW() + (30 + (n % 45)) * INTERVAL '1 day',
    t.industry,
    t.level,
    t.job_type,
    'range',
    t.smin,
    t.smax,
    'VND',
    '<p>Mock: có kinh nghiệm liên quan, giao tiếp tốt, chủ động.</p>',
    '<p>Mock: BHXH, nghỉ phép, review lương.</p>',
    t.tags,
    t.company,
    NULL,
    t.city || ' — VP mock',
    t.city,
    CASE (n % 4)
        WHEN 0 THEN '50-100'
        WHEN 1 THEN '100-500'
        WHEN 2 THEN '500-1000'
        ELSE '1000+'
    END,
    (SELECT c.id FROM companies c ORDER BY c.name LIMIT 1 OFFSET (
        (n - 1) % GREATEST((SELECT COUNT(*)::int FROM companies), 1)
    )),
    'hr-mock@vthr.local',
    1 + (n % 4),
    'APPROVED',
    NOW() - (n * INTERVAL '3 hours') - ((n % 7) * INTERVAL '1 day'),
    NOW()
FROM generate_series(1, 100) AS n
CROSS JOIN LATERAL (
    SELECT
        (ARRAY[
            'Lập trình viên Java',
            'Kỹ sư Frontend React',
            'Backend Node.js',
            'DevOps Engineer',
            'Data Engineer',
            'Business Analyst',
            'Product Owner',
            'UX/UI Designer',
            'QA Automation',
            'Mobile Flutter',
            'Kỹ sư .NET',
            'Chuyên viên Marketing số',
            'Nhân viên Tuyển dụng',
            'Kế toán tổng hợp',
            'Chuyên viên CSKH',
            'Kỹ sư an ninh mạng',
            'Technical Writer',
            'Scrum Master',
            'Intern lập trình',
            'Trưởng nhóm Backend'
        ])[(n - 1) % 20 + 1] AS title,
        (ARRAY['Công nghệ thông tin', 'Sản xuất', 'Tài chính', 'Logistics', 'Bán lẻ', 'Marketing', 'Nhân sự', 'Giáo dục'])[(n - 1) % 8 + 1] AS industry,
        (ARRAY['Hồ Chí Minh', 'Hà Nội', 'Đà Nẵng', 'Cần Thơ', 'Bình Dương', 'Remote', 'Hải Phòng', 'Huế'])[(n - 1) % 8 + 1] AS city,
        (ARRAY['Intern', 'Fresher', 'Junior', 'Senior', 'Manager'])[(n - 1) % 5 + 1] AS level,
        (ARRAY['Full-time', 'Part-time', 'Internship', 'Freelance'])[(n - 1) % 4 + 1] AS job_type,
        (ARRAY[
            'Phòng Kỹ thuật',
            'Phòng Sản phẩm',
            'Phòng Vận hành',
            'Phòng Kinh doanh',
            'Phòng Nhân sự',
            'Ban Digital'
        ])[(n - 1) % 6 + 1] AS dept,
        (ARRAY[
            'Java, Spring Boot, SQL',
            'React, TypeScript, Tailwind',
            'Node.js, PostgreSQL',
            'Docker, Kubernetes, CI/CD',
            'Python, Spark, Airflow',
            'SQL, Excel, Power BI',
            'Agile, Jira, OKR',
            'Figma, Design system',
            'Selenium, API testing',
            'Dart, REST',
            'C#, Azure',
            'SEO, Ads, Content',
            'ATS, Employer branding',
            'IFRS, MISA',
            'CRM, Zendesk',
            'SIEM, OWASP',
            'Markdown, API docs',
            'Scrum, facilitation',
            'Git, OOP cơ bản',
            'System design, mentoring'
        ])[(n - 1) % 20 + 1] AS skills,
        (ARRAY[
            'VTHR Demo Tech',
            'MegaRetail VN',
            'LogiFast Co.',
            'FinanceHub Asia',
            'EduSpark',
            'GreenFoods',
            'CloudNine SaaS',
            'HealthPlus Digital',
            'AutoParts VN',
            'TravelEase',
            'InsurTech Delta',
            'AgriConnect',
            'SmartFactory VN',
            'MediaWave',
            'BankingCore Labs'
        ])[(n - 1) % 15 + 1] AS company,
        (ARRAY['java', 'react', 'remote', 'senior', 'intern', 'hybrid', 'urgent', 'best employer'])[(n - 1) % 8 + 1]
            || ','
            || (ARRAY['fulltime', 'startup', 'enterprise', 'product'])[(n - 1) % 4 + 1] AS tags,
        (10000000 + (n * 173) % 35000000)::bigint AS smin,
        (25000000 + (n * 311) % 55000000)::bigint AS smax
) AS t
WHERE NOT EXISTS (
    SELECT 1 FROM jobs j WHERE j.title LIKE 'MOCK — %' LIMIT 1
);
