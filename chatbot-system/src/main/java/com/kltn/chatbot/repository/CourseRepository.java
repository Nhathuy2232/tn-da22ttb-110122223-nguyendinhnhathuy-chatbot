package com.kltn.chatbot.repository;

import com.kltn.chatbot.model.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho Course entity
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * Tìm khóa học theo Moodle Course ID
     */
    Optional<Course> findByMoodleCourseId(Long moodleCourseId);

    /**
     * Tìm tất cả khóa học của giảng viên
     */
    List<Course> findByInstructorId(Long instructorId);

    /**
     * Tìm tất cả khóa học đang active
     */
    List<Course> findByIsActiveTrue();

    /**
     * Tìm khóa học theo tên (case-insensitive, partial match)
     */
    @Query("SELECT c FROM Course c WHERE LOWER(c.courseName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Course> findByCourseNameContaining(@Param("name") String name);

    /**
     * Kiểm tra khóa học có tồn tại theo Moodle Course ID
     */
    boolean existsByMoodleCourseId(Long moodleCourseId);
}
