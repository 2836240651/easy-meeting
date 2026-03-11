# WebSocket 无限重连策略

## 问题

之前的重连策略有一个严重问题：
- 最多尝试 5 次重连
- 如果用户断网超过 5 分钟，5 次重连都会失败
- 之后就不会再尝试重连，用户必须手动刷新页面

这对用户体验非常不好，特别是在以下场景：
- 长时间网络不稳定
- 服务器维护时间较长
- 用户电脑休眠后恢复

## 解决方案

### 1. 无限重连

移除最大重连次数限制，改为无限重连：
```javascript
this.maxReconnectAttempts = Infinity // 无限重连
```

### 2. 指数退避算法

使用指数退避算法计算重连延迟，避免频繁重连消耗资源：

```javascript
// 延迟 = min(基础间隔 * 2^(尝试次数-1), 最大间隔)
const exponentialDelay = baseReconnectInterval * Math.pow(2, reconnectAttempts - 1)
const delay = Math.min(exponentialDelay, maxReconnectInterval)
```

**重连时间表**：
| 尝试次数 | 计算公式 | 延迟时间 |
|---------|---------|---------|
| 1 | 5000 * 2^0 | 5 秒 |
| 2 | 5000 * 2^1 | 10 秒 |
| 3 | 5000 * 2^2 | 20 秒 |
| 4 | 5000 * 2^3 | 40 秒 |
| 5 | 5000 * 2^4 | 60 秒 (达到最大值) |
| 6+ | 5000 * 2^5+ | 60 秒 (保持最大值) |

### 3. 配置参数

```javascript
this.baseReconnectInterval = 5000   // 基础重连间隔 5秒
this.maxReconnectInterval = 60000   // 最大重连间隔 60秒
```

**优势**：
- 初期快速重连（5秒、10秒、20秒）
- 长期断网时避免频繁请求（最多每 60 秒一次）
- 节省客户端和服务器资源
- 一旦网络恢复，立即重连成功

## 使用场景对比

### 场景 1：短暂网络波动（10秒）
- 第 1 次重连（5秒后）：失败
- 第 2 次重连（10秒后）：成功 ✅
- 总耗时：15秒

### 场景 2：中等时长断网（5分钟）
- 第 1-4 次重连：失败
- 第 5 次重连（约 75 秒后）：失败
- 第 6 次重连（约 135 秒后）：失败
- 第 7 次重连（约 195 秒后）：失败
- 第 8 次重连（约 255 秒后）：失败
- 第 9 次重连（约 315 秒后）：成功 ✅
- 总耗时：约 5 分钟

### 场景 3：长时间断网（30分钟）
- 持续每 60 秒重连一次
- 第 30 次重连（约 30 分钟后）：成功 ✅
- 用户无需任何操作

### 场景 4：旧策略（最多 5 次）
- 第 1-5 次重连：失败
- 停止重连 ❌
- 用户必须手动刷新页面

## 代码修改

### WebSocketService 构造函数

```javascript
constructor() {
  // ... 其他配置
  this.reconnectAttempts = 0
  this.maxReconnectAttempts = Infinity // 无限重连
  this.baseReconnectInterval = 5000 // 基础重连间隔 5秒
  this.maxReconnectInterval = 60000 // 最大重连间隔 60秒
  this.reconnectTimeoutId = null // 重连定时器ID
}
```

### scheduleReconnect 方法

```javascript
scheduleReconnect() {
  if (this.isReconnecting || !this.shouldReconnect) {
    return
  }

  this.isReconnecting = true
  this.reconnectAttempts++
  
  // 使用指数退避算法计算延迟
  const exponentialDelay = this.baseReconnectInterval * Math.pow(2, this.reconnectAttempts - 1)
  const delay = Math.min(exponentialDelay, this.maxReconnectInterval)
  
  console.log(`⏳ 计划重连 (第 ${this.reconnectAttempts} 次) 在 ${delay}ms 后`)
  
  this.reconnectTimeoutId = setTimeout(() => {
    if (this.shouldReconnect && this.currentToken && this.currentUserId) {
      this.connect(this.currentToken, this.currentUserId)
        .then(() => {
          console.log('✅ 重连成功')
          this.reconnectAttempts = 0 // 重置重连计数
          this.isReconnecting = false
        })
        .catch(error => {
          console.error(`❌ 第 ${this.reconnectAttempts} 次重连失败:`, error)
          this.isReconnecting = false
          // 继续尝试重连（无限重连）
          this.scheduleReconnect()
        })
    }
  }, delay)
}
```

### cancelReconnect 方法（新增）

```javascript
cancelReconnect() {
  if (this.reconnectTimeoutId) {
    clearTimeout(this.reconnectTimeoutId)
    this.reconnectTimeoutId = null
  }
  this.isReconnecting = false
  console.log('⏹️ 已取消重连')
}
```

### disconnect 方法

```javascript
disconnect() {
  console.log('主动断开WebSocket连接')
  this.shouldReconnect = false
  this.cancelReconnect() // 取消任何待处理的重连
  this.stopHeartbeat()
  // ... 其他清理逻辑
}
```

## UI 显示更新

### 连接状态文本

**之前**：`重连中 (3/5)` - 显示当前次数和最大次数

**现在**：`重连中 (第 3 次)` - 只显示当前次数，不显示最大次数

### 连接指示器

- 🟢 绿色脉冲：已连接
- 🟡 黄色闪烁：重连中（显示第几次）
- 🔴 红色：未连接

## 注意事项

1. **用户主动断开**：
   - 调用 `disconnect()` 方法会停止自动重连
   - 用户登出时会主动断开连接

2. **资源消耗**：
   - 指数退避算法确保不会频繁请求
   - 最大间隔 60 秒，对服务器影响很小

3. **重连成功后**：
   - 重连计数器重置为 0
   - 下次断开时从 5 秒开始

4. **浏览器标签页关闭**：
   - WebSocket 会自动断开
   - 不会继续尝试重连

## 测试场景

1. **短暂断网**：
   - 断开网络 10 秒
   - 观察自动重连成功

2. **长时间断网**：
   - 断开网络 10 分钟
   - 观察持续重连（每 60 秒一次）
   - 恢复网络后立即连接成功

3. **服务器重启**：
   - 重启后端服务
   - 观察客户端自动重连

4. **电脑休眠**：
   - 电脑休眠 30 分钟
   - 唤醒后自动重连成功

## 相关文件

- `frontend/src/api/websocket.js` - WebSocket 服务和重连逻辑
- `frontend/src/views/Dashboard.vue` - 连接状态显示和用户交互

## 优势总结

✅ 无需用户手动刷新页面
✅ 适应各种网络环境
✅ 节省服务器资源
✅ 更好的用户体验
✅ 自动恢复连接
