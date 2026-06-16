package com.kltn.chatbot.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity quản lý các hạng mục điểm (assignments, quizzes, exams)
 */
@Entity
@Table(name = "grade_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "moodle_grade_item_id", unique = true, nullable = false)
    private Long moodleGradeItemId;
    
    @Column(name = "course_id", nullable = false)
    private Long courseId;
    
    @Column(name = "item_name", nullable = false, length = 500)
    private String itemName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private GradeItemType itemType;
    
    @Column(name = "max_grade", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxGrade;
    
    @Column(name = "weight_percentage", precision = 5, scale = 2)
    private BigDecimal weightPercentage;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum GradeItemType {
        ASSIGNMENT,      // Bài tập
        QUIZ,           // Bài kiểm tra trắc nghiệm
        MIDTERM_EXAM,   // Thi giữa kỳ
        FINAL_EXAM,     // Thi cuối kỳ
        PROJECT,        // Đồ án
        PRESENTATION,   // Thuyết trình
        LAB,            // Thực hành
        ATTENDANCE,     // Điểm chuyên cần
        OTHER           // Khác
    }
}
