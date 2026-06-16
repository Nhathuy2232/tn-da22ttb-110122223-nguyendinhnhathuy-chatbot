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
 * Entity đại diện cho thông tin khóa học
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Entity
@Table(name = "courses", indexes = {
    @Index(name = "idx_moodle_course_id", columnList = "moodle_course_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "moodle_course_id", nullable = false, unique = true)
    private Long moodleCourseId;

    @Column(name = "course_name", nullable = false, length = 500)
    private String courseName;

    @Column(name = "course_code", length = 50)
    private String courseCode;

    @Column(name = "instructor_id")
    private Long instructorId;

    @Column(name = "instructor_name", length = 255)
    private String instructorName;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
