# RuntimeException Migration List

## Services Needing Migration

Based on search results, the following service classes contain RuntimeException usage that needs to be migrated to BusinessException:

### Already Migrated (Tasks 4-6)
- ✅ CartService
- ✅ ProductService (seller)
- ✅ UserProductService

### Remaining Services to Migrate

1. **SavedForLaterService** (4 occurrences)
   - Line 49: Authorization check - "Không có quyền thao tác cart item này"
   - Line 60: Duplicate check - "Sản phẩm đã được lưu trước đó"
   - Line 90: Authorization check - "Không có quyền thao tác saved item này"
   - Line 129: Authorization check - "Không có quyền xóa saved item này"

2. **OrderService** (10 occurrences)
   - Line 75: Cart not found - "Giỏ hàng không tồn tại!"
   - Line 75: Empty cart - "Giỏ hàng trống!"
   - Line 116: Insufficient stock - "Sản phẩm {name} không đủ hàng!"
   - Line 163: Authorization check - "Bạn không có quyền thao tác đơn hàng này."
   - Line 166: Invalid status - "Chỉ có thể hủy đơn khi đang ở trạng thái Chờ xác nhận."
   - Line 274: Order not found - "Đơn hàng không tồn tại!"
   - Line 275: Authorization check - "Bạn không có quyền xem đơn hàng này."
   - Line 302: Order not found - "Không tìm thấy đơn hàng!"
   - Line 303: Authorization check - "Bạn không có quyền thao tác đơn hàng này."
   - Line 307: Empty order - "Đơn hàng không có sản phẩm."
   - Line 331: Invalid status transition - "Không thể chuyển trạng thái từ {from} sang {to}"

3. **RefundService** (11 occurrences)
   - Line 44: Authorization check - "Bạn không có quyền thao tác đơn hàng này."
   - Line 49: Invalid status - "Chỉ có thể yêu cầu hoàn trả khi đơn hàng đã được giao."
   - Line 60: Missing delivery info - "Không tìm thấy thông tin giao hàng. Vui lòng liên hệ hỗ trợ."
   - Line 65: Expired window - "Đã quá thời hạn {days} ngày để yêu cầu hoàn trả."
   - Line 70: Duplicate request - "Đơn hàng này đang có yêu cầu hoàn trả chờ duyệt."
   - Line 75: Empty items - "Vui lòng chọn ít nhất 1 sản phẩm cần hoàn trả."
   - Line 84: Invalid items - "Vui lòng nhập số lượng trả lại cho ít nhất 1 sản phẩm."
   - Line 107: Invalid item - "Sản phẩm không thuộc đơn hàng này."
   - Line 122: Invalid quantity - "Số lượng trả lại không hợp lệ cho {product}"
   - Line 143: Invalid amount - "Số tiền hoàn trả không hợp lệ cho {product}"
   - Line 213: Already processed - "Yêu cầu hoàn trả này đã được xử lý."
   - Line 292: Already processed - "Yêu cầu hoàn trả này đã được xử lý."

4. **LoyaltyPointService** (4 occurrences)
   - Line 122: Minimum points - "Cần tối thiểu {min} điểm để đổi ưu đãi. Bạn muốn đổi {points} điểm."
   - Line 130: Insufficient points - "Điểm không đủ. Bạn có {current} điểm, cần {required} điểm."
   - Line 175: Race condition - "Có người khác đang thao tác với điểm tích lũy của bạn. Vui lòng thử lại."
   - Line 203: Invalid delta - "Delta không được bằng 0"
   - Line 209: Negative balance - "Không thể trừ {delta} điểm. User chỉ có {current} điểm."

5. **UserProfileServiceImpl** (3 occurrences)
   - Line 88: Wrong password - "Mật khẩu hiện tại không đúng"
   - Line 92: Password mismatch - "Mật khẩu mới và xác nhận không khớp"
   - Line 96: Password too short - "Mật khẩu mới phải có ít nhất 6 ký tự"
   - Line 120: Address limit - "Bạn chỉ được lưu tối đa 5 địa chỉ. Hãy xóa bớt địa chỉ cũ nhé!"

6. **VNPayService** (2 occurrences)
   - Line 26: Missing config - "Return URL không được cấu hình"
   - Line 33: Invalid URL - "Return URL không hợp lệ. Chỉ chấp nhận HTTPS hoặc localhost"

7. **EmailNotificationService** (2 occurrences)
   - Line 101: Email send failure - "Không thể gửi email thông báo"
   - Line 317: Test email failure - "Không thể gửi test email"

8. **CouponServiceImpl** (1 occurrence)
   - Line 49: Duplicate coupon - "Bạn đã lưu mã này rồi"

## Exception Type Mapping

Based on the error scenarios, here's the recommended BusinessException mapping:

- **Authorization errors** → UnauthorizedAccessException (HTTP 403)
- **Not found errors** → EntityNotFoundException (HTTP 404)
- **Validation errors** → BadRequestException (HTTP 400)
- **Stock/quantity errors** → InsufficientStockException (HTTP 400)
- **Status transition errors** → InvalidOrderStatusException (HTTP 400)
- **Configuration errors** → RuntimeException (keep as-is, system error)
- **External service errors** → RuntimeException (keep as-is, system error)

## Migration Priority

1. **High Priority**: SavedForLaterService, OrderService, RefundService (user-facing features)
2. **Medium Priority**: LoyaltyPointService, UserProfileServiceImpl, CouponServiceImpl
3. **Low Priority**: VNPayService, EmailNotificationService (system/infrastructure)
