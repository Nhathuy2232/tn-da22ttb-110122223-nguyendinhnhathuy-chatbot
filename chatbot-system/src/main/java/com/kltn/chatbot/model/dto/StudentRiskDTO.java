package com.kltn.chatbot.model.dto;

import com.kltn.chatbot.model.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho thông tin nguy cơ của sinh viên
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRiskDTO {

    private Long studentId;
    private String studentName;
    private String studentCode;
    private String email;
    
    private Long courseId;
    private String courseName;
    
    private RiskLevel riskLevel;
    private String riskLevelDisplay;
    
    private Double gradeAverage;
    private Double attendanceRate;
    private Double completionRate;
    private Integer lastAccessDays;
    
    private List<String> reasons;
    
    private LocalDateTime detectedAt;
    private LocalDateTime lastAccessTime;
}
