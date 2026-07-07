# TÀI KHOẢN TEST - HỆ THỐNG CẢNH BÁO SỚM

## 🔐 MẬT KHẨU CHUNG: `User@123`

---

## 👨‍🏫 GIẢNG VIÊN (Teachers)

### Giảng viên 1: **Nguyễn Văn A**
- **Username:** `gv.nguyenvana`
- **Email:** nguyenvana@uit.edu.vn
- **Password:** `User@123`
- **Vai trò:** Giảng viên phụ trách các môn học

### Giảng viên 2: **Trần Thị B**
- **Username:** `gv.tranthib`
- **Email:** tranthib@uit.edu.vn
- **Password:** `User@123`
- **Vai trò:** Giảng viên phụ trách các môn học

### Giảng viên 3: **Lê Văn C**
- **Username:** `gv.levanc`
- **Email:** levanc@uit.edu.vn
- **Password:** `User@123`
- **Vai trò:** Giảng viên phụ trách các môn học

### Giảng viên 4: **Trịnh Quốc Việt**
- **Username:** `gv.trinhquocviet`
- **Email:** trinhquocviet@uit.edu.vn
- **Password:** `User@123`
- **Vai trò:** Giảng viên phụ trách các môn học

---

## 👥 CỐ VẤN HỌC TẬP (Academic Advisors)

**LƯU Ý:** Trong hệ thống Moodle hiện tại, vai trò "cố vấn" thường được gán cho:
- **Giảng viên** với vai trò quản lý lớp
- **Manager/Course creator** 

### Khuyến nghị sử dụng:
Để test chức năng gửi thông báo cho cố vấn, có thể sử dụng **bất kỳ tài khoản giảng viên nào** ở trên, vì họ đều có thể đóng vai trò cố vấn cho sinh viên trong lớp của mình.

**Tài khoản khuyên dùng làm cố vấn test:**
- **Username:** `gv.nguyenvana`
- **Password:** `User@123`

---

## 👨‍🎓 SINH VIÊN MẪU (Students)

### Sinh viên 1: **Nguyễn Văn An**
- **Username:** `110122016`
- **Email:** nguyenvanan@student.uit.edu.vn
- **Password:** `User@123`

### Sinh viên 2: **Trần Thị Bình**
- **Username:** `110122017`
- **Email:** tranthib@student.uit.edu.vn
- **Password:** `User@123`

### Sinh viên 3: **Lê Văn Cường**
- **Username:** `110122018`
- **Email:** levanc@student.uit.edu.vn
- **Password:** `User@123`

---

## 📋 HƯỚNG DẪN TEST THÔNG BÁO

### Bước 1: Đăng nhập với tài khoản sinh viên
```
Username: 110122016
Password: User@123
```
- Kiểm tra xem có nhận được **thông báo cảnh báo đỏ** không
- Thông báo sẽ xuất hiện ở **icon chuông 🔔** trên thanh menu Moodle

### Bước 2: Đăng nhập với tài khoản giảng viên
```
Username: gv.nguyenvana
Password: User@123
```
- Kiểm tra xem có nhận được **thông báo về sinh viên có nguy cơ** không
- Giảng viên sẽ nhận thông báo về các sinh viên trong môn học mà họ phụ trách

### Bước 3: Đăng nhập với tài khoản cố vấn
```
Username: gv.nguyenvana (hoặc bất kỳ giảng viên nào)
Password: User@123
```
- Kiểm tra xem có nhận được **thông báo về sinh viên được phụ trách** không
- Cố vấn sẽ nhận thông báo về tất cả sinh viên thuộc quyền quản lý

---

## ⚙️ TRIGGER TEST THỦ CÔNG

Để test ngay lập tức mà không cần đợi 30 phút, sử dụng API:

```powershell
# Trigger risk monitoring ngay lập tức
Invoke-RestMethod -Uri "http://localhost:8082/api/risk-monitor/trigger" -Method POST
```

Hoặc truy cập: `http://localhost:8082/api/risk-monitor/trigger` trong browser

---

## 📊 KIỂM TRA KẾT QUẢ

1. **Xem log backend:**
   ```powershell
   Get-Content logs\chatbot-system.log -Tail 50
   ```

2. **Kiểm tra dashboard:**
   - URL: http://localhost:8082/dashboard.html

3. **Kiểm tra thông báo Moodle:**
   - Đăng nhập Moodle: http://localhost/moodle
   - Nhấn vào icon 🔔 (bell icon) ở góc phải trên
   - Xem danh sách thông báo

---

## 🎯 KẾT QUẢ MONG ĐỢI

Khi hệ thống chạy thành công, bạn sẽ thấy:

✅ **Sinh viên có cảnh báo ĐỎ nhận được thông báo:**
- "⚠️ CẢNH BÁO NGUY CƠ CAO - Môn: [Tên môn]"
- "Bạn đang có nguy cơ bỏ học cao. Lý do: [Chi tiết]"

✅ **Giảng viên nhận được thông báo:**
- "🔴 Sinh viên [Tên SV] có nguy cơ cao trong môn [Tên môn]"
- "Lý do: [Chi tiết cảnh báo]"

✅ **Cố vấn nhận được thông báo:**
- "⚠️ Sinh viên [Tên SV] cần sự hỗ trợ"
- "Chi tiết: [Thông tin cảnh báo]"

---

## 🐛 TROUBLESHOOTING

Nếu không thấy thông báo, kiểm tra:

1. ✅ Backend đang chạy trên port 8082
2. ✅ MySQL đang chạy
3. ✅ Đã chạy SQL fix warnings table
4. ✅ Đã trigger API /risk-monitor/trigger
5. ✅ Kiểm tra log: `logs\chatbot-system.log`

---

**Tạo bởi:** Hệ thống Chatbot Cảnh báo Sớm - KLTN  
**Ngày cập nhật:** 07/07/2026
