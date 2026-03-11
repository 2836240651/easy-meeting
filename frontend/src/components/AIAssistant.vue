<template>
  <div class="ai-assistant">
    <button
      v-if="!expanded"
      class="assistant-trigger"
      type="button"
      @click="openPanel">
      <span>AI</span>
      <small>助手</small>
    </button>

    <div
      v-else
      ref="panelRef"
      class="assistant-panel"
      :style="panelStyle">
      <div
        class="assistant-header"
        @pointerdown="startDrag">
        <div class="assistant-title">
          <div class="assistant-avatar">
            <img v-if="userAvatar" :src="userAvatar" alt="avatar" />
            <span v-else>AI</span>
          </div>
          <div>
            <h3>AI 会议助手</h3>
            <p>基于会议聊天与发言记录，生成摘要、建议与答复。</p>
          </div>
        </div>
        <div class="assistant-actions">
          <button class="header-btn" type="button" title="恢复默认大小" @click.stop="resetPanelLayout">
            还原
          </button>
          <button class="close-btn" type="button" title="关闭" @click.stop="expanded = false">
            ×
          </button>
        </div>
      </div>

      <div v-if="showSpeechControls" class="assistant-toolbar">
        <span class="status-chip" :class="{ muted: !speechSupported || speechBlocked }">
          {{ speechStatusText }}
        </span>
        <button
          class="status-action"
          type="button"
          :disabled="speechActionDisabled"
          @click="toggleSpeechCapture">
          {{ speechEnabled ? '停止采集' : '开始采集' }}
        </button>
      </div>

      <div v-if="showSpeechControls && speechHint" class="speech-hint">
        {{ speechHint }}
      </div>

      <div v-if="showSpeechControls && (speechEnabled || liveSpeechText)" class="speech-preview">
        <div class="speech-label">实时发言</div>
        <div class="speech-text">{{ liveSpeechText || '正在等待发言内容…' }}</div>
      </div>

      <div ref="messagesContainer" class="assistant-messages">
        <div
          v-for="(item, index) in messages"
          :key="index"
          :class="['message-item', item.type, { pending: item.pending }]">
          <div class="message-bubble">{{ item.content }}</div>
        </div>
      </div>

      <div class="quick-actions">
        <button type="button" :disabled="loading" @click="handleSummary">会议摘要</button>
        <button type="button" :disabled="loading" @click="handleSuggest">行动建议</button>
        <button type="button" :disabled="loading" @click="fillHelpPrompt">联合总结</button>
      </div>

      <div class="assistant-input">
        <textarea
          v-model="inputMessage"
          rows="3"
          placeholder="输入问题，或让 AI 直接生成本次会议摘要"
          @keydown.enter.exact.prevent="handleSend" />
        <button type="button" :disabled="loading || !inputMessage.trim()" @click="handleSend">
          {{ loading ? '处理中…' : '发送' }}
        </button>
      </div>

      <button
        class="resize-handle"
        type="button"
        aria-label="调整大小"
        @pointerdown.stop.prevent="startResize" />
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { aiService } from '../api/services'
import { getReadableErrorMessage } from '../api/axios'
import { buildAiResponseText, sanitizeAiText } from '../utils/ai-response'

const props = defineProps({
  meetingId: {
    type: [String, Number],
    required: true
  },
  userAvatar: {
    type: String,
    default: ''
  },
  userName: {
    type: String,
    default: ''
  }
})

const MIN_WIDTH = 360
const MIN_HEIGHT = 520
const DEFAULT_WIDTH = 420
const DEFAULT_HEIGHT = 700
const PANEL_GAP = 24
const showSpeechControls = false

const expanded = ref(false)
const loading = ref(false)
const inputMessage = ref('')
const messagesContainer = ref(null)
const panelRef = ref(null)

const liveSpeechText = ref('')
const speechSupported = ref(false)
const speechEnabled = ref(false)
const speechBlocked = ref(false)
const speechBlockReason = ref('')
const speechHint = ref('')
const speechErrorNotified = ref(false)

const panelWidth = ref(DEFAULT_WIDTH)
const panelHeight = ref(DEFAULT_HEIGHT)
const panelLeft = ref(0)
const panelTop = ref(0)

const messages = ref([
  {
    type: 'ai',
    content: '我会结合会议聊天和发言内容，帮助你整理摘要、行动项和问答结论。'
  }
])

let recognition = null
let restartTimer = null
let dragState = null
let resizeState = null

const isElectron = computed(() => /electron/i.test(window.navigator.userAgent || ''))

const speechStatusText = computed(() => {
  if (speechEnabled.value) return '语音采集中'
  if (speechBlocked.value) return speechBlockReason.value || '当前环境不支持语音识别'
  if (!speechSupported.value) return '当前浏览器不支持语音识别'
  return '语音识别可用'
})

const speechActionDisabled = computed(() => !speechSupported.value || speechBlocked.value)

const panelStyle = computed(() => ({
  width: `${panelWidth.value}px`,
  height: `${panelHeight.value}px`,
  left: `${panelLeft.value}px`,
  top: `${panelTop.value}px`
}))

const getViewportBounds = () => ({
  width: window.innerWidth,
  height: window.innerHeight
})

const clampPanelToViewport = () => {
  const { width, height } = getViewportBounds()
  const maxLeft = Math.max(PANEL_GAP, width - panelWidth.value - PANEL_GAP)
  const maxTop = Math.max(PANEL_GAP, height - panelHeight.value - PANEL_GAP)
  panelLeft.value = Math.min(Math.max(panelLeft.value, PANEL_GAP), maxLeft)
  panelTop.value = Math.min(Math.max(panelTop.value, PANEL_GAP), maxTop)
}

const resetPanelLayout = () => {
  const { width, height } = getViewportBounds()
  panelWidth.value = Math.min(DEFAULT_WIDTH, Math.max(MIN_WIDTH, width - PANEL_GAP * 2))
  panelHeight.value = Math.min(DEFAULT_HEIGHT, Math.max(MIN_HEIGHT, height - PANEL_GAP * 2))
  panelLeft.value = Math.max(PANEL_GAP, width - panelWidth.value - PANEL_GAP)
  panelTop.value = Math.max(PANEL_GAP, height - panelHeight.value - PANEL_GAP)
}

const openPanel = () => {
  expanded.value = true
  nextTick(() => {
    if (!panelLeft.value && !panelTop.value) {
      resetPanelLayout()
    } else {
      clampPanelToViewport()
    }
    scrollToBottom()
  })
}

const stopPointerTracking = () => {
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', stopPointerTracking)
  window.removeEventListener('pointercancel', stopPointerTracking)
  dragState = null
  resizeState = null
}

const handlePointerMove = (event) => {
  if (dragState) {
    panelLeft.value = dragState.startLeft + (event.clientX - dragState.startX)
    panelTop.value = dragState.startTop + (event.clientY - dragState.startY)
    clampPanelToViewport()
    return
  }

  if (resizeState) {
    const { width, height } = getViewportBounds()
    const nextWidth = resizeState.startWidth + (event.clientX - resizeState.startX)
    const nextHeight = resizeState.startHeight + (event.clientY - resizeState.startY)
    panelWidth.value = Math.min(
      Math.max(MIN_WIDTH, Math.round(nextWidth)),
      width - panelLeft.value - PANEL_GAP
    )
    panelHeight.value = Math.min(
      Math.max(MIN_HEIGHT, Math.round(nextHeight)),
      height - panelTop.value - PANEL_GAP
    )
  }
}

const startDrag = (event) => {
  if (event.target.closest('.assistant-actions')) return
  dragState = {
    startX: event.clientX,
    startY: event.clientY,
    startLeft: panelLeft.value,
    startTop: panelTop.value
  }
  window.addEventListener('pointermove', handlePointerMove)
  window.addEventListener('pointerup', stopPointerTracking)
  window.addEventListener('pointercancel', stopPointerTracking)
}

const startResize = (event) => {
  resizeState = {
    startX: event.clientX,
    startY: event.clientY,
    startWidth: panelWidth.value,
    startHeight: panelHeight.value
  }
  window.addEventListener('pointermove', handlePointerMove)
  window.addEventListener('pointerup', stopPointerTracking)
  window.addEventListener('pointercancel', stopPointerTracking)
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const appendMessage = (type, content) => {
  messages.value.push({ type, content: sanitizeAiText(content) })
  scrollToBottom()
}

const createPendingMessage = (content = 'AI 正在整理当前内容，请稍候…') => {
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

const ensureMeetingId = () => {
  if (String(props.meetingId || '').trim()) return true
  ElMessage.warning('会议 ID 不存在')
  return false
}

const handleSend = async () => {
  const message = inputMessage.value.trim()
  if (!message || loading.value || !ensureMeetingId()) return

  appendMessage('user', message)
  const pendingMessage = createPendingMessage('AI 正在生成回复，请稍候…')
  inputMessage.value = ''
  loading.value = true

  try {
    const response = await aiService.chat(String(props.meetingId), message)
    if (response.data.code === 200) {
      updatePendingMessage(pendingMessage, buildAiResponseText(response.data.data))
    } else {
      updatePendingMessage(pendingMessage, response.data.info || '暂时无法处理你的问题，请稍后再试。')
    }
  } catch (error) {
    updatePendingMessage(
      pendingMessage,
      getReadableErrorMessage(error, '暂时无法处理你的问题，请稍后再试。', { aiRequest: true })
    )
  } finally {
    loading.value = false
  }
}

const handleSummary = async () => {
  if (loading.value || !ensureMeetingId()) return
  loading.value = true
  appendMessage('user', '请生成本次会议摘要')
  const pendingMessage = createPendingMessage('AI 正在整理会议摘要，请稍候…')

  try {
    const response = await aiService.summary(String(props.meetingId))
    if (response.data.code === 200) {
      updatePendingMessage(pendingMessage, buildAiResponseText(response.data.data))
    } else {
      updatePendingMessage(pendingMessage, response.data.info || '摘要暂时生成失败，请稍后重试。')
    }
  } catch (error) {
    updatePendingMessage(
      pendingMessage,
      getReadableErrorMessage(error, '摘要暂时生成失败，请稍后重试。', { aiRequest: true })
    )
  } finally {
    loading.value = false
  }
}

const handleSuggest = async () => {
  if (loading.value || !ensureMeetingId()) return
  loading.value = true
  appendMessage('user', '请给出会议行动建议')
  const pendingMessage = createPendingMessage('AI 正在生成行动建议，请稍候…')

  try {
    const response = await aiService.suggest(String(props.meetingId))
    if (response.data.code === 200) {
      updatePendingMessage(pendingMessage, buildAiResponseText(response.data.data))
    } else {
      updatePendingMessage(pendingMessage, response.data.info || '建议暂时生成失败，请稍后重试。')
    }
  } catch (error) {
    updatePendingMessage(
      pendingMessage,
      getReadableErrorMessage(error, '建议暂时生成失败，请稍后重试。', { aiRequest: true })
    )
  } finally {
    loading.value = false
  }
}

const fillHelpPrompt = () => {
  inputMessage.value = '请结合会议聊天和成员发言，输出本次会议摘要、关键决策和后续行动项。'
}

const uploadSpeechSegment = async (text) => {
  const normalized = String(text || '').replace(/\s+/g, ' ').trim()
  if (!normalized || normalized.length < 2 || !ensureMeetingId()) return

  try {
    await aiService.saveSpeechSegment(String(props.meetingId), props.userName || '当前成员', normalized)
  } catch (error) {
    console.error('保存会议发言失败', error)
  }
}

const clearRestartTimer = () => {
  if (restartTimer) {
    clearTimeout(restartTimer)
    restartTimer = null
  }
}

const stopSpeechCapture = () => {
  speechEnabled.value = false
  clearRestartTimer()
  if (recognition) {
    recognition.onend = null
    try {
      recognition.stop()
    } catch (error) {
      console.warn('停止语音识别失败', error)
    }
    recognition.onend = handleRecognitionEnd
  }
}

const blockSpeech = (reason, hint) => {
  speechBlocked.value = true
  speechBlockReason.value = reason
  speechHint.value = hint
  speechEnabled.value = false
  clearRestartTimer()
}

const clearSpeechBlock = () => {
  speechBlocked.value = false
  speechBlockReason.value = ''
  if (!speechEnabled.value) {
    speechHint.value = ''
  }
}

const evaluateSpeechAvailability = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  speechSupported.value = Boolean(SpeechRecognition)

  if (!SpeechRecognition) {
    blockSpeech('当前浏览器不支持语音识别', '请使用文本提问，或切换到支持语音识别的 Chrome 浏览器环境。')
    return null
  }

  if (isElectron.value) {
    blockSpeech(
      '桌面端暂不支持语音识别',
      '当前 Electron 环境无法稳定调用浏览器在线语音服务，已关闭语音采集，避免出现网络错误提示。'
    )
    return null
  }

  if (!navigator.onLine) {
    blockSpeech('当前网络不可用', '检测到网络离线，语音识别依赖在线服务，请恢复网络后重试。')
    return null
  }

  clearSpeechBlock()
  return SpeechRecognition
}

const handleSpeechError = (errorType) => {
  if (errorType === 'not-allowed' || errorType === 'service-not-allowed') {
    stopSpeechCapture()
    blockSpeech('未获得语音权限', '请在浏览器中允许麦克风权限后，再重新开启语音采集。')
    if (!speechErrorNotified.value) {
      ElMessage.warning('未获得麦克风权限，已停止语音采集。')
      speechErrorNotified.value = true
    }
    return
  }

  if (errorType === 'network') {
    stopSpeechCapture()
    blockSpeech(
      '语音服务当前不可用',
      '当前环境无法连接浏览器语音识别服务，已自动停用语音采集。你仍可继续使用文本提问和 AI 摘要。'
    )
    liveSpeechText.value = ''
    if (!speechErrorNotified.value) {
      ElMessage.warning('语音识别服务当前不可用，已自动切换为文本模式。')
      speechErrorNotified.value = true
    }
    return
  }

  if (errorType === 'aborted' || errorType === 'no-speech') {
    return
  }

  stopSpeechCapture()
  if (!speechErrorNotified.value) {
    ElMessage.warning('语音识别暂时不可用，已停止采集。')
    speechErrorNotified.value = true
  }
}

const handleRecognitionEnd = () => {
  if (!speechEnabled.value || speechBlocked.value) return
  clearRestartTimer()
  restartTimer = setTimeout(() => {
    try {
      recognition?.start()
    } catch (error) {
      handleSpeechError('restart-failed')
    }
  }, 800)
}

const initSpeechRecognition = () => {
  const SpeechRecognition = evaluateSpeechAvailability()
  if (!SpeechRecognition) {
    recognition = null
    return
  }

  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = true
  recognition.interimResults = true

  recognition.onresult = async (event) => {
    let interimText = ''

    for (let i = event.resultIndex; i < event.results.length; i += 1) {
      const result = event.results[i]
      const transcript = result[0]?.transcript?.trim()
      if (!transcript) continue

      if (result.isFinal) {
        liveSpeechText.value = transcript
        await uploadSpeechSegment(transcript)
      } else {
        interimText += transcript
      }
    }

    if (interimText) {
      liveSpeechText.value = interimText
    }
  }

  recognition.onerror = (event) => {
    handleSpeechError(event.error)
  }

  recognition.onend = handleRecognitionEnd
}

const startSpeechCapture = () => {
  speechErrorNotified.value = false
  const SpeechRecognition = evaluateSpeechAvailability()
  if (!SpeechRecognition) {
    return
  }

  if (!recognition) {
    initSpeechRecognition()
  }

  if (!recognition || speechEnabled.value) return

  speechHint.value = ''
  liveSpeechText.value = ''
  speechEnabled.value = true

  try {
    recognition.start()
  } catch (error) {
    speechEnabled.value = false
    ElMessage.warning('语音采集暂时无法启动，请稍后再试。')
  }
}

const toggleSpeechCapture = () => {
  if (speechEnabled.value) {
    stopSpeechCapture()
    speechHint.value = '语音采集已停止，你仍可继续通过文字与 AI 交互。'
    return
  }

  startSpeechCapture()
}

watch(expanded, (value) => {
  if (value) {
    nextTick(() => {
      clampPanelToViewport()
      scrollToBottom()
    })
  } else {
    stopSpeechCapture()
  }
})

onMounted(() => {
  resetPanelLayout()
  initSpeechRecognition()
  window.addEventListener('resize', clampPanelToViewport)
  window.addEventListener('online', initSpeechRecognition)
  window.addEventListener('offline', initSpeechRecognition)
})

onBeforeUnmount(() => {
  stopSpeechCapture()
  stopPointerTracking()
  window.removeEventListener('resize', clampPanelToViewport)
  window.removeEventListener('online', initSpeechRecognition)
  window.removeEventListener('offline', initSpeechRecognition)
})
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1200;
}

.assistant-trigger {
  width: 68px;
  height: 68px;
  border: none;
  border-radius: 24px;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.28), transparent 50%),
    linear-gradient(145deg, #0f766e, #0f172a);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.28);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.assistant-trigger:hover {
  transform: translateY(-2px);
  box-shadow: 0 22px 52px rgba(15, 23, 42, 0.34);
}

.assistant-trigger small {
  font-size: 11px;
  opacity: 0.78;
}

.assistant-panel {
  position: fixed;
  border-radius: 28px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(247, 250, 252, 0.95));
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.24);
  backdrop-filter: blur(16px);
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.18);
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto;
}

.assistant-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 20px 14px;
  background:
    radial-gradient(circle at top left, rgba(20, 184, 166, 0.14), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0));
  cursor: move;
  user-select: none;
}

.assistant-title {
  display: flex;
  align-items: center;
  gap: 14px;
}

.assistant-title h3,
.assistant-title p {
  margin: 0;
}

.assistant-title h3 {
  font-size: 18px;
  color: #0f172a;
}

.assistant-title p {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.assistant-avatar {
  width: 46px;
  height: 46px;
  border-radius: 18px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #14b8a6, #2563eb);
  color: #fff;
  font-weight: 700;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
}

.assistant-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.assistant-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-btn,
.close-btn {
  border: none;
  cursor: pointer;
}

.header-btn {
  height: 34px;
  padding: 0 12px;
  border-radius: 12px;
  background: rgba(226, 232, 240, 0.88);
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.close-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(226, 232, 240, 0.8);
  font-size: 22px;
  line-height: 1;
  color: #475569;
}

.assistant-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 20px 14px;
}

.status-chip {
  font-size: 12px;
  color: #0f766e;
  background: rgba(20, 184, 166, 0.1);
  border: 1px solid rgba(20, 184, 166, 0.2);
  border-radius: 999px;
  padding: 7px 12px;
}

.status-chip.muted {
  color: #64748b;
  background: rgba(148, 163, 184, 0.12);
  border-color: rgba(148, 163, 184, 0.22);
}

.status-action {
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #0f766e, #14b8a6);
  color: #fff;
  padding: 9px 12px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(15, 118, 110, 0.2);
}

.status-action:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.speech-hint {
  margin: 0 20px 14px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.16);
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.speech-preview {
  margin: 0 20px 14px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.speech-label {
  font-size: 11px;
  letter-spacing: 0.08em;
  color: #64748b;
  margin-bottom: 6px;
}

.speech-text {
  font-size: 13px;
  color: #0f172a;
  min-height: 20px;
  white-space: pre-wrap;
  line-height: 1.6;
}

.assistant-messages {
  min-height: 0;
  padding: 18px 20px;
  overflow-y: auto;
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.86), rgba(241, 245, 249, 0.96));
}

.message-item {
  display: flex;
  margin-bottom: 12px;
}

.message-item.user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 86%;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 12px 14px;
  border-radius: 18px;
  font-size: 13px;
  line-height: 1.65;
}

.message-item.ai .message-bubble {
  background: #ffffff;
  color: #1e293b;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 10px 24px rgba(148, 163, 184, 0.12);
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #0f766e, #2563eb);
  color: #ffffff;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.18);
}

.message-item.pending .message-bubble {
  opacity: 0.72;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  padding: 0 20px 14px;
}

.quick-actions button {
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.88);
  color: #0f172a;
  border-radius: 14px;
  padding: 10px 12px;
  cursor: pointer;
  font-weight: 600;
}

.quick-actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.assistant-input {
  padding: 0 20px 20px;
}

.assistant-input textarea {
  width: 100%;
  resize: none;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.88);
  border-radius: 18px;
  padding: 12px 14px;
  font-size: 13px;
  outline: none;
  box-sizing: border-box;
  line-height: 1.6;
}

.assistant-input textarea:focus {
  border-color: rgba(37, 99, 235, 0.45);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.08);
}

.assistant-input button {
  margin-top: 12px;
  width: 100%;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #0f172a, #0f766e);
  color: #fff;
  padding: 12px 14px;
  cursor: pointer;
  font-weight: 700;
}

.assistant-input button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.resize-handle {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 18px;
  height: 18px;
  border: none;
  background:
    linear-gradient(135deg, transparent 0 35%, rgba(100, 116, 139, 0.4) 35% 48%, transparent 48% 60%, rgba(100, 116, 139, 0.7) 60% 72%, transparent 72%);
  cursor: nwse-resize;
  opacity: 0.8;
}

@media (max-width: 768px) {
  .ai-assistant {
    right: 14px;
    bottom: 14px;
  }

  .assistant-panel {
    max-width: calc(100vw - 12px);
    max-height: calc(100vh - 12px);
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }
}
</style>
