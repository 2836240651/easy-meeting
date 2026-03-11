<template>
  <div class="dashboard-container">
    <!-- 移动端顶部导航栏 -->
    <header class="mobile-header">
      <div class="mobile-header-content">
        <div class="mobile-avatar" @click="activeNav = 'avatar'">
          <img :src="userAvatar" alt="User Avatar" class="mobile-user-avatar">
        </div>
        <h1 class="mobile-title">EasyMeeting</h1>
        <button class="mobile-menu-toggle" @click="showMobileMenu = !showMobileMenu">
          <span class="hamburger-line"></span>
          <span class="hamburger-line"></span>
          <span class="hamburger-line"></span>
        </button>
      </div>
    </header>

    <!-- 移动端导航菜单 -->
    <nav class="mobile-nav" :class="{ 'mobile-nav-open': showMobileMenu }">
      <ul class="mobile-nav-list">
        <li :class="{ active: activeNav === 'meeting' }" @click="handleMobileNavClick('meeting')">
          <div class="mobile-nav-item">
            <img src="/meeting-icons/会议 (1).svg" alt="Meeting" class="mobile-nav-icon">
            <span class="mobile-nav-label">会议</span>
          </div>
        </li>
        <li :class="{ active: activeNav === 'contact' }" @click="handleMobileNavClick('contact')">
          <div class="mobile-nav-item">
            <div class="mobile-nav-icon-wrapper">
              <img src="/meeting-icons/联系人.svg" alt="Contact" class="mobile-nav-icon">
              <span v-if="applyCount > 0" class="mobile-nav-badge">{{ applyCount }}</span>
            </div>
            <span class="mobile-nav-label">联系人</span>
          </div>
        </li>
        <li :class="{ active: activeNav === 'inbox' }" @click="handleMobileNavClick('inbox')">
          <div class="mobile-nav-item">
            <div class="mobile-nav-icon-wrapper">
              <img src="/meeting-icons/组件-邮箱.svg" alt="Inbox" class="mobile-nav-icon">
              <span v-if="applyCount > 0" class="mobile-nav-badge">{{ applyCount }}</span>
            </div>
            <span class="mobile-nav-label">收件箱</span>
          </div>
        </li>
        <li :class="{ active: activeNav === 'settings' }" @click="handleMobileNavClick('settings')">
          <div class="mobile-nav-item">
            <img src="/meeting-icons/设置.svg" alt="Settings" class="mobile-nav-icon">
            <span class="mobile-nav-label">设置</span>
          </div>
        </li>
        <li @click="handleMobileNavClick('switch')">
          <div class="mobile-nav-item">
            <img src="/meeting-icons/切换账号.svg" alt="Switch Account" class="mobile-nav-icon">
            <span class="mobile-nav-label">切换账号</span>
          </div>
        </li>
      </ul>
    </nav>

    <!-- 桌面端左侧导航栏 -->
    <aside class="desktop-sidebar">
      <!-- 头像区域 -->
      <div class="avatar-section" @click="activeNav = 'avatar'">
        <img :src="userAvatar" alt="User Avatar" class="user-avatar">
        <!-- WebSocket 连接状态指示器 -->
        <div 
          class="connection-indicator" 
          :class="{ 
            'connected': wsService.isConnected.value, 
            'reconnecting': connectionState.isReconnecting,
            'disconnected': !wsService.isConnected.value && !connectionState.isReconnecting
          }"
          :title="getConnectionStatusText()"
          @click.stop="handleConnectionIndicatorClick"
        >
          <span class="connection-dot"></span>
        </div>
      </div>
      
      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <ul>
          <!-- 会议 -->
          <li :class="{ active: activeNav === 'meeting' }" @click="handleNavClick('meeting')">
            <div class="nav-item">
              <img src="/meeting-icons/会议 (1).svg" alt="Meeting" class="nav-icon">
              <span class="nav-label">会议</span>
            </div>
          </li>
          
          <!-- 联系人 -->
          <li :class="{ active: activeNav === 'contact' }" @click="handleNavChange('contact')">
            <div class="nav-item">
              <div class="nav-icon-wrapper">
                <img src="/meeting-icons/联系人.svg" alt="Contact" class="nav-icon">
                <span v-if="applyCount > 0" class="nav-badge">{{ applyCount }}</span>
              </div>
              <span class="nav-label">联系人</span>
            </div>
          </li>
        </ul>
      </nav>
      
      <!-- 底部区域 -->
      <div class="bottom-section">
        <!-- 收件箱 -->
        <div :class="{ active: activeNav === 'inbox' }" class="bottom-nav-item">
          <div class="nav-item" :title="'收件箱'" @click="activeNav = 'inbox'">
            <div class="nav-icon-wrapper">
              <img src="/meeting-icons/组件-邮箱.svg" alt="Inbox" class="nav-icon">
              <span v-if="applyCount > 0" class="nav-badge">{{ applyCount }}</span>
            </div>
            <span class="nav-tooltip">收件箱</span>
          </div>
        </div>
        
        <!-- 设置 -->
        <div :class="{ active: activeNav === 'settings' }" class="bottom-nav-item">
          <div class="nav-item" :title="'设置'" @click="activeNav = 'settings'">
            <img src="/meeting-icons/设置.svg" alt="Settings" class="nav-icon">
            <span class="nav-tooltip">设置</span>
          </div>
        </div>
        
        <!-- 切换账号 -->
        <div class="bottom-nav-item">
          <div class="nav-item" :title="'切换账号'" @click="handleSwitchAccount">
            <img src="/meeting-icons/切换账号.svg" alt="Switch Account" class="nav-icon">
            <span class="nav-tooltip">切换账号</span>
          </div>
        </div>
      </div>
    </aside>
    
    <!-- 主内容区域 -->
    <main class="main-content">
      <!-- 会议页面 -->
      <div v-if="activeNav === 'meeting'" class="content-page">
        <h2 class="page-title">会议</h2>
        
        <!-- 会议功能卡片 -->
        <div class="meeting-features">
          <!-- 加入会议 -->
          <div class="feature-card">
            <div class="feature-icon join-meeting">
              <img src="/meeting-icons/加入会议.svg" alt="Join Meeting" class="feature-icon-img">
            </div>
            <h3>加入会议</h3>
            <p>输入会议号加入已预约的会议</p>
            <button class="feature-button" @click="showJoinMeetingModal = true">加入会议</button>
          </div>
          
          <!-- 快速会议 -->
          <div class="feature-card">
            <div class="feature-icon quick-meeting">
              <img src="/meeting-icons/快速会议 (1).svg" alt="Quick Meeting" class="feature-icon-img">
            </div>
            <h3>快速会议</h3>
            <p>立即创建并开始一个新会议</p>
            <button class="feature-button" @click="showQuickMeetingModal = true">开始会议</button>
          </div>
          
          <!-- 预约会议 -->
          <div class="feature-card">
            <div class="feature-icon schedule-meeting">
              <img src="/meeting-icons/预约会议 (1).svg" alt="Schedule Meeting" class="feature-icon-img">
            </div>
            <h3>预约会议</h3>
            <p>预约一个未来的会议并发送邀请</p>
            <button class="feature-button" @click="showScheduleMeetingModal = true">预约会议</button>
          </div>
        </div>

        <!-- 当前会议 -->
        <div class="current-meeting-section" v-if="currentMeeting">
          <h3>当前会议</h3>
          <div class="current-meeting-card">
            <div class="meeting-status-indicator">
              <span class="status-dot active"></span>
              <span class="status-text">进行中</span>
            </div>
            <div class="meeting-details">
              <h4>{{ currentMeeting.meetingName || '未命名会议' }}</h4>
              <div class="meeting-meta">
                <p><strong>会议号:</strong> {{ currentMeeting.meetingNo || '未知' }}</p>
                <p><strong>会议ID:</strong> {{ currentMeeting.meetingId || '未知' }}</p>
                <p><strong>创建时间:</strong> {{ formatDateTime(currentMeeting.createTime) }}</p>
                <p><strong>我的角色:</strong> 
                  <span class="role-badge" :class="{ 'role-host': isCurrentMeetingHost }">
                    {{ isCurrentMeetingHost ? '主持人' : '参与者' }}
                  </span>
                </p>
              </div>
            </div>
            <div class="meeting-actions">
              <button class="action-button join-current" @click="rejoinCurrentMeeting()">
                <span class="button-icon">🚪</span>
                重新进入
              </button>
              <button v-if="isCurrentMeetingHost" class="action-button finish-current" @click="finishCurrentMeeting()">
                <span class="button-icon">🛑</span>
                结束会议
              </button>
              <button v-else class="action-button leave-current" @click="leaveCurrentMeeting()">
                <span class="button-icon">👋</span>
                离开会议
              </button>
            </div>
          </div>
        </div>

        <!-- 全部会议 -->
        <div class="all-meetings-section">
          <div class="all-meetings-header" @click="handleShowAllMeetings">
            <h3>全部会议</h3>
            <span class="view-all-arrow">→</span>
          </div>
          <div class="meetings-preview">
            <div v-if="meetingHistory.upcoming.length === 0 && meetingHistory.ended.length === 0" class="empty-meetings">
              <p>暂无会议记录</p>
            </div>
            <!-- 显示最近的几个会议作为预览 -->
            <div v-for="meeting in getRecentMeetings()" :key="meeting.meetingId" class="meeting-preview-item">
              <div class="meeting-preview-info">
                <h4>{{ meeting.meetingName || '未命名会议' }}</h4>
                <p>{{ meeting.status === 0 ? '进行中' : '已结束' }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 预约会议列表 -->
        <div class="reservation-section">
          <div class="reservation-header" @click="showReservationList = !showReservationList">
            <h3>我的预约会议</h3>
            <span class="toggle-arrow" :class="{ 'arrow-down': showReservationList }">▶</span>
          </div>
          <div v-if="showReservationList" class="reservation-content">
            <ReservationList ref="reservationListRef" />
          </div>
        </div>
      </div>
      
      <!-- 个人信息页面 -->
      <div v-if="activeNav === 'avatar'" class="content-page">
        <h2 class="page-title">个人信息</h2>
        <div class="user-profile">
          <div class="profile-avatar-container">
            <img :src="userAvatar" alt="User Avatar" class="profile-avatar">
          </div>
          <div class="profile-info">
            <div class="info-item">
              <label>昵称：</label>
              <span>{{ userInfo?.nickName || '未设置' }}</span>
            </div>
            <div class="info-item">
              <label>邮箱：</label>
              <span>{{ userInfo?.email || '未设置' }}</span>
            </div>
            <div class="info-item">
              <label>用户ID：</label>
              <span>{{ userInfo?.userId || '未设置' }}</span>
            </div>
            <div class="info-item">
              <label>个人会议号：</label>
              <span>{{ userInfo?.meetingNo || '未设置' }}</span>
            </div>
            <div class="info-item">
              <label>性别：</label>
              <span>{{ getUserGender() }}</span>
            </div>
            <div class="profile-actions">
              <button class="edit-profile-button" @click="openEditProfileModal">设置个人信息</button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 联系人页面 -->
      <div v-if="activeNav === 'contact'" class="content-page">
        <h2 class="page-title">联系人</h2>
        
        <!-- 联系人操作区 -->
        <div class="contact-actions">
          <button class="contact-action-btn" @click="showSearchContactModal = true">
            <img src="/meeting-icons/联系人.svg" alt="Add Contact" class="action-icon">
            <span>添加联系人</span>
          </button>
        </div>
        
        <!-- 联系人标签页 -->
        <div class="contact-tabs">
          <div 
            class="contact-tab" 
            :class="{ active: contactActiveTab === 'friends' }"
            @click="contactActiveTab = 'friends'"
          >
            <span>好友列表</span>
            <span v-if="contactList.length > 0" class="tab-count">({{ contactList.length }})</span>
          </div>
          <div 
            class="contact-tab" 
            :class="{ active: contactActiveTab === 'blacklist' }"
            @click="handleBlacklistTabClick"
          >
            <span>拉黑列表</span>
            <span v-if="blackList.length > 0" class="tab-count">({{ blackList.length }})</span>
          </div>
        </div>
        
        <!-- 好友列表标签页 -->
        <div v-if="contactActiveTab === 'friends'">
          <!-- 联系人申请通知 -->
          <div v-if="applyCount > 0" class="contact-notifications">
            <div class="notification-header">
              <h3>联系人申请 ({{ applyCount }})</h3>
              <button class="view-all-btn" @click="showContactApplyModalHandler">查看全部</button>
            </div>
          </div>
          
          <!-- 已发送的申请 -->
          <div v-if="myApplyList.length > 0" class="my-apply-section">
            <div class="section-header">
              <h3>已发送的申请 ({{ myApplyList.length }})</h3>
            </div>
            <div class="my-apply-list">
              <div v-for="apply in myApplyList" :key="apply.applyId" class="my-apply-item">
                <div class="apply-avatar">
                  <img :src="getAvatarUrl(apply.avatar)" :alt="apply.nickName">
                </div>
                <div class="apply-info">
                  <h4>{{ apply.nickName || apply.receiveUserId }}</h4>
                  <p class="apply-time">{{ formatApplyTime(apply.lastApplyTime) }}</p>
                </div>
                <div class="apply-status">
                  <span class="status-badge waiting-badge">等待同意</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 联系人列表 -->
          <div class="contact-list">
            <div v-if="contactList.length === 0" class="empty-contacts">
              <p>暂无联系人</p>
            </div>
            <div v-for="contact in contactList" :key="contact.contactId" class="contact-item">
              <div class="contact-avatar">
                <img :src="getContactAvatar(contact)" alt="Contact Avatar" class="contact-avatar-img">
                <div class="contact-status" :class="{ online: isContactOnline(contact) }"></div>
              </div>
              <div class="contact-info">
                <h4>{{ contact.nickName || contact.contactId }}</h4>
                <p class="contact-status-text">{{ isContactOnline(contact) ? '在线' : '离线' }}</p>
                <p class="contact-last-time">{{ getLastActiveTime(contact) }}</p>
              </div>
              <div class="contact-actions">
                <button class="contact-action-btn delete-btn" @click="handleDeleteContact(contact)" title="删除好友">
                  删除
                </button>
                <button class="contact-action-btn blacklist-btn" @click="handleBlacklistContact(contact)" title="拉黑">
                  拉黑
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 拉黑列表标签页 -->
        <div v-if="contactActiveTab === 'blacklist'">
          <div class="blacklist-section">
            <div v-if="blackList.length === 0" class="empty-blacklist">
              <p>暂无拉黑用户</p>
            </div>
            <div v-for="contact in blackList" :key="contact.contactId" class="blacklist-item">
              <div class="contact-avatar">
                <img :src="getContactAvatar(contact)" alt="Contact Avatar" class="contact-avatar-img">
              </div>
              <div class="contact-info">
                <h4>{{ contact.nickName || contact.contactId }}</h4>
                <p class="blacklist-time">拉黑时间：{{ formatBlacklistTime(contact.lastUpdateTime) }}</p>
              </div>
              <div class="contact-actions">
                <button class="contact-action-btn unblock-btn" @click="handleUnblackContact(contact)" title="取消拉黑">
                  取消拉黑
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      
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
                  
                  <!-- 即时会议邀请特殊显示 -->
                  <div v-if="notification.notificationType === 10" class="instant-meeting-invite">
                    <div class="notification-text">{{ notification.relatedUserName }} 邀请你加入会议</div>
                    <div class="meeting-details">
                      <div class="meeting-detail-item">
                        <span class="detail-label">会议名称：</span>
                        <span class="detail-value">{{ parseMeetingInvite(notification.content).meetingName }}</span>
                      </div>
                      <div class="meeting-detail-item">
                        <span class="detail-label">会议号：</span>
                        <span class="detail-value">{{ parseMeetingInvite(notification.content).meetingNo }}</span>
                      </div>
                      <div v-if="parseMeetingInvite(notification.content).password" class="meeting-detail-item">
                        <span class="detail-label">会议密码：</span>
                        <span class="detail-value password">{{ parseMeetingInvite(notification.content).password }}</span>
                      </div>
                    </div>
                    <button
                      class="btn-join-meeting"
                      :class="{ disabled: isInstantInviteEnded(notification) }"
                      :disabled="isInstantInviteEnded(notification)"
                      @click.stop="handleJoinInstantMeeting(notification)"
                    >
                      {{ isInstantInviteEnded(notification) ? '会议已经结束' : '立即加入' }}
                    </button>
                  </div>
                  
                  <!-- 普通通知显示 -->
                  <div v-else class="notification-text">{{ notification.content }}</div>
                  
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
            
            <!-- 待处理的即时会议邀请 -->
            <div v-if="pendingNotificationList.filter(n => n.notificationType === 10).length > 0" class="pending-section">
              <h4 class="pending-section-title">即时会议邀请</h4>
              <div 
                v-for="notification in pendingNotificationList.filter(n => n.notificationType === 10)" 
                :key="notification.notificationId"
                class="notification-item"
              >
                <div class="notification-content">
                  <div class="notification-body">
                    <div class="notification-header">
                      <span class="notification-title">{{ notification.title }}</span>
                      <span class="notification-time">{{ formatNotificationTime(notification.createTime) }}</span>
                    </div>
                    <div class="notification-text">
                      {{ parseMeetingInvite(notification.content).inviterName }} 邀请您加入会议「{{ parseMeetingInvite(notification.content).meetingName }}」
                    </div>
                    <div class="notification-actions">
                      <button
                        class="btn-accept"
                        :class="{ disabled: isInstantInviteEnded(notification) }"
                        :disabled="isInstantInviteEnded(notification)"
                        @click="handleJoinInstantMeeting(notification)"
                      >
                        {{ isInstantInviteEnded(notification) ? '会议已经结束' : '立即加入' }}
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
      
      <!-- 设置页面 -->
      <div v-if="activeNav === 'settings'" class="content-page">
        <h2 class="page-title">设置</h2>
        <SettingsPanel 
          @change-password="handleChangePassword"
          @logout="handleLogout"
        />
      </div>
    </main>

    <!-- 加入会议模态框 -->
    <JoinMeetingModal
      v-model:visible="showJoinMeetingModal"
      @join="handleJoinMeeting"
    />

    <!-- 快速会议模态框 -->
    <QuickMeetingModal
      v-model:visible="showQuickMeetingModal"
      @create="handleQuickMeeting"
    />

    <!-- 预约会议模态框 -->
    <ScheduleMeetingModal
      v-model:visible="showScheduleMeetingModal"
      @created="handleMeetingCreated"
    />

    <div v-if="showIncomingMeetingInviteModal" class="modal-overlay">
      <div class="modal-content incoming-meeting-invite-modal">
        <div class="modal-header">
          <h3>会议邀请</h3>
          <button class="modal-close" @click="closeIncomingMeetingInviteModal">&times;</button>
        </div>
        <div class="modal-body">
          <p class="invite-modal-text">
            {{ currentIncomingInviteInfo.inviterName || '会议主持人' }} 邀请你加入会议
          </p>
          <div class="meeting-details compact">
            <div class="meeting-detail-item">
              <span class="detail-label">会议名称：</span>
              <span class="detail-value">{{ currentIncomingInviteInfo.meetingName || '会议' }}</span>
            </div>
            <div class="meeting-detail-item">
              <span class="detail-label">会议号：</span>
              <span class="detail-value">{{ currentIncomingInviteInfo.meetingNo || '待同步' }}</span>
            </div>
            <div v-if="currentIncomingInviteInfo.password" class="meeting-detail-item">
              <span class="detail-label">会议密码：</span>
              <span class="detail-value password">{{ currentIncomingInviteInfo.password }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeIncomingMeetingInviteModal">稍后处理</button>
          <button class="btn-primary" @click="confirmIncomingMeetingInvite">立即加入</button>
        </div>
      </div>
    </div>

    <!-- 编辑个人信息模态框 -->
    <div v-if="showEditProfileModal" class="modal-overlay">
      <div class="modal-content" ref="editProfileModalRef">
        <div class="modal-header" @mousedown="startDrag($event, 'editProfileModalRef')">
          <h3>编辑个人信息</h3>
          <button class="modal-close" @click="showEditProfileModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>昵称</label>
            <input type="text" v-model="editProfileForm.nickName" placeholder="请输入昵称" class="form-input">
          </div>
          <div class="form-group">
            <label>性别</label>
            <div class="radio-group">
              <label class="radio-item">
                <input type="radio" v-model="editProfileForm.sex" :value="1">
                <span>男</span>
              </label>
              <label class="radio-item">
                <input type="radio" v-model="editProfileForm.sex" :value="0">
                <span>女</span>
              </label>
            </div>
          </div>
          <div class="form-group">
            <label>头像</label>
            <div class="avatar-upload">
              <div class="avatar-preview-container" @click="showAvatarUploadModal = true">
                <img :src="editProfileForm.avatar || userAvatar" alt="Avatar Preview" class="avatar-preview clickable">
                <div class="avatar-overlay">
                  <span class="avatar-overlay-text">点击更换头像</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="showEditProfileModal = false">取消</button>
          <button class="btn-primary" @click="handleSaveProfile">保存</button>
        </div>
      </div>
    </div>

    <!-- 搜索联系人模态框 -->
    <div v-if="showSearchContactModal" class="modal-overlay">
      <div class="modal-content" ref="searchContactModalRef">
        <div class="modal-header" @mousedown="startDrag($event, 'searchContactModalRef')">
          <h3>添加联系人</h3>
          <button class="modal-close" @click="showSearchContactModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>搜索用户</label>
            <input 
              v-model="searchContactInput" 
              type="text" 
              placeholder="请输入用户ID或邮箱" 
              class="form-input"
              @keyup.enter="handleSearchContact"
            >
          </div>
          <button class="btn-primary search-btn" @click="handleSearchContact">搜索</button>
          
          <!-- 搜索结果 -->
          <div v-if="searchContactResult" class="search-result">
            <div class="result-item">
              <img :src="getSearchContactAvatar(searchContactResult)" alt="Avatar" class="result-avatar">
              <div class="result-info">
                <h4>{{ searchContactResult.nickName }}</h4>
                <p>{{ searchContactResult.email }}</p>
                <p class="result-status">{{ getSearchResultStatus(searchContactResult) }}</p>
              </div>
              <button 
                v-if="searchContactResult.status === null"
                class="btn-primary" 
                @click="handleApplyContact(searchContactResult.userId)"
              >
                添加
              </button>
              <span v-else-if="searchContactResult.status === -1" class="status-text">这是你自己</span>
              <span v-else-if="searchContactResult.status === 0" class="status-text">已发送申请</span>
              <span v-else-if="searchContactResult.status === 1" class="status-text">已是好友</span>
              <span v-else-if="searchContactResult.status === 3" class="status-text">已拉黑</span>
            </div>
          </div>
          
          <!-- 搜索提示 -->
          <div v-if="searchContactMessage" class="search-message" :class="{ 'error': searchContactError }">
            {{ searchContactMessage }}
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeSearchContactModal">关闭</button>
        </div>
      </div>
    </div>

    <!-- 联系人申请模态框 -->
    <div v-if="showContactApplyModal" class="modal-overlay">
      <div class="modal-content" ref="contactApplyModalRef">
        <div class="modal-header" @mousedown="startDrag($event, 'contactApplyModalRef')">
          <h3>联系人申请</h3>
          <button class="modal-close" @click="showContactApplyModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="apply-list">
            <div v-if="contactApplyList.length === 0" class="no-apply">
              <p>暂无联系人申请</p>
            </div>
            <div v-for="apply in contactApplyList" :key="apply.applyUserId" class="apply-item">
              <div class="apply-info">
                <img :src="apply.avatar || (apply.sex === 1 ? '/meeting-icons/男头像.svg' : '/meeting-icons/女头像.svg')"
                     alt="Avatar" class="apply-avatar">
                <div class="apply-details">
                  <h4>{{ apply.nickName || apply.applyUserNickName || '未知用户' }}</h4>
                  <p>申请添加您为联系人</p>
                  <span class="apply-time">{{ formatApplyTime(apply.lastApplyTime) }}</span>
                </div>
              </div>
              <div class="apply-actions">
                <button class="btn-success" @click="acceptContactApply(apply.applyUserId, apply.nickName || apply.applyUserNickName)">
                  同意
                </button>
                <button class="btn-danger" @click="rejectContactApply(apply.applyUserId, apply.nickName || apply.applyUserNickName)">
                  拒绝
                </button>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="showContactApplyModal = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- 全部会议模态框 -->
    <div v-if="showAllMeetingsModal" class="modal-overlay">
      <div class="modal-content all-meetings-modal" ref="allMeetingsModalRef">
        <div class="modal-header" @mousedown="startDrag($event, 'allMeetingsModalRef')">
          <h3>全部会议</h3>
          <button class="modal-close" @click="showAllMeetingsModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <!-- 会议类型切换标签 -->
          <div class="meeting-tabs">
            <button 
              class="tab-button" 
              :class="{ active: activeMeetingTab === 'history' }" 
              @click="handleMeetingTabClick('history')"
            >
              历史会议
            </button>
            <button 
              class="tab-button" 
              :class="{ active: activeMeetingTab === 'upcoming' }" 
              @click="handleMeetingTabClick('upcoming')"
            >
              进行中的会议
            </button>
          </div>

          <!-- 历史会议列表 -->
          <div v-if="activeMeetingTab === 'history'" class="meeting-list">
            <div v-if="meetingHistory.ended.length === 0" class="empty-meetings">
              <p>暂无历史会议</p>
            </div>
            <div v-for="meeting in meetingHistory.ended" :key="meeting.meetingId" class="meeting-item history">
              <div class="meeting-info">
                <div class="meeting-header">
                  <h4>{{ meeting.meetingName || '未命名会议' }}</h4>
                  <span class="meeting-status finished">已结束</span>
                </div>
                <div class="meeting-details">
                  <p><strong>会议号:</strong> {{ meeting.meetingNo || '未知' }}</p>
                  <p><strong>会议ID:</strong> {{ meeting.meetingId || '未知' }}</p>
                  <p><strong>创建时间:</strong> {{ formatDateTime(meeting.createTime) }}</p>
                  <p><strong>结束时间:</strong> {{ formatDateTime(meeting.endTime) }}</p>
                </div>
              </div>
              <div class="meeting-actions">
                <button class="action-button details" @click="viewMeetingDetails(meeting)">查看详情</button>
              </div>
            </div>
          </div>

          <!-- 进行中的会议列表 -->
          <div v-if="activeMeetingTab === 'upcoming'" class="meeting-list">
            <div v-if="meetingHistory.upcoming.length === 0" class="empty-meetings">
              <p>暂无进行中的会议</p>
            </div>
            <div v-for="meeting in meetingHistory.upcoming" :key="meeting.meetingId" class="meeting-item upcoming">
              <div class="meeting-info">
                <div class="meeting-header">
                  <h4>{{ meeting.meetingName || '未命名会议' }}</h4>
                  <span class="meeting-status running">进行中</span>
                </div>
                <div class="meeting-details">
                  <p><strong>会议号:</strong> {{ meeting.meetingNo || '未知' }}</p>
                  <p><strong>会议ID:</strong> {{ meeting.meetingId || '未知' }}</p>
                  <p><strong>创建时间:</strong> {{ formatDateTime(meeting.createTime) }}</p>
                  <p><strong>开始时间:</strong> {{ formatDateTime(meeting.startTime) }}</p>
                  <p><strong>创建者:</strong> {{ meeting.createUserId === userInfo?.userId ? '我' : '其他用户' }}</p>
                </div>
              </div>
              <div class="meeting-actions">
                <button class="action-button join" @click="joinMyMeeting(meeting)">进入会议</button>
                <!-- 只有会议创建者才能结束会议 -->
                <button 
                  v-if="meeting.createUserId === userInfo?.userId" 
                  class="action-button finish" 
                  @click="finishMeeting(meeting)"
                >
                  结束会议
                </button>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="showAllMeetingsModal = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- 头像上传模态框 -->
    <div v-if="showAvatarUploadModal" class="modal-overlay">
      <div class="modal-content avatar-upload-modal" ref="avatarUploadModalRef">
        <div class="modal-header" @mousedown="startDrag($event, 'avatarUploadModalRef')">
          <h3>更换头像</h3>
          <button class="modal-close" @click="showAvatarUploadModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <!-- 当前头像预览 -->
          <div class="current-avatar-preview">
            <img :src="editProfileForm.avatar || userAvatar" alt="当前头像" class="current-avatar-img">
            <p class="current-avatar-label">当前头像</p>
          </div>

          <!-- 上传方式选择 -->
          <div class="upload-method-selection">
            <h4>选择上传方式</h4>
            <div class="upload-methods">
              <div class="upload-method" @click="selectUploadMethod('file')">
                <div class="method-icon">📁</div>
                <div class="method-info">
                  <h5>本地上传</h5>
                  <p>从本地选择图片文件上传</p>
                </div>
              </div>
              <div class="upload-method" @click="selectUploadMethod('url')">
                <div class="method-icon">🔗</div>
                <div class="method-info">
                  <h5>URL上传</h5>
                  <p>输入图片链接地址</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 文件上传区域 -->
          <div v-if="selectedUploadMethod === 'file'" class="file-upload-area">
            <div class="file-drop-zone" @click="triggerFileInput" @dragover.prevent @drop.prevent="handleFileDrop">
              <input type="file" ref="fileInput" accept="image/*" @change="handleAvatarUpload" style="display: none;">
              <div class="drop-zone-content">
                <div class="drop-zone-icon">📷</div>
                <p class="drop-zone-text">点击选择文件或拖拽文件到此处</p>
                <p class="drop-zone-hint">支持 JPG、PNG、GIF、WebP 格式，最大 2MB</p>
              </div>
            </div>
            <div v-if="uploadProgress > 0" class="upload-progress">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
              </div>
              <p class="progress-text">上传中... {{ uploadProgress }}%</p>
            </div>
          </div>

          <!-- URL上传区域 -->
          <div v-if="selectedUploadMethod === 'url'" class="url-upload-area">
            <div class="form-group">
              <label>图片URL地址</label>
              <input 
                type="url" 
                v-model="avatarUrlInput" 
                placeholder="请输入图片链接地址，如：https://example.com/avatar.jpg" 
                class="form-input"
                @input="handleAvatarUrlPreview"
              >
            </div>
            <div v-if="avatarUrlInput" class="url-preview">
              <img 
                :src="avatarUrlInput" 
                alt="URL预览" 
                class="url-preview-img" 
                @error="handleUrlPreviewError" 
                @load="handleUrlPreviewLoad"
                crossorigin="anonymous"
                referrerpolicy="no-referrer"
              >
              <p class="url-preview-status" :class="{ 'error': urlPreviewError, 'success': urlPreviewLoaded }">
                {{ urlPreviewError ? '图片加载失败，请检查URL是否正确。可能原因：1) URL无效 2) 服务器防盗链 3) 跨域限制' : urlPreviewLoaded ? '图片预览成功' : '正在加载预览...' }}
              </p>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="showAvatarUploadModal = false">取消</button>
          <button 
            class="btn-primary" 
            @click="confirmAvatarUpload" 
            :disabled="!canConfirmUpload"
          >
            确认更换
          </button>
        </div>
      </div>
    </div>

    <!-- 会议提醒组件 -->
    <MeetingReminder />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { wsService, MessageType } from '@/api/websocket.js'
import { getUserInfo, updateUserInfo, getContactList, getContactApplyCount, getContactApplyList, dealWithContactApply, getMeetingHistory, meetingService, searchContact, applyContact, contactService, notificationService, changePassword } from '@/api/services.js'
import { settingsManager } from '@/utils/settings-manager.js'
import ScheduleMeetingModal from '@/components/ScheduleMeetingModal.vue'
import JoinMeetingModal from '@/components/JoinMeetingModal.vue'
import QuickMeetingModal from '@/components/QuickMeetingModal.vue'
import ReservationList from '@/components/ReservationList.vue'
import MeetingReminder from '@/components/MeetingReminder.vue'
import SettingsPanel from '@/components/SettingsPanel.vue'

const router = useRouter()
const activeNav = ref('meeting')
const userInfo = ref(null)
const showMobileMenu = ref(false)

// 会议相关状态
const showJoinMeetingModal = ref(false)
const showQuickMeetingModal = ref(false)
const showScheduleMeetingModal = ref(false)
const showIncomingMeetingInviteModal = ref(false)
const incomingMeetingInviteNotification = ref(null)
const showEditProfileModal = ref(false)
const showAllMeetingsModal = ref(false)
const activeMeetingTab = ref('history') // 'history' 或 'upcoming'
const showReservationList = ref(false) // 控制预约会议列表显示

// 预约会议列表引用
const reservationListRef = ref(null)

// 头像上传相关状态
const showAvatarUploadModal = ref(false)
const selectedUploadMethod = ref('') // 'file' 或 'url'
const avatarUrlInput = ref('')
const urlPreviewError = ref(false)
const urlPreviewLoaded = ref(false)
const uploadProgress = ref(0)

// 编辑个人信息表单数据
const editProfileForm = ref({
  nickName: '',
  sex: null,
  avatar: ''
})

// 会议历史数据
const meetingHistory = ref({
  upcoming: [],
  ended: []
})

// 当前会议数据
const currentMeeting = ref(null)
const isCurrentMeetingHost = ref(false)

// 联系人相关状态
const contactList = ref([])
const blackList = ref([])  // 拉黑列表
const contactActiveTab = ref('friends')  // 联系人当前标签页：'friends' 或 'blacklist'
const applyCount = ref(0)
const contactApplyList = ref([])  // 待处理的联系人申请列表
const myApplyList = ref([])  // 当前用户发送的待处理申请列表
const allApplyList = ref([])  // 所有联系人申请列表（包括已处理）
const inboxActiveTab = ref('all')  // 收件箱当前标签页：'all' 或 'pending'，默认显示全部消息
const notificationList = ref([])  // 通知列表
const pendingNotificationList = ref([])  // 待办通知列表
const unreadNotificationCount = ref(0)  // 未读通知数量
const selectedCategory = ref('all')  // 选中的消息类别：all/contact/meeting/system
const instantInviteMeetingStatusMap = ref({})

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
  10: { category: 'meeting', label: '即时会议邀请', icon: 'video', needAction: false },
  11: { category: 'system', label: '系统通知', icon: 'bell', needAction: false },
  12: { category: 'system', label: '维护通知', icon: 'tool', needAction: false }
}

const categoryTitleMap = {
  contact: '联系人申请类消息',
  meeting: '会议消息',
  system: '系统消息'
}

const showSearchContactModal = ref(false)
const showContactApplyModal = ref(false)
const searchContactInput = ref('')
const searchContactResult = ref(null)
const searchContactMessage = ref('')
const searchContactError = ref(false)

// 联系人列表轮询相关
const contactPollingInterval = ref(null)
const CONTACT_POLLING_INTERVAL = 20000 // 20秒轻量刷新一次联系人状态

// 会议状态轮询相关
const meetingPollingInterval = ref(null)

// WebSocket 连接状态
const connectionState = ref({
  isConnected: false,
  isReconnecting: false,
  reconnectAttempts: 0,
  status: 'disconnected', // 'connected', 'disconnected', 'reconnecting'
  connectedAt: null // 连接建立的时间戳
})

// 连接状态更新定时器
const connectionStateInterval = ref(null)

// 头像版本号，用于强制刷新头像缓存
const avatarVersion = ref(Date.now())

const currentIncomingInviteInfo = computed(() => {
  if (!incomingMeetingInviteNotification.value) {
    return {}
  }
  return parseMeetingInvite(incomingMeetingInviteNotification.value.content)
})

const isInstantInviteEnded = (notification) => {
  const meetingId = parseMeetingInvite(notification.content).meetingId
  return instantInviteMeetingStatusMap.value[meetingId] === true
}

// 计算属性
const userAvatar = computed(() => {
  if (userInfo.value?.avatar) {
    return userInfo.value.avatar
  }
  return userInfo.value?.sex === 1 ? '/meeting-icons/男头像.svg' : '/meeting-icons/女头像.svg'
})

// 移动端导航处理
const handleMobileNavClick = async (nav) => {
  if (nav === 'switch') {
    handleSwitchAccount()
  } else if (nav === 'contact') {
    await handleNavChange('contact')
  } else if (nav === 'meeting') {
    // 使用统一的导航点击处理函数
    await handleNavClick('meeting')
  } else {
    activeNav.value = nav
    // 页面切换后确保焦点正确
    await ensureFocus()
  }
  showMobileMenu.value = false
}

// 导航变化处理
const handleNavChange = async (nav) => {
  activeNav.value = nav
  
  if (nav === 'contact') {
    await loadContactList()
    await loadApplyCount()
    await loadMyApplyList()  // 加载我的申请列表
  }
  
  // 如果切换到收件箱，加载通知数据
  if (nav === 'inbox') {
    await loadUnreadNotificationCount()
    if (inboxActiveTab.value === 'all') {
      await loadNotificationsByCategory(selectedCategory.value)
    } else {
      await loadPendingNotifications()
    }
  }
  
  // 页面切换后确保焦点正确
  await ensureFocus()
}

// 用户相关函数
const getUserGender = () => {
  if (!userInfo.value?.sex) return '未设置'
  return userInfo.value.sex === 1 ? '男' : '女'
}

const getDefaultAvatar = (sex) => {
  return sex === 1 ? '/meeting-icons/男头像.svg' : '/meeting-icons/女头像.svg'
}

// 焦点管理函数
const ensureFocus = async () => {
  await nextTick()
  
  // 在Electron环境中，确保窗口和页面都获得焦点
  if (window.electron && window.electron.focusWindow) {
    window.electron.focusWindow()
  }
  
  // 尝试聚焦到第一个可聚焦的元素
  setTimeout(() => {
    const focusableElements = document.querySelectorAll(
      'input:not([disabled]), textarea:not([disabled]), select:not([disabled]), button:not([disabled]), [tabindex]:not([tabindex="-1"])'
    )
    
    if (focusableElements.length > 0) {
      focusableElements[0].focus()
      console.log('聚焦到第一个可聚焦元素:', focusableElements[0])
    } else {
      // 如果没有可聚焦的元素，聚焦到body
      document.body.focus()
      console.log('聚焦到body元素')
    }
  }, 100)
}

// 判断联系人是否在线
const isContactOnline = (contact) => {
  if (typeof contact?.onlineStatus === 'number') {
    return contact.onlineStatus === 1
  }
  if (!contact?.lastLoginTime) return false
  if (!contact?.lastOffTime) return true
  return contact.lastLoginTime > contact.lastOffTime
}

// 获取联系人头像
const getContactAvatar = (contact) => {
  // 如果有avatar字段，直接使用（这是MinIO的完整URL）
  if (contact.avatar) {
    return contact.avatar
  }
  // 否则使用默认头像
  return getDefaultAvatar(contact.sex)
}

// 获取头像URL（用于申请列表）
const getAvatarUrl = (avatar, sex = 0) => {
  if (avatar) {
    return avatar
  }
  return getDefaultAvatar(sex)
}

// 获取搜索结果用户头像
const getSearchContactAvatar = (user) => {
  // 如果有avatar字段，直接使用
  if (user.avatar) {
    return user.avatar
  }
  // 否则使用默认头像
  return getDefaultAvatar(user.sex)
}

// 获取最后活跃时间
const getLastActiveTime = (contact) => {
  const isOnline = isContactOnline(contact)
  if (isOnline) {
    return contact.lastLoginTime ? `最后登录: ${formatTime(contact.lastLoginTime)}` : ''
  } else {
    return contact.lastOffTime ? `最后离线: ${formatTime(contact.lastOffTime)}` : ''
  }
}

// 格式化时间戳
const formatTime = (timestamp) => {
  if (!timestamp) return '未知'
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  
  // 小于1分钟
  if (diff < 60000) {
    return '刚刚'
  }
  // 小于1小时
  else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }
  // 小于1天
  else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  // 大于1天
  else {
    return date.toLocaleDateString('zh-CN')
  }
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '未知'
  try {
    const date = new Date(dateTime)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (error) {
    console.error('日期格式化失败:', error)
    return '格式错误'
  }
}

// 格式化申请时间（相对时间）
const formatApplyTime = (timestamp) => {
  if (!timestamp) return '未知时间'
  try {
    const now = Date.now()
    const applyTime = typeof timestamp === 'string' ? new Date(timestamp).getTime() : timestamp
    const diff = now - applyTime
    
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)
    
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days < 7) return `${days}天前`
    
    // 超过7天显示具体日期
    const date = new Date(applyTime)
    return date.toLocaleDateString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (error) {
    console.error('时间格式化失败:', error)
    return '时间错误'
  }
}

// 获取最近会议预览（显示最多3个）
const getRecentMeetings = () => {
  const allMeetings = [...meetingHistory.value.upcoming, ...meetingHistory.value.ended]
  // 按创建时间排序，最新的在前
  allMeetings.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
  return allMeetings.slice(0, 3)
}

// 查看会议详情
const viewMeetingDetails = (meeting) => {
  console.log('查看会议详情:', meeting)
  // 这里可以实现会议详情查看功能
  alert(`会议详情：
会议名称: ${meeting.meetingName || '未命名会议'}
会议号: ${meeting.meetingNo}
会议ID: ${meeting.meetingId}
创建时间: ${formatDateTime(meeting.createTime)}
状态: ${meeting.status === 0 ? '进行中' : '已结束'}`)
}

// 切换账号
const handleSwitchAccount = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  wsService.disconnect()
  router.push('/login')
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm(
    '确定要退出登录吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    handleSwitchAccount()
    ElMessage.success('已退出登录')
  }).catch(() => {
    // 用户取消
  })
}

// 修改密码
const handleChangePassword = () => {
  ElMessageBox.prompt('请输入旧密码', '修改密码', {
    confirmButtonText: '下一步',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPattern: /^.{6,}$/,
    inputErrorMessage: '密码长度至少6位'
  }).then(({ value: oldPassword }) => {
    ElMessageBox.prompt('请输入新密码', '修改密码', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPattern: /^.{6,}$/,
      inputErrorMessage: '密码长度至少6位'
    }).then(async ({ value: newPassword }) => {
      try {
        const response = await changePassword(oldPassword, newPassword)
        if (response.data.code === 200) {
          ElMessage.success('密码修改成功，请重新登录')
          setTimeout(() => {
            handleSwitchAccount()
          }, 1500)
        } else {
          ElMessage.error(response.data.message || '密码修改失败')
        }
      } catch (error) {
        ElMessage.error('密码修改失败')
      }
    })
  }).catch(() => {
    // 用户取消
  })
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    console.log('开始加载用户信息...')
    const response = await getUserInfo()
    console.log('用户信息API响应:', response.data)
    
    if (response.data.code === 200) {
      userInfo.value = response.data.data
      localStorage.setItem('userInfo', JSON.stringify(response.data.data))
      console.log('用户信息加载成功:', userInfo.value)
    } else {
      console.error('加载用户信息失败:', response.data.info)
      // 如果API调用失败，尝试从localStorage获取
      const cachedUserInfo = localStorage.getItem('userInfo')
      if (cachedUserInfo) {
        try {
          userInfo.value = JSON.parse(cachedUserInfo)
          console.log('从缓存加载用户信息:', userInfo.value)
        } catch (e) {
          console.error('解析缓存用户信息失败:', e)
        }
      }
    }
  } catch (error) {
    console.error('加载用户信息API调用失败:', error)
    // 如果API调用失败，尝试从localStorage获取
    const cachedUserInfo = localStorage.getItem('userInfo')
    if (cachedUserInfo) {
      try {
        userInfo.value = JSON.parse(cachedUserInfo)
        console.log('从缓存加载用户信息:', userInfo.value)
      } catch (e) {
        console.error('解析缓存用户信息失败:', e)
      }
    }
  }
}

// 加载当前会议信息
const loadCurrentMeeting = async () => {
  try {
    console.log('开始加载当前会议信息...')
    const response = await meetingService.getCurrentMeeting()
    console.log('当前会议响应:', response.data)
    
    if (response.data.code === 200 && response.data.data) {
      currentMeeting.value = response.data.data
      // 判断是否为主持人（创建者）
      isCurrentMeetingHost.value = currentMeeting.value.createUserId === userInfo.value?.userId
      console.log('当前会议信息:', currentMeeting.value)
      console.log('是否为主持人:', isCurrentMeetingHost.value)
    } else {
      currentMeeting.value = null
      isCurrentMeetingHost.value = false
      console.log('当前没有进行中的会议')
    }
  } catch (error) {
    console.error('加载当前会议信息失败:', error)
    currentMeeting.value = null
    isCurrentMeetingHost.value = false
  }
}

// 加载会议历史
const loadMeetingHistory = async () => {
  try {
    // 并行加载进行中和已结束的会议
    const [upcomingResponse, endedResponse] = await Promise.all([
      getMeetingHistory(1, 0), // 加载进行中的会议（status=0）
      getMeetingHistory(1, 1)  // 加载已结束的会议（status=1）
    ])
    
    if (upcomingResponse.data.code === 200 && endedResponse.data.code === 200) {
      meetingHistory.value = {
        upcoming: upcomingResponse.data.data?.list || [],
        ended: endedResponse.data.data?.list || []
      }
      console.log('会议历史加载成功:', meetingHistory.value)
    } else {
      console.error('加载会议历史失败:', 
        upcomingResponse.data.code !== 200 ? upcomingResponse.data.info : endedResponse.data.info)
    }
  } catch (error) {
    console.error('加载会议历史失败:', error)
  }
}

// 加载联系人列表
const loadContactList = async () => {
  try {
    const response = await getContactList()
    if (response.data.code === 200) {
      contactList.value = response.data.data || []
      console.log('联系人列表加载成功:', contactList.value)
    } else {
      console.error('加载联系人列表失败:', response.data.info)
    }
  } catch (error) {
    console.error('加载联系人列表失败:', error)
  }
}

// 删除好友
const handleDeleteContact = async (contact) => {
  try {
    const confirmed = await ElMessageBox.confirm(
      `确定要删除好友 ${contact.nickName || contact.contactId} 吗？删除后对方也会收到通知。`,
      '删除好友',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    if (confirmed) {
      const response = await contactService.delContact(contact.contactId, 2) // status=2 表示删除
      if (response.data.code === 200) {
        ElMessage.success(`已删除好友 ${contact.nickName || contact.contactId}`)
        // 重新加载联系人列表
        await loadContactList()
      } else {
        ElMessage.error('删除好友失败: ' + response.data.info)
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除好友异常:', error)
      ElMessage.error('删除好友失败，请稍后重试')
    }
  }
}

// 拉黑好友
const handleBlacklistContact = async (contact) => {
  try {
    const confirmed = await ElMessageBox.confirm(
      `确定要拉黑 ${contact.nickName || contact.contactId} 吗？拉黑后您将从对方的联系人列表中删除，对方不会收到通知。`,
      '拉黑好友',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    if (confirmed) {
      const response = await contactService.delContact(contact.contactId, 3) // status=3 表示拉黑
      if (response.data.code === 200) {
        ElMessage.success(`已拉黑 ${contact.nickName || contact.contactId}`)
        // 重新加载联系人列表
        await loadContactList()
      } else {
        ElMessage.error('拉黑失败: ' + response.data.info)
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('拉黑异常:', error)
      ElMessage.error('拉黑失败，请稍后重试')
    }
  }
}

// 加载拉黑列表
const loadBlackList = async () => {
  try {
    const response = await contactService.loadBlackList()
    if (response.data.code === 200) {
      blackList.value = response.data.data || []
      console.log('拉黑列表加载成功:', blackList.value.length)
    } else {
      console.error('加载拉黑列表失败:', response.data.info)
      ElMessage.error('加载拉黑列表失败')
    }
  } catch (error) {
    console.error('加载拉黑列表异常:', error)
    ElMessage.error('加载拉黑列表失败，请稍后重试')
  }
}

// 处理拉黑列表标签页点击
const handleBlacklistTabClick = async () => {
  contactActiveTab.value = 'blacklist'
  await loadBlackList()
}

// 取消拉黑
const handleUnblackContact = async (contact) => {
  try {
    const confirmed = await ElMessageBox.confirm(
      `确定要取消拉黑 ${contact.nickName || contact.contactId} 吗？取消后您可以重新添加对方为好友。`,
      '取消拉黑',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    if (confirmed) {
      const response = await contactService.unblackContact(contact.contactId)
      if (response.data.code === 200) {
        ElMessage.success(`已取消拉黑 ${contact.nickName || contact.contactId}`)
        // 重新加载拉黑列表
        await loadBlackList()
      } else {
        ElMessage.error('取消拉黑失败: ' + response.data.info)
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消拉黑异常:', error)
      ElMessage.error('取消拉黑失败，请稍后重试')
    }
  }
}

// 格式化拉黑时间
const formatBlacklistTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

// 加载申请数量
const loadApplyCount = async () => {
  try {
    const response = await getContactApplyCount()
    if (response.data.code === 200) {
      applyCount.value = response.data.data || 0
      console.log('申请数量加载成功:', applyCount.value)
    } else {
      console.error('加载申请数量失败:', response.data.info)
    }
  } catch (error) {
    console.error('加载申请数量失败:', error)
  }
}

// 启动联系人列表轮询
const startContactPolling = () => {
  // 如果已经有轮询在运行，先停止
  if (contactPollingInterval.value) {
    clearInterval(contactPollingInterval.value)
  }
  
  console.log('启动联系人列表轮询，间隔:', CONTACT_POLLING_INTERVAL / 1000, '秒')
  
  // 设置定时器，每分钟轮询一次
  contactPollingInterval.value = setInterval(async () => {
    // 始终轮询申请数量（用于收件箱徽章）
    await loadApplyCount()
    
    // 只有在联系人页面时才轮询联系人列表
    if (activeNav.value === 'contact') {
      console.log('轮询更新联系人列表...')
      await loadContactList()
    }
    
    // 如果在收件箱页面，也轮询申请列表
    if (activeNav.value === 'inbox') {
      console.log('轮询更新收件箱申请列表...')
      await loadContactApplyList()
      await loadAllApplyList()
    }
  }, CONTACT_POLLING_INTERVAL)
}

// 停止联系人列表轮询
const stopContactPolling = () => {
  if (contactPollingInterval.value) {
    console.log('停止联系人列表轮询')
    clearInterval(contactPollingInterval.value)
    contactPollingInterval.value = null
  }
}

// 加载联系人申请列表（待处理）
const loadContactApplyList = async () => {
  try {
    const response = await getContactApplyList()
    if (response.data.code === 200) {
      contactApplyList.value = response.data.data || []
      console.log('待处理申请列表加载成功:', contactApplyList.value.length, '条')
    } else {
      console.error('加载联系人申请列表失败:', response.data.info)
    }
  } catch (error) {
    console.error('加载联系人申请列表异常:', error)
  }
}

// 加载当前用户发送的待处理申请
const loadMyApplyList = async () => {
  try {
    const response = await contactService.loadMyApply()
    if (response.data.code === 200) {
      myApplyList.value = response.data.data || []
      console.log('我的申请列表加载成功:', myApplyList.value.length, '条')
    } else {
      console.error('加载我的申请列表失败:', response.data.info)
    }
  } catch (error) {
    console.error('加载我的申请列表异常:', error)
  }
}

// 加载所有联系人申请列表（包括已处理）
const loadAllApplyList = async () => {
  try {
    // 调用后端新的API获取所有申请（包括已处理的）
    const response = await contactService.loadAllContactApply()
    if (response.data.code === 200) {
      allApplyList.value = response.data.data || []
      console.log('所有申请列表加载成功:', allApplyList.value.length, '条')
    } else {
      console.error('加载所有申请列表失败:', response.data.info)
    }
  } catch (error) {
    console.error('加载所有申请列表异常:', error)
  }
}

// 加载通知列表（按类别）
const loadNotificationsByCategory = async (category = 'all') => {
  try {
    const response = await notificationService.loadNotificationsByCategory(category, 1, 50)
    if (response.data.code === 200) {
      notificationList.value = response.data.data.list || []
      await refreshInstantInviteMeetingStatuses(notificationList.value)
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
      await refreshInstantInviteMeetingStatuses(pendingNotificationList.value)
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
const refreshInstantInviteMeetingStatuses = async (notifications = []) => {
  const meetingIds = [...new Set(
    notifications
      .filter(notification => notification.notificationType === 10)
      .map(notification => parseMeetingInvite(notification.content).meetingId)
      .filter(Boolean)
  )]

  if (meetingIds.length === 0) {
    instantInviteMeetingStatusMap.value = {}
    return
  }

  const statusEntries = await Promise.all(
    meetingIds.map(async (meetingId) => {
      try {
        const response = await meetingService.getMeetingStatus(meetingId)
        const ended = response?.data?.code === 200 ? !!response.data.data?.ended : true
        return [meetingId, ended]
      } catch (error) {
        console.error('获取即时会议状态失败:', meetingId, error)
        return [meetingId, true]
      }
    })
  )

  instantInviteMeetingStatusMap.value = Object.fromEntries(statusEntries)
}

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
      await loadNotificationsByCategory(selectedCategory.value)
      await loadUnreadNotificationCount()
    } else {
      ElMessage.error(response.data.info || '操作失败')
    }
  } catch (error) {
    console.error('处理会议邀请失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

// 解析即时会议邀请内容
const parseMeetingInvite = (content) => {
  try {
    return JSON.parse(content)
  } catch (error) {
    console.error('解析会议邀请内容失败:', error)
    return {
      meetingId: '',
      meetingNo: '',
      meetingName: '会议',
      password: '',
      inviterName: '',
      inviterUserId: ''
    }
  }
}

// 处理加入即时会议
const handleJoinInstantMeeting = async (notification) => {
  try {
    console.log('=== 开始处理即时会议邀请 ===')
    console.log('通知对象:', notification)
    console.log('notificationId:', notification.notificationId)
    console.log('actionStatus:', notification.actionStatus)
    
    const meetingInfo = parseMeetingInvite(notification.content)
    console.log('解析的会议信息:', meetingInfo)

    if (isInstantInviteEnded(notification)) {
      ElMessage.warning('该会议已经结束，无法加入')
      return
    }
    
    // 更新通知的 actionStatus 为已处理（1）
    // 这样通知会从"待办消息"移到"全部消息"
    if (notification.actionStatus === 0) {
      console.log('📤 准备调用 updateActionStatus API...')
      try {
        const response = await notificationService.updateActionStatus(notification.notificationId, 1)
        console.log('✅ updateActionStatus API 响应:', response)
        // 重新加载待办消息列表，移除该通知
        await loadPendingNotifications()
        console.log('✅ 待办消息列表已重新加载')
      } catch (error) {
        console.error('❌ 更新通知状态失败:', error)
        console.error('错误详情:', error.response || error.message)
      }
    } else {
      console.log('⚠️ actionStatus 不为 0，跳过更新:', notification.actionStatus)
    }
    
    // 标记通知为已读（不重新加载列表，避免影响跳转）
    try {
      console.log('📤 准备调用 markAsRead API...')
      const response = await notificationService.markAsRead(notification.notificationId)
      console.log('✅ markAsRead API 响应:', response)
      await loadUnreadNotificationCount()
    } catch (error) {
      console.error('❌ 标记通知为已读失败:', error)
    }
    
    // 跳转到会议页面，通过 query 传递会议号和密码
    console.log('🚀 准备跳转到会议页面...')
    router.push({
      path: '/meeting',
      query: {
        meetingNo: meetingInfo.meetingNo,
        password: meetingInfo.password || ''
      }
    })
    console.log('=== 即时会议邀请处理完成 ===')
  } catch (error) {
    console.error('加入会议失败:', error)
    ElMessage.error('加入会议失败，请重试')
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

// 格式化通知时间
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

// 判断是否应该显示类别标题
const shouldShowCategoryTitle = (notification, list) => {
  const index = list.indexOf(notification)
  if (index === 0) return true
  
  const currentCategory = getNotificationTypeInfo(notification.notificationType).category
  const prevCategory = getNotificationTypeInfo(list[index - 1].notificationType).category
  
  return currentCategory !== prevCategory
}

// 处理联系人申请（同意或拒绝）
const handleContactApply = async (applyUserId, status, applyUserName) => {
  try {
    console.log('处理联系人申请:', { applyUserId, status, applyUserName })
    
    const response = await dealWithContactApply(applyUserId, status)
    if (response.data.code === 200) {
      const statusText = status === 1 ? '同意' : '拒绝'
      console.log(`${statusText}联系人申请成功`)
      
      // 显示成功提示
      alert(`已${statusText} ${applyUserName} 的联系人申请`)
      
      // 重新加载申请列表和数量
      await loadContactApplyList()
      await loadAllApplyList()  // 同时刷新所有申请列表
      await loadApplyCount()
      
      // 如果同意了申请，也重新加载联系人列表
      if (status === 1) {
        await loadContactList()
      }
    } else {
      console.error('处理联系人申请失败:', response.data.info)
      alert('处理申请失败: ' + response.data.info)
    }
  } catch (error) {
    console.error('处理联系人申请异常:', error)
    alert('处理申请失败，请稍后重试')
  }
}

// 同意联系人申请
const acceptContactApply = (applyUserId, applyUserName) => {
  handleContactApply(applyUserId, 1, applyUserName)
}

// 拒绝联系人申请
const rejectContactApply = (applyUserId, applyUserName) => {
  handleContactApply(applyUserId, 0, applyUserName)
}

// 搜索联系人
const handleSearchContact = async () => {
  if (!searchContactInput.value.trim()) {
    searchContactMessage.value = '请输入用户ID或邮箱'
    searchContactError.value = true
    return
  }
  
  try {
    console.log('搜索联系人:', searchContactInput.value)
    searchContactMessage.value = '搜索中...'
    searchContactError.value = false
    searchContactResult.value = null
    
    // 判断输入的是邮箱还是用户ID
    const input = searchContactInput.value.trim()
    const isEmail = input.includes('@')
    
    const response = await searchContact(isEmail ? null : input, isEmail ? input : null)
    
    if (response.data.code === 200) {
      searchContactResult.value = response.data.data
      searchContactMessage.value = ''
      console.log('搜索结果:', searchContactResult.value)
    } else {
      searchContactMessage.value = response.data.info || '未找到该用户'
      searchContactError.value = true
      searchContactResult.value = null
    }
  } catch (error) {
    console.error('搜索联系人失败:', error)
    searchContactMessage.value = '搜索失败，请稍后重试'
    searchContactError.value = true
    searchContactResult.value = null
  }
}

// 申请添加联系人
const handleApplyContact = async (receiveUserId) => {
  try {
    console.log('申请添加联系人:', receiveUserId)
    
    const response = await applyContact(receiveUserId)
    
    if (response.data.code === 200) {
      const status = response.data.data
      
      if (status === 0) {
        searchContactMessage.value = '申请已发送，等待对方同意'
        searchContactError.value = false
        // 更新搜索结果中的状态
        if (searchContactResult.value) {
          searchContactResult.value.status = 0
        }
        // 刷新我的申请列表
        await loadMyApplyList()
      } else if (status === 1) {
        searchContactMessage.value = '已经是好友了'
        searchContactError.value = false
        if (searchContactResult.value) {
          searchContactResult.value.status = 1
        }
      }
    } else {
      searchContactMessage.value = response.data.info || '申请失败'
      searchContactError.value = true
    }
  } catch (error) {
    console.error('申请添加联系人失败:', error)
    searchContactMessage.value = '申请失败，请稍后重试'
    searchContactError.value = true
  }
}

// 获取搜索结果状态文本
const getSearchResultStatus = (result) => {
  if (result.status === -1) {
    return '这是你自己'
  } else if (result.status === null) {
    return '可以添加'
  } else if (result.status === 0) {
    return '已发送申请'
  } else if (result.status === 1) {
    return '已是好友'
  } else if (result.status === 3) {
    return '已拉黑'
  }
  return ''
}

// 关闭搜索联系人模态框
const closeSearchContactModal = () => {
  showSearchContactModal.value = false
  searchContactInput.value = ''
  searchContactResult.value = null
  searchContactMessage.value = ''
  searchContactError.value = false
}

// 显示联系人申请模态框
const showContactApplyModalHandler = async () => {
  await loadContactApplyList()
  showContactApplyModal.value = true
}

// 导航点击处理
const handleNavClick = async (navItem) => {
  console.log('切换导航到:', navItem)
  activeNav.value = navItem
  
  // 如果切换到会议页面，重新加载会议历史
  if (navItem === 'meeting') {
    console.log('切换到会议页面，重新加载会议历史')
    await loadMeetingHistory()
  }
}

// 显示全部会议模态框处理
const handleShowAllMeetings = async () => {
  console.log('显示全部会议模态框，重新加载会议历史')
  // 先重新加载会议历史，确保数据最新
  await loadMeetingHistory()
  // 然后显示模态框
  showAllMeetingsModal.value = true
}

// 会议标签页点击处理
const handleMeetingTabClick = async (tab) => {
  console.log('切换会议标签页到:', tab)
  activeMeetingTab.value = tab
  
  // 每次切换标签页都重新加载会议历史，确保数据最新
  console.log('重新加载会议历史以确保数据最新')
  await loadMeetingHistory()
}

// 会议相关函数
// 加入我的会议（从会议历史列表）
const joinMyMeeting = async (meeting) => {
  try {
    console.log('准备加入会议:', meeting)
    
    if (!meeting || !meeting.meetingId) {
      alert('会议信息无效')
      return
    }
    
    // 检查会议状态
    if (meeting.status !== 0) {
      alert('该会议已结束，无法进入')
      return
    }
    
    // 直接跳转到会议页面
    console.log('跳转到会议页面:', meeting.meetingId)
    await router.push(`/meeting/${meeting.meetingId}`)
    
  } catch (error) {
    console.error('加入会议失败:', error)
    alert('加入会议失败: ' + error.message)
  }
}

// 结束会议（从会议历史列表）
const finishMeeting = async (meeting) => {
  try {
    console.log('准备结束会议:', meeting)
    
    if (!meeting || !meeting.meetingId) {
      alert('会议信息无效')
      return
    }
    
    // 检查会议状态
    if (meeting.status !== 0) {
      alert('该会议已经结束')
      return
    }
    
    // 确认操作
    if (!confirm(`确定要结束会议"${meeting.meetingName || '未命名会议'}"吗？`)) {
      return
    }
    
    // 调用结束会议API，传入meetingId
    const response = await meetingService.finishMeeting(meeting.meetingId)
    console.log('结束会议响应:', response)
    
    if (response.data.code === 200) {
      alert('会议已结束')
      
      // 重新加载会议历史
      await loadMeetingHistory()
      
      // 如果这是当前会议，清除当前会议状态
      if (currentMeeting.value && currentMeeting.value.meetingId === meeting.meetingId) {
        currentMeeting.value = null
        isCurrentMeetingHost.value = false
      }
    } else {
      alert('结束会议失败: ' + response.data.info)
    }
    
  } catch (error) {
    console.error('结束会议失败:', error)
    alert('结束会议失败: ' + error.message)
  }
}

// 重新进入当前会议
const rejoinCurrentMeeting = async () => {
  console.log('点击重新进入会议按钮')
  console.log('当前会议信息:', currentMeeting.value)
  
  if (!currentMeeting.value) {
    console.error('没有当前会议信息')
    alert('没有当前会议信息')
    return
  }
  
  try {
    const meetingId = currentMeeting.value.meetingId
    console.log('准备重新进入会议:', meetingId)
    console.log('跳转路径:', `/meeting/${meetingId}`)
    
    // 跳转到会议页面
    const result = await router.push(`/meeting/${meetingId}`)
    console.log('重新进入会议跳转结果:', result)
  } catch (error) {
    console.error('重新进入会议失败:', error)
    alert('进入会议失败，请重试: ' + error.message)
  }
}

// 结束当前会议（主持人）
const finishCurrentMeeting = async () => {
  if (!currentMeeting.value) {
    alert('没有当前会议信息')
    return
  }
  
  if (!isCurrentMeetingHost.value) {
    alert('只有主持人可以结束会议')
    return
  }
  
  if (!confirm('确定要结束当前会议吗？')) {
    return
  }
  
  try {
    console.log('结束当前会议:', currentMeeting.value.meetingId)
    const response = await meetingService.finishMeeting()
    console.log('结束会议响应:', response.data)
    
    if (response.data.code === 200) {
      alert('会议已结束')
      // 清除当前会议信息
      currentMeeting.value = null
      isCurrentMeetingHost.value = false
      // 重新加载会议历史
      await loadMeetingHistory()
    } else {
      alert('结束会议失败: ' + response.data.info)
    }
  } catch (error) {
    console.error('结束会议失败:', error)
    alert('结束会议失败，请重试')
  }
}

// 离开当前会议（参与者）
const leaveCurrentMeeting = async () => {
  if (!currentMeeting.value) {
    alert('没有当前会议信息')
    return
  }
  
  if (!confirm('确定要离开当前会议吗？')) {
    return
  }
  
  try {
    console.log('离开当前会议:', currentMeeting.value.meetingId)
    const response = await meetingService.exitMeeting()
    console.log('离开会议响应:', response.data)
    
    if (response.data.code === 200) {
      alert('已离开会议')
      // 清除当前会议信息
      currentMeeting.value = null
      isCurrentMeetingHost.value = false
      // 重新加载会议历史
      await loadMeetingHistory()
    } else {
      alert('离开会议失败: ' + response.data.info)
    }
  } catch (error) {
    console.error('离开会议失败:', error)
    alert('离开会议失败，请重试')
  }
}

const openEditProfileModal = () => {
  // 初始化表单数据
  editProfileForm.value = {
    nickName: userInfo.value?.nickName || '',
    sex: userInfo.value?.sex ?? null,
    avatar: userInfo.value?.avatar || ''
  }
  showEditProfileModal.value = true
}

// 处理头像URL变化
const handleAvatarUrlChange = () => {
  // URL变化时的处理逻辑（如果需要验证URL有效性）
  console.log('头像URL已更新:', editProfileForm.value.avatar)
}

// 选择上传方式
const selectUploadMethod = (method) => {
  selectedUploadMethod.value = method
  // 重置相关状态
  if (method === 'url') {
    avatarUrlInput.value = ''
    urlPreviewError.value = false
    urlPreviewLoaded.value = false
  } else if (method === 'file') {
    uploadProgress.value = 0
  }
}

// 触发文件选择
const triggerFileInput = () => {
  const fileInput = document.querySelector('input[type="file"]')
  if (fileInput) {
    fileInput.click()
  }
}

// 处理文件拖拽
const handleFileDrop = (event) => {
  const files = event.dataTransfer.files
  if (files.length > 0) {
    const file = files[0]
    handleFileUpload(file)
  }
}

// 处理文件上传（重构原有方法）
const handleFileUpload = async (file) => {
  if (!file) return
  
  // 检查文件大小（2MB限制）
  if (file.size > 2 * 1024 * 1024) {
    alert('头像文件大小不能超过2MB')
    return
  }
  
  // 检查文件类型
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    alert('只支持JPG、PNG、GIF、WebP格式的图片')
    return
  }
  
  try {
    uploadProgress.value = 0
    const formData = new FormData()
    formData.append('file', file)
    
    // 创建XMLHttpRequest以支持进度跟踪
    const xhr = new XMLHttpRequest()
    
    // 监听上传进度
    xhr.upload.addEventListener('progress', (e) => {
      if (e.lengthComputable) {
        uploadProgress.value = Math.round((e.loaded / e.total) * 100)
      }
    })
    
    // 处理响应
    xhr.onload = () => {
      if (xhr.status === 200) {
        const result = JSON.parse(xhr.responseText)
        console.log('头像上传响应:', result)
        
        if (result.code === 200) {
          editProfileForm.value.avatar = result.data
          console.log('头像上传成功:', result.data)
          uploadProgress.value = 100
        } else {
          alert('头像上传失败: ' + result.info)
          uploadProgress.value = 0
        }
      } else {
        alert('头像上传失败，请重试')
        uploadProgress.value = 0
      }
    }
    
    xhr.onerror = () => {
      console.error('头像上传失败')
      alert('头像上传失败，请重试')
      uploadProgress.value = 0
    }
    
    // 发送请求
    xhr.open('POST', '/api/upload/avatar')
    xhr.setRequestHeader('token', localStorage.getItem('token'))
    xhr.send(formData)
    
  } catch (error) {
    console.error('头像上传失败:', error)
    alert('头像上传失败，请重试')
    uploadProgress.value = 0
  }
}

// 处理头像上传（文件输入）
const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (file) {
    await handleFileUpload(file)
  }
}

// 处理URL预览
const handleAvatarUrlPreview = () => {
  urlPreviewError.value = false
  urlPreviewLoaded.value = false
  console.log('URL预览重置:', avatarUrlInput.value)
}

// URL预览加载成功
const handleUrlPreviewLoad = () => {
  urlPreviewError.value = false
  urlPreviewLoaded.value = true
  console.log('URL预览加载成功:', avatarUrlInput.value)
}

// URL预览加载失败
const handleUrlPreviewError = (event) => {
  console.error('URL预览加载失败:', avatarUrlInput.value, event)
  
  // 尝试不使用crossorigin属性重新加载
  const img = event.target
  if (img.crossOrigin) {
    console.log('尝试不使用crossOrigin重新加载')
    img.crossOrigin = null
    img.src = avatarUrlInput.value + '?t=' + Date.now() // 添加时间戳避免缓存
    return
  }
  
  urlPreviewError.value = true
  urlPreviewLoaded.value = false
  
  // 提供更详细的错误信息
  console.log('URL预览最终失败，可能原因：')
  console.log('1. URL无效或图片不存在')
  console.log('2. 服务器设置了防盗链')
  console.log('3. CORS跨域限制')
  console.log('4. HTTPS/HTTP混合内容限制')
}

// 检查是否可以确认上传
const canConfirmUpload = computed(() => {
  if (selectedUploadMethod.value === 'file') {
    return editProfileForm.value.avatar && uploadProgress.value === 100
  } else if (selectedUploadMethod.value === 'url') {
    return avatarUrlInput.value && urlPreviewLoaded.value && !urlPreviewError.value
  }
  return false
})

// 确认头像上传
const confirmAvatarUpload = async () => {
  try {
    if (selectedUploadMethod.value === 'file' && editProfileForm.value.avatar) {
      // 文件上传已经完成，直接关闭模态框
      showAvatarUploadModal.value = false
      resetAvatarUploadModal()
    } else if (selectedUploadMethod.value === 'url' && avatarUrlInput.value) {
      // URL上传，调用后端API
      const params = new URLSearchParams()
      params.append('url', avatarUrlInput.value.trim())
      
      const response = await fetch('/api/upload/avatarByUrl', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'token': localStorage.getItem('token')
        },
        body: params
      })
      
      const result = await response.json()
      console.log('URL上传头像响应:', result)
      
      if (result.code === 200) {
        editProfileForm.value.avatar = result.data
        console.log('URL上传头像成功:', result.data)
        showAvatarUploadModal.value = false
        resetAvatarUploadModal()
      } else {
        alert('头像上传失败: ' + result.info)
      }
    }
  } catch (error) {
    console.error('确认头像上传失败:', error)
    alert('头像上传失败，请重试')
  }
}

// 重置头像上传模态框
const resetAvatarUploadModal = () => {
  selectedUploadMethod.value = ''
  avatarUrlInput.value = ''
  urlPreviewError.value = false
  urlPreviewLoaded.value = false
  uploadProgress.value = 0
}

// 保存个人信息
const handleSaveProfile = async () => {
  try {
    console.log('开始保存个人信息...', editProfileForm.value)
    
    // 验证必填字段
    if (!editProfileForm.value.nickName?.trim()) {
      alert('请输入昵称')
      return
    }
    
    if (editProfileForm.value.sex === null || editProfileForm.value.sex === undefined) {
      alert('请选择性别')
      return
    }
    
    // 准备请求参数
    const params = new URLSearchParams()
    params.append('nickName', editProfileForm.value.nickName.trim())
    params.append('sex', editProfileForm.value.sex.toString())
    if (editProfileForm.value.avatar) {
      params.append('avatar', editProfileForm.value.avatar)
    }
    
    console.log('发送更新请求参数:', Object.fromEntries(params))
    
    // 调用更新API
    const response = await fetch('/api/userInfo/updateUserInfo', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'token': localStorage.getItem('token')
      },
      body: params
    })
    
    const result = await response.json()
    console.log('更新个人信息响应:', result)
    
    if (result.code === 200) {
      // 更新成功
      alert('个人信息更新成功')
      
      // 更新本地用户信息
      userInfo.value = result.data
      
      // 更新localStorage缓存
      localStorage.setItem('userInfo', JSON.stringify(result.data))
      
      // 强制触发响应式更新
      userInfo.value = { ...result.data }
      
      // 关闭模态框
      showEditProfileModal.value = false
      
      // 延迟重新加载用户信息以确保显示最新数据
      setTimeout(async () => {
        try {
          await loadUserInfo()
          // 更新头像版本号强制刷新
          avatarVersion.value = Date.now()
        } catch (error) {
          console.error('重新加载用户信息失败:', error)
        }
      }, 500)
      
      // 重置表单
      editProfileForm.value = {
        nickName: '',
        sex: null,
        avatar: ''
      }
    } else {
      alert('更新失败: ' + result.info)
    }
  } catch (error) {
    console.error('保存个人信息失败:', error)
    alert('保存失败，请重试')
  }
}

// 快速会议处理
const handleQuickMeeting = async (formData) => {
  try {
    console.log('开始创建快速会议...', formData)
    
    // 准备请求数据
    const requestData = {
      meetingNoType: formData.meetingNoType,
      MeetingName: formData.meetingName || '快速会议',
      joinType: formData.joinType,
      joinPassword: formData.joinPassword || null
    }
    
    console.log('发送请求数据:', requestData)
    
    // 调用API
    const response = await meetingService.quickMeeting(requestData)
    console.log('快速会议创建响应:', response)
    console.log('响应数据:', response.data)
    
    if (response.data.code === 200) {
      const meetingId = response.data.data
      console.log('会议创建成功，会议ID:', meetingId)
      
      // 显示成功提示
      ElMessage.success('快速会议创建成功！正在进入会议页面...')
      console.log('准备跳转到:', `/meeting/${meetingId}`)
      
      // 立即跳转到会议页面
      try {
        console.log('开始执行路由跳转...')
        const result = await router.push(`/meeting/${meetingId}`)
        console.log('路由跳转结果:', result)
        console.log('成功跳转到会议页面')
      } catch (error) {
        console.error('跳转到会议页面失败:', error)
        ElMessage.error('跳转到会议页面失败，请手动进入会议')
      }
      
      // 异步重新加载当前会议信息和会议历史（不阻塞跳转）
      Promise.all([
        loadCurrentMeeting(),
        loadMeetingHistory()
      ]).catch(error => {
        console.error('重新加载会议信息失败:', error)
      })
    } else {
      console.error('创建会议失败:', response.data.info)
      ElMessage.error('创建会议失败: ' + response.data.info)
    }
  } catch (error) {
    console.error('创建快速会议失败:', error)
    ElMessage.error('创建会议失败，请重试')
  }
}

// 处理预约会议创建成功
const handleMeetingCreated = () => {
  console.log('预约会议创建成功，刷新列表')
  // 刷新预约会议列表
  if (reservationListRef.value && reservationListRef.value.loadMeetingReserveList) {
    reservationListRef.value.loadMeetingReserveList()
  }
}

// 加入会议处理
const handleJoinMeeting = async (formData) => {
  try {
    console.log('开始加入会议...', formData)
    
    // 调用预加入会议API
    const response = await meetingService.preJoinMeeting(
      formData.meetingId,
      userInfo.value?.nickName || '用户',
      formData.password
    )
    
    console.log('加入会议响应:', response.data)
    
    if (response.data.code === 200) {
      const meetingId = response.data.data
      console.log('验证成功，会议ID:', meetingId)
      
      // 显示成功提示
      ElMessage.success('验证成功，正在进入会议...')
      
      // 跳转到会议页面
      router.push(`/meeting/${meetingId}`)
      
      // 异步重新加载会议信息（不阻塞跳转）
      Promise.all([
        loadCurrentMeeting(),
        loadMeetingHistory()
      ]).catch(error => {
        console.error('重新加载会议信息失败:', error)
      })
    } else {
      console.error('加入会议失败:', response.data.info)
      ElMessage.error('加入会议失败: ' + response.data.info)
    }
  } catch (error) {
    console.error('加入会议失败:', error)
    ElMessage.error('加入会议失败，请重试')
  }
}

// 拖拽功能
let isDragging = false
let currentModal = null
let startX = 0
let startY = 0
let initialX = 0
let initialY = 0

const startDrag = (event, modalRef) => {
  if (event.target.classList.contains('modal-close')) return
  
  isDragging = true
  currentModal = modalRef
  
  const modal = event.currentTarget.closest('.modal-content')
  if (!modal) return
  
  const rect = modal.getBoundingClientRect()
  startX = event.clientX - rect.left
  startY = event.clientY - rect.top
  initialX = rect.left
  initialY = rect.top
  
  // 移除居中的transform，改为绝对定位
  modal.style.transform = 'none'
  modal.style.left = initialX + 'px'
  modal.style.top = initialY + 'px'
  
  document.addEventListener('mousemove', drag)
  document.addEventListener('mouseup', stopDrag)
  
  event.preventDefault()
}

const drag = (event) => {
  if (!isDragging || !currentModal) return
  
  const modal = document.querySelector('.modal-content')
  if (!modal) return
  
  const newX = event.clientX - startX
  const newY = event.clientY - startY
  
  // 限制拖拽范围，确保模态框不会完全移出视窗
  const maxX = window.innerWidth - modal.offsetWidth
  const maxY = window.innerHeight - modal.offsetHeight
  
  const constrainedX = Math.max(0, Math.min(newX, maxX))
  const constrainedY = Math.max(0, Math.min(newY, maxY))
  
  modal.style.left = constrainedX + 'px'
  modal.style.top = constrainedY + 'px'
}

const stopDrag = () => {
  isDragging = false
  currentModal = null
  document.removeEventListener('mousemove', drag)
  document.removeEventListener('mouseup', stopDrag)
}

// WebSocket消息处理
const handleContactApplyMessage = (message) => {
  console.log('收到联系人申请消息:', message)
  loadApplyCount()
  
  // 显示通知
  const content = message.messageContent || {}
  const nickName = content.nickName || content.applyUserId || '未知用户'
  
  settingsManager.showDesktopNotification('新的好友申请', {
    body: `${nickName} 想要添加你为好友`,
    tag: 'friend-request',
    requireInteraction: true
  })
  
  settingsManager.playNotificationSound()
}

const handleMeetingInviteMessage = async (message) => {
  console.log('收到会议邀请消息:', message)

  let inviteContent = message.messageContent
  if (typeof inviteContent === 'string') {
    try {
      inviteContent = JSON.parse(inviteContent)
    } catch (error) {
      console.error('解析会议邀请消息失败:', error)
    }
  }

  const inviterName = message.sendUserNickName || inviteContent?.inviteUserName || '会议主持人'
  const meetingName = inviteContent?.meetingName || '会议'

  settingsManager.showDesktopNotification('新的会议邀请', {
    body: `${inviterName} 邀请你加入「${meetingName}」`,
    tag: 'meeting-invite',
    requireInteraction: true
  })
  settingsManager.playNotificationSound()

  await loadUnreadNotificationCount()
  if (inboxActiveTab.value === 'all') {
    await loadNotificationsByCategory(selectedCategory.value)
  } else {
    await loadPendingNotifications()
  }

  const targetMeetingId = inviteContent?.meetingId
  const matchedNotification =
    pendingNotificationList.value.find(n => n.notificationType === 10 && parseMeetingInvite(n.content).meetingId === targetMeetingId) ||
    notificationList.value.find(n => n.notificationType === 10 && parseMeetingInvite(n.content).meetingId === targetMeetingId)

  incomingMeetingInviteNotification.value = matchedNotification || {
    notificationId: null,
    actionStatus: 1,
    status: 0,
    content: JSON.stringify(inviteContent || {}),
    relatedUserName: inviterName
  }
  showIncomingMeetingInviteModal.value = true
}

const closeIncomingMeetingInviteModal = () => {
  showIncomingMeetingInviteModal.value = false
  incomingMeetingInviteNotification.value = null
}

const confirmIncomingMeetingInvite = async () => {
  if (!incomingMeetingInviteNotification.value) {
    return
  }

  const notification = incomingMeetingInviteNotification.value
  closeIncomingMeetingInviteModal()

  if (notification.notificationId) {
    await handleJoinInstantMeeting(notification)
    return
  }

  const meetingInfo = parseMeetingInvite(notification.content)
  router.push({
    path: '/meeting',
    query: {
      meetingNo: meetingInfo.meetingNo,
      password: meetingInfo.password || ''
    }
  })
}

// 处理用户在线状态变更消息
const handleUserOnlineStatusChange = (message) => {
  console.log('收到用户在线状态变更消息:', message)
  
  try {
    const statusData = message.messageContent
    const userId = statusData.userId
    const onlineStatus = statusData.onlineStatus
    const lastLoginTime = statusData.lastLoginTime
    const lastOffTime = statusData.lastOffTime
    
    console.log(`用户 ${userId} 状态变更: ${onlineStatus === 1 ? '在线' : '离线'}`)
    
    // 更新联系人列表中对应用户的状态
    const contactIndex = contactList.value.findIndex(c => c.contactId === userId)
    if (contactIndex !== -1) {
      const contact = contactList.value[contactIndex]
      
      // 更新时间戳
      if (lastLoginTime) {
        contact.lastLoginTime = lastLoginTime
      }
      if (lastOffTime) {
        contact.lastOffTime = lastOffTime
      }
      contact.onlineStatus = onlineStatus
      
      // 触发响应式更新
      contactList.value[contactIndex] = { ...contact }
      
      console.log(`已更新联系人 ${userId} 的状态`)
    } else {
      console.log(`联系人列表中未找到用户 ${userId}`)
    }
  } catch (error) {
    console.error('处理用户在线状态变更失败:', error)
  }
}

// 处理好友删除通知消息
const handleContactDeleteMessage = (message) => {
  console.log('收到好友删除通知:', message)
  
  try {
    const senderNickName = message.sendUserNickName || '对方'
    
    // 显示通知
    ElMessage.warning(`${senderNickName} 已将您从好友列表中删除`)
    
    // 重新加载联系人列表
    loadContactList()
  } catch (error) {
    console.error('处理好友删除通知失败:', error)
  }
}

// 更新连接状态
const updateConnectionState = () => {
  const state = wsService.getConnectionState()
  
  // 检测连接状态变化
  const wasConnected = connectionState.value.isConnected
  const isNowConnected = state.isConnected
  
  connectionState.value = {
    isConnected: state.isConnected,
    isReconnecting: state.isReconnecting,
    reconnectAttempts: state.reconnectAttempts,
    status: state.isConnected ? 'connected' : (state.isReconnecting ? 'reconnecting' : 'disconnected'),
    connectedAt: state.isConnected ? (connectionState.value.connectedAt || Date.now()) : null
  }
  
  // 如果从断开变为连接，记录连接时间
  if (!wasConnected && isNowConnected) {
    connectionState.value.connectedAt = Date.now()
  }
}

// 获取连接状态文本
const getConnectionStatusText = () => {
  if (connectionState.value.isConnected) {
    return '已连接'
  } else if (connectionState.value.isReconnecting) {
    return `重连中 (第 ${connectionState.value.reconnectAttempts} 次)`
  } else {
    return '未连接'
  }
}

// 处理连接指示器点击
const handleConnectionIndicatorClick = () => {
  const state = connectionState.value
  
  if (!state.isConnected) {
    // 如果正在重连中，显示提示
    if (state.status === 'reconnecting') {
      ElMessage.info('正在尝试重新连接，请稍候...')
      return
    }
    
    // 直接尝试重连，不需要用户确认
    ElMessage.info('正在重新连接...')
    wsService.manualReconnect()
      .then(() => {
        ElMessage.success('重新连接成功')
      })
      .catch(error => {
        ElMessage.error('重新连接失败: ' + error.message)
      })
  } else {
    // 显示连接信息
    const uptime = state.connectedAt 
      ? Math.floor((Date.now() - state.connectedAt) / 1000)
      : 0
    const minutes = Math.floor(uptime / 60)
    const seconds = uptime % 60
    ElMessage.success(`WebSocket 连接正常 (已连接 ${minutes}分${seconds}秒)`)
  }
}

// 启动连接状态监控
const startConnectionStateMonitor = () => {
  // 立即更新一次
  updateConnectionState()
  
  // 每秒更新一次连接状态
  connectionStateInterval.value = setInterval(() => {
    updateConnectionState()
  }, 1000)
}

// 停止连接状态监控
const stopConnectionStateMonitor = () => {
  if (connectionStateInterval.value) {
    clearInterval(connectionStateInterval.value)
    connectionStateInterval.value = null
  }
}

// 监听收件箱标签页切换
watch(inboxActiveTab, async (newTab) => {
  if (newTab === 'all') {
    await loadNotificationsByCategory(selectedCategory.value)
  } else {
    await loadPendingNotifications()
  }
})

// 页面初始化
onMounted(async () => {
  console.log('Dashboard页面初始化开始')
  
  // 检查token
  const token = localStorage.getItem('token')
  console.log('当前token:', token ? '存在' : '不存在')
  
  if (!token) {
    console.warn('没有token，跳转到登录页面')
    router.push('/login')
    return
  }
  
  await loadUserInfo()
  await loadCurrentMeeting()
  loadMeetingHistory()
  
  // 初始化WebSocket连接
  if (userInfo.value) {
    console.log('初始化WebSocket连接，用户ID:', userInfo.value.userId)
    wsService.connect(token, userInfo.value.userId)
    wsService.onMessage(MessageType.USER_CONTACT_APPLY, handleContactApplyMessage)
    wsService.onMessage(MessageType.INVITE_MEMBER_MEETING, handleMeetingInviteMessage)
    wsService.onMessage(MessageType.USER_ONLINE_STATUS_CHANGE, handleUserOnlineStatusChange)
    wsService.onMessage(MessageType.USER_CONTACT_DELETE, handleContactDeleteMessage)
    
    // 启动连接状态监控
    startConnectionStateMonitor()
  } else {
    console.warn('用户信息为空，无法建立WebSocket连接')
  }
  
  // 始终加载申请数量（用于显示收件箱徽章）
  await loadApplyCount()
  console.log('已加载好友申请数量')
  
  // 启动联系人和申请的轮询（始终运行）
  startContactPolling()
  
  // 加载联系人相关数据
  if (activeNav.value === 'contact') {
    await loadContactList()
  }
  
  // 如果当前在收件箱页面，加载申请列表
  if (activeNav.value === 'inbox') {
    await loadContactApplyList()
    await loadAllApplyList()
  }
  
  // 设置定时器定期检查当前会议状态
  meetingPollingInterval.value = setInterval(async () => {
    if (currentMeeting.value) {
      await loadCurrentMeeting()
    }
  }, 30000) // 每30秒检查一次
  
  console.log('Dashboard页面初始化完成')
})

// 页面卸载时清理
onUnmounted(() => {
  console.log('Dashboard页面卸载，清理资源...')
  
  // 清理会议状态轮询定时器
  if (meetingPollingInterval.value) {
    clearInterval(meetingPollingInterval.value)
    meetingPollingInterval.value = null
    console.log('✅ 会议轮询定时器已清理')
  }
  
  // 停止联系人列表轮询
  stopContactPolling()
  
  // 停止连接状态监控
  stopConnectionStateMonitor()
  
  wsService.offMessage(MessageType.USER_CONTACT_APPLY, handleContactApplyMessage)
  wsService.offMessage(MessageType.INVITE_MEMBER_MEETING, handleMeetingInviteMessage)
  wsService.offMessage(MessageType.USER_ONLINE_STATUS_CHANGE, handleUserOnlineStatusChange)
  wsService.offMessage(MessageType.USER_CONTACT_DELETE, handleContactDeleteMessage)
  wsService.disconnect()
  
  console.log('✅ Dashboard资源清理完成')
})
</script>

<style scoped>
/* CSS 变量定义 - 深色主题基调 */
:root {
  /* 主色调 */
  --primary-color: #999999;
  --primary-hover: #b3b3b3;
  --primary-light: rgba(153, 153, 153, 0.2);
  
  /* 功能色彩 */
  --success-color: #4caf50;
  --success-hover: #66bb6a;
  --warning-color: #ff9800;
  --warning-hover: #ffb74d;
  --danger-color: #f44336;
  --danger-hover: #ef5350;
  
  /* 背景色系 */
  --background-primary: #363636;
  --background-secondary: #434343;
  --background-tertiary: #4a4a4a;
  --background-dark: #2a2a2a;
  --background-card: #434343;
  
  /* 文字色系 */
  --text-primary: #ffffff;
  --text-secondary: #cccccc;
  --text-tertiary: #999999;
  --text-inverse: #000000;
  --text-muted: #888888;
  
  /* 边框和分割线 */
  --border-color: #555555;
  --border-hover: #666666;
  --divider-color: #4a4a4a;
  
  /* 阴影效果 */
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.3);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.4), 0 2px 4px -1px rgba(0, 0, 0, 0.3);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.4), 0 4px 6px -2px rgba(0, 0, 0, 0.3);
  --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 10px 10px -5px rgba(0, 0, 0, 0.4);
  
  /* 其他设计元素 */
  --border-radius-sm: 6px;
  --border-radius: 8px;
  --border-radius-lg: 12px;
  --border-radius-xl: 16px;
  --transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  /* 渐变背景 */
  --gradient-primary: linear-gradient(135deg, #999999 0%, #777777 100%);
  --gradient-secondary: linear-gradient(135deg, #666666 0%, #888888 100%);
  --gradient-success: linear-gradient(135deg, #4caf50 0%, #66bb6a 100%);
}

/* 全局布局 */
.dashboard-container {
  display: grid;
  grid-template-areas: 
    "sidebar main"
    "sidebar main";
  grid-template-columns: 80px 1fr;
  grid-template-rows: 1fr;
  height: 100vh;
  overflow: hidden;
  background: var(--background-primary);
  color: var(--text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 移动端布局 */
@media (max-width: 768px) {
  .dashboard-container {
    grid-template-areas: 
      "header"
      "main";
    grid-template-columns: 1fr;
    grid-template-rows: 60px 1fr;
  }
}

/* 移动端顶部导航栏 */
.mobile-header {
  display: none;
  grid-area: header;
  background: var(--background-secondary);
  border-bottom: 1px solid var(--border-color);
  backdrop-filter: blur(10px);
  z-index: 1000;
}

@media (max-width: 768px) {
  .mobile-header {
    display: block;
  }
}

.mobile-header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 100%;
}

.mobile-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid var(--border-color);
  transition: var(--transition);
}

.mobile-avatar:hover {
  border-color: var(--primary-color);
  transform: scale(1.05);
}

.mobile-user-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mobile-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
  color: var(--text-primary);
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.mobile-menu-toggle {
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  width: 28px;
  height: 28px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: var(--border-radius-sm);
  transition: var(--transition);
}

.mobile-menu-toggle:hover {
  background-color: var(--background-tertiary);
}

.hamburger-line {
  width: 100%;
  height: 3px;
  background: var(--gradient-primary);
  border-radius: 2px;
  transition: var(--transition);
}

/* 移动端导航菜单 */
.mobile-nav {
  display: none;
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  background: var(--background-secondary);
  border-bottom: 1px solid var(--border-color);
  transform: translateY(-100%);
  transition: var(--transition-slow);
  z-index: 999;
  box-shadow: var(--shadow-lg);
}

@media (max-width: 768px) {
  .mobile-nav {
    display: block;
  }
}

.mobile-nav-open {
  transform: translateY(0);
}

.mobile-nav-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.mobile-nav-list li {
  border-bottom: 1px solid var(--divider-color);
}

.mobile-nav-list li:last-child {
  border-bottom: none;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  padding: 18px 24px;
  cursor: pointer;
  transition: var(--transition);
  position: relative;
}

.mobile-nav-item:hover {
  background-color: var(--background-tertiary);
}

.mobile-nav-list li.active .mobile-nav-item {
  background: var(--primary-light);
  color: var(--primary-color);
  font-weight: 600;
}

.mobile-nav-list li.active .mobile-nav-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--gradient-primary);
}

.mobile-nav-icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.mobile-nav-icon {
  width: 24px;
  height: 24px;
  margin-right: 16px;
  filter: brightness(0) saturate(100%) invert(100%);
}

.mobile-nav-list li.active .mobile-nav-icon {
  filter: brightness(0) saturate(100%) invert(60%) sepia(0%) saturate(0%) hue-rotate(0deg) brightness(153%) contrast(88%);
}

.mobile-nav-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: var(--gradient-secondary);
  color: var(--text-inverse);
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  box-shadow: var(--shadow-sm);
}

.mobile-nav-label {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
}

/* 桌面端左侧导航栏 */
.desktop-sidebar {
  grid-area: sidebar;
  width: 80px;
  background: var(--background-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;
  z-index: 100;
  box-shadow: var(--shadow-sm);
}

@media (max-width: 768px) {
  .desktop-sidebar {
    display: none;
  }
}

/* 头像区域 */
.avatar-section {
  margin-bottom: 32px;
  cursor: pointer;
  transition: var(--transition);
  position: relative;
}

.avatar-section:hover {
  transform: translateY(-2px);
}

.avatar-section::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: var(--gradient-primary);
  opacity: 0;
  transition: var(--transition);
  z-index: -1;
}

.avatar-section:hover::after {
  opacity: 0.1;
}

.user-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--border-color);
  transition: var(--transition);
}

.avatar-section:hover .user-avatar {
  border-color: var(--primary-color);
}

/* WebSocket 连接状态指示器 */
.connection-indicator {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--secondary-bg);
  border: 2px solid var(--primary-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--transition);
  z-index: 10;
}

.connection-indicator:hover {
  transform: scale(1.2);
}

.connection-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #999999;
  transition: var(--transition);
}

.connection-indicator.connected .connection-dot {
  background: #52c41a;
  animation: pulse 2s infinite;
}

.connection-indicator.reconnecting .connection-dot {
  background: #faad14;
  animation: blink 1s infinite;
}

.connection-indicator.disconnected .connection-dot {
  background: #ff4d4f;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

@keyframes blink {
  0%, 50%, 100% {
    opacity: 1;
  }
  25%, 75% {
    opacity: 0.3;
  }
}

/* 导航菜单 */
.sidebar-nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
  width: 100%;
}

.sidebar-nav li {
  margin-bottom: 16px;
  position: relative;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  cursor: pointer;
  border-radius: var(--border-radius);
  transition: var(--transition);
  position: relative;
  margin: 0 8px;
}

.nav-item:hover {
  background-color: var(--background-tertiary);
  transform: translateY(-1px);
}

.sidebar-nav li.active .nav-item {
  background: var(--primary-light);
  color: var(--primary-color);
}

.sidebar-nav li.active .nav-item::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--gradient-primary);
  border-radius: 0 2px 2px 0;
}

.nav-icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-icon {
  width: 24px;
  height: 24px;
  margin-bottom: 6px;
  filter: brightness(0) saturate(100%) invert(100%);
  transition: var(--transition);
}

.sidebar-nav li.active .nav-icon {
  filter: brightness(0) saturate(100%) invert(60%) sepia(0%) saturate(0%) hue-rotate(0deg) brightness(153%) contrast(88%);
}

.nav-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: var(--gradient-secondary);
  color: var(--text-inverse);
  border-radius: 50%;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  box-shadow: var(--shadow-sm);
}

.nav-label {
  font-size: 11px;
  text-align: center;
  color: var(--text-secondary);
  font-weight: 500;
  transition: var(--transition);
}

.sidebar-nav li.active .nav-label {
  color: var(--primary-color);
  font-weight: 600;
}

/* 底部区域 */
.bottom-section {
  margin-top: auto;
  width: 100%;
}

.bottom-nav-item {
  margin-bottom: 15px;
  position: relative;
}

.bottom-nav-item:last-child {
  margin-bottom: 0;
}

.nav-tooltip {
  display: none;
  position: absolute;
  left: 70px;
  top: 50%;
  transform: translateY(-50%);
  background-color: var(--surface-color);
  color: var(--text-primary);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  box-shadow: var(--shadow);
  z-index: 1000;
}

.bottom-nav-item:hover .nav-tooltip {
  display: block;
}

/* 主内容区域 */
.main-content {
  grid-area: main;
  overflow-y: auto;
  padding: 20px;
  background-color: var(--background-primary);
}

@media (max-width: 768px) {
  .main-content {
    padding: 16px;
  }
}

/* 页面标题 */
.page-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 24px;
  color: var(--text-primary);
}

@media (max-width: 768px) {
  .page-title {
    font-size: 20px;
    margin-bottom: 20px;
  }
}

/* 会议功能区 */
.meeting-features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

@media (max-width: 768px) {
  .meeting-features {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

/* 功能卡片 */
.feature-card {
  background-color: var(--background-card);
  border-radius: var(--border-radius-lg);
  padding: 24px;
  text-align: center;
  transition: var(--transition);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.feature-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--border-hover);
}

@media (max-width: 768px) {
  .feature-card {
    padding: 20px;
  }
}

.feature-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.feature-icon-img {
  width: 32px;
  height: 32px;
}

.join-meeting {
  background-color: var(--primary-color);
}

.quick-meeting {
  background-color: var(--success-color);
}

.schedule-meeting {
  background-color: var(--warning-color);
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.feature-card p {
  color: var(--text-secondary);
  margin-bottom: 20px;
  line-height: 1.5;
}

.feature-button {
  background-color: var(--primary-color);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: var(--border-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  width: 100%;
}

.feature-button:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* 全部会议区域 */
.all-meetings-section {
  background-color: var(--background-card);
  border-radius: var(--border-radius-lg);
  padding: 24px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

@media (max-width: 768px) {
  .all-meetings-section {
    padding: 20px;
  }
}

.all-meetings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: var(--transition);
  padding: 8px;
  border-radius: var(--border-radius);
}

.all-meetings-header:hover {
  background-color: var(--background-tertiary);
}

.all-meetings-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.view-all-arrow {
  font-size: 16px;
  color: var(--text-secondary);
  transition: var(--transition);
}

.all-meetings-header:hover .view-all-arrow {
  color: var(--primary-color);
  transform: translateX(4px);
}

.meetings-preview {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meeting-preview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background-color: var(--background-tertiary);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color);
}

.meeting-preview-info h4 {
  font-size: 14px;
  font-weight: 500;
  margin: 0 0 4px 0;
  color: var(--text-primary);
}

.meeting-preview-info p {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}

.empty-meetings {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-secondary);
}

/* 预约会议区域 */
.reservation-section {
  background-color: var(--background-card);
  border-radius: var(--border-radius-lg);
  padding: 24px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  margin-top: 24px;
}

@media (max-width: 768px) {
  .reservation-section {
    padding: 20px;
  }
}

.reservation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: var(--transition);
  padding: 8px;
  border-radius: var(--border-radius);
}

.reservation-header:hover {
  background-color: var(--background-tertiary);
}

.reservation-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.toggle-arrow {
  font-size: 14px;
  color: var(--text-secondary);
  transition: var(--transition);
  display: inline-block;
}

.toggle-arrow.arrow-down {
  transform: rotate(90deg);
}

.reservation-content {
  margin-top: 16px;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 全部会议模态框 */
.all-meetings-modal {
  width: 90vw;
  max-width: 800px;
  max-height: 80vh;
}

.meeting-tabs {
  display: flex;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 20px;
}

.tab-button {
  flex: 1;
  padding: 12px 16px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  border-bottom: 2px solid transparent;
}

.tab-button:hover {
  color: var(--text-primary);
  background-color: var(--background-tertiary);
}

.tab-button.active {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
}

.meeting-list {
  max-height: 400px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.meeting-item {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 16px;
  padding: 16px;
  background-color: var(--background-tertiary);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color);
  transition: var(--transition);
}

.meeting-item:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-sm);
}

@media (max-width: 768px) {
  .meeting-item {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 12px;
  }
}

.meeting-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.meeting-header h4 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.meeting-status {
  padding: 4px 8px;
  border-radius: var(--border-radius-sm);
  font-size: 12px;
  font-weight: 500;
}

.meeting-status.running {
  background-color: var(--success-color);
  color: white;
}

.meeting-status.finished {
  background-color: var(--text-secondary);
  color: white;
}

.meeting-details p {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.meeting-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 120px;
}

@media (max-width: 768px) {
  .meeting-actions {
    flex-direction: row;
    min-width: auto;
  }
}

.action-button {
  padding: 8px 16px;
  border: none;
  border-radius: var(--border-radius);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.action-button.join {
  background-color: var(--success-color);
  color: white;
}

.action-button.join:hover {
  background-color: var(--success-hover);
  box-shadow: var(--shadow-sm);
}

.action-button.finish {
  background-color: var(--danger-color);
  color: white;
}

.action-button.finish:hover {
  background-color: var(--danger-hover);
  box-shadow: var(--shadow-sm);
}

.action-button.details {
  background-color: var(--primary-color);
  color: white;
}

.action-button.details:hover {
  background-color: var(--primary-hover);
  box-shadow: var(--shadow-sm);
}

/* 个人信息页面 */
.user-profile {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 24px;
  background-color: var(--background-card);
  border-radius: var(--border-radius-lg);
  padding: 24px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

@media (max-width: 768px) {
  .user-profile {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 20px;
  }
}

.profile-avatar-container {
  display: flex;
  justify-content: center;
}

.profile-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--border-color);
}

@media (max-width: 768px) {
  .profile-avatar {
    width: 100px;
    height: 100px;
  }
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 768px) {
  .info-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}

.info-item label {
  font-weight: 500;
  color: var(--text-secondary);
  min-width: 100px;
}

.info-item span {
  color: var(--text-primary);
}

.profile-actions {
  margin-top: 20px;
}

.edit-profile-button {
  background-color: var(--primary-color);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: var(--border-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.edit-profile-button:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* 联系人页面 */
.contact-actions {
  margin-bottom: 24px;
}

.contact-action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: var(--primary-color);
  color: white;
  border: none;
  padding: 12px 20px;
  border-radius: var(--border-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.contact-action-btn:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.action-icon {
  width: 16px;
  height: 16px;
  filter: brightness(0) saturate(100%) invert(100%);
}

.contact-notifications {
  background: var(--gradient-secondary);
  color: var(--text-primary);
  padding: 16px;
  border-radius: var(--border-radius-lg);
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notification-header h3 {
  margin: 0;
  font-size: 16px;
}

.view-all-btn {
  background: none;
  border: 1px solid var(--text-primary);
  color: var(--text-primary);
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: var(--transition);
}

.view-all-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.contact-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 已发送的申请部分 */
.my-apply-section {
  margin-bottom: 24px;
  background: var(--background-card);
  border-radius: var(--border-radius);
  padding: 16px;
  border: 1px solid var(--border-color);
}

.my-apply-section .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.my-apply-section .section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.my-apply-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.my-apply-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  padding: 12px;
  background: var(--background-secondary);
  border-radius: 8px;
  border: 1px solid var(--border-color);
  transition: var(--transition);
  align-items: center;
}

.my-apply-item:hover {
  border-color: var(--primary-color);
  transform: translateX(2px);
}

.my-apply-item .apply-avatar {
  width: 40px;
  height: 40px;
}

.my-apply-item .apply-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.my-apply-item .apply-info {
  flex: 1;
}

.my-apply-item .apply-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.my-apply-item .apply-time {
  font-size: 12px;
  color: var(--text-tertiary);
  margin: 0;
}

.my-apply-item .apply-status {
  display: flex;
  align-items: center;
}

.waiting-badge {
  background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.contact-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 16px;
  padding: 16px;
  background-color: var(--background-card);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color);
  transition: var(--transition);
  box-shadow: var(--shadow-sm);
  align-items: center;
}

.contact-item:hover {
  border-color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.contact-actions {
  display: flex;
  gap: 8px;
}

.contact-action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.delete-btn {
  background-color: #ff9800;
  color: white;
}

.delete-btn:hover {
  background-color: #f57c00;
  transform: translateY(-1px);
}

.blacklist-btn {
  background-color: #f44336;
  color: white;
}

.blacklist-btn:hover {
  background-color: #d32f2f;
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .contact-item {
    gap: 12px;
    padding: 12px;
    grid-template-columns: auto 1fr;
  }
  
  .contact-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
  
  .contact-action-btn {
    padding: 5px 10px;
    font-size: 12px;
  }
}

.contact-avatar {
  position: relative;
  width: 50px;
  height: 50px;
}

.contact-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.contact-status {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: var(--text-tertiary);
  border: 2px solid var(--background-card);
}

.contact-status.online {
  background-color: var(--success-color);
}

.contact-info h4 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--text-primary);
}

.contact-status-text {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.contact-last-time {
  font-size: 12px;
  color: var(--text-tertiary);
  margin: 2px 0 0 0;
}

.empty-contacts {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-secondary);
}

/* 欢迎消息 */
.welcome-message {
  text-align: center;
  padding: 60px 20px;
  background-color: var(--background-card);
  border-radius: var(--border-radius-lg);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.welcome-message p {
  font-size: 16px;
  color: var(--text-secondary);
  margin: 0;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
}

::-webkit-scrollbar-track {
  background: var(--background-tertiary);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: var(--text-tertiary);
  border-radius: 4px;
  transition: var(--transition);
}

::-webkit-scrollbar-thumb:hover {
  background: var(--text-secondary);
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: transparent;
  pointer-events: none;
  z-index: 2000;
}

.incoming-meeting-invite-modal {
  width: min(420px, calc(100vw - 32px));
}

.invite-modal-text {
  margin: 0 0 16px;
  color: #e2e8f0;
  font-size: 15px;
  line-height: 1.6;
}

.meeting-details.compact {
  gap: 10px;
}

.modal-content {
  background: #363636;
  border-radius: var(--border-radius-xl);
  box-shadow: var(--shadow-xl);
  width: 400px;
  max-width: 90vw;
  max-height: 80vh;
  overflow-y: auto;
  animation: modalFadeIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid #888888;
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: auto;
}

.modal-content:hover {
  border-color: #aaaaaa;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 0;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 24px;
  cursor: move;
  user-select: none;
}

.modal-header:hover {
  background-color: rgba(136, 136, 136, 0.1);
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #dfdfdf;
}

.modal-close {
  background: none;
  border: none;
  font-size: 28px;
  color: #dfdfdf;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: var(--transition);
}

.modal-close:hover {
  background-color: var(--background-tertiary);
  color: #ffffff;
}

.modal-body {
  padding: 0 24px 24px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 24px;
  border-top: 1px solid var(--border-color);
  background-color: #363636;
  border-radius: 0 0 var(--border-radius-xl) var(--border-radius-xl);
}

/* 表单样式 */
.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #dfdfdf;
  font-size: 14px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  font-size: 14px;
  color: #dfdfdf;
  background-color: var(--background-secondary);
  transition: var(--transition);
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.form-input::placeholder {
  color: #dfdfdf;
}

/* 单选按钮组 */
.radio-group {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #dfdfdf;
}

.radio-item input[type="radio"] {
  width: 16px;
  height: 16px;
  accent-color: var(--primary-color);
}

/* 按钮样式 */
.btn-primary {
  background-color: var(--primary-color);
  color: var(--text-inverse);
  border: none;
  padding: 12px 24px;
  border-radius: var(--border-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.btn-primary:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.btn-secondary {
  background-color: var(--background-tertiary);
  color: #dfdfdf;
  border: 1px solid var(--border-color);
  padding: 12px 24px;
  border-radius: var(--border-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.btn-secondary:hover {
  background-color: var(--border-color);
  border-color: var(--border-hover);
}

.btn-success {
  background-color: var(--success-color);
  color: var(--text-primary);
  border: none;
  padding: 8px 16px;
  border-radius: var(--border-radius-sm);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.btn-success:hover {
  background-color: var(--success-hover);
}

.btn-danger {
  background-color: var(--danger-color);
  color: var(--text-primary);
  border: none;
  padding: 8px 16px;
  border-radius: var(--border-radius-sm);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.btn-danger:hover {
  background-color: var(--danger-hover);
}

.search-btn {
  width: 100%;
  margin-top: 12px;
}

/* 搜索结果 */
.search-result {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--secondary-bg);
  border-radius: 8px;
}

.result-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}

.result-info {
  flex: 1;
}

.result-info h4 {
  margin: 0 0 4px 0;
  font-size: 16px;
  color: var(--text-primary);
}

.result-info p {
  margin: 2px 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.result-status {
  font-size: 12px;
  color: var(--primary-color);
}

.status-text {
  font-size: 14px;
  color: var(--text-secondary);
  padding: 6px 12px;
  background: var(--secondary-bg);
  border-radius: 4px;
}

.search-message {
  margin-top: 12px;
  padding: 12px;
  border-radius: 4px;
  text-align: center;
  background: var(--secondary-bg);
  color: var(--text-secondary);
}

.search-message.error {
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
}

/* 头像上传 */
.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.avatar-preview-container {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  transition: var(--transition);
}

.avatar-preview-container:hover {
  transform: scale(1.05);
}

.avatar-preview {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--border-color);
  transition: var(--transition);
}

.avatar-preview.clickable {
  cursor: pointer;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: var(--transition);
  border-radius: 50%;
}

.avatar-preview-container:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay-text {
  color: white;
  font-size: 12px;
  text-align: center;
  font-weight: 500;
}

/* 头像上传模态框 */
.avatar-upload-modal {
  width: 90vw;
  max-width: 500px;
  max-height: 80vh;
}

.current-avatar-preview {
  text-align: center;
  margin-bottom: 24px;
}

.current-avatar-img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border-color);
  margin-bottom: 8px;
}

.current-avatar-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.upload-method-selection h4 {
  font-size: 16px;
  color: var(--text-primary);
  margin-bottom: 16px;
  text-align: center;
}

.upload-methods {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.upload-method {
  padding: 20px;
  border: 2px solid var(--border-color);
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: var(--transition);
  text-align: center;
  background: var(--background-tertiary);
}

.upload-method:hover {
  border-color: var(--primary-color);
  background: var(--background-card);
}

.method-icon {
  font-size: 32px;
  margin-bottom: 12px;
}

.method-info h5 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.method-info p {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}

/* 文件上传区域 */
.file-upload-area {
  margin-top: 16px;
}

.file-drop-zone {
  border: 2px dashed var(--border-color);
  border-radius: var(--border-radius);
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: var(--transition);
  background: var(--background-tertiary);
}

.file-drop-zone:hover {
  border-color: var(--primary-color);
  background: var(--background-card);
}

.drop-zone-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.drop-zone-icon {
  font-size: 48px;
  opacity: 0.6;
}

.drop-zone-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
}

.drop-zone-hint {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}

.upload-progress {
  margin-top: 16px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: var(--background-tertiary);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: var(--primary-color);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
  margin: 0;
}

/* URL上传区域 */
.url-upload-area {
  margin-top: 16px;
}

.url-preview {
  margin-top: 16px;
  text-align: center;
}

.url-preview-img {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border-color);
  margin-bottom: 8px;
}

.url-preview-status {
  font-size: 12px;
  margin: 0;
}

.url-preview-status.error {
  color: var(--danger-color);
}

.url-preview-status.success {
  color: var(--success-color);
}

/* 申请列表 */
.apply-list {
  max-height: 300px;
  overflow-y: auto;
}

.apply-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  margin-bottom: 12px;
  background-color: var(--background-tertiary);
}

.apply-item:last-child {
  margin-bottom: 0;
}

.apply-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.apply-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.apply-details h4 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #dfdfdf;
}

.apply-details p {
  margin: 0;
  font-size: 12px;
  color: #dfdfdf;
}

.apply-time {
  font-size: 11px;
  color: #999999;
  margin-top: 2px;
  display: block;
}

.no-apply {
  text-align: center;
  padding: 40px 20px;
  color: #999999;
}

.no-apply p {
  margin: 0;
  font-size: 14px;
}

.apply-actions {
  display: flex;
  gap: 8px;
}

/* 移动端模态框适配 */
@media (max-width: 768px) {
  .modal-content {
    width: 95%;
    margin: 20px;
  }
  
  .modal-header,
  .modal-body,
  .modal-footer {
    padding-left: 16px;
    padding-right: 16px;
  }
  
  .avatar-upload {
    flex-direction: column;
    align-items: center;
  }
  
  .avatar-upload-modal {
    width: 95vw;
  }
  
  .upload-methods {
    grid-template-columns: 1fr;
  }
  
  .avatar-preview {
    width: 80px;
    height: 80px;
  }
  
  .file-drop-zone {
    padding: 30px 15px;
  }
  
  .drop-zone-icon {
    font-size: 36px;
  }
  
  .radio-group {
    flex-direction: column;
    gap: 12px;
  }
}

/* 收件箱标签页样式 */
.inbox-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  border-bottom: 2px solid #3a3a3a;
}

.inbox-tab {
  padding: 12px 24px;
  cursor: pointer;
  color: #999999;
  font-size: 15px;
  font-weight: 500;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.inbox-tab:hover {
  color: #b3b3b3;
}

.inbox-tab.active {
  color: #409EFF;
  border-bottom-color: #409EFF;
}

/* 联系人标签页样式 */
.contact-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  border-bottom: 2px solid #3a3a3a;
}

.contact-tab {
  padding: 12px 24px;
  cursor: pointer;
  color: #999999;
  font-size: 15px;
  font-weight: 500;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.contact-tab:hover {
  color: #b3b3b3;
}

.contact-tab.active {
  color: #409EFF;
  border-bottom-color: #409EFF;
}

.tab-count {
  background: #3a3a3a;
  color: #ffffff;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  min-width: 20px;
  text-align: center;
}

.tab-count.pending {
  background: #409EFF;
}

/* 拉黑列表样式 */
.blacklist-section {
  margin-top: 20px;
}

.empty-blacklist {
  text-align: center;
  padding: 60px 20px;
  color: #666;
  font-size: 14px;
}

.blacklist-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 16px;
  padding: 16px;
  background-color: var(--background-card);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color);
  transition: var(--transition);
  box-shadow: var(--shadow-sm);
  align-items: center;
  margin-bottom: 12px;
}

.blacklist-item:hover {
  border-color: #666;
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.blacklist-time {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.unblock-btn {
  background-color: #4CAF50;
  color: white;
}

.unblock-btn:hover {
  background-color: #45a049;
  transform: translateY(-1px);
}

/* 收件箱样式 */
.inbox-section {
  background: #2a2a2a;
  border-radius: 12px;
  padding: 20px;
}

.inbox-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #3a3a3a;
}

.inbox-header h3 {
  margin: 0;
  font-size: 18px;
  color: #ffffff;
}

.inbox-count {
  background: #409EFF;
  color: #ffffff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.apply-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.apply-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #1a1a1a;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.apply-item:hover {
  background: #252525;
  transform: translateX(5px);
}

.apply-item.apply-processed {
  opacity: 0.7;
}

.apply-item.apply-processed:hover {
  opacity: 0.85;
}

.apply-avatar {
  flex-shrink: 0;
}

.apply-avatar img {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #3a3a3a;
}

.apply-info {
  flex: 1;
  min-width: 0;
}

.apply-info h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #ffffff;
  font-weight: 600;
}

.apply-email {
  margin: 0 0 5px 0;
  font-size: 13px;
  color: #999999;
}

.apply-time {
  margin: 0;
  font-size: 12px;
  color: #666666;
}

.apply-status {
  flex-shrink: 0;
  margin-right: 10px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.pending-badge {
  background: #409EFF;
  color: #ffffff;
}

.accepted-badge {
  background: #67c23a;
  color: #ffffff;
}

.rejected-badge {
  background: #f56c6c;
  color: #ffffff;
}

.blacklist-badge {
  background: #909399;
  color: #ffffff;
}

.apply-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.apply-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.accept-btn {
  background: #67c23a;
  color: #ffffff;
}

.accept-btn:hover {
  background: #85ce61;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(103, 194, 58, 0.3);
}

.reject-btn {
  background: #f56c6c;
  color: #ffffff;
}

.reject-btn:hover {
  background: #f78989;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(245, 108, 108, 0.3);
}

.inbox-empty {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  width: 120px;
  height: 120px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.inbox-empty p {
  color: #999999;
  font-size: 16px;
  margin: 0;
}

/* 移动端收件箱样式 */
@media (max-width: 768px) {
  .inbox-section {
    padding: 15px;
  }
  
  .apply-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .apply-avatar img {
    width: 40px;
    height: 40px;
  }
  
  .apply-actions {
    width: 100%;
  }
  
  .apply-btn {
    flex: 1;
    padding: 10px;
  }
}

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

/* 即时会议邀请样式 */
.instant-meeting-invite {
  margin-top: 8px;
}

.meeting-details {
  background: #1a1a1a;
  border-radius: 6px;
  padding: 12px;
  margin: 12px 0;
}

.meeting-detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.meeting-detail-item:last-child {
  margin-bottom: 0;
}

.detail-label {
  color: #999999;
  font-size: 13px;
  min-width: 80px;
}

.detail-value {
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
}

.detail-value.password {
  font-family: monospace;
  background: #2a2a2a;
  padding: 2px 8px;
  border-radius: 4px;
}

.btn-join-meeting {
  width: 100%;
  padding: 10px;
  background: #999999;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-join-meeting:hover {
  background: #aaaaaa;
}

.btn-join-meeting.disabled,
.btn-accept.disabled {
  background: #64748b !important;
  cursor: not-allowed !important;
  box-shadow: none !important;
  opacity: 0.78;
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

.dashboard-container {
  background:
    radial-gradient(circle at top left, rgba(45, 212, 191, 0.12), transparent 30%),
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.12), transparent 28%),
    linear-gradient(180deg, #020617 0%, #0f172a 48%, #111827 100%) !important;
}

.desktop-sidebar {
  background: rgba(15, 23, 42, 0.84) !important;
  backdrop-filter: blur(16px);
  border-right: 1px solid rgba(148, 163, 184, 0.12);
  box-shadow: 18px 0 48px rgba(2, 6, 23, 0.22);
}

.mobile-header,
.mobile-nav {
  background: rgba(15, 23, 42, 0.88) !important;
  backdrop-filter: blur(16px);
}

.main-content {
  padding: 28px 28px 36px !important;
}

.content-page {
  background: rgba(15, 23, 42, 0.48) !important;
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 32px;
  padding: 28px !important;
  box-shadow: 0 24px 64px rgba(2, 6, 23, 0.2);
}

.page-title {
  font-size: 32px !important;
  font-weight: 800 !important;
  color: #f8fafc !important;
  letter-spacing: -0.02em;
  margin-bottom: 22px !important;
}

.meeting-features {
  gap: 18px !important;
}

.feature-card,
.current-meeting-card,
.all-meetings-section,
.reservation-section,
.user-profile,
.contact-list,
.contact-notifications,
.my-apply-section,
.blacklist-section,
.inbox-section {
  background:
    radial-gradient(circle at top right, rgba(56, 189, 248, 0.12), transparent 32%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.82), rgba(15, 23, 42, 0.96)) !important;
  border: 1px solid rgba(148, 163, 184, 0.12) !important;
  border-radius: 28px !important;
  box-shadow: 0 18px 44px rgba(2, 6, 23, 0.22);
}

.feature-card {
  padding: 24px !important;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.feature-card:hover,
.current-meeting-card:hover,
.meeting-preview-item:hover,
.contact-item:hover,
.blacklist-item:hover,
.notification-item:hover {
  transform: translateY(-3px);
  border-color: rgba(56, 189, 248, 0.24) !important;
  box-shadow: 0 24px 56px rgba(2, 6, 23, 0.28);
}

.feature-card h3,
.current-meeting-card h4,
.all-meetings-header h3,
.reservation-header h3,
.notification-title,
.contact-info h4,
.profile-info .info-item span,
.section-header h3,
.notification-header h3 {
  color: #f8fafc !important;
}

.feature-card p,
.meeting-meta p,
.meeting-preview-info p,
.contact-status-text,
.contact-last-time,
.blacklist-time,
.notification-text,
.profile-info label,
.info-item label,
.empty-meetings p,
.empty-contacts p,
.empty-blacklist p,
.inbox-empty p {
  color: rgba(226, 232, 240, 0.66) !important;
}

.feature-button,
.action-button,
.contact-action-btn,
.edit-profile-button,
.apply-btn,
.btn-join-meeting,
.view-all-btn {
  border-radius: 999px !important;
  font-weight: 700 !important;
  box-shadow: 0 12px 26px rgba(2, 132, 199, 0.16);
}

.feature-button,
.action-button.join-current,
.edit-profile-button,
.view-all-btn,
.btn-join-meeting,
.btn-accept {
  background: linear-gradient(135deg, #14b8a6, #0284c7) !important;
  border: none !important;
  color: #fff !important;
}

.action-button.finish-current,
.reject-btn,
.contact-action-btn.delete-btn {
  background: linear-gradient(135deg, #ef4444, #dc2626) !important;
  color: #fff !important;
}

.action-button.leave-current,
.contact-action-btn.blacklist-btn,
.contact-tab.active,
.inbox-tab.active {
  background: rgba(56, 189, 248, 0.12) !important;
  color: #e0f2fe !important;
}

.meeting-status-indicator,
.meeting-detail-item,
.notification-icon,
.status-badge,
.role-badge,
.tab-count,
.tab-badge,
.nav-badge,
.mobile-nav-badge {
  border-radius: 999px !important;
}

.meeting-meta p,
.notification-content,
.contact-item,
.blacklist-item,
.my-apply-item,
.apply-item,
.meeting-preview-item {
  border: 1px solid rgba(148, 163, 184, 0.1);
  background: rgba(255, 255, 255, 0.04) !important;
  border-radius: 18px;
}

.meeting-meta p {
  padding: 10px 12px;
}

.contact-item,
.blacklist-item,
.my-apply-item,
.apply-item,
.meeting-preview-item {
  padding: 14px !important;
}

.profile-avatar,
.user-avatar,
.mobile-user-avatar {
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.26);
  border: 3px solid rgba(255, 255, 255, 0.14);
}

.contact-tabs,
.inbox-tabs {
  border-bottom-color: rgba(148, 163, 184, 0.14) !important;
}

.contact-tab,
.inbox-tab {
  border-radius: 14px 14px 0 0;
  color: rgba(226, 232, 240, 0.6) !important;
}

.category-filter,
.form-input,
.search-input {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(148, 163, 184, 0.16) !important;
  border-radius: 16px !important;
  color: #f8fafc !important;
}

.category-filter:hover,
.form-input:hover,
.search-input:hover {
  border-color: rgba(56, 189, 248, 0.26) !important;
}

@media (max-width: 768px) {
  .main-content {
    padding: 16px 14px 24px !important;
  }

  .content-page {
    padding: 18px !important;
    border-radius: 24px;
  }

  .page-title {
    font-size: 26px !important;
  }
}

/* const refreshInstantInviteMeetingStatuses = async (notifications = []) => {
  const meetingIds = [...new Set(
    notifications
      .filter(notification => notification.notificationType === 10)
      .map(notification => parseMeetingInvite(notification.content).meetingId)
      .filter(Boolean)
  )]

  if (meetingIds.length === 0) {
    return
  }

  const statusEntries = await Promise.all(
    meetingIds.map(async (meetingId) => {
      try {
        const response = await meetingService.getMeetingStatus(meetingId)
        const ended = response?.data?.code === 200 ? !!response.data.data?.ended : true
        return [meetingId, ended]
      } catch (error) {
        console.error('获取即时会议状态失败:', meetingId, error)
        return [meetingId, true]
      }
    })
  )

  instantInviteMeetingStatusMap.value = {
    ...instantInviteMeetingStatusMap.value,
    ...Object.fromEntries(statusEntries)
  }
} */
</style>
