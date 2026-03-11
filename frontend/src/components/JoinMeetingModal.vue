<template>
  <el-dialog
    v-model="dialogVisible"
    title="加入会议"
    width="500px"
    :before-close="handleClose"
  >
    <el-form :model="form" label-width="100px">
      <el-form-item label="会议号" required>
        <el-input
          v-model="form.meetingId"
          placeholder="请输入会议号"
          clearable
        />
      </el-form-item>

      <el-form-item label="会议密码">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="如果会议需要密码，请输入"
          maxlength="5"
          show-password
          clearable
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          加入会议
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'join'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const form = ref({
  meetingId: '',
  password: ''
})

const submitting = ref(false)

// 监听对话框打开
watch(() => props.visible, (newVal) => {
  if (newVal) {
    // 重置表单
    form.value = {
      meetingId: '',
      password: ''
    }
  }
})

// 验证表单
const validateForm = () => {
  if (!form.value.meetingId || form.value.meetingId.trim() === '') {
    ElMessage.error('请输入会议号')
    return false
  }
  return true
}

// 提交表单
const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  submitting.value = true
  try {
    emit('join', {
      meetingId: form.value.meetingId.trim(),
      password: form.value.password.trim()
    })
    handleClose()
  } catch (error) {
    console.error('加入会议失败:', error)
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
/* 深色主题样式 - 移除 scoped 以确保样式生效 */
.el-dialog {
  background-color: #1a1a1a !important;
  border: 1px solid #333 !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5) !important;
}

.el-dialog__header {
  background-color: #1a1a1a !important;
  border-bottom: 1px solid #333 !important;
  padding: 20px !important;
}

.el-dialog__title {
  color: #ffffff !important;
  font-size: 18px !important;
  font-weight: 600 !important;
}

.el-dialog__headerbtn .el-dialog__close {
  color: #999 !important;
  font-size: 20px !important;
}

.el-dialog__headerbtn .el-dialog__close:hover {
  color: #ffffff !important;
}

.el-dialog__body {
  background-color: #1a1a1a !important;
  padding: 20px !important;
  color: #ffffff !important;
}

.el-dialog__footer {
  background-color: #1a1a1a !important;
  border-top: 1px solid #333 !important;
  padding: 15px 20px !important;
}

/* 表单样式 */
.el-form-item__label {
  color: #cccccc !important;
}

.el-input__wrapper {
  background-color: #2a2a2a !important;
  border: 1px solid #444 !important;
  box-shadow: none !important;
}

.el-input__wrapper:hover {
  border-color: #666 !important;
}

.el-input__wrapper.is-focus {
  border-color: #999999 !important;
  box-shadow: 0 0 0 1px rgba(153, 153, 153, 0.2) !important;
}

.el-input__inner {
  color: #ffffff !important;
  background-color: transparent !important;
}

.el-input__inner::placeholder {
  color: #666 !important;
}

.el-input__suffix {
  color: #999 !important;
}

.el-input__suffix:hover {
  color: #ffffff !important;
}

/* 按钮样式 */
.el-button {
  border: 1px solid #444 !important;
  background-color: #2a2a2a !important;
  color: #ffffff !important;
}

.el-button:hover {
  background-color: #333 !important;
  border-color: #666 !important;
}

.el-button--primary {
  background-color: #999999 !important;
  border-color: #999999 !important;
  color: #ffffff !important;
}

.el-button--primary:hover {
  background-color: #b3b3b3 !important;
  border-color: #b3b3b3 !important;
}

.el-button--primary.is-loading {
  background-color: #999999 !important;
  border-color: #999999 !important;
}

/* 遮罩层 */
.el-overlay {
  background-color: rgba(0, 0, 0, 0.7) !important;
}

/* 局部样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
