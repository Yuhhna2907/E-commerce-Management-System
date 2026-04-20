-- =====================================================
-- DEBUG STEP BY STEP - TẠI SAO RECOMMENDATIONS KHÔNG HIỆN
-- =====================================================

-- BƯỚC 1: Kiểm tra co-purchase patterns cho iPad Pro M4 (ID: 36)
SELECT 'STEP 1: Co-purchase patterns for iPad Pro M4' as debug_step;

SELECT 
    oi1.product_id as ipad_id,
    oi2.product_id as co_purchased_product_id,
    p2.name as co_purchased_product_name,
    COUNT(DISTINCT oi1.order_id) as co_purchase_count,
    GROUP_CONCAT(DISTINCT oi1.order_id ORDER BY oi1.order_id) as order_ids
FROM order_items oi1 
JOIN order_items oi2 ON oi1.order_id = oi2.order_id 
JOIN orders o ON oi1.order_id = o.id
JOIN products p2 ON oi2.product_id = p2.id
WHERE oi1.product_id = 36  -- iPad Pro M4
AND oi2.product_id != 36   -- Không tính chính nó
AND o.status = 'DELIVERED'
AND p2.active = true
GROUP BY oi1.product_id, oi2.product_id, p2.name
ORDER BY co_purchase_count DESC;

-- BƯỚC 2: Kiểm tra existing recommendations trong DB
SELECT 'STEP 2: Existing recommendations in product_recommendations table' as debug_step;

SELECT 
    pr.product_id,
    p1.name as product_name,
    pr.recommended_product_id,
    p2.name as recommended_product_name,
    pr.co_purchase_count,
    pr.co_purchase_frequency,
    pr.last_updated
FROM product_recommendations pr
JOIN products p1 ON pr.product_id = p1.id
JOIN products p2 ON pr.recommended_product_id = p2.id
WHERE pr.product_id = 36  -- iPad Pro M4
ORDER BY pr.co_purchase_frequency DESC;

-- BƯỚC 3: Kiểm tra tất cả recommendations trong DB
SELECT 'STEP 3: All recommendations in database' as debug_step;

SELECT 
    COUNT(*) as total_recommendations,
    COUNT(CASE WHEN co_purchase_count >= 2 THEN 1 END) as valid_recommendations_count_2,
    COUNT(CASE WHEN co_purchase_frequency >= 0.01 THEN 1 END) as valid_recommendations_freq_1pct
FROM product_recommendations;

-- BƯỚC 4: Kiểm tra OrderItemRepository query (giống như trong Java code)
SELECT 'STEP 4: OrderItemRepository.findCoPurchasedProducts simulation' as debug_step;

-- Đây là query tương tự như trong OrderItemRepository.findCoPurchasedProducts
SELECT 
    oi2.product_id,
    COUNT(DISTINCT oi1.order_id) as co_purchase_count
FROM order_items oi1
JOIN order_items oi2 ON oi1.order_id = oi2.order_id
JOIN orders o ON oi1.order_id = o.id
WHERE oi1.product_id = 36  -- iPad Pro M4
AND oi2.product_id != 36
AND o.status = 'DELIVERED'
GROUP BY oi2.product_id
HAVING COUNT(DISTINCT oi1.order_id) >= 2  -- Threshold = 2
ORDER BY co_purchase_count DESC;

-- BƯỚC 5: Tính frequency cho iPad Pro M4
SELECT 'STEP 5: Calculate frequency for iPad Pro M4' as debug_step;

SELECT 
    36 as product_id,
    COUNT(DISTINCT o.id) as total_orders_for_ipad,
    'This should be 3 based on your screenshot' as note
FROM order_items oi
JOIN orders o ON oi.order_id = o.id
WHERE oi.product_id = 36
AND o.status = 'DELIVERED';

-- BƯỚC 6: Manual calculation - tạo recommendations như code Java sẽ làm
SELECT 'STEP 6: Manual recommendation calculation' as debug_step;

SELECT 
    36 as product_id,
    oi2.product_id as recommended_product_id,
    p2.name as recommended_product_name,
    COUNT(DISTINCT oi1.order_id) as co_purchase_count,
    COUNT(DISTINCT oi1.order_id) / 3.0 as frequency,  -- 3 là total orders của iPad
    CASE 
        WHEN COUNT(DISTINCT oi1.order_id) >= 2 AND (COUNT(DISTINCT oi1.order_id) / 3.0) >= 0.01 
        THEN 'VALID' 
        ELSE 'INVALID' 
    END as recommendation_status
FROM order_items oi1
JOIN order_items oi2 ON oi1.order_id = oi2.order_id
JOIN orders o ON oi1.order_id = o.id
JOIN products p2 ON oi2.product_id = p2.id
WHERE oi1.product_id = 36  -- iPad Pro M4
AND oi2.product_id != 36
AND o.status = 'DELIVERED'
AND p2.active = true
GROUP BY oi2.product_id, p2.name
ORDER BY co_purchase_count DESC;

-- BƯỚC 7: Kiểm tra products table
SELECT 'STEP 7: Check products status' as debug_step;

SELECT 
    id,
    name,
    active,
    stock,
    CASE 
        WHEN active = 1 AND stock > 0 THEN 'OK'
        WHEN active = 0 THEN 'INACTIVE'
        WHEN stock <= 0 THEN 'OUT_OF_STOCK'
        ELSE 'UNKNOWN'
    END as status
FROM products 
WHERE id IN (36, 23, 38)  -- iPad, Samsung, Acer
ORDER BY id;