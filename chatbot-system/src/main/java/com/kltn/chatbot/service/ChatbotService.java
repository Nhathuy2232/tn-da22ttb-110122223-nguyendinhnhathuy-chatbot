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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chatbot Service - Sử dụng OpenAI API thay vì Rasa
 * Version 5.0 - OpenAI Integration
 * 
 * Kiến trúc mới:
 * 1. OpenAI API - Phân tích intent và entities từ tin nhắn
 * 2. ChatbotService - Điều phối request và gọi business logic
 * 3. MoodleRestController - Xử lý các REST endpoints trả về dữ liệu Moodle
 * 
 * Ưu điểm:
 * - Không cần train model
 * - Hiểu ngôn ngữ tự nhiên tốt hơn
 * - Dễ maintain và mở rộng
 * - Giảm độ phức tạp hệ thống (bỏ Python/Rasa)
 * 
 * @author Nguyễn Đình Nhật Huy - MSSV: 110122223
 * @version 5.0 - OpenAI Integration
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

    /**
     * Xử lý tin nhắn từ người dùng
     * 
     * @param request DTO chứa message, username, role
     * @return ChatResponseDTO với response text
     */
    @Transactional
    public ChatResponseDTO handleUserMessage(ChatRequestDTO request) {
        String username = resolveUsername(request);
        String role = resolveRole(request);

        log.info("========== CHATBOT REQUEST ==========");
        log.info("Username: {}", username);
        log.info("Role: {}", role);
        log.info("Message: {}", request.getMessage());
        log.info("=====================================");
        
        try {
            // Bước 1: Nhận diện nhanh chitchat (không cần gọi API)
            Map<String, Object> analysis = LocalIntentMatcher.match(request.getMessage())
                    .orElseGet(() -> openaiService.analyzeMessage(
                            request.getMessage(),
                            role,
                            username
                    ));
            
            String intent = (String) analysis.get("intent");
            double confidence = ((Number) analysis.get("confidence")).doubleValue();
            @SuppressWarnings("unchecked")
            Map<String, String> entities = (Map<String, String>) analysis.get("entities");
            String geminiResponseText = (String) analysis.get("response_text");
            
            log.info("========== OPENAI ANALYSIS ==========");
            log.info("Intent: {}", intent);
            log.info("Confidence: {}", confidence);
            log.info("Entities: {}", entities);
            log.info("Response text: {}", geminiResponseText);
            log.info("=====================================");

            // Lỗi từ Gemini API — trả thông báo thật, không dùng fallback "không hiểu"
            if ("ERROR".equals(intent)) {
                String errorResponse = (geminiResponseText != null && !geminiResponseText.isBlank())
                        ? geminiResponseText
                        : "Xin lỗi, hệ thống AI tạm thời không khả dụng. Vui lòng thử lại sau.";
                saveChatHistory(username, request.getMessage(), errorResponse, intent);
                return createResponse(errorResponse, intent, confidence, entities);
            }
            
            // Bước 2: Confidence — bỏ qua với chitchat; các intent khác cần >= 0.5
            if (confidence < 0.5 && !CHITCHAT_INTENTS.contains(intent)) {
                String fallbackResponse = (geminiResponseText != null && !geminiResponseText.isBlank())
                        ? geminiResponseText
                        : "Xin lỗi, tôi không hiểu rõ yêu cầu của bạn. Bạn có thể diễn đạt lại không?\n\n"
                          + "Ví dụ: \"Danh sách sinh viên nguy cơ\", \"Xem điểm MSSV 110122001\", \"Thống kê lớp học\"";
                saveChatHistory(username, request.getMessage(), fallbackResponse, "UNKNOWN");
                return createResponse(fallbackResponse, "UNKNOWN", confidence, entities);
            }
            
            // Bước 3: Xử lý theo intent
            String responseText = processIntent(intent, entities, username, role);
            
            // Nếu không có response từ business logic, dùng response từ Gemini
            if (responseText == null || responseText.isEmpty()) {
                responseText = geminiResponseText;
            }
            if (responseText == null || responseText.isBlank()) {
                responseText = "🤔 Tôi chưa hiểu rõ yêu cầu. Bạn có thể hỏi về điểm số, chuyên cần, sinh viên nguy cơ hoặc thống kê lớp.";
            }
            
            // Bước 4: Lưu chat history
            saveChatHistory(username, request.getMessage(), responseText, intent);
            
            // Bước 5: Trả về response
            return createResponse(responseText, intent, confidence, entities);
            
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage(), e);
            String errorResponse = "Xin lỗi, đã xảy ra lỗi khi xử lý tin nhắn của bạn. Vui lòng thử lại.";
            saveChatHistory(resolveUsername(request), request.getMessage(), errorResponse, "ERROR");
            return createResponse(errorResponse, "ERROR", 0.0, new HashMap<>());
        }
    }

    private String resolveUsername(ChatRequestDTO request) {
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            return request.getUsername();
        }
        if (request.getUserId() != null) {
            return "user-" + request.getUserId();
        }
        return "guest";
    }

    private String resolveRole(ChatRequestDTO request) {
        if (request.getRole() != null && !request.getRole().isBlank()) {
            return request.getRole();
        }
        return "LECTURER";
    }

    /**
     * Xử lý business logic theo intent
     */
    private String processIntent(String intent, Map<String, String> entities, String username, String role) {
        log.info("Processing intent: {} with entities: {}", intent, entities);
        
        try {
            switch (intent) {
                // ============ GREETING & CHITCHAT ============
                case "GREET":
                    return handleGreeting(role);
                    
                case "GOODBYE":
                    return "Tạm biệt! Chúc bạn một ngày tốt lành. 👋";
                    
                case "THANK":
                    return "Không có gì! Rất vui được giúp đỡ bạn. 😊";

                case "HELP":
                    return handleHelp(role);
                    
                // ============ STUDENT QUERIES ============
                case "LIST_OWN_GRADES":
                case "QUERY_STUDENT_GRADES":
                    return handleStudentGrades(entities, username, role);
                    
                case "CHECK_OWN_RISK_STATUS":
                case "QUERY_STUDENT_ATTENDANCE":
                    return handleStudentAttendance(entities, username, role);
                    
                case "QUERY_STUDENT_INFO":
                    return handleStudentInfo(entities, username, role);
                    
                case "QUERY_STUDENT_LAST_ACCESS":
                    return handleLastAccess(entities, username, role);
                    
                case "QUERY_STUDENT_FULL_REPORT":
                    return handleFullReport(entities, username, role);
                    
                case "GET_IMPROVEMENT_SUGGESTIONS":
                    return handleImprovementSuggestions(entities, username, role);
                    
                // ============ RISK & WARNING ============
                case "QUERY_RISK_STATUS":
                    return handleRiskStatus(entities, username, role);
                    
                case "QUERY_AT_RISK_LIST":
                case "QUERY_RED_ALERT_LIST":
                case "QUERY_YELLOW_ALERT_LIST":
                    return handleAtRiskList(intent, entities, username, role);
                    
                // ============ CLASS & STATISTICS ============
                case "QUERY_CLASS_LIST":
                    return handleClassList(username, role);
                    
                case "QUERY_CLASS_SUMMARY":
                case "VIEW_CLASS_RISK_SUMMARY":
                    return handleClassSummary(entities, username, role);
                    
                case "QUERY_GRADE_STATISTICS":
                case "QUERY_ATTENDANCE_STATISTICS":
                    return handleStatistics(intent, entities, username, role);
                    
                case "CHECK_SUBMISSIONS_AND_REMIND":
                    return handleSubmissionRemind(entities, username, role);
                    
                case "FILTER_COURSE_RISK":
                    return handleCourseRiskFilter(entities, username, role);
                    
                case "QUERY_STUDENT_INFO_NLP":
                    return handleStudentInfo(entities, username, role);
                    
                case "FIND_INACTIVE_STUDENTS":
                    return handleFindInactiveStudents(entities, username, role);
                    
                // ============ ACTIONS ============
                case "ACTION_SEND_WARNING_NOTIFICATION":
                    return handleSendNotification(entities, username, role);
                    
                // ============ ADMIN FUNCTIONS ============
                case "CONFIG_WARNING_THRESHOLD":
                    return handleConfigThreshold(entities, username, role);
                    
                case "TRIGGER_MOODLE_SYNC":
                    return handleTriggerSync(entities, username, role);
                    
                case "ADMIN_VIEW_SYSTEM_STATS":
                    return handleSystemStats(username, role);
                    
                case "ADMIN_CHECK_API_STATUS":
                    return handleCheckApiStatus(username, role);
                    
                // ============ PERMISSION DENIED ============
                case "PERMISSION_DENIED":
                    // LOG để debug
                    log.warn("Received PERMISSION_DENIED intent for user: {}, role: {}, entities: {}", 
                            username, role, entities);
                    
                    return "🔒 Bạn không có quyền thực hiện thao tác này. Vui lòng liên hệ quản trị viên.";
                    
                // ============ UNKNOWN ============
                case "UNKNOWN":
                default:
                    return "🤔 Tôi chưa hiểu rõ yêu cầu của bạn. Bạn có thể hỏi về:\n" +
                           "• Điểm số sinh viên\n" +
                           "• Chuyên cần lớp học\n" +
                           "• Danh sách sinh viên nguy cơ\n" +
                           "• Thống kê lớp học";
            }
        } catch (Exception e) {
            log.error("Error processing intent {}: {}", intent, e.getMessage(), e);
            return "Xin lỗi, đã xảy ra lỗi khi xử lý yêu cầu của bạn.";
        }
    }

    // ============================================================
    // HELPER METHODS - Gọi MoodleRestController
    // ============================================================

    private String handleHelp(String role) {
        if ("STUDENT".equals(role)) {
            return "Tôi có thể giúp bạn:\n"
                    + "• \"Điểm của tôi môn Java\"\n"
                    + "• \"Chuyên cần của tôi\"\n"
                    + "• \"Tình trạng học tập của tôi\"";
        }
        return "Tôi có thể giúp Thầy/Cô:\n"
                + "• \"Danh sách sinh viên nguy cơ\"\n"
                + "• \"Điểm sinh viên 110122001\"\n"
                + "• \"Thống kê lớp học\"\n"
                + "• \"Sinh viên mức đỏ\"";
    }

    private String handleGreeting(String role) {
        if ("LECTURER".equals(role) || "ADMIN".equals(role)) {
            return "Xin chào Thầy/Cô! 👋\n\nTôi là EduGuard, trợ lý hỗ trợ theo dõi tiến độ học tập. " +
                   "Tôi có thể giúp Thầy/Cô:\n" +
                   "📊 Xem điểm và chuyên cần sinh viên\n" +
                   "⚠️ Theo dõi sinh viên có nguy cơ cao\n" +
                   "📈 Thống kê tổng quan lớp học\n" +
                   "📨 Gửi cảnh báo và thông báo";
        } else {
            return "Xin chào Bạn! 👋\n\nTôi là EduGuard, trợ lý hỗ trợ học tập. " +
                   "Tôi có thể giúp bạn:\n" +
                   "📊 Xem điểm số của mình\n" +
                   "📅 Kiểm tra chuyên cần\n" +
                   "⚠️ Xem tình trạng học tập";
        }
    }

    private String handleStudentGrades(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);
        String courseName = entities.get("course_name");
        
        // PHÂN QUYỀN: Sinh viên chỉ được xem điểm của mình
        if ("STUDENT".equals(role)) {
            // Sinh viên hỏi về người khác → từ chối
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return "🔒 Bạn chỉ có thể xem điểm của chính mình. Vui lòng hỏi: \"Điểm của tôi\"";
            }
            // Force studentId = username (không cho sinh viên chỉ định MSSV khác)
            studentId = username;
        }
        // LECTURER và ADMIN có thể xem điểm bất kỳ sinh viên nào
        
        // Gọi API
        ResponseEntity<?> response = moodleRestController.getStudentGrades(studentId, courseName);
        return formatResponse(response);
    }

    private String handleStudentAttendance(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);
        String courseName = entities.get("course_name");
        
        // PHÂN QUYỀN: Sinh viên chỉ được xem chuyên cần của mình
        if ("STUDENT".equals(role)) {
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return "🔒 Bạn chỉ có thể xem chuyên cần của chính mình.";
            }
            studentId = username;
        }
        
        ResponseEntity<?> response = moodleRestController.getAttendance(studentId, courseName);
        return formatResponse(response);
    }

    private String handleStudentInfo(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);
        
        // PHÂN QUYỀN: Sinh viên chỉ xem thông tin của mình
        if ("STUDENT".equals(role)) {
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return "🔒 Bạn chỉ có thể xem thông tin của chính mình.";
            }
            studentId = username;
        }
        
        ResponseEntity<?> response = moodleRestController.getStudentStatus(studentId);
        return formatResponse(response);
    }

    private String handleLastAccess(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);
        
        // PHÂN QUYỀN: Sinh viên chỉ xem last access của mình
        if ("STUDENT".equals(role)) {
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return "🔒 Bạn chỉ có thể xem hoạt động của chính mình.";
            }
            studentId = username;
        }
        
        ResponseEntity<?> response = moodleRestController.getLastAccess(studentId);
        return formatResponse(response);
    }

    private String handleFullReport(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);
        
        // PHÂN QUYỀN: Sinh viên chỉ xem báo cáo của mình
        if ("STUDENT".equals(role)) {
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return "🔒 Bạn chỉ có thể xem báo cáo của chính mình.";
            }
            studentId = username;
        }
        
        // Kết hợp nhiều API calls
        StringBuilder report = new StringBuilder();
        report.append("📋 BÁO CÁO ĐẦY ĐỦ SINH VIÊN\n\n");
        
        // Grades
        ResponseEntity<?> gradesResp = moodleRestController.getStudentGrades(studentId, null);
        report.append(formatResponse(gradesResp)).append("\n\n");
        
        // Attendance
        ResponseEntity<?> attendResp = moodleRestController.getAttendance(studentId, null);
        report.append(formatResponse(attendResp)).append("\n\n");
        
        // Status
        ResponseEntity<?> statusResp = moodleRestController.getStudentStatus(studentId);
        report.append(formatResponse(statusResp));
        
        return report.toString();
    }

    private String handleRiskStatus(Map<String, String> entities, String username, String role) {
        String studentId = entities.getOrDefault("mssv", username);
        
        // PHÂN QUYỀN: Sinh viên chỉ xem risk status của mình
        if ("STUDENT".equals(role)) {
            if (entities.containsKey("mssv") && !entities.get("mssv").equals(username)) {
                return "🔒 Bạn chỉ có thể xem tình trạng học tập của chính mình.";
            }
            studentId = username;
        }
        
        ResponseEntity<?> response = moodleRestController.getStudentStatus(studentId);
        return formatResponse(response);
    }

    private String handleAtRiskList(String intent, Map<String, String> entities, String username, String role) {
        // PHÂN QUYỀN: Sinh viên KHÔNG được xem danh sách sinh viên nguy cơ
        if ("STUDENT".equals(role)) {
            return "🔒 Chức năng này chỉ dành cho giảng viên và quản trị viên.";
        }
        
        String riskLevel = null;
        
        if ("QUERY_RED_ALERT_LIST".equals(intent)) {
            riskLevel = "red";
        } else if ("QUERY_YELLOW_ALERT_LIST".equals(intent)) {
            riskLevel = "yellow";
        } else {
            riskLevel = entities.get("risk_level");
        }
        
        ResponseEntity<?> response = moodleRestController.getAtRiskStudents(riskLevel);
        return formatResponse(response);
    }

    private String handleClassList(String username, String role) {
        // Tất cả role đều có thể xem courses (nhưng sẽ filter theo quyền)
        ResponseEntity<?> response = moodleRestController.getCourses();
        return formatResponse(response);
    }

    private String handleClassSummary(Map<String, String> entities, String username, String role) {
        // PHÂN QUYỀN: Sinh viên KHÔNG được xem thống kê lớp
        if ("STUDENT".equals(role)) {
            return "🔒 Chức năng thống kê lớp học chỉ dành cho giảng viên và quản trị viên.";
        }
        
        ResponseEntity<?> response = moodleRestController.getClassOverview();
        return formatResponse(response);
    }

    private String handleStatistics(String intent, Map<String, String> entities, String username, String role) {
        // PHÂN QUYỀN: Sinh viên KHÔNG được xem thống kê
        if ("STUDENT".equals(role)) {
            return "🔒 Chức năng thống kê chỉ dành cho giảng viên và quản trị viên.";
        }
        
        ResponseEntity<?> response = moodleRestController.getClassOverview();
        return formatResponse(response);
    }

    private String handleSendNotification(Map<String, String> entities, String username, String role) {
        // PHÂN QUYỀN: Chỉ LECTURER và ADMIN
        if ("STUDENT".equals(role)) {
            return "🔒 Bạn không có quyền gửi thông báo. Chức năng này chỉ dành cho giảng viên.";
        }
        
        String studentId = entities.get("mssv");
        String riskLevel = entities.get("risk_level");
        String message = entities.getOrDefault("message", "Cảnh báo học tập");
        
        ResponseEntity<?> response = moodleRestController.sendNotification(studentId, riskLevel, message);
        return formatResponse(response);
    }

    private String handleSystemStats(String username, String role) {
        if (!"ADMIN".equals(role)) {
            return "🔒 Chức năng này chỉ dành cho quản trị viên.";
        }
        
        ResponseEntity<?> response = moodleRestController.getAtRiskStudents(null);
        return formatResponse(response);
    }

    private String handleConfigThreshold(Map<String, String> entities, String username, String role) {
        if (!"ADMIN".equals(role)) {
            return "🔒 Chức năng này chỉ dành cho quản trị viên.";
        }
        return "✅ Đã ghi nhận yêu cầu cấu hình ngưỡng cảnh báo.\n"
                + "Bạn có thể cập nhật chi tiết trong trang quản trị hoặc file cấu hình backend.";
    }

    private String handleTriggerSync(Map<String, String> entities, String username, String role) {
        if (!"ADMIN".equals(role)) {
            return "🔒 Chức năng này chỉ dành cho quản trị viên.";
        }
        return "🔄 Đã ghi nhận yêu cầu đồng bộ Moodle.\n"
                + "Hãy kiểm tra job sync, kết nối API, cache và log hệ thống để xác nhận kết quả.";
    }

    private String handleSubmissionRemind(Map<String, String> entities, String username, String role) {
        if ("STUDENT".equals(role)) {
            return "🔒 Bạn không có quyền gửi nhắc nhở cho lớp học.";
        }
        String courseName = entities.getOrDefault("course_name", "môn học");
        return "📨 Tôi đã hiểu yêu cầu nhắc nhở cho " + courseName + ".\n"
                + "Bạn có thể dùng danh sách sinh viên chưa nộp để gửi thông báo cá nhân hoá.";
    }

    private String handleCourseRiskFilter(Map<String, String> entities, String username, String role) {
        if ("STUDENT".equals(role)) {
            return "🔒 Chức năng này chỉ dành cho giảng viên và quản trị viên.";
        }
        String courseName = entities.getOrDefault("course_name", "môn học");
        String riskLevel = entities.getOrDefault("risk_level", "all");
        return "⚠️ Tôi đã hiểu yêu cầu lọc rủi ro cho " + courseName + " (mức: " + riskLevel + ").\n"
                + "Hệ thống sẽ trả về danh sách sinh viên phù hợp theo bộ lọc.";
    }

    private String handleFindInactiveStudents(Map<String, String> entities, String username, String role) {
        if ("STUDENT".equals(role)) {
            return "🔒 Chức năng này chỉ dành cho cố vấn học tập, giảng viên và quản trị viên.";
        }
        return "📭 Tôi đã hiểu yêu cầu tìm sinh viên ít tương tác / không online.\n"
                + "Hệ thống sẽ lọc theo thời gian không đăng nhập và tần suất tương tác LMS.";
    }

    private String handleImprovementSuggestions(Map<String, String> entities, String username, String role) {
        if (!"STUDENT".equals(role)) {
            return "ℹ️ Chức năng gợi ý cải thiện chủ yếu dành cho sinh viên.";
        }
        String courseName = entities.getOrDefault("course_name", "môn học");
        return "💡 Gợi ý cải thiện cho " + courseName + ":\n"
                + "• Ưu tiên nộp đủ bài tập và quiz\n"
                + "• Ôn lại phần lý thuyết nền tảng\n"
                + "• Theo dõi điểm thành phần thường xuyên\n"
                + "• Hỏi giảng viên/cố vấn nếu còn vướng nội dung\n"
                + "• Đặt mục tiêu chuyên cần ổn định để tránh rủi ro";
    }

    private String handleCheckApiStatus(String username, String role) {
        if (!"ADMIN".equals(role)) {
            return "🔒 Chức năng này chỉ dành cho quản trị viên.";
        }
        
        return "✅ Kết nối Moodle API hoạt động bình thường.\n" +
               "🔗 Base URL: http://localhost/moodle\n" +
               "⏰ Timestamp: " + LocalDateTime.now();
    }

    /**
     * Format response từ MoodleRestController thành text
     */
    private String formatResponse(ResponseEntity<?> response) {
        if (response == null || response.getBody() == null) {
            return "Không có dữ liệu";
        }
        
        Object body = response.getBody();
        
        // Nếu body là Map, format thành text dễ đọc
        if (body instanceof Map) {
            return formatMapToText((Map<?, ?>) body);
        }
        
        return body.toString();
    }

    /**
     * Format Map thành text dễ đọc với format đẹp theo yêu cầu
     */
    private String formatMapToText(Map<?, ?> map) {
        try {
            // Nếu có key "courses" - đây là response điểm/chuyên cần
            if (map.containsKey("courses")) {
                return formatCoursesResponse(map);
            }
            
            // Nếu có key "students" - đây là danh sách sinh viên nguy cơ
            if (map.containsKey("students")) {
                return formatAtRiskStudentsResponse(map);
            }
            
            // Nếu có các key thống kê
            if (map.containsKey("totalStudents") || map.containsKey("totalCourses")) {
                return formatStatisticsResponse(map);
            }
            
            // Default fallback - hiển thị bình thường
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey().toString();
                Object value = entry.getValue();
                sb.append(key).append(": ").append(value).append("\n");
            }
            return sb.toString();
            
        } catch (Exception e) {
            log.error("Error formatting map: {}", e.getMessage());
            return map.toString();
        }
    }

    /**
     * Format response courses (grades/attendance)
     */
    private String formatCoursesResponse(Map<?, ?> map) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> courses = (List<Map<String, Object>>) map.get("courses");
        
        if (courses == null || courses.isEmpty()) {
            return "Không có dữ liệu";
        }
        
        StringBuilder sb = new StringBuilder();
        Map<String, Object> firstCourse = courses.get(0);
        String studentId = (String) firstCourse.get("studentId");
        String fullName = (String) firstCourse.get("fullName");
        
        sb.append("📊 THÔNG TIN SINH VIÊN\n\n");
        sb.append("MSSV: ").append(studentId).append("\n");
        sb.append("Họ tên: ").append(fullName).append("\n");
        sb.append("Tổng số môn: ").append(courses.size()).append("\n\n");
        
        for (Map<String, Object> course : courses) {
            String courseName = (String) course.get("courseName");
            sb.append("📚 ").append(courseName).append("\n");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> grades = (List<Map<String, Object>>) course.get("grades");
            
            if (grades != null && !grades.isEmpty()) {
                for (Map<String, Object> grade : grades) {
                    String itemName = (String) grade.get("itemName");
                    Object gradeRaw = grade.get("gradeRaw");
                    Object gradeMax = grade.get("gradeMax");
                    Object percentage = grade.get("percentage");
                    
                    if (gradeRaw != null) {
                        sb.append("  • ").append(itemName).append(": ")
                          .append(String.format("%.1f", gradeRaw)).append("/")
                          .append(String.format("%.1f", gradeMax))
                          .append(" (").append(String.format("%.1f", percentage)).append("%)\n");
                    } else {
                        sb.append("  • ").append(itemName).append(": Chưa có điểm\n");
                    }
                }
            } else {
                sb.append("  • Chưa có điểm\n");
            }
            sb.append("\n");
        }
        
        return sb.toString().trim();
    }

    /**
     * Format at-risk students response
     */
    private String formatAtRiskStudentsResponse(Map<?, ?> map) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) map.get("students");
        Integer count = (Integer) map.get("count");
        String riskLevel = (String) map.get("riskLevel");
        
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ DANH SÁCH SINH VIÊN CẢNH BÁO\n\n");
        
        String icon = "🟢";
        if ("yellow".equals(riskLevel)) icon = "🟡";
        if ("red".equals(riskLevel)) icon = "🔴";
        
        sb.append("Mức: ").append(icon).append(" ").append(riskLevel != null ? riskLevel.toUpperCase() : "ALL").append("\n");
        sb.append("Số lượng: ").append(count != null ? count : students.size()).append(" sinh viên\n\n");
        
        if (students != null && !students.isEmpty()) {
            for (Map<String, Object> student : students) {
                String sid = (String) student.get("studentId");
                String name = (String) student.get("fullName");
                Object avgGrade = student.get("avgGrade");
                Object inactiveCourses = student.get("inactiveCourses");
                
                sb.append("• ").append(sid).append(" - ").append(name).append("\n");
                sb.append("  Điểm TB: ").append(String.format("%.1f", avgGrade)).append("%, ");
                sb.append("Môn không hoạt động: ").append(inactiveCourses).append("\n\n");
            }
        } else {
            sb.append("Không có sinh viên nào.\n");
        }
        
        return sb.toString();
    }

    /**
     * Format statistics response
     */
    private String formatStatisticsResponse(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("📈 THỐNG KÊ TỔNG QUAN\n\n");
        
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            
            String label = key;
            if ("totalCourses".equals(key)) label = "📚 Tổng số khóa học";
            if ("totalStudents".equals(key)) label = "👥 Tổng số sinh viên";
            if ("avgStudentsPerCourse".equals(key)) label = "📊 TB sinh viên/khóa";
            
            sb.append(label).append(": ").append(value).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * Lưu chat history vào database
     */
    private void saveChatHistory(String username, String userMessage, String botResponse, String intent) {
        try {
            // Save user message
            ChatHistory userHistory = new ChatHistory();
            userHistory.setSessionId(username);  // Dùng username làm session ID
            userHistory.setLecturerId(1L);  // TODO: Get actual user ID
            userHistory.setRole(ChatRole.USER);
            userHistory.setContent(userMessage);
            userHistory.setIntent(intent);
            userHistory.setCreatedAt(LocalDateTime.now());
            chatHistoryRepository.save(userHistory);
            
            // Save bot response
            ChatHistory botHistory = new ChatHistory();
            botHistory.setSessionId(username);
            botHistory.setLecturerId(1L);
            botHistory.setRole(ChatRole.BOT);  // Dùng BOT thay vì ASSISTANT
            botHistory.setContent(botResponse);
            botHistory.setIntent(intent);
            botHistory.setCreatedAt(LocalDateTime.now());
            chatHistoryRepository.save(botHistory);
            
            log.debug("Saved chat history for user: {}", username);
        } catch (Exception e) {
            log.error("Error saving chat history: {}", e.getMessage(), e);
        }
    }

    /**
     * Tạo ChatResponseDTO
     */
    private ChatResponseDTO createResponse(String text, String intent, double confidence, Map<String, String> entities) {
        ChatResponseDTO response = new ChatResponseDTO();
        response.setReply(text);  // Dùng setReply thay vì setMessage
        response.setIntent(intent);
        response.setTimestamp(LocalDateTime.now());
        
        return response;
    }
}
