<?php
/**
 * Script kiểm tra và xác nhận dữ liệu đã seed
 * 
 * Chạy: php verify_data.php
 */

define('CLI_SCRIPT', true);
require_once(__DIR__ . '/config.php');
require_once($CFG->libdir . '/adminlib.php');

echo "=== KIỂM TRA DỮ LIỆU MOODLE ===\n\n";

global $DB;

$errors = 0;
$warnings = 0;

// ==================== 1. Kiểm tra Users ====================
echo "1. Kiểm tra Users\n";
echo str_repeat("-", 70) . "\n";

// Giáo viên
$teachers_count = $DB->count_records_sql(
    "SELECT COUNT(*) FROM {user} WHERE username LIKE 'giangvien%' AND deleted = 0"
);
echo "Giáo viên: $teachers_count / 6 ";
echo ($teachers_count == 6) ? "✓\n" : "✗ (Thiếu)\n";
if ($teachers_count != 6) $errors++;

// Cố vấn
$advisors_count = $DB->count_records_sql(
    "SELECT COUNT(*) FROM {user} WHERE username LIKE 'covanhoctap%' AND deleted = 0"
);
echo "Cố vấn học tập: $advisors_count / 4 ";
echo ($advisors_count == 4) ? "✓\n" : "✗ (Thiếu)\n";
if ($advisors_count != 4) $errors++;

// Sinh viên
$students_count = $DB->count_records_sql(
    "SELECT COUNT(*) FROM {user} WHERE username LIKE '110122%' AND deleted = 0"
);
echo "Sinh viên: $students_count / 88 ";
echo ($students_count == 88) ? "✓\n" : "✗ (Thiếu)\n";
if ($students_count != 88) $errors++;

echo "\n";

// ==================== 2. Kiểm tra Courses ====================
echo "2. Kiểm tra Courses\n";
echo str_repeat("-", 70) . "\n";

// Lớp học
$classes_count = $DB->count_records_sql(
    "SELECT COUNT(*) FROM {course} WHERE shortname LIKE 'CLASS%'"
);
echo "Lớp học: $classes_count / 4 ";
echo ($classes_count == 4) ? "✓\n" : "✗ (Thiếu)\n";
if ($classes_count != 4) $errors++;

// Môn học
$subjects_count = $DB->count_records_sql(
    "SELECT COUNT(*) FROM {course} WHERE shortname LIKE 'SUBJ%'"
);
echo "Môn học: $subjects_count / 6 ";
echo ($subjects_count == 6) ? "✓\n" : "✗ (Thiếu)\n";
if ($subjects_count != 6) $errors++;

echo "\n";

// ==================== 3. Kiểm tra Enrolments ====================
echo "3. Kiểm tra Enrolments (Ghi danh)\n";
echo str_repeat("-", 70) . "\n";

// Kiểm tra từng lớp học
$classes = $DB->get_records_sql(
    "SELECT * FROM {course} WHERE shortname LIKE 'CLASS%' ORDER BY shortname"
);

foreach ($classes as $class) {
    $student_count = $DB->count_records_sql(
        "SELECT COUNT(DISTINCT ue.userid)
         FROM {user_enrolments} ue
         JOIN {enrol} e ON e.id = ue.enrolid
         JOIN {role_assignments} ra ON ra.userid = ue.userid
         JOIN {context} ctx ON ctx.id = ra.contextid
         JOIN {role} r ON r.id = ra.roleid
         WHERE e.courseid = ? 
         AND ctx.contextlevel = 50 
         AND ctx.instanceid = ?
         AND r.shortname = 'student'",
        [$class->id, $class->id]
    );
    
    $advisor_count = $DB->count_records_sql(
        "SELECT COUNT(DISTINCT ue.userid)
         FROM {user_enrolments} ue
         JOIN {enrol} e ON e.id = ue.enrolid
         JOIN {role_assignments} ra ON ra.userid = ue.userid
         JOIN {context} ctx ON ctx.id = ra.contextid
         JOIN {role} r ON r.id = ra.roleid
         WHERE e.courseid = ? 
         AND ctx.contextlevel = 50 
         AND ctx.instanceid = ?
         AND r.shortname = 'editingteacher'",
        [$class->id, $class->id]
    );
    
    echo "  $class->shortname: ";
    echo "$student_count sinh viên, $advisor_count cố vấn ";
    
    if ($student_count == 22 && $advisor_count == 1) {
        echo "✓\n";
    } else {
        echo "✗\n";
        $errors++;
    }
}

echo "\n";

// Kiểm tra môn học
$subjects = $DB->get_records_sql(
    "SELECT * FROM {course} WHERE shortname LIKE 'SUBJ%' ORDER BY shortname"
);

foreach ($subjects as $subject) {
    $student_count = $DB->count_records_sql(
        "SELECT COUNT(DISTINCT ue.userid)
         FROM {user_enrolments} ue
         JOIN {enrol} e ON e.id = ue.enrolid
         JOIN {role_assignments} ra ON ra.userid = ue.userid
         JOIN {context} ctx ON ctx.id = ra.contextid
         JOIN {role} r ON r.id = ra.roleid
         WHERE e.courseid = ? 
         AND ctx.contextlevel = 50 
         AND ctx.instanceid = ?
         AND r.shortname = 'student'",
        [$subject->id, $subject->id]
    );
    
    $teacher_count = $DB->count_records_sql(
        "SELECT COUNT(DISTINCT ue.userid)
         FROM {user_enrolments} ue
         JOIN {enrol} e ON e.id = ue.enrolid
         JOIN {role_assignments} ra ON ra.userid = ue.userid
         JOIN {context} ctx ON ctx.id = ra.contextid
         JOIN {role} r ON r.id = ra.roleid
         WHERE e.courseid = ? 
         AND ctx.contextlevel = 50 
         AND ctx.instanceid = ?
         AND r.shortname = 'editingteacher'",
        [$subject->id, $subject->id]
    );
    
    echo "  $subject->shortname: ";
    echo "$student_count sinh viên, $teacher_count giảng viên ";
    
    if ($student_count >= 40 && $student_count <= 50 && $teacher_count == 1) {
        echo "✓\n";
    } else {
        echo "⚠ (Expected 40-50 students)\n";
        $warnings++;
    }
}

echo "\n";

// ==================== 4. Kiểm tra Groups ====================
echo "4. Kiểm tra Groups (Nhóm học)\n";
echo str_repeat("-", 70) . "\n";

$total_groups = $DB->count_records_sql(
    "SELECT COUNT(*) FROM {groups} g
     JOIN {course} c ON c.id = g.courseid
     WHERE c.shortname LIKE 'SUBJ%'"
);

echo "Tổng số nhóm: $total_groups / 12 ";
echo ($total_groups == 12) ? "✓\n" : "✗ (Thiếu)\n";
if ($total_groups != 12) $errors++;

foreach ($subjects as $subject) {
    $groups = $DB->get_records_sql(
        "SELECT g.*, COUNT(gm.userid) as member_count
         FROM {groups} g
         LEFT JOIN {groups_members} gm ON gm.groupid = g.id
         WHERE g.courseid = ?
         GROUP BY g.id
         ORDER BY g.name",
        [$subject->id]
    );
    
    echo "  $subject->shortname: ";
    
    $group_count = count($groups);
    if ($group_count == 2) {
        echo "$group_count nhóm ";
        
        $all_ok = true;
        foreach ($groups as $group) {
            if ($group->member_count < 20 || $group->member_count > 25) {
                $all_ok = false;
            }
        }
        
        echo $all_ok ? "✓\n" : "⚠ (Size không đúng)\n";
        if (!$all_ok) $warnings++;
    } else {
        echo "✗ (Expected 2 groups)\n";
        $errors++;
    }
}

echo "\n";

// ==================== Tổng kết ====================
echo str_repeat("=", 70) . "\n";
echo "KẾT QUẢ KIỂM TRA\n";
echo str_repeat("=", 70) . "\n";

if ($errors == 0 && $warnings == 0) {
    echo "✅ HOÀN HẢO! Tất cả dữ liệu đã được seed đúng.\n\n";
} else {
    if ($errors > 0) {
        echo "❌ Có $errors lỗi nghiêm trọng\n";
    }
    if ($warnings > 0) {
        echo "⚠️  Có $warnings cảnh báo\n";
    }
    echo "\n";
    echo "Khuyến nghị: Chạy lại seed_data.php\n\n";
}

echo "Tài khoản test:\n";
echo "  Admin: admin / (mật khẩu hiện tại)\n";
echo "  Giáo viên: giangvien01 / Password@123\n";
echo "  Cố vấn: covanhoctap01 / Password@123\n";
echo "  Sinh viên: 110122001 / Password@123\n\n";

echo "=== HOÀN TẤT ===\n";
