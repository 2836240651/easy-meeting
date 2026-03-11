# WebRTC调试命令

## 在用户B的浏览器控制台运行：

```javascript
// 1. 检查基本状态
debugWebRTC()

// 2. 检查WebSocket连接
console.log('WebSocket连接:', meetingWsService.isConnected)

// 3. 检查参与者列表
console.log('参与者列表:', participants.value)

// 4. 检查peer连接
console.log('Peer连接数:', webrtcManager.peerConnections.size)
webrtcManager.peerConnections.forEach((pc, userId) => {
  console.log('Peer连接 -', userId, ':', {
    connectionState: pc.connectionState,
    iceConnectionState: pc.iceConnectionState,
    signalingState: pc.signalingState
  })
})

// 5. 检查远程流
console.log('远程流数:', webrtcManager.remoteStreams.size)
webrtcManager.remoteStreams.forEach((stream, userId) => {
  console.log('远程流 -', userId, ':', stream.getTracks())
})

// 6. 检查本地流
console.log('本地流:', webrtcManager.localStream)
if (webrtcManager.localStream) {
  console.log('本地流轨道:', webrtcManager.localStream.getTracks())
}
```

## 同时查看浏览器控制台的所有日志

特别注意：
- 是否有 "🔗 创建Peer连接" 的日志
- 是否有 "📨 收到WebRTC Offer/Answer" 的日志
- 是否有 "📺 收到远程流" 的日志
- 是否有任何错误信息（红色）

## 查看后端日志

在后端日志中搜索：
- "发送点对点消息"
- "WebSocket连接已建立"
- 用户B的userId

把这些信息发给我，我会帮你找出问题。
