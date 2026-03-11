# 视频显示测试指南

## 修复内容

### 1. 修复了 messageSend2Type 错误
- **问题**: 前端发送 `messageSend2Type: 1`，但后端 `USER=0, GROUP=1`
- **修复**: 前端改为 `messageSend2Type: 0` (USER类型)
- **影响**: WebRTC消息现在会正确路由到 `sendMsg2User` (点对点发送)

### 2. 修复了video元素渲染问题
- **问题**: `v-if="participant.videoOpen && participant.videoRef"` 导致循环依赖
  - video元素需要 videoRef 存在才渲染
  - 但 videoRef 需要 video元素渲染后才能设置
- **修复**: 改为 `v-if="participant.videoOpen"`
- **影响**: video元素现在会在 videoOpen=true 时立即渲染

### 3. 增强了远程视频流处理
- 添加详细日志 (📺📺📺 标记)
- 自动设置 `participant.videoOpen = true` 当收到远程流时
- 添加视频播放事件监听器
- 改进重试逻辑

## 测试步骤

### 步骤1: 刷新浏览器
1. 刷新用户A的浏览器 (Ctrl+F5 强制刷新)
2. 刷新用户B的浏览器 (Ctrl+F5 强制刷新)

### 步骤2: 重新加入会议
1. 两个用户重新登录并加入会议

### 步骤3: 开启视频
1. 用户A点击视频按钮
2. 用户B点击视频按钮

### 步骤4: 检查日志

#### 前端日志 (浏览器控制台)
查找以下标记：
- `📤📤📤 WebSocket.sendMessage 被调用`
- `📤📤📤 messageSend2Type: 0` ✅ (应该是0，不是1)
- `📺📺📺 处理远程视频流添加`
- `✅✅✅ 远程视频流已设置到video元素`

#### 后端日志
查找以下标记：
- `🎯🎯🎯 收到WebRTC消息: 类型=13` (Offer)
- `🔵🔵🔵 路由到 sendMsg2User` ✅ (应该是User，不是Group)
- `🟢🟢🟢 ✅ 消息已发送到用户`

## 预期结果

### 成功标志
1. ✅ 两个用户都能看到自己的视频（本地视频）
2. ✅ 两个用户都能看到对方的视频（远程视频）
3. ✅ 视频替换了头像显示
4. ✅ 后端日志显示 "路由到 sendMsg2User"
5. ✅ 前端日志显示 "远程视频流已设置到video元素"

### 如果仍然失败
1. 复制浏览器控制台的完整日志（特别是带 📺📺📺 标记的）
2. 复制后端日志（特别是带 🎯🎯🎯 🔵🔵🔵 🟢🟢🟢 标记的）
3. 检查是否有错误信息

## 调试命令

在浏览器控制台运行：

```javascript
// 检查WebRTC状态
diagnoseAndFix()

// 检查参与者列表
console.log('参与者列表:', participants.value)

// 检查远程流
console.log('远程流:', webrtcManager.remoteStreams)

// 检查peer连接
console.log('Peer连接:', webrtcManager.peerConnections)
```

## 关键修改文件
1. `frontend/src/api/webrtc-manager.js` - messageSend2Type: 0
2. `frontend/src/views/Meeting.vue` - v-if="participant.videoOpen"
3. `frontend/src/views/Meeting.vue` - handleRemoteStreamAdded 增强
