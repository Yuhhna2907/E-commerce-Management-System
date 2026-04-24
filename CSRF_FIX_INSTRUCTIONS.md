# CSRF Fix Instructions - Quick Guide

## ✅ Đã Fix

1. **compare.html** - ✅ DONE
2. **cart/list.html** - ✅ DONE

## ⏳ Cần Fix (5 pages)

### 1. wishlist/list.html
### 2. saved/list.html  
### 3. product/list.html
### 4. product/detail.html
### 5. loyalty/index.html

---

## Cách Fix Nhanh (2 bước)

### Bước 1: Thêm vào `<head>` (sau viewport meta)

```html
<meta name="viewport" content="width=device-width, initial-scale=1">

<!-- CSRF Meta Tags -->
<th:block th:replace="fragments/base-scripts :: csrf-meta"></th:block>
```

### Bước 2: Thêm trước `</body>` hoặc sau `</script>` cuối cùng

```html
</script>

<!-- CSRF Auto-Injection -->
<th:block th:replace="fragments/base-scripts :: csrf-script"></th:block>

</body>
```

---

## Copy-Paste Ready Code

### For `<head>`:
```html
    <!-- CSRF Meta Tags -->
    <th:block th:replace="fragments/base-scripts :: csrf-meta"></th:block>
```

### Before `</body>`:
```html
<!-- CSRF Auto-Injection -->
<th:block th:replace="fragments/base-scripts :: csrf-script"></th:block>

```

---

## Verification

Sau khi fix, check:

1. ✅ Meta tags có trong HTML source
2. ✅ csrf-auto.js được load
3. ✅ Console log: `[CSRF Auto] CSRF token auto-injection enabled`
4. ✅ Fetch POST requests có header `X-XSRF-TOKEN`

---

## Files Created

1. ✅ `/js/csrf-auto.js` - Auto-injection script
2. ✅ `/templates/fragments/base-scripts.html` - Reusable fragments
3. ✅ `/templates/fragments/csrf-meta.html` - Legacy (không dùng nữa)

---

## Testing Checklist

### Cart Operations
- [ ] Add to cart
- [ ] Save for later
- [ ] Move saved to cart
- [ ] Remove from cart

### Wishlist Operations
- [ ] Add to wishlist
- [ ] Remove from wishlist
- [ ] Toggle wishlist

### Product Operations
- [ ] Submit review
- [ ] Upload review images
- [ ] Register stock notification
- [ ] Submit Q&A question

### Loyalty Operations
- [ ] Redeem points

---

## Troubleshooting

### Vẫn Bị 403

1. Check meta tags:
```javascript
console.log(document.querySelector('meta[name="_csrf"]')?.getAttribute('content'));
```

2. Check script loaded:
```javascript
// Should see: [CSRF Auto] CSRF token auto-injection enabled
```

3. Check request headers in DevTools Network tab

### Token Null

- Verify user đã login
- Check Spring Security CSRF enabled
- Verify Thymeleaf template syntax đúng

---

## Next Steps

1. Fix 5 pages còn lại (15 phút)
2. Test toàn bộ user flow (30 phút)
3. Update admin pages nếu cần (30 phút)
4. Deploy và monitor

---

**Cập nhật**: 22/04/2026 - 15:45
**Status**: 2/7 pages fixed, 5 remaining
