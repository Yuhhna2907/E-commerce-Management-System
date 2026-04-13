-- =====================================================
-- FIX: Cập nhật đường dẫn ảnh về dạng số ít (/product/)
-- Chạy script này sau khi thư mục đã được đổi tên thành 'product'
-- =====================================================

UPDATE products 
SET image_url = REPLACE(image_url, '/images/products/', '/images/product/'),
    updated_at = NOW()
WHERE image_url LIKE '%/images/products/%';

-- Sửa các trường hợp không có dấu / ở đầu nếu có
UPDATE products 
SET image_url = CONCAT('/', image_url),
    updated_at = NOW()
WHERE image_url NOT LIKE '/%' AND image_url IS NOT NULL;

-- Kiểm tra kết quả
SELECT id, name, image_url FROM products;
