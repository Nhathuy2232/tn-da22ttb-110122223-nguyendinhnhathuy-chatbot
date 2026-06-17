package com.kltn.chatbot.event;

import com.kltn.chatbot.model.entity.Warning;
import com.kltn.chatbot.model.enums.RiskLevel;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Event được publish khi phát hiện sinh viên có rủi ro (đặc biệt RED).
 * Listener sẽ nhận event này và gửi thông báo vào Moodle Message drawer.
 */
@Getter
public class RiskDetectedEvent extends ApplicationEvent {

    private final Long warningId;
    private final Long studentId;
    private final String studentName;
    private final String username;
    private final Long courseId;
    private final String courseName;
    private final RiskLevel riskLevel;
    private final Double gradeAverage;
    private final Double attendanceRate;
    private final Double completionRate;
    private final Integer lastAccessDays;
    private final String reasons;
    private final LocalDateTime detectedAt;

    public RiskDetectedEvent(Object source, Warning warning) {
        super(source);
        this.warningId = warning.getId();
        this.studentId = warning.getStudent() != null ? warning.getStudent().getMoodleUserId() : null;
        this.studentName = warning.getStudent() != null
                ? warning.getStudent().getFullName()
                : "Sinh viên";
        this.username = warning.getStudent() != null ? warning.getStudent().getStudentCode() : null;
        this.courseId = warning.getCourse() != null ? warning.getCourse().getMoodleCourseId() : null;
        this.courseName = warning.getCourse() != null ? warning.getCourse().getCourseName() : "Môn học";
        this.riskLevel = warning.getRiskLevel();
        this.gradeAverage = warning.getGradeAverage();
        this.attendanceRate = warning.getAttendanceRate();
        this.completionRate = warning.getCompletionRate();
        this.lastAccessDays = warning.getLastAccessDays();
        this.reasons = warning.getReasons();
        this.detectedAt = LocalDateTime.now();
    }
}
