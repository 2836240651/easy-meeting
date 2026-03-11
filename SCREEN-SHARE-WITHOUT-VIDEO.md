# 无视频状态下的屏幕共享功能

## 问题描述

之前的实现中，只有在开启视频后才能共享屏幕。这是因为屏幕共享依赖于替换现有的视频轨道，如果没有视频轨道，`replaceVideoTrack()` 就无法工作。

## 解决方案

修改屏幕共享逻辑，使其能够在没有视频的情况下工作：

### 1. 检测视频状态
在 `shareScreen()` 中检测当前是否有视频：
- 如果有视频：使用 `replaceVideoTrack()` 替换轨道
- 如果没有视频：将屏幕共享流设置为本地流，并添加到 WebRTC 连接

### 2. 修改 shareScreen() 函数

```javascript
// 如果视频未开启，需要先开启视频状态（但使用屏幕共享流）
if (!isVideoOn.value) {
  console.log('📹 视频未开启，先开启视频状态以支持屏幕共享')
  isVideoOn.value = true
  
  // 将屏幕共享流设置为本地流
  localStream.value = stream
  
  // 通知WebRTC管理器设置本地流
  webrtcManager.setLocalStream(stream)
  console.log('✅ 屏幕共享流已设置为本地流')
} else {
  // 如果视频已开启，替换视频轨道
  const videoTrack = stream.getVideoTracks()[0]
  if (videoTrack) {
    await webrtcManager.replaceVideoTrack(videoTrack)
    console.log('✅ WebRTC视频轨道已替换为屏幕共享')
  }
}
```

### 3. 修改 stopScreenShare() 函数

停止屏幕共享时，需要判断之前是否有摄像头视频：

```javascript
// 记录屏幕共享前的视频状态
const wasVideoOnBeforeShare = localStream.value && 
                               localStream.value !== screenStream.value &&
                               localStream.value.getVideoTracks().length > 0

// 如果屏幕共享前有摄像头视频，恢复摄像头
if (wasVideoOnBeforeShare) {
  // 恢复摄像头视频
} else {
  // 屏幕共享前没有视频，关闭视频状态
  isVideoOn.value = false
  localStream.value = null
  
  // 移除所有WebRTC连接的视频轨道
  await webrtcManager.replaceVideoTrack(null)
}
```

### 4. 增强 replaceVideoTrack() 方法

修改 `webrtc-manager.js` 中的 `replaceVideoTrack()` 方法：

```javascript
if (videoSender) {
  // 如果有视频发送器，替换轨道
  await videoSender.replaceTrack(newTrack)
} else if (newTrack) {
  // 如果没有视频发送器但有新轨道，添加轨道
  peerConnection.addTrack(newTrack, this.localStream || new MediaStream([newTrack]))
  
  // 添加轨道后需要重新协商
  if (peerConnection.signalingState === 'stable') {
    await this.createAndSendOffer(userId, peerConnection)
  }
} else {
  // 没有视频发送器且新轨道为null，无需操作
}
```

## 工作流程

### 场景1：无视频 → 屏幕共享 → 停止共享
1. 用户进入会议，视频关闭（显示头像）
2. 用户点击"共享屏幕"
3. 系统检测到 `isVideoOn = false`
4. 将屏幕共享流设置为本地流
5. 开启视频状态 `isVideoOn = true`
6. 添加视频轨道到所有 WebRTC 连接
7. 其他用户看到屏幕共享
8. 用户点击"停止共享"
9. 系统检测到之前没有摄像头视频
10. 关闭视频状态 `isVideoOn = false`
11. 移除所有视频轨道
12. 恢复显示头像

### 场景2：有视频 → 屏幕共享 → 停止共享
1. 用户进入会议，开启视频
2. 用户点击"共享屏幕"
3. 系统检测到 `isVideoOn = true`
4. 使用 `replaceVideoTrack()` 替换视频轨道
5. 其他用户看到屏幕共享
6. 用户点击"停止共享"
7. 系统检测到之前有摄像头视频
8. 使用 `replaceVideoTrack()` 恢复摄像头轨道
9. 其他用户重新看到摄像头画面

## 优势

1. **灵活性** - 无论是否开启视频都能共享屏幕
2. **用户体验** - 不需要先开启视频才能共享屏幕
3. **状态管理** - 正确记录和恢复之前的视频状态
4. **带宽优化** - 不需要视频的会议可以只共享屏幕

## 测试步骤

### 测试1：无视频状态下共享屏幕
1. 用户A和用户B进入会议（视频默认关闭）
2. 用户A点击"共享屏幕"
3. 选择要共享的屏幕
4. 验证：
   - 用户A看到自己的屏幕共享
   - 用户B看到用户A的屏幕共享
   - 按钮显示"停止共享"
5. 用户A点击"停止共享"
6. 验证：
   - 用户A恢复显示头像
   - 用户B看到用户A的头像
   - 按钮显示"共享屏幕"

### 测试2：有视频状态下共享屏幕
1. 用户A开启视频
2. 用户A点击"共享屏幕"
3. 验证：用户B看到屏幕共享
4. 用户A点击"停止共享"
5. 验证：用户B重新看到用户A的摄像头画面

## 文件修改

- `frontend/src/views/Meeting.vue` - 修改 `shareScreen()` 和 `stopScreenShare()` 函数
- `frontend/src/api/webrtc-manager.js` - 增强 `replaceVideoTrack()` 方法

## 状态
✅ 完成
