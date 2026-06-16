# 🤖 Chatbot Early Warning - Auto Create Test Data

## 📋 CHỨC NĂNG

Script tự động tạo dữ liệu test trên Moodle:
- ✅ 5 courses (IT301, IT201, IT302, IT401, IT202)
- ✅ 15 students (4 RED, 4 YELLOW, 7 GREEN)
- ✅ Enrollments (students vào courses)
- ✅ 5 assignments mỗi course
- ✅ Grades theo risk level

## 🚀 CHẠY SCRIPT (30 GIÂY)

### Cách 1: Double-click (Windows)
```
Double-click: run_create_data.bat
```

### Cách 2: Command line
```bash
cd c:\xampp\htdocs\moodle
php local\chatbot\create_test_data.php
```

## 📊 KẾT QUẢ

Sau khi chạy xong, Moodle sẽ có:

### Courses (5):
- Lập trình Java nâng cao (IT301)
- Cơ sở dữ liệu (IT201)
- Phát triển ứng dụng Web (IT302)
- Trí tuệ nhân tạo (IT401)
- Mạng máy tính (IT202)

### Students (15):

**RED Students (4):**
- student01 - Nguyễn Văn An (110122001)
- student02 - Trần Thị Bình (110122002)
- student03 - Lê Văn Cường (110122003)
- student04 - Phạm Thị Dung (110122004)

**YELLOW Students (4):**
- student05 - Hoàng Văn Em (110122005)
- student06 - Vũ Thị Phương (110122006)
- student07 - Đặng Văn Giang (110122007)
- student08 - Bùi Thị Hoa (110122008)

**GREEN Students (7):**
- student09 - Ngô Văn Khoa (110122009)
- student10 - Đinh Thị Lan (110122010)
- student11 - Võ Văn Minh (110122011)
- student12 - Lý Thị Nga (110122012)
- student13 - Trương Văn Phúc (110122013)
- student14 - Phan Thị Quỳnh (110122014)
- student15 - Dương Văn Sơn (110122015)

**Password:** `Student@2026`

### Grades:
- RED: 3.0 - 4.5
- YELLOW: 4.5 - 5.5
- GREEN: 7.0 - 9.0

## ✅ VERIFY

### Check Courses:
```
Site administration → Courses → Manage courses
```

### Check Students:
```
Site administration → Users → Browse list of users
```

### Check Enrollments:
```
Course → Participants
```

### Check Grades:
```
Course → Grades
```

## 🔄 CHẠY LẠI

Script kiểm tra dữ liệu đã tồn tại, nên có thể chạy lại an toàn.

- Nếu course đã tồn tại → Skip
- Nếu student đã tồn tại → Skip
- Nếu grade đã tồn tại → Skip

## 🎯 NEXT STEPS

Sau khi tạo dữ liệu xong:

1. **Enable Web Services**
   ```
   Site administration → Advanced features → Enable web services
   ```

2. **Create Token**
   ```
   Site administration → Web services → Manage tokens → Add
   ```

3. **Update application.yml**
   ```
   token: YOUR_NEW_TOKEN
   ```

4. **Enable DataSyncService**
   ```
   Uncomment @Scheduled trong DataSyncService.java
   ```

5. **Rebuild & Restart**
   ```
   mvn clean package -DskipTests
   mvn spring-boot:run
   ```

6. **Verify Sync**
   ```
   Check logs: "Data synchronization completed successfully"
   Check database: SELECT COUNT(*) FROM students;
   ```

## 🚨 TROUBLESHOOTING

### Error: "Must run as admin"
**Fix:** Login Moodle as admin first, then run script

### Error: "Could not create course"
**Fix:** Check category exists (category ID = 1)

### Error: "Could not enrol user"
**Fix:** Check manual enrolment plugin enabled

### Script chạy nhưng không thấy data
**Fix:** Check Moodle logs in Site administration → Reports → Logs

## 📞 FILES

- `create_test_data.php` - Main script
- `run_create_data.bat` - Windows batch file
- `README.md` - This file

---

**🎉 Chúc bạn thành công!**
