# 🚀 自动创建分表功能 - 快速启动指南

## 问题已解决 ✅

之前的错误：
```
Table 'easymeeting.meeting_chat_message_02' doesn't exist
```

现在系统会**自动创建**所需的分表，无需手动操作！

## 如何使用

### 方式1：直接使用（推荐）⭐

**无需任何操作！** 系统已经自动启用了分表自动创建功能。

1. 重启后端服务
2. 在会议中发送消息
3. 系统会自动：
   - 检查对应的分表是否存在
   - 如果不存在，自动创建
   - 插入消息数据

### 方式2：预创建所有分表（可选）

如果你想在系统启动前就创建好所有32张分表，可以执行：

```bash
# 在MySQL中执行
mysql -u root -p easymeeting < create_chat_message_split_tables.sql
```

或者在MySQL客户端中直接运行 `create_chat_message_split_tables.sql` 文件。

## 测试验证

### 1. 打开测试页面
```
test-auto-create-table.html
```

### 2. 测试发送消息
- 输入会议ID（如：4Q59or2mDd）
- 输入消息内容
- 点击"发送消息"
- 查看结果

### 3. 验证数据库
```sql
-- 查看已创建的分表
SHOW TABLES LIKE 'meeting_chat_message_%';

-- 查看某个分表的数据
SELECT * FROM meeting_chat_message_02;
```

## 工作原理

```
用户发送消息
    ↓
计算会议ID对应的表名（如：meeting_chat_message_02）
    ↓
检查表是否存在？
    ├─ 是 → 直接插入数据
    └─ 否 → 自动创建表 → 插入数据
```

## 分表映射示例

| 会议ID | 对应的表 |
|--------|----------|
| 4Q59or2mDd | meeting_chat_message_02 |
| test123 | meeting_chat_message_15 |
| meeting001 | meeting_chat_message_28 |

## 常见问题

### Q: 需要手动创建表吗？
**A:** 不需要！系统会自动创建。

### Q: 会影响性能吗？
**A:** 几乎没有影响。只在首次使用某个分表时会有轻微延迟（创建表），之后都是正常操作。

### Q: 如果多个请求同时创建同一张表会怎样？
**A:** 不会有问题。使用了 `CREATE TABLE IF NOT EXISTS`，并发安全。

### Q: 需要什么数据库权限？
**A:** 需要 `CREATE TABLE` 权限和查询 `information_schema` 的权限。

### Q: 模板表在哪里？
**A:** `meeting_chat_message` 表是模板表，所有分表都基于它创建。

## 下一步

1. ✅ 重启后端服务
2. ✅ 测试发送消息
3. ✅ 验证功能正常

就这么简单！🎉
