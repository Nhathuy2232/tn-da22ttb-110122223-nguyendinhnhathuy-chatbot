package com.kltn.chatbot.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity quản lý điểm số của sinh viên cho từng hạng mục
 */
@Entity
@Table(name = "student_grades", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "grade_item_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentGrade {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "grade_item_id", nullable = false)
    private Long gradeItemId;
    
    @Column(name = "raw_grade", precision = 10, scale = 2)
    private BigDecimal rawGrade;
    
    @Column(name = "final_grade", precision = 10, scale = 2)
    private BigDecimal finalGrade;
    
    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;
    
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", length = 50)
    private SubmissionStatus submissionStatus;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "graded_at")
    private LocalDateTime gradedAt;
    
    @Column(name = "is_late", nullable = false)
    private Boolean isLate = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum SubmissionStatus {
        NOT_SUBMITTED,   // Chưa nộp
        SUBMITTED,       // Đã nộp
        GRADED,         // Đã chấm điểm
        RESUBMITTED,    // Nộp lại
        LATE            // Nộp trễ
    }
}
