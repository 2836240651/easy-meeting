# 屏幕共享原生悬浮窗实现方案

## 用户需求分析

### 期望效果

1. **共享区域边框**：
   - 选择共享区域后，四个角显示绿色边框
   - 类似录屏软件的选区指示器

2. **主窗口处理**：
   - Electron 主窗口完全最小化或隐藏
   - 不再显示页面化的会议界面

3. **顶部指示条**：
   - 屏幕最顶部显示"您正在共享屏幕"
   - 鼠标悬停时展开显示菜单（控制按钮）
   - 鼠标离开时收起

4. **右上角视频窗口**：
   - 独立的悬浮窗口
   - 显示所有成员的视频流
   - 可以拖动位置
   - 始终在最上层

5. **左下角聊天窗口**：
   - 独立的悬浮窗口
   - 显示聊天消息
   - 可以拖动位置
   - 始终在最上层

## 技术方案

### 方案：Electron 多窗口 + 透明窗口

这是唯一能实现原生悬浮效果的方案。

#### 窗口架构

```
主窗口 (BrowserWindow)
├── 隐藏/最小化
└── 负责业务逻辑和 WebRTC

顶部指示条窗口 (BrowserWindow)
├── frame: false (无边框)
├── transparent: true (透明)
├── alwaysOnTop: true (始终在最上层)
├── width: screen.width
├── height: 40 (收起) / 60 (展开)
└── y: 0

右上角视频窗口 (BrowserWindow)
├── frame: false
├── transparent: true
├── alwaysOnTop: true
├── width: 300
├── height: 400
└── 可拖动

左下角聊天窗口 (BrowserWindow)
├── frame: false
├── transparent: true
├── alwaysOnTop: true
├── width: 350
├── height: 500
└── 可拖动

共享区域边框窗口 (BrowserWindow) x4
├── frame: false
├── transparent: true
├── alwaysOnTop: true
├── 四个角的绿色边框
└── 根据共享区域位置定位
```

## 实现步骤

### 第一步：修改 Electron Main.js

添加创建悬浮窗口的函数：

```javascript
let mainWindow = null
let topBarWindow = null
let videoWindow = null
let chatWindow = null
let borderWindows = []

// 创建顶部指示条窗口
function createTopBarWindow() {
  const { screen } = require('electron')
  const primaryDisplay = screen.getPrimaryDisplay()
  const { width } = primaryDisplay.workAreaSize

  topBarWindow = new BrowserWindow({
    width: width,
    height: 40,
    x: 0,
    y: 0,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    skipTaskbar: true,
    resizable: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  })

  topBarWindow.loadURL(`${process.env.VITE_DEV_SERVER_URL}#/screen-share-topbar`)
  topBarWindow.setIgnoreMouseEvents(false)
}

// 创建视频窗口
function createVideoWindow() {
  const { screen } = require('electron')
  const primaryDisplay = screen.getPrimaryDisplay()
  const { width, height } = primaryDisplay.workAreaSize

  videoWindow = new BrowserWindow({
    width: 300,
    height: 400,
    x: width - 320,
    y: 60,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    skipTaskbar: true,
    resizable: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  })

  videoWindow.loadURL(`${process.env.VITE_DEV_SERVER_URL}#/screen-share-video`)
}

// 创建聊天窗口
function createChatWindow() {
  const { screen } = require('electron')
  const primaryDisplay = screen.getPrimaryDisplay()
  const { height } = primaryDisplay.workAreaSize

  chatWindow = new BrowserWindow({
    width: 350,
    height: 500,
    x: 20,
    y: height - 520,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    skipTaskbar: true,
    resizable: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  })

  chatWindow.loadURL(`${process.env.VITE_DEV_SERVER_URL}#/screen-share-chat`)
}

// 创建共享区域边框
function createBorderWindows(bounds) {
  const borderSize = 4
  const cornerSize = 40

  // 清除旧边框
  borderWindows.forEach(win => win.close())
  borderWindows = []

  // 左上角
  const topLeft = new BrowserWindow({
    width: cornerSize,
    height: cornerSize,
    x: bounds.x,
    y: bounds.y,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    skipTaskbar: true,
    resizable: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  })
  topLeft.loadURL(`${process.env.VITE_DEV_SERVER_URL}#/border?corner=top-left`)
  borderWindows.push(topLeft)

  // 右上角
  const topRight = new BrowserWindow({
    width: cornerSize,
    height: cornerSize,
    x: bounds.x + bounds.width - cornerSize,
    y: bounds.y,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    skipTaskbar: true,
    resizable: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  })
  topRight.loadURL(`${process.env.VITE_DEV_SERVER_URL}#/border?corner=top-right`)
  borderWindows.push(topRight)

  // 左下角
  const bottomLeft = new BrowserWindow({
    width: cornerSize,
    height: cornerSize,
    x: bounds.x,
    y: bounds.y + bounds.height - cornerSize,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    skipTaskbar: true,
    resizable: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  })
  bottomLeft.loadURL(`${process.env.VITE_DEV_SERVER_URL}#/border?corner=bottom-left`)
  borderWindows.push(bottomLeft)

  // 右下角
  const bottomRight = new BrowserWindow({
    width: cornerSize,
    height: cornerSize,
    x: bounds.x + bounds.width - cornerSize,
    y: bounds.y + bounds.height - cornerSize,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    skipTaskbar: true,
    resizable: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  })
  bottomRight.loadURL(`${process.env.VITE_DEV_SERVER_URL}#/border?corner=bottom-right`)
  borderWindows.push(bottomRight)
}

// IPC 处理
ipcMain.handle('start-screen-share-overlay', async (event, shareBounds) => {
  // 隐藏主窗口
  mainWindow.hide()
  
  // 创建悬浮窗口
  createTopBarWindow()
  createVideoWindow()
  createChatWindow()
  
  // 如果提供了共享区域边界，创建边框
  if (shareBounds) {
    createBorderWindows(shareBounds)
  }
  
  return { success: true }
})

ipcMain.handle('stop-screen-share-overlay', async () => {
  // 关闭所有悬浮窗口
  if (topBarWindow) {
    topBarWindow.close()
    topBarWindow = null
  }
  if (videoWindow) {
    videoWindow.close()
    videoWindow = null
  }
  if (chatWindow) {
    chatWindow.close()
    chatWindow = null
  }
  borderWindows.forEach(win => win.close())
  borderWindows = []
  
  // 显示主窗口
  mainWindow.show()
  
  return { success: true }
})
```

### 第二步：创建 Vue 组件

#### 1. 顶部指示条组件 (ScreenShareTopBar.vue)

```vue
<template>
  <div class="topbar-container" 
       @mouseenter="expanded = true" 
       @mouseleave="expanded = false"
       :class="{ expanded }">
    <div class="topbar-indicator">
      <span class="recording-dot"></span>
      <span class="text">您正在共享屏幕</span>
    </div>
    
    <div v-if="expanded" class="topbar-controls">
      <button @click="toggleMute">{{ isMuted ? '🔇' : '🎤' }}</button>
      <button @click="toggleVideo">{{ isVideoOn ? '📹' : '🚫' }}</button>
      <button @click="pauseShare">{{ isPaused ? '▶️' : '⏸️' }}</button>
      <button @click="stopShare" class="stop-btn">结束共享</button>
    </div>
  </div>
</template>

<style scoped>
.topbar-container {
  width: 100vw;
  height: 40px;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: height 0.3s ease;
}

.topbar-container.expanded {
  height: 60px;
}

.topbar-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: white;
}

.recording-dot {
  width: 10px;
  height: 10px;
  background: #f44336;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.topbar-controls {
  display: flex;
  gap: 8px;
  margin-left: 20px;
}
</style>
```

#### 2. 视频窗口组件 (ScreenShareVideo.vue)

```vue
<template>
  <div class="video-window" @mousedown="startDrag">
    <div class="window-header">
      <span>会议成员</span>
      <button @click="toggleExpand">{{ expanded ? '−' : '+' }}</button>
    </div>
    <div v-if="expanded" class="video-list">
      <!-- 成员视频列表 -->
    </div>
  </div>
</template>

<style scoped>
.video-window {
  width: 300px;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  overflow: hidden;
}
</style>
```

#### 3. 聊天窗口组件 (ScreenShareChat.vue)

```vue
<template>
  <div class="chat-window" @mousedown="startDrag">
    <div class="window-header">
      <span>聊天</span>
      <button @click="toggleExpand">{{ expanded ? '−' : '+' }}</button>
    </div>
    <div v-if="expanded" class="chat-content">
      <!-- 聊天消息列表 -->
    </div>
  </div>
</template>
```

#### 4. 边框组件 (ScreenShareBorder.vue)

```vue
<template>
  <div class="border-corner" :class="corner">
    <svg width="40" height="40">
      <path v-if="corner === 'top-left'" 
            d="M 0 4 L 0 0 L 4 0 M 0 40 L 0 36 M 40 0 L 36 0" 
            stroke="#4caf50" 
            stroke-width="4" 
            fill="none"/>
      <!-- 其他角的 SVG 路径 -->
    </svg>
  </div>
</template>

<style scoped>
.border-corner {
  width: 40px;
  height: 40px;
  pointer-events: none;
}
</style>
```

### 第三步：修改路由

```javascript
// router/index.js
const routes = [
  // ... 现有路由
  {
    path: '/screen-share-topbar',
    name: 'ScreenShareTopBar',
    component: () => import('../views/ScreenShareTopBar.vue')
  },
  {
    path: '/screen-share-video',
    name: 'ScreenShareVideo',
    component: () => import('../views/ScreenShareVideo.vue')
  },
  {
    path: '/screen-share-chat',
    name: 'ScreenShareChat',
    component: () => import('../views/ScreenShareChat.vue')
  },
  {
    path: '/border',
    name: 'ScreenShareBorder',
    component: () => import('../views/ScreenShareBorder.vue')
  }
]
```

### 第四步：在 Meeting.vue 中触发

```javascript
const startScreenShare = async () => {
  try {
    // ... 获取屏幕共享流
    
    // 获取共享区域的边界（如果可能）
    const shareBounds = {
      x: 0,
      y: 0,
      width: 1920,
      height: 1080
    }
    
    // 通知 Electron 创建悬浮窗口
    if (window.electron) {
      await window.electron.invoke('start-screen-share-overlay', shareBounds)
    }
    
    // ... 其他逻辑
  } catch (error) {
    console.error('屏幕共享失败:', error)
  }
}

const stopScreenShare = async () => {
  try {
    // ... 停止屏幕共享
    
    // 通知 Electron 关闭悬浮窗口
    if (window.electron) {
      await window.electron.invoke('stop-screen-share-overlay')
    }
    
    // ... 其他逻辑
  } catch (error) {
    console.error('停止屏幕共享失败:', error)
  }
}
```

## 窗口间通信

使用 Electron IPC 在窗口间传递数据：

```javascript
// 主窗口 -> 悬浮窗口
ipcMain.on('update-participants', (event, participants) => {
  if (videoWindow) {
    videoWindow.webContents.send('participants-updated', participants)
  }
})

// 悬浮窗口 -> 主窗口
ipcMain.on('toggle-mute', () => {
  mainWindow.webContents.send('toggle-mute')
})
```

## 优势

1. **原生体验** - 真正的系统级悬浮窗口
2. **性能好** - 每个窗口独立渲染
3. **灵活布局** - 可以自由拖动和调整
4. **不遮挡内容** - 透明窗口，只显示必要内容

## 挑战

1. **复杂度高** - 需要管理多个窗口
2. **窗口间通信** - 需要通过 IPC 传递数据
3. **状态同步** - 需要保持所有窗口状态一致
4. **边框定位** - 需要准确获取共享区域边界

## 下一步

这是一个复杂的实现，需要：
1. 修改 Electron main.js
2. 创建 4 个新的 Vue 组件
3. 添加路由
4. 实现窗口间通信
5. 处理拖动逻辑

是否继续实现这个方案？
