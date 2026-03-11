/**
 * 设置管理器
 * 统一管理用户设置的读取和应用
 */

// 默认设置
const DEFAULT_SETTINGS = {
  // 会议设置
  defaultVideoOn: false,
  defaultAudioOn: true,
  reminderTime: 10,
  
  // 通知设置
  desktopNotification: true,
  soundNotification: true,
  meetingInviteNotification: true,
  friendRequestNotification: true,
  
  // 隐私设置
  showOnlineStatus: true,
  allowStrangerAdd: true,
  
  // 外观设置
  darkMode: false,
  language: 'zh-CN',
  
  // 视频设置
  videoQuality: 'high',
  mirrorVideo: true,
  virtualBackground: false,
  
  // 音频设置
  echoCancellation: true,
  noiseSuppression: true,
  autoGainControl: true,
  
  // 屏幕共享设置
  shareSystemAudio: true,
  optimizeVideoSharing: true,
  
  // 网络设置
  autoReconnect: true,
  showNetworkStatus: true
}

class SettingsManager {
  constructor() {
    this.settings = this.loadSettings()
    this.listeners = []
  }

  /**
   * 加载设置
   */
  loadSettings() {
    try {
      const saved = localStorage.getItem('userSettings')
      if (saved) {
        return { ...DEFAULT_SETTINGS, ...JSON.parse(saved) }
      }
    } catch (error) {
      console.error('加载设置失败:', error)
    }
    return { ...DEFAULT_SETTINGS }
  }

  /**
   * 保存设置
   */
  saveSettings(newSettings) {
    try {
      this.settings = { ...this.settings, ...newSettings }
      localStorage.setItem('userSettings', JSON.stringify(this.settings))
      this.notifyListeners()
      return true
    } catch (error) {
      console.error('保存设置失败:', error)
      return false
    }
  }

  /**
   * 获取单个设置
   */
  get(key) {
    return this.settings[key]
  }

  /**
   * 获取所有设置
   */
  getAll() {
    return { ...this.settings }
  }

  /**
   * 重置为默认设置
   */
  reset() {
    this.settings = { ...DEFAULT_SETTINGS }
    localStorage.setItem('userSettings', JSON.stringify(this.settings))
    this.notifyListeners()
  }

  /**
   * 监听设置变化
   */
  onChange(callback) {
    this.listeners.push(callback)
    return () => {
      this.listeners = this.listeners.filter(cb => cb !== callback)
    }
  }

  /**
   * 通知监听器
   */
  notifyListeners() {
    this.listeners.forEach(callback => {
      try {
        callback(this.settings)
      } catch (error) {
        console.error('设置监听器执行失败:', error)
      }
    })
  }

  /**
   * 获取视频约束（根据设置）
   */
  getVideoConstraints() {
    const quality = this.get('videoQuality')
    const constraints = {
      width: { ideal: 1280 },
      height: { ideal: 720 },
      frameRate: { ideal: 30 }
    }

    switch (quality) {
      case 'low':
        constraints.width = { ideal: 640 }
        constraints.height = { ideal: 360 }
        constraints.frameRate = { ideal: 15 }
        break
      case 'medium':
        constraints.width = { ideal: 854 }
        constraints.height = { ideal: 480 }
        constraints.frameRate = { ideal: 24 }
        break
      case 'high':
        constraints.width = { ideal: 1280 }
        constraints.height = { ideal: 720 }
        constraints.frameRate = { ideal: 30 }
        break
      case 'ultra':
        constraints.width = { ideal: 1920 }
        constraints.height = { ideal: 1080 }
        constraints.frameRate = { ideal: 30 }
        break
    }

    return constraints
  }

  /**
   * 获取音频约束（根据设置）
   */
  getAudioConstraints() {
    return {
      echoCancellation: this.get('echoCancellation'),
      noiseSuppression: this.get('noiseSuppression'),
      autoGainControl: this.get('autoGainControl')
    }
  }

  /**
   * 获取屏幕共享约束（根据设置）
   */
  getScreenShareConstraints() {
    const optimizeVideo = this.get('optimizeVideoSharing')
    const shareAudio = this.get('shareSystemAudio')

    return {
      video: {
        cursor: 'always',
        displaySurface: 'monitor',
        frameRate: optimizeVideo ? { ideal: 30 } : { ideal: 15 },
        width: { ideal: 1920 },
        height: { ideal: 1080 }
      },
      audio: shareAudio ? {
        echoCancellation: false,
        noiseSuppression: false,
        autoGainControl: false
      } : false
    }
  }

  /**
   * 应用深色模式
   */
  applyDarkMode() {
    const darkMode = this.get('darkMode')
    if (darkMode) {
      document.documentElement.classList.add('dark-theme')
    } else {
      document.documentElement.classList.remove('dark-theme')
    }
  }

  /**
   * 应用镜像视频
   */
  shouldMirrorVideo() {
    return this.get('mirrorVideo')
  }

  /**
   * 是否显示网络状态
   */
  shouldShowNetworkStatus() {
    return this.get('showNetworkStatus')
  }

  /**
   * 是否自动重连
   */
  shouldAutoReconnect() {
    return this.get('autoReconnect')
  }

  /**
   * 获取会议提醒时间（分钟）
   */
  getReminderTime() {
    return this.get('reminderTime')
  }

  /**
   * 是否默认开启视频
   */
  shouldDefaultVideoOn() {
    return this.get('defaultVideoOn')
  }

  /**
   * 是否默认开启音频
   */
  shouldDefaultAudioOn() {
    return this.get('defaultAudioOn')
  }

  /**
   * 是否启用桌面通知
   */
  shouldShowDesktopNotification() {
    return this.get('desktopNotification')
  }

  /**
   * 是否启用声音通知
   */
  shouldPlaySoundNotification() {
    return this.get('soundNotification')
  }

  /**
   * 显示桌面通知
   */
  async showDesktopNotification(title, options = {}) {
    if (!this.shouldShowDesktopNotification()) {
      return
    }

    // 请求通知权限
    if (Notification.permission === 'default') {
      await Notification.requestPermission()
    }

    if (Notification.permission === 'granted') {
      new Notification(title, {
        icon: '/logo.png',
        badge: '/logo.png',
        ...options
      })
    }
  }

  /**
   * 播放通知声音
   */
  playNotificationSound() {
    if (!this.shouldPlaySoundNotification()) {
      return
    }

    try {
      const audio = new Audio('/sounds/notification.mp3')
      audio.volume = 0.5
      audio.play().catch(err => {
        console.warn('播放通知声音失败:', err)
      })
    } catch (error) {
      console.warn('播放通知声音失败:', error)
    }
  }
}

// 导出单例
export const settingsManager = new SettingsManager()

// 页面加载时应用深色模式
settingsManager.applyDarkMode()

export default settingsManager
