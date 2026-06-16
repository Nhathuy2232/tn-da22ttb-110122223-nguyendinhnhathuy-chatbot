package com.kltn.chatbot.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tổng hợp điểm của sinh viên trong từng khóa học
 */
@Entity
@Table(name = "course_grade_summaries",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseGradeSummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "course_id", nullable = false)
    private Long courseId;
    
    @Column(name = "total_grade", precision = 10, scale = 2)
    private BigDecimal totalGrade;
    
    @Column(name = "weighted_average", precision = 10, scale = 2)
    private BigDecimal weightedAverage;
    
    @Column(name = "letter_grade", length = 5)
    private String letterGrade;
    
    @Column(name = "total_assignments", nullable = false)
    private Integer totalAssignments = 0;
    
    @Column(name = "completed_assignments", nullable = false)
    private Integer completedAssignments = 0;
    
    @Column(name = "on_time_submissions", nullable = false)
    private Integer onTimeSubmissions = 0;
    
    @Column(name = "late_submissions", nullable = false)
    private Integer lateSubmissions = 0;
    
    @Column(name = "missing_submissions", nullable = false)
    private Integer missingSubmissions = 0;
    
    @Column(name = "completion_rate", precision = 5, scale = 2)
    private BigDecimal completionRate;
    
    @Column(name = "on_time_rate", precision = 5, scale = 2)
    private BigDecimal onTimeRate;
    
    @Column(name = "rank_in_class")
    private Integer rankInClass;
    
    @Column(name = "total_students_in_class")
    private Integer totalStudentsInClass;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Tính letter grade dựa trên điểm số
     */
    public void calculateLetterGrade() {
        if (weightedAverage == null) {
            this.letterGrade = "N/A";
            return;
        }
        
        double avg = weightedAverage.doubleValue();
        if (avg >= 8.5) {
            this.letterGrade = "A";
        } else if (avg >= 7.0) {
            this.letterGrade = "B";
        } else if (avg >= 5.5) {
            this.letterGrade = "C";
        } else if (avg >= 4.0) {
            this.letterGrade = "D";
        } else {
            this.letterGrade = "F";
        }
    }
    
    /**
     * Tính tỷ lệ hoàn thành
     */
    public void calculateCompletionRate() {
        if (totalAssignments > 0) {
            this.completionRate = new BigDecimal(completedAssignments)
                .multiply(new BigDecimal(100))
                .divide(new BigDecimal(totalAssignments), 2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.completionRate = BigDecimal.ZERO;
        }
    }
    
    /**
     * Tính tỷ lệ nộp đúng hạn
     */
    public void calculateOnTimeRate() {
        if (completedAssignments > 0) {
            this.onTimeRate = new BigDecimal(onTimeSubmissions)
                .multiply(new BigDecimal(100))
                .divide(new BigDecimal(completedAssignments), 2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.onTimeRate = BigDecimal.ZERO;
        }
    }
}
