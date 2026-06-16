-- =====================================================
-- SCHEMA FOR CHATBOT EARLY WARNING SYSTEM
-- Database: PostgreSQL
-- Author: Nguyễn Đình Nhật Huy
-- =====================================================

-- Drop existing tables if needed
DROP TABLE IF EXISTS chat_history CASCADE;
DROP TABLE IF EXISTS warnings CASCADE;
DROP TABLE IF EXISTS student_grades CASCADE;
DROP TABLE IF EXISTS course_grade_summaries CASCADE;
DROP TABLE IF EXISTS grade_items CASCADE;
DROP TABLE IF EXISTS enrollments CASCADE;
DROP TABLE IF EXISTS courses CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS students CASCADE;

-- =====================================================
-- USERS TABLE (Quản lý tất cả người dùng)
-- =====================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    moodle_user_id BIGINT UNIQUE NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(200) UNIQUE NOT NULL,
    user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('STUDENT', 'TEACHER', 'ADMIN')),
    student_code VARCHAR(50),
    department VARCHAR(200),
    institution VARCHAR(200),
    city VARCHAR(100),
    country VARCHAR(10),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_sync_at TIMESTAMP
);

CREATE INDEX idx_users_moodle_id ON users(moodle_user_id);
CREATE INDEX idx_users_type ON users(user_type);
CREATE INDEX idx_users_student_code ON users(student_code);

-- =====================================================
-- STUDENTS TABLE (Compatibility layer)
-- =====================================================
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    moodle_user_id BIGINT UNIQUE NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(200) NOT NULL,
    student_code VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- COURSES TABLE
-- =====================================================
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    moodle_course_id BIGINT UNIQUE NOT NULL,
    course_name VARCHAR(500) NOT NULL,
    course_code VARCHAR(50),
    instructor_id BIGINT,
    instructor_name VARCHAR(200),
    start_date DATE,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_sync_at TIMESTAMP
);

CREATE INDEX idx_courses_moodle_id ON courses(moodle_course_id);

-- =====================================================
-- GRADE ITEMS TABLE (Các hạng mục điểm)
-- =====================================================
CREATE TABLE grade_items (
    id BIGSERIAL PRIMARY KEY,
    moodle_grade_item_id BIGINT UNIQUE NOT NULL,
    course_id BIGINT NOT NULL,
    item_name VARCHAR(500) NOT NULL,
    item_type VARCHAR(50) NOT NULL CHECK (item_type IN (
        'ASSIGNMENT', 'QUIZ', 'MIDTERM_EXAM', 'FINAL_EXAM', 
        'PROJECT', 'PRESENTATION', 'LAB', 'ATTENDANCE', 'OTHER'
    )),
    max_grade DECIMAL(10, 2) NOT NULL,
    weight_percentage DECIMAL(5, 2),
    due_date TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE INDEX idx_grade_items_course ON grade_items(course_id);
CREATE INDEX idx_grade_items_type ON grade_items(item_type);

-- =====================================================
-- STUDENT GRADES TABLE (Điểm của sinh viên)
-- =====================================================
CREATE TABLE student_grades (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    grade_item_id BIGINT NOT NULL,
    raw_grade DECIMAL(10, 2),
    final_grade DECIMAL(10, 2),
    percentage DECIMAL(5, 2),
    feedback TEXT,
    submission_status VARCHAR(50) CHECK (submission_status IN (
        'NOT_SUBMITTED', 'SUBMITTED', 'GRADED', 'RESUBMITTED', 'LATE'
    )),
    submitted_at TIMESTAMP,
    graded_at TIMESTAMP,
    is_late BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_sync_at TIMESTAMP,
    
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (grade_item_id) REFERENCES grade_items(id) ON DELETE CASCADE,
    UNIQUE (student_id, grade_item_id)
);

CREATE INDEX idx_student_grades_student ON student_grades(student_id);
CREATE INDEX idx_student_grades_item ON student_grades(grade_item_id);
CREATE INDEX idx_student_grades_status ON student_grades(submission_status);

-- =====================================================
-- COURSE GRADE SUMMARIES TABLE (Tổng hợp điểm khóa học)
-- =====================================================
CREATE TABLE course_grade_summaries (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    total_grade DECIMAL(10, 2),
    weighted_average DECIMAL(10, 2),
    letter_grade VARCHAR(5),
    total_assignments INTEGER DEFAULT 0,
    completed_assignments INTEGER DEFAULT 0,
    on_time_submissions INTEGER DEFAULT 0,
    late_submissions INTEGER DEFAULT 0,
    missing_submissions INTEGER DEFAULT 0,
    completion_rate DECIMAL(5, 2),
    on_time_rate DECIMAL(5, 2),
    rank_in_class INTEGER,
    total_students_in_class INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_calculated_at TIMESTAMP,
    
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    UNIQUE (student_id, course_id)
);

CREATE INDEX idx_course_summary_student ON course_grade_summaries(student_id);
CREATE INDEX idx_course_summary_course ON course_grade_summaries(course_id);
CREATE INDEX idx_course_summary_grade ON course_grade_summaries(weighted_average);

-- =====================================================
-- ENROLLMENTS TABLE
-- =====================================================
CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrollment_date DATE NOT NULL,
    last_access TIMESTAMP,
    completion_rate DECIMAL(5, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    UNIQUE (student_id, course_id)
);

CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_course ON enrollments(course_id);

-- =====================================================
-- WARNINGS TABLE
-- =====================================================
CREATE TABLE warnings (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    risk_level VARCHAR(10) NOT NULL CHECK (risk_level IN ('GREEN', 'YELLOW', 'RED')),
    grade_average DECIMAL(5, 2),
    attendance_rate DECIMAL(5, 2),
    completion_rate DECIMAL(5, 2),
    last_access_days INTEGER,
    reasons TEXT,
    is_acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE INDEX idx_warnings_student ON warnings(student_id);
CREATE INDEX idx_warnings_course ON warnings(course_id);
CREATE INDEX idx_warnings_risk_level ON warnings(risk_level);
CREATE INDEX idx_warnings_created ON warnings(created_at DESC);

-- =====================================================
-- CHAT HISTORY TABLE
-- =====================================================
CREATE TABLE chat_history (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    lecturer_id BIGINT,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'BOT')),
    content TEXT NOT NULL,
    intent VARCHAR(100),
    confidence DECIMAL(5, 4),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_session ON chat_history(session_id);
CREATE INDEX idx_chat_lecturer ON chat_history(lecturer_id);
CREATE INDEX idx_chat_created ON chat_history(created_at DESC);

-- =====================================================
-- VIEWS FOR REPORTING
-- =====================================================

-- View: Student Performance Overview
CREATE OR REPLACE VIEW v_student_performance AS
SELECT 
    u.id as student_id,
    u.full_name,
    u.student_code,
    u.email,
    COUNT(DISTINCT cgs.course_id) as enrolled_courses,
    AVG(cgs.weighted_average) as overall_average,
    AVG(cgs.completion_rate) as avg_completion_rate,
    COUNT(CASE WHEN w.risk_level = 'RED' THEN 1 END) as red_warnings,
    COUNT(CASE WHEN w.risk_level = 'YELLOW' THEN 1 END) as yellow_warnings
FROM users u
LEFT JOIN course_grade_summaries cgs ON u.id = cgs.student_id
LEFT JOIN warnings w ON u.id = w.student_id
WHERE u.user_type = 'STUDENT' AND u.is_active = TRUE
GROUP BY u.id, u.full_name, u.student_code, u.email;

-- View: Course Performance Overview
CREATE OR REPLACE VIEW v_course_performance AS
SELECT 
    c.id as course_id,
    c.course_name,
    c.course_code,
    c.instructor_name,
    COUNT(DISTINCT cgs.student_id) as total_students,
    AVG(cgs.weighted_average) as class_average,
    AVG(cgs.completion_rate) as avg_completion_rate,
    COUNT(CASE WHEN w.risk_level = 'RED' THEN 1 END) as students_at_risk,
    COUNT(CASE WHEN w.risk_level = 'YELLOW' THEN 1 END) as students_warned
FROM courses c
LEFT JOIN course_grade_summaries cgs ON c.id = cgs.course_id
LEFT JOIN warnings w ON c.id = w.course_id
WHERE c.is_active = TRUE
GROUP BY c.id, c.course_name, c.course_code, c.instructor_name;

COMMENT ON TABLE users IS 'Quản lý tất cả người dùng trong hệ thống';
COMMENT ON TABLE grade_items IS 'Các hạng mục điểm: bài tập, quiz, thi';
COMMENT ON TABLE student_grades IS 'Điểm chi tiết của sinh viên cho từng hạng mục';
COMMENT ON TABLE course_grade_summaries IS 'Tổng hợp điểm và thống kê của sinh viên trong khóa học';
