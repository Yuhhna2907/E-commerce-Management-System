-- ============================================
-- RESET PASSWORD TRỰC TIẾP TRONG DATABASE
-- Password: Tittom343@
-- ============================================

SET SQL_SAFE_UPDATES = 0;

-- BCrypt hash của password "Tittom343@" (strength 10)
-- Hash này được generate bằng BCrypt với cost factor = 10
UPDATE users 
SET password = '$2a$10$xQJ5YvH8vKGKZN8fJ5YvH8vKGKZN8fJ5YvH8vKGKZN8fJ5YvH8vKG',
    lockout_time = NULL,
    failed_login_attempts = 0,
    enabled = 1
WHERE email = 'luonganhhuy2004@gmail.com';

-- NOTE: Hash trên là PLACEHOLDER. Cần generate hash thật từ Java code.
-- Chạy đoạn code Java này để generate hash:
-- 
-- import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
-- 
-- public class GenerateHash {
--     public static void main(String[] args) {
--         BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
--         String hash = encoder.encode("Tittom343@");
--         System.out.println(hash);
--     }
-- }

SET SQL_SAFE_UPDATES = 1;

-- Verify
SELECT id, username, email, 
       SUBSTRING(password, 1, 20) as password_preview,
       enabled, lockout_time, failed_login_attempts 
FROM users 
WHERE email = 'luonganhhuy2004@gmail.com';
