# 🛒 Kế Hoạch Cải Thiện UX Giỏ Hàng & Thanh Toán

## 📋 Tổng Quan

Kế hoạch chi tiết để cải thiện trải nghiệm người dùng cho luồng Giỏ hàng → Thanh toán → Hoàn tất, dựa trên đánh giá UX với mức độ ưu tiên **🔴 CAO**.

---

## 🎯 Mục Tiêu

### Conversion Rate
- **Hiện tại:** ~2-3% (ước tính)
- **Mục tiêu:** Tăng lên 4-5% (+50-70%)

### Cart Abandonment Rate
- **Hiện tại:** ~70-80% (ước tính)
- **Mục tiêu:** Giảm xuống 60-65% (-10-15%)

### User Satisfaction
- **Hiện tại:** 6/10
- **Mục tiêu:** 8.5/10

---

## ✅ Các Tính Năng Cần Thực Hiện

### 1. **Lưu Để Mua Sau (Save for Later)** ⭐⭐⭐

**Mô tả:**
Cho phép người dùng chuyển sản phẩm từ giỏ hàng sang danh sách "Lưu để mua sau" khi chưa muốn mua ngay.

**Lợi ích:**
- Giảm cart abandonment (người dùng không phải xóa sản phẩm)
- Tăng retention (người dùng quay lại mua sau)
- Capture intent (biết người dùng quan tâm sản phẩm gì)

**Thiết kế UI:**
```
┌─────────────────────────────────────────┐
│ Giỏ Hàng (3 sản phẩm)                   │
├─────────────────────────────────────────┤
│ [Ảnh] iPhone 15 Pro Max                 │
│       29.990.000₫                        │
│       [Xóa] [💾 Lưu để mua sau]         │
├─────────────────────────────────────────┤
│ ...                                      │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ 💾 Đã Lưu Để Mua Sau (2 sản phẩm)      │
├─────────────────────────────────────────┤
│ [Ảnh] Samsung Galaxy S24                │
│       22.990.000₫                        │
│       [Xóa] [🛒 Thêm vào giỏ]          │
└─────────────────────────────────────────┘
```

**Backend:**
- Entity: `SavedForLater`
  - `id`: Long
  - `userId`: Long
  - `productId`: Long
  - `variantId`: Long (nullable)
  - `quantity`: Integer
  - `savedAt`: LocalDateTime
  - `note`: String (nullable)

- Repository: `SavedForLaterRepository`
- Service: `SavedForLaterService`
  - `saveForLater(userId, cartItemId)`
  - `moveToCart(userId, savedItemId)`
  - `getSavedItems(userId)`
  - `removeSavedItem(userId, savedItemId)`

- Controller: `SavedForLaterController`
  - `POST /user/cart/save-for-later`
  - `POST /user/cart/move-to-cart`
  - `GET /user/saved-items`
  - `DELETE /user/saved-items/{id}`

**Frontend:**
- Button "Lưu để mua sau" trong mỗi cart item
- Section "Đã lưu để mua sau" dưới giỏ hàng
- Toast notification khi save/move
- Animation smooth khi chuyển item

**Thời gian:** 1-2 ngày

---

### 2. **Ngày Giao Hàng Dự Kiến (Estimated Delivery Date)** ⭐⭐

**Mô tả:**
Hiển thị ngày giao hàng dự kiến cho từng sản phẩm và tổng đơn hàng.

**Lợi ích:**
- Tăng trust (người dùng biết khi nào nhận hàng)
- Giảm anxiety (không lo lắng về thời gian giao)
- Tăng conversion (rõ ràng về timeline)

**Thiết kế UI:**
```
┌─────────────────────────────────────────┐
│ [Ảnh] iPhone 15 Pro Max                 │
│       29.990.000₫                        │
│       📦 Dự kiến giao: 18-20/04/2026    │
│       🚚 Giao hàng nhanh (2-3 ngày)     │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ Tổng Cộng: 52.980.000₫                  │
│ 📦 Dự kiến giao: 20/04/2026             │
│ (Dựa trên sản phẩm giao chậm nhất)      │
└─────────────────────────────────────────┘
```

**Logic tính toán:**
```java
// Công thức: Ngày đặt + Thời gian xử lý + Thời gian vận chuyển
LocalDate estimatedDate = LocalDate.now()
    .plusDays(processingDays)      // 1-2 ngày
    .plusDays(shippingDays);       // 1-3 ngày (tùy khu vực)

// Nếu cuối tuần, cộng thêm 1-2 ngày
if (estimatedDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
    estimatedDate = estimatedDate.plusDays(2);
} else if (estimatedDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
    estimatedDate = estimatedDate.plusDays(1);
}
```

**Backend:**
- Service: `DeliveryEstimationService`
  - `calculateDeliveryDate(productId, shippingAddress)`
  - `getShippingDays(province)` // Mapping tỉnh thành → số ngày
  - `getProcessingDays(productId)` // Thời gian xử lý đơn

- DTO: `DeliveryEstimateDTO`
  - `estimatedDate`: LocalDate
  - `minDays`: Integer
  - `maxDays`: Integer
  - `shippingMethod`: String

**Frontend:**
- Icon 📦 + text "Dự kiến giao: DD/MM/YYYY"
- Tooltip hover: "Thời gian giao hàng có thể thay đổi tùy khu vực"
- Highlight nếu giao nhanh (< 3 ngày)

**Thời gian:** 1 ngày

---

### 3. **Thanh Tiến Trình Checkout (Progress Indicator)** ⭐⭐⭐

**Mô tả:**
Progress bar hiển thị bước hiện tại trong quá trình checkout.

**Lợi ích:**
- Giảm confusion (người dùng biết đang ở bước nào)
- Tăng completion rate (thấy còn bao nhiêu bước)
- Better UX (professional, modern)

**Thiết kế UI:**
```
┌─────────────────────────────────────────────────────────┐
│  ●━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━│
│  ✓ Giỏ Hàng    ● Thanh Toán    ○ Hoàn Tất             │
└─────────────────────────────────────────────────────────┘

Bước 1: Giỏ Hàng (✓ Hoàn thành)
Bước 2: Thanh Toán (● Đang thực hiện)
Bước 3: Hoàn Tất (○ Chưa hoàn thành)
```

**Các bước:**
1. **Giỏ Hàng** (`/user/cart`)
   - Review sản phẩm
   - Điều chỉnh số lượng
   - Áp dụng voucher
   - Button: "Tiến hành thanh toán"

2. **Thanh Toán** (`/user/checkout`)
   - Nhập thông tin giao hàng
   - Chọn phương thức thanh toán
   - Chọn phương thức vận chuyển
   - Button: "Đặt hàng"

3. **Hoàn Tất** (`/user/order/success`)
   - Xác nhận đơn hàng
   - Hiển thị mã đơn hàng
   - Thông tin giao hàng
   - Button: "Tiếp tục mua sắm"

**Frontend:**
```html
<div class="checkout-progress">
    <div class="progress-step completed">
        <div class="step-icon">✓</div>
        <div class="step-label">Giỏ Hàng</div>
    </div>
    <div class="progress-line completed"></div>
    
    <div class="progress-step active">
        <div class="step-icon">2</div>
        <div class="step-label">Thanh Toán</div>
    </div>
    <div class="progress-line"></div>
    
    <div class="progress-step">
        <div class="step-icon">3</div>
        <div class="step-label">Hoàn Tất</div>
    </div>
</div>
```

**CSS:**
- Glassmorphism design
- Smooth transitions
- Mobile responsive (vertical layout)
- Active step highlight với animation

**Thời gian:** 0.5 ngày

---

### 4. **Nút "Tiếp Tục Mua Sắm" (Continue Shopping)** ⭐

**Mô tả:**
Quick link để quay lại trang sản phẩm từ giỏ hàng.

**Lợi ích:**
- Tăng AOV (Average Order Value) - người dùng mua thêm
- Better navigation
- Reduce friction

**Thiết kế UI:**
```
┌─────────────────────────────────────────┐
│ Giỏ Hàng (3 sản phẩm)                   │
├─────────────────────────────────────────┤
│ [← Tiếp tục mua sắm]                    │
│                                          │
│ [Sản phẩm 1]                            │
│ [Sản phẩm 2]                            │
│ [Sản phẩm 3]                            │
│                                          │
│ [Tổng cộng: 52.980.000₫]               │
│ [Tiến hành thanh toán →]                │
└─────────────────────────────────────────┘
```

**Vị trí:**
- Top của cart page (dưới breadcrumb)
- Bottom của cart page (trên footer)
- Success page (sau khi đặt hàng)

**Frontend:**
```html
<a href="/user/products" class="btn-continue-shopping">
    <i class="bi bi-arrow-left me-2"></i>
    Tiếp tục mua sắm
</a>
```

**Thời gian:** 0.25 ngày

---

### 5. **Nhắc Nhở Giỏ Hàng Bỏ Quên (Cart Abandonment Reminder)** ⭐⭐

**Mô tả:**
Nhắc nhở người dùng về giỏ hàng chưa thanh toán qua:
- Browser notification (nếu cho phép)
- Email reminder (sau 24h)
- Banner khi quay lại site

**Lợi ích:**
- Recover lost sales (15-20% cart abandonment recovery)
- Increase retention
- Better customer engagement

**Thiết kế UI:**

**Banner trên site:**
```
┌─────────────────────────────────────────────────────────┐
│ 🛒 Bạn có 3 sản phẩm trong giỏ hàng chưa thanh toán!   │
│    [Xem giỏ hàng] [Đóng]                                │
└─────────────────────────────────────────────────────────┘
```

**Browser notification:**
```
🛒 Smartphone Store
Bạn có 3 sản phẩm trong giỏ hàng chưa thanh toán!
iPhone 15 Pro Max và 2 sản phẩm khác
[Xem ngay]
```

**Email template:**
```
Subject: 🛒 Bạn quên gì trong giỏ hàng rồi!

Xin chào [Tên],

Bạn có 3 sản phẩm trong giỏ hàng chưa thanh toán:
- iPhone 15 Pro Max - 29.990.000₫
- Samsung Galaxy S24 - 22.990.000₫
- ...

Tổng cộng: 52.980.000₫

[Hoàn tất đơn hàng ngay]

Lưu ý: Giỏ hàng sẽ được giữ trong 7 ngày.
```

**Backend:**
- Service: `CartAbandonmentService`
  - `trackCartAbandonment(userId)`
  - `sendReminderEmail(userId)`
  - `sendBrowserNotification(userId)`
  - `getAbandonedCarts()` // Scheduled job

- Scheduled Job: Chạy mỗi 1h để check giỏ hàng bỏ quên
  ```java
  @Scheduled(cron = "0 0 * * * *") // Mỗi giờ
  public void checkAbandonedCarts() {
      List<Cart> abandonedCarts = cartService.getAbandonedCarts(24); // 24h
      for (Cart cart : abandonedCarts) {
          cartAbandonmentService.sendReminderEmail(cart.getUserId());
      }
  }
  ```

**Frontend:**
- LocalStorage: Lưu timestamp khi add to cart
- Banner sticky top khi có giỏ hàng chưa thanh toán
- Browser Notification API (request permission)

**Thời gian:** 1.5 ngày

---

## 📐 Thiết Kế Chi Tiết

### Database Schema

```sql
-- Bảng Lưu Để Mua Sau
CREATE TABLE saved_for_later (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT,
    quantity INT NOT NULL DEFAULT 1,
    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(500),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

-- Bảng Theo Dõi Cart Abandonment
CREATE TABLE cart_abandonment_tracking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    cart_id BIGINT NOT NULL,
    abandoned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reminder_sent BOOLEAN DEFAULT FALSE,
    reminder_sent_at TIMESTAMP,
    recovered BOOLEAN DEFAULT FALSE,
    recovered_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (cart_id) REFERENCES carts(id)
);

-- Thêm cột vào bảng products
ALTER TABLE products ADD COLUMN processing_days INT DEFAULT 1;
ALTER TABLE products ADD COLUMN shipping_days_min INT DEFAULT 2;
ALTER TABLE products ADD COLUMN shipping_days_max INT DEFAULT 4;
```

### API Endpoints

```
# Save for Later
POST   /user/cart/save-for-later
POST   /user/cart/move-to-cart
GET    /user/saved-items
DELETE /user/saved-items/{id}

# Delivery Estimation
GET    /user/cart/delivery-estimate
GET    /user/products/{id}/delivery-estimate

# Cart Abandonment
POST   /user/cart/track-abandonment
GET    /user/cart/abandoned
POST   /user/cart/recover
```

---

## 🎨 Design System

### Colors
- **Primary:** `#0d6efd` (Blue)
- **Success:** `#10b981` (Green)
- **Warning:** `#f59e0b` (Orange)
- **Info:** `#3b82f6` (Light Blue)
- **Muted:** `#64748b` (Gray)

### Icons
- 💾 Save for Later
- 🛒 Cart
- 📦 Delivery
- 🚚 Shipping
- ✓ Completed
- ● Active
- ○ Pending

### Animations
```css
/* Smooth slide animation */
@keyframes slideIn {
    from { transform: translateX(-100%); opacity: 0; }
    to { transform: translateX(0); opacity: 1; }
}

/* Progress bar fill */
@keyframes progressFill {
    from { width: 0%; }
    to { width: 100%; }
}
```

---

## 📱 Responsive Design

### Desktop (>= 1024px)
- Progress bar horizontal
- Save for later section bên phải
- 2 columns layout

### Tablet (768px - 1023px)
- Progress bar horizontal (smaller)
- Save for later section dưới cart
- 1 column layout

### Mobile (< 768px)
- Progress bar vertical (compact)
- Save for later collapsible
- Stack layout
- Sticky checkout button

---

## 🧪 Testing Plan

### Unit Tests
- [ ] SavedForLaterService.saveForLater()
- [ ] SavedForLaterService.moveToCart()
- [ ] DeliveryEstimationService.calculateDeliveryDate()
- [ ] CartAbandonmentService.sendReminderEmail()

### Integration Tests
- [ ] POST /user/cart/save-for-later
- [ ] POST /user/cart/move-to-cart
- [ ] GET /user/cart/delivery-estimate
- [ ] Scheduled job: checkAbandonedCarts()

### E2E Tests
- [ ] User saves item for later
- [ ] User moves saved item to cart
- [ ] User sees delivery estimate
- [ ] User completes checkout with progress bar
- [ ] User receives abandonment email

### Manual Tests
- [ ] UI/UX flow từ cart → checkout → success
- [ ] Responsive trên mobile/tablet/desktop
- [ ] Browser notification permission
- [ ] Email template rendering

---

## 📊 Success Metrics

### KPIs
1. **Cart Abandonment Rate**
   - Baseline: 75%
   - Target: 65%
   - Measurement: (Abandoned Carts / Total Carts) × 100

2. **Conversion Rate**
   - Baseline: 2.5%
   - Target: 4%
   - Measurement: (Orders / Sessions) × 100

3. **Average Order Value (AOV)**
   - Baseline: 25.000.000₫
   - Target: 28.000.000₫
   - Measurement: Total Revenue / Total Orders

4. **Save for Later Usage**
   - Target: 15% of cart items
   - Measurement: (Saved Items / Cart Items) × 100

5. **Email Recovery Rate**
   - Target: 10-15%
   - Measurement: (Recovered Orders / Reminder Emails) × 100

### Analytics Events
```javascript
// Track save for later
gtag('event', 'save_for_later', {
    product_id: productId,
    product_name: productName,
    value: price
});

// Track move to cart
gtag('event', 'move_to_cart', {
    product_id: productId,
    source: 'saved_for_later'
});

// Track checkout progress
gtag('event', 'checkout_progress', {
    step: 2, // 1=cart, 2=checkout, 3=success
    value: totalAmount
});

// Track cart abandonment
gtag('event', 'cart_abandoned', {
    cart_value: totalAmount,
    items_count: itemsCount
});
```

---

## ⏱️ Timeline & Effort

### Phase 1: Core Features (3-4 ngày)
- **Ngày 1-2:** Save for Later
  - Backend: Entity, Repository, Service, Controller
  - Frontend: UI, API integration, animations
  - Testing: Unit + Integration tests

- **Ngày 3:** Delivery Estimation
  - Backend: Service, logic tính toán
  - Frontend: Display UI
  - Testing: Unit tests

- **Ngày 3.5:** Progress Indicator
  - Frontend: UI component
  - CSS: Animations, responsive
  - Testing: Manual testing

- **Ngày 4:** Continue Shopping Button
  - Frontend: Add buttons
  - Testing: Quick check

### Phase 2: Advanced Features (1.5-2 ngày)
- **Ngày 5-6:** Cart Abandonment
  - Backend: Tracking service, scheduled job
  - Email: Template, sending logic
  - Frontend: Banner, notification
  - Testing: E2E tests

### Phase 3: Polish & Testing (1 ngày)
- **Ngày 7:** Final testing, bug fixes, optimization

**Tổng thời gian:** 6-7 ngày làm việc

---

## 🚀 Deployment Plan

### Pre-deployment
1. Code review
2. QA testing
3. Performance testing
4. Database migration script
5. Rollback plan

### Deployment Steps
1. Deploy database changes (migration)
2. Deploy backend services
3. Deploy frontend changes
4. Enable scheduled jobs
5. Monitor logs & metrics

### Post-deployment
1. Monitor error rates
2. Check analytics events
3. Verify email sending
4. User feedback collection
5. A/B testing setup

---

## 🎯 Priority Order

### Must Have (P0)
1. ✅ Save for Later
2. ✅ Progress Indicator
3. ✅ Continue Shopping Button

### Should Have (P1)
4. ✅ Delivery Estimation
5. ✅ Cart Abandonment Email

### Nice to Have (P2)
6. Browser Notification
7. Advanced analytics
8. Personalized recommendations

---

## 📝 Notes

### Technical Considerations
- **Performance:** Cache delivery estimates (Redis)
- **Scalability:** Queue system cho email sending
- **Security:** Validate user permissions
- **Privacy:** GDPR compliance cho email

### Business Considerations
- **Cost:** Email sending service (SendGrid, AWS SES)
- **Legal:** Terms & conditions cho save for later
- **Support:** Customer service training

### Future Enhancements
- AI-powered delivery prediction
- Dynamic pricing based on cart value
- Personalized abandonment emails
- SMS reminders
- WhatsApp integration

---

**Người tạo:** AI Developer  
**Ngày tạo:** 14/04/2026  
**Phiên bản:** 1.0  
**Status:** 📋 READY FOR IMPLEMENTATION
