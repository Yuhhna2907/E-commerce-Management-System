-- Migration: Thêm version columns cho optimistic locking
-- Date: 2026-04-16
-- Purpose: Fix race conditions trong CartService và LoyaltyPointService

-- 1. Thêm version column cho ProductVariant (Fix #1)
ALTER TABLE product_variants 
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- 2. Thêm version column cho LoyaltyAccount (Fix #13)
ALTER TABLE loyalty_accounts 
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- 3. Thêm unique constraint cho reviews (Fix #6)
ALTER TABLE reviews 
ADD CONSTRAINT IF NOT EXISTS uk_user_product 
UNIQUE (user_id, product_id);

-- 4. Thêm indexes cho performance
CREATE INDEX IF NOT EXISTS idx_user_address_user_default 
ON user_addresses(user_id, is_default);

CREATE INDEX IF NOT EXISTS idx_product_name_active 
ON products(name, active);

-- Comments
COMMENT ON COLUMN product_variants.version IS 'Optimistic locking version để tránh race condition khi update stock';
COMMENT ON COLUMN loyalty_accounts.version IS 'Optimistic locking version để tránh race condition khi redeem points';
