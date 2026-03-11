import { wsService, MessageType } from './websocket.js'

/**
 * 会议WebSocket服务
 * 专门处理会议相关的WebSocket消息
 */
class MeetingWebSocketService {
  constructor() {
    this.isConnected = false
    this.currentMeetingId = null
    this.currentUserId = null
    this.messageHandlers = new Map()
    
    // 绑定消息处理器
    this.setupMessageHandlers()
  }

  /**
   * 连接到会议WebSocket
   * @param {string} token - 用户token
   * @param {string} userId - 用户ID
   * @param {string} meetingId - 会议ID
   */
  async connect(token, userId, meetingId) {
    try {
      console.log('连接会议WebSocket...', { userId, meetingId })
      
      // 连接基础WebSocket服务
      await wsService.connect(token, userId)
      
      this.currentUserId = userId
      this.currentMeetingId = meetingId
      this.isConnected = true
      
      console.log('会议WebSocket连接成功')
      return true
    } catch (error) {
      console.error('会议WebSocket连接失败:', error)
      this.isConnected = false
      throw error
    }
  }

  /**
   * 设置消息处理器
   */
  setupMessageHandlers() {
    // 加入会议房间消息
    wsService.onMessage(MessageType.ADD_MEETING_ROOM, (message) => {
      console.log('收到加入会议房间消息:', message)
      this.handleMessage('memberJoined', message)
    })

    // 退出会议房间消息
    wsService.onMessage(MessageType.EXIT_MEETING_ROOM, (message) => {
      console.log('收到退出会议房间消息:', message)
      this.handleMessage('memberLeft', message)
    })

    // 结束会议消息
    wsService.onMessage(MessageType.FINISH_MEETING, (message) => {
      console.log('收到结束会议消息:', message)
      this.handleMessage('meetingFinished', message)
    })

    // 强制下线消息（被踢出）
    wsService.onMessage(MessageType.FORCE_OFF_LINE, (message) => {
      console.log('收到强制下线消息:', message)
      this.handleMessage('forceOffline', message)
    })

    // 文本聊天消息
    wsService.onMessage(MessageType.CHAT_TEXT_MESSAGE, (message) => {
      console.log('📨 WebSocket收到文本聊天消息 (MessageType=5):', message)
      console.log('消息详情:', {
        messageType: message.messageType,
        messageContent: message.messageContent,
        sendUserId: message.sendUserId,
        sendUserNickName: message.sendUserNickName,
        meetingId: message.meetingId,
        sendTime: message.sendTime
      })
      this.handleMessage('chatMessage', message)
      console.log('✅ 消息已分发到chatMessage事件处理器')
    })

    // 媒体聊天消息
    wsService.onMessage(MessageType.CHAT_MEDIA_MESSAGE, (message) => {
      console.log('收到媒体消息:', message)
      this.handleMessage('mediaMessage', message)
    })

    // 用户视频状态改变
    wsService.onMessage(MessageType.MEETING_USER_VIDEO_CHANGE, (message) => {
      console.log('收到用户视频状态改变消息:', message)
      this.handleMessage('videoStatusChanged', message)
    })

    // 邀请成员入会
    wsService.onMessage(MessageType.INVITE_MEMBER_MEETING, (message) => {
      console.log('收到邀请成员消息:', message)
      this.handleMessage('memberInvited', message)
    })

    // WebRTC Peer消息
    wsService.onMessage(MessageType.PEER, (message) => {
      console.log('收到Peer消息:', message)
      this.handleMessage('peerMessage', message)
    })
    
    // WebRTC Offer消息
    wsService.onMessage(13, (message) => { // WEBRTC_OFFER
      console.log('收到WebRTC Offer消息:', message)
      this.handleMessage('webrtcOffer', message)
    })
    
    // WebRTC Answer消息
    wsService.onMessage(14, (message) => { // WEBRTC_ANSWER
      console.log('收到WebRTC Answer消息:', message)
      this.handleMessage('webrtcAnswer', message)
    })
    
    // WebRTC ICE候选消息
    wsService.onMessage(15, (message) => { // WEBRTC_ICE_CANDIDATE
      console.log('收到WebRTC ICE候选消息:', message)
      this.handleMessage('webrtcIceCandidate', message)
    })
    
    // 屏幕共享开始消息
    wsService.onMessage(16, (message) => { // SCREEN_SHARE_START
      console.log('收到屏幕共享开始消息:', message)
      this.handleMessage('screenShareStart', message)
    })
    
    // 屏幕共享停止消息
    wsService.onMessage(17, (message) => { // SCREEN_SHARE_STOP
      console.log('收到屏幕共享停止消息:', message)
      this.handleMessage('screenShareStop', message)
    })
    
    // 屏幕共享 Offer 消息
    wsService.onMessage(18, (message) => { // SCREEN_SHARE_OFFER
      console.log('收到屏幕共享 Offer 消息:', message)
      this.handleMessage('screenShareOffer', message)
    })
    
    // 屏幕共享 Answer 消息
    wsService.onMessage(19, (message) => { // SCREEN_SHARE_ANSWER
      console.log('收到屏幕共享 Answer 消息:', message)
      this.handleMessage('screenShareAnswer', message)
    })
    
    // 屏幕共享 ICE候选 消息
    wsService.onMessage(20, (message) => { // SCREEN_SHARE_ICE_CANDIDATE
      console.log('收到屏幕共享 ICE候选 消息:', message)
      this.handleMessage('screenShareIceCandidate', message)
    })
  }

  /**
   * 处理消息
   * @param {string} eventType - 事件类型
   * @param {object} message - 消息内容
   */
  handleMessage(eventType, message) {
    const handlers = this.messageHandlers.get(eventType)
    if (handlers) {
      handlers.forEach(handler => {
        try {
          handler(message)
        } catch (error) {
          console.error(`处理${eventType}消息失败:`, error)
        }
      })
    }
  }

  /**
   * 注册消息处理器
   * @param {string} eventType - 事件类型
   * @param {function} handler - 处理函数
   */
  on(eventType, handler) {
    if (!this.messageHandlers.has(eventType)) {
      this.messageHandlers.set(eventType, [])
    }
    this.messageHandlers.get(eventType).push(handler)
  }

  /**
   * 移除消息处理器
   * @param {string} eventType - 事件类型
   * @param {function} handler - 处理函数
   */
  off(eventType, handler) {
    const handlers = this.messageHandlers.get(eventType)
    if (handlers) {
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    }
  }

  /**
   * 发送视频状态改变消息
   * @param {boolean} videoOpen - 视频是否开启
   * @param {string} nickName - 用户昵称
   */
  sendVideoStatusChange(videoOpen, nickName) {
    if (!this.isConnected || !this.currentMeetingId) {
      console.warn('WebSocket未连接或未加入会议，无法发送视频状态消息')
      return false
    }

    const message = {
      messageType: MessageType.MEETING_USER_VIDEO_CHANGE,
      messageSend2Type: 2, // GROUP类型
      meetingId: this.currentMeetingId,
      sendUserId: this.currentUserId,
      sendUserNickName: nickName,
      messageContent: {
        userId: this.currentUserId,
        videoOpen: videoOpen
      },
      sendTime: Date.now()
    }

    console.log('发送视频状态改变消息:', message)
    wsService.sendMessage(message)
    return true
  }

  /**
   * 发送退出会议消息
   * @param {string} nickName - 用户昵称
   */
  sendExitMeeting(nickName) {
    if (!this.isConnected || !this.currentMeetingId) {
      console.warn('WebSocket未连接或未加入会议，无法发送退出消息')
      return false
    }

    const message = {
      messageType: MessageType.EXIT_MEETING_ROOM,
      messageSend2Type: 2, // GROUP类型
      meetingId: this.currentMeetingId,
      sendUserId: this.currentUserId,
      sendUserNickName: nickName,
      messageContent: {
        exitUserId: this.currentUserId,
        exitUserNickName: nickName
      },
      sendTime: Date.now()
    }

    console.log('发送退出会议消息:', message)
    wsService.sendMessage(message)
    return true
  }

  /**
   * 发送结束会议消息（仅主持人）
   * @param {string} nickName - 主持人昵称
   */
  sendFinishMeeting(nickName) {
    if (!this.isConnected || !this.currentMeetingId) {
      console.warn('WebSocket未连接或未加入会议，无法发送结束会议消息')
      return false
    }

    const message = {
      messageType: MessageType.FINISH_MEETING,
      messageSend2Type: 2, // GROUP类型
      meetingId: this.currentMeetingId,
      sendUserId: this.currentUserId,
      sendUserNickName: nickName,
      messageContent: {
        finishUserId: this.currentUserId,
        finishUserNickName: nickName
      },
      sendTime: Date.now()
    }

    console.log('发送结束会议消息:', message)
    wsService.sendMessage(message)
    return true
  }

  /**
   * 发送WebRTC Peer消息
   * @param {string} receiveUserId - 接收者用户ID
   * @param {object} signalData - 信令数据
   * @param {string} signalType - 信令类型
   */
  sendPeerMessage(receiveUserId, signalData, signalType) {
    if (!this.isConnected) {
      console.warn('WebSocket未连接，无法发送Peer消息')
      return false
    }

    const message = {
      token: localStorage.getItem('token'),
      receiveUserId: receiveUserId,
      signalData: signalData,
      signalType: signalType
    }

    console.log('发送Peer消息:', message)
    wsService.sendMessage(message)
    return true
  }
  
  /**
   * 发送WebSocket消息（通用方法）
   * @param {object} message - 消息对象
   */
  sendMessage(message) {
    if (!this.isConnected) {
      console.warn('WebSocket未连接，无法发送消息')
      return false
    }
    
    wsService.sendMessage(message)
    return true
  }

  /**
   * 断开连接
   */
  disconnect() {
    console.log('断开会议WebSocket连接')
    
    // 如果在会议中，发送退出消息
    if (this.isConnected && this.currentMeetingId) {
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
      this.sendExitMeeting(userInfo.nickName || '用户')
    }

    this.isConnected = false
    this.currentMeetingId = null
    this.currentUserId = null
    
    // 清理消息处理器
    this.messageHandlers.clear()
    
    // 注意：不要断开底层的 WebSocket 连接
    // 因为 Dashboard 还需要使用它来接收其他消息（如联系人申请、在线状态等）
    console.log('会议WebSocket已断开，但保持底层WebSocket连接用于Dashboard')
  }

  /**
   * 获取连接状态
   */
  getConnectionStatus() {
    return {
      isConnected: this.isConnected,
      meetingId: this.currentMeetingId,
      userId: this.currentUserId
    }
  }
}

// 创建全局会议WebSocket服务实例
export const meetingWsService = new MeetingWebSocketService()

// 重新导出MessageType，以便其他模块可以使用
export { MessageType } from './websocket.js'

// 消息发送类型枚举
export const MessageSend2Type = {
  USER: 1,    // 发送给特定用户
  GROUP: 2    // 发送给群组（会议房间）
}

// 成员类型枚举
export const MemberType = {
  NORMAL: 0,    // 普通成员
  HOST: 1       // 主持人
}

// 成员状态枚举
export const MemberStatus = {
  DEL_MEETING: 0,   // 删除会议
  NORMAL: 1,        // 正常
  EXIT_MEETING: 2,  // 退出会议
  KICK_OUT: 3,      // 被踢出会议
  BLACKLIST: 4      // 被拉黑
}

export default meetingWsService