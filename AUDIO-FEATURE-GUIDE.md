# 音频功能实现指南

## 已实现的功能

### 1. 自动获取音频流
当用户开启视频时，会同时获取音频流：
```javascript
audio: {
  echoCancellation: true,  // 回声消除
  noiseSuppression: true,  // 噪音抑制
  autoGainControl: true    // 自动增益控制
}
```

### 2. 静音/解除静音功能
- ✅ 点击静音按钮切换状态
- ✅ 控制音频轨道的 `enabled` 属性
- ✅ 静音时音频轨道仍然存在，只是禁用
- ✅ 解除静音时立即恢复音频

### 3. 音频轨道管理
- ✅ 自动检测音频轨道
- ✅ 支持动态添加/移除音频
- ✅ 音频状态与视频独立控制

## 使用方法

### 开启音视频
1. 点击"视频"按钮
2. 浏览器会请求摄像头和麦克风权限
3. 允许权限后，音视频同时开启
4. 默认状态：视频开启，音频未静音

### 静音控制
1. 点击"静音"按钮 → 麦克风静音（🔇图标）
2. 再次点击 → 解除静音（🎤图标）
3. 静音状态会显示在参与者列表中

## 技术实现

### 音频流获取
```javascript
const stream = await navigator.mediaDevices.getUserMedia({ 
  video: { ... },
  audio: {
    echoCancellation: true,  // 回声消除
    noiseSuppression: true,  // 噪音抑制
    autoGainControl: true    // 自动增益控制
  }
})
```

### 静音控制
```javascript
const toggleMute = async () => {
  if (localStream.value) {
    const audioTracks = localStream.value.getAudioTracks()
    audioTracks.forEach(track => {
      track.enabled = !isMuted.value  // 切换enabled状态
    })
    isMuted.value = !isMuted.value
  }
}
```

### WebRTC音频传输
音频轨道会自动通过WebRTC传输：
```javascript
// 添加本地流到peer连接
peerConnection.addTrack(track, localStream)

// 接收远程流
peerConnection.ontrack = (event) => {
  const remoteStream = event.streams[0]
  // remoteStream包含音频和视频轨道
}
```

## 测试步骤

### 步骤1: 刷新浏览器
强制刷新两个用户的浏览器 (Ctrl+F5)

### 步骤2: 开启音视频
1. 用户A点击视频按钮
2. 允许摄像头和麦克风权限
3. 用户B点击视频按钮
4. 允许摄像头和麦克风权限

### 步骤3: 测试音频
1. 用户A说话，用户B应该能听到
2. 用户B说话，用户A应该能听到

### 步骤4: 测试静音
1. 用户A点击静音按钮
2. 用户A说话，用户B应该听不到
3. 用户A再次点击解除静音
4. 用户A说话，用户B应该能听到

## 检查日志

### 开启音视频时
```
✅ 摄像头和麦克风权限获取成功
📹 视频轨道数: 1
🎤 音频轨道数: 1
🎤 音频轨道信息: {label: "...", enabled: true, ...}
```

### 切换静音时
```
🎤 切换静音状态，当前: false
🎤 音频轨道数: 1
🎤 设置音频轨道enabled: false 标签: ...
✅ 静音状态已更新: true
```

## 常见问题

### Q: 听不到对方声音？
A: 检查：
1. 浏览器是否允许了麦克风权限
2. 系统音量是否打开
3. 浏览器控制台是否有错误
4. WebRTC连接是否建立成功

### Q: 有回声？
A: 
- 使用耳机可以避免回声
- 代码已启用 `echoCancellation: true`
- 确保只有一个标签页在使用麦克风

### Q: 音质不好？
A: 
- 检查网络连接
- 检查麦克风质量
- 可以调整音频约束参数

### Q: 静音后仍能听到声音？
A: 
- 检查是否点击了正确的静音按钮
- 检查浏览器控制台日志
- 确认 `track.enabled` 是否正确设置

## 高级配置

### 自定义音频约束
```javascript
audio: {
  echoCancellation: true,
  noiseSuppression: true,
  autoGainControl: true,
  sampleRate: 48000,        // 采样率
  channelCount: 1,          // 单声道
  volume: 1.0               // 音量
}
```

### 音频设备选择
```javascript
// 获取音频设备列表
const devices = await navigator.mediaDevices.enumerateDevices()
const audioDevices = devices.filter(d => d.kind === 'audioinput')

// 使用特定设备
audio: {
  deviceId: { exact: selectedDeviceId }
}
```

## 下一步优化

可以考虑添加：
1. 音频设备选择器（类似摄像头选择）
2. 音量指示器（显示说话音量）
3. 音频质量设置
4. 背景噪音抑制级别调整
5. 音频录制功能
