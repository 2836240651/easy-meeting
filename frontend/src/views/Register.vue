<template>
  <div class="auth-page register-page">
    <div class="auth-shell">
      <section class="brand-panel">
        <div class="brand-badge">EasyMeeting</div>
        <h1>创建一个更顺手的会议入口</h1>
        <p>注册后即可使用预约、聊天、屏幕共享和 AI 会议助手。</p>
        <div class="brand-points">
          <div class="brand-point">
            <strong>快速发起会议</strong>
            <span>创建即时会议或预约会议，并统一管理参会成员。</span>
          </div>
          <div class="brand-point">
            <strong>会中协作清晰</strong>
            <span>视频、聊天、共享、成员管理维持一致的交互体验。</span>
          </div>
          <div class="brand-point">
            <strong>会后沉淀更快</strong>
            <span>自动生成摘要与行动项，减少人工整理成本。</span>
          </div>
        </div>
      </section>

      <section class="auth-card">
        <div class="auth-card-header">
          <span class="eyebrow">创建账号</span>
          <h2>注册 EasyMeeting</h2>
          <p>填写基础信息后即可开始使用。</p>
        </div>

        <form class="auth-form" @submit.prevent="handleRegister">
          <div class="form-group">
            <label for="email">邮箱</label>
            <input
              id="email"
              v-model.trim="form.email"
              type="email"
              required
              placeholder="请输入邮箱" />
          </div>

          <div class="form-group">
            <label for="nickName">昵称</label>
            <input
              id="nickName"
              v-model.trim="form.nickName"
              type="text"
              required
              maxlength="20"
              placeholder="请输入昵称" />
          </div>

          <div class="form-group">
            <label for="password">密码</label>
            <input
              id="password"
              v-model="form.password"
              type="password"
              required
              placeholder="请输入密码" />
          </div>

          <div class="form-group">
            <label for="checkCode">验证码</label>
            <div class="captcha-row">
              <input
                id="checkCode"
                v-model.trim="form.checkCode"
                type="text"
                required
                placeholder="请输入验证码" />
              <button type="button" class="captcha-button" @click="getCaptcha" title="刷新验证码">
                <img :src="captchaImage" alt="验证码" class="captcha-image" />
              </button>
            </div>
          </div>

          <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
          <p v-if="successMessage" class="feedback success">{{ successMessage }}</p>

          <button type="submit" class="submit-button" :disabled="submitting">
            {{ submitting ? '注册中...' : '注册' }}
          </button>

          <div class="auth-link">
            已有账号？
            <router-link to="/login">立即登录</router-link>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authService } from '../api/services'
import { getReadableErrorMessage } from '../api/axios'

const router = useRouter()

const form = ref({
  email: '',
  nickName: '',
  password: '',
  checkCode: '',
  checkCodeKey: ''
})

const captchaImage = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const submitting = ref(false)

const getCaptcha = async () => {
  try {
    const response = await authService.getCaptcha()
    if (response.data?.code === 200 && response.data?.data) {
      form.value.checkCodeKey = response.data.data.checkCodeKey
      captchaImage.value = `data:image/png;base64,${response.data.data.checkCode}`
      errorMessage.value = ''
      return
    }

    errorMessage.value = response.data?.info || '验证码加载失败，请刷新重试。'
  } catch (error) {
    console.error('获取验证码失败:', error)
    errorMessage.value = getReadableErrorMessage(error, '验证码加载失败，请检查网络后重试。')
  }
}

const handleRegister = async () => {
  errorMessage.value = ''
  successMessage.value = ''
  submitting.value = true

  try {
    const response = await authService.register({
      checkCodeKey: form.value.checkCodeKey,
      email: form.value.email,
      nickName: form.value.nickName,
      password: form.value.password,
      checkCode: form.value.checkCode
    })

    if (response.data?.code === 200 || response.data?.status === 'success') {
      successMessage.value = response.data?.info || '注册成功，即将跳转到登录页。'
      setTimeout(() => {
        router.push('/login')
      }, 1500)
      return
    }

    errorMessage.value = response.data?.info || '注册失败，请检查输入信息。'
    await getCaptcha()
  } catch (error) {
    console.error('注册失败:', error)
    errorMessage.value = error.response?.data?.info || getReadableErrorMessage(error, '注册失败，请稍后重试。')
    await getCaptcha()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  getCaptcha()
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 18px;
  background:
    radial-gradient(circle at top left, rgba(45, 212, 191, 0.18), transparent 28%),
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.16), transparent 24%),
    linear-gradient(180deg, #020617 0%, #0f172a 54%, #111827 100%);
}

.auth-shell {
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: 1.04fr 0.96fr;
  gap: 24px;
}

.brand-panel,
.auth-card {
  border-radius: 32px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 28px 70px rgba(2, 6, 23, 0.28);
}

.brand-panel {
  padding: 40px;
  background:
    radial-gradient(circle at top right, rgba(56, 189, 248, 0.18), transparent 34%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.88), rgba(15, 23, 42, 0.98));
  color: #f8fafc;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.16);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.brand-panel h1 {
  margin: 22px 0 12px;
  font-size: clamp(36px, 5vw, 54px);
  line-height: 1.04;
  letter-spacing: -0.04em;
}

.brand-panel > p {
  margin: 0 0 28px;
  font-size: 16px;
  color: rgba(226, 232, 240, 0.74);
}

.brand-points {
  display: grid;
  gap: 14px;
}

.brand-point {
  padding: 18px 18px 16px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.1);
}

.brand-point strong {
  display: block;
  margin-bottom: 6px;
  font-size: 16px;
}

.brand-point span {
  color: rgba(226, 232, 240, 0.68);
  line-height: 1.6;
  font-size: 14px;
}

.auth-card {
  padding: 34px 32px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
}

.auth-card-header {
  margin-bottom: 26px;
}

.eyebrow {
  display: inline-block;
  margin-bottom: 10px;
  color: #0284c7;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.auth-card-header h2 {
  margin: 0 0 8px;
  font-size: 30px;
  color: #0f172a;
  letter-spacing: -0.03em;
}

.auth-card-header p {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.auth-form {
  display: grid;
  gap: 18px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.form-group input {
  width: 100%;
  box-sizing: border-box;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: #fff;
  color: #0f172a;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-group input:focus {
  border-color: rgba(56, 189, 248, 0.42);
  box-shadow: 0 0 0 4px rgba(56, 189, 248, 0.08);
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 118px;
  gap: 10px;
}

.captcha-button {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 18px;
  background: #fff;
  padding: 0;
  cursor: pointer;
  overflow: hidden;
}

.captcha-image {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 50px;
  object-fit: cover;
}

.feedback {
  margin: -4px 0 0;
  padding: 12px 14px;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.5;
}

.feedback.error {
  background: rgba(239, 68, 68, 0.1);
  color: #b91c1c;
}

.feedback.success {
  background: rgba(20, 184, 166, 0.12);
  color: #0f766e;
}

.submit-button {
  border: none;
  border-radius: 20px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #0f766e, #2563eb);
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 16px 34px rgba(37, 99, 235, 0.18);
}

.submit-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.auth-link {
  text-align: center;
  font-size: 14px;
  color: #64748b;
}

.auth-link a {
  color: #0284c7;
  font-weight: 700;
  text-decoration: none;
}

@media (max-width: 900px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    padding: 28px;
  }
}

@media (max-width: 640px) {
  .auth-card,
  .brand-panel {
    border-radius: 24px;
  }

  .auth-card {
    padding: 24px 20px;
  }

  .captcha-row {
    grid-template-columns: 1fr 100px;
  }
}
</style>
