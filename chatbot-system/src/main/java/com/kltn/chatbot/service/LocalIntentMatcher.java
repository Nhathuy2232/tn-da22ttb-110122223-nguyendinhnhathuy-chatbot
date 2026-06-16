package com.kltn.chatbot.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Nhận diện intent cục bộ cho các câu chào hỏi và các câu phổ biến theo 4 quyền.
 */
public final class LocalIntentMatcher {

    private static final Pattern GREET = Pattern.compile(
            "^(xin\\s*chào|chào|hello|hi|hey|alo|chào\\s*bạn|chào\\s*thầy|chào\\s*cô)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern GOODBYE = Pattern.compile(
            "^(tạm\\s*biệt|bye|goodbye|see\\s*you|hẹn\\s*gặp\\s*lại)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern THANK = Pattern.compile(
            "^(cảm\\s*ơn|thank|thanks|camon|cám\\s*ơn)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern HELP = Pattern.compile(
            "(giúp|giup|hướng\\s*dẫn|huong\\s*dan|làm\\s*gì|lam\\s*gi|bạn\\s*làm\\s*gì|ban\\s*lam\\s*gi|help|hỗ\\s*trợ|hỗ trợ)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern ADMIN_THRESHOLD = Pattern.compile(
            "(ngưỡng\\s*cảnh\\s*báo|nguong\\s*canh\\s*bao|mức\\s*đỏ|muc\\s*do|mức\\s*vàng|muc\\s*vang|mức\\s*xanh|muc\\s*xanh|rủi\\s*ro\\s*học\\s*vụ|rui\\s*ro\\s*hoc\\s*vu|cấu\\s*hình|cai\\s*dat|set\\s*điều\\s*kiện|set\\s*dieu\\s*kien)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ADMIN_SYNC = Pattern.compile(
            "(đồng\\s*bộ|dong\\s*bo|sync|cập\\s*nhật\\s*dữ\\s*liệu|cap\\s*nhat\\s*du\\s*lieu|kiểm\\s*tra\\s*kết\\s*nối|kiem\\s*tra\\s*ket\\s*noi|api\\s*moodle|moodle\\s*web\\s*services|redis|postgresql|cache|log\\s*chuyên\\s*cần|log\\s*chuyen\\s*can)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern STUDENT_SELF = Pattern.compile(
            "(điểm\\s*của\\s*tôi|điểm\\s*của\\s*em|kết\\s*quả\\s*học\\s*tập\\s*của\\s*tôi|chuyên\\s*cần\\s*của\\s*tôi|tình\\s*trạng\\s*học\\s*tập\\s*của\\s*tôi|mức\\s*độ\\s*rủi\\s*ro\\s*của\\s*tôi|của\\s*em|của\\s*tôi|xem\\s*điểm|gradebook|bảng\\s*điểm|rủi\\s*ro\\s*học\\s*tập\\s*của\\s*tôi)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern LECTURER_SUBMISSION = Pattern.compile(
            "(chưa\\s*nộp|chua\\s*nop|nộp\\s*bài|nop\\s*bai|assignment|quiz|lab|nhắc\\s*nhở|nhac\\s*nho|tiến\\s*độ\\s*nộp|tien\\s*do\\s*nop|ai\\s*chưa\\s*nộp|chưa\\s*làm)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LECTURER_RISK = Pattern.compile(
            "(môn\\s*này|mon\\s*nay|môn\\s*tôi\\s*đang\\s*dạy|mon\\s*toi\\s*dang\\s*day|nguy\\s*cơ\\s*cao|mức\\s*đỏ|mức\\s*vàng|vắng\\s*quá|vang\\s*qua|thôi\\s*học|thoi\\s*hoc|rủi\\s*ro\\s*môn|rui\\s*ro\\s*mon)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LECTURER_STUDENT_LOOKUP = Pattern.compile(
            "(kiểm\\s*tra\\s*điểm|kiem\\s*tra\\s*diem|tra\\s*cứu\\s*chuyên\\s*cần|tra\\s*cuu\\s*chuyen\\s*can|mssv\\s*\\d{9}|sinh\\s*viên\\s*.+\\s*môn\\s*này|lịch\\s*sử\\s*tương\\s*tác|luong\\s*tuong\\s*tac|điểm\\s*assignment\\s*và\\s*quiz)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern ADVISOR_CLASS_RISK = Pattern.compile(
            "(lớp\\s*cố\\s*vấn|lop\\s*co\\s*van|lớp\\s*chủ\\s*nhiệm|lop\\s*chu\\s*nhiem|cảnh\\s*báo\\s*lớp|canh\\s*bao\\s*lop|sinh\\s*viên\\s*mức\\s*đỏ|sinh\\s*vien\\s*muc\\s*do|ngừng\\s*tương\\s*tác|ngung\\s*tuong\\s*tac|không\\s*online|khong\\s*online|lần\\s*đăng\\s*nhập\\s*cuối|dang\\s*nhap\\s*cuoi|không\\s*đăng\\s*nhập|danh\\s*sach\\s*cảnh\\s*báo|danh\\s*sach\\s*canh\\s*bao)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ADVISOR_INACTIVE = Pattern.compile(
            "(không\\s*online|khong\\s*online|chưa\\s*đăng\\s*nhập|chua\\s*dang\\s*nhap|bỏ\\s*học\\s*trực\\s*tuyến|bo\\s*hoc\\s*truc\\s*tuyen|tần\\s*suất\\s*tương\\s*tác\\s*thấp|tan\\s*suat\\s*tuong\\s*tac\\s*thap|logs\\s*đăng\\s*nhập|dang\\s*nhap\\s*lan\\s*cuoi)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private LocalIntentMatcher() {
    }

    public static Optional<Map<String, Object>> match(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }

        String normalized = message.trim().toLowerCase(Locale.forLanguageTag("vi"));

        if (GREET.matcher(normalized).find()) {
            return Optional.of(result("GREET", 1.0));
        }
        if (GOODBYE.matcher(normalized).find()) {
            return Optional.of(result("GOODBYE", 1.0));
        }
        if (THANK.matcher(normalized).find()) {
            return Optional.of(result("THANK", 1.0));
        }
        if (HELP.matcher(normalized).find()) {
            return Optional.of(result("HELP", 1.0));
        }
        if (STUDENT_SELF.matcher(normalized).find()) {
            if (normalized.contains("cải thiện") || normalized.contains("cai thien") || normalized.contains("gợi ý") || normalized.contains("goi y")) {
                return Optional.of(result("GET_IMPROVEMENT_SUGGESTIONS", 0.94));
            }
            if (normalized.contains("cảnh báo") || normalized.contains("rủi ro") || normalized.contains("rui ro") || normalized.contains("mức") || normalized.contains("nguy cơ")) {
                return Optional.of(result("CHECK_OWN_RISK_STATUS", 0.95));
            }
            return Optional.of(result("LIST_OWN_GRADES", 0.96));
        }
        if (ADMIN_THRESHOLD.matcher(normalized).find()) {
            return Optional.of(result("CONFIG_WARNING_THRESHOLD", 0.95));
        }
        if (ADMIN_SYNC.matcher(normalized).find()) {
            return Optional.of(result("TRIGGER_MOODLE_SYNC", 0.95));
        }
        if (ADVISOR_INACTIVE.matcher(normalized).find()) {
            return Optional.of(result("FIND_INACTIVE_STUDENTS", 0.93));
        }
        if (ADVISOR_CLASS_RISK.matcher(normalized).find()) {
            return Optional.of(result("VIEW_CLASS_RISK_SUMMARY", 0.93));
        }
        if (LECTURER_SUBMISSION.matcher(normalized).find()) {
            return Optional.of(result("CHECK_SUBMISSIONS_AND_REMIND", 0.92));
        }
        if (LECTURER_RISK.matcher(normalized).find()) {
            return Optional.of(result("FILTER_COURSE_RISK", 0.92));
        }
        if (LECTURER_STUDENT_LOOKUP.matcher(normalized).find()) {
            return Optional.of(result("QUERY_STUDENT_INFO_NLP", 0.92));
        }

        return Optional.empty();
    }

    private static Map<String, Object> result(String intent, double confidence) {
        return Map.of(
                "intent", intent,
                "confidence", confidence,
                "entities", Map.of(),
                "response_text", ""
        );
    }
}
