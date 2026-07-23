package com.kltn.chatbot.repository;

import com.kltn.chatbot.model.entity.Warning;
import com.kltn.chatbot.model.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository cho Warning entity
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Repository
public interface WarningRepository extends JpaRepository<Warning, Long> {

    /**
     * Tìm tất cả cảnh báo theo risk level
     */
    List<Warning> findByRiskLevel(RiskLevel riskLevel);

    /**
     * Tìm cảnh báo gần đây (trong N ngày)
     */
    @Query("SELECT w FROM Warning w WHERE w.detectedAt >= :fromDate ORDER BY w.detectedAt DESC")
    List<Warning> findRecentWarnings(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Tìm cảnh báo của sinh viên trong khóa học
     */
    @Query("SELECT w FROM Warning w WHERE w.studentId = :studentId AND w.courseId = :courseId ORDER BY w.detectedAt DESC")
    List<Warning> findByStudentAndCourse(@Param("studentId") Long studentId, 
                                          @Param("courseId") Long courseId);

    /**
     * Tìm cảnh báo chưa được acknowledge
     */
    @Query("SELECT w FROM Warning w WHERE w.isAcknowledged = false ORDER BY w.detectedAt DESC")
    List<Warning> findUnacknowledgedWarnings();

    /**
     * Tìm cảnh báo RED level chưa được acknowledge
     */
    @Query("SELECT w FROM Warning w WHERE w.riskLevel = 'RED' AND w.isAcknowledged = false ORDER BY w.detectedAt DESC")
    List<Warning> findUnacknowledgedRedWarnings();

    /**
     * Đếm số lượng cảnh báo theo risk level
     */
    @Query("SELECT COUNT(w) FROM Warning w WHERE w.riskLevel = :riskLevel")
    Long countByRiskLevel(@Param("riskLevel") RiskLevel riskLevel);

    /**
     * Tìm cảnh báo của khóa học theo risk level
     */
    @Query("SELECT w FROM Warning w WHERE w.courseId = :courseId AND w.riskLevel = :riskLevel ORDER BY w.detectedAt DESC")
    List<Warning> findByCourseAndRiskLevel(@Param("courseId") Long courseId, 
                                            @Param("riskLevel") RiskLevel riskLevel);
    
    /**
     * Tìm cảnh báo gần đây của sinh viên trong khóa học (để tránh spam notification)
     */
    @Query("SELECT w FROM Warning w WHERE w.studentId = :studentId AND w.courseId = :courseId AND w.detectedAt >= :fromDate ORDER BY w.detectedAt DESC")
    List<Warning> findByStudentIdAndCourseIdAndDetectedAtAfter(
        @Param("studentId") Long studentId,
        @Param("courseId") Long courseId,
        @Param("fromDate") LocalDateTime fromDate
    );
}
