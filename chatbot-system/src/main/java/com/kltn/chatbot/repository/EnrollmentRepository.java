package com.kltn.chatbot.repository;

import com.kltn.chatbot.model.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho Enrollment entity
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Tìm enrollment theo student ID và course ID
     */
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId")
    Optional<Enrollment> findByStudentIdAndCourseId(@Param("studentId") Long studentId, 
                                                      @Param("courseId") Long courseId);

    /**
     * Tìm tất cả enrollment của sinh viên
     */
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId")
    List<Enrollment> findByStudentId(@Param("studentId") Long studentId);

    /**
     * Tìm tất cả enrollment của khóa học
     */
    @Query("SELECT e FROM Enrollment e WHERE e.course.id = :courseId")
    List<Enrollment> findByCourseId(@Param("courseId") Long courseId);

    /**
     * Tìm enrollment với điểm trung bình thấp trong khóa học
     */
    @Query("SELECT e FROM Enrollment e WHERE e.course.id = :courseId AND e.gradeAverage < :threshold")
    List<Enrollment> findLowGradeEnrollments(@Param("courseId") Long courseId, 
                                              @Param("threshold") Double threshold);

    /**
     * Tìm enrollment với tỷ lệ chuyên cần thấp trong khóa học
     */
    @Query("SELECT e FROM Enrollment e WHERE e.course.id = :courseId AND e.attendanceRate < :threshold")
    List<Enrollment> findLowAttendanceEnrollments(@Param("courseId") Long courseId, 
                                                    @Param("threshold") Double threshold);
}
