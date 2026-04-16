# Q&A Section - Hướng Dẫn Sử Dụng Không Có Spring Security

## Thay Đổi Cho Môi Trường Không Có Security

Vì bạn chưa tích hợp Spring Security, tôi đã sửa lại code để:

### 1. Backend Changes (ProductQuestionController.java)

✅ **Đã sửa:**
- Bỏ tất cả `@PreAuthorize` annotations
- Hardcode user ID = 1L
- Thêm `UserRepository` để lấy user từ database
- Tất cả requests sẽ sử dụng user ID = 1L

```java
// TEMPORARY: Hardcoded user ID cho testing
private static final Long HARDCODED_USER_ID = 1L;

private User getCurrentUser() {
    return userRepository.findById(HARDCODED_USER_ID)
            .orElseThrow(() -> new UnauthorizedException("User ID 1 không tồn tại"));
}
```

### 2. Frontend Integration (Đơn Giản Hóa)

Thêm vào `detail.html`:

```html
<!-- CSS -->
<link rel="stylesheet" th:href="@{/css/qa-section.css}">

<!-- Container -->
<div class="container mt-5">
    <div id="qa-section-container"></div>
</div>

<!-- JavaScript -->
<script th:src="@{/js/QASection.js}"></script>

<script th:inline="javascript">
    document.addEventListener('DOMContentLoaded', function() {
        const container = document.getElementById('qa-section-container');
        const productId = /*[[${product.id}]]*/ 1; // Thay bằng product ID thực tế
        
        // TEMPORARY: Hardcode authentication = true, role = SELLER
        // Vì backend đã hardcode user ID = 1L
        new QASection(container, productId, {
            pageSize: 10,
            sortBy: 'recent',
            isAuthenticated: true,        // Hardcode = true
            currentUserRole: 'SELLER',    // Hardcode = SELLER (để test answer feature)
            csrfToken: /*[[${_csrf.token}]]*/ null
        });
    });
</script>
```

### 3. Yêu Cầu Database

**QUAN TRỌNG:** Đảm bảo trong database có user với ID = 1:

```sql
-- Kiểm tra user ID = 1 có tồn tại không
SELECT * FROM users WHERE id = 1;

-- Nếu chưa có, tạo user mẫu
INSERT INTO users (id, username, email, password, role, created_at) 
VALUES (1, 'testuser', 'test@example.com', 'password123', 'SELLER', NOW());
```

### 4. Test Features

Với cấu hình này, bạn có thể test:

✅ **Xem danh sách câu hỏi** - Không cần authentication
✅ **Đặt câu hỏi** - Sử dụng user ID = 1L
✅ **Trả lời câu hỏi** - Sử dụng user ID = 1L (role = SELLER)
✅ **Bình chọn câu trả lời** - Sử dụng user ID = 1L
✅ **Sorting và Pagination** - Hoạt động bình thường

### 5. API Endpoints (Không Cần Authentication)

Tất cả endpoints đều có thể gọi trực tiếp:

```bash
# Lấy danh sách câu hỏi
GET http://localhost:8080/api/products/1/questions?sortBy=recent&page=0&size=10

# Tạo câu hỏi (user ID = 1L tự động)
POST http://localhost:8080/api/products/1/questions
Content-Type: application/json

{
  "questionText": "Sản phẩm này có bảo hành không?"
}

# Trả lời câu hỏi (user ID = 1L tự động)
POST http://localhost:8080/api/products/questions/1/answers
Content-Type: application/json

{
  "answerText": "Có, sản phẩm được bảo hành 12 tháng."
}

# Bình chọn câu trả lời (user ID = 1L tự động)
POST http://localhost:8080/api/products/answers/1/vote
Content-Type: application/json

{
  "isHelpful": true
}
```

### 6. Khi Nào Cần Sửa Lại?

Khi bạn tích hợp Spring Security, cần:

1. **Backend:**
   - Thêm lại `@PreAuthorize` annotations
   - Sửa `getCurrentUser()` để lấy từ `SecurityContextHolder`
   - Bỏ hardcoded user ID

2. **Frontend:**
   - Sử dụng Thymeleaf để lấy authentication status thực tế
   - Lấy user role từ Spring Security
   - Xử lý trường hợp user chưa đăng nhập

### 7. Troubleshooting

**Lỗi: "User ID 1 không tồn tại"**
- Kiểm tra database có user với ID = 1 chưa
- Tạo user mẫu nếu chưa có

**Lỗi: "Cannot read property 'id' of undefined"**
- Kiểm tra `product.id` trong Thymeleaf template
- Đảm bảo product object được truyền vào model

**Không thấy nút "Trả lời"**
- Kiểm tra `currentUserRole` = 'SELLER' hoặc 'ADMIN'
- Xem console log để debug

### 8. Demo Data

Để test đầy đủ, tạo dữ liệu mẫu:

```sql
-- Tạo câu hỏi mẫu
INSERT INTO product_questions (product_id, user_id, question_text, created_at)
VALUES (1, 1, 'Sản phẩm này có màu nào khác không?', NOW());

-- Tạo câu trả lời mẫu
INSERT INTO product_answers (question_id, user_id, answer_text, created_at)
VALUES (1, 1, 'Có 3 màu: Đen, Trắng, Xanh dương', NOW());

-- Tạo vote mẫu
INSERT INTO answer_votes (answer_id, user_id, is_helpful, created_at)
VALUES (1, 1, true, NOW());
```

## Tóm Tắt

✅ Backend: Hardcode user ID = 1L, bỏ security annotations
✅ Frontend: Hardcode isAuthenticated = true, role = SELLER
✅ Database: Cần có user với ID = 1
✅ Testing: Tất cả features hoạt động với user ID = 1L

Khi có Spring Security, chỉ cần sửa lại phần authentication logic!
