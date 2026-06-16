
INSERT INTO courses (moodle_course_id, course_name, course_code, instructor_id, instructor_name, start_date, end_date, is_active, last_sync_at) VALUES
(1, 'Lập trình Java nâng cao', 'IT301', 1001, 'TS. Trịnh Quốc Việt', '2026-01-15', '2026-06-30', true, CURRENT_TIMESTAMP),
(2, 'Cơ sở dữ liệu', 'IT201', 1002, 'ThS. Nguyễn Văn A', '2026-01-15', '2026-06-30', true, CURRENT_TIMESTAMP),
(3, 'Phát triển ứng dụng Web', 'IT302', 1003, 'ThS. Trần Thị B', '2026-01-15', '2026-06-30', true, CURRENT_TIMESTAMP),
(4, 'Trí tuệ nhân tạo', 'IT401', 1001, 'TS. Trịnh Quốc Việt', '2026-01-15', '2026-06-30', true, CURRENT_TIMESTAMP),
(5, 'Mạng máy tính', 'IT202', 1004, 'ThS. Lê Văn C', '2026-01-15', '2026-06-30', true, CURRENT_TIMESTAMP);

-- INSERT STUDENTS
INSERT INTO students (moodle_user_id, full_name, email, student_code) VALUES
-- RED students (nguy cơ cao)
(1, 'Nguyễn Văn An', 'nguyenvanan@student.edu.vn', '110122001'),
(2, 'Trần Thị Bình', 'tranthib@student.edu.vn', '110122002'),
(3, 'Lê Văn Cường', 'levanc@student.edu.vn', '110122003'),
(4, 'Phạm Thị Dung', 'phamthid@student.edu.vn', '110122004'),

-- YELLOW students (cảnh báo)
(5, 'Hoàng Văn Em', 'hoangvane@student.edu.vn', '110122005'),
(6, 'Vũ Thị Phương', 'vuthif@student.edu.vn', '110122006'),
(7, 'Đặng Văn Giang', 'dangvang@student.edu.vn', '110122007'),
(8, 'Bùi Thị Hoa', 'buithih@student.edu.vn', '110122008'),

-- GREEN students (an toàn)
(9, 'Ngô Văn Ích', 'ngovani@student.edu.vn', '110122009'),
(10, 'Đinh Thị Kim', 'dinhthik@student.edu.vn', '110122010'),
(11, 'Trương Văn Long', 'truongvanl@student.edu.vn', '110122011'),
(12, 'Lý Thị Mai', 'lythim@student.edu.vn', '110122012'),
(13, 'Phan Văn Nam', 'phanvann@student.edu.vn', '110122013'),
(14, 'Võ Thị Oanh', 'vothio@student.edu.vn', '110122014'),
(15, 'Dương Văn Phúc', 'duongvanp@student.edu.vn', '110122015');

-- =====================================================
-- INSERT ENROLLMENTS
-- =====================================================
-- Course 1: Lập trình Java nâng cao
INSERT INTO enrollments (student_id, course_id, enrollment_date, last_access, completion_rate) VALUES
(1, 1, '2026-01-15', '2026-04-10', 25.00),  -- RED: không truy cập 52 ngày
(2, 1, '2026-01-15', '2026-05-25', 35.00),  -- RED: completion thấp
(3, 1, '2026-01-15', '2026-05-30', 40.00),  -- RED
(5, 1, '2026-01-15', '2026-06-01', 65.00),  -- YELLOW
(6, 1, '2026-01-15', '2026-06-01', 70.00),  -- YELLOW
(9, 1, '2026-01-15', '2026-06-01', 95.00),  -- GREEN
(10, 1, '2026-01-15', '2026-06-01', 90.00), -- GREEN
(11, 1, '2026-01-15', '2026-06-01', 88.00), -- GREEN
(12, 1, '2026-01-15', '2026-06-01', 92.00); -- GREEN

-- Course 2: Cơ sở dữ liệu
INSERT INTO enrollments (student_id, course_id, enrollment_date, last_access, completion_rate) VALUES
(2, 2, '2026-01-15', '2026-05-30', 30.00),  -- RED
(4, 2, '2026-01-15', '2026-05-28', 45.00),  -- RED
(7, 2, '2026-01-15', '2026-06-01', 68.00),  -- YELLOW
(8, 2, '2026-01-15', '2026-06-01', 72.00),  -- YELLOW
(13, 2, '2026-01-15', '2026-06-01', 94.00), -- GREEN
(14, 2, '2026-01-15', '2026-06-01', 89.00), -- GREEN
(15, 2, '2026-01-15', '2026-06-01', 91.00); -- GREEN

-- =====================================================
-- INSERT WARNINGS
-- =====================================================
-- RED warnings
INSERT INTO warnings (student_id, course_id, risk_level, grade_average, attendance_rate, completion_rate, last_access_days, reasons, is_acknowledged) VALUES
(1, 1, 'RED', 3.2, 45.00, 25.00, 52, 'Điểm trung bình thấp (3.2); Vắng nhiều (45%); Không truy cập > 14 ngày (52 ngày); Hoàn thành bài tập thấp (25%)', false),
(2, 1, 'RED', 3.8, 65.00, 35.00, 7, 'Điểm trung bình thấp (3.8); Hoàn thành bài tập thấp (35%)', false),
(2, 2, 'RED', 3.5, 70.00, 30.00, 3, 'Điểm trung bình thấp (3.5); Hoàn thành bài tập thấp (30%)', false),
(3, 1, 'RED', 4.0, 55.00, 40.00, 2, 'Điểm trung bình thấp (4.0); Vắng nhiều (55%); Hoàn thành bài tập thấp (40%)', false),
(4, 2, 'RED', 3.9, 60.00, 45.00, 5, 'Điểm trung bình thấp (3.9); Vắng nhiều (60%)', false);

-- YELLOW warnings
INSERT INTO warnings (student_id, course_id, risk_level, grade_average, attendance_rate, completion_rate, last_access_days, reasons, is_acknowledged) VALUES
(5, 1, 'YELLOW', 4.8, 82.00, 65.00, 1, 'Điểm trung bình gần ngưỡng (4.8)', false),
(6, 1, 'YELLOW', 5.2, 78.00, 70.00, 1, 'Chuyên cần cần cải thiện (78%)', false),
(7, 2, 'YELLOW', 4.9, 85.00, 68.00, 1, 'Điểm trung bình gần ngưỡng (4.9)', false),
(8, 2, 'YELLOW', 5.3, 75.00, 72.00, 1, 'Chuyên cần cần cải thiện (75%)', false);

-- GREEN warnings (for statistics)
INSERT INTO warnings (student_id, course_id, risk_level, grade_average, attendance_rate, completion_rate, last_access_days, reasons, is_acknowledged) VALUES
(9, 1, 'GREEN', 8.5, 95.00, 95.00, 1, '', false),
(10, 1, 'GREEN', 8.2, 92.00, 90.00, 1, '', false),
(11, 1, 'GREEN', 7.8, 90.00, 88.00, 1, '', false),
(12, 1, 'GREEN', 8.0, 94.00, 92.00, 1, '', false),
(13, 2, 'GREEN', 8.7, 96.00, 94.00, 1, '', false),
(14, 2, 'GREEN', 7.9, 91.00, 89.00, 1, '', false),
(15, 2, 'GREEN', 8.3, 93.00, 91.00, 1, '', false);

-- =====================================================
-- INSERT CHAT HISTORY (sample)
-- =====================================================
INSERT INTO chat_history (session_id, lecturer_id, role, content, intent) VALUES
('session-001', 1001, 'USER', 'Cho tôi xem danh sách sinh viên nguy cơ', 'LIST_AT_RISK_STUDENTS'),
('session-001', 1001, 'BOT', 'Hiện có 5 sinh viên ở mức nguy cơ cao (RED): Nguyễn Văn An, Trần Thị Bình...', 'LIST_AT_RISK_STUDENTS'),
('session-001', 1001, 'USER', 'Tình trạng sinh viên Nguyễn Văn An như thế nào?', 'CHECK_STUDENT_STATUS'),
('session-001', 1001, 'BOT', 'Sinh viên Nguyễn Văn An đang ở mức nguy cơ cao (RED). Điểm TB: 3.2, Chuyên cần: 45%, Không truy cập 52 ngày.', 'CHECK_STUDENT_STATUS'),
('session-002', 1002, 'USER', 'Thống kê tổng quan', 'GET_STATISTICS'),
('session-002', 1002, 'BOT', 'Tổng số sinh viên: 15. GREEN: 7 (47%), YELLOW: 4 (27%), RED: 4 (27%).', 'GET_STATISTICS');

-- =====================================================
-- VERIFY DATA
-- =====================================================
-- Count records
SELECT 'courses' as table_name, COUNT(*) as count FROM courses
UNION ALL
SELECT 'students', COUNT(*) FROM students
UNION ALL
SELECT 'enrollments', COUNT(*) FROM enrollments
UNION ALL
SELECT 'warnings', COUNT(*) FROM warnings
UNION ALL
SELECT 'chat_history', COUNT(*) FROM chat_history;

-- Summary by risk level
SELECT 
    risk_level,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM warnings), 2) as percentage
FROM warnings
GROUP BY risk_level
ORDER BY 
    CASE risk_level 
        WHEN 'RED' THEN 1 
        WHEN 'YELLOW' THEN 2 
        WHEN 'GREEN' THEN 3 
    END;
