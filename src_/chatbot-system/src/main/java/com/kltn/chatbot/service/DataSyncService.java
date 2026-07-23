package com.kltn.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kltn.chatbot.model.entity.Course;
import com.kltn.chatbot.model.entity.Enrollment;
import com.kltn.chatbot.model.entity.Student;
import com.kltn.chatbot.repository.CourseRepository;
import com.kltn.chatbot.repository.EnrollmentRepository;
import com.kltn.chatbot.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service đồng bộ dữ liệu từ Moodle
 * Chạy tự động mỗi 6 giờ
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataSyncService {

    private final MoodleApiService moodleApiService;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final WarningAnalysisService warningAnalysisService;
    private final UserManagementService userManagementService;
    private final GradeManagementService gradeManagementService;

    /**
     * Sync tất cả dữ liệu từ Moodle
     * Chạy mỗi 6 giờ (21600000 ms)
     * DISABLED: Comment @Scheduled để tránh lỗi khi Moodle chưa setup
     */
    // @Scheduled(fixedRate = 21600000, initialDelay = 60000) // 6 hours, start after 1 minute
    @Transactional
    public void syncAllData() {
        log.info("=== Starting FULL data synchronization from Moodle ===");
        
        try {
            // 1. Sync users (students and teachers)
            log.info("Step 1: Syncing users...");
            userManagementService.syncAllUsersFromMoodle();
            
            // 2. Sync courses
            log.info("Step 2: Syncing courses...");
            syncCourses();
            
            // 3. Sync students and enrollments
            log.info("Step 3: Syncing enrollments...");
            syncStudentsAndEnrollments();
            
            // 4. Sync grade items and student grades
            log.info("Step 4: Syncing grades...");
            syncGradesForAllCourses();
            
            // 5. Update risk levels
            log.info("Step 5: Updating risk levels...");
            updateAllRiskLevels();
            
            log.info("=== Data synchronization completed successfully ===");
        } catch (Exception e) {
            log.error("Error during data synchronization", e);
            throw new RuntimeException("Data sync failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Đồng bộ điểm cho tất cả khóa học
     */
    private void syncGradesForAllCourses() {
        log.info("Syncing grades for all courses...");
        
        List<Course> courses = courseRepository.findByIsActiveTrue();
        int totalSynced = 0;
        
        for (Course course : courses) {
            try {
                log.info("Syncing grades for course: {} (ID: {})", course.getCourseName(), course.getId());
                
                // 1. Sync grade items
                gradeManagementService.syncGradeItemsForCourse(course.getId());
                
                // 2. Sync student grades
                List<Enrollment> enrollments = enrollmentRepository.findByCourseId(course.getId());
                
                for (Enrollment enrollment : enrollments) {
                    try {
                        gradeManagementService.syncStudentGrades(
                            enrollment.getStudent().getId(), 
                            course.getId()
                        );
                        totalSynced++;
                    } catch (Exception e) {
                        log.warn("Failed to sync grades for student {} in course {}: {}", 
                            enrollment.getStudent().getId(), course.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error syncing grades for course {}: {}", course.getId(), e.getMessage());
            }
        }
        
        log.info("Grades synced successfully. Total student-course combinations: {}", totalSynced);
    }

    /**
     * Sync courses từ Moodle
     */
    private void syncCourses() {
        log.info("Syncing courses from Moodle...");
        
        try {
            JsonNode coursesJson = moodleApiService.getAllCourses();
            
            if (coursesJson.isArray()) {
                for (JsonNode courseNode : coursesJson) {
                    Long moodleCourseId = courseNode.get("id").asLong();
                    String courseName = courseNode.get("fullname").asText();
                    
                    Course course = courseRepository.findByMoodleCourseId(moodleCourseId)
                            .orElse(Course.builder()
                                    .moodleCourseId(moodleCourseId)
                                    .build());
                    
                    course.setCourseName(courseName);
                    course.setIsActive(true);
                    course.setLastSyncAt(LocalDateTime.now());
                    
                    courseRepository.save(course);
                }
            }
            
            log.info("Courses synced successfully");
        } catch (Exception e) {
            log.error("Error syncing courses", e);
        }
    }

    /**
     * Sync students và enrollments từ Moodle
     */
    private void syncStudentsAndEnrollments() {
        log.info("Syncing students and enrollments from Moodle...");
        
        List<Course> courses = courseRepository.findByIsActiveTrue();
        
        for (Course course : courses) {
            try {
                JsonNode enrolledUsers = moodleApiService.getEnrolledUsers(course.getMoodleCourseId());
                
                if (enrolledUsers.isArray()) {
                    for (JsonNode userNode : enrolledUsers) {
                        Long moodleUserId = userNode.get("id").asLong();
                        String fullName = userNode.get("fullname").asText();
                        String email = userNode.has("email") ? userNode.get("email").asText() : "";
                        
                        // Save or update student
                        Student student = studentRepository.findByMoodleUserId(moodleUserId)
                                .orElse(Student.builder()
                                        .moodleUserId(moodleUserId)
                                        .build());
                        
                        student.setFullName(fullName);
                        student.setEmail(email);
                        student.setLastSyncAt(LocalDateTime.now());
                        
                        student = studentRepository.save(student);
                        
                        // Save or update enrollment
                        Enrollment enrollment = enrollmentRepository
                                .findByStudentIdAndCourseId(student.getId(), course.getId())
                                .orElse(Enrollment.builder()
                                        .student(student)
                                        .course(course)
                                        .build());
                        
                        // Get grades
                        try {
                            JsonNode gradesJson = moodleApiService.getGradeItems(
                                    course.getMoodleCourseId(), moodleUserId);
                            Double gradeAverage = moodleApiService.calculateAverageGrade(
                                    moodleApiService.parseGradeItems(gradesJson));
                            enrollment.setGradeAverage(gradeAverage);
                        } catch (Exception e) {
                            log.warn("Could not get grades for student {}", moodleUserId);
                        }
                        
                        // TODO: Get attendance rate from Moodle
                        // For now, set random value for demo
                        enrollment.setAttendanceRate(80.0 + Math.random() * 20);
                        
                        // Get completion rate
                        try {
                            JsonNode completionJson = moodleApiService.getActivitiesCompletionStatus(
                                    course.getMoodleCourseId(), moodleUserId);
                            // TODO: Calculate completion rate from JSON
                            enrollment.setCompletionRate(70.0 + Math.random() * 30);
                        } catch (Exception e) {
                            log.warn("Could not get completion for student {}", moodleUserId);
                        }
                        
                        enrollmentRepository.save(enrollment);
                    }
                }
            } catch (Exception e) {
                log.error("Error syncing enrollments for course {}", course.getId(), e);
            }
        }
        
        log.info("Students and enrollments synced successfully");
    }

    /**
     * Update risk levels cho tất cả enrollments
     */
    private void updateAllRiskLevels() {
        log.info("Updating risk levels for all students...");
        
        List<Course> courses = courseRepository.findByIsActiveTrue();
        
        for (Course course : courses) {
            List<Enrollment> enrollments = enrollmentRepository.findByCourseId(course.getId());
            warningAnalysisService.updateRiskLevelsForCourse(course, enrollments);
        }
        
        log.info("Risk levels updated successfully");
    }

    /**
     * Manual sync trigger (for testing)
     */
    public void triggerManualSync() {
        log.info("Manual sync triggered");
        syncAllData();
    }
    
    /**
     * Getter for MoodleApiService (used by controller)
     */
    public MoodleApiService getMoodleApiService() {
        return moodleApiService;
    }
}
