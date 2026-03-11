<template>
  <div 
    class="topbar-container" 
    :class="{ expanded }"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave">
    
    <!-- 指示器（始终显示） -->
    <div class="topbar-indicator">
      <span class="recording-dot"></span>
      <span class="indicator-text">您正在共享屏幕</span>
      <span class="meeting-time">{{ formattedDuration }}</span>
    </div>
    
    <!-- 控制按钮（悬停时显示） -->
    <transition name="slide-down">
      <div v-if="expanded" class="topbar-controls">
        <button 
          class="control-btn" 
          :class="{ active: isMuted }" 
          @click="handleToggleMute"
          :title="isMuted ? '解除静音' : '静音'">
          <span class="btn-icon">{{ isMuted ? '🔇' : '🎤' }}</span>
        </button>
        
        <button 
          class="control-btn" 
          :class="{ active: !isVideoOn }" 
          @click="handleToggleVideo"
          :title="isVideoOn ? '关闭视频' : '开启视频'">
          <span class="btn-icon">{{ isVideoOn ? '📹' : '🚫' }}</span>
        </button>
        
        <button 
          class="control-btn" 
          @click="handleShowParticipants"
          title="成员">
          <span class="btn-icon">👥</span>
        </button>
        
        <button 
          class="control-btn" 
          @click="handleShowChat"
          title="聊天">
          <span class="btn-icon">💬</span>
        </button>
        
        <button 
          class="control-btn" 
          :class="{ active: isPaused }" 
          @click="handleTogglePause"
          :title="isPaused ? '恢复共享' : '暂停共享'">
          <span class="btn-icon">{{ isPaused ? '▶️' : '⏸️' }}</span>
        </button>
        
        <button 
          class="control-btn stop-btn" 
          @click="handleStopShare"
          title="结束共享">
          <span class="btn-icon">🛑</span>
          <span class="btn-text">结束共享</span>
        </button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

// 状态
const expanded = ref(false)
const isMuted = ref(false)
const isVideoOn = ref(true)
const isPaused = ref(false)
const meetingDuration = ref(0)
const durationTimer = ref(null)

// 格式化会议时长
const formattedDuration = computed(() => {
  const duration = meetingDuration.value
  const hours = Math.floor(duration / 3600)
  const minutes = Math.floor((duration % 3600) / 60)
  const seconds = duration % 60
  
  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  } else {
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }
})

// 鼠标进入
const handleMouseEnter = () => {
  expanded.value = true
}

// 鼠标离开
const handleMouseLeave = () => {
  expanded.value = false
}

// 发送操作到主窗口
const sendAction = (action, data = {}) => {
  if (window.electron) {
    window.electron.sendOverlayAction({ type: action, ...data })
  }
}

// 控制按钮处理
const handleToggleMute = () => {
  isMuted.value = !isMuted.value
  sendAction('toggle-mute', { muted: isMuted.value })
}

const handleToggleVideo = () => {
  isVideoOn.value = !isVideoOn.value
  sendAction('toggle-video', { videoOn: isVideoOn.value })
}

const handleShowParticipants = () => {
  sendAction('show-participants')
}

const handleShowChat = () => {
  sendAction('show-chat')
}

const handleTogglePause = () => {
  isPaused.value = !isPaused.value
  sendAction('toggle-pause', { paused: isPaused.value })
}

const handleStopShare = () => {
  sendAction('stop-share')
}

// 启动计时器
const startTimer = () => {
  durationTimer.value = setInterval(() => {
    meetingDuration.value++
  }, 1000)
}

// 监听主窗口的状态更新
onMounted(() => {
  console.log('🎯 顶部指示条已挂载')
  startTimer()
  
  // 监听主窗口的操作
  if (window.electron && window.electron.onOverlayAction) {
    window.electron.onOverlayAction((action) => {
      console.log('📥 收到主窗口操作:', action)
      
      // 更新本地状态
      if (action.type === 'update-state') {
        if (action.isMuted !== undefined) isMuted.value = action.isMuted
        if (action.isVideoOn !== undefined) isVideoOn.value = action.isVideoOn
        if (action.isPaused !== undefined) isPaused.value = action.isPaused
      }
    })
  }
})

onUnmounted(() => {
  if (durationTimer.value) {
    clearInterval(durationTimer.value)
  }
})
</script>

<style scoped>
.topbar-container {
  width: 100vw;
  height: 40px;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(15px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: height 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  -webkit-app-region: no-drag;
}

.topbar-container.expanded {
  height: 80px;
}

.topbar-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
}

.recording-dot {
  width: 10px;
  height: 10px;
  background: #f44336;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.1);
  }
}

.indicator-text {
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
}

.meeting-time {
  color: #4caf50;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  font-weight: 600;
  padding: 4px 12px;
  background: rgba(76, 175, 80, 0.2);
  border-radius: 6px;
  border: 1px solid rgba(76, 175, 80, 0.4);
}

.topbar-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
}

.control-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.control-btn.active {
  background: rgba(244, 67, 54, 0.3);
  border-color: rgba(244, 67, 54, 0.6);
  color: #f44336;
}

.btn-icon {
  font-size: 18px;
}

.btn-text {
  font-size: 13px;
  font-weight: 500;
}

.stop-btn {
  background: rgba(244, 67, 54, 0.2);
  border-color: rgba(244, 67, 54, 0.5);
}

.stop-btn:hover {
  background: rgba(244, 67, 54, 0.4);
  border-color: rgba(244, 67, 54, 0.7);
}

/* 过渡动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
