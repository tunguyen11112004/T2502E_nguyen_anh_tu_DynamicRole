INSERT INTO roles (name)
SELECT 'ROLE_STUDENT'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_STUDENT');

INSERT INTO roles (name)
SELECT 'ROLE_LECTURER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_LECTURER');

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, student.id
FROM users u
JOIN roles student ON student.name = 'ROLE_STUDENT'
WHERE EXISTS (
    SELECT 1 FROM users_roles ur
    JOIN roles old_role ON old_role.id = ur.role_id
    WHERE ur.user_id = u.id AND old_role.name = 'ROLE_USER'
)
AND NOT EXISTS (
    SELECT 1 FROM users_roles ur
    JOIN roles admin_role ON admin_role.id = ur.role_id
    WHERE ur.user_id = u.id AND admin_role.name = 'ROLE_ADMIN'
)
AND NOT EXISTS (
    SELECT 1 FROM users_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = student.id
);

CREATE TABLE tmp_admin_users (
    user_id BIGINT PRIMARY KEY
);

INSERT INTO tmp_admin_users (user_id)
SELECT ur.user_id
FROM users_roles ur
JOIN roles admin_role ON admin_role.id = ur.role_id
WHERE admin_role.name = 'ROLE_ADMIN';

DELETE FROM users_roles
WHERE role_id = (SELECT id FROM roles WHERE name = 'ROLE_USER')
AND user_id NOT IN (SELECT user_id FROM tmp_admin_users);

DROP TABLE tmp_admin_users;

CREATE TABLE assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    deadline TIMESTAMP(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    lecturer_id BIGINT NOT NULL,
    CONSTRAINT fk_assignments_lecturer FOREIGN KEY (lecturer_id) REFERENCES users (id),
    CONSTRAINT ck_assignments_status CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED'))
);

CREATE INDEX idx_assignments_lecturer_deadline ON assignments (lecturer_id, deadline);

CREATE TABLE assignment_students (
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    PRIMARY KEY (assignment_id, student_id),
    CONSTRAINT fk_assignment_students_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id),
    CONSTRAINT fk_assignment_students_student FOREIGN KEY (student_id) REFERENCES users (id)
);

CREATE TABLE submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    content VARCHAR(5000) NOT NULL,
    submitted_at TIMESTAMP(6) NOT NULL,
    score DECIMAL(4, 2),
    feedback VARCHAR(2000),
    CONSTRAINT uk_submissions_assignment_student UNIQUE (assignment_id, student_id),
    CONSTRAINT fk_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id),
    CONSTRAINT fk_submissions_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT ck_submissions_score CHECK (score IS NULL OR (score >= 0 AND score <= 10))
);

CREATE INDEX idx_submissions_assignment ON submissions (assignment_id);
