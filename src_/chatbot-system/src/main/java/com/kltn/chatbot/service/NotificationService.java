package com.kltn.chatbot.service;

import com.kltn.chatbot.event.RiskDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service gửi thông báo Moodle cho giáo viên phụ trách môn, cố vấn lớp và sinh viên.
 * Ghi vào mdl_notifications + mdl_message_popup_notifications để hiện ở chuông thông báo Moodle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JdbcTemplate jdbc;

    @Value("${moodle.message-sender-id:2}")
    private long systemSenderId;

    @Value("${moodle.api.base-url:http://localhost/moodle}")
    private String moodleBaseUrl;

    public int notifyViolation(long studentId, long courseId, String riskLevel, String reason) {
        Optional<Map<String, Object>> studentOpt = findUser(studentId);
        if (studentOpt.isEmpty()) return 0;

        Map<String, Object> student = studentOpt.get();
        String studentName = (String) student.get("fullname");
        String username = (String) student.get("username");

        Optional<Map<String, Object>> courseOpt = findCourse(courseId);
        String courseName = courseOpt.isPresent() ? (String) courseOpt.get().get("fullname") : "Môn học";

        String icon = "🟡";
        if ("red".equalsIgnoreCase(riskLevel)) icon = "🔴";
        else if ("green".equalsIgnoreCase(riskLevel)) icon = "🟢";

        String subject = String.format("[%s] Cảnh báo học vụ - %s", icon, courseName);
        String shortMsg = String.format("%s vi phạm tại %s: %s", studentName, courseName, reason);
        String fullMsg = String.format(
                "⚠️ CẢNH BÁO HỌC VỤ\n\n" +
                "Sinh viên: %s (%s)\n" +
                "Môn học: %s\n" +
                "Mức rủi ro: %s %s\n" +
                "Lý do: %s\n\n" +
                "Vui lòng kiểm tra và liên hệ với sinh viên để hỗ trợ kịp thời.\n" +
                "Thời gian phát hiện: %s",
                studentName, username, courseName, icon, riskLevel.toUpperCase(),
                reason, Instant.now()
        );

        Set<Long> recipients = new LinkedHashSet<>();
        recipients.addAll(findCourseTeachers(courseId));
        recipients.addAll(findStudentAdvisers(studentId));
        recipients.add(2L);
        recipients.add(studentId);

        int sent = 0;
        for (Long toUserId : recipients) {
            if (toUserId != null && sendMessage(toUserId, subject, shortMsg, fullMsg)) {
                sent++;
            }
        }

        log.info("Sent {} notifications for student {} in course {}", sent, studentId, courseId);
        return sent;
    }

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
            default -> "🟡";
        };

        String subject = String.format("[%s] Cảnh báo học vụ - %s", icon, event.getCourseName());
        String shortMsg = String.format("%s - %s - %s %s",
                event.getStudentName(), event.getCourseName(), icon, event.getRiskLevel().name());

        StringBuilder detail = new StringBuilder();
        detail.append("⚠️ CẢNH BÁO HỌC VỤ\n\n");
        detail.append(String.format("Sinh viên: %s (%s)\n", event.getStudentName(),
                event.getUsername() != null ? event.getUsername() : "N/A"));
        detail.append(String.format("Môn học: %s\n", event.getCourseName()));
        detail.append(String.format("Mức rủi ro: %s %s\n\n", icon, event.getRiskLevel().name()));
        detail.append("Chỉ số chi tiết:\n");
        if (event.getGradeAverage() != null) detail.append(String.format("   • Điểm trung bình: %.2f\n", event.getGradeAverage()));
        if (event.getAttendanceRate() != null) detail.append(String.format("   • Tỷ lệ chuyên cần: %.1f%%\n", event.getAttendanceRate()));
        if (event.getCompletionRate() != null) detail.append(String.format("   • Tỷ lệ hoàn thành: %.1f%%\n", event.getCompletionRate()));
        if (event.getLastAccessDays() != null) detail.append(String.format("   • Số ngày từ lần truy cập cuối: %d\n", event.getLastAccessDays()));
        if (event.getReasons() != null && !event.getReasons().isBlank()) {
            detail.append("\nPhân tích:\n").append(event.getReasons()).append("\n");
        }
        detail.append("\nVui lòng kiểm tra và liên hệ sinh viên để hỗ trợ kịp thời.\n");
        detail.append(String.format("Phát hiện lúc: %s", event.getDetectedAt()));
        String fullMsg = detail.toString();

        Set<Long> recipients = new LinkedHashSet<>();
        recipients.addAll(findCourseTeachers(event.getCourseId()));
        recipients.addAll(findStudentAdvisers(event.getStudentId()));
        recipients.add(2L);
        recipients.add(event.getStudentId());

        int sent = 0;
        for (Long toUserId : recipients) {
            if (toUserId != null && sendMessage(toUserId, subject, shortMsg, fullMsg)) sent++;
        }

        log.info("Auto-sent {} notifications for warning id={} (student={}, course={})",
                sent, event.getWarningId(), event.getStudentId(), event.getCourseId());
        return sent;
    }

    private boolean hasRecentNotification(long studentId, long courseId, int windowSeconds) {
        String sql = "SELECT COUNT(*) FROM mdl_notifications WHERE useridto = ? AND subject LIKE ? AND timecreated > UNIX_TIMESTAMP() - ?";
        try {
            Integer c = jdbc.queryForObject(sql, Integer.class, 2L, "%" + studentId + "%" + courseId + "%", windowSeconds);
            return c != null && c > 0;
        } catch (Exception e) {
            log.warn("hasRecentNotification check failed: {}", e.getMessage());
            return false;
        }
    }

    public int sendBatchNotifications() {
        return 0;
    }

    private boolean sendMessage(long toUserId, String subject, String shortMsg, String fullMsg) {
        String notificationsUrl = moodleBaseUrl.replaceAll("/$", "") + "/message/output/popup/notifications.php";
        String insertNotification = "INSERT INTO mdl_notifications " +
                "(useridfrom, useridto, subject, fullmessage, fullmessageformat, fullmessagehtml, " +
                " smallmessage, component, eventtype, contexturl, contexturlname, timecreated) " +
                "VALUES (?, ?, ?, ?, 1, ?, ?, 'local_chatbot', 'risk_notification', ?, ?, UNIX_TIMESTAMP())";
        String insertPopup = "INSERT INTO mdl_message_popup_notifications (notificationid) VALUES (?)";
        try {
            jdbc.update(insertNotification, systemSenderId, toUserId, subject, fullMsg, fullMsg, shortMsg,
                    notificationsUrl, "Xem cảnh báo");
            Long notificationId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            if (notificationId != null && notificationId > 0) {
                jdbc.update(insertPopup, notificationId);
            }
            return true;
        } catch (Exception e) {
            log.warn("Send notification failed for user {}: {}", toUserId, e.getMessage());
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
                            "SELECT DISTINCT ra.userid " +
                                    "FROM mdl_role_assignments ra " +
                                    "JOIN mdl_context ctx ON ctx.id = ra.contextid " +
                                    "JOIN mdl_role r ON r.id = ra.roleid " +
                                    "WHERE ctx.contextlevel = 50 AND ctx.instanceid = ? " +
                                    "AND LOWER(r.shortname) IN ('editingteacher', 'teacher', 'manager')",
                            Long.class,
                            courseId)
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("findCourseTeachers failed for course {}: {}", courseId, e.getMessage());
            return List.of();
        }
    }

    private List<Long> findStudentAdvisers(long studentId) {
        try {
            return jdbc.queryForList(
                            "SELECT DISTINCT ra.userid " +
                                    "FROM mdl_role_assignments ra " +
                                    "JOIN mdl_context ctx ON ctx.id = ra.contextid " +
                                    "JOIN mdl_role r ON r.id = ra.roleid " +
                                    "JOIN mdl_user u ON u.id = ra.userid " +
                                    "WHERE ctx.contextlevel = 40 AND ctx.instanceid = ? " +
                                    "AND LOWER(r.shortname) IN ('studentadvisor', 'advisor', 'mentor', 'tutor')",
                            Long.class,
                            studentId)
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("findStudentAdvisers failed for student {}: {}", studentId, e.getMessage());
            return List.of();
        }
    }
}