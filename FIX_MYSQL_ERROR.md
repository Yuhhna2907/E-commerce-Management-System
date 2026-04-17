# 🚨 LỖI KẾT NỐI MYSQL - HƯỚNG DẪN KHẮC PHỤC

## ❌ Lỗi Hiện Tại
```
Access denied for user 'root'@'localhost' (using password: YES)
```

**Nguyên nhân:** MySQL server không chạy hoặc chưa được cài đặt đúng cách.

---

## 📋 Các Bước Khắc Phục

### **Bước 1: Kiểm Tra MySQL Đã Cài Đặt Chưa**

#### **Cách 1: Kiểm tra qua Services**
```
1. Nhấn Win + R → gõ "services.msc" → Enter
2. Tìm service có tên chứa "MySQL"
3. Nếu có: Click chuột phải → Start
4. Nếu không có: Tiếp tục bước 2
```

#### **Cách 2: Kiểm tra qua Command Line**
```cmd
# Mở Command Prompt as Administrator
# Chạy lệnh:
sc query mysql
# Hoặc:
net start mysql
```

---

### **Bước 2: Nếu MySQL Chưa Cài Đặt**

#### **Tải và Cài Đặt MySQL**

**Option 1: MySQL Installer (Khuyên dùng)**
```
1. Truy cập: https://dev.mysql.com/downloads/installer/
2. Tải: "MySQL Installer for Windows"
3. Chạy installer
4. Chọn: "Developer Default" hoặc "Server Only"
5. Đặt password cho root: "tittom343" (theo config)
6. Hoàn thành cài đặt
```

**Option 2: XAMPP (Dễ hơn cho beginner)**
```
1. Truy cập: https://www.apachefriends.org/download.html
2. Tải XAMPP cho Windows
3. Cài đặt XAMPP
4. Mở XAMPP Control Panel
5. Start MySQL module
```

---

### **Bước 3: Tạo Database**

Sau khi MySQL chạy, tạo database:

#### **Cách 1: Qua MySQL Workbench**
```
1. Mở MySQL Workbench
2. Connect to localhost
3. Username: root
4. Password: tittom343
5. Tạo query mới:
   CREATE DATABASE smart_phone CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### **Cách 2: Qua Command Line**
```cmd
# Mở Command Prompt
mysql -u root -p
# Nhập password: tittom343

# Trong MySQL shell:
CREATE DATABASE smart_phone CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SHOW DATABASES;
EXIT;
```

---

### **Bước 4: Kiểm Tra Kết Nối**

#### **Test Connection**
```cmd
mysql -u root -p -e "SHOW DATABASES;"
# Nhập password: tittom343
```

**Kết quả mong đợi:**
```
+--------------------+
| Database           |
+--------------------+
| information_schema |
| mysql              |
| performance_schema |
| smart_phone        |  ← Phải có database này
| sys                |
+--------------------+
```

---

### **Bước 5: Chạy Lại Ứng Dụng**

```cmd
cd D:\Module4\case\E-commerce-Management-System
./gradlew bootRun
```

**Nếu thành công, bạn sẽ thấy:**
```
2026-04-16T20:07:04.031+07:00  INFO ... HikariPool-1 - Starting...
2026-04-16T20:07:06.958+07:00  INFO ... HikariPool-1 - Start completed.
Tomcat started on port(s): 8080 (http) with context path ''
Started SmartphoneManagementApplication in 10.123 seconds
```

---

## 🔧 Cấu Hình Database Trong Code

File: `src/main/resources/application-local.properties`

```ini
# DATABASE
spring.datasource.url=jdbc:mysql://localhost:3306/smart_phone
spring.datasource.username=root
spring.datasource.password=tittom343
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

## 🚨 Nếu Vẫn Lỗi

### **Lỗi: "Access denied for user 'root'@'localhost'"**

**Khắc phục:**
```sql
-- Đăng nhập MySQL với user khác (nếu có)
mysql -u [other_user] -p

-- Hoặc reset password root
ALTER USER 'root'@'localhost' IDENTIFIED BY 'tittom343';
FLUSH PRIVILEGES;
```

### **Lỗi: "Unknown database 'smart_phone'"**

**Khắc phục:**
```sql
CREATE DATABASE smart_phone;
```

### **Lỗi: Port 3306 bị chiếm**

**Khắc phục:**
```cmd
# Kiểm tra port
netstat -ano | findstr :3306

# Tìm PID và kill process
taskkill /PID [PID_NUMBER] /F
```

---

## 📞 Hỗ Trợ

Nếu vẫn gặp vấn đề:

```
1. Kiểm tra MySQL service đang chạy
2. Kiểm tra password trong application-local.properties
3. Kiểm tra database 'smart_phone' tồn tại
4. Kiểm tra port 3306 không bị block
5. Restart máy tính
```

---

**Cập nhật:** 16/04/2026  
**Trạng thái:** Chờ cài đặt MySQL

