// 在浏览器控制台运行此脚本来诊断WebSocket连接问题

console.log('=== WebSocket连接诊断 ===')

// 1. 检查WebSocket服务状态
console.log('\n1️⃣ WebSocket服务状态:')
console.log('  wsService存在:', typeof wsService !== 'undefined')
if (typeof wsService !== 'undefined') {
  console.log('  ws对象:', wsService.ws)
  console.log('  连接状态:', wsService.ws?.readyState)
  console.log('  状态说明:', 
    wsService.ws?.readyState === 0 ? 'CONNECTING (连接中)' :
    wsService.ws?.readyState === 1 ? 'OPEN (已连接)' :
    wsService.ws?.readyState === 2 ? 'CLOSING (关闭中)' :
    wsService.ws?.readyState === 3 ? 'CLOSED (已关闭)' : '未知')
  console.log('  isConnected:', wsService.isConnected?.value)
  console.log('  shouldReconnect:', wsService.shouldReconnect)
  console.log('  isReconnecting:', wsService.isReconnecting)
  console.log('  reconnectAttempts:', wsService.reconnectAttempts)
  console.log('  currentToken:', wsService.currentToken ? '存在' : '不存在')
  console.log('  currentUserId:', wsService.currentUserId)
}

// 2. 检查会议WebSocket服务
console.log('\n2️⃣ 会议WebSocket服务状态:')
console.log('  meetingWsService存在:', typeof meetingWsService !== 'undefined')
if (typeof meetingWsService !== 'undefined') {
  console.log('  isConnected:', meetingWsService.isConnected)
  console.log('  currentUserId:', meetingWsService.currentUserId)
  console.log('  currentMeetingId:', meetingWsService.currentMeetingId)
}

// 3. 检查token
console.log('\n3️⃣ Token状态:')
const token = localStorage.getItem('token')
console.log('  Token存在:', !!token)
console.log('  Token长度:', token?.length)

// 4. 尝试重新连接
console.log('\n4️⃣ 尝试重新连接...')
if (typeof wsService !== 'undefined' && token && wsService.currentUserId) {
  console.log('  调用 wsService.connect()')
  wsService.connect(token, wsService.currentUserId)
    .then(() => {
      console.log('  ✅ 重新连接成功!')
      console.log('  新的连接状态:', wsService.ws?.readyState)
    })
    .catch(err => {
      console.error('  ❌ 重新连接失败:', err)
    })
} else {
  console.log('  ⚠️ 无法重新连接，缺少必要信息')
}

console.log('\n=== 诊断完成 ===')
console.log('\n💡 如果WebSocket状态是CLOSED(3)，请运行以下命令重新连接:')
console.log('wsService.connect(localStorage.getItem("token"), wsService.currentUserId || "你的用户ID")')
