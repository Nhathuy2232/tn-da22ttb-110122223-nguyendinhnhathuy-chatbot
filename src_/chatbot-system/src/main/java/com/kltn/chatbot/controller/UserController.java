package com.kltn.chatbot.controller;

import com.kltn.chatbot.model.entity.User;
import com.kltn.chatbot.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller quản lý người dùng (Sinh viên và Giáo viên)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "API quản lý người dùng")
public class UserController {
    
    private final UserManagementService userManagementService;
    
    @Operation(summary = "Đồng bộ tất cả người dùng từ Moodle")
    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncAllUsers() {
        try {
            userManagementService.syncAllUsersFromMoodle();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Đồng bộ người dùng thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    @Operation(summary = "Lấy danh sách tất cả sinh viên")
    @GetMapping("/students")
    public ResponseEntity<List<User>> getAllStudents() {
        List<User> students = userManagementService.getAllStudents();
        return ResponseEntity.ok(students);
    }
    
    @Operation(summary = "Lấy danh sách tất cả giáo viên")
    @GetMapping("/teachers")
    public ResponseEntity<List<User>> getAllTeachers() {
        List<User> teachers = userManagementService.getAllTeachers();
        return ResponseEntity.ok(teachers);
    }
    
    @Operation(summary = "Tìm sinh viên theo từ khóa")
    @GetMapping("/students/search")
    public ResponseEntity<List<User>> searchStudents(@RequestParam String keyword) {
        List<User> students = userManagementService.searchStudents(keyword);
        return ResponseEntity.ok(students);
    }
    
    @Operation(summary = "Lấy thông tin sinh viên theo mã sinh viên")
    @GetMapping("/students/code/{studentCode}")
    public ResponseEntity<User> getUserByStudentCode(@PathVariable String studentCode) {
        User user = userManagementService.getUserByStudentCode(studentCode);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
    
    @Operation(summary = "Lấy thông tin người dùng theo Moodle ID")
    @GetMapping("/moodle/{moodleUserId}")
    public ResponseEntity<User> getUserByMoodleId(@PathVariable Long moodleUserId) {
        User user = userManagementService.getUserByMoodleId(moodleUserId);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
    
    @Operation(summary = "Thống kê số lượng người dùng")
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getUserStatistics() {
        Map<String, Long> stats = userManagementService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }
}
