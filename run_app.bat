@echo off
echo ========================================
echo  SMARTPHONE MANAGEMENT - AUTO SETUP
echo ========================================
echo.

echo [1/3] Checking MySQL connection...
mysql -u root -p tittom343 -e "SELECT 1;" >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ MySQL connection failed!
    echo.
    echo Please:
    echo 1. Install MySQL or XAMPP
    echo 2. Start MySQL service
    echo 3. Create database 'smart_phone'
    echo 4. Set root password to 'tittom343'
    echo.
    echo See: QUICK_FIX_MYSQL.md for details
    pause
    exit /b 1
) else (
    echo ✅ MySQL connected successfully!
)

echo.
echo [2/3] Setting up database...
mysql -u root -p tittom343 < setup_database.sql
if %errorlevel% neq 0 (
    echo ❌ Database setup failed!
    pause
    exit /b 1
) else (
    echo ✅ Database setup completed!
)

echo.
echo [3/3] Starting application...
cd /d D:\Module4\case\E-commerce-Management-System
./gradlew bootRun

echo.
echo ========================================
echo  SETUP COMPLETED!
echo ========================================
echo.
echo Access your app at: http://localhost:8080
echo.
pause
