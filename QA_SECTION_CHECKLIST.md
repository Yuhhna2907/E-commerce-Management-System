# ✅ Q&A Section - Checklist Setup

## 🎯 Checklist Nhanh (Copy & Check)

### 1. Database Setup
- [ ] Kiểm tra user ID = 1 tồn tại: `SELECT * FROM users WHERE id = 1;`
- [ ] Nếu chưa có, tạo user: Chạy INSERT statement
- [ ] Chạy `test-qa-data.sql` để tạo dữ liệu mẫu
- [ ] Verify: `SELECT COUNT(*) FROM product_questions WHERE product_id = 1;`

### 2. Backend Files
- [x] ✅ `ProductQuestionController.java` - Đã sửa với hardcoded user ID = 1L
- [x] ✅ Đã bỏ `@PreAuthorize` annotations
- [x] ✅ Đã thêm `UserRepository` dependency

### 3. Frontend Files
- [x] ✅ `/static/js/QASection.js` - Component JavaScript
- [x] ✅ `/static/css/qa-section.css` - Styling
- [x] ✅ `/static/test-qa-section.html` - Test page

### 4. Test Standalone Page
- [ ] Start backend server: `./gradlew bootRun`
- [ ] Mở browser: `http://localhost:8080/test-qa-section.html`
- [ ] Click "Test API Connection" - Phải thấy "✅ API hoạt động!"
- [ ] Test đặt câu hỏi
- [ ] Test trả lời câu hỏi
- [ ] Test bình chọn

### 5. Integration vào Product Detail
- [ ] Thêm CSS link vào `detail.html`
- [ ] Thêm container `<div id="qa-section-container"></div>`
- [ ] Thêm JavaScript script
- [ ] Initialize QASection với config
- [ ] Test trên product detail page thực tế

### 6. Testing Features
- [ ] ✅ Xem danh sách câu hỏi
- [ ] ✅ Sorting: Recent
- [ ] ✅ Sorting: Helpful
- [ ] ✅ Pagination (nếu có > 10 questions)
- [ ] ✅ Đặt câu hỏi mới
- [ ] ✅ Trả lời câu hỏi
- [ ] ✅ Vote Helpful
- [ ] ✅ Vote Not Helpful
- [ ] ✅ Empty state (khi chưa có questions)

### 7. Responsive Testing
- [ ] Desktop (1920px)
- [ ] Laptop (1280px)
- [ ] Tablet (768px)
- [ ] Mobile (375px)

### 8. Browser Testing
- [ ] Chrome
- [ ] Firefox
- [ ] Safari (nếu có Mac)
- [ ] Edge

## 🚨 Common Issues Checklist

### Issue: "User ID 1 không tồn tại"
- [ ] Chạy: `SELECT * FROM users WHERE id = 1;`
- [ ] Nếu empty, chạy INSERT user statement
- [ ] Restart backend server

### Issue: "Cannot connect to API"
- [ ] Backend server có đang chạy không?
- [ ] Port có đúng 8080 không?
- [ ] Check console log có lỗi gì không?

### Issue: "Questions not loading"
- [ ] Product ID có đúng không?
- [ ] Database có questions cho product đó không?
- [ ] Check Network tab trong DevTools
- [ ] Check backend log

### Issue: "Không thấy nút Trả lời"
- [ ] `currentUserRole` = 'SELLER' hoặc 'ADMIN'?
- [ ] Check console log
- [ ] Verify config trong initialization

## 📝 Quick Commands

### Start Backend
```bash
cd E-commerce-Management-System
./gradlew bootRun
```

### Import Test Data
```bash
mysql -u root -p your_database < test-qa-data.sql
```

### Test API với curl
```bash
# Get questions
curl http://localhost:8080/api/products/1/questions

# Create question
curl -X POST http://localhost:8080/api/products/1/questions \
  -H "Content-Type: application/json" \
  -d '{"questionText": "Test question?"}'
```

## 🎯 Success Criteria

Khi tất cả đều OK:
- ✅ Test page load không lỗi
- ✅ API connection test pass
- ✅ Có thể đặt câu hỏi
- ✅ Có thể trả lời câu hỏi
- ✅ Có thể bình chọn
- ✅ Sorting hoạt động
- ✅ Pagination hoạt động (nếu có nhiều questions)
- ✅ Responsive trên mobile

## 📚 Documentation Reference

1. **QA_SECTION_SETUP_GUIDE.md** - Setup đầy đủ
2. **QA_SECTION_NO_SECURITY_GUIDE.md** - Chi tiết no-security mode
3. **QA_SECTION_INTEGRATION_GUIDE.md** - Tích hợp vào project
4. **test-qa-section.html** - Test page
5. **test-qa-data.sql** - Test data

---

**Tip:** Làm theo thứ tự từ trên xuống dưới! ✨
