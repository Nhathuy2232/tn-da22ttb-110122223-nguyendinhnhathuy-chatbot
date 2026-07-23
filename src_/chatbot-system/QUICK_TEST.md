# ⚡ Quick Test Guide - Risk Monitoring

## 🚀 Test nhanh nhất (3 bước)

### Bước 1: Khởi động backend
```bash
cd C:\xampp\htdocs\moodle\chatbot-system
mvn spring-boot:run
```

Đợi thấy: `Started ChatbotApplication`

---

### Bước 2: Chạy script test tự động
```powershell
cd C:\xampp\htdocs\moodle\chatbot-system
.\test-risk-monitor.ps1
```

**Output mẫu:**
```
========================================
  RISK MONITORING TEST SCRIPT
========================================

[1/4] Checking backend health...
✓ Backend is UP and running!

[2/4] Checking risk monitor status...
✓ Risk Monitor Service is active!
  - Scheduled: Every 30 minutes
  - Red Grade Threshold: 50.0
  - Red Inactive Days: 14

[3/4] Triggering risk monitoring manually...
  (This may take 10-30 seconds...)
✓ Risk monitoring completed successfully!
  - Duration: 2345 ms
  - Note: Check logs for detailed results

[4/4] Checking warnings dashboard...
✓ Dashboard data retrieved!
  - Green (Safe): 120
  - Yellow (Warning): 25
  - Red (High Risk): 5
  - Total: 150

========================================
  TEST COMPLETED!
========================================
```

---

### Bước 3: Xem kết quả trong log

```powershell
Get-Content logs\chatbot-system.log -Tail 50
```

**Tìm dòng:**
```
🔴 RED ALERT: Nguyễn Văn A (110122001) - Course: Java
   Reason: Điểm TB thấp (45.00/100). Không online 15 ngày.
   Notifications sent: 3
```

---

## 🌐 Test qua Browser (Không cần PowerShell)

### 1. Kiểm tra status
Mở browser, truy cập:
```
http://localhost:8082/api/risk-monitor/status
```

### 2. Trigger monitoring
- Cài extension **REST Client** hoặc mở **Swagger UI**: 
  ```
  http://localhost:8082/swagger-ui.html
  ```
- Tìm section "Risk Monitor"
- POST `/api/risk-monitor/trigger`
- Click "Try it out" → "Execute"

### 3. Xem dashboard
```
http://localhost:8082/api/warnings/dashboard
```

---

## 🔍 Kiểm tra database

Mở **phpMyAdmin** hoặc **MySQL Workbench**:

```sql
-- Xem warnings mới nhất
SELECT w.*, u.username, u.firstname, c.fullname as course
FROM mdl_warning w
JOIN mdl_user u ON u.id = w.student_id
LEFT JOIN mdl_course c ON c.id = w.course_id
ORDER BY w.detected_at DESC
LIMIT 10;

-- Đếm theo risk level
SELECT risk_level, COUNT(*) as count
FROM mdl_warning
GROUP BY risk_level;
```

---

## ❌ Không có RED alerts?

### Tạo dữ liệu test nhanh:

```sql
-- Đổi lastaccess của 1 sinh viên (làm họ offline > 14 ngày)
UPDATE mdl_user 
SET lastaccess = UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 20 DAY))
WHERE username LIKE '1101%'
LIMIT 1;

-- Kiểm tra
SELECT username, FROM_UNIXTIME(lastaccess) as last_access
FROM mdl_user 
WHERE username LIKE '1101%';
```

Sau đó chạy lại script test!

---

## ⚙️ Test với ngưỡng thấp hơn (dễ trigger)

Sửa `AutomaticRiskMonitorService.java` (tạm thời):

```java
private static final double RED_GRADE_THRESHOLD = 80.0;  // Từ 50 → 80
private static final int RED_INACTIVE_DAYS = 3;          // Từ 14 → 3
```

Restart backend và test lại!

---

## 📞 Lỗi thường gặp

### "Backend is not responding"
→ Backend chưa chạy hoặc chạy sai port
```bash
mvn spring-boot:run
# Kiểm tra port 8082
```

### "Risk Monitor Service is not active"
→ File `RiskMonitorController.java` chưa được compile
```bash
mvn clean compile
mvn spring-boot:run
```

### "No RED alerts found"
→ Không có sinh viên nào đáp ứng điều kiện
→ Tạo dữ liệu test (xem phần trên)

---

## ✅ Thành công khi:

- ✓ Script chạy không lỗi (all green checkmarks)
- ✓ Log hiển thị "RED ALERT" hoặc "Red alerts found: X"
- ✓ Database có records mới trong `mdl_warning`
- ✓ Thông báo được gửi (check `mdl_message`)

---

**Xem hướng dẫn đầy đủ**: `TEST_RISK_MONITORING.md`
