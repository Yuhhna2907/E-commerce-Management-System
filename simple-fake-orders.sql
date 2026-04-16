-- =====================================================
-- SIMPLE FAKE ORDERS - CUSTOM CHO DATABASE CỦA BẠN
-- =====================================================
-- Thay đổi các ID sản phẩm theo database thực tế của bạn

-- =====================================================
-- BƯỚC 1: TÌM SẢN PHẨM CÓ SẴN
-- =====================================================
-- Chạy query này trước để xem sản phẩm có sẵn:
-- SELECT id, name, price FROM products WHERE active = true ORDER BY id LIMIT 10;

-- =====================================================
-- BƯỚC 2: THAY ĐỔI CÁC ID DƯỚI ĐÂY
-- =====================================================
-- Thay đổi các số này theo ID sản phẩm thực tế:
SET @product1_id = 1;  -- Thay bằng ID sản phẩm chính (muốn test recommendation)
SET @product2_id = 2;  -- Thay bằng ID sản phẩm phụ 1
SET @product3_id = 3;  -- Thay bằng ID sản phẩm phụ 2
SET @product4_id = 4;  -- Thay bằng ID sản phẩm phụ 3
SET @product5_id = 5;  -- Thay bằng ID sản phẩm phụ 4

-- =====================================================
-- BƯỚC 3: TẠO FAKE USERS
-- =====================================================
INSERT IGNORE INTO users (id, username, email, password, full_name, phone, address, role, active, created_at, updated_at)
VALUES 
(991, 'fake1', 'fake1@test.com', '$2a$10$dummy', 'Fake 1', '0901111111', 'Fake Address 1', 'USER', true, NOW(), NOW()),
(992, 'fake2', 'fake2@test.com', '$2a$10$dummy', 'Fake 2', '0901111112', 'Fake Address 2', 'USER', true, NOW(), NOW()),
(993, 'fake3', 'fake3@test.com', '$2a$10$dummy', 'Fake 3', '0901111113', 'Fake Address 3', 'USER', true, NOW(), NOW()),
(994, 'fake4', 'fake4@test.com', '$2a$10$dummy', 'Fake 4', '0901111114', 'Fake Address 4', 'USER', true, NOW(), NOW()),
(995, 'fake5', 'fake5@test.com', '$2a$10$dummy', 'Fake 5', '0901111115', 'Fake Address 5', 'USER', true, NOW(), NOW());

-- =====================================================
-- BƯỚC 4: TẠO FAKE ORDERS (STATUS = DELIVERED)
-- =====================================================

-- Order 1: Product1 + Product2
INSERT IGNORE INTO orders (id, user_id, total_amount, status, payment_status, shipping_address, created_at, updated_at)
VALUES (991, 991, 1000000, 'DELIVERED', 'COMPLETED', 'Fake Address 1', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
VALUES 
(991, 991, @product1_id, 1, 500000, 500000),
(992, 991, @product2_id, 1, 500000, 500000);

-- Order 2: Product1 + Product3 + Product4
INSERT IGNORE INTO orders (id, user_id, total_amount, status, payment_status, shipping_address, created_at, updated_at)
VALUES (992, 992, 1500000, 'DELIVERED', 'COMPLETED', 'Fake Address 2', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY));

INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
VALUES 
(993, 992, @product1_id, 1, 500000, 500000),
(994, 992, @product3_id, 1, 500000, 500000),
(995, 992, @product4_id, 1, 500000, 500000);

-- Order 3: Product1 + Product2 + Product5
INSERT IGNORE INTO orders (id, user_id, total_amount, status, payment_status, shipping_address, created_at, updated_at)
VALUES (993, 993, 1500000, 'DELIVERED', 'COMPLETED', 'Fake Address 3', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY));

INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
VALUES 
(996, 993, @product1_id, 1, 500000, 500000),
(997, 993, @product2_id, 1, 500000, 500000),
(998, 993, @product5_id, 1, 500000, 500000);

-- Order 4: Product1 + Product4
INSERT IGNORE INTO orders (id, user_id, total_amount, status, payment_status, shipping_address, created_at, updated_at)
VALUES (994, 994, 1000000, 'DELIVERED', 'COMPLETED', 'Fake Address 4', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY));

INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
VALUES 
(999, 994, @product1_id, 1, 500000, 500000),
(1000, 994, @product4_id, 1, 500000, 500000);

-- Order 5: Product1 + Product3
INSERT IGNORE INTO orders (id, user_id, total_amount, status, payment_status, shipping_address, created_at, updated_at)
VALUES (995, 995, 1000000, 'DELIVERED', 'COMPLETED', 'Fake Address 5', DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY));

INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
VALUES 
(1001, 995, @product1_id, 1, 500000, 500000),
(1002, 995, @product3_id, 1, 500000, 500000);

-- =====================================================
-- BƯỚC 5: TẠO RECOMMENDATIONS CHO PRODUCT1
-- =====================================================

-- Xóa recommendations cũ
DELETE FROM product_recommendations WHERE product_id = @product1_id;

-- Tạo recommendations mới
INSERT INTO product_recommendations (product_id, recommended_product_id, score, reason, last_updated)
VALUES 
(@product1_id, @product2_id, 0.8, 'Frequently bought together', NOW()),  -- Xuất hiện trong 2/5 orders
(@product3_id, @product3_id, 0.6, 'Frequently bought together', NOW()),  -- Xuất hiện trong 2/5 orders  
(@product1_id, @product4_id, 0.6, 'Frequently bought together', NOW()),  -- Xuất hiện trong 2/5 orders
(@product1_id, @product5_id, 0.2, 'Frequently bought together', NOW());  -- Xuất hiện trong 1/5 orders

-- =====================================================
-- BƯỚC 6: VERIFICATION
-- =====================================================

-- Check co-purchase patterns
SELECT 'Co-purchase with Product1' as info, 
       oi2.product_id, 
       COUNT(DISTINCT oi2.order_id) as frequency
FROM order_items oi1 
JOIN order_items oi2 ON oi1.order_id = oi2.order_id 
JOIN orders o ON oi1.order_id = o.id
WHERE oi1.product_id = @product1_id
AND oi2.product_id != @product1_id
AND o.status = 'DELIVERED'
GROUP BY oi2.product_id
ORDER BY frequency DESC;

-- Check recommendations
SELECT 'Recommendations for Product1' as info, 
       recommended_product_id, 
       score
FROM product_recommendations 
WHERE product_id = @product1_id
ORDER BY score DESC;

-- =====================================================
-- HƯỚNG DẪN
-- =====================================================
-- 1. Sửa @product1_id, @product2_id, ... theo ID thực tế
-- 2. Chạy: mysql -u root -p your_database < simple-fake-orders.sql  
-- 3. Restart app
-- 4. Mở: http://localhost:8080/user/products/{@product1_id}
-- 5. Scroll xuống → Thấy "Khách Hàng Cũng Mua"
-- =====================================================