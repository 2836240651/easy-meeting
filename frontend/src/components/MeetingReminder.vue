<template>
  <div class="meeting-reminder"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElNotification } from 'element-plus'
import { useRouter } from 'vue-router'
import { meetingReserveService } from '@/api/services'

const router = useRouter()

const reminderInterval = ref(null)
// 记录每个会议的提醒阶段：{ meetingId: { 60: true, 15: false, 5: false } }
const notifiedMeetings = ref(new Map())

// 检查即将开始的会议
const checkUpcomingMeetings = async () => {
  try {
    const result = await meetingReserveService.getUpcomingMeetings()
    
    if (result && result.data && result.data.data && result.data.data.length > 0) {
      result.data.data.forEach(meeting => {
        checkAndNotifyMeeting(meeting)
      })
    }
  } catch (error) {
    console.error('检查即将开始的会议失败:', error)
  }
}

// 检查并通知会议
const checkAndNotifyMeeting = (meeting) => {
  const now = Date.now()
  
  // 确保 startTime 是数字类型的时间戳
  let startTime = meeting.startTime
  if (typeof startTime === 'string') {
    startTime = new Date(startTime).getTime()
  } else if (startTime instanceof Date) {
    startTime = startTime.getTime()
  } else if (typeof startTime !== 'number') {
    console.error('无效的开始时间格式:', meeting.startTime)
    return
  }
  
  const diff = startTime - now
  const minutes = Math.floor(diff / 60000)
  
  // 获取或初始化该会议的提醒记录
  if (!notifiedMeetings.value.has(meeting.meetingId)) {
    notifiedMeetings.value.set(meeting.meetingId, {
      60: false,
      15: false,
      5: false
    })
  }
  
  const notifyRecord = notifiedMeetings.value.get(meeting.meetingId)
  
  // 判断应该触发哪个阶段的提醒
  if (minutes <= 60 && minutes > 15 && !notifyRecord[60]) {
    // 60分钟提醒
    showMeetingNotification(meeting, 60)
    notifyRecord[60] = true
  } else if (minutes <= 15 && minutes > 5 && !notifyRecord[15]) {
    // 15分钟提醒
    showMeetingNotification(meeting, 15)
    notifyRecord[15] = true
  } else if (minutes <= 5 && minutes >= 0 && !notifyRecord[5]) {
    // 5分钟提醒
    showMeetingNotification(meeting, 5)
    notifyRecord[5] = true
  }
}

// 显示会议通知
const showMeetingNotification = (meeting, minutesBefore) => {
  let message = ''
  let type = 'warning'
  
  if (minutesBefore === 60) {
    message = `会议"${meeting.meetingName}"将在60分钟后开始`
    type = 'info'
  } else if (minutesBefore === 15) {
    message = `会议"${meeting.meetingName}"将在15分钟后开始`
    type = 'warning'
  } else if (minutesBefore === 5) {
    message = `会议"${meeting.meetingName}"将在5分钟后开始，请做好准备`
    type = 'error'
  }
  
  ElNotification({
    title: '会议提醒',
    message: message,
    type: type,
    duration: 10000, // 10秒后自动关闭
    position: 'bottom-right',
    dangerouslyUseHTMLString: true,
    customClass: 'meeting-notification',
    onClick: () => {
      quickJoin(meeting)
    },
    showClose: true
  })
}

// 快速加入会议
const quickJoin = (meeting) => {
  router.push({
    path: '/meeting',
    query: { meetingId: meeting.meetingId }
  })
}

// 启动定时检查
const startReminderCheck = () => {
  // 立即检查一次
  checkUpcomingMeetings()
  
  // 每分钟检查一次
  reminderInterval.value = setInterval(() => {
    checkUpcomingMeetings()
  }, 60000) // 60秒
}

// 停止定时检查
const stopReminderCheck = () => {
  if (reminderInterval.value) {
    clearInterval(reminderInterval.value)
    reminderInterval.value = null
  }
}

onMounted(() => {
  startReminderCheck()
})

onBeforeUnmount(() => {
  stopReminderCheck()
})
</script>

<style>
.meeting-notification {
  cursor: pointer;
}

.meeting-notification:hover {
  opacity: 0.9;
}
</style>
