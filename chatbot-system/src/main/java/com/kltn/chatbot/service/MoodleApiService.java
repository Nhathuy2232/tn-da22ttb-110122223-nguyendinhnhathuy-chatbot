package com.kltn.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kltn.chatbot.config.MoodleApiConfig;
import com.kltn.chatbot.exception.MoodleApiException;
import com.kltn.chatbot.model.dto.MoodleGradeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service kết nối và tương tác với Moodle Web Services API
 * 
 * Tuân thủ quy ước:
 * - Moodle API chỉ được gọi qua service này
 * - Không gọi trực tiếp từ Controller hoặc Service khác
 * - Implement retry logic với exponential backoff
 * - Cache data vào PostgreSQL để giảm API calls
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoodleApiService {

    private final MoodleApiConfig moodleConfig;
    private final ObjectMapper objectMapper;

    /**
     * Gọi Moodle Web Service function
     * 
     * @param wsfunction Tên function (vd: core_course_get_courses)
     * @param params Parameters cho function
     * @return JSON response từ Moodle
     */
    public JsonNode callMoodleApi(String wsfunction, List<NameValuePair> params) {
        int retries = 0;
        Exception lastException = null;

        while (retries < moodleConfig.getMaxRetries()) {
            try {
                return executeApiCall(wsfunction, params);
            } catch (Exception e) {
                lastException = e;
                retries++;
                log.warn("Moodle API call failed (attempt {}/{}): {}", 
                        retries, moodleConfig.getMaxRetries(), e.getMessage());
                
                if (retries < moodleConfig.getMaxRetries()) {
                    try {
                        // Exponential backoff
                        long delay = moodleConfig.getRetryDelay() * (long) Math.pow(2, retries - 1);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new MoodleApiException("Retry interrupted", ie);
                    }
                }
            }
        }

        throw new MoodleApiException(
                "Moodle API call failed after " + moodleConfig.getMaxRetries() + " retries", 
                lastException);
    }

    /**
     * Execute API call (internal method)
     */
    private JsonNode executeApiCall(String wsfunction, List<NameValuePair> params) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(moodleConfig.getWebServiceUrl());

            // Build parameters
            List<NameValuePair> allParams = new ArrayList<>();
            allParams.add(new BasicNameValuePair("wstoken", moodleConfig.getToken()));
            allParams.add(new BasicNameValuePair("wsfunction", wsfunction));
            allParams.add(new BasicNameValuePair("moodlewsrestformat", "json"));
            
            if (params != null) {
                allParams.addAll(params);
            }

            httpPost.setEntity(new UrlEncodedFormEntity(allParams, StandardCharsets.UTF_8));

            log.debug("Calling Moodle API: {} with {} params", wsfunction, allParams.size());

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                log.debug("Moodle API response: {}", responseBody);

                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                // Check for Moodle error response
                if (jsonResponse.has("exception")) {
                    String errorMessage = jsonResponse.get("message").asText();
                    throw new MoodleApiException("Moodle API error: " + errorMessage);
                }

                return jsonResponse;
            }
        }
    }

    /**
     * Lấy danh sách tất cả khóa học
     * Function: core_course_get_courses
     */
    public JsonNode getAllCourses() {
        log.info("Fetching all courses from Moodle");
        return callMoodleApi("core_course_get_courses", null);
    }

    /**
     * Lấy danh sách sinh viên enrolled trong khóa học
     * Function: core_enrol_get_enrolled_users
     * 
     * @param courseId ID của khóa học
     */
    public JsonNode getEnrolledUsers(Long courseId) {
        log.info("Fetching enrolled users for course: {}", courseId);
        
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("courseid", courseId.toString()));
        
        return callMoodleApi("core_enrol_get_enrolled_users", params);
    }

    /**
     * Lấy thông tin user theo ID
     * Function: core_user_get_users_by_field
     * 
     * @param userId ID của user
     */
    public JsonNode getUserById(Long userId) {
        log.info("Fetching user info for userId: {}", userId);
        
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("field", "id"));
        params.add(new BasicNameValuePair("values[0]", userId.toString()));
        
        return callMoodleApi("core_user_get_users_by_field", params);
    }

    /**
     * Lấy điểm của sinh viên trong khóa học
     * Function: gradereport_user_get_grade_items
     * 
     * @param courseId ID của khóa học
     * @param userId ID của sinh viên
     */
    public JsonNode getGradeItems(Long courseId, Long userId) {
        log.info("Fetching grades for userId: {} in course: {}", userId, courseId);
        
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("courseid", courseId.toString()));
        params.add(new BasicNameValuePair("userid", userId.toString()));
        
        return callMoodleApi("gradereport_user_get_grade_items", params);
    }

    /**
     * Lấy completion status của sinh viên trong khóa học
     * Function: core_completion_get_activities_completion_status
     * 
     * @param courseId ID của khóa học
     * @param userId ID của sinh viên
     */
    public JsonNode getActivitiesCompletionStatus(Long courseId, Long userId) {
        log.info("Fetching completion status for userId: {} in course: {}", userId, courseId);
        
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("courseid", courseId.toString()));
        params.add(new BasicNameValuePair("userid", userId.toString()));
        
        return callMoodleApi("core_completion_get_activities_completion_status", params);
    }

    /**
     * Lấy thông tin site (test connection)
     * Function: core_webservice_get_site_info
     */
    public JsonNode getSiteInfo() {
        log.info("Fetching Moodle site info");
        return callMoodleApi("core_webservice_get_site_info", null);
    }

    /**
     * Parse grade items từ JSON response
     */
    public List<MoodleGradeDTO> parseGradeItems(JsonNode response) {
        List<MoodleGradeDTO> grades = new ArrayList<>();
        
        if (response.has("usergrades") && response.get("usergrades").isArray()) {
            JsonNode userGrades = response.get("usergrades").get(0);
            if (userGrades != null && userGrades.has("gradeitems")) {
                JsonNode gradeItems = userGrades.get("gradeitems");
                for (JsonNode item : gradeItems) {
                    MoodleGradeDTO grade = MoodleGradeDTO.builder()
                            .itemName(item.get("itemname").asText())
                            .itemType(item.has("itemtype") ? item.get("itemtype").asText() : null)
                            .gradeRaw(item.has("graderaw") ? item.get("graderaw").asDouble() : null)
                            .gradeFormatted(item.has("gradeformatted") ? item.get("gradeformatted").asText() : null)
                            .gradeMax(item.has("grademax") ? item.get("grademax").asDouble() : null)
                            .gradeMin(item.has("grademin") ? item.get("grademin").asDouble() : null)
                            .build();
                    grades.add(grade);
                }
            }
        }
        
        return grades;
    }

    /**
     * Tính điểm trung bình từ grade items
     */
    public Double calculateAverageGrade(List<MoodleGradeDTO> grades) {
        if (grades == null || grades.isEmpty()) {
            return 0.0;
        }

        double totalGrade = 0.0;
        int count = 0;

        for (MoodleGradeDTO grade : grades) {
            if (grade.getGradeRaw() != null && grade.getGradeMax() != null && grade.getGradeMax() > 0) {
                // Normalize to 10-point scale
                double normalizedGrade = (grade.getGradeRaw() / grade.getGradeMax()) * 10.0;
                totalGrade += normalizedGrade;
                count++;
            }
        }

        return count > 0 ? totalGrade / count : 0.0;
    }
    
    /**
     * Lấy tất cả users từ Moodle
     * Function: core_user_get_users
     */
    public List<java.util.Map<String, Object>> getAllUsers() {
        log.info("Fetching all users from Moodle");
        
        List<java.util.Map<String, Object>> users = new ArrayList<>();
        
        try {
            // Lấy thông tin site để có user list
            JsonNode siteInfo = getSiteInfo();
            
            // Thực tế cần dùng core_user_get_users với criteria
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("criteria[0][key]", "id"));
            params.add(new BasicNameValuePair("criteria[0][value]", "%"));
            
            JsonNode response = callMoodleApi("core_user_get_users", params);
            
            if (response.has("users") && response.get("users").isArray()) {
                for (JsonNode userNode : response.get("users")) {
                    java.util.Map<String, Object> user = objectMapper.convertValue(userNode, java.util.Map.class);
                    users.add(user);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching all users: {}", e.getMessage());
        }
        
        return users;
    }
    
    /**
     * Lấy grade items của một khóa học
     * Function: core_course_get_contents
     */
    public List<java.util.Map<String, Object>> getGradeItems(Long courseId) {
        log.info("Fetching grade items for course: {}", courseId);
        
        List<java.util.Map<String, Object>> items = new ArrayList<>();
        
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("courseid", courseId.toString()));
            
            JsonNode response = callMoodleApi("core_grade_get_grade_items", params);
            
            if (response.has("items") && response.get("items").isArray()) {
                for (JsonNode itemNode : response.get("items")) {
                    java.util.Map<String, Object> item = objectMapper.convertValue(itemNode, java.util.Map.class);
                    items.add(item);
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching grade items, trying alternative method: {}", e.getMessage());
            
            // Fallback: lấy qua core_course_get_contents
            try {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("courseid", courseId.toString()));
                
                JsonNode response = callMoodleApi("core_course_get_contents", params);
                
                if (response.isArray()) {
                    for (JsonNode section : response) {
                        if (section.has("modules")) {
                            for (JsonNode module : section.get("modules")) {
                                String modname = module.get("modname").asText();
                                if ("assign".equals(modname) || "quiz".equals(modname)) {
                                    java.util.Map<String, Object> item = objectMapper.convertValue(module, java.util.Map.class);
                                    items.add(item);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e2) {
                log.error("Fallback also failed: {}", e2.getMessage());
            }
        }
        
        return items;
    }
    
    /**
     * Lấy điểm của sinh viên
     * Function: gradereport_user_get_grade_items
     */
    public List<java.util.Map<String, Object>> getStudentGrades(Long userId, Long courseId) {
        log.info("Fetching grades for user {} in course {}", userId, courseId);
        
        List<java.util.Map<String, Object>> grades = new ArrayList<>();
        
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("courseid", courseId.toString()));
            params.add(new BasicNameValuePair("userid", userId.toString()));
            
            JsonNode response = callMoodleApi("gradereport_user_get_grade_items", params);
            
            if (response.has("usergrades") && response.get("usergrades").isArray()) {
                JsonNode userGrades = response.get("usergrades").get(0);
                if (userGrades != null && userGrades.has("gradeitems")) {
                    for (JsonNode gradeNode : userGrades.get("gradeitems")) {
                        java.util.Map<String, Object> grade = objectMapper.convertValue(gradeNode, java.util.Map.class);
                        grades.add(grade);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching student grades: {}", e.getMessage());
        }
        
        return grades;
    }
    
    /**
     * Lấy assignments của khóa học
     * Function: mod_assign_get_assignments
     */
    public List<java.util.Map<String, Object>> getAssignments(Long courseId) {
        log.info("Fetching assignments for course: {}", courseId);
        
        List<java.util.Map<String, Object>> assignments = new ArrayList<>();
        
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("courseids[0]", courseId.toString()));
            
            JsonNode response = callMoodleApi("mod_assign_get_assignments", params);
            
            if (response.has("courses") && response.get("courses").isArray()) {
                JsonNode course = response.get("courses").get(0);
                if (course != null && course.has("assignments")) {
                    for (JsonNode assignNode : course.get("assignments")) {
                        java.util.Map<String, Object> assignment = objectMapper.convertValue(assignNode, java.util.Map.class);
                        assignments.add(assignment);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching assignments: {}", e.getMessage());
        }
        
        return assignments;
    }
    
    /**
     * Lấy thông tin submission của sinh viên
     * Function: mod_assign_get_submissions
     */
    public List<java.util.Map<String, Object>> getSubmissions(Long assignmentId) {
        log.info("Fetching submissions for assignment: {}", assignmentId);
        
        List<java.util.Map<String, Object>> submissions = new ArrayList<>();
        
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("assignmentids[0]", assignmentId.toString()));
            
            JsonNode response = callMoodleApi("mod_assign_get_submissions", params);
            
            if (response.has("assignments") && response.get("assignments").isArray()) {
                JsonNode assignment = response.get("assignments").get(0);
                if (assignment != null && assignment.has("submissions")) {
                    for (JsonNode subNode : assignment.get("submissions")) {
                        java.util.Map<String, Object> submission = objectMapper.convertValue(subNode, java.util.Map.class);
                        submissions.add(submission);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching submissions: {}", e.getMessage());
        }
        
        return submissions;
    }
}
