# Cart & Checkout UX Improvements

## ✅ Completed Tasks

### 1. Save for Later Functionality ✅
**Status:** COMPLETED

**What was done:**
- Created `SavedForLaterService` to handle saving cart items for later purchase
- Created `SavedItemsViewController` with route `/user/saved-items`
- Created template `user/saved/list.html` to display saved items
- Updated `CartController.saveForLater()` to use `SavedForLaterService` instead of wishlist
- Added bookmark icon to header navigation
- Added JavaScript to remove cart item from UI when saved
- Fixed API endpoint from `/user/saved/cart/save` to `/user/cart/save-for-later`

**Files modified:**
- `CartController.java` - Updated saveForLater endpoint
- `SavedItemsViewController.java` - NEW file for /user/saved-items route
- `user/saved/list.html` - NEW template
- `user/cart/list.html` - Fixed JavaScript API call
- `fragments/header.html` - Added bookmark icon

### 2. Estimated Delivery Date ✅
**Status:** COMPLETED (Cart page only)

**What was done:**
- Added delivery date display in cart items (3-5 business days)
- Added JavaScript function `calculateDeliveryDate()` to calculate business days
- Skips weekends (Saturday/Sunday) in calculation
- Displays date range in Vietnamese format

**Files modified:**
- `user/cart/list.html` - Added delivery estimate section and JavaScript

**Still TODO:**
- Add delivery date to checkout page (needs CSS styling)
- Add delivery date calculation script to checkout page

---

## 🔄 In Progress

### 2. Estimated Delivery Date (Checkout Page)
**Status:** PARTIALLY COMPLETE

**What needs to be done:**
1. Add CSS styling for `.delivery-estimate-box` in checkout.html
2. Add delivery date HTML section before "ĐẶT HÀNG NGAY" button
3. Add JavaScript to calculate and display delivery date on page load

**Suggested CSS:**
```css
.delivery-estimate-box {
    background: rgba(16, 185, 129, 0.1);
    border: 1px solid rgba(16, 185, 129, 0.3);
    border-radius: 12px;
    padding: 16px;
    margin: 20px 0;
    display: flex;
    align-items: center;
    gap: 12px;
}
```

**Suggested HTML location:** After "TỔNG CỘNG" row, before submit button

**Suggested JavaScript:**
```javascript
// Add to existing script section
document.addEventListener('DOMContentLoaded', function() {
    calculateCheckoutDeliveryDate();
});

function calculateCheckoutDeliveryDate() {
    // Same logic as cart page
    const today = new Date();
    let businessDays = 0;
    let currentDate = new Date(today);
    
    while (businessDays < 3) {
        currentDate.setDate(currentDate.getDate() + 1);
        const dayOfWeek = currentDate.getDay();
        if (dayOfWeek !== 0 && dayOfWeek !== 6) businessDays++;
    }
    const minDate = new Date(currentDate);
    
    while (businessDays < 5) {
        currentDate.setDate(currentDate.getDate() + 1);
        const dayOfWeek = currentDate.getDay();
        if (dayOfWeek !== 0 && dayOfWeek !== 6) businessDays++;
    }
    const maxDate = new Date(currentDate);
    
    const options = { weekday: 'short', day: 'numeric', month: 'numeric' };
    const minDateStr = minDate.toLocaleDateString('vi-VN', options);
    const maxDateStr = maxDate.toLocaleDateString('vi-VN', options);
    
    document.getElementById('checkout-delivery-date').textContent = `${minDateStr} - ${maxDateStr}`;
}
```

---

## 📋 Remaining Tasks

### 3. Progress Indicator in Checkout
**Status:** NOT STARTED

**Requirements:**
- Add step indicator showing: Cart → Checkout → Payment → Success
- Highlight current step
- Show completed steps with checkmark
- Responsive design

**Suggested location:** Top of checkout page, below breadcrumb

### 4. "Continue Shopping" Quick Link
**Status:** NOT STARTED

**Requirements:**
- Add prominent link/button to return to product listing
- Should be visible in cart page
- Should be visible in empty cart state

**Suggested locations:**
- Cart page: Below cart items or in empty state
- Checkout page: In header or sidebar

### 5. Cart Abandonment Reminder
**Status:** NOT STARTED

**Requirements:**
- Track when user adds items to cart
- Show reminder if user hasn't checked out after X minutes
- Use localStorage to persist cart state
- Show notification/modal encouraging checkout

**Implementation approach:**
- Use localStorage to track cart timestamp
- Use setTimeout to show reminder after 10-15 minutes
- Show modal with cart summary and "Complete Purchase" button

---

## Testing Checklist

### Save for Later
- [ ] Click "Save for Later" button in cart
- [ ] Verify item disappears from cart with animation
- [ ] Verify item appears in `/user/saved-items`
- [ ] Click "Move to Cart" in saved items page
- [ ] Verify item returns to cart
- [ ] Verify saved items count updates in header

### Estimated Delivery Date
- [ ] Open cart page
- [ ] Verify delivery date shows "3-5 business days" range
- [ ] Verify weekends are skipped in calculation
- [ ] Open checkout page
- [ ] Verify delivery date displays correctly

### Wishlist Integration
- [ ] Click heart icon on product
- [ ] Verify product added to wishlist
- [ ] Reload page
- [ ] Verify heart icon is red for wishlisted products
- [ ] Visit `/user/wishlist`
- [ ] Verify products display correctly

---

## Known Issues

1. **Wishlist page may not display products** - Need to check `WishlistService.getWishlistProductsByUserId()` implementation
2. **Checkout delivery date styling** - CSS not yet added
3. **Delivery date calculation** - Currently hardcoded to 3-5 days, should be configurable per product/region

---

## Next Steps

1. Complete checkout page delivery date (add CSS + HTML + JS)
2. Add progress indicator to checkout
3. Add "Continue Shopping" links
4. Implement cart abandonment reminder
5. Test all features end-to-end
6. Fix wishlist display issue if still present
