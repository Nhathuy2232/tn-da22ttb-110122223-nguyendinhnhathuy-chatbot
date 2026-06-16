package com.kltn.chatbot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO cho chat response trả về frontend
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponseDTO {

    private String reply;
    
    /**
     * Rich data có thể là table, list, chart data
     * Format: { "type": "table", "data": [...] }
     */
    private Map<String, Object> richData;
    
    private String intent;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    private Long responseTimeMs;
}
