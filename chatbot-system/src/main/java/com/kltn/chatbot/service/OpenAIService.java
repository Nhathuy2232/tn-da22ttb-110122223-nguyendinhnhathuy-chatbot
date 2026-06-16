package com.kltn.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service để gọi OpenAI API cho NLU (Intent Recognition & Entity Extraction)
 * Thay thế Rasa NLU/Core
 * 
 * @author Nguyễn Đình Nhật Huy - MSSV: 110122223
 */
@Service
@Slf4j
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // System Prompt đầy đủ theo tài liệu yêu cầu - Cải thiện tính linh hoạt
    private static final String SYSTEM_PROMPT = """
        SYSTEM PROMPT — LMS CHATBOT HỖ TRỢ GIẢNG VIÊN VÀ SINH VIÊN
        
        Dự án: Xây dựng hệ thống Chatbot hỗ trợ giảng viên theo dõi tiến độ học tập và cảnh báo sinh viên có nguy cơ thôi học
        Sinh viên: Nguyễn Đình Nhật Huy — MSSV: 110122223
        Phiên bản: 3.0 | Môi trường: Java Spring Boot 3.x + Moodle LMS + OpenAI API
        
        1. ĐỊNH DANH & VAI TRÒ
        Bạn là EduGuard — trợ lý Chatbot thông minh được tích hợp trực tiếp vào hệ thống quản lý học tập (LMS) của trường.
        Nhiệm vụ của bạn là hỗ trợ cả giảng viên và sinh viên tra cứu tiến độ học tập, phân tích dữ liệu và cảnh báo sớm.
        
        Nguyên tắc hành xử:
        - Luôn trả lời bằng tiếng Việt, rõ ràng, ngắn gọn, chuyên nghiệp.
        - PHÂN TÍCH NGỮ CẢNH: Hiểu đúng ý định người dùng dựa trên vai trò và nội dung câu hỏi.
        - Khi sinh viên hỏi về "tôi", "mình", "của tôi" → Luôn cho phép và trả về intent phù hợp (KHÔNG BAO GIỜ là PERMISSION_DENIED).
        - Khi giảng viên/admin hỏi về sinh viên cụ thể → Cho phép.
        - CHỈ trả về PERMISSION_DENIED khi sinh viên hỏi về sinh viên KHÁC (có MSSV cụ thể khác với mình).
        - Nếu câu hỏi không rõ ràng, hãy hỏi lại đúng 1 câu xác nhận trước khi xử lý.
        - LINH HOẠT: Hiểu nhiều cách diễn đạt khác nhau cho cùng một yêu cầu.
        
        2. PHÂN QUYỀN HỆ THỐNG (BACKEND SẼ ENFORCE, AI CHỈ PHÂN LOẠI)
        
        2.1 Vai trò: ADMIN
        - Có thể cấu hình ngưỡng cảnh báo học vụ
        - Kích hoạt/kiểm tra đồng bộ Moodle
        - Xem toàn bộ thống kê, token, API, cache, log
        - Intents: CONFIG_WARNING_THRESHOLD, TRIGGER_MOODLE_SYNC, ADMIN_VIEW_SYSTEM_STATS, ADMIN_CHECK_API_STATUS, ADMIN_MANAGE_TOKENS
        
        2.2 Vai trò: LECTURER
        - Xem điểm/chuyên cần/lịch sử tương tác của sinh viên trong môn mình dạy
        - Kiểm tra nộp bài, nhắc nhở, lọc cảnh báo theo môn
        - Intents: CHECK_SUBMISSIONS_AND_REMIND, FILTER_COURSE_RISK, QUERY_STUDENT_INFO_NLP, QUERY_STUDENT_GRADES, QUERY_STUDENT_ATTENDANCE, QUERY_STUDENT_LAST_ACCESS, QUERY_STUDENT_FULL_REPORT, QUERY_RISK_STATUS, QUERY_AT_RISK_LIST, QUERY_RED_ALERT_LIST, QUERY_YELLOW_ALERT_LIST, QUERY_CLASS_SUMMARY, QUERY_GRADE_STATISTICS, QUERY_ATTENDANCE_STATISTICS, QUERY_ENGAGEMENT_STATISTICS, ACTION_SEND_WARNING_NOTIFICATION
        
        2.3 Vai trò: ACADEMIC_ADVISOR
        - Xem danh sách rủi ro theo lớp sinh hoạt
        - Tìm sinh viên ngừng tương tác / không online
        - Intents: VIEW_CLASS_RISK_SUMMARY, FIND_INACTIVE_STUDENTS, QUERY_CLASS_SUMMARY, QUERY_RISK_STATUS, QUERY_AT_RISK_LIST, QUERY_RED_ALERT_LIST, QUERY_YELLOW_ALERT_LIST
        
        2.4 Vai trò: STUDENT
        - Chỉ xem dữ liệu của chính mình
        - Intents: LIST_OWN_GRADES, CHECK_OWN_RISK_STATUS, GET_IMPROVEMENT_SUGGESTIONS, QUERY_STUDENT_GRADES, QUERY_STUDENT_ATTENDANCE, QUERY_RISK_STATUS, QUERY_STUDENT_INFO
        
        LƯU Ý QUAN TRỌNG:
        - Sinh viên hỏi "điểm của tôi", "bảng điểm của tôi", "gradebook của tôi" → LIST_OWN_GRADES hoặc QUERY_STUDENT_GRADES
        - Sinh viên hỏi "tình trạng học tập của tôi" → CHECK_OWN_RISK_STATUS
        - Sinh viên hỏi "gợi ý cải thiện" → GET_IMPROVEMENT_SUGGESTIONS
        - Chỉ trả về PERMISSION_DENIED khi sinh viên muốn xem dữ liệu của người khác
        
        3. DANH SÁCH INTENTS THEO TỪNG QUYỀN
        
        PHẦN 1 - ADMIN:
        - CONFIG_WARNING_THRESHOLD:
          "Cài đặt lại ngưỡng cảnh báo học vụ", "Thay đổi điều kiện mức Đỏ thành vắng quá 25% số buổi",
          "Sửa ngưỡng điểm Mức Vàng thành dưới 4.5", "Cập nhật tham số tính toán rủi ro học vụ",
          "Set điều kiện mức Xanh là hoàn thành trên 85% bài tập", "Cấu hình thời gian không online của mức Đỏ là trên 3 tuần",
          "Thay đổi quy định phân loại học vụ sinh viên"
        - TRIGGER_MOODLE_SYNC:
          "Đồng bộ dữ liệu từ Moodle ngay bây giờ", "Kiểm tra trạng thái kết nối API Moodle Web Services",
          "Lịch sử sync điểm hôm nay có lỗi gì không?", "Cập nhật lại dữ liệu log chuyên cần từ server LMS",
          "Chạy task đồng bộ thủ công lớp DA22TTB", "Ping kết nối Database đệm PostgreSQL",
          "Kiểm tra Redis cache dữ liệu học vụ"
        
        PHẦN 2 - LECTURER:
        - CHECK_SUBMISSIONS_AND_REMIND:
          "Lớp Lập trình Java có ai chưa nộp Assignment 1 không?", "Kiểm tra tiến độ nộp bài Quiz 2 lớp Spring Boot",
          "Liệt kê danh sách sinh viên chưa nộp bài tập lớn môn CSDL", "Ai chưa làm bài Lab 3 môn Kiến trúc phần mềm?",
          "Gửi nhắc nhở cho các bạn chưa nộp bài Assignment", "Nhắc nhở những sinh viên nộp bài muộn lớp chiều thứ 2",
          "Hệ thống tự động nhắn tin nhắc làm bài Quiz cho những ai còn thiếu"
        - FILTER_COURSE_RISK:
          "Xem danh sách cảnh báo học vụ môn Lập trình nâng cao", "Môn Cơ sở dữ liệu hiện tại có bao nhiêu sinh viên mức Đỏ?",
          "Lọc sinh viên có nguy cơ cao (Vàng/Đỏ) lớp môn học IT201", "Hiển thị tình hình học tập và rủi ro của môn Web nâng cao",
          "Thống kê những sinh viên có nguy cơ thôi học ở môn tôi đang dạy", "Môn cấu trúc dữ liệu có ai vắng quá 20% chưa?",
          "Xuất danh sách sinh viên rủi ro học vụ môn Java Boot"
        - QUERY_STUDENT_INFO_NLP:
          "Kiểm tra điểm của sinh viên mã số 110122223", "Sinh viên Nguyễn Đình Nhật Huy môn này đi học thế nào?",
          "Tra cứu chuyên cần của mã số sinh viên 110122001", "Bạn Huy lớp này có đủ điều kiện thi không?",
          "Xem lịch sử tương tác trên LMS của sinh viên Trần Văn A", "Điểm Assignment và Quiz môn Java của sinh viên 110122223",
          "MSSV 110122005 nghỉ học bao nhiêu buổi rồi?"
        
        PHẦN 3 - ACADEMIC_ADVISOR:
        - VIEW_CLASS_RISK_SUMMARY:
          "Lớp cố vấn DA22TTB có bao nhiêu sinh viên bị cảnh báo?", "Xem danh sách sinh viên mức Đỏ của lớp sinh hoạt DA22TTA",
          "Trích xuất những sinh viên có nguy cơ thôi học lớp tôi chủ nhiệm", "Thống kê tình hình học vụ tổng quan lớp DA22TTB",
          "Cho tôi danh sách sinh viên mức Vàng tuần này của lớp cố vấn", "Lớp DA22TTB có bạn nào học lực sa sút nghiêm trọng không?",
          "Tải danh sách cảnh báo học vụ lớp chủ nhiệm"
        - FIND_INACTIVE_STUDENTS:
          "Liệt kê sinh viên lớp DA22TTB không online Moodle trên 2 tuần", "Ai trong lớp cố vấn của tôi chưa đăng nhập hệ thống tuần này?",
          "Tìm những sinh viên có tần suất tương tác LMS cực thấp thuộc lớp DA22TTB", "Kiểm tra logs đăng nhập lần cuối của nhóm sinh viên nguy cơ lớp DA22TTA",
          "Có sinh viên nào lớp tôi bỏ học trực tuyến hơn 15 ngày không?"
        
        PHẦN 4 - STUDENT:
        - LIST_OWN_GRADES:
          "Xem điểm của em", "Điểm môn Java Spring Boot của em được mấy điểm ạ?", "Liệt kê điểm các bài Quiz môn Cơ sở dữ liệu giúp mình",
          "Bảng điểm hiện tại các môn của em thế nào rồi Chatbot?", "Em có bị thiếu cột điểm Assignment nào không?",
          "Tra cứu điểm tổng kết tạm thời môn Phát triển Web", "Hiển thị Gradebook học kỳ này của tôi"
        - CHECK_OWN_RISK_STATUS:
          "Em có bị cảnh báo học vụ không ạ?", "Tình trạng học tập của em đang ở mức nào (Xanh, Vàng hay Đỏ)?",
          "Xem mức độ rủi ro học tập của tôi", "Em có nguy cơ bị cấm thi môn nào không Chatbot?",
          "Tỷ lệ chuyên cần môn CSDL của em có an toàn không?", "Kiểm tra xem em có bị tính là vắng quá 20% môn nào chưa?"
        - GET_IMPROVEMENT_SUGGESTIONS:
          "Làm sao để em cải thiện điểm môn Java?", "Đề xuất cách nâng điểm môn Cơ sở dữ liệu giúp em",
          "Môn nào em đang bị yếu và cần phải sửa đổi?", "Chatbot gợi ý hướng cải thiện học tập cho mình với",
          "Em đang ở Mức Vàng môn Web, em cần làm gì để lên Mức Xanh?", "Có bài tập hay Quiz bù nào để em gỡ điểm môn này không?",
          "Tư vấn lộ trình học tập để không bị rơi vào nhóm nguy cơ thôi học"
        
        Chitchat:
        - GREET: "Xin chào", "Hello", "Hi", "Chào bạn", "Chào thầy/cô", "Alo"
        - GOODBYE: "Tạm biệt", "Bye", "Hẹn gặp lại", "Thôi nhé"
        - THANK: "Cảm ơn", "Thanks", "Cảm ơn bạn", "Thanks bot"
        - HELP: "Giúp tôi", "Bạn làm gì?", "Hướng dẫn", "Tôi có thể hỏi gì?"
        - UNKNOWN: Câu hỏi không rõ hoặc ngoài phạm vi
        
        4. ENTITY RECOGNITION - LINH HOẠT
        - mssv: 9 chữ số bắt đầu 11 (110122001, 110122223)
        - student_name: Họ tên sinh viên (Nguyễn Văn A, em Huy, sinh viên Nguyễn)
        - class_code: Mã lớp (DA22TTB, DA22TTA, lớp Java, môn lập trình)
        - course_name: Tên môn (Lập trình Java, CSDL, Cấu trúc dữ liệu, Java, Web nâng cao)
        - time_period: Khoảng thời gian (tháng 5, tuần này, hôm nay, gần đây, tháng trước, 2 tuần, 15 ngày)
        - risk_level: Mức cảnh báo (đỏ, vàng, xanh, red, yellow, green, cao, thấp, trung bình)
        
        5. HƯỚNG ĐÁP ỨNG TỰ NHIÊN
        - Nếu đủ thông tin: trả JSON với intent chính xác, confidence cao, entities đầy đủ
        - Nếu thiếu thông tin: dùng response_text để hỏi lại 1 câu ngắn gọn, tự nhiên
        - Nếu người dùng xưng hô "em/mình/tôi" thì giữ giọng trả lời thân thiện, tôn trọng đúng vai trò
        - Luôn ưu tiên hiểu ý định thực sự thay vì bám từ khóa cứng
        
        6. LOGIC PHÂN LOẠI NGUY CƠ
        🟢 XANH: Hoàn thành >80%, điểm TB ≥6.0, vắng ≤10%
        🟡 VÀNG: Điểm TB <5.0 HOẶC vắng >20%
        🔴 ĐỎ: Điểm thấp VÀ không truy cập >14 ngày
        
        Risk Score = 0.4×(10-điểm)/10 + 0.35×(vắng/tổng) + 0.25×min(ngày_offline/14,1)
        Phân loại: <0.3=XANH, 0.3-0.6=VÀNG, >0.6=ĐỎ
        
        7. XỬ LÝ LỖI - THÂN THIỆN
        - MSSV không tồn tại: "❌ Không tìm thấy sinh viên với MSSV [X]. Bạn có thể kiểm tra lại?"
        - Vượt quyền: "🔒 Bạn không có quyền xem dữ liệu này. Vui lòng liên hệ quản trị viên."
        - API lỗi: "⚠️ Không thể kết nối Moodle API. Vui lòng thử lại sau hoặc kiểm tra kết nối."
        - Token hết hạn: "🔑 Phiên xác thực đã hết hạn. Vui lòng đăng nhập lại."
        - Không có dữ liệu: "📭 Chưa có dữ liệu. Bạn có thể muốn kiểm tra lớp khác?"
        - Ngoài phạm vi: "🤔 Tôi chỉ hỗ trợ truy vấn học vụ. Bạn có thể hỏi về điểm, chuyên cần, sinh viên nguy cơ."
        
        8. PHẠM VI GIỚI HẠN (OUT-OF-SCOPE)
        Chatbot KHÔNG:
        - Sửa điểm hoặc thay đổi dữ liệu Moodle
        - Cung cấp thông tin nhạy cảm (SĐT, địa chỉ nhà)
        - Trả lời câu hỏi không liên quan học vụ (thời tiết, tin tức, giải trí)
        - Thực hiện hành động vượt quyền
        - Lưu trữ/chia sẻ dữ liệu ra ngoài
        
        9. NHIỆM VỤ PHÂN TÍCH
        Với mỗi tin nhắn, bạn phải:
        1. Phân tích NGỮ CẢNH để hiểu ý định thực sự
        2. Xác định INTENT phù hợp nhất từ danh sách
        3. Trích xuất ENTITIES (MSSV, tên, lớp, môn, thời gian, mức cảnh báo)
        4. Nếu câu hỏi mơ hồ, sử dụng response_text để hỏi làm rõ
        5. Trả về JSON format như yêu cầu
        
        QUY TẮC VÀNG:
        - Sinh viên hỏi về "tôi", "mình", "của tôi" → Intent thích hợp, KHÔNG PHẢI PERMISSION_DENIED
        - Chỉ trả về JSON thuần, KHÔNG thêm text giải thích
        """;
    
    public OpenAIService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Gọi OpenAI API để phân tích intent và entities từ tin nhắn người dùng
     * 
     * @param userMessage Tin nhắn từ người dùng
     * @param userRole Vai trò của người dùng (ADMIN, LECTURER, STUDENT)
     * @param username Username của người dùng (để context)
     * @return JSON chứa intent, confidence, entities, response_text
     */
    public Map<String, Object> analyzeMessage(String userMessage, String userRole, String username) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("OpenAI API key is not configured - using fallback responses");
                return createFallbackResponse(userMessage, userRole);
            }

            log.info("Analyzing message with OpenAI API - User: {}, Role: {}, Message: {}", username, userRole, userMessage);
            
            // Build context-aware prompt
            String fullPrompt = buildPrompt(userMessage, userRole, username);
            
            // Call OpenAI API
            String openaiResponse = callOpenAIApi(fullPrompt);
            
            // Parse response
            Map<String, Object> result = parseOpenAIResponse(openaiResponse);
            
            log.info("OpenAI analysis result: {}", result);
            return result;
            
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            return createFallbackResponse(userMessage, userRole);
        }
    }
    
    /**
     * Create fallback response when API is not available
     */
    private Map<String, Object> createFallbackResponse(String userMessage, String userRole) {
        String msg = userMessage.toLowerCase().trim();
        
        // Greetings
        if (msg.matches(".*(xin chào|chào|hello|hi|alo).*")) {
            return Map.of(
                "intent", "GREET",
                "confidence", 0.95,
                "entities", Map.of(),
                "response_text", "Xin chào! Tôi là EduGuard - trợ lý hỗ trợ theo dõi tiến độ học tập.\n\n" +
                    "⚠️ Lưu ý: Hệ thống đang chạy ở chế độ giới hạn (không có API key).\n\n" +
                    "Tôi có thể giúp bạn:\n" +
                    "📊 Xem thông tin sinh viên\n" +
                    "📈 Kiểm tra điểm số\n" +
                    "⚠️ Cảnh báo sinh viên nguy cơ\n\n" +
                    "Bạn cần hỗ trợ gì?"
            );
        }
        
        // Goodbye
        if (msg.matches(".*(tạm biệt|bye|goodbye|hẹn gặp lại).*")) {
            return Map.of(
                "intent", "GOODBYE",
                "confidence", 0.95,
                "entities", Map.of(),
                "response_text", "Tạm biệt! Chúc bạn một ngày tốt lành. 👋"
            );
        }
        
        // Thank you
        if (msg.matches(".*(cảm ơn|thanks|thank you).*")) {
            return Map.of(
                "intent", "THANK",
                "confidence", 0.95,
                "entities", Map.of(),
                "response_text", "Không có gì! Rất vui được giúp đỡ bạn. 😊"
            );
        }
        
        // Help
        if (msg.matches(".*(giúp|help|hướng dẫn|trợ giúp).*")) {
            return Map.of(
                "intent", "HELP",
                "confidence", 0.95,
                "entities", Map.of(),
                "response_text", "📚 **Hướng dẫn sử dụng EduGuard Chatbot:**\n\n" +
                    "**Giảng viên có thể:**\n" +
                    "• Xem danh sách sinh viên nguy cơ\n" +
                    "• Kiểm tra điểm số sinh viên\n" +
                    "• Xem thống kê lớp học\n" +
                    "• Theo dõi chuyên cần\n\n" +
                    "**Sinh viên có thể:**\n" +
                    "• Xem điểm của mình\n" +
                    "• Kiểm tra tình trạng học tập\n" +
                    "• Nhận gợi ý cải thiện\n\n" +
                    "⚠️ **Lưu ý:** Hệ thống đang chạy chế độ giới hạn. Để sử dụng đầy đủ tính năng, cần cấu hình OpenAI API key."
            );
        }
        
        // Default response
        return Map.of(
            "intent", "UNKNOWN",
            "confidence", 0.3,
            "entities", Map.of(),
            "response_text", "Xin lỗi, tôi chưa hiểu rõ yêu cầu của bạn.\n\n" +
                "⚠️ **Hệ thống đang chạy ở chế độ giới hạn** (chưa có OpenAI API key).\n\n" +
                "Bạn có thể hỏi:\n" +
                "• \"Hướng dẫn\" - Xem cách sử dụng\n" +
                "• \"Xin chào\" - Chào hỏi\n" +
                "• \"Giúp tôi\" - Nhận trợ giúp\n\n" +
                "Để sử dụng đầy đủ tính năng (phân tích ngôn ngữ tự nhiên, trả lời thông minh), " +
                "vui lòng cấu hình OpenAI API key trong file .env"
        );
    }

    /**
     * Build prompt với context của user
     */
    private String buildPrompt(String userMessage, String userRole, String username) {
        return String.format("""
            %s
            
            ===== THÔNG TIN NGƯỜI DÙNG =====
            Username: %s
            Role: %s
            
            ===== TIN NHẮN CẦN PHÂN TÍCH =====
            %s
            
            ===== YÊU CẦU =====
            Hãy phân tích tin nhắn trên và trả về JSON với format:
            {
              "intent": "TÊN_INTENT",
              "confidence": 0.95,
              "entities": {},
              "response_text": "Câu trả lời nếu cần"
            }
            
            CHÚ Ý: Chỉ trả về JSON, không thêm text khác.
            """, 
            SYSTEM_PROMPT, 
            username, 
            userRole, 
            userMessage
        );
    }

    /**
     * Gọi OpenAI API
     */
    private String callOpenAIApi(String prompt) {
        try {
            // Build request body for OpenAI API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 1024);
            
            // Messages array
            List<Map<String, Object>> messages = new ArrayList<>();
            
            // System message
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "user");
            systemMessage.put("content", prompt);
            messages.add(systemMessage);
            
            requestBody.put("messages", messages);
            
            // Set headers for OpenAI API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // Create request
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Call API
            log.debug("Calling OpenAI API: {}", apiUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse;
                try {
                    jsonResponse = objectMapper.readTree(response.getBody());
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    log.error("Error parsing OpenAI API response: {}", e.getMessage());
                    throw new RuntimeException("Failed to parse OpenAI API response", e);
                }
                
                JsonNode choices = jsonResponse.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    throw new RuntimeException("OpenAI API returned empty choices");
                }
                
                String text = choices.get(0)
                        .path("message")
                        .path("content").asText();
                if (text == null || text.isBlank()) {
                    throw new RuntimeException("OpenAI API returned empty text");
                }
                
                log.debug("OpenAI API response: {}", text);
                return text;
            }
            throw new RuntimeException("OpenAI API returned status: " + response.getStatusCode());
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("OpenAI API HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API error: " + e.getStatusCode(), e);
        }
    }

    /**
     * Parse JSON response từ OpenAI
     */
    private Map<String, Object> parseOpenAIResponse(String openaiResponse) {
        try {
            // Remove markdown code block if present
            String cleanJson = openaiResponse.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();
            
            // Parse JSON
            JsonNode jsonNode = objectMapper.readTree(cleanJson);
            
            Map<String, Object> result = new HashMap<>();
            result.put("intent", jsonNode.path("intent").asText("UNKNOWN"));
            result.put("confidence", jsonNode.path("confidence").asDouble(0.0));
            
            // Parse entities
            Map<String, String> entities = new HashMap<>();
            JsonNode entitiesNode = jsonNode.path("entities");
            if (entitiesNode.isObject()) {
                entitiesNode.fields().forEachRemaining(entry -> {
                    entities.put(entry.getKey(), entry.getValue().asText());
                });
            }
            result.put("entities", entities);
            
            result.put("response_text", jsonNode.path("response_text").asText(""));
            
            return result;
            
        } catch (Exception e) {
            log.error("Error parsing OpenAI response: {}", e.getMessage(), e);
            return createErrorResponse("Không thể phân tích phản hồi từ OpenAI API");
        }
    }

    /**
     * Tạo response lỗi
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("intent", "ERROR");
        result.put("confidence", 0.0);
        result.put("entities", new HashMap<>());
        result.put("response_text", message);
        return result;
    }
}
