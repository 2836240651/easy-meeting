// WebSocket消息诊断脚本
// 在用户A的浏览器控制台运行此脚本

console.log('=== WebSocket诊断开始 ===')

// 1. 检查WebSocket连接
console.log('1. WebSocket连接状态:')
console.log('  isConnected:', meetingWsService.isConnected)
console.log('  readyState:', meetingWsService.wsService.ws.readyState)
console.log('  readyState说明:', {
  0: 'CONNECTING',
  1: 'OPEN',
  2: 'CLOSING',
  3: 'CLOSED'
}[meetingWsService.wsService.ws.readyState])

// 2. 检查用户信息
console.log('\n2. 用户信息:')
console.log('  当前用户ID:', currentUserId.value)
console.log('  会议ID:', meetingId.value)

// 3. 检查消息处理器
console.log('\n3. 已注册的消息处理器:')
console.log('  WebSocket消息处理器数量:', meetingWsService.wsService.messageHandlers.size)
meetingWsService.wsService.messageHandlers.forEach((handlers, type) => {
  console.log(`  类型 ${type}: ${handlers.length} 个处理器`)
})

// 4. 检查WebRTC消息处理器
console.log('\n4. WebRTC消息处理器:')
console.log('  类型13 (OFFER):', meetingWsService.wsService.messageHandlers.get(13)?.length || 0)
console.log('  类型14 (ANSWER):', meetingWsService.wsService.messageHandlers.get(14)?.length || 0)
console.log('  类型15 (ICE):', meetingWsService.wsService.messageHandlers.get(15)?.length || 0)

// 5. 添加临时消息监听器
console.log('\n5. 添加临时WebSocket消息监听器...')
const tempListener = (event) => {
  try {
    const data = JSON.parse(event.data)
    console.log('📨 [临时监听器] 收到消息:', {
      messageType: data.messageType,
      sendUserId: data.sendUserId,
      receiveUserId: data.receiveUserId,
      messageSend2Type: data.messageSend2Type
    })
    
    if (data.messageType === 13) {
      console.log('🔔 [临时监听器] 这是WebRTC Offer消息！')
      console.log('  完整消息:', data)
    }
  } catch (e) {
    console.log('📨 [临时监听器] 收到非JSON消息')
  }
}

meetingWsService.wsService.ws.addEventListener('message', tempListener)
console.log('✅ 临时监听器已添加')

// 6. 测试发送消息
console.log('\n6. 测试发送消息...')
try {
  meetingWsService.wsService.sendMessage({
    messageType: 999,
    test: true,
    timestamp: Date.now()
  })
  console.log('✅ 测试消息已发送')
} catch (e) {
  console.error('❌ 发送测试消息失败:', e)
}

console.log('\n=== 诊断完成 ===')
console.log('现在请让用户B刷新页面重新加入会议')
console.log('观察是否收到WebRTC Offer消息')
console.log('\n要移除临时监听器，运行：')
console.log('meetingWsService.wsService.ws.removeEventListener("message", tempListener)')
