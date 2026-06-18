<?php
define('CLI_SCRIPT', true);
require_once(__DIR__ . '/../config.php');
$out = function($s){ echo $s."\n"; @ob_flush(); @flush(); };
$out(str_repeat('=', 80));
$out('VERIFY (sau khi script fix chạy xong)');
$out(str_repeat('=', 80));
$courses = $DB->get_records_select('course', 'id > 1', null, 'shortname', 'id, shortname, fullname, summary, summaryformat');
foreach ($courses as $c) {
    $has_summary = !empty(trim(strip_tags($c->summary)));
    $n_assigns   = $DB->count_records_sql(
        "SELECT COUNT(cm.id) FROM {course_modules} cm
           JOIN {modules} m ON m.id = cm.module
          WHERE cm.course = ? AND m.name = 'assign'", [$c->id]);
    $n_students = $DB->count_records_sql(
        "SELECT COUNT(DISTINCT ra.userid) FROM {role_assignments} ra
           JOIN {context} ctx ON ctx.id = ra.contextid
           JOIN {role} r ON r.id = ra.roleid
          WHERE ctx.instanceid = ? AND ctx.contextlevel = 50 AND r.shortname = 'student'", [$c->id]);
    $n_subs = $DB->count_records_sql(
        "SELECT COUNT(*) FROM {assign_submission} s JOIN {assign} a ON a.id = s.assignment WHERE a.course = ?", [$c->id]);
    $n_graded = $DB->count_records_sql(
        "SELECT COUNT(*) FROM {assign_grades} g JOIN {assign} a ON a.id = g.assignment WHERE a.course = ?", [$c->id]);

    // Số SV tham gia môn có submission
    $students_with_sub = $DB->count_records_sql(
        "SELECT COUNT(DISTINCT s.userid) FROM {assign_submission} s
           JOIN {assign} a ON a.id = s.assignment
          WHERE a.course = ?", [$c->id]);
    $students_with_grade = $DB->count_records_sql(
        "SELECT COUNT(DISTINCT g.userid) FROM {assign_grades} g
           JOIN {assign} a ON a.id = g.assignment
          WHERE a.course = ?", [$c->id]);

    $out(sprintf("   %-12s | intro=%-3s | assigns=%-2d | students=%-3d | subs=%-4d | grades=%-4d | SV_co_sub=%-3d | SV_co_grade=%-3d",
        $c->shortname, $has_summary ? 'YES' : 'NO', $n_assigns, $n_students, $n_subs, $n_graded, $students_with_sub, $students_with_grade));
}
