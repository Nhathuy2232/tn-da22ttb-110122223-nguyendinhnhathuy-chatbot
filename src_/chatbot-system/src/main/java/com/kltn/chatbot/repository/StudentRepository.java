package com.kltn.chatbot.repository;

import com.kltn.chatbot.model.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho Student entity
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * Tìm sinh viên theo Moodle User ID
     */
    Optional<Student> findByMoodleUserId(Long moodleUserId);

    /**
     * Tìm sinh viên theo email
     */
    Optional<Student> findByEmail(String email);

    /**
     * Tìm sinh viên theo student code
     */
    Optional<Student> findByStudentCode(String studentCode);

    /**
     * Kiểm tra sinh viên có tồn tại theo Moodle User ID
     */
    boolean existsByMoodleUserId(Long moodleUserId);

    /**
     * Tìm sinh viên theo tên (case-insensitive, partial match)
     */
    @Query("SELECT s FROM Student s WHERE LOWER(s.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Optional<Student> findByFullNameContaining(@Param("name") String name);
}
