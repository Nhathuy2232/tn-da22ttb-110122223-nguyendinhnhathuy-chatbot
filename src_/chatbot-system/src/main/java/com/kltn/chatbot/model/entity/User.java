package com.kltn.chatbot.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity quản lý người dùng (Giáo viên và Sinh viên)
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "moodle_user_id", unique = true, nullable = false)
    private Long moodleUserId;
    
    @Column(name = "username", unique = true, nullable = false, length = 100)
    private String username;
    
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;
    
    @Column(name = "email", unique = true, nullable = false, length = 200)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType;
    
    @Column(name = "student_code", length = 50)
    private String studentCode;
    
    @Column(name = "department", length = 200)
    private String department;
    
    @Column(name = "institution", length = 200)
    private String institution;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "country", length = 10)
    private String country;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum UserType {
        STUDENT,
        TEACHER,
        ADMIN
    }
}
