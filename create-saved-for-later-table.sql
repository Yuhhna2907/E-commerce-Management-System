-- Tạo bảng saved_for_later
CREATE TABLE IF NOT EXISTS saved_for_later (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT,
    quantity INT NOT NULL DEFAULT 1,
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(500),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    INDEX idx_saved_at (saved_at),
    UNIQUE KEY unique_user_product_variant (user_id, product_id, variant_id)
);

-- Comment
COMMENT ON TABLE saved_for_later IS 'Bảng lưu trữ sản phẩm người dùng muốn mua sau';
COMMENT ON COLUMN saved_for_later.user_id IS 'ID người dùng';
COMMENT ON COLUMN saved_for_later.product_id IS 'ID sản phẩm';
COMMENT ON COLUMN saved_for_later.variant_id IS 'ID biến thể sản phẩm (nullable)';
COMMENT ON COLUMN saved_for_later.quantity IS 'Số lượng';
COMMENT ON COLUMN saved_for_later.saved_at IS 'Thời gian lưu';
COMMENT ON COLUMN saved_for_later.note IS 'Ghi chú (nullable)';
