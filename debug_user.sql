-- DEBUG: Check user account status
SELECT 
    id,
    username,
    email,
    password,  -- BCrypt hash
    enabled,
    lockout_time,
    failed_login_attempts,
    created_at
FROM users 
WHERE email = 'luonganhhuy2004@gmail.com';

-- Check if user exists
SELECT COUNT(*) as user_exists FROM users WHERE email = 'luonganhhuy2004@gmail.com';

-- Check roles
SELECT u.username, r.name as role_name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'luonganhhuy2004@gmail.com';
