package com.kltn.chatbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kltn.chatbot.service.MoodleApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Moodle REST API Controller - Phiên bản cải tiến.
 *
 * Cải tiến chính:
 * - Lọc sinh viên vi phạm theo tiêu chí cụ thể (KHÔNG xuất toàn bộ)
 * - Hỗ trợ nhiều tiêu chí: vắng quá X%, điểm dưới Y, không online quá N ngày
 * - Phân tích theo lớp, theo môn, theo sinh viên cụ thể
 * - Lọc riêng: chỉ trả về những SV THỰC SỰ vi phạm
 *
 * @author Nguyễn Đình Nhật Huy - MSSV: 110122223
 */
@RestController
@RequestMapping("/api/moodle")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MoodleRestController {

    private final MoodleApiService moodleApiService;

    // ============================================================
    // GRADES
    // ============================================================

    @GetMapping("/students/grades")
    public ResponseEntity<?> getStudentGrades(
            @RequestParam String studentId,
            @RequestParam(required = false) String courseName) {

        try {
            log.info("API: Get grades for student: {}, course: {}", studentId, courseName);

            JsonNode courses = moodleApiService.getAllCourses();
            List<Map<String, Object>> gradesList = new ArrayList<>();
            boolean foundStudent = false;

            if (courses.isArray()) {
                for (JsonNode course : courses) {
                    long courseId = course.get("id").asLong();
                    if (courseId <= 1) continue;

                    String courseFullName = course.get("fullname").asText();

                    if (courseName != null && !courseFullName.toLowerCase().contains(courseName.toLowerCase())) {
                        continue;
                    }

                    JsonNode users = moodleApiService.getEnrolledUsers(courseId);
                    if (users.isArray()) {
                        for (JsonNode user : users) {
                            String username = user.has("username") ? user.get("username").asText() : "";
                            if (username.equals(studentId)) {
                                foundStudent = true;
                                long userId = user.get("id").asLong();
                                String fullName = user.get("fullname").asText();

                                Map<String, Object> courseGrades = new HashMap<>();
                                courseGrades.put("studentId", studentId);
                                courseGrades.put("fullName", fullName);
                                courseGrades.put("courseId", courseId);
                                courseGrades.put("courseName", courseFullName);

                                List<Map<String, Object>> gradeItems = new ArrayList<>();
                                try {
                                    JsonNode grades = moodleApiService.getGradeItems(courseId, userId);
                                    if (grades.has("usergrades") && grades.get("usergrades").isArray()
                                        && grades.get("usergrades").size() > 0) {
                                        JsonNode userGrade = grades.get("usergrades").get(0);
                                        if (userGrade.has("gradeitems")) {
                                            for (JsonNode item : userGrade.get("gradeitems")) {
                                                Map<String, Object> gradeItem = new HashMap<>();
                                                gradeItem.put("itemName", item.get("itemname").asText());
                                                if (item.has("graderaw")) {
                                                    double gradeRaw = item.get("graderaw").asDouble();
                                                    double gradeMax = item.has("grademax") ? item.get("grademax").asDouble() : 100;
                                                    gradeItem.put("gradeRaw", gradeRaw);
                                                    gradeItem.put("gradeMax", gradeMax);
                                                    gradeItem.put("percentage", (gradeRaw / gradeMax) * 100);
                                                } else {
                                                    gradeItem.put("gradeRaw", null);
                                                    gradeItem.put("gradeMax", null);
                                                    gradeItem.put("percentage", null);
                                                }
                                                gradeItems.add(gradeItem);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("Cannot get grades for course {}: {}", courseId, e.getMessage());
                                }

                                courseGrades.put("grades", gradeItems);
                                gradesList.add(courseGrades);
                                break;
                            }
                        }
                    }
                }
            }

            if (!foundStudent) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy sinh viên", "studentId", studentId));
            }

            return ResponseEntity.ok(Map.of(
                    "studentId", studentId,
                    "courses", gradesList,
                    "totalCourses", gradesList.size()
            ));
        } catch (Exception e) {
            log.error("Error getting student grades", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch grades", "message", e.getMessage()));
        }
    }

    // ============================================================
    // ATTENDANCE
    // ============================================================

    @GetMapping("/students/attendance")
    public ResponseEntity<?> getAttendance(
            @RequestParam String studentId,
            @RequestParam(required = false) String courseName) {

        try {
            log.info("API: Get attendance for student: {}, course: {}", studentId, courseName);

            JsonNode courses = moodleApiService.getAllCourses();
            List<Map<String, Object>> attendanceList = new ArrayList<>();
            boolean foundStudent = false;

            if (courses.isArray()) {
                for (JsonNode course : courses) {
                    long courseId = course.get("id").asLong();
                    if (courseId <= 1) continue;

                    String courseFullName = course.get("fullname").asText();

                    if (courseName != null && !courseFullName.toLowerCase().contains(courseName.toLowerCase())) {
                        continue;
                    }

                    JsonNode users = moodleApiService.getEnrolledUsers(courseId);
                    if (users.isArray()) {
                        for (JsonNode user : users) {
                            String username = user.has("username") ? user.get("username").asText() : "";
                            if (username.equals(studentId)) {
                                foundStudent = true;
                                String fullName = user.get("fullname").asText();

                                Map<String, Object> courseAttendance = new HashMap<>();
                                courseAttendance.put("studentId", studentId);
                                courseAttendance.put("fullName", fullName);
                                courseAttendance.put("courseId", courseId);
                                courseAttendance.put("courseName", courseFullName);

                                if (user.has("lastaccess")) {
                                    long lastAccess = user.get("lastaccess").asLong();
                                    if (lastAccess > 0) {
                                        Instant instant = Instant.ofEpochSecond(lastAccess);
                                        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                        long daysSinceAccess = Duration.between(dateTime, LocalDateTime.now()).toDays();

                                        courseAttendance.put("lastAccess", dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                                        courseAttendance.put("daysSinceAccess", daysSinceAccess);
                                        courseAttendance.put("status", daysSinceAccess > 7 ? "inactive" : "active");
                                    } else {
                                        courseAttendance.put("lastAccess", null);
                                        courseAttendance.put("daysSinceAccess", null);
                                        courseAttendance.put("status", "never_accessed");
                                    }
                                }

                                attendanceList.add(courseAttendance);
                                break;
                            }
                        }
                    }
                }
            }

            if (!foundStudent) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy sinh viên", "studentId", studentId));
            }

            return ResponseEntity.ok(Map.of(
                    "studentId", studentId,
                    "courses", attendanceList,
                    "totalCourses", attendanceList.size()
            ));
        } catch (Exception e) {
            log.error("Error getting attendance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch attendance", "message", e.getMessage()));
        }
    }

    // ============================================================
    // STUDENT STATUS
    // ============================================================

    @GetMapping("/students/{studentId}/status")
    public ResponseEntity<?> getStudentStatus(@PathVariable String studentId) {
        try {
            log.info("API: Get status for student: {}", studentId);
            StudentRiskAnalysis analysis = analyzeStudent(studentId);

            if (analysis == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy sinh viên", "studentId", studentId));
            }

            return ResponseEntity.ok(analysis.toMap());
        } catch (Exception e) {
            log.error("Error getting student status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch status", "message", e.getMessage()));
        }
    }

    // ============================================================
    // AT-RISK STUDENTS - LỌC CHỈ SINH VIÊN VI PHẠM
    // ============================================================

    /**
     * Lấy danh sách sinh viên vi phạm theo tiêu chí.
     *
     * @param riskLevel red/yellow/green (optional - nếu null thì trả về TẤT CẢ các mức có vi phạm)
     * @param minAvgGrade Ngưỡng điểm tối thiểu (sinh viên có điểm < ngưỡng sẽ bị liệt kê). Mặc định: 50
     * @param maxInactiveCourses Số môn không hoạt động tối đa. Mặc định: 1
     * @param inactiveDays Số ngày không online tối thiểu để tính là vi phạm. Mặc định: 14
     * @param onlyViolations true = chỉ trả về SV vi phạm (mặc định); false = trả tất cả
     * @return Danh sách sinh viên đã được lọc
     */
    @GetMapping("/students/at-risk")
    public ResponseEntity<?> getAtRiskStudents(
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false, defaultValue = "50") Double minAvgGrade,
            @RequestParam(required = false, defaultValue = "1") Integer maxInactiveCourses,
            @RequestParam(required = false, defaultValue = "14") Integer inactiveDays,
            @RequestParam(required = false, defaultValue = "true") Boolean onlyViolations) {

        try {
            log.info("API: Get at-risk students - riskLevel: {}, minAvgGrade: {}, maxInactive: {}, inactiveDays: {}, onlyViolations: {}",
                    riskLevel, minAvgGrade, maxInactiveCourses, inactiveDays, onlyViolations);

            // Phân tích tất cả sinh viên
            List<StudentRiskAnalysis> allStudents = analyzeAllStudents();

            // Lọc theo tiêu chí vi phạm
            List<Map<String, Object>> violations = new ArrayList<>();
            List<Map<String, Object>> normal = new ArrayList<>();

            for (StudentRiskAnalysis s : allStudents) {
                if (s.isViolation(minAvgGrade, maxInactiveCourses, inactiveDays)) {
                    violations.add(s.toMap());
                } else {
                    normal.add(s.toMap());
                }
            }

            // Nếu onlyViolations = true → chỉ trả vi phạm
            List<Map<String, Object>> result = Boolean.TRUE.equals(onlyViolations) ? violations : allStudents.stream().map(StudentRiskAnalysis::toMap).toList();

            // Nếu có yêu cầu riskLevel cụ thể
            if (riskLevel != null && !riskLevel.isBlank()) {
                String levelLower = riskLevel.toLowerCase();
                String targetLevel = null;
                if (levelLower.contains("đỏ") || levelLower.contains("red") || levelLower.equals("cao")) {
                    targetLevel = "red";
                } else if (levelLower.contains("vàng") || levelLower.contains("yellow") || levelLower.equals("trung")) {
                    targetLevel = "yellow";
                } else if (levelLower.contains("xanh") || levelLower.contains("green")) {
                    targetLevel = "green";
                }

                if (targetLevel != null) {
                    final String finalTarget = targetLevel;
                    result = result.stream()
                            .filter(m -> finalTarget.equals(m.get("riskLevel")))
                            .toList();
                }
            }

            return ResponseEntity.ok(Map.of(
                    "riskLevel", riskLevel != null ? riskLevel : "all",
                    "students", result,
                    "count", result.size(),
                    "totalAnalyzed", allStudents.size(),
                    "violationsCount", violations.size(),
                    "criteria", Map.of(
                            "minAvgGrade", minAvgGrade,
                            "maxInactiveCourses", maxInactiveCourses,
                            "inactiveDays", inactiveDays
                    )
            ));
        } catch (Exception e) {
            log.error("Error getting at-risk students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch at-risk students", "message", e.getMessage()));
        }
    }

    // ============================================================
    // CLASS OVERVIEW
    // ============================================================

    @GetMapping("/class-overview")
    public ResponseEntity<?> getClassOverview() {
        try {
            log.info("API: Get class overview");

            JsonNode courses = moodleApiService.getAllCourses();
            int courseCount = 0;
            Set<String> uniqueStudents = new HashSet<>();

            if (courses.isArray()) {
                for (JsonNode course : courses) {
                    long courseId = course.get("id").asLong();
                    if (courseId > 1) {
                        courseCount++;
                        JsonNode users = moodleApiService.getEnrolledUsers(courseId);
                        if (users.isArray()) {
                            for (JsonNode user : users) {
                                String userId = user.get("id").asText();
                                uniqueStudents.add(userId);
                            }
                        }
                    }
                }
            }

            int studentCount = uniqueStudents.size();
            double avgStudentsPerCourse = courseCount > 0 ? (double) studentCount / courseCount : 0;

            return ResponseEntity.ok(Map.of(
                    "totalCourses", courseCount,
                    "totalStudents", studentCount,
                    "avgStudentsPerCourse", Math.round(avgStudentsPerCourse * 10) / 10.0
            ));
        } catch (Exception e) {
            log.error("Error getting class overview", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch overview", "message", e.getMessage()));
        }
    }

    // ============================================================
    // LAST ACCESS
    // ============================================================

    @GetMapping("/students/{studentId}/last-access")
    public ResponseEntity<?> getLastAccess(@PathVariable String studentId) {
        try {
            log.info("API: Get last access for student: {}", studentId);

            JsonNode courses = moodleApiService.getAllCourses();
            List<Map<String, Object>> accessList = new ArrayList<>();
            boolean foundStudent = false;
            String fullName = "";

            if (courses.isArray()) {
                for (JsonNode course : courses) {
                    long courseId = course.get("id").asLong();
                    if (courseId <= 1) continue;

                    String courseFullName = course.get("fullname").asText();

                    JsonNode users = moodleApiService.getEnrolledUsers(courseId);
                    if (users.isArray()) {
                        for (JsonNode user : users) {
                            String username = user.has("username") ? user.get("username").asText() : "";
                            if (username.equals(studentId)) {
                                foundStudent = true;
                                fullName = user.get("fullname").asText();

                                Map<String, Object> courseAccess = new HashMap<>();
                                courseAccess.put("courseId", courseId);
                                courseAccess.put("courseName", courseFullName);

                                if (user.has("lastaccess")) {
                                    long lastAccess = user.get("lastaccess").asLong();
                                    if (lastAccess > 0) {
                                        Instant instant = Instant.ofEpochSecond(lastAccess);
                                        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                        long daysSinceAccess = Duration.between(dateTime, LocalDateTime.now()).toDays();

                                        courseAccess.put("lastAccess", dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                                        courseAccess.put("lastAccessTimestamp", lastAccess);
                                        courseAccess.put("daysSinceAccess", daysSinceAccess);
                                        courseAccess.put("status", daysSinceAccess > 7 ? "inactive" : "active");
                                    } else {
                                        courseAccess.put("lastAccess", null);
                                        courseAccess.put("lastAccessTimestamp", 0);
                                        courseAccess.put("daysSinceAccess", null);
                                        courseAccess.put("status", "never_accessed");
                                    }
                                }

                                accessList.add(courseAccess);
                                break;
                            }
                        }
                    }
                }
            }

            if (!foundStudent) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy sinh viên", "studentId", studentId));
            }

            return ResponseEntity.ok(Map.of(
                    "studentId", studentId,
                    "fullName", fullName,
                    "courses", accessList,
                    "totalCourses", accessList.size()
            ));
        } catch (Exception e) {
            log.error("Error getting last access", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch last access", "message", e.getMessage()));
        }
    }

    // ============================================================
    // COURSES
    // ============================================================

    @GetMapping("/courses")
    public ResponseEntity<?> getCourses() {
        try {
            log.info("API: Get all courses");

            JsonNode courses = moodleApiService.getAllCourses();
            List<Map<String, Object>> courseList = new ArrayList<>();

            if (courses.isArray()) {
                for (JsonNode course : courses) {
                    long courseId = course.get("id").asLong();
                    if (courseId > 1) {
                        Map<String, Object> courseData = new HashMap<>();
                        courseData.put("id", courseId);
                        courseData.put("fullname", course.get("fullname").asText());
                        courseData.put("shortname", course.has("shortname") ? course.get("shortname").asText() : "");

                        try {
                            JsonNode users = moodleApiService.getEnrolledUsers(courseId);
                            courseData.put("enrolledCount", users.isArray() ? users.size() : 0);
                        } catch (Exception e) {
                            courseData.put("enrolledCount", 0);
                        }

                        courseList.add(courseData);
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                    "courses", courseList,
                    "count", courseList.size()
            ));
        } catch (Exception e) {
            log.error("Error getting courses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch courses", "message", e.getMessage()));
        }
    }

    // ============================================================
    // NOTIFICATIONS
    // ============================================================

    @PostMapping("/notifications/send")
    public ResponseEntity<?> sendNotification(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String riskLevel,
            @RequestParam String message) {

        try {
            log.info("API: Send notification - studentId: {}, riskLevel: {}", studentId, riskLevel);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Notification sent successfully",
                    "targetStudentId", studentId != null ? studentId : "all",
                    "targetRiskLevel", riskLevel != null ? riskLevel : "all",
                    "notificationMessage", message
            ));
        } catch (Exception e) {
            log.error("Error sending notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send notification", "message", e.getMessage()));
        }
    }

    // ============================================================
    // SUBMISSIONS - SINH VIÊN CHƯA NỘP BÀI
    // ============================================================

    /**
     * Lấy danh sách sinh viên CHƯA NỘP bài (assignment/quiz) trong một môn.
     * LỌC: chỉ trả về những SV thực sự chưa nộp.
     */
    @GetMapping("/courses/submissions/missing")
    public ResponseEntity<?> getMissingSubmissions(
            @RequestParam String courseName,
            @RequestParam(required = false, defaultValue = "Assignment") String activityType) {

        try {
            log.info("API: Get missing submissions - course: {}, type: {}", courseName, activityType);

            JsonNode courses = moodleApiService.getAllCourses();
            List<Map<String, Object>> missingStudents = new ArrayList<>();
            String foundCourseName = "";

            if (courses.isArray()) {
                for (JsonNode course : courses) {
                    long courseId = course.get("id").asLong();
                    if (courseId <= 1) continue;

                    String courseFullName = course.get("fullname").asText();

                    if (!courseFullName.toLowerCase().contains(courseName.toLowerCase())) {
                        continue;
                    }

                    foundCourseName = courseFullName;
                    JsonNode users = moodleApiService.getEnrolledUsers(courseId);
                    if (users.isArray()) {
                        for (JsonNode user : users) {
                            String username = user.has("username") ? user.get("username").asText() : "";
                            if (!username.matches("^1101\\d{5}$")) continue;

                            long userId = user.get("id").asLong();
                            String fullName = user.get("fullname").asText();
                            int submitted = 0;
                            int total = 0;

                            try {
                                JsonNode grades = moodleApiService.getGradeItems(courseId, userId);
                                if (grades.has("usergrades") && grades.get("usergrades").isArray()
                                    && grades.get("usergrades").size() > 0) {
                                    JsonNode userGrade = grades.get("usergrades").get(0);
                                    if (userGrade.has("gradeitems")) {
                                        for (JsonNode item : userGrade.get("gradeitems")) {
                                            String itemName = item.get("itemname").asText().toLowerCase();
                                            if (itemName.contains(activityType.toLowerCase())
                                                || (activityType.equalsIgnoreCase("Assignment") && itemName.contains("assign"))
                                                || (activityType.equalsIgnoreCase("Quiz") && itemName.contains("quiz"))
                                                || (activityType.equalsIgnoreCase("Lab") && itemName.contains("lab"))) {
                                                total++;
                                                if (item.has("graderaw") && !item.get("graderaw").isNull()) {
                                                    submitted++;
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Cannot get grades for student {}: {}", username, e.getMessage());
                            }

                            if (total > 0 && submitted < total) {
                                Map<String, Object> missing = new HashMap<>();
                                missing.put("studentId", username);
                                missing.put("fullName", fullName);
                                missing.put("submitted", submitted);
                                missing.put("total", total);
                                missing.put("missing", total - submitted);
                                missingStudents.add(missing);
                            }
                        }
                    }
                    break; // Đã tìm thấy course
                }
            }

            return ResponseEntity.ok(Map.of(
                    "courseName", foundCourseName.isEmpty() ? courseName : foundCourseName,
                    "activityType", activityType,
                    "missingStudents", missingStudents,
                    "count", missingStudents.size()
            ));
        } catch (Exception e) {
            log.error("Error getting missing submissions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch submissions", "message", e.getMessage()));
        }
    }

    // ============================================================
    // COURSE RISK - LỌC CHỈ SINH VIÊN VI PHẠM THEO MÔN
    // ============================================================

    /**
     * Lọc sinh viên có rủi ro trong một môn cụ thể.
     * LỌC: chỉ trả về những SV thực sự có rủi ro (điểm thấp HOẶC không online HOẶC cả hai).
     */
    @GetMapping("/courses/risk-filter")
    public ResponseEntity<?> getCourseRiskFilter(
            @RequestParam String courseName,
            @RequestParam(required = false, defaultValue = "yellow") String riskLevel,
            @RequestParam(required = false, defaultValue = "50") Double minAvgGrade,
            @RequestParam(required = false, defaultValue = "7") Integer maxInactiveDays) {

        try {
            log.info("API: Course risk filter - course: {}, level: {}", courseName, riskLevel);

            JsonNode courses = moodleApiService.getAllCourses();
            List<Map<String, Object>> atRiskStudents = new ArrayList<>();
            String foundCourseName = "";
            int totalStudents = 0;

            if (courses.isArray()) {
                for (JsonNode course : courses) {
                    long courseId = course.get("id").asLong();
                    if (courseId <= 1) continue;

                    String courseFullName = course.get("fullname").asText();
                    if (!courseFullName.toLowerCase().contains(courseName.toLowerCase())) {
                        continue;
                    }

                    foundCourseName = courseFullName;
                    JsonNode users = moodleApiService.getEnrolledUsers(courseId);
                    if (users.isArray()) {
                        for (JsonNode user : users) {
                            String username = user.has("username") ? user.get("username").asText() : "";
                            if (!username.matches("^1101\\d{5}$")) continue;

                            totalStudents++;
                            long userId = user.get("id").asLong();
                            String fullName = user.get("fullname").asText();
                            double avgGrade = 0;
                            boolean hasGrades = false;
                            long daysSinceAccess = 0;

                            // Tính điểm trung bình
                            try {
                                JsonNode grades = moodleApiService.getGradeItems(courseId, userId);
                                if (grades.has("usergrades") && grades.get("usergrades").isArray()
                                    && grades.get("usergrades").size() > 0) {
                                    JsonNode userGrade = grades.get("usergrades").get(0);
                                    if (userGrade.has("gradeitems")) {
                                        double total = 0;
                                        int count = 0;
                                        for (JsonNode item : userGrade.get("gradeitems")) {
                                            if (item.has("graderaw") && item.has("grademax") && !item.get("graderaw").isNull()) {
                                                double raw = item.get("graderaw").asDouble();
                                                double max = item.get("grademax").asDouble();
                                                if (max > 0) {
                                                    total += (raw / max) * 100;
                                                    count++;
                                                }
                                            }
                                        }
                                        if (count > 0) {
                                            avgGrade = total / count;
                                            hasGrades = true;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // ignore
                            }

                            // Last access
                            if (user.has("lastaccess")) {
                                long lastAccess = user.get("lastaccess").asLong();
                                if (lastAccess > 0) {
                                    Instant instant = Instant.ofEpochSecond(lastAccess);
                                    LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                    daysSinceAccess = Duration.between(dateTime, LocalDateTime.now()).toDays();
                                } else {
                                    daysSinceAccess = 999; // Chưa từng truy cập
                                }
                            }

                            // Xác định mức rủi ro
                            String level = "green";
                            if ((hasGrades && avgGrade < 50) || daysSinceAccess > 14) {
                                level = "red";
                            } else if ((hasGrades && avgGrade < minAvgGrade) || daysSinceAccess > maxInactiveDays) {
                                level = "yellow";
                            }

                            // LỌC: chỉ giữ SV có rủi ro theo yêu cầu
                            if (matchesRiskLevel(level, riskLevel)) {
                                Map<String, Object> student = new LinkedHashMap<>();
                                student.put("studentId", username);
                                student.put("fullName", fullName);
                                student.put("avgGrade", Math.round(avgGrade * 10) / 10.0);
                                student.put("hasGrades", hasGrades);
                                student.put("daysSinceAccess", daysSinceAccess);
                                student.put("riskLevel", level);
                                atRiskStudents.add(student);
                            }
                        }
                    }
                    break; // Đã tìm thấy course
                }
            }

            return ResponseEntity.ok(Map.of(
                    "courseName", foundCourseName.isEmpty() ? courseName : foundCourseName,
                    "filterLevel", riskLevel,
                    "totalStudents", totalStudents,
                    "atRiskStudents", atRiskStudents,
                    "count", atRiskStudents.size(),
                    "criteria", Map.of(
                            "minAvgGrade", minAvgGrade,
                            "maxInactiveDays", maxInactiveDays
                    )
            ));
        } catch (Exception e) {
            log.error("Error in course risk filter", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to filter course risk", "message", e.getMessage()));
        }
    }

    // ============================================================
    // INACTIVE STUDENTS
    // ============================================================

    /**
     * Tìm sinh viên ngừng tương tác (không online quá N ngày).
     * LỌC: chỉ trả về những SV thực sự không online quá ngưỡng.
     */
    @GetMapping("/students/inactive")
    public ResponseEntity<?> getInactiveStudents(
            @RequestParam(required = false, defaultValue = "14") Integer inactiveDays,
            @RequestParam(required = false) String classCode) {

        try {
            log.info("API: Inactive students - days: {}, class: {}", inactiveDays, classCode);

            List<StudentRiskAnalysis> allStudents = analyzeAllStudents();
            List<Map<String, Object>> inactiveStudents = new ArrayList<>();

            for (StudentRiskAnalysis s : allStudents) {
                if (s.maxDaysSinceAccess >= inactiveDays) {
                    Map<String, Object> student = s.toMap();
                    student.put("daysSinceAccess", s.maxDaysSinceAccess);
                    student.put("lastAccessDate", s.lastAccessDate);
                    inactiveStudents.add(student);
                }
            }

            // Sắp xếp theo số ngày không online giảm dần
            inactiveStudents.sort((a, b) -> {
                Long da = ((Number) a.get("daysSinceAccess")).longValue();
                Long db = ((Number) b.get("daysSinceAccess")).longValue();
                return db.compareTo(da);
            });

            return ResponseEntity.ok(Map.of(
                    "inactiveDaysThreshold", inactiveDays,
                    "inactiveStudents", inactiveStudents,
                    "count", inactiveStudents.size()
            ));
        } catch (Exception e) {
            log.error("Error getting inactive students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch inactive students", "message", e.getMessage()));
        }
    }

    // ============================================================
    // CLASS RISK - CHO CỐ VẤN
    // ============================================================

    /**
     * Xem danh sách sinh viên có rủi ro theo lớp.
     * LỌC: chỉ trả về những SV có rủi ro.
     */
    @GetMapping("/class-risk-summary")
    public ResponseEntity<?> getClassRiskSummary(
            @RequestParam(required = false) String classCode) {

        try {
            log.info("API: Class risk summary - class: {}", classCode);

            List<StudentRiskAnalysis> allStudents = analyzeAllStudents();
            List<Map<String, Object>> atRisk = new ArrayList<>();
            int redCount = 0, yellowCount = 0, greenCount = 0;

            for (StudentRiskAnalysis s : allStudents) {
                switch (s.riskLevel) {
                    case "red" -> redCount++;
                    case "yellow" -> yellowCount++;
                    default -> greenCount++;
                }
                if (!"green".equals(s.riskLevel)) {
                    atRisk.add(s.toMap());
                }
            }

            // Sắp xếp: red trước, yellow sau
            atRisk.sort((a, b) -> {
                String la = (String) a.get("riskLevel");
                String lb = (String) b.get("riskLevel");
                if (la.equals(lb)) return 0;
                if ("red".equals(la)) return -1;
                return 1;
            });

            return ResponseEntity.ok(Map.of(
                    "classCode", classCode != null ? classCode : "ALL",
                    "summary", Map.of(
                            "red", redCount,
                            "yellow", yellowCount,
                            "green", greenCount,
                            "total", allStudents.size()
                    ),
                    "atRiskStudents", atRisk,
                    "violationCount", atRisk.size()
            ));
        } catch (Exception e) {
            log.error("Error in class risk summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch class risk", "message", e.getMessage()));
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private boolean matchesRiskLevel(String level, String filter) {
        if (filter == null || filter.isBlank() || filter.equalsIgnoreCase("all")) {
            return !"green".equals(level); // Mặc định lấy tất cả trừ green
        }
        String f = filter.toLowerCase();
        if (f.contains("đỏ") || f.contains("red") || f.equals("cao")) {
            return "red".equals(level);
        }
        if (f.contains("vàng") || f.contains("yellow") || f.equals("trung")) {
            return "yellow".equals(level) || "red".equals(level);
        }
        if (f.contains("xanh") || f.contains("green")) {
            return true; // Lấy tất cả
        }
        return !"green".equals(level);
    }

    /**
     * Phân tích một sinh viên cụ thể.
     */
    private StudentRiskAnalysis analyzeStudent(String studentId) {
        JsonNode courses = moodleApiService.getAllCourses();
        int totalCourses = 0;
        int coursesWithGrades = 0;
        double totalGrade = 0;
        int inactiveCourses = 0;
        long maxDaysSinceAccess = 0;
        String lastAccessDate = "Chưa truy cập";
        String fullName = "";

        if (courses.isArray()) {
            for (JsonNode course : courses) {
                long courseId = course.get("id").asLong();
                if (courseId <= 1) continue;

                JsonNode users = moodleApiService.getEnrolledUsers(courseId);
                if (users.isArray()) {
                    for (JsonNode user : users) {
                        String username = user.has("username") ? user.get("username").asText() : "";
                        if (username.equals(studentId)) {
                            long userId = user.get("id").asLong();
                            fullName = user.get("fullname").asText();
                            totalCourses++;

                            if (user.has("lastaccess")) {
                                long lastAccess = user.get("lastaccess").asLong();
                                if (lastAccess > 0) {
                                    Instant instant = Instant.ofEpochSecond(lastAccess);
                                    LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                    long daysSince = Duration.between(dateTime, LocalDateTime.now()).toDays();
                                    if (daysSince > maxDaysSinceAccess) {
                                        maxDaysSinceAccess = daysSince;
                                        lastAccessDate = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                                    }
                                    if (daysSince > 14) inactiveCourses++;
                                } else {
                                    if (maxDaysSinceAccess < 999) {
                                        maxDaysSinceAccess = 999;
                                    }
                                }
                            }

                            try {
                                JsonNode grades = moodleApiService.getGradeItems(courseId, userId);
                                if (grades.has("usergrades") && grades.get("usergrades").isArray()
                                    && grades.get("usergrades").size() > 0) {
                                    JsonNode userGrade = grades.get("usergrades").get(0);
                                    if (userGrade.has("gradeitems")) {
                                        for (JsonNode item : userGrade.get("gradeitems")) {
                                            if (item.has("graderaw") && item.has("grademax") && !item.get("graderaw").isNull()) {
                                                double gradeRaw = item.get("graderaw").asDouble();
                                                double gradeMax = item.get("grademax").asDouble();
                                                if (gradeMax > 0) {
                                                    totalGrade += (gradeRaw / gradeMax) * 100;
                                                    coursesWithGrades++;
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Cannot get grades for student {} in course {}", studentId, courseId);
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (totalCourses == 0) return null;
        return new StudentRiskAnalysis(studentId, fullName, totalCourses, coursesWithGrades, totalGrade, inactiveCourses, maxDaysSinceAccess, lastAccessDate);
    }

    /**
     * Phân tích tất cả sinh viên.
     */
    private List<StudentRiskAnalysis> analyzeAllStudents() {
        JsonNode courses = moodleApiService.getAllCourses();
        Map<String, StudentRiskAnalysis> studentMap = new HashMap<>();

        if (courses.isArray()) {
            for (JsonNode course : courses) {
                long courseId = course.get("id").asLong();
                if (courseId <= 1) continue;

                JsonNode users = moodleApiService.getEnrolledUsers(courseId);
                if (users.isArray()) {
                    for (JsonNode user : users) {
                        String username = user.has("username") ? user.get("username").asText() : "";
                        if (!username.matches("^1101\\d{5}$")) continue;

                        long userId = user.get("id").asLong();
                        String fullName = user.get("fullname").asText();

                        StudentRiskAnalysis info = studentMap.computeIfAbsent(username, k -> new StudentRiskAnalysis(username, fullName, 0, 0, 0, 0, 0, "Chưa truy cập"));
                        info.totalCourses++;

                        if (user.has("lastaccess")) {
                            long lastAccess = user.get("lastaccess").asLong();
                            if (lastAccess > 0) {
                                Instant instant = Instant.ofEpochSecond(lastAccess);
                                LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                long daysSince = Duration.between(dateTime, LocalDateTime.now()).toDays();
                                if (daysSince > info.maxDaysSinceAccess) {
                                    info.maxDaysSinceAccess = daysSince;
                                    info.lastAccessDate = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                                }
                                if (daysSince > 14) info.inactiveCourses++;
                            } else {
                                if (info.maxDaysSinceAccess < 999) {
                                    info.maxDaysSinceAccess = 999;
                                }
                            }
                        }

                        try {
                            JsonNode grades = moodleApiService.getGradeItems(courseId, userId);
                            if (grades.has("usergrades") && grades.get("usergrades").isArray()
                                && grades.get("usergrades").size() > 0) {
                                JsonNode userGrade = grades.get("usergrades").get(0);
                                if (userGrade.has("gradeitems")) {
                                    for (JsonNode item : userGrade.get("gradeitems")) {
                                        if (item.has("graderaw") && item.has("grademax") && !item.get("graderaw").isNull()) {
                                            double gradeRaw = item.get("graderaw").asDouble();
                                            double gradeMax = item.get("grademax").asDouble();
                                            if (gradeMax > 0) {
                                                info.totalGrade += (gradeRaw / gradeMax) * 100;
                                                info.coursesWithGrades++;
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
        }
        return new ArrayList<>(studentMap.values());
    }

    // ============================================================
    // STUDENT RISK ANALYSIS - DATA CLASS
    // ============================================================

    private static class StudentRiskAnalysis {
        String studentId;
        String fullName;
        int totalCourses;
        int coursesWithGrades;
        double totalGrade;
        int inactiveCourses;
        long maxDaysSinceAccess;
        String lastAccessDate;
        String riskLevel;

        StudentRiskAnalysis(String studentId, String fullName, int totalCourses, int coursesWithGrades,
                            double totalGrade, int inactiveCourses, long maxDaysSinceAccess, String lastAccessDate) {
            this.studentId = studentId;
            this.fullName = fullName;
            this.totalCourses = totalCourses;
            this.coursesWithGrades = coursesWithGrades;
            this.totalGrade = totalGrade;
            this.inactiveCourses = inactiveCourses;
            this.maxDaysSinceAccess = maxDaysSinceAccess;
            this.lastAccessDate = lastAccessDate;
            this.riskLevel = computeRiskLevel();
        }

        String computeRiskLevel() {
            double avgGrade = coursesWithGrades > 0 ? totalGrade / coursesWithGrades : 0;
            if (avgGrade >= 70 && inactiveCourses == 0) return "green";
            if (avgGrade >= 50 && inactiveCourses <= 1) return "yellow";
            return "red";
        }

        boolean isViolation(double minAvgGrade, int maxInactive, int inactiveDays) {
            double avgGrade = coursesWithGrades > 0 ? totalGrade / coursesWithGrades : 0;
            return avgGrade < minAvgGrade || inactiveCourses > maxInactive || maxDaysSinceAccess > inactiveDays;
        }

        Map<String, Object> toMap() {
            double avgGrade = coursesWithGrades > 0 ? totalGrade / coursesWithGrades : 0;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("studentId", studentId);
            map.put("fullName", fullName);
            map.put("avgGrade", Math.round(avgGrade * 10) / 10.0);
            map.put("inactiveCourses", inactiveCourses);
            map.put("totalCourses", totalCourses);
            map.put("maxDaysSinceAccess", maxDaysSinceAccess);
            map.put("lastAccessDate", lastAccessDate);
            map.put("riskLevel", riskLevel);
            return map;
        }
    }
}
