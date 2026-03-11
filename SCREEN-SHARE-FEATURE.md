# 屏幕共享功能实现完成

## 实现内容

### 1. webrtc-manager.js 新增方法
- ✅ `replaceVideoTrack(newTrack)` - 替换所有peer连接的视频轨道
  - 遍历所有peer连接
  - 查找视频发送器（video sender）
  - 调用 `sender.replaceTrack()` 替换轨道
  - 支持从摄像头切换到屏幕共享，或从屏幕共享切换回摄像头

### 2. Meeting.vue 功能
- ✅ `shareScreen()` - 开启屏幕共享
  - 使用 `navigator.mediaDevices.getDisplayMedia()` 获取屏幕流
  - 浏览器会显示原生UI让用户选择屏幕/窗口/标签页
  - 替换本地视频显示
  - 调用 `webrtcManager.replaceVideoTrack()` 更新所有WebRTC连接
  - 监听用户停止共享事件

- ✅ `stopScreenShare()` - 停止屏幕共享
  - 停止屏幕共享流的所有轨道
  - 恢复摄像头视频（如果视频是开启状态）
  - 调用 `webrtcManager.replaceVideoTrack()` 恢复摄像头轨道

- ✅ UI更新
  - 按钮文本动态显示："共享屏幕" / "停止共享"
  - 共享时按钮高亮（active样式）

## 功能特点

1. **浏览器原生UI** - 使用 `getDisplayMedia()` 让浏览器显示选择界面
2. **无缝切换** - 使用 `replaceTrack()` 实现平滑切换，不需要重新协商
3. **自动恢复** - 用户点击浏览器停止按钮时自动恢复摄像头
4. **音频支持** - 支持共享系统音频（如果浏览器和系统支持）
5. **状态管理** - 正确管理 `isScreenSharing` 状态

## 测试步骤

1. 两个用户加入会议并开启视频
2. 用户A点击"共享屏幕"按钮
3. 浏览器显示选择界面，选择要共享的屏幕/窗口
4. 用户B应该能看到用户A的屏幕共享内容
5. 用户A点击"停止共享"或浏览器的停止按钮
6. 用户B应该重新看到用户A的摄像头画面

## 技术细节

### replaceTrack() vs renegotiation
- `replaceTrack()` - 快速、无缝，不需要重新协商SDP
- 适用于同类型轨道替换（视频→视频）
- 不会触发 `onnegotiationneeded` 事件

### getDisplayMedia() 参数
```javascript
{
  video: {
    cursor: 'always',        // 显示鼠标光标
    displaySurface: 'monitor' // 优先选择整个屏幕
  },
  audio: {
    echoCancellation: true,
    noiseSuppression: true,
    autoGainControl: true
  }
}
```

## 文件修改

- `frontend/src/api/webrtc-manager.js` - 新增 `replaceVideoTrack()` 方法
- `frontend/src/views/Meeting.vue` - 屏幕共享按钮UI更新

## 状态
✅ 完成
