<?php
/**
 * Script kiểm tra và hiển thị phân quyền trong Moodle
 * 
 * Chạy: php check_roles.php
 */

define('CLI_SCRIPT', true);
require_once(__DIR__ . '/config.php');
require_once($CFG->libdir . '/adminlib.php');

echo "=== KIỂM TRA PHÂN QUYỀN MOODLE ===\n\n";

global $DB;

// Lấy danh sách roles
$roles = $DB->get_records('role', null, 'sortorder');

echo "Các role trong hệ thống:\n";
echo str_repeat("=", 70) . "\n";

foreach ($roles as $role) {
    echo sprintf("%-20s | %-30s | ID: %d\n", 
        $role->shortname, 
        $role->name, 
        $role->id
    );
}

echo "\n";

// Kiểm tra role cần thiết cho chatbot
$required_roles = [
    'manager' => 'Quản trị viên (Admin)',
    'editingteacher' => 'Giáo viên (Teacher)',
    'teacher' => 'Giáo viên không chỉnh sửa',
    'student' => 'Sinh viên (Student)',
];

echo "Kiểm tra roles quan trọng:\n";
echo str_repeat("=", 70) . "\n";

foreach ($required_roles as $shortname => $description) {
    $role = $DB->get_record('role', ['shortname' => $shortname]);
    
    if ($role) {
        echo "✓ $shortname: Tồn tại (ID: $role->id) - $description\n";
        
        // Đếm số user có role này
        $count = $DB->count_records_sql(
            "SELECT COUNT(DISTINCT ra.userid) 
             FROM {role_assignments} ra 
             WHERE ra.roleid = ?", 
            [$role->id]
        );
        echo "  → Có $count người dùng\n";
    } else {
        echo "✗ $shortname: Không tồn tại - $description\n";
    }
    echo "\n";
}

// Kiểm tra context levels
echo "Context Levels:\n";
echo str_repeat("=", 70) . "\n";
echo "  CONTEXT_SYSTEM (10): Toàn hệ thống\n";
echo "  CONTEXT_USER (30): User cá nhân\n";
echo "  CONTEXT_COURSECAT (40): Danh mục khóa học\n";
echo "  CONTEXT_COURSE (50): Khóa học\n";
echo "  CONTEXT_MODULE (70): Module/Activity\n";
echo "  CONTEXT_BLOCK (80): Block\n\n";

// Gợi ý phân quyền cho chatbot
echo "Phân quyền đề xuất cho EduGuard Chatbot:\n";
echo str_repeat("=", 70) . "\n";
echo "1. ADMIN (manager):\n";
echo "   - Xem toàn bộ dữ liệu hệ thống\n";
echo "   - Quản lý người dùng, khóa học\n";
echo "   - Xem báo cáo tổng hợp\n\n";

echo "2. GIÁO VIÊN (editingteacher):\n";
echo "   - Xem điểm sinh viên trong lớp mình dạy\n";
echo "   - Xem chuyên cần của lớp\n";
echo "   - Xem danh sách sinh viên nguy cơ\n";
echo "   - Không xem được dữ liệu lớp khác\n\n";

echo "3. CỐ VẤN HỌC TẬP (editingteacher trong lớp):\n";
echo "   - Xem điểm tất cả môn của sinh viên lớp mình\n";
echo "   - Xem chuyên cần toàn bộ lớp\n";
echo "   - Theo dõi sinh viên nguy cơ trong lớp\n";
echo "   - Xem báo cáo tiến độ học tập\n\n";

echo "4. SINH VIÊN (student):\n";
echo "   - Xem điểm của chính mình\n";
echo "   - Xem lịch sử chuyên cần\n";
echo "   - Không xem được dữ liệu sinh viên khác\n\n";

echo "=== HOÀN TẤT ===\n";
