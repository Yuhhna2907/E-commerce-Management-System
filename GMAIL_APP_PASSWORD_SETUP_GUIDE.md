# Hướng Dẫn Lấy Gmail App Password Cho luonganhhuy2004@gmail.com

## Bước 1: Bật 2-Step Verification (Xác minh 2 bước)

### 1.1. Truy cập Google Account
1. Mở trình duyệt và truy cập: https://myaccount.google.com/
2. Đăng nhập bằng tài khoản: **luonganhhuy2004@gmail.com**

### 1.2. Vào Security Settings
1. Ở menu bên trái, click vào **"Security"** (Bảo mật)
2. Hoặc truy cập trực tiếp: https://myaccount.google.com/security

### 1.3. Bật 2-Step Verification
1. Tìm mục **"2-Step Verification"** (Xác minh 2 bước)
2. Click vào **"2-Step Verification"**
3. Click nút **"Get Started"** (Bắt đầu)
4. Nhập lại mật khẩu Gmail của bạn
5. Chọn phương thức xác minh:
   - **Khuyến nghị**: Dùng số điện thoại (nhận SMS hoặc gọi điện)
   - Nhập số điện thoại của bạn
   - Chọn nhận mã qua SMS hoặc gọi điện
6. Nhập mã xác minh nhận được
7. Click **"Turn On"** (Bật)

✅ **Xong bước 1!** 2-Step Verification đã được bật.

---

## Bước 2: Tạo App Password

### 2.1. Truy cập App Passwords
1. Vẫn ở trang Security: https://myaccount.google.com/security
2. Tìm mục **"2-Step Verification"**
3. Scroll xuống dưới, tìm **"App passwords"** (Mật khẩu ứng dụng)
4. Click vào **"App passwords"**
5. Hoặc truy cập trực tiếp: https://myaccount.google.com/apppasswords

**LƯU Ý**: Nếu không thấy "App passwords", có thể do:
- Chưa bật 2-Step Verification (quay lại Bước 1)
- Tài khoản dùng Google Workspace (liên hệ admin)

### 2.2. Tạo App Password Mới
1. Tại trang App passwords, bạn sẽ thấy dropdown **"Select app"**
2. Click vào dropdown, chọn **"Mail"**
3. Click vào dropdown **"Select device"**, chọn **"Other (Custom name)"**
4. Nhập tên: **"SmartZone Spring Boot App"** (hoặc tên bạn muốn)
5. Click nút **"Generate"** (Tạo)

### 2.3. Copy App Password
1. Google sẽ hiển thị một mật khẩu 16 ký tự, ví dụ:
   ```
   abcd efgh ijkl mnop
   ```
2. **QUAN TRỌNG**: Copy mật khẩu này ngay! (Bỏ dấu cách)
   ```
   abcdefghijklmnop
   ```
3. Click **"Done"**

⚠️ **CHÚ Ý**: Bạn chỉ thấy mật khẩu này 1 lần duy nhất! Nếu mất, phải tạo lại.

✅ **Xong bước 2!** Bạn đã có App Password.

---

## Bước 3: Cấu Hình Trong Spring Boot

### 3.1. Thêm vào application.properties

Mở file `E-commerce-Management-System/src/main/resources/application.properties` và thêm:

```properties
# ===== EMAIL CONFIGURATION =====
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME}
spring.mail.password=${EMAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Base URL for email links
app.base-url=http://localhost:8080
```

### 3.2. Cấu hình Environment Variables

**Cách 1: Trong IntelliJ IDEA (Khuyến nghị cho Development)**

1. Click vào **Run** → **Edit Configurations...**
2. Chọn configuration của Spring Boot app
3. Tìm mục **"Environment variables"**
4. Click vào icon folder hoặc nhập trực tiếp:
   ```
   EMAIL_USERNAME=luonganhhuy2004@gmail.com;EMAIL_PASSWORD=abcdefghijklmnop
   ```
   (Thay `abcdefghijklmnop` bằng App Password bạn vừa copy)

5. Click **"Apply"** → **"OK"**

**Cách 2: Trong Windows PowerShell (Cho cả hệ thống)**

Mở PowerShell và chạy:
```powershell
# Set environment variables
$env:EMAIL_USERNAME="luonganhhuy2004@gmail.com"
$env:EMAIL_PASSWORD="abcdefghijklmnop"

# Verify
echo $env:EMAIL_USERNAME
echo $env:EMAIL_PASSWORD
```

**Cách 3: Tạo file .env (Khuyến nghị cho Production)**

1. Tạo file `.env` trong thư mục root project:
   ```
   E-commerce-Management-System/.env
   ```

2. Thêm nội dung:
   ```env
   EMAIL_USERNAME=luonganhhuy2004@gmail.com
   EMAIL_PASSWORD=abcdefghijklmnop
   ```

3. Thêm `.env` vào `.gitignore`:
   ```gitignore
   .env
   ```

4. Cài đặt thư viện đọc .env (nếu cần):
   ```gradle
   implementation 'me.paulschwarz:spring-dotenv:2.5.4'
   ```

---

## Bước 4: Test Email Configuration

### 4.1. Tạo Test Controller (Optional)

Tạo file `controller/EmailTestController.java`:

```java
package com.codegym.smartphonemanagement.controller;

import com.codegym.smartphonemanagement.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmailTestController {
    
    private final EmailService emailService;
    
    @GetMapping("/test-email")
    public String testEmail(@RequestParam String to) {
        try {
            emailService.sendPasswordResetEmail(
                to,
                "Test User",
                "http://localhost:8080/reset-password?token=test123"
            );
            return "Email sent successfully to: " + to;
        } catch (Exception e) {
            return "Failed to send email: " + e.getMessage();
        }
    }
}
```

### 4.2. Test Gửi Email

1. Chạy Spring Boot application
2. Mở trình duyệt, truy cập:
   ```
   http://localhost:8080/test-email?to=your-test-email@gmail.com
   ```
3. Kiểm tra email inbox

---

## Troubleshooting (Xử lý lỗi)

### Lỗi 1: "Username and Password not accepted"
**Nguyên nhân**: App Password sai hoặc chưa bật 2-Step Verification

**Giải pháp**:
1. Kiểm tra lại App Password (không có dấu cách)
2. Đảm bảo 2-Step Verification đã bật
3. Tạo lại App Password mới

### Lỗi 2: "Could not connect to SMTP host"
**Nguyên nhân**: Firewall hoặc antivirus chặn port 587

**Giải pháp**:
1. Tắt tạm thời firewall/antivirus để test
2. Thử đổi port sang 465 và dùng SSL:
   ```properties
   spring.mail.port=465
   spring.mail.properties.mail.smtp.ssl.enable=true
   ```

### Lỗi 3: "Authentication failed"
**Nguyên nhân**: Environment variables chưa được load

**Giải pháp**:
1. Restart IntelliJ IDEA
2. Rebuild project
3. Kiểm tra lại environment variables trong Run Configuration

### Lỗi 4: "Less secure app access"
**Nguyên nhân**: Gmail yêu cầu App Password, không dùng mật khẩu thường

**Giải pháp**:
- Phải dùng App Password (16 ký tự), KHÔNG dùng mật khẩu Gmail thường

---

## Thông Tin Cấu Hình Cho Bạn

**Email Username**: `luonganhhuy2004@gmail.com`  
**Email Password**: `[App Password 16 ký tự bạn vừa tạo]`  
**SMTP Host**: `smtp.gmail.com`  
**SMTP Port**: `587` (hoặc `465` nếu dùng SSL)  
**TLS/SSL**: Enabled

---

## Bảo Mật

⚠️ **QUAN TRỌNG**:
1. **KHÔNG BAO GIỜ** commit App Password vào Git
2. **LUÔN LUÔN** dùng environment variables
3. **THÊM** `.env` vào `.gitignore`
4. **XÓA** App Password cũ nếu không dùng nữa
5. **TẠO MỚI** App Password cho mỗi ứng dụng khác nhau

---

## Tóm Tắt Nhanh

```bash
# 1. Bật 2-Step Verification tại:
https://myaccount.google.com/security

# 2. Tạo App Password tại:
https://myaccount.google.com/apppasswords

# 3. Copy App Password (16 ký tự)

# 4. Set environment variables:
EMAIL_USERNAME=luonganhhuy2004@gmail.com
EMAIL_PASSWORD=[your-16-char-app-password]

# 5. Restart app và test!
```

---

## Liên Hệ

Nếu gặp vấn đề, kiểm tra:
1. 2-Step Verification đã bật chưa?
2. App Password có đúng không? (16 ký tự, không có dấu cách)
3. Environment variables đã set chưa?
4. Firewall có chặn port 587 không?

Good luck! 🚀
