# Hướng Dẫn Sử Dụng Security Metrics và Login Attempt Cleanup

## Tổng Quan

Hệ thống đã được nâng cấp với các tính năng bảo mật sau:
- **Scheduled Cleanup**: Tự động dọn dẹp các bản ghi đăng nhập cũ hơn 90 ngày
- **Security Metrics API**: Cung cấp thống kê về các lần đăng nhập và tài khoản bị khóa

## 1. Scheduled Cleanup

### Mô Tả
- **Class**: `SecurityMaintenanceScheduler`
- **Chức năng**: Tự động xóa các bản ghi đăng nhập cũ hơn 90 ngày
- **Lịch chạy**: Hàng ngày lúc 00:00 (nửa đêm)
- **Cron Expression**: `0 0 0 * * ?`

### Cấu Hình
Scheduled task được bật tự động thông qua annotation `@EnableScheduling` trong `SmartphoneManagementApplication`.

Để thay đổi lịch chạy, sửa cron expression trong `SecurityMaintenanceScheduler.java`:

```java
@Scheduled(cron = "0 0 0 * * ?") // Chạy lúc nửa đêm mỗi ngày
public void cleanupOldLoginAttempts() {
    // ...
}
```

### Logs
Kiểm tra logs để xác nhận cleanup đã chạy:
```
[INFO] Starting scheduled cleanup of old login attempts
[INFO] Cleaned up login attempts older than 90 days
[INFO] Successfully completed cleanup of old login attempts
```

## 2. Security Metrics API

### Endpoints

Tất cả endpoints yêu cầu quyền **ROLE_ADMIN**.

#### 2.1. Tổng Quan Metrics Đăng Nhập

**Endpoint**: `GET /api/admin/security/metrics/login-attempts`

**Parameters**:
- `hours` (optional, default: 24): Số giờ để xem lại

**Response**:
```json
{
  "timeRange": "24 hours",
  "totalAttempts": 150,
  "successfulAttempts": 140,
  "failedAttempts": 10,
  "lockedAccounts": 2,
  "successRate": "93.33%"
}
```

**Ví dụ**:
```bash
curl -X GET "http://localhost:8080/api/admin/security/metrics/login-attempts?hours=48" \
  -H "Authorization: Bearer <admin-token>"
```

#### 2.2. Danh Sách Tài Khoản Bị Khóa

**Endpoint**: `GET /api/admin/security/metrics/locked-accounts`

**Response**:
```json
[
  {
    "username": "user123",
    "lockoutTime": "2024-01-15T10:30:00",
    "failedAttempts": 5
  },
  {
    "username": "user456",
    "lockoutTime": "2024-01-15T11:00:00",
    "failedAttempts": 5
  }
]
```

**Ví dụ**:
```bash
curl -X GET "http://localhost:8080/api/admin/security/metrics/locked-accounts" \
  -H "Authorization: Bearer <admin-token>"
```

#### 2.3. Lần Đăng Nhập Thất Bại Theo IP

**Endpoint**: `GET /api/admin/security/metrics/failed-by-ip`

**Parameters**:
- `hours` (optional, default: 24): Số giờ để xem lại

**Response**:
```json
{
  "192.168.1.100": 15,
  "192.168.1.101": 8,
  "10.0.0.50": 3
}
```

**Ví dụ**:
```bash
curl -X GET "http://localhost:8080/api/admin/security/metrics/failed-by-ip?hours=12" \
  -H "Authorization: Bearer <admin-token>"
```

#### 2.4. Lần Đăng Nhập Của User Cụ Thể

**Endpoint**: `GET /api/admin/security/metrics/user-attempts`

**Parameters**:
- `username` (required): Tên người dùng
- `hours` (optional, default: 24): Số giờ để xem lại

**Response**:
```json
[
  {
    "id": 123,
    "username": "user123",
    "attemptTime": "2024-01-15T10:25:00",
    "success": false,
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0..."
  }
]
```

**Ví dụ**:
```bash
curl -X GET "http://localhost:8080/api/admin/security/metrics/user-attempts?username=user123&hours=6" \
  -H "Authorization: Bearer <admin-token>"
```

## 3. Cấu Hình Database

### Migration Script
File migration `V14__add_account_lockout_fields.sql` đã được tạo để thêm các trường:
- `lockout_time`: Thời gian mở khóa tài khoản
- `failed_login_attempts`: Số lần đăng nhập thất bại liên tiếp

### Chạy Migration
Nếu sử dụng Flyway:
```bash
./gradlew flywayMigrate
```

Nếu sử dụng Hibernate auto-DDL (như hiện tại):
- Các trường sẽ được tự động tạo khi khởi động ứng dụng

## 4. Monitoring và Troubleshooting

### Kiểm Tra Scheduled Task
Xem logs để đảm bảo scheduled task đang chạy:
```bash
tail -f logs/application.log | grep "cleanup"
```

### Kiểm Tra Metrics
Test metrics endpoint:
```bash
curl -X GET "http://localhost:8080/api/admin/security/metrics/login-attempts" \
  -u admin:password
```

### Common Issues

**Issue 1**: Scheduled task không chạy
- **Giải pháp**: Kiểm tra `@EnableScheduling` đã được thêm vào `SmartphoneManagementApplication`

**Issue 2**: Metrics endpoint trả về 403 Forbidden
- **Giải pháp**: Đảm bảo user có ROLE_ADMIN

**Issue 3**: Database migration lỗi
- **Giải pháp**: Chạy migration script thủ công hoặc kiểm tra Hibernate auto-DDL

## 5. Best Practices

1. **Monitoring**: Thiết lập alerts cho số lượng tài khoản bị khóa cao
2. **Cleanup**: Giữ nguyên lịch cleanup hàng ngày để tránh database phình to
3. **Security**: Chỉ cấp quyền ROLE_ADMIN cho những người cần thiết
4. **Logging**: Theo dõi security audit logs thường xuyên

## 6. Tham Khảo

- **LoginAttemptService**: Service chính cho tracking và lockout
- **SecurityMaintenanceScheduler**: Scheduled tasks cho maintenance
- **SecurityMetricsController**: REST API cho metrics
- **SecurityAuditLogger**: Centralized security logging

## 7. Liên Hệ

Nếu có vấn đề hoặc câu hỏi, vui lòng liên hệ team bảo mật.
