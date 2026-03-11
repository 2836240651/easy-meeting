# 统一收件箱前端集成指南

## 已完成的工作

✅ 后端编译成功
✅ 数据库迁移完成
✅ 前端 API 服务已扩展（services.js）

## 需要修改的前端文件

### 1. Dashboard.vue - 收件箱部分

当前收件箱只显示好友申请，需要扩展为支持多种消息类型。

#### 需要添加的响应式数据

在 `<script setup>` 部分添加：

```javascript
// 通知相关
const notificationList = ref([])  // 通知列表
const pendingNotificationList = ref([])  // 待办通知列表
const unreadNotificationCount = ref(0)  // 未读通知数量
const selectedCategory = ref('all')  // 选中的消息类别：all/contact/meeting/system

// 通知类型映射
const notificationTypeMap = {
  1: { category: 'contact', label: '好友申请', icon: 'user-add', needAction: true },
  2: { category: 'contact', label: '好友申请已同意', icon: 'user-check', needAction: false },
  3: { category: 'contact', label: '好友申请已拒绝', icon: 'user-x', needAction: false },
  4: { category: 'contact', label: '联系人删除', icon: 'user-minus', needAction: false },
  5: { category: 'meeting', label: '会议邀请', icon: 'calendar-plus', needAction: true },
  6: { category: 'meeting', label: '会议邀请已接受', icon: 'calendar-check', needAction: false },
  7: { category: 'meeting', label: '会议邀请已拒绝', icon: 'calendar-x', needAction: false },
  8: { category: 'meeting', label: '会议已取消', icon: 'calendar-minus', needAction: false },
  9: { category: 'meeting', label: '会议时间变更', icon: 'calendar-edit', needAction: false },
  10: { category: 'system', label: '系统通知', icon: 'bell', needAction: false },
  11: { category: 'system', label: '维护通知', icon: 'tool', needAction: false }
}

const categoryTitleMap = {
  contact: '联系人申请类消息',
  meeting: '会议消息',
  system: '系统消息'
}
```

#### 需要添加的方法

```javascript
// 加载通知列表（按类别）
const loadNotificationsByCategory = async (category = 'all') => {
  try {
    const response = await notificationService.loadNotificationsByCategory(category, 1, 50)
    if (response.data.code === 200) {
      notificationList.value = response.data.data.list || []
      console.log('通知列表加载成功:', notificationList.value.length, '条')
    } else {
      console.error('加载通知列表失败:', response.data.info)
    }
  } catch (error) {
    console.error('加载通知列表异常:', error)
  }
}

// 加载待办消息列表
const loadPendingNotifications = async () => {
  try {
    const response = await notificationService.loadPendingActions()
    if (response.data.code === 200) {
      pendingNotificationList.value = response.data.data || []
      console.log('待办消息加载成功:', pendingNotificationList.value.length, '条')
    } else {
      console.error('加载待办消息失败:', response.data.info)
    }
  } catch (error) {
    console.error('加载待办消息异常:', error)
  }
}

// 加载未读通知数量
const loadUnreadNotificationCount = async () => {
  try {
    const response = await notificationService.getUnreadCount()
    if (response.data.code === 200) {
      unreadNotificationCount.value = response.data.data || 0
      // 更新 applyCount 显示未读通知数量
      applyCount.value = unreadNotificationCount.value
    }
  } catch (error) {
    console.error('加载未读通知数量异常:', error)
  }
}

// 标记通知为已读
const markNotificationAsRead = async (notificationId) => {
  try {
    await notificationService.markAsRead(notificationId)
    await loadUnreadNotificationCount()
    // 重新加载当前标签页的数据
    if (inboxActiveTab.value === 'all') {
      await loadNotificationsByCategory(selectedCategory.value)
    } else {
      await loadPendingNotifications()
    }
  } catch (error) {
    console.error('标记通知为已读失败:', error)
  }
}

// 处理会议邀请
const handleMeetingInvite = async (notificationId, accepted) => {
  try {
    const response = await notificationService.handleMeetingInvite(notificationId, accepted)
    if (response.data.code === 200) {
      ElMessage.success(accepted ? '已接受会议邀请' : '已拒绝会议邀请')
      // 重新加载数据
      await loadPendingNotifications()
      await loadUnreadNotificationCount()
    } else {
      ElMessage.error(response.data.info || '操作失败')
    }
  } catch (error) {
    console.error('处理会议邀请失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

// 处理通知点击
const handleNotificationClick = async (notification) => {
  // 标记为已读
  if (notification.status === 0) {
    await markNotificationAsRead(notification.notificationId)
  }
}

// 获取通知类型信息
const getNotificationTypeInfo = (type) => {
  return notificationTypeMap[type] || { category: 'system', label: '未知', icon: 'bell', needAction: false }
}

// 格式化时间
const formatNotificationTime = (timeStr) => {
  const time = new Date(timeStr)
  const now = new Date()
  const diff = now - time
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  
  return time.toLocaleDateString()
}

// 切换类别筛选
const handleCategoryChange = async (category) => {
  selectedCategory.value = category
  await loadNotificationsByCategory(category)
}
```

#### 修改 handleNavChange 方法

在切换到收件箱时加载通知数据：

```javascript
const handleNavChange = async (nav) => {
  activeNav.value = nav
  
  if (nav === 'inbox') {
    // 加载通知数据
    await loadUnreadNotificationCount()
    if (inboxActiveTab.value === 'all') {
      await loadNotificationsByCategory(selectedCategory.value)
    } else {
      await loadPendingNotifications()
    }
  }
  // ... 其他逻辑
}
```

#### 修改 watch inboxActiveTab

```javascript
watch(inboxActiveTab, async (newTab) => {
  if (newTab === 'all') {
    await loadNotificationsByCategory(selectedCategory.value)
  } else {
    await loadPendingNotifications()
  }
})
```

#### 修改模板部分

替换现有的收件箱模板（大约在第 347-470 行）：

```vue
<!-- 收件箱页面 -->
<div v-if="activeNav === 'inbox'" class="content-page">
  <h2 class="page-title">收件箱</h2>
  
  <!-- 标签页 -->
  <div class="inbox-tabs">
    <div 
      class="inbox-tab" 
      :class="{ active: inboxActiveTab === 'all' }"
      @click="inboxActiveTab = 'all'"
    >
      <span>全部消息</span>
    </div>
    <div 
      class="inbox-tab" 
      :class="{ active: inboxActiveTab === 'pending' }"
      @click="inboxActiveTab = 'pending'"
    >
      <span>待办消息</span>
      <span v-if="pendingNotificationList.length > 0" class="tab-badge">{{ pendingNotificationList.length }}</span>
    </div>
  </div>
  
  <!-- 全部消息 -->
  <div v-if="inboxActiveTab === 'all'" class="inbox-section">
    <div class="inbox-header">
      <h3>全部消息</h3>
      
      <!-- 类别筛选器 -->
      <select v-model="selectedCategory" @change="handleCategoryChange(selectedCategory)" class="category-filter">
        <option value="all">全部消息</option>
        <option value="contact">联系人消息</option>
        <option value="meeting">会议消息</option>
        <option value="system">系统消息</option>
      </select>
    </div>
    
    <!-- 通知列表 -->
    <div v-if="notificationList.length > 0" class="notification-list">
      <div 
        v-for="notification in notificationList" 
        :key="notification.notificationId"
        class="notification-item"
        :class="{ unread: notification.status === 0 }"
        @click="handleNotificationClick(notification)"
      >
        <!-- 类别封面标题（当类别变化时显示） -->
        <div 
          v-if="shouldShowCategoryTitle(notification, notificationList)"
          class="category-title"
        >
          {{ categoryTitleMap[getNotificationTypeInfo(notification.notificationType).category] }}
        </div>
        
        <!-- 通知内容 -->
        <div class="notification-content">
          <div class="notification-icon">
            <span class="icon-placeholder">📧</span>
          </div>
          <div class="notification-body">
            <div class="notification-header">
              <span class="notification-title">{{ notification.title }}</span>
              <span class="notification-time">{{ formatNotificationTime(notification.createTime) }}</span>
            </div>
            <div class="notification-text">{{ notification.content }}</div>
            
            <!-- 操作按钮（如果需要操作且未处理） -->
            <div v-if="notification.actionRequired === 1 && notification.actionStatus === 0" class="notification-actions">
              <button 
                v-if="notification.notificationType === 1"
                class="btn-accept" 
                @click.stop="handleContactApply(notification.relatedUserId, 1, notification.relatedUserName)"
              >
                同意
              </button>
              <button 
                v-if="notification.notificationType === 1"
                class="btn-reject" 
                @click.stop="handleContactApply(notification.relatedUserId, 2, notification.relatedUserName)"
              >
                拒绝
              </button>
              <button 
                v-if="notification.notificationType === 5"
                class="btn-accept" 
                @click.stop="handleMeetingInvite(notification.notificationId, true)"
              >
                接受
              </button>
              <button 
                v-if="notification.notificationType === 5"
                class="btn-reject" 
                @click.stop="handleMeetingInvite(notification.notificationId, false)"
              >
                拒绝
              </button>
            </div>
            
            <!-- 操作状态显示 -->
            <div v-else-if="notification.actionStatus === 1" class="notification-status">
              已同意
            </div>
            <div v-else-if="notification.actionStatus === 2" class="notification-status">
              已拒绝
            </div>
          </div>
          <div v-if="notification.status === 0" class="unread-badge"></div>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-else class="inbox-empty">
      <img src="https://api.dicebear.com/9.x/icons/svg?seed=empty&backgroundColor=95a5a6" alt="Empty" class="empty-icon">
      <p>暂无消息</p>
    </div>
  </div>
  
  <!-- 待办消息 -->
  <div v-if="inboxActiveTab === 'pending'" class="inbox-section">
    <div class="inbox-header">
      <h3>待办消息</h3>
      <span class="inbox-count" v-if="pendingNotificationList.length > 0">{{ pendingNotificationList.length }} 条待处理</span>
    </div>
    
    <!-- 待办通知列表 -->
    <div v-if="pendingNotificationList.length > 0" class="notification-list">
      <!-- 待处理的好友申请 -->
      <div v-if="pendingNotificationList.filter(n => n.notificationType === 1).length > 0" class="pending-section">
        <h4 class="pending-section-title">待处理的好友申请</h4>
        <div 
          v-for="notification in pendingNotificationList.filter(n => n.notificationType === 1)" 
          :key="notification.notificationId"
          class="notification-item"
        >
          <div class="notification-content">
            <div class="notification-body">
              <div class="notification-header">
                <span class="notification-title">{{ notification.title }}</span>
                <span class="notification-time">{{ formatNotificationTime(notification.createTime) }}</span>
              </div>
              <div class="notification-text">{{ notification.content }}</div>
              <div class="notification-actions">
                <button 
                  class="btn-accept" 
                  @click="handleContactApply(notification.relatedUserId, 1, notification.relatedUserName)"
                >
                  同意
                </button>
                <button 
                  class="btn-reject" 
                  @click="handleContactApply(notification.relatedUserId, 2, notification.relatedUserName)"
                >
                  拒绝
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 待处理的会议邀请 -->
      <div v-if="pendingNotificationList.filter(n => n.notificationType === 5).length > 0" class="pending-section">
        <h4 class="pending-section-title">待处理的会议邀请</h4>
        <div 
          v-for="notification in pendingNotificationList.filter(n => n.notificationType === 5)" 
          :key="notification.notificationId"
          class="notification-item"
        >
          <div class="notification-content">
            <div class="notification-body">
              <div class="notification-header">
                <span class="notification-title">{{ notification.title }}</span>
                <span class="notification-time">{{ formatNotificationTime(notification.createTime) }}</span>
              </div>
              <div class="notification-text">{{ notification.content }}</div>
              <div class="notification-actions">
                <button 
                  class="btn-accept" 
                  @click="handleMeetingInvite(notification.notificationId, true)"
                >
                  接受
                </button>
                <button 
                  class="btn-reject" 
                  @click="handleMeetingInvite(notification.notificationId, false)"
                >
                  拒绝
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-else class="inbox-empty">
      <img src="https://api.dicebear.com/9.x/icons/svg?seed=empty&backgroundColor=95a5a6" alt="Empty" class="empty-icon">
      <p>暂无待处理的消息</p>
    </div>
  </div>
</div>
```

#### 添加辅助方法

```javascript
// 判断是否应该显示类别标题
const shouldShowCategoryTitle = (notification, list) => {
  const index = list.indexOf(notification)
  if (index === 0) return true
  
  const currentCategory = getNotificationTypeInfo(notification.notificationType).category
  const prevCategory = getNotificationTypeInfo(list[index - 1].notificationType).category
  
  return currentCategory !== prevCategory
}
```

#### 添加样式

在 `<style scoped>` 部分添加：

```css
/* 类别筛选器 */
.category-filter {
  padding: 8px 16px;
  background: #1a1a1a;
  border: 1px solid #3a3a3a;
  border-radius: 6px;
  color: #999999;
  cursor: pointer;
  font-size: 14px;
}

.category-filter:hover {
  border-color: #999999;
}

/* 类别封面标题 */
.category-title {
  padding: 12px 16px;
  background: #1a1a1a;
  color: #999999;
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px solid #3a3a3a;
}

/* 通知列表 */
.notification-list {
  display: flex;
  flex-direction: column;
}

.notification-item {
  border-bottom: 1px solid #3a3a3a;
  transition: background 0.2s;
  cursor: pointer;
}

.notification-item:hover {
  background: #333;
}

.notification-item.unread {
  background: #2a2a2a;
}

.notification-content {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  gap: 12px;
}

.notification-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #3a3a3a;
  border-radius: 50%;
  font-size: 20px;
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notification-title {
  font-weight: 600;
  color: #fff;
  font-size: 15px;
}

.notification-time {
  font-size: 12px;
  color: #888;
  flex-shrink: 0;
  margin-left: 12px;
}

.notification-text {
  color: #ccc;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 12px;
}

.notification-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.btn-accept,
.btn-reject {
  padding: 6px 16px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: opacity 0.2s;
}

.btn-accept {
  background: #999999;
  color: #fff;
}

.btn-accept:hover {
  opacity: 0.8;
}

.btn-reject {
  background: #444;
  color: #fff;
}

.btn-reject:hover {
  opacity: 0.8;
}

.notification-status {
  color: #888;
  font-size: 14px;
  margin-top: 8px;
}

.unread-badge {
  width: 8px;
  height: 8px;
  background: #409EFF;
  border-radius: 50%;
  flex-shrink: 0;
}

/* 待办消息分组 */
.pending-section {
  margin-bottom: 24px;
}

.pending-section-title {
  padding: 12px 16px;
  background: #1a1a1a;
  color: #999999;
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px solid #3a3a3a;
  margin: 0;
}

/* 标签页徽章 */
.tab-badge {
  display: inline-block;
  padding: 2px 8px;
  background: #409EFF;
  color: #fff;
  border-radius: 10px;
  font-size: 12px;
  margin-left: 8px;
}
```

## 测试步骤

1. 重启后端服务
2. 刷新前端页面
3. 登录两个不同的账号
4. 测试好友申请流程
5. 测试会议邀请流程（需要先创建预约会议）
6. 测试类别筛选功能
7. 测试待办消息功能

## 注意事项

1. 确保后端服务已重启
2. 确保数据库迁移已执行
3. WebSocket 连接正常才能收到实时通知
4. 会议邀请功能需要配合会议预约功能使用
