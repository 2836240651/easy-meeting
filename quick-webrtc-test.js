/**
 * WebRTC快速测试脚本
 * 在浏览器控制台中运行此脚本来诊断WebRTC连接问题
 */

// 用户A执行此脚本
function testUserA() {
  console.log('=== 用户A WebRTC诊断 ===\n')
  
  // 1. 基本状态检查
  console.log('1️⃣ 基本状态检查')
  console.log('  WebSocket连接:', window.meetingWsService?.isConnected)
  console.log('  用户ID:', window.meetingWsService?.currentUserId)
  console.log('  会议ID:', window.meetingWsService?.currentMeetingId)
  console.log('  本地视频流:', !!window.webrtcManager?.localStream)
  console.log('')
  
  // 2. WebRTC状态
  console.log('2️⃣ WebRTC连接状态')
  if (window.webrtcManager) {
    console.log('  Peer连接数:', window.webrtcManager.peerConnections.size)
    console.log('  远程流数:', window.webrtcManager.remoteStreams.size)
    
    window.webrtcManager.peerConnections.forEach((pc, userId) => {
      console.log(`  连接 [${userId}]:`, {
        signaling: pc.signalingState,
        connection: pc.connectionState,
        ice: pc.iceConnectionState
      })
    })
  }
  console.log('')
  
  // 3. 检查视频状态
  console.log('3️⃣ 视频状态')
  const videoBtn = document.querySelector('.control-button:nth-child(2)')
  const isVideoOn = !videoBtn?.classList.contains('active')
  console.log('  视频开启:', isVideoOn)
  
  if (!isVideoOn) {
    console.log('  ⚠️ 视频未开启！')
    console.log('  💡 建议：点击视频按钮开启视频')
    console.log('  或执行：document.querySelector(".control-button:nth-child(2)").click()')
  }
  console.log('')
  
  // 4. 设置Offer消息监听
  console.log('4️⃣ 设置消息监听')
  if (window.webrtcManager && !window._offerListenerSet) {
    window._offerCount = 0
    const original = window.webrtcManager.handleOffer.bind(window.webrtcManager)
    window.webrtcManager.handleOffer = async function(message) {
      window._offerCount++
      console.log(`\n🎯 收到第 ${window._offerCount} 个Offer消息`)
      console.log('  发送者:', message.sendUserId)
      console.log('  SDP类型:', message.messageContent?.type)
      return await original(message)
    }
    window._offerListenerSet = true
    console.log('  ✅ Offer监听已设置')
    console.log('  等待接收Offer消息...')
  } else if (window._offerListenerSet) {
    console.log('  ℹ️ Offer监听已经设置过了')
    console.log('  已收到Offer数量:', window._offerCount || 0)
  }
  console.log('')
  
  // 5. 诊断建议
  console.log('5️⃣ 诊断建议')
  if (!window.meetingWsService?.isConnected) {
    console.log('  ❌ WebSocket未连接！')
    console.log('  💡 建议：刷新页面重新加入会议')
  } else if (!window.webrtcManager?.localStream) {
    console.log('  ⚠️ 本地视频流不存在')
    console.log('  💡 建议：开启视频')
  } else {
    console.log('  ✅ 状态正常，等待对方发送Offer')
    console.log('  💡 确保对方也开启了视频')
  }
  console.log('\n=== 诊断完成 ===\n')
}

// 用户B执行此脚本
function testUserB() {
  console.log('=== 用户B WebRTC诊断 ===\n')
  
  // 1. 基本状态检查
  console.log('1️⃣ 基本状态检查')
  console.log('  WebSocket连接:', window.meetingWsService?.isConnected)
  console.log('  用户ID:', window.meetingWsService?.currentUserId)
  console.log('  会议ID:', window.meetingWsService?.currentMeetingId)
  console.log('  本地视频流:', !!window.webrtcManager?.localStream)
  console.log('')
  
  // 2. WebRTC状态
  console.log('2️⃣ WebRTC连接状态')
  if (window.webrtcManager) {
    console.log('  Peer连接数:', window.webrtcManager.peerConnections.size)
    console.log('  远程流数:', window.webrtcManager.remoteStreams.size)
    
    window.webrtcManager.peerConnections.forEach((pc, userId) => {
      console.log(`  连接 [${userId}]:`, {
        signaling: pc.signalingState,
        connection: pc.connectionState,
        ice: pc.iceConnectionState
      })
      
      // 检查是否卡在have-local-offer
      if (pc.signalingState === 'have-local-offer') {
        console.log(`  ⚠️ 连接 [${userId}] 卡在 have-local-offer 状态`)
        console.log('  这意味着：Offer已发送，但未收到Answer')
        console.log('  可能原因：')
        console.log('    1. 对方未收到Offer消息')
        console.log('    2. 对方WebSocket连接异常')
        console.log('    3. 对方未开启视频')
      }
    })
  }
  console.log('')
  
  // 3. 检查视频状态
  console.log('3️⃣ 视频状态')
  const videoBtn = document.querySelector('.control-button:nth-child(2)')
  const isVideoOn = !videoBtn?.classList.contains('active')
  console.log('  视频开启:', isVideoOn)
  console.log('')
  
  // 4. 设置Answer消息监听
  console.log('4️⃣ 设置消息监听')
  if (window.webrtcManager && !window._answerListenerSet) {
    window._answerCount = 0
    const original = window.webrtcManager.handleAnswer.bind(window.webrtcManager)
    window.webrtcManager.handleAnswer = async function(message) {
      window._answerCount++
      console.log(`\n🎯 收到第 ${window._answerCount} 个Answer消息`)
      console.log('  发送者:', message.sendUserId)
      console.log('  SDP类型:', message.messageContent?.type)
      return await original(message)
    }
    window._answerListenerSet = true
    console.log('  ✅ Answer监听已设置')
    console.log('  等待接收Answer消息...')
  } else if (window._answerListenerSet) {
    console.log('  ℹ️ Answer监听已经设置过了')
    console.log('  已收到Answer数量:', window._answerCount || 0)
  }
  console.log('')
  
  // 5. 诊断建议
  console.log('5️⃣ 诊断建议')
  const pc = Array.from(window.webrtcManager?.peerConnections.values())[0]
  if (!window.meetingWsService?.isConnected) {
    console.log('  ❌ WebSocket未连接！')
    console.log('  💡 建议：刷新页面重新加入会议')
  } else if (!window.webrtcManager?.localStream) {
    console.log('  ⚠️ 本地视频流不存在')
    console.log('  💡 建议：开启视频')
  } else if (pc?.signalingState === 'have-local-offer') {
    console.log('  ⚠️ 信令交换未完成')
    console.log('  💡 建议：')
    console.log('    1. 确认对方已开启视频')
    console.log('    2. 检查对方的WebSocket连接')
    console.log('    3. 查看后端日志确认消息转发')
    console.log('    4. 如果等待超过10秒，执行重置命令：')
    console.log('       resetConnection()')
  } else if (pc?.connectionState === 'connected') {
    console.log('  ✅ 连接已建立！')
    console.log('  💡 应该能看到对方的视频了')
  } else {
    console.log('  ⏳ 等待连接建立...')
  }
  console.log('\n=== 诊断完成 ===\n')
}

// 重置连接（用户B执行）
function resetConnection() {
  console.log('🔄 重置WebRTC连接...\n')
  
  if (!window.webrtcManager) {
    console.log('❌ WebRTC管理器不存在')
    return
  }
  
  const peerIds = Array.from(window.webrtcManager.peerConnections.keys())
  console.log('关闭现有连接:', peerIds)
  
  peerIds.forEach(userId => {
    window.webrtcManager.closePeerConnection(userId)
  })
  
  console.log('等待1秒后重新建立连接...')
  setTimeout(() => {
    peerIds.forEach(userId => {
      console.log('重新连接到:', userId)
      window.webrtcManager.connectToParticipant(userId)
    })
    console.log('\n✅ 连接重置完成')
    console.log('💡 等待5-10秒后执行 testUserB() 检查状态')
  }, 1000)
}

// 检查后端消息转发（两个用户都可以执行）
function checkBackendLogs() {
  console.log('=== 后端日志检查指南 ===\n')
  console.log('在后端日志中搜索以下关键词：\n')
  console.log('1️⃣ WebRTC Offer消息')
  console.log('   搜索：发送点对点消息: 类型=13')
  console.log('   期望：发送点对点消息: 类型=13, 发送者=xxx, 接收者=xxx')
  console.log('         ✅ 消息已发送到用户: xxx\n')
  
  console.log('2️⃣ WebRTC Answer消息')
  console.log('   搜索：发送点对点消息: 类型=14')
  console.log('   期望：发送点对点消息: 类型=14, 发送者=xxx, 接收者=xxx')
  console.log('         ✅ 消息已发送到用户: xxx\n')
  
  console.log('3️⃣ ICE候选消息')
  console.log('   搜索：发送点对点消息: 类型=15')
  console.log('   期望：多条ICE候选消息\n')
  
  console.log('4️⃣ 错误日志')
  console.log('   搜索：error, exception, 失败, 异常')
  console.log('   检查是否有WebSocket或消息转发相关的错误\n')
  
  console.log('=== 检查完成 ===\n')
}

// 导出函数到全局
window.testUserA = testUserA
window.testUserB = testUserB
window.resetConnection = resetConnection
window.checkBackendLogs = checkBackendLogs

console.log('✅ WebRTC测试脚本已加载\n')
console.log('可用命令：')
console.log('  testUserA()        - 用户A执行诊断')
console.log('  testUserB()        - 用户B执行诊断')
console.log('  resetConnection()  - 重置连接（用户B执行）')
console.log('  checkBackendLogs() - 后端日志检查指南')
console.log('  debugWebRTC()      - 详细调试信息\n')
