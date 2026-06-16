package com.kltn.chatbot.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho thông tin đăng ký khóa học của sinh viên
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Entity
@Table(name = "enrollments", indexes = {
    @Index(name = "idx_student_course", columnList = "student_id, course_id"),
    @Index(name = "idx_course_id", columnList = "course_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_student_course", columnNames = {"student_id", "course_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "enrollment_status", length = 50)
    private String enrollmentStatus;

    @Column(name = "attendance_rate")
    private Double attendanceRate;

    @Column(name = "grade_average")
    private Double gradeAverage;

    @Column(name = "completion_rate")
    private Double completionRate;

    @Column(name = "last_access_time")
    private LocalDateTime lastAccessTime;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
