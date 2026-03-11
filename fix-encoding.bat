@echo off
REM 修复Spring Boot日志中文乱码
REM 设置控制台编码为UTF-8
chcp 65001

REM 设置Java环境变量
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

REM 启动Spring Boot应用
echo 正在启动Spring Boot应用...
mvn spring-boot:run

pause
