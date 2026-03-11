<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content invite-modal">
      <div class="modal-header">
        <h3>邀请成员</h3>
        <button class="modal-close" @click="$emit('close')">&times;</button>
      </div>
      
      <div class="modal-body">
        <!-- 搜索区域 -->
        <div class="search-section">
          <div class="search-input-group">
            <input 
              v-model="searchInput" 
              type="text" 
              placeholder="搜索用户名或邮箱" 
              class="search-input"
              @keyup.enter="handleSearch"
            >
            <button class="search-btn" @click="handleSearch">
              <el-icon class="search-icon"><Search /></el-icon>
            </button>
          </div>
          
          <!-- 搜索结果 -->
          <div v-if="searchResult" class="search-result-item">
            <img :src="getUserAvatar(searchResult)" alt="Avatar" class="user-avatar">
            <div class="user-info">
              <div class="user-name">{{ searchResult.nickName }}</div>
              <div class="user-email">{{ searchResult.email }}</div>
            </div>
            <button 
              v-if="!isUserInMeeting(searchResult.userId)"
              class="invite-btn" 
              @click="handleInviteUser(searchResult)"
              :disabled="inviting">
              {{ inviting ? '邀请中...' : '邀请' }}
            </button>
            <span v-else class="status-text">已在会议中</span>
          </div>
          
          <!-- 搜索提示 -->
          <div v-if="searchMessage" class="search-message" :class="{ 'error': searchError }">
            {{ searchMessage }}
          </div>
        </div>
        
        <!-- 联系人列表 -->
        <div class="contacts-section">
          <div class="section-header">
            <h4>我的联系人</h4>
            <span class="contact-count">{{ filteredContacts.length }} 人</span>
          </div>
          
          <div v-if="loading" class="loading-state">
            加载中...
          </div>
          
          <div v-else-if="filteredContacts.length === 0" class="empty-state">
            暂无可邀请的联系人
          </div>
          
          <div v-else class="contacts-list">
            <div 
              v-for="contact in filteredContacts" 
              :key="contact.contactId"
              class="contact-item">
              <img :src="getUserAvatar(contact)" alt="Avatar" class="user-avatar">
              <div class="user-info">
                <div class="user-name">{{ contact.nickName }}</div>
                <div class="user-status" :class="{ 'online': contact.onlineStatus === 1 }">
                  {{ contact.onlineStatus === 1 ? '在线' : '离线' }}
                </div>
              </div>
              <button 
                class="invite-btn" 
                @click="handleInviteContact(contact)"
                :disabled="inviting">
                {{ inviting ? '邀请中...' : '邀请' }}
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <div class="modal-footer">
        <button class="btn-secondary" @click="$emit('close')">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElIcon } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { contactService } from '@/api/services.js'

const props = defineProps({
  meetingId: {
    type: String,
    required: true
  },
  meetingName: {
    type: String,
    default: '会议'
  },
  currentMembers: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'invite'])

const searchInput = ref('')
const searchResult = ref(null)
const searchMessage = ref('')
const searchError = ref(false)
const contacts = ref([])
const loading = ref(false)
const inviting = ref(false)

// 过滤掉已在会议中的联系人
const filteredContacts = computed(() => {
  return contacts.value.filter(contact => !isUserInMeeting(contact.contactId))
})

// 检查用户是否已在会议中
const isUserInMeeting = (userId) => {
  return props.currentMembers.some(member => member.userId === userId)
}

// 获取用户头像
const getUserAvatar = (user) => {
  if (user.avatar) {
    return user.avatar.startsWith('http') 
      ? user.avatar 
      : `http://localhost:6099/api/file/getAvatar/${user.userId}`
  }
  return '/meeting-icons/默认头像.svg'
}

// 加载联系人列表
const loadContacts = async () => {
  loading.value = true
  try {
    const response = await contactService.loadContactUser()
    if (response.data.code === 200) {
      contacts.value = response.data.data || []
    } else {
      ElMessage.error('加载联系人失败')
    }
  } catch (error) {
    console.error('加载联系人失败:', error)
    ElMessage.error('加载联系人失败')
  } finally {
    loading.value = false
  }
}

// 搜索用户
const handleSearch = async () => {
  if (!searchInput.value.trim()) {
    searchMessage.value = '请输入用户名或邮箱'
    searchError.value = true
    return
  }
  
  try {
    searchMessage.value = '搜索中...'
    searchError.value = false
    searchResult.value = null
    
    const input = searchInput.value.trim()
    const isEmail = input.includes('@')
    
    const response = await contactService.searchContact(
      isEmail ? null : input,
      isEmail ? input : null
    )
    
    if (response.data.code === 200) {
      searchResult.value = response.data.data
      searchMessage.value = ''
      
      if (!searchResult.value) {
        searchMessage.value = '未找到该用户'
        searchError.value = true
      }
    } else {
      searchMessage.value = response.data.info || '搜索失败'
      searchError.value = true
    }
  } catch (error) {
    console.error('搜索失败:', error)
    searchMessage.value = '搜索失败，请重试'
    searchError.value = true
  }
}

// 邀请搜索到的用户
const handleInviteUser = async (user) => {
  if (inviting.value) return
  
  inviting.value = true
  try {
    emit('invite', {
      userId: user.userId,
      nickName: user.nickName,
      email: user.email
    })
    ElMessage.success(`已向 ${user.nickName} 发送邀请`)
    searchResult.value = null
    searchInput.value = ''
  } catch (error) {
    console.error('邀请失败:', error)
    ElMessage.error('邀请失败')
  } finally {
    inviting.value = false
  }
}

// 邀请联系人
const handleInviteContact = async (contact) => {
  if (inviting.value) return
  
  inviting.value = true
  try {
    emit('invite', {
      userId: contact.contactId,
      nickName: contact.nickName,
      email: contact.email
    })
    ElMessage.success(`已向 ${contact.nickName} 发送邀请`)
  } catch (error) {
    console.error('邀请失败:', error)
    ElMessage.error('邀请失败')
  } finally {
    inviting.value = false
  }
}

onMounted(() => {
  loadContacts()
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  backdrop-filter: blur(4px);
}

.invite-modal {
  width: 500px;
  max-height: 80vh;
  background: #2a2a2a;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid #3a3a3a;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  color: #ffffff;
  font-size: 18px;
}

.modal-close {
  background: none;
  border: none;
  color: #999999;
  font-size: 28px;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.modal-close:hover {
  background: #3a3a3a;
  color: #ffffff;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.search-section {
  margin-bottom: 24px;
}

.search-input-group {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.search-input {
  flex: 1;
  padding: 10px 12px;
  background: #1a1a1a;
  border: 1px solid #3a3a3a;
  border-radius: 6px;
  color: #ffffff;
  font-size: 14px;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #999999;
}

.search-btn {
  padding: 10px 16px;
  background: #999999;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-btn:hover {
  background: #aaaaaa;
}

.search-icon {
  font-size: 18px;
  color: white;
}

.search-result-item,
.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #1a1a1a;
  border-radius: 8px;
  transition: all 0.2s;
}

.search-result-item {
  margin-bottom: 12px;
}

.contact-item:hover {
  background: #252525;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-email {
  color: #999999;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-status {
  color: #666666;
  font-size: 12px;
}

.user-status.online {
  color: #52c41a;
}

.invite-btn {
  padding: 6px 16px;
  background: #999999;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.invite-btn:hover:not(:disabled) {
  background: #aaaaaa;
}

.invite-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.status-text {
  color: #666666;
  font-size: 13px;
  white-space: nowrap;
}

.search-message {
  padding: 8px 12px;
  background: #1a1a1a;
  border-radius: 6px;
  color: #999999;
  font-size: 13px;
  text-align: center;
}

.search-message.error {
  color: #ff4d4f;
  background: rgba(255, 77, 79, 0.1);
}

.contacts-section {
  margin-top: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 0 4px;
}

.section-header h4 {
  margin: 0;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
}

.contact-count {
  color: #999999;
  font-size: 12px;
}

.contacts-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.loading-state,
.empty-state {
  padding: 40px 20px;
  text-align: center;
  color: #666666;
  font-size: 14px;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #3a3a3a;
  display: flex;
  justify-content: flex-end;
}

.btn-secondary {
  padding: 8px 20px;
  background: #3a3a3a;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: #4a4a4a;
}

/* 滚动条样式 */
.modal-body::-webkit-scrollbar,
.contacts-list::-webkit-scrollbar {
  width: 6px;
}

.modal-body::-webkit-scrollbar-track,
.contacts-list::-webkit-scrollbar-track {
  background: #1a1a1a;
  border-radius: 3px;
}

.modal-body::-webkit-scrollbar-thumb,
.contacts-list::-webkit-scrollbar-thumb {
  background: #3a3a3a;
  border-radius: 3px;
}

.modal-body::-webkit-scrollbar-thumb:hover,
.contacts-list::-webkit-scrollbar-thumb:hover {
  background: #4a4a4a;
}
</style>
