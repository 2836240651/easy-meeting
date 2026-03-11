<template>
  <div 
    class="video-window"
    :class="{ collapsed: !expanded }"
    :style="{ cursor: isDragging ? 'grabbing' : 'default' }">
    
    <!-- 窗口头部（可拖动） -->
    <div 
      class="window-header"
      @mousedown="startDrag"
      :style="{ cursor: isDragging ? 'grabbing' : 'grab' }">
      <span class="header-title">会议成员 ({{ participants.length + 1 }})</span>
      <button class="header-btn" @click="toggleExpand">
        {{ expanded ? '−' : '+' }}
      </button>
    </div>
    
    <!-- 窗口内容 -->
    <transition name="expand">
      <div v-if="expanded" class="window-content">
        <!-- 自己的视频 -->
        <div class="video-item my-video">
          <video 
            v-if="localVideoOn" 
            ref="localVideoRef"
            autoplay 
            muted 
            playsinline
            class="video-element">
          </video>
          <img 
            v-else
            :src="localAvatar" 
            alt="我" 
            class="avatar-element">
          <div class="video-overlay">
            <span class="video-name">我</span>
            <button 
              class="audio-btn" 
              :class="{ muted: localMuted }"
              @click="handleToggleMute">
              {{ localMuted ? '🔇' : '🎤' }}
            </button>
          </div>
        </div>
        
        <!-- 其他成员视频 -->
        <div 
          v-for="participant in participants" 
          :key="participant.userId"
          class="video-item">
          <video 
            v-if="participant.videoOpen"
            :ref="el => setVideoRef(participant.userId, el)"
            autoplay 
            playsinline
            class="video-element">
          </video>
          <img 
            v-else
            :src="participant.avatar" 
            :alt="participant.name" 
            class="avatar-element">
          <div class="video-overlay">
            <span class="video-name">{{ participant.name }}</span>
            <span class="audio-status" :class="{ muted: participant.isMuted }">
              {{ participant.isMuted ? '🔇' : '🎤' }}
            </span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

// 状态
const expanded = ref(true)
const isDragging = ref(false)
const participants = ref([])
const localVideoOn = ref(true)
const localMuted = ref(false)
const localAvatar = ref('/meeting-icons/男头像.svg')
const videoRefs = ref(new Map())

// 设置视频引用
const setVideoRef = (userId, el) => {
  if (el) {
    videoRefs.value.set(userId, el)
  }
}

// 切换展开/折叠
const toggleExpand = () => {
  expanded.value = !expanded.value
}

// 拖动功能
let initialWindowPos = { x: 0, y: 0 }

const startDrag = async (event) => {
  event.preventDefault()
  isDragging.value = true
  
  const startX = event.screenX
  const startY = event.screenY
  
  // 获取当前窗口位置（通过 Electron API）
  // 由于我们无法直接获取窗口位置，我们使用相对移动
  let lastX = startX
  let lastY = startY
  
  const onMouseMove = async (e) => {
    if (!isDragging.value) return
    
    const deltaX = e.screenX - lastX
    const deltaY = e.screenY - lastY
    
    lastX = e.screenX
    lastY = e.screenY
    
    // 计算新位置
    initialWindowPos.x += deltaX
    initialWindowPos.y += deltaY
    
    // 通过 Electron API 移动窗口
    if (window.electron && window.electron.moveWindow) {
      try {
        await window.electron.moveWindow('video', initialWindowPos.x, initialWindowPos.y)
      } catch (error) {
        console.error('移动窗口失败:', error)
      }
    }
  }
  
  const onMouseUp = () => {
    isDragging.value = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }
  
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

// 切换静音
const handleToggleMute = () => {
  localMuted.value = !localMuted.value
  if (window.electron) {
    window.electron.sendOverlayAction({ 
      type: 'toggle-mute', 
      muted: localMuted.value 
    })
  }
}

// 监听参与者更新
onMounted(() => {
  console.log('🎯 视频窗口已挂载')
  
  if (window.electron && window.electron.onParticipantsUpdated) {
    window.electron.onParticipantsUpdated((data) => {
      console.log('📥 收到参与者更新:', data)
      participants.value = data.participants || []
      
      // 更新本地状态
      if (data.localVideoOn !== undefined) localVideoOn.value = data.localVideoOn
      if (data.localMuted !== undefined) localMuted.value = data.localMuted
      if (data.localAvatar) localAvatar.value = data.localAvatar
      
      // 更新视频流
      if (data.videoStreams) {
        data.videoStreams.forEach(({ userId, stream }) => {
          const videoEl = videoRefs.value.get(userId)
          if (videoEl && stream) {
            videoEl.srcObject = stream
          }
        })
      }
    })
  }
})

onUnmounted(() => {
  console.log('🎯 视频窗口已卸载')
})
</script>

<style scoped>
.video-window {
  width: 300px;
  max-height: 600px;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
  transition: all 0.3s ease;
}

.video-window.collapsed {
  max-height: 48px;
}

.window-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  user-select: none;
  -webkit-app-region: drag;
}

.header-title {
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
}

.header-btn {
  width: 28px;
  height: 28px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  -webkit-app-region: no-drag;
}

.header-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.window-content {
  padding: 12px;
  max-height: 540px;
  overflow-y: auto;
}

.window-content::-webkit-scrollbar {
  width: 6px;
}

.window-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.window-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.window-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.video-item {
  position: relative;
  width: 100%;
  aspect-ratio: 16/9;
  background: #1a1a1a;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 10px;
}

.video-item:last-child {
  margin-bottom: 0;
}

.video-item.my-video {
  border: 2px solid #4caf50;
}

.video-element,
.avatar-element {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.video-name {
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.audio-btn {
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  color: #ffffff;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.audio-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.audio-btn.muted {
  background: rgba(244, 67, 54, 0.3);
  color: #f44336;
}

.audio-status {
  color: #4caf50;
  font-size: 16px;
}

.audio-status.muted {
  color: #f44336;
}

/* 展开/折叠动画 */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}
</style>
