package com.kltn.chatbot.repository;

import com.kltn.chatbot.model.entity.StudentGrade;
import com.kltn.chatbot.model.entity.StudentGrade.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {
    
    Optional<StudentGrade> findByStudentIdAndGradeItemId(Long studentId, Long gradeItemId);
    
    List<StudentGrade> findByStudentId(Long studentId);
    
    List<StudentGrade> findByGradeItemId(Long gradeItemId);
    
    List<StudentGrade> findByStudentIdAndSubmissionStatus(Long studentId, SubmissionStatus status);
    
    @Query("SELECT sg FROM StudentGrade sg WHERE sg.studentId = :studentId AND sg.gradeItemId IN " +
           "(SELECT gi.id FROM GradeItem gi WHERE gi.courseId = :courseId)")
    List<StudentGrade> findByStudentIdAndCourseId(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    
    @Query("SELECT AVG(sg.finalGrade) FROM StudentGrade sg WHERE sg.studentId = :studentId AND sg.finalGrade IS NOT NULL")
    BigDecimal getAverageGradeByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT AVG(sg.finalGrade) FROM StudentGrade sg WHERE sg.gradeItemId = :gradeItemId AND sg.finalGrade IS NOT NULL")
    BigDecimal getAverageGradeByGradeItemId(@Param("gradeItemId") Long gradeItemId);
    
    @Query("SELECT COUNT(sg) FROM StudentGrade sg WHERE sg.studentId = :studentId AND sg.submissionStatus = 'NOT_SUBMITTED'")
    Long countMissingSubmissions(@Param("studentId") Long studentId);
    
    @Query("SELECT COUNT(sg) FROM StudentGrade sg WHERE sg.studentId = :studentId AND sg.isLate = true")
    Long countLateSubmissions(@Param("studentId") Long studentId);
    
    @Query("SELECT sg FROM StudentGrade sg WHERE sg.studentId = :studentId AND sg.finalGrade IS NOT NULL ORDER BY sg.gradedAt DESC")
    List<StudentGrade> findRecentGradesByStudentId(@Param("studentId") Long studentId);
}
