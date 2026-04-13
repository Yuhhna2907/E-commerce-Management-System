# 🚀 HƯỚNG DẪN NHANH: Thêm Ảnh iPhone 15 Pro

## ⚡ 3 Bước Đơn Giản

### Bước 1: Chạy SQL Script

```bash
# Mở MySQL
mysql -u root -p smartphone_management

# Chạy lệnh này trong MySQL
source update-iphone-image-simple.sql
```

**Hoặc copy-paste trực tiếp:**

```sql
UPDATE products 
SET imageUrl = 'https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg',
    updatedAt = NOW()
WHERE name LIKE '%iPhone 15 Pro%';
```

### Bước 2: Khởi Động Lại Server

```bash
cd E-commerce-Management-System
./gradlew bootRun
```

### Bước 3: Kiểm Tra Trang Chủ

Mở trình duyệt: `http://localhost:8080/user/products`

---

## ✅ Kết Quả Mong Đợi

Bạn sẽ thấy:
- ✅ Ảnh iPhone 15 Pro hiển thị rõ nét
- ✅ Hiệu ứng hover mượt mà (scale + shadow)
- ✅ Responsive trên mọi thiết bị
- ✅ Glassmorphism design đẹp mắt

---

## 🎨 Hiệu Ứng Đã Có Sẵn

Template `list.html` đã có sẵn:
- ✅ Lazy loading
- ✅ Fallback image khi lỗi
- ✅ Drop shadow effect
- ✅ Hover animation (scale 1.12x + translateY -8px)
- ✅ Responsive design (200px mobile → 280px desktop)
- ✅ Alt text cho accessibility

---

## 🔍 Nếu Chưa Có Sản Phẩm iPhone 15 Pro

Chạy lệnh này để thêm sản phẩm mới:

```sql
-- Kiểm tra category_id trước
SELECT id, name FROM categories;

-- Thêm sản phẩm (thay category_id = 1 bằng ID thực tế)
INSERT INTO products (name, brand, price, stock, sold, active, imageUrl, averageRating, totalReviews, description, createdAt, updatedAt, category_id)
VALUES (
    'iPhone 15 Pro 256GB',
    'Apple',
    27990000,
    50,
    15,
    true,
    'https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg',
    4.8,
    24,
    'iPhone 15 Pro với chip A17 Pro mạnh mẽ, camera 48MP, màn hình Dynamic Island, khung titan cao cấp',
    NOW(),
    NOW(),
    1
);
```

---

## 🐛 Troubleshooting

### Ảnh không hiển thị?

1. Kiểm tra database:
```sql
SELECT id, name, imageUrl FROM products WHERE name LIKE '%iPhone%';
```

2. Kiểm tra console browser (F12) xem có lỗi không

3. Thử URL ảnh trực tiếp trong trình duyệt

### Ảnh bị méo?

CSS đã có `object-fit: contain` để giữ tỷ lệ, không cần sửa gì!

---

## 📸 Muốn Dùng Ảnh Khác?

Thay URL trong SQL:

```sql
UPDATE products 
SET imageUrl = 'URL_ANH_MOI_CUA_BAN'
WHERE name LIKE '%iPhone 15 Pro%';
```

**Gợi ý URL ảnh iPhone 15 Pro:**
- Thế Giới Di Động: `https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg`
- FPT Shop: `https://cdn.fptshop.com.vn/Uploads/Originals/2023/9/13/638299848616954397_iphone-15-pro-max-blue-1.jpg`
- Hoặc dùng ảnh bạn đã gửi trong chat!

---

## 🎉 Xong!

Chỉ cần 3 bước đơn giản là ảnh iPhone 15 Pro sẽ hiển thị đẹp mắt trên trang chủ với đầy đủ hiệu ứng glassmorphism! 🚀
