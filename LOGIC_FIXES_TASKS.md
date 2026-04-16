# Tasks: Sửa Lỗi Logic Toàn Hệ Thống

## Tổng Quan
Danh sách tasks để sửa 15 lỗi logic đã phát hiện trong báo cáo đánh giá.

**Ưu tiên**: Critical → Medium → Low  
**Thời gian ước tính**: 4-6 ngày

---

## Phase 1: Critical Issues (1-2 ngày)

### [ ] Task 1: Fix CartService - Race Condition Khi Thêm Vào Giỏ
**Lỗi**: #1 - Race condition khi 2 users cùng mua sản phẩm  
**File**: `service/cart/user/CartService.java`  
**Mức độ**: 🔴 Critical

**Các bước thực hiện**:
1. [ ] Thêm `@Version` column vào `ProductVariant` entity
   - Mở file `model/ProductVariant.java`
   - Thêm field: `@Version private Long version;`
   
2. [ ] Tạo migration SQL
   ```sql
   ALTER TABLE product_variants 
   ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
   ```

3. [ ] Sửa method `addToCart()` trong CartService
   - Thêm try-catch cho `OptimisticLockException`
   - Throw user-friendly error message

4. [ ] Test concurrent operations
   - Tạo 2 requests đồng thời
   - Verify không vượt quá stock

**Thời gian**: 2-3 giờ

---

### [ ] Task 2: Fix CartService - Validate Số Lượng Âm
**Lỗi**: #2 - Không kiểm tra quantity âm  
**File**: `service/cart/user/CartService.java`  
**Mức độ**: 🔴 Critical

**Các bước thực hiện**:
1. [ ] Thêm validation ở đầu method `addToCart()`
   ```java
   if (request.getQuantity() == null || request.getQuantity() <= 0) {
       throw new BadRequestException("Số lượng phải lớn hơn 0");
   }
   ```

2. [ ] Test với quantity âm
   - POST với quantity = -100
   - Verify bị reject

**Thời gian**: 30 phút

---

### [ ] Task 3: Fix UserProductService - Review Duplicate Race Condition
**Lỗi**: #6 - Review duplicate không được prevent đúng  
**File**: `service/product/user/UserProductService.java`  
**Mức độ**: 🔴 Critical

**Các bước thực hiện**:
1. [ ] Tạo unique constraint trong database
   ```sql
   ALTER TABLE reviews 
   ADD CONSTRAINT uk_user_product 
   UNIQUE (user_id, product_id);
   ```

2. [ ] Sửa method `reviewProduct()`
   - Wrap `reviewRepository.save()` trong try-catch
   - Catch `DataIntegrityViolationException`
   - Throw user-friendly message

3. [ ] Test duplicate review
   - Gửi 2 requests review cùng lúc
   - Verify chỉ 1 review được tạo

**Thời gian**: 1-2 giờ

---

### [ ] Task 4: Fix UserProfileService - Password Không Hash
**Lỗi**: #8 - Password lưu plain text  
**File**: `service/profile/UserProfileServiceImpl.java`  
**Mức độ**: 🔴 Critical (Security)

**Các bước thực hiện**:
1. [ ] Thêm dependency vào `build.gradle`
   ```gradle
   implementation 'org.springframework.security:spring-security-crypto'
   ```

2. [ ] Tạo PasswordEncoder bean
   ```java
   @Bean
   public PasswordEncoder passwordEncoder() {
       return new BCryptPasswordEncoder();
   }
   ```

3. [ ] Sửa method `changePassword()`
   - Inject `PasswordEncoder`
   - Dùng `passwordEncoder.matches()` để verify
   - Dùng `passwordEncoder.encode()` để hash

4. [ ] Migration: Hash existing passwords
   - Tạo script để hash tất cả passwords hiện tại
   - Hoặc force users đổi password lần đầu login

5. [ ] Test password change
   - Verify password được hash
   - Verify login vẫn hoạt động

**Thời gian**: 2-3 giờ

---

### [ ] Task 5: Fix DiscountService - Logic Giảm Giá Mâu Thuẫn
**Lỗi**: #11 - Ưu tiên 3 ghi đè ưu tiên 1 và 2  
**File**: `service/logicDiscount/DiscountService.java`  
**Mức độ**: 🔴 Critical

**Các bước thực hiện**:
1. [ ] Sửa method `applyDiscount()`
   - Đổi từ if-else-if sang if-else if-else if
   - Đảm bảo không có case nào ghi đè case khác

2. [ ] Test với nhiều cases
   - iPhone 35M → 29.75M (giảm 15%)
   - iPhone 8M → 6.8M (giảm 15%, không trừ 500k)
   - Samsung 15M → 13.2M (giảm 12%)
   - Xiaomi 5M → 4.5M (trừ 500k)

**Thời gian**: 1-2 giờ

---

### [ ] Task 6: Fix DiscountService - Giá Có Thể Âm
**Lỗi**: #12 - Giá âm khi trừ 500k  
**File**: `service/logicDiscount/DiscountService.java`  
**Mức độ**: 🔴 Critical

**Các bước thực hiện**:
1. [ ] Thêm check cuối method `applyDiscount()`
   ```java
   return discountPrice.max(BigDecimal.ZERO);
   ```

2. [ ] Test với giá thấp
   - Sản phẩm 300k → 0đ (không âm)

**Thời gian**: 15 phút

---

## Phase 2: Medium Issues (2-3 ngày)

### [ ] Task 7: Fix CartService - Giá Không Được Cập Nhật
**Lỗi**: #3 - Giá cũ không update khi re-add  
**File**: `service/cart/user/CartService.java`  
**Mức độ**: 🟡 Medium

**Các bước thực hiện**:
1. [ ] Quyết định policy: Luôn update giá mới hay giữ giá cũ?
   - Option 1: Luôn update (khuyến nghị)
   - Option 2: Giữ giá cũ (bảo vệ user)

2. [ ] Sửa code trong `addToCart()` khi item đã tồn tại
   ```java
   if (item != null) {
       // Luôn cập nhật giá mới
       item.setPriceAtTime(currentPrice);
       item.setTotalPrice(currentPrice.multiply(BigDecimal.valueOf(newQuantity)));
   }
   ```

3. [ ] Test re-add scenario
   - Thêm sản phẩm vào giỏ
   - Đổi giá sản phẩm
   - Thêm lại → verify giá mới

**Thời gian**: 1 giờ

---

### [ ] Task 8: Fix ProductService - Validate Duplicate Product Name
**Lỗi**: #4 - Có thể tạo nhiều products cùng tên  
**File**: `service/product/seller/ProductService.java`  
**Mức độ**: 🟡 Medium

**Các bước thực hiện**:
1. [ ] Thêm method vào ProductRepository
   ```java
   boolean existsByNameAndActiveTrue(String name);
   ```

2. [ ] Thêm validation trong `create()` và `update()`
   ```java
   if (productRepository.existsByNameAndActiveTrue(request.getName())) {
       throw new BadRequestException("Sản phẩm với tên này đã tồn tại");
   }
   ```

3. [ ] Tạo index cho performance
   ```sql
   CREATE INDEX idx_product_name_active 
   ON products(name, active);
   ```

4. [ ] Test duplicate name
   - Tạo product "iPhone 15"
   - Tạo lại "iPhone 15" → verify bị reject

**Thời gian**: 1 giờ

---

### [ ] Task 9: Fix ProductService - Soft Delete Không Check Giỏ Hàng
**Lỗi**: #5 - Xóa product đang có trong cart  
**File**: `service/product/seller/ProductService.java`  
**Mức độ**: 🟡 Medium

**Các bước thực hiện**:
1. [ ] Thêm method vào CartItemRepository
   ```java
   long countByProductId(Long productId);
   ```

2. [ ] Sửa method `deleteProduct()`
   ```java
   long cartCount = cartItemRepository.countByProductId(id);
   if (cartCount > 0) {
       throw new BadRequestException(
           "Không thể xóa sản phẩm này vì đang có " + cartCount + 
           " người dùng có trong giỏ hàng"
       );
   }
   ```

3. [ ] Test delete với product trong cart
   - Thêm product vào cart
   - Admin xóa product → verify bị reject

**Thời gian**: 1 giờ

---

### [ ] Task 10: Fix UserProductService - Average Rating Không Chính Xác
**Lỗi**: #7 - Average rating null khi review đầu tiên  
**File**: `service/product/user/UserProductService.java`  
**Mức độ**: 🟡 Medium

**Các bước thực hiện**:
1. [ ] Thêm `flush()` sau `save()`
   ```java
   reviewRepository.save(review);
   reviewRepository.flush(); // Force commit ngay
   
   Double avg = reviewRepository.getAverageRating(product.getId());
   ```

2. [ ] Test với review đầu tiên
   - Tạo review đầu tiên cho product
   - Verify average rating được tính đúng

**Thời gian**: 30 phút

---

### [ ] Task 11: Fix UserProfileService - Delete Address Race Condition
**Lỗi**: #9 - Race condition khi xóa địa chỉ mặc định  
**File**: `service/profile/UserProfileServiceImpl.java`  
**Mức độ**: 🟡 Medium

**Các bước thực hiện**:
1. [ ] Thêm method với lock vào UserAddressRepository
   ```java
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   Optional<UserAddress> findByIdAndUserIdWithLock(Long id, Long userId);
   ```

2. [ ] Sửa method `deleteAddress()`
   - Dùng `findByIdAndUserIdWithLock()` thay vì `findByIdAndUserId()`
   - Wrap trong `@Transactional`

3. [ ] Tạo index
   ```sql
   CREATE INDEX idx_user_address_user_default 
   ON user_addresses(user_id, is_default);
   ```

4. [ ] Test concurrent delete
   - Xóa 2 addresses cùng lúc
   - Verify vẫn có 1 address mặc định

**Thời gian**: 1-2 giờ

---

### [ ] Task 12: Fix CategoryService - Delete Không Check Soft Deleted Products
**Lỗi**: #10 - Không cho xóa category có products đã soft delete  
**File**: `service/category/CategoryService.java`  
**Mức độ**: 🟡 Medium

**Các bước thực hiện**:
1. [ ] Sửa method `delete()`
   ```java
   long activeProductCount = category.getProducts().stream()
           .filter(p -> p.getActive() != null && p.getActive())
           .count();
   
   if (activeProductCount > 0) {
       throw new RuntimeException(
           "Danh mục đang có " + activeProductCount + 
           " sản phẩm đang hoạt động, không thể xóa!"
       );
   }
   ```

2. [ ] Test delete category
   - Tạo category với 2 products
   - Soft delete 1 product
   - Xóa category → verify được phép (vì chỉ còn 1 active)

**Thời gian**: 30 phút

---

### [ ] Task 13: Fix LoyaltyPointService - Race Condition Khi Đổi Điểm
**Lỗi**: #13 - Race condition khi redeem points  
**File**: `service/loyalty/LoyaltyPointService.java`  
**Mức độ**: 🟡 Medium

**Các bước thực hiện**:
1. [ ] Thêm `@Version` vào LoyaltyAccount entity
   ```java
   @Version
   private Long version;
   ```

2. [ ] Tạo migration
   ```sql
   ALTER TABLE loyalty_accounts 
   ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
   ```

3. [ ] Sửa method `redeemPoints()`
   - Wrap trong try-catch `OptimisticLockException`
   - Throw user-friendly message

4. [ ] Test concurrent redeem
   - User có 500 điểm
   - Gửi 2 requests đổi 300 điểm cùng lúc
   - Verify chỉ 1 request thành công

**Thời gian**: 1-2 giờ

---

### [ ] Task 14: Fix SavedForLaterService - Không Kiểm Tra Stock Đúng Cách
**Lỗi**: #14 - Stock check không có lock  
**File**: `service/savedforlater/SavedForLaterService.java`  
**Mức độ**: 🟡 Medium

**Các bước thực hiện**:
1. [ ] Refactor method `moveToCart()`
   - Thay vì tự implement, gọi `CartService.addToCart()`
   - CartService đã có logic check stock + lock đầy đủ

2. [ ] Code mới:
   ```java
   @Transactional
   public void moveToCart(Long userId, Long savedItemId) {
       SavedForLater savedItem = savedForLaterRepository.findById(savedItemId)
               .orElseThrow(() -> new RuntimeException("Saved item không tồn tại"));
       
       if (!savedItem.getUserId().equals(userId)) {
           throw new RuntimeException("Không có quyền thao tác saved item này");
       }
       
       // Gọi CartService thay vì tự implement
       CartItemRequestDTO request = new CartItemRequestDTO();
       request.setProductId(savedItem.getProductId());
       request.setVariantId(savedItem.getVariantId());
       request.setQuantity(savedItem.getQuantity());
       
       cartService.addToCart(userId, request);
       
       savedForLaterRepository.delete(savedItem);
   }
   ```

3. [ ] Test move to cart với low stock
   - Lưu item với quantity = 5
   - Giảm stock xuống 3
   - Move to cart → verify bị reject

**Thời gian**: 1 giờ

---

## Phase 3: Low Priority Issues (1 ngày)

### [ ] Task 15: Fix VNPayService - Validate Return URL
**Lỗi**: #15 - Không validate return URL  
**File**: `service/payment/VNPayService.java`  
**Mức độ**: 🟢 Low (Security Best Practice)

**Các bước thực hiện**:
1. [ ] Thêm validation ở đầu method `createPaymentUrl()`
   ```java
   String returnUrl = vnpayConfig.getVnp_ReturnUrl();
   if (returnUrl == null || returnUrl.trim().isEmpty()) {
       throw new RuntimeException("Return URL không được cấu hình");
   }
   
   // Validate return URL format
   if (!returnUrl.startsWith("http://localhost") && 
       !returnUrl.startsWith("https://yourdomain.com")) {
       throw new RuntimeException("Return URL không hợp lệ");
   }
   ```

2. [ ] Test với invalid return URL
   - Set return URL = "https://evil.com"
   - Verify bị reject

**Thời gian**: 30 phút

---

## Testing Checklist

Sau khi hoàn thành tất cả tasks, chạy test suite:

### [ ] Concurrent Operations Tests
- [ ] 2 users cùng mua sản phẩm cuối cùng
- [ ] 2 users cùng đổi điểm loyalty
- [ ] 2 users cùng xóa địa chỉ mặc định

### [ ] Validation Tests
- [ ] Thêm vào cart với quantity âm
- [ ] Tạo product với tên trùng
- [ ] Tạo review duplicate
- [ ] Xóa product đang có trong cart

### [ ] Security Tests
- [ ] Password được hash đúng
- [ ] Login với password cũ vẫn hoạt động
- [ ] VNPay return URL validation

### [ ] Business Logic Tests
- [ ] Discount calculation với nhiều cases
- [ ] Average rating tính đúng
- [ ] Giá không bao giờ âm
- [ ] Category delete với soft deleted products

---

## Deployment Checklist

### [ ] Database Migrations
```sql
-- Run these in order:

-- 1. ProductVariant version
ALTER TABLE product_variants 
ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 2. LoyaltyAccount version
ALTER TABLE loyalty_accounts 
ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 3. Review unique constraint
ALTER TABLE reviews 
ADD CONSTRAINT uk_user_product 
UNIQUE (user_id, product_id);

-- 4. Indexes
CREATE INDEX idx_user_address_user_default 
ON user_addresses(user_id, is_default);

CREATE INDEX idx_product_name_active 
ON products(name, active);
```

### [ ] Code Deployment
1. [ ] Deploy Phase 1 (Critical fixes)
2. [ ] Monitor for 1 day
3. [ ] Deploy Phase 2 (Medium fixes)
4. [ ] Monitor for 1 day
5. [ ] Deploy Phase 3 (Low priority fixes)

### [ ] Post-Deployment Verification
- [ ] Check error logs
- [ ] Monitor concurrent operations
- [ ] Verify no new issues introduced
- [ ] Update Logic Health Score

---

## Estimated Timeline

| Phase | Tasks | Time | Priority |
|-------|-------|------|----------|
| Phase 1 | Tasks 1-6 | 1-2 ngày | 🔴 Critical |
| Phase 2 | Tasks 7-14 | 2-3 ngày | 🟡 Medium |
| Phase 3 | Task 15 | 1 ngày | 🟢 Low |
| **Total** | **15 tasks** | **4-6 ngày** | |

---

## Notes

- Mỗi task nên được test kỹ trước khi merge
- Critical tasks phải được review bởi senior developer
- Sau mỗi phase, chạy full regression test
- Update Logic Health Score sau khi hoàn thành: Dự kiến từ 72/100 → 95/100
