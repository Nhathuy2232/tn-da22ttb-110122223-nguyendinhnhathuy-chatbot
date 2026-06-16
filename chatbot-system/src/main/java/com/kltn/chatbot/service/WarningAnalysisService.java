package com.kltn.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kltn.chatbot.model.entity.Course;
import com.kltn.chatbot.model.entity.Enrollment;
import com.kltn.chatbot.model.entity.Student;
import com.kltn.chatbot.model.entity.Warning;
import com.kltn.chatbot.model.enums.RiskLevel;
import com.kltn.chatbot.repository.WarningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service phân tích và đánh giá mức độ nguy cơ của sinh viên
 * 
 * Logic cảnh báo (Rule-based):
 * - GREEN: Completion > 80% AND Grade ≥ 5.0 AND Attendance > 80%
 * - YELLOW: Grade < 5.0 OR Attendance ≤ 20%
 * - RED: (Grade < 5.0 AND Attendance ≤ 20%) OR LastAccess > 14 days
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WarningAnalysisService {

    private final WarningRepository warningRepository;
    private final MoodleApiService moodleApiService;

    @Value("${app.risk-analysis.green-threshold.completion-rate:80}")
    private Double greenCompletionThreshold;

    @Value("${app.risk-analysis.green-threshold.grade-average:5.0}")
    private Double greenGradeThreshold;

    @Value("${app.risk-analysis.green-threshold.attendance-rate:80}")
    private Double greenAttendanceThreshold;

    @Value("${app.risk-analysis.yellow-threshold.grade-average:5.0}")
    private Double yellowGradeThreshold;

    @Value("${app.risk-analysis.yellow-threshold.attendance-rate:20}")
    private Double yellowAttendanceThreshold;

    @Value("${app.risk-analysis.red-threshold.last-access-days:14}")
    private Integer redLastAccessThreshold;

    /**
     * Tính toán risk score cho sinh viên trong khóa học
     * 
     * @param student Sinh viên
     * @param course Khóa học
     * @param enrollment Enrollment data
     * @return Warning entity với risk level
     */
    @Transactional
    public Warning calculateRiskScore(Student student, Course course, Enrollment enrollment) {
        log.info("Calculating risk score for student: {} in course: {}", 
                student.getId(), course.getId());

        Double gradeAverage = enrollment.getGradeAverage() != null ? enrollment.getGradeAverage() : 0.0;
        Double attendanceRate = enrollment.getAttendanceRate() != null ? enrollment.getAttendanceRate() : 0.0;
        Double completionRate = enrollment.getCompletionRate() != null ? enrollment.getCompletionRate() : 0.0;
        
        Integer lastAccessDays = calculateLastAccessDays(enrollment.getLastAccessTime());

        // Determine risk level
        RiskLevel riskLevel = determineRiskLevel(gradeAverage, attendanceRate, completionRate, lastAccessDays);

        // Build reasons list
        List<String> reasons = buildReasons(gradeAverage, attendanceRate, completionRate, lastAccessDays, riskLevel);

        // Create or update warning
        Warning warning = Warning.builder()
                .student(student)
                .course(course)
                .riskLevel(riskLevel)
                .gradeAverage(gradeAverage)
                .attendanceRate(attendanceRate)
                .completionRate(completionRate)
                .lastAccessDays(lastAccessDays)
                .reasons(String.join("; ", reasons))
                .isAcknowledged(false)
                .build();

        warning = warningRepository.save(warning);

        log.info("Risk level calculated: {} for student: {} in course: {}", 
                riskLevel, student.getId(), course.getId());

        return warning;
    }

    /**
     * Xác định risk level dựa trên các chỉ số
     * 
     * Logic:
     * 1. RED nếu: (Grade < 5.0 AND Attendance ≤ 20%) OR LastAccess > 14 days
     * 2. YELLOW nếu: Grade < 5.0 OR Attendance ≤ 20%
     * 3. GREEN nếu: Completion > 80% AND Grade ≥ 5.0 AND Attendance > 80%
     */
    private RiskLevel determineRiskLevel(Double gradeAverage, Double attendanceRate, 
                                          Double completionRate, Integer lastAccessDays) {
        // RED conditions
        if ((gradeAverage < yellowGradeThreshold && attendanceRate <= yellowAttendanceThreshold) ||
            (lastAccessDays != null && lastAccessDays > redLastAccessThreshold)) {
            return RiskLevel.RED;
        }

        // YELLOW conditions
        if (gradeAverage < yellowGradeThreshold || attendanceRate <= yellowAttendanceThreshold) {
            return RiskLevel.YELLOW;
        }

        // GREEN conditions
        if (completionRate > greenCompletionThreshold && 
            gradeAverage >= greenGradeThreshold && 
            attendanceRate > greenAttendanceThreshold) {
            return RiskLevel.GREEN;
        }

        // Default to YELLOW if not clearly GREEN
        return RiskLevel.YELLOW;
    }

    /**
     * Tính số ngày kể từ lần truy cập cuối
     */
    private Integer calculateLastAccessDays(LocalDateTime lastAccessTime) {
        if (lastAccessTime == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(lastAccessTime, LocalDateTime.now());
    }

    /**
     * Xây dựng danh sách lý do cảnh báo
     */
    private List<String> buildReasons(Double gradeAverage, Double attendanceRate, 
                                       Double completionRate, Integer lastAccessDays, 
                                       RiskLevel riskLevel) {
        List<String> reasons = new ArrayList<>();

        if (riskLevel == RiskLevel.RED) {
            if (gradeAverage < yellowGradeThreshold) {
                reasons.add(String.format("Điểm trung bình thấp (%.2f < %.2f)", gradeAverage, yellowGradeThreshold));
            }
            if (attendanceRate <= yellowAttendanceThreshold) {
                reasons.add(String.format("Tỷ lệ chuyên cần thấp (%.2f%% ≤ %.2f%%)", attendanceRate, yellowAttendanceThreshold));
            }
            if (lastAccessDays != null && lastAccessDays > redLastAccessThreshold) {
                reasons.add(String.format("Không truy cập hệ thống %d ngày (> %d ngày)", lastAccessDays, redLastAccessThreshold));
            }
        } else if (riskLevel == RiskLevel.YELLOW) {
            if (gradeAverage < yellowGradeThreshold) {
                reasons.add(String.format("Điểm trung bình cần cải thiện (%.2f < %.2f)", gradeAverage, yellowGradeThreshold));
            }
            if (attendanceRate <= yellowAttendanceThreshold) {
                reasons.add(String.format("Tỷ lệ chuyên cần cần cải thiện (%.2f%% ≤ %.2f%%)", attendanceRate, yellowAttendanceThreshold));
            }
            if (completionRate < greenCompletionThreshold) {
                reasons.add(String.format("Tỷ lệ hoàn thành thấp (%.2f%% < %.2f%%)", completionRate, greenCompletionThreshold));
            }
        } else {
            reasons.add("Sinh viên đang học tập tốt");
        }

        return reasons;
    }

    /**
     * Cập nhật risk level cho tất cả sinh viên trong khóa học
     * Được gọi bởi scheduled task hàng ngày
     */
    @Transactional
    public void updateRiskLevelsForCourse(Course course, List<Enrollment> enrollments) {
        log.info("Updating risk levels for course: {} with {} enrollments", 
                course.getId(), enrollments.size());

        for (Enrollment enrollment : enrollments) {
            try {
                calculateRiskScore(enrollment.getStudent(), course, enrollment);
            } catch (Exception e) {
                log.error("Error calculating risk score for student: {} in course: {}", 
                        enrollment.getStudent().getId(), course.getId(), e);
            }
        }

        log.info("Completed risk level update for course: {}", course.getId());
    }

    /**
     * Lấy danh sách cảnh báo chưa được acknowledge
     */
    public List<Warning> getUnacknowledgedWarnings() {
        return warningRepository.findUnacknowledgedWarnings();
    }

    /**
     * Lấy danh sách cảnh báo RED chưa được acknowledge
     */
    public List<Warning> getUnacknowledgedRedWarnings() {
        return warningRepository.findUnacknowledgedRedWarnings();
    }

    /**
     * Acknowledge warning
     */
    @Transactional
    public Warning acknowledgeWarning(Long warningId, Long lecturerId) {
        Warning warning = warningRepository.findById(warningId)
                .orElseThrow(() -> new RuntimeException("Warning not found"));

        warning.setIsAcknowledged(true);
        warning.setAcknowledgedBy(lecturerId);
        warning.setAcknowledgedAt(LocalDateTime.now());

        return warningRepository.save(warning);
    }

    /**
     * Đếm số lượng warnings theo risk level
     */
    public Long countWarningsByRiskLevel(RiskLevel riskLevel) {
        return warningRepository.countByRiskLevel(riskLevel);
    }
}
