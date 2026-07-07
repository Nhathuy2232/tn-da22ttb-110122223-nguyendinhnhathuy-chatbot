package com.kltn.chatbot.service;

import com.kltn.chatbot.model.entity.Warning;
import com.kltn.chatbot.model.enums.RiskLevel;
import com.kltn.chatbot.model.enums.WarningType;
import com.kltn.chatbot.repository.WarningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Automatic Risk Monitor Service
 * Tự động phân tích rủi ro và gửi thông báo cảnh báo đỏ
 * 
 * Chức năng:
 * - Chạy định kỳ mỗi 30 phút
 * - Phân tích tất cả sinh viên đang học
 * - Phát hiện sinh viên có cảnh báo ĐỎ (nguy cơ cao)
 * - Gửi thông báo tự động cho:
 *   + Sinh viên đó
 *   + Giáo viên phụ trách môn học
 *   + Cố vấn học tập của sinh viên
 * 
 * @author Nguyễn Đình Nhật Huy - MSSV: 110122223
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutomaticRiskMonitorService {

    private final MoodleDirectQueryService directQuery;
    private final WarningRepository warningRepository;
    private final WarningWriteService warningWriteService;
    private final NotificationService notificationService;
    
    // Configuration từ application.yml
    private static final double RED_GRADE_THRESHOLD = 50.0;  // Điểm TB < 50 = ĐỎ
    private static final int RED_INACTIVE_DAYS = 14;          // Không online > 14 ngày = ĐỎ
    
    /**
     * Scheduled task chạy mỗi 30 phút
     * Cron expression: giây 0, mỗi 30 phút, mọi giờ, mọi ngày
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void monitorAndNotifyRedAlerts() {
        log.info("=================================================");
        log.info("AUTOMATIC RISK MONITOR - Starting...");
        log.info("Time: {}", LocalDateTime.now());
        log.info("=================================================");
        
        try {
            // 1. Lấy danh sách tất cả sinh viên
            List<Map<String, Object>> allUsers = directQuery.findAllActiveStudents();
            log.info("Total active students: {}", allUsers.size());
            
            int redCount = 0;
            int notificationsSent = 0;
            
            // 2. Phân tích từng sinh viên
            for (Map<String, Object> user : allUsers) {
                try {
                    long userId = ((Number) user.get("id")).longValue();
                    String username = (String) user.get("username");
                    String fullName = (String) user.get("fullname");
                    String email = (String) user.get("email");
                    
                    // 3. Kiểm tra rủi ro theo từng môn học
                    List<Map<String, Object>> courses = directQuery.findEnrolledCourses(userId);
                    
                    for (Map<String, Object> course : courses) {
                        long courseId = ((Number) course.get("id")).longValue();
                        String courseName = (String) course.get("fullname");
                        
                        // Lấy điểm TB và ngày online cuối
                        Map<String, Object> gradeInfo = directQuery.getCourseAverageGrade(courseId, userId);
                        double avgGrade = ((Number) gradeInfo.get("avgGrade")).doubleValue();
                        boolean hasGrades = (Boolean) gradeInfo.get("hasGrades");
                        long daysSinceAccess = directQuery.getDaysSinceAccess(userId);
                        
                        // 4. Xác định mức độ rủi ro
                        RiskLevel riskLevel = determineRiskLevel(avgGrade, hasGrades, daysSinceAccess);
                        
                        // 5. Nếu là ĐỎ -> Tạo warning và gửi thông báo
                        if (riskLevel == RiskLevel.RED) {
                            redCount++;
                            
                            try {
                                // Kiểm tra xem đã có warning chưa (trong vòng 24h)
                                boolean alreadyWarned = checkRecentWarning(userId, courseId);
                                
                                if (!alreadyWarned) {
                                    String reason = buildReasonMessage(avgGrade, hasGrades, daysSinceAccess);
                                    Warning warning = createWarning(userId, courseId, avgGrade, daysSinceAccess, reason);
                                    
                                    // Save trong transaction riêng
                                    warningWriteService.save(warning);
                                    
                                    int sent = notificationService.notifyViolation(
                                        userId, 
                                        courseId, 
                                        "red", 
                                        reason
                                    );
                                    notificationsSent += sent;
                                    
                                    log.warn("🔴 RED ALERT: {} ({}) - Course: {} - Reason: {} - Notifications sent: {}", 
                                        fullName, username, courseName, reason, sent);
                                }
                            } catch (Exception warningEx) {
                                log.error("Failed to create warning for user {} in course {}: {}", 
                                    userId, courseId, warningEx.getMessage());
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("Error analyzing user {}: {}", user.get("username"), e.getMessage());
                }
            }
            
            log.info("=================================================");
            log.info("AUTOMATIC RISK MONITOR - Completed");
            log.info("Red alerts found: {}", redCount);
            log.info("Notifications sent: {}", notificationsSent);
            log.info("=================================================");
            
        } catch (Exception e) {
            log.error("Fatal error in automatic risk monitor: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Xác định mức độ rủi ro dựa trên điểm và chuyên cần
     */
    private RiskLevel determineRiskLevel(double avgGrade, boolean hasGrades, long daysSinceAccess) {
        // ĐỎ: Điểm TB < 50 HOẶC không online > 14 ngày
        if ((hasGrades && avgGrade < RED_GRADE_THRESHOLD) || daysSinceAccess > RED_INACTIVE_DAYS) {
            return RiskLevel.RED;
        }
        
        // VÀNG: Điểm TB < 70 HOẶC không online > 7 ngày
        if ((hasGrades && avgGrade < 70) || daysSinceAccess > 7) {
            return RiskLevel.YELLOW;
        }
        
        // XANH: Ổn định
        return RiskLevel.GREEN;
    }
    
    /**
     * Xây dựng thông điệp lý do cảnh báo
     */
    private String buildReasonMessage(double avgGrade, boolean hasGrades, long daysSinceAccess) {
        StringBuilder reason = new StringBuilder();
        
        if (hasGrades && avgGrade < RED_GRADE_THRESHOLD) {
            reason.append(String.format("Điểm TB thấp (%.2f/100). ", avgGrade));
        }
        
        if (daysSinceAccess > RED_INACTIVE_DAYS) {
            reason.append(String.format("Không online %d ngày. ", daysSinceAccess));
        }
        
        if (reason.length() == 0) {
            reason.append("Cảnh báo nguy cơ thôi học cao.");
        }
        
        return reason.toString().trim();
    }
    
    /**
     * Tạo Warning entity với student ID và course ID trực tiếp
     */
    private Warning createWarning(long userId, long courseId, double avgGrade, long daysSinceAccess, String reason) {
        return Warning.builder()
                .studentId(userId)
                .courseId(courseId)
                .riskLevel(RiskLevel.RED)
                .severity(RiskLevel.RED)
                .warningType(WarningType.GENERAL)
                .message(reason)
                .gradeAverage(avgGrade)
                .attendanceRate(0.0)
                .lastAccessDays((int) daysSinceAccess)
                .reasons(reason)
                .isAcknowledged(false)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                // Don't set detectedAt - let @CreationTimestamp handle it
                // Don't set createdAt/updatedAt - let annotations handle them
                .build();
    }
    
    /**
     * Kiểm tra xem đã có warning gần đây chưa (trong 24h)
     * Tránh spam thông báo
     */
    private boolean checkRecentWarning(long userId, long courseId) {
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        List<Warning> recentWarnings = warningRepository.findByStudentIdAndCourseIdAndDetectedAtAfter(
            userId, courseId, last24h
        );
        return !recentWarnings.isEmpty();
    }
}
