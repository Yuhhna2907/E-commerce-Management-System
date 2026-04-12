-- Migration script to add version column for optimistic locking
-- This prevents race conditions when multiple users try to use the same coupon simultaneously

-- Add version column to coupons table
ALTER TABLE coupons ADD COLUMN version BIGINT DEFAULT 0;

-- Update existing rows to have version = 0
UPDATE coupons SET version = 0 WHERE version IS NULL;

-- Make version column NOT NULL after setting default values
ALTER TABLE coupons MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
