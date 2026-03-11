<template>
  <div class="ai-test-container">
    <div class="ai-test-card">
      <h2>AI 助手测试</h2>

      <div v-if="!isLoggedIn" class="login-section">
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="loginForm.email" type="email" placeholder="请输入邮箱" />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="loginForm.password" type="password" placeholder="请输入密码" />
        </div>
        <div class="form-group">
          <label>验证码</label>
          <div class="captcha-container">
            <input v-model="loginForm.checkCode" type="text" placeholder="请输入验证码" />
            <img :src="captchaImage" alt="验证码" class="captcha-image" @click="getCaptcha" />
          </div>
        </div>
        <button class="btn btn-primary w-full" @click="handleLogin">登录</button>
        <p v-if="loginError" class="error-message">{{ loginError }}</p>
      </div>

      <div v-else class="ai-chat-section">
        <div class="user-info">
          <span>已登录：{{ userInfo.email || userInfo.nickName || userInfo.userId }}</span>
          <button class="btn btn-secondary btn-sm" @click="handleLogout">退出登录</button>
        </div>

        <div class="form-group">
          <label>会议 ID</label>
          <input v-model="meetingId" type="text" placeholder="请输入会议 ID" />
        </div>

        <div class="chat-box">
          <div ref="messagesContainer" class="messages">
            <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.type, { pending: msg.pending }]">
              <div class="message-content">{{ msg.content }}</div>
            </div>
          </div>

          <div class="quick-commands">
            <span>快捷命令：</span>
            <button @click="sendCommand('/summary')">/summary</button>
            <button @click="sendCommand('/suggest')">/suggest</button>
            <button @click="sendCommand('/help')">/help</button>
            <button @click="generateSummary">API 摘要</button>
            <button @click="generateSuggestions">API 建议</button>
            <button @click="testAIConnection">连接测试</button>
          </div>

          <div class="input-area">
            <input
              v-model="userMessage"
              type="text"
              placeholder="输入消息或命令（/summary /suggest /help）"
              @keyup.enter="sendMessage"
            />
            <button class="btn btn-primary" :disabled="sending" @click="sendMessage">
              {{ sending ? '发送中...' : '发送' }}
            </button>
          </div>
        </div>

        <p v-if="errorMsg" class="error-message">{{ errorMsg }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { authService, aiService } from '../api/services'
import { getReadableErrorMessage } from '../api/axios'
import { buildAiResponseText, sanitizeAiText } from '../utils/ai-response'

const isLoggedIn = ref(false)
const userInfo = ref({})
const loginForm = ref({
  email: '',
  password: '',
  checkCode: '',
  checkCodeKey: ''
})

const captchaImage = ref('')
const loginError = ref('')
const meetingId = ref('')
const userMessage = ref('')
const messages = ref([])
const sending = ref(false)
const errorMsg = ref('')
const messagesContainer = ref(null)

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const appendMessage = (type, content) => {
  messages.value.push({ type, content: sanitizeAiText(content) })
  scrollToBottom()
}

const createPendingMessage = (content = 'AI 正在处理中，请稍候...') => {
  const message = { type: 'ai', content, pending: true }
  messages.value.push(message)
  scrollToBottom()
  return message
}

const updatePendingMessage = (message, content) => {
  if (!message) {
    appendMessage('ai', content)
    return
  }

  message.content = sanitizeAiText(content)
  message.pending = false
  messages.value = [...messages.value]
  scrollToBottom()
}

const getCaptcha = async () => {
  try {
    const response = await authService.getCaptcha()
    if (response.data.code === 200) {
      loginForm.value.checkCodeKey = response.data.data.checkCodeKey
      captchaImage.value = `data:image/png;base64,${response.data.data.checkCode}`
    }
  } catch (error) {
    console.error('获取验证码失败', error)
  }
}

const handleLogin = async () => {
  loginError.value = ''
  try {
    const response = await authService.login({
      checkCodeKey: loginForm.value.checkCodeKey,
      email: loginForm.value.email,
      password: loginForm.value.password,
      checkCode: loginForm.value.checkCode
    })

    if (response.data.code === 200) {
      userInfo.value = response.data.data
      localStorage.setItem('token', userInfo.value.token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
      isLoggedIn.value = true
      appendMessage('ai', '登录成功，可以开始测试 AI 功能。')
      return
    }

    loginError.value = response.data.info || '登录失败'
    await getCaptcha()
  } catch (error) {
    loginError.value = error.response?.data?.info || '登录失败'
    await getCaptcha()
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  isLoggedIn.value = false
  userInfo.value = {}
  messages.value = []
}

const requireMeetingId = () => {
  if (meetingId.value.trim()) return true
  errorMsg.value = '请先输入会议 ID'
  return false
}

const sendMessage = async () => {
  if (!userMessage.value.trim() || !requireMeetingId() || sending.value) return

  const msg = userMessage.value.trim()
  appendMessage('user', msg)
  const pendingMessage = createPendingMessage('AI 正在生成回复，请稍候...')
  sending.value = true
  errorMsg.value = ''

  try {
    const response = await aiService.chat(meetingId.value.trim(), msg)
    if (response.data.code === 200) {
      const aiResponse = response.data.data
      updatePendingMessage(pendingMessage, buildAiResponseText(aiResponse))
    } else {
      updatePendingMessage(pendingMessage, response.data.info || '暂时无法处理请求，请稍后再试。')
    }
  } catch (error) {
    updatePendingMessage(
      pendingMessage,
      getReadableErrorMessage(error, '暂时无法处理请求，请稍后再试。', { aiRequest: true })
    )
  } finally {
    userMessage.value = ''
    sending.value = false
  }
}

const sendCommand = (cmd) => {
  userMessage.value = cmd
  sendMessage()
}

const generateSummary = async () => {
  if (!requireMeetingId()) return
  const pendingMessage = createPendingMessage('AI 正在整理会议摘要，请稍候...')
  try {
    const response = await aiService.summary(meetingId.value.trim())
    if (response.data.code === 200) {
      const data = response.data.data
      updatePendingMessage(pendingMessage, buildAiResponseText(data))
      return
    }
    updatePendingMessage(pendingMessage, response.data.info || '摘要暂时生成失败，请稍后重试。')
  } catch (error) {
    updatePendingMessage(
      pendingMessage,
      getReadableErrorMessage(error, '摘要暂时生成失败，请稍后重试。', { aiRequest: true })
    )
  }
}

const generateSuggestions = async () => {
  if (!requireMeetingId()) return
  const pendingMessage = createPendingMessage('AI 正在生成行动建议，请稍候...')
  try {
    const response = await aiService.suggest(meetingId.value.trim())
    if (response.data.code === 200) {
      const list = response.data.data?.suggestions || []
      updatePendingMessage(
        pendingMessage,
        list.length ? sanitizeAiText(list.join('\n')) : '暂无建议'
      )
      return
    }
    updatePendingMessage(pendingMessage, response.data.info || '建议暂时生成失败，请稍后重试。')
  } catch (error) {
    updatePendingMessage(
      pendingMessage,
      getReadableErrorMessage(error, '建议暂时生成失败，请稍后重试。', { aiRequest: true })
    )
  }
}

const testAIConnection = async () => {
  try {
    const response = await aiService.test()
    if (response.data.code === 200) {
      appendMessage('ai', `连接测试成功：${response.data.data?.response || 'OK'}`)
      return
    }
    appendMessage('ai', `连接测试失败：${response.data.info || '未知错误'}`)
  } catch (error) {
    appendMessage(
      'ai',
      getReadableErrorMessage(error, 'AI 服务暂时不可用，请稍后重试。', { aiRequest: true })
    )
  }
}

onMounted(() => {
  const token = localStorage.getItem('token')
  const storedUserInfo = localStorage.getItem('userInfo')

  if (token && storedUserInfo) {
    isLoggedIn.value = true
    userInfo.value = JSON.parse(storedUserInfo)
  } else {
    getCaptcha()
  }
})
</script>

<style scoped>
.ai-test-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.ai-test-card {
  background: white;
  border-radius: 12px;
  padding: 30px;
  width: 100%;
  max-width: 700px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.ai-test-card h2 {
  text-align: center;
  margin-bottom: 24px;
  color: #333;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #555;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.captcha-container {
  display: flex;
  gap: 10px;
}

.captcha-container input {
  flex: 1;
}

.captcha-image {
  height: 40px;
  width: 100px;
  object-fit: cover;
  cursor: pointer;
  border-radius: 4px;
}

.w-full {
  width: 100%;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover {
  background: #5568d3;
}

.btn-primary:disabled {
  background: #aaa;
  cursor: not-allowed;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.error-message {
  color: #e74c3c;
  font-size: 14px;
  margin-top: 10px;
  text-align: center;
}

.user-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
}

.chat-box {
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: hidden;
}

.messages {
  height: 320px;
  overflow-y: auto;
  padding: 15px;
  background: #f9f9f9;
}

.message {
  margin-bottom: 12px;
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message.ai {
  justify-content: flex-start;
}

.message-content {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.message.user .message-content {
  background: #667eea;
  color: white;
}

.message.ai .message-content {
  background: white;
  border: 1px solid #ddd;
  color: #333;
}

.message.pending .message-content {
  opacity: 0.72;
}

.quick-commands {
  padding: 10px;
  background: #f0f0f0;
  border-top: 1px solid #ddd;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.quick-commands span {
  font-size: 12px;
  color: #666;
}

.quick-commands button {
  padding: 4px 10px;
  font-size: 12px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.quick-commands button:hover {
  background: #5568d3;
}

.input-area {
  display: flex;
  gap: 10px;
  padding: 10px;
  background: white;
  border-top: 1px solid #ddd;
}

.input-area input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
}

.input-area input:focus {
  outline: none;
  border-color: #667eea;
}
</style>
