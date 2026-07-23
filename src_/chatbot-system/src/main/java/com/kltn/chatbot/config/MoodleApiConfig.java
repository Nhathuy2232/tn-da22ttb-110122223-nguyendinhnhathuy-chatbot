package com.kltn.chatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration cho Moodle API
 * Đọc từ application.yml section moodle.api
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Configuration
@ConfigurationProperties(prefix = "moodle.api")
@Data
public class MoodleApiConfig {

    /**
     * Base URL của Moodle instance
     * VD: http://localhost/moodle
     */
    private String baseUrl;

    /**
     * Token xác thực Moodle Web Services
     */
    private String token;

    /**
     * Prefix cho web service functions
     * Default: core_
     */
    private String wsfunctionPrefix = "core_";

    /**
     * Timeout cho HTTP requests (milliseconds)
     * Default: 30000 (30 seconds)
     */
    private Integer timeout = 30000;

    /**
     * Số lần retry khi API call fail
     * Default: 3
     */
    private Integer maxRetries = 3;

    /**
     * Delay giữa các lần retry (milliseconds)
     * Default: 1000 (1 second)
     */
    private Integer retryDelay = 1000;

    /**
     * Lấy full URL của Moodle Web Service endpoint
     */
    public String getWebServiceUrl() {
        return baseUrl + "/webservice/rest/server.php";
    }
}
