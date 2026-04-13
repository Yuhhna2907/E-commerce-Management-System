-- =====================================================
-- DEBUG: Kiểm tra lỗi 404 ảnh không tải được
-- =====================================================

-- Bước 1: Kiểm tra image_url trong database
SELECT id, name, image_url FROM products WHERE id = 21;

-- Bước 2: Kiểm tra tất cả sản phẩm có ảnh
SELECT id, name, image_url FROM products WHERE image_url IS NOT NULL AND image_url != '';

-- =====================================================
-- PHÂN TÍCH LỖI 404:
--
-- Lỗi 404 có thể do:
-- 1. Đường dẫn trong database không đúng
-- 2. Tên file không khớp (chữ hoa/thường, khoảng trắng)
-- 3. File không đúng vị trí
-- 4. Server chưa restart sau khi thêm file
--
-- GIẢI PHÁP:
--
-- Nếu image_url = '/images/products/iphone-15-pro.jpg'
-- → File phải ở: src/main/resources/static/images/products/iphone-15-pro.jpg
-- → URL truy cập: http://localhost:8080/images/products/iphone-15-pro.jpg
--
-- Nếu image_url = 'iphone-15-pro.jpg' (không có /)
-- → SAI! Phải có / ở đầu
-- → Sửa: UPDATE products SET image_url = '/images/products/iphone-15-pro.jpg' WHERE id = 21;
--
-- Nếu image_url = '/static/images/products/iphone-15-pro.jpg'
-- → SAI! Không cần /static
-- → Sửa: UPDATE products SET image_url = '/images/products/iphone-15-pro.jpg' WHERE id = 21;
-- =====================================================
