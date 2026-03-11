import { ref } from 'vue'
import { settingsManager } from '@/utils/settings-manager.js'

class WebSocketService {
  constructor() {
    this.ws = null
    this.isConnected = ref(false)
    this.messageHandlers = new Map()
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = Infinity // 无限重连
    this.baseReconnectInterval = 5000 // 基础重连间隔 5秒
    this.maxReconnectInterval = 60000 // 最大重连间隔 60秒
    this.reconnectInterval = 5000 // 当前重连间隔
    this.heartbeatInterval = null
    this.heartbeatTimer = 20000 // 20秒心跳
    this.heartbeatTimeout = null // 心跳超时定时器
    this.heartbeatTimeoutDuration = 10000 // 10秒心跳超时
    this.missedHeartbeats = 0 // 错过的心跳次数
    this.maxMissedHeartbeats = 3 // 最大允许错过的心跳次数
    this.isReconnecting = false
    this.shouldReconnect = true
    this.currentToken = null
    this.currentUserId = null
    this.connectionPromise = null
    this.lastHeartbeatTime = null // 最后一次心跳时间
    this.reconnectTimeoutId = null // 重连定时器ID
  }

  // 连接WebSocket
  connect(token, userId) {
    // 检查连接状态：必须同时满足 ws 存在、readyState 为 OPEN、且 isConnected 为 true
    if (this.ws && this.ws.readyState === WebSocket.OPEN && this.isConnected.value) {
      console.log('WebSocket已连接，跳过重复连接')
      console.log('连接状态:', {
        readyState: this.ws.readyState,
        isConnected: this.isConnected.value,
        lastHeartbeat: this.lastHeartbeatTime ? new Date(this.lastHeartbeatTime).toLocaleTimeString() : '无'
      })
      return Promise.resolve()
    }

    // 如果 readyState 显示已连接但 isConnected 为 false，说明连接可能已断开
    if (this.ws && this.ws.readyState === WebSocket.OPEN && !this.isConnected.value) {
      console.warn('⚠️ WebSocket readyState 为 OPEN 但 isConnected 为 false，可能连接已断开')
      console.log('强制关闭旧连接并重新连接...')
      try {
        this.ws.close()
      } catch (e) {
        console.error('关闭旧连接失败:', e)
      }
      this.ws = null
    }

    // 如果正在连接中，返回现有的连接Promise
    if (this.ws && this.ws.readyState === WebSocket.CONNECTING) {
      console.log('WebSocket正在连接中，等待连接完成')
      return this.connectionPromise || Promise.resolve()
    }

    if (this.isReconnecting) {
      console.log('正在重连中，跳过重复连接')
      return Promise.resolve()
    }

    // 保存当前连接参数
    this.currentToken = token
    this.currentUserId = userId
    this.shouldReconnect = true

    this.connectionPromise = new Promise((resolve, reject) => {
      try {
        console.log('🔌🔌🔌 开始建立WebSocket连接 🔌🔌🔌')
        console.log('URL: ws://localhost:6098/ws')
        console.log('Token:', token ? `存在(长度${token.length})` : '不存在')
        console.log('用户ID:', userId)
        
        // 将token作为查询参数传递
        this.ws = new WebSocket(`ws://localhost:6098/ws?token=${encodeURIComponent(token)}`)
        console.log('WebSocket对象已创建，等待连接...')
        
        this.ws.onopen = () => {
          console.log('✅✅✅ WebSocket连接成功 ✅✅✅')
          console.log('连接URL: ws://localhost:6098/ws')
          console.log('Token长度:', token?.length)
          console.log('用户ID:', userId)
          this.isConnected.value = true
          this.reconnectAttempts = 0
          this.isReconnecting = false
          
          // 开始心跳
          this.startHeartbeat()
          resolve()
        }

        this.ws.onmessage = (event) => {
          try {
            // 处理心跳响应
            if (event.data === 'pong') {
              console.log('✅ 收到心跳响应')
              this.lastHeartbeatTime = Date.now()
              this.missedHeartbeats = 0 // 重置错过的心跳计数
              
              // 清除心跳超时定时器
              if (this.heartbeatTimeout) {
                clearTimeout(this.heartbeatTimeout)
                this.heartbeatTimeout = null
              }
              return
            }
            
            const message = JSON.parse(event.data)
            console.log('📥 WebSocket收到原始消息:', message)
            console.log('消息类型 (messageType):', message.messageType)
            this.handleMessage(message)
          } catch (error) {
            console.error('解析WebSocket消息失败:', error)
          }
        }

        this.ws.onclose = (event) => {
          console.log('❌❌❌ WebSocket连接关闭 ❌❌❌')
          console.log('关闭代码:', event.code)
          console.log('关闭原因:', event.reason || '无')
          console.log('是否正常关闭:', event.code === 1000 || event.code === 1001)
          this.isConnected.value = false
          this.stopHeartbeat()
          this.connectionPromise = null
          
          // 只有在应该重连且不是正常关闭的情况下才重连
          if (this.shouldReconnect && event.code !== 1000 && event.code !== 1001) {
            console.log('🔄 准备重连...')
            this.scheduleReconnect()
          } else {
            console.log('⏹️ 不重连 (shouldReconnect:', this.shouldReconnect, ', code:', event.code, ')')
          }
        }

        this.ws.onerror = (error) => {
          console.error('❌❌❌ WebSocket错误 ❌❌❌')
          console.error('错误对象:', error)
          console.error('readyState:', this.ws?.readyState)
          this.isConnected.value = false
          this.connectionPromise = null
          reject(error)
        }

      } catch (error) {
        console.error('WebSocket连接失败:', error)
        this.isReconnecting = false
        this.connectionPromise = null
        reject(error)
      }
    })

    return this.connectionPromise
  }

  // 计划重连
  scheduleReconnect() {
    // 检查是否启用自动重连
    if (!settingsManager.shouldAutoReconnect()) {
      console.log('⏹️ 自动重连已禁用，不进行重连')
      this.isReconnecting = false
      return
    }
    
    if (this.isReconnecting || !this.shouldReconnect) {
      return
    }

    this.isReconnecting = true
    this.reconnectAttempts++
    
    // 使用指数退避算法计算延迟
    // 延迟 = min(基础间隔 * 2^(尝试次数-1), 最大间隔)
    const exponentialDelay = this.baseReconnectInterval * Math.pow(2, this.reconnectAttempts - 1)
    const delay = Math.min(exponentialDelay, this.maxReconnectInterval)
    
    console.log(`⏳ 计划重连 (第 ${this.reconnectAttempts} 次) 在 ${delay}ms 后`)
    console.log(`📊 重连策略: 基础间隔=${this.baseReconnectInterval}ms, 当前延迟=${delay}ms, 最大间隔=${this.maxReconnectInterval}ms`)
    console.log(`⚙️ 自动重连设置: ${settingsManager.shouldAutoReconnect() ? '启用' : '禁用'}`)
    
    this.reconnectTimeoutId = setTimeout(() => {
      if (this.shouldReconnect && this.currentToken && this.currentUserId) {
        console.log(`🔄 开始第 ${this.reconnectAttempts} 次重连尝试...`)
        this.connect(this.currentToken, this.currentUserId)
          .then(() => {
            console.log('✅ 重连成功')
            this.reconnectAttempts = 0 // 重置重连计数
            this.isReconnecting = false
          })
          .catch(error => {
            console.error(`❌ 第 ${this.reconnectAttempts} 次重连失败:`, error)
            this.isReconnecting = false
            // 继续尝试重连（无限重连）
            this.scheduleReconnect()
          })
      } else {
        this.isReconnecting = false
        console.log('⏹️ 取消重连（缺少必要参数）')
      }
    }, delay)
  }
  
  // 取消重连
  cancelReconnect() {
    if (this.reconnectTimeoutId) {
      clearTimeout(this.reconnectTimeoutId)
      this.reconnectTimeoutId = null
    }
    this.isReconnecting = false
    console.log('⏹️ 已取消重连')
  }
  
  // 通知连接失败
  notifyConnectionFailed() {
    console.error('🚨 WebSocket连接失败，请检查网络或刷新页面')
    // 可以在这里触发一个全局事件，让UI显示错误提示
    const handlers = this.messageHandlers.get('CONNECTION_FAILED')
    if (handlers) {
      handlers.forEach(handler => {
        try {
          handler({ error: 'WebSocket连接失败，已达到最大重连次数' })
        } catch (error) {
          console.error('连接失败处理器执行失败:', error)
        }
      })
    }
  }

  // 发送消息
  sendMessage(message) {
    console.log('📤📤📤 WebSocket.sendMessage 被调用')
    console.log('📤📤📤 消息对象:', message)
    console.log('📤📤📤 消息类型:', message.messageType)
    console.log('📤📤📤 messageSend2Type:', message.messageSend2Type)
    console.log('📤📤📤 发送者:', message.sendUserId)
    console.log('📤📤📤 接收者:', message.receiveUserId)
    console.log('📤📤📤 WebSocket状态:', this.ws?.readyState)
    
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const jsonStr = JSON.stringify(message)
      console.log('📤📤📤 JSON字符串长度:', jsonStr.length)
      console.log('📤📤📤 JSON字符串前200字符:', jsonStr.substring(0, 200))
      this.ws.send(jsonStr)
      console.log('📤📤📤 ✅ 消息已发送到WebSocket')
    } else {
      console.warn('📤📤📤 ❌ WebSocket未连接，无法发送消息')
      console.warn('📤📤📤 WebSocket状态:', this.ws?.readyState)
    }
  }

  // 处理接收到的消息
  handleMessage(message) {
    const messageType = message.messageType
    console.log(`🔍 处理消息类型: ${messageType}`)
    console.log(`🔍 完整消息对象:`, message)
    
    const handlers = this.messageHandlers.get(messageType)
    
    if (handlers) {
      console.log(`找到 ${handlers.length} 个处理器用于消息类型 ${messageType}`)
      handlers.forEach(handler => {
        try {
          handler(message)
        } catch (error) {
          console.error('消息处理器执行失败:', error)
        }
      })
    } else {
      console.warn(`⚠️ 没有找到消息类型 ${messageType} 的处理器`)
      console.warn(`⚠️ 已注册的消息类型:`, Array.from(this.messageHandlers.keys()))
    }
  }

  // 注册消息处理器
  onMessage(messageType, handler) {
    if (!this.messageHandlers.has(messageType)) {
      this.messageHandlers.set(messageType, [])
    }
    this.messageHandlers.get(messageType).push(handler)
  }

  // 移除消息处理器
  offMessage(messageType, handler) {
    const handlers = this.messageHandlers.get(messageType)
    if (handlers) {
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    }
  }

  // 开始心跳
  startHeartbeat() {
    this.stopHeartbeat() // 先停止之前的心跳
    console.log('🫀 启动心跳机制，间隔:', this.heartbeatTimer, 'ms')
    this.lastHeartbeatTime = Date.now()
    this.missedHeartbeats = 0
    
    this.heartbeatInterval = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        // 检查上次心跳是否超时
        const timeSinceLastHeartbeat = Date.now() - (this.lastHeartbeatTime || Date.now())
        
        if (timeSinceLastHeartbeat > this.heartbeatTimer + this.heartbeatTimeoutDuration) {
          console.warn('⚠️ 心跳超时，连接可能已断开')
          this.missedHeartbeats++
          
          if (this.missedHeartbeats >= this.maxMissedHeartbeats) {
            console.error('❌ 连续错过多次心跳，主动关闭连接并重连')
            this.stopHeartbeat()
            if (this.ws) {
              this.ws.close()
            }
            return
          }
        }
        
        // 发送心跳
        console.log('🫀 发送心跳 ping')
        this.ws.send('ping')
        
        // 设置心跳超时定时器
        this.heartbeatTimeout = setTimeout(() => {
          console.warn('⚠️ 心跳响应超时')
          this.missedHeartbeats++
          
          if (this.missedHeartbeats >= this.maxMissedHeartbeats) {
            console.error('❌ 心跳响应超时次数过多，主动关闭连接')
            this.stopHeartbeat()
            if (this.ws) {
              this.ws.close()
            }
          }
        }, this.heartbeatTimeoutDuration)
        
      } else {
        // 如果连接不正常，停止心跳并尝试重连
        console.warn('🫀 心跳检测到连接异常，状态:', this.ws?.readyState)
        this.stopHeartbeat()
        
        // 如果应该重连，则触发重连
        if (this.shouldReconnect && this.currentToken && this.currentUserId) {
          console.log('🫀 触发自动重连...')
          this.scheduleReconnect()
        }
      }
    }, this.heartbeatTimer)
  }

  // 停止心跳
  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
    if (this.heartbeatTimeout) {
      clearTimeout(this.heartbeatTimeout)
      this.heartbeatTimeout = null
    }
    this.missedHeartbeats = 0
  }

  // 断开连接
  disconnect() {
    console.log('主动断开WebSocket连接')
    this.shouldReconnect = false // 阻止自动重连
    this.cancelReconnect() // 取消任何待处理的重连
    this.stopHeartbeat()
    
    if (this.ws) {
      this.ws.close(1000, '用户主动断开') // 正常关闭
      this.ws = null
    }
    
    this.isConnected.value = false
    this.isReconnecting = false
    this.connectionPromise = null
    this.currentToken = null
    this.currentUserId = null
    this.reconnectAttempts = 0
    this.missedHeartbeats = 0
    this.lastHeartbeatTime = null
  }
  
  // 获取连接状态
  getConnectionState() {
    return {
      isConnected: this.isConnected.value,
      isReconnecting: this.isReconnecting,
      reconnectAttempts: this.reconnectAttempts,
      maxReconnectAttempts: this.maxReconnectAttempts,
      readyState: this.ws?.readyState,
      missedHeartbeats: this.missedHeartbeats,
      lastHeartbeatTime: this.lastHeartbeatTime
    }
  }
  
  // 手动触发重连
  manualReconnect() {
    if (this.currentToken && this.currentUserId) {
      console.log('🔄 手动触发重连...')
      this.reconnectAttempts = 0 // 重置重连计数
      this.shouldReconnect = true
      this.isReconnecting = false
      
      // 先断开现有连接
      if (this.ws) {
        this.ws.close()
        this.ws = null
      }
      
      // 重新连接
      return this.connect(this.currentToken, this.currentUserId)
    } else {
      console.error('❌ 无法重连：缺少token或userId')
      return Promise.reject(new Error('缺少必要的连接参数'))
    }
  }
}

// 创建全局WebSocket服务实例
export const wsService = new WebSocketService()

// 消息类型枚举
export const MessageType = {
  INIT: 0,
  ADD_MEETING_ROOM: 1,
  PEER: 2,
  EXIT_MEETING_ROOM: 3,
  FINISH_MEETING: 4,
  CHAT_TEXT_MESSAGE: 5,
  CHAT_MEDIA_MESSAGE: 6,
  CHAT_MEDIA_MESSAGE_UPDATE: 7,
  USER_CONTACT_APPLY: 8,
  INVITE_MEMBER_MEETING: 9,
  FORCE_OFF_LINE: 10,
  MEETING_USER_VIDEO_CHANGE: 11,
  USER_CONTACT_DEAL_WITH: 12,
  WEBRTC_OFFER: 13,
  WEBRTC_ANSWER: 14,
  WEBRTC_ICE_CANDIDATE: 15,
  SCREEN_SHARE_START: 16,
  SCREEN_SHARE_STOP: 17,
  SCREEN_SHARE_OFFER: 18,
  SCREEN_SHARE_ANSWER: 19,
  SCREEN_SHARE_ICE_CANDIDATE: 20,
  USER_ONLINE_STATUS_CHANGE: 21,
  USER_CONTACT_DELETE: 22
}

export default wsService