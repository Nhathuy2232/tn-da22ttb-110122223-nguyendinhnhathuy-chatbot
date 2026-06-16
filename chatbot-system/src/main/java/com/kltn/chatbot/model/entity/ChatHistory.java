package com.kltn.chatbot.model.entity;

import com.kltn.chatbot.model.enums.ChatRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho lịch sử chat
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Entity
@Table(name = "chat_history", indexes = {
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_lecturer_id", columnList = "lecturer_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "lecturer_id", nullable = false)
    private Long lecturerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ChatRole role;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "intent", length = 100)
    private String intent;

    @Column(name = "entities", columnDefinition = "TEXT")
    private String entities;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
