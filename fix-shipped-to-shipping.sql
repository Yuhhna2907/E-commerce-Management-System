-- =====================================================
-- Script: Fix SHIPPED to SHIPPING in database
-- Description: Update all orders with status 'SHIPPED' to 'SHIPPING'
--              to match the OrderStatus enum
-- =====================================================

-- Update orders table
UPDATE orders 
SET status = 'SHIPPING' 
WHERE status = 'SHIPPED';

-- Update order_history table (if exists)
UPDATE order_history 
SET status_from = 'SHIPPING' 
WHERE status_from = 'SHIPPED';

UPDATE order_history 
SET status_to = 'SHIPPING' 
WHERE status_to = 'SHIPPED';

-- Verify the changes
SELECT 'Orders with SHIPPED status (should be 0):' as check_result, COUNT(*) as count 
FROM orders 
WHERE status = 'SHIPPED'
UNION ALL
SELECT 'Orders with SHIPPING status:', COUNT(*) 
FROM orders 
WHERE status = 'SHIPPING';

-- Show affected records
SELECT id, status, created_at 
FROM orders 
WHERE status = 'SHIPPING' 
ORDER BY created_at DESC 
LIMIT 10;
