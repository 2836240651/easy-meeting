<template>
  <div class="auth-page login-page">
    <div class="auth-shell">
      <section class="brand-panel">
        <div class="brand-badge">EasyMeeting</div>
        <h1>进入你的会议空间</h1>
        <p>更清晰的会议入口，更统一的桌面体验。</p>
        <div class="brand-points">
          <div class="brand-point">
            <strong>实时协作</strong>
            <span>语音、视频、共享与聊天集中在一个界面完成。</span>
          </div>
          <div class="brand-point">
            <strong>预约管理</strong>
            <span>快速发起会议、查看提醒、跟进待办事项。</span>
          </div>
          <div class="brand-point">
            <strong>AI 助手</strong>
            <span>自动生成摘要、行动建议和会后总结。</span>
          </div>
        </div>
      </section>

      <section class="auth-card">
        <div class="auth-card-header">
          <span class="eyebrow">欢迎回来</span>
          <h2>登录 EasyMeeting</h2>
          <p>输入账号信息后即可进入工作台。</p>
        </div>

        <form class="auth-form" @submit.prevent="handleLogin">
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

          <button type="submit" class="submit-button" :disabled="submitting">
            {{ submitting ? '登录中...' : '登录' }}
          </button>

          <div class="auth-link">
            还没有账号？
            <router-link to="/register">立即注册</router-link>
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
  password: '',
  checkCode: '',
  checkCodeKey: ''
})

const captchaImage = ref('')
const errorMessage = ref('')
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

const handleLogin = async () => {
  errorMessage.value = ''
  submitting.value = true

  try {
    const response = await authService.login({
      checkCodeKey: form.value.checkCodeKey,
      email: form.value.email,
      password: form.value.password,
      checkCode: form.value.checkCode
    })

    if (response.data?.code === 200 && response.data?.data) {
      const userInfo = response.data.data
      localStorage.setItem('token', userInfo.token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
      router.push('/dashboard')
      return
    }

    errorMessage.value = response.data?.info || '登录失败，请检查输入信息。'
    await getCaptcha()
  } catch (error) {
    console.error('登录失败:', error)
    errorMessage.value = error.response?.data?.info || getReadableErrorMessage(error, '登录失败，请稍后重试。')
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
  grid-template-columns: 1.1fr 0.9fr;
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
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.18), transparent 34%),
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
  color: #0f766e;
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
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
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

.submit-button {
  border: none;
  border-radius: 20px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #14b8a6, #0284c7);
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 16px 34px rgba(2, 132, 199, 0.18);
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
  color: #0f766e;
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
