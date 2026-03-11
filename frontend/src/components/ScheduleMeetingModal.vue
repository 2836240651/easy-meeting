<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '修改预约会议' : '预约会议'"
    width="600px"
    :before-close="handleClose"
  >
    <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
      <el-form-item label="会议名称" prop="meetingName">
        <el-input
          v-model="form.meetingName"
          placeholder="请输入会议名称"
          maxlength="50"
          show-word-limit
          clearable
        />
      </el-form-item>

      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker
          v-model="form.startTime"
          type="datetime"
          placeholder="选择开始时间（至少1小时后）"
          :disabled-date="disabledDate"
          :disabled-hours="disabledHours"
          :disabled-minutes="disabledMinutes"
          format="YYYY-MM-DD HH:mm"
          value-format="x"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="会议时长" prop="duration">
        <el-radio-group v-model="form.duration">
          <el-radio :label="30">30 分钟</el-radio>
          <el-radio :label="45">45 分钟</el-radio>
          <el-radio :label="60">60 分钟</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="加入方式" prop="joinType">
        <el-radio-group v-model="form.joinType">
          <el-radio :label="0">无需密码</el-radio>
          <el-radio :label="1">需要密码</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item 
        v-if="form.joinType === 1" 
        label="会议密码" 
        prop="joinPassword"
      >
        <el-input
          v-model="form.joinPassword"
          placeholder="请输入5位数字密码"
          maxlength="5"
          show-word-limit
          clearable
        />
        <div class="form-tip">密码为5位数字，用于验证参会者身份</div>
      </el-form-item>

      <el-form-item label="邀请成员" prop="inviteUserIds">
        <el-select
          v-model="form.inviteUserIds"
          multiple
          filterable
          placeholder="选择要邀请的成员（可选）"
          style="width: 100%"
          collapse-tags
          collapse-tags-tooltip
          :max-collapse-tags="3"
        >
          <el-option
            v-for="contact in contactList"
            :key="contact.userId"
            :label="contact.nickName"
            :value="contact.userId"
          >
            <div class="contact-option">
              <span>{{ contact.nickName }}</span>
              <span class="contact-email">{{ contact.email }}</span>
            </div>
          </el-option>
        </el-select>
        <div class="form-tip">
          已选择 {{ form.inviteUserIds.length }} 人
          <span v-if="form.inviteUserIds.length > 0">（包括您自己，共 {{ form.inviteUserIds.length + 1 }} 人）</span>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存修改' : '创建会议' }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { meetingReserveService, contactService } from '@/api/services'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  meetingData: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'created', 'updated'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const isEdit = computed(() => !!props.meetingData)

const formRef = ref(null)

const form = ref({
  meetingName: '',
  startTime: null,
  duration: 60, // 默认60分钟，可选30/45/60
  joinType: 0, // 0-无需密码，1-需要密码
  joinPassword: '',
  inviteUserIds: []
})

// 表单验证规则
const rules = {
  meetingName: [
    { required: true, message: '请输入会议名称', trigger: 'blur' },
    { min: 1, max: 50, message: '会议名称长度在 1 到 50 个字符', trigger: 'blur' }
  ],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        const oneHourLater = Date.now() + 60 * 60 * 1000
        if (value < oneHourLater) {
          callback(new Error('开始时间必须至少在1小时后'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  duration: [
    { required: true, message: '请选择会议时长', trigger: 'change' }
  ],
  joinPassword: [
    {
      validator: (rule, value, callback) => {
        // 只有选择需要密码时才验证
        if (form.value.joinType === 1) {
          if (!value || value.trim() === '') {
            callback(new Error('请输入会议密码'))
          } else if (!/^\d{5}$/.test(value)) {
            callback(new Error('密码必须是5位数字'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const contactList = ref([])
const submitting = ref(false)

// 禁用过去的日期和时间
const disabledDate = (time) => {
  // 获取当前时间加1小时
  const oneHourLater = new Date(Date.now() + 60 * 60 * 1000)
  // 禁用早于1小时后的日期
  return time.getTime() < oneHourLater.getTime() - 24 * 60 * 60 * 1000
}

// 禁用小时
const disabledHours = () => {
  const now = new Date()
  const oneHourLater = new Date(Date.now() + 60 * 60 * 1000)
  
  // 如果选择的日期是今天，禁用早于1小时后的小时
  if (form.value.startTime) {
    const selectedDate = new Date(form.value.startTime)
    if (selectedDate.toDateString() === now.toDateString()) {
      const currentHour = oneHourLater.getHours()
      const hours = []
      for (let i = 0; i < currentHour; i++) {
        hours.push(i)
      }
      return hours
    }
  }
  return []
}

// 禁用分钟
const disabledMinutes = (hour) => {
  const now = new Date()
  const oneHourLater = new Date(Date.now() + 60 * 60 * 1000)
  
  // 如果选择的日期是今天，且小时等于1小时后的小时
  if (form.value.startTime) {
    const selectedDate = new Date(form.value.startTime)
    if (selectedDate.toDateString() === now.toDateString()) {
      const currentHour = oneHourLater.getHours()
      const currentMinute = oneHourLater.getMinutes()
      
      if (hour === currentHour) {
        const minutes = []
        for (let i = 0; i < currentMinute; i++) {
          minutes.push(i)
        }
        return minutes
      }
    }
  }
  return []
}

// 加载联系人列表
const loadContacts = async () => {
  try {
    console.log('开始加载联系人列表...')
    const result = await contactService.loadContactUser()
    console.log('联系人列表 API 响应:', result)
    
    if (result && result.data) {
      if (result.data.code === 200) {
        // 后端返回的是 UserContact 对象，需要映射字段
        // contactId -> userId (用于选择)
        const contacts = result.data.data || []
        contactList.value = contacts.map(contact => ({
          userId: contact.contactId,  // 联系人的真实 userId
          nickName: contact.nickName,
          email: contact.email || '',
          avatar: contact.avatar,
          lastLoginTime: contact.lastLoginTime,
          lastOffTime: contact.lastOffTime
        }))
        console.log('联系人列表加载成功，数量:', contactList.value.length)
        console.log('联系人列表:', contactList.value)
      } else {
        console.error('加载联系人失败，错误码:', result.data.code, '错误信息:', result.data.info)
        ElMessage.error(result.data.info || '加载联系人失败')
      }
    } else {
      console.error('联系人列表响应格式错误:', result)
    }
  } catch (error) {
    console.error('加载联系人失败:', error)
    ElMessage.error('加载联系人失败: ' + (error.message || '未知错误'))
  }
}

// 监听对话框打开
watch(() => props.visible, (newVal) => {
  if (newVal) {
    loadContacts()
    if (props.meetingData) {
      // 编辑模式，填充数据
      form.value = {
        meetingId: props.meetingData.meetingId,
        meetingName: props.meetingData.meetingName,
        startTime: typeof props.meetingData.startTime === 'string' 
          ? new Date(props.meetingData.startTime).getTime()
          : props.meetingData.startTime,
        duration: props.meetingData.duration || 60,
        joinType: props.meetingData.joinType || 0,
        joinPassword: props.meetingData.joinPassword || '',
        inviteUserIds: props.meetingData.inviteUserIds ? 
          props.meetingData.inviteUserIds.split(',').filter(id => id.trim()) : []
      }
    } else {
      // 新建模式，重置表单，默认开始时间为1小时后
      const oneHourLater = Date.now() + 60 * 60 * 1000
      form.value = {
        meetingName: '',
        startTime: oneHourLater,
        duration: 60,
        joinType: 0,
        joinPassword: '',
        inviteUserIds: []
      }
    }
    // 清除验证
    if (formRef.value) {
      formRef.value.clearValidate()
    }
  }
})

// 提交表单
const handleSubmit = async () => {
  // 验证表单
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const params = {
      meetingName: form.value.meetingName.trim(),
      startTime: form.value.startTime,
      duration: form.value.duration, // 使用用户选择的时长
      joinType: form.value.joinType,
      joinPassword: form.value.joinType === 1 ? form.value.joinPassword : null,
      inviteUserIds: form.value.inviteUserIds.join(',')
    }

    if (isEdit.value) {
      // 更新预约会议
      params.meetingId = form.value.meetingId
      await meetingReserveService.updateMeetingReserve(params)
      ElMessage.success('修改成功')
      emit('updated')
    } else {
      // 创建预约会议
      await meetingReserveService.createMeetingReserve(params)
      ElMessage.success('创建成功')
      emit('created')
    }

    handleClose()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false
}
</script>

<style>
.el-overlay {
  background: rgba(2, 6, 23, 0.68) !important;
  backdrop-filter: blur(8px);
}

.el-dialog {
  overflow: hidden;
  border-radius: 28px !important;
  border: 1px solid rgba(148, 163, 184, 0.16) !important;
  background:
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.16), transparent 34%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.96), rgba(15, 23, 42, 0.98)) !important;
  box-shadow: 0 32px 80px rgba(2, 6, 23, 0.34) !important;
}

.el-dialog__header {
  padding: 24px 24px 14px !important;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12) !important;
}

.el-dialog__title {
  color: #f8fafc !important;
  font-size: 20px !important;
  font-weight: 700 !important;
}

.el-dialog__headerbtn {
  top: 18px !important;
  right: 18px !important;
}

.el-dialog__headerbtn .el-dialog__close {
  color: rgba(226, 232, 240, 0.72) !important;
  font-size: 22px !important;
}

.el-dialog__headerbtn .el-dialog__close:hover {
  color: #ffffff !important;
}

.el-dialog__body {
  padding: 20px 24px !important;
  color: #f8fafc !important;
}

.el-dialog__footer {
  padding: 16px 24px 24px !important;
  border-top: 1px solid rgba(148, 163, 184, 0.12) !important;
}

.el-form-item__label {
  color: rgba(226, 232, 240, 0.82) !important;
  font-weight: 600 !important;
}

.el-input__wrapper,
.el-select .el-input__wrapper,
.el-date-editor .el-input__wrapper {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(148, 163, 184, 0.18) !important;
  border-radius: 16px !important;
  box-shadow: none !important;
}

.el-input__wrapper:hover,
.el-select .el-input__wrapper:hover,
.el-date-editor .el-input__wrapper:hover {
  border-color: rgba(56, 189, 248, 0.28) !important;
}

.el-input__wrapper.is-focus,
.el-select .el-input__wrapper.is-focus,
.el-date-editor .el-input__wrapper.is-focus {
  border-color: rgba(56, 189, 248, 0.42) !important;
  box-shadow: 0 0 0 4px rgba(56, 189, 248, 0.08) !important;
}

.el-input__inner,
.el-select .el-input__inner,
.el-date-editor .el-input__inner {
  color: #f8fafc !important;
}

.el-input__inner::placeholder,
.el-select .el-select__placeholder {
  color: rgba(226, 232, 240, 0.42) !important;
}

.el-input__prefix,
.el-input__suffix,
.el-select .el-input__suffix {
  color: rgba(226, 232, 240, 0.58) !important;
}

.el-select__tags .el-tag {
  background: rgba(20, 184, 166, 0.16) !important;
  border-color: rgba(45, 212, 191, 0.22) !important;
  color: #ccfbf1 !important;
  border-radius: 999px !important;
}

.el-button {
  border-radius: 999px !important;
  border: 1px solid rgba(148, 163, 184, 0.16) !important;
  background: rgba(255, 255, 255, 0.08) !important;
  color: #f8fafc !important;
}

.el-button:hover {
  border-color: rgba(56, 189, 248, 0.28) !important;
  background: rgba(255, 255, 255, 0.12) !important;
}

.el-button--primary {
  border-color: transparent !important;
  background: linear-gradient(135deg, #14b8a6, #0284c7) !important;
  box-shadow: 0 12px 26px rgba(2, 132, 199, 0.18) !important;
}

.el-button--primary:hover {
  background: linear-gradient(135deg, #2dd4bf, #0ea5e9) !important;
}

.el-radio-group {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
}

.el-radio {
  color: rgba(226, 232, 240, 0.82) !important;
}

.el-radio__inner {
  background: rgba(255, 255, 255, 0.06) !important;
  border-color: rgba(148, 163, 184, 0.28) !important;
}

.el-radio__input.is-checked .el-radio__inner {
  background: linear-gradient(135deg, #14b8a6, #0284c7) !important;
  border-color: transparent !important;
}

.el-radio__input.is-checked + .el-radio__label {
  color: #f8fafc !important;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.form-tip {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: rgba(226, 232, 240, 0.58);
}

.contact-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.contact-email {
  font-size: 12px;
  color: rgba(125, 211, 252, 0.82);
}
</style>
