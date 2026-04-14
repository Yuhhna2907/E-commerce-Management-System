-- =====================================================
-- FIX: Cập nhật ảnh iPhone 15 Pro sau khi xử lý
-- Vấn đề: Ảnh PNG có nền trong suốt, hiển thị không rõ
-- Giải pháp: Xử lý ảnh PNG (crop + thêm nền trắng) hoặc convert sang JPG
-- =====================================================

-- Bước 1: Kiểm tra sản phẩm hiện tại
SELECT id, name, image_url FROM products WHERE id = 21;

-- Bước 2: Cập nhật với ảnh đã xử lý (chọn 1 trong 2)

-- OPTION 1: Nếu bạn xử lý và lưu file PNG
UPDATE products 
SET image_url = '/images/products/iphone-15-pro.png',
    updated_at = NOW()
WHERE id = 21;

-- OPTION 2: Nếu bạn convert sang JPG
-- UPDATE products 
-- SET image_url = '/images/products/iphone-15-pro.jpg',
--     updated_at = NOW()
-- WHERE id = 21;

-- Bước 3: Kiểm tra kết quả
SELECT id, name, image_url FROM products WHERE id = 21;

-- =====================================================
-- HƯỚNG DẪN XỬ LÝ ẢNH:
-- 
-- 1. Dùng Photopea (Online, Miễn phí): https://www.photopea.com/
--    - Mở ảnh "iPhone 15 Pro.png"
--    - Image → Trim (crop transparent)
--    - Image → Canvas Size (thêm padding 50px)
--    - Layer → Flatten Image (thêm nền trắng)
--    - File → Export As → PNG
--    - Lưu thành "iphone-15-pro.png"
--
-- 2. Hoặc dùng Paint (Windows):
--    - Mở ảnh bằng Paint
--    - File → Save As → JPEG
--    - Lưu thành "iphone-15-pro.jpg"
--
-- 3. Lưu file vào:
--    E-commerce-Management-System/src/main/resources/static/images/products/
--
-- 4. Chạy SQL này để cập nhật database
--
-- 5. Khởi động lại server:
--    cd E-commerce-Management-System
--    ./gradlew bootRun
--
-- 6. Kiểm tra:
--    http://localhost:8080/user/products/21
-- =====================================================
