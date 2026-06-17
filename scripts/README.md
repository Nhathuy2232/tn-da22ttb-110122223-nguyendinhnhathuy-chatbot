# Scripts thủ công / tiện ích

Folder này chứa các script PHP/JSON dùng để **seed dữ liệu, kiểm tra, dọn dẹp cache** trong Moodle. KHÔNG chạy tự động trong production.

## Seed dữ liệu
- `seed_moodle_data.php` — seed users, courses, assignments (gọi các helper)
- `seed_sub_sql.php` — seed bằng raw SQL
- `seed_submissions.php` / `seed_submissions_full.php` — seed submission + grading

## Phân quyền (roles)
- `install_roles.php` — cài custom role (counselor, ...)
- `check_roles_status.php` — kiểm tra role hiện có
- `assign_role.php` — gán role cho user
- `find_test_users.php` — tìm user test theo cohort
- `ROLES_GUIDE.md` — hướng dẫn sử dụng roles

## Tiện ích
- `clear_cache.php` — purge Moodle cache (theme, JS, string)
- `test_footer.php` — test footer trên cả boost/moove
- `test_chat.json` — payload mẫu để test API chatbot

## Cách chạy

```bash
# Từ thư mục gốc Moodle
php scripts/clear_cache.php
php scripts/seed_moodle_data.php
```
