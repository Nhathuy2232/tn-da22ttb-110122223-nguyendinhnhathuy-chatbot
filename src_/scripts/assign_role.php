<?php
/**
 * Script gán role cho user theo MSSV hoặc username.
 * Dùng để test nhanh 4 role: Sinh viên, Giảng viên, Cố vấn, Admin.
 *
 * Cách dùng:
 *   php assign_role.php --username=110122001 --role=student
 *   php assign_role.php --username=gv.nguyenvana --role=editingteacher
 *   php assign_role.php --username=cv.leanh --role=academicadviser
 *   php assign_role.php --username=admin --role=manager
 *   php assign_role.php --list    (liệt kê các role có thể gán)
 *   php assign_role.php --user=110122001  (xem role hiện tại của user)
 */

define('CLI_SCRIPT', true);
require_once(__DIR__ . '/config.php');
require_once($CFG->libdir . '/accesslib.php');
require_once($CFG->libdir . '/moodlelib.php');

// Pseudo-admin cho CLI
$syscontext = context_system::instance();
$USER = get_admin();

$args = parse_cli_args();

echo "\n=== GÁN PHÂN QUYỀN CHO USER ===\n\n";

global $DB;

// ===========================
// 1. Liệt kê các role có thể gán
// ===========================
if (!empty($args['list'])) {
    echo "CÁC ROLE CÓ THỂ GÁN:\n";
    echo str_repeat("-", 60) . "\n";
    $roles = [
        'student'         => 'Sinh viên',
        'editingteacher'  => 'Giảng viên (có quyền chỉnh sửa)',
        'teacher'         => 'Giảng viên (không chỉnh sửa)',
        'academicadviser' => 'Cố vấn học tập',
        'manager'         => 'Quản trị viên',
    ];
    foreach ($roles as $sn => $label) {
        $r = $DB->get_record('role', ['shortname' => $sn]);
        $exists = $r ? "✓ ID:{$r->id}" : "✗ chưa tạo";
        printf("  %-20s - %-30s [%s]\n", $sn, $label, $exists);
    }
    echo "\nVí dụ: php assign_role.php --username=110122001 --role=student\n\n";
    exit(0);
}

// ===========================
// 2. Xem role hiện tại của user
// ===========================
if (!empty($args['user'])) {
    $username = $args['user'];
    $user = $DB->get_record('user', ['username' => $username]);
    if (!$user) {
        echo "✗ Không tìm thấy user với username '$username'\n\n";
        exit(1);
    }

    echo "Thông tin user: {$user->username} ({$user->firstname} {$user->lastname})\n";
    echo "ID: {$user->id}\n";
    echo "Email: {$user->email}\n\n";

    $roles = $DB->get_records_sql(
        "SELECT r.shortname, r.name, ra.contextid
           FROM {role_assignments} ra
           JOIN {role} r ON r.id = ra.roleid
          WHERE ra.userid = ?",
        [$user->id]
    );

    echo "Các role hiện tại:\n";
    if (empty($roles)) {
        echo "  (chưa có role nào)\n";
    } else {
        foreach ($roles as $r) {
            $ctx = $DB->get_record('context', ['id' => $r->contextid]);
            $ctxlabel = context_helper::get_level_name($ctx->contextlevel ?? 0);
            echo "  • {$r->shortname} ({$r->name}) - context: $ctxlabel\n";
        }
    }
    echo "\n";
    exit(0);
}

// ===========================
// 3. Gán role cho user
// ===========================
if (empty($args['username']) || empty($args['role'])) {
    echo "✗ Thiếu tham số!\n";
    echo "Sử dụng: php assign_role.php --username=<username> --role=<rolename>\n";
    echo "Hoặc:    php assign_role.php --user=<username>   (xem role hiện tại)\n";
    echo "Hoặc:    php assign_role.php --list                (liệt kê role)\n\n";
    exit(1);
}

$username = $args['username'];
$rolename = $args['role'];

// Tìm user
$user = $DB->get_record('user', ['username' => $username]);
if (!$user) {
    echo "✗ Không tìm thấy user với username '$username'\n\n";
    exit(1);
}

// Tìm role
$role = $DB->get_record('role', ['shortname' => $rolename]);
if (!$role) {
    echo "✗ Không tìm thấy role '$rolename'\n";
    echo "  → Chạy: php install_roles.php để tạo role\n";
    echo "  → Hoặc:  php assign_role.php --list để xem danh sách\n\n";
    exit(1);
}

// Kiểm tra đã có role này chưa
$existing = $DB->get_record('role_assignments', [
    'roleid'    => $role->id,
    'userid'    => $user->id,
    'contextid' => 1, // system context
]);

if ($existing) {
    echo "↻ User '{$user->username}' ĐÃ CÓ role '{$role->shortname}'\n\n";
    exit(0);
}

// Gán role
role_assign($role->id, $user->id, $syscontext->id);

echo "✓ ĐÃ GÁN role '{$role->shortname}' ({$role->name}) cho user '{$user->username}' (UID: {$user->id})\n";
echo "  Context: Hệ thống (system)\n\n";

// Hiển thị tất cả role hiện tại
$roles = $DB->get_records_sql(
    "SELECT r.shortname, r.name
       FROM {role_assignments} ra
       JOIN {role} r ON r.id = ra.roleid
      WHERE ra.userid = ?",
    [$user->id]
);
echo "Tổng số role của user: " . count($roles) . "\n";
foreach ($roles as $r) {
    echo "  • {$r->shortname} - {$r->name}\n";
}
echo "\n";

/**
 * Parse tham số CLI đơn giản: --key=value hoặc --key
 */
function parse_cli_args() {
    $args = [];
    foreach ($_SERVER['argv'] as $arg) {
        if (strpos($arg, '--') === 0) {
            $arg = substr($arg, 2);
            if (strpos($arg, '=') !== false) {
                list($k, $v) = explode('=', $arg, 2);
                $args[$k] = $v;
            } else {
                $args[$arg] = true;
            }
        }
    }
    return $args;
}
