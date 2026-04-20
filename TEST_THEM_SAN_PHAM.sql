-- 🧪 TEST SCRIPT - Kiểm tra chức năng thêm sản phẩm
-- Chạy các câu lệnh này trong MySQL để test

-- ✅ 1. Kiểm tra database tồn tại
SELECT DATABASE();
USE smart_phone;

-- ✅ 2. Kiểm tra bảng products
DESCRIBE products;

-- ✅ 3. Kiểm tra danh mục (để chọn khi thêm sản phẩm)
SELECT id, name FROM categories LIMIT 5;

-- ✅ 4. Xem tất cả sản phẩm hiện có
SELECT id, name, brand, price, stock FROM products LIMIT 10;

-- ✅ 5. Xoá sản phẩm test (nếu cần)
-- DELETE FROM products WHERE name LIKE '%iPhone 15 Pro%';

-- ✅ 6. Thêm sản phẩm test để kiểm tra DB connection
INSERT INTO products (name, brand, price, stock, category_id, active, created_at, updated_at)
VALUES ('iPhone 15 Pro Test', 'Apple', 25000000, 10, 1, true, NOW(), NOW());

-- ✅ 7. Kiểm tra sản phẩm vừa thêm
SELECT * FROM products WHERE name = 'iPhone 15 Pro Test';

-- ✅ 8. Xoá sản phẩm test
DELETE FROM products WHERE name = 'iPhone 15 Pro Test';

-- ✅ 9. Kiểm tra nếu có lỗi null
SELECT id, name, brand, price, stock, category_id
FROM products
WHERE stock IS NULL OR price IS NULL OR name IS NULL;

-- ✅ 10. Xem số lượng sản phẩm
SELECT COUNT(*) as total_products FROM products;

-- ✅ 11. Xem active status
SELECT id, name, active FROM products LIMIT 5;

