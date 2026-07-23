package com.kltn.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

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
        SpringApplication app = new SpringApplication(ChatbotApplication.class);
        app.setAdditionalProfiles(resolveProfile(args));
        app.run(args);
        System.out.println("=================================================");
        System.out.println("Chatbot Early Warning System Started Successfully");
        System.out.println("Swagger UI: http://localhost:8081/api/swagger-ui.html");
        System.out.println("=================================================");
    }

    private static String resolveProfile(String[] args) {
        return Arrays.stream(args)
                .filter(arg -> arg.startsWith("--spring.profiles.active="))
                .map(arg -> arg.substring("--spring.profiles.active=".length()))
                .findFirst()
                .orElse("default");
    }
}
