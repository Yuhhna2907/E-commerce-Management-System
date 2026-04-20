# 🚀 QUICK START - Thêm Sản Phẩm Mới

## 📝 Các Bước Thêm Sản Phẩm (5 Phút)

### **Bước 1: Truy cập trang Quản Lý Sản Phẩm**
```
URL: http://localhost:8080/admin/products
```

### **Bước 2: Nhấn Nút "Thêm Sản Phẩm"**
```
- Nút màu đỏ cam ở góc phải
- Cửa sổ modal sẽ mở ra
```

### **Bước 3: Điền Thông Tin Bắt Buộc** (*)
```
Tên sản phẩm: [Nhập >= 2 ký tự]
  ✓ Ví dụ: iPhone 15 Pro

Hãng sản xuất: [Nhập >= 2 ký tự]
  ✓ Ví dụ: Apple

Danh mục: [Chọn từ dropdown]
  ✓ Ví dụ: iPhone, Samsung, ...

Giá (₫): [Nhập > 0]
  ✓ Ví dụ: 25000000

Số lượng: [Nhập >= 0]
  ✓ Ví dụ: 10
```

### **Bước 4: Điền Thông Tin Tùy Chọn** (không bắt buộc)
```
Ảnh sản phẩm:
  ✓ Click chọn file
  ✓ Định dạng: JPG, PNG, GIF, WebP
  ✓ Kích thước: <= 5MB
  ✓ Sẽ hiển thị preview

Mô tả:
  ✓ Nhập mô tả sản phẩm
  ✓ Tự do format
```

### **Bước 5: Nhấn "Lưu Sản Phẩm"**
```
- Button sẽ chuyển sang "Đang xử lý..."
- Chờ khoảng 2-3 giây
- Sẽ thấy thông báo "✅ Thêm sản phẩm thành công!"
- Trang sẽ reload tự động
```

---

## ✅ Kiểm Tra Thêm Thành Công

```
1️⃣ Xem danh sách sản phẩm cập nhật
   → Sản phẩm vừa thêm xuất hiện ở đầu danh sách

2️⃣ Xem chi tiết sản phẩm
   → Nhấn nút "Xem" (mắt) trong hàng sản phẩm

3️⃣ Kiểm tra database
   → SELECT * FROM products WHERE name = '...';

4️⃣ Kiểm tra file ảnh
   → /src/main/resources/static/uploads/
```

---

## ❌ Khi Gặp Lỗi

### **Lỗi: Nút quay "Đang xử lý..." không dừng**

#### Nguyên nhân & Cách Khắc Phục:

**1. Timeout (quá 30 giây)**
```
Lỗi hiển thị: "⏱️ Yêu cầu bị timeout"

Cách khắc phục:
□ Kiểm tra file ảnh < 5MB
□ Kiểm tra internet connection (F12 → Network)
□ Thử lại sau vài giây
□ Nếu vẫn lỗi, restart server
```

**2. Network Error**
```
Lỗi hiển thị: "🌐 Lỗi kết nối"

Cách khắc phục:
□ Kiểm tra server chạy: http://localhost:8080/admin/dashboard
□ Kiểm tra URL path: /admin/products/save
□ Restart server (./gradlew bootRun)
□ Kiểm tra firewall/proxy
```

**3. Validation Error từ Server**
```
Lỗi hiển thị: "❌ Lỗi: [chi tiết]"

Ví dụ: 
  - "❌ Lỗi: Sản phẩm với tên '...' đã tồn tại"
  - "❌ Lỗi: Giá phải lớn hơn 0"
  - "❌ Lỗi: Category không tồn tại"

Cách khắc phục:
□ Đọc lỗi cẩn thận
□ Sửa theo yêu cầu
□ Nhấn "Xóa và thử lại"
□ Điền lại thông tin
□ Thêm lại
```

**4. File Upload Error**
```
Lỗi hiển thị: "❌ Lỗi: File ảnh không được vượt quá 5MB"

Cách khắc phục:
□ Chọn file ảnh < 5MB
□ Nếu file lớn, nén bằng:
  - Photoshop, Paint, GIMP
  - Online tool: tinypng.com, compressor.io
  - Windows: Chuột phải → Gửi tới → Folder được nén
```

---

## 🔍 Debug Mode (Nếu Cần)

### **1. Mở Browser Console**
```
F12 → Tab Console
```

### **2. Tìm Các Log**
```
📤 Gửi request đến: /admin/products/save
📋 Dữ liệu: {
  name: "iPhone 15 Pro",
  brand: "Apple",
  categoryId: "1",
  price: "25000000",
  stock: "10"
}
```

### **3. Xem Response**
```
Thành công:
✅ Response status: 200
✅ Response data: {
  status: "success",
  message: "✅ Thêm sản phẩm thành công!",
  product: { id: 123, name: "iPhone 15 Pro", ... }
}

Lỗi:
❌ Response status: 400 (Validation error)
❌ Server error response: "..."
```

### **4. Xem Network Activity**
```
F12 → Tab Network → Click vào request POST /admin/products/save
  → Headers: Method POST, URL, Content-Type multipart/form-data
  → Payload: Form data (name, brand, price, stock, imageFile)
  → Response: JSON từ server
```

---

## 📊 Ví Dụ Dữ Liệu Test

### **Sản Phẩm 1: iPhone**
```
Tên: iPhone 15 Pro 256GB
Hãng: Apple
Danh mục: iPhone
Giá: 25000000
Số lượng: 5
Mô tả: Điện thoại flagship của Apple
```

### **Sản Phẩm 2: Samsung**
```
Tên: Samsung Galaxy S24 Ultra
Hãng: Samsung
Danh mục: Android Flagship
Giá: 24000000
Số lượng: 8
Mô tả: Điện thoại cao cấp của Samsung
```

### **Sản Phẩm 3: Xiaomi**
```
Tên: Xiaomi 14 Ultra
Hãng: Xiaomi
Danh mục: Xiaomi
Giá: 15000000
Số lượng: 20
Mô tả: Điện thoại giá rẻ chất lượng
```

---

## 🎬 Video Hướng Dẫn (Mô Tả Từng Bước)

```
1. Bước vào /admin/products
2. Nhấn "Thêm sản phẩm"
3. Modal mở → Điền form
4. Chọn file ảnh (tùy chọn)
5. Nhấn "Lưu sản phẩm"
6. Chờ "Đang xử lý..." → xong
7. Thấy thông báo "✅ Thêm sản phẩm thành công!"
8. Trang reload → Sản phẩm mới ở trên
```

---

## 💾 Database Check

Nếu muốn kiểm tra trực tiếp database:

```sql
-- 1. Kiểm tra sản phẩm vừa thêm
SELECT * FROM products 
WHERE name = 'iPhone 15 Pro 256GB';

-- 2. Xem ảnh đã upload
SELECT id, name, imageUrl FROM products 
WHERE imageUrl IS NOT NULL 
LIMIT 5;

-- 3. Kiểm tra stock
SELECT id, name, stock FROM products 
WHERE stock > 0 
ORDER BY id DESC 
LIMIT 10;
```

---

## ⚡ Performance Tips

```
✓ Upload ảnh < 2MB để nhanh nhất
✓ Nếu nhiều ảnh, upload từng cái một
✓ Tránh refresh/reload khi đang "Đang xử lý..."
✓ Mở Network Monitor (F12) để theo dõi
✓ Kiểm tra console log khi có lỗi
```

---

## 📞 Hỗ Trợ

Nếu vẫn gặp vấn đề:

```
1. Kiểm tra KICH_HOAT_THEM_SAN_PHAM.md (hướng dẫn chi tiết)
2. Xem TEST_THEM_SAN_PHAM.sql (test database)
3. Mở DevTools (F12) xem chi tiết lỗi
4. Restart server: ./gradlew bootRun
5. Xoá browser cache (Ctrl+Shift+Del)
```

---

**Cập nhật:** 16/04/2026  
**Phiên bản:** 1.0 (Quick Start)

