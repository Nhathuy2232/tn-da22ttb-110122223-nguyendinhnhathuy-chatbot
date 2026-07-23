<?php
/**
 * Re-seed submissions + grades:
 * - 100% sinh viên enrolled trong môn sẽ có submission record
 * - 65% đã nộp bài (grade 30-100)
 * - 35% chưa nộp (grade = 0, status = 'draft' hoặc 'new')
 */
$now = time();
mt_srand(42);

$mysqli = new mysqli('localhost', 'root', '', 'moodle');
if ($mysqli->connect_error) die("Connection failed: " . $mysqli->connect_error);
$mysqli->set_charset('utf8mb4');

$totalSubs = 0;
$totalMissing = 0;
$totalGrades = 0;

$result = $mysqli->query("SELECT id, course FROM mdl_assign");
$allAssigns = $result->fetch_all(MYSQLI_ASSOC);

foreach ($allAssigns as $assign) {
    $aid = $assign['id'];
    $cid = $assign['course'];

    // Lấy students enrolled trong course
    $stmt = $mysqli->prepare(
        "SELECT u.id FROM mdl_user u " .
        "JOIN mdl_user_enrolments ue ON ue.userid = u.id " .
        "JOIN mdl_enrol e ON e.id = ue.enrolid " .
        "WHERE e.courseid = ? AND u.username REGEXP '^1101[0-9]{5}$'"
    );
    $stmt->bind_param('i', $cid);
    $stmt->execute();
    $students = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    // Lấy grade item
    $gstmt = $mysqli->prepare("SELECT id FROM mdl_grade_items WHERE itemtype='mod' AND itemmodule='assign' AND iteminstance=?");
    $gstmt->bind_param('i', $aid);
    $gstmt->execute();
    $gres = $gstmt->get_result()->fetch_assoc();
    $gstmt->close();
    $giid = $gres ? $gres['id'] : null;

    foreach ($students as $stu) {
        $userid = $stu['id'];
        $rand = mt_rand(0, 100);

        if ($rand < 65) {
            // 65% đã nộp bài - grade 30-100
            $gradeVal = mt_rand(30, 100);
            $status = 'submitted';
            $tc = $now - mt_rand(1, 30) * 24 * 3600;
            $tm = $now - mt_rand(0, 7) * 24 * 3600;
            $totalSubs++;
        } else {
            // 35% chưa nộp - grade = 0, status = 'new'
            $gradeVal = 0;
            $status = 'new';
            $tc = 0;
            $tm = 0;
            $totalMissing++;
        }

        // Tạo submission record (kể cả chưa nộp)
        $check = $mysqli->prepare("SELECT id FROM mdl_assign_submission WHERE assignment=? AND userid=? AND groupid=0 AND attemptnumber=0");
        $check->bind_param('ii', $aid, $userid);
        $check->execute();
        $check->store_result();
        if ($check->num_rows == 0) {
            $sstmt = $mysqli->prepare(
                "INSERT INTO mdl_assign_submission (assignment, userid, timecreated, timemodified, timestarted, status, groupid, attemptnumber, latest) " .
                "VALUES (?, ?, ?, ?, ?, ?, 0, 0, 1)"
            );
            $sstmt->bind_param('iiisis', $aid, $userid, $tc, $tm, $tc, $status);
            $sstmt->execute();
            $sstmt->close();
        }
        $check->close();

        // Tạo grade record (kể cả chưa nộp = 0)
        if ($giid) {
            $gcheck = $mysqli->prepare("SELECT id FROM mdl_grade_grades WHERE itemid=? AND userid=?");
            $gcheck->bind_param('ii', $giid, $userid);
            $gcheck->execute();
            $gcheckres = $gcheck->get_result();
            $hasGrade = ($gcheckres->num_rows > 0);
            $gcheck->close();

            if (!$hasGrade) {
                $gstmt2 = $mysqli->prepare(
                    "INSERT INTO mdl_grade_grades (itemid, userid, rawgrade, rawgrademax, rawgrademin, finalgrade, hidden, locked, locktime, exported, overridden, excluded, feedbackformat, informationformat, timecreated, timemodified, aggregationstatus) " .
                    "VALUES (?, ?, ?, 100, 0, ?, 0, 0, 0, 0, 0, 0, 0, 0, ?, ?, 'unknown')"
                );
                $gstmt2->bind_param('iidiii', $giid, $userid, $gradeVal, $gradeVal, $now, $now);
                $gstmt2->execute();
                $gstmt2->close();
                $totalGrades++;
            }
        }
    }
}
echo "✅ Đã tạo:\n";
echo "   - $totalSubs bài nộp (status=submitted)\n";
echo "   - $totalMissing bài chưa nộp (status=new, grade=0)\n";
echo "   - $totalGrades grade records\n";
$mysqli->close();
