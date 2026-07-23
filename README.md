# 🤖 Hệ Thống Chatbot Web  
### Đồ án: `tn-da22ttb-110122223-nguyendinhnhathuy-chatbot`

<div align="center">

![Repo Name](https://img.shields.io/badge/Repository-Nhathuy2232%2Ftn--da22ttb--110122223--nguyendinhnhathuy--chatbot-blue?style=for-the-badge)
![Top Language](https://img.shields.io/github/languages/top/Nhathuy2232/tn-da22ttb-110122223-nguyendinhnhathuy-chatbot?style=for-the-badge)
![Last Commit](https://img.shields.io/github/last-commit/Nhathuy2232/tn-da22ttb-110122223-nguyendinhnhathuy-chatbot?style=for-the-badge)
![Issues](https://img.shields.io/github/issues/Nhathuy2232/tn-da22ttb-110122223-nguyendinhnhathuy-chatbot?style=for-the-badge)
![Stars](https://img.shields.io/github/stars/Nhathuy2232/tn-da22ttb-110122223-nguyendinhnhathuy-chatbot?style=for-the-badge)

</div>

---

## 📌 Thông tin đề tài

- **Repository:** [Nhathuy2232/tn-da22ttb-110122223-nguyendinhnhathuy-chatbot](https://github.com/Nhathuy2232/tn-da22ttb-110122223-nguyendinhnhathuy-chatbot)  
- **Repo ID:** `1265369999`  
- **Sinh viên thực hiện:** Nguyễn Đình Nhật Huy *(cập nhật lại nếu cần)*  
- **MSSV:** `110122223`  
- **Loại dự án:** Ứng dụng chatbot trên nền tảng web  

---

## 📖 Giới thiệu

Dự án xây dựng một hệ thống chatbot giúp người dùng tương tác trực tiếp qua giao diện web.  
Hệ thống tập trung vào tính dễ sử dụng, khả năng mở rộng và phù hợp cho mục đích học tập/nghiên cứu.

---

## 🎯 Mục tiêu dự án

- Xây dựng chatbot hoạt động ổn định trên web  
- Quản lý luồng hội thoại người dùng ↔ hệ thống  
- Tối ưu giao diện tương tác trực quan  
- Tạo nền tảng để mở rộng AI/NLP trong tương lai  

---

## 🛠️ Công nghệ sử dụng (theo dữ liệu thực tế repo)

### 🔹 Language Composition

| Ngôn ngữ | Tỷ lệ |
|---------|------:|
| PHP | **77.2%** |
| JavaScript | **14.7%** |
| Gherkin | **3.6%** |
| CSS | **2.1%** |
| Mustache | **1.7%** |
| SCSS | **0.4%** |
| Other | **0.3%** |

> Với tỷ lệ này, backend của hệ thống chủ yếu phát triển bằng **PHP**, kết hợp **JavaScript/CSS** cho phần giao diện và có sử dụng template/test behavior.

---

## 🧩 Kiến trúc tổng quan

```text
[Người dùng]
     │
     ▼
[Frontend: JS/CSS/Mustache]
     │
     ▼
[Backend: PHP]
     │
     ├── Xử lý hội thoại chatbot
     ├── Điều phối dữ liệu
     └── Tích hợp dịch vụ ngoài (nếu có)
```

---

## ⚙️ Hướng dẫn cài đặt nhanh

### 1) Clone source code

```bash
git clone https://github.com/Nhathuy2232/tn-da22ttb-110122223-nguyendinhnhathuy-chatbot.git
cd tn-da22ttb-110122223-nguyendinhnhathuy-chatbot
```

### 2) Cài đặt dependencies

Nếu dự án dùng Composer:

```bash
composer install
```

Nếu có frontend package:

```bash
npm install
```

### 3) Chạy ứng dụng (tham khảo)

```bash
php -S localhost:8000 -t public
```

Sau đó truy cập: `http://localhost:8000`

> Nếu repo của bạn dùng framework cụ thể (Laravel/Symfony/khác), hãy thay lệnh chạy tương ứng ở phần này.

---

## 🧪 Kiểm thử

Tùy cấu hình dự án, có thể dùng:

```bash
# PHPUnit
vendor/bin/phpunit

# hoặc
composer test
```

Nếu có test theo Gherkin/BDD thì thêm hướng dẫn chạy tương ứng tại đây.

---

## 📁 Cấu trúc dự án

```text
.
tn-da22ttb-110122223-nguyendinhnhathuy-chatbot/
├── .github/
├── .grunt/
├── .phpstorm.meta.php/
├── .upgradenotes/
├── .vscode/
├── README.md
├── doc/
│   ├── 110122223_Bao_cao_KLTN.docx
│   ├── 110122223_Bao_cao_KLTN.pdf
│   ├── BIACHINH_.docx
│   ├── Poster_110122223.pdf
│   └── Slide.pptx
└── src_/
    ├── admin/
    ├── analytics/
    ├── auth/
    ├── availability/
    ├── backup/
    ├── badges/
    ├── blocks/
    ├── blog/
    ├── cache/
    ├── calendar/
    ├── chatbot-system/
    ├── cohort/
    ├── comment/
    ├── communication/
    ├── competency/
    ├── completion/
    ├── contentbank/
    ├── course/
    ├── customfield/
    ├── dataformat/
    ├── enrol/
    ├── error/
    ├── favourites/
    ├── files/
    ├── filter/
    ├── grade/
    ├── group/
    ├── h5p/
    ├── install/
    ├── iplookup/
    ├── lang/
    ├── lib/
    ├── local/
    ├── login/
    ├── media/
    ├── message/
    ├── mnet/
    ├── mod/
    ├── moodlenet/
    ├── my/
    ├── notes/
    ├── payment/
    ├── pix/
    ├── plagiarism/
    ├── portfolio/
    ├── privacy/
    ├── question/
    ├── rating/
    ├── report/
    ├── reportbuilder/
    ├── repository/
    ├── rss/
    ├── scripts/
    ├── search/
    ├── src/
    ├── tag/
    ├── theme/
    ├── user/
    ├── userpix/
    ├── webservice/
    ├── .eslintrc
    ├── .gherkin-lintrc
    ├── .gitattributes
    ├── .gitignore
    ├── .jshintignore
    ├── .jshintrc
    ├── .nvmrc
    ├── .shifter.json
    ├── .stylelintrc
    ├── .user.ini
    ├── CONTRIBUTING.md
    ├── COPYING.txt
    ├── Gruntfile.js
    ├── INSTALL.txt
    ├── SIMPLE_FIX.txt
    ├── TRADEMARK.txt
    ├── behat.yml.dist
    ├── check_roles.php
    ├── composer.json
    ├── composer.lock
    ├── config-dist.php
    ├── draftfile.php
    ├── file.php
    ├── help.php
    ├── help_ajax.php
    ├── index.php
    ├── install.php
    ├── npm-shrinkwrap.json
    ├── package.json
    ├── phpcs.xml.dist
    ├── phpunit.xml.dist
    ├── pluginfile.php
    ├── purge_cache.bat
    ├── run_seed.bat
    ├── run_verify.bat
    ├── security.txt
    ├── seed_data.php
    ├── test_api.html
    ├── tokenpluginfile.php
    ├── verify_data.php
    └── version.php
```

> Bạn nên cập nhật lại phần này theo đúng cây thư mục thực tế trong repo.

---

## 🚀 Kế hoạch phát triển

- [ ] Hoàn thiện kiến trúc module chatbot  
- [ ] Tăng chất lượng phản hồi hội thoại  
- [ ] Bổ sung logging & monitoring  
- [ ] Mở rộng test coverage  
- [ ] Tích hợp mô hình AI/NLP nâng cao  

---

## 🤝 Đóng góp

Mọi đóng góp đều được chào đón:

1. Fork repository  
2. Tạo branch mới (`feature/ten-tinh-nang`)  
3. Commit thay đổi  
4. Tạo Pull Request  

---

## 🐛 Báo lỗi & yêu cầu tính năng

- Tạo issue tại đây:  
  👉 [Issues](https://github.com/Nhathuy2232/tn-da22ttb-110122223-nguyendinhnhathuy-chatbot/issues)

---
