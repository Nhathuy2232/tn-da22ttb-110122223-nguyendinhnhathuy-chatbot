# Hệ thống Giám sát Rủi ro Tự động (Automatic Risk Monitoring)

## 📋 Tổng quan

Hệ thống tự động phân tích rủi ro học tập của sinh viên và **gửi thông báo cảnh báo đỏ ngay lập tức** cho:
- ✅ **Sinh viên** có nguy cơ
- ✅ **Giáo viên** phụ trách môn học  
- ✅ **Cố vấn** học tập của sinh viên

## ⚙️ Cấu hình

### Thời gian chạy
- **Mặc định**: Mỗi **30 phút** một lần
- **Cron expression**: `0 */30 * * * *`

Có thể thay đổi trong file `AutomaticRiskMonitorService.java`:
```java
@Scheduled(cron = "0 */30 * * * *")  // Mỗi 30 phút
// Hoặc
@Scheduled(cron = "0 0 */1 * * *")   // Mỗi 1 giờ
// Hoặc  
@Scheduled(cron = "0 0 8,12,16,20 * * *")  // 4 lần/ngày: 8h, 12h, 16h, 20h
```

### Ngưỡng cảnh báo ĐỎ

```java
// Trong AutomaticRiskMonitorService.java
private static final double RED_GRADE_THRESHOLD = 50.0;   // Điểm TB < 50
private static final int RED_INACTIVE_DAYS = 14;          // Không online > 14 ngày
```

**Điều kiện cảnh báo ĐỎ** (một trong hai):
1. Điểm trung bình môn học **< 50/100**
2. Không truy cập Moodle trong **> 14 ngày**

## 🔔 Luồng hoạt động

### 1. Phân tích tự động (mỗi 30 phút)
```
Bước 1: Lấy danh sách tất cả sinh viên đang hoạt động
         ↓
Bước 2: Với mỗi sinh viên:
         • Lấy danh sách môn học đang theo học
         • Tính điểm TB từng môn
         • Kiểm tra ngày online cuối
         ↓
Bước 3: Xác định mức độ rủi ro (GREEN/YELLOW/RED)
         ↓
Bước 4: Nếu là ĐỎ → Tạo Warning + Gửi thông báo
```

### 2. Gửi thông báo
Khi phát hiện cảnh báo ĐỎ, hệ thống **TỰ ĐỘNG GỬI**:

#### 📧 Đến Sinh viên:
```
Tiêu đề: ⚠️ CẢNH BÁO HỌC TẬP - Môn [Tên môn]

Chào [Tên sinh viên],

Hệ thống phát hiện bạn đang có nguy cơ trong môn học [Tên môn]:
• Điểm trung bình: XX/100
• Không online: XX ngày

Khuyến nghị:
- Liên hệ ngay với giảng viên
- Tham gia đầy đủ các buổi học
- Nộp bài tập đúng hạn

Cố vấn học tập: [Tên cố vấn]
```

#### 👨‍🏫 Đến Giáo viên phụ trách môn:
```
Tiêu đề: 🔴 Cảnh báo SV nguy cơ cao - [Tên môn]

Thầy/Cô [Tên giáo viên],

Sinh viên [MSSV] - [Tên] đang có nguy cơ cao trong môn [Tên môn]:
• Điểm TB: XX/100
• Không online: XX ngày
• Trạng thái: Cảnh báo ĐỎ

Đề nghị quan tâm và hỗ trợ sinh viên.
```

#### 👔 Đến Cố vấn học tập:
```
Tiêu đề: ⚠️ SV lớp cần quan tâm - [MSSV]

Thầy/Cô Cố vấn,

Sinh viên [MSSV] - [Tên] trong lớp [Mã lớp] cần quan tâm:
• Môn học: [Tên môn]
• Điểm TB: XX/100  
• Không online: XX ngày
• Mức độ: Cảnh báo ĐỎ

Đề nghị liên hệ và tư vấn cho sinh viên.
```

## 🚫 Chống spam thông báo

Hệ thống **KHÔNG GỬI LẠI** thông báo nếu:
- Đã có cảnh báo cho sinh viên này trong môn học này
- Trong vòng **24 giờ** gần nhất

Điều này tránh spam notification mỗi 30 phút.

## 📊 Log và Theo dõi

### Log mẫu khi chạy:
```
=================================================
AUTOMATIC RISK MONITOR - Starting...
Time: 2026-07-07 10:00:00
=================================================
Total active students: 150

🔴 RED ALERT: Nguyễn Văn A (110122001) - Course: Lập trình Java 
   Reason: Điểm TB thấp (45.00/100). Không online 15 ngày. 
   Notifications sent: 3

🔴 RED ALERT: Trần Thị B (110122002) - Course: Cơ sở dữ liệu
   Reason: Không online 20 ngày.
   Notifications sent: 3

=================================================
AUTOMATIC RISK MONITOR - Completed
Red alerts found: 12
Notifications sent: 36
=================================================
```

### Xem log:
```bash
# Trong terminal backend
tail -f logs/chatbot-system.log | grep "AUTOMATIC RISK MONITOR"

# Hoặc xem full log
cat logs/chatbot-system.log | grep "RED ALERT"
```

## 🔧 Bật/Tắt tính năng

### Tắt hoàn toàn:
Comment dòng `@Scheduled` trong `AutomaticRiskMonitorService.java`:
```java
// @Scheduled(cron = "0 */30 * * * *")
public void monitorAndNotifyRedAlerts() {
    // ...
}
```

### Bật lại:
Bỏ comment dòng `@Scheduled` và restart backend.

## 🧪 Test thủ công

Gọi trực tiếp method để test (không cần đợi 30 phút):

```java
@Autowired
private AutomaticRiskMonitorService riskMonitor;

// Chạy ngay
riskMonitor.monitorAndNotifyRedAlerts();
```

Hoặc tạo REST endpoint test:
```java
@GetMapping("/api/admin/trigger-risk-monitor")
public String triggerRiskMonitor() {
    riskMonitor.monitorAndNotifyRedAlerts();
    return "Risk monitor triggered manually!";
}
```

## 📈 Dashboard & Thống kê

Để xem thống kê cảnh báo, sử dụng các endpoint:

```bash
# Xem tất cả cảnh báo ĐỎ chưa xác nhận
GET http://localhost:8082/api/warnings/red

# Xem dashboard tổng quan
GET http://localhost:8082/api/warnings/dashboard

# Xem cảnh báo của 1 sinh viên
GET http://localhost:8082/api/warnings/student/{studentId}
```

## ⚠️ Lưu ý quan trọng

1. **Database**: Đảm bảo kết nối đến Moodle database (MySQL) ổn định
2. **Email service**: Cấu hình SMTP trong Moodle để gửi email được
3. **Performance**: Nếu có > 1000 sinh viên, cân nhắc tăng interval lên 1 giờ
4. **Ngưỡng cảnh báo**: Điều chỉnh theo chính sách của trường

## 🎯 Tùy chỉnh nâng cao

### Thay đổi điều kiện cảnh báo:

```java
private RiskLevel determineRiskLevel(double avgGrade, boolean hasGrades, long daysSinceAccess) {
    // ĐỎ: Thêm điều kiện mới
    if (hasGrades && avgGrade < 30) {
        return RiskLevel.RED;  // Điểm < 30 = ĐỎ ngay
    }
    
    if ((hasGrades && avgGrade < 50) || daysSinceAccess > 14) {
        return RiskLevel.RED;
    }
    
    // Thêm điều kiện VÀNG
    if (daysSinceAccess > 10) {  // Giảm từ 14 → 10 ngày
        return RiskLevel.YELLOW;
    }
    
    return RiskLevel.GREEN;
}
```

### Thêm thông báo cho Admin:

```java
// Sau khi hoàn thành phân tích
if (redCount > 10) {
    // Gửi email tổng hợp cho Admin
    notificationService.notifyAdmin(
        "Cảnh báo: Có " + redCount + " sinh viên mức ĐỎ!"
    );
}
```

## 📞 Hỗ trợ

- Tác giả: Nguyễn Đình Nhật Huy - MSSV: 110122223
- Email: nhathuy.dev@gmail.com
- Xem log lỗi: `logs/chatbot-system.log`
