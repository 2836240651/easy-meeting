/**
 * WebRTC快速修复脚本
 * 在浏览器控制台中运行此脚本来诊断和修复WebRTC连接问题
 */

// 诊断并修复WebRTC连接
async function diagnoseAndFix() {
  console.log('=== WebRTC诊断和修复工具 ===\n')
  
  // 1. 基本检查
  console.log('1️⃣ 基本状态检查')
  const wsConnected = window.meetingWsService?.isConnected
  const userId = window.meetingWsService?.currentUserId
  const meetingId = window.meetingWsService?.currentMeetingId
  const hasLocalStream = !!window.webrtcManager?.localStream
  
  console.log('  WebSocket连接:', wsConnected)
  console.log('  用户ID:', userId)
  console.log('  会议ID:', meetingId)
  console.log('  本地视频流:', hasLocalStream)
  
  if (!wsConnected) {
    console.log('\n❌ WebSocket未连接，无法修复')
    console.log('💡 建议：刷新页面重新加入会议')
    return
  }
  
  if (!hasLocalStream) {
    console.log('\n⚠️ 本地视频流不存在')
    console.log('💡 建议：先开启视频')
    console.log('执行：document.querySelector(".control-button:nth-child(2)").click()')
    return
  }
  
  console.log('\n2️⃣ 检查Peer连接')
  const peerConnections = window.webrtcManager.peerConnections
  
  if (peerConnections.size === 0) {
    console.log('  ⚠️ 没有Peer连接')
    console.log('  💡 可能原因：对方未加入会议或连接未建立')
    return
  }
  
  console.log(`  找到 ${peerConnections.size} 个Peer连接\n`)
  
  // 3. 检查每个连接的状态
  console.log('3️⃣ 连接状态详情')
  let needsFix = false
  const fixes = []
  
  for (const [peerId, pc] of peerConnections.entries()) {
    console.log(`\n  连接 [${peerId}]:`)
    console.log(`    信令状态: ${pc.signalingState}`)
    console.log(`    连接状态: ${pc.connectionState}`)
    console.log(`    ICE状态: ${pc.iceConnectionState}`)
    console.log(`    远程描述: ${pc.remoteDescription ? '存在' : '不存在'}`)
    console.log(`    本地描述: ${pc.localDescription ? '存在' : '不存在'}`)
    
    // 诊断问题
    if (pc.signalingState === 'have-remote-offer') {
      console.log('    🔍 诊断：已收到远程Offer，但未发送Answer')
      needsFix = true
      fixes.push({
        type: 'createAnswer',
        peerId: peerId,
        pc: pc
      })
    } else if (pc.signalingState === 'have-local-offer') {
      console.log('    🔍 诊断：已发送Offer，等待对方Answer')
      console.log('    💡 建议：检查对方是否收到Offer消息')
    } else if (pc.connectionState === 'failed') {
      console.log('    🔍 诊断：连接失败')
      needsFix = true
      fixes.push({
        type: 'reset',
        peerId: peerId,
        pc: pc
      })
    } else if (pc.connectionState === 'connected') {
      console.log('    ✅ 连接正常')
    }
  }
  
  // 4. 执行修复
  if (needsFix && fixes.length > 0) {
    console.log('\n4️⃣ 执行修复')
    
    for (const fix of fixes) {
      if (fix.type === 'createAnswer') {
        console.log(`\n  修复连接 [${fix.peerId}]: 创建Answer`)
        try {
          const answer = await fix.pc.createAnswer()
          await fix.pc.setLocalDescription(answer)
          
          // 发送Answer
          window.meetingWsService.sendMessage({
            messageType: 14, // WEBRTC_ANSWER
            messageSend2Type: 1, // USER
            sendUserId: userId,
            receiveUserId: fix.peerId,
            meetingId: meetingId,
            messageContent: {
              type: answer.type,
              sdp: answer.sdp
            }
          })
          
          console.log('  ✅ Answer已创建并发送')
        } catch (error) {
          console.error('  ❌ 创建Answer失败:', error)
        }
      } else if (fix.type === 'reset') {
        console.log(`\n  修复连接 [${fix.peerId}]: 重置连接`)
        window.webrtcManager.closePeerConnection(fix.peerId)
        setTimeout(() => {
          window.webrtcManager.connectToParticipant(fix.peerId)
        }, 1000)
        console.log('  ✅ 连接已重置')
      }
    }
    
    console.log('\n✅ 修复完成')
    console.log('💡 等待5-10秒后执行 checkConnection() 查看结果')
  } else if (needsFix) {
    console.log('\n⚠️ 检测到问题但无法自动修复')
    console.log('💡 建议：执行 resetAllConnections() 重置所有连接')
  } else {
    console.log('\n✅ 所有连接状态正常')
  }
  
  console.log('\n=== 诊断完成 ===\n')
}

// 检查连接状态
function checkConnection() {
  console.log('=== 连接状态检查 ===\n')
  
  const peerConnections = window.webrtcManager?.peerConnections
  if (!peerConnections || peerConnections.size === 0) {
    console.log('❌ 没有Peer连接')
    return
  }
  
  let allConnected = true
  
  for (const [peerId, pc] of peerConnections.entries()) {
    const isConnected = pc.connectionState === 'connected'
    const hasRemoteStream = window.webrtcManager.remoteStreams.has(peerId)
    
    console.log(`连接 [${peerId}]:`)
    console.log(`  信令状态: ${pc.signalingState}`)
    console.log(`  连接状态: ${pc.connectionState}`)
    console.log(`  ICE状态: ${pc.iceConnectionState}`)
    console.log(`  远程流: ${hasRemoteStream ? '✅ 存在' : '❌ 不存在'}`)
    
    if (!isConnected) {
      allConnected = false
    }
    
    console.log('')
  }
  
  if (allConnected) {
    console.log('✅ 所有连接已建立')
    console.log('💡 应该能看到对方的视频了')
  } else {
    console.log('⚠️ 部分连接未建立')
    console.log('💡 建议：执行 diagnoseAndFix() 尝试修复')
  }
  
  console.log('=== 检查完成 ===\n')
}

// 重置所有连接
function resetAllConnections() {
  console.log('=== 重置所有连接 ===\n')
  
  const peerIds = Array.from(window.webrtcManager?.peerConnections.keys() || [])
  
  if (peerIds.length === 0) {
    console.log('❌ 没有需要重置的连接')
    return
  }
  
  console.log('关闭现有连接:', peerIds)
  peerIds.forEach(peerId => {
    window.webrtcManager.closePeerConnection(peerId)
  })
  
  console.log('\n等待2秒后重新建立连接...')
  setTimeout(() => {
    peerIds.forEach(peerId => {
      console.log('重新连接到:', peerId)
      window.webrtcManager.connectToParticipant(peerId)
    })
    console.log('\n✅ 连接重置完成')
    console.log('💡 等待5-10秒后执行 checkConnection() 查看结果')
  }, 2000)
}

// 添加详细日志
function enableDetailedLogs() {
  console.log('=== 启用详细日志 ===\n')
  
  // 监听Offer
  if (window.webrtcManager && !window._offerLogEnabled) {
    const originalHandleOffer = window.webrtcManager.handleOffer.bind(window.webrtcManager)
    window.webrtcManager.handleOffer = async function(message) {
      console.log('\n🎯🎯🎯 收到Offer消息 🎯🎯🎯')
      console.log('发送者:', message.sendUserId)
      console.log('消息内容:', message.messageContent)
      try {
        const result = await originalHandleOffer(message)
        console.log('✅ Offer处理成功\n')
        return result
      } catch (error) {
        console.error('❌ Offer处理失败:', error, '\n')
        throw error
      }
    }
    window._offerLogEnabled = true
    console.log('✅ Offer日志已启用')
  }
  
  // 监听Answer
  if (window.webrtcManager && !window._answerLogEnabled) {
    const originalHandleAnswer = window.webrtcManager.handleAnswer.bind(window.webrtcManager)
    window.webrtcManager.handleAnswer = async function(message) {
      console.log('\n🎯🎯🎯 收到Answer消息 🎯🎯🎯')
      console.log('发送者:', message.sendUserId)
      console.log('消息内容:', message.messageContent)
      try {
        const result = await originalHandleAnswer(message)
        console.log('✅ Answer处理成功\n')
        return result
      } catch (error) {
        console.error('❌ Answer处理失败:', error, '\n')
        throw error
      }
    }
    window._answerLogEnabled = true
    console.log('✅ Answer日志已启用')
  }
  
  // 监听ICE候选
  if (window.webrtcManager && !window._iceLogEnabled) {
    const originalHandleIce = window.webrtcManager.handleIceCandidate.bind(window.webrtcManager)
    window.webrtcManager.handleIceCandidate = async function(message) {
      console.log('🧊 收到ICE候选:', message.sendUserId)
      try {
        const result = await originalHandleIce(message)
        return result
      } catch (error) {
        console.error('❌ ICE候选处理失败:', error)
        throw error
      }
    }
    window._iceLogEnabled = true
    console.log('✅ ICE日志已启用')
  }
  
  console.log('\n=== 详细日志已启用 ===\n')
}

// 导出函数到全局
window.diagnoseAndFix = diagnoseAndFix
window.checkConnection = checkConnection
window.resetAllConnections = resetAllConnections
window.enableDetailedLogs = enableDetailedLogs

console.log('✅ WebRTC修复工具已加载\n')
console.log('可用命令：')
console.log('  diagnoseAndFix()      - 诊断并自动修复问题')
console.log('  checkConnection()     - 检查连接状态')
console.log('  resetAllConnections() - 重置所有连接')
console.log('  enableDetailedLogs()  - 启用详细日志')
console.log('  debugWebRTC()         - 查看调试信息\n')
console.log('💡 建议：先执行 diagnoseAndFix()\n')
