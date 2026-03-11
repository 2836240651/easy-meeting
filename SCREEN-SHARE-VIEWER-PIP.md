# 屏幕共享观看者画中画模式

## 功能描述

当其他用户开启屏幕共享时，观看者（接收屏幕共享的用户）会看到：
- 屏幕共享内容全屏显示
- 共享者的摄像头视频作为可拖动的小窗口（画中画）显示在左上角

## 设计理念

这个功能在观看者端实现，而不是共享者端：
- 共享者：正常共享屏幕，看到自己的正常视图
- 观看者：看到全屏的屏幕共享 + 共享者的摄像头画中画

## 实现内容

### 1. UI 结构

#### 观看者视图（当其他人共享屏幕时）
```vue
<div v-if="currentScreenSharingUserId && currentScreenSharingUserId !== currentUserId" 
     class="screen-share-view">
  <!-- 屏幕共享画面（全屏） -->
  <video ref="remoteScreenShareVideo" class="screen-share-video"></video>
  
  <!-- 画中画：共享者的摄像头（可拖动） -->
  <div 
    v-if="sharingUserVideoStream"
    ref="pipSharingUserCamera"
    class="pip-camera"
    :style="{ left: pipPosition.x + 'px', top: pipPosition.y + 'px' }"
    @mousedown="startDrag">
    <video ref="pipSharingUserVideo" class="pip-camera-video"></video>
    <div class="pip-camera-info">
      <span class="pip-camera-name">{{ sharingUserName }}</span>
    </div>
  </div>
</div>
```

#### 正常视图（没有人共享屏幕时）
```vue
<div v-else class="participants-grid">
  <!-- 参与者网格 -->
</div>
```

### 2. 状态管理

```javascript
// 屏幕共享观看相关（观看者端）
const remoteScreenShareVideo = ref(null)  // 远程屏幕共享视频元素
const pipSharingUserVideo = ref(null)  // 画中画：共享者摄像头视频元素
const pipSharingUserCamera = ref(null)  // 画中画容器元素
const sharingUserVideoStream = ref(null)  // 共享者的摄像头流
const sharingUserName = ref('')  // 共享者的名称

// 画中画位置和拖动
const pipPosition = ref({ x: 20, y: 20 })
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
```

### 3. WebSocket 消息处理

#### 收到屏幕共享开始消息
```javascript
meetingWsService.on('screenShareStart', async (message) => {
  if (message.sendUserId !== currentUserId.value) {
    // 设置共享者信息
    currentScreenSharingUserId.value = message.sendUserId
    sharingUserName.value = message.sendUserNickName
    
    // 查找共享者的参与者信息
    const sharingParticipant = participants.value.find(
      p => p.userId === message.sendUserId
    )
    
    if (sharingParticipant && sharingParticipant.videoRef) {
      // 保存共享者的视频流
      const videoElement = sharingParticipant.videoRef
      if (videoElement && videoElement.srcObject) {
        sharingUserVideoStream.value = videoElement.srcObject
        
        await nextTick()
        
        // 设置画中画视频（共享者的摄像头）
        if (pipSharingUserVideo.value) {
          pipSharingUserVideo.value.srcObject = sharingUserVideoStream.value
        }
        
        // 设置屏幕共享视频（共享者的屏幕）
        setTimeout(() => {
          if (remoteScreenShareVideo.value && videoElement.srcObject) {
            remoteScreenShareVideo.value.srcObject = videoElement.srcObject
          }
        }, 500)
      }
    }
  }
})
```

#### 收到屏幕共享停止消息
```javascript
meetingWsService.on('screenShareStop', (message) => {
  if (message.sendUserId === currentScreenSharingUserId.value) {
    // 清除状态
    currentScreenSharingUserId.value = null
    sharingUserName.value = ''
    sharingUserVideoStream.value = null
    
    // 清空视频元素
    if (remoteScreenShareVideo.value) {
      remoteScreenShareVideo.value.srcObject = null
    }
    if (pipSharingUserVideo.value) {
      pipSharingUserVideo.value.srcObject = null
    }
  }
})
```

### 4. 共享者端逻辑

共享者端保持简单，只需要：
1. 获取屏幕共享流
2. 替换WebRTC视频轨道为屏幕共享
3. 通知其他用户开始/停止共享
4. 自己看到的是正常的参与者网格视图

```javascript
const shareScreen = async () => {
  // 获取屏幕共享流
  const stream = await navigator.mediaDevices.getDisplayMedia(...)
  
  // 替换WebRTC视频轨道
  const videoTrack = stream.getVideoTracks()[0]
  await webrtcManager.replaceVideoTrack(videoTrack)
  
  // 通知其他用户
  meetingWsService.sendMessage({
    messageType: 16, // SCREEN_SHARE_START
    messageSend2Type: 1, // GROUP
    sendUserId: currentUserId.value,
    sendUserNickName: userName.value,
    meetingId: meetingId.value,
    messageContent: {
      userId: currentUserId.value,
      userName: userName.value
    }
  })
}
```

### 5. 拖动功能

画中画窗口可以拖动，限制在视频区域内：

```javascript
const startDrag = (event) => {
  isDragging.value = true
  const rect = pipSharingUserCamera.value.getBoundingClientRect()
  dragOffset.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top
  }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  event.preventDefault()
}

const onDrag = (event) => {
  if (!isDragging.value) return
  
  let newX = event.clientX - dragOffset.value.x
  let newY = event.clientY - dragOffset.value.y
  
  // 限制在视频区域内
  const videoArea = document.querySelector('.video-area')
  if (videoArea && pipSharingUserCamera.value) {
    const videoRect = videoArea.getBoundingClientRect()
    const pipRect = pipSharingUserCamera.value.getBoundingClientRect()
    
    newX = Math.max(0, Math.min(newX, videoRect.width - pipRect.width))
    newY = Math.max(0, Math.min(newY, videoRect.height - pipRect.height))
  }
  
  pipPosition.value = { x: newX, y: newY }
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
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

/* 画中画窗口 */
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

/* 屏幕共享图标 */
.status-icon.screen-sharing {
  filter: drop-shadow(0 0 2px rgba(76, 175, 80, 0.8));
}
```

## 用户体验流程

### 场景1：用户A共享屏幕，用户B观看

**用户A（共享者）：**
1. 点击"共享屏幕"
2. 选择要共享的屏幕
3. 看到自己的正常参与者网格视图
4. 自己的视频框显示"🖥️"图标

**用户B（观看者）：**
1. 收到屏幕共享开始消息
2. 视图自动切换到屏幕共享模式
3. 看到用户A的屏幕内容全屏显示
4. 左上角显示用户A的摄像头小窗口（如果用户A开启了摄像头）
5. 可以拖动小窗口到任意位置

### 场景2：用户A停止共享

**用户A：**
1. 点击"停止共享"
2. 恢复正常视图

**用户B：**
1. 收到屏幕共享停止消息
2. 视图自动切换回参与者网格
3. 看到所有参与者的正常视图

## 功能特点

1. **观看者端实现** - 画中画在观看者端显示，共享者看到正常视图
2. **全屏显示** - 屏幕共享内容占据整个视频区域
3. **画中画窗口** - 共享者的摄像头显示在小窗口中
4. **可拖动** - 画中画窗口可以拖到任意位置
5. **边界限制** - 窗口不会拖出视频区域
6. **视觉反馈** - 悬停和拖动时有视觉效果
7. **状态图标** - 正在共享屏幕的用户显示"🖥️"图标
8. **自动切换** - 开始/停止共享时自动切换视图

## 测试步骤

1. 用户A和用户B加入会议
2. 用户A开启摄像头视频
3. 用户A点击"共享屏幕"
4. 验证用户A：
   - 看到正常的参与者网格
   - 自己的视频框显示"🖥️"图标
5. 验证用户B：
   - 看到全屏的屏幕共享内容
   - 左上角显示用户A的摄像头小窗口
   - 小窗口显示用户A的名称
   - 小窗口有绿色边框
6. 用户B拖动画中画窗口
7. 验证：
   - 可以拖动到任意位置
   - 不会拖出视频区域
   - 悬停时边框高亮
8. 用户A点击"停止共享"
9. 验证：
   - 用户A和用户B都恢复到正常网格视图
   - "🖥️"图标消失

## 文件修改

- `frontend/src/views/Meeting.vue` - 添加观看者端屏幕共享视图、画中画窗口、WebSocket消息处理和样式

## 状态
✅ 完成
