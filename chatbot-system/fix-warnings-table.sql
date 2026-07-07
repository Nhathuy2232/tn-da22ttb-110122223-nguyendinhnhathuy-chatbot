-- Fix warnings table structure
-- Xóa bảng cũ và tạo lại với cấu trúc mới (không có foreign key)

USE moodle;

-- Drop table nếu tồn tại
DROP TABLE IF EXISTS warnings;

-- Tạo lại bảng với cấu trúc mới (chỉ dùng Long ID, không có relationship)
CREATE TABLE warnings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT,
    risk_level VARCHAR(20) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'GREEN',
    warning_type VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    message TEXT NOT NULL,
    grade_average DOUBLE,
    attendance_rate DOUBLE,
    completion_rate DOUBLE,
    last_access_days INT,
    reasons TEXT,
    is_acknowledged BOOLEAN,
    acknowledged_by BIGINT,
    acknowledged_at DATETIME,
    is_sent BOOLEAN DEFAULT FALSE,
    sent_at DATETIME,
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    detected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id),
    INDEX idx_risk_level (risk_level),
    INDEX idx_detected_at (detected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Xong! Bảng warnings đã sẵn sàng
SELECT 'Bảng warnings đã được tạo lại thành công!' AS status;
