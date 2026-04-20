-- =====================================================
-- FAKE RECOMMENDATION DATA - SỬ DỤNG SẢN PHẨM CÓ SẴN
-- =====================================================
-- Script này sẽ tạo fake orders dựa trên sản phẩm đã có trong database
-- Không cần tạo sản phẩm mới, chỉ tạo orders và users fake

-- =====================================================
-- 1. TẠO FAKE USERS (nếu chưa có)
-- =====================================================

INSERT IGNORE INTO users (id, username, email, password, full_name, phone, address, role, active, created_at, updated_at)
VALUES 
(901, 'fakeuser1', 'fake1@test.com', '$2a$10$dummy.hash.for.testing', 'Fake User 1', '0901111111', '123 Fake St, HCM', 'USER', true, NOW(), NOW()),
(902, 'fakeuser2', 'fake2@test.com', '$2a$10$dummy.hash.for.testing', 'Fake User 2', '0901111112', '456 Fake Ave, HN', 'USER', true, NOW(), NOW()),
(903, 'fakeuser3', 'fake3@test.com', '$2a$10$dummy.hash.for.testing', 'Fake User 3', '0901111113', '789 Fake Rd, DN', 'USER', true, NOW(), NOW()),
(904, 'fakeuser4', 'fake4@test.com', '$2a$10$dummy.hash.for.testing', 'Fake User 4', '0901111114', '321 Fake Ln, CT', 'USER', true, NOW(), NOW()),
(905, 'fakeuser5', 'fake5@test.com', '$2a$10$dummy.hash.for.testing', 'Fake User 5', '0901111115', '654 Fake Blvd, HP', 'USER', true, NOW(), NOW());

-- =====================================================
-- 2. LẤY SẢN PHẨM CÓ SẴN (giả sử có ít nhất 5 sản phẩm)
-- =====================================================
-- Sẽ dùng 5 sản phẩm đầu tiên trong database
-- Bạn có thể thay đổi ID theo sản phẩm thực tế

-- =====================================================
-- 3. TẠO FAKE ORDERS VỚI STATUS = DELIVERED
-- =====================================================

-- Fake Order 1: Sản phẩm 1 + Sản phẩm 2
INSERT IGNORE INTO orders (id, user_id, total_amount, status, payment_status, shipping_address, created_at, updated_at)
VALUES (801, 901, 5000000, 'DELIVERED', 'COMPLETED', '123 Fake St, HCM', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

-- Fake Order 2: Sản phẩm 1 + Sản phẩm 3 + Sản phẩm 4
INSERT IGNORE INTO orders (id, user_id, total_amount, status, payment_status, shipping_address, created_at, updated_at)
VALUES (802, 902, 7500000, 'DELIVERED', 'COMPLETED', '456 Fake Ave, HN', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY));

-- Fake Order 3: Sản phẩm 1 + Sản phẩm 2 + Sản phẩm 5
INSERT IGNORE INTO orders (id, user_id, total_amount, status, payment_status, shipping_address, created_at, updated_at)
VALUES (803, 903, 6000000, 'DELIVERED', 'COMPLETED', '789 Fake Rd, DN', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY));

-- Fake Order 4: Sản phẩm 1 + Sản phẩm 4
INSERT IGNORE INTO orders (id, user_id, total_amount, status, payment_status, shipping_address, created_at, updated_at)
VALUES (804, 904, 4500000, 'DELIVERED', 'COMPLETED', '321 Fake Ln, CT', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY));

-- Fake Order 5: Sản phẩm 1 + Sản phẩm 3
INSERT IGNORE INTO orders (id, user_id, total_amount, status, payment_status, shipping_address, created_at, updated_at)
VALUES (805, 905, 5500000, 'DELIVERED', 'COMPLETED', '654 Fake Blvd, HP', DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY));

-- =====================================================
-- 4. TẠO ORDER ITEMS (SỬ DỤNG SẢN PHẨM CÓ SẴN)
-- =====================================================
-- QUAN TRỌNG: Thay đổi product_id theo sản phẩm thực tế trong database của bạn!

-- Order 801: Product 1 + Product 2
INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 701, 801, p1.id, 1, p1.price, p1.price FROM products p1 WHERE p1.id = (SELECT MIN(id) FROM products WHERE active = true) LIMIT 1;

INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 702, 801, p2.id, 1, p2.price, p2.price FROM products p2 WHERE p2.id = (SELECT MIN(id) FROM products WHERE active = true AND id > (SELECT MIN(id) FROM products WHERE active = true)) LIMIT 1;

-- Order 802: Product 1 + Product 3 + Product 4
INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 703, 802, p1.id, 1, p1.price, p1.price FROM products p1 WHERE p1.id = (SELECT MIN(id) FROM products WHERE active = true) LIMIT 1;

INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 704, 802, p3.id, 1, p3.price, p3.price FROM products p3 WHERE p3.id = (SELECT id FROM products WHERE active = true ORDER BY id LIMIT 2,1) LIMIT 1;

INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 705, 802, p4.id, 1, p4.price, p4.price FROM products p4 WHERE p4.id = (SELECT id FROM products WHERE active = true ORDER BY id LIMIT 3,1) LIMIT 1;

-- Order 803: Product 1 + Product 2 + Product 5
INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 706, 803, p1.id, 1, p1.price, p1.price FROM products p1 WHERE p1.id = (SELECT MIN(id) FROM products WHERE active = true) LIMIT 1;

INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 707, 803, p2.id, 1, p2.price, p2.price FROM products p2 WHERE p2.id = (SELECT MIN(id) FROM products WHERE active = true AND id > (SELECT MIN(id) FROM products WHERE active = true)) LIMIT 1;

INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 708, 803, p5.id, 1, p5.price, p5.price FROM products p5 WHERE p5.id = (SELECT id FROM products WHERE active = true ORDER BY id LIMIT 4,1) LIMIT 1;

-- Order 804: Product 1 + Product 4
INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 709, 804, p1.id, 1, p1.price, p1.price FROM products p1 WHERE p1.id = (SELECT MIN(id) FROM products WHERE active = true) LIMIT 1;

INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 710, 804, p4.id, 1, p4.price, p4.price FROM products p4 WHERE p4.id = (SELECT id FROM products WHERE active = true ORDER BY id LIMIT 3,1) LIMIT 1;

-- Order 805: Product 1 + Product 3
INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 711, 805, p1.id, 1, p1.price, p1.price FROM products p1 WHERE p1.id = (SELECT MIN(id) FROM products WHERE active = true) LIMIT 1;

INSERT IGNORE INTO order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 712, 805, p3.id, 1, p3.price, p3.price FROM products p3 WHERE p3.id = (SELECT id FROM products WHERE active = true ORDER BY id LIMIT 2,1) LIMIT 1;

-- =====================================================
-- 5. TẠO RECOMMENDATIONS CHO SẢN PHẨM ĐẦU TIÊN
-- =====================================================

-- Xóa recommendations cũ cho sản phẩm đầu tiên
DELETE FROM product_recommendations WHERE product_id = (SELECT MIN(id) FROM products WHERE active = true);

-- Tạo recommendations mới dựa trên co-purchase patterns
INSERT INTO product_recommendations (product_id, recommended_product_id, score, reason, last_updated)
SELECT 
    (SELECT MIN(id) FROM products WHERE active = true) as product_id,
    p2.id as recommended_product_id,
    0.8 as score,
    'Frequently bought together' as reason,
    NOW() as last_updated
FROM products p2 
WHERE p2.id = (SELECT MIN(id) FROM products WHERE active = true AND id > (SELECT MIN(id) FROM products WHERE active = true))
LIMIT 1;

INSERT INTO product_recommendations (product_id, recommended_product_id, score, reason, last_updated)
SELECT 
    (SELECT MIN(id) FROM products WHERE active = true) as product_id,
    p3.id as recommended_product_id,
    0.6 as score,
    'Frequently bought together' as reason,
    NOW() as last_updated
FROM products p3 
WHERE p3.id = (SELECT id FROM products WHERE active = true ORDER BY id LIMIT 2,1)
LIMIT 1;

INSERT INTO product_recommendations (product_id, recommended_product_id, score, reason, last_updated)
SELECT 
    (SELECT MIN(id) FROM products WHERE active = true) as product_id,
    p4.id as recommended_product_id,
    0.4 as score,
    'Frequently bought together' as reason,
    NOW() as last_updated
FROM products p4 
WHERE p4.id = (SELECT id FROM products WHERE active = true ORDER BY id LIMIT 3,1)
LIMIT 1;

-- =====================================================
-- 6. VERIFICATION QUERIES
-- =====================================================

-- Check sản phẩm có sẵn
SELECT 'Available Products' as info, id, name FROM products WHERE active = true ORDER BY id LIMIT 5;

-- Check fake orders đã tạo
SELECT 'Fake Orders' as info, COUNT(*) as count FROM orders WHERE status = 'DELIVERED' AND id BETWEEN 801 AND 805;

-- Check co-purchase patterns cho sản phẩm đầu tiên
SELECT 'Co-purchase Patterns' as info, 
       oi2.product_id, 
       p.name, 
       COUNT(DISTINCT oi2.order_id) as frequency
FROM order_items oi1 
JOIN order_items oi2 ON oi1.order_id = oi2.order_id 
JOIN products p ON oi2.product_id = p.id
JOIN orders o ON oi1.order_id = o.id
WHERE oi1.product_id = (SELECT MIN(id) FROM products WHERE active = true)
AND oi2.product_id != (SELECT MIN(id) FROM products WHERE active = true)
AND o.status = 'DELIVERED'
GROUP BY oi2.product_id, p.name
ORDER BY frequency DESC;

-- Check recommendations đã tạo
SELECT 'Recommendations Created' as info, 
       pr.recommended_product_id, 
       p.name, 
       pr.score
FROM product_recommendations pr
JOIN products p ON pr.recommended_product_id = p.id  
WHERE pr.product_id = (SELECT MIN(id) FROM products WHERE active = true)
ORDER BY pr.score DESC;

-- =====================================================
-- HƯỚNG DẪN SỬ DỤNG
-- =====================================================
-- 1. Chạy script: mysql -u root -p your_database < fake-recommendation-data.sql
-- 2. Restart application
-- 3. Lấy ID sản phẩm đầu tiên: SELECT MIN(id) FROM products WHERE active = true;
-- 4. Mở: http://localhost:8080/user/products/{ID_SAN_PHAM_DAU_TIEN}
-- 5. Scroll xuống → Sẽ thấy "Khách Hàng Cũng Mua"
-- =====================================================