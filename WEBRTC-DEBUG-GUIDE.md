# WebRTC视频流问题诊断指南

## 当前问题分析

根据调试信息，发现以下问题：

### 1. 用户A（主持人 6cq7Pg48b4Rq）
- ❌ **本地视频流不存在** (`localStream: false`)
- ✅ 信令状态正常 (`signalingState: 'stable'`)
- ❌ ICE连接未建立 (`iceConnectionState: 'new'`)
- ❌ 没有开启视频

### 2. 用户B（参与者 2Lj7co9YQMps）
- ✅ 本地视频流存在 (`localStream: true`)
- ⚠️ 信令状态异常 (`signalingState: 'have-local-offer'`) - 已发送Offer但未收到Answer
- ❌ ICE连接未建立 (`iceConnectionState: 'new'`)
- ✅ 已开启视频

### 3. 核心问题
1. **用户A没有开启视频** - 这是最主要的问题！
2. **信令交换未完成** - 用户B发送了Offer，但用户A没有响应Answer
3. **ICE候选无法使用** - 因为信令交换未完成

## 解决方案

### 步骤1：确保两个用户都开启视频

**在用户A的浏览器控制台执行：**
```javascript
// 检查视频状态
console.log('视频开启状态:', window.meetingVue?.isVideoOn)

// 如果视频未开启，手动开启
if (!window.meetingVue?.isVideoOn) {
  console.log('视频未开启，正在开启...')
  // 点击视频按钮或手动调用
  document.querySelector('.control-button:nth-child(2)')?.click()
}
```

### 步骤2：检查WebRTC连接状态

**在两个用户的浏览器控制台都执行：**
```javascript
debugWebRTC()
```

**期望看到：**
- 用户A和用户B都有 `localStream: true`
- 双方的 `signalingState` 都是 `'stable'`
- `iceConnectionState` 应该是 `'connected'` 或 `'completed'`

### 步骤3：检查后端日志

**搜索关键词：**
```
发送点对点消息: 类型=13
发送点对点消息: 类型=14
发送点对点消息: 类型=15
```

**期望看到的消息流：**
```
1. 用户B → 用户A: Offer (类型=13)
2. 用户A → 用户B: Answer (类型=14)
3. 双方交换: ICE候选 (类型=15)
```

### 步骤4：检查前端WebSocket消息接收

**在用户A的浏览器控制台执行：**
```javascript
// 检查是否收到WebRTC消息
window.webrtcMessageCount = 0
window.originalWebrtcHandler = window.webrtcManager.handleOffer
window.webrtcManager.handleOffer = function(...args) {
  window.webrtcMessageCount++
  console.log('🎯 收到第', window.webrtcMessageCount, '个Offer消息')
  return window.originalWebrtcHandler.apply(this, args)
}
```

## 测试流程

### 完整测试步骤

1. **用户A（主持人）操作：**
   ```
   1. 进入会议
   2. 点击"开启视频"按钮
   3. 确认摄像头权限
   4. 在控制台执行: debugWebRTC()
   5. 确认 localStream: true
   ```

2. **用户B（参与者）操作：**
   ```
   1. 进入会议
   2. 点击"开启视频"按钮
   3. 确认摄像头权限
   4. 在控制台执行: debugWebRTC()
   5. 确认 localStream: true
   ```

3. **等待5-10秒，让WebRTC连接建立**

4. **再次检查状态：**
   ```javascript
   // 在两个用户的控制台都执行
   debugWebRTC()
   ```

5. **期望结果：**
   ```
   用户A:
   - Peer连接数量: 1
   - 远程流数量: 1
   - 用户 2Lj7co9YQMps: {
       connectionState: 'connected',
       iceConnectionState: 'connected',
       signalingState: 'stable'
     }
   
   用户B:
   - Peer连接数量: 1
   - 远程流数量: 1
   - 用户 6cq7Pg48b4Rq: {
       connectionState: 'connected',
       iceConnectionState: 'connected',
       signalingState: 'stable'
     }
   ```

## 常见问题排查

### 问题1：用户A没有收到Offer消息

**检查：**
```javascript
// 在用户A的控制台
console.log('WebSocket连接状态:', window.meetingWsService.isConnected)
console.log('当前用户ID:', window.meetingWsService.currentUserId)
console.log('会议ID:', window.meetingWsService.currentMeetingId)
```

**解决：**
- 确保WebSocket连接正常
- 检查后端是否正确转发消息

### 问题2：信令状态卡在 'have-local-offer'

**原因：**
- 用户A没有响应Offer
- 用户A的WebSocket消息处理器未正确注册

**检查：**
```javascript
// 在用户A的控制台
console.log('WebRTC消息处理器:', window.webrtcManager.wsService)
console.log('是否注册了Offer处理器:', typeof window.webrtcManager.handleOffer)
```

### 问题3：ICE连接失败

**原因：**
- STUN服务器无法访问
- 网络防火墙阻止

**检查：**
```javascript
// 测试STUN服务器
const pc = new RTCPeerConnection({
  iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
})
pc.onicecandidate = (e) => {
  if (e.candidate) {
    console.log('✅ ICE候选生成成功:', e.candidate.candidate)
  }
}
pc.createOffer().then(offer => pc.setLocalDescription(offer))
```

## 快速修复命令

### 重置WebRTC连接

**在任一用户的控制台执行：**
```javascript
// 关闭所有连接
window.webrtcManager.closeAllConnections()

// 重新建立连接
window.webrtcManager.connectToAllParticipants(window.meetingVue.participants)
```

### 强制重新协商

**在用户B的控制台执行：**
```javascript
const targetUserId = '6cq7Pg48b4Rq' // 用户A的ID
const pc = window.webrtcManager.peerConnections.get(targetUserId)
if (pc) {
  window.webrtcManager.createAndSendOffer(targetUserId, pc)
}
```

## 下一步行动

1. ✅ **确保用户A开启视频** - 这是最关键的！
2. 检查双方的 `debugWebRTC()` 输出
3. 检查后端日志中的WebRTC消息流
4. 如果仍有问题，提供完整的调试输出

## 预期的正常流程

```
1. 用户A进入会议，开启视频
   → localStream 创建成功
   → 等待其他用户

2. 用户B进入会议，开启视频
   → localStream 创建成功
   → 比较userId: 2Lj7co9YQMps < 6cq7Pg48b4Rq
   → 用户B发起连接（发送Offer）

3. 用户A收到Offer
   → 创建Answer
   → 发送Answer给用户B

4. 用户B收到Answer
   → 设置远程描述
   → 信令交换完成

5. 双方交换ICE候选
   → ICE连接建立
   → 视频流开始传输

6. 双方都能看到对方的视频
   ✅ 成功！
```
