package com.kltn.chatbot.repository;

import com.kltn.chatbot.model.entity.CourseGradeSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseGradeSummaryRepository extends JpaRepository<CourseGradeSummary, Long> {
    
    Optional<CourseGradeSummary> findByStudentIdAndCourseId(Long studentId, Long courseId);
    
    List<CourseGradeSummary> findByStudentId(Long studentId);
    
    List<CourseGradeSummary> findByCourseId(Long courseId);
    
    @Query("SELECT cgs FROM CourseGradeSummary cgs WHERE cgs.courseId = :courseId ORDER BY cgs.weightedAverage DESC")
    List<CourseGradeSummary> findByCourseIdOrderByGrade(@Param("courseId") Long courseId);
    
    @Query("SELECT cgs FROM CourseGradeSummary cgs WHERE cgs.studentId = :studentId ORDER BY cgs.weightedAverage ASC")
    List<CourseGradeSummary> findWeakestCoursesByStudent(@Param("studentId") Long studentId);
    
    @Query("SELECT AVG(cgs.weightedAverage) FROM CourseGradeSummary cgs WHERE cgs.studentId = :studentId")
    Double getOverallAverageByStudent(@Param("studentId") Long studentId);
    
    @Query("SELECT AVG(cgs.weightedAverage) FROM CourseGradeSummary cgs WHERE cgs.courseId = :courseId")
    Double getClassAverageByCourse(@Param("courseId") Long courseId);
    
    @Query("SELECT COUNT(cgs) FROM CourseGradeSummary cgs WHERE cgs.courseId = :courseId AND cgs.weightedAverage >= :minGrade")
    Long countStudentsAboveGrade(@Param("courseId") Long courseId, @Param("minGrade") Double minGrade);
    
    @Query("SELECT cgs FROM CourseGradeSummary cgs WHERE cgs.courseId = :courseId AND cgs.completionRate < :threshold")
    List<CourseGradeSummary> findStudentsWithLowCompletion(@Param("courseId") Long courseId, @Param("threshold") Double threshold);
}
