<?php
/**
 * Script thêm dữ liệu mẫu vào Moodle
 * 
 * Cấu trúc:
 * - 4 lớp, mỗi lớp 20-25 sinh viên
 * - Mỗi lớp có 1 cố vấn học tập riêng
 * - Mỗi giáo viên dạy 1 môn học cụ thể
 * - Mỗi môn học có 2 nhóm, mỗi nhóm 20-25 sinh viên từ nhiều lớp
 * 
 * Chạy: php seed_data.php
 */

define('CLI_SCRIPT', true);
require_once(__DIR__ . '/config.php');
require_once($CFG->libdir . '/adminlib.php');
require_once($CFG->dirroot . '/user/lib.php');
require_once($CFG->dirroot . '/course/lib.php');
require_once($CFG->dirroot . '/group/lib.php');
require_once($CFG->dirroot . '/enrol/manual/locallib.php');

echo "=== MOODLE DATA SEEDER ===\n\n";

// Cấu hình
$CONFIG = [
    'num_classes' => 4,
    'students_per_class' => 22, // Trung bình 20-25
    'num_courses' => 6,
    'students_per_course_group' => 22,
    'groups_per_course' => 2,
    'password' => 'Password@123',
];

// ==================== HELPER FUNCTIONS ====================

function create_user($username, $firstname, $lastname, $email, $password) {
    global $DB;
    
    // Kiểm tra user đã tồn tại
    if ($DB->record_exists('user', ['username' => $username])) {
        echo "  - User '$username' đã tồn tại, bỏ qua\n";
        return $DB->get_record('user', ['username' => $username]);
    }
    
    $user = new stdClass();
    $user->auth = 'manual';
    $user->confirmed = 1;
    $user->mnethostid = 1;
    $user->username = $username;
    $user->password = hash_internal_user_password($password);
    $user->firstname = $firstname;
    $user->lastname = $lastname;
    $user->email = $email;
    $user->lang = 'vi';
    $user->timecreated = time();
    $user->timemodified = time();
    
    $user->id = user_create_user($user, false, false);
    
    echo "  ✓ Tạo user: $username ($firstname $lastname)\n";
    return $user;
}

function assign_role($userid, $rolename, $contextid) {
    global $DB;
    
    $role = $DB->get_record('role', ['shortname' => $rolename]);
    if (!$role) {
        echo "  ⚠ Role '$rolename' không tồn tại\n";
        return false;
    }
    
    role_assign($role->id, $userid, $contextid);
    return true;
}

function create_category($name, $description) {
    global $DB;
    
    // Kiểm tra category đã tồn tại
    if ($DB->record_exists('course_categories', ['name' => $name])) {
        echo "  - Category '$name' đã tồn tại, bỏ qua\n";
        return $DB->get_record('course_categories', ['name' => $name]);
    }
    
    $category = new stdClass();
    $category->name = $name;
    $category->description = $description;
    $category->descriptionformat = FORMAT_HTML;
    $category->parent = 0;
    $category->sortorder = 0;
    $category->timemodified = time();
    
    $category->id = $DB->insert_record('course_categories', $category);
    
    echo "  ✓ Tạo category: $name\n";
    return $category;
}

function create_course($shortname, $fullname, $categoryid, $summary = '') {
    global $DB;
    
    // Kiểm tra course đã tồn tại
    if ($DB->record_exists('course', ['shortname' => $shortname])) {
        echo "  - Course '$shortname' đã tồn tại, bỏ qua\n";
        return $DB->get_record('course', ['shortname' => $shortname]);
    }
    
    $course = new stdClass();
    $course->category = $categoryid;
    $course->shortname = $shortname;
    $course->fullname = $fullname;
    $course->summary = $summary;
    $course->summaryformat = FORMAT_HTML;
    $course->format = 'topics';
    $course->showgrades = 1;
    $course->newsitems = 5;
    $course->startdate = time();
    $course->enddate = time() + (180 * 24 * 60 * 60); // 6 tháng
    $course->timecreated = time();
    $course->timemodified = time();
    
    $course = create_course($course);
    
    echo "  ✓ Tạo course: $fullname\n";
    return $course;
}

function enrol_user_to_course($userid, $courseid, $rolename = 'student') {
    global $DB;
    
    // Lấy enrol instance
    $enrol = $DB->get_record('enrol', [
        'courseid' => $courseid,
        'enrol' => 'manual'
    ]);
    
    if (!$enrol) {
        // Tạo manual enrol nếu chưa có
        $enrol = new stdClass();
        $enrol->enrol = 'manual';
        $enrol->status = 0;
        $enrol->courseid = $courseid;
        $enrol->sortorder = 0;
        $enrol->timecreated = time();
        $enrol->timemodified = time();
        $enrol->id = $DB->insert_record('enrol', $enrol);
    }
    
    // Lấy role
    $role = $DB->get_record('role', ['shortname' => $rolename]);
    if (!$role) {
        return false;
    }
    
    // Enrol user
    $manual = enrol_get_plugin('manual');
    $manual->enrol_user($enrol, $userid, $role->id, time(), 0);
    
    return true;
}

function create_group($courseid, $name, $description = '') {
    global $DB;
    
    // Kiểm tra group đã tồn tại
    $existing = $DB->get_record('groups', [
        'courseid' => $courseid,
        'name' => $name
    ]);
    
    if ($existing) {
        return $existing;
    }
    
    $group = new stdClass();
    $group->courseid = $courseid;
    $group->name = $name;
    $group->description = $description;
    $group->descriptionformat = FORMAT_HTML;
    $group->timecreated = time();
    $group->timemodified = time();
    
    $group->id = groups_create_group($group);
    
    return $group;
}

// ==================== MAIN SCRIPT ====================

echo "Bước 1: Tạo Categories\n";
echo str_repeat("-", 50) . "\n";

$categories = [];
$categories['classes'] = create_category(
    'Lớp Học',
    'Các lớp sinh viên'
);
$categories['courses'] = create_category(
    'Môn Học',
    'Các môn học chuyên ngành'
);

echo "\n";

echo "Bước 2: Tạo Giáo viên (Teachers)\n";
echo str_repeat("-", 50) . "\n";

$teachers = [];
$teacher_courses = [
    'Lập trình Web',
    'Cơ sở dữ liệu',
    'Mạng máy tính',
    'Trí tuệ nhân tạo',
    'An toàn thông tin',
    'Công nghệ phần mềm'
];

foreach ($teacher_courses as $index => $course_name) {
    $teacher_num = $index + 1;
    $username = 'giangvien' . str_pad($teacher_num, 2, '0', STR_PAD_LEFT);
    
    $teacher = create_user(
        $username,
        'Giảng viên',
        'Nguyễn Văn ' . chr(65 + $index), // A, B, C...
        $username . '@example.com',
        $CONFIG['password']
    );
    
    $teachers[$teacher_num] = $teacher;
}

echo "\n";

echo "Bước 3: Tạo Cố vấn học tập (Academic Advisors)\n";
echo str_repeat("-", 50) . "\n";

$advisors = [];
for ($i = 1; $i <= $CONFIG['num_classes']; $i++) {
    $username = 'covanhoctap' . str_pad($i, 2, '0', STR_PAD_LEFT);
    
    $advisor = create_user(
        $username,
        'Cố vấn',
        'Trần Thị ' . chr(64 + $i), // A, B, C, D
        $username . '@example.com',
        $CONFIG['password']
    );
    
    $advisors[$i] = $advisor;
}

echo "\n";

echo "Bước 4: Tạo Sinh viên (Students)\n";
echo str_repeat("-", 50) . "\n";

$students = [];
$student_counter = 1;

for ($class_num = 1; $class_num <= $CONFIG['num_classes']; $class_num++) {
    echo "Lớp $class_num:\n";
    
    for ($i = 1; $i <= $CONFIG['students_per_class']; $i++) {
        $mssv = '110122' . str_pad($student_counter, 3, '0', STR_PAD_LEFT);
        
        $student = create_user(
            $mssv,
            'Sinh viên',
            'Lớp' . $class_num . '-' . $i,
            $mssv . '@student.example.com',
            $CONFIG['password']
        );
        
        $students[$class_num][] = $student;
        $student_counter++;
    }
    
    echo "\n";
}

echo "\n";

echo "Bước 5: Tạo Lớp học (Class Courses)\n";
echo str_repeat("-", 50) . "\n";

$class_courses = [];
for ($i = 1; $i <= $CONFIG['num_classes']; $i++) {
    $shortname = 'CLASS' . $i;
    $fullname = 'Lớp Học ' . $i . ' - Khóa 2022';
    
    $course = create_course(
        $shortname,
        $fullname,
        $categories['classes']->id,
        'Lớp sinh viên khóa 2022, có cố vấn học tập'
    );
    
    $class_courses[$i] = $course;
    
    // Gán cố vấn học tập vào lớp với role teacher
    echo "  → Gán cố vấn học tập cho lớp $i\n";
    enrol_user_to_course($advisors[$i]->id, $course->id, 'editingteacher');
    
    // Gán sinh viên vào lớp
    echo "  → Gán sinh viên vào lớp $i\n";
    foreach ($students[$i] as $student) {
        enrol_user_to_course($student->id, $course->id, 'student');
    }
}

echo "\n";

echo "Bước 6: Tạo Môn học (Subject Courses)\n";
echo str_repeat("-", 50) . "\n";

$subject_courses = [];
foreach ($teacher_courses as $index => $course_name) {
    $teacher_num = $index + 1;
    $shortname = 'SUBJ' . str_pad($teacher_num, 2, '0', STR_PAD_LEFT);
    
    $course = create_course(
        $shortname,
        $course_name,
        $categories['courses']->id,
        'Môn học: ' . $course_name
    );
    
    $subject_courses[$teacher_num] = $course;
    
    // Gán giáo viên vào môn học
    echo "  → Gán giảng viên " . $teachers[$teacher_num]->username . " vào môn\n";
    enrol_user_to_course($teachers[$teacher_num]->id, $course->id, 'editingteacher');
    
    // Tạo 2 nhóm cho môn học
    $group1 = create_group($course->id, 'Nhóm 1', 'Nhóm học tập 1');
    $group2 = create_group($course->id, 'Nhóm 2', 'Nhóm học tập 2');
    
    echo "  → Tạo nhóm 1 và nhóm 2\n";
    
    // Lấy danh sách tất cả sinh viên từ các lớp
    $all_students = [];
    foreach ($students as $class_students) {
        $all_students = array_merge($all_students, $class_students);
    }
    
    // Shuffle để trộn sinh viên từ các lớp khác nhau
    shuffle($all_students);
    
    // Chia sinh viên vào 2 nhóm (mỗi nhóm 20-25 sinh viên)
    $students_group1 = array_slice($all_students, 0, $CONFIG['students_per_course_group']);
    $students_group2 = array_slice($all_students, $CONFIG['students_per_course_group'], $CONFIG['students_per_course_group']);
    
    // Gán sinh viên nhóm 1
    echo "  → Gán " . count($students_group1) . " sinh viên vào nhóm 1\n";
    foreach ($students_group1 as $student) {
        enrol_user_to_course($student->id, $course->id, 'student');
        groups_add_member($group1->id, $student->id);
    }
    
    // Gán sinh viên nhóm 2
    echo "  → Gán " . count($students_group2) . " sinh viên vào nhóm 2\n";
    foreach ($students_group2 as $student) {
        enrol_user_to_course($student->id, $course->id, 'student');
        groups_add_member($group2->id, $student->id);
    }
    
    echo "\n";
}

echo "\n";

echo "Bước 7: Tổng kết\n";
echo str_repeat("=", 50) . "\n";
echo "✓ Tạo thành công:\n";
echo "  - " . count($teachers) . " giáo viên\n";
echo "  - " . count($advisors) . " cố vấn học tập\n";
echo "  - " . ($CONFIG['num_classes'] * $CONFIG['students_per_class']) . " sinh viên\n";
echo "  - " . count($class_courses) . " lớp học\n";
echo "  - " . count($subject_courses) . " môn học\n";
echo "  - " . (count($subject_courses) * 2) . " nhóm học tập\n\n";

echo "Thông tin đăng nhập:\n";
echo "  - Mật khẩu chung: " . $CONFIG['password'] . "\n\n";

echo "Tài khoản mẫu:\n";
echo "  - Admin: admin / (mật khẩu Moodle hiện tại)\n";
echo "  - Giáo viên: giangvien01, giangvien02, ...\n";
echo "  - Cố vấn: covanhoctap01, covanhoctap02, ...\n";
echo "  - Sinh viên: 110122001, 110122002, ...\n\n";

echo "=== HOÀN TẤT ===\n";
