# WebSocket 连接状态监控功能

## 问题描述

WebSocket 连接异常关闭（错误代码 1006）后，系统会尝试重连，但用户无法看到连接状态，也无法手动触发重连。

## 解决方案

### 1. 改进 WebSocket 服务

**文件**: `frontend/src/api/websocket.js`

#### 1.1 添加心跳超时检测

```javascript
constructor() {
  // ... 其他属性 ...
  this.heartbeatTimeout = null // 心跳超时定时器
  this.heartbeatTimeoutDuration = 10000 // 10秒心跳超时
  this.missedHeartbeats = 0 // 错过的心跳次数
  this.maxMissedHeartbeats = 3 // 最大允许错过的心跳次数
  this.lastHeartbeatTime = null // 最后一次心跳时间
}
```

#### 1.2 改进心跳机制

- 记录每次心跳响应的时间
- 设置心跳超时定时器
- 连续错过 3 次心跳后主动关闭连接并重连

```javascript
startHeartbeat() {
  this.heartbeatInterval = setInterval(() => {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      // 发送心跳
      this.ws.send('ping')
      
      // 设置心跳超时定时器
      this.heartbeatTimeout = setTimeout(() => {
        this.missedHeartbeats++
        
        if (this.missedHeartbeats >= this.maxMissedHeartbeats) {
          // 主动关闭连接
          this.ws.close()
        }
      }, this.heartbeatTimeoutDuration)
    }
  }, this.heartbeatTimer)
}
```

#### 1.3 改进重连逻辑

- 使用递增延迟策略（第1次5秒，第2次10秒，第3次15秒...）
- 重连成功后重置重连计数
- 达到最大重连次数后通知用户

```javascript
scheduleReconnect() {
  if (this.reconnectAttempts < this.maxReconnectAttempts) {
    this.reconnectAttempts++
    const delay = this.reconnectInterval * this.reconnectAttempts // 递增延迟
    
    setTimeout(() => {
      this.connect(this.currentToken, this.currentUserId)
        .then(() => {
          this.reconnectAttempts = 0 // 重置重连计数
          this.isReconnecting = false
        })
        .catch(error => {
          this.isReconnecting = false
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.scheduleReconnect()
          } else {
            this.notifyConnectionFailed()
          }
        })
    }, delay)
  }
}
```

#### 1.4 添加连接状态查询

```javascript
getConnectionState() {
  return {
    isConnected: this.isConnected.value,
    isReconnecting: this.isReconnecting,
    reconnectAttempts: this.reconnectAttempts,
    maxReconnectAttempts: this.maxReconnectAttempts,
    readyState: this.ws?.readyState,
    missedHeartbeats: this.missedHeartbeats,
    lastHeartbeatTime: this.lastHeartbeatTime
  }
}
```

#### 1.5 添加手动重连

```javascript
manualReconnect() {
  if (this.currentToken && this.currentUserId) {
    this.reconnectAttempts = 0 // 重置重连计数
    this.shouldReconnect = true
    this.isReconnecting = false
    
    // 先断开现有连接
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    
    // 重新连接
    return this.connect(this.currentToken, this.currentUserId)
  }
}
```

### 2. 前端 UI 改进

**文件**: `frontend/src/views/Dashboard.vue`

#### 2.1 添加连接状态指示器

在用户头像右下角显示连接状态：

```vue
<div class="avatar-section" @click="activeNav = 'avatar'">
  <img :src="userAvatar" alt="User Avatar" class="user-avatar">
  <!-- WebSocket 连接状态指示器 -->
  <div 
    class="connection-indicator" 
    :class="{ 
      'connected': wsService.isConnected.value, 
      'reconnecting': connectionState.isReconnecting,
      'disconnected': !wsService.isConnected.value && !connectionState.isReconnecting
    }"
    :title="getConnectionStatusText()"
    @click.stop="handleConnectionIndicatorClick"
  >
    <span class="connection-dot"></span>
  </div>
</div>
```

#### 2.2 连接状态样式

- **绿色脉冲**：已连接
- **黄色闪烁**：重连中
- **红色**：未连接

```css
.connection-indicator.connected .connection-dot {
  background: #52c41a;
  animation: pulse 2s infinite;
}

.connection-indicator.reconnecting .connection-dot {
  background: #faad14;
  animation: blink 1s infinite;
}

.connection-indicator.disconnected .connection-dot {
  background: #ff4d4f;
}
```

#### 2.3 连接状态监控

每秒更新一次连接状态：

```javascript
const startConnectionStateMonitor = () => {
  updateConnectionState()
  
  connectionStateInterval.value = setInterval(() => {
    updateConnectionState()
  }, 1000)
}

const updateConnectionState = () => {
  const state = wsService.getConnectionState()
  connectionState.value = {
    isConnected: state.isConnected,
    isReconnecting: state.isReconnecting,
    reconnectAttempts: state.reconnectAttempts,
    maxReconnectAttempts: state.maxReconnectAttempts
  }
}
```

#### 2.4 手动重连功能

点击连接指示器可以手动触发重连：

```javascript
const handleConnectionIndicatorClick = () => {
  if (!connectionState.value.isConnected) {
    ElMessageBox.confirm(
      '检测到 WebSocket 连接断开，是否尝试重新连接？',
      '连接状态',
      {
        confirmButtonText: '重新连接',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      wsService.manualReconnect()
        .then(() => {
          ElMessage.success('重新连接成功')
        })
        .catch(error => {
          ElMessage.error('重新连接失败: ' + error.message)
        })
    })
  } else {
    ElMessage.info('WebSocket 连接正常')
  }
}
```

### 3. 修复搜索联系人功能

**问题**: `searchContact is not defined`

**解决**: 在 Dashboard.vue 中添加导入

```javascript
import { 
  getUserInfo, 
  updateUserInfo, 
  getContactList, 
  getContactApplyCount, 
  getContactApplyList, 
  dealWithContactApply, 
  getMeetingHistory, 
  meetingService, 
  searchContact,  // 添加
  applyContact    // 添加
} from '@/api/services.js'
```

## 功能特性

### 1. 自动重连

- 连接断开后自动尝试重连
- 使用递增延迟策略（5秒、10秒、15秒...）
- 最多尝试 5 次
- 重连成功后重置计数

### 2. 心跳检测

- 每 20 秒发送一次心跳
- 10 秒内未收到响应视为超时
- 连续 3 次超时后主动关闭连接并重连

### 3. 连接状态可视化

- 绿色脉冲：连接正常
- 黄色闪烁：正在重连
- 红色：连接断开

### 4. 手动重连

- 点击连接指示器可查看状态
- 连接断开时可手动触发重连
- 显示重连进度（如：重连中 2/5）

### 5. 状态监控

- 每秒更新一次连接状态
- 实时显示重连进度
- 鼠标悬停显示详细状态

## 用户体验

### 正常连接

- 绿色指示器平稳脉冲
- 鼠标悬停显示"已连接"
- 点击显示"WebSocket 连接正常"

### 连接断开

- 红色指示器
- 鼠标悬停显示"未连接"
- 点击弹出重连确认对话框

### 重连中

- 黄色指示器快速闪烁
- 鼠标悬停显示"重连中 (2/5)"
- 自动尝试重连，无需用户操作

### 重连失败

- 红色指示器
- 可手动点击重新尝试
- 显示错误提示

## 测试建议

### 测试场景 1：正常连接

1. 登录系统
2. 观察头像右下角的连接指示器
3. 应该显示绿色脉冲
4. 鼠标悬停应显示"已连接"

### 测试场景 2：网络断开

1. 断开网络连接
2. 观察连接指示器变为红色
3. 系统应自动尝试重连（黄色闪烁）
4. 恢复网络后应自动连接成功（绿色脉冲）

### 测试场景 3：手动重连

1. 在连接断开状态下
2. 点击红色连接指示器
3. 确认重连对话框
4. 观察重连过程

### 测试场景 4：重连失败

1. 关闭后端服务
2. 观察系统尝试重连 5 次
3. 5 次失败后停止重连
4. 手动点击可再次尝试

## 文件修改清单

- `frontend/src/api/websocket.js` - 改进 WebSocket 服务
- `frontend/src/views/Dashboard.vue` - 添加连接状态 UI 和监控
- `frontend/src/api/services.js` - 已有（无需修改）

## 注意事项

1. **心跳间隔**：20秒，确保在服务端 45 秒超时前发送
2. **心跳超时**：10秒，给网络延迟留出余地
3. **重连延迟**：递增策略，避免频繁重连
4. **最大重连次数**：5次，避免无限重连
5. **状态更新频率**：1秒，平衡性能和实时性
