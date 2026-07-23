<?php
/**
 * Inspect & fix:
 *  - Course summary (giới thiệu môn học) cho mọi course id > 1
 *  - Đảm bảo mỗi course có >= 5 assignment
 *  - Random sinh viên tham gia môn nộp bài & được chấm điểm
 *    (bổ sung cho SV chưa có submission trong course đó)
 *
 * Chạy CLI:  php scripts/fix_courses_full.php
 */

define('NO_OUTPUT_BUFFERING', true);
define('CLI_SCRIPT', php_sapi_name() === 'cli');

require_once(__DIR__ . '/../config.php');
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

$iscli = CLI_SCRIPT;
$out   = function ($line) use ($iscli) {
    echo ($iscli ? $line : nl2br(htmlspecialchars($line))) . "\n";
    @ob_flush(); @flush();
};

$out(str_repeat('=', 72));
$out('FIX COURSES - summary, assignments, submissions & grades');
$out(str_repeat('=', 72));

mt_srand(20260617);

$course_meta = [
    'IT301' => [
        'name' => 'Lập trình Java',
        'intro' => '<p><strong>Lập trình Java (IT301)</strong> là môn học nền tảng về ngôn ngữ lập trình hướng đối tượng Java.</p>'
            . '<p>Môn học trang bị cho sinh viên:</p><ul>'
            . '<li>Cú pháp Java, OOP (kế thừa, đa hình, đóng gói, trừu tượng)</li>'
            . '<li>Collection Framework, Generic, Lambda Expression</li>'
            . '<li>Java I/O, NIO, Multithreading, Concurrency</li>'
            . '<li>Kết nối cơ sở dữ liệu với JDBC</li>'
            . '<li>Xây dựng ứng dụng desktop và web backend bằng Java</li></ul>'
            . '<p><em>Phù hợp với sinh viên năm 3 ngành Công nghệ thông tin.</em></p>',
    ],
    'IT201' => [
        'name' => 'Cơ sở dữ liệu',
        'intro' => '<p><strong>Cơ sở dữ liệu (IT201)</strong> cung cấp kiến thức nền tảng về thiết kế, xây dựng và quản trị cơ sở dữ liệu quan hệ.</p>'
            . '<p>Nội dung chính:</p><ul>'
            . '<li>Mô hình thực thể - mối quan hệ (ER), mô hình quan hệ</li>'
            . '<li>Ngôn ngữ SQL: DDL, DML, DCL, truy vấn nâng cao</li>'
            . '<li>Phụ thuộc hàm, chuẩn hóa CSDL (1NF, 2NF, 3NF, BCNF)</li>'
            . '<li>Transaction, đồng thuận, khóa (lock) và phục hồi dữ liệu</li>'
            . '<li>Index, tối ưu truy vấn và NoSQL cơ bản</li></ul>'
            . '<p><em>Môn tiên quyết cho các môn phát triển phần mềm và hệ thống thông tin.</em></p>',
    ],
    'IT302' => [
        'name' => 'Lập trình Web',
        'intro' => '<p><strong>Lập trình Web (IT302)</strong> giúp sinh viên xây dựng các ứng dụng web hiện đại từ frontend đến backend.</p>'
            . '<p>Chủ đề bao gồm:</p><ul>'
            . '<li>HTML5, CSS3, JavaScript ES6+</li>'
            . '<li>Responsive Web Design, Bootstrap, Tailwind</li>'
            . '<li>RESTful API với Node.js / Spring Boot</li>'
            . '<li>Frontend Framework: React / Vue</li>'
            . '<li>Authentication (JWT, OAuth2), Session, Cookie</li>'
            . '<li>Triển khai dự án web full-stack</li></ul>'
            . '<p><em>Yêu cầu: đã học qua Cơ sở dữ liệu và Lập trình hướng đối tượng.</em></p>',
    ],
    'IT401' => [
        'name' => 'Trí tuệ nhân tạo',
        'intro' => '<p><strong>Trí tuệ nhân tạo (IT401)</strong> giới thiệu các kỹ thuật và thuật toán cốt lõi của AI hiện đại.</p>'
            . '<p>Nội dung trọng tâm:</p><ul>'
            . '<li>Tìm kiếm heuristic, A*, tìm kiếm đối kháng</li>'
            . '<li>Machine Learning cơ bản: hồi quy, phân lớp, clustering</li>'
            . '<li>Neural Network, Deep Learning</li>'
            . '<li>Xử lý ngôn ngữ tự nhiên (NLP) với Transformer</li>'
            . '<li>Thị giác máy tính (CNN, YOLO)</li>'
            . '<li>Reinforcement Learning và ứng dụng thực tế</li></ul>'
            . '<p><em>Môn tự chọn nâng cao cho SV năm 4 ngành CNTT.</em></p>',
    ],
    'IT202' => [
        'name' => 'Mạng máy tính',
        'intro' => '<p><strong>Mạng máy tính (IT202)</strong> trang bị kiến thức về kiến trúc, giao thức và vận hành hệ thống mạng.</p>'
            . '<p>Chủ đề chính:</p><ul>'
            . '<li>Mô hình OSI 7 lớp và TCP/IP 4 lớp</li>'
            . '<li>Địa chỉ IP, Subnetting, VLSM, Supernetting</li>'
            . '<li>Routing, Switching (static & dynamic: RIP, OSPF, BGP)</li>'
            . '<li>An toàn mạng, VPN, Firewall, IDS/IPS</li>'
            . '<li>Wireless, mạng di động 4G/5G</li>'
            . '<li>Thực hành cấu hình thiết bị Cisco</li></ul>'
            . '<p><em>Đi kèm bài lab thực tế trên Packet Tracer / GNS3.</em></p>',
    ],
    'VITICHPHAN' => [
        'name' => 'Vi tích phân',
        'intro' => '<p><strong>Vi tích phân (VITICHPHAN)</strong> là môn toán cơ bản cung cấp công cụ tính toán cho khoa học và kỹ thuật.</p>'
            . '<p>Nội dung học:</p><ul>'
            . '<li>Giới hạn, liên tục, vô cùng bé - vô cùng lớn</li>'
            . '<li>Đạo hàm, vi phân và ứng dụng</li>'
            . '<li>Tích phân bất định, xác định, suy rộng</li>'
            . '<li>Ứng dụng tích phân: tính diện tích, thể tích, độ dài cung</li>'
            . '<li>Chuỗi số, chuỗi hàm, chuỗi lũy thừa</li></ul>'
            . '<p><em>Môn nền tảng cho Vật lý, Xác suất thống kê và các môn chuyên ngành.</em></p>',
    ],
    'IT303' => [
        'name' => 'Cấu trúc dữ liệu & Giải thuật',
        'intro' => '<p><strong>Cấu trúc dữ liệu và Giải thuật (IT303)</strong> là môn cốt lõi giúp sinh viên tư duy giải quyết vấn đề bằng lập trình.</p>'
            . '<p>Bao gồm:</p><ul>'
            . '<li>Mảng, danh sách liên kết (đơn, đôi, vòng)</li>'
            . '<li>Stack, Queue, Deque và ứng dụng</li>'
            . '<li>Cây nhị phân, cây AVL, cây đỏ-đen, B-Tree</li>'
            . '<li>Đồ thị: BFS, DFS, Dijkstra, Floyd-Warshall</li>'
            . '<li>Các thuật toán sắp xếp và tìm kiếm kinh điển</li>'
            . '<li>Quy hoạch động, Quay lui, Nhánh cận</li></ul>'
            . '<p><em>Rèn luyện kỹ năng phân tích độ phức tạp O(n).</em></p>',
    ],
    'IT304' => [
        'name' => 'Hệ điều hành',
        'intro' => '<p><strong>Hệ điều hành (IT304)</strong> nghiên cứu nguyên lý hoạt động của các hệ điều hành hiện đại (Linux, Windows).</p>'
            . '<p>Trọng tâm:</p><ul>'
            . '<li>Quản lý tiến trình và luồng (Process, Thread)</li>'
            . '<li>Đồng bộ hóa, semaphore, monitor, deadlock</li>'
            . '<li>Quản lý bộ nhớ: phân trang, phân đoạn, bộ nhớ ảo</li>'
            . '<li>File system: FAT, NTFS, ext4</li>'
            . '<li>Quản lý I/O, scheduler (FCFS, SJF, RR, MLFQ)</li>'
            . '<li>Bảo mật và cơ chế bảo vệ trong HĐH</li></ul>'
            . '<p><em>Có bài lab thực hành trên Linux Ubuntu.</em></p>',
    ],
    'IT305' => [
        'name' => 'Lập trình Python',
        'intro' => '<p><strong>Lập trình Python (IT305)</strong> giúp sinh viên làm chủ ngôn ngữ Python từ cơ bản đến ứng dụng thực tế.</p>'
            . '<p>Nội dung:</p><ul>'
            . '<li>Cú pháp Python, biến, kiểu dữ liệu, toán tử</li>'
            . '<li>List, Tuple, Set, Dict và comprehension</li>'
            . '<li>Hàm, lambda, decorator, generator</li>'
            . '<li>OOP trong Python: class, kế thừa, magic method</li>'
            . '<li>NumPy, Pandas cho phân tích dữ liệu</li>'
            . '<li>Django/Flask cho phát triển web</li>'
            . '<li>Dự án cuối kỳ: web app hoàn chỉnh</li></ul>'
            . '<p><em>Phù hợp cho cả người mới bắt đầu lập trình.</em></p>',
    ],
];

/* ============================================================================
 * A) INSPECT trạng thái hiện tại
 * ========================================================================== */
$out("\n[A] Tình trạng hiện tại");
$courses = $DB->get_records_select('course', 'id > 1', null, 'shortname', 'id, shortname, fullname, summary, summaryformat');
foreach ($courses as $c) {
    $has_summary = !empty(trim(strip_tags($c->summary)));
    $n_assigns   = $DB->count_records_sql(
        "SELECT COUNT(cm.id) FROM {course_modules} cm
           JOIN {modules} m ON m.id = cm.module
          WHERE cm.course = ? AND m.name = 'assign'",
        [$c->id]
    );
    $n_students = $DB->count_records_sql(
        "SELECT COUNT(DISTINCT ra.userid) FROM {role_assignments} ra
           JOIN {context} ctx ON ctx.id = ra.contextid
           JOIN {role} r ON r.id = ra.roleid
          WHERE ctx.instanceid = ? AND ctx.contextlevel = 50 AND r.shortname = 'student'",
        [$c->id]
    );
    $n_subs = $DB->count_records_sql(
        "SELECT COUNT(*) FROM {assign_submission} s
           JOIN {assign} a ON a.id = s.assignment
          WHERE a.course = ?",
        [$c->id]
    );
    $n_graded = $DB->count_records_sql(
        "SELECT COUNT(*) FROM {assign_grades} g
           JOIN {assign} a ON a.id = g.assignment
          WHERE a.course = ?",
        [$c->id]
    );
    $short = $c->shortname;
    $meta  = $course_meta[$short] ?? null;
    $name  = $meta ? $meta['name'] : $c->fullname;
    $out(sprintf("   %-12s | intro=%-3s | assigns=%-2d | students=%-3d | subs=%-4d | grades=%-4d | %s",
        $short, $has_summary ? 'YES' : 'NO', $n_assigns, $n_students, $n_subs, $n_graded,
        mb_substr($name, 0, 50)
    ));
}

/* ============================================================================
 * B) Cập nhật summary (giới thiệu môn học) cho mỗi course
 * ========================================================================== */
$out("\n[B] Cập nhật summary (giới thiệu môn học)");
$summary_updated = 0;
foreach ($courses as $c) {
    $meta = $course_meta[$c->shortname] ?? null;
    if (!$meta) {
        $out("   ! {$c->shortname}: không có meta, bỏ qua");
        continue;
    }
    if (trim(strip_tags($c->summary)) === trim(strip_tags($meta['intro']))) {
        $out("   = {$c->shortname}: summary đã đúng");
        continue;
    }
    $DB->set_field('course', 'summary', $meta['intro'], ['id' => $c->id]);
    $DB->set_field('course', 'summaryformat', FORMAT_HTML, ['id' => $c->id]);
    $DB->set_field('course', 'timemodified', time(), ['id' => $c->id]);
    $summary_updated++;
    $out("   + {$c->shortname}: đã cập nhật summary (giới thiệu mới)");
}
$out("   ✓ Đã cập nhật $summary_updated summary");

/* ============================================================================
 * C) Đảm bảo mỗi course có >= 5 assignment (nếu thiếu thì tạo thêm)
 * ========================================================================== */
$out("\n[C] Đảm bảo mỗi course có >= 5 assignment");
$target_assigns = 5;

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

function count_assigns_fix($courseid) {
    global $DB;
    return $DB->count_records_sql(
        "SELECT COUNT(cm.id) FROM {course_modules} cm
           JOIN {modules} m ON m.id = cm.module
          WHERE cm.course = ? AND m.name = 'assign'",
        [$courseid]
    );
}

function list_existing_assign_titles($courseid) {
    global $DB;
    $rows = $DB->get_records_sql(
        "SELECT a.id, a.name FROM {assign} a
           JOIN {course_modules} cm ON cm.instance = a.id
           JOIN {modules} m ON m.id = cm.module
          WHERE a.course = ? AND m.name = 'assign'
          ORDER BY a.id",
        [$courseid]
    );
    $out = [];
    foreach ($rows as $r) {
        $out[strtolower(trim($r->name))] = true;
    }
    return $out;
}

function create_assign_module_fix($courseid, $name) {
    global $DB;
    $now = time();
    $duedate = $now + 7 * 24 * 3600;
    $cutoffdate = $duedate + 24 * 3600;

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
    $id = $DB->insert_record('assign', $assign);

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
    $cmid = $DB->insert_record('course_modules', $cm);

    $sectionid = $DB->get_field('course_sections', 'id', ['course' => $courseid, 'section' => 1]);
    if (!$sectionid) {
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

    $section = $DB->get_record('course_sections', ['id' => $sectionid], 'id, sequence');
    $seq = $section && !empty($section->sequence) ? explode(',', $section->sequence) : [];
    $seq[] = $cmid;
    $DB->set_field('course_sections', 'sequence', implode(',', $seq), ['id' => $sectionid]);

    return $cmid;
}

$assign_added = 0;
foreach ($courses as $c) {
    $current = count_assigns_fix($c->id);
    $existing_titles = list_existing_assign_titles($c->id);
    $titles = $assign_titles_by_course[$c->shortname] ?? [];

    if ($current >= $target_assigns) {
        $out("   = {$c->shortname}: đã có $current bài assign (>= $target_assigns)");
        continue;
    }

    $needed = $target_assigns - $current;
    $added  = 0;
    foreach ($titles as $title) {
        if ($added >= $needed) break;
        if (isset($existing_titles[strtolower(trim($title))])) continue;
        create_assign_module_fix($c->id, $title);
        $added++;
        $assign_added++;
        $out("   + {$c->shortname}: tạo assign '$title'");
    }
    // Nếu vẫn thiếu, bổ sung bằng tên generic
    $i = 1;
    while ($added < $needed) {
        $title = "Bài tập bổ sung $i";
        if (!isset($existing_titles[strtolower(trim($title))])) {
            create_assign_module_fix($c->id, $title);
            $added++;
            $assign_added++;
            $out("   + {$c->shortname}: tạo assign '$title'");
        }
        $i++;
    }
}
$out("   ✓ Đã thêm $assign_added assignment mới");

/* ============================================================================
 * D) Random sinh viên nộp bài & chấm điểm (cho SV tham gia môn học)
 * ========================================================================== */
$out("\n[D] Sinh viên nộp bài & chấm điểm (80-100% tùy course)");
$assigns_in_courses = $DB->get_records_sql(
    "SELECT cm.id AS cmid, a.id AS assignid, a.course, a.name
       FROM {course_modules} cm
       JOIN {modules} m ON m.id = cm.module
       JOIN {assign} a ON a.id = cm.instance
       JOIN {course} c ON c.id = a.course
      WHERE c.id > 1"
);
$submitted_total = 0;
$graded_total    = 0;
$skipped_total   = 0;
foreach ($assigns_in_courses as $ai) {
    $students_in_course = $DB->get_records_sql(
        "SELECT ue.userid
           FROM {user_enrolments} ue
           JOIN {enrol} e ON e.id = ue.enrolid
           JOIN {role_assignments} ra ON ra.userid = ue.userid
           JOIN {role} r ON r.id = ra.roleid
           JOIN {context} ctx ON ctx.id = ra.contextid
          WHERE e.courseid = ?
            AND r.shortname = 'student'
            AND ue.status = 0
            AND ctx.contextlevel = 50
            AND ctx.instanceid = e.courseid",
        [$ai->course]
    );
    if (!$students_in_course) {
        continue;
    }
    foreach ($students_in_course as $st) {
        if ($DB->record_exists('assign_submission', ['assignment' => $ai->assignid, 'userid' => $st->userid])) {
            $skipped_total++;
            continue;
        }
        $rand = mt_rand(0, 100);
        // 100% sinh viên đều có submission (vì user muốn "đều có điểm")
        if ($rand > 100) {
            continue;
        }
        $now = time();
        $status = 'submitted';

        $sub = new stdClass();
        $sub->assignment = $ai->assignid;
        $sub->userid = $st->userid;
        $sub->timecreated = $now - mt_rand(1, 10) * 24 * 3600;
        $sub->timemodified = $now;
        $sub->status = $status;
        $sub->groupid = 0;
        $sub->attemptnumber = 0;
        $sub->latest = 1;
        $subid = $DB->insert_record('assign_submission', $sub);
        $submitted_total++;

        $ots = new stdClass();
        $ots->assignment = $ai->assignid;
        $ots->submission = $subid;
        $ots->onlinetext = '<p>Bài làm của sinh viên cho bài tập "' . s($ai->name) . '"</p>';
        $ots->onlineformat = FORMAT_HTML;
        $DB->insert_record('assignsubmission_onlinetext', $ots);

        // Điểm 5.0 - 9.5 (realistic, không quá thấp / quá cao)
        $grade = mt_rand(50, 95) / 10.0;
        $now2 = time();
        $grade_rec = new stdClass();
        $grade_rec->assignment = $ai->assignid;
        $grade_rec->userid = $st->userid;
        $grade_rec->timecreated = $now2;
        $grade_rec->timemodified = $now2;
        $grade_rec->grader = -1;
        $grade_rec->grade = $grade;
        $grade_rec->gradeformat = 0;
        $grade_rec->attemptnumber = 0;
        $DB->insert_record('assign_grades', $grade_rec);
        $graded_total++;

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
}
$out("   ✓ Tạo mới $submitted_total submission, $graded_total lượt chấm điểm (skipped $skipped_total đã có)");

/* ============================================================================
 * E) Rebuild course cache
 * ========================================================================== */
$out("\n[E] Rebuild course cache");
foreach ($courses as $c) {
    rebuild_course_cache($c->id, true);
}
$out("   ✓ Đã rebuild cache cho " . count($courses) . " course");

/* ============================================================================
 * F) VERIFY
 * ========================================================================== */
$out("\n[F] Kết quả sau fix");
$total_subs = 0;
$total_grades = 0;
foreach ($courses as $c) {
    $has_summary = !empty(trim(strip_tags($c->summary)));
    $n_assigns   = count_assigns_fix($c->id);
    $n_students  = $DB->count_records_sql(
        "SELECT COUNT(DISTINCT ra.userid) FROM {role_assignments} ra
           JOIN {context} ctx ON ctx.id = ra.contextid
           JOIN {role} r ON r.id = ra.roleid
          WHERE ctx.instanceid = ? AND ctx.contextlevel = 50 AND r.shortname = 'student'",
        [$c->id]
    );
    $n_subs = $DB->count_records_sql(
        "SELECT COUNT(*) FROM {assign_submission} s JOIN {assign} a ON a.id = s.assignment WHERE a.course = ?",
        [$c->id]
    );
    $n_graded = $DB->count_records_sql(
        "SELECT COUNT(*) FROM {assign_grades} g JOIN {assign} a ON a.id = g.assignment WHERE a.course = ?",
        [$c->id]
    );
    $total_subs += $n_subs;
    $total_grades += $n_graded;
    $out(sprintf("   %-12s | intro=%-3s | assigns=%-2d | students=%-3d | subs=%-4d | grades=%-4d",
        $c->shortname, $has_summary ? 'YES' : 'NO', $n_assigns, $n_students, $n_subs, $n_graded
    ));
}
$out("   Tổng: $total_subs submissions, $total_grades grades trên toàn hệ thống");

$out("\n" . str_repeat('=', 72));
$out('HOÀN TẤT FIX COURSES');
$out(str_repeat('=', 72));
