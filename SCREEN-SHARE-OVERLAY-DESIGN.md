# 屏幕共享悬浮窗设计方案

## 需求分析

### 用户期望的行为

1. **选择屏幕共享后**：
   - Electron 主窗口最小化到屏幕顶部（类似菜单栏）
   - 显示紧凑的控制条

2. **屏幕右上角**：
   - 显示会议成员视频缩略图（悬浮窗口）
   - 默认显示自己的摄像头
   - 可以切换查看其他成员
   - 包含音频控制按钮

## 技术方案

### 方案 A：单窗口 + CSS 悬浮层（推荐）

**优点**：
- 实现简单
- 性能好
- 易于维护

**实现**：
1. 主窗口保持全屏或最大化
2. 使用 CSS 创建顶部控制条（position: fixed, top: 0）
3. 使用 CSS 创建右上角视频悬浮窗（position: fixed, top: 50px, right: 20px）
4. 两个悬浮层都可拖动
5. 点击空白区域可以隐藏/显示

### 方案 B：多窗口（复杂）

**优点**：
- 更接近原生应用体验
- 可以独立控制每个窗口

**缺点**：
- 实现复杂
- 窗口管理困难
- 性能开销大

**实现**：
1. 主窗口最小化
2. 创建顶部控制条窗口（alwaysOnTop, frame: false）
3. 创建右上角视频窗口（alwaysOnTop, frame: false）

## 推荐实现：方案 A

### 1. 顶部控制条

```vue
<div v-if="isScreenSharing" class="screen-share-overlay-bar">
  <div class="overlay-bar-left">
    <span class="sharing-indicator">🔴 正在共享屏幕</span>
    <span class="meeting-duration">{{ formattedDuration }}</span>
  </div>
  <div class="overlay-bar-center">
    <button @click="toggleMute">{{ isMuted ? '🔇' : '🎤' }}</button>
    <button @click="toggleVideo">{{ isVideoOn ? '📹' : '🚫' }}</button>
    <button @click="showParticipants">👥</button>
    <button @click="showChat">💬</button>
    <button @click="toggleScreenSharePause">{{ isScreenSharePaused ? '▶️' : '⏸️' }}</button>
  </div>
  <div class="overlay-bar-right">
    <button @click="stopScreenShare" class="stop-btn">结束共享</button>
  </div>
</div>
```

**样式**：
```css
.screen-share-overlay-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 50px;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(10px);
  z-index: 10000;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}
```

### 2. 右上角视频悬浮窗

```vue
<div v-if="isScreenSharing" class="video-overlay-panel">
  <div class="panel-header">
    <span class="panel-title">会议成员</span>
    <button @click="toggleVideoPanel">{{ videoPanelExpanded ? '−' : '+' }}</button>
  </div>
  
  <div v-if="videoPanelExpanded" class="panel-content">
    <!-- 自己的视频 -->
    <div class="video-thumbnail active">
      <video ref="localVideoOverlay" autoplay muted playsinline></video>
      <div class="thumbnail-info">
        <span class="name">我</span>
        <button @click="toggleMute" class="audio-btn">
          {{ isMuted ? '🔇' : '🎤' }}
        </button>
      </div>
    </div>
    
    <!-- 其他成员视频 -->
    <div v-for="participant in participants" :key="participant.userId" class="video-thumbnail">
      <video v-if="participant.videoOpen" :ref="el => setParticipantVideoRef(participant.userId, el)" autoplay playsinline></video>
      <img v-else :src="participant.avatar" class="avatar">
      <div class="thumbnail-info">
        <span class="name">{{ participant.name }}</span>
        <span class="audio-status">{{ participant.isMuted ? '🔇' : '🎤' }}</span>
      </div>
    </div>
  </div>
</div>
```

**样式**：
```css
.video-overlay-panel {
  position: fixed;
  top: 60px;
  right: 20px;
  width: 280px;
  max-height: 500px;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(15px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  z-index: 10000;
  overflow: hidden;
}

.video-thumbnail {
  position: relative;
  width: 100%;
  aspect-ratio: 16/9;
  background: #1a1a1a;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 8px;
}

.video-thumbnail video,
.video-thumbnail .avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
  padding: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.audio-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  cursor: pointer;
  font-size: 16px;
}
```

### 3. 拖动功能

```javascript
// 使视频面板可拖动
const videoPanelDraggable = ref(null)
const isDraggingPanel = ref(false)
const panelPosition = ref({ x: window.innerWidth - 300, y: 60 })

const startDragPanel = (event) => {
  isDraggingPanel.value = true
  const rect = videoPanelDraggable.value.getBoundingClientRect()
  dragOffset.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top
  }
  document.addEventListener('mousemove', onDragPanel)
  document.addEventListener('mouseup', stopDragPanel)
}

const onDragPanel = (event) => {
  if (!isDraggingPanel.value) return
  panelPosition.value = {
    x: event.clientX - dragOffset.value.x,
    y: event.clientY - dragOffset.value.y
  }
}

const stopDragPanel = () => {
  isDraggingPanel.value = false
  document.removeEventListener('mousemove', onDragPanel)
  document.removeEventListener('mouseup', stopDragPanel)
}
```

## 实现步骤

### 第一步：修改 Meeting.vue 模板

1. 添加顶部控制条悬浮层
2. 添加右上角视频面板悬浮层
3. 在屏幕共享时隐藏原有的会议界面

### 第二步：添加响应式数据

```javascript
const videoPanelExpanded = ref(true)  // 视频面板是否展开
const panelPosition = ref({ x: window.innerWidth - 300, y: 60 })  // 面板位置
```

### 第三步：添加 CSS 样式

1. 顶部控制条样式
2. 视频面板样式
3. 拖动效果

### 第四步：处理视频流

1. 在视频面板中显示本地视频流
2. 显示远程参与者视频流
3. 处理视频开关状态

## 用户体验

### 共享者视角

1. 点击"共享屏幕"
2. 选择屏幕/窗口
3. 主界面淡出，显示：
   - 顶部：黑色半透明控制条
   - 右上角：视频面板（可折叠、可拖动）
4. 可以正常使用电脑，控制条始终在最上层
5. 点击"结束共享"恢复正常界面

### 观看者视角

保持不变：
- 全屏显示共享屏幕
- 左上角显示共享者摄像头（PIP）

## 下一步

需要确认：
1. 是否采用方案 A（单窗口 + CSS 悬浮层）？
2. 视频面板是否需要可拖动？
3. 是否需要折叠/展开功能？
4. 是否需要自动隐藏功能（鼠标移开后隐藏）？
