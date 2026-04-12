# Báo Cáo Lỗi Logic Toàn Hệ Thống E-commerce

## Tổng Quan
Đã review toàn bộ 11 Service classes trong hệ thống để tìm lỗi logic.

**Ngày**: 12/04/2026  
**Trạng thái**: Phát hiện 12 lỗi logic mới (ngoài 8 lỗi đã fix ở OMS/Refund)

---

## Danh Sách Service Đã Review

| # | Service | File | Số Lỗi |
|---|---------|------|--------|
| 1 | CartService | cart/user/CartService.java | 3 |
| 2 | ProductService (Seller) | product/seller/ProductService.java | 2 |
| 3 | UserProductService | product/user/UserProductService.java | 2 |
| 4 | WishlistServiceImpl | wishlist/WishlistServiceImpl.java | 0 ✅ |
| 5 | UserProfileServiceImpl | profile/UserProfileServiceImpl.java | 2 |
| 6 | DashboardService | dashboard/seller/DashboardService.java | 0 ✅ |
| 7 | CategoryService | category/CategoryService.java | 1 |
| 8 | DiscountService | logicDiscount/DiscountService.java | 2 |
| 9 | OrderService | order/user/OrderService.java | ✅ Đã fix |
| 10 | RefundService | order/RefundService.java | ✅ Đã fix |
| 11 | CouponServiceImpl | coupon/CouponServiceImpl.java | ✅ Đã fix |

**Tổng cộng**: 12 lỗi logic mới cần fix

---

## Chi Tiết Các Lỗi

### 🔴 LỖI #1: CartService - Race Condition Khi Thêm Vào Giỏ
**File**: `CartService.java`  
**Method**: `addToCart()`  
**Mức độ**: 🔴 Critical

#### Vấn đề
Khi 2 user cùng thêm sản phẩm vào giỏ đồng thời, có thể vượt quá số lượng tồn kho.

#### Kịch bản
```
Sản phẩm A: stockQuantity = 5

Thời gian    User 1                          User 2
--------    ------                          ------
T1          Đọc stock = 5                   
T2                                          Đọc stock = 5
T3          Check: 3 < 5 ✓                  
T4                                          Check: 3 < 5 ✓
T5          Thêm 3 vào giỏ                  
T6                                          Thêm 3 vào giỏ
T7          Checkout: stock = 2             
T8                                          Checkout: stock = -1 ❌
```

#### Code hiện tại
```java
private void checkStock(ProductVariant variant, int quantity) {
    if (variant.getStockQuantity() < quantity) {
        throw new BadRequestException("Không đủ hàng");
    }
}
```

#### Giải pháp
Thêm optimistic locking hoặc pessimistic locking cho ProductVariant:

```java
// Option 1: Optimistic Locking (Khuyến nghị)
@Entity
public class ProductVariant {
    @Version
    private Long version;
    // ...
}

// Option 2: Pessimistic Locking trong addToCart
@Lock(LockModeType.PESSIMISTIC_WRITE)
ProductVariant variant = productVariantRepository.findByIdWithLock(variantId);
```

---

### 🔴 LỖI #2: CartService - Không Validate Số Lượng Âm
**File**: `CartService.java`  
**Method**: `addToCart()`  
**Mức độ**: 🔴 Critical

#### Vấn đề
Không kiểm tra `request.getQuantity()` có thể âm, dẫn đến giỏ hàng có số lượng âm.

#### Code hiện tại
```java
public CartResponseDTO addToCart(Long userId, CartItemRequestDTO request) {
    // Không có validation cho request.getQuantity()
    if (item != null) {
        int newQuantity = item.getQuantity() + request.getQuantity(); // Có thể âm!
        checkStock(variant, newQuantity);
```

#### Kịch bản tấn công
```
POST /cart/add
{
  "productId": 1,
  "variantId": 1,
  "quantity": -100  // ❌ Không bị chặn
}
```

#### Giải pháp
```java
public CartResponseDTO addToCart(Long userId, CartItemRequestDTO request) {
    // Thêm validation
    if (request.getQuantity() == null || request.getQuantity() <= 0) {
        throw new BadRequestException("Số lượng phải lớn hơn 0");
    }
    // ... rest of code
}
```

---

### 🟡 LỖI #3: CartService - Giá Không Được Cập Nhật Khi Thêm Vào Giỏ
**File**: `CartService.java`  
**Method**: `addToCart()`  
**Mức độ**: 🟡 Medium

#### Vấn đề
Khi thêm sản phẩm đã có trong giỏ, giá cũ (`priceAtTime`) không được cập nhật, dẫn đến user mua với giá cũ khi giá mới rẻ hơn.

#### Code hiện tại
```java
if (item != null) {
    int newQuantity = item.getQuantity() + request.getQuantity();
    checkStock(variant, newQuantity);
    item.setQuantity(newQuantity);
    
    // ❌ Giữ nguyên giá cũ
    if (item.getPriceAtTime() == null) {
        item.setPriceAtTime(currentPrice);
    }
    
    item.setTotalPrice(item.getPriceAtTime().multiply(BigDecimal.valueOf(newQuantity)));
}
```

#### Kịch bản
```
Ngày 1: User thêm iPhone vào giỏ, giá = 30,000,000đ
Ngày 2: Giá giảm còn 25,000,000đ
Ngày 2: User thêm thêm 1 chiếc nữa
Kết quả: Cả 2 chiếc đều tính 30,000,000đ (giá cũ) ❌
```

#### Giải pháp
Có 2 cách:

**Option 1: Luôn cập nhật giá mới (Khuyến nghị cho user)**
```java
if (item != null) {
    int newQuantity = item.getQuantity() + request.getQuantity();
    checkStock(variant, newQuantity);
    item.setQuantity(newQuantity);
    
    // Cập nhật giá mới
    item.setPriceAtTime(currentPrice);
    item.setTotalPrice(currentPrice.multiply(BigDecimal.valueOf(newQuantity)));
}
```

**Option 2: Giữ nguyên giá cũ (Bảo vệ user khỏi tăng giá)**
```java
// Giữ nguyên code hiện tại, nhưng thêm comment giải thích
// Giữ nguyên giá cũ để bảo vệ user khỏi tăng giá đột ngột
```

---

### 🔴 LỖI #4: ProductService - Không Validate Duplicate Product Name
**File**: `product/seller/ProductService.java`  
**Method**: `create()`, `update()`  
**Mức độ**: 🟡 Medium

#### Vấn đề
Có thể tạo nhiều sản phẩm cùng tên, gây nhầm lẫn.

#### Giải pháp
```java
public ProductResponseDTO create(ProductRequestDTO request) {
    // Thêm check duplicate
    if (productRepository.existsByNameAndActiveTrue(request.getName())) {
        throw new BadRequestException("Sản phẩm với tên này đã tồn tại");
    }
    // ... rest of code
}
```

---

### 🟡 LỖI #5: ProductService - Soft Delete Không Kiểm Tra Sản Phẩm Trong Giỏ Hàng
**File**: `product/seller/ProductService.java`  
**Method**: `deleteProduct()`  
**Mức độ**: 🟡 Medium

#### Vấn đề
Khi admin xóa sản phẩm (soft delete), không kiểm tra xem có user nào đang có sản phẩm này trong giỏ hàng không.

#### Kịch bản
```
1. User A thêm iPhone vào giỏ
2. Admin xóa iPhone (active = false)
3. User A checkout → Lỗi vì sản phẩm không active
```

#### Giải pháp
```java
@Override
public void deleteProduct(Long id) {
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product không tồn tại"));

    if (!product.getActive()) {
        throw new BadRequestException("Product đã bị xoá trước đó");
    }

    // Kiểm tra có trong giỏ hàng không
    long cartCount = cartItemRepository.countByProductId(id);
    if (cartCount > 0) {
        throw new BadRequestException(
            "Không thể xóa sản phẩm này vì đang có " + cartCount + " người dùng có trong giỏ hàng"
        );
    }

    product.setActive(false);
    product.setUpdatedAt(LocalDateTime.now());
    productRepository.save(product);
}
```

---

### 🔴 LỖI #6: UserProductService - Review Duplicate Không Kiểm Tra Đúng
**File**: `product/user/UserProductService.java`  
**Method**: `reviewProduct()`  
**Mức độ**: 🔴 Critical

#### Vấn đề
Logic kiểm tra duplicate review có race condition.

#### Code hiện tại
```java
if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
    throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi");
}

// Tạo review
Review review = new Review();
// ...
reviewRepository.save(review);
```

#### Kịch bản race condition
```
User gửi 2 request đánh giá cùng lúc:
T1: Request 1 check → chưa có review ✓
T2: Request 2 check → chưa có review ✓
T3: Request 1 save → OK
T4: Request 2 save → OK (Duplicate!) ❌
```

#### Giải pháp
Thêm unique constraint trong database:

```sql
ALTER TABLE reviews 
ADD CONSTRAINT uk_user_product 
UNIQUE (user_id, product_id);
```

Và handle exception:
```java
try {
    reviewRepository.save(review);
} catch (DataIntegrityViolationException e) {
    throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi");
}
```

---

### 🟡 LỖI #7: UserProductService - Average Rating Không Chính Xác Khi Có Review Đầu Tiên
**File**: `product/user/UserProductService.java`  
**Method**: `reviewProduct()`  
**Mức độ**: 🟡 Medium

#### Vấn đề
Khi tạo review đầu tiên, `getAverageRating()` trả về `null` vì query chưa có data.

#### Code hiện tại
```java
reviewRepository.save(review);

Double avg = reviewRepository.getAverageRating(product.getId());

product.setAverageRating(
    avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0
);
```

#### Vấn đề
Query `getAverageRating()` có thể chưa thấy review vừa save (transaction chưa commit).

#### Giải pháp
```java
reviewRepository.save(review);
reviewRepository.flush(); // Force commit ngay

Double avg = reviewRepository.getAverageRating(product.getId());
product.setAverageRating(
    avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0
);
```

---

### 🟡 LỖI #8: UserProfileService - Password Change Không Hash
**File**: `profile/UserProfileServiceImpl.java`  
**Method**: `changePassword()`  
**Mức độ**: 🔴 Critical (Security)

#### Vấn đề
Mật khẩu được lưu dạng plain text, không hash.

#### Code hiện tại
```java
// Vì chưa có BCrypt, so sánh plain text (sẽ thay bằng BCrypt khi thêm Security)
if (!dto.getCurrentPassword().equals(user.getPassword())) {
    throw new RuntimeException("Mật khẩu hiện tại không đúng");
}
// ...
user.setPassword(dto.getNewPassword()); // ❌ Plain text
```

#### Giải pháp
```java
// Thêm dependency
// implementation 'org.springframework.security:spring-security-crypto'

private final PasswordEncoder passwordEncoder;

public void changePassword(Long userId, PasswordChangeRequestDTO dto) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

    // So sánh với BCrypt
    if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
        throw new RuntimeException("Mật khẩu hiện tại không đúng");
    }
    
    if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
        throw new RuntimeException("Mật khẩu mới và xác nhận không khớp");
    }
    
    if (dto.getNewPassword().length() < 6) {
        throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
    }

    // Hash trước khi lưu
    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
    userRepository.save(user);
}
```

---

### 🟡 LỖI #9: UserProfileService - Delete Address Race Condition
**File**: `profile/UserProfileServiceImpl.java`  
**Method**: `deleteAddress()`  
**Mức độ**: 🟡 Medium

#### Vấn đề
Khi xóa địa chỉ mặc định, logic tự động set địa chỉ đầu tiên làm mặc định có race condition.

#### Code hiện tại
```java
boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
userAddressRepository.delete(address);

// Nếu xóa địa chỉ mặc định, tự động set địa chỉ đầu tiên còn lại làm mặc định
if (wasDefault) {
    userAddressRepository.findByUserIdOrderByIsDefaultDesc(userId)
            .stream().findFirst().ifPresent(first -> {
                first.setIsDefault(true);
                userAddressRepository.save(first);
            });
}
```

#### Vấn đề
Nếu 2 request xóa địa chỉ cùng lúc, có thể không có địa chỉ mặc định nào.

#### Giải pháp
Wrap trong transaction và thêm lock:

```java
@Transactional
public void deleteAddress(Long userId, Long addressId) {
    UserAddress address = userAddressRepository.findByIdAndUserIdWithLock(addressId, userId)
            .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại"));

    boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
    userAddressRepository.delete(address);

    if (wasDefault) {
        List<UserAddress> remaining = userAddressRepository.findByUserIdOrderByIsDefaultDesc(userId);
        if (!remaining.isEmpty()) {
            UserAddress first = remaining.get(0);
            first.setIsDefault(true);
            userAddressRepository.save(first);
        }
    }
}
```

---

### 🟡 LỖI #10: CategoryService - Delete Category Không Kiểm Tra Soft Deleted Products
**File**: `category/CategoryService.java`  
**Method**: `delete()`  
**Mức độ**: 🟡 Medium

#### Vấn đề
Chỉ kiểm tra `category.getProducts()` nhưng không lọc theo `active = true`.

#### Code hiện tại
```java
public void delete(Long id) {
    Category category = findById(id);
    if (category != null) {
        // Check an toàn: không cho xóa nếu có sản phẩm trỏ tới
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new RuntimeException("Danh mục đang có " + category.getProducts().size() + " sản phẩm, không thể xóa!");
        }
        categoryRepository.deleteById(id);
    }
}
```

#### Vấn đề
Nếu tất cả products đều đã soft delete (active = false), vẫn không cho xóa category.

#### Giải pháp
```java
public void delete(Long id) {
    Category category = findById(id);
    if (category != null) {
        // Chỉ đếm sản phẩm active
        long activeProductCount = category.getProducts().stream()
                .filter(p -> p.getActive() != null && p.getActive())
                .count();
        
        if (activeProductCount > 0) {
            throw new RuntimeException("Danh mục đang có " + activeProductCount + " sản phẩm đang hoạt động, không thể xóa!");
        }
        categoryRepository.deleteById(id);
    }
}
```

---

### 🔴 LỖI #11: DiscountService - Logic Giảm Giá Mâu Thuẫn
**File**: `logicDiscount/DiscountService.java`  
**Method**: `applyDiscount()`  
**Mức độ**: 🔴 Critical

#### Vấn đề
Logic giảm giá có nhiều điều kiện chồng chéo, dẫn đến kết quả không đúng.

#### Code hiện tại
```java
public BigDecimal applyDiscount(Product product) {
    BigDecimal price = product.getPrice();
    BigDecimal discountPrice = price;

    // Ưu tiên 1: Hàng xa xỉ ≥ 30 triệu → giảm 15%
    if (price.compareTo(BigDecimal.valueOf(30_000_000)) >= 0) {
        discountPrice = price.multiply(BigDecimal.valueOf(0.85));
    }
    // Ưu tiên 2: Theo thương hiệu
    else if ("Apple".equalsIgnoreCase(product.getBrand())) {
        discountPrice = price.multiply(BigDecimal.valueOf(0.85));
    } else if ("Samsung".equalsIgnoreCase(product.getBrand())) {
        discountPrice = price.multiply(BigDecimal.valueOf(0.88));
    } else {
        discountPrice = price.multiply(BigDecimal.valueOf(0.90));
    }
    // Ưu tiên 3: Giá rẻ < 10 triệu → trừ thẳng 500k
    if (price.compareTo(BigDecimal.valueOf(10_000_000)) < 0) {
        discountPrice = price.subtract(BigDecimal.valueOf(500_000));
    }

    return discountPrice;
}
```

#### Vấn đề
Ưu tiên 3 luôn ghi đè ưu tiên 1 và 2!

#### Ví dụ
```
iPhone 15 Pro Max: 35,000,000đ, Brand = Apple
- Ưu tiên 1: 35M >= 30M → giảm 15% = 29,750,000đ
- Ưu tiên 3: KHÔNG áp dụng (35M >= 10M)
- Kết quả: 29,750,000đ ✓

iPhone SE: 8,000,000đ, Brand = Apple
- Ưu tiên 2: Apple → giảm 15% = 6,800,000đ
- Ưu tiên 3: 8M < 10M → trừ 500k = 6,300,000đ ❌ (Ghi đè!)
- Kết quả: 6,300,000đ (Sai! Nên là 6,800,000đ)
```

#### Giải pháp
Sửa logic để không chồng chéo:

```java
public BigDecimal applyDiscount(Product product) {
    BigDecimal price = product.getPrice();
    BigDecimal discountPrice;

    // Case 1: Giá rẻ < 10 triệu → trừ thẳng 500k
    if (price.compareTo(BigDecimal.valueOf(10_000_000)) < 0) {
        discountPrice = price.subtract(BigDecimal.valueOf(500_000));
    }
    // Case 2: Hàng xa xỉ ≥ 30 triệu → giảm 15%
    else if (price.compareTo(BigDecimal.valueOf(30_000_000)) >= 0) {
        discountPrice = price.multiply(BigDecimal.valueOf(0.85));
    }
    // Case 3: Theo thương hiệu (10M - 30M)
    else if ("Apple".equalsIgnoreCase(product.getBrand())) {
        discountPrice = price.multiply(BigDecimal.valueOf(0.85));
    } else if ("Samsung".equalsIgnoreCase(product.getBrand())) {
        discountPrice = price.multiply(BigDecimal.valueOf(0.88));
    } else {
        discountPrice = price.multiply(BigDecimal.valueOf(0.90));
    }

    // Đảm bảo không âm
    return discountPrice.max(BigDecimal.ZERO);
}
```

---

### 🔴 LỖI #12: DiscountService - Giá Có Thể Âm
**File**: `logicDiscount/DiscountService.java`  
**Method**: `applyDiscount()`, `applyDiscountToVariant()`  
**Mức độ**: 🔴 Critical

#### Vấn đề
Khi giá < 500k, trừ 500k sẽ ra số âm.

#### Ví dụ
```
Sản phẩm giá 300,000đ
Giảm 500,000đ
Kết quả: -200,000đ ❌
```

#### Giải pháp
Đã có trong code mới ở trên: `return discountPrice.max(BigDecimal.ZERO);`

---

## Tổng Kết

### Mức độ ưu tiên

**🔴 Critical (Phải fix ngay):**
1. CartService - Race condition khi thêm vào giỏ
2. CartService - Không validate số lượng âm
3. UserProductService - Review duplicate race condition
4. UserProfileService - Password không hash
5. DiscountService - Logic giảm giá mâu thuẫn
6. DiscountService - Giá có thể âm

**🟡 Medium (Nên fix):**
7. CartService - Giá không được cập nhật
8. ProductService - Không validate duplicate name
9. ProductService - Soft delete không check giỏ hàng
10. UserProductService - Average rating không chính xác
11. UserProfileService - Delete address race condition
12. CategoryService - Delete không check soft deleted products

---

## Các Bước Tiếp Theo

### 1. Database Migrations Cần Thiết

```sql
-- Fix #6: Unique constraint cho reviews
ALTER TABLE reviews 
ADD CONSTRAINT uk_user_product 
UNIQUE (user_id, product_id);

-- Fix #1: Version column cho ProductVariant
ALTER TABLE product_variants 
ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- Fix #9: Index cho performance
CREATE INDEX idx_user_address_user_default 
ON user_addresses(user_id, is_default);
```

### 2. Code Changes Priority

**Phase 1 (Critical - 1-2 ngày):**
- Fix #1, #2: CartService validations + locking
- Fix #5: DiscountService logic
- Fix #6: DiscountService negative price
- Fix #4: UserProfileService password hashing

**Phase 2 (Medium - 2-3 ngày):**
- Fix #3: CartService price update
- Fix #7, #8: ProductService validations
- Fix #9, #10: UserProductService improvements
- Fix #11, #12: UserProfileService + CategoryService

### 3. Testing Checklist

Sau khi fix, cần test:
- [ ] Concurrent cart operations (2+ users cùng mua)
- [ ] Negative quantity attacks
- [ ] Discount calculation với nhiều cases
- [ ] Review duplicate prevention
- [ ] Password hashing/verification
- [ ] Address management edge cases
- [ ] Category deletion với soft deleted products

---

## Kết Luận

Hệ thống có **12 lỗi logic mới** cần fix, trong đó:
- **6 lỗi Critical** (bảo mật + race conditions)
- **6 lỗi Medium** (business logic + edge cases)

Ưu tiên fix các lỗi Critical trước để đảm bảo:
1. Không bị vượt quá tồn kho
2. Không bị tấn công với số lượng âm
3. Giá giảm được tính đúng
4. Mật khẩu được bảo mật
5. Không có duplicate reviews
6. Không có giá âm

**Thời gian ước tính**: 3-5 ngày để fix hết tất cả lỗi.
