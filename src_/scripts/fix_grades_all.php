<?php
/**
 * Fix: Chấm điểm cho TẤT CẢ submission chưa có grade.
 * Đảm bảo mỗi sinh viên tham gia môn học đều có điểm (assign_grades).
 */
define('NO_OUTPUT_BUFFERING', true);
define('CLI_SCRIPT', true);
require_once(__DIR__ . '/../config.php');
require_once($CFG->dirroot . '/mod/assign/lib.php');
require_once($CFG->dirroot . '/lib/gradelib.php');

$out = function($s){ echo $s."\n"; @ob_flush(); @flush(); };

$out(str_repeat('=', 72));
$out('FIX GRADES - chấm điểm cho mọi submission');
$out(str_repeat('=', 72));

mt_srand(20260617);

/* Tìm tất cả submission chưa có grade */
$pending = $DB->get_records_sql(
    "SELECT s.id AS subid, s.assignment, s.userid, a.course, a.name
       FROM {assign_submission} s
       JOIN {assign} a ON a.id = s.assignment
       JOIN {course} c ON c.id = a.course
      WHERE c.id > 1
        AND s.status = 'submitted'
        AND NOT EXISTS (
            SELECT 1 FROM {assign_grades} g
             WHERE g.assignment = s.assignment AND g.userid = s.userid
        )"
);

$out("\n[A] Tổng submission cần chấm: " . count($pending));

$grade_count = 0;
$now = time();
foreach ($pending as $p) {
    $grade = mt_rand(50, 95) / 10.0;
    $rec = new stdClass();
    $rec->assignment = $p->assignment;
    $rec->userid = $p->userid;
    $rec->timecreated = $now - mt_rand(1, 5) * 24 * 3600;
    $rec->timemodified = $now;
    $rec->grader = -1;
    $rec->grade = $grade;
    $rec->gradeformat = 0;
    $rec->attemptnumber = 0;
    $DB->insert_record('assign_grades', $rec);
    $grade_count++;

    // Cập nhật user_flags
    $flags = $DB->get_record('assign_user_flags',
        ['assignment' => $p->assignment, 'userid' => $p->userid]);
    $fdata = (object)[
        'assignment' => $p->assignment, 'userid' => $p->userid,
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
$out("   ✓ Đã chấm $grade_count bài");

/* Trigger gradebook recompute */
$out("\n[B] Recompute gradebook cho mọi course");
$courses = $DB->get_records_select('course', 'id > 1');
foreach ($courses as $c) {
    grade_regrade_final_grades($c->id);
    rebuild_course_cache($c->id, true);
    $out("   ✓ Course {$c->shortname} - OK");
}

$out("\n" . str_repeat('=', 72));
$out('HOÀN TẤT CHẤM ĐIỂM');
$out(str_repeat('=', 72));
