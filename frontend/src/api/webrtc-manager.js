/**
 * WebRTC管理器
 * 负责管理所有peer连接和视频流
 */

class WebRTCManager {
  constructor() {
    // 存储所有peer连接: { userId: RTCPeerConnection }
    this.peerConnections = new Map()
    
    // 存储屏幕共享peer连接: { userId: RTCPeerConnection }
    this.screenShareConnections = new Map()
    
    // 存储远程视频流: { userId: MediaStream }
    this.remoteStreams = new Map()
    
    // 存储远程屏幕共享流: { userId: MediaStream }
    this.remoteScreenStreams = new Map()
    
    // 本地视频流
    this.localStream = null
    
    // 本地屏幕共享流
    this.localScreenStream = null
    
    // WebSocket服务（用于信令）
    this.wsService = null
    
    // 当前用户ID
    this.currentUserId = null
    
    // 当前会议ID
    this.meetingId = null
    
    // ICE服务器配置
    this.iceServers = {
      iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' },
        { urls: 'stun:stun2.l.google.com:19302' }
      ]
    }
    
    // 事件回调
    this.onRemoteStreamAdded = null
    this.onRemoteStreamRemoved = null
    this.onRemoteScreenStreamAdded = null
    this.onRemoteScreenStreamRemoved = null
    this.onConnectionStateChange = null
  }
  
  /**
   * 初始化WebRTC管理器
   */
  init(wsService, currentUserId, meetingId) {
    console.log('🎬 初始化WebRTC管理器, userId:', currentUserId, 'meetingId:', meetingId)
    this.wsService = wsService
    this.currentUserId = currentUserId
    this.meetingId = meetingId
    
    // 注册WebRTC信令消息处理器
    this.setupSignalingHandlers()
  }
  
  /**
   * 设置信令消息处理器
   */
  setupSignalingHandlers() {
    if (!this.wsService) {
      console.error('❌ WebSocket服务未初始化')
      return
    }
    
    // 处理WebRTC Offer
    this.wsService.on('webrtcOffer', async (message) => {
      console.log('📨 收到WebRTC Offer:', message)
      await this.handleOffer(message)
    })
    
    // 处理WebRTC Answer
    this.wsService.on('webrtcAnswer', async (message) => {
      console.log('📨 收到WebRTC Answer:', message)
      await this.handleAnswer(message)
    })
    
    // 处理ICE候选
    this.wsService.on('webrtcIceCandidate', async (message) => {
      console.log('📨 收到ICE候选:', message)
      await this.handleIceCandidate(message)
    })
    
    // 处理屏幕共享 Offer
    this.wsService.on('screenShareOffer', async (message) => {
      console.log('📨 收到屏幕共享 Offer:', message)
      await this.handleScreenShareOffer(message)
    })
    
    // 处理屏幕共享 Answer
    this.wsService.on('screenShareAnswer', async (message) => {
      console.log('📨 收到屏幕共享 Answer:', message)
      await this.handleScreenShareAnswer(message)
    })
    
    // 处理屏幕共享 ICE候选
    this.wsService.on('screenShareIceCandidate', async (message) => {
      console.log('📨 收到屏幕共享 ICE候选:', message)
      await this.handleScreenShareIceCandidate(message)
    })
  }
  
  /**
   * 设置本地视频流
   */
  setLocalStream(stream) {
    console.log('📹 设置本地视频流')
    this.localStream = stream
    
    // 为所有现有的peer连接添加本地流并重新协商
    this.peerConnections.forEach(async (pc, userId) => {
      await this.addLocalStreamToPeerAndRenegotiate(pc, userId)
    })
  }
  
  /**
   * 为peer连接添加本地流
   */
  addLocalStreamToPeer(peerConnection, userId) {
    if (!this.localStream) {
      console.warn('⚠️ 本地流不存在，无法添加到peer连接')
      return
    }
    
    console.log('➕ 添加本地流到peer连接:', userId)
    
    // 检查是否已经添加过轨道
    const senders = peerConnection.getSenders()
    const existingTrackIds = senders.map(sender => sender.track?.id).filter(Boolean)
    
    // 添加所有音视频轨道
    this.localStream.getTracks().forEach(track => {
      if (!existingTrackIds.includes(track.id)) {
        console.log(`  添加轨道: ${track.kind} (${track.label})`)
        peerConnection.addTrack(track, this.localStream)
      } else {
        console.log(`  轨道已存在，跳过: ${track.kind} (${track.label})`)
      }
    })
  }
  
  /**
   * 为peer连接添加本地流并重新协商
   */
  async addLocalStreamToPeerAndRenegotiate(peerConnection, userId) {
    if (!this.localStream) {
      console.warn('⚠️ 本地流不存在，无法添加到peer连接')
      return
    }
    
    console.log('➕ 添加本地流到peer连接并重新协商:', userId)
    
    // 检查是否已经添加过轨道
    const senders = peerConnection.getSenders()
    const existingTrackIds = senders.map(sender => sender.track?.id).filter(Boolean)
    
    let needsRenegotiation = false
    
    // 添加所有音视频轨道
    this.localStream.getTracks().forEach(track => {
      if (!existingTrackIds.includes(track.id)) {
        console.log(`  添加轨道: ${track.kind} (${track.label})`)
        peerConnection.addTrack(track, this.localStream)
        needsRenegotiation = true
      } else {
        console.log(`  轨道已存在，跳过: ${track.kind} (${track.label})`)
      }
    })
    
    // 如果添加了新轨道，需要重新协商
    if (needsRenegotiation) {
      console.log('🔄 需要重新协商，当前信令状态:', peerConnection.signalingState)
      
      if (peerConnection.signalingState === 'stable') {
        console.log('  → 信令状态stable，发送新的Offer')
        await this.createAndSendOffer(userId, peerConnection)
      } else if (peerConnection.signalingState === 'have-local-offer') {
        console.log('  → 已有本地Offer但对方未响应')
        console.log('  → 这可能是因为对方还没有开启视频或连接有问题')
        console.log('  → 关闭并重新创建连接')
        
        // 关闭旧连接
        peerConnection.close()
        this.peerConnections.delete(userId)
        
        // 重新创建连接（作为发起方）
        const shouldInitiate = this.currentUserId < userId
        console.log('  → 重新创建连接，是否发起:', shouldInitiate)
        await this.createPeerConnection(userId, shouldInitiate)
        
      } else if (peerConnection.signalingState === 'have-remote-offer') {
        console.log('  → 已收到远程Offer，创建Answer')
        const answer = await peerConnection.createAnswer()
        await peerConnection.setLocalDescription(answer)
        this.sendAnswer(userId, answer)
      } else {
        console.warn('  → 信令状态不稳定，等待状态恢复:', peerConnection.signalingState)
      }
    } else {
      console.log('  → 轨道已存在，无需重新协商')
    }
  }
  
  /**
   * 创建与指定用户的peer连接
   */
  async createPeerConnection(userId, isInitiator = false) {
    if (this.peerConnections.has(userId)) {
      console.log('⚠️ Peer连接已存在:', userId)
      return this.peerConnections.get(userId)
    }
    
    console.log(`🔗 创建Peer连接: ${userId} (发起方: ${isInitiator})`)
    
    const peerConnection = new RTCPeerConnection(this.iceServers)
    this.peerConnections.set(userId, peerConnection)
    
    // 监听ICE候选
    peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        console.log('🧊 发送ICE候选到:', userId)
        this.sendIceCandidate(userId, event.candidate)
      }
    }
    
    // 监听远程流
    peerConnection.ontrack = (event) => {
      console.log('📺 收到远程流:', userId, event.streams[0])
      const remoteStream = event.streams[0]
      this.remoteStreams.set(userId, remoteStream)
      
      // 触发回调
      if (this.onRemoteStreamAdded) {
        this.onRemoteStreamAdded(userId, remoteStream)
      }
    }
    
    // 监听连接状态变化
    peerConnection.onconnectionstatechange = () => {
      console.log(`🔌 连接状态变化 [${userId}]:`, peerConnection.connectionState)
      
      if (this.onConnectionStateChange) {
        this.onConnectionStateChange(userId, peerConnection.connectionState)
      }
      
      // 如果连接失败或关闭，清理资源
      if (peerConnection.connectionState === 'failed' || 
          peerConnection.connectionState === 'closed') {
        this.closePeerConnection(userId)
      }
    }
    
    // 监听ICE连接状态
    peerConnection.oniceconnectionstatechange = () => {
      console.log(`❄️ ICE连接状态 [${userId}]:`, peerConnection.iceConnectionState)
    }
    
    // 添加本地流
    if (this.localStream) {
      this.addLocalStreamToPeer(peerConnection, userId)
    }
    
    // 如果是发起方，创建offer
    if (isInitiator) {
      await this.createAndSendOffer(userId, peerConnection)
    }
    
    return peerConnection
  }
  
  /**
   * 创建并发送Offer
   */
  async createAndSendOffer(userId, peerConnection) {
    try {
      console.log('📤📤📤 创建Offer发送给:', userId, '📤📤📤')
      console.log('当前信令状态:', peerConnection.signalingState)
      
      const offer = await peerConnection.createOffer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: true
      })
      
      console.log('✅ Offer已创建')
      console.log('Offer类型:', offer.type)
      console.log('SDP长度:', offer.sdp?.length)
      
      await peerConnection.setLocalDescription(offer)
      console.log('✅ 本地描述已设置')
      console.log('新的信令状态:', peerConnection.signalingState)
      
      // 通过WebSocket发送offer
      console.log('准备发送Offer到WebSocket...')
      this.sendOffer(userId, offer)
      console.log('✅✅✅ Offer已发送 ✅✅✅')
      
    } catch (error) {
      console.error('❌ 创建Offer失败:', error)
      console.error('错误详情:', error.message)
      console.error('错误堆栈:', error.stack)
    }
  }
  
  /**
   * 处理收到的Offer
   */
  async handleOffer(message) {
    const { sendUserId, messageContent } = message
    
    console.log('🎯🎯🎯 收到Offer消息 🎯🎯🎯')
    console.log('发送者:', sendUserId)
    console.log('当前用户:', this.currentUserId)
    
    if (!messageContent || !messageContent.sdp) {
      console.error('❌ Offer消息格式错误')
      return
    }
    
    try {
      console.log('📥 处理Offer来自:', sendUserId)
      
      // 检查是否已经有连接
      let peerConnection = this.peerConnections.get(sendUserId)
      
      if (peerConnection) {
        console.log('⚠️ Peer连接已存在，当前状态:', peerConnection.signalingState)
        
        // 处理glare情况（双方同时发送offer）
        if (peerConnection.signalingState === 'have-local-offer') {
          console.log('🔄 检测到glare（双方同时发送offer）')
          console.log('  当前用户ID:', this.currentUserId)
          console.log('  发送者ID:', sendUserId)
          console.log('  比较结果:', this.currentUserId, this.currentUserId > sendUserId ? '>' : '<', sendUserId)
          
          // 比较userId，较大的一方回滚并接受对方的offer
          if (this.currentUserId > sendUserId) {
            console.log('  → 当前用户ID较大，回滚本地offer并接受对方的offer')
            try {
              await peerConnection.setLocalDescription({type: 'rollback'})
              console.log('  ✅ 本地offer已回滚')
            } catch (rollbackError) {
              console.error('  ❌ 回滚失败:', rollbackError)
              // 如果回滚失败，关闭连接重新创建
              console.log('  → 关闭连接并重新创建')
              peerConnection.close()
              this.peerConnections.delete(sendUserId)
              peerConnection = await this.createPeerConnection(sendUserId, false)
            }
          } else {
            console.log('  → 当前用户ID较小，应该由我们发起连接')
            console.log('  → 但对方也发送了Offer，说明连接逻辑有问题')
            console.log('  → 为了避免死锁，ID较小的一方也接受对方的Offer')
            // 回滚本地offer
            try {
              await peerConnection.setLocalDescription({type: 'rollback'})
              console.log('  ✅ 本地offer已回滚')
            } catch (rollbackError) {
              console.error('  ❌ 回滚失败:', rollbackError)
              peerConnection.close()
              this.peerConnections.delete(sendUserId)
              peerConnection = await this.createPeerConnection(sendUserId, false)
            }
          }
        }
      } else {
        // 创建新的peer连接（不是发起方）
        console.log('创建新的peer连接（接收方）')
        peerConnection = await this.createPeerConnection(sendUserId, false)
      }
      
      // 设置远程描述
      console.log('设置远程描述...')
      await peerConnection.setRemoteDescription(new RTCSessionDescription(messageContent))
      console.log('✅ 远程描述已设置')
      
      // 创建answer
      console.log('创建Answer...')
      const answer = await peerConnection.createAnswer()
      await peerConnection.setLocalDescription(answer)
      console.log('✅ 本地描述（Answer）已设置')
      
      // 发送answer
      console.log('发送Answer...')
      this.sendAnswer(sendUserId, answer)
      
      console.log('✅✅✅ Answer已发送给:', sendUserId, '✅✅✅')
      
    } catch (error) {
      console.error('❌ 处理Offer失败:', error)
      console.error('错误详情:', error.message)
      console.error('错误堆栈:', error.stack)
    }
  }
  
  /**
   * 处理收到的Answer
   */
  async handleAnswer(message) {
    const { sendUserId, messageContent } = message
    
    console.log('🎯🎯🎯 收到Answer消息 🎯🎯🎯')
    console.log('发送者:', sendUserId)
    
    if (!messageContent || !messageContent.sdp) {
      console.error('❌ Answer消息格式错误')
      return
    }
    
    try {
      console.log('📥 处理Answer来自:', sendUserId)
      
      const peerConnection = this.peerConnections.get(sendUserId)
      if (!peerConnection) {
        console.error('❌ 找不到对应的peer连接:', sendUserId)
        return
      }
      
      console.log('当前信令状态:', peerConnection.signalingState)
      
      await peerConnection.setRemoteDescription(new RTCSessionDescription(messageContent))
      
      console.log('✅✅✅ Answer已处理:', sendUserId, '✅✅✅')
      console.log('新的信令状态:', peerConnection.signalingState)
      
    } catch (error) {
      console.error('❌ 处理Answer失败:', error)
      console.error('错误详情:', error.message)
    }
  }
  
  /**
   * 处理收到的ICE候选
   */
  async handleIceCandidate(message) {
    const { sendUserId, messageContent } = message
    
    console.log('🧊 收到ICE候选:', sendUserId)
    
    if (!messageContent) {
      console.error('❌ ICE候选消息格式错误')
      return
    }
    
    try {
      const peerConnection = this.peerConnections.get(sendUserId)
      if (!peerConnection) {
        console.warn('⚠️ 找不到对应的peer连接，忽略ICE候选:', sendUserId)
        return
      }
      
      // 检查远程描述是否已设置
      if (!peerConnection.remoteDescription) {
        console.warn('⚠️ 远程描述未设置，暂时无法添加ICE候选')
        return
      }
      
      await peerConnection.addIceCandidate(new RTCIceCandidate(messageContent))
      console.log('✅ ICE候选已添加:', sendUserId)
      
    } catch (error) {
      console.error('❌ 添加ICE候选失败:', error)
      console.error('错误详情:', error.message)
    }
  }
  
  /**
   * 发送Offer
   */
  sendOffer(targetUserId, offer) {
    if (!this.wsService) {
      console.error('❌ WebSocket服务不存在，无法发送Offer')
      return
    }
    
    console.log('📤 发送Offer到:', targetUserId)
    console.log('  Offer类型:', offer.type)
    console.log('  SDP长度:', offer.sdp?.length)
    console.log('  当前用户ID:', this.currentUserId)
    console.log('  会议ID:', this.meetingId)
    console.log('  wsService类型:', this.wsService.constructor.name)
    console.log('  wsService.sendMessage存在:', typeof this.wsService.sendMessage)
    
    const message = {
      messageType: 13, // WEBRTC_OFFER
      messageSend2Type: 0, // USER (0=个人, 1=群体)
      sendUserId: this.currentUserId,
      receiveUserId: targetUserId,
      meetingId: this.meetingId,
      messageContent: {
        type: offer.type,
        sdp: offer.sdp
      }
    }
    
    console.log('📤 完整消息对象:', JSON.stringify(message).substring(0, 200) + '...')
    
    try {
      const result = this.wsService.sendMessage(message)
      console.log('✅ sendMessage调用结果:', result)
    } catch (error) {
      console.error('❌ sendMessage调用失败:', error)
    }
  }
  
  /**
   * 发送Answer
   */
  sendAnswer(targetUserId, answer) {
    if (!this.wsService) return
    
    console.log('📤 发送Answer到:', targetUserId)
    
    this.wsService.sendMessage({
      messageType: 14, // WEBRTC_ANSWER
      messageSend2Type: 0, // USER (0=个人, 1=群体)
      sendUserId: this.currentUserId,
      receiveUserId: targetUserId,
      meetingId: this.meetingId,
      messageContent: {
        type: answer.type,
        sdp: answer.sdp
      }
    })
  }
  
  /**
   * 发送ICE候选
   */
  sendIceCandidate(targetUserId, candidate) {
    if (!this.wsService) return
    
    console.log('📤 发送ICE候选到:', targetUserId)
    
    this.wsService.sendMessage({
      messageType: 15, // WEBRTC_ICE_CANDIDATE
      messageSend2Type: 0, // USER (0=个人, 1=群体)
      sendUserId: this.currentUserId,
      receiveUserId: targetUserId,
      meetingId: this.meetingId,
      messageContent: {
        candidate: candidate.candidate,
        sdpMLineIndex: candidate.sdpMLineIndex,
        sdpMid: candidate.sdpMid
      }
    })
  }
  
  /**
   * 与所有参与者建立连接
   */
  async connectToAllParticipants(participants) {
    console.log('🌐 与所有参与者建立连接，参与者数:', participants.length)
    
    for (const participant of participants) {
      if (participant.userId !== this.currentUserId) {
        console.log(`准备与用户 ${participant.userId} 建立连接`)
        console.log(`  当前用户ID: ${this.currentUserId}`)
        console.log(`  对方用户ID: ${participant.userId}`)
        
        // 使用字符串比较决定谁发起连接（避免glare问题）
        const shouldInitiate = this.currentUserId < participant.userId
        console.log(`  是否发起连接: ${shouldInitiate}`)
        
        await this.createPeerConnection(participant.userId, shouldInitiate)
      }
    }
  }
  
  /**
   * 与新参与者建立连接
   */
  async connectToParticipant(userId) {
    if (userId === this.currentUserId) {
      console.log('⚠️ 不能与自己建立连接')
      return
    }
    
    console.log('🤝 与新参与者建立连接:', userId)
    console.log(`  当前用户ID: ${this.currentUserId}`)
    console.log(`  新用户ID: ${userId}`)
    
    // 使用字符串比较决定谁发起连接
    const shouldInitiate = this.currentUserId < userId
    console.log(`  是否发起连接: ${shouldInitiate}`)
    
    await this.createPeerConnection(userId, shouldInitiate)
    
    // 如果当前正在屏幕共享，也为新成员创建屏幕共享连接
    if (this.localScreenStream) {
      console.log('🖥️ 当前正在屏幕共享，为新成员创建屏幕共享连接')
      await this.createScreenShareConnection(userId)
    }
  }
  
  /**
   * 关闭与指定用户的连接
   */
  closePeerConnection(userId) {
    console.log('🔌 关闭Peer连接:', userId)
    
    const peerConnection = this.peerConnections.get(userId)
    if (peerConnection) {
      peerConnection.close()
      this.peerConnections.delete(userId)
    }
    
    const remoteStream = this.remoteStreams.get(userId)
    if (remoteStream) {
      remoteStream.getTracks().forEach(track => track.stop())
      this.remoteStreams.delete(userId)
      
      // 触发回调
      if (this.onRemoteStreamRemoved) {
        this.onRemoteStreamRemoved(userId)
      }
    }
  }
  
  /**
   * 关闭所有连接
   */
  closeAllConnections() {
    console.log('🔌 关闭所有Peer连接')
    
    this.peerConnections.forEach((pc, userId) => {
      this.closePeerConnection(userId)
    })
    
    this.peerConnections.clear()
    this.remoteStreams.clear()
  }
  
  /**
   * 替换视频轨道（用于屏幕共享）
   * @param {MediaStreamTrack|null} newTrack - 新的视频轨道，null表示移除视频
   */
  async replaceVideoTrack(newTrack) {
    console.log('🔄 替换所有连接的视频轨道')
    console.log('新轨道类型:', newTrack?.kind)
    console.log('新轨道标签:', newTrack?.label)
    console.log('当前连接数:', this.peerConnections.size)
    
    const replacePromises = []
    
    // 遍历所有peer连接
    this.peerConnections.forEach((peerConnection, userId) => {
      console.log(`  → 处理用户 ${userId} 的视频轨道`)
      
      // 获取所有发送器
      const senders = peerConnection.getSenders()
      console.log(`    发送器数量: ${senders.length}`)
      
      // 查找视频发送器
      const videoSender = senders.find(sender => {
        const track = sender.track
        const isVideo = track && track.kind === 'video'
        console.log(`    检查发送器: kind=${track?.kind}, label=${track?.label}, isVideo=${isVideo}`)
        return isVideo
      })
      
      if (videoSender) {
        console.log(`    ✅ 找到视频发送器，准备替换`)
        // 替换轨道（可以是新轨道或null）
        const promise = videoSender.replaceTrack(newTrack)
          .then(() => {
            if (newTrack) {
              console.log(`    ✅ 用户 ${userId} 的视频轨道替换成功`)
            } else {
              console.log(`    ✅ 用户 ${userId} 的视频轨道已移除`)
            }
          })
          .catch(error => {
            console.error(`    ❌ 用户 ${userId} 的视频轨道操作失败:`, error)
          })
        
        replacePromises.push(promise)
      } else if (newTrack) {
        // 如果没有视频发送器但有新轨道，需要添加轨道
        console.log(`    ⚠️ 未找到视频发送器，添加新轨道`)
        try {
          peerConnection.addTrack(newTrack, this.localStream || new MediaStream([newTrack]))
          console.log(`    ✅ 用户 ${userId} 的视频轨道已添加`)
          
          // 添加轨道后需要重新协商
          if (peerConnection.signalingState === 'stable') {
            const promise = this.createAndSendOffer(userId, peerConnection)
            replacePromises.push(promise)
          }
        } catch (error) {
          console.error(`    ❌ 添加视频轨道失败:`, error)
        }
      } else {
        console.log(`    ℹ️ 没有视频发送器且新轨道为null，无需操作`)
      }
    })
    
    // 等待所有替换完成
    await Promise.all(replacePromises)
    console.log('✅ 所有视频轨道操作完成')
  }
  
  /**
   * 获取远程视频流
   */
  getRemoteStream(userId) {
    return this.remoteStreams.get(userId)
  }
  
  /**
   * 获取远程屏幕共享流
   */
  getRemoteScreenStream(userId) {
    return this.remoteScreenStreams.get(userId)
  }
  
  /**
   * 开始屏幕共享
   */
  async startScreenShare(screenStream) {
    console.log('🖥️🖥️🖥️ 开始屏幕共享，为所有参与者创建屏幕共享连接')
    console.log('🖥️ 屏幕流ID:', screenStream.id)
    console.log('🖥️ 屏幕流轨道数:', screenStream.getTracks().length)
    
    this.localScreenStream = screenStream
    
    // 为所有参与者创建屏幕共享连接
    const participants = Array.from(this.peerConnections.keys())
    console.log('🖥️ 当前参与者数量:', participants.length)
    console.log('🖥️ 参与者列表:', participants)
    
    if (participants.length === 0) {
      console.log('ℹ️ 当前没有其他参与者，屏幕共享仅本地显示')
      return
    }
    
    for (const userId of participants) {
      console.log(`🖥️ 为用户 ${userId} 创建屏幕共享连接...`)
      await this.createScreenShareConnection(userId)
    }
    
    console.log('✅ 所有屏幕共享连接已创建')
  }
  
  /**
   * 停止屏幕共享
   */
  async stopScreenShare() {
    console.log('🖥️ 停止屏幕共享，关闭所有屏幕共享连接')
    
    // 关闭所有屏幕共享连接
    this.screenShareConnections.forEach((pc, userId) => {
      console.log(`  关闭用户 ${userId} 的屏幕共享连接`)
      pc.close()
    })
    
    this.screenShareConnections.clear()
    this.localScreenStream = null
  }
  
  /**
   * 替换屏幕共享轨道（用于切换共享源）
   */
  async replaceScreenShareTrack(newVideoTrack) {
    console.log('🔄 替换屏幕共享视频轨道')
    
    if (!this.localScreenStream) {
      console.warn('⚠️ 没有活动的屏幕共享流')
      return
    }
    
    // 更新本地屏幕流
    const oldVideoTrack = this.localScreenStream.getVideoTracks()[0]
    if (oldVideoTrack) {
      this.localScreenStream.removeTrack(oldVideoTrack)
    }
    this.localScreenStream.addTrack(newVideoTrack)
    
    // 替换所有屏幕共享连接中的视频轨道
    const replacePromises = []
    this.screenShareConnections.forEach((pc, userId) => {
      console.log(`  替换用户 ${userId} 的屏幕共享视频轨道`)
      
      const senders = pc.getSenders()
      const videoSender = senders.find(sender => sender.track && sender.track.kind === 'video')
      
      if (videoSender) {
        const promise = videoSender.replaceTrack(newVideoTrack)
          .then(() => {
            console.log(`  ✅ 用户 ${userId} 的视频轨道已替换`)
          })
          .catch(error => {
            console.error(`  ❌ 替换用户 ${userId} 的视频轨道失败:`, error)
          })
        replacePromises.push(promise)
      }
    })
    
    await Promise.all(replacePromises)
    console.log('✅ 所有屏幕共享视频轨道已替换')
  }
  
  /**
   * 创建屏幕共享连接
   */
  async createScreenShareConnection(userId) {
    console.log(`🖥️ 为用户 ${userId} 创建屏幕共享连接`)
    
    // 创建新的 PeerConnection
    const pc = new RTCPeerConnection(this.iceServers)
    this.screenShareConnections.set(userId, pc)
    
    // 添加屏幕共享流
    if (this.localScreenStream) {
      this.localScreenStream.getTracks().forEach(track => {
        console.log(`  添加屏幕共享轨道: ${track.kind}`)
        pc.addTrack(track, this.localScreenStream)
      })
    }
    
    // 处理ICE候选
    pc.onicecandidate = (event) => {
      if (event.candidate) {
        console.log(`🧊 屏幕共享ICE候选生成，发送给用户 ${userId}`)
        this.wsService.sendMessage({
          messageType: 20, // SCREEN_SHARE_ICE_CANDIDATE
          messageSend2Type: 0, // USER
          sendUserId: this.currentUserId,
          receiveUserId: userId,
          meetingId: this.meetingId,
          messageContent: {
            candidate: event.candidate
          },
          sendTime: Date.now()
        })
      }
    }
    
    // 处理连接状态变化
    pc.onconnectionstatechange = () => {
      console.log(`🔗 屏幕共享连接状态变化 (${userId}):`, pc.connectionState)
    }
    
    // 创建并发送 Offer
    try {
      const offer = await pc.createOffer()
      await pc.setLocalDescription(offer)
      
      console.log(`📤 发送屏幕共享 Offer 给用户 ${userId}`)
      this.wsService.sendMessage({
        messageType: 18, // SCREEN_SHARE_OFFER
        messageSend2Type: 0, // USER
        sendUserId: this.currentUserId,
        receiveUserId: userId,
        meetingId: this.meetingId,
        messageContent: {
          sdp: offer
        },
        sendTime: Date.now()
      })
    } catch (error) {
      console.error(`❌ 创建屏幕共享 Offer 失败:`, error)
    }
  }
  
  /**
   * 处理屏幕共享 Offer
   */
  async handleScreenShareOffer(message) {
    const { sendUserId, messageContent } = message
    
    console.log(`📥 处理屏幕共享 Offer 来自:`, sendUserId)
    
    if (!messageContent || !messageContent.sdp) {
      console.error('❌ 屏幕共享 Offer 消息格式错误')
      return
    }
    
    try {
      // 创建新的 PeerConnection
      const pc = new RTCPeerConnection(this.iceServers)
      this.screenShareConnections.set(sendUserId, pc)
      
      // 处理远程屏幕共享流
      pc.ontrack = (event) => {
        console.log(`📺 收到屏幕共享流来自 ${sendUserId}`)
        const stream = event.streams[0]
        this.remoteScreenStreams.set(sendUserId, stream)
        
        // 触发回调
        if (this.onRemoteScreenStreamAdded) {
          this.onRemoteScreenStreamAdded(sendUserId, stream)
        }
      }
      
      // 处理ICE候选
      pc.onicecandidate = (event) => {
        if (event.candidate) {
          console.log(`🧊 屏幕共享ICE候选生成，发送给用户 ${sendUserId}`)
          this.wsService.sendMessage({
            messageType: 20, // SCREEN_SHARE_ICE_CANDIDATE
            messageSend2Type: 0, // USER
            sendUserId: this.currentUserId,
            receiveUserId: sendUserId,
            meetingId: this.meetingId,
            messageContent: {
              candidate: event.candidate
            },
            sendTime: Date.now()
          })
        }
      }
      
      // 设置远程描述
      await pc.setRemoteDescription(new RTCSessionDescription(messageContent.sdp))
      
      // 创建并发送 Answer
      const answer = await pc.createAnswer()
      await pc.setLocalDescription(answer)
      
      console.log(`📤 发送屏幕共享 Answer 给用户 ${sendUserId}`)
      this.wsService.sendMessage({
        messageType: 19, // SCREEN_SHARE_ANSWER
        messageSend2Type: 0, // USER
        sendUserId: this.currentUserId,
        receiveUserId: sendUserId,
        meetingId: this.meetingId,
        messageContent: {
          sdp: answer
        },
        sendTime: Date.now()
      })
    } catch (error) {
      console.error(`❌ 处理屏幕共享 Offer 失败:`, error)
    }
  }
  
  /**
   * 处理屏幕共享 Answer
   */
  async handleScreenShareAnswer(message) {
    const { sendUserId, messageContent } = message
    
    console.log(`📥 处理屏幕共享 Answer 来自:`, sendUserId)
    
    if (!messageContent || !messageContent.sdp) {
      console.error('❌ 屏幕共享 Answer 消息格式错误')
      return
    }
    
    const pc = this.screenShareConnections.get(sendUserId)
    if (!pc) {
      console.error(`❌ 未找到用户 ${sendUserId} 的屏幕共享连接`)
      return
    }
    
    try {
      await pc.setRemoteDescription(new RTCSessionDescription(messageContent.sdp))
      console.log(`✅ 屏幕共享 Answer 已设置`)
    } catch (error) {
      console.error(`❌ 设置屏幕共享 Answer 失败:`, error)
    }
  }
  
  /**
   * 处理屏幕共享 ICE候选
   */
  async handleScreenShareIceCandidate(message) {
    const { sendUserId, messageContent } = message
    
    if (!messageContent || !messageContent.candidate) {
      console.error('❌ 屏幕共享 ICE候选消息格式错误')
      return
    }
    
    const pc = this.screenShareConnections.get(sendUserId)
    if (!pc) {
      console.error(`❌ 未找到用户 ${sendUserId} 的屏幕共享连接`)
      return
    }
    
    try {
      await pc.addIceCandidate(new RTCIceCandidate(messageContent.candidate))
      console.log(`✅ 屏幕共享 ICE候选已添加`)
    } catch (error) {
      console.error(`❌ 添加屏幕共享 ICE候选失败:`, error)
    }
  }
  
  /**
   * 清理资源
   */
  destroy() {
    console.log('🧹 清理WebRTC管理器')
    
    this.closeAllConnections()
    
    // 关闭所有屏幕共享连接
    this.screenShareConnections.forEach(pc => pc.close())
    this.screenShareConnections.clear()
    
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop())
      this.localStream = null
    }
    
    if (this.localScreenStream) {
      this.localScreenStream.getTracks().forEach(track => track.stop())
      this.localScreenStream = null
    }
    
    this.wsService = null
    this.currentUserId = null
  }
}

// 导出单例
export const webrtcManager = new WebRTCManager()
