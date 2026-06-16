package com.kltn.chatbot.controller;

import com.kltn.chatbot.model.dto.ApiResponse;
import com.kltn.chatbot.model.entity.Warning;
import com.kltn.chatbot.model.enums.RiskLevel;
import com.kltn.chatbot.service.WarningAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller xử lý warning requests
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@RestController
@RequestMapping("/api/warnings")
@RequiredArgsConstructor
@Tag(name = "Warnings", description = "Warning Management APIs")
@CrossOrigin(origins = "*")
public class WarningController {

    private final WarningAnalysisService warningAnalysisService;

    @GetMapping
    @Operation(summary = "Get all warnings", description = "Retrieve all warnings")
    public ResponseEntity<ApiResponse<List<Warning>>> getAllWarnings() {
        List<Warning> warnings = warningAnalysisService.getUnacknowledgedWarnings();
        return ResponseEntity.ok(ApiResponse.success(warnings));
    }

    @GetMapping("/red")
    @Operation(summary = "Get RED warnings", description = "Retrieve all RED level warnings")
    public ResponseEntity<ApiResponse<List<Warning>>> getRedWarnings() {
        List<Warning> warnings = warningAnalysisService.getUnacknowledgedRedWarnings();
        return ResponseEntity.ok(ApiResponse.success(warnings));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard data", description = "Retrieve dashboard statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Long greenCount = warningAnalysisService.countWarningsByRiskLevel(RiskLevel.GREEN);
        Long yellowCount = warningAnalysisService.countWarningsByRiskLevel(RiskLevel.YELLOW);
        Long redCount = warningAnalysisService.countWarningsByRiskLevel(RiskLevel.RED);
        
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("greenCount", greenCount);
        dashboard.put("yellowCount", yellowCount);
        dashboard.put("redCount", redCount);
        dashboard.put("totalCount", greenCount + yellowCount + redCount);
        
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @PutMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge warning", description = "Mark warning as acknowledged")
    public ResponseEntity<ApiResponse<Warning>> acknowledgeWarning(
            @PathVariable Long id,
            @RequestParam Long lecturerId) {
        
        Warning warning = warningAnalysisService.acknowledgeWarning(id, lecturerId);
        return ResponseEntity.ok(ApiResponse.success("Warning acknowledged successfully", warning));
    }
}
