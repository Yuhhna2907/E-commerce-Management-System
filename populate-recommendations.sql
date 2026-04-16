-- =====================================================
-- POPULATE PRODUCT_RECOMMENDATIONS TABLE
-- =====================================================
-- Script để tạo dữ liệu cho bảng product_recommendations dựa trên data thực

-- BƯỚC 1: Xóa tất cả recommendations cũ
DELETE FROM product_recommendations;

-- BƯỚC 2: Tạo recommendations cho iPad Pro M4 (ID: 36)
-- Dựa trên screenshot: iPad có 3 đơn hàng DELIVERED

-- Tìm co-purchase patterns cho iPad Pro M4
INSERT INTO product_recommendations (product_id, recommended_product_id, co_purchase_count, co_purchase_frequency, last_updated)
SELECT 
    36 as product_id,  -- iPad Pro M4
    oi2.product_id as recommended_product_id,
    COUNT(DISTINCT oi1.order_id) as co_purchase_count,
    COUNT(DISTINCT oi1.order_id) / 3.0 as co_purchase_frequency,  -- 3 là total orders của iPad
    NOW() as last_updated
FROM order_items oi1
JOIN order_items oi2 ON oi1.order_id = oi2.order_id
JOIN orders o ON oi1.order_id = o.id
JOIN products p2 ON oi2.product_id = p2.id
WHERE oi1.product_id = 36  -- iPad Pro M4
AND oi2.product_id != 36   -- Không tính chính nó
AND o.status = 'DELIVERED'
AND p2.active = true
AND p2.stock > 0
GROUP BY oi2.product_id
HAVING COUNT(DISTINCT oi1.order_id) >= 2  -- Threshold = 2
AND (COUNT(DISTINCT oi1.order_id) / 3.0) >= 0.01  -- Frequency >= 1%
ORDER BY co_purchase_count DESC;

-- BƯỚC 3: Tạo recommendations cho Samsung Galaxy S24 (ID: 23)
-- Dựa trên screenshot: Samsung có 2 đơn hàng DELIVERED

INSERT INTO product_recommendations (product_id, recommended_product_id, co_purchase_count, co_purchase_frequency, last_updated)
SELECT 
    23 as product_id,  -- Samsung Galaxy S24
    oi2.product_id as recommended_product_id,
    COUNT(DISTINCT oi1.order_id) as co_purchase_count,
    COUNT(DISTINCT oi1.order_id) / 2.0 as co_purchase_frequency,  -- 2 là total orders của Samsung
    NOW() as last_updated
FROM order_items oi1
JOIN order_items oi2 ON oi1.order_id = oi2.order_id
JOIN orders o ON oi1.order_id = o.id
JOIN products p2 ON oi2.product_id = p2.id
WHERE oi1.product_id = 23  -- Samsung Galaxy S24
AND oi2.product_id != 23   -- Không tính chính nó
AND o.status = 'DELIVERED'
AND p2.active = true
AND p2.stock > 0
GROUP BY oi2.product_id
HAVING COUNT(DISTINCT oi1.order_id) >= 2  -- Threshold = 2
AND (COUNT(DISTINCT oi1.order_id) / 2.0) >= 0.01  -- Frequency >= 1%
ORDER BY co_purchase_count DESC;

-- BƯỚC 4: Tạo recommendations cho Acer Nitro 5 (ID: 38)
-- Dựa trên screenshot: Acer có 2 đơn hàng DELIVERED

INSERT INTO product_recommendations (product_id, recommended_product_id, co_purchase_count, co_purchase_frequency, last_updated)
SELECT 
    38 as product_id,  -- Acer Nitro 5
    oi2.product_id as recommended_product_id,
    COUNT(DISTINCT oi1.order_id) as co_purchase_count,
    COUNT(DISTINCT oi1.order_id) / 2.0 as co_purchase_frequency,  -- 2 là total orders của Acer
    NOW() as last_updated
FROM order_items oi1
JOIN order_items oi2 ON oi1.order_id = oi2.order_id
JOIN orders o ON oi1.order_id = o.id
JOIN products p2 ON oi2.product_id = p2.id
WHERE oi1.product_id = 38  -- Acer Nitro 5
AND oi2.product_id != 38   -- Không tính chính nó
AND o.status = 'DELIVERED'
AND p2.active = true
AND p2.stock > 0
GROUP BY oi2.product_id
HAVING COUNT(DISTINCT oi1.order_id) >= 2  -- Threshold = 2
AND (COUNT(DISTINCT oi1.order_id) / 2.0) >= 0.01  -- Frequency >= 1%
ORDER BY co_purchase_count DESC;

-- BƯỚC 5: Tạo recommendations cho TẤT CẢ sản phẩm có >= 2 đơn hàng DELIVERED

INSERT INTO product_recommendations (product_id, recommended_product_id, co_purchase_count, co_purchase_frequency, last_updated)
SELECT 
    oi1.product_id as product_id,
    oi2.product_id as recommended_product_id,
    COUNT(DISTINCT oi1.order_id) as co_purchase_count,
    COUNT(DISTINCT oi1.order_id) / (
        SELECT COUNT(DISTINCT oi_total.order_id) 
        FROM order_items oi_total 
        JOIN orders o_total ON oi_total.order_id = o_total.id 
        WHERE oi_total.product_id = oi1.product_id 
        AND o_total.status = 'DELIVERED'
    ) as co_purchase_frequency,
    NOW() as last_updated
FROM order_items oi1
JOIN order_items oi2 ON oi1.order_id = oi2.order_id
JOIN orders o ON oi1.order_id = o.id
JOIN products p1 ON oi1.product_id = p1.id
JOIN products p2 ON oi2.product_id = p2.id
WHERE oi1.product_id != oi2.product_id  -- Không tính chính nó
AND o.status = 'DELIVERED'
AND p1.active = true
AND p2.active = true
AND p2.stock > 0
-- Chỉ lấy sản phẩm có >= 2 đơn hàng DELIVERED
AND oi1.product_id IN (
    SELECT oi_check.product_id
    FROM order_items oi_check
    JOIN orders o_check ON oi_check.order_id = o_check.id
    WHERE o_check.status = 'DELIVERED'
    GROUP BY oi_check.product_id
    HAVING COUNT(DISTINCT o_check.id) >= 2
)
GROUP BY oi1.product_id, oi2.product_id
HAVING COUNT(DISTINCT oi1.order_id) >= 2  -- Co-purchase threshold = 2
AND (COUNT(DISTINCT oi1.order_id) / (
    SELECT COUNT(DISTINCT oi_total.order_id) 
    FROM order_items oi_total 
    JOIN orders o_total ON oi_total.order_id = o_total.id 
    WHERE oi_total.product_id = oi1.product_id 
    AND o_total.status = 'DELIVERED'
)) >= 0.01  -- Frequency threshold = 1%
-- Tránh duplicate với các INSERT trước
AND NOT EXISTS (
    SELECT 1 FROM product_recommendations pr_existing 
    WHERE pr_existing.product_id = oi1.product_id 
    AND pr_existing.recommended_product_id = oi2.product_id
)
ORDER BY oi1.product_id, co_purchase_count DESC;

-- BƯỚC 6: Kiểm tra kết quả
SELECT 'FINAL RESULTS' as step;

SELECT 
    pr.product_id,
    p1.name as product_name,
    pr.recommended_product_id,
    p2.name as recommended_product_name,
    pr.co_purchase_count,
    ROUND(pr.co_purchase_frequency * 100, 2) as frequency_percent,
    pr.last_updated
FROM product_recommendations pr
JOIN products p1 ON pr.product_id = p1.id
JOIN products p2 ON pr.recommended_product_id = p2.id
ORDER BY pr.product_id, pr.co_purchase_frequency DESC;

-- BƯỚC 7: Thống kê tổng quan
SELECT 
    COUNT(*) as total_recommendations,
    COUNT(DISTINCT product_id) as products_with_recommendations,
    AVG(co_purchase_count) as avg_co_purchase_count,
    AVG(co_purchase_frequency) as avg_frequency
FROM product_recommendations;