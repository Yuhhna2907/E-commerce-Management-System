# CSRF Audit Report - User Pages

## Tổng Quan

Scan toàn bộ `/user` templates để tìm fetch POST requests thiếu CSRF token.

**Ngày scan**: 22/04/2026  
**Tổng số pages**: 9  
**Pages có vấn đề**: 7

---

## ❌ Pages Thiếu CSRF Token

### 1. `/user/cart/list.html` - CRITICAL
**Số lượng fetch POST**: 3

```javascript
// Line 955 - Save for later
fetch('/user/cart/save-for-later', { method: 'POST' })

// Line 1340 - Move saved item to cart  
fetch(`/user/saved/cart/move?savedItemId=${savedItemId}`, { method: 'POST' })

// Line 1395 - Save cart item
fetch(`/user/saved/cart/save?cartItemId=${cartItemId}`, { method: 'POST' })
```

**Impact**: Không thể save for later, move items
**Priority**: HIGH

---

### 2. `/user/wishlist/list.html` - CRITICAL
**Số lượng fetch POST**: 1

```javascript
// Line 481 - Remove from wishlist
fetch('/api/user/wishlist/remove', { method: 'POST' })
```

**Impact**: Không thể xóa khỏi wishlist
**Priority**: HIGH

---

### 3. `/user/saved/list.html` - CRITICAL
**Số lượng fetch POST**: 1

```javascript
// Line 467 - Move to cart
fetch('/user/saved/cart/move?savedItemId=' + savedItemId, { method: 'POST' })
```

**Impact**: Không thể move saved items to cart
**Priority**: HIGH

---

### 4. `/user/product/list.html` - PARTIAL FIX
**Số lượng fetch POST**: 2

```javascript
// Line 2167 - Add to cart (✅ ĐÃ FIX - dùng fetchWithCsrf)
fetchWithCsrf('/user/cart/add', { method: 'POST' })

// Line 2264 - Toggle wishlist (❌ THIẾU CSRF)
fetch('/api/user/wishlist/toggle', { method: 'POST' })
```

**Impact**: Wishlist toggle không hoạt động
**Priority**: MEDIUM

---

### 5. `/user/product/detail.html` - PARTIAL FIX
**Số lượng fetch POST**: 5

```javascript
// Line 2484 - Stock notification (❌ THIẾU CSRF)
fetch('/user/stock-notification/register', { method: 'POST' })

// Line 2856 - Add to cart (✅ ĐÃ FIX - dùng fetchWithCsrf)
fetchWithCsrf('/user/cart/add', { method: 'POST' })

// Line 2988 - Submit review (❌ THIẾU CSRF)
fetch('/user/products/review', { method: 'POST' })

// Line 3009 - Upload review images (❌ THIẾU CSRF)
fetch(`/api/reviews/${reviewId}/images`, { method: 'POST' })

// Line 3061 - Toggle wishlist (❌ THIẾU CSRF)
fetch('/api/user/wishlist/toggle', { method: 'POST' })

// Line 3490 - Submit Q&A question (❌ THIẾU CSRF)
fetch(`/qa/products/${QA_PRODUCT_ID}/questions`, { method: 'POST' })
```

**Impact**: Stock notification, review, wishlist, Q&A không hoạt động
**Priority**: HIGH

---

### 6. `/user/loyalty/index.html` - CRITICAL
**Số lượng fetch POST**: 1

```javascript
// Line 475 - Redeem points
fetch('/user/loyalty/redeem', { method: 'POST' })
```

**Impact**: Không thể đổi điểm loyalty
**Priority**: HIGH

---

### 7. `/user/product/compare.html` - ✅ ĐÃ FIX
**Số lượng fetch POST**: 1

```javascript
// Line 1079 - Add to cart (✅ ĐÃ FIX - có csrf-auto.js)
fetch('/user/cart/add', { method: 'POST' })
```

**Status**: ✅ FIXED
**Solution**: Đã thêm csrf-auto.js

---

## ✅ Pages Không Có Vấn Đề

### 1. `/user/home.html`
- Không có fetch POST requests

### 2. `/user/order/checkout.html`
- Cần kiểm tra thêm

### 3. `/user/order/success.html`
- Không có fetch POST requests

### 4. `/user/profile/index.html`
- Cần kiểm tra thêm

---

## Thống Kê

| Metric | Count |
|--------|-------|
| Tổng pages scan | 9 |
| Pages có vấn đề | 7 |
| Pages đã fix | 1 |
| Tổng fetch POST | 15 |
| Fetch thiếu CSRF | 12 |
| Fetch đã có CSRF | 3 |

---

## Giải Pháp

### Option 1: Thêm csrf-auto.js vào TẤT CẢ pages (RECOMMENDED)

**Ưu điểm**:
- Fix tất cả pages cùng lúc
- Không cần sửa code
- Tự động protect future code

**Cách làm**:
1. Thêm CSRF meta tags vào header chung
2. Include csrf-auto.js vào footer chung
3. Done!

### Option 2: Sửa từng fetch call

**Ưu điểm**:
- Kiểm soát tốt hơn

**Nhược điểm**:
- Mất nhiều thời gian
- Dễ miss

---

## Action Plan

### Phase 1: Quick Fix (30 phút)
- [x] Fix compare.html (đã xong)
- [ ] Thêm csrf-auto.js vào header/footer chung
- [ ] Test 3-4 pages quan trọng

### Phase 2: Comprehensive Fix (1 giờ)
- [ ] Thêm CSRF meta tags vào tất cả pages
- [ ] Verify tất cả fetch POST có CSRF token
- [ ] Test toàn bộ user flow

### Phase 3: Testing (30 phút)
- [ ] Test cart operations
- [ ] Test wishlist operations
- [ ] Test review submission
- [ ] Test loyalty redemption
- [ ] Test Q&A submission

---

## Recommended Implementation

### 1. Tạo Base Layout Fragment

```html
<!-- fragments/base-scripts.html -->
<div th:fragment="base-scripts">
    <!-- CSRF Meta Tags -->
    <meta name="_csrf" th:content="${_csrf.token}"/>
    <meta name="_csrf_header" th:content="${_csrf.headerName}"/>
    
    <!-- CSRF Auto-Injection -->
    <script th:src="@{/js/csrf-auto.js}"></script>
</div>
```

### 2. Include Trong Mọi Page

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <th:block th:replace="fragments/base-scripts :: base-scripts"></th:block>
</head>
<body>
    <!-- Content -->
</body>
</html>
```

---

## Next Steps

1. ✅ Tạo csrf-auto.js (done)
2. ✅ Fix compare.html (done)
3. ⏳ Tạo base-scripts fragment
4. ⏳ Update tất cả user pages
5. ⏳ Test comprehensive
6. ⏳ Update admin pages (nếu cần)

---

**Cập nhật**: 22/04/2026 - 15:30
**Status**: 🔴 7/9 pages cần fix
