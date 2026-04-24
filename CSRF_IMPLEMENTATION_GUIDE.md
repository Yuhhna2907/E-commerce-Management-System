# CSRF Protection Implementation Guide

## Vấn Đề Đã Sửa

**Triệu chứng**: Các nút "Mua hàng" gọi `fetch(POST)` đến `/user/cart/add` nhưng không gửi CSRF token → Server trả 403 Forbidden → Frontend hiển thị "Lỗi kết nối"

**Nguyên nhân**: Spring Security đang bật CSRF protection nhưng các fetch requests không include CSRF token

## Giải Pháp Đã Implement

### 1. CSRF Auto-Injection Script

Tạo file `/js/csrf-auto.js` - tự động inject CSRF token vào TẤT CẢ fetch POST/PUT/DELETE/PATCH requests.

**Cách hoạt động**:
- Override `window.fetch` function
- Tự động thêm CSRF token header vào mọi state-changing request
- Không cần thay đổi code hiện tại!

### 2. CSRF Meta Tags

Thêm vào `<head>` của mọi page:

```html
<!-- CSRF Token Meta Tags -->
<meta name="_csrf" th:content="${_csrf.token}"/>
<meta name="_csrf_header" th:content="${_csrf.headerName}"/>
```

Hoặc dùng fragment:

```html
<head th:replace="fragments/csrf-meta :: csrf-meta"></head>
```

### 3. Include Script

Thêm vào cuối `<body>` (TRƯỚC các script khác sử dụng fetch):

```html
<!-- CSRF Auto-Injection (must load before other scripts that use fetch) -->
<script th:src="@{/js/csrf-auto.js}"></script>
```

## Cách Sử Dụng

### Option 1: Auto-Injection (Recommended)

**Ưu điểm**: Không cần thay đổi code hiện tại

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta name="_csrf" th:content="${_csrf.token}"/>
    <meta name="_csrf_header" th:content="${_csrf.headerName}"/>
</head>
<body>
    <!-- Your content -->
    
    <!-- Load CSRF auto-injection FIRST -->
    <script th:src="@{/js/csrf-auto.js}"></script>
    
    <!-- Your other scripts -->
    <script>
        // This will automatically include CSRF token!
        fetch('/user/cart/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ productId: 123, quantity: 1 })
        });
    </script>
</body>
</html>
```

### Option 2: Manual with csrf-utils.js

**Ưu điểm**: Kiểm soát tốt hơn

```html
<script th:src="@{/js/csrf-utils.js}"></script>
<script>
    // Use fetchWithCsrf instead of fetch
    fetchWithCsrf('/user/cart/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productId: 123, quantity: 1 })
    });
</script>
```

### Option 3: Manual Token Injection

```javascript
const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const headerName = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

fetch('/user/cart/add', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        [headerName]: token
    },
    body: JSON.stringify({ productId: 123, quantity: 1 })
});
```

## Files Đã Sửa

### 1. compare.html
- ✅ Thêm CSRF meta tags
- ✅ Include csrf-auto.js
- ✅ Fetch request tự động có CSRF token

### 2. Các file mới
- ✅ `/js/csrf-auto.js` - Auto-injection script
- ✅ `/templates/fragments/csrf-meta.html` - CSRF meta fragment
- ✅ `CSRF_IMPLEMENTATION_GUIDE.md` - Document này

## Checklist Cho Pages Khác

Để thêm CSRF protection cho page mới:

- [ ] Thêm CSRF meta tags vào `<head>`
- [ ] Include `csrf-auto.js` TRƯỚC các script khác
- [ ] Test POST/PUT/DELETE requests
- [ ] Verify không còn 403 errors

## Testing

### 1. Test CSRF Token Có Được Gửi

Mở DevTools → Network → Click "Mua hàng" → Check request headers:

```
X-XSRF-TOKEN: <token-value>
```

### 2. Test Khi Không Có Token

Tạm comment csrf-auto.js → Reload → Click "Mua hàng" → Expect 403

### 3. Test Khi Token Hết Hạn

Wait for session timeout → Click "Mua hàng" → Should redirect to login

## Troubleshooting

### Vẫn Bị 403

1. Check meta tags có trong HTML không:
```javascript
console.log(document.querySelector('meta[name="_csrf"]')?.getAttribute('content'));
```

2. Check csrf-auto.js đã load chưa:
```javascript
// Should see: [CSRF Auto] CSRF token auto-injection enabled for fetch API
```

3. Check request headers:
```javascript
// In DevTools Network tab, check if X-XSRF-TOKEN header exists
```

### Token Null

- Verify Spring Security CSRF enabled
- Check Thymeleaf template có `${_csrf.token}` không
- Verify user đã login

### Script Load Order

csrf-auto.js PHẢI load TRƯỚC các script sử dụng fetch:

```html
<!-- CORRECT -->
<script th:src="@{/js/csrf-auto.js}"></script>
<script th:src="@{/js/my-app.js}"></script>

<!-- WRONG -->
<script th:src="@{/js/my-app.js}"></script>
<script th:src="@{/js/csrf-auto.js}"></script>
```

## Security Notes

1. **CSRF token chỉ gửi cho state-changing methods** (POST, PUT, DELETE, PATCH)
2. **GET requests không cần CSRF token** (theo REST principles)
3. **Token được refresh mỗi session**
4. **Token không được expose trong URL** (chỉ trong header)

## Next Steps

1. ✅ Sửa compare.html
2. ⏳ Thêm csrf-auto.js vào các pages khác:
   - home.html
   - cart/list.html
   - wishlist/list.html
   - saved/list.html
   - profile/*.html
   - order/*.html
3. ⏳ Test toàn bộ flow mua hàng
4. ⏳ Update documentation

---

**Cập nhật**: 22/04/2026
**Status**: ✅ CSRF protection đã được implement cho compare.html
