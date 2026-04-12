-- ============================================================
-- SQL Migration: Fix All Logic Errors
-- Ngày: 12/04/2026
-- Mục đích: Thêm constraints và indexes để hỗ trợ các fix logic
-- ============================================================

-- FIX #6: Thêm unique constraint cho reviews (tránh duplicate review)
-- Kiểm tra xem constraint đã tồn tại chưa
SELECT CONSTRAINT_NAME 
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_NAME = 'reviews' 
  AND CONSTRAINT_NAME = 'uk_user_product';

-- Nếu chưa có, thêm constraint
ALTER TABLE reviews 
ADD CONSTRAINT uk_user_product 
UNIQUE (user_id, product_id);

-- FIX #1: Thêm version column cho ProductVariant (optimistic locking)
-- Kiểm tra xem column đã tồn tại chưa
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'product_variants' 
  AND COLUMN_NAME = 'version';

-- Nếu chưa có, thêm column
ALTER TABLE product_variants 
ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- Update existing rows
UPDATE product_variants SET version = 0 WHERE version IS NULL;

-- FIX #9: Thêm index cho performance khi query user addresses
CREATE INDEX IF NOT EXISTS idx_user_address_user_default 
ON user_addresses(user_id, is_default);

-- Thêm index cho cart_items để query nhanh hơn
CREATE INDEX IF NOT EXISTS idx_cart_items_product 
ON cart_items(product_id);

-- Thêm index cho reviews để query nhanh hơn
CREATE INDEX IF NOT EXISTS idx_reviews_user_product 
ON reviews(user_id, product_id);

-- ============================================================
-- VERIFICATION QUERIES
-- Chạy các query này để verify migration thành công
-- ============================================================

-- 1. Check unique constraint reviews
SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE 
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_NAME = 'reviews' 
  AND CONSTRAINT_NAME = 'uk_user_product';
-- Expected: 1 row với CONSTRAINT_TYPE = 'UNIQUE'

-- 2. Check version column
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'product_variants' 
  AND COLUMN_NAME = 'version';
-- Expected: 1 row với DATA_TYPE = 'bigint', IS_NULLABLE = 'NO'

-- 3. Check indexes
SHOW INDEX FROM user_addresses WHERE Key_name = 'idx_user_address_user_default';
SHOW INDEX FROM cart_items WHERE Key_name = 'idx_cart_items_product';
SHOW INDEX FROM reviews WHERE Key_name = 'idx_reviews_user_product';
-- Expected: Mỗi query trả về ít nhất 1 row

-- ============================================================
-- ROLLBACK (Nếu cần)
-- ============================================================

-- Rollback FIX #6
-- ALTER TABLE reviews DROP CONSTRAINT uk_user_product;

-- Rollback FIX #1
-- ALTER TABLE product_variants DROP COLUMN version;

-- Rollback indexes
-- DROP INDEX idx_user_address_user_default ON user_addresses;
-- DROP INDEX idx_cart_items_product ON cart_items;
-- DROP INDEX idx_reviews_user_product ON reviews;
