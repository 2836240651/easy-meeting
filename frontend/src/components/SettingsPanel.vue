<template>
  <div class="settings-panel">
    <div class="settings-section">
      <h3 class="section-title">会议设置</h3>
      <div class="setting-item">
        <div class="setting-info">
          <label>默认开启摄像头</label>
          <span class="setting-desc">加入会议时自动开启摄像头</span>
        </div>
        <label class="switch">
          <input v-model="settings.defaultVideoOn" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>默认开启麦克风</label>
          <span class="setting-desc">加入会议时自动开启麦克风</span>
        </div>
        <label class="switch">
          <input v-model="settings.defaultAudioOn" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>会议提醒时间</label>
          <span class="setting-desc">预约会议开始前的提醒时机</span>
        </div>
        <select v-model="settings.reminderTime" class="setting-select" @change="saveSettings">
          <option :value="5">提前 5 分钟</option>
          <option :value="10">提前 10 分钟</option>
          <option :value="15">提前 15 分钟</option>
          <option :value="30">提前 30 分钟</option>
        </select>
      </div>
    </div>

    <div class="settings-section">
      <h3 class="section-title">通知设置</h3>
      <div class="setting-item">
        <div class="setting-info">
          <label>桌面通知</label>
          <span class="setting-desc">显示系统桌面通知</span>
        </div>
        <label class="switch">
          <input v-model="settings.desktopNotification" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>声音提醒</label>
          <span class="setting-desc">收到消息时播放提示音</span>
        </div>
        <label class="switch">
          <input v-model="settings.soundNotification" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>会议邀请通知</label>
          <span class="setting-desc">收到会议邀请时通知</span>
        </div>
        <label class="switch">
          <input v-model="settings.meetingInviteNotification" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>好友申请通知</label>
          <span class="setting-desc">收到好友申请时通知</span>
        </div>
        <label class="switch">
          <input v-model="settings.friendRequestNotification" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>
    </div>

    <div class="settings-section">
      <h3 class="section-title">隐私设置</h3>
      <div class="setting-item">
        <div class="setting-info">
          <label>显示在线状态</label>
          <span class="setting-desc">允许好友看到你的在线状态</span>
        </div>
        <label class="switch">
          <input v-model="settings.showOnlineStatus" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>允许陌生人添加</label>
          <span class="setting-desc">允许非好友用户添加你为联系人</span>
        </div>
        <label class="switch">
          <input v-model="settings.allowStrangerAdd" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>
    </div>

    <div class="settings-section">
      <h3 class="section-title">外观设置</h3>
      <div class="setting-item">
        <div class="setting-info">
          <label>深色模式</label>
          <span class="setting-desc">切换深色与浅色主题</span>
        </div>
        <label class="switch">
          <input v-model="settings.darkMode" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>语言</label>
          <span class="setting-desc">选择界面语言</span>
        </div>
        <select v-model="settings.language" class="setting-select" @change="saveSettings">
          <option value="zh-CN">简体中文</option>
          <option value="en-US">English</option>
        </select>
      </div>
    </div>

    <div class="settings-section">
      <h3 class="section-title">视频设置</h3>
      <div class="setting-item">
        <div class="setting-info">
          <label>视频质量</label>
          <span class="setting-desc">选择视频清晰度</span>
        </div>
        <select v-model="settings.videoQuality" class="setting-select" @change="saveSettings">
          <option value="low">流畅 (360p)</option>
          <option value="medium">标清 (480p)</option>
          <option value="high">高清 (720p)</option>
          <option value="ultra">超清 (1080p)</option>
        </select>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>镜像我的视频</label>
          <span class="setting-desc">水平翻转自己的视频画面</span>
        </div>
        <label class="switch">
          <input v-model="settings.mirrorVideo" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>虚拟背景</label>
          <span class="setting-desc">启用虚拟背景功能</span>
        </div>
        <label class="switch">
          <input v-model="settings.virtualBackground" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>
    </div>

    <div class="settings-section">
      <h3 class="section-title">音频设置</h3>
      <div class="setting-item">
        <div class="setting-info">
          <label>回声消除</label>
          <span class="setting-desc">自动消除回声和噪音</span>
        </div>
        <label class="switch">
          <input v-model="settings.echoCancellation" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>噪音抑制</label>
          <span class="setting-desc">降低环境噪音</span>
        </div>
        <label class="switch">
          <input v-model="settings.noiseSuppression" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>自动增益</label>
          <span class="setting-desc">自动调节麦克风音量</span>
        </div>
        <label class="switch">
          <input v-model="settings.autoGainControl" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>
    </div>

    <div class="settings-section">
      <h3 class="section-title">屏幕共享设置</h3>
      <div class="setting-item">
        <div class="setting-info">
          <label>共享系统音频</label>
          <span class="setting-desc">共享屏幕时包含系统声音</span>
        </div>
        <label class="switch">
          <input v-model="settings.shareSystemAudio" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>优化视频共享</label>
          <span class="setting-desc">共享视频内容时优化流畅度</span>
        </div>
        <label class="switch">
          <input v-model="settings.optimizeVideoSharing" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>
    </div>

    <div class="settings-section">
      <h3 class="section-title">网络设置</h3>
      <div class="setting-item">
        <div class="setting-info">
          <label>自动重连</label>
          <span class="setting-desc">网络断开时自动重新连接</span>
        </div>
        <label class="switch">
          <input v-model="settings.autoReconnect" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>显示网络状态</label>
          <span class="setting-desc">在会议中显示网络质量指示器</span>
        </div>
        <label class="switch">
          <input v-model="settings.showNetworkStatus" type="checkbox" @change="saveSettings" />
          <span class="slider"></span>
        </label>
      </div>
    </div>

    <div class="settings-section">
      <h3 class="section-title">账号设置</h3>
      <div class="setting-item">
        <div class="setting-info">
          <label>修改密码</label>
          <span class="setting-desc">更新当前账号登录密码</span>
        </div>
        <button class="setting-button" @click="$emit('change-password')">修改</button>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>重置设置</label>
          <span class="setting-desc">恢复为系统默认设置</span>
        </div>
        <button class="setting-button ghost" @click="resetSettings">重置</button>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <label>退出登录</label>
          <span class="setting-desc">退出当前账号</span>
        </div>
        <button class="setting-button danger" @click="$emit('logout')">退出</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { settingsManager } from '@/utils/settings-manager.js'

defineEmits(['change-password', 'logout'])

const settings = ref(settingsManager.getAll())
let unsubscribe = null

const saveSettings = () => {
  try {
    settingsManager.saveSettings(settings.value)
    settingsManager.applyDarkMode()
    ElMessage.success('设置已保存')
  } catch (error) {
    console.error('保存设置失败:', error)
    ElMessage.error('保存设置失败')
  }
}

const resetSettings = () => {
  settingsManager.reset()
  settings.value = settingsManager.getAll()
  ElMessage.success('设置已恢复默认值')
}

onMounted(() => {
  unsubscribe = settingsManager.onChange((newSettings) => {
    settings.value = { ...newSettings }
  })
})

onUnmounted(() => {
  unsubscribe?.()
})

defineExpose({
  settings,
  resetSettings
})
</script>

<style scoped>
.settings-panel {
  max-width: 920px;
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.settings-section {
  background:
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.14), transparent 36%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.88), rgba(15, 23, 42, 0.96));
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 26px;
  padding: 24px 24px 8px;
  box-shadow: 0 18px 40px rgba(2, 6, 23, 0.22);
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #f8fafc;
  margin: 0 0 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 16px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.setting-info label {
  font-size: 15px;
  font-weight: 600;
  color: #f8fafc;
}

.setting-desc {
  font-size: 13px;
  color: rgba(226, 232, 240, 0.68);
  line-height: 1.5;
}

.switch {
  position: relative;
  display: inline-block;
  width: 54px;
  height: 30px;
  flex-shrink: 0;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  inset: 0;
  cursor: pointer;
  background: rgba(148, 163, 184, 0.22);
  transition: 0.3s;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.slider::before {
  position: absolute;
  content: '';
  height: 22px;
  width: 22px;
  left: 3px;
  bottom: 3px;
  background: #ffffff;
  transition: 0.3s;
  border-radius: 50%;
  box-shadow: 0 3px 8px rgba(15, 23, 42, 0.22);
}

input:checked + .slider {
  background: linear-gradient(135deg, #14b8a6, #0ea5e9);
}

input:checked + .slider::before {
  transform: translateX(24px);
}

.setting-select {
  min-width: 172px;
  padding: 10px 14px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.setting-select:hover,
.setting-select:focus {
  border-color: rgba(56, 189, 248, 0.42);
  box-shadow: 0 0 0 4px rgba(56, 189, 248, 0.08);
}

.setting-select option {
  background: #0f172a;
  color: #f8fafc;
}

.setting-button {
  padding: 10px 18px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #14b8a6, #0284c7);
  color: white;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 12px 26px rgba(2, 132, 199, 0.18);
}

.setting-button:hover {
  transform: translateY(-1px);
}

.setting-button.ghost {
  background: rgba(255, 255, 255, 0.08);
  color: #e2e8f0;
  box-shadow: none;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.setting-button.danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  box-shadow: 0 12px 26px rgba(220, 38, 38, 0.18);
}

@media (max-width: 768px) {
  .settings-section {
    padding: 20px 18px 6px;
    border-radius: 22px;
  }

  .setting-item {
    flex-direction: column;
    align-items: stretch;
  }

  .switch,
  .setting-select,
  .setting-button {
    align-self: flex-end;
  }
}
</style>
