// 预加载脚本
// 可以在这里注入全局变量或设置事件监听器

// 在 DOMContentLoaded 之前就设置好 Electron API
const setupElectronAPIs = () => {
  try {
    const { ipcRenderer } = require('electron')
    
    console.log('✅ Electron IPC 已加载')
    
    // 向渲染进程暴露一些 Electron API
    window.electron = {
      // 示例：获取应用版本
      getAppVersion: () => {
        return process.versions.electron
      },
      // 获取屏幕共享源（通过 IPC 调用主进程）
      getDesktopCapturerSources: async (options) => {
        try {
          console.log('🖥️ Preload: 通过 IPC 请求屏幕源', options)
          const sources = await ipcRenderer.invoke('get-desktop-sources', options)
          console.log('🖥️ Preload: 收到', sources.length, '个屏幕源')
          return sources
        } catch (error) {
          console.error('🖥️ Preload: 获取屏幕共享源失败:', error)
          throw error
        }
      },
      // 屏幕共享悬浮层控制
      startScreenShareOverlay: async (shareBounds) => {
        try {
          console.log('🚀 Preload: 请求创建屏幕共享悬浮层', shareBounds)
          const result = await ipcRenderer.invoke('start-screen-share-overlay', shareBounds)
          console.log('✅ Preload: 屏幕共享悬浮层创建结果:', result)
          return result
        } catch (error) {
          console.error('❌ Preload: 创建屏幕共享悬浮层失败:', error)
          throw error
        }
      },
      stopScreenShareOverlay: async () => {
        try {
          console.log('🛑 Preload: 请求停止屏幕共享悬浮层')
          const result = await ipcRenderer.invoke('stop-screen-share-overlay')
          console.log('✅ Preload: 屏幕共享悬浮层停止结果:', result)
          return result
        } catch (error) {
          console.error('❌ Preload: 停止屏幕共享悬浮层失败:', error)
          throw error
        }
      },
      // 窗口间通信
      updateParticipants: (participants) => {
        ipcRenderer.send('update-participants', participants)
      },
      updateChatMessages: (messages) => {
        ipcRenderer.send('update-chat-messages', messages)
      },
      sendOverlayAction: (action) => {
        ipcRenderer.send('overlay-action', action)
      },
      onParticipantsUpdated: (callback) => {
        ipcRenderer.on('participants-updated', (event, participants) => callback(participants))
      },
      onChatMessagesUpdated: (callback) => {
        ipcRenderer.on('chat-messages-updated', (event, messages) => callback(messages))
      },
      onOverlayAction: (callback) => {
        ipcRenderer.on('overlay-action', (event, action) => callback(action))
      },
      // 移动窗口
      moveWindow: async (windowType, x, y) => {
        try {
          const result = await ipcRenderer.invoke('move-window', windowType, x, y)
          return result
        } catch (error) {
          console.error('移动窗口失败:', error)
          throw error
        }
      },
      // 剪贴板操作
      clipboard: {
        writeText: (text) => {
          const { clipboard } = require('electron')
          clipboard.writeText(text)
        },
        readText: () => {
          const { clipboard } = require('electron')
          return clipboard.readText()
        }
      },
      // 标记是否在 Electron 环境中
      isElectron: true
    }
    
    // 覆盖 navigator.mediaDevices.getDisplayMedia 以支持 Electron
    const originalGetUserMedia = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices)
    
    // 等待 DOM 加载后再覆盖 getDisplayMedia
    const overrideGetDisplayMedia = () => {
      if (navigator.mediaDevices && !navigator.mediaDevices._electronPatched) {
        navigator.mediaDevices._electronPatched = true
        
        navigator.mediaDevices.getDisplayMedia = async function(constraints) {
          try {
            console.log('🖥️ Electron: 请求屏幕共享')
            
            // 通过 window.electron API 获取屏幕源
            console.log('🖥️ Electron: 正在获取屏幕源...')
            const sources = await window.electron.getDesktopCapturerSources({
              types: ['screen', 'window']
            })
            
            console.log('🖥️ Electron: 找到', sources.length, '个屏幕/窗口源')
            
            if (sources.length === 0) {
              throw new Error('没有找到可共享的屏幕或窗口')
            }
            
            // 打印所有可用源
            sources.forEach((source, index) => {
              console.log(`🖥️ Electron: 源 ${index + 1}: ${source.name} (ID: ${source.id})`)
            })
            
            // 让用户选择屏幕源
            console.log('🖥️ Electron: 显示屏幕源选择对话框')
            const selectedSource = await showSourcePicker(sources)
            if (!selectedSource) {
              throw new Error('用户取消了屏幕共享')
            }
            
            console.log('🖥️ Electron: 选择源:', selectedSource.name, 'ID:', selectedSource.id)
            
            // 使用 getUserMedia 获取屏幕流
            console.log('🖥️ Electron: 正在获取屏幕流...')
            console.log('🖥️ Electron: 使用源ID:', selectedSource.id)
            
            const stream = await navigator.mediaDevices.getUserMedia({
              audio: false,
              video: {
                mandatory: {
                  chromeMediaSource: 'desktop',
                  chromeMediaSourceId: selectedSource.id
                }
              }
            })
            
            console.log('🖥️ Electron: 屏幕共享流获取成功')
            console.log('🖥️ Electron: 流ID:', stream.id)
            console.log('🖥️ Electron: 视频轨道数:', stream.getVideoTracks().length)
            
            // 检查视频轨道状态
            const videoTrack = stream.getVideoTracks()[0]
            if (videoTrack) {
              console.log('🖥️ Electron: 视频轨道信息:')
              console.log('  - ID:', videoTrack.id)
              console.log('  - Label:', videoTrack.label)
              console.log('  - Enabled:', videoTrack.enabled)
              console.log('  - Muted:', videoTrack.muted)
              console.log('  - ReadyState:', videoTrack.readyState)
              console.log('  - Settings:', videoTrack.getSettings())
            }
            
            return stream
            
          } catch (error) {
            // 用户取消操作，静默处理
            if (error.message === '用户取消了屏幕共享') {
              console.log('ℹ️ Electron: 用户取消了屏幕共享')
              throw error
            }
            
            // 其他错误才输出详细日志
            console.error('🖥️ Electron: 屏幕共享失败:', error)
            console.error('🖥️ Electron: 错误堆栈:', error.stack)
            throw error
          }
        }
        
        console.log('✅ Electron: getDisplayMedia 已覆盖')
      }
    }
    
    // 显示屏幕源选择对话框
    const showSourcePicker = (sources) => {
      return new Promise((resolve) => {
        // 分类源：屏幕和窗口
        const screens = sources.filter(s => s.id.startsWith('screen:'))
        const windows = sources.filter(s => s.id.startsWith('window:'))
        
        // 创建遮罩层
        const overlay = document.createElement('div')
        overlay.style.cssText = `
          position: fixed;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          background: rgba(0, 0, 0, 0.6);
          display: flex;
          justify-content: center;
          align-items: center;
          z-index: 10000;
          animation: fadeIn 0.2s ease-out;
        `
        
        // 添加淡入动画
        const style = document.createElement('style')
        style.textContent = `
          @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
          }
          @keyframes slideUp {
            from { transform: translateY(20px); opacity: 0; }
            to { transform: translateY(0); opacity: 1; }
          }
          .source-item:hover {
            transform: translateY(-4px);
            box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2) !important;
          }
        `
        document.head.appendChild(style)
        
        // 创建对话框
        const dialog = document.createElement('div')
        dialog.style.cssText = `
          background: #fff;
          border-radius: 12px;
          width: 90%;
          max-width: 720px;
          max-height: 85vh;
          display: flex;
          flex-direction: column;
          box-shadow: 0 24px 48px rgba(0, 0, 0, 0.3);
          animation: slideUp 0.3s ease-out;
          overflow: hidden;
        `
        
        // 标题栏
        const header = document.createElement('div')
        header.style.cssText = `
          padding: 20px 24px;
          border-bottom: 1px solid #e0e0e0;
          display: flex;
          justify-content: space-between;
          align-items: center;
        `
        
        const title = document.createElement('h2')
        title.textContent = '选择要共享的内容'
        title.style.cssText = `
          margin: 0;
          font-size: 20px;
          font-weight: 500;
          color: #202124;
        `
        
        const closeBtn = document.createElement('button')
        closeBtn.innerHTML = '✕'
        closeBtn.style.cssText = `
          background: none;
          border: none;
          font-size: 24px;
          color: #5f6368;
          cursor: pointer;
          padding: 0;
          width: 32px;
          height: 32px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: background 0.2s;
        `
        closeBtn.onmouseover = () => closeBtn.style.background = '#f1f3f4'
        closeBtn.onmouseout = () => closeBtn.style.background = 'none'
        closeBtn.onclick = () => {
          document.body.removeChild(overlay)
          document.head.removeChild(style)
          resolve(null)
        }
        
        header.appendChild(title)
        header.appendChild(closeBtn)
        
        // 标签页容器
        const tabContainer = document.createElement('div')
        tabContainer.style.cssText = `
          display: flex;
          border-bottom: 1px solid #e0e0e0;
          background: #f8f9fa;
        `
        
        let activeTab = screens.length > 0 ? 'screen' : 'window'
        
        const createTab = (label, type, count) => {
          const tab = document.createElement('button')
          tab.textContent = `${label} (${count})`
          tab.style.cssText = `
            flex: 1;
            padding: 12px 24px;
            border: none;
            background: ${type === activeTab ? '#fff' : 'transparent'};
            color: ${type === activeTab ? '#1a73e8' : '#5f6368'};
            font-size: 14px;
            font-weight: 500;
            cursor: pointer;
            border-bottom: 2px solid ${type === activeTab ? '#1a73e8' : 'transparent'};
            transition: all 0.2s;
          `
          tab.onclick = () => {
            activeTab = type
            updateContent()
            // 更新所有标签样式
            Array.from(tabContainer.children).forEach(t => {
              const isActive = t.dataset.type === activeTab
              t.style.background = isActive ? '#fff' : 'transparent'
              t.style.color = isActive ? '#1a73e8' : '#5f6368'
              t.style.borderBottom = `2px solid ${isActive ? '#1a73e8' : 'transparent'}`
            })
          }
          tab.dataset.type = type
          return tab
        }
        
        if (screens.length > 0) {
          tabContainer.appendChild(createTab('整个屏幕', 'screen', screens.length))
        }
        if (windows.length > 0) {
          tabContainer.appendChild(createTab('窗口', 'window', windows.length))
        }
        
        // 内容区域
        const content = document.createElement('div')
        content.style.cssText = `
          flex: 1;
          overflow-y: auto;
          padding: 24px;
        `
        
        const updateContent = () => {
          const sourcesToShow = activeTab === 'screen' ? screens : windows
          content.innerHTML = ''
          
          const grid = document.createElement('div')
          grid.style.cssText = `
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
            gap: 16px;
          `
          
          sourcesToShow.forEach((source) => {
            const item = document.createElement('div')
            item.className = 'source-item'
            item.style.cssText = `
              border: 2px solid #dadce0;
              border-radius: 8px;
              padding: 12px;
              cursor: pointer;
              transition: all 0.2s;
              background: #fff;
              box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
            `
            
            item.onclick = () => {
              document.body.removeChild(overlay)
              document.head.removeChild(style)
              resolve(source)
            }
            
            // 缩略图容器
            const imgContainer = document.createElement('div')
            imgContainer.style.cssText = `
              width: 100%;
              aspect-ratio: 16/9;
              background: #f1f3f4;
              border-radius: 4px;
              overflow: hidden;
              display: flex;
              align-items: center;
              justify-content: center;
            `
            
            const img = document.createElement('img')
            img.src = source.thumbnail
            img.style.cssText = `
              width: 100%;
              height: 100%;
              object-fit: cover;
            `
            
            imgContainer.appendChild(img)
            
            // 名称
            const name = document.createElement('div')
            name.textContent = source.name
            name.style.cssText = `
              margin-top: 12px;
              font-size: 14px;
              color: #202124;
              font-weight: 400;
              text-align: center;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            `
            
            item.appendChild(imgContainer)
            item.appendChild(name)
            grid.appendChild(item)
          })
          
          content.appendChild(grid)
        }
        
        updateContent()
        
        // 底部按钮栏
        const footer = document.createElement('div')
        footer.style.cssText = `
          padding: 16px 24px;
          border-top: 1px solid #e0e0e0;
          display: flex;
          justify-content: flex-end;
          gap: 12px;
          background: #f8f9fa;
        `
        
        const cancelBtn = document.createElement('button')
        cancelBtn.textContent = '取消'
        cancelBtn.style.cssText = `
          padding: 10px 24px;
          background: #fff;
          color: #1a73e8;
          border: 1px solid #dadce0;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          font-weight: 500;
          transition: all 0.2s;
        `
        cancelBtn.onmouseover = () => {
          cancelBtn.style.background = '#f8f9fa'
        }
        cancelBtn.onmouseout = () => {
          cancelBtn.style.background = '#fff'
        }
        cancelBtn.onclick = () => {
          document.body.removeChild(overlay)
          document.head.removeChild(style)
          resolve(null)
        }
        
        footer.appendChild(cancelBtn)
        
        // 组装对话框
        dialog.appendChild(header)
        if (screens.length > 0 || windows.length > 0) {
          dialog.appendChild(tabContainer)
        }
        dialog.appendChild(content)
        dialog.appendChild(footer)
        
        overlay.appendChild(dialog)
        document.body.appendChild(overlay)
      })
    }
    
    // 立即尝试覆盖
    overrideGetDisplayMedia()
    
    // 在 DOMContentLoaded 时再次尝试
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', overrideGetDisplayMedia)
    }
    
    console.log('✅ Electron APIs 已设置')
    
  } catch (error) {
    console.error('❌ 设置 Electron APIs 失败:', error)
    console.error('❌ 错误堆栈:', error.stack)
  }
}

// 立即执行设置
setupElectronAPIs()

// 页面加载完成后的焦点管理
document.addEventListener('DOMContentLoaded', () => {
  console.log('DOM内容加载完成')
  
  // 确保页面获得焦点
  if (document.body) {
    document.body.focus()
  }
  
  // 监听路由变化（Vue Router）
  let currentPath = window.location.pathname
  const observer = new MutationObserver(() => {
    if (window.location.pathname !== currentPath) {
      currentPath = window.location.pathname
      console.log('检测到路由变化:', currentPath)
      
      // 路由变化后重新聚焦
      setTimeout(() => {
        // 尝试聚焦到第一个可聚焦的元素
        const focusableElements = document.querySelectorAll(
          'input, textarea, select, button, [tabindex]:not([tabindex="-1"])'
        )
        
        if (focusableElements.length > 0) {
          focusableElements[0].focus()
        } else {
          // 如果没有可聚焦的元素，聚焦到body
          document.body.focus()
        }
      }, 100)
    }
  })
  
  // 开始观察DOM变化
  observer.observe(document.body, {
    childList: true,
    subtree: true
  })
})

// 监听窗口焦点事件
window.addEventListener('focus', () => {
  console.log('窗口获得焦点')
})

window.addEventListener('blur', () => {
  console.log('窗口失去焦点')
})

// 键盘事件调试
document.addEventListener('keydown', (e) => {
  console.log('键盘按下:', e.key, e.code)
})

document.addEventListener('keyup', (e) => {
  console.log('键盘释放:', e.key, e.code)
})

// 可以在这里添加其他预加载逻辑
console.log('Preload script loaded')

try {
  const { ipcRenderer } = require('electron')
  let lastEditableElement = null

  const isEditableElement = (element) => {
    if (!element || typeof element.matches !== 'function') {
      return false
    }

    return element.matches(
      'input:not([type="button"]):not([type="checkbox"]):not([type="radio"]):not([disabled]), textarea:not([disabled]), [contenteditable="true"], [contenteditable=""], .el-input__inner:not([disabled]), .el-textarea__inner:not([disabled])'
    )
  }

  const rememberEditableTarget = (event) => {
    const target = event.target
    if (isEditableElement(target)) {
      lastEditableElement = target
    }
  }

  const restoreEditableFocus = () => {
    const activeElement = document.activeElement

    if (isEditableElement(activeElement)) {
      return
    }

    const candidate = lastEditableElement && document.contains(lastEditableElement)
      ? lastEditableElement
      : document.querySelector(
          'input:not([type="hidden"]):not([disabled]), textarea:not([disabled]), [contenteditable="true"], [contenteditable=""], .el-input__inner:not([disabled]), .el-textarea__inner:not([disabled])'
        )

    if (!candidate || typeof candidate.focus !== 'function') {
      return
    }

    setTimeout(() => {
      try {
        candidate.focus({ preventScroll: true })
        if (typeof candidate.select === 'function' && candidate.tagName === 'INPUT') {
          candidate.select()
        }
      } catch (error) {
        console.error('恢复输入焦点失败:', error)
      }
    }, 30)
  }

  if (window.electron) {
    window.electron.focusWindow = async () => ipcRenderer.invoke('focus-current-window')
    window.electron.onWindowFocused = (callback) => {
      ipcRenderer.on('window-focused', () => callback())
    }
  }

  document.addEventListener('focusin', rememberEditableTarget, true)
  document.addEventListener('mousedown', rememberEditableTarget, true)
  window.addEventListener('focus', restoreEditableFocus)
  window.addEventListener('mouseup', () => {
    setTimeout(restoreEditableFocus, 0)
  })
  ipcRenderer.on('window-focused', restoreEditableFocus)
} catch (error) {
  console.error('附加焦点恢复逻辑失败:', error)
}
