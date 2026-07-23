package com.kltn.chatbot.controller;

import com.kltn.chatbot.service.AutomaticRiskMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller để test và trigger Automatic Risk Monitor
 * 
 * @author Nguyễn Đình Nhật Huy - MSSV: 110122223
 */
@RestController
@RequestMapping("/api/risk-monitor")
@RequiredArgsConstructor
@Tag(name = "Risk Monitor", description = "Test & Trigger Automatic Risk Monitoring")
@CrossOrigin(origins = "*")
@Slf4j
public class RiskMonitorController {

    private final AutomaticRiskMonitorService riskMonitorService;

    /**
     * Test endpoint - Trigger risk monitoring manually
     * Không cần đợi 30 phút, chạy ngay lập tức
     */
    @PostMapping("/trigger")
    @Operation(summary = "Trigger risk monitoring manually", 
               description = "Chạy phân tích rủi ro và gửi thông báo ngay lập tức (không đợi scheduled time)")
    public ResponseEntity<Map<String, Object>> triggerRiskMonitoring() {
        log.info("=================================================");
        log.info("MANUAL TRIGGER - Risk monitoring started by admin");
        log.info("=================================================");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Chạy risk monitoring ngay
            riskMonitorService.monitorAndNotifyRedAlerts();
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Risk monitoring completed successfully");
            response.put("durationMs", duration);
            response.put("note", "Check logs for detailed results");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in manual risk monitoring trigger", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Kiểm tra status của risk monitor service
     */
    @GetMapping("/status")
    @Operation(summary = "Check risk monitor status", 
               description = "Kiểm tra xem service có đang hoạt động không")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("serviceActive", true);
        status.put("scheduledInterval", "Every 30 minutes");
        status.put("cronExpression", "0 */30 * * * *");
        status.put("redGradeThreshold", 50.0);
        status.put("redInactiveDays", 14);
        status.put("notificationCooldown", "24 hours");
        
        return ResponseEntity.ok(status);
    }
}
