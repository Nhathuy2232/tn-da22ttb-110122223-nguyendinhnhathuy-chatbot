package com.kltn.chatbot.model.enums;

/**
 * Enum đại diện cho mức độ nguy cơ của sinh viên
 * 
 * GREEN: Sinh viên hoàn thành > 80% yêu cầu VÀ điểm TB ≥ 5.0 VÀ vắng < 20%
 * YELLOW: Điểm TB < 5.0 HOẶC vắng ≥ 20% số buổi
 * RED: (Điểm TB < 5.0 VÀ vắng ≥ 20%) HOẶC không truy cập Moodle > 14 ngày
 * 
 * @author Nguyễn Đình Nhật Huy
 */
public enum RiskLevel {
    GREEN("An toàn", "Sinh viên đang học tập tốt"),
    YELLOW("Cảnh báo", "Sinh viên cần theo dõi"),
    RED("Nguy cơ cao", "Sinh viên có nguy cơ thôi học");

    private final String displayName;
    private final String description;

    RiskLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
