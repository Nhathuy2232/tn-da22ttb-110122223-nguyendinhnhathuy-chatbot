package com.kltn.chatbot.controller;

import com.kltn.chatbot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Gửi 1 thông báo thủ công
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody Map<String, Object> body) {
        long studentId = ((Number) body.get("studentId")).longValue();
        long courseId = ((Number) body.get("courseId")).longValue();
        String riskLevel = (String) body.getOrDefault("riskLevel", "red");
        String reason = (String) body.getOrDefault("reason", "Vi phạm học vụ");

        int sent = notificationService.notifyViolation(studentId, courseId, riskLevel, reason);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "sentTo", sent,
                "message", "Đã gửi " + sent + " thông báo"
        ));
    }

    /**
     * Chạy batch scan + gửi thông báo tự động
     */
    @PostMapping("/batch")
    public ResponseEntity<?> batchScan() {
        int count = notificationService.sendBatchNotifications();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "notificationsSent", count,
                "message", "Đã gửi " + count + " thông báo tự động"
        ));
    }

    /**
     * Lấy danh sách thông báo của 1 user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserMessages(@PathVariable long userId,
                                              @RequestParam(defaultValue = "20") int limit) {
        // Trả về qua Moodle DB
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "limit", limit,
                "info", "Xem trong Moodle Message drawer"
        ));
    }
}
