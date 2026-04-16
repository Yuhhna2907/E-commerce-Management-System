-- 🚀 AUTO SETUP DATABASE SCRIPT
-- Chạy script này sau khi MySQL đã chạy

-- 1. Kết nối MySQL
-- mysql -u root -p
-- Password: tittom343

-- 2. Chạy các lệnh sau:

-- Tạo database nếu chưa có
CREATE DATABASE IF NOT EXISTS smart_phone
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- Sử dụng database
USE smart_phone;

-- Kiểm tra database
SELECT DATABASE();

-- Tạo user và cấp quyền (nếu cần)
-- GRANT ALL PRIVILEGES ON smart_phone.* TO 'root'@'localhost';
-- FLUSH PRIVILEGES;

-- Kiểm tra tables (sẽ được tạo tự động bởi Hibernate)
SHOW TABLES;

-- Thông báo hoàn thành
SELECT '✅ Database smart_phone đã được tạo thành công!' as Status;
