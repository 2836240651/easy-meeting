# WebRTC信令问题诊断

## 当前状态

### 用户B (2Lj7co9YQMps - 主持人)
```
✅ 本地视频流: 存在
✅ 视频播放: 成功 (1280x720)
⚠️ 信令状态: 'have-local-offer' (已发送Offer，等待Answer)
❌ 连接状态: 'new' (未建立连接)
❌ 远程流: 0
```

### 用户A (6cq7Pg48b4Rq)
```
❓ 状态未知 - 需要检查
```

## 问题诊断

### 步骤1：检查用户A是否收到Offer消息

**在用户A的浏览器控制台执行：**
```javascript
// 检查WebSocket连接
console.log('WebSocket状态:', window.meetingWsService.isConnected)
console.log('用户ID:', window.meetingWsService.currentUserId)
console.log('会议ID:', window.meetingWsService.currentMeetingId)

// 检查是否注册了WebRTC消息处理器
console.log('WebRTC管理器:', window.webrtcManager)
console.log('handleOffer方法:', typeof window.webrtcManager.handleOffer)
```

### 步骤2：检查后端日志

**搜索关键词：**
```
发送点对点消息: 类型=13
接收用户 6cq7Pg48b4Rq
```

**期望看到：**
```
发送点对点消息: 类型=13, 发送者=2Lj7co9YQMps, 接收者=6cq7Pg48b4Rq
✅ 消息已发送到用户: 6cq7Pg48b4Rq
```

### 步骤3：在用户A添加消息监听

**在用户A的浏览器控制台执行：**
```javascript
// 添加WebRTC消息监听
let offerReceived = false
window.webrtcManager.handleOffer = new Proxy(window.webrtcManager.handleOffer, {
  apply: function(target, thisArg, args) {
    offerReceived = true
    console.log('🎯 收到Offer消息！', args[0])
    return target.apply(thisArg, args)
  }
})

// 5秒后检查
setTimeout(() => {
  console.log('Offer是否收到:', offerReceived)
}, 5000)
```

### 步骤4：检查用户A是否开启了视频

**在用户A的浏览器控制台执行：**
```javascript
debugWebRTC()
```

**检查输出：**
- `本地流存在: true/false` - 如果是false，用户A需要开启视频
- `Peer连接数量: 1` - 应该有一个连接
- `signalingState` - 应该是 'stable' 或 'have-remote-offer'

## 可能的原因

### 原因1：用户A没有开启视频

**症状：**
- 用户A的 `localStream: false`
- 用户A没有调用 `toggleVideo()`

**解决：**
```javascript
// 在用户A的控制台执行
document.querySelector('.control-button:nth-child(2)')?.click()
```

### 原因2：WebSocket消息未正确转发

**症状：**
- 后端日志显示消息已发送
- 但用户A的前端没有收到

**检查：**
1. 用户A的WebSocket连接是否正常
2. 后端的 `sendMsg2User` 方法是否正确执行
3. 消息类型13是否被正确处理

### 原因3：前端消息处理器未注册

**症状：**
- WebSocket收到消息
- 但 `handleOffer` 没有被调用

**检查：**
```javascript
// 在用户A的控制台
console.log('消息处理器:', window.meetingWsService.messageHandlers)
console.log('webrtcOffer处理器:', window.meetingWsService.messageHandlers.get('webrtcOffer'))
```

## 快速修复步骤

### 方案1：确保两个用户都开启视频

1. **用户A开启视频**
2. **用户B开启视频**
3. **等待5-10秒**
4. **执行 `debugWebRTC()` 检查状态**

### 方案2：手动触发重新协商

**在用户B的控制台执行：**
```javascript
// 重置连接
window.webrtcManager.closePeerConnection('6cq7Pg48b4Rq')

// 重新建立连接
window.webrtcManager.connectToParticipant('6cq7Pg48b4Rq')
```

### 方案3：检查并修复WebSocket连接

**在用户A的控制台执行：**
```javascript
// 检查WebSocket状态
const ws = window.wsService?.ws
console.log('WebSocket readyState:', ws?.readyState)
// 0=CONNECTING, 1=OPEN, 2=CLOSING, 3=CLOSED

// 如果不是OPEN状态，重新连接
if (ws?.readyState !== 1) {
  const token = localStorage.getItem('token')
  const userId = window.meetingWsService.currentUserId
  window.wsService.connect(token, userId)
}
```

## 测试命令集合

### 在用户A执行
```javascript
// 1. 检查基本状态
console.log('=== 用户A状态检查 ===')
console.log('WebSocket连接:', window.meetingWsService.isConnected)
console.log('用户ID:', window.meetingWsService.currentUserId)
console.log('会议ID:', window.meetingWsService.currentMeetingId)
debugWebRTC()

// 2. 如果视频未开启，开启视频
if (!window.webrtcManager.localStream) {
  console.log('⚠️ 本地视频流不存在，请点击开启视频按钮')
  document.querySelector('.control-button:nth-child(2)')?.click()
}

// 3. 监听Offer消息
window.offerCount = 0
const originalHandleOffer = window.webrtcManager.handleOffer.bind(window.webrtcManager)
window.webrtcManager.handleOffer = async function(message) {
  window.offerCount++
  console.log('🎯 收到第', window.offerCount, '个Offer消息:', message)
  return await originalHandleOffer(message)
}
console.log('✅ Offer监听已设置')
```

### 在用户B执行
```javascript
// 1. 检查基本状态
console.log('=== 用户B状态检查 ===')
debugWebRTC()

// 2. 检查是否发送了Offer
const pc = window.webrtcManager.peerConnections.get('6cq7Pg48b4Rq')
if (pc) {
  console.log('Peer连接状态:', {
    signalingState: pc.signalingState,
    connectionState: pc.connectionState,
    iceConnectionState: pc.iceConnectionState
  })
  
  // 如果卡在have-local-offer，尝试重新发送
  if (pc.signalingState === 'have-local-offer') {
    console.log('⚠️ 信令状态异常，尝试重置连接...')
    window.webrtcManager.closePeerConnection('6cq7Pg48b4Rq')
    setTimeout(() => {
      window.webrtcManager.connectToParticipant('6cq7Pg48b4Rq')
    }, 1000)
  }
}
```

## 预期的正常流程

```
1. 用户A和用户B都进入会议
2. 用户A和用户B都开启视频
3. 用户B (userId较小) 发起连接
   → 创建Offer
   → 通过WebSocket发送给用户A
4. 用户A收到Offer
   → 打印: 📨 收到WebRTC Offer消息
   → 打印: 📥 处理Offer来自: 2Lj7co9YQMps
   → 创建Answer
   → 发送Answer给用户B
5. 用户B收到Answer
   → 打印: 📨 收到WebRTC Answer消息
   → 打印: 📥 处理Answer来自: 6cq7Pg48b4Rq
   → 设置远程描述
6. 双方交换ICE候选
   → 打印: 📨 收到ICE候选
   → 打印: ✅ ICE候选已添加
7. 连接建立
   → connectionState: 'connected'
   → iceConnectionState: 'connected'
   → 打印: 📺 收到远程流
8. 视频显示
   → 双方都能看到对方的视频
```

## 下一步行动

1. ✅ **在用户A执行检查命令**
2. ✅ **确保用户A开启视频**
3. ✅ **检查用户A是否收到Offer消息**
4. ✅ **查看后端日志确认消息转发**
5. ✅ **如果仍有问题，提供完整的调试输出**
