<?php
/**
 * Tìm các user mẫu để test 4 role.
 */

define('CLI_SCRIPT', true);
require_once(__DIR__ . '/config.php');
require_once($CFG->libdir . '/accesslib.php');

$USER = get_admin();
global $DB;

echo "\n=== TÌM USER MẪU ĐỂ TEST ===\n\n";

$roles_to_check = [
    'manager' => 'Admin',
    'editingteacher' => 'Giảng viên',
    'student' => 'Sinh viên',
];

foreach ($roles_to_check as $shortname => $label) {
    echo "--- $label ($shortname) ---\n";
    $users = $DB->get_records_sql(
        "SELECT DISTINCT u.id, u.username, u.firstname, u.lastname, u.email
           FROM {user} u
           JOIN {role_assignments} ra ON ra.userid = u.id
           JOIN {role} r ON r.id = ra.roleid
          WHERE r.shortname = ?
            AND u.deleted = 0
            AND u.suspended = 0
          LIMIT 5",
        [$shortname]
    );
    if (empty($users)) {
        echo "  (chưa có user nào)\n";
    } else {
        foreach ($users as $u) {
            printf("  ID:%-4d %-25s | %s %s | %s\n",
                $u->id, $u->username, $u->firstname, $u->lastname, $u->email);
        }
    }
    echo "\n";
}

echo "--- Gợi ý test role Cố vấn học tập ---\n";
echo "Vì chưa có user nào có role academicadviser, bạn cần gán thủ công.\n";
echo "Gợi ý: chọn 1 user giảng viên và gán thêm role academicadviser để test.\n\n";
