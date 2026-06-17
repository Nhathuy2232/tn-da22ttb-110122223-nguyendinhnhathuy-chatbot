package com.kltn.chatbot.model.entity;

import com.kltn.chatbot.model.enums.RiskLevel;
import com.kltn.chatbot.model.enums.WarningType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho thông tin cảnh báo sinh viên
 *
 * @author Nguyễn Đình Nhật Huy
 */
@Entity
@Table(name = "warnings", indexes = {
    @Index(name = "idx_student_id", columnList = "student_id"),
    @Index(name = "idx_course_id", columnList = "course_id"),
    @Index(name = "idx_risk_level", columnList = "risk_level"),
    @Index(name = "idx_detected_at", columnList = "detected_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private RiskLevel severity = RiskLevel.GREEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "warning_type", nullable = false, length = 20)
    @Builder.Default
    private WarningType warningType = WarningType.GENERAL;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "grade_average")
    private Double gradeAverage;

    @Column(name = "attendance_rate")
    private Double attendanceRate;

    @Column(name = "completion_rate")
    private Double completionRate;

    @Column(name = "last_access_days")
    private Integer lastAccessDays;

    @Column(name = "reasons", columnDefinition = "TEXT")
    private String reasons;

    @Column(name = "is_acknowledged")
    private Boolean isAcknowledged;

    @Column(name = "acknowledged_by")
    private Long acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "is_sent")
    @Builder.Default
    private Boolean isSent = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "is_resolved")
    @Builder.Default
    private Boolean isResolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;
}
