package com.kltn.chatbot.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Load .env file into Spring Environment
 * Đọc file .env và thêm vào Spring Environment Properties
 * 
 * @author Nguyễn Đình Nhật Huy - MSSV: 110122223
 */
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            // Load .env file from project root
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // Don't fail if .env doesn't exist
                    .load();
            
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            Map<String, Object> dotenvProperties = new HashMap<>();
            
            // Add all .env entries to Spring Environment
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Only add if not already set by system environment
                if (System.getenv(key) == null) {
                    dotenvProperties.put(key, value);
                    System.out.println("✓ Loaded from .env: " + key + " = " + (key.contains("KEY") || key.contains("PASSWORD") ? "***" : value));
                }
            });
            
            environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", dotenvProperties));
            
        } catch (Exception e) {
            System.err.println("⚠️ Could not load .env file: " + e.getMessage());
        }
    }
}
