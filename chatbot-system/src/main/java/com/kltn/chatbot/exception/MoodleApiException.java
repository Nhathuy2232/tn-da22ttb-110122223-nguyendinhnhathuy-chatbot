package com.kltn.chatbot.exception;

/**
 * Exception khi gọi Moodle API bị lỗi
 * 
 * @author Nguyễn Đình Nhật Huy
 */
public class MoodleApiException extends RuntimeException {

    public MoodleApiException(String message) {
        super(message);
    }

    public MoodleApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
