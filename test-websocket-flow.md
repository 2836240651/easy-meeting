# WebSocket消息流测试

## 当前问题

用户B发送了Offer，但用户A没有收到。

**调试信息显示：**
- Peer连接状态：`have-local-offer`（已发送offer，等待answer）
- 远程流数量：0
- 连接状态：new

## 测试步骤

### 1. 在用户B的控制台查看发送的消息

```javascript
// 查看WebSocket连接状态
console.log('WebSocket状态:', meetingWsService.wsService.ws.readyState)
// 0 = CONNECTING, 1 = OPEN, 2 = CLOSING, 3 = CLOSED

// 查看是否真的发送了消息
// 刷新页面后重新加入会议，观察控制台日志
```

### 2. 在用户A的控制台查看是否收到消息

```javascript
// 运行调试
debugWebRTC()

// 查看WebSocket消息处理器
console.log('WebSocket消息处理器:', meetingWsService.messageHandlers)

// 手动监听WebRTC消息
meetingWsService.wsService.ws.addEventListener('message', (event) => {
  const data = JSON.parse(event.data)
  if (data.messageType === 13 || data.messageType === 14 || data.messageType === 15) {
    console.log('🔔 收到WebRTC消息:', data)
  }
})
```

### 3. 查看后端日志

在后端日志中搜索：
```
发送点对点消息: 类型=13
发送点对点消息: 类型=14
发送点对点消息: 类型=15
```

应该看到类似：
```
发送点对点消息: 类型=13, 发送者=2Lj7co9YQMps, 接收者=6cq7Pg48b4Rq
✅ 消息已发送到用户: 6cq7Pg48b4Rq
```

### 4. 检查用户A的WebSocket连接

在用户A的控制台：
```javascript
// 检查连接
console.log('WebSocket连接:', meetingWsService.isConnected)
console.log('WebSocket readyState:', meetingWsService.wsService.ws.readyState)
console.log('当前用户ID:', currentUserId.value)

// 检查是否在会议房间中
console.log('会议ID:', meetingId.value)
```

## 可能的问题和解决方案

### 问题1：后端没有找到用户A的WebSocket连接

**症状：** 后端日志显示 "接收用户的WebSocket连接不存在"

**原因：** 用户A的WebSocket连接没有正确建立或已断开

**解决：**
1. 用户A刷新页面重新加入会议
2. 检查用户A的网络连接
3. 查看用户A的控制台是否有WebSocket错误

### 问题2：消息类型不匹配

**症状：** 用户A收到消息但没有触发处理器

**原因：** messageType可能被转换为字符串

**解决：** 检查消息处理器注册

### 问题3：消息被过滤或丢失

**症状：** 后端发送了消息但用户A没收到

**原因：** WebSocket消息在传输过程中丢失

**解决：**
1. 检查网络连接
2. 查看浏览器Network标签的WS消息
3. 重新建立WebSocket连接

## 临时解决方案：手动触发连接

如果自动连接失败，可以手动触发：

### 在用户A的控制台：

```javascript
// 获取用户B的ID
const userBId = '2Lj7co9YQMps'

// 手动创建peer连接（作为接收方）
await webrtcManager.createPeerConnection(userBId, false)

// 等待用户B重新发送Offer
// 或者让用户B刷新页面重新加入
```

### 在用户B的控制台：

```javascript
// 获取用户A的ID
const userAId = '6cq7Pg48b4Rq'

// 获取现有的peer连接
const pc = webrtcManager.peerConnections.get(userAId)

if (pc && pc.signalingState === 'have-local-offer') {
  console.log('连接处于等待answer状态')
  console.log('请检查用户A是否收到了Offer')
  
  // 如果等待太久，可以重新创建连接
  webrtcManager.closePeerConnection(userAId)
  await webrtcManager.connectToParticipant(userAId)
}
```

## 下一步调试

请提供以下信息：

1. **用户A的控制台输出**
   - `debugWebRTC()` 的结果
   - 是否有 "收到WebRTC Offer消息" 的日志
   - 是否有任何错误

2. **用户B的控制台输出**
   - 是否有 "📤 发送Offer到" 的日志
   - 是否有 "✅ Offer已发送" 的日志
   - 完整的WebRTC相关日志

3. **后端日志**
   - 搜索 "发送点对点消息"
   - 搜索用户A和用户B的userId
   - 查看是否有警告或错误

把这些信息发给我，我会帮你精确定位问题！
