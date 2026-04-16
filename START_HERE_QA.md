# 🚀 BẮT ĐẦU TẠI ĐÂY - Q&A Section

## ⚡ Quick Start (3 Phút)

### Bước 1: Database (30 giây)
```sql
-- Tạo user ID = 1 (nếu chưa có)
INSERT INTO users (id, username, email, password, role, created_at) 
VALUES (1, 'testuser', 'test@example.com', 'password123', 'SELLER', NOW());
```

### Bước 2: Import Test Data (30 giây)
```bash
mysql -u root -p your_database < test-qa-data.sql
```

### Bước 3: Start Server (1 phút)
```bash
./gradlew bootRun
```

### Bước 4: Test (1 phút)
Mở browser:
```
http://localhost:8080/test-qa-section.html
```

Click nút **"Test API Connection"** ✅

## 🎯 Đã Tạo Gì?

### Files Chính
```
✅ QASection.js          - Component JavaScript (1000+ lines)
✅ qa-section.css        - Styling responsive (600+ lines)
✅ ProductQuestionController.java - Backend (hardcoded user ID = 1L)
```

### Files Test & Docs
```
✅ test-qa-section.html  - Test page standalone
✅ test-qa-data.sql      - Dữ liệu mẫu (7 questions, 4 answers)
✅ QA_SECTION_SETUP_GUIDE.md - Hướng dẫn đầy đủ
✅ QA_SECTION_CHECKLIST.md   - Checklist nhanh
```

## 🔑 Key Points

### Backend
- **User ID = 1L** (hardcoded)
- **Không cần Spring Security** (đã bỏ @PreAuthorize)
- **Tất cả API endpoints** hoạt động với user ID = 1L

### Frontend
- **isAuthenticated = true** (hardcoded)
- **currentUserRole = 'SELLER'** (hardcoded)
- **Không cần CSRF token**

## ✨ Features

- ✅ Xem danh sách câu hỏi
- ✅ Đặt câu hỏi mới
- ✅ Trả lời câu hỏi (SELLER/ADMIN)
- ✅ Bình chọn (Helpful/Not Helpful)
- ✅ Sorting (Recent/Helpful)
- ✅ Pagination
- ✅ Responsive design

## 📱 Test Checklist

- [ ] Mở `test-qa-section.html`
- [ ] Click "Test API Connection" → Phải thấy "✅ API hoạt động!"
- [ ] Click "Đặt câu hỏi" → Nhập và submit
- [ ] Click "Trả lời" → Nhập và submit
- [ ] Click "Hữu ích" → Vote count tăng
- [ ] Thử sorting: Recent / Helpful
- [ ] Test trên mobile (resize browser)

## 🔧 Integration vào Product Detail

Thêm vào `detail.html`:

```html
<!-- CSS -->
<link rel="stylesheet" th:href="@{/css/qa-section.css}">

<!-- Container -->
<div id="qa-section-container"></div>

<!-- JavaScript -->
<script th:src="@{/js/QASection.js}"></script>
<script th:inline="javascript">
    new QASection(
        document.getElementById('qa-section-container'),
        /*[[${product.id}]]*/ 1,
        {
            isAuthenticated: true,
            currentUserRole: 'SELLER'
        }
    );
</script>
```

## 🐛 Troubleshooting

### "User ID 1 không tồn tại"
→ Chạy INSERT user statement ở Bước 1

### "Cannot connect to API"
→ Backend server có chạy không? Check port 8080

### "Questions not loading"
→ Chạy `test-qa-data.sql` để tạo dữ liệu mẫu

## 📚 Đọc Thêm

1. **QA_SECTION_SETUP_GUIDE.md** - Setup đầy đủ
2. **QA_SECTION_CHECKLIST.md** - Checklist chi tiết
3. **QA_SECTION_COMPLETE.md** - Tổng kết hoàn chỉnh

## 🎉 Done!

Nếu test page hoạt động → Bạn đã setup thành công! 🎊

---

**Có vấn đề?** Xem `QA_SECTION_SETUP_GUIDE.md` section Troubleshooting
