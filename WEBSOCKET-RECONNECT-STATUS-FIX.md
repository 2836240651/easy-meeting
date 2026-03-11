# WebSocket 重连后在线状态同步问题修复

## 问题描述

用户 A 的 WebSocket 连接断开后重连成功，但用户 B 看到用户 A 仍然显示为离线状态。

## 问题分析

### 现有机制

1. **首次连接**：
   - `HandlerTokenValidation` 验证 token
   - 调用 `ChannelContextUtils.addContext()`
   - 更新 `lastLoginTime`
   - 调用 `broadcastOnlineStatusChange()` 广播在线状态

2. **断开连接**：
   - `HandlerWebSocket.channelInactive()` 被触发
   - 调用 `ChannelContextUtils.handleUserOffline()`
   - 更新 `lastOffTime`
   - 调用 `broadcastOnlineStatusChange()` 广播离线状态

3. **重连**：
   - 创建新的 WebSocket 连接
   - `HandlerTokenValidation` 再次验证 token
   - 应该调用 `addContext()` 并广播在线状态

### 可能的问题

1. **旧连接未清理**：重连时可能存在旧的 Channel 对象未完全清理
2. **时序问题**：离线和上线消息的发送顺序可能有问题
3. **Channel 替换**：新连接的 Channel 可能没有正确替换旧的 Channel

## 修复方案

### 1. 改进 `addContext` 方法

添加了以下逻辑：

```java
// 检查是否有旧的连接
Channel oldChannel = USER_CONTEXT_MAP.get(userId);
if (oldChannel != null && oldChannel != channel) {
    log.info("🟢🟢🟢 用户 {} 有旧连接，关闭旧连接: oldChannelId={}", userId, oldChannel.id());
    try {
        oldChannel.close();
    } catch (Exception e) {
        log.warn("关闭旧连接失败: {}", e.getMessage());
    }
}
```

### 2. 增强日志

在关键步骤添加详细日志：
- `addContext` 被调用时记录 userId 和 channelId
- 记录 `USER_CONTEXT_MAP` 的大小
- 记录 `lastLoginTime` 更新
- 记录在线状态广播的开始和完成
- 记录向每个联系人发送状态变更通知

### 3. 日志标记

使用 🟢🟢🟢 标记重要的在线状态相关日志，方便调试。

## 测试步骤

1. **重启后端服务**：
   ```bash
   # 停止当前运行的后端服务
   # 重新启动后端服务
   ```

2. **测试场景**：
   - 用户 A 和用户 B 都登录
   - 用户 A 断开网络（模拟网络波动）
   - 用户 B 应该看到用户 A 离线
   - 用户 A 恢复网络连接
   - 用户 B 应该看到用户 A 重新上线

3. **查看日志**：
   - 搜索 `🟢🟢🟢` 标记的日志
   - 确认 `addContext` 被调用
   - 确认旧连接被关闭
   - 确认在线状态广播完成
   - 确认联系人收到状态变更通知

## 预期日志输出

### 用户 A 重连时（后端）

```
🟢🟢🟢 addContext 被调用: userId=xxx, channelId=yyy
🟢🟢🟢 用户 xxx 有旧连接，关闭旧连接: oldChannelId=zzz
🟢🟢🟢 用户 xxx 已添加到 USER_CONTEXT_MAP，当前在线用户数: 2
🟢🟢🟢 用户 xxx 的 lastLoginTime 已更新
🟢🟢🟢 准备广播用户 xxx 的在线状态
广播用户在线状态变更: userId=xxx, onlineStatus=1
用户 xxx 有 1 个联系人，准备广播状态变更
🟢🟢🟢 发送点对点消息: 类型=21, 发送者=null, 接收者=yyy
已向联系人 yyy 发送用户 xxx 的状态变更通知
🟢🟢🟢 用户 xxx 的在线状态广播完成
```

### 用户 B 收到状态变更（前端）

```javascript
📥 WebSocket收到原始消息: {messageType: 21, messageContent: {...}}
消息类型 (messageType): 21
处理用户在线状态变更: userId=xxx, onlineStatus=1
```

## 相关文件

- `src/main/java/com/easymeeting/websocket/ChannelContextUtils.java` - 添加旧连接清理逻辑和详细日志
- `src/main/java/com/easymeeting/websocket/netty/HandlerTokenValidation.java` - Token 验证和 addContext 调用
- `src/main/java/com/easymeeting/websocket/netty/HandlerWebSocket.java` - 连接断开处理
- `frontend/src/api/websocket.js` - 前端 WebSocket 重连逻辑
- `frontend/src/views/Dashboard.vue` - 前端在线状态更新处理

## 注意事项

1. 必须重启后端服务才能使修改生效
2. 测试时注意观察后端日志，确认状态广播是否正常
3. 如果问题仍然存在，检查前端是否正确处理了 `USER_ONLINE_STATUS_CHANGE` 消息（类型 21）
