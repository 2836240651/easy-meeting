# 后端 500 错误修复

## 问题描述

前端出现多个 API 调用失败，返回 500 错误：
- `/api/meetingReserve/getUpcomingMeetings`
- `/api/userContact/loadContactUser`
- `/api/userContact/loadContactApplyDealWithCount`

## 根本原因

在 `MeetingReserve.java` 实体类中，错误地添加了以下字段：
```java
private String userId;
private String queryUserInfo;  // 这个字段应该只在 Query 类中，不应该在实体类中
```

`queryUserInfo` 字段在实体类中被定义为 `String` 类型，但在 `MeetingReserveQuery` 中是 `Boolean` 类型，导致类型不匹配错误。

## 修复内容

### 1. 修改 `MeetingReserve.java`

移除了不应该存在的字段：
- 移除 `userId` 字段
- 移除 `queryUserInfo` 字段及其 getter/setter

保留了正确的字段：
- `nickName` - 创建者昵称（非数据库字段，用于前端显示）
- `inviteUserIds` - 被邀请的用户ID列表（非数据库字段，用于前端显示）

### 2. 重新编译

```bash
mvn clean compile -DskipTests
```

编译成功，没有错误。

## 下一步操作

需要重启后端服务才能使修复生效：

1. 停止当前运行的后端服务（端口 6099）
2. 重新启动后端服务

## 测试方法

使用 `test-backend-apis.html` 测试页面验证修复：

1. 在浏览器中打开 `test-backend-apis.html`
2. 输入有效的 token
3. 点击各个测试按钮
4. 确认所有 API 都返回成功响应

## 相关文件

- `src/main/java/com/easymeeting/entity/po/MeetingReserve.java` - 修复的实体类
- `src/main/java/com/easymeeting/entity/query/MeetingReserveQuery.java` - Query 类（包含正确的 queryUserInfo 字段）
- `test-backend-apis.html` - API 测试页面
