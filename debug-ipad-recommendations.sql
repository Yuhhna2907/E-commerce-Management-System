-- =====================================================
-- DEBUG IPAD PRO M4 RECOMMENDATIONS
-- =====================================================
-- Script để debug tại sao iPad Pro M4 không có recommendations

-- =====================================================
-- 1. TÌM IPAD PRO M4
-- =====================================================
SELECT 'iPad Pro M4 Product' as info, id, name, price, stock, active
FROM products 
WHERE LOWER(name) LIKE '%ipad%pro%m4%' 
   OR LOWER(name) LIKE '%ipad%m4%'
   OR LOWER(name) LIKE '%pro%m4%'
ORDER BY id;

-- Lưu product_id để dùng cho các query sau
SET @ipad_id = (SELECT id FROM products WHERE LOWER(name) LIKE '%ipad%pro%m4%' OR LOWER(name) LIKE '%ipad%m4%' LIMIT 1);

-- =====================================================
-- 2. CHECK ORDERS CHỨA IPAD PRO M4
-- =====================================================
SELECT 'Orders containing iPad Pro M4' as info, 
       o.id as order_id, 
       o.status, 
       o.created_at,
       u.username,
       oi.quantity,
       oi.unit_price
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
JOIN users u ON o.user_id = u.id
WHERE oi.product_id = @ipad_id
ORDER BY o.created_at DESC;

-- =====================================================
-- 3. CHECK DELIVERED ORDERS CHỨA IPAD PRO M4
-- =====================================================
SELECT 'DELIVERED Orders with iPad Pro M4' as info, 
       COUNT(*) as total_delivered_orders
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
WHERE oi.product_id = @ipad_id
AND o.status = 'DELIVERED';

-- =====================================================
-- 4. CHECK CO-PURCHASE PATTERNS (DELIVERED ONLY)
-- =====================================================
SELECT 'Co-purchase patterns (DELIVERED)' as info,
       oi2.product_id as co_purchased_product_id,
       p2.name as co_purchased_product_name,
       COUNT(DISTINCT oi2.order_id) as co_purchase_count
FROM order_items oi1 
JOIN order_items oi2 ON oi1.order_id = oi2.order_id 
JOIN products p2 ON oi2.product_id = p2.id
JOIN orders o ON oi1.order_id = o.id
WHERE oi1.product_id = @ipad_id
AND oi2.product_id != @ipad_id
AND o.status = 'DELIVERED'
GROUP BY oi2.product_id, p2.name
ORDER BY co_purchase_count DESC;

-- =====================================================
-- 5. CHECK EXISTING RECOMMENDATIONS
-- =====================================================
SELECT 'Existing recommendations for iPad Pro M4' as info,
       pr.recommended_product_id,
       p.name as recommended_product_name,
       pr.co_purchase_count,
       pr.co_purchase_frequency,
       pr.last_updated
FROM product_recommendations pr
JOIN products p ON pr.recommended_product_id = p.id
WHERE pr.product_id = @ipad_id
ORDER BY pr.co_purchase_frequency DESC;

-- =====================================================
-- 6. CHECK THRESHOLDS
-- =====================================================
SELECT 'Threshold Analysis' as info,
       @ipad_id as ipad_product_id,
       (SELECT COUNT(DISTINCT oi.order_id) 
        FROM order_items oi 
        JOIN orders o ON oi.order_id = o.id 
        WHERE oi.product_id = @ipad_id AND o.status = 'DELIVERED') as total_delivered_orders,
       10 as min_co_purchase_count_required,
       0.05 as min_frequency_required,
       'Need at least 10 co-purchases AND 5% frequency' as note;

-- =====================================================
-- 7. MANUAL RECOMMENDATIONS (LOWER THRESHOLD)
-- =====================================================
-- Xóa recommendations cũ
DELETE FROM product_recommendations WHERE product_id = @ipad_id;

-- Tạo recommendations mới với threshold thấp hơn (chỉ cần 1 co-purchase)
INSERT INTO product_recommendations (product_id, recommended_product_id, co_purchase_count, co_purchase_frequency, last_updated)
SELECT 
    @ipad_id as product_id,
    oi2.product_id as recommended_product_id,
    COUNT(DISTINCT oi2.order_id) as co_purchase_count,
    COUNT(DISTINCT oi2.order_id) / (
        SELECT COUNT(DISTINCT oi.order_id) 
        FROM order_items oi 
        JOIN orders o ON oi.order_id = o.id 
        WHERE oi.product_id = @ipad_id AND o.status = 'DELIVERED'
    ) as co_purchase_frequency,
    NOW() as last_updated
FROM order_items oi1 
JOIN order_items oi2 ON oi1.order_id = oi2.order_id 
JOIN orders o ON oi1.order_id = o.id
JOIN products p2 ON oi2.product_id = p2.id
WHERE oi1.product_id = @ipad_id
AND oi2.product_id != @ipad_id
AND o.status = 'DELIVERED'
AND p2.active = true
AND p2.stock > 0
GROUP BY oi2.product_id
HAVING co_purchase_count >= 1  -- Chỉ cần 1 lần mua chung
ORDER BY co_purchase_count DESC, co_purchase_frequency DESC
LIMIT 6;

-- =====================================================
-- 8. VERIFY NEW RECOMMENDATIONS
-- =====================================================
SELECT 'NEW Recommendations for iPad Pro M4' as info,
       pr.recommended_product_id,
       p.name as recommended_product_name,
       pr.co_purchase_count,
       ROUND(pr.co_purchase_frequency * 100, 2) as frequency_percent,
       pr.last_updated
FROM product_recommendations pr
JOIN products p ON pr.recommended_product_id = p.id
WHERE pr.product_id = @ipad_id
ORDER BY pr.co_purchase_frequency DESC;

-- =====================================================
-- 9. API TEST QUERY
-- =====================================================
SELECT 'API Test - Products for Recommendation' as info,
       p.id,
       p.name,
       p.price,
       p.image_url,
       p.stock,
       p.active
FROM product_recommendations pr
JOIN products p ON pr.recommended_product_id = p.id
WHERE pr.product_id = @ipad_id
AND p.active = true
AND p.stock > 0
ORDER BY pr.co_purchase_frequency DESC;

-- =====================================================
-- SUMMARY
-- =====================================================
SELECT 'SUMMARY' as info,
       'iPad Pro M4 ID' as field,
       @ipad_id as value
UNION ALL
SELECT 'SUMMARY' as info,
       'Total DELIVERED Orders' as field,
       (SELECT COUNT(DISTINCT oi.order_id) 
        FROM order_items oi 
        JOIN orders o ON oi.order_id = o.id 
        WHERE oi.product_id = @ipad_id AND o.status = 'DELIVERED') as value
UNION ALL
SELECT 'SUMMARY' as info,
       'Recommendations Created' as field,
       (SELECT COUNT(*) FROM product_recommendations WHERE product_id = @ipad_id) as value;