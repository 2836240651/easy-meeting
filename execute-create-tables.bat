@echo off
echo 正在创建会议聊天消息分表...
echo.
echo 请确保MySQL服务正在运行
echo.

REM 请根据你的实际情况修改以下参数
set MYSQL_HOST=localhost
set MYSQL_PORT=3306
set MYSQL_USER=root
set MYSQL_PASSWORD=your_password
set MYSQL_DATABASE=easymeeting

echo 连接到数据库: %MYSQL_DATABASE%
echo.

mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% %MYSQL_DATABASE% < create_chat_message_split_tables.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo 成功创建32张会议聊天消息分表！
    echo ========================================
) else (
    echo.
    echo ========================================
    echo 创建失败，请检查MySQL连接信息
    echo ========================================
)

pause
