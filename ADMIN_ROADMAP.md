# 🚀 SMARTZONE ENTERPRISE - ADMIN ROADMAP

Chào mừng đến với bản đồ lộ trình phát triển hệ thống Quản trị SmartZone. Bản tài liệu này ghi lại các cột mốc đã đạt được và các tính năng dự kiến trong tương lai.

---

## 🧭 TRẠNG THÁI TỔNG QUÁT
- **Current Version**: v2.4.0 (Loyalty & Wallet Integrated)
- **Engine**: Spring Boot 3.2, Thymeleaf, Glassmorphism UI
- **Health Score**: 🟢 Stable (95/100)

---

## 🛠 DANH SÁCH TÍNH NĂNG & TIẾN ĐỘ

### 1. Master Dashboard & Intelligent Analytics
- [x] **Core UI Framework**: Glassmorphism, Responsive sidebar, Bento grids.
- [x] **Master KPI Row**: Doanh thu, User, Đơn hàng, Tồn kho.
- [x] **Financial KPI Row**: Số dư ví, Rút tiền chờ duyệt, Loyalty balance.
- [ ] **Restock Intelligence**: Thống kê sản phẩm hết hàng được đăng ký nhận thông báo nhiều nhất.
- [ ] **Marketing Intelligence**: Thống kê sản phẩm được "Yêu thích" và "So sánh" nhiều nhất.
- [ ] **Trending Search**: Theo dõi các từ khóa người dùng đang tìm kiếm.
- [ ] **Recommendation Tuner**: UI kích hoạt Rebuild AI gợi ý sản phẩm hoặc thủ công ghim sản phẩm nổi bật.

### 2. Quản lý Tài chính & Thanh toán (Finance & Payment)
- [x] **Ví SmartZone Xu**: Hệ thống ví tiền thật cho User.
- [x] **Manual Top-up**: Admin nạp tiền bồi thường/hỗ trợ cho User.
- [x] **Withdrawal Workflow**: Quy trình yêu cầu và duyệt rút tiền ngân hàng.
- [x] **Payment Auditing**: Nhật ký giao dịch VNPAY/Ví (Checksum, Transaction ID) để đối soát lỗi thanh toán.
- [ ] **Policy Manager**: Cấu hình tỷ lệ đổi điểm và quy định rút tiền trực tiếp trên UI.
- [x] **Financial Reporting**: Xuất báo cáo giao dịch ví (Excel/PDF) định kỳ.

### 3. CRM & Hỗ trợ khách hàng (CRM & Support)
- [x] **User Management**: Quản lý trạng thái, Ban/Unban tài khoản.
- [x] **SmartZone Loyalty**: Hệ thống tích điểm, phân hạng Tier (Đồng -> Kim Cương).
- [ ] **Notification History**: Xem lịch sử thông báo đã gửi cho từng User để hỗ trợ khiếu nại.
- [ ] **Tier Threshold UI**: Giao diện cấu hình ngưỡng điểm lên hạng linh hoạt.
- [ ] **Customer Segmentation**: Phân nhóm khách hàng (VIP, Newbie) để chăm sóc.

### 4. CMS & Vận hành (MỚI)
- [x] **Order Management**: Xử lý trạng thái đơn hàng và Duyệt hoàn tiền.
- [x] **Product & Inventory**: Quản lý kho, biến thể, soft-delete.
- [ ] **Storefront CMS**: Chỉnh sửa Hero Section, Banner, Feature Cards trang chủ từ Admin.
- [ ] **Review Moderation**: Gallery duyệt/ẩn hình ảnh và nội dung đánh giá từ khách hàng.
- [ ] **Stock Change Log**: Nhật ký chi tiết lịch sử nhập/xuất kho.
- [ ] **Flash Sale Manager**: Cấu hình sự kiện giảm giá chớp nhoáng.

### 5. Engagement & Secure Channels
- [x] **Review & Rating**: Quản lý toàn bộ đánh giá của khách hàng.
- [ ] **Q&A Pro Center**: Phản hồi câu hỏi sản phẩm kèm mẫu trả lời nhanh.
- [ ] **System Health Monitor**: Xem log lỗi hệ thống và trạng thái server (dựa trên Debug specs).
- [ ] **Broadcast Notification**: Gửi thông báo/email khuyến mãi cá nhân hóa.

### 6. Hệ thống & Bảo mật (Core System)
- [x] **RBAC Security**: Phân quyền Admin/User.
- [x] **Data Integrity**: Optimistic locking cho toàn bộ giao dịch tài chính/kho.
- [ ] **Audit Logs**: Nhật ký hành động Admin (Ai đã duyệt tiền? Ai đã ban user?).
- [ ] **Maintenance Mode**: Bật/tắt chế độ bảo trì toàn hệ thống.

---

## 📅 LỊCH TRÌNH TIẾP THEO (NEXT STEPS)
1. **Giai đoạn A**: Hoàn thiện Q&A Center với tính năng Quick-Reply.
2. **Giai đoạn B**: Triển khai Audit Logs bảo mật.
3. **Giai đoạn C**: Xây dựng UI cấu hình chính sách Loyalty.

---
*Cập nhật lần cuối: 20/04/2026 - SmartZone Dev Team*
