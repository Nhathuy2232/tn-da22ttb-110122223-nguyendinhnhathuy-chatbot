<?php
/**
 * Script tạo và cấu hình 4 role cho hệ thống EduGuard Chatbot:
 *  1. Sinh viên (Student)         - sử dụng role 'student' có sẵn
 *  2. Giảng viên (Lecturer)      - sử dụng role 'editingteacher' có sẵn
 *  3. Cố vấn học tập (Adviser)   - TẠO MỚI role 'academicadviser'
 *  4. Quản trị viên (Admin)      - sử dụng role 'manager' có sẵn
 *
 * Phương án: HYBRID - tận dụng role Moodle có sẵn + tạo mới Adviser
 *
 * Chạy:  php install_roles.php
 * hoặc:  http://localhost/moodle/install_roles.php  (CLI: define('CLI_SCRIPT', true))
 */

define('CLI_SCRIPT', true);
require_once(__DIR__ . '/config.php');
require_once($CFG->libdir . '/accesslib.php');
require_once($CFG->libdir . '/moodlelib.php');

// CLI mode không cần đăng nhập
if (CLI_SCRIPT) {
    $syscontext = context_system::instance();
    $USER = get_admin(); // Pseudo-user cho CLI
} else {
    require_login();
    require_capability('moodle/site:config', context_system::instance());
}

echo "\n=== CÀI ĐẶT PHÂN QUYỀN EDUGUARD CHATBOT ===\n\n";

global $DB;

/**
 * Hàm tạo role mới với archetype
 */
function create_or_update_role($shortname, $name, $description, $archetype = '') {
    global $DB;

    $role = $DB->get_record('role', ['shortname' => $shortname]);

    if ($role) {
        echo "  ↻ Role '$shortname' đã tồn tại (ID: {$role->id}), đang cập nhật...\n";
        $role->name = $name;
        $role->description = $description;
        if ($archetype) {
            $role->archetype = $archetype;
        }
        $DB->update_record('role', $role);
    } else {
        // Tìm sortorder cao nhất
        $max = $DB->get_record_sql("SELECT MAX(sortorder) AS m FROM {role}");
        $sortorder = ($max->m ?? 0) + 1;

        $roleid = create_role($name, $shortname, $description, $archetype);
        echo "  ✓ Tạo mới role '$shortname' (ID: $roleid)\n";
        $role = $DB->get_record('role', ['id' => $roleid]);
    }

    return $role;
}

/**
 * Hàm gán một capability cho role ở context system
 */
function assign_capability_to_role($roleid, $capability, $permission = CAP_ALLOW) {
    global $DB;

    $existing = $DB->get_record('role_capabilities', [
        'contextid' => 1, // system context
        'roleid'    => $roleid,
        'capability'=> $capability
    ]);

    if ($existing) {
        $existing->permission = $permission;
        $existing->timemodified = time();
        $DB->update_record('role_capabilities', $existing);
    } else {
        $record = new stdClass();
        $record->contextid = 1;
        $record->roleid = $roleid;
        $record->capability = $capability;
        $record->permission = $permission;
        $record->timemodified = time();
        $record->modifierid = 2; // admin user
        $DB->insert_record('role_capabilities', $record);
    }
}

// ============================================================
// 1. SINH VIÊN (Student) - dùng role 'student' có sẵn
// ============================================================
echo "1. SINH VIÊN (Student):\n";
$student = $DB->get_record('role', ['shortname' => 'student']);
if ($student) {
    echo "  ✓ Sử dụng role 'student' có sẵn (ID: {$student->id})\n";
    // Đảm bảo các capability cơ bản cho student với chatbot
    assign_capability_to_role($student->id, 'moodle/site:viewuseridentity', CAP_ALLOW);
} else {
    echo "  ✗ Không tìm thấy role student - tạo mới\n";
    $student = create_or_update_role('student', 'Sinh viên', 'Sinh viên trong hệ thống', 'student');
}
echo "\n";

// ============================================================
// 2. GIẢNG VIÊN (Lecturer) - dùng role 'editingteacher' có sẵn
// ============================================================
echo "2. GIẢNG VIÊN (Lecturer):\n";
$lecturer = $DB->get_record('role', ['shortname' => 'editingteacher']);
if ($lecturer) {
    echo "  ✓ Sử dụng role 'editingteacher' có sẵn (ID: {$lecturer->id})\n";
} else {
    echo "  ✗ Không tìm thấy role editingteacher - tạo mới\n";
    $lecturer = create_or_update_role(
        'editingteacher',
        'Giảng viên',
        'Giảng viên có quyền chỉnh sửa khóa học, điểm danh, nhập điểm',
        'editingteacher'
    );
}
echo "\n";

// ============================================================
// 3. CỐ VẤN HỌC TẬP (Academic Adviser) - TẠO MỚI
// ============================================================
echo "3. CỐ VẤN HỌC TẬP (Academic Adviser):\n";
$adviser = create_or_update_role(
    'academicadviser',
    'Cố vấn học tập',
    'Cố vấn học tập - theo dõi tiến độ học tập của sinh viên trong lớp chủ nhiệm, '
        . 'xem điểm tất cả các môn, cảnh báo sớm nguy cơ học tập. '
        . 'Có quyền xem dữ liệu sinh viên trong lớp chủ nhiệm nhưng không chỉnh sửa điểm.',
    'teacher' // Kế thừa từ teacher để có quyền xem khóa học
);

// Gán các capability cho Cố vấn học tập
echo "  → Gán quyền cho Cố vấn học tập...\n";

// Quyền xem (view) - quan trọng nhất
$view_caps = [
    'moodle/site:viewuseridentity',   // Xem thông tin user
    'moodle/user:viewdetails',        // Xem chi tiết user
    'moodle/user:viewalldetails',     // Xem tất cả chi tiết (kể cả profile ẩn)
    'moodle/course:view',             // Xem khóa học
    'moodle/course:viewparticipants', // Xem danh sách tham gia
    'moodle/course:viewhiddencourses',
    'moodle/grade:viewall',           // Xem tất cả điểm (của lớp mình)
    'moodle/grade:view',              // Xem điểm cá nhân
    'moodle/grade:readall',           // Đọc tất cả grade items
    'moodle/grade:export',            // Export điểm
    'moodle/site:viewreports',        // Xem báo cáo
    'moodle/site:viewanonymised',     // Xem dữ liệu ẩn danh
    'mod/assign:view',                // Xem bài tập
    'mod/quiz:view',                  // Xem bài kiểm tra
    'mod/attendance:viewreports',     // Xem báo cáo điểm danh
    'mod/attendance:view',            // Xem session điểm danh
    'mod/forum:viewdiscussion',       // Xem diễn đàn
    'report/outline:view',            // Báo cáo outline khóa học
    'report/participation:view',      // Báo cáo tham gia
    'report/log:view',                // Xem log
];

foreach ($view_caps as $cap) {
    assign_capability_to_role($adviser->id, $cap, CAP_ALLOW);
}

// Quyền KHÔNG cho phép (Prevent) - không được chỉnh sửa điểm
$deny_caps = [
    'moodle/grade:edit',              // Không sửa điểm
    'moodle/grade:manage',            // Không quản lý gradebook
    'moodle/grade:delete',            // Không xóa điểm
    'mod/assign:grade',               // Không chấm bài
    'mod/quiz:grade',                 // Không chấm quiz
];

foreach ($deny_caps as $cap) {
    assign_capability_to_role($adviser->id, $cap, CAP_PREVENT);
}

// Cho phép Cố vấn truy cập chatbot
assign_capability_to_role($adviser->id, 'local/eduguard:view', CAP_ALLOW);
assign_capability_to_role($adviser->id, 'local/eduguard:usereports', CAP_ALLOW);

echo "  ✓ Đã gán " . (count($view_caps) + count($deny_caps) + 2) . " quyền cho Cố vấn\n";
echo "\n";

// ============================================================
// 4. QUẢN TRỊ VIÊN (Admin) - dùng role 'manager' có sẵn
// ============================================================
echo "4. QUẢN TRỊ VIÊN (Admin):\n";
$admin = $DB->get_record('role', ['shortname' => 'manager']);
if ($admin) {
    echo "  ✓ Sử dụng role 'manager' có sẵn (ID: {$admin->id})\n";
} else {
    echo "  ✗ Không tìm thấy role manager - tạo mới\n";
    $admin = create_or_update_role('manager', 'Quản trị viên', 'Quản trị hệ thống', 'manager');
}
echo "\n";

// ============================================================
// 5. CẬP NHẬT CONTEXT LEVELS cho role Adviser
// ============================================================
echo "5. CẬP NHẬT CONTEXT LEVELS cho Cố vấn học tập:\n";
$ctxlevels = [
    CONTEXT_SYSTEM => 'Hệ thống',
    CONTEXT_USER => 'User',
    CONTEXT_COURSECAT => 'Danh mục khóa học',
    CONTEXT_COURSE => 'Khóa học',
    CONTEXT_MODULE => 'Module',
    CONTEXT_BLOCK => 'Block',
];

foreach ($ctxlevels as $ctx => $label) {
    $exists = $DB->get_record('role_context_levels', [
        'roleid' => $adviser->id,
        'contextlevel' => $ctx
    ]);
    if (!$exists) {
        $rec = new stdClass();
        $rec->roleid = $adviser->id;
        $rec->contextlevel = $ctx;
        $DB->insert_record('role_context_levels', $rec);
        echo "  + Thêm context level: $label\n";
    }
}
echo "\n";

// ============================================================
// 6. XÁC NHẬN
// ============================================================
echo "6. TỔNG KẾT:\n";
echo str_repeat("=", 70) . "\n";

$final_roles = [
    'student'         => 'Sinh viên',
    'editingteacher'  => 'Giảng viên',
    'academicadviser' => 'Cố vấn học tập',
    'manager'         => 'Quản trị viên',
];

foreach ($final_roles as $sn => $label) {
    $r = $DB->get_record('role', ['shortname' => $sn]);
    if ($r) {
        $capcount = $DB->count_records('role_capabilities', ['roleid' => $r->id]);
        $usercount = $DB->count_records_sql(
            "SELECT COUNT(DISTINCT userid) FROM {role_assignments} WHERE roleid = ?",
            [$r->id]
        );
        printf("  [%-18s] %-20s | ID:%2d | %d quyền | %d users\n",
            $sn, $label, $r->id, $capcount, $usercount);
    }
}

echo "\n=== HOÀN TẤT CÀI ĐẶT ===\n";
echo "Bạn có thể chạy: php check_roles_status.php để kiểm tra lại.\n\n";
