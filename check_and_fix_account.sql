-- ============================================
-- SCRIPT KIỂM TRA VÀ SỬA TÀI KHOẢN
-- ============================================

-- BƯỚC 1: Tắt safe mode
SET SQL_SAFE_UPDATES = 0;

-- BƯỚC 2: Kiểm tra trạng thái tài khoản
SELECT 
    id,
    username,
    email,
    enabled,
    lockout_time,
    failed_login_attempts,
    CASE 
        WHEN lockout_time IS NULL THEN 'KHÔNG BỊ KHÓA'
        WHEN lockout_time > NOW() THEN 'BỊ KHÓA'
        ELSE 'HẾT HẠN KHÓA'
    END as lock_status,
    password as password_hash
FROM users 
WHERE email = 'luonganhhuy2004@gmail.com';

-- BƯỚC 3: Mở khóa tài khoản
UPDATE users 
SET lockout_time = NULL, 
    failed_login_attempts = 0
WHERE email = 'luonganhhuy2004@gmail.com';

-- BƯỚC 4: Đặt lại mật khẩu về Tittom343@ (BCrypt hash)
-- Hash này là BCrypt của "Tittom343@"
UPDATE users 
SET password = '$2a$10$YourBCryptHashHere'
WHERE email = 'luonganhhuy2004@gmail.com';

-- BƯỚC 5: Verify kết quả
SELECT 
    id,
    username,
    email,
    enabled,
    lockout_time,
    failed_login_attempts,
    'TÀI KHOẢN ĐÃ MỞ KHÓA' as status
FROM users 
WHERE email = 'luonganhhuy2004@gmail.com';

-- BƯỚC 6: Bật lại safe mode
SET SQL_SAFE_UPDATES = 1;
