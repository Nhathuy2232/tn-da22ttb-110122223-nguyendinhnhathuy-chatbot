package com.kltn.chatbot.controller;

import com.kltn.chatbot.model.dto.ApiResponse;
import com.kltn.chatbot.service.DataSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller để trigger manual sync từ Moodle
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sync", description = "APIs để đồng bộ dữ liệu từ Moodle")
public class SyncController {

    private final DataSyncService dataSyncService;

    /**
     * Trigger manual sync từ Moodle
     * Sử dụng khi muốn sync ngay lập tức thay vì đợi scheduled task
     * 
     * @return ApiResponse với message thành công
     */
    @PostMapping("/manual")
    @Operation(
        summary = "Đồng bộ TOÀN BỘ dữ liệu thủ công",
        description = "Đồng bộ users, courses, enrollments, grades, và warnings từ Moodle"
    )
    public ResponseEntity<ApiResponse<String>> manualSync() {
        log.info("=== FULL Manual sync triggered via API ===");
        
        try {
            dataSyncService.triggerManualSync();
            return ResponseEntity.ok(
                ApiResponse.success("✓ Đồng bộ TOÀN BỘ dữ liệu từ Moodle thành công! " +
                    "Bao gồm: Users, Courses, Enrollments, Grade Items, Student Grades, Warnings")
            );
        } catch (Exception e) {
            log.error("Error during manual sync", e);
            return ResponseEntity.ok(
                ApiResponse.error("✗ Lỗi khi đồng bộ dữ liệu: " + e.getMessage())
            );
        }
    }
    
    @PostMapping("/test-connection")
    @Operation(
        summary = "Test kết nối Moodle API",
        description = "Kiểm tra token và kết nối tới Moodle có hoạt động không"
    )
    public ResponseEntity<ApiResponse<String>> testConnection() {
        log.info("Testing Moodle API connection...");
        
        try {
            com.fasterxml.jackson.databind.JsonNode siteInfo = 
                dataSyncService.getMoodleApiService().getSiteInfo();
            
            String siteName = siteInfo.get("sitename").asText();
            String moodleVersion = siteInfo.get("release").asText();
            
            return ResponseEntity.ok(
                ApiResponse.success(String.format(
                    "✓ Kết nối Moodle thành công!\nSite: %s\nVersion: %s", 
                    siteName, moodleVersion))
            );
        } catch (Exception e) {
            log.error("Moodle connection test failed", e);
            return ResponseEntity.ok(
                ApiResponse.error("✗ Không thể kết nối Moodle: " + e.getMessage() + 
                    "\nKiểm tra token và web services configuration!")
            );
        }
    }
}
