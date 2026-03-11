import axios from 'axios'

const GENERAL_REQUEST_TIMEOUT = 10000
export const AI_REQUEST_TIMEOUT = 60000

const DEFAULT_TIMEOUT_MESSAGE = '请求暂时没有完成，请稍后再试。'
const AI_TIMEOUT_MESSAGE = 'AI 正在处理中，本次回复耗时较长，请稍后再试。'
const SERVER_ERROR_MESSAGE = '服务暂时不可用，请稍后再试。'
const NETWORK_ERROR_MESSAGE = '网络连接异常，请检查网络后重试。'

const isAiRequest = (config) => String(config?.url || '').startsWith('/ai/')

const isTimeoutError = (error) => {
  const message = String(error?.message || '').toLowerCase()
  return error?.code === 'ECONNABORTED' || message.includes('timeout') || message.includes('exceeded')
}

const api = axios.create({
  baseURL: '/api',
  timeout: GENERAL_REQUEST_TIMEOUT,
  headers: {
    'Content-Type': 'application/json'
  }
})

export const getReadableErrorMessage = (
  error,
  fallback = DEFAULT_TIMEOUT_MESSAGE,
  options = {}
) => {
  const aiRequest = options.aiRequest ?? isAiRequest(error?.config)

  if (isTimeoutError(error)) {
    return aiRequest ? AI_TIMEOUT_MESSAGE : fallback
  }

  if (error?.response?.data?.info) {
    return error.response.data.info
  }

  if (error?.response?.status >= 500) {
    return SERVER_ERROR_MESSAGE
  }

  if (error?.message === 'Network Error') {
    return NETWORK_ERROR_MESSAGE
  }

  return fallback
}

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.token = token
    }

    if (isAiRequest(config)) {
      config.timeout = AI_REQUEST_TIMEOUT
    }

    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
    }

    error.userMessage = getReadableErrorMessage(error)
    return Promise.reject(error)
  }
)

export default api
