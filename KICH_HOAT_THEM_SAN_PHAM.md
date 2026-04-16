# 🔧 Hướng Dẫn Chi Tiết - Xử Lý Lỗi Thêm/Sửa Sản Phẩm

## ✅ Những cải tiến đã thực hiện

### 1. **Frontend - Form Submission (JavaScript)**
```javascript
✓ Thêm TIMEOUT HANDLING (30 giây)
  - Nếu request quá lâu, sẽ abort và hiển thị: 
    "⏱️ Yêu cầu bị timeout. Vui lòng thử lại!"

✓ Cải tiến Validation
  - Kiểm tra: Tên, Hãng, Danh mục, Giá, Số lượng, Kích thước file
  - Hiển thị lỗi cụ thể ngay khi validate fail

✓ Error Handling Chi Tiết
  - Timeout Error: "⏱️ Yêu cầu bị timeout"
  - Network Error: "🌐 Lỗi kết nối"
  - Server Error: Hiển thị lỗi từ server
  - Parse Error: "Server không trả về JSON hợp lệ"

✓ Logging - Giúp Debug
  - console.log() để theo dõi quá trình
  - console.error() cho các lỗi
  - In ra dữ liệu request/response

✓ Button State Management
  - Button được ENABLE lại ngay trong FINALLY block
  - Đảm bảo không bị stuck ở "Đang xử lý..."
```

### 2. **Backend - Controller**
```java
✓ Validation Chi Tiết
  - Kiểm tra các trường bắt buộc: name, brand, categoryId, price, stock
  - Kiểm tra file size (max 5MB)
  - Kiểm tra MIME type của ảnh

✓ File Upload Handling
  - Tạo thư mục uploads nếu chưa tồn tại
  - Tạo UUID filename để tránh trùng
  - Lưu file vật lý + đường dẫn vào DTO

✓ Logging (Debugging)
  - In ra từng bước xử lý
  - Log lỗi chi tiết để debug
  - Trace từ request đến response

✓ Exception Handling
  - Bắt IllegalArgumentException → Validation error
  - Bắt IOException → File upload error
  - Bắt Exception chung → Server error
  - Trả về response với status + message rõ ràng
```

### 3. **Service Layer - ProductService**
```java
✓ Thêm Stock Field
  - Tạo Product: .stock(request.getStock())
  - Cập nhật Product: product.setStock(request.getStock())
  - Response: .stock(product.getStock())

✓ Validation
  - Kiểm tra duplicate name
  - Kiểm tra price > 0
  - Kiểm tra stock >= 0
  - Kiểm tra category tồn tại
```

---

## 📋 Quy Trình Thêm Sản Phẩm Chi Tiết

```
1️⃣ FRONTEND VALIDATION (Ngay khi nhấn Submit)
   ├─ Kiểm tra tên (min 2 ký tự) → ❌ Error Toast
   ├─ Kiểm tra hãng (min 2 ký tự) → ❌ Error Toast
   ├─ Kiểm tra category không trống → ❌ Error Toast
   ├─ Kiểm tra price > 0 → ❌ Error Toast
   ├─ Kiểm tra stock >= 0 → ❌ Error Toast
   ├─ Kiểm tra file size <= 5MB → ❌ Error Toast
   └─ ✅ Nếu pass → Tiếp tục bước 2

2️⃣ DISABLE BUTTON & SHOW LOADING
   ├─ Button disabled = true
   ├─ Button innerHTML = "Đang xử lý..."
   ├─ Spinner icon hiển thị
   └─ Tạo AbortController với timeout 30s

3️⃣ GỬI REQUEST ĐẾN SERVER
   ├─ POST /admin/products/save
   ├─ Body: FormData (multipart/form-data)
   ├─ Timeout: 30 giây (nếu lâu hơn → AbortError)
   └─ Console log: "📤 Gửi request"

4️⃣ BACKEND VALIDATION & PROCESSING
   ├─ ProductController.saveProduct()
   ├─ Validate DTO (name, brand, categoryId, price, stock)
   ├─ Validate file (size, MIME type)
   ├─ Upload file → /src/main/resources/static/uploads/
   ├─ Generate UUID filename
   ├─ Save file vật lý
   ├─ Call ProductService.create()
   ├─ Service validate (duplicate, category, price, stock)
   ├─ Save to Database
   └─ Return response {status: "success", message: "...", product: {...}}

5️⃣ FRONTEND RECEIVE RESPONSE
   ├─ Clear timeout
   ├─ Check response.ok (HTTP 200-299)
   ├─ Parse JSON
   ├─ Check res.status === "success"
   ├─ ✅ Success: Reset form → Đóng modal → Reload trang
   ├─ ❌ Error: Show error toast → Show "Xóa và thử lại" button
   └─ Error handling (timeout, network, parse)

6️⃣ FINALLY BLOCK (Luôn chạy)
   ├─ Button disabled = false (IMPORTANT!)
   ├─ Button innerHTML = originalText
   └─ Đảm bảo không bị treo ở "Đang xử lý..."
```

---

## 🔍 Debug Khi Nút Quay Không Dừng

### **Bước 1: Mở Developer Console**
```
Nhấn F12 → Tab Console → Xem các log
```

### **Bước 2: Tìm Console Log**
```
📤 Gửi request đến: /admin/products/save
📋 Dữ liệu: {name: "...", brand: "...", ...}
```

### **Bước 3: Xem Response**
```
✅ Response status: 200
✅ Response data: {status: "success", message: "..."}

HOẶC

❌ Response status: 500
❌ Server error response: [error details]
```

### **Bước 4: Xem Console Error**
```
❌ Lỗi chi tiết: [error message]

Có thể là:
- ⏱️ AbortError: Timeout 30 giây
- 🌐 Failed to fetch: Network error
- Parse JSON error: Server response không phải JSON
- Validation error: Data không hợp lệ
```

---

## 📝 Ví Dụ Lỗi & Cách Khắc Phục

### **Lỗi 1: "⏱️ Yêu cầu bị timeout"**
```
Nguyên nhân:
  - Server quá chậm
  - File upload quá lớn
  - Kết nối mạng yếu

Cách khắc phục:
  1. Kiểm tra file size <= 5MB
  2. Kiểm tra internet connection
  3. Thử lại sau vài giây
```

### **Lỗi 2: "🌐 Lỗi kết nối"**
```
Nguyên nhân:
  - Server không chạy
  - URL sai
  - Firewall chặn

Cách khắc phục:
  1. Kiểm tra server đang chạy (localhost:8080)
  2. Kiểm tra Network tab trong DevTools
  3. Kiểm tra URL: /admin/products/save
```

### **Lỗi 3: "❌ Lỗi: Sản phẩm với tên ... đã tồn tại"**
```
Nguyên nhân:
  - Tên sản phẩm đã có trong database

Cách khắc phục:
  1. Thay đổi tên sản phẩm
  2. Hoặc xoá sản phẩm cũ trước
```

### **Lỗi 4: "❌ Lỗi: Giá phải lớn hơn 0"**
```
Nguyên nhân:
  - Giá <= 0 hoặc trống

Cách khắc phục:
  1. Nhập giá > 0
```

### **Lỗi 5: "❌ Lỗi: File ảnh không được vượt quá 5MB"**
```
Nguyên nhân:
  - File ảnh > 5MB

Cách khắc phục:
  1. Chọn ảnh < 5MB
  2. Nén ảnh trước khi upload
```

---

## 🎯 Checklist Trước Khi Thêm Sản Phẩm

```
□ Tên sản phẩm:      >= 2 ký tự, không trùng
□ Hãng sản xuất:     >= 2 ký tự
□ Danh mục:          Đã chọn
□ Giá:               > 0 (VD: 5000000)
□ Số lượng:          >= 0 (VD: 10)
□ Ảnh (tùy chọn):    <= 5MB, JPEG/PNG/GIF/WebP
□ Mô tả (tùy chọn):  Tự do
□ Kết nối mạng:      OK
□ Server:            Chạy ở localhost:8080
```

---

## 🚀 Công Nghệ Sử Dụng

```
Frontend:
  - Vanilla JavaScript (ES6+)
  - Fetch API (không jQuery)
  - FormData (multipart/form-data)
  - AbortController (timeout handling)
  - Bootstrap Toast (thông báo)

Backend:
  - Spring Boot
  - Spring MVC (@Controller, @PostMapping)
  - @ModelAttribute (DTO binding)
  - MultipartFile (file upload)
  - Exception handling (@ResponseBody)

Database:
  - JPA/Hibernate
  - MySQL
```

---

## 📞 Troubleshooting Tips

```
1. Kiểm tra Browser Console (F12 → Console)
   → Log sẽ hướng dẫn từng bước

2. Kiểm tra Network Tab (F12 → Network)
   → Xem request/response chi tiết

3. Kiểm tra Server Log (Terminal)
   → Xem System.out.println() debug log

4. Kiểm tra File System
   → /src/main/resources/static/uploads/
   → Xem file ảnh có được lưu không

5. Kiểm trap Database
   → SELECT * FROM products
   → Xem dữ liệu có được save không
```

---

## ✨ Ưu Điểm Của Cải Tiến

```
✅ Timeout Handling      → Không bị treo vô thời hạn
✅ Detailed Validation  → Lỗi cụ thể, dễ hiểu
✅ Error Logging        → Debug dễ dàng qua console
✅ Button State Fix     → Không bị stuck
✅ File Size Validation → Tránh upload file lớn
✅ MIME Type Check      → Chỉ upload ảnh đúng
✅ User-Friendly UI     → Thông báo rõ ràng
✅ Retry Mechanism      → Nút "Xóa và thử lại"
```

---

**Ngày tạo:** 16/04/2026
**Phiên bản:** 2.0 (Cải tiến timeout + logging)

