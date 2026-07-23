package com.kltn.chatbot.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalIntentMatcher {

    private static final Pattern GREET = Pattern.compile(
            "\\b(xin\\s*chào|chào|hello|hi|hey|alo|chào\\s*bạn|chào\\s*thầy|chào\\s*cô|chào\\s*buổi|good\\s*morning|good\\s*afternoon|good\\s*evening)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern GOODBYE = Pattern.compile(
            "\\b(tạm\\s*biệt|bye|goodbye|see\\s*you|hẹn\\s*gặp\\s*lại|thôi\\s*nhé|chào\\s*tạm\\s*biệt|bai\\s*bai|bai\\s*nha|ngủ\\s*ngon|hẹn\\s*gặp)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern THANK = Pattern.compile(
            "\\b(cảm\\s*ơn|thank|thanks|camon|cám\\s*ơn|cảm\\s*ơn\\s*bạn|cảm\\s*ơn\\s*nhiều|cảm\\s*ơn\\s*nha|cám\\s*ơn\\s*nhiều|thank\\s*you|thanks\\s*a\\s*lot|cảm\\s*ơn\\s*em|cảm\\s*ơn\\s*ạ)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern HELP = Pattern.compile(
            "(?:^|\\b)(giúp|giup|hướng\\s*dẫn|huong\\s*dan|làm\\s*gì|lam\\s*gi|bạn\\s*làm\\s*gì|ban\\s*lam\\s*gi|help|hỗ\\s*trợ|trợ\\s*giúp|có\\s*thể\\s*hỏi\\s*gì|cho\\s*tôi\\s*biết|bạn\\s*có\\s*thể\\s*làm\\s*gì|bạn\\s*làm\\s*được\\s*gì|bạn\\s*biết\\s*làm\\s*gì|em\\s*cần\\s*gì|bạn\\s*hỗ\\s*trợ\\s*gì|bạn\\s*có\\s*thể\\s*giúp|bạn\\s*giúp\\s*gì|bạn\\s*giúp\\s*được\\s*gì|giúp\\s*gì|giúp\\s*được\\s*gì)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern ADMIN_THRESHOLD = Pattern.compile(
            "(ngưỡng\\s*cảnh\\s*báo|nguong\\s*canh\\s*bao|mức\\s*đỏ|muc\\s*do|mức\\s*vàng|muc\\s*vang|mức\\s*xanh|muc\\s*xanh|cấu\\s*hình|cai\\s*hinh|set\\s*điều\\s*kiện|set\\s*dieu\\s*kien|thay\\s*đổi\\s*quy\\s*định|thay\\s*doi\\s*quy\\s*dinh|cài\\s*đặt\\s*lại|cai\\s*dat\\s*la|cập\\s*nhật\\s*tham\\s*số|cap\\s*nhat\\s*tham\\s*so|tham\\s*số\\s*tính\\s*toán|tham\\s*so\\s*tinh\\s*toan|phân\\s*loại\\s*học\\s*vụ|phan\\s*loai\\s*hoc\\s*vu|quy\\s*định\\s*học\\s*vụ|quy\\s*dinh\\s*hoc\\s*vu)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    // Pattern: phủ định admin nếu câu có "sinh viên" (đó là filter LECTURER/ADVISER)
    private static final Pattern ADMIN_EXCLUDE = Pattern.compile(
            "(sinh\\s*viên|sinhvien|môn\\s|năm\\s|học\\s*kỳ)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ADMIN_SYNC = Pattern.compile(
            "(đồng\\s*bộ|dong\\s*bo|^sync$|cập\\s*nhật\\s*dữ\\s*liệu|cap\\s*nhat\\s*du\\s*lieu|task\\s*đồng\\s*bộ|task\\s*dong\\s*bo|chạy\\s*task|chay\\s*task|đồng\\s*bộ\\s*ngay|chạy\\s*sync|redis|postgresql|cache|log\\s*chuyên\\s*cần|log\\s*chuyen\\s*can|lịch\\s*sử\\s*sync|lich\\s*su\\s*sync|ping\\s*kết\\s*nối|ping\\s*ket\\s*noi|ping\\s*database|ping\\s*cache)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ADMIN_API_STATUS = Pattern.compile(
            "(trạng\\s*thái\\s*kết\\s*nối|trang\\s*thai\\s*ket\\s*noi|trạng\\s*thái\\s*api|trang\\s*thai\\s*api|tình\\s*trạng\\s*api|tinh\\s*trang\\s*api|kiểm\\s*tra\\s*kết\\s*nối|kiem\\s*tra\\s*ket\\s*noi|kiểm\\s*tra\\s*api|kiem\\s*tra\\s*api|test\\s*api|api\\s*moodle|moodle\\s*web\\s*services|token\\s*moodle|web\\s*services)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ADMIN_STATS = Pattern.compile(
            "(thống\\s*kê\\s*hệ\\s*thống|thong\\s*ke\\s*he\\s*thong|tổng\\s*quan\\s*hệ\\s*thống|tong\\s*quan\\s*he\\s*thong|system\\s*stats|admin\\s*stats|stats\\s*server)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern ADVISER_CLASS_RISK = Pattern.compile(
            "(lớp\\s*cố\\s*vấn|lop\\s*co\\s*van|lớp\\s*chủ\\s*nhiệm|lop\\s*chu\\s*nhiem|lớp\\s*sinh\\s*hoạt|lop\\s*sinh\\s*hoat|cảnh\\s*báo\\s*lớp|canh\\s*bao\\s*lop|sinh\\s*viên\\s*mức\\s*đỏ\\s*của\\s*lớp|sinh\\s*vien\\s*muc\\s*do\\s*cua\\s*lop|tình\\s*hình\\s*học\\s*vụ|tinh\\s*hinh\\s*hoc\\s*vu|thống\\s*kê\\s*tổng\\s*quan|thong\\s*ke\\s*tong\\s*quan|danh\\s*sách\\s*cảnh\\s*báo|danh\\s*sach\\s*canh\\s*bao|ngừng\\s*tương\\s*tác|ngung\\s*tuong\\s*tac|trích\\s*xuất\\s*sinh\\s*viên|trich\\s*xuat\\s*sinh\\s*vien|tải\\s*danh\\s*sách\\s*cảnh\\s*báo|tai\\s*danh\\s*sach\\s*canh\\s*bao|sinh\\s*viên\\s*nguy\\s*cơ\\s*lớp|sinh\\s*vien\\s*nguy\\s*co\\s*lop|sa\\s*sút|sa\\s*su|tổng\\s*quan\\s*lớp|tong\\s*quan\\s*lop|thống\\s*kê\\s*tình\\s*hình|thong\\s*ke\\s*tinh\\s*hinh|học\\s*lực\\s*sa\\s*sút|sinh\\s*viên\\s*bị\\s*cảnh\\s*báo|sinh\\s*vien\\s*bi\\s*canh\\s*bao)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ADVISER_INACTIVE = Pattern.compile(
            "(không\\s*online|khong\\s*online|chưa\\s*đăng\\s*nhập|chua\\s*dang\\s*nhap|bỏ\\s*học\\s*trực\\s*tuyến|bo\\s*hoc\\s*truc\\s*tuyen|tần\\s*suất\\s*tương\\s*tác\\s*thấp|tan\\s*suat\\s*tuong\\s*tac\\s*thap|logs\\s*đăng\\s*nhập|dang\\s*nhap\\s*lan\\s*cuoi|lần\\s*đăng\\s*nhập\\s*cuối|không\\s*online\\s*trên\\s*2\\s*tuần|khong\\s*online\\s*tren\\s*2\\s*tuan|hơn\\s*15\\s*ngày|hon\\s*15\\s*ngay|trên\\s*\\d+\\s*ngày|trên\\s*\\d+\\s*tuần)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern STUDENT_GRADES = Pattern.compile(
            "(xem\\s*điểm|xem\\s*bảng\\s*điểm|liệt\\s*kê\\s*điểm|liet\\s*ke\\s*diem|gradebook|môn\\s*.+\\s*của\\s*em|môn\\s*.+\\s*của\\s*tôi|điểm\\s*môn|diem\\s*mon|bảng\\s*điểm|bang\\s*diem|điểm\\s*tổng\\s*kết|điểm\\s*tạm\\s*thời|thiếu\\s*cột\\s*điểm)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern STUDENT_RISK = Pattern.compile(
            "(cảnh\\s*báo\\s*học\\s*vụ|canh\\s*bao\\s*hoc\\s*vu|rủi\\s*ro\\s*học\\s*tập|rui\\s*ro\\s*hoc\\s*tap|mức\\s*độ\\s*rủi\\s*ro|muc\\s*do\\s*rui\\s*ro|vắng\\s*quá\\s*20|vang\\s*qua\\s*20|bị\\s*cấm\\s*thi|bi\\s*cam\\s*thi|chuyên\\s*cần|chuyen\\s*can|bị\\s*cảnh\\s*báo|bi\\s*canh\\s*bao|mức\\s*nào|muc\\s*nao|tỷ\\s*lệ\\s*chuyên\\s*cần|tình\\s*trạng\\s*học\\s*tập|tinh\\s*trang\\s*hoc\\s*tap)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern STUDENT_IMPROVE = Pattern.compile(
            "(cải\\s*thiện|cai\\s*thien|nâng\\s*điểm|nang\\s*diem|gợi\\s*ý|goi\\s*y|lộ\\s*trình|lo\\s*trinh|bù\\s*điểm|bu\\s*diem|quiz\\s*bù|quiz\\s*bu|bài\\s*tập\\s*bù|bai\\s*tap\\s*bu|tư\\s*vấn\\s*lộ\\s*trình|môn\\s*yếu|mon\\s*yeu|sửa\\s*đổi|hướng\\s*cải\\s*thiện|huong\\s*cai\\s*thien)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern STUDENT_SELF = Pattern.compile(
            "(điểm\\s*của\\s*tôi|điểm\\s*của\\s*em|kết\\s*quả\\s*học\\s*tập\\s*của\\s*tôi|chuyên\\s*cần\\s*của\\s*tôi|tình\\s*trạng\\s*học\\s*tập\\s*của\\s*tôi|mức\\s*độ\\s*rủi\\s*ro\\s*của\\s*tôi|của\\s*em|của\\s*tôi|xem\\s*điểm|gradebook|bảng\\s*điểm|rủi\\s*ro\\s*học\\s*tập\\s*của\\s*tôi)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern LECTURER_SUBMISSION = Pattern.compile(
            "(chưa\\s*nộp|chua\\s*nop|nộp\\s*bài|nop\\s*bai|assignment|quiz|lab|nhắc\\s*nhở|nhac\\s*nho|tiến\\s*độ\\s*nộp|tien\\s*do\\s*nop|ai\\s*chưa\\s*nộp|chưa\\s*làm|nộp\\s*muộn|nop\\s*muon)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LECTURER_RISK = Pattern.compile(
            "(môn\\s+này|mon\\s+nay|môn\\s+tôi\\s*đang\\s*dạy|mon\\s*toi\\s*dang\\s*day|nguy\\s*cơ\\s*cao|mức\\s*đỏ|mức\\s*vàng|thôi\\s*học|thoi\\s*hoc|rủi\\s*ro\\s*môn|rui\\s*ro\\s*mon|xuất\\s*danh\\s*sách\\s*rủi\\s*ro|xuat\\s*danh\\s*sach\\s*rui\\s*ro|hiện\\s*tại|bao\\s*nhiêu\\s*sinh\\s*viên\\s*mức)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LECTURER_RISK_EXCLUDE = Pattern.compile(
            "(của\\s*lớp|cua\\s*lop|trong\\s*lớp|lớp\\s*sinh\\s*hoạt|lớp\\s*cố\\s*vấn|lớp\\s*chủ\\s*nhiệm)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LECTURER_STUDENT_LOOKUP = Pattern.compile(
            "(kiểm\\s*tra\\s*điểm|kiem\\s*tra\\s*diem|tra\\s*cứu\\s*chuyên\\s*cần|tra\\s*cuu\\s*chuyen\\s*can|mssv\\s*\\d{9}|sinh\\s*viên\\s*.+\\s*môn\\s*này|điểm\\s*assignment\\s*và\\s*quiz|đủ\\s*điều\\s*kiện\\s*thi|du\\s*dieu\\s*kien\\s*thi|nghỉ\\s*học\\s*bao\\s*nhiêu\\s*buổi|nghi\\s*hoc\\s*bao\\s*nhieu\\s*buoi|đi\\s*học\\s*thế\\s*nào|di\\s*hoc\\s*the\\s*nao)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern MSSV_PATTERN = Pattern.compile("\\b(11\\d{7})\\b");
    private static final Pattern CLASS_CODE_PATTERN = Pattern.compile(
            "(DA\\d{2}[A-Za-z]{2,3}|[A-Z]{2,3}\\d{2,4}|K\\d{2}|HK\\d)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern RISK_LEVEL_PATTERN = Pattern.compile(
            "(?:mức\\s*|level\\s*|rủi\\s*ro\\s*|nguy\\s*cơ\\s*)?(đỏ|vàng|xanh|red|yellow|green|cao|trung\\s*bình|thấp)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    // Extract tên môn học từ câu: "Môn X", "môn X", "trong môn X", "của môn X", hoặc short name
    private static final Pattern COURSE_NAME_PATTERN = Pattern.compile(
            "(?:môn\\s+|mon\\s+|m\\s+)?(java|web|cơ\\s*sở\\s*dữ\\s*liệu|co\\s*so\\s*du\\s*lieu|csdl|trí\\s*tuệ\\s*nhân\\s*tạo|tri\\s*tue\\s*nhan\\s*tao|ai|mạng\\s*máy\\s*tính|mang\\s*may\\s*tinh|vi\\s*tích\\s*phân|vi\\s*tich\\s*phan|it\\d+|IT\\d+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    // Map tên rút gọn sang tên đầy đủ để query Moodle
    private static final java.util.Map<String, String> COURSE_NAME_MAP = new java.util.HashMap<>();
    static {
        COURSE_NAME_MAP.put("java", "java");
        COURSE_NAME_MAP.put("web", "web");
        COURSE_NAME_MAP.put("cơ sở dữ liệu", "cơ sở dữ liệu");
        COURSE_NAME_MAP.put("co so du lieu", "cơ sở dữ liệu");
        COURSE_NAME_MAP.put("csdl", "cơ sở dữ liệu");
        COURSE_NAME_MAP.put("trí tuệ nhân tạo", "trí tuệ nhân tạo");
        COURSE_NAME_MAP.put("tri tue nhan tao", "trí tuệ nhân tạo");
        COURSE_NAME_MAP.put("ai", "trí tuệ nhân tạo");
        COURSE_NAME_MAP.put("mạng máy tính", "mạng máy tính");
        COURSE_NAME_MAP.put("mang may tinh", "mạng máy tính");
        COURSE_NAME_MAP.put("vi tích phân", "vi tích phân");
        COURSE_NAME_MAP.put("vi tich phan", "vi tích phân");
    }

    private static final Set<String> ADMIN_ONLY_INTENTS = Set.of(
            "CONFIG_WARNING_THRESHOLD", "TRIGGER_MOODLE_SYNC",
            "ADMIN_VIEW_SYSTEM_STATS", "ADMIN_CHECK_API_STATUS",
            "ADMIN_MANAGE_USERS", "ADMIN_VIEW_SYNC_LOGS"
    );

    private static final Set<String> LECTURER_ONLY_INTENTS = Set.of(
            "CHECK_SUBMISSIONS_AND_REMIND", "FILTER_COURSE_RISK", "QUERY_STUDENT_INFO_NLP"
    );

    private static final Set<String> ADVISER_ONLY_INTENTS = Set.of(
            "VIEW_CLASS_RISK_SUMMARY", "FIND_INACTIVE_STUDENTS",
            "ADVISER_STUDENT_PROGRESS", "ADVISER_OVERALL_REPORT"
    );

    private LocalIntentMatcher() {
    }

    public static Optional<Map<String, Object>> match(String message) {
        return match(message, null);
    }

    public static Optional<Map<String, Object>> match(String message, String role) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }

        String normalized = message.trim().toLowerCase(Locale.forLanguageTag("vi"));
        Map<String, String> entities = extractEntities(normalized, role);
        String upperRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);

        // Chitchat
        if (GREET.matcher(normalized).find()) return Optional.of(result("GREET", 1.0, entities));
        if (GOODBYE.matcher(normalized).find()) return Optional.of(result("GOODBYE", 1.0, entities));
        if (THANK.matcher(normalized).find()) return Optional.of(result("THANK", 1.0, entities));
        if (HELP.matcher(normalized).find()) return Optional.of(result("HELP", 1.0, entities));

        // Student intents (chỉ cho STUDENT role)
        if ("STUDENT".equals(upperRole)) {
            if (STUDENT_IMPROVE.matcher(normalized).find()) {
                return Optional.of(result("GET_IMPROVEMENT_SUGGESTIONS", 0.94, entities));
            }
            if (STUDENT_RISK.matcher(normalized).find()) {
                return Optional.of(result("CHECK_OWN_RISK_STATUS", 0.95, entities));
            }
            if (STUDENT_SELF.matcher(normalized).find() || STUDENT_GRADES.matcher(normalized).find()) {
                return Optional.of(result("LIST_OWN_GRADES", 0.96, entities));
            }
        }

        // Admin
        if (ADMIN_THRESHOLD.matcher(normalized).find() && !ADMIN_EXCLUDE.matcher(normalized).find()) return Optional.of(result("CONFIG_WARNING_THRESHOLD", 0.96, entities));
        if (ADMIN_API_STATUS.matcher(normalized).find()) return Optional.of(result("ADMIN_CHECK_API_STATUS", 0.95, entities));
        if (ADMIN_SYNC.matcher(normalized).find()) return Optional.of(result("TRIGGER_MOODLE_SYNC", 0.96, entities));
        if (ADMIN_STATS.matcher(normalized).find()) return Optional.of(result("ADMIN_VIEW_SYSTEM_STATS", 0.95, entities));

        // Lecturer (ưu tiên trước Adviser)
        if (LECTURER_SUBMISSION.matcher(normalized).find()) {
            return Optional.of(result("CHECK_SUBMISSIONS_AND_REMIND", 0.95, entities));
        }
        if (LECTURER_RISK.matcher(normalized).find() && !LECTURER_RISK_EXCLUDE.matcher(normalized).find()) {
            return Optional.of(result("FILTER_COURSE_RISK", 0.95, entities));
        }
        if (LECTURER_STUDENT_LOOKUP.matcher(normalized).find() || MSSV_PATTERN.matcher(normalized).find()) {
            return Optional.of(result("QUERY_STUDENT_INFO_NLP", 0.95, entities));
        }

        // Adviser
        if (ADVISER_INACTIVE.matcher(normalized).find()) {
            return Optional.of(result("FIND_INACTIVE_STUDENTS", 0.95, entities));
        }
        if (ADVISER_CLASS_RISK.matcher(normalized).find()) {
            return Optional.of(result("VIEW_CLASS_RISK_SUMMARY", 0.95, entities));
        }

        return Optional.empty();
    }

    public static Optional<Map<String, Object>> matchForRole(String message, String role) {
        return matchForRole(message, role, null);
    }

    public static Optional<Map<String, Object>> matchForRole(String message, String role, String username) {
        Optional<Map<String, Object>> base = match(message, role);
        if (base.isEmpty()) return Optional.empty();

        Map<String, Object> resultMap = base.get();
        String intent = (String) resultMap.get("intent");

        if (role == null || role.isBlank()) return base;
        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);

        if ("STUDENT".equals(normalizedRole)) {
            // Nếu sinh viên cố truy vấn MSSV khác → từ chối
            @SuppressWarnings("unchecked")
            Map<String, String> entities = (Map<String, String>) resultMap.get("entities");
            if (entities != null && entities.containsKey("mssv") && username != null
                    && !username.equals(entities.get("mssv"))) {
                return Optional.of(denied(resultMap));
            }
            if (ADMIN_ONLY_INTENTS.contains(intent) || LECTURER_ONLY_INTENTS.contains(intent) || ADVISER_ONLY_INTENTS.contains(intent)) {
                return Optional.of(denied(resultMap));
            }
        }
        if ("ADVISER".equals(normalizedRole)) {
            if (ADMIN_ONLY_INTENTS.contains(intent) || LECTURER_ONLY_INTENTS.contains(intent)) {
                return Optional.of(denied(resultMap));
            }
        }
        if ("LECTURER".equals(normalizedRole)) {
            if (ADMIN_ONLY_INTENTS.contains(intent) || ADVISER_ONLY_INTENTS.contains(intent)) {
                return Optional.of(denied(resultMap));
            }
        }
        return base;
    }

    private static Map<String, Object> denied(Map<String, Object> original) {
        Map<String, Object> denied = new LinkedHashMap<>();
        denied.put("intent", "PERMISSION_DENIED");
        denied.put("confidence", 0.99);
        denied.put("entities", original.get("entities"));
        denied.put("response_text", "");
        return denied;
    }

    private static Map<String, String> extractEntities(String normalized, String role) {
        Map<String, String> entities = new HashMap<>();
        Matcher m = MSSV_PATTERN.matcher(normalized);
        if (m.find()) entities.put("mssv", m.group(1));
        m = CLASS_CODE_PATTERN.matcher(normalized);
        if (m.find()) entities.put("class_code", m.group(1).toUpperCase(Locale.ROOT));
        m = RISK_LEVEL_PATTERN.matcher(normalized);
        if (m.find()) {
            String level = m.group(1).toLowerCase(Locale.ROOT);
            if (level.equals("đỏ") || level.equals("red") || level.equals("cao")) {
                entities.put("risk_level", "red");
            } else if (level.equals("vàng") || level.equals("yellow") || level.equals("trung bình")) {
                entities.put("risk_level", "yellow");
            } else if (level.equals("xanh") || level.equals("green") || level.equals("thấp")) {
                entities.put("risk_level", "green");
            }
        }
        Matcher dayMatcher = Pattern.compile("(?:hơn|trên|over)\\s+(\\d+)\\s*(ngày|tuần|day|week)").matcher(normalized);
        if (dayMatcher.find()) {
            int value = Integer.parseInt(dayMatcher.group(1));
            String unit = dayMatcher.group(2);
            if (unit.startsWith("tuần") || unit.startsWith("week")) value *= 7;
            entities.put("inactive_days", String.valueOf(value));
        }
        // Extract tên môn học
        Matcher courseMatcher = COURSE_NAME_PATTERN.matcher(normalized);
        if (courseMatcher.find()) {
            String course = courseMatcher.group(1).toLowerCase(Locale.ROOT).trim();
            // Normalize: bỏ khoảng trắng thừa
            course = course.replaceAll("\\s+", " ");
            // Map về tên chuẩn
            String mapped = COURSE_NAME_MAP.getOrDefault(course, course);
            entities.put("course_name", mapped);
        }
        return entities;
    }

    private static Map<String, Object> result(String intent, double confidence, Map<String, String> entities) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("intent", intent);
        map.put("confidence", confidence);
        map.put("entities", entities != null ? entities : new HashMap<>());
        map.put("response_text", "");
        return map;
    }
}
