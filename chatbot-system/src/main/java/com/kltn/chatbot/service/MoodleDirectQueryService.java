package com.kltn.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Service truy vấn trực tiếp Moodle database (MySQL) thay vì qua Web Services API.
 * Nhanh hơn và không cần token. Cấu trúc dựa trên schema Moodle chuẩn.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoodleDirectQueryService {

    private final JdbcTemplate jdbc;

    // ============ COURSE / ENROLLMENT ============

    /**
     * Lấy danh sách khóa học theo tên (LIKE search).
     */
    public List<Map<String, Object>> findCoursesByName(String courseName) {
        if (courseName == null || courseName.isBlank()) return List.of();
        String kw = courseName.toLowerCase().trim();
        String sql = "SELECT id, shortname, fullname, category, visible " +
                "FROM mdl_course " +
                "WHERE id > 1 AND (LOWER(fullname) LIKE ? OR LOWER(shortname) LIKE ?) " +
                "ORDER BY id";
        try {
            return jdbc.queryForList(sql, "%" + kw + "%", "%" + kw + "%");
        } catch (Exception e) {
            log.error("findCoursesByName failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy course id theo tên rút gọn (java, web, csdl, ai, mmt, vtp).
     */
    public Optional<Map<String, Object>> findCourseByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return Optional.empty();
        String k = keyword.toLowerCase().trim();
        // Map keyword -> search term (dùng HashMap vì có > 10 entries)
        Map<String, String> map = new HashMap<>();
        map.put("java", "java");
        map.put("web", "web");
        map.put("cơ sở dữ liệu", "cơ sở dữ liệu");
        map.put("co so du lieu", "cơ sở dữ liệu");
        map.put("csdl", "cơ sở dữ liệu");
        map.put("trí tuệ nhân tạo", "trí tuệ nhân tạo");
        map.put("tri tue nhan tao", "trí tuệ nhân tạo");
        map.put("ai", "trí tuệ nhân tạo");
        map.put("mạng máy tính", "mạng máy tính");
        map.put("mang may tinh", "mạng máy tính");
        map.put("mmt", "mạng máy tính");
        map.put("vi tích phân", "vi tích phân");
        map.put("vi tich phan", "vi tích phân");
        map.put("vtp", "vi tích phân");
        String search = map.getOrDefault(k, k);
        List<Map<String, Object>> courses = findCoursesByName(search);
        if (courses.isEmpty()) return Optional.empty();
        return Optional.of(courses.get(0));
    }

    /**
     * Lấy tất cả sinh viên đã ghi danh vào 1 khóa học.
     */
    public List<Map<String, Object>> findEnrolledStudents(long courseId) {
        String sql = "SELECT u.id, u.username, u.firstname, u.lastname, " +
                "       CONCAT(u.firstname,' ',u.lastname) AS fullname, " +
                "       u.email, u.lastaccess, ue.timecreated, ue.timestart, ue.timeend " +
                "FROM mdl_user u " +
                "JOIN mdl_user_enrolments ue ON ue.userid = u.id " +
                "JOIN mdl_enrol e ON e.id = ue.enrolid " +
                "WHERE e.courseid = ? AND u.username REGEXP '^1101[0-9]{5}$' " +
                "ORDER BY u.username";
        try {
            return jdbc.queryForList(sql, courseId);
        } catch (Exception e) {
            log.error("findEnrolledStudents failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy tất cả khóa học mà 1 user đã ghi danh.
     */
    public List<Map<String, Object>> findEnrolledCourses(long userId) {
        String sql = "SELECT DISTINCT c.id, c.shortname, c.fullname " +
                "FROM mdl_course c " +
                "JOIN mdl_enrol e ON e.courseid = c.id " +
                "JOIN mdl_user_enrolments ue ON ue.enrolid = e.id " +
                "WHERE ue.userid = ? AND c.id > 1 " +
                "ORDER BY c.fullname";
        try {
            return jdbc.queryForList(sql, userId);
        } catch (Exception e) {
            log.error("findEnrolledCourses failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ============ GRADES ============

    /**
     * Tính điểm trung bình (0-100) của 1 user trong 1 course.
     * Trả về hasGrades=false nếu tất cả grade = 0 (chưa nộp).
     */
    public Map<String, Object> getCourseAverageGrade(long courseId, long userId) {
        String sql = "SELECT gg.finalgrade, gg.rawgrade, gg.rawgrademax " +
                "FROM mdl_grade_grades gg " +
                "JOIN mdl_grade_items gi ON gi.id = gg.itemid " +
                "WHERE gi.courseid = ? AND gi.itemtype = 'mod' " +
                "AND gg.userid = ?";
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql, courseId, userId);
            if (rows.isEmpty()) {
                return Map.of("avgGrade", 0.0, "hasGrades", false, "itemsCount", 0);
            }
            double total = 0;
            int count = 0;
            int nonZeroCount = 0;
            for (Map<String, Object> row : rows) {
                Object finalGrade = row.get("finalgrade");
                if (finalGrade != null) {
                    double v = ((Number) finalGrade).doubleValue();
                    total += v;
                    count++;
                    if (v > 0) nonZeroCount++;
                }
            }
            double avg = count > 0 ? total / count : 0.0;
            // hasGrades = true nếu có grade (kể cả grade = 0 - vì có record rồi)
            return Map.of("avgGrade", avg, "hasGrades", count > 0, "itemsCount", count,
                    "nonZeroCount", nonZeroCount);
        } catch (Exception e) {
            log.error("getCourseAverageGrade failed: {}", e.getMessage());
            return Map.of("avgGrade", 0.0, "hasGrades", false, "itemsCount", 0);
        }
    }

    /**
     * Lấy điểm từng bài tập (assignment) trong 1 course của 1 user.
     * Trả về danh sách: assignmentId, assignmentName, grade, maxGrade, status, submittedAt.
     * Điểm hiển thị dạng số thực (không phần trăm).
     */
    public List<Map<String, Object>> findAssignmentGrades(long courseId, long userId) {
        String sql = "SELECT a.id AS assignid, a.name AS assignname, " +
                "       a.duedate AS duedate, " +
                "       gg.finalgrade AS grade, gg.rawgrademax AS maxgrade, " +
                "       s.status AS submitstatus, s.timemodified AS submittedat " +
                "FROM mdl_assign a " +
                "LEFT JOIN mdl_grade_items gi ON gi.iteminstance = a.id AND gi.itemtype='mod' AND gi.itemmodule='assign' " +
                "LEFT JOIN mdl_grade_grades gg ON gg.itemid = gi.id AND gg.userid = ? " +
                "LEFT JOIN mdl_assign_submission s ON s.assignment = a.id AND s.userid = ? AND s.latest = 1 " +
                "WHERE a.course = ? " +
                "ORDER BY a.id";
        try {
            return jdbc.queryForList(sql, userId, userId, courseId);
        } catch (Exception e) {
            log.error("findAssignmentGrades failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ============ LAST ACCESS ============

    /**
     * Tính số ngày từ lần cuối truy cập tới nay.
     */
    public long getDaysSinceAccess(long userId) {
        String sql = "SELECT lastaccess FROM mdl_user WHERE id = ?";
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql, userId);
            if (rows.isEmpty()) return 999;
            Object last = rows.get(0).get("lastaccess");
            if (last == null) return 999;
            long ts = ((Number) last).longValue();
            if (ts == 0) return 999;
            Instant instant = Instant.ofEpochSecond(ts);
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return java.time.Duration.between(dateTime, LocalDateTime.now()).toDays();
        } catch (Exception e) {
            log.error("getDaysSinceAccess failed: {}", e.getMessage());
            return 999;
        }
    }

    // ============ USER INFO ============

    public Optional<Map<String, Object>> findUserByUsername(String username) {
        String sql = "SELECT id, username, firstname, lastname, " +
                "       CONCAT(firstname,' ',lastname) AS fullname, " +
                "       email, lastaccess, city, country " +
                "FROM mdl_user WHERE username = ?";
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql, username);
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (Exception e) {
            log.error("findUserByUsername failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> findUserById(long userId) {
        String sql = "SELECT id, username, firstname, lastname, " +
                "       CONCAT(firstname,' ',lastname) AS fullname, " +
                "       email, lastaccess, city, country " +
                "FROM mdl_user WHERE id = ?";
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql, userId);
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (Exception e) {
            log.error("findUserById failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ============ COHORT (LỚP) ============

    /**
     * Lấy danh sách sinh viên trong 1 lớp (cohort).
     */
    public List<Map<String, Object>> findStudentsByCohortName(String classCode) {
        if (classCode == null || classCode.isBlank()) return List.of();
        String sql = "SELECT u.id, u.username, u.firstname, u.lastname, " +
                "       CONCAT(u.firstname,' ',u.lastname) AS fullname, " +
                "       u.email, u.lastaccess " +
                "FROM mdl_cohort c " +
                "JOIN mdl_cohort_members cm ON cm.cohortid = c.id " +
                "JOIN mdl_user u ON u.id = cm.userid " +
                "WHERE c.name LIKE ? OR c.idnumber LIKE ? " +
                "ORDER BY u.username";
        try {
            return jdbc.queryForList(sql, "%" + classCode + "%", "%" + classCode + "%");
        } catch (Exception e) {
            log.error("findStudentsByCohortName failed: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> findAllCohorts() {
        String sql = "SELECT id, name, idnumber, description FROM mdl_cohort ORDER BY name";
        try {
            return jdbc.queryForList(sql);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Lấy tất cả sinh viên (username khớp pattern 1101xxxxx).
     */
    public List<Map<String, Object>> findAllStudents() {
        String sql = "SELECT id, username, firstname, lastname, " +
                "       CONCAT(firstname,' ',lastname) AS fullname, " +
                "       email, lastaccess " +
                "FROM mdl_user " +
                "WHERE username REGEXP '^1101[0-9]{5}$' " +
                "ORDER BY username";
        try {
            return jdbc.queryForList(sql);
        } catch (Exception e) {
            log.error("findAllStudents failed: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Lấy tất cả sinh viên ĐANG HOẠT ĐỘNG (có enrolment còn hiệu lực).
     * Dùng cho automatic risk monitoring.
     */
    public List<Map<String, Object>> findAllActiveStudents() {
        String sql = "SELECT DISTINCT u.id, u.username, u.firstname, u.lastname, " +
                "       CONCAT(u.firstname,' ',u.lastname) AS fullname, " +
                "       u.email, u.lastaccess " +
                "FROM mdl_user u " +
                "JOIN mdl_user_enrolments ue ON ue.userid = u.id " +
                "JOIN mdl_enrol e ON e.id = ue.enrolid " +
                "WHERE u.username REGEXP '^1101[0-9]{5}$' " +
                "  AND u.deleted = 0 " +
                "  AND u.suspended = 0 " +
                "  AND ue.status = 0 " +
                "ORDER BY u.username";
        try {
            return jdbc.queryForList(sql);
        } catch (Exception e) {
            log.error("findAllActiveStudents failed: {}", e.getMessage());
            return List.of();
        }
    }
}
