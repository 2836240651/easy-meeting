# WebSocket 重连问题修复

## 问题描述

用户报告了以下问题：
1. 离开会议后返回 Dashboard，WebSocket 没有自动重连
2. 日志显示"WebSocket已连接，跳过重复连接"，但实际连接已断开
3. 用户需要点击"快速会议"才能触发 WebSocket 重连

## 问题分析

### 根本原因

1. **连接状态检查不准确**
   - `websocket.js` 的 `connect()` 方法只检查 `this.ws.readyState === WebSocket.OPEN`
   - 没有同时检查 `this.isConnected.value` 的状态
   - 导致即使连接实际已断开，但 `readyState` 仍然显示为 OPEN

2. **状态不同步**
   - 当 WebSocket 连接异常断开时，`readyState` 可能还没有更新
   - 但 `isConnected.value` 已经被设置为 false
   - 这种状态不一致导致重连逻辑被跳过

## 修复方案

### 1. 改进连接状态检查逻辑

修改 `frontend/src/api/websocket.js` 的 `connect()` 方法：

```javascript
// 修改前
if (this.ws && this.ws.readyState === WebSocket.OPEN) {
  console.log('WebSocket已连接，跳过重复连接')
  return Promise.resolve()
}

// 修改后
if (this.ws && this.ws.readyState === WebSocket.OPEN && this.isConnected.value) {
  console.log('WebSocket已连接，跳过重复连接')
  console.log('连接状态:', {
    readyState: this.ws.readyState,
    isConnected: this.isConnected.value,
    lastHeartbeat: this.lastHeartbeatTime ? new Date(this.lastHeartbeatTime).toLocaleTimeString() : '无'
  })
  return Promise.resolve()
}

// 如果 readyState 显示已连接但 isConnected 为 false，说明连接可能已断开
if (this.ws && this.ws.readyState === WebSocket.OPEN && !this.isConnected.value) {
  console.warn('⚠️ WebSocket readyState 为 OPEN 但 isConnected 为 false，可能连接已断开')
  console.log('强制关闭旧连接并重新连接...')
  try {
    this.ws.close()
  } catch (e) {
    console.error('关闭旧连接失败:', e)
  }
  this.ws = null
}
```

### 2. 改进会议 WebSocket 断开逻辑

修改 `frontend/src/api/meeting-websocket.js` 的 `disconnect()` 方法，添加注释说明：

```javascript
disconnect() {
  console.log('断开会议WebSocket连接')
  
  // 如果在会议中，发送退出消息
  if (this.isConnected && this.currentMeetingId) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    this.sendExitMeeting(userInfo.nickName || '用户')
  }

  this.isConnected = false
  this.currentMeetingId = null
  this.currentUserId = null
  
  // 清理消息处理器
  this.messageHandlers.clear()
  
  // 注意：不要断开底层的 WebSocket 连接
  // 因为 Dashboard 还需要使用它来接收其他消息（如联系人申请、在线状态等）
  console.log('会议WebSocket已断开，但保持底层WebSocket连接用于Dashboard')
}
```

## 修复效果

1. **准确的连接状态检查**
   - 同时检查 `readyState` 和 `isConnected.value`
   - 避免状态不一致导致的重连失败

2. **自动检测并修复异常状态**
   - 当检测到 `readyState` 为 OPEN 但 `isConnected` 为 false 时
   - 自动关闭旧连接并重新建立连接

3. **详细的日志输出**
   - 输出连接状态的详细信息
   - 包括 readyState、isConnected、最后心跳时间
   - 便于调试和问题排查

## 测试建议

1. **正常流程测试**
   - 登录 → 进入会议 → 离开会议 → 返回 Dashboard
   - 检查 WebSocket 是否自动重连
   - 检查是否能正常接收消息

2. **异常情况测试**
   - 网络断开后重连
   - 长时间空闲后重新激活
   - 多次进出会议

3. **日志检查**
   - 查看浏览器控制台日志
   - 确认连接状态的变化
   - 确认重连逻辑是否正确触发

## 相关文件

- `frontend/src/api/websocket.js` - 主 WebSocket 服务
- `frontend/src/api/meeting-websocket.js` - 会议 WebSocket 服务
- `frontend/src/views/Dashboard.vue` - Dashboard 页面
- `frontend/src/views/Meeting.vue` - 会议页面

## 注意事项

1. **不要在会议结束时断开主 WebSocket**
   - Dashboard 需要保持 WebSocket 连接来接收其他消息
   - 只断开会议相关的消息处理器

2. **连接状态的双重检查**
   - 始终同时检查 `readyState` 和 `isConnected.value`
   - 避免依赖单一状态指示器

3. **心跳机制的重要性**
   - 心跳可以及时发现连接异常
   - 确保 `lastHeartbeatTime` 正确更新

## 后续优化建议

1. **添加连接质量监控**
   - 记录重连次数和频率
   - 监控心跳延迟
   - 提供连接质量指示器

2. **优化重连策略**
   - 根据网络状况动态调整重连间隔
   - 添加快速重连机制（前几次重连使用较短间隔）

3. **用户体验改进**
   - 在 UI 上显示连接状态
   - 连接异常时给予用户提示
   - 提供手动重连按钮
