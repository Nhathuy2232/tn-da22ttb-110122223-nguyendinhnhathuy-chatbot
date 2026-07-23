<?php
/**
 * Script tạo submission + grade bằng SQL trực tiếp
 */
define('CLI_SCRIPT', true);
require_once('C:/xampp/htdocs/moodle/config.php');
global $DB;

$now = time();
mt_srand(42);

$totalSubs = 0;
$totalMissing = 0;
$allAssigns = $DB->get_records('assign');
foreach ($allAssigns as $assign) {
    $students = $DB->get_records_sql(
        "SELECT u.id, u.username FROM mdl_user u " .
        "JOIN mdl_user_enrolments ue ON ue.userid = u.id " .
        "JOIN mdl_enrol e ON e.id = ue.enrolid " .
        "WHERE e.courseid = ? AND u.username REGEXP '^1101[0-9]{5}$'",
        array($assign->course)
    );
    $gradeitem = $DB->get_record('grade_items', array('itemtype' => 'mod', 'itemmodule' => 'assign', 'iteminstance' => $assign->id));
    foreach ($students as $stu) {
        $rand = mt_rand(0, 100);
        if ($rand < 60) {
            $gradeVal = mt_rand(30, 100);
            $tc = $now - mt_rand(1, 30) * 24 * 3600;
            $tm = $now - mt_rand(0, 7) * 24 * 3600;
            try {
                $DB->execute(
                    "INSERT INTO mdl_assign_submission (assignment, userid, timecreated, timemodified, timestarted, status, groupid, attemptnumber, latest) " .
                    "VALUES (?, ?, ?, ?, ?, 'submitted', 0, 0, 1)",
                    array($assign->id, $stu->id, $tc, $tm, $tc)
                );
                $totalSubs++;
            } catch (Exception $e) {
                echo "Skip sub: " . $e->getMessage() . "\n";
            }
            if ($gradeitem) {
                try {
                    $DB->execute(
                        "INSERT INTO mdl_grade_grades (itemid, userid, rawgrade, rawgrademax, rawgrademin, finalgrade, hidden, locked, locktime, exported, overridden, excluded, feedbackformat, informationformat, timecreated, timemodified, aggregationstatus) " .
                        "VALUES (?, ?, ?, 100, 0, ?, 0, 0, 0, 0, 0, 0, 0, 0, ?, ?, 'unknown')",
                        array($gradeitem->id, $stu->id, $gradeVal, $gradeVal, $now, $now)
                    );
                } catch (Exception $e) {
                    // ignore
                }
            }
        } else {
            $totalMissing++;
        }
    }
}
echo "✅ Đã tạo $totalSubs bài nộp, $totalMissing bài chưa nộp\n";
