package com.kltn.chatbot.exception;

/**
 * Exception khi không tìm thấy resource (Student, Course, etc.)
 * 
 * @author Nguyễn Đình Nhật Huy
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s không tìm thấy với %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
