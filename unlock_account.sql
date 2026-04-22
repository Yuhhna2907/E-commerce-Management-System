-- ============================================
-- BƯỚC 1: TẮT SAFE MODE (nếu cần)
-- ============================================
SET SQL_SAFE_UPDATES = 0;

-- ============================================
-- BƯỚC 2: UNLOCK ACCOUNT
-- ============================================
UPDATE users 
SET lockout_time = NULL, 
    failed_login_attempts = 0
WHERE email = 'luonganhhuy2004@gmail.com';

-- ============================================
-- BƯỚC 3: BẬT LẠI SAFE MODE (recommended)
-- ============================================
SET SQL_SAFE_UPDATES = 1;

-- ============================================
-- BƯỚC 4: VERIFY KẾT QUẢ
-- ============================================
SELECT id, username, email, lockout_time, failed_login_attempts 
FROM users 
WHERE email = 'luonganhhuy2004@gmail.com';
