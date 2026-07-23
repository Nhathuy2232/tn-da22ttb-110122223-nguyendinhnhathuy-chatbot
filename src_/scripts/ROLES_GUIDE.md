# Hướng dẫn Phân quyền EduGuard Chatbot

Tài liệu này mô tả 4 role được sử dụng trong hệ thống Moodle + EduGuard Chatbot.

## 1. Tổng quan 4 Role

| # | Role Moodle (`shortname`) | Role Chatbot | Mô tả |
|---|---|---|---|
| 1 | `manager` | `ADMIN` | Quản trị viên hệ thống. Toàn quyền. |
| 2 | `academicadviser` | `ADVISER` | Cố vấn học tập. Xem dữ liệu lớp chủ nhiệm, không sửa điểm. |
| 3 | `editingteacher` / `teacher` | `LECTURER` | Giảng viên. Quản lý lớp, nhập điểm, theo dõi SV. |
| 4 | `student` | `STUDENT` | Sinh viên. Chỉ xem dữ liệu của bản thân. |

> **Thứ tự ưu tiên** (khi user có nhiều role): `ADMIN > ADVISER > LECTURER > STUDENT`

## 2. Cài đặt

### 2.1. Chạy script tạo role

```bash
cd c:\xampp\htdocs\moodle
php install_roles.php
```

Script này sẽ:
- Tạo role `academicadviser` (nếu chưa có) với 27 quyền
- Cập nhật `context_level` cho role Adviser ở tất cả các cấp
- Không thay đổi các role Moodle có sẵn

### 2.2. Gán role cho user

```bash
# Gán role Sinh viên
php assign_role.php --username=110122001 --role=student

# Gán role Giảng viên
php assign_role.php --username=gv.nguyenvana --role=editingteacher

# Gán role Cố vấn học tập
php assign_role.php --username=gv.levanc --role=academicadviser

# Gán role Admin
php assign_role.php --username=admin --role=manager

# Xem role hiện tại của 1 user
php assign_role.php --user=110122001

# Liệt kê các role có thể gán
php assign_role.php --list
```

### 2.3. Xóa role khỏi user

Trong giao diện Moodle:
`Site administration → Users → Assign system roles`

Hoặc qua CLI:
```php
// Unassign code mẫu
$syscontext = context_system::instance();
role_unassign($roleid, $userid, $syscontext->id);
```

## 3. Chi tiết quyền của từng role

### 3.1. ADMIN (`manager`)
- **Toàn quyền Moodle** (564 capabilities)
- Cấu hình hệ thống
- Trigger đồng bộ dữ liệu từ Moodle API
- Cấu hình ngưỡng cảnh báo (xanh/vàng/đỏ)
- Xem tất cả báo cáo

### 3.2. ADVISER (`academicadviser`) - 27 quyền

**Được phép (ALLOW):**
- `moodle/site:viewuseridentity`
- `moodle/user:viewdetails`, `moodle/user:viewalldetails`
- `moodle/course:view`, `moodle/course:viewparticipants`
- `moodle/grade:viewall`, `moodle/grade:view`, `moodle/grade:readall`, `moodle/grade:export`
- `moodle/site:viewreports`, `moodle/site:viewanonymised`
- `mod/assign:view`, `mod/quiz:view`
- `mod/attendance:viewreports`, `mod/attendance:view`
- `mod/forum:viewdiscussion`
- `report/outline:view`, `report/participation:view`, `report/log:view`

**Bị cấm (PREVENT):**
- `moodle/grade:edit`, `moodle/grade:manage`, `moodle/grade:delete`
- `mod/assign:grade`, `mod/quiz:grade`

> Cố vấn học tập được **xem** nhưng **không được sửa** điểm.

### 3.3. LECTURER (`editingteacher`)
- Quyền đầy đủ của `editingteacher` trong Moodle (465 capabilities)
- Trong khóa học được phân công: nhập điểm, chấm bài, quản lý sinh viên
- Xem danh sách sinh viên nguy cơ cao
- Gửi nhắc nhở nộp bài

### 3.4. STUDENT (`student`)
- Quyền mặc định của sinh viên trong Moodle
- Chỉ xem được dữ liệu cá nhân (điểm của mình, chuyên cần của mình)

## 4. Mapping role → chatbot

Mapping được thực hiện trong hàm `theme_moove_chatbot_resolve_role()` ở file `theme/moove/layout/drawers.php`.

```php
$priority = [
    'ADMIN'    => ['manager', 'admin'],
    'ADVISER'  => ['academicadviser', 'adviser', 'covan', 'coursecreator'],
    'LECTURER' => ['editingteacher', 'teacher'],
    'STUDENT'  => ['student'],
];
```

## 5. Test nhanh

### 5.1. Chuẩn bị user test

```bash
# Gán role cho 4 user mẫu
php assign_role.php --username=gv.levanc --role=academicadviser    # Cố vấn
php assign_role.php --username=gv.trinhquocviet --role=editingteacher  # Giảng viên
php assign_role.php --username=110122001 --role=student              # Sinh viên
php assign_role.php --username=webservice --role=manager             # Admin
```

### 5.2. Test phân quyền chatbot

Sau khi đăng nhập với mỗi user, mở chat widget và thử các câu:

| Role | Câu test | Kỳ vọng |
|---|---|---|
| STUDENT | "Xem điểm của tôi" | Hiển thị điểm cá nhân |
| STUDENT | "Danh sách sinh viên nguy cơ" | ❌ "Bạn không có quyền" |
| LECTURER | "Sinh viên chưa nộp bài" | Hiển thị danh sách lớp |
| LECTURER | "Cấu hình ngưỡng cảnh báo" | ❌ "Bạn không có quyền" |
| ADVISER | "Tình hình lớp chủ nhiệm" | Hiển thị tổng quan lớp |
| ADVISER | "Sinh viên chưa nộp bài môn X" | ❌ "Bạn không có quyền" |
| ADMIN | "Cấu hình ngưỡng" | ✅ Cho phép |
| ADMIN | "Đồng bộ dữ liệu" | ✅ Cho phép |

## 6. Troubleshooting

### Lỗi "Cannot load Zend OPcache"
Bỏ qua, không ảnh hưởng. Hoặc tắt OPcache trong `php.ini`:
```ini
[opcache]
opcache.enable=0
```

### Role không hiển thị trong Moodle UI
Chạy lại `install_roles.php` để đảm bảo role đã được tạo với đầy đủ quyền.

### User đã login nhưng chatbot báo "STUDENT" dù là giảng viên
Xóa cache Moodle:
- Vào: `Site administration → Development → Purge all caches`
- Hoặc truy cập: `http://localhost/moodle/clear_cache.php` (yêu cầu admin)

## 7. File liên quan

| File | Mô tả |
|---|---|
| `install_roles.php` | Tạo role academicadviser + gán quyền |
| `check_roles_status.php` | Xem trạng thái các role trong DB |
| `assign_role.php` | Gán/xem role cho user |
| `find_test_users.php` | Tìm user mẫu để test |
| `theme/moove/layout/drawers.php` | Hàm resolve role → chatbot |
| `chatbot-system/.../LocalIntentMatcher.java` | Kiểm tra quyền theo intent |
| `clear_cache.php` | Xóa cache Moodle (khi cần) |
| `test_footer.php` | Test hiển thị footer |
