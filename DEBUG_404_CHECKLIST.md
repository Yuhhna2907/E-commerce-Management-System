# 🔍 DEBUG: Lỗi 404 - Ảnh Không Tải Được

## ✅ Checklist Kiểm Tra

### 1. Kiểm tra file có tồn tại không?

**Chạy lệnh này trong terminal:**
```bash
ls -la E-commerce-Management-System/src/main/resources/static/images/products/
```

**Kết quả mong đợi:**
```
iphone-15-pro.jpg
```

✅ **ĐÃ OK** - File đã tồn tại!

---

### 2. Kiểm tra tên file có đúng không?

**Lưu ý:**
- ❌ Sai: `iPhone 15 Pro.jpg` (có khoảng trắng, viết hoa)
- ❌ Sai: `iphone-15-pro .jpg` (có khoảng trắng thừa)
- ✅ Đúng: `iphone-15-pro.jpg` (không khoảng trắng, chữ thường)

**Nếu tên file sai, đổi tên:**
```bash
cd E-commerce-Management-System/src/main/resources/static/images/products/
mv "iPhone 15 Pro.jpg" "iphone-15-pro.jpg"
```

---

### 3. Kiểm tra database có đúng không?

**Chạy SQL này:**
```sql
SELECT id, name, image_url FROM products WHERE id = 21;
```

**Kết quả phải là:**
```
image_url = '/images/products/iphone-15-pro.jpg'
```

**Các trường hợp SAI thường gặp:**

❌ **Sai 1:** `image_url = 'iphone-15-pro.jpg'` (thiếu `/images/products/`)
```sql
-- Sửa:
UPDATE products 
SET image_url = '/images/products/iphone-15-pro.jpg' 
WHERE id = 21;
```

❌ **Sai 2:** `image_url = '/static/images/products/iphone-15-pro.jpg'` (thừa `/static`)
```sql
-- Sửa:
UPDATE products 
SET image_url = '/images/products/iphone-15-pro.jpg' 
WHERE id = 21;
```

❌ **Sai 3:** `image_url = 'images/products/iphone-15-pro.jpg'` (thiếu `/` đầu)
```sql
-- Sửa:
UPDATE products 
SET image_url = '/images/products/iphone-15-pro.jpg' 
WHERE id = 21;
```

❌ **Sai 4:** `image_url = '/images/products/iPhone 15 Pro.jpg'` (tên file có khoảng trắng)
```sql
-- Sửa:
UPDATE products 
SET image_url = '/images/products/iphone-15-pro.jpg' 
WHERE id = 21;
```

---

### 4. Kiểm tra server đã restart chưa?

**Sau khi:**
- Thêm file mới vào `static/`
- Cập nhật database

**Phải restart server:**
```bash
# Stop server (Ctrl + C)
# Sau đó start lại:
cd E-commerce-Management-System
./gradlew bootRun
```

⚠️ **LƯU Ý:** Spring Boot cache static resources, nên PHẢI restart!

---

### 5. Kiểm tra truy cập trực tiếp URL

**Mở trình duyệt, truy cập:**
```
http://localhost:8080/images/products/iphone-15-pro.jpg
```

**Kết quả:**
- ✅ Thấy ảnh → OK, file đúng vị trí
- ❌ 404 Not Found → File sai vị trí hoặc server chưa restart

---

### 6. Kiểm tra console browser

**Mở DevTools (F12) → Console tab**

Xem lỗi chi tiết, ví dụ:
```
GET http://localhost:8080/images/products/iphone-15-pro.jpg 404 (Not Found)
```

Hoặc:
```
GET http://localhost:8080/static/images/products/iphone-15-pro.jpg 404 (Not Found)
```

→ Nếu thấy `/static/` trong URL → Database sai, phải sửa lại

---

### 7. Kiểm tra Network tab

**Mở DevTools (F12) → Network tab → Reload trang**

Tìm request ảnh `iphone-15-pro.jpg`:
- Status: 404 → File không tìm thấy
- Status: 200 → File tải thành công

Click vào request để xem:
- Request URL: URL đầy đủ
- Response: Nội dung trả về

---

## 🔧 Script Sửa Lỗi Nhanh

**Chạy script này để đảm bảo mọi thứ đúng:**

```sql
-- 1. Kiểm tra hiện tại
SELECT id, name, image_url FROM products WHERE id = 21;

-- 2. Sửa đường dẫn đúng
UPDATE products 
SET image_url = '/images/products/iphone-15-pro.jpg',
    updated_at = NOW()
WHERE id = 21;

-- 3. Kiểm tra lại
SELECT id, name, image_url FROM products WHERE id = 21;
```

**Sau đó:**
1. Restart server: `./gradlew bootRun`
2. Xóa cache browser: `Ctrl + Shift + R`
3. Kiểm tra: `http://localhost:8080/user/products/21`

---

## 🎯 Nguyên Nhân Thường Gặp

### Nguyên nhân 1: Đường dẫn database sai (90% trường hợp)

**Kiểm tra:**
```sql
SELECT image_url FROM products WHERE id = 21;
```

**Phải là:**
```
/images/products/iphone-15-pro.jpg
```

**KHÔNG PHẢI:**
- ❌ `iphone-15-pro.jpg`
- ❌ `/static/images/products/iphone-15-pro.jpg`
- ❌ `images/products/iphone-15-pro.jpg`
- ❌ `/images/products/iPhone 15 Pro.jpg`

---

### Nguyên nhân 2: Server chưa restart (5% trường hợp)

Spring Boot cache static resources. Sau khi thêm file mới, PHẢI restart server.

---

### Nguyên nhân 3: Tên file không khớp (3% trường hợp)

Database: `/images/products/iphone-15-pro.jpg`  
File thực tế: `iPhone 15 Pro.jpg`

→ Không khớp! Phải đổi tên file hoặc sửa database.

---

### Nguyên nhân 4: File sai vị trí (2% trường hợp)

File phải ở:
```
E-commerce-Management-System/
└── src/main/resources/static/images/products/
    └── iphone-15-pro.jpg
```

KHÔNG PHẢI:
- ❌ `src/main/resources/images/products/` (thiếu `static/`)
- ❌ `src/main/resources/static/products/` (thiếu `images/`)
- ❌ `src/main/webapp/images/products/` (sai thư mục)

---

## 🆘 Nếu Vẫn Lỗi 404

**Gửi cho tôi kết quả của các lệnh này:**

1. **Kiểm tra file:**
```bash
ls -la E-commerce-Management-System/src/main/resources/static/images/products/
```

2. **Kiểm tra database:**
```sql
SELECT id, name, image_url FROM products WHERE id = 21;
```

3. **Kiểm tra URL trong console browser:**
- Mở F12 → Console
- Copy lỗi 404 đầy đủ

4. **Kiểm tra truy cập trực tiếp:**
- Mở: `http://localhost:8080/images/products/iphone-15-pro.jpg`
- Chụp màn hình kết quả

---

## ✅ Khi Nào Coi Như OK?

1. ✅ File tồn tại: `ls` thấy file
2. ✅ Database đúng: `image_url = '/images/products/iphone-15-pro.jpg'`
3. ✅ Server đã restart
4. ✅ Truy cập trực tiếp thấy ảnh: `http://localhost:8080/images/products/iphone-15-pro.jpg`
5. ✅ Trang chi tiết hiển thị ảnh: `http://localhost:8080/user/products/21`

---

**Chúc bạn fix thành công! 🚀**
