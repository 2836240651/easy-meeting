<template>
  <div class="reservation-list">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="即将开始" name="upcoming">
        <div v-if="upcomingMeetings.length === 0" class="empty-state">
          <el-empty description="暂时没有即将开始的会议" />
        </div>
        <div v-else class="meeting-list">
          <el-card
            v-for="meeting in upcomingMeetings"
            :key="meeting.meetingId"
            class="meeting-card"
            shadow="hover">
            <div class="meeting-header">
              <h3>{{ meeting.meetingName }}</h3>
              <el-tag :type="getStatusType(meeting)">
                {{ getMeetingStatus(meeting) }}
              </el-tag>
            </div>
            <div class="meeting-info">
              <p><span class="label">创建者</span>{{ meeting.nickName || '未知' }}</p>
              <p><span class="label">开始时间</span>{{ formatMeetingTime(meeting.startTime) }}</p>
              <p><span class="label">会议时长</span>{{ meeting.duration }} 分钟</p>
            </div>
            <div class="meeting-actions">
              <el-button
                type="primary"
                size="small"
                :disabled="!canJoinMeeting(meeting)"
                @click="joinMeeting(meeting)">
                {{ getJoinButtonText(meeting) }}
              </el-button>
              <template v-if="meeting.createUserId === currentUserId">
                <el-button size="small" @click="editMeeting(meeting)">修改</el-button>
                <el-button type="danger" size="small" @click="cancelMeeting(meeting)">取消</el-button>
              </template>
              <template v-else>
                <el-button type="warning" size="small" @click="leaveMeeting(meeting)">退出</el-button>
              </template>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="已结束" name="ended">
        <div v-if="endedMeetings.length === 0" class="empty-state">
          <el-empty description="暂时没有已结束的会议" />
        </div>
        <div v-else class="meeting-list">
          <el-card
            v-for="meeting in endedMeetings"
            :key="meeting.meetingId"
            class="meeting-card"
            shadow="hover">
            <div class="meeting-header">
              <h3>{{ meeting.meetingName }}</h3>
              <el-tag type="info">
                {{ getMeetingStatus(meeting) }}
              </el-tag>
            </div>
            <div class="meeting-info">
              <p><span class="label">创建者</span>{{ meeting.nickName || '未知' }}</p>
              <p><span class="label">开始时间</span>{{ formatMeetingTime(meeting.startTime) }}</p>
              <p><span class="label">会议时长</span>{{ meeting.duration }} 分钟</p>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="passwordDialogVisible" title="输入会议密码" width="400px">
      <el-input
        v-model="meetingPassword"
        placeholder="请输入 5 位会议密码"
        maxlength="5"
        show-word-limit />
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmJoin">确定</el-button>
      </template>
    </el-dialog>

    <ScheduleMeetingModal
      v-model:visible="editDialogVisible"
      :meeting-data="currentMeeting"
      @updated="handleMeetingUpdated" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { meetingReserveService } from '@/api/services'
import ScheduleMeetingModal from './ScheduleMeetingModal.vue'

const router = useRouter()

const activeTab = ref('upcoming')
const allMeetings = ref([])
const currentUserId = ref('')
const passwordDialogVisible = ref(false)
const meetingPassword = ref('')
const currentMeeting = ref(null)
const editDialogVisible = ref(false)

const upcomingMeetings = computed(() => {
  const now = Date.now()
  return allMeetings.value
    .filter((meeting) => {
      const startTime =
        typeof meeting.startTime === 'string' ? new Date(meeting.startTime).getTime() : meeting.startTime
      return startTime >= now && meeting.status === 0
    })
    .sort((a, b) => {
      const timeA = typeof a.startTime === 'string' ? new Date(a.startTime).getTime() : a.startTime
      const timeB = typeof b.startTime === 'string' ? new Date(b.startTime).getTime() : b.startTime
      return timeA - timeB
    })
})

const endedMeetings = computed(() => {
  const now = Date.now()
  return allMeetings.value
    .filter((meeting) => {
      const startTime =
        typeof meeting.startTime === 'string' ? new Date(meeting.startTime).getTime() : meeting.startTime
      const endTime = startTime + meeting.duration * 60 * 1000
      return endTime < now || meeting.status !== 0
    })
    .sort((a, b) => {
      const timeA = typeof a.startTime === 'string' ? new Date(a.startTime).getTime() : a.startTime
      const timeB = typeof b.startTime === 'string' ? new Date(b.startTime).getTime() : b.startTime
      return timeB - timeA
    })
})

const loadMeetingReserveList = async () => {
  try {
    const result = await meetingReserveService.loadMeetingReserveList()
    if (result?.data) {
      allMeetings.value = result.data.data || []
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
      currentUserId.value = userInfo.userId || ''
    }
  } catch (error) {
    console.error('加载预约会议列表失败:', error)
    ElMessage.error('加载预约会议列表失败')
  }
}

const formatMeetingTime = (timestamp) => {
  if (!timestamp) return ''
  if (typeof timestamp === 'string') return timestamp

  const date = new Date(timestamp)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const getMeetingStatus = (meeting) => {
  const now = Date.now()
  const startTime =
    typeof meeting.startTime === 'string' ? new Date(meeting.startTime).getTime() : meeting.startTime
  const endTime = startTime + meeting.duration * 60 * 1000

  if (meeting.status === 3) return '已取消'
  if (meeting.status === 2) return '已结束'
  if (meeting.status === 1) return '进行中'
  if (endTime < now) return '已结束'
  if (startTime <= now) return '进行中'

  const diff = startTime - now
  const minutes = Math.floor(diff / 60000)
  if (minutes < 60) return `${minutes} 分钟后开始`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时后开始`
  const days = Math.floor(hours / 24)
  return `${days} 天后开始`
}

const getStatusType = (meeting) => {
  if (meeting.status === 3 || meeting.status === 2) return 'info'
  if (meeting.status === 1) return 'success'

  const now = Date.now()
  const startTime =
    typeof meeting.startTime === 'string' ? new Date(meeting.startTime).getTime() : meeting.startTime
  const diff = startTime - now
  const minutes = Math.floor(diff / 60000)

  if (minutes < 15) return 'danger'
  if (minutes < 60) return 'warning'
  return 'success'
}

const canJoinMeeting = (meeting) => {
  const now = Date.now()
  const startTime =
    typeof meeting.startTime === 'string' ? new Date(meeting.startTime).getTime() : meeting.startTime
  const endTime = startTime + meeting.duration * 60 * 1000

  if (meeting.status !== 0 || endTime < now) {
    return false
  }

  const fiveMinutesBefore = startTime - 5 * 60 * 1000
  return now >= fiveMinutesBefore
}

const getJoinButtonText = (meeting) => {
  if (!canJoinMeeting(meeting)) {
    const now = Date.now()
    const startTime =
      typeof meeting.startTime === 'string' ? new Date(meeting.startTime).getTime() : meeting.startTime
    const endTime = startTime + meeting.duration * 60 * 1000

    if (meeting.status !== 0) return '会议已取消'
    if (endTime < now) return '会议已结束'
    return '等待开始'
  }
  return '加入会议'
}

const joinMeeting = (meeting) => {
  if (!canJoinMeeting(meeting)) {
    const now = Date.now()
    const startTime =
      typeof meeting.startTime === 'string' ? new Date(meeting.startTime).getTime() : meeting.startTime
    const diff = startTime - now
    const minutes = Math.floor(diff / 60000)

    if (minutes > 0) {
      const hours = Math.floor(minutes / 60)
      const remainMinutes = minutes % 60
      const timeText = hours > 0 ? `${hours} 小时 ${remainMinutes} 分钟` : `${minutes} 分钟`
      ElMessage.warning(`会议尚未开始，请等待预约时间。距离开始还有 ${timeText}`)
    } else {
      ElMessage.warning('会议已结束')
    }
    return
  }

  currentMeeting.value = meeting
  if (meeting.joinType === 1) {
    passwordDialogVisible.value = true
  } else {
    confirmJoin()
  }
}

const confirmJoin = async () => {
  try {
    const params = {
      meetingId: currentMeeting.value.meetingId,
      password: currentMeeting.value.joinType === 1 ? meetingPassword.value : null
    }

    const result = await meetingReserveService.joinMeetingReserve(params)
    if (result?.data) {
      passwordDialogVisible.value = false
      meetingPassword.value = ''
      ElMessage.success('加入会议成功')
      router.push({
        path: '/meeting',
        query: { meetingId: currentMeeting.value.meetingId }
      })
    }
  } catch (error) {
    console.error('加入会议失败:', error)
    ElMessage.error(error.message || '加入会议失败')
  }
}

const editMeeting = (meeting) => {
  currentMeeting.value = meeting
  editDialogVisible.value = true
}

const handleMeetingUpdated = () => {
  loadMeetingReserveList()
}

const cancelMeeting = async (meeting) => {
  try {
    await ElMessageBox.confirm('确定要取消这个预约会议吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await meetingReserveService.cancelMeetingReserve(meeting.meetingId)
    ElMessage.success('取消成功')
    loadMeetingReserveList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消会议失败:', error)
      ElMessage.error(error.message || '取消会议失败')
    }
  }
}

const leaveMeeting = async (meeting) => {
  try {
    await ElMessageBox.confirm('确定要退出这个预约会议吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await meetingReserveService.leaveMeetingReserve(meeting.meetingId)
    ElMessage.success('退出成功')
    loadMeetingReserveList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出会议失败:', error)
      ElMessage.error(error.message || '退出会议失败')
    }
  }
}

onMounted(() => {
  loadMeetingReserveList()
})

defineExpose({
  loadMeetingReserveList
})
</script>

<style scoped>
.reservation-list {
  padding: 0;
}

.reservation-list :deep(.el-tabs__item) {
  color: rgba(148, 163, 184, 0.82) !important;
  font-weight: 600;
  font-size: 15px;
  height: 42px;
}

.reservation-list :deep(.el-tabs__item:hover) {
  color: #f8fafc !important;
}

.reservation-list :deep(.el-tabs__item.is-active) {
  color: #f8fafc !important;
}

.reservation-list :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #14b8a6, #38bdf8) !important;
  height: 3px !important;
  border-radius: 999px !important;
}

.reservation-list :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(148, 163, 184, 0.14) !important;
}

.reservation-list :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.meeting-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.meeting-card {
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background:
    radial-gradient(circle at top right, rgba(56, 189, 248, 0.16), transparent 35%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.86), rgba(15, 23, 42, 0.94));
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.reservation-list :deep(.meeting-card .el-card__body) {
  padding: 22px;
}

.meeting-card:hover {
  transform: translateY(-4px);
  border-color: rgba(56, 189, 248, 0.28);
  box-shadow: 0 18px 40px rgba(2, 6, 23, 0.28);
}

.meeting-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.meeting-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #f8fafc;
}

.meeting-info {
  margin-bottom: 18px;
  display: grid;
  gap: 10px;
}

.meeting-info p {
  margin: 0;
  padding: 10px 12px;
  border-radius: 14px;
  color: rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.1);
}

.meeting-info .label {
  display: inline-block;
  min-width: 74px;
  margin-right: 8px;
  font-weight: 600;
  color: rgba(125, 211, 252, 0.88);
}

.meeting-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.reservation-list :deep(.meeting-actions .el-button) {
  border-radius: 999px;
  padding: 9px 16px;
  font-weight: 600;
}

.reservation-list :deep(.meeting-actions .el-button--primary) {
  background: linear-gradient(135deg, #14b8a6, #0284c7);
  border-color: transparent;
}

.reservation-list :deep(.meeting-actions .el-button--warning) {
  background: rgba(251, 191, 36, 0.14);
  border-color: rgba(251, 191, 36, 0.26);
  color: #fde68a;
}

.reservation-list :deep(.meeting-actions .el-button--danger) {
  background: rgba(248, 113, 113, 0.12);
  border-color: rgba(248, 113, 113, 0.24);
  color: #fecaca;
}

.meeting-actions .el-button.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.empty-state {
  padding: 56px 0;
  border-radius: 24px;
  background: rgba(15, 23, 42, 0.58);
  border: 1px dashed rgba(148, 163, 184, 0.2);
}

.reservation-list :deep(.el-empty__description p) {
  color: rgba(226, 232, 240, 0.72);
}

.reservation-list :deep(.el-tag) {
  border-radius: 999px;
  padding: 0 12px;
  font-weight: 700;
}

@media (max-width: 768px) {
  .meeting-list {
    grid-template-columns: 1fr;
  }
}
</style>
