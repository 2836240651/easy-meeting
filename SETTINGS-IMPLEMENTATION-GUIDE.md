# 设置功能实现指南

## 概述

本文档说明如何在系统各个模块中应用用户设置。

## 已创建的工具

### SettingsManager (`frontend/src/utils/settings-manager.js`)

统一管理所有用户设置的工具类，提供：
- 设置的读取和保存
- 根据设置生成媒体约束
- 应用主题、通知等功能
- 设置变化监听

## 实现步骤

### 1. 在 Meeting.vue 中应用设置

#### 1.1 导入 SettingsManager

```javascript
import { settingsManager } from '@/utils/settings-manager.js'
```

#### 1.2 应用默认音视频状态

在 `onMounted` 或初始化时：

```javascript
// 根据设置决定初始状态
isVideoOn.value = settingsManager.shouldDefaultVideoOn()
isMuted.value = !settingsManager.shouldDefaultAudioOn()
```

#### 1.3 应用视频质量设置

修改 `getUserMediaWithAudio` 函数：

```javascript
const getUserMediaWithAudio = async () => {
  try {
    const videoConstraints = settingsManager.getVideoConstraints()
    const audioConstraints = settingsManager.getAudioConstraints()
    
    const stream = await navigator.mediaDevices.getUserMedia({
      video: isVideoOn.value ? videoConstraints : false,
      audio: audioConstraints
    })
    
    return stream
  } catch (error) {
    console.error('获取媒体流失败:', error)
    throw error
  }
}
```

#### 1.4 应用屏幕共享设置

修改 `startScreenShare` 函数：

```javascript
const startScreenShare = async () => {
  try {
    const constraints = settingsManager.getScreenShareConstraints()
    const stream = await navigator.mediaDevices.getDisplayMedia(constraints)
    
    // ... 其他代码
  } catch (error) {
    console.error('屏幕共享失败:', error)
  }
}
```

#### 1.5 应用视频镜像

在视频元素的 CSS 中：

```vue
<video 
  :style="{ transform: settingsManager.shouldMirrorVideo() ? 'scaleX(-1)' : 'none' }"
  ref="localVideo"
  autoplay 
  muted 
  playsinline>
</video>
```

或在 style 中：

```css
.local-video {
  transform: v-bind('settingsManager.shouldMirrorVideo() ? "scaleX(-1)" : "none"');
}
```

#### 1.6 显示网络状态

```vue
<div v-if="settingsManager.shouldShowNetworkStatus()" class="network-status">
  <span :class="networkQuality">{{ networkQuality }}</span>
</div>
```

### 2. 在 WebSocket 中应用自动重连设置

修改 `frontend/src/api/websocket.js` 或相关文件：

```javascript
import { settingsManager } from '@/utils/settings-manager.js'

class WebSocketService {
  // ... 其他代码
  
  handleDisconnect() {
    if (settingsManager.shouldAutoReconnect()) {
      this.reconnect()
    }
  }
}
```

### 3. 在 Dashboard.vue 中应用通知设置

#### 3.1 桌面通知

```javascript
import { settingsManager } from '@/utils/settings-manager.js'

// 收到好友申请时
const handleFriendRequest = (data) => {
  settingsManager.showDesktopNotification('新的好友申请', {
    body: `${data.nickName} 想要添加你为好友`,
    tag: 'friend-request'
  })
  
  settingsManager.playNotificationSound()
}

// 收到会议邀请时
const handleMeetingInvite = (data) => {
  settingsManager.showDesktopNotification('会议邀请', {
    body: `${data.inviterName} 邀请你参加会议`,
    tag: 'meeting-invite'
  })
  
  settingsManager.playNotificationSound()
}
```

### 4. 应用深色模式

#### 4.1 自动应用

`SettingsManager` 在初始化时会自动应用深色模式，给 `<html>` 添加 `dark-theme` 类。

#### 4.2 创建深色主题 CSS

创建 `frontend/src/styles/dark-theme.css`:

```css
:root.dark-theme {
  --bg-color: #1a1a1a;
  --card-bg: #2d2d2d;
  --text-primary: #ffffff;
  --text-secondary: #b0b0b0;
  --border-color: #404040;
  --primary-color: #4CAF50;
  --primary-hover: #45a049;
  --input-bg: #3a3a3a;
}

.dark-theme {
  background-color: var(--bg-color);
  color: var(--text-primary);
}

.dark-theme .card,
.dark-theme .modal-content,
.dark-theme .settings-section {
  background-color: var(--card-bg);
  border-color: var(--border-color);
}

.dark-theme input,
.dark-theme select,
.dark-theme textarea {
  background-color: var(--input-bg);
  color: var(--text-primary);
  border-color: var(--border-color);
}
```

在 `main.js` 中导入：

```javascript
import './styles/dark-theme.css'
```

#### 4.3 在 SettingsPanel 中监听变化

```javascript
import { settingsManager } from '@/utils/settings-manager.js'

const saveSettings = () => {
  settingsManager.saveSettings(settings.value)
  
  // 应用深色模式
  if (settings.value.darkMode !== undefined) {
    settingsManager.applyDarkMode()
  }
  
  ElMessage.success('设置已保存')
}
```

### 5. 应用会议提醒时间

在 `MeetingReminder.vue` 或相关组件中：

```javascript
import { settingsManager } from '@/utils/settings-manager.js'

const checkUpcomingMeetings = () => {
  const reminderTime = settingsManager.getReminderTime() // 分钟
  const now = Date.now()
  
  reservations.value.forEach(meeting => {
    const meetingTime = new Date(meeting.startTime).getTime()
    const timeDiff = meetingTime - now
    const minutesDiff = Math.floor(timeDiff / 60000)
    
    if (minutesDiff <= reminderTime && minutesDiff > 0) {
      // 显示提醒
      showReminder(meeting)
    }
  })
}
```

### 6. 应用隐私设置

#### 6.1 在线状态显示

```javascript
// 在 WebSocket 连接时
const connectWebSocket = () => {
  if (settingsManager.get('showOnlineStatus')) {
    wsService.connect()
    wsService.sendOnlineStatus(true)
  } else {
    // 隐身模式
    wsService.sendOnlineStatus(false)
  }
}
```

#### 6.2 陌生人添加权限

在后端 API 或前端验证：

```javascript
const handleAddContact = async (userId) => {
  const allowStrangerAdd = settingsManager.get('allowStrangerAdd')
  
  if (!allowStrangerAdd) {
    ElMessage.warning('该用户不允许陌生人添加')
    return
  }
  
  // 继续添加逻辑
}
```

## 完整示例：修改 Meeting.vue

```javascript
<script setup>
import { ref, onMounted } from 'vue'
import { settingsManager } from '@/utils/settings-manager.js'

// 初始化音视频状态（根据设置）
const isVideoOn = ref(settingsManager.shouldDefaultVideoOn())
const isMuted = ref(!settingsManager.shouldDefaultAudioOn())

// 获取媒体流（应用设置）
const getUserMediaWithAudio = async () => {
  try {
    const videoConstraints = settingsManager.getVideoConstraints()
    const audioConstraints = settingsManager.getAudioConstraints()
    
    const stream = await navigator.mediaDevices.getUserMedia({
      video: isVideoOn.value ? videoConstraints : false,
      audio: audioConstraints
    })
    
    return stream
  } catch (error) {
    console.error('获取媒体流失败:', error)
    throw error
  }
}

// 屏幕共享（应用设置）
const startScreenShare = async () => {
  try {
    const constraints = settingsManager.getScreenShareConstraints()
    const stream = await navigator.mediaDevices.getDisplayMedia(constraints)
    
    // ... 其他代码
  } catch (error) {
    console.error('屏幕共享失败:', error)
  }
}

// 监听设置变化
onMounted(() => {
  const unsubscribe = settingsManager.onChange((newSettings) => {
    console.log('设置已更新:', newSettings)
    
    // 可以在这里响应设置变化
    if (newSettings.darkMode !== undefined) {
      settingsManager.applyDarkMode()
    }
  })
  
  // 组件卸载时取消监听
  onUnmounted(() => {
    unsubscribe()
  })
})
</script>

<template>
  <div class="meeting-container">
    <!-- 本地视频（应用镜像设置） -->
    <video 
      ref="localVideo"
      :style="{ 
        transform: settingsManager.shouldMirrorVideo() ? 'scaleX(-1)' : 'none' 
      }"
      autoplay 
      muted 
      playsinline>
    </video>
    
    <!-- 网络状态（根据设置显示） -->
    <div v-if="settingsManager.shouldShowNetworkStatus()" class="network-status">
      <span>网络质量: {{ networkQuality }}</span>
    </div>
  </div>
</template>
```

## 测试清单

- [ ] 修改视频质量设置，加入会议时验证分辨率
- [ ] 开启/关闭回声消除，测试音频效果
- [ ] 切换深色模式，验证主题变化
- [ ] 修改会议提醒时间，验证提醒触发时机
- [ ] 开启/关闭桌面通知，验证通知显示
- [ ] 测试视频镜像功能
- [ ] 测试屏幕共享音频选项
- [ ] 测试自动重连功能
- [ ] 测试网络状态显示
- [ ] 测试默认音视频状态

## 注意事项

1. **浏览器兼容性**: 某些设置（如虚拟背景）可能需要特定浏览器支持
2. **权限请求**: 桌面通知需要用户授权
3. **性能影响**: 高质量视频会增加带宽和CPU使用
4. **设置同步**: 目前设置只保存在本地，考虑后续同步到服务器
5. **默认值**: 确保所有设置都有合理的默认值

## 后续优化

1. 添加设置导入/导出功能
2. 添加设置重置功能
3. 添加设置预设（如"低带宽模式"）
4. 添加设置验证和错误处理
5. 添加设置变化的动画效果
6. 支持设置云端同步
