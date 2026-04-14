-- =====================================================
-- FIX: Sửa tất cả đường dẫn ảnh sai trong database
-- Vấn đề: Nhiều sản phẩm có image_url sai format
-- =====================================================

-- Bước 1: Kiểm tra tất cả image_url hiện tại
SELECT id, name, image_url FROM products WHERE image_url IS NOT NULL ORDER BY id;

-- Bước 2: Sửa các đường dẫn sai (thêm /images/products/ nếu thiếu)

-- Sản phẩm có đường dẫn bắt đầu bằng /user/ (SAI)
UPDATE products 
SET image_url = CONCAT('/images/products/', SUBSTRING(image_url, 7)),
    updated_at = NOW()
WHERE image_url LIKE '/user/%';

-- Sản phẩm có đường dẫn không bắt đầu bằng / (SAI)
UPDATE products 
SET image_url = CONCAT('/images/products/', image_url),
    updated_at = NOW()
WHERE image_url NOT LIKE '/%' AND image_url IS NOT NULL AND image_url != '';

-- Bước 3: Kiểm tra kết quả
SELECT id, name, image_url FROM products WHERE image_url IS NOT NULL ORDER BY id;

-- =====================================================
-- LƯU Ý:
-- Sau khi chạy SQL này, TẤT CẢ image_url sẽ có format:
-- /images/products/ten-file.jpg
--
-- Nhưng bạn cần đảm bảo các file ảnh tồn tại trong:
-- src/main/resources/static/images/products/
--
-- Nếu file không tồn tại, vẫn sẽ bị 404!
-- =====================================================

-- Bước 4: Liệt kê các sản phẩm cần thêm ảnh
SELECT id, name, image_url 
FROM products 
WHERE image_url IS NULL OR image_url = '' 
ORDER BY id;
