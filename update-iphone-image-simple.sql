-- =====================================================
-- SCRIPT ĐƠN GIẢN: Cập nhật ảnh iPhone 15 Pro
-- Chạy script này để thêm ảnh cho sản phẩm iPhone 15 Pro
-- =====================================================

-- Cách 1: Nếu đã có sản phẩm iPhone 15 Pro trong database
UPDATE products 
SET imageUrl = 'https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg',
    updatedAt = NOW()
WHERE name LIKE '%iPhone 15 Pro%';

-- Cách 2: Nếu chưa có, thêm sản phẩm mới (chỉ chạy 1 trong 2 cách)
-- Lưu ý: Thay category_id = 1 bằng ID category thực tế của bạn
/*
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
    1  -- Thay bằng category_id thực tế
);
*/

-- Kiểm tra kết quả
SELECT id, name, brand, imageUrl, stock FROM products WHERE name LIKE '%iPhone%';
