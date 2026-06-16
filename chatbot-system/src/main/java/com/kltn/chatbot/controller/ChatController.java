package com.kltn.chatbot.controller;

import com.kltn.chatbot.model.dto.ApiResponse;
import com.kltn.chatbot.model.dto.ChatRequestDTO;
import com.kltn.chatbot.model.dto.ChatResponseDTO;
import com.kltn.chatbot.model.entity.ChatHistory;
import com.kltn.chatbot.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý chat requests với Gemini API
 * Version 3.0 - Gemini Integration
 * 
 * @author Nguyễn Đình Nhật Huy - MSSV: 110122223
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chatbot APIs with Gemini Integration")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    @Operation(summary = "Send chat message", description = "Process user message with Gemini API and Moodle data")
    public ResponseEntity<ApiResponse<ChatResponseDTO>> sendMessage(
            @Valid @RequestBody ChatRequestDTO request) {
        
        ChatResponseDTO response = chatbotService.handleUserMessage(request);
        return ResponseEntity.ok(ApiResponse.success("Message processed successfully", response));
    }

    @GetMapping("/history/{username}")
    @Operation(summary = "Get chat history", description = "Retrieve chat history for a user")
    public ResponseEntity<ApiResponse<List<ChatHistory>>> getChatHistory(
            @PathVariable String username) {
        
        // TODO: Implement getChatHistory in ChatbotService if needed
        return ResponseEntity.ok(ApiResponse.success("Chat history", null));
    }
}
