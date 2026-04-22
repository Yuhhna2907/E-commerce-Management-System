-- ============================================
-- TẮT SAFE MODE VĨNH VIỄN (cho session hiện tại)
-- ============================================
SET SQL_SAFE_UPDATES = 0;

-- Verify safe mode đã tắt
SELECT @@SQL_SAFE_UPDATES;
-- Kết quả = 0 nghĩa là đã tắt

-- ============================================
-- SAU KHI TẮT, CHẠY CÁC LỆNH UPDATE/DELETE
-- ============================================
