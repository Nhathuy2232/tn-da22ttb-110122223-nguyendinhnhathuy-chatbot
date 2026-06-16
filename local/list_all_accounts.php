<?php
/**
 * List all user accounts with roles
 */

define('CLI_SCRIPT', true);
require(__DIR__.'/../config.php');
require_once($CFG->libdir.'/accesslib.php');

global $DB;

echo "===========================================\n";
echo "  DANH SÁCH TÀI KHOẢN HỆ THỐNG\n";
echo "===========================================\n\n";

// Lấy tất cả users (trừ admin và guest)
$users = $DB->get_records_sql("
    SELECT u.* 
    FROM {user} u 
    WHERE u.deleted = 0 
    AND u.suspended = 0
    AND u.id > 2
    ORDER BY u.lastname, u.firstname
");

echo "MẬT KHẨU MẶC ĐỊNH CHO TẤT CẢ: User@123\n\n";

echo "-------------------------------------------\n";
echo "PHÂN QUYỀN:\n";
echo "-------------------------------------------\n\n";

echo "1. EDITINGTEACHER (Giáo viên):\n";
echo "   - Tạo và chỉnh sửa khóa học\n";
echo "   - Thêm/xóa tài liệu, bài tập\n";
echo "   - Chấm điểm sinh viên\n";
echo "   - Quản lý sinh viên trong khóa học\n";
echo "   - Xem báo cáo và thống kê\n\n";

echo "2. STUDENT (Sinh viên):\n";
echo "   - Xem nội dung khóa học\n";
echo "   - Nộp bài tập\n";
echo "   - Xem điểm của mình\n";
echo "   - Tham gia diễn đàn, quiz\n";
echo "   - KHÔNG thể chỉnh sửa khóa học\n\n";

echo "===========================================\n";
echo "  DANH SÁCH CHI TIẾT\n";
echo "===========================================\n\n";

$count_teacher = 0;
$count_student = 0;

foreach ($users as $user) {
    // Lấy role của user trong hệ thống
    $system_context = context_system::instance();
    $roles = get_user_roles($system_context, $user->id, false);
    
    $role_names = [];
    $is_teacher = false;
    
    foreach ($roles as $role) {
        $role_names[] = $role->shortname;
        if ($role->shortname == 'editingteacher' || $role->shortname == 'teacher') {
            $is_teacher = true;
        }
    }
    
    // Nếu không có role system, kiểm tra trong courses
    if (empty($role_names)) {
        $courses = enrol_get_users_courses($user->id);
        foreach ($courses as $course) {
            $context = context_course::instance($course->id);
            $course_roles = get_user_roles($context, $user->id, false);
            foreach ($course_roles as $role) {
                if (!in_array($role->shortname, $role_names)) {
                    $role_names[] = $role->shortname;
                }
                if ($role->shortname == 'editingteacher' || $role->shortname == 'teacher') {
                    $is_teacher = true;
                }
            }
        }
    }
    
    if (empty($role_names)) {
        $role_names = ['student']; // Default
    }
    
    $role_display = $is_teacher ? 'GIÁO VIÊN (editingteacher)' : 'SINH VIÊN (student)';
    
    if ($is_teacher) {
        $count_teacher++;
    } else {
        $count_student++;
    }
    
    echo "Username: {$user->username}\n";
    echo "  Họ tên: {$user->firstname} {$user->lastname}\n";
    echo "  Email: {$user->email}\n";
    echo "  Mật khẩu: User@123\n";
    echo "  Quyền hạn: {$role_display}\n";
    
    if ($is_teacher) {
        echo "  Chức năng: Tạo/sửa khóa học, chấm điểm, quản lý sinh viên\n";
    } else {
        echo "  Chức năng: Xem khóa học, nộp bài, xem điểm\n";
    }
    
    // Lấy số khóa học đang tham gia
    $courses = enrol_get_users_courses($user->id);
    echo "  Số khóa học: " . count($courses) . "\n";
    
    echo "\n";
}

echo "===========================================\n";
echo "  THỐNG KÊ\n";
echo "===========================================\n\n";

echo "Tổng số tài khoản: " . count($users) . "\n";
echo "  - Giáo viên: $count_teacher\n";
echo "  - Sinh viên: $count_student\n\n";

echo "===========================================\n";
echo "  DANH SÁCH NHÓM\n";
echo "===========================================\n\n";

$groups = $DB->get_records_sql("
    SELECT g.*, c.fullname as coursename
    FROM {groups} g
    JOIN {course} c ON g.courseid = c.id
    ORDER BY c.fullname, g.name
");

foreach ($groups as $group) {
    $members = groups_get_members($group->id);
    echo "{$group->coursename} - {$group->name}: " . count($members) . " thành viên\n";
}

echo "\n===========================================\n";
echo "  DANH SÁCH BÀI TẬP\n";
echo "===========================================\n\n";

$assigns = $DB->get_records_sql("
    SELECT a.*, c.fullname as coursename
    FROM {assign} a
    JOIN {course} c ON a.course = c.id
    ORDER BY c.fullname, a.name
");

$course_assigns = [];
foreach ($assigns as $assign) {
    if (!isset($course_assigns[$assign->coursename])) {
        $course_assigns[$assign->coursename] = [];
    }
    $course_assigns[$assign->coursename][] = $assign->name;
}

foreach ($course_assigns as $coursename => $assignlist) {
    echo "{$coursename}: " . count($assignlist) . " bài tập\n";
    foreach ($assignlist as $assignname) {
        echo "  - {$assignname}\n";
    }
    echo "\n";
}

echo "===========================================\n";
