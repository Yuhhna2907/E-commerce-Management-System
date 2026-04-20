-- =====================================================
-- QUERY ĐỢN GIẢN: SẢN PHẨM XUẤT HIỆN ÍT NHẤT 2 LẦN TRONG ĐỢN HÀNG DELIVERED
-- =====================================================

-- 1. Query cơ bản nhất
SELECT 
    p.id,
    p.name,
    COUNT(DISTINCT o.id) as delivered_orders
FROM products p
JOIN order_items oi ON p.id = oi.product_id
JOIN orders o ON oi.order_id = o.id
WHERE o.status = 'DELIVERED'
GROUP BY p.id, p.name
HAVING COUNT(DISTINCT o.id) >= 2
ORDER BY delivered_orders DESC;

-- 2. Với thêm thông tin giá và stock
SELECT 
    p.id,
    p.name,
    p.price,
    p.stock,
    COUNT(DISTINCT o.id) as delivered_orders,
    SUM(oi.quantity) as total_sold
FROM products p
JOIN order_items oi ON p.id = oi.product_id
JOIN orders o ON oi.order_id = o.id
WHERE o.status = 'DELIVERED'
AND p.active = true
GROUP BY p.id, p.name, p.price, p.stock
HAVING COUNT(DISTINCT o.id) >= 2
ORDER BY delivered_orders DESC;

-- 3. Chỉ lấy top 10
SELECT 
    p.id,
    p.name,
    COUNT(DISTINCT o.id) as delivered_orders
FROM products p
JOIN order_items oi ON p.id = oi.product_id
JOIN orders o ON oi.order_id = o.id
WHERE o.status = 'DELIVERED'
AND p.active = true
GROUP BY p.id, p.name
HAVING COUNT(DISTINCT o.id) >= 2
ORDER BY delivered_orders DESC
LIMIT 10;