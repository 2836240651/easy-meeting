# 屏幕共享画中画模式

## 功能描述

当用户开启屏幕共享时，如果摄像头视频是开启的，摄像头画面会变成一个可拖动的小窗口（画中画），显示在屏幕共享画面的左上角。

## 实现内容

### 1. UI 结构

#### 屏幕共享视图
```vue
<div v-if="isScreenSharing" class="screen-share-view">
  <!-- 屏幕共享画面（全屏显示） -->
  <video ref="screenShareVideo" class="screen-share-video"></video>
  
  <!-- 画中画摄像头窗口（可拖动） -->
  <div 
    v-if="cameraStream"
    ref="pipCamera"
    class="pip-camera"
    :style="{ left: pipPosition.x + 'px', top: pipPosition.y + 'px' }"
    @mousedown="startDrag">
    <video ref="pipCameraVideo" class="pip-camera-video"></video>
    <div class="pip-camera-info">
      <span class="pip-camera-name">{{ userName }}</span>
    </div>
  </div>
</div>
```

#### 正常视图
```vue
<div v-else class="participants-grid">
  <!-- 参与者网格 -->
</div>
```

### 2. 状态管理

```javascript
// 视频流
const cameraStream = ref(null)  // 摄像头流（用于画中画）
const screenShareVideo = ref(null)  // 屏幕共享视频元素
const pipCameraVideo = ref(null)  // 画中画摄像头视频元素
const pipCamera = ref(null)  // 画中画容器元素

// 画中画位置和拖动
const pipPosition = ref({ x: 20, y: 20 })  // 初始位置：左上角
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
```

### 3. 拖动功能

```javascript
// 开始拖动
const startDrag = (event) => {
  isDragging.value = true
  
  // 计算鼠标相对于画中画窗口的偏移
  const rect = pipCamera.value.getBoundingClientRect()
  dragOffset.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top
  }
  
  // 添加全局事件监听
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  
  event.preventDefault()
}

// 拖动中
const onDrag = (event) => {
  if (!isDragging.value) return
  
  // 计算新位置
  let newX = event.clientX - dragOffset.value.x
  let newY = event.clientY - dragOffset.value.y
  
  // 限制在视频区域内
  const videoArea = document.querySelector('.video-area')
  if (videoArea && pipCamera.value) {
    const videoRect = videoArea.getBoundingClientRect()
    const pipRect = pipCamera.value.getBoundingClientRect()
    
    newX = Math.max(0, Math.min(newX, videoRect.width - pipRect.width))
    newY = Math.max(0, Math.min(newY, videoRect.height - pipRect.height))
  }
  
  pipPosition.value = { x: newX, y: newY }
}

// 停止拖动
const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}
```

### 4. 屏幕共享逻辑

```javascript
const shareScreen = async () => {
  // 如果当前有摄像头视频，保存为画中画流
  if (isVideoOn.value && localStream.value) {
    console.log('📹 保存当前摄像头流用于画中画')
    cameraStream.value = localStream.value
    
    // 设置画中画视频
    await nextTick()
    if (pipCameraVideo.value && cameraStream.value) {
      pipCameraVideo.value.srcObject = cameraStream.value
    }
  }
  
  // 获取屏幕共享流
  const stream = await navigator.mediaDevices.getDisplayMedia(...)
  
  // 设置屏幕共享视频
  await nextTick()
  if (screenShareVideo.value) {
    screenShareVideo.value.srcObject = stream
  }
  
  // 替换WebRTC连接的视频轨道为屏幕共享
  const videoTrack = stream.getVideoTracks()[0]
  await webrtcManager.replaceVideoTrack(videoTrack)
}
```

### 5. 停止共享逻辑

```javascript
const stopScreenShare = async () => {
  // 如果有摄像头流，恢复摄像头视频
  if (cameraStream.value) {
    console.log('🎥 恢复摄像头视频')
    
    // 恢复本地流
    localStream.value = cameraStream.value
    isVideoOn.value = true
    
    // 恢复本地视频显示
    if (localVideo.value) {
      localVideo.value.srcObject = cameraStream.value
    }
    
    // 替换WebRTC视频轨道
    const videoTrack = cameraStream.value.getVideoTracks()[0]
    await webrtcManager.replaceVideoTrack(videoTrack)
    
    // 清空画中画流
    cameraStream.value = null
  } else {
    // 没有摄像头流，关闭视频状态
    isVideoOn.value = false
    await webrtcManager.replaceVideoTrack(null)
  }
}
```

### 6. CSS 样式

```css
/* 屏幕共享视图 */
.screen-share-view {
  width: 100%;
  height: 100%;
  position: relative;
  background-color: #000000;
}

.screen-share-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* 画中画摄像头窗口 */
.pip-camera {
  position: absolute;
  width: 240px;
  height: 180px;
  background-color: #1a1a1a;
  border: 2px solid #4CAF50;
  border-radius: 8px;
  cursor: move;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.6);
  z-index: 10;
}

.pip-camera:hover {
  box-shadow: 0 6px 24px rgba(76, 175, 80, 0.4);
  border-color: #66BB6A;
}

.pip-camera:active {
  cursor: grabbing;
}

.pip-camera-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pip-camera-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  padding: 8px 12px;
}

.pip-camera-name {
  color: #ffffff;
  font-size: 12px;
  font-weight: 500;
}
```

## 用户体验

### 场景1：有摄像头视频 → 屏幕共享
1. 用户开启摄像头视频
2. 点击"共享屏幕"
3. 视图切换到屏幕共享模式
4. 摄像头画面变成左上角的小窗口
5. 小窗口可以拖动到任意位置
6. 其他用户看到屏幕共享内容

### 场景2：无摄像头视频 → 屏幕共享
1. 用户未开启摄像头
2. 点击"共享屏幕"
3. 视图切换到屏幕共享模式
4. 只显示屏幕共享，没有画中画窗口
5. 其他用户看到屏幕共享内容

### 场景3：停止屏幕共享
1. 点击"停止共享"
2. 如果之前有摄像头视频，恢复到正常视图并显示摄像头
3. 如果之前没有摄像头，恢复到正常视图并显示头像

## 功能特点

1. **画中画显示** - 摄像头视频变成小窗口，不遮挡主要内容
2. **可拖动** - 用户可以将画中画窗口拖到任意位置
3. **边界限制** - 画中画窗口不会拖出视频区域
4. **视觉反馈** - 鼠标悬停和拖动时有视觉反馈
5. **自动适配** - 根据是否有摄像头自动显示/隐藏画中画
6. **状态保持** - 停止共享后正确恢复之前的状态

## 测试步骤

1. 用户A开启摄像头视频
2. 用户A点击"共享屏幕"
3. 验证：
   - 屏幕共享全屏显示
   - 摄像头视频显示在左上角小窗口
   - 小窗口有绿色边框
   - 显示用户名称
4. 拖动画中画窗口
5. 验证：
   - 可以拖动到任意位置
   - 不会拖出视频区域
   - 鼠标悬停时边框高亮
6. 用户A点击"停止共享"
7. 验证：
   - 恢复到正常网格视图
   - 摄像头视频正常显示

## 文件修改

- `frontend/src/views/Meeting.vue` - 添加屏幕共享视图、画中画窗口、拖动逻辑和样式

## 状态
✅ 完成
