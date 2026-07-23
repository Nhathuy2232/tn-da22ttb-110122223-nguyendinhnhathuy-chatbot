# Tài khoản demo cho hệ thống chatbot

> File này dùng cho mục đích demo / báo cáo đồ án. Không nên dùng mật khẩu này cho môi trường thật.

| Vai trò | Tài khoản | Mật khẩu | Ghi chú |
|---|---|---|---|
| Sinh viên | sv.110122001 | sv123456 | Sinh viên mẫu |
| Sinh viên | sv.110122002 | sv123456 | Sinh viên mẫu |
| Sinh viên | sv.110122003 | sv123456 | Sinh viên mẫu |
| Sinh viên | sv.110122004 | sv123456 | Sinh viên mẫu |
| Sinh viên | sv.110122005 | sv123456 | Sinh viên mẫu |
| Giáo viên | gv.nguyenvana | gv123456 | Giảng viên mẫu |
| Giáo viên | gv.tranthib | gv123456 | Giảng viên mẫu |
| Cố vấn học tập | cv.hoangc | cv123456 | Cố vấn mẫu |
| Cố vấn học tập | cv.lethid | cv123456 | Cố vấn mẫu |
| Quản trị viên | admin | admin123456 | Tài khoản quản trị |

## Quy ước đăng nhập
- `username` là định danh dùng cho chatbot và phân quyền.
- `role` tương ứng:
  - `STUDENT`
  - `LECTURER` / `TEACHER`
  - `ADVISOR`
  - `ADMIN`

## Gợi ý cấu hình dữ liệu
Nếu cần import vào hệ thống, có thể dùng các trường:
- `username`
- `password`
- `fullName`
- `role`
- `studentCode` hoặc `teacherCode`
- `email`

## Lưu ý
- Đây chỉ là tài khoản demo để phục vụ thử nghiệm.
- Nếu triển khai thật, cần mã hóa mật khẩu bằng BCrypt và không lưu plain text.
