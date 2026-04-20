-- Tạo role ADMIN nếu chưa có
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN');

-- Tạo user admin với mật khẩu mã hóa của 'admin123' (sử dụng BCrypt)
-- Mật khẩu 'admin123' được mã hóa: $2a$10$... (thay bằng giá trị thực tế)
-- Để đơn giản, sử dụng mật khẩu đã mã hóa sẵn
INSERT INTO users (username, password, full_name, email, phone, enabled, created_at)
VALUES ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Administrator', 'admin@smartzone.com', '0123456789', true, NOW())
ON DUPLICATE KEY UPDATE username=username;

-- Gán role ADMIN cho user admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE user_id=user_id;
