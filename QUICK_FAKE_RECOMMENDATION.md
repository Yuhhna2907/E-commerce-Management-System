# Quick Guide: Fake Recommendation Data

## 🎯 Vấn Đề

Bạn chỉ có 1 khách hàng thực → Không đủ data để tạo co-purchase patterns → Recommendation không hiển thị.

## ⚡ Giải Pháp Nhanh

Fake thêm 5 orders với status `DELIVERED` để tạo patterns.

## 🚀 Cách Làm (5 phút)

### Bước 1: Xem sản phẩm có sẵn
```sql
SELECT id, name, price FROM products WHERE active = true ORDER BY id LIMIT 10;
```

### Bước 2: Sửa file `simple-fake-orders.sql`
Mở file và thay đổi các ID này:
```sql
SET @product1_id = 1;  -- ID sản phẩm chính (muốn test)
SET @product2_id = 2;  -- ID sản phẩm phụ 1  
SET @product3_id = 3;  -- ID sản phẩm phụ 2
SET @product4_id = 4;  -- ID sản phẩm phụ 3
SET @product5_id = 5;  -- ID sản phẩm phụ 4
```

### Bước 3: Chạy script
```bash
mysql -u root -p your_database < simple-fake-orders.sql
```

### Bước 4: Restart app
```bash
./gradlew bootRun
```

### Bước 5: Test
Mở: `http://localhost:8080/user/products/{product1_id}`

## 📊 Kết Quả Mong Đợi

### Database sẽ có:
- ✅ 5 fake users (ID: 991-995)
- ✅ 5 fake orders với status `DELIVERED` 
- ✅ Co-purchase patterns:
  - Product1 + Product2: 2 lần
  - Product1 + Product3: 2 lần  
  - Product1 + Product4: 2 lần
  - Product1 + Product5: 1 lần

### UI sẽ hiển thị:
```
Product Details
Reviews
↓
┌─────────────────────────────────────┐
│ Khách Hàng Cũng Mua                │
│ [Product2] [Product3] [Product4]    │
│    ←                            →   │
└─────────────────────────────────────┘
```

## 🔍 Debug Nhanh

### Check co-purchase patterns:
```sql
SELECT oi2.product_id, COUNT(*) as frequency
FROM order_items oi1 
JOIN order_items oi2 ON oi1.order_id = oi2.order_id 
JOIN orders o ON oi1.order_id = o.id
WHERE oi1.product_id = 1  -- Thay 1 bằng product1_id
AND oi2.product_id != 1 
AND o.status = 'DELIVERED'
GROUP BY oi2.product_id;
```

### Check API:
```
GET http://localhost:8080/api/products/1/recommendations
```

### Check console (F12):
```
"Recommendation carousel initialized for product: 1"
```

## 💡 Tips

1. **Chọn sản phẩm phổ biến**: Dùng sản phẩm có nhiều views làm product1
2. **Realistic data**: Chọn sản phẩm cùng category làm recommendations
3. **Test multiple**: Thử với nhiều sản phẩm khác nhau

## 🎉 Thành Công!

Sau khi chạy script, recommendation carousel sẽ hiển thị ngay lập tức với data fake realistic!