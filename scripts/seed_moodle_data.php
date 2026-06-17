<?php
/**
 * Seed Moodle data:
 *  - Phân lớp cố định DA22TTA, B, C, D cho tất cả sinh viên (profile field "classgroup")
 *  - Đảm bảo mỗi khóa học có 1 giáo viên editingteacher (tạo nếu thiếu)
 *  - Đảm bảo mỗi khóa học có 5-7 bài assignment
 *  - Random sinh viên nộp bài / không nộp với điểm ngẫu nhiên (sau khi enrollment xong)
 *  - Chia 2 nhóm + grouping cho mỗi khóa học
 *
 * Chạy CLI:
 *   php seed_moodle_data.php [--dry-run]
 * Hoặc qua web (admin only):
 *   https://your-moodle/seed_moodle_data.php
 */

define('NO_OUTPUT_BUFFERING', true);
define('CLI_SCRIPT', php_sapi_name() === 'cli');

require_once(__DIR__ . '/config.php');
require_once($CFG->libdir . '/adminlib.php');
require_once($CFG->dirroot . '/course/lib.php');
require_once($CFG->dirroot . '/group/lib.php');
require_once($CFG->dirroot . '/mod/assign/lib.php');
require_once($CFG->dirroot . '/user/lib.php');
require_once($CFG->dirroot . '/lib/gradelib.php');
require_once($CFG->libdir . '/accesslib.php');
require_once($CFG->libdir . '/moodlelib.php');

if (!CLI_SCRIPT) {
    require_login();
    require_capability('moodle/site:config', context_system::instance());
}

raise_memory_limit(MEMORY_HUGE);

$dryrun = in_array('--dry-run', $argv ?? [], true);
$iscli  = CLI_SCRIPT;
$out    = function ($line) use ($iscli) {
    echo ($iscli ? $line : nl2br(htmlspecialchars($line))) . "\n";
    @ob_flush(); @flush();
};

$out(str_repeat('=', 72));
$out('MOODLE SEED SCRIPT  -  ' . ($dryrun ? '[DRY-RUN]' : '[LIVE]'));
$out(str_repeat('=', 72));

mt_srand(20260617); // reproducible random

/* ============================================================================
 * 0) Profile field "classgroup" (DA22TTA / DA22TTB / DA22TTC / DA22TTD)
 * ========================================================================== */
$out("\n[0] Tạo/đảm bảo profile field 'classgroup'");
$field = $DB->get_record('user_info_field', ['shortname' => 'classgroup']);
if (!$field) {
    $field = new stdClass();
    $field->shortname = 'classgroup';
    $field->name      = 'Lớp';
    $field->datatype  = 'text';
    $field->description = 'Lớp sinh viên cố định (DA22TTA/B/C/D)';
    $field->descriptionformat = FORMAT_HTML;
    $field->categoryid  = 1;
    $field->sortorder   = 0;
    $field->required    = 0;
    $field->locked      = 0;
    $field->visible     = 2;
    $field->forceunique = 0;
    $field->signup      = 0;
    $field->param1      = 30;
    $field->param2      = 0;
    $field->param3      = 0;
    $field->param4      = 0;
    $field->param5      = 0;
    $field->defaultdata = '';
    $field->defaultdataformat = 0;
    $field->id = $DB->insert_record('user_info_field', $field);
    $out("   + Tạo user_info_field 'classgroup' id={$field->id}");
} else {
    $out("   = user_info_field 'classgroup' đã có (id={$field->id})");
}
$classgroups = ['DA22TTA', 'DA22TTB', 'DA22TTC', 'DA22TTD'];

/* ============================================================================
 * 1) Phân lớp cố định DA22TTA/B/C/D cho tất cả sinh viên
 * ========================================================================== */
$out("\n[1] Phân lớp DA22TTA/B/C/D cho sinh viên (cố định theo userid % 4)");
$students = $DB->get_records_sql("
    SELECT DISTINCT u.id, u.username
      FROM {user} u
      JOIN {role_assignments} ra ON ra.userid = u.id
      JOIN {role} r ON r.id = ra.roleid
     WHERE r.shortname = 'student' AND u.deleted = 0 AND u.id > 1
     ORDER BY u.id
");
$existing_class = $DB->get_records_menu('user_info_data', ['fieldid' => $field->id], '', 'userid, data');
$out("   Tổng sinh viên unique: " . count($students) . " (đã có lớp: " . count($existing_class) . ")");
$assigned = 0;
foreach ($students as $st) {
    $target = $classgroups[$st->id % 4];
    $current = $existing_class[$st->id] ?? null;
    if ($current === $target) {
        continue;
    }
    $assigned++;
    if ($dryrun) {
        $out("   - [DRY] user {$st->id} ({$st->username}) -> $target");
        continue;
    }
    if (isset($existing_class[$st->id])) {
        $DB->set_field('user_info_data', 'data', $target, ['userid' => $st->id, 'fieldid' => $field->id]);
    } else {
        $rec = (object)['userid' => $st->id, 'fieldid' => $field->id, 'data' => $target, 'dataformat' => 0];
        $DB->insert_record('user_info_data', $rec);
    }
}
$out("   ✓ Đã cập nhật lớp cho $assigned sinh viên");

/* ============================================================================
 * 2) Đảm bảo mỗi course có 1 giáo viên editingteacher
 * ========================================================================== */
$out("\n[2] Đảm bảo mỗi course có giáo viên editingteacher");
$role_editingteacher = $DB->get_record('role', ['shortname' => 'editingteacher']);
$courses = $DB->get_records_select('course', 'id > 1', null, 'id', 'id, shortname, fullname');

$teacher_pool = [
    'IT301'      => ['gv_it301',   'Nguyễn Văn',  'An',     'gv_it301@tvu.edu.vn'],
    'IT201'      => ['gv_it201',   'Trần Thị',    'Bình',   'gv_it201@tvu.edu.vn'],
    'IT302'      => ['gv_it302',   'Lê Văn',      'Cường',  'gv_it302@tvu.edu.vn'],
    'IT401'      => ['gv_it401',   'Phạm Thị',    'Dung',   'gv_it401@tvu.edu.vn'],
    'IT202'      => ['gv_it202',   'Hoàng Văn',   'Em',     'gv_it202@tvu.edu.vn'],
    'VITICHPHAN' => ['gv_vtp',     'Vũ Thị',      'Phương', 'gv_vtp@tvu.edu.vn'],
    'IT303'      => ['gv_it303',   'Đặng Văn',    'Giang',  'gv_it303@tvu.edu.vn'],
    'IT304'      => ['gv_it304',   'Bùi Thị',     'Hoa',    'gv_it304@tvu.edu.vn'],
    'IT305'      => ['gv_it305',   'Ngô Văn',     'Khoa',   'gv_it305@tvu.edu.vn'],
];

function ensure_user($username, $firstname, $lastname, $email) {
    global $DB;
    $u = $DB->get_record('user', ['username' => $username]);
    if ($u) {
        return $u;
    }
    $u = new stdClass();
    $u->username  = $username;
    $u->firstname = $firstname;
    $u->lastname  = $lastname;
    $u->email     = $email;
    $u->mnethostid = $CFG->mnet_localhost_id;
    $u->confirmed = 1;
    $u->auth      = 'manual';
    $u->id        = user_create_user($u, false, false);
    return $u;
}

function ensure_course_teacher($courseid, $userid, $roleid) {
    global $DB;
    $ctx = context_course::instance($courseid, IGNORE_MISSING);
    if (!$ctx) {
        return false;
    }
    if (is_enrolled($ctx, $userid)) {
        return true;
    }
    // Tạo manual enrol instance nếu chưa có
    $enrol = $DB->get_record('enrol', ['courseid' => $courseid, 'enrol' => 'manual', 'status' => 0]);
    if (!$enrol) {
        $enrol = new stdClass();
        $enrol->enrol      = 'manual';
        $enrol->status     = 0;
        $enrol->courseid   = $courseid;
        $enrol->sortorder  = 5;
        $enrol->name       = 'Manual enrol';
        $enrol->enrolstartdate = 0;
        $enrol->enrolenddate   = 0;
        $enrol->expirynotify   = 0;
        $enrol->expirytreshold = 0;
        $enrol->timecreated    = time();
        $enrol->timemodified   = time();
        $enrol->id = $DB->insert_record('enrol', $enrol);
    }
    $ue = new stdClass();
    $ue->status        = 0;
    $ue->enrolid       = $enrol->id;
    $ue->userid        = $userid;
    $ue->timestart     = time();
    $ue->timeend       = 0;
    $ue->modifierid    = 0;
    $ue->timecreated   = time();
    $ue->timemodified  = time();
    $DB->insert_record('user_enrolments', $ue);

    $ra = new stdClass();
    $ra->roleid        = $roleid;
    $ra->contextid     = $ctx->id;
    $ra->userid        = $userid;
    $ra->timemodified  = time();
    $ra->modifierid    = 0;
    $ra->component     = '';
    $ra->itemid        = 0;
    $ra->sortorder     = 0;
    $DB->insert_record('role_assignments', $ra);
    return true;
}

$teacher_count = 0;
foreach ($courses as $c) {
    $ctx = context_course::instance($c->id, IGNORE_MISSING);
    $has_teacher = $DB->record_exists_sql("
        SELECT 1 FROM {role_assignments} ra
         JOIN {context} ctx ON ctx.id = ra.contextid
         WHERE ctx.instanceid = ? AND ctx.contextlevel = 50 AND ra.roleid = ?
    ", [$c->id, $role_editingteacher->id]);
    if ($has_teacher) {
        $out("   = {$c->shortname} đã có editingteacher");
        continue;
    }
    $info = $teacher_pool[$c->shortname] ?? null;
    if (!$info) {
        $out("   ! {$c->shortname} không có GV trong pool, bỏ qua");
        continue;
    }
    [$uname, $fn, $ln, $em] = $info;
    if ($dryrun) {
        $out("   - [DRY] Tạo/Gán $uname cho {$c->shortname}");
        continue;
    }
    $user = ensure_user($uname, $fn, $ln, $em);
    ensure_course_teacher($c->id, $user->id, $role_editingteacher->id);
    $teacher_count++;
    $out("   + Tạo/gán $uname cho {$c->shortname}");
}
$out("   ✓ Đã thêm $teacher_count giáo viên");

/* ============================================================================
 * 3) Đảm bảo mỗi course có 5-7 bài assignment
 * ========================================================================== */
$out("\n[3] Đảm bảo mỗi course có 5-7 bài assignment");
$target_min = 5;
$target_max = 7;

$assign_titles_by_course = [
    'IT301'      => ['Lý thuyết Java', 'Thực hành OOP', 'Collection Framework', 'Java I/O & NIO', 'Multithreading', 'JDBC & Database', 'Dự án cuối kỳ'],
    'IT201'      => ['Mô hình ER', 'Truy vấn SQL cơ bản', 'Truy vấn SQL nâng cao', 'Chuẩn hóa CSDL', 'Transaction & Lock', 'Index & Optimization', 'NoSQL Overview'],
    'IT302'      => ['HTML/CSS cơ bản', 'JavaScript cơ bản', 'Responsive Web Design', 'RESTful API', 'Frontend Framework', 'Authentication & Session', 'Dự án Web Full-stack'],
    'IT401'      => ['Tìm kiếm heuristic', 'Machine Learning cơ bản', 'Neural Network', 'Xử lý ngôn ngữ tự nhiên', 'Thị giác máy tính', 'Reinforcement Learning', 'Dự án AI'],
    'IT202'      => ['Mô hình OSI & TCP/IP', 'Subnetting', 'Routing & Switching', 'Network Security', 'Wireless & Mobile', 'VPN & Firewall', 'Dự án mạng'],
    'VITICHPHAN' => ['Giới hạn và liên tục', 'Đạo hàm', 'Vi phân', 'Tích phân', 'Ứng dụng tích phân', 'Chuỗi số', 'Dự án cuối kỳ'],
    'IT303'      => ['Mảng & danh sách liên kết', 'Stack & Queue', 'Cây nhị phân', 'Đồ thị', 'Sắp xếp & tìm kiếm', 'Quy hoạch động', 'Dự án CTDL'],
    'IT304'      => ['Quản lý tiến trình', 'Đồng bộ hóa', 'Quản lý bộ nhớ', 'File system', 'I/O & Deadlock', 'Bảo mật HĐH', 'Dự án cuối kỳ'],
    'IT305'      => ['Cú pháp Python', 'List/Dict/Set', 'Hàm & Lambda', 'OOP trong Python', 'NumPy & Pandas', 'Django cơ bản', 'Dự án Web Python'],
];

function count_assigns($courseid) {
    global $DB;
    return $DB->count_records_sql("
        SELECT COUNT(cm.id) FROM {course_modules} cm
        JOIN {modules} m ON m.id = cm.module
        WHERE cm.course = ? AND m.name = 'assign'
    ", [$courseid]);
}

function create_assign_module($courseid, $name) {
    global $DB, $CFG;
    $now = time();
    $duedate = $now + 7 * 24 * 3600;
    $cutoffdate = $duedate + 24 * 3600;

    // 1) Tạo record mdl_assign
    $assign = new stdClass();
    $assign->course = $courseid;
    $assign->name = $name;
    $assign->intro = '<p>Bài tập: <strong>' . s($name) . '</strong></p>';
    $assign->introformat = FORMAT_HTML;
    $assign->alwaysshowdescription = 1;
    $assign->nosubmissions = 0;
    $assign->submissiondrafts = 0;
    $assign->sendnotifications = 0;
    $assign->sendlatenotifications = 0;
    $assign->duedate = $duedate;
    $assign->cutoffdate = $cutoffdate;
    $assign->gradingduedate = $cutoffdate;
    $assign->allowsubmissionsfromdate = $now;
    $assign->grade = 10;
    $assign->gradetype = GRADE_TYPE_VALUE;
    $assign->grademax = 10;
    $assign->grademin = 0;
    $assign->gradeformat = 0;
    $assign->timemodified = $now;
    $assign->requiresubmissionstatement = 0;
    $assign->completionsubmit = 1;
    $assign->teamsubmission = 0;
    $assign->requireallteammemberssubmit = 0;
    $assign->teamsubmissiongroupingid = 0;
    $assign->blindmarking = 0;
    $assign->hidegrader = 0;
    $assign->attemptreopenmethod = 'none';
    $assign->maxattempts = -1;
    $assign->markingworkflow = 0;
    $assign->markingallocation = 0;
    $assign->markinganonymous = 0;
    $assign->sendstudentnotifications = 1;
    $assign->preventsubmissionnotingroup = 0;
    $assign->revealidentities = 0;
    $assign->activity = $assign->intro;
    $assign->activityformat = FORMAT_HTML;
    $assign->timelimit = 0;
    $assign->submissionattachments = 0;
    $assign->assignsubmission_onlinetext_enabled = 1;
    $assign->assignsubmission_file_enabled = 1;
    $assign->assignsubmission_file_filetypes = '';
    $assign->assignsubmission_file_maxsizebytes = 0;
    $assign->assignsubmission_file_maxfiles = 1;
    $assign->assignfeedback_comments_enabled = 1;
    $assign->assignfeedback_comments_commentinline = 0;
    $assign->assignfeedback_file_enabled = 0;
    try {
        $id = $DB->insert_record('assign', $assign);
    } catch (Exception $e) {
        throw new moodle_exception('assign_insert_failed', '', '', null, $e->getMessage() . ' :: ' . $e->getTraceAsString());
    }

    // 2) Tạo course_module
    $module = $DB->get_record('modules', ['name' => 'assign']);
    $cm = new stdClass();
    $cm->course       = $courseid;
    $cm->module       = $module->id;
    $cm->instance     = $id;
    $cm->section      = 0;
    $cm->idnumber     = '';
    $cm->added        = $now;
    $cm->score        = 0;
    $cm->indent       = 0;
    $cm->visible      = 1;
    $cm->visibleoncoursepage = 1;
    $cm->visibleold   = 1;
    $cm->groupmode    = 0;
    $cm->groupingid   = 0;
    $cm->groupmembersonly = 0;
    $cm->completion            = 1;
    $cm->completiongradeitemnumber = null;
    $cm->completionview         = 0;
    $cm->completionexpected     = 0;
    $cm->completionpassgrade    = 0;
    $cm->availability          = null;
    $cm->showdescription       = 1;
    $cm->deletioninprogress    = 0;
    $cm->downloadcontent       = 1;
    $cm->lang                  = null;
    try {
        $cmid = $DB->insert_record('course_modules', $cm);
    } catch (Exception $e) {
        throw new moodle_exception('cm_insert_failed', '', '', null, $e->getMessage());
    }

    // 3) Update section phù hợp (topic 1)
    $sectionid = $DB->get_field('course_sections', 'id', ['course' => $courseid, 'section' => 1]);
    if (!$sectionid) {
        // Tạo section nếu chưa có
        $cw = new stdClass();
        $cw->course = $courseid;
        $cw->section = 1;
        $cw->summary = '';
        $cw->summaryformat = FORMAT_HTML;
        $cw->name = null;
        $cw->visible = 1;
        $sectionid = $DB->insert_record('course_sections', $cw);
    }
    $DB->set_field('course_modules', 'section', $sectionid, ['id' => $cmid]);
    $DB->set_field('assign', 'id', $id, ['id' => $id]); // touch

    // 4) Tạo grade_item
    $gi = new stdClass();
    $gi->courseid = $courseid;
    $gi->categoryid = null;
    $gi->itemname = $name;
    $gi->itemtype = 'mod';
    $gi->itemmodule = 'assign';
    $gi->iteminstance = $id;
    $gi->itemnumber = 0;
    $gi->iteminfo = '';
    $gi->calculation = '';
    $gi->gradetype = GRADE_TYPE_VALUE;
    $gi->grademax = 10;
    $gi->grademin = 0;
    $gi->gradepass = 0;
    $gi->multfactor = 1;
    $gi->plusfactor = 0;
    $gi->aggregationcoef = 0;
    $gi->sortorder = 0;
    $gi->display = 0;
    $gi->decimals = 2;
    $gi->hidden = 0;
    $gi->locked = 0;
    $gi->locktime = 0;
    $gi->needsupdate = 0;
    $gi->timecreated = $now;
    $gi->timemodified = $now;
    $gi->outcomeid = null;
    $gradeitemid = $DB->insert_record('grade_items', $gi);

    $DB->set_field('assign', 'grade', $gradeitemid, ['id' => $id]);

    // 5) Cập nhật section sequence
    $section = $DB->get_record('course_sections', ['id' => $sectionid], 'id, sequence');
    $section_sequence = $section && !empty($section->sequence) ? explode(',', $section->sequence) : [];
    $section_sequence[] = $cmid;
    $DB->set_field('course_sections', 'sequence', implode(',', $section_sequence), ['id' => $sectionid]);

    return $cmid;
}

$assign_added = 0;
foreach ($courses as $c) {
    $current = count_assigns($c->id);
    $titles  = $assign_titles_by_course[$c->shortname] ?? [];
    if (count($titles) < $target_max) {
        $titles = array_pad($titles, $target_max, 'Bài tập thêm');
    }
    if ($current >= $target_min) {
        $out("   = {$c->shortname} đã có $current bài assign (OK)");
        continue;
    }
    $needed = $target_max - $current;
    for ($i = $current; $i < $target_max && $i < count($titles); $i++) {
        $title = $titles[$i];
        if ($dryrun) {
            $out("   - [DRY] {$c->shortname}: tạo '$title'");
            $assign_added++;
            continue;
        }
        $newid = create_assign_module($c->id, $title);
        $assign_added++;
        $out("   + {$c->shortname}: tạo assign '$title' (cmid=$newid)");
    }
    if (!$dryrun) {
        // Bỏ qua grade_regrade_final_grades
    }
}
$out("   ✓ Đã thêm $assign_added bài assignment");

/* ============================================================================
 * 4) Random sinh viên nộp bài / không nộp với điểm ngẫu nhiên
 * ========================================================================== */
$out("\n[4] Random sinh viên nộp bài (60-90% nộp) với điểm 0-10");
$assigns_in_courses = $DB->get_records_sql("
    SELECT cm.id AS cmid, a.id AS assignid, a.course, a.name
      FROM {course_modules} cm
      JOIN {modules} m ON m.id = cm.module
      JOIN {assign} a ON a.id = cm.instance
");
$submitted_total = 0;
$graded_total    = 0;
foreach ($assigns_in_courses as $ai) {
    // Lấy danh sách sinh viên đã enroll khóa này
    $students_in_course = $DB->get_records_sql("
        SELECT ue.userid
          FROM {user_enrolments} ue
          JOIN {enrol} e ON e.id = ue.enrolid
          JOIN {role_assignments} ra ON ra.userid = ue.userid
          JOIN {role} r ON r.id = ra.roleid
          JOIN {context} ctx ON ctx.id = ra.contextid
         WHERE e.courseid = ?
           AND r.shortname = 'student'
           AND ue.status = 0
           AND ctx.contextlevel = 50
           AND ctx.instanceid = e.courseid
    ", [$ai->course]);
    if (!$students_in_course) {
        continue;
    }
    foreach ($students_in_course as $st) {
        // Bỏ qua nếu đã có submission
        if ($DB->record_exists('assign_submission', ['assignment' => $ai->assignid, 'userid' => $st->userid])) {
            continue;
        }
        $rand = mt_rand(0, 100);
        if ($rand > 80) {
            // 20% không nộp
            continue;
        }
        $now = time();
        $status = ($rand > 5) ? 'submitted' : 'draft'; // 5% draft
        $sub = new stdClass();
        $sub->assignment = $ai->assignid;
        $sub->userid = $st->userid;
        $sub->timecreated = $now - mt_rand(0, 5 * 24 * 3600);
        $sub->timemodified = $now;
        $sub->status = $status;
        $sub->groupid = 0;
        $sub->attemptnumber = 0;
        $sub->latest = 1;
        $subid = $DB->insert_record('assign_submission', $sub);
        $submitted_total++;
        if ($status !== 'submitted') {
            continue;
        }
        // Tạo onlinetext + file submission để giống thật
        $ots = new stdClass();
        $ots->assignment = $ai->assignid;
        $ots->submission = $subid;
        $ots->onlinetext = '<p>Bài làm của sinh viên cho bài tập "' . s($ai->name) . '"</p>';
        $ots->onlineformat = FORMAT_HTML;
        $DB->insert_record('assignsubmission_onlinetext', $ots);

        // Tạo grade ngẫu nhiên 4-10
        $grade = mt_rand(40, 100) / 10.0;
        $now2 = time();
        $grade_rec = new stdClass();
        $grade_rec->assignment = $ai->assignid;
        $grade_rec->userid = $st->userid;
        $grade_rec->timecreated = $now2;
        $grade_rec->timemodified = $now2;
        $grade_rec->grader = -1; // system
        $grade_rec->grade = $grade;
        $grade_rec->gradeformat = 0;
        $grade_rec->attemptnumber = 0;
        $DB->insert_record('assign_grades', $grade_rec);
        $graded_total++;

        // Cập nhật assign_user_flags
        $flags = $DB->get_record('assign_user_flags', ['assignment' => $ai->assignid, 'userid' => $st->userid]);
        $fdata = (object)[
            'assignment' => $ai->assignid, 'userid' => $st->userid,
            'locked' => 0, 'extensionduedate' => 0, 'workflowstate' => 'graded',
            'allocatedmarker' => 0,
        ];
        if ($flags) {
            $fdata->id = $flags->id;
            $DB->update_record('assign_user_flags', $fdata);
        } else {
            $DB->insert_record('assign_user_flags', $fdata);
        }
    }
    if (!$dryrun) {
        // Bỏ qua grade_regrade_final_grades
    }
}
$out("   ✓ Đã tạo $submitted_total submission, $graded_total lượt chấm điểm");

/* ============================================================================
 * 5) Chia 2 nhóm + grouping cho mỗi khóa học
 * ========================================================================== */
$out("\n[5] Chia 2 nhóm (Nhóm 1, Nhóm 2) + 1 grouping cho mỗi course");
$grouped_total = 0;
foreach ($courses as $c) {
    // Kiểm tra đã có 2 group chưa
    $existing_groups = $DB->get_records('groups', ['courseid' => $c->id], 'id', 'id, name');
    $g1 = $g2 = null;
    foreach ($existing_groups as $g) {
        if (stripos($g->name, 'nhóm 1') !== false || strcasecmp($g->name, 'nhom 1') === 0) {
            $g1 = $g;
        } elseif (stripos($g->name, 'nhóm 2') !== false || strcasecmp($g->name, 'nhom 2') === 0) {
            $g2 = $g;
        }
    }
    if (!$g1) {
        $g1 = new stdClass();
        $g1->courseid = $c->id;
        $g1->name = 'Nhóm 1';
        $g1->description = 'Nhóm 1 của môn học';
        $g1->descriptionformat = FORMAT_HTML;
        $g1->enrolmentkey = '';
        $g1->picture = 0;
        $g1->hidepicture = 0;
        $g1->timecreated = time();
        $g1->timemodified = time();
        $g1->id = $DB->insert_record('groups', $g1);
    }
    if (!$g2) {
        $g2 = new stdClass();
        $g2->courseid = $c->id;
        $g2->name = 'Nhóm 2';
        $g2->description = 'Nhóm 2 của môn học';
        $g2->descriptionformat = FORMAT_HTML;
        $g2->enrolmentkey = '';
        $g2->picture = 0;
        $g2->hidepicture = 0;
        $g2->timecreated = time();
        $g2->timemodified = time();
        $g2->id = $DB->insert_record('groups', $g2);
    }

    // Tạo grouping
    $existing_grouping = $DB->get_record('groupings', ['courseid' => $c->id, 'name' => 'Phân nhóm']);
    if (!$existing_grouping) {
        $grouping = new stdClass();
        $grouping->courseid = $c->id;
        $grouping->name = 'Phân nhóm';
        $grouping->description = 'Phân nhóm Nhóm 1 + Nhóm 2 cho môn học';
        $grouping->descriptionformat = FORMAT_HTML;
        $grouping->configdata = '';
        $grouping->timecreated = time();
        $grouping->timemodified = time();
        $grouping->id = $DB->insert_record('groupings', $grouping);
        $DB->insert_record('groupings_groups', ['groupingid' => $grouping->id, 'groupid' => $g1->id, 'timeadded' => time()]);
        $DB->insert_record('groupings_groups', ['groupingid' => $grouping->id, 'groupid' => $g2->id, 'timeadded' => time()]);
    }

    // Phân sinh viên vào 2 nhóm theo parity userid
    $enrolled = $DB->get_records_sql("
        SELECT ue.userid
          FROM {user_enrolments} ue
          JOIN {enrol} e ON e.id = ue.enrolid
          JOIN {role_assignments} ra ON ra.userid = ue.userid
          JOIN {role} r ON r.id = ra.roleid
          JOIN {context} ctx ON ctx.id = ra.contextid
         WHERE e.courseid = ?
           AND r.shortname = 'student'
           AND ue.status = 0
           AND ctx.contextlevel = 50
           AND ctx.instanceid = e.courseid
    ", [$c->id]);
    $g1_count = $g2_count = 0;
    foreach ($enrolled as $st) {
        $groupid = (($st->userid % 2) === 0) ? $g1->id : $g2->id;
        if ($DB->record_exists('groups_members', ['groupid' => $groupid, 'userid' => $st->userid])) {
            ($groupid == $g1->id) ? $g1_count++ : $g2_count++;
            continue;
        }
        if ($dryrun) {
            $out("   - [DRY] {$c->shortname}: user $st->userid -> group " . ($groupid == $g1->id ? '1' : '2'));
            continue;
        }
        $DB->insert_record('groups_members', [
            'groupid' => $groupid, 'userid' => $st->userid,
            'timeadded' => time(), 'component' => '', 'itemid' => 0,
        ]);
        $grouped_total++;
        ($groupid == $g1->id) ? $g1_count++ : $g2_count++;
    }
    $out("   = {$c->shortname}: Nhóm 1=$g1_count, Nhóm 2=$g2_count");
}
$out("   ✓ Đã thêm $grouped_total lượt phân nhóm");

/* ============================================================================
 * Rebuild course caches
 * ========================================================================== */
$out("\n[6] Rebuild course cache");
if (!$dryrun) {
    foreach ($courses as $c) {
        rebuild_course_cache($c->id, true);
    }
    $out("   ✓ Đã rebuild cache cho " . count($courses) . " course");
} else {
    $out("   = [DRY] bỏ qua rebuild cache");
}

$out("\n" . str_repeat('=', 72));
$out('HOÀN TẤT SEED DỮ LIỆU  -  ' . ($dryrun ? '[DRY-RUN]' : '[LIVE]'));
$out(str_repeat('=', 72));
