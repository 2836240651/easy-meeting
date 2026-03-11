# Emoji 表情支持修复

## 问题描述

在会议聊天中发送 Emoji 表情时，后端报错：

```
java.sql.SQLException: Incorrect string value: '\xF0\x9F\x98\x94' for column 'message_content' at row 1
```

## 原因分析

这是一个 MySQL 字符集问题：

1. **UTF-8 vs UTF8MB4**：
   - MySQL 的 `utf8` 字符集只支持最多 3 字节的 UTF-8 字符
   - Emoji 表情是 4 字节的 UTF-8 字符（如 😔 = `\xF0\x9F\x98\x94`）
   - 需要使用 `utf8mb4` 字符集才能支持 4 字节字符

2. **影响范围**：
   - `meeting_chat_message_00` 到 `meeting_chat_message_99` 共 100 张分表
   - 所有表的 `message_content` 字段都需要支持 Emoji

## 解决方案

### 方法 1：执行 SQL 脚本（推荐）

1. 打开 MySQL 客户端或数据库管理工具
2. 连接到 `easymeeting` 数据库
3. 执行 `fix-emoji-support.sql` 脚本

```bash
mysql -u root -p easymeeting < fix-emoji-support.sql
```

### 方法 2：手动执行 SQL

如果只想修改部分表（如当前使用的表），可以手动执行：

```sql
-- 修改数据库默认字符集
ALTER DATABASE easymeeting CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 修改特定的聊天消息表（例如 meeting_chat_message_23）
ALTER TABLE meeting_chat_message_23 CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 执行步骤

1. **备份数据库**（重要！）
   ```bash
   mysqldump -u root -p easymeeting > easymeeting_backup_$(date +%Y%m%d).sql
   ```

2. **执行修复脚本**
   ```bash
   mysql -u root -p easymeeting < fix-emoji-support.sql
   ```

3. **验证修改**
   ```sql
   SELECT TABLE_NAME, TABLE_COLLATION 
   FROM information_schema.TABLES 
   WHERE TABLE_SCHEMA = 'easymeeting' 
   AND TABLE_NAME LIKE 'meeting_chat_message_%';
   ```
   
   应该看到所有表的 `TABLE_COLLATION` 都是 `utf8mb4_unicode_ci`

4. **测试 Emoji**
   - 重启后端服务（如果需要）
   - 在会议聊天中发送 Emoji 表情
   - 验证是否能正常发送和显示

## 注意事项

1. **执行时间**：
   - 如果表中有大量数据，转换可能需要一些时间
   - 建议在低峰期执行

2. **连接配置**：
   - 确保 `application.yml` 中的数据库连接 URL 包含字符集参数：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/easymeeting?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
   ```

3. **兼容性**：
   - MySQL 5.5.3+ 支持 `utf8mb4`
   - 如果使用更早版本的 MySQL，需要升级

## 其他可能需要修改的表

如果其他表也需要存储 Emoji（如用户昵称、会议名称等），也需要转换：

```sql
-- 用户信息表
ALTER TABLE user_info CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 会议信息表
ALTER TABLE meeting_info CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 会议预约表
ALTER TABLE meeting_reserve CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 通知表
ALTER TABLE user_notification CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 验证方法

### 1. 查看表字符集
```sql
SHOW CREATE TABLE meeting_chat_message_23;
```

应该看到：
```sql
CREATE TABLE `meeting_chat_message_23` (
  ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2. 查看字段字符集
```sql
SHOW FULL COLUMNS FROM meeting_chat_message_23 WHERE Field = 'message_content';
```

应该看到 `Collation` 列显示 `utf8mb4_unicode_ci`

### 3. 测试插入 Emoji
```sql
INSERT INTO meeting_chat_message_23 (message_id, meeting_id, message_type, message_content, send_user_id, send_user_nick_name, send_time, receive_type, status)
VALUES ('test123', 'test', 1, '测试 Emoji 😀😁😂🤣😃😄😅😆', 'user1', 'Test User', NOW(), 1, 1);
```

如果执行成功，说明已支持 Emoji。

## 常见问题

### Q1: 执行脚本时报错 "Unknown database"
**A**: 确保已连接到正确的数据库：
```bash
mysql -u root -p
USE easymeeting;
SOURCE fix-emoji-support.sql;
```

### Q2: 修改后仍然无法存储 Emoji
**A**: 检查以下几点：
1. 数据库连接 URL 是否包含 `characterEncoding=utf8mb4`
2. 是否重启了后端服务
3. 是否修改了正确的表（检查分表编号）

### Q3: 修改会影响现有数据吗？
**A**: 不会。`CONVERT TO CHARACTER SET` 会保留现有数据，只是改变字符集。但建议先备份数据库。

### Q4: 需要修改代码吗？
**A**: 不需要。Java 和 MyBatis 会自动处理 UTF-8 编码，只需要确保数据库连接 URL 正确即可。

## 总结

执行 `fix-emoji-support.sql` 脚本后，所有聊天消息表都将支持 Emoji 表情。这是一个一次性的数据库升级操作，完成后就可以正常使用 Emoji 了。

## 相关文件

- `fix-emoji-support.sql` - 修复脚本
- `src/main/resources/application.yml` - 数据库连接配置
- `EMOJI-SUPPORT-FIX.md` - 本文档
