# ⚡ QUICK FIX - Chạy Ứng Dụng Ngay

## 🚨 Lỗi Hiện Tại
```
Access denied for user 'root'@'localhost' (using password: YES)
```

## 🛠️ Giải Pháp Nhanh

### **Bước 1: Cài Đặt XAMPP (Nhanh Nhất)**

```
1. Tải XAMPP: https://www.apachefriends.org/download.html
2. Chạy installer
3. Mở XAMPP Control Panel
4. Start Apache + MySQL
5. Click "Admin" bên MySQL → Mở phpMyAdmin
```

### **Bước 2: Tạo Database**

Trong phpMyAdmin:
```
1. Click tab "SQL"
2. Chạy lệnh:
   CREATE DATABASE smart_phone CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
3. Click "Go"
```

### **Bước 3: Đặt Password MySQL**

Trong XAMPP:
```
1. Click "Config" bên MySQL
2. Chọn "my.ini"
3. Thêm vào cuối file:
   [mysqld]
   alter user 'root'@'localhost' identified by 'tittom343';
4. Restart MySQL
```

### **Bước 4: Chạy Ứng Dụng**

```cmd
cd D:\Module4\case\E-commerce-Management-System
./gradlew bootRun
```

---

## ✅ Kiểm Tra Thành Công

**Nếu thành công, bạn sẽ thấy:**
```
Tomcat started on port(s): 8080 (http)
Started SmartphoneManagementApplication
```

**Truy cập:** http://localhost:8080/admin/products

---

## 🔄 Nếu Vẫn Lỗi

**Thử cách khác:**
```
1. Dùng MySQL Workbench thay vì XAMPP
2. Đặt password root thành '123456' thay vì 'tittom343'
3. Kiểm tra file application-local.properties
```

---

**Thời gian:** 10-15 phút  
**Độ khó:** Dễ  
**Công cụ:** XAMPP

