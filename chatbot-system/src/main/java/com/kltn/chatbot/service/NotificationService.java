package com.kltn.chatbot.service;

import com.kltn.chatbot.event.RiskDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service gửi thông báo Moodle cho giáo viên phụ trách môn và cố vấn lớp.
 * Mỗi cảnh báo sẽ tạo message trong bảng mdl_message.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JdbcTemplate jdbc;

    /**
     * Gửi thông báo về 1 sinh viên vi phạm tới:
     * - Giáo viên phụ trách môn
     * - Cố vấn lớp của sinh viên
     * - Bản thân sinh viên
     */
    public int notifyViolation(long studentId, long courseId, String riskLevel, String reason) {
        Optional<Map<String, Object>> studentOpt = findUser(studentId);
        if (studentOpt.isEmpty()) return 0;

        Map<String, Object> student = studentOpt.get();
        String studentName = (String) student.get("fullname");
        String username = (String) student.get("username");

        // Lấy tên môn
        Optional<Map<String, Object>> courseOpt = findCourse(courseId);
        String courseName = courseOpt.isPresent() ? (String) courseOpt.get().get("fullname") : "Môn học";

        String icon = "🟡";
        if ("red".equals(riskLevel)) icon = "🔴";
        else if ("green".equals(riskLevel)) icon = "🟢";

        String subject = String.format("[%s] Cảnh báo học vụ - %s", icon, courseName);
        String shortMsg = String.format("%s vi phạm tại %s: %s", studentName, courseName, reason);
        String fullMsg = String.format(
                "⚠️ CẢNH BÁO HỌC VỤ\n\n" +
                "👤 Sinh viên: %s (%s)\n" +
                "📚 Môn học: %s\n" +
                "🎯 Mức rủi ro: %s\n" +
                "📋 Lý do: %s\n\n" +
                "Vui lòng kiểm tra và liên hệ với sinh viên để hỗ trợ kịp thời.\n" +
                "Thời gian phát hiện: %s",
                studentName, username, courseName, icon + " " + riskLevel.toUpperCase(),
                reason, Instant.now()
        );

        int sent = 0;

        // 1. Gửi cho giáo viên phụ trách môn (editingteacher trong course)
        List<Long> teacherIds = findCourseTeachers(courseId);
        for (Long tid : teacherIds) {
            if (sendMessage(tid, subject, shortMsg, fullMsg)) sent++;
        }

        // 2. Gửi cho cố vấn lớp của sinh viên
        List<Long> adviserIds = findStudentAdvisers(studentId);
        for (Long aid : adviserIds) {
            if (sendMessage(aid, subject, shortMsg, fullMsg)) sent++;
        }

        // 3. Gửi cho admin (id=2)
        if (sendMessage(2L, subject, shortMsg, fullMsg)) sent++;

        log.info("Sent {} notifications for student {} in course {}", sent, studentId, courseId);
        return sent;
    }

    /**
     * Gửi thông báo tự động khi nhận RiskDetectedEvent (realtime).
     * Sử dụng thông tin chi tiết từ event (grade, attendance, reasons) thay vì chỉ reason đơn lẻ.
     * Chống spam: bỏ qua nếu đã gửi cho cùng (student, course) trong 1 giờ qua.
     */
    public int notifyFromEvent(RiskDetectedEvent event) {
        if (event == null || event.getStudentId() == null || event.getCourseId() == null) {
            log.warn("Skipping invalid RiskDetectedEvent: missing studentId or courseId");
            return 0;
        }

        if (hasRecentNotification(event.getStudentId(), event.getCourseId(), 3600)) {
            log.info("Skipping notification for student {} course {} - already sent within last hour",
                    event.getStudentId(), event.getCourseId());
            return 0;
        }

        String icon = switch (event.getRiskLevel().name()) {
            case "RED" -> "🔴";
            case "YELLOW" -> "🟡";
            case "GREEN" -> "🟢";
            default -> "⚪";
        };

        String subject = String.format("[%s AUTO] Cảnh báo học vụ - %s", icon, event.getCourseName());
        String shortMsg = String.format("%s - %s - rủi ro %s",
                event.getStudentName(), event.getCourseName(), event.getRiskLevel().name());

        StringBuilder detail = new StringBuilder();
        detail.append("⚠️ CẢNH BÁO HỌC VỤ TỰ ĐỘNG\n\n");
        detail.append(String.format("👤 Sinh viên: %s (%s)\n", event.getStudentName(),
                event.getUsername() != null ? event.getUsername() : "N/A"));
        detail.append(String.format("📚 Môn học: %s\n", event.getCourseName()));
        detail.append(String.format("🎯 Mức rủi ro: %s %s\n\n", icon, event.getRiskLevel().name()));
        detail.append("📊 Chỉ số chi tiết:\n");
        if (event.getGradeAverage() != null) {
            detail.append(String.format("   • Điểm trung bình: %.2f\n", event.getGradeAverage()));
        }
        if (event.getAttendanceRate() != null) {
            detail.append(String.format("   • Tỷ lệ chuyên cần: %.1f%%\n", event.getAttendanceRate()));
        }
        if (event.getCompletionRate() != null) {
            detail.append(String.format("   • Tỷ lệ hoàn thành: %.1f%%\n", event.getCompletionRate()));
        }
        if (event.getLastAccessDays() != null) {
            detail.append(String.format("   • Số ngày từ lần truy cập cuối: %d\n", event.getLastAccessDays()));
        }
        if (event.getReasons() != null && !event.getReasons().isBlank()) {
            detail.append("\n📋 Phân tích:\n").append(event.getReasons()).append("\n");
        }
        detail.append("\nVui lòng kiểm tra và liên hệ sinh viên để hỗ trợ kịp thời.\n");
        detail.append(String.format("Phát hiện lúc: %s", event.getDetectedAt()));
        String fullMsg = detail.toString();

        int sent = 0;

        // 1. Gửi cho giáo viên phụ trách môn
        List<Long> teacherIds = findCourseTeachers(event.getCourseId());
        for (Long tid : teacherIds) {
            if (sendMessage(tid, subject, shortMsg, fullMsg)) sent++;
        }

        // 2. Gửi cho cố vấn lớp
        List<Long> adviserIds = findStudentAdvisers(event.getStudentId());
        for (Long aid : adviserIds) {
            if (sendMessage(aid, subject, shortMsg, fullMsg)) sent++;
        }

        // 3. Gửi cho admin
        if (sendMessage(2L, subject, shortMsg, fullMsg)) sent++;

        // 4. Gửi cho bản thân sinh viên
        if (sendMessage(event.getStudentId(), subject, shortMsg, fullMsg)) sent++;

        log.info("Auto-sent {} notifications for RED warning id={} (student={}, course={})",
                sent, event.getWarningId(), event.getStudentId(), event.getCourseId());
        return sent;
    }

    /**
     * Check if a notification was sent recently (within windowSeconds seconds) for a (student, course) pair.
     */
    private boolean hasRecentNotification(long studentId, long courseId, int windowSeconds) {
        String sql = "SELECT COUNT(*) AS c FROM mdl_message " +
                "WHERE smallmessage LIKE ? " +
                "AND timecreated > UNIX_TIMESTAMP() - ?";
        try {
            Integer c = jdbc.queryForObject(sql, Integer.class,
                    "%" + studentId + "%" + courseId + "%", windowSeconds);
            return c != null && c > 0;
        } catch (Exception e) {
            log.warn("hasRecentNotification check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gửi thông báo hàng loạt (batch) - dùng cho scheduled task.
     */
    public int sendBatchNotifications() {
        log.info("Running batch notification scan...");

        // Tìm tất cả SV có rủi ro đỏ ở mỗi môn
        String sql = "SELECT u.id AS userid, e.courseid AS courseid, " +
                "       AVG(CASE WHEN gg.finalgrade IS NOT NULL THEN gg.finalgrade ELSE NULL END) AS avgGrade, " +
                "       (UNIX_TIMESTAMP() - u.lastaccess) / 86400 AS daysAccess " +
                "FROM mdl_user u " +
                "JOIN mdl_user_enrolments ue ON ue.userid = u.id " +
                "JOIN mdl_enrol e ON e.id = ue.enrolid " +
                "LEFT JOIN mdl_grade_grades gg ON gg.userid = u.id " +
                "LEFT JOIN mdl_grade_items gi ON gi.id = gg.itemid AND gi.courseid = e.courseid " +
                "WHERE u.username REGEXP '^1101[0-9]{5}$' AND e.courseid > 1 " +
                "GROUP BY u.id, e.courseid";

        List<Map<String, Object>> students = jdbc.queryForList(sql);
        int count = 0;

        for (Map<String, Object> s : students) {
            long userid = ((Number) s.get("userid")).longValue();
            long courseid = ((Number) s.get("courseid")).longValue();
            Object avgObj = s.get("avgGrade");
            Object daysObj = s.get("daysAccess");

            double avg = avgObj != null ? ((Number) avgObj).doubleValue() : 100;
            long days = daysObj != null ? ((Number) daysObj).longValue() : 0;

            String level = "green";
            String reason = "";
            if ((avg > 0 && avg < 50) || days > 14) {
                level = "red";
                if (days > 14) reason = "Không online quá 14 ngày";
                else reason = "Điểm TB dưới 50%";
            } else if ((avg > 0 && avg < 80) || days > 7) {
                level = "yellow";
                if (days > 7) reason = "Ít tương tác (7-14 ngày)";
                else reason = "Điểm TB dưới 80%";
            }

            if ("red".equals(level) || "yellow".equals(level)) {
                // Kiểm tra chưa gửi thông báo trong 24h qua
                if (!hasRecentNotification(userid, courseid)) {
                    notifyViolation(userid, courseid, level, reason);
                    count++;
                }
            }
        }
        log.info("Batch scan complete: {} new notifications sent", count);
        return count;
    }

    private boolean hasRecentNotification(long userid, long courseid) {
        String sql = "SELECT COUNT(*) AS c FROM mdl_message " +
                "WHERE useridto IN (2) " +
                "AND smallmessage LIKE ? " +
                "AND timecreated > UNIX_TIMESTAMP() - 86400";
        try {
            Integer c = jdbc.queryForObject(sql, Integer.class, "%course " + courseid + " student " + userid + "%");
            return c != null && c > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean sendMessage(long toUserId, String subject, String shortMsg, String fullMsg) {
        String sql = "INSERT INTO mdl_message " +
                "(useridfrom, useridto, subject, fullmessage, fullmessageformat, fullmessagehtml, " +
                " smallmessage, notification, contexturl, contexturlname, timecreated, " +
                " timeuserfromdeleted, timeusertodeleted, component, eventtype) " +
                "VALUES (2, ?, ?, ?, 1, ?, ?, 1, ?, ?, UNIX_TIMESTAMP(), 0, 0, 'mod_assign', 'assign_notification')";
        try {
            jdbc.update(sql, toUserId, subject, fullMsg, fullMsg, shortMsg,
                    "/moodle/user/profile.php?id=" + toUserId, "Xem chi tiết");
            return true;
        } catch (Exception e) {
            log.warn("Send message failed: {}", e.getMessage());
            return false;
        }
    }

    private Optional<Map<String, Object>> findUser(long userId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT id, username, CONCAT(firstname,' ',lastname) AS fullname FROM mdl_user WHERE id = ?",
                    userId);
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<Map<String, Object>> findCourse(long courseId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT id, fullname, shortname FROM mdl_course WHERE id = ?", courseId);
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private List<Long> findCourseTeachers(long courseId) {
        try {
            return jdbc.queryForList(
                    "SELECT DISTINCT ra.userid FROM mdl_role_assignments ra " +
                            "JOIN mdl_context ctx ON ctx.id = ra.contextid " +
                            "WHERE ctx.contextlevel = 50 AND ctx.instanceid = ? " +
                            "AND ra.roleid IN (3, 4)",
                    Long.class, courseId);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Long> findStudentAdvisers(long studentId) {
        try {
            return jdbc.queryForList(
                    "SELECT DISTINCT ra.userid FROM mdl_cohort_members cm " +
                            "JOIN mdl_cohort_members cm2 ON cm2.cohortid = cm.cohortid " +
                            "JOIN mdl_role_assignments ra ON ra.userid = cm2.userid " +
                            "WHERE cm.userid = ? AND ra.roleid = 11",
                    Long.class, studentId);
        } catch (Exception e) {
            return List.of();
        }
    }
}
