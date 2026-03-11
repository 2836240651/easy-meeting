<template>
  <div class="smart-summary-container">
    <div class="summary-header">
      <h2>📋 智能会议纪要</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="generateSummary" :disabled="loading">
          {{ loading ? '生成中...' : '🔄 生成纪要' }}
        </button>
        <button class="btn btn-secondary" @click="exportSummary" :disabled="!summary">
          📥 导出
        </button>
        <button class="btn btn-secondary" @click="shareSummary" :disabled="!summary">
          📤 分享
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>AI 正在分析会议内容...</p>
    </div>

    <div v-else-if="summary" class="summary-content">
      <!-- 会议基本信息 -->
      <div class="summary-card basic-info">
        <h3>📌 会议基本信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">会议名称：</span>
            <span class="value">{{ summary.meetingName }}</span>
          </div>
          <div class="info-item">
            <span class="label">会议时间：</span>
            <span class="value">{{ formatDate(summary.meetingTime) }}</span>
          </div>
          <div class="info-item">
            <span class="label">参会人数：</span>
            <span class="value">{{ summary.participantCount }} 人</span>
          </div>
          <div class="info-item">
            <span class="label">会议时长：</span>
            <span class="value">{{ summary.duration }} 分钟</span>
          </div>
        </div>
        <div class="participants">
          <span class="label">参会人员：</span>
          <span class="value">{{ summary.participants ? summary.participants.join('、') : '' }}</span>
        </div>
      </div>

      <!-- 会议概要 -->
      <div class="summary-card overview">
        <h3>📝 会议概要</h3>
        <p>{{ summary.overview }}</p>
      </div>

      <!-- 讨论要点 -->
      <div class="summary-card discussion-points">
        <h3>💬 讨论要点</h3>
        <ul>
          <li v-for="(point, index) in summary.discussionPoints" :key="index">
            {{ point }}
          </li>
        </ul>
      </div>

      <!-- 关键决策 -->
      <div class="summary-card decisions" v-if="summary.decisions && summary.decisions.length">
        <h3>✅ 关键决策</h3>
        <ul>
          <li v-for="(decision, index) in summary.decisions" :key="index">
            {{ decision }}
          </li>
        </ul>
      </div>

      <!-- 待办事项 -->
      <div class="summary-card action-items" v-if="summary.actionItems && summary.actionItems.length">
        <h3>📌 待办事项</h3>
        <table class="action-table">
          <thead>
            <tr>
              <th>任务</th>
              <th>负责人</th>
              <th>截止时间</th>
              <th>优先级</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in summary.actionItems" :key="index">
              <td>{{ item.task }}</td>
              <td>{{ item.assignee }}</td>
              <td>{{ item.deadline || '-' }}</td>
              <td>
                <span :class="['priority-badge', item.priority.toLowerCase()]">
                  {{ getPriorityText(item.priority) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 精彩时刻 -->
      <div class="summary-card highlights" v-if="summary.highlights && summary.highlights.length">
        <h3>⭐ 精彩时刻</h3>
        <ul>
          <li v-for="(highlight, index) in summary.highlights" :key="index">
            {{ highlight }}
          </li>
        </ul>
      </div>

      <!-- 会议氛围 -->
      <div class="summary-card sentiment" v-if="summary.sentiment">
        <h3>😊 会议氛围</h3>
        <p>{{ summary.sentiment }}</p>
      </div>

      <!-- AI 建议 -->
      <div class="summary-card suggestions" v-if="summary.suggestions && summary.suggestions.length">
        <h3>💡 AI 建议</h3>
        <ul>
          <li v-for="(suggestion, index) in summary.suggestions" :key="index">
            {{ suggestion }}
          </li>
        </ul>
      </div>
    </div>

    <div v-else class="empty-state">
      <p>点击上方按钮生成智能会议纪要</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { aiService } from '../api/services'

const props = defineProps({
  meetingId: {
    type: String,
    required: true
  }
})

const loading = ref(false)
const summary = ref(null)

const generateSummary = async () => {
  loading.value = true
  try {
    const response = await aiService.generateSmartSummary(props.meetingId)
    if (response.code === 0 && response.data) {
      summary.value = response.data
    } else {
      alert('生成失败: ' + (response.msg || '未知错误'))
    }
  } catch (error) {
    console.error('生成智能纪要失败:', error)
    alert('生成失败，请重试')
  } finally {
    loading.value = false
  }
}

const exportSummary = () => {
  if (!summary.value) return
  
  let content = `# ${summary.value.meetingName}\n\n`
  content += `## 会议概要\n${summary.value.overview}\n\n`
  
  if (summary.value.discussionPoints) {
    content += `## 讨论要点\n`
    summary.value.discussionPoints.forEach((point, i) => {
      content += `${i + 1}. ${point}\n`
    })
    content += '\n'
  }
  
  if (summary.value.decisions) {
    content += `## 关键决策\n`
    summary.value.decisions.forEach((d, i) => {
      content += `${i + 1}. ${d}\n`
    })
    content += '\n'
  }
  
  if (summary.value.actionItems) {
    content += `## 待办事项\n`
    content += '| 任务 | 负责人 | 截止时间 | 优先级 |\n'
    content += '|------|--------|----------|--------|\n'
    summary.value.actionItems.forEach(item => {
      content += `| ${item.task} | ${item.assignee} | ${item.deadline || '-'} | ${item.priority} |\n`
    })
    content += '\n'
  }
  
  if (summary.value.suggestions) {
    content += `## AI 建议\n`
    summary.value.suggestions.forEach((s, i) => {
      content += `${i + 1}. ${s}\n`
    })
  }

  const blob = new Blob([content], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${summary.value.meetingName}_会议纪要.md`
  a.click()
  URL.revokeObjectURL(url)
}

const shareSummary = () => {
  if (!summary.value) return
  
  // 复制到剪贴板
  const text = `会议纪要：${summary.value.meetingName}\n\n${summary.value.overview}`
  navigator.clipboard.writeText(text).then(() => {
    alert('已复制到剪贴板')
  })
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const getPriorityText = (priority) => {
  const map = {
    'HIGH': '高',
    'MEDIUM': '中',
    'LOW': '低'
  }
  return map[priority] || priority
}

onMounted(() => {
  // 可以选择自动生成
})
</script>

<style scoped>
.smart-summary-container {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.summary-header h2 {
  margin: 0;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.summary-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.summary-card h3 {
  margin: 0 0 16px 0;
  color: #333;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.basic-info .info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.basic-info .info-item {
  display: flex;
}

.basic-info .label {
  color: #666;
  margin-right: 8px;
  white-space: nowrap;
}

.basic-info .value {
  color: #333;
  font-weight: 500;
}

.basic-info .participants {
  margin-top: 8px;
}

.discussion-points ul,
.decisions ul,
.highlights ul,
.suggestions ul {
  margin: 0;
  padding-left: 20px;
}

.discussion-points li,
.decisions li,
.highlights li,
.suggestions li {
  margin-bottom: 8px;
  line-height: 1.6;
}

.decisions li {
  color: #67c23a;
}

.action-table {
  width: 100%;
  border-collapse: collapse;
}

.action-table th,
.action-table td {
  padding: 10px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.action-table th {
  background: #f5f7fa;
  font-weight: 600;
}

.priority-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.priority-badge.high {
  background: #f56c6c;
  color: #fff;
}

.priority-badge.medium {
  background: #e6a23c;
  color: #fff;
}

.priority-badge.low {
  background: #909399;
  color: #fff;
}

.sentiment p {
  margin: 0;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}
</style>
