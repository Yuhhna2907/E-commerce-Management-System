-- =====================================================
-- LẤY SẢN PHẨM XUẤT HIỆN ÍT NHẤT 2 LẦN TRONG ĐỢN HÀNG DELIVERED
-- =====================================================

-- Query chính: Lấy sản phẩm xuất hiện >= 2 lần trong đơn hàng DELIVERED
SELECT 
    oi.product_id,
    p.name as product_name,
    p.price,
    p.stock,
    COUNT(DISTINCT o.id) as delivered_order_count,
    SUM(oi.quantity) as total_quantity_sold
FROM order_items oi
JOIN orders o ON oi.order_id = o.id
JOIN products p ON oi.product_id = p.id
WHERE o.status = 'DELIVERED'
AND p.active = true
GROUP BY oi.product_id, p.name, p.price, p.stock
HAVING COUNT(DISTINCT o.id) >= 2  -- Xuất hiện ít nhất 2 lần
ORDER BY delivered_order_count DESC, total_quantity_sold DESC;

-- =====================================================
-- QUERY CHI TIẾT HỚN: Bao gồm thông tin đơn hàng
-- =====================================================

SELECT 
    oi.product_id,
    p.name as product_name,
    p.price,
    p.category_id,
    c.name as category_name,
    COUNT(DISTINCT o.id) as delivered_order_count,
    SUM(oi.quantity) as total_quantity_sold,
    AVG(oi.unit_price) as avg_unit_price,
    MIN(o.created_at) as first_order_date,
    MAX(o.created_at) as last_order_date,
    COUNT(DISTINCT o.user_id) as unique_customers
FROM order_items oi
JOIN orders o ON oi.order_id = o.id
JOIN products p ON oi.product_id = p.id
LEFT JOIN categories c ON p.category_id = c.id
WHERE o.status = 'DELIVERED'
AND p.active = true
GROUP BY oi.product_id, p.name, p.price, p.category_id, c.name
HAVING COUNT(DISTINCT o.id) >= 2  -- Xuất hiện ít nhất 2 lần
ORDER BY delivered_order_count DESC, unique_customers DESC;

-- =====================================================
-- QUERY ĐỂ KIỂM TRA SẢN PHẨM CỤ THỂ (VÍ DỤ: IPAD)
-- =====================================================

-- Kiểm tra iPad Pro M4 cụ thể
SELECT 
    oi.product_id,
    p.name as product_name,
    COUNT(DISTINCT o.id) as delivered_order_count,
    GROUP_CONCAT(DISTINCT o.id ORDER BY o.id) as order_ids,
    GROUP_CONCAT(DISTINCT o.user_id ORDER BY o.user_id) as user_ids
FROM order_items oi
JOIN orders o ON oi.order_id = o.id
JOIN products p ON oi.product_id = p.id
WHERE o.status = 'DELIVERED'
AND p.active = true
AND (LOWER(p.name) LIKE '%ipad%' OR LOWER(p.name) LIKE '%pro%m4%')
GROUP BY oi.product_id, p.name
ORDER BY delivered_order_count DESC;

-- =====================================================
-- QUERY ĐỂ TÌM CO-PURCHASE PATTERNS
-- =====================================================

-- Tìm sản phẩm được mua cùng với những sản phẩm xuất hiện >= 2 lần
WITH popular_products AS (
    SELECT oi.product_id
    FROM order_items oi
    JOIN orders o ON oi.order_id = o.id
    JOIN products p ON oi.product_id = p.id
    WHERE o.status = 'DELIVERED'
    AND p.active = true
    GROUP BY oi.product_id
    HAVING COUNT(DISTINCT o.id) >= 2
)
SELECT 
    pp.product_id as popular_product_id,
    p1.name as popular_product_name,
    oi2.product_id as co_purchased_product_id,
    p2.name as co_purchased_product_name,
    COUNT(DISTINCT oi2.order_id) as co_purchase_count
FROM popular_products pp
JOIN products p1 ON pp.product_id = p1.id
JOIN order_items oi1 ON pp.product_id = oi1.product_id
JOIN order_items oi2 ON oi1.order_id = oi2.order_id
JOIN products p2 ON oi2.product_id = p2.id
JOIN orders o ON oi1.order_id = o.id
WHERE oi2.product_id != pp.product_id  -- Không tính chính nó
AND o.status = 'DELIVERED'
AND p2.active = true
GROUP BY pp.product_id, p1.name, oi2.product_id, p2.name
HAVING COUNT(DISTINCT oi2.order_id) >= 2  -- Co-purchase ít nhất 2 lần
ORDER BY pp.product_id, co_purchase_count DESC;

-- =====================================================
-- QUERY ĐƠN GIẢN NHẤT: CHỈ LẤY ID VÀ TÊN
-- =====================================================

SELECT 
    p.id,
    p.name,
    COUNT(DISTINCT o.id) as order_count
FROM products p
JOIN order_items oi ON p.id = oi.product_id
JOIN orders o ON oi.order_id = o.id
WHERE o.status = 'DELIVERED'
AND p.active = true
GROUP BY p.id, p.name
HAVING COUNT(DISTINCT o.id) >= 2
ORDER BY order_count DESC;

-- =====================================================
-- THỐNG KÊ TỔNG QUAN
-- =====================================================

SELECT 
    'Tổng số sản phẩm active' as metric,
    COUNT(*) as value
FROM products 
WHERE active = true

UNION ALL

SELECT 
    'Sản phẩm có trong đơn hàng DELIVERED' as metric,
    COUNT(DISTINCT oi.product_id) as value
FROM order_items oi
JOIN orders o ON oi.order_id = o.id
JOIN products p ON oi.product_id = p.id
WHERE o.status = 'DELIVERED'
AND p.active = true

UNION ALL

SELECT 
    'Sản phẩm xuất hiện >= 2 lần trong DELIVERED' as metric,
    COUNT(DISTINCT oi.product_id) as value
FROM order_items oi
JOIN orders o ON oi.order_id = o.id
JOIN products p ON oi.product_id = p.id
WHERE o.status = 'DELIVERED'
AND p.active = true
GROUP BY oi.product_id
HAVING COUNT(DISTINCT o.id) >= 2;