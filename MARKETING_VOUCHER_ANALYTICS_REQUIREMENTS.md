# 📊 Marketing & Voucher Analytics - Tóm Tắt

## 🎯 CẦN LÀM GÌ?

### **4 KPI Chính (Dashboard)**
1. Tổng voucher (Active/Expired/Inactive)
2. Tổng lượt sử dụng + Tỷ lệ sử dụng
3. Tổng giá trị giảm + Doanh thu tạo ra
4. ROI = (Doanh thu - Chi phí) / Chi phí × 100%

### **Top Voucher**
- Top 10 được dùng nhiều nhất
- Top 10 tạo doanh thu cao nhất
- Top 10 tỷ lệ chuyển đổi cao

### **Phân Tích Theo**
- **Loại**: PERCENTAGE vs FIXED_AMOUNT
- **Category**: Product/Shipping/Bundle/Loyalty/Seasonal/Flash Sale
- **Khách hàng**: Mới vs Thân thiết, Top users
- **Sản phẩm**: Danh mục, Brand
- **Payment**: COD, VNPAY, Bank Transfer
- **Thời gian**: Trends, Peak hours, Vòng đời voucher

### **Cảnh Báo**
- ⚠️ Sắp hết hạn chưa dùng (< 3 ngày)
- ⚠️ Tỷ lệ sử dụng thấp (< 10%)
- ⚠️ ROI âm
- 💡 Gợi ý tối ưu

### **Bảng Chi Tiết**
- Full table với filter/sort/pagination
- Export CSV/Excel/PDF
- Biểu đồ: Pie, Bar, Line, Heatmap

---

## � LÀM THEO THỨ TỰ

### **Phase 1 - MVP (Làm trước)**
1. KPI Dashboard (4 metrics)
2. Top 10 voucher
3. Bảng chi tiết + filter/sort
4. Export CSV
5. Phân tích theo loại

### **Phase 2 - Mở rộng**
6. Phân tích khách hàng
7. Phân tích sản phẩm/danh mục
8. Biểu đồ
9. Cảnh báo & gợi ý
10. Export Excel/PDF

### **Phase 3 - Nâng cao**
11. AI Insights
12. A/B Testing
13. Heatmap

---

## ⚠️ DỮ LIỆU CẦN BỔ SUNG

### **Bảng mới: `coupon_usage_history`**
```sql
- id
- coupon_id
- user_id
- order_id
- discount_amount
- used_at
```

### **Thêm vào Order**
```sql
- coupon_id (nullable)
- discount_amount
- original_total_price
```

---

## 📐 LAYOUT TRANG

```
┌─────────────────────────────────────┐
│ HEADER + FILTER + EXPORT            │
└─────────────────────────────────────┘
┌────┬────┬────┬────┐
│KPI1│KPI2│KPI3│KPI4│
└────┴────┴────┴────┘
┌─────────────────────────────────────┐
│ TABS: Tổng Quan | Top | Theo Loại  │
│       Theo KH | Theo SP | Cảnh Báo  │
└─────────────────────────────────────┘
┌──────────┬──────────┐
│ Chart 1  │ Chart 2  │
└──────────┴──────────┘
┌─────────────────────────────────────┐
│ TABLE + PAGINATION                  │
└─────────────────────────────────────┘
```

---

## 💡 BEST PRACTICES (Shopee/Lazada/Tiki)

- **Flash Sale** - Giảm sốc ngắn hạn
- **Seasonal** - Tết, Black Friday, 11/11
- **Loyalty** - Thưởng VIP
- **Gamification** - Săn voucher, minigame
- **Voucher Bank** - Ví voucher của user
- **Stack Voucher** - Dùng nhiều voucher cùng lúc
