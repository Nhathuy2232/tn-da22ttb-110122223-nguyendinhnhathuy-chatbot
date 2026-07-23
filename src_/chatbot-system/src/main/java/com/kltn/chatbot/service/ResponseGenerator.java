package com.kltn.chatbot.service;

import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * ResponseGenerator - Sinh câu trả lời đa dạng, tự nhiên hơn
 *
 * Thay vì dùng 1 template cứng duy nhất, generator này cung cấp nhiều biến thể
 * cho cùng 1 ý nghĩa, kèm theo:
 * - Chào theo thời gian trong ngày (sáng/trưa/chiều/tối)
 * - Xưng hô theo role + tên user
 * - Random chọn biến thể (tránh lặp lại khi hỏi nhiều lần)
 * - Context-aware dựa trên dữ liệu thực (số lượng SV, mức rủi ro, điểm số...)
 * - Personality: thân thiện, khuyến khích, không máy móc
 *
 * Mục tiêu: câu trả lời đọc như người thật, không cứng nhắc.
 *
 * @author Nguyễn Đình Nhật Huy - MSSV: 110122223
 */
@Component
public class ResponseGenerator {

    private final Random random = new Random();

    // ============================================================
    // CHITCHAT - GREETING (nhiều biến thể theo giờ + role)
    // ============================================================

    public String greet(String role, String username) {
        String timeGreeting = getTimeBasedGreeting();
        String name = resolveDisplayName(role, username);

        if ("STUDENT".equalsIgnoreCase(role)) {
            String[] variants = {
                    timeGreeting + " " + name + "! 👋\n\nMình là EduGuard - trợ lý học tập của bạn. Mình có thể giúp bạn tra cứu điểm, theo dõi chuyên cần và đề xuất cải thiện kết quả học tập.\n\nBạn muốn xem điểm hôm nay chứ?",
                    timeGreeting + " " + name + "! 😊\n\nEduGuard đây! Mình hỗ trợ bạn theo dõi tiến độ học tập 24/7.\n\nGõ \"xem điểm của em\" để bắt đầu nhé!",
                    "Chào bạn " + name + "! 🌟\n\nMình là EduGuard - người bạn đồng hành học tập. Mình sẽ giúp bạn nắm rõ điểm số và tình trạng học tập hiện tại.\n\nBạn cứ hỏi mình bất cứ điều gì liên quan đến điểm nhé!",
                    timeGreeting + "! 👋\n\nMình là EduGuard đây. Sẵn sàng hỗ trợ bạn tra cứu điểm, kiểm tra chuyên cần và đưa ra gợi ý cải thiện kết quả học tập.\n\nBạn muốn bắt đầu từ đâu?"
            };
            return pick(variants);
        }
        if ("LECTURER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            String[] variants = {
                    timeGreeting + " " + name + "! 👋\n\nEduGuard đây ạ. Em sẵn sàng hỗ trợ Thầy/Cô theo dõi tiến độ lớp học và tình hình sinh viên.\n\nThầy/Cô muốn xem thông tin gì ạ?",
                    timeGreeting + " " + name + "! 😊\n\nEm là EduGuard - trợ lý theo dõi học tập. Em có thể giúp Thầy/Cô kiểm tra nộp bài, lọc sinh viên rủi ro theo môn, hoặc tra cứu thông tin bất kỳ SV nào.\n\nThầy/Cô cần em hỗ trợ gì ạ?",
                    "Chào Thầy/Cô " + name + "! 🌟\n\nEduGuard trực chiều rồi ạ. Mọi dữ liệu về điểm, chuyên cần, sinh viên nguy cơ đều đã được đồng bộ - Thầy/Cô cứ hỏi em nhé!",
                    timeGreeting + "! 👋\n\nEm là EduGuard đây ạ. Hôm nay Thầy/Cô muốn em giúp theo dõi điều gì - điểm danh, nộp bài, hay danh sách SV cần quan tâm ạ?"
            };
            return pick(variants);
        }
        if ("ADVISER".equalsIgnoreCase(role)) {
            String[] variants = {
                    timeGreeting + " " + name + "! 👋\n\nEduGuard đây ạ - trợ lý hỗ trợ cố vấn học tập. Em sẽ giúp Thầy/Cô nắm rõ tình hình lớp chủ nhiệm và phát hiện sớm SV cần hỗ trợ.\n\nThầy/Cô muốn xem lớp nào ạ?",
                    timeGreeting + " " + name + "! 😊\n\nEm là EduGuard. Em đã tổng hợp xong dữ liệu lớp chủ nhiệm - Thầy/Cô cứ hỏi em về bất kỳ SV nào hoặc tình hình chung cả lớp nhé!",
                    "Chào Cố vấn " + name + "! 🌟\n\nEduGuard sẵn sàng hỗ trợ. Thầy/Cô có thể hỏi em về: SV ngừng tương tác, SV mức đỏ/vàng, hoặc tổng quan tình hình lớp."
            };
            return pick(variants);
        }
        return timeGreeting + "! 👋\n\nMình là EduGuard. Mình có thể giúp gì cho bạn?";
    }

    public String goodbye() {
        String[] variants = {
                "Tạm biệt bạn! 👋 Học tốt nhé, hẹn gặp lại!",
                "Chào tạm biệt! 😊 Chúc bạn một ngày nhiều năng lượng. Khi nào cần mình cứ quay lại nhé!",
                "Bye bye! 🌟 Đừng quên kiểm tra điểm thường xuyên nhé. Hẹn gặp lại!",
                "Hẹn gặp lại bạn sau! 👋 Chúc bạn học tập hiệu quả!",
                "Tạm biệt! Mình luôn sẵn sàng hỗ trợ bạn. Hẹn gặp lại nhé! 😊"
        };
        return pick(variants);
    }

    public String thank() {
        String[] variants = {
                "Không có gì đâu bạn! 😊 Rất vui khi được giúp bạn.",
                "Bạn quá lịch sự! 🌟 Cứ hỏi mình bất cứ khi nào cần nhé!",
                "Rất vui được hỗ trợ bạn! 💪 Mình luôn sẵn sàng.",
                "Không có chi! Mình chỉ làm đúng nhiệm vụ thôi 😊 Hẹn gặp lại!",
                "Bạn cứ thoải mái nhé! Đó là việc của mình mà. Có gì cứ hỏi tiếp!"
        };
        return pick(variants);
    }

    // ============================================================
    // HELP - nhiều biến thể theo role
    // ============================================================

    public String help(String role) {
        if ("STUDENT".equalsIgnoreCase(role)) {
            String[] variants = {
                    "📚 Mình có thể giúp bạn những việc sau:\n\n"
                            + "🔹 Xem điểm: \"Xem điểm của em\", \"Bảng điểm môn Java của em\"\n"
                            + "🔹 Chuyên cần: \"Em có bị cảnh báo không?\", \"Tình trạng học tập của em\"\n"
                            + "🔹 Cải thiện: \"Làm sao để cải thiện điểm môn X?\", \"Em yếu môn gì?\"\n\n"
                            + "💬 Bạn cứ hỏi tự nhiên như đang chat với mình thôi!",

                    "🤖 Mình là EduGuard - mình hỗ trợ bạn 3 nhóm việc chính:\n\n"
                            + "1️⃣ Tra cứu điểm chi tiết từng bài, từng môn\n"
                            + "2️⃣ Theo dõi mức cảnh báo học vụ và chuyên cần\n"
                            + "3️⃣ Gợi ý lộ trình cải thiện điểm số\n\n"
                            + "✨ Gõ câu hỏi của bạn - mình sẽ tìm cách giúp!",

                    "💡 Đừng ngại hỏi mình nhé! Dưới đây là vài gợi ý:\n\n"
                            + "• \"Điểm của em thế nào rồi?\"\n"
                            + "• \"Môn Cơ sở dữ liệu em được bao nhiêu?\"\n"
                            + "• \"Em có đang bị cảnh báo không?\"\n"
                            + "• \"Làm sao để cải thiện điểm môn Web?\"\n\n"
                            + "Mình hiểu tiếng Việt tự nhiên, bạn cứ chat thoải mái!"
            };
            return pick(variants);
        }
        if ("LECTURER".equalsIgnoreCase(role)) {
            String[] variants = {
                    "📚 Em hỗ trợ Thầy/Cô các việc sau:\n\n"
                            + "🔹 Kiểm tra nộp bài: \"Lớp Java có ai chưa nộp bài?\"\n"
                            + "🔹 Lọc rủi ro: \"Môn CSDL có bao nhiêu SV mức đỏ?\"\n"
                            + "🔹 Tra cứu SV: \"Sinh viên 110122001 đi học thế nào?\", \"MSSV 110122223 đủ điều kiện thi chưa?\"\n"
                            + "🔹 Gửi cảnh báo: \"Gửi thông báo cho SV mức đỏ môn Java\"\n\n"
                            + "Thầy/Cô cứ hỏi em tự nhiên!",

                    "🤖 Em là EduGuard đây ạ! Em giúp Thầy/Cô:\n\n"
                            + "1️⃣ Theoo dõi SV chưa nộp bài theo môn\n"
                            + "2️⃣ Lọc SV theo mức rủi ro (đỏ/vàng/xanh)\n"
                            + "3️⃣ Tra cứu thông tin chi tiết bất kỳ SV nào\n"
                            + "4️⃣ Gửi thông báo cảnh báo tự động\n\n"
                            + "💬 Thầy/Cô cứ đặt câu hỏi - em sẽ phản hồi ngay!"
            };
            return pick(variants);
        }
        if ("ADVISER".equalsIgnoreCase(role)) {
            String[] variants = {
                    "📚 Em hỗ trợ Cố vấn các việc:\n\n"
                            + "🔹 Tổng quan lớp: \"Lớp DA22TTB có bao nhiêu SV cảnh báo?\"\n"
                            + "🔹 SV không online: \"SV ngừng tương tác lớp tôi\", \"SV không online 3 tuần\"\n"
                            + "🔹 SV mức đỏ: \"Danh sách SV mức đỏ lớp cố vấn\"\n"
                            + "🔹 Tình hình tổng: \"Tình hình học vụ lớp chủ nhiệm\"\n\n"
                            + "Thầy/Cô cứ hỏi em nhé!",

                    "🤖 Em là EduGuard - trợ lý cố vấn ạ. Em giúp Thầy/Cô nắm rõ:\n\n"
                            + "1️⃣ Tình hình học tập toàn lớp chủ nhiệm\n"
                            + "2️⃣ Phát hiện sớm SV có dấu hiệu sa sút\n"
                            + "3️⃣ Thống kê SV ngừng tương tác theo ngưỡng ngày\n\n"
                            + "💬 Cứ hỏi em - em sẽ phản hồi tức thì!"
            };
            return pick(variants);
        }
        // ADMIN
        String[] variants = {
                "🛠 Em hỗ trợ Quản trị viên:\n\n"
                        + "🔹 Cấu hình: \"Cấu hình ngưỡng cảnh báo\", \"Thay đổi quy định học vụ\"\n"
                        + "🔹 Đồng bộ: \"Đồng bộ dữ liệu Moodle\", \"Sync dữ liệu ngay\"\n"
                        + "🔹 Giám sát: \"Kiểm tra trạng thái API\", \"Thống kê hệ thống\"\n"
                        + "🔹 Log: \"Xem lịch sử đồng bộ\", \"Lỗi cache\"\n\n"
                        + "Admin cứ ra lệnh cho em!",

                "🤖 Em là EduGuard - hỗ trợ quản trị:\n\n"
                        + "1️⃣ Cấu hình ngưỡng cảnh báo học vụ\n"
                        + "2️⃣ Đồng bộ dữ liệu từ Moodle\n"
                        + "3️⃣ Kiểm tra trạng thái kết nối API\n"
                        + "4️⃣ Thống kê và giám sát hệ thống\n\n"
                        + "💬 Admin cứ hỏi em nhé!"
        };
        return pick(variants);
    }

    // ============================================================
    // PERMISSION DENIED - linh hoạt theo role
    // ============================================================

    public String permissionDenied(String role, String intent) {
        if ("STUDENT".equalsIgnoreCase(role)) {
            String[] variants = {
                    "🔒 Ơ, chức năng này dành cho giảng viên/cố vấn/admin cơ. Bạn chỉ có thể xem thông tin của chính mình thôi nhé!",
                    "🔒 Bạn không có quyền truy cập tính năng này đâu. Nếu cần, bạn có thể liên hệ giảng viên hoặc cố vấn học tập của mình nhé!",
                    "🔒 Hmm, cái này không phải quyền của sinh viên. Mình chỉ có thể giúp bạn xem điểm, chuyên cần và gợi ý cải thiện thôi!",
                    "🔒 Xin lỗi bạn, mình không thể chia sẻ thông tin đó. Bạn có muốn hỏi gì về điểm của chính mình không?"
            };
            return pick(variants);
        }
        if ("ADVISER".equalsIgnoreCase(role)) {
            return "🔒 Chức năng này không thuộc quyền của Cố vấn ạ. Em có thể giúp Thầy/Cô xem tình hình lớp chủ nhiệm hoặc tra cứu SV cụ thể ạ!";
        }
        if ("LECTURER".equalsIgnoreCase(role)) {
            return "🔒 Chức năng này không thuộc quyền của giảng viên ạ. Em có thể hỗ trợ Thầy/Cô kiểm tra nộp bài, lọc rủi ro môn học hoặc tra cứu SV ạ!";
        }
        return "🔒 Bạn không có quyền thực hiện thao tác này. Vui lòng liên hệ quản trị viên.";
    }

    // ============================================================
    // STUDENT - GRADES (variants theo số lượng môn + điểm)
    // ============================================================

    public String gradesHeader(String studentId, String fullName, int courseCount) {
        if (courseCount == 0) {
            return "📚 Bạn " + fullName + " (MSSV " + studentId + ") hiện chưa ghi danh môn học nào trong hệ thống.\n\nNếu có thắc mắc, bạn liên hệ phòng đào tạo nhé!";
        }
        if (courseCount == 1) {
            return "📊 ĐIỂM CHI TIẾT - " + fullName + " (MSSV " + studentId + ")\n\nMình có dữ liệu điểm 1 môn học của bạn:";
        }
        return "📊 ĐIỂM CHI TIẾT TỪNG BÀI - " + fullName + " (MSSV " + studentId + ")\n\n"
                + "Bạn đang học " + courseCount + " môn. Đây là chi tiết từng bài:";
    }

    public String gradesNoMatch(String courseName) {
        String[] variants = {
                "ℹ️ Mình không tìm thấy môn nào khớp với \"" + courseName + "\" trong danh sách môn bạn đang học.\n\n💡 Bạn thử gõ: java, web, csdl, ai, mạng, vi tích phân xem sao nhé!",
                "🤔 Mình tìm rồi nhưng không có môn nào khớp với \"" + courseName + "\" trong học kỳ này của bạn. Bạn có thể kiểm tra lại tên môn giúp mình không?"
        };
        return pick(variants);
    }

    public String gradesNotFound(String studentId) {
        String[] variants = {
                "❌ Mình không tìm thấy sinh viên với MSSV " + studentId + ". Bạn kiểm tra lại giúp mình nhé!",
                "❌ Xin lỗi, MSSV " + studentId + " không có trong hệ thống. Bạn xem lại giúp mình con số này được không?"
        };
        return pick(variants);
    }

    public String gradesPermissionDenied() {
        return "🔒 Bạn chỉ có thể xem điểm của chính mình thôi nhé. Nếu muốn xem điểm của bạn khác, bạn cần hỏi giảng viên hoặc cố vấn ạ!";
    }

    public String gradesEmptyCourse(String courseName) {
        return "📘 " + courseName + "\n   ⏳ Môn này hiện chưa có bài tập nào được giao. Bạn quay lại kiểm tra sau nhé!";
    }

    /**
     * Format dòng điểm 1 bài
     */
    public String gradeLine(String assignName, double grade, double maxGrade, String status, int daysAgo) {
        String icon, statusText;
        if ("submitted".equals(status) && grade > 0) {
            icon = "✅";
            statusText = String.format("%.2f / %.0f điểm", grade, maxGrade);
        } else if ("submitted".equals(status) && grade == 0) {
            icon = "📝";
            statusText = "0 / " + (long) maxGrade + " điểm (đã nộp, chờ chấm)";
        } else if (grade > 0) {
            icon = "📊";
            statusText = String.format("%.2f / %.0f điểm (chưa nộp bổ sung)", grade, maxGrade);
        } else {
            icon = "❌";
            statusText = "Chưa nộp";
        }
        String time = "";
        if (daysAgo == 0) time = " [nộp hôm nay]";
        else if (daysAgo > 0) time = " [nộp " + daysAgo + " ngày trước]";
        return "   " + icon + " " + assignName + ": " + statusText + time;
    }

    public String gradesFooter(int submitted, int total, double avg) {
        StringBuilder sb = new StringBuilder();
        sb.append("   ───────────────────────────────────────\n");
        sb.append("   📈 Đã nộp: ").append(submitted).append("/").append(total).append(" bài");
        if (total > 0) {
            sb.append(" | TB: ").append(String.format("%.2f", avg)).append(" / 100");
        }
        sb.append("\n");
        // Đánh giá + lời khuyên linh hoạt
        if (avg >= 80) {
            sb.append("   🟢 Xuất sắc! ").append(pick(new String[]{
                    "Bạn đang làm rất tốt, cứ giữ vững phong độ này nhé! 🌟",
                    "Tuyệt vời! Bạn cứ tiếp tục phát huy nhé! 💪",
                    "Kết quả rất khả quan! Bạn đang đi đúng hướng rồi! ✨"
            })).append("\n");
        } else if (avg >= 70) {
            sb.append("   🟡 Khá tốt! ").append(pick(new String[]{
                    "Bạn chỉ cần cố thêm một chút là sẽ lên mức xanh thôi!",
                    "Cố lên! Bạn đang tiến bộ rõ rệt rồi đó!",
                    "Kết quả ổn đấy, mình tin bạn sẽ cải thiện thêm! 👍"
            })).append("\n");
        } else if (avg >= 50) {
            sb.append("   🟠 Cần cố gắng! ").append(pick(new String[]{
                    "Bạn nên dành thêm thời gian ôn tập và làm bài tập đầy đủ nhé!",
                    "Mình khuyên bạn xem lại lý thuyết và làm thêm bài tập cơ bản.",
                    "Đừng nản nhé! Mỗi ngày cải thiện một chút là được! 💪"
            })).append("\n");
        } else {
            sb.append("   🔴 Nguy cơ cao! ").append(pick(new String[]{
                    "Bạn nên gặp giảng viên hoặc cố vấn sớm để được hỗ trợ kịp thời nhé!",
                    "Mình khuyên bạn liên hệ giảng viên ngay để tìm hướng cải thiện!",
                    "Bạn cần hành động ngay - nộp bổ sung bài hoặc đăng ký học cải thiện nhé!"
            })).append("\n");
        }
        return sb.toString();
    }

    public String gradesNoCourses(String studentId, String fullName) {
        return "📚 Bạn " + fullName + " (MSSV " + studentId + ") hiện chưa ghi danh môn học nào.";
    }

    // ============================================================
    // STUDENT - ATTENDANCE / RISK STATUS
    // ============================================================

    public String attendanceHeader(String fullName, String studentId) {
        return "⏰ TÌNH TRẠNG HỌC TẬP CỦA " + fullName.toUpperCase() + " (MSSV " + studentId + ")";
    }

    public String gradesOnlyHeader(String fullName, String studentId) {
        return "📊 ĐIỂM SỐ CỦA " + fullName.toUpperCase() + " (MSSV " + studentId + ")";
    }

    public String attendanceOnlyHeader(String fullName, String studentId) {
        return "📅 CHUYÊN CẦN RIÊNG CỦA " + fullName.toUpperCase() + " (MSSV " + studentId + ")";
    }

    public String attendanceLastAccess(long daysAccess) {
        if (daysAccess >= 999) {
            return pick(new String[]{
                    "📅 Mình thấy bạn chưa từng truy cập Moodle. Có thể tài khoản đang gặp vấn đề - bạn liên hệ phòng đào tạo nhé!",
                    "📅 Hmm, dường như bạn chưa từng đăng nhập vào hệ thống. Bạn thử đăng nhập thử xem sao nhé!"
            });
        }
        if (daysAccess == 0) {
            return "📅 Bạn vừa online hôm nay - rất tốt! 👍";
        }
        if (daysAccess == 1) {
            return "📅 Lần cuối bạn online là hôm qua. Vẫn ổn, cứ duy trì nhé!";
        }
        if (daysAccess <= 7) {
            return "📅 Lần cuối bạn online là " + daysAccess + " ngày trước. Bạn vẫn đang hoạt động đều đặn.";
        }
        if (daysAccess <= 14) {
            return "📅 Lần cuối online là " + daysAccess + " ngày trước. Hơi lâu rồi đó bạn ơi!";
        }
        if (daysAccess <= 30) {
            return "📅 Lần cuối online là " + daysAccess + " ngày trước. Cảnh báo: bạn đang mất kết nối với lớp học!";
        }
        return "📅 Lần cuối online đã " + daysAccess + " ngày trước. Nghiêm trọng - bạn cần đăng nhập lại ngay!";
    }

    public String attendanceWarning(long daysAccess) {
        if (daysAccess > 30) {
            return "🔴 Cảnh báo đỏ: Bạn đã " + daysAccess + " ngày không online. Nguy cơ thôi học rất cao. Mình khuyên bạn liên hệ cố vấn ngay!";
        }
        if (daysAccess > 14) {
            return "🔴 Cảnh báo đỏ: Hơn 2 tuần không online. Bạn cần đăng nhập lại và làm bài tập gấp nhé!";
        }
        if (daysAccess > 7) {
            return "🟡 Cảnh báo vàng: 1-2 tuần gần đây bạn ít tương tác. Mình khuyên bạn vào Moodle hàng ngày để không bị tụt hậu.";
        }
        return "🟢 Tình trạng hoạt động bình thường. Bạn cứ duy trì nhé!";
    }

    public String gradesListHeader() {
        return pick(new String[]{
                "📊 Điểm các môn đang học:",
                "📚 Tổng quan điểm số các môn:",
                "📈 Điểm số chi tiết từng môn của bạn:"
        });
    }

    /**
     * Format dòng môn
     */
    public String courseLine(String courseName, double avg, boolean hasGrades) {
        String icon;
        String level;
        if (hasGrades && avg < 50) {
            icon = "🔴";
            level = "yếu";
        } else if (hasGrades && avg < 70) {
            icon = "🟡";
            level = "trung bình";
        } else if (hasGrades) {
            icon = "🟢";
            level = "tốt";
        } else {
            icon = "⚪";
            level = "chưa có điểm";
        }
        if (hasGrades) {
            return "  " + icon + " " + courseName + " - TB: " + String.format("%.2f", avg) + " / 100 (" + level + ")";
        }
        return "  " + icon + " " + courseName + " - " + level;
    }

    // ============================================================
    // IMPROVEMENT SUGGESTIONS - linh hoạt theo mức rủi ro
    // ============================================================

    public String improvement(String courseName, double avgGrade) {
        if (avgGrade > 0 && avgGrade < 50) {
            return "💡 Gợi ý cải thiện cho " + courseName + ":\n\n"
                    + "Bạn đang ở mức nguy cơ cao. Mình khuyên:\n"
                    + "1️⃣ Gặp riêng giảng viên trong giờ office hour để xin tư vấn\n"
                    + "2️⃣ Làm lại toàn bộ bài tập cũ - bắt đầu từ bài cơ bản nhất\n"
                    + "3️⃣ Đăng ký học nhóm với bạn giỏi hoặc tìm gia sư\n"
                    + "4️⃣ Nộp bổ sung các bài đã trễ hạn (nếu giảng viên cho phép)\n"
                    + "5️⃣ Xét đăng ký thi cải thiện hoặc học lại vào kỳ sau\n\n"
                    + "💬 Gõ \"điểm môn " + courseName + " của em\" để xem chi tiết bài nào cần làm lại nhé!";
        }
        return "💡 Gợi ý cải thiện cho " + courseName + ":\n\n"
                + "1️⃣ Ôn lại lý thuyết nền tảng và làm bài tập cơ bản trước\n"
                + "2️⃣ Nộp đầy đủ bài Assignment và Quiz - tránh bị 0 điểm oan\n"
                + "3️⃣ Tham gia diễn đàn LMS, đặt câu hỏi cho giảng viên thường xuyên\n"
                + "4️⃣ Tìm bạn học cùng để thảo luận nhóm - hiệu quả gấp đôi\n"
                + "5️⃣ Nếu điểm < 5, đăng ký thi cải thiện hoặc học lại kịp thời\n"
                + "6️⃣ Đặt mục tiêu chuyên cần ≥ 80% để không bị cấm thi\n\n"
                + "💬 Bạn cứ hỏi mình \"điểm môn " + courseName + " của em\" để xem chi tiết!";
    }

    public String improvementForNonStudent() {
        return "ℹ️ Chức năng gợi ý cải thiện này chủ yếu dành cho sinh viên. Nếu bạn là cố vấn/giảng viên và muốn xem tình trạng SV, bạn có thể hỏi: \"Tình hình lớp DA22TTB\" nhé!";
    }

    // ============================================================
    // LECTURER HANDLERS
    // ============================================================

    public String submissionAskCourse() {
        return pick(new String[]{
                "📨 Bạn muốn kiểm tra nộp bài môn nào ạ?\n\nVí dụ: \"Lớp Java có ai chưa nộp Assignment 1?\"",
                "📨 Em cần biết môn cụ thể Thầy/Cô muốn kiểm tra ạ.\n\n💡 Ví dụ: \"Môn Cơ sở dữ liệu có SV nào chưa nộp bài không?\"",
                "📨 Môn nào ạ? Bạn cho mình biết tên môn - mình sẽ liệt kê SV chưa nộp liền!"
        });
    }

    public String submissionStudentDenied() {
        return "🔒 Chức năng kiểm tra nộp bài chỉ dành cho giảng viên/cố vấn ạ. Bạn có thể xem bài tập của chính mình qua mục \"Xem điểm\" nhé!";
    }

    public String courseRiskAskCourse() {
        return pick(new String[]{
                "📊 Bạn muốn xem rủi ro môn nào ạ?\n\nVí dụ: \"Môn Cơ sở dữ liệu có bao nhiêu SV mức đỏ?\"",
                "📊 Em cần biết tên môn ạ. Bạn có thể hỏi \"Môn Java SV mức đỏ\" hoặc \"Môn CSDL mức vàng\" nhé!",
                "📊 Cho mình biết tên môn và mức rủi ro (đỏ/vàng/xanh) bạn muốn xem nhé!"
        });
    }

    public String courseRiskNotFound(String courseName) {
        return "❌ Mình không tìm thấy môn học nào khớp với \"" + courseName + "\".\n\n"
                + "💡 Bạn thử một trong các từ khoá sau: java, web, csdl/cơ sở dữ liệu, ai/trí tuệ nhân tạo, mạng, vi tích phân.";
    }

    public String courseRiskHeader(String courseName) {
        return "⚠️ TÌNH HÌNH RỦI RO - " + courseName;
    }

    public String courseRiskSummary(int red, int yellow, int green, int total) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Tổng quan ").append(total).append(" SV:\n");
        sb.append("   🔴 Mức đỏ: ").append(red).append(" SV");
        if (red > 0) sb.append(" (nguy cơ cao)");
        sb.append("\n");
        sb.append("   🟡 Mức vàng: ").append(yellow).append(" SV");
        if (yellow > 0) sb.append(" (cần theo dõi)");
        sb.append("\n");
        sb.append("   🟢 Mức xanh: ").append(green).append(" SV");
        if (green == total) sb.append(" - tất cả đều ổn! 🎉");
        sb.append("\n\n");
        return sb.toString();
    }

    public String courseRiskNoMatch(String riskLevel) {
        return "✅ Môn này hiện không có SV nào ở mức rủi ro \"" + riskLevel + "\". Tin tốt lành!";
    }

    public String courseRiskListHeader(String riskLevel) {
        return "📋 Danh sách SV mức " + riskLevel + ":";
    }

    public String courseRiskRow(int idx, String icon, String username, String fullName, double avg, boolean hasGrades, long daysAccess) {
        StringBuilder sb = new StringBuilder();
        sb.append(idx).append(". ").append(icon).append(" ").append(username).append(" - ").append(fullName).append("\n");
        sb.append("   📊 Điểm TB: ").append(String.format("%.2f", avg));
        sb.append(hasGrades ? " (đã có điểm)" : " (chưa có điểm)");
        sb.append(" | 📅 Không online: ").append(daysAccess).append(" ngày\n");
        return sb.toString();
    }

    public String courseRiskNotifSent(int count) {
        if (count == 1) return "\n📨 Mình đã tự động gửi 1 thông báo đến giáo viên phụ trách và cố vấn ạ.";
        return "\n📨 Mình đã tự động gửi " + count + " thông báo đến giáo viên phụ trách và cố vấn ạ.";
    }

    public String courseRiskNoEnrollment(String courseName) {
        return "📚 Môn " + courseName + " hiện chưa có sinh viên ghi danh nào ạ.";
    }

    // ============================================================
    // ADVISER
    // ============================================================

    public String classSummaryAskClass(List<Map<String, Object>> cohorts) {
        StringBuilder sb = new StringBuilder("📊 Bạn muốn xem tình hình lớp nào ạ?\n\n");
        sb.append("Các lớp hiện có trong hệ thống:\n");
        if (cohorts.isEmpty()) {
            sb.append("  (chưa có lớp nào được tạo)\n");
        } else {
            for (Map<String, Object> c : cohorts) {
                sb.append("  • ").append(c.get("name"));
                if (c.get("idnumber") != null) sb.append(" (").append(c.get("idnumber")).append(")");
                sb.append("\n");
            }
        }
        sb.append("\n💡 Ví dụ: \"Tình hình học vụ tổng quan lớp DA22TTB\"");
        return sb.toString();
    }

    public String classSummaryNotFound(String classCode) {
        return "❌ Mình không tìm thấy lớp nào khớp với mã \"" + classCode + "\" hoặc lớp chưa có sinh viên. Bạn kiểm tra lại giúp mình nhé!";
    }

    public String classSummaryHeader(String classCode) {
        return "📊 TÌNH HÌNH HỌC VỤ LỚP " + classCode.toUpperCase();
    }

    public String classSummaryStats(int red, int yellow, int green, int total) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔴 Mức đỏ: ").append(red);
        if (red > 0) sb.append(" (cần xử lý ngay)");
        sb.append("\n");
        sb.append("🟡 Mức vàng: ").append(yellow);
        if (yellow > 0) sb.append(" (cần theo dõi)");
        sb.append("\n");
        sb.append("🟢 Mức xanh: ").append(green);
        sb.append("\n📚 Tổng: ").append(total).append(" sinh viên\n\n");
        return sb.toString();
    }

    public String classSummaryAllGood() {
        return "✅ Tuyệt vời! Tất cả sinh viên trong lớp đều đang hoạt động bình thường. 🎉";
    }

    public String classSummaryAtRiskHeader(int count) {
        return "⚠️ Danh sách SV cần quan tâm (" + count + " người):";
    }

    public String classSummaryRow(int idx, String icon, String username, String fullName, long days) {
        return idx + ". " + icon + " " + username + " - " + fullName + " (không online: " + days + " ngày)";
    }

    public String inactiveStudentsHeader(int days) {
        return "⏰ SINH VIÊN KHÔNG ONLINE TRÊN " + days + " NGÀY";
    }

    public String inactiveStudentsEmpty(int days, String classCode) {
        if (classCode != null) {
            return "✅ Lớp " + classCode + " không có SV nào không online quá " + days + " ngày. Tốt lắm!";
        }
        return "✅ Toàn trường không có SV nào không online quá " + days + " ngày. Tuyệt vời!";
    }

    public String inactiveStudentsCount(int count, String classCode) {
        if (classCode != null) {
            return "📭 Tìm thấy " + count + " SV trong lớp " + classCode + " không online quá lâu:";
        }
        return "📭 Tìm thấy " + count + " SV không online quá lâu:";
    }

    public String inactiveStudentRow(int idx, String username, String fullName, long days) {
        return idx + ". " + username + " - " + fullName + " (không online: " + days + " ngày)";
    }

    // ============================================================
    // ADMIN
    // ============================================================

    public String configThreshold() {
        return "✅ Em đã ghi nhận yêu cầu cấu hình ngưỡng cảnh báo ạ.\n\n"
                + "📝 Các tham số có thể điều chỉnh:\n"
                + "• Ngưỡng mức Đỏ (điểm tối thiểu, số ngày không online)\n"
                + "• Ngưỡng mức Vàng (điểm tối thiểu, tỷ lệ vắng)\n"
                + "• Ngưỡng mức Xanh (điểm tối thiểu, hoàn thành bài tập)\n\n"
                + "💡 Bạn có thể cập nhật trong file `application.yml` hoặc qua giao diện quản trị ạ.";
    }

    public String triggerSync() {
        return "🔄 Em đã ghi nhận yêu cầu đồng bộ dữ liệu từ Moodle ạ.\n\n"
                + "📋 Các bước kiểm tra:\n"
                + "1. ✅ Kết nối API Moodle Web Services\n"
                + "2. 🔄 Đồng bộ users, courses, enrollments\n"
                + "3. 📊 Đồng bộ grade items, log chuyên cần\n"
                + "4. ⚠️ Tính toán lại mức cảnh báo\n\n"
                + "💡 Bạn có thể kiểm tra log hệ thống và cache Redis/PostgreSQL để xác nhận kết quả ạ.";
    }

    public String checkApiStatus() {
        return "✅ Trạng thái kết nối Moodle API:\n\n"
                + "🔗 Base URL: http://localhost/moodle\n"
                + "🟢 Web Services: Hoạt động bình thường\n"
                + "🟢 Token: Hợp lệ\n"
                + "🟢 Database: Kết nối thành công\n"
                + "⏰ Kiểm tra lúc: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public String adminPermissionDenied() {
        return "🔒 Chức năng này chỉ dành cho quản trị viên ạ.";
    }

    public String studentPermissionDenied() {
        return "🔒 Chức năng này chỉ dành cho giảng viên, cố vấn và quản trị viên ạ.";
    }

    // ============================================================
    // GENERIC
    // ============================================================

    public String atRiskHeader(String level) {
        if (level == null || "all".equalsIgnoreCase(level)) {
            return "⚠️ DANH SÁCH SINH VIÊN CÓ RỦI RO";
        }
        return "⚠️ DANH SÁCH SINH VIÊN MỨC " + level.toUpperCase();
    }

    public String atRiskCount(int count, Integer total) {
        if (total != null) {
            return "Tìm thấy " + count + " / " + total + " SV vi phạm:";
        }
        return "Tìm thấy " + count + " SV vi phạm:";
    }

    public String atRiskEmpty() {
        return "✅ Tốt rồi! Hiện không có SV nào vi phạm ạ.";
    }

    public String atRiskRow(String icon, String studentId, String fullName, double avg, int inactiveCourses, Object daysSince) {
        StringBuilder sb = new StringBuilder();
        sb.append(icon).append(" ").append(studentId).append(" - ").append(fullName).append("\n");
        sb.append("    📊 Điểm TB: ").append(String.format("%.2f", avg));
        sb.append(" | Môn không hoạt động: ").append(inactiveCourses);
        if (daysSince != null) sb.append(" | 📅 Không online: ").append(daysSince).append(" ngày");
        sb.append("\n");
        return sb.toString();
    }

    public String atRiskNote() {
        return "\n💡 Lưu ý: Mình chỉ liệt kê SV có điểm < 50% hoặc môn không hoạt động hoặc không online > 14 ngày.";
    }

    public String unknown(String role) {
        if ("STUDENT".equalsIgnoreCase(role)) {
            return pick(new String[]{
                    "🤔 Xin lỗi, mình chưa hiểu rõ câu hỏi của bạn. Bạn có thể hỏi về:\n• Điểm số, chuyên cần của bạn\n• Tình trạng cảnh báo\n• Gợi ý cải thiện",
                    "😅 Mình chưa rõ ý bạn lắm. Bạn thử hỏi \"Xem điểm của em\" hoặc \"Em có bị cảnh báo không?\" xem sao nhé!",
                    "🤔 Mình không tìm thấy thông tin khớp với câu hỏi. Bạn có thể diễn đạt khác giúp mình không? Ví dụ: \"Điểm môn Java của em\""
            });
        }
        return pick(new String[]{
                "🤔 Xin lỗi, mình chưa hiểu rõ yêu cầu. Bạn có thể hỏi về:\n• Điểm/chuyên cần SV\n• SV nguy cơ theo môn/lớp\n• Cấu hình hệ thống (admin)",
                "😅 Mình chưa rõ ý bạn. Bạn thử diễn đạt cụ thể hơn nhé - ví dụ: \"Môn Java có SV nào mức đỏ?\"",
                "🤔 Mình không tìm thấy dữ liệu khớp. Bạn thử cung cấp thêm MSSV hoặc tên môn nhé!"
        });
    }

    public String dataMissing() {
        return "🤔 Xin lỗi, mình chưa có dữ liệu cho yêu cầu này. Bạn thử hỏi khác nhé!";
    }

    // ============================================================
    // UTILITIES
    // ============================================================

    private String pick(String[] variants) {
        return variants[random.nextInt(variants.length)];
    }

    private String getTimeBasedGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 11) {
            return pick(new String[]{"Chào buổi sáng", "Sáng nay", "Buổi sáng tốt lành"});
        }
        if (hour >= 11 && hour < 14) {
            return pick(new String[]{"Chào buổi trưa", "Trưa nay", "Buổi trưa"});
        }
        if (hour >= 14 && hour < 18) {
            return pick(new String[]{"Chào buổi chiều", "Chiều nay", "Buổi chiều tốt lành"});
        }
        if (hour >= 18 && hour < 22) {
            return pick(new String[]{"Chào buổi tối", "Tối nay", "Buổi tối"});
        }
        return pick(new String[]{"Chào bạn", "Đêm nay", "Khuya rồi"});
    }

    private String resolveDisplayName(String role, String username) {
        if (username == null || username.isBlank()) return "bạn";
        if (role == null) return username;
        if ("LECTURER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role) || "ADVISER".equalsIgnoreCase(role)) {
            return "Thầy/Cô";
        }
        return "bạn";
    }
}
