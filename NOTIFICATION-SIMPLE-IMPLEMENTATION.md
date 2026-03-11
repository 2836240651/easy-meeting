# 简化版通知系统实现方案（无需新表）

## 方案说明

如果不想添加新的数据库表，可以使用前端 localStorage 来管理通知的已读状态。这个方案更简单，但功能略有限制。

## 实现方案

### 1. 前端数据结构

```javascript
// Dashboard.vue

// 通知列表（合并多种类型）
const notificationList = ref([])

// 已读通知ID列表（存储在 localStorage）
const readNotificationIds = ref(new Set())

// 加载已读状态
const loadReadStatus = () => {
  const stored = localStorage.getItem('readNotifications')
  if (stored) {
    readNotificationIds.value = new Set(JSON.parse(stored))
  }
}

// 保存已读状态
const saveReadStatus = () => {
  localStorage.setItem('readNotifications', 
    JSON.stringify([...readNotificationIds.value]))
}

// 标记为已读
const markAsRead = (notificationId) => {
  readNotificationIds.value.add(notificationId)
  saveReadStatus()
}
```

### 2. 通知数据结构

```javascript
// 通知对象结构
{
  id: 'unique_id',           // 唯一标识
  type: 'contact_apply',     // 类型：contact_apply, contact_delete
  title: '好友申请',          // 标题
  content: '张三请求添加您为好友', // 内容
  relatedUserId: 'user123',  // 相关用户ID
  relatedUserName: '张三',    // 相关用户昵称
  avatar: 'avatar_url',      // 头像
  actionRequired: true,      // 是否需要操作
  actionStatus: 0,           // 操作状态：0=待处理，1=已同意，2=已拒绝
  createTime: timestamp,     // 创建时间
  referenceData: {}          // 原始数据引用
}
```

### 3. 构建通知列表

```javascript
// 从好友申请构建通知
const buildNotificationsFromApply = () => {
  const notifications = []
  
  // 待处理的好友申请
  contactApplyList.value.forEach(apply => {
    notifications.push({
      id: `apply_${apply.applyId}`,
      type: 'contact_apply',
      title: '好友申请',
      content: `${apply.nickName} 请求添加您为好友`,
      relatedUserId: apply.applyUserId,
      relatedUserName: apply.nickName,
      avatar: apply.avatar,
      actionRequired: true,
      actionStatus: 0, // 待处理
      createTime: new Date(apply.lastApplyTime).getTime(),
      referenceData: apply
    })
  })
  
  // 已处理的好友申请
  allApplyList.value.forEach(apply => {
    if (apply.status !== 0) { // 已处理
      notifications.push({
        id: `apply_${apply.applyId}`,
        type: 'contact_apply',
        title: '好友申请',
        content: `${apply.nickName} 的好友申请已${apply.status === 1 ? '同意' : '拒绝'}`,
        relatedUserId: apply.applyUserId,
        relatedUserName: apply.nickName,
        avatar: apply.avatar,
        actionRequired: true,
        actionStatus: apply.status, // 1=已同意，2=已拒绝
        createTime: new Date(apply.lastApplyTime).getTime(),
        referenceData: apply
      })
    }
  })
  
  return notifications
}

// 从 localStorage 获取联系人删除通知
const buildNotificationsFromDelete = () => {
  const stored = localStorage.getItem('deleteNotifications')
  if (!stored) return []
  
  try {
    return JSON.parse(stored)
  } catch (e) {
    return []
  }
}

// 合并所有通知
const buildAllNotifications = () => {
  const applyNotifications = buildNotificationsFromApply()
  const deleteNotifications = buildNotificationsFromDelete()
  
  // 合并并按时间排序
  notificationList.value = [...applyNotifications, ...deleteNotifications]
    .sort((a, b) => b.createTime - a.createTime)
}
```

### 4. 处理联系人删除通知

```javascript
// 接收到删除通知时
const handleContactDeleteMessage = (message) => {
  console.log('收到好友删除通知:', message)
  
  try {
    const senderNickName = message.sendUserNickName || '对方'
    const senderId = message.sendUserId || message.receiveUserId
    
    // 创建删除通知对象
    const deleteNotification = {
      id: `delete_${Date.now()}_${senderId}`,
      type: 'contact_delete',
      title: '联系人删除通知',
      content: `${senderNickName} 已将您从好友列表中删除`,
      relatedUserId: senderId,
      relatedUserName: senderNickName,
      avatar: null,
      actionRequired: false,
      actionStatus: null,
      createTime: Date.now()
    }
    
    // 保存到 localStorage
    const stored = localStorage.getItem('deleteNotifications')
    const deleteNotifications = stored ? JSON.parse(stored) : []
    deleteNotifications.unshift(deleteNotification)
    
    // 只保留最近30条
    if (deleteNotifications.length > 30) {
      deleteNotifications.splice(30)
    }
    
    localStorage.setItem('deleteNotifications', JSON.stringify(deleteNotifications))
    
    // 显示提示
    ElMessage.warning(`${senderNickName} 已将您从好友列表中删除`)
    
    // 重新构建通知列表
    buildAllNotifications()
    
    // 重新加载联系人列表
    loadContactList()
  } catch (error) {
    console.error('处理好友删除通知失败:', error)
  }
}
```

### 5. 计算属性

```javascript
// 待办通知（未读且需要操作的）
const pendingNotifications = computed(() => {
  return notificationList.value.filter(n => {
    // 未读
    const isUnread = !readNotificationIds.value.has(n.id)
    // 需要操作且未处理
    const needsAction = n.actionRequired && n.actionStatus === 0
    
    return isUnread && (needsAction || !n.actionRequired)
  })
})

// 所有通知（包括已读和已处理）
const allNotifications = computed(() => {
  return notificationList.value
})

// 未读数量
const unreadCount = computed(() => {
  return pendingNotifications.value.length
})
```

### 6. 通知操作

```javascript
// 查看通知（标记为已读）
const viewNotification = (notification) => {
  markAsRead(notification.id)
  
  // 如果是联系人删除通知，只需标记已读
  if (notification.type === 'contact_delete') {
    // 已经标记为已读，通知会自动移到"全部消息"
    return
  }
  
  // 如果是好友申请，可以打开详情或直接操作
  // ...
}

// 处理好友申请
const handleContactApply = async (applyUserId, status, applyUserName) => {
  try {
    const response = await dealWithContactApply(applyUserId, status)
    if (response.data.code === 200) {
      const statusText = status === 1 ? '同意' : '拒绝'
      ElMessage.success(`已${statusText} ${applyUserName} 的联系人申请`)
      
      // 重新加载申请列表
      await loadContactApplyList()
      await loadAllApplyList()
      await loadApplyCount()
      
      // 重新构建通知列表
      buildAllNotifications()
      
      // 如果同意了申请，重新加载联系人列表
      if (status === 1) {
        await loadContactList()
      }
    }
  } catch (error) {
    console.error('处理联系人申请异常:', error)
    ElMessage.error('处理申请失败，请稍后重试')
  }
}
```

### 7. 收件箱 UI

```vue
<!-- 收件箱页面 -->
<div v-if="activeNav === 'inbox'" class="content-page">
  <h2 class="page-title">收件箱</h2>
  
  <!-- 标签页 -->
  <div class="inbox-tabs">
    <div 
      class="inbox-tab" 
      :class="{ active: inboxActiveTab === 'pending' }"
      @click="inboxActiveTab = 'pending'"
    >
      <span>待办消息</span>
      <span v-if="pendingNotifications.length > 0" class="tab-count pending">
        {{ pendingNotifications.length }}
      </span>
    </div>
    <div 
      class="inbox-tab" 
      :class="{ active: inboxActiveTab === 'all' }"
      @click="inboxActiveTab = 'all'"
    >
      <span>全部消息</span>
      <span v-if="allNotifications.length > 0" class="tab-count">
        {{ allNotifications.length }}
      </span>
    </div>
  </div>
  
  <!-- 待办消息 -->
  <div v-if="inboxActiveTab === 'pending'" class="inbox-section">
    <div class="inbox-header">
      <h3>待办消息</h3>
      <span class="inbox-count" v-if="pendingNotifications.length > 0">
        {{ pendingNotifications.length }} 条未处理
      </span>
    </div>
    
    <div v-if="pendingNotifications.length > 0" class="notification-list">
      <div 
        v-for="notification in pendingNotifications" 
        :key="notification.id"
        class="notification-item"
        :class="`notification-${notification.type}`"
      >
        <!-- 好友申请 -->
        <template v-if="notification.type === 'contact_apply'">
          <div class="notification-avatar">
            <img :src="getAvatarUrl(notification.avatar)" :alt="notification.relatedUserName">
          </div>
          <div class="notification-info">
            <h4>{{ notification.title }}</h4>
            <p>{{ notification.content }}</p>
            <p class="notification-time">{{ formatApplyTime(notification.createTime) }}</p>
          </div>
          <div class="notification-actions">
            <button 
              class="notification-btn accept-btn" 
              @click="handleContactApply(notification.relatedUserId, 1, notification.relatedUserName)"
            >
              接受
            </button>
            <button 
              class="notification-btn reject-btn" 
              @click="handleContactApply(notification.relatedUserId, 2, notification.relatedUserName)"
            >
              拒绝
            </button>
          </div>
        </template>
        
        <!-- 联系人删除通知 -->
        <template v-else-if="notification.type === 'contact_delete'">
          <div class="notification-avatar">
            <img :src="getAvatarUrl(notification.avatar)" :alt="notification.relatedUserName">
          </div>
          <div class="notification-info">
            <h4>{{ notification.title }}</h4>
            <p>{{ notification.content }}</p>
            <p class="notification-time">{{ formatApplyTime(notification.createTime) }}</p>
          </div>
          <div class="notification-actions">
            <button 
              class="notification-btn view-btn" 
              @click="viewNotification(notification)"
            >
              知道了
            </button>
          </div>
        </template>
      </div>
    </div>
    
    <div v-else class="inbox-empty">
      <p>暂无待办消息</p>
    </div>
  </div>
  
  <!-- 全部消息 -->
  <div v-if="inboxActiveTab === 'all'" class="inbox-section">
    <!-- 类似结构，显示所有通知 -->
  </div>
</div>
```

## 优点

1. **无需数据库改动**：使用 localStorage 存储状态
2. **实现简单**：前端逻辑清晰
3. **快速部署**：不需要后端改动

## 缺点

1. **数据不持久**：清除浏览器数据会丢失
2. **无法跨设备同步**：不同设备看到的状态不同
3. **有数量限制**：localStorage 有大小限制

## 建议

- 如果是短期方案或小型应用，使用简化版
- 如果需要长期维护和跨设备同步，建议使用完整的数据库方案
