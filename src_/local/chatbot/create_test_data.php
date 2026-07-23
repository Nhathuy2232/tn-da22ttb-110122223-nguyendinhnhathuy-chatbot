<?php
/**
 * Script tự động tạo dữ liệu test cho Chatbot Early Warning
 * 
 * Tạo:
 * - 5 courses
 * - 15 students (4 RED, 4 YELLOW, 7 GREEN)
 * - Enrollments
 * - Assignments
 * - Grades
 * 
 * @author Nguyễn Đình Nhật Huy
 * @version 1.0.0
 * 
 * USAGE:
 * php create_test_data.php
 */

define('CLI_SCRIPT', true);
require(__DIR__.'/../../config.php');
require_once($CFG->libdir.'/adminlib.php');
require_once($CFG->dirroot.'/course/lib.php');
require_once($CFG->dirroot.'/user/lib.php');
require_once($CFG->dirroot.'/enrol/manual/locallib.php');
require_once($CFG->dirroot.'/mod/assign/lib.php');
require_once($CFG->dirroot.'/lib/gradelib.php');

// For CLI, we need to set up admin user
if (!isset($USER->id)) {
    $admin = get_admin();
    if ($admin) {
        \core\session\manager::set_user($admin);
    }
}

echo "=====================================\n";
echo "CHATBOT EARLY WARNING - CREATE TEST DATA\n";
echo "=====================================\n\n";

// ==================== COURSES ====================
echo "1. Creating Courses...\n";

$courses_data = [
    ['fullname' => 'Lập trình Java nâng cao', 'shortname' => 'IT301', 'category' => 1],
    ['fullname' => 'Cơ sở dữ liệu', 'shortname' => 'IT201', 'category' => 1],
    ['fullname' => 'Phát triển ứng dụng Web', 'shortname' => 'IT302', 'category' => 1],
    ['fullname' => 'Trí tuệ nhân tạo', 'shortname' => 'IT401', 'category' => 1],
    ['fullname' => 'Mạng máy tính', 'shortname' => 'IT202', 'category' => 1],
];

$courses = [];
foreach ($courses_data as $course_data) {
    // Check if course exists
    $existing = $DB->get_record('course', ['shortname' => $course_data['shortname']]);
    if ($existing) {
        echo "   - Course {$course_data['shortname']} already exists (ID: {$existing->id})\n";
        $courses[] = $existing;
        continue;
    }
    
    $course = new stdClass();
    $course->fullname = $course_data['fullname'];
    $course->shortname = $course_data['shortname'];
    $course->category = $course_data['category'];
    $course->visible = 1;
    $course->startdate = strtotime('2026-01-15');
    $course->enddate = strtotime('2026-06-30');
    $course->format = 'topics';
    $course->numsections = 10;
    
    $course_obj = create_course($course);
    $courses[] = $course_obj;
    echo "   ✓ Created: {$course_data['shortname']} (ID: {$course_obj->id})\n";
}

echo "   → Total courses: " . count($courses) . "\n\n";

// ==================== STUDENTS ====================
echo "2. Creating Students...\n";

$students_data = [
    // RED Students (4)
    ['username' => 'student01', 'firstname' => 'An', 'lastname' => 'Nguyễn Văn', 'idnumber' => '110122001', 'risk' => 'RED'],
    ['username' => 'student02', 'firstname' => 'Bình', 'lastname' => 'Trần Thị', 'idnumber' => '110122002', 'risk' => 'RED'],
    ['username' => 'student03', 'firstname' => 'Cường', 'lastname' => 'Lê Văn', 'idnumber' => '110122003', 'risk' => 'RED'],
    ['username' => 'student04', 'firstname' => 'Dung', 'lastname' => 'Phạm Thị', 'idnumber' => '110122004', 'risk' => 'RED'],
    
    // YELLOW Students (4)
    ['username' => 'student05', 'firstname' => 'Em', 'lastname' => 'Hoàng Văn', 'idnumber' => '110122005', 'risk' => 'YELLOW'],
    ['username' => 'student06', 'firstname' => 'Phương', 'lastname' => 'Vũ Thị', 'idnumber' => '110122006', 'risk' => 'YELLOW'],
    ['username' => 'student07', 'firstname' => 'Giang', 'lastname' => 'Đặng Văn', 'idnumber' => '110122007', 'risk' => 'YELLOW'],
    ['username' => 'student08', 'firstname' => 'Hoa', 'lastname' => 'Bùi Thị', 'idnumber' => '110122008', 'risk' => 'YELLOW'],
    
    // GREEN Students (7)
    ['username' => 'student09', 'firstname' => 'Khoa', 'lastname' => 'Ngô Văn', 'idnumber' => '110122009', 'risk' => 'GREEN'],
    ['username' => 'student10', 'firstname' => 'Lan', 'lastname' => 'Đinh Thị', 'idnumber' => '110122010', 'risk' => 'GREEN'],
    ['username' => 'student11', 'firstname' => 'Minh', 'lastname' => 'Võ Văn', 'idnumber' => '110122011', 'risk' => 'GREEN'],
    ['username' => 'student12', 'firstname' => 'Nga', 'lastname' => 'Lý Thị', 'idnumber' => '110122012', 'risk' => 'GREEN'],
    ['username' => 'student13', 'firstname' => 'Phúc', 'lastname' => 'Trương Văn', 'idnumber' => '110122013', 'risk' => 'GREEN'],
    ['username' => 'student14', 'firstname' => 'Quỳnh', 'lastname' => 'Phan Thị', 'idnumber' => '110122014', 'risk' => 'GREEN'],
    ['username' => 'student15', 'firstname' => 'Sơn', 'lastname' => 'Dương Văn', 'idnumber' => '110122015', 'risk' => 'GREEN'],
];

$students = [];
foreach ($students_data as $student_data) {
    // Check if student exists
    $existing = $DB->get_record('user', ['username' => $student_data['username']]);
    if ($existing) {
        echo "   - Student {$student_data['username']} already exists (ID: {$existing->id})\n";
        $existing->risk = $student_data['risk']; // Add risk level for later use
        $students[] = $existing;
        continue;
    }
    
    $user = new stdClass();
    $user->username = $student_data['username'];
    $user->password = 'Student@2026';
    $user->firstname = $student_data['firstname'];
    $user->lastname = $student_data['lastname'];
    $user->email = $student_data['username'] . '@student.edu.vn';
    $user->idnumber = $student_data['idnumber'];
    $user->city = 'Hà Nội';
    $user->country = 'VN';
    $user->confirmed = 1;
    $user->mnethostid = $CFG->mnet_localhost_id;
    $user->auth = 'manual';
    
    $userid = user_create_user($user, false, false);
    $user->id = $userid;
    $user->risk = $student_data['risk']; // Add risk level for later use
    
    // Set password
    update_internal_user_password($user, 'Student@2026');
    
    $students[] = $user;
    echo "   ✓ Created: {$student_data['username']} - {$student_data['firstname']} {$student_data['lastname']} ({$student_data['risk']})\n";
}

echo "   → Total students: " . count($students) . "\n";
echo "   → RED: 4, YELLOW: 4, GREEN: 7\n\n";

// ==================== ENROLLMENTS ====================
echo "3. Creating Enrollments...\n";

// Enrollment plan
$enrollment_plan = [
    0 => [0, 1, 2, 4, 5, 8, 9, 10, 11], // IT301: students 01,02,03,05,06,09,10,11,12
    1 => [1, 3, 6, 7, 12, 13, 14],      // IT201: students 02,04,07,08,13,14,15
    2 => [0, 2, 4, 6, 8, 10, 12, 14],   // IT302: students 01,03,05,07,09,11,13,15
    3 => [1, 3, 5, 7, 9, 11, 13],       // IT401: students 02,04,06,08,10,12,14
    4 => [0, 4, 8, 9, 10, 11, 12, 13, 14], // IT202: students 01,05,09,10,11,12,13,14,15
];

$total_enrollments = 0;
foreach ($courses as $course_idx => $course) {
    // Get manual enrolment plugin
    $enrol = $DB->get_record('enrol', ['courseid' => $course->id, 'enrol' => 'manual']);
    if (!$enrol) {
        // Create manual enrolment if not exists
        $enrol = new stdClass();
        $enrol->enrol = 'manual';
        $enrol->status = 0;
        $enrol->courseid = $course->id;
        $enrol->id = $DB->insert_record('enrol', $enrol);
    }
    
    $enrol_plugin = enrol_get_plugin('manual');
    $student_role = $DB->get_record('role', ['shortname' => 'student']);
    
    $student_indices = $enrollment_plan[$course_idx];
    foreach ($student_indices as $student_idx) {
        $student = $students[$student_idx];
        
        // Check if already enrolled
        if (is_enrolled(context_course::instance($course->id), $student->id)) {
            continue;
        }
        
        $enrol_plugin->enrol_user($enrol, $student->id, $student_role->id);
        $total_enrollments++;
    }
    
    echo "   ✓ Enrolled " . count($student_indices) . " students to {$course->shortname}\n";
}

echo "   → Total enrollments: {$total_enrollments}\n\n";

// ==================== ASSIGNMENTS ====================
echo "4. Creating Assignments...\n";

$total_assignments = 0;
$assignment_dates = [
    strtotime('2026-01-22'),
    strtotime('2026-01-29'),
    strtotime('2026-02-05'),
    strtotime('2026-02-12'),
    strtotime('2026-02-19'),
];

foreach ($courses as $course) {
    for ($i = 1; $i <= 5; $i++) {
        // Check if assignment exists
        $existing = $DB->get_record('assign', [
            'course' => $course->id,
            'name' => "Bài tập tuần {$i}"
        ]);
        
        if ($existing) {
            continue;
        }
        
        $assign = new stdClass();
        $assign->course = $course->id;
        $assign->name = "Bài tập tuần {$i}";
        $assign->intro = "Bài tập thực hành tuần {$i}";
        $assign->introformat = FORMAT_HTML;
        $assign->alwaysshowdescription = 1;
        $assign->submissiondrafts = 0;
        $assign->sendnotifications = 0;
        $assign->sendlatenotifications = 0;
        $assign->duedate = $assignment_dates[$i-1];
        $assign->allowsubmissionsfromdate = $assignment_dates[$i-1] - (7 * 24 * 60 * 60);
        $assign->grade = 10;
        $assign->timemodified = time();
        $assign->completionsubmit = 0;
        
        // Create course module
        $module = $DB->get_record('modules', ['name' => 'assign']);
        $coursemodule = new stdClass();
        $coursemodule->course = $course->id;
        $coursemodule->module = $module->id;
        $coursemodule->instance = 0;
        $coursemodule->section = 1;
        $coursemodule->visible = 1;
        $coursemodule->groupmode = 0;
        $coursemodule->groupingid = 0;
        
        $coursemodule->id = add_course_module($coursemodule);
        $assign->coursemodule = $coursemodule->id;
        
        // Create assignment
        $assign->id = $DB->insert_record('assign', $assign);
        
        // Update course module instance
        $DB->set_field('course_modules', 'instance', $assign->id, ['id' => $coursemodule->id]);
        
        // Rebuild course cache
        rebuild_course_cache($course->id);
        
        $total_assignments++;
    }
    
    echo "   ✓ Created 5 assignments for {$course->shortname}\n";
}

echo "   → Total assignments: {$total_assignments}\n\n";

// ==================== GRADES ====================
echo "5. Creating Grades...\n";

// Grade ranges by risk level
$grade_ranges = [
    'RED' => [3.0, 4.5],
    'YELLOW' => [4.5, 5.5],
    'GREEN' => [7.0, 9.0],
];

$total_grades = 0;
foreach ($courses as $course) {
    // Get all assignments in course
    $assignments = $DB->get_records('assign', ['course' => $course->id]);
    
    // Get enrolled students
    $context = context_course::instance($course->id);
    $enrolled = get_enrolled_users($context, 'mod/assign:submit');
    
    foreach ($enrolled as $student) {
        // Find student risk level
        $student_risk = 'GREEN';
        foreach ($students as $s) {
            if ($s->id == $student->id) {
                $student_risk = $s->risk;
                break;
            }
        }
        
        $grade_range = $grade_ranges[$student_risk];
        
        foreach ($assignments as $assignment) {
            // Generate random grade within range
            $grade = rand($grade_range[0] * 10, $grade_range[1] * 10) / 10;
            
            // Get grade item
            $grade_item = $DB->get_record('grade_items', [
                'courseid' => $course->id,
                'itemmodule' => 'assign',
                'iteminstance' => $assignment->id
            ]);
            
            if (!$grade_item) {
                continue;
            }
            
            // Check if grade exists
            $existing_grade = $DB->get_record('grade_grades', [
                'itemid' => $grade_item->id,
                'userid' => $student->id
            ]);
            
            if ($existing_grade) {
                continue;
            }
            
            // Create grade
            $grade_obj = new stdClass();
            $grade_obj->itemid = $grade_item->id;
            $grade_obj->userid = $student->id;
            $grade_obj->finalgrade = $grade;
            $grade_obj->timemodified = time();
            $grade_obj->timecreated = time();
            
            $DB->insert_record('grade_grades', $grade_obj);
            $total_grades++;
        }
    }
    
    echo "   ✓ Created grades for {$course->shortname}\n";
}

echo "   → Total grades: {$total_grades}\n\n";

// ==================== SUMMARY ====================
echo "=====================================\n";
echo "SUMMARY\n";
echo "=====================================\n";
echo "✓ Courses:      " . count($courses) . "\n";
echo "✓ Students:     " . count($students) . " (4 RED, 4 YELLOW, 7 GREEN)\n";
echo "✓ Enrollments:  {$total_enrollments}\n";
echo "✓ Assignments:  {$total_assignments}\n";
echo "✓ Grades:       {$total_grades}\n";
echo "\n";
echo "=====================================\n";
echo "COMPLETED SUCCESSFULLY!\n";
echo "=====================================\n\n";

echo "NEXT STEPS:\n";
echo "1. Enable Web Services in Moodle\n";
echo "2. Create token for Chatbot\n";
echo "3. Update token in application.yml\n";
echo "4. Enable DataSyncService\n";
echo "5. Restart Spring Boot\n";
echo "6. Verify sync in database\n\n";

echo "Done! 🎉\n";
