# Hướng Dẫn Kiểm Tra Bảo Mật Thủ Công

**Dự án:** E-commerce Management System
**Spec:** Spring Security Enhancements
**Mục đích:** Xác minh tất cả các tính năng bảo mật hoạt động đúng cách

---

## Chuẩn Bị

### Yêu Cầu
- Ứng dụng đang chạy tại `http://localhost:8080`
- Có ít nhất 2 tài khoản test:
  - **User thường:** username=`testuser`, password=`ValidP@ss123`
  - **Admin:** username=`admin`, password=`Admin@123`
- Trình duyệt: Chrome, Firefox, Edge (để test concurrent sessions)
- Developer Tools (F12) đã bật

### Tạo Tài Khoản Test

```sql
-- Nếu chưa có, tạo tài khoản test trong database
INSERT INTO users (username, password, email, enabled, failed_login_attempts, lockout_time)
VALUES ('testuser', '$2a$10$...', 'test@example.com', true, 0, NULL);

-- Tạo admin account
INSERT INTO users (username, password, email, enabled, failed_login_attempts, lockout_time)
VALUES ('admin', '$2a$10$...', 'admin@example.com', true, 0, NULL);
```

---

## Test Suite 1: CSRF Protection (Requirement 2)

### Test 1.1: Form Submission với CSRF Token Hợp Lệ

**Mục tiêu:** Xác minh form với CSRF token được chấp nhận

**Các bước:**
1. Mở trình duyệt và truy cập `http://localhost:8080/login`
2. Mở Developer Tools (F12) → Network tab
3. Nhập username và password hợp lệ
4. Click "Login"
5. Trong Network tab, click vào request `do-login`
6. Kiểm tra tab "Payload" hoặc "Form Data"

**Kết quả mong đợi:**
- ✅ Request có parameter `_csrf` với giá trị token
- ✅ Response status: 302 (Redirect)
- ✅ Redirect đến `/user/products` (user) hoặc `/admin/dashboard` (admin)
- ✅ Đăng nhập thành công

**Ghi chú:**
```
Form Data:
username: testuser
password: ValidP@ss123
_csrf: a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

---

### Test 1.2: CSRF Attack Simulation (External Form)

**Mục tiêu:** Xác minh form từ external site bị reject

**Các bước:**
1. Tạo file `csrf-attack.html` trên desktop:

```html
<!DOCTYPE html>
<html>
<head>
    <title>CSRF Attack Test</title>
</head>
<body>
    <h1>CSRF Attack Simulation</h1>
    <p>This form attempts to update user profile without CSRF token</p>
    
    <form action="http://localhost:8080/user/profile/update" method="POST">
        <input type="text" name="email" value="hacker@evil.com">
        <input type="text" name="phone" value="0000000000">
        <button type="submit">Update Profile (Attack)</button>
    </form>
</body>
</html>
```

2. Mở `csrf-attack.html` trong trình duyệt
3. Ở tab khác, đăng nhập vào `http://localhost:8080` với tài khoản hợp lệ
4. Quay lại tab với `csrf-attack.html`
5. Click nút "Update Profile (Attack)"

**Kết quả mong đợi:**
- ✅ Response status: 403 Forbidden
- ✅ Hiển thị error page về CSRF protection
- ✅ Profile không bị thay đổi
- ✅ Log ghi nhận CSRF error trong `logs/security-audit.log`

**Kiểm tra log:**
```bash
tail -f logs/security-audit.log | grep CSRF_ERROR
```

---

### Test 1.3: Invalid CSRF Token

**Mục tiêu:** Xác minh form với CSRF token không hợp lệ bị reject

**Các bước:**
1. Đăng nhập vào ứng dụng
2. Truy cập trang có form (ví dụ: `/user/profile`)
3. Mở Developer Tools → Console
4. Chạy JavaScript để modify CSRF token:
```javascript
document.querySelector('input[name="_csrf"]').value = 'invalid-token-12345';
```
5. Submit form

**Kết quả mong đợi:**
- ✅ Response status: 403 Forbidden
- ✅ Hiển thị error page về CSRF protection
- ✅ Dữ liệu không được cập nhật

---

## Test Suite 2: Account Lockout (Requirement 3)

### Test 2.1: Khóa Tài Khoản Sau 5 Lần Thử Sai

**Mục tiêu:** Xác minh account lockout mechanism

**Các bước:**
1. Đảm bảo tài khoản `testuser` không bị khóa:
```sql
UPDATE users SET failed_login_attempts = 0, lockout_time = NULL WHERE username = 'testuser';
```

2. Thử đăng nhập với password SAI 5 lần liên tiếp:
   - Lần 1: Nhập password sai → Submit
   - Lần 2: Nhập password sai → Submit
   - Lần 3: Nhập password sai → Submit
   - Lần 4: Nhập password sai → Submit
   - Lần 5: Nhập password sai → Submit

3. Kiểm tra database:
```sql
SELECT username, failed_login_attempts, lockout_time 
FROM users 
WHERE username = 'testuser';
```

4. Thử đăng nhập lần thứ 6 với password ĐÚNG

**Kết quả mong đợi:**
- ✅ Lần 1-4: Hiển thị "Invalid username or password"
- ✅ Lần 5: Account bị khóa
- ✅ Database: `failed_login_attempts = 5`, `lockout_time = NOW() + 15 minutes`
- ✅ Lần 6: Hiển thị "Account is temporarily locked. Please try again in X minutes"
- ✅ Log ghi nhận:
  - 5 entries `[LOGIN_FAILURE]`
  - 1 entry `[ACCOUNT_LOCKED]`

**Kiểm tra log:**
```bash
tail -20 logs/security-audit.log | grep -E "LOGIN_FAILURE|ACCOUNT_LOCKED"
```

---

### Test 2.2: Tự Động Mở Khóa Sau 15 Phút

**Mục tiêu:** Xác minh automatic unlock

**Các bước:**

**Option A: Đợi thực tế (15 phút)**
1. Sau Test 2.1, đợi 16 phút
2. Thử đăng nhập với password đúng

**Option B: Test nhanh (modify database)**
1. Sau Test 2.1, chạy SQL:
```sql
UPDATE users 
SET lockout_time = NOW() - INTERVAL 1 MINUTE 
WHERE username = 'testuser';
```
2. Thử đăng nhập với password đúng

**Kết quả mong đợi:**
- ✅ Đăng nhập thành công
- ✅ Database: `lockout_time = NULL`, `failed_login_attempts = 0`
- ✅ Log ghi nhận:
  - `[ACCOUNT_UNLOCKED] username=testuser reason=Lockout period expired`
  - `[LOGIN_SUCCESS] username=testuser`

---

### Test 2.3: Reset Counter Khi Đăng Nhập Thành Công

**Mục tiêu:** Xác minh failed attempts counter được reset

**Các bước:**
1. Reset tài khoản:
```sql
UPDATE users SET failed_login_attempts = 0, lockout_time = NULL WHERE username = 'testuser';
```

2. Thử đăng nhập SAI 3 lần
3. Kiểm tra database:
```sql
SELECT failed_login_attempts FROM users WHERE username = 'testuser';
-- Kết quả: 3
```

4. Đăng nhập ĐÚNG lần thứ 4
5. Kiểm tra database lại:
```sql
SELECT failed_login_attempts FROM users WHERE username = 'testuser';
-- Kết quả: 0
```

**Kết quả mong đợi:**
- ✅ Sau 3 lần sai: `failed_login_attempts = 3`
- ✅ Sau đăng nhập đúng: `failed_login_attempts = 0`
- ✅ Log ghi nhận `[LOGIN_SUCCESS]`

---

## Test Suite 3: Session Management (Requirement 7)

### Test 3.1: Giới Hạn 2 Concurrent Sessions

**Mục tiêu:** Xác minh session limit enforcement

**Các bước:**
1. Mở Chrome → Đăng nhập với `testuser`
2. Mở Firefox → Đăng nhập với `testuser`
3. Mở Edge → Đăng nhập với `testuser` (session thứ 3)
4. Quay lại Chrome
5. Thử truy cập bất kỳ trang nào (ví dụ: `/user/cart`)

**Kết quả mong đợi:**
- ✅ Chrome: Session bị invalidate (session cũ nhất)
- ✅ Chrome: Redirect đến `/login?session=expired`
- ✅ Firefox và Edge: Vẫn đăng nhập bình thường
- ✅ Log ghi nhận `[SESSION_EXPIRED]`

---

### Test 3.2: Session Fixation Protection

**Mục tiêu:** Xác minh session ID thay đổi sau login

**Các bước:**
1. Mở trình duyệt Incognito
2. Truy cập `http://localhost:8080/login`
3. Mở Developer Tools → Application → Cookies
4. Ghi lại giá trị `JSESSIONID` TRƯỚC khi đăng nhập
   ```
   JSESSIONID (before): ABC123DEF456
   ```
5. Đăng nhập với tài khoản hợp lệ
6. Kiểm tra `JSESSIONID` SAU khi đăng nhập
   ```
   JSESSIONID (after): XYZ789GHI012
   ```

**Kết quả mong đợi:**
- ✅ `JSESSIONID` TRƯỚC ≠ `JSESSIONID` SAU
- ✅ Session ID thay đổi sau khi đăng nhập thành công

---

### Test 3.3: Session Timeout (30 phút)

**Mục tiêu:** Xác minh session timeout

**Các bước:**

**Option A: Đợi thực tế (30 phút)**
1. Đăng nhập vào ứng dụng
2. Không thực hiện bất kỳ action nào trong 31 phút
3. Thử truy cập trang bất kỳ

**Option B: Test nhanh (modify config)**
1. Tạm thời sửa `application.properties`:
```properties
server.servlet.session.timeout=1m
```
2. Restart ứng dụng
3. Đăng nhập
4. Đợi 2 phút
5. Thử truy cập trang bất kỳ

**Kết quả mong đợi:**
- ✅ Redirect đến `/login?session=expired`
- ✅ Hiển thị thông báo "Your session has expired. Please log in again."

---

## Test Suite 4: Security Headers (Requirement 8)

### Test 4.1: Kiểm Tra Headers với Browser DevTools

**Mục tiêu:** Xác minh tất cả security headers present

**Các bước:**
1. Mở trình duyệt
2. Truy cập `http://localhost:8080/user/products`
3. Mở Developer Tools (F12) → Network tab
4. Reload trang (Ctrl+R)
5. Click vào request đầu tiên (document)
6. Kiểm tra tab "Headers" → "Response Headers"

**Kết quả mong đợi:**

✅ **Content-Security-Policy:**
```
default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:
```

✅ **Strict-Transport-Security:**
```
max-age=31536000; includeSubDomains
```

✅ **X-Frame-Options:**
```
DENY
```

✅ **X-Content-Type-Options:**
```
nosniff
```

✅ **X-XSS-Protection:**
```
1; mode=block
```

✅ **Referrer-Policy:**
```
strict-origin-when-cross-origin
```

✅ **Permissions-Policy:**
```
geolocation=(), microphone=(), camera=()
```

---

### Test 4.2: Kiểm Tra Headers với curl

**Mục tiêu:** Xác minh headers qua command line

**Các bước:**
```bash
curl -I http://localhost:8080/user/products
```

**Kết quả mong đợi:**
```
HTTP/1.1 200 OK
Content-Security-Policy: default-src 'self'; ...
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=(), camera=()
```

---

## Test Suite 5: Remember-Me Functionality (Requirement 6)

### Test 5.1: Tạo Remember-Me Token

**Mục tiêu:** Xác minh token được tạo khi checkbox được chọn

**Các bước:**
1. Truy cập `http://localhost:8080/login`
2. Nhập username và password
3. **Check vào checkbox "Remember Me"**
4. Click "Login"
5. Mở Developer Tools → Application → Cookies
6. Kiểm tra cookie `remember-me`
7. Kiểm tra database:
```sql
SELECT * FROM persistent_logins WHERE username = 'testuser';
```

**Kết quả mong đợi:**
- ✅ Cookie `remember-me` tồn tại
- ✅ Cookie có `Max-Age` = 1209600 seconds (14 days)
- ✅ Database có record với `username`, `series`, `token`, `last_used`

---

### Test 5.2: Xác Thực Tự Động với Remember-Me Token

**Mục tiêu:** Xác minh auto-login với token

**Các bước:**
1. Sau Test 5.1, đóng trình duyệt HOÀN TOÀN (tất cả windows)
2. Mở trình duyệt mới
3. Truy cập `http://localhost:8080/user/products`

**Kết quả mong đợi:**
- ✅ Tự động đăng nhập (không cần nhập credentials)
- ✅ Có thể truy cập các trang yêu cầu authentication
- ✅ Header hiển thị username đã đăng nhập

---

### Test 5.3: Vô Hiệu Hóa Token Khi Logout

**Mục tiêu:** Xác minh token bị xóa khi logout

**Các bước:**
1. Sau Test 5.2 (đang tự động đăng nhập)
2. Click nút "Logout"
3. Kiểm tra cookies: `remember-me` cookie bị xóa
4. Kiểm tra database:
```sql
SELECT * FROM persistent_logins WHERE username = 'testuser';
-- Kết quả: 0 rows
```
5. Đóng và mở lại trình duyệt
6. Truy cập `http://localhost:8080/user/products`

**Kết quả mong đợi:**
- ✅ Cookie `remember-me` bị xóa
- ✅ Database không còn token
- ✅ Không tự động đăng nhập (redirect đến `/login`)

---

## Test Suite 6: Password Strength Validation (Requirement 4)

### Test 6.1: Password Không Hợp Lệ

**Mục tiêu:** Xác minh validation rules

**Các bước:**
Truy cập trang đăng ký và thử các password sau:

**Test Case 1: Quá ngắn**
- Password: `Short1!`
- ✅ Mong đợi: "Password must be at least 8 characters"

**Test Case 2: Thiếu chữ hoa**
- Password: `lowercase123!`
- ✅ Mong đợi: "Password must contain at least one uppercase letter"

**Test Case 3: Thiếu chữ thường**
- Password: `UPPERCASE123!`
- ✅ Mong đợi: "Password must contain at least one lowercase letter"

**Test Case 4: Thiếu số**
- Password: `NoDigits!`
- ✅ Mong đợi: "Password must contain at least one digit"

**Test Case 5: Thiếu ký tự đặc biệt**
- Password: `NoSpecial123`
- ✅ Mong đợi: "Password must contain at least one special character"

---

### Test 6.2: Password Hợp Lệ

**Mục tiêu:** Xác minh password hợp lệ được chấp nhận

**Các bước:**
1. Truy cập trang đăng ký
2. Nhập password: `ValidP@ss123`
3. Submit form

**Kết quả mong đợi:**
- ✅ Không có lỗi validation
- ✅ Đăng ký thành công
- ✅ Password được hash và lưu vào database

---

## Test Suite 7: Authorization Rules (Requirement 5)

### Test 7.1: Public Access (Không Cần Đăng Nhập)

**Mục tiêu:** Xác minh public endpoints accessible

**Các bước:**
Mở trình duyệt Incognito và truy cập các URLs sau:

✅ **Phải truy cập được:**
- `http://localhost:8080/login`
- `http://localhost:8080/register`
- `http://localhost:8080/user/products`
- `http://localhost:8080/user/products/1`
- `http://localhost:8080/css/style.css`
- `http://localhost:8080/js/script.js`

**Kết quả mong đợi:**
- ✅ Tất cả URLs trên truy cập được mà không cần đăng nhập
- ✅ Không redirect đến `/login`

---

### Test 7.2: Authenticated Access (Cần Đăng Nhập)

**Mục tiêu:** Xác minh user endpoints require authentication

**Các bước:**
1. Mở trình duyệt Incognito (chưa đăng nhập)
2. Truy cập các URLs sau:
   - `http://localhost:8080/user/cart`
   - `http://localhost:8080/user/orders`
   - `http://localhost:8080/user/profile`
   - `http://localhost:8080/user/wishlist`

3. Đăng nhập với user thường
4. Truy cập lại các URLs trên

**Kết quả mong đợi:**
- ✅ Bước 2: Redirect đến `/login` (chưa đăng nhập)
- ✅ Bước 4: Truy cập thành công (đã đăng nhập)

---

### Test 7.3: Admin Access (Chỉ Admin)

**Mục tiêu:** Xác minh admin endpoints require ROLE_ADMIN

**Các bước:**
1. Đăng nhập với **user thường** (`testuser`)
2. Truy cập `http://localhost:8080/admin/dashboard`
3. Logout
4. Đăng nhập với **admin** (`admin`)
5. Truy cập `http://localhost:8080/admin/dashboard`

**Kết quả mong đợi:**
- ✅ Bước 2: HTTP 403 Forbidden (user không có quyền)
- ✅ Bước 5: Truy cập thành công (admin có quyền)
- ✅ Log ghi nhận `[AUTHORIZATION_DENIED]` cho bước 2

---

## Test Suite 8: Security Audit Logs (Requirement 9)

### Test 8.1: Kiểm Tra Log File

**Mục tiêu:** Xác minh security events được log đúng cách

**Các bước:**
1. Thực hiện các actions sau:
   - Đăng nhập thành công
   - Đăng nhập thất bại
   - Logout
   - Truy cập trang không có quyền

2. Kiểm tra file log:
```bash
cat logs/security-audit.log
```

**Kết quả mong đợi:**

✅ **Login Success:**
```
2024-01-15 10:30:45.123 [SECURITY_AUDIT] INFO  - [LOGIN_SUCCESS] username=testuser ip=127.0.0.1 userAgent=Mozilla/5.0...
```

✅ **Login Failure:**
```
2024-01-15 10:31:12.456 [SECURITY_AUDIT] WARN  - [LOGIN_FAILURE] username=testuser ip=127.0.0.1 reason=Invalid credentials
```

✅ **Logout:**
```
2024-01-15 10:45:30.789 [SECURITY_AUDIT] INFO  - [LOGOUT] username=testuser sessionDuration=15 minutes
```

✅ **Account Locked:**
```
2024-01-15 10:32:00.111 [SECURITY_AUDIT] WARN  - [ACCOUNT_LOCKED] username=testuser reason=Exceeded maximum failed login attempts (5) lockDuration=15 minutes
```

✅ **Authorization Denied:**
```
2024-01-15 10:35:15.222 [SECURITY_AUDIT] WARN  - [AUTHORIZATION_DENIED] username=testuser resource=/admin/dashboard requiredRole=ROLE_ADMIN
```

---

### Test 8.2: Xác Minh Không Rò Rỉ Thông Tin Nhạy Cảm

**Mục tiêu:** Đảm bảo passwords/tokens không bị log

**Các bước:**
1. Đăng nhập với password `ValidP@ss123`
2. Kiểm tra tất cả log files:
```bash
grep -r "ValidP@ss123" logs/
grep -r "password" logs/security-audit.log
```

**Kết quả mong đợi:**
- ✅ Không tìm thấy password trong logs
- ✅ Không tìm thấy password hash trong logs
- ✅ Không tìm thấy tokens trong logs

---

## Checklist Tổng Hợp

### CSRF Protection
- [ ] Form với CSRF token hợp lệ được chấp nhận
- [ ] Form từ external site bị reject (403)
- [ ] Form với CSRF token không hợp lệ bị reject (403)

### Account Lockout
- [ ] Account bị khóa sau 5 lần đăng nhập sai
- [ ] Account tự động mở khóa sau 15 phút
- [ ] Failed attempts counter được reset khi đăng nhập thành công
- [ ] Tất cả events được ghi vào security audit log

### Session Management
- [ ] Session thứ 3 invalidate session cũ nhất
- [ ] Session ID thay đổi sau khi đăng nhập (session fixation protection)
- [ ] Session timeout sau 30 phút không hoạt động

### Security Headers
- [ ] Content-Security-Policy present
- [ ] Strict-Transport-Security present
- [ ] X-Frame-Options: DENY
- [ ] X-Content-Type-Options: nosniff
- [ ] X-XSS-Protection: 1; mode=block
- [ ] Referrer-Policy present
- [ ] Permissions-Policy present

### Remember-Me
- [ ] Token được tạo khi checkbox được chọn
- [ ] Tự động đăng nhập với token hợp lệ
- [ ] Token bị vô hiệu hóa khi logout

### Password Validation
- [ ] Tất cả password không hợp lệ bị reject với thông báo rõ ràng
- [ ] Password hợp lệ được chấp nhận

### Authorization Rules
- [ ] Public endpoints accessible without authentication
- [ ] User endpoints require authentication
- [ ] Admin endpoints require ROLE_ADMIN
- [ ] Unauthorized access returns 403 Forbidden

### Security Audit Logs
- [ ] Login success events được log
- [ ] Login failure events được log
- [ ] Logout events được log
- [ ] Account lock/unlock events được log
- [ ] Authorization denied events được log
- [ ] Không có password/token trong logs

---

## Kết Luận

Sau khi hoàn thành tất cả tests trên, hệ thống bảo mật đã được xác minh đầy đủ và sẵn sàng cho production deployment.

**Lưu ý quan trọng:**
- Tất cả tests phải PASS trước khi deploy
- Security audit logs phải được giám sát thường xuyên
- Định kỳ review và update security configurations
- Thực hiện penetration testing định kỳ

**Liên hệ:** Nếu phát hiện bất kỳ vấn đề bảo mật nào, báo cáo ngay cho team security.
