package com.kltn.chatbot.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho chat request từ frontend
 * Supports both teachers and students
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequestDTO {

    @NotBlank(message = "Message không được để trống")
    private String message;

    // Optional - auto-generated if not provided
    private Long userId;
    
    // Username from Moodle (for role detection)
    private String username;
    
    // User role: ADMIN, LECTURER, STUDENT
    private String role;
    
    // Legacy field for backward compatibility
    private Long lecturerId;

    // Optional - auto-generated from username if not provided
    private String sessionId;
    
    /**
     * Get user ID (supports both userId and lecturerId for backward compatibility)
     */
    public Long getUserId() {
        return userId != null ? userId : lecturerId;
    }
}
