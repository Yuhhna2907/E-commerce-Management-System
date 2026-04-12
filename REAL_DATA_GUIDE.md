# Hướng dẫn Thêm Dữ Liệu Thực Tế từ Thế Giới Di Động

## Dữ liệu Smartphone Flagship 2024-2025

### 1. iPhone 15 Pro Max
```sql
-- Product
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('iPhone 15 Pro Max 256GB', 'Apple', 29990000, 50, 120, true, 
'https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg',
'iPhone 15 Pro Max - Đỉnh cao công nghệ với chip A17 Pro, camera 48MP, màn hình Super Retina XDR 6.7 inch',
1, NOW(), NOW(), 4.8, 245);

-- ProductSpecification
INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Apple A17 Pro', 1850000, 6.7, 'Super Retina XDR OLED, 120Hz', 
'Camera chính: 48MP, Ultra Wide: 12MP, Telephoto: 12MP (5x zoom quang học)', 
4422, 27, 'iOS 18', 221);
```

### 2. Samsung Galaxy S24 Ultra
```sql
-- Product
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Samsung Galaxy S24 Ultra 12GB 256GB', 'Samsung', 26990000, 45, 98, true,
'https://cdn.tgdd.vn/Products/Images/42/307174/samsung-galaxy-s24-ultra-grey-thumbnew-600x600.jpg',
'Galaxy S24 Ultra - Siêu phẩm AI với Snapdragon 8 Gen 3, S Pen tích hợp, camera 200MP',
1, NOW(), NOW(), 4.7, 189);

-- ProductSpecification
INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Snapdragon 8 Gen 3 for Galaxy', 1920000, 6.8, 'Dynamic AMOLED 2X, 120Hz', 
'Camera chính: 200MP, Ultra Wide: 12MP, Telephoto: 50MP (5x) + 10MP (3x)', 
5000, 45, 'Android 14, One UI 6.1', 232);
```

### 3. Xiaomi 14 Ultra
```sql
-- Product
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Xiaomi 14 Ultra 16GB 512GB', 'Xiaomi', 24990000, 30, 67, true,
'https://cdn.tgdd.vn/Products/Images/42/320722/xiaomi-14-ultra-black-thumbnew-600x600.jpg',
'Xiaomi 14 Ultra - Chuyên gia nhiếp ảnh với Leica, Snapdragon 8 Gen 3, sạc nhanh 90W',
1, NOW(), NOW(), 4.6, 134);

-- ProductSpecification
INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Snapdragon 8 Gen 3', 1880000, 6.73, 'AMOLED LTPO, 120Hz', 
'Camera chính: 50MP (Leica), Ultra Wide: 50MP, Telephoto: 50MP (3.2x) + 50MP (5x)', 
5000, 90, 'Android 14, HyperOS', 219);
```

### 4. OPPO Find X7 Ultra
```sql
-- Product
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('OPPO Find X7 Ultra 16GB 512GB', 'Oppo', 22990000, 25, 45, true,
'https://cdn.tgdd.vn/Products/Images/42/320896/oppo-find-x7-ultra-black-thumbnew-600x600.jpg',
'OPPO Find X7 Ultra - Flagship camera Hasselblad, Snapdragon 8 Gen 3, sạc siêu nhanh 100W',
1, NOW(), NOW(), 4.5, 98);

-- ProductSpecification
INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Snapdragon 8 Gen 3', 1870000, 6.82, 'AMOLED LTPO, 120Hz', 
'Camera chính: 50MP (Hasselblad), Ultra Wide: 50MP, Telephoto: 50MP (3x) + 50MP (6x)', 
5000, 100, 'Android 14, ColorOS 14', 221);
```

### 5. iPhone 14 Pro Max
```sql
-- Product
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('iPhone 14 Pro Max 256GB', 'Apple', 24990000, 40, 156, true,
'https://cdn.tgdd.vn/Products/Images/42/289700/iphone-14-pro-max-purple-1.jpg',
'iPhone 14 Pro Max - Dynamic Island, chip A16 Bionic, camera 48MP Pro',
1, NOW(), NOW(), 4.7, 312);

-- ProductSpecification
INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Apple A16 Bionic', 1420000, 6.7, 'Super Retina XDR OLED, 120Hz', 
'Camera chính: 48MP, Ultra Wide: 12MP, Telephoto: 12MP (3x zoom quang học)', 
4323, 27, 'iOS 18', 240);
```

### 6. Samsung Galaxy Z Fold5
```sql
-- Product
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Samsung Galaxy Z Fold5 12GB 256GB', 'Samsung', 35990000, 20, 34, true,
'https://cdn.tgdd.vn/Products/Images/42/309831/samsung-galaxy-z-fold5-xanh-256gb-thumb-600x600.jpg',
'Galaxy Z Fold5 - Điện thoại gập cao cấp, màn hình 7.6 inch, Snapdragon 8 Gen 2',
1, NOW(), NOW(), 4.6, 87);

-- ProductSpecification
INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Snapdragon 8 Gen 2 for Galaxy', 1650000, 7.6, 'Dynamic AMOLED 2X, 120Hz', 
'Camera chính: 50MP, Ultra Wide: 12MP, Telephoto: 10MP (3x zoom)', 
4400, 25, 'Android 14, One UI 6', 253);
```

### 7. Google Pixel 8 Pro
```sql
-- Product
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Google Pixel 8 Pro 12GB 256GB', 'Google', 21990000, 35, 56, true,
'https://cdn.tgdd.vn/Products/Images/42/309831/google-pixel-8-pro-xanh-thumb-600x600.jpg',
'Pixel 8 Pro - AI thuần khiết từ Google, Tensor G3, camera 50MP với Magic Eraser',
1, NOW(), NOW(), 4.5, 123);

-- ProductSpecification
INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Google Tensor G3', 1180000, 6.7, 'LTPO OLED, 120Hz', 
'Camera chính: 50MP, Ultra Wide: 48MP, Telephoto: 48MP (5x zoom)', 
5050, 30, 'Android 14', 213);
```

### 8. OnePlus 12
```sql
-- Product
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('OnePlus 12 16GB 512GB', 'OnePlus', 19990000, 28, 42, true,
'https://cdn.tgdd.vn/Products/Images/42/320123/oneplus-12-green-thumb-600x600.jpg',
'OnePlus 12 - Flagship killer với Snapdragon 8 Gen 3, sạc nhanh 100W',
1, NOW(), NOW(), 4.4, 89);

-- ProductSpecification
INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Snapdragon 8 Gen 3', 1890000, 6.82, 'AMOLED LTPO, 120Hz', 
'Camera chính: 50MP (Hasselblad), Ultra Wide: 48MP, Telephoto: 64MP (3x)', 
5400, 100, 'Android 14, OxygenOS 14', 220);
```

### 9. iPhone 15 Plus
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('iPhone 15 Plus 128GB', 'Apple', 22990000, 55, 145, true,
'https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-plus-128gb-xanh-thumb-600x600.jpg',
'iPhone 15 Plus - Màn hình lớn 6.7 inch, chip A16 Bionic, camera 48MP',
1, NOW(), NOW(), 4.6, 198);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Apple A16 Bionic', 1420000, 6.7, 'Super Retina XDR OLED, 60Hz', 
'Camera chính: 48MP, Ultra Wide: 12MP', 
4383, 20, 'iOS 18', 201);
```

### 10. Samsung Galaxy S23 Ultra
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Samsung Galaxy S23 Ultra 12GB 256GB', 'Samsung', 22990000, 38, 167, true,
'https://cdn.tgdd.vn/Products/Images/42/301608/samsung-galaxy-s23-ultra-1-1.jpg',
'Galaxy S23 Ultra - Snapdragon 8 Gen 2, S Pen, camera 200MP đỉnh cao',
1, NOW(), NOW(), 4.7, 234);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Snapdragon 8 Gen 2 for Galaxy', 1650000, 6.8, 'Dynamic AMOLED 2X, 120Hz', 
'Camera chính: 200MP, Ultra Wide: 12MP, Telephoto: 10MP (10x) + 10MP (3x)', 
5000, 45, 'Android 14, One UI 6', 234);
```

### 11. Xiaomi 13T Pro
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Xiaomi 13T Pro 12GB 256GB', 'Xiaomi', 12990000, 42, 89, true,
'https://cdn.tgdd.vn/Products/Images/42/309816/xiaomi-13t-pro-xanh-thumb-600x600.jpg',
'Xiaomi 13T Pro - Dimensity 9200+, camera Leica 50MP, sạc 120W',
1, NOW(), NOW(), 4.5, 156);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'MediaTek Dimensity 9200+', 1450000, 6.67, 'AMOLED, 144Hz', 
'Camera chính: 50MP (Leica), Ultra Wide: 12MP, Telephoto: 50MP (2x)', 
5000, 120, 'Android 14, HyperOS', 200);
```

### 12. OPPO Reno11 Pro
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('OPPO Reno11 Pro 5G 12GB 256GB', 'Oppo', 11990000, 48, 76, true,
'https://cdn.tgdd.vn/Products/Images/42/320896/oppo-reno11-pro-xam-thumb-600x600.jpg',
'OPPO Reno11 Pro - Dimensity 8200, camera chân dung 32MP, sạc 80W',
1, NOW(), NOW(), 4.4, 112);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'MediaTek Dimensity 8200', 980000, 6.7, 'AMOLED, 120Hz', 
'Camera chính: 50MP, Ultra Wide: 8MP, Telephoto: 32MP (2x)', 
4600, 80, 'Android 14, ColorOS 14', 181);
```

### 13. Vivo V30 Pro
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Vivo V30 Pro 12GB 256GB', 'Vivo', 12990000, 35, 58, true,
'https://cdn.tgdd.vn/Products/Images/42/320123/vivo-v30-pro-xanh-thumb-600x600.jpg',
'Vivo V30 Pro - Dimensity 8200, camera Aura Light 50MP, thiết kế mỏng nhẹ',
1, NOW(), NOW(), 4.3, 94);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'MediaTek Dimensity 8200', 980000, 6.78, 'AMOLED, 120Hz', 
'Camera chính: 50MP, Ultra Wide: 50MP, Telephoto: 50MP (2x)', 
5000, 80, 'Android 14, Funtouch OS 14', 188);
```

### 14. Realme GT 5 Pro
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Realme GT 5 Pro 12GB 256GB', 'Realme', 13990000, 32, 47, true,
'https://cdn.tgdd.vn/Products/Images/42/320456/realme-gt5-pro-xanh-thumb-600x600.jpg',
'Realme GT 5 Pro - Snapdragon 8 Gen 3, màn hình 144Hz, sạc 100W',
1, NOW(), NOW(), 4.4, 78);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Snapdragon 8 Gen 3', 1880000, 6.78, 'AMOLED LTPO, 144Hz', 
'Camera chính: 50MP (Sony IMX890), Ultra Wide: 8MP, Telephoto: 50MP (3x)', 
5400, 100, 'Android 14, Realme UI 5.0', 218);
```

### 15. iPhone 13
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('iPhone 13 128GB', 'Apple', 14990000, 65, 234, true,
'https://cdn.tgdd.vn/Products/Images/42/230529/iphone-13-pink-1-600x600.jpg',
'iPhone 13 - Chip A15 Bionic, camera kép 12MP, pin trâu',
1, NOW(), NOW(), 4.6, 456);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Apple A15 Bionic', 820000, 6.1, 'Super Retina XDR OLED, 60Hz', 
'Camera chính: 12MP, Ultra Wide: 12MP', 
3240, 20, 'iOS 18', 174);
```

### 16. Samsung Galaxy A55 5G
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Samsung Galaxy A55 5G 8GB 128GB', 'Samsung', 9990000, 78, 145, true,
'https://cdn.tgdd.vn/Products/Images/42/320123/samsung-galaxy-a55-5g-xanh-thumb-600x600.jpg',
'Galaxy A55 5G - Exynos 1480, camera 50MP OIS, thiết kế kim loại cao cấp',
1, NOW(), NOW(), 4.3, 189);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Exynos 1480', 620000, 6.6, 'Super AMOLED, 120Hz', 
'Camera chính: 50MP OIS, Ultra Wide: 12MP, Macro: 5MP', 
5000, 25, 'Android 14, One UI 6.1', 213);
```

### 17. Xiaomi Redmi Note 13 Pro+
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Xiaomi Redmi Note 13 Pro+ 5G 12GB 256GB', 'Xiaomi', 9490000, 85, 167, true,
'https://cdn.tgdd.vn/Products/Images/42/320123/redmi-note-13-pro-plus-den-thumb-600x600.jpg',
'Redmi Note 13 Pro+ - Dimensity 7200 Ultra, camera 200MP, sạc 120W',
1, NOW(), NOW(), 4.4, 234);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'MediaTek Dimensity 7200 Ultra', 750000, 6.67, 'AMOLED, 120Hz', 
'Camera chính: 200MP, Ultra Wide: 8MP, Macro: 2MP', 
5000, 120, 'Android 14, HyperOS', 204);
```

### 18. OPPO A78
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('OPPO A78 8GB 256GB', 'Oppo', 6490000, 92, 198, true,
'https://cdn.tgdd.vn/Products/Images/42/309816/oppo-a78-xanh-thumb-600x600.jpg',
'OPPO A78 - Snapdragon 680, màn hình 90Hz, pin 5000mAh',
1, NOW(), NOW(), 4.2, 267);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Snapdragon 680', 280000, 6.43, 'AMOLED, 90Hz', 
'Camera chính: 50MP, Depth: 2MP', 
5000, 67, 'Android 13, ColorOS 13.1', 180);
```

### 19. Vivo Y36
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Vivo Y36 8GB 128GB', 'Vivo', 5490000, 105, 223, true,
'https://cdn.tgdd.vn/Products/Images/42/309816/vivo-y36-xanh-thumb-600x600.jpg',
'Vivo Y36 - Snapdragon 680, camera 50MP, pin khủng 5000mAh',
1, NOW(), NOW(), 4.1, 312);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Snapdragon 680', 280000, 6.64, 'IPS LCD, 90Hz', 
'Camera chính: 50MP, Depth: 2MP', 
5000, 44, 'Android 13, Funtouch OS 13', 202);
```

### 20. Realme C55
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Realme C55 8GB 256GB', 'Realme', 4990000, 118, 289, true,
'https://cdn.tgdd.vn/Products/Images/42/309816/realme-c55-den-thumb-600x600.jpg',
'Realme C55 - Helio G88, camera 64MP, sạc nhanh 33W',
1, NOW(), NOW(), 4.0, 378);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'MediaTek Helio G88', 220000, 6.72, 'IPS LCD, 90Hz', 
'Camera chính: 64MP, Depth: 2MP', 
5000, 33, 'Android 13, Realme UI 4.0', 189);
```

### 21. Samsung Galaxy M34 5G
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Samsung Galaxy M34 5G 8GB 128GB', 'Samsung', 6490000, 87, 156, true,
'https://cdn.tgdd.vn/Products/Images/42/309816/samsung-galaxy-m34-5g-xanh-thumb-600x600.jpg',
'Galaxy M34 5G - Exynos 1280, màn hình 120Hz, pin 6000mAh khủng',
1, NOW(), NOW(), 4.2, 198);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Exynos 1280', 480000, 6.5, 'Super AMOLED, 120Hz', 
'Camera chính: 50MP, Ultra Wide: 8MP, Macro: 2MP', 
6000, 25, 'Android 14, One UI 6', 208);
```

### 22. Xiaomi Redmi 13C
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Xiaomi Redmi 13C 6GB 128GB', 'Xiaomi', 2990000, 145, 412, true,
'https://cdn.tgdd.vn/Products/Images/42/309816/redmi-13c-xanh-thumb-600x600.jpg',
'Redmi 13C - Helio G85, màn hình 90Hz, pin 5000mAh giá rẻ',
1, NOW(), NOW(), 3.9, 523);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'MediaTek Helio G85', 210000, 6.74, 'IPS LCD, 90Hz', 
'Camera chính: 50MP, Depth: 2MP', 
5000, 18, 'Android 13, MIUI 14', 192);
```

### 23. iPhone 12
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('iPhone 12 64GB', 'Apple', 11990000, 58, 345, true,
'https://cdn.tgdd.vn/Products/Images/42/213031/iphone-12-xanh-la-1-600x600.jpg',
'iPhone 12 - Chip A14 Bionic, 5G, camera kép 12MP, thiết kế vuông vức',
1, NOW(), NOW(), 4.5, 567);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Apple A14 Bionic', 630000, 6.1, 'Super Retina XDR OLED, 60Hz', 
'Camera chính: 12MP, Ultra Wide: 12MP', 
2815, 20, 'iOS 18', 162);
```

### 24. Samsung Galaxy Z Flip5
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Samsung Galaxy Z Flip5 8GB 256GB', 'Samsung', 19990000, 28, 67, true,
'https://cdn.tgdd.vn/Products/Images/42/309831/samsung-galaxy-z-flip5-tim-thumb-600x600.jpg',
'Galaxy Z Flip5 - Điện thoại gập nhỏ gọn, màn hình phụ 3.4 inch, Snapdragon 8 Gen 2',
1, NOW(), NOW(), 4.5, 134);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Snapdragon 8 Gen 2 for Galaxy', 1650000, 6.7, 'Dynamic AMOLED 2X, 120Hz', 
'Camera chính: 12MP, Ultra Wide: 12MP', 
3700, 25, 'Android 14, One UI 6', 187);
```

### 25. OPPO Find N3 Flip
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('OPPO Find N3 Flip 12GB 256GB', 'Oppo', 18990000, 22, 45, true,
'https://cdn.tgdd.vn/Products/Images/42/309831/oppo-find-n3-flip-hong-thumb-600x600.jpg',
'OPPO Find N3 Flip - Gập dọc cao cấp, Dimensity 9200, camera 50MP',
1, NOW(), NOW(), 4.4, 89);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'MediaTek Dimensity 9200', 1380000, 6.8, 'AMOLED LTPO, 120Hz', 
'Camera chính: 50MP, Ultra Wide: 48MP, Telephoto: 32MP (2x)', 
4300, 44, 'Android 13, ColorOS 13.2', 198);
```

### 26. Xiaomi 13
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Xiaomi 13 8GB 256GB', 'Xiaomi', 14990000, 35, 98, true,
'https://cdn.tgdd.vn/Products/Images/42/301608/xiaomi-13-xanh-thumb-600x600.jpg',
'Xiaomi 13 - Snapdragon 8 Gen 2, camera Leica 50MP, sạc 67W',
1, NOW(), NOW(), 4.5, 167);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Snapdragon 8 Gen 2', 1620000, 6.36, 'AMOLED, 120Hz', 
'Camera chính: 50MP (Leica), Ultra Wide: 12MP, Telephoto: 10MP (3.2x)', 
4500, 67, 'Android 14, HyperOS', 189);
```

### 27. Vivo V29e
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Vivo V29e 8GB 256GB', 'Vivo', 7990000, 62, 123, true,
'https://cdn.tgdd.vn/Products/Images/42/309816/vivo-v29e-xanh-thumb-600x600.jpg',
'Vivo V29e - Snapdragon 695, camera 64MP OIS, thiết kế đổi màu',
1, NOW(), NOW(), 4.2, 178);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'Snapdragon 695 5G', 420000, 6.67, 'AMOLED, 120Hz', 
'Camera chính: 64MP OIS, Depth: 2MP', 
4800, 80, 'Android 13, Funtouch OS 13', 181);
```

### 28. Realme 11 Pro+
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Realme 11 Pro+ 5G 12GB 512GB', 'Realme', 10990000, 45, 87, true,
'https://cdn.tgdd.vn/Products/Images/42/309816/realme-11-pro-plus-xanh-thumb-600x600.jpg',
'Realme 11 Pro+ - Dimensity 7050, camera 200MP, sạc 100W',
1, NOW(), NOW(), 4.3, 145);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'MediaTek Dimensity 7050', 580000, 6.7, 'AMOLED, 120Hz', 
'Camera chính: 200MP, Ultra Wide: 8MP, Macro: 2MP', 
5000, 100, 'Android 13, Realme UI 4.0', 183);
```

### 29. Samsung Galaxy A34 5G
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('Samsung Galaxy A34 5G 8GB 128GB', 'Samsung', 7490000, 72, 167, true,
'https://cdn.tgdd.vn/Products/Images/42/301608/samsung-galaxy-a34-5g-xanh-thumb-600x600.jpg',
'Galaxy A34 5G - Dimensity 1080, màn hình 120Hz, camera 48MP OIS',
1, NOW(), NOW(), 4.2, 234);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'MediaTek Dimensity 1080', 540000, 6.6, 'Super AMOLED, 120Hz', 
'Camera chính: 48MP OIS, Ultra Wide: 8MP, Macro: 5MP', 
5000, 25, 'Android 14, One UI 6', 199);
```

### 30. OPPO A58
```sql
INSERT INTO products (name, brand, price, stock, sold, active, image_url, description, category_id, created_at, updated_at, average_rating, total_reviews)
VALUES ('OPPO A58 8GB 128GB', 'Oppo', 4990000, 98, 245, true,
'https://cdn.tgdd.vn/Products/Images/42/309816/oppo-a58-xanh-thumb-600x600.jpg',
'OPPO A58 - Helio G85, màn hình 90Hz, pin 5000mAh bền bỉ',
1, NOW(), NOW(), 4.0, 312);

INSERT INTO product_specifications (product_id, processor, antutu_score, screen_size, screen_tech, camera_info, battery_capacity, charging_speed, os, weight)
VALUES (LAST_INSERT_ID(), 'MediaTek Helio G85', 210000, 6.72, 'IPS LCD, 90Hz', 
'Camera chính: 50MP, Depth: 2MP', 
5000, 33, 'Android 13, ColorOS 13.1', 192);
```

## Lưu ý khi thêm dữ liệu:

1. **Antutu Score**: Điểm benchmark thực tế từ Antutu (tham khảo: https://www.antutu.com/en/ranking/rank1.htm)
2. **Camera Info**: Mô tả chi tiết hệ thống camera từ spec chính thức
3. **Screen Tech**: Công nghệ màn hình chính xác (OLED, AMOLED, LTPO, tần số quét)
4. **Charging Speed**: Công suất sạc nhanh tối đa (W)
5. **Weight**: Trọng lượng chính xác (gram)

## Cách chạy SQL:

1. Mở MySQL Workbench
2. Kết nối đến database `smart_phone`
3. Copy từng block SQL và chạy
4. Kiểm tra dữ liệu: `SELECT * FROM products ORDER BY id DESC LIMIT 10;`

## Kiểm tra trang So sánh:

Sau khi thêm dữ liệu, truy cập:
- http://localhost:8080/user/compare?ids=1,2,3
- Thay đổi IDs theo sản phẩm bạn vừa thêm

## Nguồn dữ liệu tham khảo:

- **Thế Giới Di Động**: https://www.thegioididong.com/dtdd
- **Antutu Benchmark**: https://www.antutu.com/en/ranking/rank1.htm
- **GSMArena**: https://www.gsmarena.com/
