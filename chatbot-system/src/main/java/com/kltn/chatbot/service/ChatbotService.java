package com.kltn.chatbot.service;

import com.kltn.chatbot.controller.MoodleRestController;
import com.kltn.chatbot.model.dto.ChatRequestDTO;
import com.kltn.chatbot.model.dto.ChatResponseDTO;
import com.kltn.chatbot.model.entity.ChatHistory;
import com.kltn.chatbot.model.enums.ChatRole;
import com.kltn.chatbot.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Chatbot Service - Phiên bản 6.0
 *
 * Tính năng mới:
 * - Lọc sinh viên vi phạm theo tiêu chí (chỉ trả về SV vi phạm)
 * - Hỗ trợ đầy đủ intent cho 4 quyền
 * - Format response thân thiện, dễ đọc
 * - Tích hợp tốt với LocalIntentMatcher đã train
 *
 * @author Nguyễn Đình Nhật Huy - MSSV: 110122223
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private static final Set<String> CHITCHAT_INTENTS = Set.of(
            "GREET", "GOODBYE", "THANK", "HELP", "UNKNOWN"
    );

    private final OpenAIService openaiService;
    private final MoodleRestController moodleRestController;
    private final ChatHistoryRepository chatHistoryRepository;
    private final MoodleDirectQueryService directQuery;
    private final NotificationService notificationService;
    private final ResponseGenerator responseGenerator;

    @Transactional
    public ChatResponseDTO handleUserMessage(ChatRequestDTO request) {
        String username = resolveUsername(request);
        String role = resolveRole(request);

        log.info("========== CHATBOT REQUEST ==========");
        log.info("Username: {} | Role: {} | Message: {}", username, role, request.getMessage());

        try {
            Map<String, Object> analysis = LocalIntentMatcher.matchForRole(request.getMessage(), role, username)
                    .orElseGet(() -> openaiService.analyzeMessage(request.getMessage(), role, username));
            if (analysis == null) {
                analysis = new HashMap<>();
                analysis.put("intent", "UNKNOWN");
                analysis.put("confidence", 0.0);
                analysis.put("entities", new HashMap<String, String>());
            }

            String intent = String.valueOf(analysis.getOrDefault("intent", "UNKNOWN"));
            double confidence = analysis.get("confidence") instanceof Number n ? n.doubleValue() : 0.0;
            @SuppressWarnings("unchecked")
            Map<String, String> entities = analysis.get("entities") instanceof Map<?, ?> m
                    ? (Map<String, String>) m
                    : new HashMap<>();

            log.info("Intent: {} | Confidence: {} | Entities: {}", intent, confidence, entities);

            String responseText;
            if ("ERROR".equals(intent)) {
                responseText = "Xin lỗi, hệ thống AI tạm thời không khả dụng. Vui lòng thử lại sau.";
            } else if (confidence < 0.5 && !CHITCHAT_INTENTS.contains(intent)) {
                responseText = "Xin lỗi, tôi chưa hiểu rõ yêu cầu của bạn. Bạn có thể hỏi về:\n"
                        + "• Điểm số riêng\n"
                        + "• Chuyên cần riêng\n"
                        + "• Sinh viên nguy cơ thôi học\n"
                        + "• Cấu hình hệ thống (admin)\n"
                        + "• Lớp chủ nhiệm (cố vấn)";
                intent = "UNKNOWN";
            } else {
                responseText = processIntent(intent, entities, username, role);
                if (responseText == null || responseText.isBlank()) {
                    responseText = responseGenerator.dataMissing();
                }
            }

            saveChatHistory(username, request.getMessage(), responseText, intent);
            return createResponse(responseText, intent, confidence, entities);

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage(), e);
            String errorResponse = "Xin lỗi, đã xảy ra lỗi khi xử lý tin nhắn của bạn.";
            saveChatHistory(resolveUsername(request), request.getMessage(), errorResponse, "ERROR");
            return createResponse(errorResponse, "ERROR", 0.0, new HashMap<>());
        }
    }

    private String resolveUsername(ChatRequestDTO request) {
        if (request.getUsername() != null && !request.getUsername().isBlank()) return request.getUsername();
        if (request.getUserId() != null) return "user-" + request.getUserId();
        return "guest";
    }

    private String resolveRole(ChatRequestDTO request) {
        if (request.getRole() != null && !request.getRole().isBlank()) return request.getRole();
        return "STUDENT";
    }

    // ============================================================
    // INTENT ROUTING
    // ============================================================

    private String processIntent(String intent, Map<String, String> entities, String username, String role) {
        try {
            return switch (intent) {
                // CHITCHAT - dùng ResponseGenerator cho câu trả lời đa dạng
                case "GREET" -> responseGenerator.greet(role, username);
                case "GOODBYE" -> responseGenerator.goodbye();
                case "THANK" -> responseGenerator.thank();
                case "HELP" -> responseGenerator.help(role);

                // ADMIN
                case "CONFIG_WARNING_THRESHOLD" -> handleConfigThreshold(entities, role);
                case "TRIGGER_MOODLE_SYNC" -> handleTriggerSync(entities, role);
                case "ADMIN_VIEW_SYSTEM_STATS" -> handleSystemStats(role);
                case "ADMIN_CHECK_API_STATUS" -> handleCheckApiStatus(role);

                // STUDENT
                case "LIST_OWN_GRADES", "QUERY_STUDENT_GRADES" -> handleStudentGrades(entities, username, role);
                case "CHECK_OWN_RISK_STATUS", "QUERY_STUDENT_ATTENDANCE" -> handleStudentAttendance(entities, username, role);
                case "GET_IMPROVEMENT_SUGGESTIONS" -> handleImprovementSuggestions(entities, role);
                case "QUERY_STUDENT_INFO" -> handleStudentInfo(entities, username, role);
                case "QUERY_STUDENT_LAST_ACCESS" -> handleLastAccess(entities, username, role);
                case "QUERY_STUDENT_FULL_REPORT" -> handleFullReport(entities, username, role);

                // LECTURER
                case "CHECK_SUBMISSIONS_AND_REMIND" -> handleSubmissionRemind(entities, role);
                case "FILTER_COURSE_RISK" -> handleCourseRiskFilter(entities, role);
                case "QUERY_STUDENT_INFO_NLP" -> handleStudentInfo(entities, username, role);

                // ADVISER
                case "VIEW_CLASS_RISK_SUMMARY" -> handleClassRiskSummary(entities, role);
                case "FIND_INACTIVE_STUDENTS" -> handleFindInactiveStudents(entities, role);

                // GENERAL
                case "QUERY_AT_RISK_LIST", "QUERY_RED_ALERT_LIST", "QUERY_YELLOW_ALERT_LIST" -> handleAtRiskList(intent, entities, role);
                case "QUERY_CLASS_SUMMARY" -> handleClassSummary(entities, role);
                case "QUERY_RISK_STATUS" -> handleRiskStatus(entities, username, role);
                case "ACTION_SEND_WARNING_NOTIFICATION" -> handleSendNotification(entities, role);

                case "QUERY_GRADE_ONLY" -> handleGradeOnly(entities, username, role);
                case "QUERY_ATTENDANCE_ONLY" -> handleAttendanceOnly(entities, username, role);

                case "PERMISSION_DENIED" -> responseGenerator.permissionDenied(role, intent);

                default -> responseGenerator.unknown(role);
            };
        } catch (Exception e) {
            log.error("Error processing intent {}: {}", intent, e.getMessage(), e);
            return "Xin lỗi, đã xảy ra lỗi khi xử lý yêu cầu của bạn.";
        }
    }

    // ============================================================
    // CHITCHAT
    // ============================================================

    private String handleGreeting(String role) {
        if ("LECTURER".equals(role) || "ADMIN".equals(role)) {
            return "Xin chào Thầy/Cô! 👋\n\nTôi là EduGuard - trợ lý hỗ trợ theo dõi tiến độ học tập.\n\n"
                    + "Tôi có thể giúp Thầy/Cô:\n"
                    + "📊 Xem điểm và chuyên cần sinh viên\n"
                    + "⚠️ Theo dõi sinh viên có nguy cơ cao\n"
                    + "📈 Thống kê tổng quan lớp học\n"
                    + "📨 Gửi cảnh báo và thông báo";
        }
        if ("ADVISER".equals(role)) {
            return "Xin chào Cố vấn! 👋\n\nTôi là EduGuard - trợ lý hỗ trợ theo dõi sinh viên.\n\n"
                    + "Tôi có thể giúp:\n"
                    + "📊 Xem tình hình lớp chủ nhiệm\n"
                    + "⚠️ Theo dõi SV nguy cơ theo lớp\n"
                    + "📭 Tìm SV ngừng tương tác";
        }
        return "Xin chào Bạn! 👋\n\nTôi là EduGuard - trợ lý hỗ trợ học tập.\n\n"
                + "Tôi có thể giúp bạn:\n"
                + "📊 Xem điểm số của mình\n"
                + "📅 Kiểm tra chuyên cần\n"
                + "⚠️ Xem tình trạng học tập";
    }

    private String handleHelp(String role) {
        if ("STUDENT".equals(role)) {
            return "📚 Hướng dẫn cho Sinh viên:\n\n"
                    + "• \"Xem điểm của em\"\n"
                    + "• \"Bảng điểm của em\"\n"
                    + "• \"Chuyên cần của em\"\n"
                    + "• \"Em có bị cảnh báo không?\"\n"
                    + "• \"Làm sao để cải thiện điểm môn Java?\"";
        }
        if ("LECTURER".equals(role)) {
            return "📚 Hướng dẫn cho Giảng viên:\n\n"
                    + "• \"Lớp Java có ai chưa nộp bài?\"\n"
                    + "• \"Môn CSDL có bao nhiêu SV mức đỏ?\"\n"
                    + "• \"Sinh viên 110122001 có đủ điều kiện thi không?\"\n"
                    + "• \"Kiểm tra điểm MSSV 110122223\"";
        }
        if ("ADVISER".equals(role)) {
            return "📚 Hướng dẫn cho Cố vấn học tập:\n\n"
                    + "• \"Lớp DA22TTB có bao nhiêu SV bị cảnh báo?\"\n"
                    + "• \"Sinh viên ngừng tương tác lớp tôi\"\n"
                    + "• \"Danh sách SV mức đỏ lớp cố vấn\"\n"
                    + "• \"Tình hình học vụ tổng quan lớp\"";
        }
        return "📚 Hướng dẫn cho Quản trị viên:\n\n"
                + "• \"Cấu hình ngưỡng cảnh báo\"\n"
                + "• \"Đồng bộ dữ liệu Moodle\"\n"
                + "• \"Kiểm tra trạng thái API\"\n"
                + "• \"Thống kê hệ thống\"";
    }

    // ============================================================
    // STUDENT HANDLERS
    // ============================================================

    private String handleStudentGrades(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);
        String courseName = entities.get("course_name");

        if ("STUDENT".equals(role)) {
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return responseGenerator.gradesPermissionDenied();
            }
            studentId = username;
        }

        Optional<Map<String, Object>> userOpt = directQuery.findUserByUsername(studentId);
        if (userOpt.isEmpty()) {
            return responseGenerator.gradesNotFound(studentId);
        }
        long userId = ((Number) userOpt.get().get("id")).longValue();
        String fullName = (String) userOpt.get().get("fullname");

        List<Map<String, Object>> courses = directQuery.findEnrolledCourses(userId);
        if (courses.isEmpty()) {
            return responseGenerator.gradesNoCourses(studentId, fullName);
        }

        List<Map<String, Object>> filteredCourses = new ArrayList<>();
        if (courseName != null && !courseName.isBlank()) {
            for (Map<String, Object> c : courses) {
                String cname = (String) c.get("fullname");
                if (cname != null && cname.toLowerCase().contains(courseName.toLowerCase())) {
                    filteredCourses.add(c);
                }
            }
            if (filteredCourses.isEmpty()) {
                return responseGenerator.gradesNoMatch(courseName);
            }
        } else {
            filteredCourses = courses;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(responseGenerator.gradesHeader(studentId, fullName, filteredCourses.size())).append("\n\n");

        for (Map<String, Object> c : filteredCourses) {
            long courseId = ((Number) c.get("id")).longValue();
            String cname = (String) c.get("fullname");
            sb.append("📘 ").append(cname).append("\n");

            List<Map<String, Object>> assigns = directQuery.findAssignmentGrades(courseId, userId);
            if (assigns.isEmpty()) {
                sb.append(responseGenerator.gradesEmptyCourse(cname)).append("\n\n");
                continue;
            }

            double sumGrade = 0;
            int gradeCount = 0;
            int submittedCount = 0;

            for (Map<String, Object> a : assigns) {
                String aname = (String) a.get("assignname");
                Object gradeObj = a.get("grade");
                Object maxObj = a.get("maxgrade");
                String status = (String) a.get("submitstatus");
                long submittedAt = a.get("submittedat") != null ? ((Number) a.get("submittedat")).longValue() : 0;

                double grade = gradeObj != null ? ((Number) gradeObj).doubleValue() : 0.0;
                double maxGrade = maxObj != null ? ((Number) maxObj).doubleValue() : 100.0;

                if ("submitted".equals(status)) submittedCount++;
                if (grade > 0) {
                    sumGrade += grade;
                    gradeCount++;
                } else if ("submitted".equals(status)) {
                    gradeCount++;
                }

                int daysAgo = submittedAt > 0 ? (int) ((System.currentTimeMillis() / 1000 - submittedAt) / 86400) : -1;
                sb.append(responseGenerator.gradeLine(aname, grade, maxGrade, status, daysAgo)).append("\n");
            }

            double avg = gradeCount > 0 ? sumGrade / gradeCount : 0.0;
            sb.append(responseGenerator.gradesFooter(submittedCount, assigns.size(), avg)).append("\n");
        }
        return sb.toString();
    }

    private String handleGradeOnly(Map<String, String> entities, String username, String role) {
        return handleStudentGrades(entities, username, role);
    }

    private String handleAttendanceOnly(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);

        if ("STUDENT".equals(role)) {
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return responseGenerator.permissionDenied(role, "QUERY_ATTENDANCE_ONLY");
            }
            studentId = username;
        }

        Optional<Map<String, Object>> userOpt = directQuery.findUserByUsername(studentId);
        if (userOpt.isEmpty()) {
            return responseGenerator.gradesNotFound(studentId);
        }
        Map<String, Object> user = userOpt.get();
        long userId = ((Number) user.get("id")).longValue();
        String fullName = (String) user.get("fullname");
        long daysAccess = directQuery.getDaysSinceAccess(userId);

        StringBuilder sb = new StringBuilder();
        sb.append(responseGenerator.attendanceHeader(fullName, studentId)).append("\n\n");
        sb.append(responseGenerator.attendanceLastAccess(daysAccess)).append("\n");
        sb.append(responseGenerator.attendanceWarning(daysAccess)).append("\n");
        return sb.toString();
    }

    private String handleStudentAttendance(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);
        String courseName = entities.get("course_name");

        if ("STUDENT".equals(role)) {
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return responseGenerator.permissionDenied(role, "QUERY_STUDENT_ATTENDANCE");
            }
            studentId = username;
        }

        Optional<Map<String, Object>> userOpt = directQuery.findUserByUsername(studentId);
        if (userOpt.isEmpty()) {
            return responseGenerator.gradesNotFound(studentId);
        }
        Map<String, Object> user = userOpt.get();
        long userId = ((Number) user.get("id")).longValue();
        String fullName = (String) user.get("fullname");
        long daysAccess = directQuery.getDaysSinceAccess(userId);

        StringBuilder sb = new StringBuilder();
        sb.append(responseGenerator.attendanceHeader(fullName, studentId)).append("\n\n");
        sb.append(responseGenerator.attendanceLastAccess(daysAccess)).append("\n");
        sb.append(responseGenerator.attendanceWarning(daysAccess)).append("\n\n");

        List<Map<String, Object>> courses = directQuery.findEnrolledCourses(userId);
        if (courses.isEmpty()) {
            sb.append("Không có môn học nào để kiểm tra chuyên cần.");
            return sb.toString();
        }

        sb.append("📋 BẢNG CHUYÊN CẦN\n\n");
        sb.append(String.format("%-35s %-18s %-10s\n", "Môn học", "Lần online cuối", "Trạng thái"));
        sb.append("────────────────────────────────────────────────────────────\n");

        boolean hasRow = false;
        for (Map<String, Object> c : courses) {
            String cname = (String) c.get("fullname");
            if (courseName != null && !courseName.isBlank() && (cname == null || !cname.toLowerCase().contains(courseName.toLowerCase()))) {
                continue;
            }

            long courseId = ((Number) c.get("id")).longValue();
            Map<String, Object> grade = directQuery.getCourseAverageGrade(courseId, userId);
            boolean has = (Boolean) grade.get("hasGrades");
            long daysSinceAccess = daysAccess;
            String status = daysSinceAccess > 14 ? "🔴 Không hoạt động" : daysSinceAccess > 7 ? "🟡 Cần theo dõi" : "🟢 Hoạt động";
            String lastAccessText = daysSinceAccess >= 999 ? "Chưa có" : daysSinceAccess + " ngày";

            sb.append(String.format("%-35s %-18s %-10s\n",
                    truncateText(cname, 34),
                    lastAccessText,
                    status));
            hasRow = true;
        }

        if (!hasRow) {
            sb.append("Không có môn nào khớp để hiển thị chuyên cần.");
        }
        return sb.toString();
    }

    private String handleImprovementSuggestions(Map<String, String> entities, String role) {
        if (!"STUDENT".equals(role)) {
            return responseGenerator.improvementForNonStudent();
        }
        String courseName = entities.getOrDefault("course_name", "môn học của em");
        return responseGenerator.improvement(courseName, 0);
    }

    // ============================================================
    // LECTURER HANDLERS
    // ============================================================

    private String handleSubmissionRemind(Map<String, String> entities, String role) {
        if ("STUDENT".equals(role)) {
            return responseGenerator.submissionStudentDenied();
        }

        String courseName = entities.get("course_name");
        if (courseName == null) {
            return responseGenerator.submissionAskCourse();
        }

        String activityType = "Assignment";
        String lower = courseName.toLowerCase();
        if (lower.contains("quiz")) activityType = "Quiz";
        else if (lower.contains("lab")) activityType = "Lab";

        ResponseEntity<?> response = moodleRestController.getMissingSubmissions(courseName, activityType);
        return formatMissingSubmissionsResponse(response, courseName, activityType);
    }

    private String handleCourseRiskFilter(Map<String, String> entities, String role) {
        if ("STUDENT".equals(role)) {
            return responseGenerator.studentPermissionDenied();
        }

        String courseName = entities.get("course_name");
        String riskLevel = entities.getOrDefault("risk_level", "yellow");
        if (riskLevel.equals("cao")) riskLevel = "red";
        if (riskLevel.equals("trung bình") || riskLevel.equals("thấp")) riskLevel = "yellow";

        if (courseName == null) {
            return responseGenerator.courseRiskAskCourse();
        }

        Optional<Map<String, Object>> courseOpt = directQuery.findCourseByKeyword(courseName);
        if (courseOpt.isEmpty()) {
            return responseGenerator.courseRiskNotFound(courseName);
        }
        Map<String, Object> course = courseOpt.get();
        long courseId = ((Number) course.get("id")).longValue();
        String realCourseName = (String) course.get("fullname");

        List<Map<String, Object>> enrolled = directQuery.findEnrolledStudents(courseId);
        if (enrolled.isEmpty()) {
            return responseGenerator.courseRiskNoEnrollment(realCourseName);
        }

        List<Map<String, Object>> atRisk = new ArrayList<>();
        int redCount = 0, yellowCount = 0, greenCount = 0;
        for (Map<String, Object> stu : enrolled) {
            long userId = ((Number) stu.get("id")).longValue();
            Map<String, Object> grade = directQuery.getCourseAverageGrade(courseId, userId);
            long daysAccess = directQuery.getDaysSinceAccess(userId);
            double avg = ((Number) grade.get("avgGrade")).doubleValue();
            boolean hasGrades = (Boolean) grade.get("hasGrades");

            String level = "green";
            if ((hasGrades && avg < 50) || daysAccess > 14) {
                level = "red";
            } else if ((hasGrades && avg < 80) || daysAccess > 7) {
                level = "yellow";
            }
            if ("red".equals(level)) redCount++;
            else if ("yellow".equals(level)) yellowCount++;
            else greenCount++;

            // Lọc theo mức yêu cầu (chính xác)
            boolean match = false;
            if ("red".equals(riskLevel) && "red".equals(level)) match = true;
            else if ("yellow".equals(riskLevel) && "yellow".equals(level)) match = true;
            else if ("green".equals(riskLevel) && "green".equals(level)) match = true;
            else if ("all".equals(riskLevel)) match = true;

            if (match) {
                Map<String, Object> row = new LinkedHashMap<>(stu);
                row.put("avgGrade", avg);
                row.put("hasGrades", hasGrades);
                row.put("daysSinceAccess", daysAccess);
                row.put("riskLevel", level);
                atRisk.add(row);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(responseGenerator.courseRiskHeader(realCourseName)).append("\n\n");
        sb.append(responseGenerator.courseRiskSummary(redCount, yellowCount, greenCount, enrolled.size()));

        if (atRisk.isEmpty()) {
            sb.append(responseGenerator.courseRiskNoMatch(riskLevel));
        } else {
            sb.append(responseGenerator.courseRiskListHeader(riskLevel)).append("\n");
            int idx = 1;
            for (Map<String, Object> s : atRisk) {
                String icon = "🟢";
                String level = (String) s.get("riskLevel");
                if ("yellow".equals(level)) icon = "🟡";
                if ("red".equals(level)) icon = "🔴";
                sb.append(responseGenerator.courseRiskRow(idx++, icon,
                        (String) s.get("username"),
                        (String) s.get("fullname"),
                        ((Number) s.get("avgGrade")).doubleValue(),
                        (Boolean) s.get("hasGrades"),
                        ((Number) s.get("daysSinceAccess")).longValue()));
            }
            // Auto gửi thông báo cho giáo viên phụ trách + cố vấn (mức đỏ)
            if ("red".equals(riskLevel)) {
                try {
                    int notifSent = 0;
                    for (Map<String, Object> s : atRisk) {
                        if ("red".equals(s.get("riskLevel"))) {
                            long sid = ((Number) s.get("id")).longValue();
                            String reason = (Boolean) s.get("hasGrades")
                                    ? "Điểm TB thấp + không online " + s.get("daysSinceAccess") + " ngày"
                                    : "Chưa có điểm và không online " + s.get("daysSinceAccess") + " ngày";
                            notifSent += notificationService.notifyViolation(sid, courseId, "red", reason);
                        }
                    }
                    if (notifSent > 0) {
                        sb.append(responseGenerator.courseRiskNotifSent(notifSent));
                    }
                } catch (Exception ex) {
                    log.warn("Auto notification failed: {}", ex.getMessage());
                }
            }
        }
        return sb.toString();
    }

    private String handleStudentInfo(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);

        if ("STUDENT".equals(role)) {
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return responseGenerator.permissionDenied(role, "QUERY_STUDENT_INFO_NLP");
            }
            studentId = username;
        }

        Optional<Map<String, Object>> userOpt = directQuery.findUserByUsername(studentId);
        if (userOpt.isEmpty()) {
            return responseGenerator.gradesNotFound(studentId);
        }
        Map<String, Object> user = userOpt.get();
        long userId = ((Number) user.get("id")).longValue();
        String fullName = (String) user.get("fullname");
        long daysAccess = directQuery.getDaysSinceAccess(userId);

        // Lấy danh sách môn + điểm
        List<Map<String, Object>> courses = directQuery.findEnrolledCourses(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("👤 THÔNG TIN SINH VIÊN\n\n");
        sb.append("🆔 MSSV: ").append(studentId).append("\n");
        sb.append("📛 Họ tên: ").append(fullName).append("\n");
        sb.append("📧 Email: ").append(user.get("email") != null ? user.get("email") : "-").append("\n");
        sb.append(responseGenerator.attendanceLastAccess(daysAccess)).append("\n");
        sb.append("🏫 Khoa: ").append(user.get("department") != null ? user.get("department") : "Khoa CNTT").append("\n");
        sb.append("📚 Tổng số môn đang học: ").append(courses.size()).append("\n\n");

        // Trạng thái rủi ro tổng quan
        int red = 0, yellow = 0, green = 0;
        for (Map<String, Object> c : courses) {
            long cid = ((Number) c.get("id")).longValue();
            Map<String, Object> grade = directQuery.getCourseAverageGrade(cid, userId);
            double avg = ((Number) grade.get("avgGrade")).doubleValue();
            boolean has = (Boolean) grade.get("hasGrades");
            if ((has && avg < 50) || daysAccess > 14) red++;
            else if ((has && avg < 80) || daysAccess > 7) yellow++;
            else green++;
        }
        sb.append(responseGenerator.classSummaryStats(red, yellow, green, courses.size())).append("\n");

        if (!courses.isEmpty()) {
            sb.append("📚 Chi tiết các môn:\n");
            for (Map<String, Object> c : courses) {
                long cid = ((Number) c.get("id")).longValue();
                Map<String, Object> grade = directQuery.getCourseAverageGrade(cid, userId);
                double avg = ((Number) grade.get("avgGrade")).doubleValue();
                boolean has = (Boolean) grade.get("hasGrades");
                int items = ((Number) grade.get("itemsCount")).intValue();
                String level = "green";
                if ((has && avg < 50) || daysAccess > 14) level = "red";
                else if ((has && avg < 70) || daysAccess > 7) level = "yellow";

                String icon = "🟢";
                if ("yellow".equals(level)) icon = "🟡";
                if ("red".equals(level)) icon = "🔴";

                sb.append("  ").append(icon).append(" ").append(c.get("fullname"));
                if (has) {
                    sb.append(" - TB: ").append(String.format("%.2f", avg)).append(" / 100 (").append(items).append(" bài)");
                } else {
                    sb.append(" - chưa có điểm");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String handleLastAccess(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);

        if ("STUDENT".equals(role)) {
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return responseGenerator.permissionDenied(role, "QUERY_STUDENT_LAST_ACCESS");
            }
            studentId = username;
        }

        ResponseEntity<?> response = moodleRestController.getLastAccess(studentId);
        return formatLastAccessResponse(response);
    }

    private String handleFullReport(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);

        if ("STUDENT".equals(role)) {
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return responseGenerator.permissionDenied(role, "QUERY_STUDENT_FULL_REPORT");
            }
            studentId = username;
        }

        StringBuilder report = new StringBuilder("📋 BÁO CÁO ĐẦY ĐỦ\n\n");
        report.append(formatStudentStatusResponse(moodleRestController.getStudentStatus(studentId)));
        report.append("\n\n").append(formatGradesResponse(moodleRestController.getStudentGrades(studentId, null)));
        return report.toString();
    }

    // ============================================================
    // ADVISER HANDLERS
    // ============================================================

    private String handleClassRiskSummary(Map<String, String> entities, String role) {
        if ("STUDENT".equals(role)) {
            return responseGenerator.studentPermissionDenied();
        }

        String classCode = entities.get("class_code");
        if (classCode == null || classCode.isBlank()) {
            // Lấy tất cả cohorts làm gợi ý
            List<Map<String, Object>> cohorts = directQuery.findAllCohorts();
            return responseGenerator.classSummaryAskClass(cohorts);
        }

        List<Map<String, Object>> students = directQuery.findStudentsByCohortName(classCode);
        if (students.isEmpty()) {
            return responseGenerator.classSummaryNotFound(classCode);
        }

        int red = 0, yellow = 0, green = 0;
        List<Map<String, Object>> atRiskStudents = new ArrayList<>();

        for (Map<String, Object> stu : students) {
            long userId = ((Number) stu.get("id")).longValue();
            long daysAccess = directQuery.getDaysSinceAccess(userId);
            String level = "green";
            if (daysAccess > 14) level = "red";
            else if (daysAccess > 7) level = "yellow";

            if ("red".equals(level)) red++;
            else if ("yellow".equals(level)) yellow++;
            else green++;

            if (!"green".equals(level)) {
                Map<String, Object> row = new LinkedHashMap<>(stu);
                row.put("daysSinceAccess", daysAccess);
                row.put("riskLevel", level);
                atRiskStudents.add(row);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(responseGenerator.classSummaryHeader(classCode)).append("\n\n");
        sb.append(responseGenerator.classSummaryStats(red, yellow, green, students.size()));

        if (atRiskStudents.isEmpty()) {
            sb.append(responseGenerator.classSummaryAllGood());
        } else {
            sb.append(responseGenerator.classSummaryAtRiskHeader(atRiskStudents.size())).append("\n");
            int idx = 1;
            for (Map<String, Object> s : atRiskStudents) {
                String icon = "🟡";
                if ("red".equals(s.get("riskLevel"))) icon = "🔴";
                sb.append(responseGenerator.classSummaryRow(idx++, icon,
                        (String) s.get("username"),
                        (String) s.get("fullname"),
                        ((Number) s.get("daysSinceAccess")).longValue())).append("\n");
            }
        }
        return sb.toString();
    }

    private String handleFindInactiveStudents(Map<String, String> entities, String role) {
        if ("STUDENT".equals(role)) {
            return responseGenerator.studentPermissionDenied();
        }

        int inactiveDays = 14;
        if (entities.containsKey("inactive_days")) {
            try {
                inactiveDays = Integer.parseInt(entities.get("inactive_days"));
            } catch (NumberFormatException ignored) {
            }
        }

        String classCode = entities.get("class_code");
        List<Map<String, Object>> students;
        if (classCode != null && !classCode.isBlank()) {
            students = directQuery.findStudentsByCohortName(classCode);
        } else {
            // Lấy tất cả SV (username bắt đầu 1101)
            students = directQuery.findAllStudents();
        }

        List<Map<String, Object>> inactive = new ArrayList<>();
        for (Map<String, Object> stu : students) {
            long userId = ((Number) stu.get("id")).longValue();
            long days = directQuery.getDaysSinceAccess(userId);
            if (days >= inactiveDays) {
                Map<String, Object> row = new LinkedHashMap<>(stu);
                row.put("daysSinceAccess", days);
                inactive.add(row);
            }
        }

        if (inactive.isEmpty()) {
            return responseGenerator.inactiveStudentsEmpty(inactiveDays, classCode);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(responseGenerator.inactiveStudentsHeader(inactiveDays)).append("\n\n");
        if (classCode != null) sb.append("Lớp: ").append(classCode).append("\n");
        sb.append(responseGenerator.inactiveStudentsCount(inactive.size(), classCode)).append("\n");
        int idx = 1;
        for (Map<String, Object> s : inactive) {
            sb.append(responseGenerator.inactiveStudentRow(idx++,
                    (String) s.get("username"),
                    (String) s.get("fullname"),
                    ((Number) s.get("daysSinceAccess")).longValue())).append("\n");
        }
        return sb.toString();
    }

    // ============================================================
    // ADMIN HANDLERS
    // ============================================================

    private String handleConfigThreshold(Map<String, String> entities, String role) {
        if (!"ADMIN".equals(role)) {
            return responseGenerator.adminPermissionDenied();
        }
        return responseGenerator.configThreshold();
    }

    private String handleTriggerSync(Map<String, String> entities, String role) {
        if (!"ADMIN".equals(role)) {
            return responseGenerator.adminPermissionDenied();
        }
        return responseGenerator.triggerSync();
    }

    private String handleSystemStats(String role) {
        if (!"ADMIN".equals(role)) {
            return responseGenerator.adminPermissionDenied();
        }
        ResponseEntity<?> response = moodleRestController.getClassOverview();
        return formatResponse(response);
    }

    private String handleCheckApiStatus(String role) {
        if (!"ADMIN".equals(role)) {
            return responseGenerator.adminPermissionDenied();
        }
        return responseGenerator.checkApiStatus();
    }

    // ============================================================
    // GENERAL HANDLERS
    // ============================================================

    private String handleAtRiskList(String intent, Map<String, String> entities, String role) {
        if ("STUDENT".equals(role)) {
            return responseGenerator.studentPermissionDenied();
        }

        String riskLevel = null;
        if ("QUERY_RED_ALERT_LIST".equals(intent)) riskLevel = "red";
        else if ("QUERY_YELLOW_ALERT_LIST".equals(intent)) riskLevel = "yellow";
        else riskLevel = entities.get("risk_level");

        // LỌC CHỈ SINH VIÊN VI PHẠM
        ResponseEntity<?> response = moodleRestController.getAtRiskStudents(riskLevel, 50.0, 1, 14, true);
        return formatAtRiskStudentsResponse(response, true);
    }

    private String handleClassSummary(Map<String, String> entities, String role) {
        if ("STUDENT".equals(role)) {
            return responseGenerator.studentPermissionDenied();
        }
        ResponseEntity<?> response = moodleRestController.getClassOverview();
        return formatResponse(response);
    }

    private String handleRiskStatus(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);
        if ("STUDENT".equals(role)) {
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return responseGenerator.permissionDenied(role, "QUERY_RISK_STATUS");
            }
            studentId = username;
        }
        ResponseEntity<?> response = moodleRestController.getStudentStatus(studentId);
        return formatStudentStatusResponse(response);
    }

    private String handleSendNotification(Map<String, String> entities, String role) {
        if ("STUDENT".equals(role)) {
            return responseGenerator.studentPermissionDenied();
        }
        String studentId = entities.get("mssv");
        String riskLevel = entities.get("risk_level");
        ResponseEntity<?> response = moodleRestController.sendNotification(studentId, riskLevel, "Cảnh báo học tập từ hệ thống EduGuard");
        return formatResponse(response);
    }

    // ============================================================
    // FORMAT RESPONSES
    // ============================================================

    private String formatResponse(ResponseEntity<?> response) {
        if (response == null || response.getBody() == null) return "Không có dữ liệu";
        Object body = response.getBody();
        if (body instanceof Map) {
            return formatGenericMap((Map<?, ?>) body);
        }
        return body.toString();
    }

    private String formatGenericMap(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            if ("students".equals(key) || "courses".equals(key) || "missingStudents".equals(key)
                    || "atRiskStudents".equals(key) || "inactiveStudents".equals(key)) continue;
            Object value = entry.getValue();
            sb.append(key).append(": ").append(value).append("\n");
        }
        return sb.toString();
    }

    private String formatGradesResponse(ResponseEntity<?> response) {
        if (response == null || response.getBody() == null) return "Không có dữ liệu điểm";
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        if (body.containsKey("error")) return "❌ " + body.get("error");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> courses = (List<Map<String, Object>>) body.get("courses");
        if (courses == null || courses.isEmpty()) return "Không có dữ liệu điểm";

        StringBuilder sb = new StringBuilder("📊 BẢNG ĐIỂM SINH VIÊN\n\n");
        String studentId = (String) body.get("studentId");
        sb.append("MSSV: ").append(studentId).append("\n\n");

        for (Map<String, Object> course : courses) {
            String courseName = (String) course.get("courseName");
            sb.append("📚 ").append(courseName).append("\n");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> grades = (List<Map<String, Object>>) course.get("grades");
            if (grades != null && !grades.isEmpty()) {
                for (Map<String, Object> grade : grades) {
                    String itemName = (String) grade.get("itemName");
                    Object gradeRaw = grade.get("gradeRaw");
                    if (gradeRaw != null) {
                        sb.append("  • ").append(itemName).append(": ")
                          .append(String.format("%.1f", gradeRaw)).append("/")
                          .append(String.format("%.1f", grade.get("gradeMax")))
                          .append(" (").append(String.format("%.1f", grade.get("percentage"))).append("%)\n");
                    } else {
                        sb.append("  • ").append(itemName).append(": ⚠️ Chưa có điểm\n");
                    }
                }
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String formatAttendanceResponse(ResponseEntity<?> response) {
        if (response == null || response.getBody() == null) return "Không có dữ liệu chuyên cần";
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        if (body.containsKey("error")) return "❌ " + body.get("error");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> courses = (List<Map<String, Object>>) body.get("courses");
        if (courses == null || courses.isEmpty()) return "Không có dữ liệu chuyên cần";

        StringBuilder sb = new StringBuilder("📅 CHUYÊN CẦN SINH VIÊN\n\n");
        sb.append("MSSV: ").append(body.get("studentId")).append("\n\n");

        for (Map<String, Object> course : courses) {
            sb.append("📚 ").append(course.get("courseName")).append("\n");
            Object lastAccess = course.get("lastAccess");
            Object days = course.get("daysSinceAccess");
            Object status = course.get("status");

            if (lastAccess != null) {
                sb.append("  • Lần cuối truy cập: ").append(lastAccess).append("\n");
                sb.append("  • Số ngày: ").append(days).append("\n");
                sb.append("  • Trạng thái: ").append("inactive".equals(status) ? "🔴 Không hoạt động" : "🟢 Hoạt động").append("\n");
            } else {
                sb.append("  • ⚠️ Chưa từng truy cập môn này\n");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String formatStudentStatusResponse(ResponseEntity<?> response) {
        if (response == null || response.getBody() == null) return "Không có dữ liệu";
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        if (body.containsKey("error")) return "❌ " + body.get("error");

        StringBuilder sb = new StringBuilder("📊 TÌNH TRẠNG HỌC TẬP\n\n");
        sb.append("MSSV: ").append(body.get("studentId")).append("\n");
        Object fullName = body.get("fullName");
        sb.append("Họ tên: ").append(fullName != null ? fullName : "N/A").append("\n");
        sb.append("Tổng số môn: ").append(body.get("totalCourses")).append("\n");
        sb.append("Điểm TB: ").append(body.get("avgGrade")).append("\n");
        sb.append("Môn không hoạt động: ").append(body.get("inactiveCourses")).append("\n\n");
        sb.append("Mức cảnh báo: ").append(formatRiskLevel((String) body.get("riskLevel"))).append("\n");
        return sb.toString();
    }

    private String formatRiskLevel(String level) {
        if (level == null) return "⚪ Chưa xác định";
        return switch (level) {
            case "green" -> "🟢 XANH - An toàn";
            case "yellow" -> "🟡 VÀNG - Cần theo dõi";
            case "red" -> "🔴 ĐỎ - Nguy cơ cao";
            default -> "⚪ " + level;
        };
    }

    private String formatLastAccessResponse(ResponseEntity<?> response) {
        if (response == null || response.getBody() == null) return "Không có dữ liệu";
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        if (body.containsKey("error")) return "❌ " + body.get("error");
        return formatAttendanceResponse(response);
    }

    /**
     * Format danh sách SV vi phạm - CHỈ hiển thị SV thực sự vi phạm
     * Dùng ResponseGenerator cho phần header/count/note đa dạng.
     */
    private String formatAtRiskStudentsResponse(ResponseEntity<?> response, boolean onlyViolations) {
        if (response == null || response.getBody() == null) return "Không có dữ liệu";
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) body.get("students");
        Integer count = (Integer) body.get("count");
        Integer violationsCount = (Integer) body.get("violationsCount");
        String level = (String) body.get("riskLevel");

        StringBuilder sb = new StringBuilder();
        sb.append(responseGenerator.atRiskHeader(level)).append("\n\n");

        if (count == null) count = students != null ? students.size() : 0;
        Integer total = body.get("totalAnalyzed") != null ? ((Number) body.get("totalAnalyzed")).intValue() : null;
        sb.append(responseGenerator.atRiskCount(count, total)).append("\n\n");

        if (students != null && !students.isEmpty()) {
            for (Map<String, Object> student : students) {
                String icon = "🟢";
                String level2 = (String) student.get("riskLevel");
                if ("yellow".equals(level2)) icon = "🟡";
                if ("red".equals(level2)) icon = "🔴";
                double avg = student.get("avgGrade") != null ? ((Number) student.get("avgGrade")).doubleValue() : 0;
                int inactive = student.get("inactiveCourses") != null ? ((Number) student.get("inactiveCourses")).intValue() : 0;
                Object days = student.get("maxDaysSinceAccess");
                sb.append(responseGenerator.atRiskRow(icon,
                        String.valueOf(student.get("studentId")),
                        String.valueOf(student.get("fullName")),
                        avg, inactive, days));
            }
        } else {
            sb.append(responseGenerator.atRiskEmpty());
        }

        if (onlyViolations) {
            sb.append(responseGenerator.atRiskNote());
        }
        return sb.toString();
    }

    private String formatMissingSubmissionsResponse(ResponseEntity<?> response, String courseName, String activityType) {
        if (response == null || response.getBody() == null) return "Không có dữ liệu";
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) body.get("missingStudents");
        Integer count = (Integer) body.get("count");

        StringBuilder sb = new StringBuilder();
        sb.append("📋 SINH VIÊN CHƯA NỘP ").append(activityType.toUpperCase()).append("\n\n");
        sb.append("Môn: ").append(body.get("courseName")).append("\n");
        sb.append("Loại: ").append(activityType).append("\n");
        sb.append("Số SV chưa nộp: ").append(count != null ? count : 0).append("\n\n");

        if (students != null && !students.isEmpty()) {
            for (Map<String, Object> s : students) {
                sb.append("⚠️ ").append(s.get("studentId")).append(" - ").append(s.get("fullName")).append("\n");
                sb.append("    Đã nộp: ").append(s.get("submitted")).append("/").append(s.get("total"));
                sb.append(" (thiếu ").append(s.get("missing")).append(")\n\n");
            }
        } else {
            sb.append("✅ Tất cả sinh viên đã nộp đầy đủ.\n");
        }
        return sb.toString();
    }

    private String formatCourseRiskResponse(ResponseEntity<?> response) {
        if (response == null || response.getBody() == null) return "Không có dữ liệu";
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) body.get("atRiskStudents");
        Integer count = (Integer) body.get("count");
        Integer total = (Integer) body.get("totalStudents");

        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ SINH VIÊN CÓ RỦI RO - ").append(body.get("courseName")).append("\n\n");
        sb.append("Tổng SV: ").append(total).append(" | Có rủi ro: ").append(count).append("\n\n");

        if (students != null && !students.isEmpty()) {
            for (Map<String, Object> s : students) {
                String icon = "🟢";
                String level = (String) s.get("riskLevel");
                if ("yellow".equals(level)) icon = "🟡";
                if ("red".equals(level)) icon = "🔴";
                sb.append(icon).append(" ").append(s.get("studentId")).append(" - ").append(s.get("fullName")).append("\n");
                sb.append("    Điểm TB: ").append(s.get("avgGrade"));
                if (Boolean.TRUE.equals(s.get("hasGrades"))) {
                    sb.append(" (đã có điểm)");
                } else {
                    sb.append(" (chưa có điểm)");
                }
                sb.append(" | Không online: ").append(s.get("daysSinceAccess")).append(" ngày\n\n");
            }
        } else {
            sb.append("✅ Không có sinh viên vi phạm trong môn này.\n");
        }
        return sb.toString();
    }

    private String formatClassRiskResponse(ResponseEntity<?> response) {
        if (response == null || response.getBody() == null) return "Không có dữ liệu";
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) body.get("summary");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) body.get("atRiskStudents");

        StringBuilder sb = new StringBuilder();
        sb.append("📊 TÌNH HÌNH HỌC VỤ LỚP ").append(body.get("classCode")).append("\n\n");
        if (summary != null) {
            sb.append("🔴 Mức đỏ: ").append(summary.get("red")).append("\n");
            sb.append("🟡 Mức vàng: ").append(summary.get("yellow")).append("\n");
            sb.append("🟢 Mức xanh: ").append(summary.get("green")).append("\n");
            sb.append("📚 Tổng: ").append(summary.get("total")).append("\n\n");
        }

        if (students != null && !students.isEmpty()) {
            sb.append("⚠️ Danh sách sinh viên có rủi ro (chỉ SV vi phạm):\n\n");
            for (Map<String, Object> s : students) {
                String icon = "🟡";
                String level = (String) s.get("riskLevel");
                if ("red".equals(level)) icon = "🔴";
                sb.append(icon).append(" ").append(s.get("studentId")).append(" - ").append(s.get("fullName")).append("\n");
                sb.append("    Điểm TB: ").append(s.get("avgGrade"));
                sb.append(" | Môn không hoạt động: ").append(s.get("inactiveCourses"));
                sb.append(" | Không online: ").append(s.get("maxDaysSinceAccess")).append(" ngày\n\n");
            }
        } else {
            sb.append("✅ Lớp không có sinh viên vi phạm.\n");
        }
        return sb.toString();
    }

    private String formatInactiveStudentsResponse(ResponseEntity<?> response) {
        if (response == null || response.getBody() == null) return "Không có dữ liệu";
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) body.get("inactiveStudents");
        Integer count = (Integer) body.get("count");
        Integer threshold = (Integer) body.get("inactiveDaysThreshold");

        StringBuilder sb = new StringBuilder();
        sb.append("📭 SINH VIÊN NGỪNG TƯƠNG TÁC (>").append(threshold).append(" ngày)\n\n");
        sb.append("Số sinh viên vi phạm: ").append(count).append("\n\n");

        if (students != null && !students.isEmpty()) {
            for (Map<String, Object> s : students) {
                long days = ((Number) s.get("daysSinceAccess")).longValue();
                sb.append("⚠️ ").append(s.get("studentId")).append(" - ").append(s.get("fullName")).append("\n");
                sb.append("    Không online: ").append(days).append(" ngày");
                sb.append(" | Lần cuối: ").append(s.get("lastAccessDate")).append("\n\n");
            }
        } else {
            sb.append("✅ Không có sinh viên nào ngừng tương tác quá ").append(threshold).append(" ngày.\n");
        }
        return sb.toString();
    }

    // ============================================================
    // HISTORY
    // ============================================================

    private void saveChatHistory(String username, String userMessage, String botResponse, String intent) {
        try {
            ChatHistory userHistory = new ChatHistory();
            userHistory.setSessionId(username);
            userHistory.setLecturerId(1L);
            userHistory.setRole(ChatRole.USER);
            userHistory.setContent(userMessage);
            userHistory.setIntent(intent);
            userHistory.setCreatedAt(LocalDateTime.now());
            chatHistoryRepository.save(userHistory);

            ChatHistory botHistory = new ChatHistory();
            botHistory.setSessionId(username);
            botHistory.setLecturerId(1L);
            botHistory.setRole(ChatRole.BOT);
            botHistory.setContent(botResponse);
            botHistory.setIntent(intent);
            botHistory.setCreatedAt(LocalDateTime.now());
            chatHistoryRepository.save(botHistory);
        } catch (Exception e) {
            log.error("Error saving chat history: {}", e.getMessage(), e);
        }
    }

    private ChatResponseDTO createResponse(String text, String intent, double confidence, Map<String, String> entities) {
        ChatResponseDTO response = new ChatResponseDTO();
        response.setReply(text);
        response.setIntent(intent);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "-";
        if (text.length() <= maxLength) return text;
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    /**
     * Get chat history for a user
     * @param username Username to retrieve history for
     * @return List of chat history records
     */
    public List<ChatHistory> getChatHistory(String username) {
        try {
            return chatHistoryRepository.findBySessionIdOrderByCreatedAtAsc(username);
        } catch (Exception e) {
            log.error("Error retrieving chat history for user {}: {}", username, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
