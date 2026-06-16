package com.kltn.chatbot.service;

import com.kltn.chatbot.model.entity.*;
import com.kltn.chatbot.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service quản lý điểm số và đánh giá chi tiết
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GradeManagementService {
    
    private final StudentGradeRepository studentGradeRepository;
    private final GradeItemRepository gradeItemRepository;
    private final CourseGradeSummaryRepository courseSummaryRepository;
    private final UserRepository userRepository;
    private final MoodleApiService moodleApiService;
    
    /**
     * Đồng bộ tất cả grade items của một khóa học
     */
    @Transactional
    public void syncGradeItemsForCourse(Long courseId) {
        log.info("Đồng bộ grade items cho khóa học ID: {}", courseId);
        
        try {
            List<Map<String, Object>> moodleGradeItems = moodleApiService.getGradeItems(courseId);
            
            for (Map<String, Object> item : moodleGradeItems) {
                syncGradeItem(courseId, item);
            }
            
            log.info("Đồng bộ {} grade items cho khóa học {}", moodleGradeItems.size(), courseId);
        } catch (Exception e) {
            log.error("Lỗi đồng bộ grade items: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Đồng bộ một grade item
     */
    @Transactional
    public GradeItem syncGradeItem(Long courseId, Map<String, Object> moodleItem) {
        Long moodleGradeItemId = getLong(moodleItem, "id");
        
        GradeItem gradeItem = gradeItemRepository.findByMoodleGradeItemId(moodleGradeItemId)
                .orElse(new GradeItem());
        
        gradeItem.setMoodleGradeItemId(moodleGradeItemId);
        gradeItem.setCourseId(courseId);
        gradeItem.setItemName((String) moodleItem.get("itemname"));
        
        // Xác định loại grade item
        String itemType = (String) moodleItem.get("itemtype");
        gradeItem.setItemType(mapGradeItemType(itemType));
        
        // Set max grade
        Object grademax = moodleItem.get("grademax");
        if (grademax != null) {
            gradeItem.setMaxGrade(new BigDecimal(grademax.toString()));
        }
        
        // Set weight
        Object aggregationcoef = moodleItem.get("aggregationcoef");
        if (aggregationcoef != null) {
            gradeItem.setWeightPercentage(new BigDecimal(aggregationcoef.toString()));
        }
        
        return gradeItemRepository.save(gradeItem);
    }
    
    /**
     * Đồng bộ điểm của sinh viên
     */
    @Transactional
    public void syncStudentGrades(Long studentId, Long courseId) {
        log.info("Đồng bộ điểm cho sinh viên {} trong khóa học {}", studentId, courseId);
        
        try {
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên"));
            
            List<Map<String, Object>> moodleGrades = moodleApiService.getStudentGrades(
                    student.getMoodleUserId(), courseId);
            
            for (Map<String, Object> gradeData : moodleGrades) {
                syncStudentGrade(studentId, gradeData);
            }
            
            // Cập nhật tổng hợp điểm
            updateCourseGradeSummary(studentId, courseId);
            
            log.info("Đồng bộ {} điểm cho sinh viên {}", moodleGrades.size(), student.getFullName());
        } catch (Exception e) {
            log.error("Lỗi đồng bộ điểm sinh viên: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Đồng bộ một điểm
     */
    @Transactional
    public StudentGrade syncStudentGrade(Long studentId, Map<String, Object> gradeData) {
        Long gradeItemId = getLong(gradeData, "itemid");
        
        StudentGrade grade = studentGradeRepository.findByStudentIdAndGradeItemId(studentId, gradeItemId)
                .orElse(new StudentGrade());
        
        grade.setStudentId(studentId);
        grade.setGradeItemId(gradeItemId);
        
        // Set điểm
        Object rawGrade = gradeData.get("rawgrade");
        if (rawGrade != null) {
            grade.setRawGrade(new BigDecimal(rawGrade.toString()));
        }
        
        Object finalGrade = gradeData.get("finalgrade");
        if (finalGrade != null) {
            grade.setFinalGrade(new BigDecimal(finalGrade.toString()));
        }
        
        // Set feedback
        grade.setFeedback((String) gradeData.get("feedback"));
        
        // Set submission status
        String status = (String) gradeData.get("status");
        grade.setSubmissionStatus(mapSubmissionStatus(status));
        
        // Set timestamps
        Object submitted = gradeData.get("timesubmitted");
        if (submitted != null && !submitted.toString().equals("0")) {
            grade.setSubmittedAt(LocalDateTime.ofEpochSecond(Long.parseLong(submitted.toString()), 0, 
                    java.time.ZoneOffset.UTC));
        }
        
        Object graded = gradeData.get("timegraded");
        if (graded != null && !graded.toString().equals("0")) {
            grade.setGradedAt(LocalDateTime.ofEpochSecond(Long.parseLong(graded.toString()), 0, 
                    java.time.ZoneOffset.UTC));
        }
        
        grade.setLastSyncAt(LocalDateTime.now());
        
        return studentGradeRepository.save(grade);
    }
    
    /**
     * Cập nhật tổng hợp điểm khóa học
     */
    @Transactional
    public void updateCourseGradeSummary(Long studentId, Long courseId) {
        CourseGradeSummary summary = courseSummaryRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElse(new CourseGradeSummary());
        
        summary.setStudentId(studentId);
        summary.setCourseId(courseId);
        
        // Lấy tất cả điểm của sinh viên trong khóa học
        List<StudentGrade> grades = studentGradeRepository.findByStudentIdAndCourseId(studentId, courseId);
        List<GradeItem> items = gradeItemRepository.findByCourseIdAndIsActive(courseId, true);
        
        // Tính toán thống kê
        summary.setTotalAssignments(items.size());
        summary.setCompletedAssignments((int) grades.stream()
                .filter(g -> g.getFinalGrade() != null)
                .count());
        summary.setOnTimeSubmissions((int) grades.stream()
                .filter(g -> g.getSubmittedAt() != null && !g.getIsLate())
                .count());
        summary.setLateSubmissions((int) grades.stream()
                .filter(StudentGrade::getIsLate)
                .count());
        summary.setMissingSubmissions(summary.getTotalAssignments() - summary.getCompletedAssignments());
        
        // Tính điểm trung bình có trọng số
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        for (StudentGrade grade : grades) {
            if (grade.getFinalGrade() != null) {
                GradeItem item = gradeItemRepository.findById(grade.getGradeItemId()).orElse(null);
                if (item != null && item.getWeightPercentage() != null) {
                    BigDecimal normalizedGrade = grade.getFinalGrade()
                            .divide(item.getMaxGrade(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("10")); // Chuẩn hóa về thang điểm 10
                    
                    weightedSum = weightedSum.add(normalizedGrade.multiply(item.getWeightPercentage()));
                    totalWeight = totalWeight.add(item.getWeightPercentage());
                }
            }
        }
        
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            summary.setWeightedAverage(weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP));
        }
        
        // Tính các tỷ lệ
        summary.calculateCompletionRate();
        summary.calculateOnTimeRate();
        summary.calculateLetterGrade();
        
        summary.setLastCalculatedAt(LocalDateTime.now());
        
        courseSummaryRepository.save(summary);
        log.debug("Cập nhật tổng hợp điểm cho sinh viên {} khóa học {}", studentId, courseId);
    }
    
    /**
     * Lấy chi tiết điểm của sinh viên trong khóa học
     */
    public Map<String, Object> getDetailedGradeReport(Long studentId, Long courseId) {
        List<StudentGrade> grades = studentGradeRepository.findByStudentIdAndCourseId(studentId, courseId);
        CourseGradeSummary summary = courseSummaryRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElse(null);
        
        return Map.of(
            "grades", grades,
            "summary", summary != null ? summary : new CourseGradeSummary(),
            "totalItems", gradeItemRepository.countActiveByCourseId(courseId),
            "averageGrade", studentGradeRepository.getAverageGradeByStudentId(studentId)
        );
    }
    
    // Helper methods
    private GradeItem.GradeItemType mapGradeItemType(String moodleType) {
        if (moodleType == null) return GradeItem.GradeItemType.OTHER;
        
        return switch (moodleType.toLowerCase()) {
            case "mod" -> GradeItem.GradeItemType.ASSIGNMENT;
            case "quiz" -> GradeItem.GradeItemType.QUIZ;
            case "manual" -> GradeItem.GradeItemType.OTHER;
            default -> GradeItem.GradeItemType.OTHER;
        };
    }
    
    private StudentGrade.SubmissionStatus mapSubmissionStatus(String status) {
        if (status == null) return StudentGrade.SubmissionStatus.NOT_SUBMITTED;
        
        return switch (status.toLowerCase()) {
            case "submitted" -> StudentGrade.SubmissionStatus.SUBMITTED;
            case "graded" -> StudentGrade.SubmissionStatus.GRADED;
            case "late" -> StudentGrade.SubmissionStatus.LATE;
            default -> StudentGrade.SubmissionStatus.NOT_SUBMITTED;
        };
    }
    
    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
        }
        return null;
    }
}
