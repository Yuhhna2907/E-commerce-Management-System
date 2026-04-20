# 🚀 Q&A Section - Hướng Dẫn Setup Hoàn Chỉnh (No Security Mode)

## 📋 Tổng Quan

Q&A Section đã được triển khai với **hardcoded user ID = 1L** để test mà không cần Spring Security.

## ✅ Files Đã Tạo

### 1. Backend
- ✅ `ProductQuestionController.java` - Controller với hardcoded user ID = 1L
- ✅ Đã bỏ tất cả `@PreAuthorize` annotations

### 2. Frontend
- ✅ `QASection.js` - Component JavaScript hoàn chỉnh
- ✅ `qa-section.css` - Styling responsive

### 3. Test & Documentation
- ✅ `test-qa-section.html` - Trang test standalone
- ✅ `test-qa-data.sql` - Script tạo dữ liệu mẫu
- ✅ `QA_SECTION_NO_SECURITY_GUIDE.md` - Hướng dẫn chi tiết
- ✅ `QA_SECTION_INTEGRATION_GUIDE.md` - Hướng dẫn tích hợp

## 🔧 Setup Nhanh (5 Bước)

### Bước 1: Kiểm Tra Database

Đảm bảo có user với ID = 1:

```sql
SELECT * FROM users WHERE id = 1;
```

Nếu chưa có, tạo user:

```sql
INSERT INTO users (id, username, email, password, role, created_at) 
VALUES (1, 'testuser', 'test@example.com', 'password123', 'SELLER', NOW());
```

### Bước 2: Tạo Dữ Liệu Test

Chạy file `test-qa-data.sql`:

```bash
mysql -u root -p your_database < test-qa-data.sql
```

Hoặc copy-paste vào MySQL Workbench/phpMyAdmin.

### Bước 3: Khởi Động Backend

```bash
cd E-commerce-Management-System
./gradlew bootRun
```

Hoặc chạy từ IDE (IntelliJ/Eclipse).

### Bước 4: Test API

Mở browser và truy cập:

```
http://localhost:8080/test-qa-section.html
```

Click nút **"Test API Connection"** để kiểm tra.

### Bước 5: Tích Hợp Vào Product Detail Page

Thêm vào `detail.html`:

```html
<!-- CSS -->
<link rel="stylesheet" th:href="@{/css/qa-section.css}">

<!-- Container (đặt sau phần reviews) -->
<div class="container mt-5">
    <div id="qa-section-container"></div>
</div>

<!-- JavaScript -->
<script th:src="@{/js/QASection.js}"></script>
<script th:inline="javascript">
    document.addEventListener('DOMContentLoaded', function() {
        const container = document.getElementById('qa-section-container');
        const productId = /*[[${product.id}]]*/ 1;
        
        new QASection(container, productId, {
            pageSize: 10,
            sortBy: 'recent',
            isAuthenticated: true,
            currentUserRole: 'SELLER',
            csrfToken: /*[[${_csrf.token}]]*/ null
        });
    });
</script>
```

## 🧪 Test Features

### 1. Xem Danh Sách Câu Hỏi
- ✅ Hiển thị questions với answers
- ✅ Sorting: Recent / Helpful
- ✅ Pagination
- ✅ Empty state khi chưa có câu hỏi

### 2. Đặt Câu Hỏi
- ✅ Click nút "Đặt câu hỏi"
- ✅ Nhập câu hỏi (10-500 ký tự)
- ✅ Character counter
- ✅ Submit và reload danh sách

### 3. Trả Lời Câu Hỏi
- ✅ Nút "Trả lời" hiển thị (vì role = SELLER)
- ✅ Nhập câu trả lời (10-1000 ký tự)
- ✅ Character counter
- ✅ Submit và reload danh sách

### 4. Bình Chọn
- ✅ Click "Hữu ích" hoặc "Không hữu ích"
- ✅ Active state hiển thị vote hiện tại
- ✅ Vote count cập nhật real-time

## 📊 API Endpoints

### GET Questions
```bash
curl http://localhost:8080/api/products/1/questions?sortBy=recent&page=0&size=10
```

### POST Question
```bash
curl -X POST http://localhost:8080/api/products/1/questions \
  -H "Content-Type: application/json" \
  -d '{"questionText": "Sản phẩm có bảo hành không?"}'
```

### POST Answer
```bash
curl -X POST http://localhost:8080/api/products/questions/1/answers \
  -H "Content-Type: application/json" \
  -d '{"answerText": "Có, bảo hành 12 tháng."}'
```

### POST Vote
```bash
curl -X POST http://localhost:8080/api/products/answers/1/vote \
  -H "Content-Type: application/json" \
  -d '{"isHelpful": true}'
```

## 🐛 Troubleshooting

### Lỗi: "User ID 1 không tồn tại"

**Nguyên nhân:** Database không có user với ID = 1

**Giải pháp:**
```sql
INSERT INTO users (id, username, email, password, role, created_at) 
VALUES (1, 'testuser', 'test@example.com', 'password123', 'SELLER', NOW());
```

### Lỗi: "Cannot connect to API"

**Nguyên nhân:** Backend server chưa chạy hoặc port sai

**Giải pháp:**
1. Kiểm tra server đang chạy: `http://localhost:8080`
2. Kiểm tra port trong `application.properties`
3. Xem log console có lỗi gì không

### Lỗi: "Questions not loading"

**Nguyên nhân:** Product ID không tồn tại hoặc chưa có questions

**Giải pháp:**
1. Kiểm tra product ID trong code
2. Chạy `test-qa-data.sql` để tạo dữ liệu mẫu
3. Xem Network tab trong browser DevTools

### Không thấy nút "Trả lời"

**Nguyên nhân:** `currentUserRole` không phải SELLER hoặc ADMIN

**Giải pháp:**
```javascript
new QASection(container, productId, {
    currentUserRole: 'SELLER'  // Đảm bảo có dòng này
});
```

## 📱 Responsive Design

Component hoạt động tốt trên:
- ✅ Desktop (≥1200px)
- ✅ Tablet (768px - 1199px)
- ✅ Mobile (320px - 767px)

## 🔐 Security Notes

**QUAN TRỌNG:** Đây là temporary solution cho testing!

Khi tích hợp Spring Security:
1. Sửa lại `ProductQuestionController.java`:
   - Thêm lại `@PreAuthorize` annotations
   - Sửa `getCurrentUser()` để lấy từ SecurityContext
   - Bỏ hardcoded user ID

2. Sửa lại frontend integration:
   - Lấy authentication status từ Thymeleaf
   - Lấy user role từ Spring Security
   - Xử lý trường hợp chưa đăng nhập

## 📚 Documentation Files

1. **QA_SECTION_NO_SECURITY_GUIDE.md** - Chi tiết về no-security mode
2. **QA_SECTION_INTEGRATION_GUIDE.md** - Hướng dẫn tích hợp đầy đủ
3. **test-qa-section.html** - Trang test standalone
4. **test-qa-data.sql** - Script tạo dữ liệu mẫu

## ✨ Features Implemented

✅ Question list với sorting (recent/helpful)
✅ Question submission modal
✅ Answer submission modal (SELLER/ADMIN)
✅ Vote buttons với active state
✅ Pagination với smooth scroll
✅ Responsive design
✅ Toast notifications
✅ Error handling
✅ XSS prevention
✅ Character counters
✅ Loading states
✅ Empty states

## 🎯 Next Steps

1. ✅ Test tất cả features trên `test-qa-section.html`
2. ✅ Tích hợp vào product detail page
3. ✅ Test trên mobile devices
4. ⏳ Tích hợp Spring Security (khi sẵn sàng)
5. ⏳ Thêm email notifications (backend đã có)
6. ⏳ Thêm rate limiting (optional)

## 💡 Tips

- Sử dụng browser DevTools để debug
- Xem Console log để theo dõi API calls
- Xem Network tab để kiểm tra requests/responses
- Test trên nhiều browsers khác nhau

## 🆘 Support

Nếu gặp vấn đề:
1. Kiểm tra console log
2. Kiểm tra Network tab
3. Kiểm tra backend log
4. Xem lại documentation files
5. Test với `test-qa-section.html` trước

---

**Happy Testing! 🎉**
