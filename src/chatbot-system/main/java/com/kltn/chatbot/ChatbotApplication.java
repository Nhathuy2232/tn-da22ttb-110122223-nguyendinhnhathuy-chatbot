package com.kltn.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Application Class for Chatbot Early Warning System
 * 
 * @author Nguyễn Đình Nhật Huy (MSSV: 110122223)
 * @version 1.0.0
 * @since 2026-04-27
 */
@SpringBootApplication
@EnableScheduling
public class ChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotApplication.class, args);
        System.out.println("=================================================");
        System.out.println("Chatbot Early Warning System Started Successfully");
        System.out.println("Swagger UI: http://localhost:8080/api/swagger-ui.html");
        System.out.println("=================================================");
    }
}
