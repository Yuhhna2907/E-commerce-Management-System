-- =====================================================
-- Script: Thêm ảnh sản phẩm iPhone 15 Pro
-- Mục đích: Cập nhật imageUrl cho sản phẩm iPhone 15 Pro
-- Ngày tạo: 2026-04-13
-- =====================================================

-- Bước 1: Kiểm tra sản phẩm iPhone 15 Pro có tồn tại không
SELECT id, name, brand, imageUrl 
FROM products 
WHERE name LIKE '%iPhone 15 Pro%' OR name LIKE '%iPhone%';

-- Bước 2: Cập nhật imageUrl cho iPhone 15 Pro
-- Sử dụng URL ảnh từ CDN hoặc local path
UPDATE products 
SET imageUrl = 'https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg',
    updatedAt = NOW()
WHERE name LIKE '%iPhone 15 Pro%';

-- Bước 3: Nếu chưa có sản phẩm iPhone 15 Pro, thêm mới
-- (Chỉ chạy nếu bước 1 không trả về kết quả)
INSERT INTO products (name, brand, price, stock, sold, active, imageUrl, averageRating, totalReviews, description, createdAt, updatedAt, category_id)
SELECT 
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
    (SELECT id FROM categories WHERE name LIKE '%Smartphone%' OR name LIKE '%Điện thoại%' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE name LIKE '%iPhone 15 Pro%'
);

-- Bước 4: Kiểm tra kết quả
SELECT id, name, brand, price, imageUrl, stock 
FROM products 
WHERE name LIKE '%iPhone 15 Pro%';

-- =====================================================
-- LƯU Ý:
-- 1. Ảnh được lưu từ URL CDN của Thế Giới Di Động
-- 2. Nếu muốn dùng ảnh local, cần:
--    - Lưu ảnh vào: src/main/resources/static/images/products/
--    - Đổi imageUrl thành: '/images/products/iphone-15-pro.jpg'
-- 3. Đảm bảo category_id tồn tại trước khi chạy INSERT
-- =====================================================
