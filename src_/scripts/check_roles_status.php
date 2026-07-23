<?php
/**
 * Script kiểm tra trạng thái các role trong Moodle
 * Chạy: php check_roles_status.php
 *
 * @package    local_chatbot
 */

define('CLI_SCRIPT', true);
require_once(__DIR__ . '/config.php');
require_once($CFG->libdir . '/accesslib.php');

echo "\n=== KIỂM TRA TRẠNG THÁI ROLES MOODLE ===\n\n";

global $DB;

// 1. Liệt kê tất cả roles hiện có
echo "1. TẤT CẢ CÁC ROLE TRONG HỆ THỐNG:\n";
echo str_repeat("-", 80) . "\n";
$roles = $DB->get_records('role', null, 'sortorder ASC');
foreach ($roles as $role) {
    $archetype = $role->archetype ?: '-';
    printf("  [ID:%3d] %-25s | %-30s | archetype: %s\n",
        $role->id, $role->shortname, $role->name, $archetype);
}
echo "\n";

// 2. Kiểm tra role Cố vấn học tập
echo "2. KIỂM TRA ROLE CỐ VẤN HỌC TẬP:\n";
echo str_repeat("-", 80) . "\n";
$adviser = $DB->get_record('role', ['shortname' => 'academicadviser']);
$adviser2 = $DB->get_record('role', ['shortname' => 'adviser']);
$adviser3 = $DB->get_record('role', ['shortname' => 'covan']);

if ($adviser) {
    echo "  ✓ Tìm thấy role 'academicadviser' (ID: {$adviser->id})\n";
} elseif ($adviser2) {
    echo "  ✓ Tìm thấy role 'adviser' (ID: {$adviser2->id})\n";
} elseif ($adviser3) {
    echo "  ✓ Tìm thấy role 'covan' (ID: {$adviser3->id})\n";
} else {
    echo "  ✗ CHƯA CÓ role Cố vấn học tập - CẦN TẠO MỚI\n";
    echo "    (Có thể chạy install_roles.php để tạo)\n";
}
echo "\n";

// 3. Kiểm tra role cốt lõi cần cho chatbot
echo "3. CÁC ROLE CỐT LÕI CHO CHATBOT:\n";
echo str_repeat("-", 80) . "\n";
$core = [
    'manager'        => 'ADMIN (Quản trị hệ thống)',
    'editingteacher' => 'LECTURER (Giảng viên - có quyền chỉnh sửa)',
    'teacher'        => 'LECTURER (Giảng viên - không chỉnh sửa)',
    'student'        => 'STUDENT (Sinh viên)',
];
foreach ($core as $shortname => $desc) {
    $role = $DB->get_record('role', ['shortname' => $shortname]);
    if ($role) {
        $count = $DB->count_records_sql(
            "SELECT COUNT(DISTINCT ra.userid) FROM {role_assignments} ra WHERE ra.roleid = ?",
            [$role->id]
        );
        echo "  ✓ $shortname (ID:{$role->id}) - $desc - Có $count người dùng\n";
    } else {
        echo "  ✗ $shortname - $desc - KHÔNG TỒN TẠI\n";
    }
}
echo "\n";

// 4. Thống kê user theo role
echo "4. TOP 10 USERS THEO SỐ ROLE:\n";
echo str_repeat("-", 80) . "\n";
$sql = "SELECT u.id, u.username, u.firstname, u.lastname, COUNT(ra.id) AS rolecount
          FROM {user} u
          JOIN {role_assignments} ra ON ra.userid = u.id
          WHERE u.deleted = 0 AND u.suspended = 0
          GROUP BY u.id, u.username, u.firstname, u.lastname
          ORDER BY rolecount DESC
          LIMIT 10";
$users = $DB->get_records_sql($sql);
foreach ($users as $u) {
    printf("  [UID:%4d] %-25s | %s %s | %d role(s)\n",
        $u->id, $u->username, $u->firstname, $u->lastname, $u->rolecount);
}
echo "\n";

// 5. Kết luận
echo "5. KẾT LUẬN:\n";
echo str_repeat("-", 80) . "\n";
$need = [];
if (!$DB->get_record('role', ['shortname' => 'manager'])) $need[] = 'manager';
if (!$DB->get_record('role', ['shortname' => 'editingteacher'])) $need[] = 'editingteacher';
if (!$DB->get_record('role', ['shortname' => 'student'])) $need[] = 'student';
if (!$adviser && !$adviser2 && !$adviser3) $need[] = 'academicadviser (custom)';

if (empty($need)) {
    echo "  ✓ Tất cả role cần thiết đã có sẵn.\n";
    echo "  → Chạy install_roles.php để đảm bảo quyền được cập nhật.\n";
} else {
    echo "  ✗ Thiếu các role: " . implode(', ', $need) . "\n";
    echo "  → Chạy install_roles.php để tạo/cấu hình.\n";
}

echo "\n=== HOÀN TẤT ===\n\n";
