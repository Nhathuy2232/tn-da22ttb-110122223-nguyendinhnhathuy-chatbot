# 🧪 Hướng dẫn Test Automatic Risk Monitoring

## 📋 Chuẩn bị

### 1. Đảm bảo backend đang chạy
```bash
cd C:\xampp\htdocs\moodle\chatbot-system
mvn spring-boot:run
```

Đợi cho đến khi thấy:
```
Tomcat started on port 8082 (http)
Started ChatbotApplication in X.XXX seconds
```

### 2. Đảm bảo có dữ liệu test

Cần có:
- ✅ Ít nhất 1 sinh viên trong database (MSSV dạng `1101xxxxx`)
- ✅ Sinh viên đó đã đăng ký môn học
- ✅ Có dữ liệu điểm (hoặc không online lâu)

---

## 🚀 Phương pháp 1: Test qua REST API (Nhanh nhất)

### Bước 1: Test endpoint status
Kiểm tra service có hoạt động không:

```bash
# Windows PowerShell
Invoke-RestMethod -Uri "http://localhost:8082/api/risk-monitor/status" -Method GET
```

**Kết quả mong đợi:**
```json
{
  "serviceActive": true,
  "scheduledInterval": "Every 30 minutes",
  "cronExpression": "0 */30 * * * *",
  "redGradeThreshold": 50.0,
  "redInactiveDays": 14,
  "notificationCooldown": "24 hours"
}
```

### Bước 2: Trigger monitoring thủ công

```bash
# Windows PowerShell
Invoke-RestMethod -Uri "http://localhost:8082/api/risk-monitor/trigger" -Method POST
```

**Kết quả mong đợi:**
```json
{
  "success": true,
  "message": "Risk monitoring completed successfully",
  "durationMs": 2345,
  "note": "Check logs for detailed results"
}
```

### Bước 3: Xem log kết quả

Trong terminal đang chạy backend, bạn sẽ thấy:

```
=================================================
MANUAL TRIGGER - Risk monitoring started by admin
=================================================
AUTOMATIC RISK MONITOR - Starting...
Time: 2026-07-07 10:30:00
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

---

## 🌐 Phương pháp 2: Test qua Swagger UI

### Bước 1: Mở Swagger UI
Truy cập: **http://localhost:8082/swagger-ui.html**

### Bước 2: Tìm endpoint "Risk Monitor"
Mở rộng section **"Risk Monitor - Test & Trigger Automatic Risk Monitoring"**

### Bước 3: Test các endpoint

#### a) GET `/api/risk-monitor/status`
1. Click "Try it out"
2. Click "Execute"
3. Xem response

#### b) POST `/api/risk-monitor/trigger`
1. Click "Try it out"
2. Click "Execute"
3. Đợi (có thể mất 10-30 giây)
4. Xem response và log

---

## 🔍 Phương pháp 3: Test bằng Browser

### Test endpoint status:
Mở trình duyệt và truy cập:
```
http://localhost:8082/api/risk-monitor/status
```

Bạn sẽ thấy JSON response hiển thị status.

### Test trigger (cần extension REST Client):
Cài extension như **RESTClient** hoặc **Postman**, sau đó:
- Method: POST
- URL: `http://localhost:8082/api/risk-monitor/trigger`
- Click Send

---

## 📊 Kiểm tra kết quả

### 1. Xem log file
```bash
# Windows PowerShell
Get-Content chatbot-system\logs\chatbot-system.log -Tail 50
```

Hoặc mở file: `chatbot-system/logs/chatbot-system.log`

### 2. Kiểm tra database

Kết nối vào MySQL và chạy:

```sql
-- Xem tất cả warnings vừa tạo
SELECT * FROM mdl_warning 
ORDER BY detected_at DESC 
LIMIT 20;

-- Đếm số warnings theo risk level
SELECT risk_level, COUNT(*) as count 
FROM mdl_warning 
GROUP BY risk_level;

-- Xem warnings RED chưa acknowledge
SELECT w.*, u.username, u.firstname, u.lastname
FROM mdl_warning w
JOIN mdl_user u ON u.id = w.student_id
WHERE w.risk_level = 'RED' 
  AND w.is_acknowledged = 0
ORDER BY w.detected_at DESC;
```

### 3. Kiểm tra thông báo đã gửi

```sql
-- Xem notifications gần đây
SELECT * FROM mdl_message 
ORDER BY timecreated DESC 
LIMIT 20;

-- Đếm notifications theo user
SELECT useridto, COUNT(*) as notification_count
FROM mdl_message
WHERE timecreated > UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 1 HOUR))
GROUP BY useridto;
```

---

## 🎯 Tạo dữ liệu test

### Cách 1: Thủ công qua Moodle

1. Đăng nhập Moodle với tài khoản sinh viên
2. Đăng ký vào 1 khóa học
3. **Không online** trong > 14 ngày (đổi `lastaccess` trong database)
4. Hoặc có điểm TB < 50

### Cách 2: Chèn dữ liệu test vào database

```sql
-- Tạo sinh viên test
INSERT INTO mdl_user (username, firstname, lastname, email, password, lastaccess)
VALUES ('110199999', 'Test', 'Student', 'test@example.com', 
        '$2y$10$...hash...', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 20 DAY)));

-- Đăng ký vào khóa học
INSERT INTO mdl_user_enrolments (enrolid, userid, timecreated, status)
SELECT e.id, u.id, UNIX_TIMESTAMP(), 0
FROM mdl_user u, mdl_enrol e
WHERE u.username = '110199999' 
  AND e.courseid = 2  -- Thay bằng ID khóa học thực tế
LIMIT 1;

-- Tạo điểm thấp
INSERT INTO mdl_grade_grades (itemid, userid, finalgrade, rawgrademax)
VALUES (1, (SELECT id FROM mdl_user WHERE username = '110199999'), 35, 100);
```

---

## ⚙️ Tùy chỉnh để test dễ hơn

### Giảm ngưỡng cảnh báo (tạm thời)

Sửa file `AutomaticRiskMonitorService.java`:

```java
// Thay đổi tạm thời để test dễ
private static final double RED_GRADE_THRESHOLD = 80.0;  // Từ 50 → 80
private static final int RED_INACTIVE_DAYS = 3;          // Từ 14 → 3
```

**Lưu ý**: Nhớ đổi lại sau khi test!

### Giảm thời gian cooldown

Sửa phần kiểm tra trong `AutomaticRiskMonitorService.java`:

```java
private boolean checkRecentWarning(long userId, long courseId) {
    // Từ 24 giờ → 1 giờ để test
    LocalDateTime lastHour = LocalDateTime.now().minusHours(1);
    // ...
}
```

### Thay đổi tần suất chạy (test tự động)

Sửa `@Scheduled`:

```java
// Test: Chạy mỗi 2 phút
@Scheduled(cron = "0 */2 * * * *")

// Test: Chạy mỗi 5 phút
@Scheduled(cron = "0 */5 * * * *")

// Production: Chạy mỗi 30 phút
@Scheduled(cron = "0 */30 * * * *")
```

---

## 🐛 Debug và Troubleshooting

### Không có RED alerts?

1. **Kiểm tra có sinh viên nào đáp ứng điều kiện không:**
```sql
-- Sinh viên có điểm TB < 50
SELECT u.username, u.firstname, u.lastname, 
       AVG(gg.finalgrade) as avg_grade
FROM mdl_user u
JOIN mdl_grade_grades gg ON gg.userid = u.id
WHERE u.username REGEXP '^1101[0-9]{5}$'
GROUP BY u.id
HAVING avg_grade < 50;

-- Sinh viên không online > 14 ngày
SELECT username, firstname, lastname,
       FROM_UNIXTIME(lastaccess) as last_access,
       DATEDIFF(NOW(), FROM_UNIXTIME(lastaccess)) as days_offline
FROM mdl_user
WHERE username REGEXP '^1101[0-9]{5}$'
  AND lastaccess < UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 14 DAY));
```

2. **Kiểm tra log có lỗi không:**
```bash
Get-Content chatbot-system\logs\chatbot-system.log | Select-String "ERROR"
```

### Notifications không được gửi?

1. **Kiểm tra NotificationService có hoạt động không:**
```java
// Trong log, tìm:
"notificationService.notifyViolation"
```

2. **Kiểm tra cấu hình email trong Moodle:**
- Site administration → Server → Email → Outgoing mail configuration

3. **Test trực tiếp NotificationService:**
```java
// Tạo test endpoint
@GetMapping("/api/test/notification")
public String testNotification() {
    int sent = notificationService.notifyViolation(2L, 2L, "red", "Test notification");
    return "Sent: " + sent + " notifications";
}
```

### Service không chạy tự động sau 30 phút?

1. **Kiểm tra @EnableScheduling có được bật không:**
```java
// Trong ChatbotApplication.java phải có:
@EnableScheduling
```

2. **Xem log có thông báo scheduled task không:**
```
Exposing 3 endpoint(s) beneath base path '/actuator'
```

---

## ✅ Checklist Test

- [ ] Backend đã khởi động thành công
- [ ] Endpoint `/api/risk-monitor/status` trả về 200 OK
- [ ] Có thể trigger manual qua `/api/risk-monitor/trigger`
- [ ] Log hiển thị "AUTOMATIC RISK MONITOR - Starting..."
- [ ] Có sinh viên đáp ứng điều kiện RED alert
- [ ] Warnings được tạo trong database
- [ ] Notifications được gửi (check mdl_message)
- [ ] Log hiển thị "RED ALERT: ..." và "Notifications sent: X"
- [ ] Scheduled task tự động chạy sau 30 phút

---

## 📞 Hỗ trợ

Nếu gặp lỗi:
1. Copy toàn bộ log error
2. Kiểm tra database connection
3. Xem file: `chatbot-system/logs/chatbot-system.log`
4. Check Swagger UI: http://localhost:8082/swagger-ui.html
