package com.kltn.chatbot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration cho Swagger/OpenAPI 3
 * Swagger UI: http://localhost:8080/api/swagger-ui.html
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chatbot Early Warning System API")
                        .version("1.0.0")
                        .description("API Documentation cho Hệ thống Chatbot hỗ trợ giảng viên theo dõi tiến độ học tập và cảnh báo sinh viên có nguy cơ thôi học")
                        .contact(new Contact()
                                .name("Nguyễn Đình Nhật Huy")
                                .email("110122223@student.vlute.edu.vn")
                                .url("https://github.com/nhathuy"))
                        .license(new License()
                                .name("Đồ án Tốt nghiệp")
                                .url("https://vlute.edu.vn")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token")));
    }
}
