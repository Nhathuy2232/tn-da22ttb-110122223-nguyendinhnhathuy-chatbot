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
 *
 * @author Nguyễn Đình Nhật Huy - MSSV: 110122223
 */
@Service
@Slf4j
public class OpenAIService {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAIService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * System prompt với danh sách intent đầy đủ theo 4 quyền.
     * Tối ưu cho việc phân loại intent tiếng Việt tự nhiên.
     */
    private static final String SYSTEM_PROMPT = """
        Bạn là EduGuard - trợ lý AI phân loại ý định (intent) và trích xuất thực thể (entity) cho hệ thống LMS Moodle.

        ## QUY TẮC VÀNG
        1. Luôn trả về JSON thuần, KHÔNG giải thích thêm
        2. Hiểu NGỮ CẢNH và ý định thực sự, không chỉ khớp từ khóa
        3. Nếu sinh viên dùng "tôi/mình/em" → đó là intent về bản thân, KHÔNG PHẢI PERMISSION_DENIED
        4. CHỈ trả PERMISSION_DENIED khi sinh viên muốn xem dữ liệu NGƯỜI KHÁC

        ## 4 VAI TRÒ & INTENT

        ### 1. ADMIN (Quản trị viên)
        - CONFIG_WARNING_THRESHOLD: cấu hình ngưỡng cảnh báo
        - TRIGGER_MOODLE_SYNC: đồng bộ dữ liệu
        - ADMIN_CHECK_API_STATUS: kiểm tra API
        - ADMIN_VIEW_SYSTEM_STATS: thống kê hệ thống

        ### 2. LECTURER (Giảng viên)
        - CHECK_SUBMISSIONS_AND_REMIND: kiểm tra nộp bài, nhắc nhở
        - FILTER_COURSE_RISK: lọc cảnh báo theo môn
        - QUERY_STUDENT_INFO_NLP: tra cứu sinh viên bằng ngôn ngữ tự nhiên (có MSSV hoặc tên)
        - QUERY_AT_RISK_LIST: danh sách nguy cơ chung

        ### 3. ADVISER (Cố vấn học tập)
        - VIEW_CLASS_RISK_SUMMARY: xem rủi ro theo lớp chủ nhiệm
        - FIND_INACTIVE_STUDENTS: tìm sinh viên ngừng tương tác

        ### 4. STUDENT (Sinh viên)
        - LIST_OWN_GRADES: xem điểm của mình
        - CHECK_OWN_RISK_STATUS: kiểm tra mức cảnh báo của mình
        - GET_IMPROVEMENT_SUGGESTIONS: đề xuất cải thiện

        ### Chitchat
        - GREET, GOODBYE, THANK, HELP, UNKNOWN

        ## ENTITY
        - mssv: 9 chữ số bắt đầu 11 (110122001)
        - class_code: mã lớp (DA22TTB)
        - course_name: tên môn học
        - risk_level: red/yellow/green
        - inactive_days: số ngày không online

        ## OUTPUT FORMAT (CHỈ TRẢ JSON)
        {
          "intent": "TÊN_INTENT",
          "confidence": 0.95,
          "entities": {"mssv": "110122001", "course_name": "Java"},
          "response_text": ""
        }
        """;

    /**
     * Phân tích tin nhắn người dùng.
     */
    public Map<String, Object> analyzeMessage(String userMessage, String userRole, String username) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("OpenAI API key is not configured - using fallback");
                return createFallbackResponse(userMessage, userRole);
            }

            String fullPrompt = buildPrompt(userMessage, userRole, username);
            String openaiResponse = callOpenAIApi(fullPrompt);
            return parseOpenAIResponse(openaiResponse);
        } catch (Exception e) {
            log.error("OpenAI API error: {}", e.getMessage(), e);
            return createFallbackResponse(userMessage, userRole);
        }
    }

    /**
     * Fallback khi không có OpenAI - ưu tiên dùng LocalIntentMatcher.
     */
    private Map<String, Object> createFallbackResponse(String userMessage, String userRole) {
        return LocalIntentMatcher.matchForRole(userMessage, userRole)
                .orElseGet(() -> {
                    Map<String, Object> unknown = new HashMap<>();
                    unknown.put("intent", "UNKNOWN");
                    unknown.put("confidence", 0.3);
                    unknown.put("entities", new HashMap<>());
                    unknown.put("response_text", "Xin lỗi, tôi chưa hiểu rõ yêu cầu.");
                    return unknown;
                });
    }

    private String buildPrompt(String userMessage, String userRole, String username) {
        return SYSTEM_PROMPT + "\n\nUser: " + username + " | Role: " + userRole + "\nMessage: " + userMessage;
    }

    private String callOpenAIApi(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 500);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", SYSTEM_PROMPT);
        messages.add(sysMsg);

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            try {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode choices = jsonResponse.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    return choices.get(0).path("message").path("content").asText();
                }
            } catch (Exception e) {
                log.error("Error parsing OpenAI response: {}", e.getMessage());
            }
        }
        throw new RuntimeException("OpenAI API call failed");
    }

    private Map<String, Object> parseOpenAIResponse(String openaiResponse) {
        try {
            String cleanJson = openaiResponse.trim();
            if (cleanJson.startsWith("```json")) cleanJson = cleanJson.substring(7);
            if (cleanJson.startsWith("```")) cleanJson = cleanJson.substring(3);
            if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            cleanJson = cleanJson.trim();

            JsonNode jsonNode = objectMapper.readTree(cleanJson);

            Map<String, Object> result = new HashMap<>();
            result.put("intent", jsonNode.path("intent").asText("UNKNOWN"));
            result.put("confidence", jsonNode.path("confidence").asDouble(0.0));

            Map<String, String> entities = new HashMap<>();
            JsonNode entitiesNode = jsonNode.path("entities");
            if (entitiesNode.isObject()) {
                entitiesNode.fields().forEachRemaining(e -> entities.put(e.getKey(), e.getValue().asText()));
            }
            result.put("entities", entities);
            result.put("response_text", jsonNode.path("response_text").asText(""));
            return result;
        } catch (Exception e) {
            log.error("Error parsing OpenAI response: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("intent", "ERROR");
            error.put("confidence", 0.0);
            error.put("entities", new HashMap<>());
            error.put("response_text", "Lỗi xử lý phản hồi");
            return error;
        }
    }
}
