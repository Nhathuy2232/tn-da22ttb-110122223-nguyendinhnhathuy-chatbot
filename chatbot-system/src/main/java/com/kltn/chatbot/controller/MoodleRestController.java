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
 * Moodle REST API Controller
 * 
 * Expose REST endpoints for Rasa Custom Actions (Python) to fetch data from Moodle.
 * 
 * Endpoints:
 * - GET /api/moodle/students/grades - Get student grades
 * - GET /api/moodle/students/attendance - Get student attendance
 * - GET /api/moodle/students/{studentId}/status - Get student risk status
 * - GET /api/moodle/students/at-risk - List at-risk students
 * - GET /api/moodle/class-overview - Get class statistics
 * - GET /api/moodle/students/{studentId}/last-access - Get last access time
 * - GET /api/moodle/courses - List all courses
 * - POST /api/moodle/notifications/send - Send notification
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

    /**
     * GET /api/moodle/students/grades
     * 
     * Get student grades (all courses or specific course)
     * 
     * @param studentId Student MSSV (e.g., "110122001")
     * @param courseName Optional course name filter
     * @return JSON with student grades
     */
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
                    if (courseId <= 1) continue; // Skip site course
                    
                    String courseFullName = course.get("fullname").asText();
                    
                    // Filter by course name if specified
                    if (courseName != null && !courseFullName.toLowerCase().contains(courseName.toLowerCase())) {
                        continue;
                    }
                    
                    // Get enrolled users
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
                                
                                // Get grades
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
                        .body(Map.of("error", "Student not found", "studentId", studentId));
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

    /**
     * GET /api/moodle/students/attendance
     * 
     * Get student attendance/activity data
     * 
     * @param studentId Student MSSV
     * @param courseName Optional course name filter
     * @return JSON with attendance data
     */
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
                                
                                // Check last access
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
                        .body(Map.of("error", "Student not found", "studentId", studentId));
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

    /**
     * GET /api/moodle/students/{studentId}/status
     * 
     * Get student risk status (green/yellow/red)
     */
    @GetMapping("/students/{studentId}/status")
    public ResponseEntity<?> getStudentStatus(@PathVariable String studentId) {
        
        try {
            log.info("API: Get status for student: {}", studentId);
            
            JsonNode courses = moodleApiService.getAllCourses();
            boolean foundStudent = false;
            int totalCourses = 0;
            int coursesWithGrades = 0;
            double totalGrade = 0;
            int inactiveCourses = 0;
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
                                foundStudent = true;
                                long userId = user.get("id").asLong();
                                fullName = user.get("fullname").asText();
                                totalCourses++;
                                
                                // Check last access
                                if (user.has("lastaccess")) {
                                    long lastAccess = user.get("lastaccess").asLong();
                                    if (lastAccess > 0) {
                                        Instant instant = Instant.ofEpochSecond(lastAccess);
                                        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                        long daysSinceAccess = Duration.between(dateTime, LocalDateTime.now()).toDays();
                                        
                                        if (daysSinceAccess > 14) {
                                            inactiveCourses++;
                                        }
                                    }
                                }
                                
                                // Get grades
                                try {
                                    JsonNode grades = moodleApiService.getGradeItems(courseId, userId);
                                    if (grades.has("usergrades") && grades.get("usergrades").isArray() 
                                        && grades.get("usergrades").size() > 0) {
                                        JsonNode userGrade = grades.get("usergrades").get(0);
                                        
                                        if (userGrade.has("gradeitems")) {
                                            for (JsonNode item : userGrade.get("gradeitems")) {
                                                if (item.has("graderaw") && item.has("grademax")) {
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
            
            if (!foundStudent) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Student not found", "studentId", studentId));
            }
            
            // Calculate average and determine risk level
            double avgGrade = coursesWithGrades > 0 ? totalGrade / coursesWithGrades : 0;
            
            String riskLevel;
            String riskColor;
            
            if (avgGrade >= 70 && inactiveCourses == 0) {
                riskLevel = "green";
                riskColor = "XANH - An toàn";
            } else if (avgGrade >= 50 && inactiveCourses <= 1) {
                riskLevel = "yellow";
                riskColor = "VÀNG - Cần theo dõi";
            } else {
                riskLevel = "red";
                riskColor = "ĐỎ - Nguy cơ cao";
            }
            
            return ResponseEntity.ok(Map.of(
                    "studentId", studentId,
                    "fullName", fullName,
                    "totalCourses", totalCourses,
                    "coursesWithGrades", coursesWithGrades,
                    "avgGrade", Math.round(avgGrade * 10) / 10.0,
                    "inactiveCourses", inactiveCourses,
                    "riskLevel", riskLevel,
                    "riskColor", riskColor
            ));
            
        } catch (Exception e) {
            log.error("Error getting student status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch status", "message", e.getMessage()));
        }
    }

    /**
     * GET /api/moodle/students/at-risk
     * 
     * Get list of at-risk students, optionally filtered by risk level
     */
    @GetMapping("/students/at-risk")
    public ResponseEntity<?> getAtRiskStudents(
            @RequestParam(required = false) String riskLevel) {
        
        try {
            log.info("API: Get at-risk students, filter: {}", riskLevel);
            
            JsonNode courses = moodleApiService.getAllCourses();
            Map<String, StudentRiskInfo> studentRiskMap = new HashMap<>();
            
            if (courses.isArray()) {
                for (JsonNode course : courses) {
                    long courseId = course.get("id").asLong();
                    if (courseId <= 1) continue;
                    
                    JsonNode users = moodleApiService.getEnrolledUsers(courseId);
                    if (users.isArray()) {
                        for (JsonNode user : users) {
                            String username = user.has("username") ? user.get("username").asText() : "";
                            
                            // Only analyze students (MSSV format: 1101XXXXX)
                            if (username.matches("^1101\\d{5}$")) {
                                long userId = user.get("id").asLong();
                                String fullName = user.get("fullname").asText();
                                
                                StudentRiskInfo riskInfo = studentRiskMap.getOrDefault(username, 
                                    new StudentRiskInfo(username, fullName));
                                
                                riskInfo.totalCourses++;
                                
                                // Check last access
                                if (user.has("lastaccess")) {
                                    long lastAccess = user.get("lastaccess").asLong();
                                    if (lastAccess > 0) {
                                        Instant instant = Instant.ofEpochSecond(lastAccess);
                                        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                        long daysSinceAccess = Duration.between(dateTime, LocalDateTime.now()).toDays();
                                        
                                        if (daysSinceAccess > 14) {
                                            riskInfo.inactiveCourses++;
                                        }
                                    }
                                }
                                
                                // Get grades
                                try {
                                    JsonNode grades = moodleApiService.getGradeItems(courseId, userId);
                                    if (grades.has("usergrades") && grades.get("usergrades").isArray() 
                                        && grades.get("usergrades").size() > 0) {
                                        JsonNode userGrade = grades.get("usergrades").get(0);
                                        
                                        if (userGrade.has("gradeitems")) {
                                            for (JsonNode item : userGrade.get("gradeitems")) {
                                                if (item.has("graderaw") && item.has("grademax")) {
                                                    double gradeRaw = item.get("graderaw").asDouble();
                                                    double gradeMax = item.get("grademax").asDouble();
                                                    
                                                    if (gradeMax > 0) {
                                                        riskInfo.totalGrade += (gradeRaw / gradeMax) * 100;
                                                        riskInfo.coursesWithGrades++;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // Skip if can't get grades
                                }
                                
                                studentRiskMap.put(username, riskInfo);
                            }
                        }
                    }
                }
            }
            
            // Categorize students by risk level
            List<Map<String, Object>> redRisk = new ArrayList<>();
            List<Map<String, Object>> yellowRisk = new ArrayList<>();
            List<Map<String, Object>> greenRisk = new ArrayList<>();
            
            for (StudentRiskInfo info : studentRiskMap.values()) {
                double avgGrade = info.coursesWithGrades > 0 ? info.totalGrade / info.coursesWithGrades : 0;
                
                Map<String, Object> studentData = new HashMap<>();
                studentData.put("studentId", info.studentId);
                studentData.put("fullName", info.fullName);
                studentData.put("avgGrade", Math.round(avgGrade * 10) / 10.0);
                studentData.put("inactiveCourses", info.inactiveCourses);
                studentData.put("totalCourses", info.totalCourses);
                
                if (avgGrade >= 70 && info.inactiveCourses == 0) {
                    studentData.put("riskLevel", "green");
                    greenRisk.add(studentData);
                } else if (avgGrade >= 50 && info.inactiveCourses <= 1) {
                    studentData.put("riskLevel", "yellow");
                    yellowRisk.add(studentData);
                } else {
                    studentData.put("riskLevel", "red");
                    redRisk.add(studentData);
                }
            }
            
            // Filter by requested risk level
            if (riskLevel != null) {
                String levelLower = riskLevel.toLowerCase();
                if (levelLower.contains("đỏ") || levelLower.contains("red") || levelLower.equals("cao")) {
                    return ResponseEntity.ok(Map.of(
                            "riskLevel", "red",
                            "students", redRisk,
                            "count", redRisk.size()
                    ));
                } else if (levelLower.contains("vàng") || levelLower.contains("yellow") || levelLower.equals("trung")) {
                    return ResponseEntity.ok(Map.of(
                            "riskLevel", "yellow",
                            "students", yellowRisk,
                            "count", yellowRisk.size()
                    ));
                } else if (levelLower.contains("xanh") || levelLower.contains("green")) {
                    return ResponseEntity.ok(Map.of(
                            "riskLevel", "green",
                            "students", greenRisk,
                            "count", greenRisk.size()
                    ));
                }
            }
            
            // Return all levels
            return ResponseEntity.ok(Map.of(
                    "red", Map.of("students", redRisk, "count", redRisk.size()),
                    "yellow", Map.of("students", yellowRisk, "count", yellowRisk.size()),
                    "green", Map.of("students", greenRisk, "count", greenRisk.size()),
                    "totalStudents", studentRiskMap.size()
            ));
            
        } catch (Exception e) {
            log.error("Error getting at-risk students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch at-risk students", "message", e.getMessage()));
        }
    }

    /**
     * GET /api/moodle/class-overview
     * 
     * Get class statistics and overview
     */
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

    /**
     * GET /api/moodle/students/{studentId}/last-access
     * 
     * Get student's last access time across all courses
     */
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
                        .body(Map.of("error", "Student not found", "studentId", studentId));
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

    /**
     * GET /api/moodle/courses
     * 
     * Get list of all courses
     */
    @GetMapping("/courses")
    public ResponseEntity<?> getCourses() {
        
        try {
            log.info("API: Get all courses");
            
            JsonNode courses = moodleApiService.getAllCourses();
            List<Map<String, Object>> courseList = new ArrayList<>();
            
            if (courses.isArray()) {
                for (JsonNode course : courses) {
                    long courseId = course.get("id").asLong();
                    if (courseId > 1) { // Skip site course
                        Map<String, Object> courseData = new HashMap<>();
                        courseData.put("id", courseId);
                        courseData.put("fullname", course.get("fullname").asText());
                        courseData.put("shortname", course.has("shortname") ? course.get("shortname").asText() : "");
                        
                        // Count enrolled students
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

    /**
     * POST /api/moodle/notifications/send
     * 
     * Send notification to students (placeholder for now)
     */
    @PostMapping("/notifications/send")
    public ResponseEntity<?> sendNotification(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String riskLevel,
            @RequestParam String message) {
        
        try {
            log.info("API: Send notification - studentId: {}, riskLevel: {}, message: {}", 
                    studentId, riskLevel, message);
            
            // TODO: Implement actual notification sending
            // For now, just return success
            
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

    // ==================== HELPER CLASS ====================
    
    /**
     * Helper class for student risk analysis
     */
    private static class StudentRiskInfo {
        String studentId;
        String fullName;
        int totalCourses = 0;
        int coursesWithGrades = 0;
        double totalGrade = 0;
        int inactiveCourses = 0;
        
        StudentRiskInfo(String studentId, String fullName) {
            this.studentId = studentId;
            this.fullName = fullName;
        }
    }
}
