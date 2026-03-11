# WebRTC故障排查指南

## 问题：用户B看不到用户A的视频

### 步骤1：确认基本连接

在**用户B**的浏览器控制台运行：

```javascript
// 检查WebSocket连接
console.log('1. WebSocket连接:', meetingWsService.isConnected)

// 检查是否看到用户A
console.log('2. 参与者列表:', participants.value.map(p => ({
  userId: p.userId,
  name: p.name,
  videoOpen: p.videoOpen
})))

// 检查peer连接
console.log('3. Peer连接数:', webrtcManager.peerConnections.size)

// 检查远程流
console.log('4. 远程流数:', webrtcManager.remoteStreams.size)
```

**预期结果：**
- WebSocket连接: true
- 参与者列表: 应该包含用户A
- Peer连接数: 应该 >= 1
- 远程流数: 应该 >= 1（如果用户A开启了视频）

### 步骤2：检查详细连接状态

```javascript
// 查看每个peer连接的详细状态
webrtcManager.peerConnections.forEach((pc, userId) => {
  console.log(`Peer连接 [${userId}]:`, {
    connectionState: pc.connectionState,
    iceConnectionState: pc.iceConnectionState,
    signalingState: pc.signalingState,
    localDescription: pc.localDescription ? 'exists' : 'null',
    remoteDescription: pc.remoteDescription ? 'exists' : 'null'
  })
  
  // 查看发送者（本地轨道）
  console.log(`  发送者:`, pc.getSenders().map(s => ({
    track: s.track ? `${s.track.kind} - ${s.track.label}` : 'null'
  })))
  
  // 查看接收者（远程轨道）
  console.log(`  接收者:`, pc.getReceivers().map(r => ({
    track: r.track ? `${r.track.kind} - ${r.track.label}` : 'null'
  })))
})
```

**预期结果：**
- connectionState: 'connected'
- iceConnectionState: 'connected' 或 'completed'
- signalingState: 'stable'
- localDescription: 'exists'
- remoteDescription: 'exists'
- 接收者应该有视频轨道

### 步骤3：检查视频元素

```javascript
// 查看participants中的videoRef
participants.value.forEach(p => {
  console.log(`参与者 [${p.name}]:`, {
    userId: p.userId,
    videoOpen: p.videoOpen,
    hasVideoRef: !!p.videoRef,
    videoRefSrcObject: p.videoRef?.srcObject ? 'exists' : 'null'
  })
})
```

### 步骤4：手动设置视频流（临时测试）

如果远程流存在但video元素没有显示，尝试手动设置：

```javascript
// 获取用户A的userId（从participants中查看）
const userAId = 'USER_A_ID_HERE'  // 替换为实际的userId

// 获取远程流
const remoteStream = webrtcManager.remoteStreams.get(userAId)
console.log('远程流:', remoteStream)

if (remoteStream) {
  // 查找对应的参与者
  const participant = participants.value.find(p => p.userId === userAId)
  console.log('参与者:', participant)
  
  if (participant && participant.videoRef) {
    // 手动设置srcObject
    participant.videoRef.srcObject = remoteStream
    console.log('✅ 已手动设置视频流')
  } else {
    console.log('❌ 找不到video元素引用')
  }
} else {
  console.log('❌ 找不到远程流')
}
```

## 常见问题和解决方案

### 问题A：Peer连接数为0

**原因：** WebRTC连接没有建立

**检查：**
1. 查看控制台是否有 "🔗 创建Peer连接" 的日志
2. 查看是否有JavaScript错误
3. 确认用户A和用户B都在同一个会议中

**解决：**
```javascript
// 手动触发连接建立
const userAId = 'USER_A_ID_HERE'
await webrtcManager.connectToParticipant(userAId)
```

### 问题B：连接状态为connecting或new

**原因：** ICE候选交换失败或信令交换未完成

**检查：**
1. 查看是否有 "📨 收到WebRTC Offer/Answer" 的日志
2. 查看是否有 "🧊 发送ICE候选" 的日志
3. 检查后端日志是否有 "发送点对点消息"

**解决：**
- 等待几秒钟，ICE连接需要时间
- 检查网络连接
- 查看后端日志确认消息是否发送

### 问题C：远程流数为0

**原因：** 对方没有发送视频流或ontrack事件未触发

**检查：**
1. 确认用户A已经开启视频
2. 查看是否有 "📺 收到远程流" 的日志
3. 检查peer连接的接收者是否有轨道

**解决：**
```javascript
// 检查接收者
webrtcManager.peerConnections.forEach((pc, userId) => {
  const receivers = pc.getReceivers()
  console.log(`用户 ${userId} 的接收者:`, receivers)
  receivers.forEach(r => {
    if (r.track) {
      console.log(`  轨道: ${r.track.kind}, enabled: ${r.track.enabled}, readyState: ${r.track.readyState}`)
    }
  })
})
```

### 问题D：远程流存在但video元素不显示

**原因：** video元素的srcObject未设置或videoRef未绑定

**检查：**
```javascript
// 检查video元素
const videoElements = document.querySelectorAll('video')
console.log('页面上的video元素数量:', videoElements.length)
videoElements.forEach((video, index) => {
  console.log(`Video ${index}:`, {
    srcObject: video.srcObject ? 'exists' : 'null',
    muted: video.muted,
    autoplay: video.autoplay,
    readyState: video.readyState
  })
})
```

**解决：**
- 确保v-if条件正确
- 确保ref绑定正确
- 手动设置srcObject（见步骤4）

## 完整调试流程

1. **用户A创建会议并开启视频**
2. **用户B加入会议**
3. **在用户B的控制台运行：**

```javascript
// 完整调试信息
console.log('=== 完整调试信息 ===')
console.log('1. WebSocket:', meetingWsService.isConnected)
console.log('2. 当前用户ID:', currentUserId.value)
console.log('3. 会议ID:', meetingId.value)
console.log('4. 参与者数:', participants.value.length)
console.log('5. Peer连接数:', webrtcManager.peerConnections.size)
console.log('6. 远程流数:', webrtcManager.remoteStreams.size)
console.log('7. 本地流:', webrtcManager.localStream ? 'exists' : 'null')

// 详细信息
debugWebRTC()
```

4. **用户B开启视频**
5. **再次运行调试命令**
6. **检查是否能看到用户A的视频**

## 如果还是不行

请提供以下信息：

1. **用户B控制台的完整输出**（特别是红色错误）
2. **debugWebRTC()的输出**
3. **后端日志**（搜索用户B的userId）
4. **网络标签**（F12 -> Network -> WS，查看WebSocket消息）

把这些信息发给我，我会帮你找出问题。
