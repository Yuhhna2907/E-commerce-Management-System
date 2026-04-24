-- Fix NULL version in coupons table for optimistic locking
-- This fixes NullPointerException when Hibernate tries to increment version

UPDATE coupons 
SET version = 0 
WHERE version IS NULL;

-- Ensure version column has default value for future inserts
ALTER TABLE coupons 
MODIFY COLUMN version BIGINT DEFAULT 0;
