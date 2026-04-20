# 📋 Hướng dẫn: Lỗi 413 - Payload Too Large

## 🔴 Lỗi là gì?

```
HTTP 413 Payload Too Large
```

Lỗi này xảy ra khi bạn gửi dữ liệu (upload file) quá lớn vượt quá giới hạn của server.

---

## 🎯 Nguyên nhân

**Spring Boot mặc định chỉ cho phép upload tối đa 1MB!**

### Các thành phần kiểm soát kích thước:

| Thành phần | Mặc định | Tác dụng |
|-----------|---------|---------|
| `spring.servlet.multipart.max-file-size` | 1MB | Kích thước file tối đa |
| `spring.servlet.multipart.max-request-size` | 10MB | Kích thước toàn bộ request |
| `server.tomcat.max-http-form-post-size` | Không giới hạn | HTTP form data |

---

## ✅ Giải pháp đã được áp dụng

Tôi đã cấu hình trong `application.properties`:

```properties
# FILE UPLOAD CONFIGURATION
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
server.tomcat.max-http-form-post-size=10485760
```

### Ý nghĩa từng dòng:

1. **`spring.servlet.multipart.max-file-size=10MB`**
   - Cho phép upload **file tối đa 10MB**
   - Trước đó: 1MB → Giờ: 10MB

2. **`spring.servlet.multipart.max-request-size=10MB`**
   - Toàn bộ request (form + files) tối đa **10MB**
   - Bảo vệ server khỏi request quá lớn

3. **`server.tomcat.max-http-form-post-size=10485760`**
   - Cấu hình Tomcat servlet container
   - 10485760 bytes = 10MB

---

## 📊 So sánh cấu hình

### Trước (Default):
```
Max file size:         1MB ❌
Max request size:      10MB
Upload speed:          Chậm
Hình ảnh quality:      Limited
```

### Sau (Hiện tại):
```
Max file size:         10MB ✅
Max request size:      10MB ✅
Upload speed:          Nhanh hơn
Hình ảnh quality:      Cao hơn
```

---

## 🧪 Test chức năng

### Cách test upload:

1. **Truy cập** `/admin/products`
2. **Click** "Thêm sản phẩm"
3. **Upload ảnh** (≤ 10MB)
4. **Kết quả**: ✅ Không lỗi 413

### Upload files:
- ✅ JPG, PNG, GIF, WebP
- ✅ Tối đa 10MB
- ✅ Không lỗi

---

## ⚠️ Vấn đề có thể gặp

### 1️⃣ Nếu upload > 10MB:
```
HTTP 413 Payload Too Large
```
**Giải pháp**: Tăng giới hạn trong `application.properties`

### 2️⃣ Nếu upload lại còn lỗi:
```
Restart server!
```
Spring Boot chỉ đọc properties lúc startup.

### 3️⃣ Nếu muốn upload lớn hơn 10MB:
```properties
# Tăng lên 50MB
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
server.tomcat.max-http-form-post-size=52428800
```

---

## 🔒 Best Practices

### 1. Không nên để quá lớn:
```properties
# ❌ Quá rộng rãi
spring.servlet.multipart.max-file-size=100MB

# ✅ Hợp lý
spring.servlet.multipart.max-file-size=10MB
```

### 2. Validate phía client:
```javascript
const maxSize = 10 * 1024 * 1024; // 10MB
if (file.size > maxSize) {
    showError('File too large! Max 10MB');
}
```

### 3. Validate phía server:
```java
if (imageFile.getSize() > 10485760) {
    throw new BadRequestException("File too large!");
}
```

---

## 📝 Tóm tắt

| Phần | Chi tiết |
|-----|---------|
| **Lỗi** | 413 Payload Too Large |
| **Nguyên nhân** | File upload vượt giới hạn |
| **Giải pháp** | Tăng giới hạn trong `application.properties` |
| **Giới hạn hiện tại** | 10MB |
| **Cấu hình cần thay** | `spring.servlet.multipart.*` |
| **Cần restart** | Có, Spring Boot chỉ đọc lúc startup |

---

## 🚀 Kiểm tra cấu hình hiện tại

**File**: `src/main/resources/application.properties`

```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
server.tomcat.max-http-form-post-size=10485760
```

✅ **Đã cấu hình đúng!**

---

## 📞 Liên hệ

Nếu có vấn đề:
1. Check file `application.properties`
2. Restart server
3. Clear browser cache (Ctrl+Shift+Delete)
4. Test lại upload

**Giờ upload file lên đến 10MB mà không gặp lỗi 413!** 🎉

