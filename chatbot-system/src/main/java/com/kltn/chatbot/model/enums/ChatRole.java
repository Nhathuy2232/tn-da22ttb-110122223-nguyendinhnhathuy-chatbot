package com.kltn.chatbot.model.enums;

/**
 * Enum đại diện cho vai trò trong chat
 * 
 * @author Nguyễn Đình Nhật Huy
 */
public enum ChatRole {
    USER("Người dùng"),
    BOT("Chatbot");

    private final String displayName;

    ChatRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
