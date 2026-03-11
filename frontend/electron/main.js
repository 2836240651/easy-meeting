const { app, BrowserWindow, Menu, desktopCapturer, ipcMain, screen } = require('electron')
const path = require('path')
const DEV_SERVER_URL = 'http://localhost:3001'

// 禁用默认菜单
Menu.setApplicationMenu(null)

// 窗口引用
let mainWindow = null
let topBarWindow = null
let videoWindow = null
let chatWindow = null
let borderWindows = []

const focusWindowContents = (targetWindow) => {
  if (!targetWindow || targetWindow.isDestroyed()) {
    return
  }

  targetWindow.focus()
  targetWindow.moveTop()

  setTimeout(() => {
    if (!targetWindow.isDestroyed()) {
      targetWindow.webContents.focus()
      targetWindow.webContents.send('window-focused')
    }
  }, 50)
}

// 处理屏幕共享源请求
ipcMain.handle('get-desktop-sources', async (event, options) => {
  try {
    console.log('主进程: 收到获取屏幕源请求', options)
    const sources = await desktopCapturer.getSources(options)
    console.log('主进程: 找到', sources.length, '个屏幕/窗口源')
    
    // 返回源信息（只返回必要的字段，避免序列化问题）
    return sources.map(source => ({
      id: source.id,
      name: source.name,
      thumbnail: source.thumbnail.toDataURL()
    }))
  } catch (error) {
    console.error('主进程: 获取屏幕源失败:', error)
    throw error
  }
})

// 创建顶部指示条窗口
function createTopBarWindow() {
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
    hasShadow: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: true,
      contextIsolation: false
    }
  })

  topBarWindow.setIgnoreMouseEvents(false)
  topBarWindow.loadURL(`${DEV_SERVER_URL}/#/screen-share-topbar`)
    .catch(() => {
      topBarWindow.loadFile(path.join(__dirname, '../dist/index.html'), { hash: '/screen-share-topbar' })
    })

  console.log('✅ 顶部指示条窗口已创建')
}

// 创建视频窗口
function createVideoWindow() {
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
    hasShadow: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: true,
      contextIsolation: false
    }
  })

  videoWindow.setIgnoreMouseEvents(false)
  videoWindow.loadURL(`${DEV_SERVER_URL}/#/screen-share-video`)
    .catch(() => {
      videoWindow.loadFile(path.join(__dirname, '../dist/index.html'), { hash: '/screen-share-video' })
    })

  console.log('✅ 视频窗口已创建')
}

// 创建聊天窗口
function createChatWindow() {
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
    hasShadow: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: true,
      contextIsolation: false
    }
  })

  chatWindow.setIgnoreMouseEvents(false)
  chatWindow.loadURL(`${DEV_SERVER_URL}/#/screen-share-chat`)
    .catch(() => {
      chatWindow.loadFile(path.join(__dirname, '../dist/index.html'), { hash: '/screen-share-chat' })
    })

  console.log('✅ 聊天窗口已创建')
}

// 创建共享区域边框窗口
function createBorderWindows(bounds) {
  // 清除旧边框
  borderWindows.forEach(win => {
    if (win && !win.isDestroyed()) {
      win.close()
    }
  })
  borderWindows = []

  const cornerSize = 60
  const corners = [
    { name: 'top-left', x: bounds.x, y: bounds.y },
    { name: 'top-right', x: bounds.x + bounds.width - cornerSize, y: bounds.y },
    { name: 'bottom-left', x: bounds.x, y: bounds.y + bounds.height - cornerSize },
    { name: 'bottom-right', x: bounds.x + bounds.width - cornerSize, y: bounds.y + bounds.height - cornerSize }
  ]

  corners.forEach(corner => {
    const borderWindow = new BrowserWindow({
      width: cornerSize,
      height: cornerSize,
      x: corner.x,
      y: corner.y,
      frame: false,
      transparent: true,
      alwaysOnTop: true,
      skipTaskbar: true,
      resizable: false,
      hasShadow: false,
      webPreferences: {
        preload: path.join(__dirname, 'preload.js'),
        nodeIntegration: true,
        contextIsolation: false
      }
    })

    borderWindow.setIgnoreMouseEvents(true)
    borderWindow.loadURL(`${DEV_SERVER_URL}/#/border?corner=${corner.name}`)
      .catch(() => {
        borderWindow.loadFile(path.join(__dirname, '../dist/index.html'), { hash: `/border?corner=${corner.name}` })
      })

    borderWindows.push(borderWindow)
  })

  console.log('✅ 边框窗口已创建 (4个角)')
}

// 开始屏幕共享悬浮层
ipcMain.handle('start-screen-share-overlay', async (event, shareBounds) => {
  try {
    console.log('🚀 开始创建屏幕共享悬浮层')
    
    // 隐藏主窗口
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.hide()
      console.log('✅ 主窗口已隐藏')
    }
    
    // 创建悬浮窗口
    createTopBarWindow()
    createVideoWindow()
    createChatWindow()
    
    // 如果提供了共享区域边界，创建边框
    if (shareBounds) {
      createBorderWindows(shareBounds)
    }
    
    return { success: true }
  } catch (error) {
    console.error('❌ 创建屏幕共享悬浮层失败:', error)
    return { success: false, error: error.message }
  }
})

// 停止屏幕共享悬浮层
ipcMain.handle('stop-screen-share-overlay', async () => {
  try {
    console.log('🛑 停止屏幕共享悬浮层')
    
    // 关闭所有悬浮窗口
    if (topBarWindow && !topBarWindow.isDestroyed()) {
      topBarWindow.close()
      topBarWindow = null
    }
    if (videoWindow && !videoWindow.isDestroyed()) {
      videoWindow.close()
      videoWindow = null
    }
    if (chatWindow && !chatWindow.isDestroyed()) {
      chatWindow.close()
      chatWindow = null
    }
    borderWindows.forEach(win => {
      if (win && !win.isDestroyed()) {
        win.close()
      }
    })
    borderWindows = []
    
    // 显示主窗口
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.show()
      focusWindowContents(mainWindow)
      console.log('✅ 主窗口已恢复')
    }
    
    return { success: true }
  } catch (error) {
    console.error('❌ 停止屏幕共享悬浮层失败:', error)
    return { success: false, error: error.message }
  }
})

// 窗口间通信：更新参与者列表
ipcMain.on('update-participants', (event, participants) => {
  if (videoWindow && !videoWindow.isDestroyed()) {
    videoWindow.webContents.send('participants-updated', participants)
  }
})

// 窗口间通信：更新聊天消息
ipcMain.on('update-chat-messages', (event, messages) => {
  if (chatWindow && !chatWindow.isDestroyed()) {
    chatWindow.webContents.send('chat-messages-updated', messages)
  }
})

// 窗口间通信：从悬浮窗口发送操作到主窗口
ipcMain.on('overlay-action', (event, action) => {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('overlay-action', action)
  }
})

// 移动窗口位置
ipcMain.handle('move-window', async (event, windowType, x, y) => {
  try {
    let targetWindow = null
    
    switch (windowType) {
      case 'video':
        targetWindow = videoWindow
        break
      case 'chat':
        targetWindow = chatWindow
        break
      default:
        return { success: false, error: 'Unknown window type' }
    }
    
    if (targetWindow && !targetWindow.isDestroyed()) {
      targetWindow.setPosition(Math.round(x), Math.round(y))
      return { success: true }
    }
    
    return { success: false, error: 'Window not found' }
  } catch (error) {
    console.error('移动窗口失败:', error)
    return { success: false, error: error.message }
  }
})

ipcMain.handle('focus-current-window', async (event) => {
  try {
    const targetWindow = BrowserWindow.fromWebContents(event.sender)
    focusWindowContents(targetWindow)
    return { success: true }
  } catch (error) {
    console.error('聚焦当前窗口失败:', error)
    return { success: false, error: error.message }
  }
})

function createWindow () {
  // 创建浏览器窗口
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    minWidth: 800,
    minHeight: 600,
    webPreferences: {
      // 预加载脚本
      preload: path.join(__dirname, 'preload.js'),
      // 启用 nodeIntegration
      nodeIntegration: true,
      // 禁用 contextIsolation
      contextIsolation: false,
      // 启用网页安全
      webSecurity: false,
      // 允许运行不安全的内容
      allowRunningInsecureContent: true,
      // 启用媒体权限
      enableRemoteModule: true
    },
    // 应用图标
    icon: path.join(__dirname, '../public/vite.svg'),
    // 显示时聚焦
    show: false,
    // 始终在顶部
    alwaysOnTop: false
  })

  // 处理媒体权限请求（摄像头、麦克风、屏幕共享）
  mainWindow.webContents.session.setPermissionRequestHandler((webContents, permission, callback) => {
    console.log('权限请求:', permission)
    // 自动允许所有媒体权限
    if (permission === 'media' || permission === 'mediaKeySystem' || permission === 'geolocation' || permission === 'notifications' || permission === 'midiSysex' || permission === 'pointerLock' || permission === 'fullscreen') {
      callback(true)
    } else {
      callback(false)
    }
  })

  // 处理媒体访问请求
  mainWindow.webContents.session.setPermissionCheckHandler((webContents, permission, requestingOrigin, details) => {
    console.log('权限检查:', permission, requestingOrigin)
    // 允许所有媒体相关权限
    if (permission === 'media' || permission === 'mediaKeySystem') {
      return true
    }
    return true
  })

  // 窗口准备好后显示并聚焦
  mainWindow.once('ready-to-show', () => {
    mainWindow.show()
    focusWindowContents(mainWindow)
  })

  // 处理焦点事件
  mainWindow.on('focus', () => {
    console.log('窗口获得焦点')
    // 确保网页内容也获得焦点
    focusWindowContents(mainWindow)
  })

  mainWindow.on('blur', () => {
    console.log('窗口失去焦点')
  })

  // 监听页面加载完成事件
  mainWindow.webContents.on('did-finish-load', () => {
    console.log('页面加载完成')
    // 确保页面获得焦点
    focusWindowContents(mainWindow)
  })

  // 监听页面导航事件
  mainWindow.webContents.on('did-navigate', () => {
    console.log('页面导航完成')
    // 页面导航后重新聚焦
    setTimeout(() => {
      focusWindowContents(mainWindow)
    }, 100)
  })

  // 监听页面内导航事件（SPA路由切换）
  mainWindow.webContents.on('did-navigate-in-page', () => {
    console.log('页面内导航完成')
    // SPA路由切换后重新聚焦
    setTimeout(() => {
      focusWindowContents(mainWindow)
    }, 100)
  })

  // 加载应用的 index.html
  // 尝试加载 Vite 开发服务器
  // 如果失败，再尝试加载打包后的 index.html
  mainWindow.loadURL(DEV_SERVER_URL)
    .then(() => {
      console.log('成功加载 Vite 开发服务器')
      // 开发模式下打开开发者工具
      mainWindow.webContents.openDevTools()
    })
    .catch((error) => {
      console.error('加载 Vite 开发服务器失败:', error)
      console.log('尝试加载打包后的 index.html')
      // 尝试加载打包后的 index.html
      mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
        .catch((loadError) => {
          console.error('加载打包后的 index.html 失败:', loadError)
          // 如果都失败，显示错误页面
          mainWindow.loadURL(`data:text/html,<h1>加载失败</h1><p>无法加载应用，请确保前端开发服务器正在运行或已构建项目。</p><p>错误信息: ${loadError.message}</p>`)
        })
    })

  // 处理窗口关闭事件
  mainWindow.on('closed', function () {
    // 在 macOS 上，除非用户用 Cmd + Q 明确退出，否则应用及其菜单栏会保持激活
    if (process.platform !== 'darwin') {
      app.quit()
    }
  })

  return mainWindow
}

// Electron 会在初始化后并准备创建浏览器窗口时，调用这个函数
app.whenReady().then(() => {
  createWindow()

  // 在 macOS 上，点击 Dock 图标并且没有其他窗口打开时，重新创建一个窗口
  app.on('activate', function () {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

// 当所有窗口都关闭时退出应用
app.on('window-all-closed', function () {
  // 在 macOS 上，除非用户用 Cmd + Q 明确退出，否则应用及其菜单栏会保持激活
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
