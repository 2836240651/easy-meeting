<template>
  <el-dialog
    v-model="dialogVisible"
    title="快速会议"
    width="500px"
    :before-close="handleClose"
  >
    <el-form :model="form" label-width="120px">
      <el-form-item label="会议名称">
        <el-input
          v-model="form.meetingName"
          placeholder="请输入会议名称（可选）"
          clearable
        />
      </el-form-item>

      <el-form-item label="会议号类型" required>
        <el-radio-group v-model="form.meetingNoType">
          <el-radio :label="0">使用个人会议号</el-radio>
          <el-radio :label="1">随机生成会议号</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="会议密码" required>
        <el-radio-group v-model="form.joinType">
          <el-radio :label="0">无密码</el-radio>
          <el-radio :label="1">设置密码</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="form.joinType === 1" label="密码">
        <el-input
          v-model="form.joinPassword"
          type="password"
          placeholder="请输入会议密码（最多5位）"
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
          创建会议
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

const emit = defineEmits(['update:visible', 'create'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const form = ref({
  meetingName: '',
  meetingNoType: 0,
  joinType: 0,
  joinPassword: ''
})

const submitting = ref(false)

// 监听对话框打开
watch(() => props.visible, (newVal) => {
  if (newVal) {
    // 重置表单
    form.value = {
      meetingName: '',
      meetingNoType: 0,
      joinType: 0,
      joinPassword: ''
    }
  }
})

// 验证表单
const validateForm = () => {
  if (form.value.joinType === 1) {
    if (!form.value.joinPassword || form.value.joinPassword.trim() === '') {
      ElMessage.error('请输入会议密码')
      return false
    }
    if (form.value.joinPassword.length > 5) {
      ElMessage.error('会议密码最多5位')
      return false
    }
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
    emit('create', {
      meetingName: form.value.meetingName.trim(),
      meetingNoType: form.value.meetingNoType,
      joinType: form.value.joinType,
      joinPassword: form.value.joinType === 1 ? form.value.joinPassword.trim() : ''
    })
    handleClose()
  } catch (error) {
    console.error('创建会议失败:', error)
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

/* 单选按钮样式 */
.el-radio {
  color: #cccccc !important;
  margin-right: 0 !important;
}

.el-radio__input.is-checked .el-radio__inner {
  background-color: #999999 !important;
  border-color: #999999 !important;
}

.el-radio__input.is-checked + .el-radio__label {
  color: #ffffff !important;
}

.el-radio__inner {
  background-color: #2a2a2a !important;
  border-color: #444 !important;
}

.el-radio__inner:hover {
  border-color: #666 !important;
}

.el-radio__label {
  color: #cccccc !important;
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

.el-radio-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
