/**
 * 创建假的视频流用于测试
 * 在浏览器控制台中运行此脚本
 */

function createFakeVideoStream() {
  console.log('🎨 创建假视频流...')
  
  // 创建一个canvas
  const canvas = document.createElement('canvas')
  canvas.width = 640
  canvas.height = 480
  const ctx = canvas.getContext('2d')
  
  // 绘制动画
  let hue = 0
  function draw() {
    // 渐变背景
    hue = (hue + 1) % 360
    const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height)
    gradient.addColorStop(0, `hsl(${hue}, 100%, 50%)`)
    gradient.addColorStop(1, `hsl(${(hue + 60) % 360}, 100%, 50%)`)
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    
    // 绘制文字
    ctx.fillStyle = 'white'
    ctx.font = 'bold 48px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('测试视频流', canvas.width / 2, canvas.height / 2 - 40)
    
    // 绘制时间
    ctx.font = '24px Arial'
    ctx.fillText(new Date().toLocaleTimeString(), canvas.width / 2, canvas.height / 2 + 20)
    
    // 绘制用户ID
    ctx.font = '20px Arial'
    const userId = window.meetingWsService?.currentUserId || 'Unknown'
    ctx.fillText(`用户: ${userId}`, canvas.width / 2, canvas.height / 2 + 60)
  }
  
  // 开始动画
  setInterval(draw, 1000 / 30) // 30 FPS
  
  // 从canvas创建视频流
  const stream = canvas.captureStream(30)
  
  console.log('✅ 假视频流已创建')
  console.log('视频轨道数:', stream.getVideoTracks().length)
  
  return stream
}

// 使用假视频流
function useFakeVideo() {
  console.log('=== 使用假视频流 ===\n')
  
  // 创建假视频流
  const fakeStream = createFakeVideoStream()
  
  // 设置到WebRTC管理器
  window.webrtcManager.localStream = fakeStream
  window.webrtcManager.setLocalStream(fakeStream)
  
  // 设置到本地video元素（如果存在）
  const localVideo = document.querySelector('.participant-video-item video')
  if (localVideo) {
    localVideo.srcObject = fakeStream
    localVideo.play()
    console.log('✅ 假视频已设置到本地video元素')
  }
  
  // 更新Meeting.vue的状态
  if (window.meetingVue) {
    window.meetingVue.isVideoOn = true
  }
  
  console.log('✅ 假视频流设置完成')
  console.log('💡 现在可以执行 diagnoseAndFix() 测试WebRTC连接\n')
}

// 导出到全局
window.createFakeVideoStream = createFakeVideoStream
window.useFakeVideo = useFakeVideo

console.log('✅ 假视频流工具已加载')
console.log('使用方法：')
console.log('  useFakeVideo() - 使用假视频流（不需要真实摄像头）')
console.log('')
console.log('💡 建议：')
console.log('  1. 用户A执行: useFakeVideo()')
console.log('  2. 用户B正常开启真实摄像头')
console.log('  3. 两个用户都执行: diagnoseAndFix()')
