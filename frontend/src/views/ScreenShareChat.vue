<template>
  <div 
    class="chat-window"
    :class="{ collapsed: !expanded }"
    :style="{ cursor: isDragging ? 'grabbing' : 'default' }">
    
    <!-- 窗口头部（可拖动） -->
    <div 
      class="window-header"
      @mousedown="startDrag"
      :style="{ cursor: isDragging ? 'grabbing' : 'grab' }">
      <span class="header-title">聊天 ({{ messages.length }})</span>
      <button class="header-btn" @click="toggleExpand">
        {{ expanded ? '−' : '+' }}
      </button>
    </div>
    
    <!-- 窗口内容 -->
    <transition name="expand">
      <div v-if="expanded" class="window-content">
        <!-- 消息列表 -->
        <div class="messages-container" ref="messagesContainer">
          <div 
            v-for="message in messages" 
            :key="message.id"
            class="message-item"
            :class="{ 'my-message': message.isMe }">
            <img :src="message.avatar" :alt="message.sender" class="message-avatar">
            <div class="message-content">
              <div class="message-header">
                <span class="message-sender">{{ message.sender }}</span>
                <span class="message-time">{{ message.time }}</span>
              </div>
              <div class="message-text">{{ message.text }}</div>
            </div>
          </div>
          
          <div v-if="messages.length === 0" class="empty-state">
            <span class="empty-icon">💬</span>
            <span class="empty-text">暂无消息</span>
          </div>
        </div>
        
        <!-- 输入框 -->
        <div class="input-container">
          <input 
            v-model="inputText"
            type="text" 
            placeholder="输入消息..."
            class="message-input"
            @keyup.enter="sendMessage">
          <button 
            class="send-btn"
            :disabled="!inputText.trim()"
            @click="sendMessage">
            发送
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'

// 状态
const expanded = ref(true)
const isDragging = ref(false)
const messages = ref([])
const inputText = ref('')
const messagesContainer = ref(null)

// 切换展开/折叠
const toggleExpand = () => {
  expanded.value = !expanded.value
}

// 拖动功能
let initialWindowPos = { x: 20, y: 0 }  // 初始位置：左下角

const startDrag = async (event) => {
  event.preventDefault()
  isDragging.value = true
  
  const startX = event.screenX
  const startY = event.screenY
  
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
        await window.electron.moveWindow('chat', initialWindowPos.x, initialWindowPos.y)
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

// 发送消息
const sendMessage = () => {
  if (!inputText.value.trim()) return
  
  const message = {
    text: inputText.value.trim(),
    timestamp: Date.now()
  }
  
  // 发送到主窗口
  if (window.electron) {
    window.electron.sendOverlayAction({ 
      type: 'send-message', 
      message 
    })
  }
  
  inputText.value = ''
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 监听消息更新
onMounted(() => {
  console.log('🎯 聊天窗口已挂载')
  
  if (window.electron && window.electron.onChatMessagesUpdated) {
    window.electron.onChatMessagesUpdated((data) => {
      console.log('📥 收到聊天消息更新:', data)
      messages.value = data || []
      scrollToBottom()
    })
  }
})

onUnmounted(() => {
  console.log('🎯 聊天窗口已卸载')
})
</script>

<style scoped>
.chat-window {
  width: 350px;
  height: 500px;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
}

.chat-window.collapsed {
  height: 48px;
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
  flex-shrink: 0;
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
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.messages-container::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.message-item.my-message {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.message-sender {
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
}

.message-time {
  color: #999999;
  font-size: 11px;
}

.message-text {
  color: #dfdfdf;
  font-size: 14px;
  line-height: 1.4;
  background: rgba(255, 255, 255, 0.1);
  padding: 8px 12px;
  border-radius: 8px;
  word-wrap: break-word;
}

.my-message .message-text {
  background: rgba(76, 175, 80, 0.2);
  border: 1px solid rgba(76, 175, 80, 0.3);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.3;
}

.empty-text {
  color: #999999;
  font-size: 14px;
}

.input-container {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.message-input {
  flex: 1;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  color: #ffffff;
  font-size: 14px;
  outline: none;
}

.message-input::placeholder {
  color: #999999;
}

.message-input:focus {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(76, 175, 80, 0.5);
}

.send-btn {
  padding: 8px 16px;
  background: rgba(76, 175, 80, 0.3);
  border: 1px solid rgba(76, 175, 80, 0.5);
  border-radius: 6px;
  color: #4caf50;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  background: rgba(76, 175, 80, 0.4);
  border-color: rgba(76, 175, 80, 0.7);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 展开/折叠动画 */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
}
</style>
