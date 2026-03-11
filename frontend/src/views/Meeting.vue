<template>
  <div class="meeting-container" :class="{ 'screen-sharing-mode': isScreenSharing && currentScreenSharingUserId === currentUserId }">
    <!-- 正常会议界面 -->
    <div v-show="!(isScreenSharing && currentScreenSharingUserId === currentUserId)" class="normal-meeting-view">
    <!-- 会议顶部栏 -->
    <div class="meeting-header">
      <!-- 左上角：会议详情按钮 -->
      <div class="meeting-info-section">
        <button class="meeting-info-btn" @click="showMeetingInfo">
          <span class="meeting-info-text">会议详情</span>
        </button>
      </div>

      <!-- 右上角：窗口控制按钮 -->
      <div class="window-controls">
        <button class="window-btn settings-btn" @click="showSettingsModal = true" title="设置">
          <img src="/meeting-icons/设置.svg" alt="设置" class="window-icon">
        </button>
        <button class="window-btn minimize-btn" @click="minimizeWindow" title="最小化">
          <span class="window-icon">−</span>
        </button>
        <button class="window-btn fullscreen-btn" @click="toggleFullscreen" title="全屏">
          <img v-if="!isFullscreen" src="/meeting-icons/进入全屏.svg" alt="全屏" class="window-icon">
          <span v-else class="window-icon">⧉</span>
        </button>
        <button class="window-btn close-btn" @click="closeMeeting" title="关闭">
          <span class="window-icon">×</span>
        </button>
      </div>
    </div>

    <!-- 会议主体区域 -->
    <div class="meeting-body">
      <!-- 视频区域 -->
      <div class="video-area">
        <!-- 屏幕共享观看视图（当其他人共享屏幕时显示） -->
        <div v-if="currentScreenSharingUserId && currentScreenSharingUserId !== currentUserId" class="screen-share-view">
          <!-- 屏幕共享画面 -->
          <video 
            ref="remoteScreenShareVideo"
            autoplay 
            playsinline
            class="screen-share-video">
          </video>
          
          <!-- 画中画：共享者的摄像头窗口（可拖动） -->
          <div 
            v-if="sharingUserVideoStream"
            ref="pipSharingUserCamera"
            class="pip-camera"
            :style="{ left: pipPosition.x + 'px', top: pipPosition.y + 'px' }"
            @mousedown="startDrag">
            <video 
              ref="pipSharingUserVideo"
              autoplay 
              playsinline
              class="pip-camera-video">
            </video>
            <div class="pip-camera-info">
              <span class="pip-camera-name">{{ sharingUserName }}</span>
            </div>
          </div>

          <!-- 右上角成员视频面板（可拖动） -->
          <div class="viewer-video-panel" :style="{ left: viewerPanelPosition.x + 'px', top: viewerPanelPosition.y + 'px' }">
            <div class="viewer-panel-header" @mousedown="startDragViewerPanel">
              <span class="viewer-panel-title">会议成员 ({{ allParticipants.length }})</span>
              <div class="viewer-panel-controls">
                <button 
                  class="viewer-panel-mode-btn" 
                  @click="toggleViewerDisplayMode"
                  :title="viewerDisplayMode === 'self' ? '显示全部成员' : '只显示自己'">
                  {{ viewerDisplayMode === 'self' ? '👤' : '👥' }}
                </button>
                <button class="viewer-panel-toggle-btn" @click="toggleViewerPanel">
                  {{ viewerPanelExpanded ? '−' : '+' }}
                </button>
              </div>
            </div>
            
            <div v-if="viewerPanelExpanded" class="viewer-panel-content">
              <!-- 只显示自己模式 -->
              <template v-if="viewerDisplayMode === 'self'">
                <div class="viewer-video-item my-video">
                  <video v-if="isVideoOn" ref="viewerLocalVideo" autoplay muted playsinline class="viewer-video-element"></video>
                  <div v-else-if="showCameraUnavailableCard" :class="['camera-fallback-card', 'viewer-camera-fallback', `camera-state-${cameraUnavailableType}`]">
                    <div class="camera-fallback-icon">{{ cameraUnavailableIcon }}</div>
                    <div class="camera-fallback-title">{{ cameraUnavailableShortTitle }}</div>
                  </div>
                  <img v-else :src="userAvatar" alt="我" class="viewer-avatar-element">
                  <div class="viewer-video-overlay">
                    <span class="viewer-video-name">我</span>
                    <button class="viewer-audio-btn" :class="{ muted: isMuted }" @click="toggleMute">
                      {{ isMuted ? '🔇' : '🎤' }}
                    </button>
                  </div>
                </div>
              </template>
              
              <!-- 显示全部成员模式 -->
              <template v-else>
                <!-- 自己的视频 -->
                <div class="viewer-video-item my-video">
                  <video v-if="isVideoOn" ref="viewerLocalVideoAll" autoplay muted playsinline class="viewer-video-element"></video>
                  <div v-else-if="showCameraUnavailableCard" :class="['camera-fallback-card', 'viewer-camera-fallback', `camera-state-${cameraUnavailableType}`]">
                    <div class="camera-fallback-icon">{{ cameraUnavailableIcon }}</div>
                    <div class="camera-fallback-title">{{ cameraUnavailableShortTitle }}</div>
                  </div>
                  <img v-else :src="userAvatar" alt="我" class="viewer-avatar-element">
                  <div class="viewer-video-overlay">
                    <span class="viewer-video-name">我</span>
                    <button class="viewer-audio-btn" :class="{ muted: isMuted }" @click="toggleMute">
                      {{ isMuted ? '🔇' : '🎤' }}
                    </button>
                  </div>
                </div>
                
                <!-- 其他成员视频 -->
                <div v-for="participant in participants" :key="participant.userId" class="viewer-video-item">
                  <video 
                    v-if="participant.videoOpen"
                    :ref="el => setViewerParticipantVideoRef(participant.userId, el)"
                    autoplay 
                    playsinline
                    class="viewer-video-element">
                  </video>
                  <img v-else :src="participant.avatar" :alt="participant.name" class="viewer-avatar-element">
                  <div class="viewer-video-overlay">
                    <span class="viewer-video-name">{{ participant.name }}</span>
                    <span class="viewer-audio-status" :class="{ muted: participant.isMuted }">
                      {{ participant.isMuted ? '🔇' : '🎤' }}
                    </span>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>
        
        <!-- 参与者网格（正常视图） -->
        <div v-else class="participants-grid" :class="getGridClass()">
          <!-- 当前用户 -->
          <div class="participant-video-item">
            <div class="video-frame">
              <!-- 视频流或头像 -->
              <video 
                v-if="isVideoOn" 
                ref="localVideo"
                autoplay 
                muted 
                playsinline
                class="participant-video"
                :style="{ transform: settingsManager.shouldMirrorVideo() ? 'scaleX(-1)' : 'none' }"
                @loadedmetadata="() => console.log('✅ 视频元数据已加载')"
                @canplay="() => console.log('✅ 视频可以播放')">
              </video>
              <div v-else-if="showCameraUnavailableCard" :class="['camera-fallback-card', 'participant-camera-fallback', `camera-state-${cameraUnavailableType}`]">
                <div class="camera-fallback-badge">{{ cameraUnavailableBadge }}</div>
                <div class="camera-fallback-icon">{{ cameraUnavailableIcon }}</div>
                <div class="camera-fallback-title">{{ cameraUnavailableTitle }}</div>
                <p class="camera-fallback-text">{{ cameraUnavailableMessage }}</p>
                <div class="camera-fallback-actions">
                  <button class="camera-fallback-btn primary" @click="retryEnableCamera">打开摄像头</button>
                  <button class="camera-fallback-btn" @click="dismissCameraUnavailablePrompt">稍后再说</button>
                </div>
              </div>
              <img 
                v-else
                :src="userAvatar" 
                alt="我的头像" 
                class="participant-avatar-large">
              <div class="participant-info-overlay">
                <span class="participant-name">{{ userName }} (我)</span>
                <div class="participant-status">
                  <span v-if="isMuted" class="status-icon muted">🔇</span>
                  <span v-if="!isVideoOn" class="status-icon video-off">📹</span>
                  <span v-if="isScreenSharing" class="status-icon screen-sharing">🖥️</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 其他参与者 -->
          <div v-for="participant in participants" 
               :key="participant.userId" 
               class="participant-video-item">
            <div class="video-frame">
              <!-- 如果视频开启，显示video元素；否则显示头像 -->
              <video 
                v-if="participant.videoOpen"
                :ref="el => { if (el) participant.videoRef = el }"
                autoplay 
                playsinline
                class="participant-video">
              </video>
              <img 
                v-else
                :src="participant.avatar" 
                :alt="participant.name" 
                class="participant-avatar-large">
              <div class="participant-info-overlay">
                <span class="participant-name">
                  {{ participant.name }}
                  <span v-if="participant.isHost" class="host-badge">主持人</span>
                </span>
                <div class="participant-status">
                  <span v-if="participant.isMuted" class="status-icon muted">🔇</span>
                  <span v-if="!participant.videoOpen" class="status-icon video-off">📹</span>
                  <span v-if="participant.userId === currentScreenSharingUserId" class="status-icon screen-sharing">🖥️</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 会议底部控制栏 -->
    <div class="meeting-primary-toolbar">
      <button class="primary-toolbar-btn" :class="{ active: isMuted }" @click="toggleMute">
        <span class="primary-toolbar-icon" aria-hidden="true">{{ isMuted ? '🔇' : '🎤' }}</span>
        <span class="primary-toolbar-text">{{ isMuted ? '解除静音' : '静音' }}</span>
      </button>
      <button class="primary-toolbar-btn" :class="{ active: !isVideoOn }" @click="toggleVideo">
        <span class="primary-toolbar-icon" aria-hidden="true">{{ isVideoOn ? '📹' : '🚫' }}</span>
        <span class="primary-toolbar-text">{{ isVideoOn ? '关闭视频' : '开启视频' }}</span>
      </button>
      <button class="primary-toolbar-btn" :class="{ active: isScreenSharing }" @click="shareScreen">
        <span class="primary-toolbar-icon" aria-hidden="true">{{ isScreenSharing ? '🛑' : '🖥️' }}</span>
        <span class="primary-toolbar-text">{{ isScreenSharing ? '停止共享' : '共享屏幕' }}</span>
      </button>
    </div>

    <div class="meeting-footer">
      <!-- 音频控制 -->
      <button class="control-button" :class="{ active: isMuted }" @click="toggleMute">
        <img v-if="isMuted" src="/meeting-icons/静音.svg" alt="静音" class="control-icon">
        <img v-else src="/meeting-icons/解除静音.svg" alt="解除静音" class="control-icon">
        <span class="control-text">{{ isMuted ? '解除静音' : '静音' }}</span>
      </button>

      <!-- 视频控制 -->
      <button class="control-button" :class="{ active: !isVideoOn }" @click="toggleVideo">
        <img v-if="isVideoOn" src="/meeting-icons/开启视频.svg" alt="开启视频" class="control-icon">
        <img v-else src="/meeting-icons/关闭视频.svg" alt="关闭视频" class="control-icon">
        <span class="control-text">{{ isVideoOn ? '关闭视频' : '开启视频' }}</span>
      </button>

      <!-- 共享屏幕 -->
      <button class="control-button" :class="{ active: isScreenSharing }" @click="shareScreen">
        <img src="/meeting-icons/共享屏幕.svg" alt="共享屏幕" class="control-icon">
        <span class="control-text">{{ isScreenSharing ? '停止共享' : '共享屏幕' }}</span>
      </button>

      <!-- 邀请 -->
      <button class="control-button" @click="inviteParticipants">
        <img src="/meeting-icons/邀请.svg" alt="邀请" class="control-icon">
        <span class="control-text">邀请</span>
      </button>

      <!-- 成员 -->
      <button class="control-button" @click="showParticipants">
        <img src="/meeting-icons/成员.svg" alt="成员" class="control-icon">
        <span class="control-text">成员</span>
      </button>

      <!-- 聊天 -->
      <button class="control-button" @click="showChat">
        <img src="/meeting-icons/聊天.svg" alt="聊天" class="control-icon">
        <span class="control-text">聊天</span>
        <span v-if="unreadMessageCount > 0" class="unread-badge">{{ unreadMessageCount > 99 ? '99+' : unreadMessageCount }}</span>
      </button>

      <!-- 离开会议 -->
      <button class="control-button leave-btn" @click="leaveMeeting">
        <span class="control-icon">🚪</span>
        <span class="control-text">离开会议</span>
      </button>

      <!-- 结束会议（仅主持人） -->
      <button v-if="isHost" class="control-button end-meeting-btn" @click="endMeeting">
        <span class="control-icon">🛑</span>
        <span class="control-text">结束会议</span>
      </button>
    </div>

    <!-- 会议详情浮框 -->
    <div v-if="showMeetingInfoModal" class="floating-modal meeting-info-modal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>会议详情</h3>
          <button class="modal-close" @click="showMeetingInfoModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="info-item">
            <label>会议名称：</label>
            <span>{{ meetingName || '快速会议' }}</span>
          </div>
          <div class="info-item">
            <label>会议号：</label>
            <div class="meeting-no-container">
              <input type="text" :value="meetingNo || '未知'" readonly class="meeting-no-input" @click="selectMeetingNo">
              <button class="copy-btn" @click="copyMeetingNo" title="复制会议号">📋</button>
            </div>
          </div>
          <div class="info-item">
            <label>发起人（主持人）：</label>
            <span>{{ hostName || '未知' }}</span>
          </div>
          <div class="info-item">
            <label>我的名称：</label>
            <span>{{ userName || '用户' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 设置浮框 -->
    <div v-if="showSettingsModal" class="floating-modal settings-modal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>设置</h3>
          <button class="modal-close" @click="showSettingsModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="settings-menu">
            <button class="settings-item" :class="{ active: activeSettingsTab === 'general' }" @click="activeSettingsTab = 'general'">
              常规设置
            </button>
            <button class="settings-item" :class="{ active: activeSettingsTab === 'shortcuts' }" @click="activeSettingsTab = 'shortcuts'">
              快捷键
            </button>
            <button class="settings-item" :class="{ active: activeSettingsTab === 'about' }" @click="activeSettingsTab = 'about'">
              关于我们
            </button>
          </div>
          <div class="settings-content">
            <!-- 常规设置 -->
            <div v-if="activeSettingsTab === 'general'" class="settings-panel">
              <h4>常规设置</h4>
              <div class="setting-option">
                <label>
                  <input type="checkbox" v-model="settings.autoMute">
                  加入会议时自动静音
                </label>
              </div>
              <div class="setting-option">
                <label>
                  <input type="checkbox" v-model="settings.autoVideo">
                  加入会议时自动开启视频
                </label>
              </div>
              <div class="setting-option">
                <label>
                  <input type="checkbox" v-model="settings.showNotifications">
                  显示会议通知
                </label>
              </div>
            </div>
            
            <!-- 快捷键 -->
            <div v-if="activeSettingsTab === 'shortcuts'" class="settings-panel">
              <h4>快捷键</h4>
              <div class="shortcut-list">
                <div class="shortcut-item">
                  <span class="shortcut-desc">静音/解除静音</span>
                  <span class="shortcut-key">Ctrl + M</span>
                </div>
                <div class="shortcut-item">
                  <span class="shortcut-desc">开启/关闭视频</span>
                  <span class="shortcut-key">Ctrl + V</span>
                </div>
                <div class="shortcut-item">
                  <span class="shortcut-desc">共享屏幕</span>
                  <span class="shortcut-key">Ctrl + S</span>
                </div>
                <div class="shortcut-item">
                  <span class="shortcut-desc">显示/隐藏聊天</span>
                  <span class="shortcut-key">Ctrl + H</span>
                </div>
                <div class="shortcut-item">
                  <span class="shortcut-desc">离开会议</span>
                  <span class="shortcut-key">Ctrl + L</span>
                </div>
              </div>
            </div>
            
            <!-- 关于我们 -->
            <div v-if="activeSettingsTab === 'about'" class="settings-panel">
              <h4>关于我们</h4>
              <div class="about-content">
                <p><strong>EasyMeeting</strong></p>
                <p>版本：1.0.0</p>
                <p>一个简单易用的在线会议系统</p>
                <p>支持多人视频通话、屏幕共享、实时聊天等功能</p>
                <p class="copyright">© 2026 EasyMeeting. All rights reserved.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 成员列表模态框 -->
    <div v-if="showParticipantsModal" class="modal-overlay" @click="showParticipantsModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>会议成员 ({{ allParticipants.length }})</h3>
          <button class="modal-close" @click="showParticipantsModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="participant-list">
            <div v-for="participant in allParticipants" :key="participant.userId" class="participant-item">
              <img :src="participant.avatar" alt="参与者头像" class="participant-avatar">
              <div class="participant-info">
                <h4>{{ participant.name }}
                  <span v-if="participant.isHost" class="participant-badge">主持人</span>
                  <span v-if="participant.isCurrentUser" class="participant-badge" style="background: #2196F3;">我</span>
                </h4>
                <p class="participant-status">{{ participant.isMuted ? '已静音' : '正常' }}</p>
              </div>
              <div v-if="isHost && !participant.isHost && !participant.isCurrentUser" class="participant-actions">
                <button class="action-button kick" @click="kickOutParticipant(participant.userId)">
                  踢出
                </button>
                <button class="action-button black" @click="blackParticipant(participant.userId)">
                  拉黑
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 邀请成员模态框 -->
    <InviteMemberModal
      v-if="showInviteModal"
      :meeting-id="meetingId"
      :meeting-name="meetingName"
      :current-members="allParticipants"
      @close="showInviteModal = false"
      @invite="handleInviteUser"
    />

    <!-- 聊天模态框 -->
    <div v-if="showChatModal" class="modal-overlay" @click="showChatModal = false">
      <div class="modal-content chat-modal-content" @click.stop>
        <div class="modal-header">
          <h3>聊天</h3>
          <button class="modal-close" @click="showChatModal = false">&times;</button>
        </div>
        <div class="modal-body chat-modal-body">
          <!-- 时间线选择器 -->
          <div class="chat-timeline-selector">
            <label>查看消息时间范围：</label>
            <select v-model="selectedTimeRange" @change="onTimeRangeChange" class="time-range-select">
              <option value="all">所有消息</option>
              <option value="5m">最近5分钟</option>
              <option value="15m">最近15分钟</option>
              <option value="30m">最近30分钟</option>
              <option value="1h">最近1小时</option>
              <option value="2h">最近2小时</option>
              <option value="6h">最近6小时</option>
              <option value="12h">最近12小时</option>
              <option value="24h">最近24小时</option>
            </select>
            <button @click="refreshMessages" class="refresh-button">刷新</button>
          </div>
          
          <div class="chat-messages">
            <template v-for="(group, index) in groupedChatMessages" :key="index">
              <!-- 日期分隔符 -->
              <div class="date-divider">
                <span class="date-text">{{ group.dateLabel }}</span>
              </div>
              
              <!-- 该日期下的消息 -->
              <div v-for="message in group.messages" :key="message.id" 
                   class="chat-message" 
                   :class="{ 
                     'private-message': message.isPrivate,
                     'my-message': message.sendUserId === currentUserId
                   }">
                <img :src="message.avatar" alt="发送者头像" class="message-avatar">
                <div class="message-content">
                  <div class="message-header">
                    <h4>{{ message.sender }}</h4>
                    <span v-if="message.isPrivate" class="private-badge">私聊</span>
                    <span v-if="message.receiveUserName && message.isPrivate" class="receive-info">
                      → {{ message.receiveUserName }}
                    </span>
                  </div>
                  <p class="message-text">{{ message.text }}</p>
                  <span class="message-time">{{ message.time }}</span>
                </div>
              </div>
            </template>
          </div>
          <div class="chat-input-area">
            <button class="emoji-btn" @click="showEmojiPicker = !showEmojiPicker" title="表情">
              😀
            </button>
            <input type="text" v-model="chatInput" placeholder="输入消息..." class="chat-input" @keyup.enter="sendMessage">
            <button class="send-button" @click="sendMessage">发送</button>
            
            <!-- 表情选择器 -->
            <div v-if="showEmojiPicker" class="emoji-picker">
              <div class="emoji-picker-header">
                <span>选择表情</span>
                <button class="emoji-close-btn" @click="showEmojiPicker = false">×</button>
              </div>
              <div class="emoji-grid">
                <button 
                  v-for="(emoji, index) in emojiList" 
                  :key="index"
                  class="emoji-item"
                  @click="insertEmoji(emoji)">
                  {{ emoji }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 摄像头选择模态框 -->
    <div v-if="showCameraSelectModal" class="modal-overlay" @click="cancelCameraSelection">
      <div class="modal-content camera-select-modal" @click.stop>
        <div class="modal-header">
          <h3>选择摄像头</h3>
          <button class="modal-close" @click="cancelCameraSelection">&times;</button>
        </div>
        <div class="modal-body">
          <p class="camera-select-hint">检测到多个摄像头设备，请选择要使用的摄像头：</p>
          <div class="camera-list">
            <div 
              v-for="camera in availableCameras" 
              :key="camera.deviceId"
              class="camera-item"
              :class="{ selected: selectedCameraId === camera.deviceId }"
              @click="selectedCameraId = camera.deviceId">
              <div class="camera-icon">📹</div>
              <div class="camera-info">
                <div class="camera-name">{{ camera.label }}</div>
                <div class="camera-id">{{ camera.deviceId.substring(0, 20) }}...</div>
              </div>
              <div class="camera-check" v-if="selectedCameraId === camera.deviceId">✓</div>
            </div>
          </div>
          <div class="camera-select-actions">
            <button class="btn-cancel" @click="cancelCameraSelection">取消</button>
            <button class="btn-confirm" @click="confirmCameraSelection">确认</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 屏幕共享选项对话框 -->
    <div v-if="showScreenShareOptions" class="modal-overlay" @click="showScreenShareOptions = false">
      <div class="modal-content screen-share-options-modal" @click.stop>
        <div class="modal-header">
          <h3>屏幕共享选项</h3>
          <button class="modal-close" @click="showScreenShareOptions = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="option-item">
            <label class="option-label">
              <input type="checkbox" v-model="screenShareOptions.shareAudio" class="option-checkbox">
              <span class="option-text">同时共享电脑声音</span>
            </label>
            <p class="option-hint">允许其他参与者听到您电脑播放的声音</p>
          </div>
          <div class="option-item">
            <label class="option-label">
              <input type="checkbox" v-model="screenShareOptions.showPip" class="option-checkbox">
              <span class="option-text">人像画中画</span>
            </label>
            <p class="option-hint">在共享屏幕时显示您的摄像头画面</p>
          </div>
          <div class="option-actions">
            <button class="btn-cancel" @click="showScreenShareOptions = false">取消</button>
            <button class="btn-confirm" @click="startScreenShare">开始共享</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 结束正常会议视图 -->
    </div>

    <!-- 屏幕共享悬浮层（共享者视角） -->
    <div v-if="isScreenSharing && currentScreenSharingUserId === currentUserId" class="screen-share-overlay">
      <!-- 顶部控制条 -->
      <div class="overlay-control-bar">
        <div class="bar-left">
          <span class="sharing-indicator">
            <span class="recording-dot"></span>
            正在共享屏幕
          </span>
          <span class="meeting-no-display">会议号: {{ meetingNo }}</span>
          <span class="meeting-time">{{ formattedDuration }}</span>
        </div>
        <div class="bar-center">
          <button class="overlay-btn" :class="{ active: isMuted }" @click="toggleMute" :title="isMuted ? '解除静音' : '静音'">
            <span class="btn-icon">{{ isMuted ? '🔇' : '🎤' }}</span>
          </button>
          <button class="overlay-btn" :class="{ active: !isVideoOn }" @click="toggleVideo" :title="isVideoOn ? '关闭视频' : '开启视频'">
            <span class="btn-icon">{{ isVideoOn ? '📹' : '🚫' }}</span>
          </button>
          <button class="overlay-btn" @click="showParticipants" title="成员">
            <span class="btn-icon">👥</span>
          </button>
          <button class="overlay-btn" @click="showChat" title="聊天">
            <span class="btn-icon">💬</span>
            <span v-if="unreadMessageCount > 0" class="unread-badge-overlay">{{ unreadMessageCount > 99 ? '99+' : unreadMessageCount }}</span>
          </button>
          <button class="overlay-btn" @click="switchScreenShareSource" title="切换共享源">
            <span class="btn-icon">🔄</span>
          </button>
          <button class="overlay-btn" :class="{ active: isScreenSharePaused }" @click="toggleScreenSharePause" :title="isScreenSharePaused ? '恢复共享' : '暂停共享'">
            <span class="btn-icon">{{ isScreenSharePaused ? '▶️' : '⏸️' }}</span>
          </button>
        </div>
        <div class="bar-right">
          <button class="overlay-btn stop-sharing-btn" @click="stopScreenShare">
            <span class="btn-icon">🛑</span>
            <span class="btn-text">结束共享</span>
          </button>
        </div>
      </div>

      <!-- 中间屏幕共享预览区域 -->
      <div class="screen-share-preview-area">
        <video 
          ref="localScreenSharePreview"
          autoplay 
          muted
          playsinline
          class="screen-share-preview-video">
        </video>
        <div class="preview-label">屏幕共享预览</div>
      </div>

      <!-- 右上角视频面板 -->
      <div class="video-overlay-panel" :style="{ left: videoPanelPosition.x + 'px', top: videoPanelPosition.y + 'px' }">
        <div class="panel-header" @mousedown="startDragVideoPanel">
          <span class="panel-title">会议成员 ({{ allParticipants.length }})</span>
          <button class="panel-toggle-btn" @click="toggleVideoPanel">
            {{ videoPanelExpanded ? '−' : '+' }}
          </button>
        </div>
        
        <div v-if="videoPanelExpanded" class="panel-content">
          <!-- 自己的视频 -->
          <div class="video-thumbnail my-video">
            <video v-if="isVideoOn" ref="localVideoOverlay" autoplay muted playsinline class="thumbnail-video"></video>
            <div v-else-if="showCameraUnavailableCard" :class="['camera-fallback-card', 'overlay-camera-fallback', `camera-state-${cameraUnavailableType}`]">
              <div class="camera-fallback-icon">{{ cameraUnavailableIcon }}</div>
              <div class="camera-fallback-title">{{ cameraUnavailableShortTitle }}</div>
            </div>
            <img v-else :src="userAvatar" alt="我" class="thumbnail-avatar">
            <div class="thumbnail-overlay">
              <span class="thumbnail-name">我</span>
              <button class="thumbnail-audio-btn" :class="{ muted: isMuted }" @click="toggleMute">
                {{ isMuted ? '🔇' : '🎤' }}
              </button>
            </div>
          </div>
          
          <!-- 其他成员视频 -->
          <div v-for="participant in participants" :key="participant.userId" class="video-thumbnail">
            <video v-if="participant.videoOpen" :ref="el => setOverlayParticipantVideoRef(participant.userId, el)" autoplay playsinline class="thumbnail-video"></video>
            <img v-else :src="participant.avatar" :alt="participant.name" class="thumbnail-avatar">
            <div class="thumbnail-overlay">
              <span class="thumbnail-name">{{ participant.name }}</span>
              <span class="thumbnail-audio-status" :class="{ muted: participant.isMuted }">
                {{ participant.isMuted ? '🔇' : '🎤' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 左下角聊天气泡面板 -->
      <div class="chat-bubble-panel" :style="{ left: chatPanelPosition.x + 'px', top: chatPanelPosition.y + 'px' }">
        <div class="chat-panel-header" @mousedown="startDragChatPanel">
          <span class="chat-panel-title">💬 聊天</span>
          <button class="chat-panel-toggle-btn" @click="toggleChatPanel">
            {{ chatPanelExpanded ? '−' : '+' }}
          </button>
        </div>
        
        <div v-if="chatPanelExpanded" class="chat-panel-content">
          <!-- 聊天消息列表 -->
          <div class="chat-panel-messages">
            <div v-for="message in recentChatMessages" :key="message.id" 
                 class="chat-panel-message"
                 :class="{ 'my-message': message.sendUserId === currentUserId }">
              <img :src="message.avatar" alt="头像" class="chat-message-avatar">
              <div class="chat-message-content">
                <div class="chat-message-header">
                  <span class="chat-message-sender">{{ message.sender }}</span>
                  <span class="chat-message-time">{{ message.time }}</span>
                </div>
                <div class="chat-message-text">{{ message.text }}</div>
              </div>
            </div>
          </div>
          
          <!-- 聊天输入区 -->
          <div class="chat-panel-input-area">
            <button class="emoji-btn-quick" @click="showQuickEmojiPicker = !showQuickEmojiPicker" title="表情">
              😀
            </button>
            <input 
              type="text" 
              v-model="quickChatInput" 
              placeholder="输入消息..." 
              class="chat-panel-input" 
              @keyup.enter="sendQuickMessage">
            <button class="chat-panel-send-btn" @click="sendQuickMessage">发送</button>
            
            <!-- 表情选择器 -->
            <div v-if="showQuickEmojiPicker" class="emoji-picker emoji-picker-quick">
              <div class="emoji-picker-header">
                <span>选择表情</span>
                <button class="emoji-close-btn" @click="showQuickEmojiPicker = false">×</button>
              </div>
              <div class="emoji-grid">
                <button 
                  v-for="(emoji, index) in emojiList" 
                  :key="index"
                  class="emoji-item"
                  @click="insertQuickEmoji(emoji)">
                  {{ emoji }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- AI助手面板 -->
      <AIAssistant 
        v-if="meetingId"
        :meeting-id="meetingId"
        :user-avatar="userAvatar"
        :user-name="userName" />
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { meetingService, chatService } from '../api/services'
import { meetingWsService, MemberType, MemberStatus, MessageType } from '../api/meeting-websocket'
import { wsService } from '../api/websocket'
import { webrtcManager } from '../api/webrtc-manager'
import InviteMemberModal from '../components/InviteMemberModal.vue'
import AIAssistant from '../components/AIAssistant.vue'
import { ElMessage } from 'element-plus'
import { settingsManager } from '@/utils/settings-manager.js'

const router = useRouter()
const route = useRoute()

const meetingId = computed(() => route.params.meetingId || route.query.meetingId || '')
const meetingName = ref('')
const meetingNo = ref('')
const userName = ref('')
const userAvatar = ref('')
const currentUserId = ref('')
const hostName = ref('')

// 会议状态（根据用户设置初始化）
const isMuted = ref(!settingsManager.shouldDefaultAudioOn())
const isVideoOn = ref(settingsManager.shouldDefaultVideoOn())
const isHost = ref(false)
const isFullscreen = ref(false)
const isScreenSharing = ref(false)  // 屏幕共享状态
const screenStream = ref(null)      // 屏幕共享流

// 视频流相关
const localStream = ref(null)
const localVideo = ref(null)

// 屏幕共享观看相关（观看者端）
const remoteScreenShareVideo = ref(null)  // 远程屏幕共享视频元素
const pipSharingUserVideo = ref(null)  // 画中画：共享者摄像头视频元素
const pipSharingUserCamera = ref(null)  // 画中画容器元素
const sharingUserVideoStream = ref(null)  // 共享者的摄像头流
const sharingUserName = ref('')  // 共享者的名称

// 画中画位置和拖动状态
const pipPosition = ref({ x: 20, y: 20 })  // 初始位置：左上角
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

// 观看者视频面板状态
const viewerPanelExpanded = ref(true)  // 观看者面板是否展开
const viewerDisplayMode = ref('self')  // 显示模式：'self' 只显示自己，'all' 显示全部成员
const viewerLocalVideo = ref(null)  // 观看者面板中的本地视频元素（只显示自己模式）
const viewerLocalVideoAll = ref(null)  // 观看者面板中的本地视频元素（显示全部模式）
const viewerParticipantVideoRefs = ref(new Map())  // 观看者面板中的参与者视频元素
const viewerPanelPosition = ref({ x: 0, y: 60 })  // 观看者面板位置
const isDraggingViewerPanel = ref(false)  // 是否正在拖动观看者面板

// 模态框状态
const showMeetingInfoModal = ref(false)
const showSettingsModal = ref(false)
const showParticipantsModal = ref(false)
const showChatModal = ref(false)
const showInviteModal = ref(false)
const showCameraSelectModal = ref(false)

// 摄像头选择相关
const availableCameras = ref([])
const selectedCameraId = ref(null)
const cameraUnavailable = ref(false)
const cameraUnavailableType = ref('generic')
const cameraUnavailableTitle = ref('摄像头暂时不可用')
const cameraUnavailableMessage = ref('已为你保留入会开视频偏好，请检查摄像头后重新开启。')
const autoVideoPreference = ref(settingsManager.shouldDefaultVideoOn())
const showCameraUnavailableCard = computed(() => cameraUnavailable.value && !isVideoOn.value)
const cameraUnavailableBadge = computed(() => {
  if (cameraUnavailableType.value === 'permission') return '权限被拒绝'
  if (cameraUnavailableType.value === 'busy') return '设备被占用'
  if (cameraUnavailableType.value === 'missing') return '未检测到设备'
  return '自动开视频'
})
const cameraUnavailableIcon = computed(() => {
  if (cameraUnavailableType.value === 'permission') return '🔒'
  if (cameraUnavailableType.value === 'busy') return '⛔'
  if (cameraUnavailableType.value === 'missing') return '📷'
  return '⚠️'
})
const cameraUnavailableShortTitle = computed(() => {
  if (cameraUnavailableType.value === 'permission') return '未授权摄像头'
  if (cameraUnavailableType.value === 'busy') return '摄像头被占用'
  if (cameraUnavailableType.value === 'missing') return '未找到摄像头'
  return '摄像头未开启'
})

// 设置相关
const activeSettingsTab = ref('general')
const settings = ref({
  autoMute: false,
  autoVideo: false,  // 默认不自动开启视频
  showNotifications: true
})

// 参与者和聊天
const participants = ref([])  // 用于视频区域显示（不包括自己）
const allParticipants = ref([])  // 用于成员列表模态框（包括所有人）
const chatMessages = ref([])
const chatInput = ref('')
const selectedTimeRange = ref('all')  // 选择的时间范围

// 屏幕共享状态跟踪
const currentScreenSharingUserId = ref(null)  // 当前正在共享屏幕的用户ID

// 屏幕共享选项
const showScreenShareOptions = ref(false)  // 显示屏幕共享选项对话框
const screenShareOptions = ref({
  shareAudio: false,  // 同时共享电脑声音
  showPip: true       // 人像画中画
})

// 屏幕共享控制栏
const isScreenSharePaused = ref(false)     // 屏幕共享是否暂停
const isRecording = ref(false)             // 录制状态

// 视频面板
const videoPanelExpanded = ref(true)  // 视频面板是否展开
const videoPanelPosition = ref({ x: 0, y: 60 })  // 视频面板位置
const isDraggingVideoPanel = ref(false)  // 是否正在拖动视频面板
const localVideoOverlay = ref(null)  // 悬浮层中的本地视频元素
const localScreenSharePreview = ref(null)  // 屏幕共享预览视频元素
const overlayParticipantVideoRefs = ref(new Map())  // 悬浮层中的参与者视频元素

// 聊天气泡面板
const chatPanelExpanded = ref(true)  // 聊天面板是否展开
const chatPanelPosition = ref({ x: 20, y: 0 })  // 聊天面板位置（左下角）
const isDraggingChatPanel = ref(false)  // 是否正在拖动聊天面板
const quickChatInput = ref('')  // 快速聊天输入框内容
const unreadMessageCount = ref(0)  // 未读消息数量
const showEmojiPicker = ref(false)  // 显示表情选择器（主聊天框）
const showQuickEmojiPicker = ref(false)  // 显示表情选择器（快速聊天框）

// 常用表情包列表
const emojiList = [
  '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂',
  '🙂', '🙃', '😉', '😊', '😇', '🥰', '😍', '🤩',
  '😘', '😗', '😚', '😙', '🥲', '😋', '😛', '😜',
  '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔', '🤐',
  '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬',
  '🤥', '😌', '😔', '😪', '🤤', '😴', '😷', '🤒',
  '🤕', '🤢', '🤮', '🤧', '🥵', '🥶', '😶‍🌫️', '🥴',
  '😵', '🤯', '🤠', '🥳', '🥸', '😎', '🤓', '🧐',
  '😕', '😟', '🙁', '☹️', '😮', '😯', '😲', '😳',
  '🥺', '😦', '😧', '😨', '😰', '😥', '😢', '😭',
  '😱', '😖', '😣', '😞', '😓', '😩', '😫', '🥱',
  '😤', '😡', '😠', '🤬', '😈', '👿', '💀', '☠️',
  '💩', '🤡', '👹', '👺', '👻', '👽', '👾', '🤖',
  '👋', '🤚', '🖐️', '✋', '🖖', '👌', '🤌', '🤏',
  '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆',
  '🖕', '👇', '☝️', '👍', '👎', '✊', '👊', '🤛',
  '🤜', '👏', '🙌', '👐', '🤲', '🤝', '🙏', '✍️',
  '💪', '🦾', '🦿', '🦵', '🦶', '👂', '🦻', '👃',
  '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍',
  '🤎', '💔', '❤️‍🔥', '❤️‍🩹', '💕', '💞', '💓', '💗',
  '💖', '💘', '💝', '💟', '☮️', '✝️', '☪️', '🕉️',
  '⭐', '🌟', '✨', '⚡', '🔥', '💥', '💫', '💦',
  '💨', '🌈', '☀️', '🌤️', '⛅', '🌥️', '☁️', '🌦️',
  '🌧️', '⛈️', '🌩️', '🌨️', '❄️', '☃️', '⛄', '🌬️',
  '👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙',
  '💯', '🔥', '⚡', '💥', '✨', '🎉', '🎊', '🎈'
]

// 会议时长
const meetingDuration = ref(0)  // 会议时长（秒）
const meetingStartTime = ref(null)  // 会议开始时间
const durationTimer = ref(null)  // 计时器

// 计算属性：格式化的会议时长
const formattedDuration = computed(() => {
  const duration = meetingDuration.value
  const hours = Math.floor(duration / 3600)
  const minutes = Math.floor((duration % 3600) / 60)
  const seconds = duration % 60
  
  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  } else {
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }
})

// 计算属性：最近的聊天消息（最多显示5条）
const recentChatMessages = computed(() => {
  return chatMessages.value.slice(-5)
})

// 远程视频流存储: { userId: { stream: MediaStream, videoRef: ref } }
const remoteVideoStreams = ref(new Map())

// 加载用户信息
const loadUserInfo = () => {
  const storedUserInfo = localStorage.getItem('userInfo')
  if (storedUserInfo) {
    const userInfo = JSON.parse(storedUserInfo)
    userName.value = userInfo.nickName || '用户'
    currentUserId.value = userInfo.userId || ''
    
    // 优先使用数据库中的头像
    if (userInfo.avatar) {
      userAvatar.value = userInfo.avatar
    } else if (userInfo.sex === 1) {
      userAvatar.value = '/meeting-icons/男头像.svg'
    } else if (userInfo.sex === 0) {
      userAvatar.value = '/meeting-icons/女头像.svg'
    } else {
      const firstChar = userName.value.charAt(0).toUpperCase()
      userAvatar.value = `https://ui-avatars.com/api/?name=${encodeURIComponent(firstChar)}&background=3498db&color=fff&size=64`
    }
  }
}

// 加入会议
const joinMeeting = async () => {
  try {
    console.log('开始加入会议流程...')
    console.log('路由 meetingId:', meetingId.value)
    console.log('当前用户ID:', currentUserId.value)
    
    // 检查 meetingId 是否存在
    if (!meetingId.value) {
      console.error('❌ meetingId 为空！')
      alert('会议ID不存在，无法加入会议')
      return
    }
    
    try {
      // 1. 调用后端加入会议API，传递meetingId以支持断线重连
      const response = await meetingService.joinMeeting(isVideoOn.value, meetingId.value)
      console.log('加入会议API响应:', response)
    } catch (joinError) {
      console.warn('直接加入会议失败，尝试恢复会议状态:', joinError)
      
      // 如果直接加入失败，可能是因为currentMeetingId为null
      // 尝试通过preJoinMeeting重新建立会议状态
      try {
        console.log('尝试通过会议号重新加入会议...')
        
        // 首先获取会议信息来获取会议号
        const meetingInfoResponse = await meetingService.getMeetingInfoByMeetingId(meetingId.value)
        if (meetingInfoResponse.data.code === 200 && meetingInfoResponse.data.data) {
          const meetingInfo = meetingInfoResponse.data.data
          const meetingNo = meetingInfo.meetingNo
          console.log('获取到会议号:', meetingNo)
          
          // 使用preJoinMeeting重新建立会议状态
          const preJoinResponse = await meetingService.preJoinMeeting(
            meetingNo, 
            userName.value || '用户', 
            null // 假设没有密码，如果有密码需要用户输入
          )
          
          if (preJoinResponse.data.code === 200) {
            console.log('会议状态恢复成功，重新尝试加入会议')
            // 重新尝试加入会议
            const retryResponse = await meetingService.joinMeeting(isVideoOn.value, meetingId.value)
            console.log('重试加入会议成功:', retryResponse)
          }
        } else {
          throw new Error('无法获取会议信息')
        }
      } catch (recoveryError) {
        console.error('会议状态恢复失败:', recoveryError)
        throw new Error('加入会议失败，可能是网络问题导致的状态丢失。请返回主页重新进入会议。')
      }
    }
    
    // 2. 获取当前会议信息
    await loadCurrentMeetingInfo()
    
    // 3. 建立WebSocket连接
    const token = localStorage.getItem('token')
    if (token && currentUserId.value && meetingId.value) {
      await meetingWsService.connect(token, currentUserId.value, meetingId.value)
      console.log('WebSocket连接成功')
      
      // 4. 设置WebSocket消息处理器
      setupWebSocketHandlers()
      
      // 5. 加载会议成员
      await loadParticipants()
      
      // 6. 根据用户设置自动初始化媒体流
      console.log('📹 根据用户设置初始化媒体流...')
      console.log('默认开启视频:', isVideoOn.value)
      console.log('默认开启音频:', !isMuted.value)
      
      // 如果设置了默认开启视频或音频,自动获取媒体流
      if (isVideoOn.value || !isMuted.value) {
        try {
          await getUserMediaWithAudio()
          console.log('✅ 媒体流初始化成功')
        } catch (error) {
          console.error('❌ 媒体流初始化失败:', error)
          if (autoVideoPreference.value) {
            setCameraUnavailableState(error, true)
          }
          // 如果获取媒体流失败,不影响加入会议,用户可以稍后手动开启
        }
      }
    }
    
    console.log('会议加入流程完成')
  } catch (error) {
    console.error('加入会议失败:', error)
    alert('加入会议失败: ' + (error.message || '请稍后重试'))
  }
}

// 设置WebSocket消息处理器
const setupWebSocketHandlers = () => {
  console.log('设置WebSocket消息处理器...')
  
  // 处理成员加入
  meetingWsService.on('memberJoined', (message) => {
    console.log('🎉 有新成员加入会议:', message)
    if (message.messageContent && message.messageContent.newMember) {
      console.log('新成员信息:', message.messageContent.newMember)
      addParticipant(message.messageContent.newMember)
    }
    // 更新完整成员列表
    if (message.messageContent && message.messageContent.meetingMemberList) {
      console.log('更新完整成员列表，成员数:', message.messageContent.meetingMemberList.length)
      updateParticipantsList(message.messageContent.meetingMemberList)
    }
  })
  
  // 处理成员离开
  meetingWsService.on('memberLeft', (message) => {
    console.log('=== 收到成员离开消息 ===')
    console.log('完整消息对象:', message)
    console.log('消息类型:', message.messageType)
    console.log('消息内容类型:', typeof message.messageContent)
    console.log('消息内容原始值:', message.messageContent)
    
    // 解析消息内容（可能是JSON字符串）
    let exitData = message.messageContent
    if (typeof exitData === 'string') {
      try {
        exitData = JSON.parse(exitData)
        console.log('✅ JSON解析成功，退出数据:', exitData)
      } catch (e) {
        console.error('❌ JSON解析失败:', e)
        console.error('原始内容:', message.messageContent)
        return
      }
    } else {
      console.log('消息内容已经是对象，无需解析')
    }
    
    if (exitData && exitData.exitUserId) {
      console.log('📤 准备移除退出的成员:', exitData.exitUserId)
      console.log('退出状态:', exitData.exitStatus)
      console.log('当前参与者列表:', participants.value.map(p => ({ userId: p.userId, name: p.name })))
      console.log('当前所有参与者列表:', allParticipants.value.map(p => ({ userId: p.userId, name: p.name })))
      
      // 移除退出的成员
      removeParticipant(exitData.exitUserId)
      
      console.log('✅ 成员移除完成')
      console.log('更新后参与者列表:', participants.value.map(p => ({ userId: p.userId, name: p.name })))
      console.log('更新后所有参与者列表:', allParticipants.value.map(p => ({ userId: p.userId, name: p.name })))
      
      // 如果有更新的成员列表，也更新一下
      if (exitData.meetingMemberDtoList && Array.isArray(exitData.meetingMemberDtoList)) {
        console.log('📋 后端提供了完整成员列表，共', exitData.meetingMemberDtoList.length, '人')
        console.log('成员列表详情:', exitData.meetingMemberDtoList.map(m => ({ 
          userId: m.userId, 
          nickName: m.nickName, 
          status: m.status 
        })))
        updateParticipantsList(exitData.meetingMemberDtoList)
        console.log('✅ 成员列表已更新')
      }
    } else {
      console.warn('⚠️ 消息中没有 exitUserId')
    }
    console.log('=== 成员离开消息处理完成 ===\n')
  })
  
  // 处理会议结束
  meetingWsService.on('meetingFinished', (message) => {
    console.log('会议已结束:', message)
    alert('会议已结束')
    router.push('/dashboard')
  })
  
  // 处理强制下线（被踢出）
  meetingWsService.on('forceOffline', (message) => {
    console.log('收到强制下线消息:', message)
    const reason = message.messageContent || '您已被主持人移出会议'
    alert(reason)
    // 立即退出会议并返回主页（不需要确认）
    forceExitMeeting()
  })
  
  // 处理聊天消息
  meetingWsService.on('chatMessage', (message) => {
    console.log('🔔 收到聊天消息 WebSocket 事件:', message)
    console.log('消息类型:', message.messageType)
    console.log('消息内容:', message.messageContent)
    console.log('发送者ID:', message.sendUserId)
    console.log('发送者昵称:', message.sendUserNickName)
    
    // 获取发送者信息
    const senderName = message.sendUserNickName || '未知用户'
    const senderAvatar = getMemberAvatar(message.sendUserId)
    
    console.log('解析后的发送者昵称:', senderName)
    console.log('解析后的发送者头像:', senderAvatar)
    
    // 判断是否为私聊消息
    const isPrivate = message.receiveType === 1
    
    // 获取接收者信息（如果是私聊）
    let receiveUserName = ''
    if (isPrivate && message.receiveUserId) {
      const receiver = participants.value.find(p => p.userId === message.receiveUserId)
      receiveUserName = receiver ? receiver.name : '未知用户'
    }
    
    const chatMessage = {
      id: message.messageId || Date.now(),
      sender: senderName,
      avatar: senderAvatar,
      text: message.messageContent,
      time: formatTime(message.sendTime || Date.now()),
      timestamp: message.sendTime || Date.now(),  // 添加时间戳用于日期分组
      sendUserId: message.sendUserId,
      isPrivate: isPrivate,
      receiveUserId: message.receiveUserId,
      receiveUserName: receiveUserName
    }
    
    console.log('准备添加到聊天框的消息对象:', chatMessage)
    addChatMessage(chatMessage)
    console.log('✅ 消息已添加到聊天框')
  })
  
  // 处理视频状态改变
  meetingWsService.on('videoStatusChanged', (message) => {
    console.log('用户视频状态改变:', message)
    if (message.messageContent && message.messageContent.userId) {
      updateParticipantVideoStatus(message.messageContent.userId, message.messageContent.videoOpen)
    }
  })
  
  // 处理屏幕共享开始
  meetingWsService.on('screenShareStart', async (message) => {
    console.log('🖥️ 收到屏幕共享开始消息:', message)
    if (message.sendUserId && message.sendUserId !== currentUserId.value) {
      const userName = message.sendUserNickName || '某位用户'
      console.log(`${userName} 开始共享屏幕`)
      
      // 只设置状态，视频流由 handleRemoteScreenStreamAdded 处理
      currentScreenSharingUserId.value = message.sendUserId
      sharingUserName.value = userName
      
      console.log('✅ 屏幕共享状态已更新，等待接收屏幕流...')
      
      // 初始化观看者面板位置
      await nextTick()
      initViewerPanelPosition()
      
      // 更新观看者视频面板的视频流
      updateViewerVideoStreams()
    }
  })
  
  // 处理屏幕共享停止
  meetingWsService.on('screenShareStop', (message) => {
    console.log('🖥️ 收到屏幕共享停止消息:', message)
    if (message.sendUserId === currentScreenSharingUserId.value) {
      console.log('屏幕共享已停止')
      currentScreenSharingUserId.value = null
      sharingUserName.value = ''
      sharingUserVideoStream.value = null
      
      // 清空视频元素
      if (remoteScreenShareVideo.value) {
        remoteScreenShareVideo.value.srcObject = null
      }
      if (pipSharingUserVideo.value) {
        pipSharingUserVideo.value.srcObject = null
      }
    }
  })
  
  // 初始化WebRTC管理器
  console.log('🎬 初始化WebRTC管理器')
  webrtcManager.init(meetingWsService, currentUserId.value, meetingId.value)
  
  // 设置WebRTC事件回调
  webrtcManager.onRemoteStreamAdded = (userId, stream) => {
    console.log('📺 收到远程视频流:', userId)
    handleRemoteStreamAdded(userId, stream)
  }
  
  webrtcManager.onRemoteStreamRemoved = (userId) => {
    console.log('🔌 远程视频流已移除:', userId)
    handleRemoteStreamRemoved(userId)
  }
  
  webrtcManager.onRemoteScreenStreamAdded = (userId, stream) => {
    console.log('🖥️ 收到远程屏幕共享流:', userId)
    handleRemoteScreenStreamAdded(userId, stream)
  }
  
  webrtcManager.onRemoteScreenStreamRemoved = (userId) => {
    console.log('🖥️ 远程屏幕共享流已移除:', userId)
    handleRemoteScreenStreamRemoved(userId)
  }
  
  webrtcManager.onConnectionStateChange = (userId, state) => {
    console.log(`🔌 连接状态变化 [${userId}]:`, state)
  }
}

// 处理远程视频流添加
const handleRemoteStreamAdded = async (userId, stream) => {
  console.log('📺📺📺 处理远程视频流添加:', userId)
  console.log('📺📺📺 流ID:', stream.id)
  console.log('📺📺📺 视频轨道数:', stream.getVideoTracks().length)
  console.log('📺📺📺 音频轨道数:', stream.getAudioTracks().length)
  
  // 查找对应的参与者
  const participant = participants.value.find(p => p.userId === userId)
  if (!participant) {
    console.warn('❌ 找不到对应的参与者:', userId)
    console.warn('当前参与者列表:', participants.value.map(p => p.userId))
    return
  }
  
  console.log('📺📺📺 找到参与者:', participant.name)
  console.log('📺📺📺 参与者videoOpen状态:', participant.videoOpen)
  
  // 确保videoOpen为true（如果还没设置）
  if (!participant.videoOpen) {
    console.log('📺📺📺 设置参与者videoOpen为true')
    participant.videoOpen = true
  }
  
  // 等待DOM更新
  await nextTick()
  
  // 设置视频流到video元素
  if (participant.videoRef) {
    console.log('📺📺📺 videoRef存在，设置srcObject')
    participant.videoRef.srcObject = stream
    console.log('✅✅✅ 远程视频流已设置到video元素')
    
    // 添加事件监听器来确认视频播放
    participant.videoRef.onloadedmetadata = () => {
      console.log('✅ 远程视频元数据已加载')
      participant.videoRef.play().catch(e => console.error('播放远程视频失败:', e))
    }
  } else {
    console.warn('⚠️⚠️⚠️ videoRef不存在，稍后重试')
    // 延迟重试
    setTimeout(async () => {
      await nextTick()
      if (participant.videoRef) {
        console.log('📺📺📺 延迟设置：videoRef现在存在了')
        participant.videoRef.srcObject = stream
        console.log('✅✅✅ 远程视频流已设置到video元素（延迟）')
        
        participant.videoRef.onloadedmetadata = () => {
          console.log('✅ 远程视频元数据已加载（延迟）')
          participant.videoRef.play().catch(e => console.error('播放远程视频失败:', e))
        }
      } else {
        console.error('❌❌❌ 延迟后videoRef仍然不存在')
      }
    }, 500)
  }
}

// 处理远程视频流移除
const handleRemoteStreamRemoved = (userId) => {
  console.log('处理远程视频流移除:', userId)
  
  const participant = participants.value.find(p => p.userId === userId)
  if (participant && participant.videoRef) {
    participant.videoRef.srcObject = null
  }
}

// 处理远程屏幕共享流添加
const handleRemoteScreenStreamAdded = async (userId, stream) => {
  console.log('🖥️🖥️🖥️ 处理远程屏幕共享流:', userId)
  console.log('🖥️ 流ID:', stream.id)
  console.log('🖥️ 视频轨道数:', stream.getVideoTracks().length)
  
  // 检查视频轨道信息
  const videoTrack = stream.getVideoTracks()[0]
  if (videoTrack) {
    console.log('🖥️ 视频轨道详情:')
    console.log('  - ID:', videoTrack.id)
    console.log('  - Label:', videoTrack.label)
    console.log('  - Settings:', videoTrack.getSettings())
  }
  
  // 设置当前屏幕共享用户
  currentScreenSharingUserId.value = userId
  
  // 查找共享者的参与者信息
  const sharingParticipant = participants.value.find(p => p.userId === userId)
  console.log('🖥️ 找到共享者参与者信息:', !!sharingParticipant)
  
  if (sharingParticipant) {
    sharingUserName.value = sharingParticipant.name
    console.log('🖥️ 共享者名称:', sharingUserName.value)
    
    // 保存共享者的摄像头视频流（用于画中画）
    if (sharingParticipant.videoRef && sharingParticipant.videoRef.srcObject) {
      sharingUserVideoStream.value = sharingParticipant.videoRef.srcObject
      console.log('📹 保存共享者的摄像头流用于画中画')
      console.log('📹 摄像头流ID:', sharingUserVideoStream.value.id)
      
      const cameraTrack = sharingUserVideoStream.value.getVideoTracks()[0]
      if (cameraTrack) {
        console.log('📹 摄像头轨道 Label:', cameraTrack.label)
      }
    } else {
      console.warn('⚠️ 未找到共享者的摄像头流')
    }
  }
  
  // 等待 DOM 更新
  await nextTick()
  
  console.log('🖥️ 检查视频元素:')
  console.log('  - pipSharingUserVideo 存在:', !!pipSharingUserVideo.value)
  console.log('  - remoteScreenShareVideo 存在:', !!remoteScreenShareVideo.value)
  
  // 设置画中画视频（共享者的摄像头）
  if (pipSharingUserVideo.value && sharingUserVideoStream.value) {
    pipSharingUserVideo.value.srcObject = sharingUserVideoStream.value
    console.log('✅ 画中画视频已设置（摄像头流）')
  } else {
    console.warn('⚠️ 无法设置画中画视频')
  }
  
  // 设置屏幕共享视频（共享者的屏幕）
  if (remoteScreenShareVideo.value) {
    remoteScreenShareVideo.value.srcObject = stream
    console.log('✅ 屏幕共享视频已设置（屏幕流）')
    console.log('✅ 屏幕流ID:', stream.id)
  } else {
    console.warn('⚠️ 无法设置屏幕共享视频')
  }
}

// 处理远程屏幕共享流移除
const handleRemoteScreenStreamRemoved = (userId) => {
  console.log('🖥️ 处理远程屏幕共享流移除:', userId)
  
  if (currentScreenSharingUserId.value === userId) {
    currentScreenSharingUserId.value = null
    sharingUserName.value = ''
    sharingUserVideoStream.value = null
    
    // 清空视频元素
    if (remoteScreenShareVideo.value) {
      remoteScreenShareVideo.value.srcObject = null
    }
    if (pipSharingUserVideo.value) {
      pipSharingUserVideo.value.srcObject = null
    }
  }
}

// 添加参与者
const addParticipant = (memberData) => {
  console.log('添加参与者到列表:', memberData.nickName, 'userId:', memberData.userId)
  
  const formatted = formatParticipant(memberData)
  formatted.isCurrentUser = memberData.userId === currentUserId.value
  
  // 添加到 allParticipants
  const existingAllIndex = allParticipants.value.findIndex(p => p.userId === memberData.userId)
  if (existingAllIndex === -1) {
    allParticipants.value.push(formatted)
    console.log('✅ 已添加到allParticipants，当前总数:', allParticipants.value.length)
  } else {
    console.log('⚠️ 成员已存在于allParticipants中')
  }
  
  // 如果不是当前用户，也添加到 participants
  if (memberData.userId !== currentUserId.value) {
    const existingIndex = participants.value.findIndex(p => p.userId === memberData.userId)
    if (existingIndex === -1) {
      participants.value.push(formatted)
      console.log('✅ 已添加到participants（视频区域），当前总数:', participants.value.length)
      
      // 与新成员建立WebRTC连接
      webrtcManager.connectToParticipant(memberData.userId)
    } else {
      console.log('⚠️ 成员已存在于participants中')
    }
  }
}

// 移除参与者
const removeParticipant = (userId) => {
  console.log('🗑️ removeParticipant 被调用，userId:', userId)
  
  // 关闭WebRTC连接
  console.log('关闭WebRTC连接...')
  webrtcManager.closePeerConnection(userId)
  
  // 从 allParticipants 移除
  const allIndex = allParticipants.value.findIndex(p => p.userId === userId)
  if (allIndex > -1) {
    const removed = allParticipants.value[allIndex]
    allParticipants.value.splice(allIndex, 1)
    console.log('✅ 从 allParticipants 移除:', removed.name, '(索引:', allIndex, ')')
  } else {
    console.warn('⚠️ 在 allParticipants 中未找到用户:', userId)
  }
  
  // 从 participants 移除
  const index = participants.value.findIndex(p => p.userId === userId)
  if (index > -1) {
    const participant = participants.value[index]
    participants.value.splice(index, 1)
    console.log('✅ 从 participants 移除:', participant.name, '(索引:', index, ')')
  } else {
    console.warn('⚠️ 在 participants 中未找到用户:', userId)
  }
  
  console.log('当前 allParticipants 数量:', allParticipants.value.length)
  console.log('当前 participants 数量:', participants.value.length)
}

// 更新参与者列表
const updateParticipantsList = (memberList) => {
  console.log('收到成员列表数据:', memberList)
  
  if (!Array.isArray(memberList)) {
    console.error('成员列表不是数组:', typeof memberList, memberList)
    return
  }
  
  console.log('过滤前成员数量:', memberList.length)
  
  // 过滤出状态正常的成员
  const normalMembers = memberList.filter(member => {
    console.log('成员状态检查:', member.nickName, 'status:', member.status, 'userId:', member.userId, 'currentUserId:', currentUserId.value)
    return member.status === MemberStatus.NORMAL
  })
  
  // allParticipants: 包括所有正常状态的成员（用于成员列表模态框）
  allParticipants.value = normalMembers.map(member => {
    const formatted = formatParticipant(member)
    // 标记是否是当前用户
    formatted.isCurrentUser = member.userId === currentUserId.value
    return formatted
  })
  
  // participants: 只包括其他成员，不包括自己（用于视频区域显示）
  participants.value = normalMembers
    .filter(member => member.userId !== currentUserId.value)
    .map(member => formatParticipant(member))
  
  console.log('所有成员数量（包括自己）:', allParticipants.value.length)
  console.log('其他成员数量（排除自己）:', participants.value.length)
}

// 格式化参与者数据
const formatParticipant = (memberData) => {
  let avatar = ''
  if (memberData.avatar) {
    avatar = memberData.avatar
  } else if (memberData.sex === 1) {
    avatar = '/meeting-icons/男头像.svg'
  } else if (memberData.sex === 0) {
    avatar = '/meeting-icons/女头像.svg'
  } else {
    // 如果没有性别信息，使用昵称首字母生成头像
    const firstChar = (memberData.nickName || '用户').charAt(0).toUpperCase()
    avatar = `https://ui-avatars.com/api/?name=${encodeURIComponent(firstChar)}&background=3498db&color=fff&size=64`
  }
  
  return {
    userId: memberData.userId,
    name: memberData.nickName || '用户',
    avatar: avatar,
    isMuted: false, // 这个需要从其他地方获取
    isHost: memberData.memberType === MemberType.HOST,
    videoOpen: memberData.openVideo || false,
    status: memberData.status,
    memberType: memberData.memberType
  }
}

// 获取用户头像
const getUserAvatar = (userId) => {
  const participant = participants.value.find(p => p.userId === userId)
  if (participant) {
    return participant.avatar
  }
  
  // 如果是当前用户
  if (userId === currentUserId.value) {
    return userAvatar.value
  }
  
  // 默认头像
  return '/meeting-icons/男头像.svg'
}

// 获取成员头像（优化版，支持从会议成员列表获取）
const getMemberAvatar = (userId) => {
  // 先从参与者列表查找
  const participant = participants.value.find(p => p.userId === userId)
  if (participant && participant.avatar) {
    return participant.avatar
  }
  
  // 如果是当前用户
  if (userId === currentUserId.value) {
    return userAvatar.value
  }
  
  // 默认头像
  return '/meeting-icons/男头像.svg'
}

// 更新参与者视频状态
const updateParticipantVideoStatus = (userId, videoOpen) => {
  const participant = participants.value.find(p => p.userId === userId)
  if (participant) {
    participant.videoOpen = videoOpen
    console.log(`更新${participant.name}的视频状态:`, videoOpen)
  }

}

// 添加聊天消息
const addChatMessage = (message) => {
  // 检查消息是否已存在（基于消息ID去重）
  const existingIndex = chatMessages.value.findIndex(msg => msg.id === message.id)
  
  if (existingIndex === -1) {
    // 消息不存在，添加到数组
    chatMessages.value.push(message)
    console.log('添加新消息:', message.sender, message.text)
    
    // 如果不是自己发送的消息，且聊天窗口未打开，增加未读计数
    if (message.sendUserId !== currentUserId.value && !showChatModal.value) {
      unreadMessageCount.value++
      console.log('未读消息数:', unreadMessageCount.value)
    }
  } else {
    // 消息已存在，更新消息内容（确保显示最新的昵称和头像）
    chatMessages.value[existingIndex] = message
    console.log('更新已存在的消息:', message.sender, message.text)
  }
  
  // 限制消息数量，避免内存占用过多
  if (chatMessages.value.length > 100) {
    chatMessages.value.splice(0, chatMessages.value.length - 100)
  }
  
  // 滚动到最新消息
  setTimeout(() => {
    const chatContainer = document.querySelector('.chat-messages')
    if (chatContainer) {
      chatContainer.scrollTop = chatContainer.scrollHeight
    }
  }, 100)
}

// 获取日期标签（今天、昨天、具体日期）
const getDateLabel = (timestamp) => {
  const messageDate = new Date(timestamp)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  
  // 重置时间为0点，只比较日期
  const messageDateOnly = new Date(messageDate.getFullYear(), messageDate.getMonth(), messageDate.getDate())
  const todayOnly = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const yesterdayOnly = new Date(yesterday.getFullYear(), yesterday.getMonth(), yesterday.getDate())
  
  if (messageDateOnly.getTime() === todayOnly.getTime()) {
    return '今天'
  } else if (messageDateOnly.getTime() === yesterdayOnly.getTime()) {
    return '昨天'
  } else {
    // 显示具体日期：2月22日
    return `${messageDate.getMonth() + 1}月${messageDate.getDate()}日`
  }
}

// 按日期分组聊天消息
const groupedChatMessages = computed(() => {
  const groups = []
  const groupMap = new Map()
  
  // 按日期分组
  chatMessages.value.forEach(message => {
    const dateLabel = getDateLabel(message.timestamp || Date.now())
    
    if (!groupMap.has(dateLabel)) {
      const group = {
        dateLabel,
        messages: []
      }
      groupMap.set(dateLabel, group)
      groups.push(group)
    }
    
    groupMap.get(dateLabel).messages.push(message)
  })
  
  return groups
})

// 格式化时间
const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

// 计算参与者网格布局类名
const getGridClass = () => {
  const totalParticipants = participants.value.length + 1 // +1 包括自己
  
  if (totalParticipants === 1) return 'grid-1'
  if (totalParticipants === 2) return 'grid-2'
  if (totalParticipants <= 4) return 'grid-4'
  if (totalParticipants <= 6) return 'grid-6'
  if (totalParticipants <= 9) return 'grid-9'
  return 'grid-many'
}

const loadCurrentMeetingInfo = async () => {
  try {
    console.log('=== 开始加载会议信息 ===')
    const response = await meetingService.getCurrentMeeting()
    console.log('API响应:', response)
    console.log('响应数据:', response.data)
    
    if (response.data && response.data.data) {
      const meetingInfo = response.data.data
      console.log('会议信息对象:', meetingInfo)
      
      meetingName.value = meetingInfo.meetingName || '快速会议'
      meetingNo.value = meetingInfo.meetingNo || ''
      
      // 设置主持人名称（从后端返回的createUserNickName）
      hostName.value = meetingInfo.createUserNickName || '未知'
      
      console.log('设置后的值:')
      console.log('- meetingName:', meetingName.value)
      console.log('- meetingNo:', meetingNo.value)
      console.log('- hostName:', hostName.value)
      
      // 判断是否为主持人
      if (meetingInfo.createUserId && currentUserId.value) {
        isHost.value = meetingInfo.createUserId === currentUserId.value
        console.log('- isHost:', isHost.value)
      }
      
      console.log('=== 会议信息加载成功 ===')
      return meetingInfo
    } else {
      console.warn('API返回数据为空，用户可能不在会议中:', response.data)
      // 如果用户不在会议中，但在Meeting页面，说明状态不一致
      if (meetingId.value) {
        console.warn('用户在Meeting页面但没有当前会议，尝试重新加入会议')
        // 可以尝试重新加入会议或者跳转回Dashboard
        throw new Error('用户不在会议中，但在会议页面')
      }
      return null
    }
  } catch (error) {
    console.error('=== 获取会议信息失败 ===')
    console.error('错误详情:', error)
    console.error('错误响应:', error.response)
    throw error
  }
}

// 显示会议详情（先加载最新信息）
const showMeetingInfo = async () => {
  console.log('点击会议详情按钮')
  try {
    // 先加载最新的会议信息
    const meetingInfo = await loadCurrentMeetingInfo()
    if (meetingInfo) {
      // 如果有会议信息，显示模态框
      showMeetingInfoModal.value = true
    } else {
      // 如果没有会议信息，提示用户
      console.warn('当前没有会议信息，无法显示会议详情')
      alert('当前没有进行中的会议，无法查看会议详情')
    }
  } catch (error) {
    console.error('加载会议信息失败:', error)
    alert('获取会议信息失败，请稍后重试')
  }
}

// 选择会议号文本
const selectMeetingNo = (event) => {
  if (event.target && event.target.select) {
    event.target.select()
  }
}

// 复制会议号
const copyMeetingNo = async () => {
  if (!meetingNo.value) {
    alert('会议号不存在')
    return
  }
  
  try {
    // 方法1: 检查是否在 Electron 环境中
    if (window.electron && window.electron.isElectron && window.electron.clipboard) {
      // 使用 Electron 的剪贴板 API
      window.electron.clipboard.writeText(meetingNo.value)
      alert(`会议号 ${meetingNo.value} 已复制到剪贴板`)
      return
    }
    
    // 方法2: 使用现代的Clipboard API（浏览器环境）
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(meetingNo.value)
      alert(`会议号 ${meetingNo.value} 已复制到剪贴板`)
      return
    }
    
    // 方法3: 使用传统的execCommand方法
    const textArea = document.createElement('textarea')
    textArea.value = meetingNo.value
    textArea.style.position = 'fixed'
    textArea.style.left = '-999999px'
    textArea.style.top = '-999999px'
    textArea.style.opacity = '0'
    textArea.style.pointerEvents = 'none'
    textArea.setAttribute('readonly', '')
    textArea.setAttribute('contenteditable', 'true')
    
    document.body.appendChild(textArea)
    
    // 选择文本
    textArea.focus()
    textArea.select()
    textArea.setSelectionRange(0, textArea.value.length)
    
    // 尝试复制
    const successful = document.execCommand('copy')
    document.body.removeChild(textArea)
    
    if (successful) {
      alert(`会议号 ${meetingNo.value} 已复制到剪贴板`)
    } else {
      throw new Error('execCommand复制失败')
    }
  } catch (error) {
    console.error('复制会议号失败:', error)
    
    // 方法4: 显示会议号让用户手动复制
    alert(`复制失败，会议号为：${meetingNo.value}\n\n请手动选择并复制`)
  }
}

// 音频控制
const toggleMute = async () => {
  try {
    console.log('🎤 切换静音状态，当前:', isMuted.value)
    
    // 如果要解除静音但没有本地流，先获取音频流
    if (!isMuted.value && !localStream.value) {
      console.log('🎤 没有本地流，先获取音频流')
      await getAudioStream()
      return
    }
    
    // 如果有本地流，检查是否有音频轨道
    if (localStream.value) {
      const audioTracks = localStream.value.getAudioTracks()
      console.log('🎤 音频轨道数:', audioTracks.length)
      
      if (audioTracks.length > 0) {
        // 有音频轨道，直接切换enabled状态
        const newMutedState = !isMuted.value
        audioTracks.forEach(track => {
          track.enabled = !newMutedState // enabled=true表示未静音
          console.log('🎤 设置音频轨道enabled:', track.enabled, '标签:', track.label)
        })
        isMuted.value = newMutedState
        console.log('✅ 静音状态已更新:', isMuted.value)
      } else {
        // 没有音频轨道，需要获取音频
        console.log('🎤 没有音频轨道，获取音频流')
        await getAudioStream()
      }
    }
  } catch (error) {
    console.error('❌ 切换静音失败:', error)
  }
}

// 获取纯音频流（不包含视频）
const getAudioStream = async () => {
  try {
    console.log('🎤 获取纯音频流...')
    
    // 从设置管理器获取音频约束
    const audioConstraints = settingsManager.getAudioConstraints()
    
    const audioStream = await navigator.mediaDevices.getUserMedia({
      video: false,
      audio: audioConstraints
    })
    
    console.log('✅ 音频流获取成功')
    console.log('🎤 音频轨道数:', audioStream.getAudioTracks().length)
    console.log('🎤 音频约束:', audioConstraints)
    
    const audioTrack = audioStream.getAudioTracks()[0]
    if (audioTrack) {
      console.log('🎤 音频轨道信息:', {
        label: audioTrack.label,
        enabled: audioTrack.enabled,
        readyState: audioTrack.readyState
      })
    }
    
    // 如果已有本地流（只有视频），添加音频轨道
    if (localStream.value) {
      console.log('🎤 添加音频轨道到现有流')
      const audioTrack = audioStream.getAudioTracks()[0]
      localStream.value.addTrack(audioTrack)
      
      // 更新WebRTC连接
      webrtcManager.setLocalStream(localStream.value)
    } else {
      // 没有本地流，创建新的流
      console.log('🎤 创建新的音频流')
      localStream.value = audioStream
      
      // 设置到WebRTC管理器
      webrtcManager.setLocalStream(audioStream)
    }
    
    // 设置为未静音状态
    isMuted.value = false
    console.log('✅ 音频已开启，未静音')
    
  } catch (error) {
    console.error('❌ 获取音频流失败:', error)
    if (error.name === 'NotAllowedError') {
      alert('需要麦克风权限才能使用音频功能')
    } else if (error.name === 'NotFoundError') {
      alert('未检测到麦克风设备')
    } else {
      alert('获取麦克风失败: ' + error.message)
    }
    throw error
  }
}

// 获取带音频的媒体流
const getUserMediaWithAudio = async () => {
  try {
    console.log('🎤 获取带音频的媒体流...')
    
    // 从设置管理器获取约束
    const videoConstraints = settingsManager.getVideoConstraints()
    const audioConstraints = settingsManager.getAudioConstraints()
    
    const stream = await navigator.mediaDevices.getUserMedia({
      video: isVideoOn.value ? videoConstraints : false,
      audio: audioConstraints
    })
    
    console.log('✅ 获取媒体流成功')
    console.log('音频轨道:', stream.getAudioTracks().length)
    console.log('视频轨道:', stream.getVideoTracks().length)
    console.log('视频质量设置:', settingsManager.get('videoQuality'))
    console.log('音频约束:', audioConstraints)
    
    // 停止旧的流
    if (localStream.value) {
      localStream.value.getTracks().forEach(track => track.stop())
    }
    
    localStream.value = stream
    clearCameraUnavailableState()
    clearCameraUnavailableState()
    
    // 更新本地视频显示
    if (isVideoOn.value && localVideo.value) {
      localVideo.value.srcObject = stream
    }
    
    // 更新WebRTC连接
    webrtcManager.setLocalStream(stream)
    
    return stream
  } catch (error) {
    console.error('❌ 获取音频流失败:', error)
    throw error
  }
}

// 获取可用摄像头列表
const loadAvailableCameras = async () => {
  try {
    const devices = await navigator.mediaDevices.enumerateDevices()
    const videoDevices = devices.filter(device => device.kind === 'videoinput')
    
    availableCameras.value = videoDevices.map((device, index) => ({
      deviceId: device.deviceId,
      label: device.label || `摄像头 ${index + 1}`,
      index: index
    }))
    
    console.log('找到摄像头数量:', availableCameras.value.length)
    availableCameras.value.forEach(camera => {
      console.log(`  - ${camera.label} (${camera.deviceId})`)
    })
    
    return availableCameras.value
  } catch (error) {
    console.error('获取摄像头列表失败:', error)
    return []
  }
}

// 选择摄像头并开启视频
const selectCameraAndStart = async () => {
  try {
    console.log('📹 加载可用摄像头列表...')
    
    const cameras = await loadAvailableCameras()
    
    if (cameras.length === 0) {
      alert('未检测到摄像头设备')
      return
    }
    
    if (cameras.length === 1) {
      // 只有一个摄像头，直接使用
      console.log('只有一个摄像头，直接使用')
      await startVideoWithDevice(cameras[0].deviceId)
      return
    }
    
    // 多个摄像头，显示选择模态框
    showCameraSelectModal.value = true
    
  } catch (error) {
    console.error('选择摄像头失败:', error)
    alert('获取摄像头列表失败')
  }
}

// 确认选择摄像头
const confirmCameraSelection = async () => {
  if (!selectedCameraId.value) {
    alert('请选择一个摄像头')
    return
  }
  
  showCameraSelectModal.value = false
  await startVideoWithDevice(selectedCameraId.value)
}

// 取消摄像头选择
const cancelCameraSelection = () => {
  showCameraSelectModal.value = false
  selectedCameraId.value = null
}

const clearCameraUnavailableState = () => {
  cameraUnavailable.value = false
  cameraUnavailableType.value = 'generic'
  cameraUnavailableTitle.value = '摄像头暂时不可用'
  cameraUnavailableMessage.value = '已为你保留入会开视频偏好，请检查摄像头后重新开启。'
}

const setCameraUnavailableState = (error, preferredVideo = false) => {
  cameraUnavailable.value = true
  isVideoOn.value = false

  if (error?.name === 'NotAllowedError' || error?.name === 'PermissionDeniedError') {
    cameraUnavailableType.value = 'permission'
    cameraUnavailableTitle.value = '需要摄像头权限'
    cameraUnavailableMessage.value = '请在系统或浏览器/Electron 设置里允许访问摄像头，然后重新开启视频。'
  } else if (error?.name === 'NotFoundError' || error?.name === 'DevicesNotFoundError') {
    cameraUnavailableType.value = 'missing'
    cameraUnavailableTitle.value = '未检测到摄像头'
    cameraUnavailableMessage.value = '当前没有可用摄像头设备，请连接摄像头后再次开启视频。'
  } else if (error?.name === 'NotReadableError' || error?.name === 'TrackStartError') {
    cameraUnavailableType.value = 'busy'
    cameraUnavailableTitle.value = '摄像头被占用'
    cameraUnavailableMessage.value = '摄像头可能被其他应用占用。关闭占用程序后，再重新开启视频。'
  } else {
    cameraUnavailableType.value = 'generic'
    cameraUnavailableTitle.value = '摄像头开启失败'
    cameraUnavailableMessage.value = error?.message || '暂时无法访问摄像头，请稍后重试。'
  }

  if (preferredVideo) {
    ElMessage.warning('已为你保留自动开视频偏好，请处理摄像头后重新开启')
  }
}

const dismissCameraUnavailablePrompt = () => {
  cameraUnavailable.value = false
}

const retryEnableCamera = async () => {
  dismissCameraUnavailablePrompt()
  await toggleVideo()
}

// 使用指定的摄像头设备开启视频
const startVideoWithDevice = async (deviceId) => {
  try {
    console.log('🎥 使用摄像头:', deviceId)
    
    const stream = await navigator.mediaDevices.getUserMedia({ 
      video: {
        deviceId: deviceId ? { exact: deviceId } : undefined,
        width: { ideal: 1280 },
        height: { ideal: 720 }
      }, 
      audio: {
        echoCancellation: true,  // 回声消除
        noiseSuppression: true,  // 噪音抑制
        autoGainControl: true    // 自动增益控制
      }
    })
    
    console.log('✅ 摄像头和麦克风权限获取成功')
    console.log('📹 视频轨道数:', stream.getVideoTracks().length)
    console.log('🎤 音频轨道数:', stream.getAudioTracks().length)
    
    const videoTrack = stream.getVideoTracks()[0]
    if (videoTrack) {
      console.log('📹 视频轨道信息:', {
        label: videoTrack.label,
        enabled: videoTrack.enabled,
        readyState: videoTrack.readyState,
        muted: videoTrack.muted
      })
    }
    
    const audioTrack = stream.getAudioTracks()[0]
    if (audioTrack) {
      console.log('🎤 音频轨道信息:', {
        label: audioTrack.label,
        enabled: audioTrack.enabled,
        readyState: audioTrack.readyState,
        muted: audioTrack.muted
      })
      // 根据当前静音状态设置音频轨道
      audioTrack.enabled = !isMuted.value
    }
    
    // 先保存流
    localStream.value = stream
    console.log('💾 音视频流已保存到 localStream.value')
    
    // 将本地流设置到WebRTC管理器
    webrtcManager.setLocalStream(stream)
    console.log('🎬 本地流已设置到WebRTC管理器')
    
    // 先更新状态，让 Vue 渲染 video 元素
    isVideoOn.value = true
    console.log('🔄 isVideoOn 已设置为 true，等待 DOM 更新...')
    
    // 使用 nextTick 确保 DOM 已更新
    await new Promise(resolve => {
      setTimeout(() => {
        console.log('⏰ setTimeout 回调执行')
        
        if (localVideo.value) {
          console.log('✅ video 元素存在，设置 srcObject')
          localVideo.value.srcObject = stream
          console.log('✅ srcObject 已设置')
          
          // 确保视频开始播放
          localVideo.value.play().then(() => {
            console.log('✅ 视频播放成功')
          }).catch(err => {
            console.error('❌ 视频播放失败:', err)
          })
        } else {
          console.error('❌ localVideo.value 为 null，无法设置视频流')
        }
        resolve()
      }, 300)
    })
    
    // 调用后端API更新视频状态
    await meetingService.sendVideoChange(true)
    console.log('✅ 视频状态已更新到服务器')
    
  } catch (error) {
    console.error('❌ 开启摄像头失败:', error)
    
    let errorMessage = '无法访问摄像头'
    if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
      errorMessage = '摄像头权限被拒绝，请在浏览器设置中允许访问摄像头'
    } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
      errorMessage = '未检测到摄像头设备，请确保摄像头已连接'
    } else if (error.name === 'NotReadableError' || error.name === 'TrackStartError') {
      errorMessage = '摄像头正在被其他应用使用，请关闭其他应用后重试\n\n💡 提示：如果安装了OBS虚拟摄像头，请尝试使用 selectCameraAndStart() 命令选择不同的摄像头'
    }
    
    setCameraUnavailableState(error, autoVideoPreference.value)
    alert(errorMessage)
    throw error
  }
}

// 视频控制
const toggleVideo = async () => {
  const newVideoState = !isVideoOn.value
  
  console.log('🎥 toggleVideo 被调用，当前状态:', isVideoOn.value, '→ 新状态:', newVideoState)
  
  try {
    if (newVideoState) {
      // 开启视频 - 尝试使用默认摄像头
      console.log('🎥 尝试获取摄像头权限...')
      
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ 
          video: {
            width: { ideal: 1280 },
            height: { ideal: 720 }
          }, 
          audio: false 
        })
        
        console.log('✅ 摄像头权限获取成功')
        console.log('📹 视频轨道数:', stream.getVideoTracks().length)
        
        const videoTrack = stream.getVideoTracks()[0]
        if (videoTrack) {
          console.log('📹 视频轨道信息:', {
            label: videoTrack.label,
            enabled: videoTrack.enabled,
            readyState: videoTrack.readyState,
            muted: videoTrack.muted
          })
        }
        
        // 先保存流
        localStream.value = stream
        clearCameraUnavailableState()
        console.log('💾 视频流已保存到 localStream.value')
        
        // 将本地流设置到WebRTC管理器
        webrtcManager.setLocalStream(stream)
        console.log('🎬 本地流已设置到WebRTC管理器')
        
        // 先更新状态，让 Vue 渲染 video 元素
        isVideoOn.value = true
        console.log('🔄 isVideoOn 已设置为 true，等待 DOM 更新...')
        
        // 使用 nextTick 确保 DOM 已更新
        await new Promise(resolve => {
          setTimeout(() => {
            console.log('⏰ setTimeout 回调执行')
            console.log('🔍 检查 localVideo.value:', localVideo.value)
            console.log('🔍 检查 localVideo.value 类型:', localVideo.value?.constructor.name)
            
            if (localVideo.value) {
              console.log('✅ video 元素存在，设置 srcObject')
              localVideo.value.srcObject = stream
              console.log('✅ srcObject 已设置')
              
              // 打印 video 元素的属性
              console.log('📺 video 元素属性:', {
                videoWidth: localVideo.value.videoWidth,
                videoHeight: localVideo.value.videoHeight,
                readyState: localVideo.value.readyState,
                paused: localVideo.value.paused,
                muted: localVideo.value.muted,
                autoplay: localVideo.value.autoplay
              })
              
              // 确保视频开始播放
              localVideo.value.play().then(() => {
                console.log('✅ 视频播放成功')
                console.log('📺 播放后的 video 属性:', {
                  videoWidth: localVideo.value.videoWidth,
                  videoHeight: localVideo.value.videoHeight,
                  paused: localVideo.value.paused
                })
              }).catch(err => {
                console.error('❌ 视频播放失败:', err)
              })
            } else {
              console.error('❌ localVideo.value 为 null，无法设置视频流')
              console.error('❌ 这可能是因为 v-if 条件不满足或 ref 绑定失败')
            }
            resolve()
          }, 300)
        })
        
        // 调用后端API更新视频状态
        await meetingService.sendVideoChange(true)
        console.log('✅ 视频状态已更新到服务器')
        
      } catch (error) {
        console.error('❌ 获取摄像头失败:', error)
        
        // 如果是摄像头被占用，自动显示摄像头选择框
        if (error.name === 'NotReadableError' || error.name === 'TrackStartError') {
          console.log('摄像头被占用，显示摄像头选择框')
          await selectCameraAndStart()
          return
        }
        
        let errorMessage = '无法访问摄像头'
        if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
          errorMessage = '摄像头权限被拒绝，请在浏览器设置中允许访问摄像头'
        } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
          errorMessage = '未检测到摄像头设备，请确保摄像头已连接'
        }
        
        setCameraUnavailableState(error, true)
        alert(errorMessage)
        throw error
      }
      
    } else {
      // 关闭视频 - 停止摄像头流
      console.log('关闭摄像头')
      
      if (localStream.value) {
        localStream.value.getTracks().forEach(track => {
          track.stop()
          console.log('视频轨道已停止:', track.kind)
        })
        localStream.value = null
      }
      
      if (localVideo.value) {
        localVideo.value.srcObject = null
      }
      
      isVideoOn.value = false
      clearCameraUnavailableState()
      
      // 调用后端API更新视频状态
      await meetingService.sendVideoChange(false)
      console.log('视频状态已更新到服务器')
    }
    
  } catch (error) {
    console.error('切换视频状态失败:', error)
  }
}

// 画中画拖动功能
const startDrag = (event) => {
  isDragging.value = true
  
  // 计算鼠标相对于画中画窗口的偏移
  const rect = pipSharingUserCamera.value.getBoundingClientRect()
  dragOffset.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top
  }
  
  // 添加全局鼠标移动和释放事件
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  
  // 防止文本选择
  event.preventDefault()
}

const onDrag = (event) => {
  if (!isDragging.value) return
  
  // 计算新位置
  let newX = event.clientX - dragOffset.value.x
  let newY = event.clientY - dragOffset.value.y
  
  // 获取视频区域的边界
  const videoArea = document.querySelector('.video-area')
  if (videoArea && pipSharingUserCamera.value) {
    const videoRect = videoArea.getBoundingClientRect()
    const pipRect = pipSharingUserCamera.value.getBoundingClientRect()
    
    // 限制在视频区域内
    newX = Math.max(0, Math.min(newX, videoRect.width - pipRect.width))
    newY = Math.max(0, Math.min(newY, videoRect.height - pipRect.height))
  }
  
  pipPosition.value = { x: newX, y: newY }
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 观看者视频面板控制
const toggleViewerPanel = () => {
  viewerPanelExpanded.value = !viewerPanelExpanded.value
}

const toggleViewerDisplayMode = () => {
  viewerDisplayMode.value = viewerDisplayMode.value === 'self' ? 'all' : 'self'
  
  // 切换模式后，重新设置视频流
  nextTick(() => {
    updateViewerVideoStreams()
  })
}

const setViewerParticipantVideoRef = (userId, el) => {
  if (el) {
    viewerParticipantVideoRefs.value.set(userId, el)
  }
}

const updateViewerVideoStreams = () => {
  // 设置本地视频流
  if (viewerDisplayMode.value === 'self' && viewerLocalVideo.value && localStream.value) {
    viewerLocalVideo.value.srcObject = localStream.value
  } else if (viewerDisplayMode.value === 'all' && viewerLocalVideoAll.value && localStream.value) {
    viewerLocalVideoAll.value.srcObject = localStream.value
  }
  
  // 设置参与者视频流
  if (viewerDisplayMode.value === 'all') {
    participants.value.forEach(participant => {
      if (participant.videoOpen && participant.videoRef) {
        const viewerVideoEl = viewerParticipantVideoRefs.value.get(participant.userId)
        if (viewerVideoEl && participant.videoRef.srcObject) {
          viewerVideoEl.srcObject = participant.videoRef.srcObject
        }
      }
    })
  }
}

// 初始化观看者面板位置
const initViewerPanelPosition = () => {
  const videoArea = document.querySelector('.video-area')
  if (videoArea) {
    const rect = videoArea.getBoundingClientRect()
    viewerPanelPosition.value = {
      x: rect.width - 300,  // 右上角，距离右边 20px
      y: 60  // 距离顶部 60px
    }
  }
}

// 开始拖动观看者面板
const startDragViewerPanel = (event) => {
  isDraggingViewerPanel.value = true
  const rect = event.currentTarget.parentElement.getBoundingClientRect()
  const offsetX = event.clientX - rect.left
  const offsetY = event.clientY - rect.top
  
  const onMouseMove = (e) => {
    if (!isDraggingViewerPanel.value) return
    
    const videoArea = document.querySelector('.video-area')
    if (videoArea) {
      const videoRect = videoArea.getBoundingClientRect()
      let newX = e.clientX - videoRect.left - offsetX
      let newY = e.clientY - videoRect.top - offsetY
      
      // 限制在视频区域内
      newX = Math.max(0, Math.min(newX, videoRect.width - rect.width))
      newY = Math.max(0, Math.min(newY, videoRect.height - rect.height))
      
      viewerPanelPosition.value = { x: newX, y: newY }
    }
  }
  
  const onMouseUp = () => {
    isDraggingViewerPanel.value = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }
  
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
  
  event.preventDefault()
}

// 共享屏幕
const shareScreen = async () => {
  try {
    if (isScreenSharing.value) {
      // 停止屏幕共享
      console.log('🖥️ 停止屏幕共享')
      stopScreenShare()
      return
    }
    
    // 检查是否已有其他人在共享屏幕
    if (currentScreenSharingUserId.value && currentScreenSharingUserId.value !== currentUserId.value) {
      const sharingUser = allParticipants.value.find(p => p.userId === currentScreenSharingUserId.value)
      const sharingUserName = sharingUser ? sharingUser.name : '其他用户'
      alert(`${sharingUserName} 正在共享屏幕，同一时间只能有一个人共享屏幕。`)
      return
    }
    
    // 显示屏幕共享选项对话框
    showScreenShareOptions.value = true
  } catch (error) {
    console.error('❌ 显示屏幕共享选项失败:', error)
  }
}

// 开始屏幕共享（从选项对话框确认后调用）
const startScreenShare = async () => {
  try {
    console.log('🖥️ 开始屏幕共享...')
    console.log('🖥️ 选项:', screenShareOptions.value)
    
    // 关闭选项对话框
    showScreenShareOptions.value = false
    
    // 从设置管理器获取屏幕共享约束
    const constraints = settingsManager.getScreenShareConstraints()
    
    // 如果用户在对话框中选择了不共享音频，覆盖设置
    if (!screenShareOptions.value.shareAudio) {
      constraints.audio = false
    }
    
    console.log('🖥️ 屏幕共享约束:', constraints)
    
    // 获取屏幕共享流
    const stream = await navigator.mediaDevices.getDisplayMedia(constraints)
    
    console.log('✅ 屏幕共享流获取成功')
    console.log('📹 视频轨道数:', stream.getVideoTracks().length)
    console.log('🎤 音频轨道数:', stream.getAudioTracks().length)
    
    // 检查视频轨道详细信息
    const videoTrack = stream.getVideoTracks()[0]
    if (videoTrack) {
      console.log('📹 视频轨道详细信息:')
      console.log('  - ID:', videoTrack.id)
      console.log('  - Label:', videoTrack.label)
      console.log('  - Enabled:', videoTrack.enabled)
      console.log('  - Muted:', videoTrack.muted)
      console.log('  - ReadyState:', videoTrack.readyState)
      const settings = videoTrack.getSettings()
      console.log('  - Settings:', settings)
      console.log('  - Width:', settings.width)
      console.log('  - Height:', settings.height)
      console.log('  - FrameRate:', settings.frameRate)
    }
    
    // 保存屏幕共享流
    screenStream.value = stream
    
    // 监听用户停止共享（点击浏览器的停止共享按钮）
    stream.getVideoTracks()[0].onended = () => {
      console.log('🖥️ 用户停止了屏幕共享')
      stopScreenShare()
    }
    
    // 保存屏幕共享流（不替换摄像头流）
    // 使用 WebRTC 管理器创建屏幕共享连接
    console.log('🔍 准备调用 webrtcManager.startScreenShare')
    console.log('🔍 webrtcManager 存在:', !!webrtcManager)
    console.log('🔍 startScreenShare 方法存在:', !!webrtcManager.startScreenShare)
    
    await webrtcManager.startScreenShare(stream)
    console.log('✅ 屏幕共享连接已创建')
    
    // 更新本地显示：显示屏幕共享内容
    if (localVideo.value) {
      localVideo.value.srcObject = stream
      console.log('✅ 本地视频元素已更新为屏幕共享流')
    }
    
    isScreenSharing.value = true
    currentScreenSharingUserId.value = currentUserId.value
    
    // 初始化视频面板位置
    initVideoPanelPosition()
    
    // 初始化聊天面板位置
    initChatPanelPosition()
    
    // 在悬浮层中显示本地视频和屏幕共享预览
    await nextTick()
    if (localVideoOverlay.value && localStream.value) {
      localVideoOverlay.value.srcObject = localStream.value
    }
    
    // 设置屏幕共享预览
    if (localScreenSharePreview.value && screenStream.value) {
      localScreenSharePreview.value.srcObject = screenStream.value
      console.log('✅ 屏幕共享预览已设置')
    }
    
    // 通知其他用户：我开始共享屏幕
    meetingWsService.sendMessage({
      messageType: 16, // SCREEN_SHARE_START
      messageSend2Type: 1, // GROUP (发送给所有人)
      sendUserId: currentUserId.value,
      sendUserNickName: userName.value,
      meetingId: meetingId.value,
      messageContent: {
        userId: currentUserId.value,
        userName: userName.value
      },
      sendTime: Date.now()
    })
    
    console.log('✅ 屏幕共享已开启')
    
  } catch (error) {
    // 用户取消操作，静默处理
    if (error.message === '用户取消了屏幕共享' || error.name === 'NotAllowedError') {
      console.log('ℹ️ 用户取消了屏幕共享')
      return
    }
    
    // 其他错误才显示
    console.error('❌ 屏幕共享失败:', error)
    
    if (error.name === 'NotFoundError') {
      alert('未找到可共享的屏幕')
    } else {
      alert('屏幕共享失败: ' + error.message)
    }
  }
}

// 停止屏幕共享
const stopScreenShare = async () => {
  try {
    console.log('🖥️ 停止屏幕共享...')
    
    // 停止屏幕共享流
    if (screenStream.value) {
      screenStream.value.getTracks().forEach(track => {
        track.stop()
        console.log('停止轨道:', track.kind, track.label)
      })
      screenStream.value = null
    }
    
    // 关闭屏幕共享 WebRTC 连接
    await webrtcManager.stopScreenShare()
    
    isScreenSharing.value = false
    
    // 清除屏幕共享状态
    if (currentScreenSharingUserId.value === currentUserId.value) {
      currentScreenSharingUserId.value = null
      
      // 通知其他用户：我停止共享屏幕
      meetingWsService.sendMessage({
        messageType: 17, // SCREEN_SHARE_STOP
        messageSend2Type: 1, // GROUP (发送给所有人)
        sendUserId: currentUserId.value,
        sendUserNickName: userName.value,
        meetingId: meetingId.value,
        messageContent: {
          userId: currentUserId.value,
          userName: userName.value
        },
        sendTime: Date.now()
      })
    }
    
    // 恢复本地视频显示为摄像头
    if (isVideoOn.value && localStream.value && localVideo.value) {
      localVideo.value.srcObject = localStream.value
      console.log('✅ 本地视频元素已恢复为摄像头流')
    } else if (localVideo.value) {
      localVideo.value.srcObject = null
      console.log('✅ 本地视频元素已清空')
    }
    
    console.log('✅ 屏幕共享已停止')
    
  } catch (error) {
    console.error('❌ 停止屏幕共享失败:', error)
  }
}

// 暂停/恢复屏幕共享
const toggleScreenSharePause = () => {
  if (!screenStream.value) return
  
  isScreenSharePaused.value = !isScreenSharePaused.value
  
  // 暂停或恢复视频轨道
  const videoTrack = screenStream.value.getVideoTracks()[0]
  if (videoTrack) {
    videoTrack.enabled = !isScreenSharePaused.value
    console.log(isScreenSharePaused.value ? '⏸️ 屏幕共享已暂停' : '▶️ 屏幕共享已恢复')
  }
}

// 切换屏幕共享源（重新选择要共享的屏幕/窗口）
const switchScreenShareSource = async () => {
  if (!isScreenSharing.value || !screenStream.value) {
    console.warn('⚠️ 当前没有进行屏幕共享')
    return
  }
  
  try {
    console.log('🔄 切换屏幕共享源...')
    
    // 获取新的屏幕共享流
    const newStream = await navigator.mediaDevices.getDisplayMedia({
      video: {
        cursor: 'always',
        displaySurface: 'monitor'
      },
      audio: screenShareOptions.value.shareAudio ? {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      } : false
    })
    
    console.log('✅ 新的屏幕共享流获取成功')
    
    // 停止旧的屏幕共享流
    if (screenStream.value) {
      screenStream.value.getTracks().forEach(track => {
        track.stop()
        console.log('🛑 停止旧的轨道:', track.kind)
      })
    }
    
    // 更新屏幕共享流
    screenStream.value = newStream
    
    // 监听用户停止共享
    newStream.getVideoTracks()[0].onended = () => {
      console.log('🖥️ 用户停止了屏幕共享')
      stopScreenShare()
    }
    
    // 更新 WebRTC 连接中的屏幕共享轨道
    if (webrtcManager) {
      await webrtcManager.replaceScreenShareTrack(newStream.getVideoTracks()[0])
      console.log('✅ WebRTC 屏幕共享轨道已更新')
    }
    
    // 更新本地显示
    if (localVideo.value) {
      localVideo.value.srcObject = newStream
    }
    
    // 更新屏幕共享预览
    if (localScreenSharePreview.value) {
      localScreenSharePreview.value.srcObject = newStream
      console.log('✅ 屏幕共享预览已更新')
    }
    
    console.log('✅ 屏幕共享源切换成功')
  } catch (error) {
    console.error('❌ 切换屏幕共享源失败:', error)
    // 如果用户取消选择，不做任何操作
    if (error.name === 'NotAllowedError' || error.name === 'AbortError') {
      console.log('ℹ️ 用户取消了选择')
    } else {
      alert('切换屏幕共享源失败: ' + error.message)
    }
  }
}

// 分享会议
const shareMeeting = () => {
  if (!meetingNo.value) {
    alert('会议号不存在')
    return
  }
  
  const shareText = `邀请您参加会议\n会议名称：${meetingName.value || '快速会议'}\n会议号：${meetingNo.value}\n\n请使用 EasyMeeting 加入会议`
  
  // 使用 Electron 的剪贴板 API
  if (window.electron && window.electron.clipboard) {
    try {
      window.electron.clipboard.writeText(shareText)
      alert('会议信息已复制到剪贴板，可以分享给其他人')
      console.log('✅ 会议信息已复制')
    } catch (error) {
      console.error('复制失败:', error)
      alert('复制失败: ' + error.message)
    }
  } else {
    // 降级方案：显示会议信息
    alert(shareText)
  }
}

// 切换录制状态
const toggleRecording = () => {
  if (isRecording.value) {
    // 停止录制
    isRecording.value = false
    console.log('⏹️ 停止录制')
    alert('录制功能暂未实现')
  } else {
    // 开始录制
    isRecording.value = true
    console.log('⏺️ 开始录制')
    alert('录制功能暂未实现')
  }
}

// 启动会议时长计时器
const startMeetingDurationTimer = () => {
  meetingStartTime.value = Date.now()
  meetingDuration.value = 0
  
  // 每秒更新一次
  durationTimer.value = setInterval(() => {
    meetingDuration.value = Math.floor((Date.now() - meetingStartTime.value) / 1000)
  }, 1000)
  
  console.log('⏱️ 会议时长计时器已启动')
}

// 停止会议时长计时器
const stopMeetingDurationTimer = () => {
  if (durationTimer.value) {
    clearInterval(durationTimer.value)
    durationTimer.value = null
    console.log('⏱️ 会议时长计时器已停止')
  }
}

// 切换视频面板展开/折叠
const toggleVideoPanel = () => {
  videoPanelExpanded.value = !videoPanelExpanded.value
}

// 开始拖动视频面板
const startDragVideoPanel = (event) => {
  isDraggingVideoPanel.value = true
  const rect = event.currentTarget.parentElement.getBoundingClientRect()
  const offsetX = event.clientX - rect.left
  const offsetY = event.clientY - rect.top
  
  const onMouseMove = (e) => {
    if (!isDraggingVideoPanel.value) return
    
    let newX = e.clientX - offsetX
    let newY = e.clientY - offsetY
    
    // 限制在窗口范围内
    const maxX = window.innerWidth - 300
    const maxY = window.innerHeight - 100
    
    newX = Math.max(0, Math.min(newX, maxX))
    newY = Math.max(60, Math.min(newY, maxY))
    
    videoPanelPosition.value = { x: newX, y: newY }
  }
  
  const onMouseUp = () => {
    isDraggingVideoPanel.value = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }
  
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

// 设置悬浮层中参与者视频元素的引用
const setOverlayParticipantVideoRef = (userId, el) => {
  if (el) {
    overlayParticipantVideoRefs.value.set(userId, el)
    // 如果已有视频流，立即设置
    const participant = participants.value.find(p => p.userId === userId)
    if (participant && participant.videoRef && participant.videoRef.srcObject) {
      el.srcObject = participant.videoRef.srcObject
    }
  }
}

// 初始化视频面板位置（右上角）
const initVideoPanelPosition = () => {
  videoPanelPosition.value = {
    x: window.innerWidth - 320,
    y: 60
  }
}

// 初始化聊天面板位置（左下角）
const initChatPanelPosition = () => {
  chatPanelPosition.value = {
    x: 20,
    y: window.innerHeight - 420  // 距离底部420px（面板高度约400px）
  }
}

// 切换聊天面板展开/折叠
const toggleChatPanel = () => {
  chatPanelExpanded.value = !chatPanelExpanded.value
}

// 开始拖动聊天面板
const startDragChatPanel = (event) => {
  isDraggingChatPanel.value = true
  const rect = event.currentTarget.parentElement.getBoundingClientRect()
  const offsetX = event.clientX - rect.left
  const offsetY = event.clientY - rect.top
  
  const onMouseMove = (e) => {
    if (!isDraggingChatPanel.value) return
    
    let newX = e.clientX - offsetX
    let newY = e.clientY - offsetY
    
    // 限制在窗口范围内
    const maxX = window.innerWidth - 320
    const maxY = window.innerHeight - 100
    
    newX = Math.max(0, Math.min(newX, maxX))
    newY = Math.max(60, Math.min(newY, maxY))
    
    chatPanelPosition.value = { x: newX, y: newY }
  }
  
  const onMouseUp = () => {
    isDraggingChatPanel.value = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }
  
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
  
  event.preventDefault()
}

// 发送快速聊天消息
const sendQuickMessage = async () => {
  if (quickChatInput.value.trim()) {
    const messageText = quickChatInput.value.trim()
    
    try {
      console.log('发送快速聊天消息:', messageText)
      
      // 调用HTTP API发送消息
      const response = await chatService.sendMessage(
        messageText,
        MessageType.CHAT_TEXT_MESSAGE,
        '0'
      )
      
      console.log('快速消息发送成功:', response.data)
      
      // 清空输入框
      quickChatInput.value = ''
      
      // 关闭表情选择器
      showQuickEmojiPicker.value = false
      
    } catch (error) {
      console.error('发送快速消息失败:', error)
      alert('发送消息失败，请重试')
    }
  }
}

// 插入表情到主聊天输入框
const insertEmoji = (emoji) => {
  chatInput.value += emoji
  showEmojiPicker.value = false
}

// 插入表情到快速聊天输入框
const insertQuickEmoji = (emoji) => {
  quickChatInput.value += emoji
  showQuickEmojiPicker.value = false
}

// 发送聊天消息
const sendChatMessage = async (messageText) => {
  try {
    const response = await chatService.sendMessage(
      messageText,
      0, // receiveType: 0 = 群聊
      null, // receiveUserId
      null, // receiveUserNickName
      meetingId.value
    )
    
    if (response.data && response.data.code === 200) {
      console.log('✅ 消息发送成功')
    }
  } catch (error) {
    console.error('❌ 发送消息失败:', error)
  }
}

// 邀请参与者
const inviteParticipants = () => {
  console.log('打开邀请弹窗')
  showInviteModal.value = true
}

// 处理邀请用户
const handleInviteUser = async (userInfo) => {
  try {
    const response = await meetingService.inviteUserToMeeting(userInfo.userId)
    if (response.data.code === 200) {
      ElMessage.success(`已向 ${userInfo.nickName} 发送邀请`)
    } else {
      ElMessage.error(response.data.info || '邀请失败')
    }
  } catch (error) {
    console.error('邀请用户失败:', error)
    ElMessage.error('邀请失败，请重试')
  }
}

// 显示参与者列表
const showParticipants = () => {
  // 直接显示模态框，不重新加载（依赖WebSocket实时更新）
  showParticipantsModal.value = true
  console.log('显示参与者列表，当前成员数:', allParticipants.value.length)
}

// 加载参与者列表
const loadParticipants = async () => {
  if (!meetingId.value) {
    console.error('会议ID不存在')
    return
  }
  
  try {
    const response = await meetingService.getMeetingMembers(meetingId.value)
    if (response.data && response.data.code === 200 && response.data.data) {
      updateParticipantsList(response.data.data)
      console.log('加载参与者列表成功:', participants.value.length, '人')
      
      // 与所有参与者建立WebRTC连接
      await webrtcManager.connectToAllParticipants(participants.value)
    }
  } catch (error) {
    console.error('获取参与者列表失败:', error)
  }
}

// 计算时间戳（根据选择的时间范围）
const calculateMinMessageId = (timeRange) => {
  if (timeRange === 'all') {
    return null // 不传minMessageId，获取所有消息
  }
  
  const now = Date.now()
  let minutesAgo = 0
  
  switch (timeRange) {
    case '5m':
      minutesAgo = 5
      break
    case '15m':
      minutesAgo = 15
      break
    case '30m':
      minutesAgo = 30
      break
    case '1h':
      minutesAgo = 60
      break
    case '2h':
      minutesAgo = 120
      break
    case '6h':
      minutesAgo = 360
      break
    case '12h':
      minutesAgo = 720
      break
    case '24h':
      minutesAgo = 1440
      break
    default:
      return null
  }
  
  // 计算指定时间前的时间戳（作为最小消息ID）
  const targetTime = now - (minutesAgo * 60 * 1000)
  console.log(`时间范围: ${timeRange}, 起始时间: ${new Date(targetTime).toLocaleString()}, 时间戳: ${targetTime}`)
  return targetTime
}

// 时间范围改变处理
const onTimeRangeChange = async () => {
  console.log('时间范围改变:', selectedTimeRange.value)
  await loadChatHistory()
}

// 刷新消息
const refreshMessages = async () => {
  console.log('手动刷新消息')
  await loadChatHistory()
}

// 加载聊天历史消息
const loadChatHistory = async () => {
  try {
    console.log('加载聊天历史消息...')
    console.log('当前选择的时间范围:', selectedTimeRange.value)
    
    // 根据选择的时间范围计算minMessageId
    const minMessageId = calculateMinMessageId(selectedTimeRange.value)
    
    const response = await chatService.loadMessage(null, minMessageId, 1)
    
    if (response.data && response.data.code === 200 && response.data.data) {
      const messages = response.data.data.list || []
      console.log(`加载到历史消息: ${messages.length} 条 (时间范围: ${selectedTimeRange.value})`)
      
      // 创建一个新的消息数组
      const newMessages = []
      
      // 添加历史消息（按时间顺序）
      messages.reverse().forEach(msg => {
        const senderName = msg.sendUserNickName || '未知用户'
        const senderAvatar = getMemberAvatar(msg.sendUserId)
        
        // 判断是否为私聊消息
        const isPrivate = msg.receiveType === 1
        
        // 获取接收者信息（如果是私聊）
        let receiveUserName = ''
        if (isPrivate && msg.receiveUserId) {
          const receiver = participants.value.find(p => p.userId === msg.receiveUserId)
          receiveUserName = receiver ? receiver.name : '未知用户'
        }
        
        newMessages.push({
          id: msg.messageId,
          sender: senderName,
          avatar: senderAvatar,
          text: msg.messageContent,
          time: formatTime(msg.sendTime),
          timestamp: msg.sendTime,  // 添加时间戳用于日期分组
          sendUserId: msg.sendUserId,
          isPrivate: isPrivate,
          receiveUserId: msg.receiveUserId,
          receiveUserName: receiveUserName
        })
      })
      
      // 使用Set去重（基于消息ID）
      const messageMap = new Map()
      newMessages.forEach(msg => {
        messageMap.set(msg.id, msg)
      })
      
      // 将去重后的消息设置到chatMessages
      chatMessages.value = Array.from(messageMap.values())
      
      // 滚动到最新消息
      setTimeout(() => {
        const chatContainer = document.querySelector('.chat-messages')
        if (chatContainer) {
          chatContainer.scrollTop = chatContainer.scrollHeight
        }
      }, 100)
      
      // 显示时间范围信息
      if (minMessageId) {
        const timeRangeText = getTimeRangeText(selectedTimeRange.value)
        console.log(`✅ 已加载${timeRangeText}的消息`)
      } else {
        console.log('✅ 已加载所有历史消息')
      }
    }
  } catch (error) {
    console.error('加载聊天历史失败:', error)
  }
}

// 获取时间范围描述文本
const getTimeRangeText = (timeRange) => {
  const rangeMap = {
    '5m': '最近5分钟',
    '15m': '最近15分钟',
    '30m': '最近30分钟',
    '1h': '最近1小时',
    '2h': '最近2小时',
    '6h': '最近6小时',
    '12h': '最近12小时',
    '24h': '最近24小时',
    'all': '所有时间'
  }
  return rangeMap[timeRange] || '未知时间范围'
}

// 显示聊天
const showChat = async () => {
  // 加载历史消息
  await loadChatHistory()
  showChatModal.value = true
  
  // 清空未读消息计数
  unreadMessageCount.value = 0
}

// 发送消息
const sendMessage = async () => {
  if (chatInput.value.trim()) {
    const messageText = chatInput.value.trim()
    
    try {
      console.log('发送聊天消息:', messageText)
      
      // 调用HTTP API发送消息（会自动存储到数据库并通过WebSocket广播）
      const response = await chatService.sendMessage(
        messageText,                    // message
        MessageType.CHAT_TEXT_MESSAGE,  // messageType (5)
        '0'                            // receiveUserId ('0' 表示发送给所有人)
      )
      
      console.log('消息发送成功:', response.data)
      
      // 清空输入框
      chatInput.value = ''
      
      // 关闭表情选择器
      showEmojiPicker.value = false
      
      // 注意：不需要立即刷新，因为后端会通过WebSocket推送消息
      // WebSocket的chatMessage事件处理器会自动添加消息到聊天框
      console.log('等待WebSocket推送消息...')
      
    } catch (error) {
      console.error('发送消息失败:', error)
      alert('发送消息失败，请重试')
    }
  }
}

// 踢人（主持人权限）
const kickOutParticipant = async (userId) => {
  if (!isHost.value) return
  
  if (confirm('确定要将该成员踢出会议吗？')) {
    try {
      const response = await meetingService.kickOutMeeting(userId)
      console.log('踢人成功:', response)
      alert('踢出成功')
      
      // 从两个数组中移除
      participants.value = participants.value.filter(p => p.userId !== userId)
      allParticipants.value = allParticipants.value.filter(p => p.userId !== userId)
    } catch (error) {
      console.error('踢人失败:', error)
      alert('踢出失败，请稍后重试')
    }
  }
}

// 拉黑（主持人权限）
const blackParticipant = async (userId) => {
  if (!isHost.value) return
  
  if (confirm('确定要将该成员拉黑吗？')) {
    try {
      const response = await meetingService.blackMeeting(userId)
      console.log('拉黑成功:', response)
      alert('拉黑成功')
      
      // 从两个数组中移除
      participants.value = participants.value.filter(p => p.userId !== userId)
      allParticipants.value = allParticipants.value.filter(p => p.userId !== userId)
    } catch (error) {
      console.error('拉黑失败:', error)
      alert('拉黑失败，请稍后重试')
    }
  }
}

// 结束会议（主持人权限）
const endMeeting = async () => {
  if (!isHost.value) return
  
  if (confirm('确定要结束会议吗？')) {
    try {
      // 调用后端API结束会议
      const response = await meetingService.finishMeeting()
      console.log('结束会议成功:', response)
      
      // 确保后端响应成功
      if (response.data && response.data.code === 200) {
        // 通过WebSocket发送结束会议消息
        meetingWsService.sendFinishMeeting(userName.value)
        
        // 等待一小段时间确保消息发送完成
        await new Promise(resolve => setTimeout(resolve, 500))
        
        // 跳转到仪表板
        router.push('/dashboard')
      } else {
        alert('结束会议失败: ' + (response.data?.info || '未知错误'))
      }
    } catch (error) {
      console.error('结束会议失败:', error)
      alert('结束会议失败，请稍后重试')
    }
  }
}

// 离开会议
const leaveMeeting = async () => {
  if (confirm('确定要离开会议吗？')) {
    try {
      console.log('=== 开始离开会议流程 ===')
      console.log('当前用户ID:', currentUserId.value)
      console.log('当前用户名:', userName.value)
      
      // 调用后端API离开会议（后端会发送退出消息给其他人）
      console.log('1. 调用后端API离开会议...')
      const response = await meetingService.exitMeeting()
      console.log('后端API响应:', response)
      
      // 等待足够的时间，确保后端消息已通过Redis发送并分发到其他用户
      console.log('2. 等待消息分发...')
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      // 断开WebSocket连接
      console.log('3. 断开WebSocket连接...')
      meetingWsService.disconnect()
      
      // 跳转到仪表板
      console.log('4. 跳转到Dashboard...')
      router.push('/dashboard')
      console.log('=== 离开会议流程完成 ===')
    } catch (error) {
      console.error('❌ 离开会议失败:', error)
      alert('离开会议失败，请稍后重试')
    }
  }
}

// 强制退出会议（被踢出时调用，不需要确认）
const forceExitMeeting = async () => {
  try {
    console.log('强制退出会议...')
    
    // 调用后端API离开会议
    const response = await meetingService.exitMeeting()
    console.log('强制退出会议成功:', response)
    
    // 断开WebSocket连接
    meetingWsService.disconnect()
    
    // 跳转到仪表板
    router.push('/dashboard')
  } catch (error) {
    console.error('强制退出会议失败:', error)
    // 即使API调用失败，也要断开连接并跳转
    meetingWsService.disconnect()
    router.push('/dashboard')
  }
}

// 窗口控制
const minimizeWindow = () => {
  console.log('最小化窗口')
  // 在Electron环境中可以调用相应的API
  if (window.electronAPI) {
    window.electronAPI.minimizeWindow()
  }
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

const closeMeeting = () => {
  if (confirm('确定要关闭会议窗口吗？')) {
    // 在Electron环境中关闭窗口
    if (window.electronAPI) {
      window.electronAPI.closeWindow()
    } else {
      // 在浏览器中返回到仪表板
      router.push('/dashboard')
    }
  }
}

// 键盘快捷键
const handleKeydown = (event) => {
  if (event.ctrlKey) {
    switch (event.key.toLowerCase()) {
      case 'm':
        event.preventDefault()
        toggleMute()
        break
      case 'v':
        event.preventDefault()
        toggleVideo()
        break
      case 's':
        event.preventDefault()
        shareScreen()
        break
      case 'h':
        event.preventDefault()
        showChat()
        break
      case 'l':
        event.preventDefault()
        leaveMeeting()
        break
    }
  }
}

// 全屏状态监听
const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
}

// 调试方法：检查WebRTC连接状态
const debugWebRTC = () => {
  console.log('=== WebRTC调试信息 ===')
  console.log('当前用户ID:', currentUserId.value)
  console.log('会议ID:', meetingId.value)
  console.log('WebSocket连接状态:', meetingWsService.isConnected)
  console.log('Peer连接数量:', webrtcManager.peerConnections.size)
  console.log('远程流数量:', webrtcManager.remoteStreams.size)
  console.log('本地流存在:', !!webrtcManager.localStream)
  
  console.log('\n--- Peer连接详情 ---')
  webrtcManager.peerConnections.forEach((pc, userId) => {
    console.log(`用户 ${userId}:`, {
      connectionState: pc.connectionState,
      iceConnectionState: pc.iceConnectionState,
      signalingState: pc.signalingState
    })
  })
  
  console.log('\n--- 参与者列表 ---')
  console.log('participants:', participants.value.map(p => ({
    userId: p.userId,
    name: p.name,
    videoOpen: p.videoOpen,
    hasVideoRef: !!p.videoRef
  })))
  
  console.log('===================')
}

// 诊断并修复WebRTC连接
const diagnoseAndFix = async () => {
  console.log('=== WebRTC诊断和修复工具 ===\n')
  
  // 1. 基本检查
  console.log('1️⃣ 基本状态检查')
  const wsConnected = meetingWsService.isConnected
  const userId = currentUserId.value
  const meetingIdValue = meetingId.value
  const hasLocalStream = !!webrtcManager.localStream
  
  console.log('  WebSocket连接:', wsConnected)
  console.log('  用户ID:', userId)
  console.log('  会议ID:', meetingIdValue)
  console.log('  本地视频流:', hasLocalStream)
  
  if (!wsConnected) {
    console.log('\n❌ WebSocket未连接，无法修复')
    console.log('💡 建议：刷新页面重新加入会议')
    return
  }
  
  if (!hasLocalStream) {
    console.log('\n⚠️ 本地视频流不存在')
    console.log('💡 建议：先开启视频（点击视频按钮）')
    return
  }
  
  console.log('\n2️⃣ 检查Peer连接')
  const peerConnections = webrtcManager.peerConnections
  
  if (peerConnections.size === 0) {
    console.log('  ⚠️ 没有Peer连接')
    console.log('  💡 可能原因：对方未加入会议或连接未建立')
    return
  }
  
  console.log(`  找到 ${peerConnections.size} 个Peer连接\n`)
  
  // 3. 检查每个连接的状态
  console.log('3️⃣ 连接状态详情')
  let needsFix = false
  const fixes = []
  
  for (const [peerId, pc] of peerConnections.entries()) {
    console.log(`\n  连接 [${peerId}]:`)
    console.log(`    信令状态: ${pc.signalingState}`)
    console.log(`    连接状态: ${pc.connectionState}`)
    console.log(`    ICE状态: ${pc.iceConnectionState}`)
    console.log(`    远程描述: ${pc.remoteDescription ? '存在' : '不存在'}`)
    console.log(`    本地描述: ${pc.localDescription ? '存在' : '不存在'}`)
    
    // 诊断问题
    if (pc.signalingState === 'have-remote-offer') {
      console.log('    🔍 诊断：已收到远程Offer，但未发送Answer')
      needsFix = true
      fixes.push({ type: 'createAnswer', peerId, pc })
    } else if (pc.signalingState === 'have-local-offer') {
      console.log('    🔍 诊断：已发送Offer，等待对方Answer')
      console.log('    💡 建议：检查对方是否收到Offer消息')
    } else if (pc.connectionState === 'failed') {
      console.log('    🔍 诊断：连接失败')
      needsFix = true
      fixes.push({ type: 'reset', peerId, pc })
    } else if (pc.connectionState === 'connected') {
      console.log('    ✅ 连接正常')
    }
  }
  
  // 4. 执行修复
  if (needsFix && fixes.length > 0) {
    console.log('\n4️⃣ 执行修复')
    
    for (const fix of fixes) {
      if (fix.type === 'createAnswer') {
        console.log(`\n  修复连接 [${fix.peerId}]: 创建Answer`)
        try {
          const answer = await fix.pc.createAnswer()
          await fix.pc.setLocalDescription(answer)
          
          // 发送Answer
          meetingWsService.sendMessage({
            messageType: 14, // WEBRTC_ANSWER
            messageSend2Type: 1, // USER
            sendUserId: userId,
            receiveUserId: fix.peerId,
            meetingId: meetingIdValue,
            messageContent: {
              type: answer.type,
              sdp: answer.sdp
            }
          })
          
          console.log('  ✅ Answer已创建并发送')
        } catch (error) {
          console.error('  ❌ 创建Answer失败:', error)
        }
      } else if (fix.type === 'reset') {
        console.log(`\n  修复连接 [${fix.peerId}]: 重置连接`)
        webrtcManager.closePeerConnection(fix.peerId)
        setTimeout(() => {
          webrtcManager.connectToParticipant(fix.peerId)
        }, 1000)
        console.log('  ✅ 连接已重置')
      }
    }
    
    console.log('\n✅ 修复完成')
    console.log('💡 等待5-10秒后执行 checkConnection() 查看结果')
  } else if (needsFix) {
    console.log('\n⚠️ 检测到问题但无法自动修复')
    console.log('💡 建议：执行 resetAllConnections() 重置所有连接')
  } else {
    console.log('\n✅ 所有连接状态正常')
  }
  
  console.log('\n=== 诊断完成 ===\n')
}

// 检查连接状态
const checkConnection = () => {
  console.log('=== 连接状态检查 ===\n')
  
  const peerConnections = webrtcManager.peerConnections
  if (!peerConnections || peerConnections.size === 0) {
    console.log('❌ 没有Peer连接')
    return
  }
  
  let allConnected = true
  
  for (const [peerId, pc] of peerConnections.entries()) {
    const isConnected = pc.connectionState === 'connected'
    const hasRemoteStream = webrtcManager.remoteStreams.has(peerId)
    
    console.log(`连接 [${peerId}]:`)
    console.log(`  信令状态: ${pc.signalingState}`)
    console.log(`  连接状态: ${pc.connectionState}`)
    console.log(`  ICE状态: ${pc.iceConnectionState}`)
    console.log(`  远程流: ${hasRemoteStream ? '✅ 存在' : '❌ 不存在'}`)
    
    if (!isConnected) {
      allConnected = false
    }
    
    console.log('')
  }
  
  if (allConnected) {
    console.log('✅ 所有连接已建立')
    console.log('💡 应该能看到对方的视频了')
  } else {
    console.log('⚠️ 部分连接未建立')
    console.log('💡 建议：执行 diagnoseAndFix() 尝试修复')
  }
  
  console.log('=== 检查完成 ===\n')
}

// 重置所有连接
const resetAllConnections = () => {
  console.log('=== 重置所有连接 ===\n')
  
  const peerIds = Array.from(webrtcManager.peerConnections.keys())
  
  if (peerIds.length === 0) {
    console.log('❌ 没有需要重置的连接')
    return
  }
  
  console.log('关闭现有连接:', peerIds)
  peerIds.forEach(peerId => {
    webrtcManager.closePeerConnection(peerId)
  })
  
  console.log('\n等待2秒后重新建立连接...')
  setTimeout(() => {
    peerIds.forEach(peerId => {
      console.log('重新连接到:', peerId)
      webrtcManager.connectToParticipant(peerId)
    })
    console.log('\n✅ 连接重置完成')
    console.log('💡 等待5-10秒后执行 checkConnection() 查看结果')
  }, 2000)
}

// 创建假视频流（用于测试，不需要真实摄像头）
const createFakeVideoStream = () => {
  console.log('🎨 创建假视频流...')
  
  const canvas = document.createElement('canvas')
  canvas.width = 640
  canvas.height = 480
  const ctx = canvas.getContext('2d')
  
  let hue = 0
  function draw() {
    hue = (hue + 1) % 360
    const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height)
    gradient.addColorStop(0, `hsl(${hue}, 100%, 50%)`)
    gradient.addColorStop(1, `hsl(${(hue + 60) % 360}, 100%, 50%)`)
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    
    ctx.fillStyle = 'white'
    ctx.font = 'bold 48px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('测试视频流', canvas.width / 2, canvas.height / 2 - 40)
    
    ctx.font = '24px Arial'
    ctx.fillText(new Date().toLocaleTimeString(), canvas.width / 2, canvas.height / 2 + 20)
    
    ctx.font = '20px Arial'
    const userId = currentUserId.value || 'Unknown'
    ctx.fillText(`用户: ${userId}`, canvas.width / 2, canvas.height / 2 + 60)
  }
  
  setInterval(draw, 1000 / 30)
  const stream = canvas.captureStream(30)
  
  console.log('✅ 假视频流已创建')
  return stream
}

// 使用假视频流
const useFakeVideo = () => {
  console.log('=== 使用假视频流 ===\n')
  
  const fakeStream = createFakeVideoStream()
  
  localStream.value = fakeStream
  webrtcManager.setLocalStream(fakeStream)
  
  if (localVideo.value) {
    localVideo.value.srcObject = fakeStream
    localVideo.value.play()
    console.log('✅ 假视频已设置到本地video元素')
  }
  
  isVideoOn.value = true
  
  console.log('✅ 假视频流设置完成')
  console.log('💡 现在可以执行 diagnoseAndFix() 测试WebRTC连接\n')
}

// 将调试方法暴露到window，方便在控制台调用
if (typeof window !== 'undefined') {
  window.debugWebRTC = debugWebRTC
  window.diagnoseAndFix = diagnoseAndFix
  window.checkConnection = checkConnection
  window.resetAllConnections = resetAllConnections
  window.useFakeVideo = useFakeVideo
  window.selectCameraAndStart = selectCameraAndStart
  window.webrtcManager = webrtcManager
  window.meetingWsService = meetingWsService
  window.meetingVue = { participants, isVideoOn, currentUserId }
  
  console.log('✅ WebRTC调试工具已加载')
  console.log('可用命令：')
  console.log('  debugWebRTC()           - 查看详细调试信息')
  console.log('  diagnoseAndFix()        - 诊断并自动修复问题')
  console.log('  checkConnection()       - 检查连接状态')
  console.log('  resetAllConnections()   - 重置所有连接')
  console.log('  useFakeVideo()          - 使用假视频流（不需要真实摄像头）')
  console.log('  selectCameraAndStart()  - 选择摄像头并开启视频')
}

onMounted(async () => {
  await loadUserInfo()
  
  // 检查是否从即时会议邀请跳转过来（有 meetingNo 和 password 参数）
  const queryMeetingNo = route.query.meetingNo
  const queryPassword = route.query.password
  
  if (queryMeetingNo && !meetingId.value) {
    // 从即时会议邀请跳转过来，需要先调用 preJoinMeeting
    try {
      console.log('从即时会议邀请加入，会议号:', queryMeetingNo)
      const preJoinResponse = await meetingService.preJoinMeeting(
        queryMeetingNo,
        userName.value || '用户',
        queryPassword || null
      )
      
      if (preJoinResponse.data.code === 200) {
        // preJoinMeeting 成功后，响应的 data 就是 meetingId
        const responseMeetingId = preJoinResponse.data.data
        console.log('获取到 meetingId:', responseMeetingId)
        
        if (responseMeetingId) {
          // 更新路由，添加 meetingId 到 query 参数
          await router.replace({
            path: '/meeting',
            query: {
              meetingId: responseMeetingId
            }
          })
          
          // 等待路由更新后再调用 joinMeeting
          await nextTick()
          await joinMeeting()
        } else {
          throw new Error('无法获取会议ID')
        }
      } else {
        alert(preJoinResponse.data.info || '加入会议失败')
        router.push('/dashboard')
      }
    } catch (error) {
      console.error('从即时会议邀请加入失败:', error)
      alert('加入会议失败，请重试')
      router.push('/dashboard')
    }
  } else {
    // 正常流程，直接加入会议
    await joinMeeting()
  }
  
  // 启动会议时长计时器
  startMeetingDurationTimer()
  
  // 添加键盘事件监听
  document.addEventListener('keydown', handleKeydown)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  
  // 添加WebSocket连接状态监控
  const connectionCheckInterval = setInterval(() => {
    // 检查WebSocket连接状态
    if (wsService.ws && wsService.ws.readyState !== WebSocket.OPEN) {
      console.warn('⚠️ WebSocket连接异常，状态:', wsService.ws.readyState)
      console.warn('⚠️ 状态说明:', 
        wsService.ws.readyState === 0 ? 'CONNECTING' :
        wsService.ws.readyState === 2 ? 'CLOSING' :
        wsService.ws.readyState === 3 ? 'CLOSED' : '未知')
      
      // 如果连接已关闭且应该重连，触发重连
      if (wsService.ws.readyState === WebSocket.CLOSED && 
          wsService.shouldReconnect && 
          wsService.currentToken && 
          wsService.currentUserId) {
        console.log('🔄 触发WebSocket重连...')
        wsService.connect(wsService.currentToken, wsService.currentUserId)
          .then(() => {
            console.log('✅ WebSocket重连成功')
            // 重新初始化WebRTC
            webrtcManager.init(meetingWsService, currentUserId.value, meetingId.value)
          })
          .catch(err => {
            console.error('❌ WebSocket重连失败:', err)
          })
      }
    }
  }, 5000) // 每5秒检查一次
  
  // 保存interval ID以便清理
  window.connectionCheckInterval = connectionCheckInterval
})

onUnmounted(() => {
  // 停止会议时长计时器
  stopMeetingDurationTimer()
  
  // 清理连接状态监控
  if (window.connectionCheckInterval) {
    clearInterval(window.connectionCheckInterval)
    window.connectionCheckInterval = null
  }
  
  // 清理WebRTC资源
  webrtcManager.destroy()
  console.log('WebRTC资源已清理')
  
  // 清理视频流
  if (localStream.value) {
    localStream.value.getTracks().forEach(track => {
      track.stop()
      console.log('清理视频轨道:', track.kind)
    })
    localStream.value = null
  }
  
  // 移除事件监听
  document.removeEventListener('keydown', handleKeydown)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  
  // 断开WebSocket连接
  meetingWsService.disconnect()
})
</script>

<style scoped>
/* 会议容器 */
.meeting-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #363636;
  color: #dfdfdf;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  position: relative;
}

/* 屏幕共享模式 */
.meeting-container.screen-sharing-mode {
  background-color: transparent;
}

/* 正常会议视图 */
.normal-meeting-view {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 会议顶部栏 */
.meeting-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background-color: #434343;
  border-bottom: 1px solid #555555;
  height: 60px;
  z-index: 100;
  transition: all 0.3s ease;
}

/* 紧凑模式（屏幕共享时） */
.meeting-header.compact-mode {
  height: 40px;
  padding: 6px 16px;
  background-color: rgba(67, 67, 67, 0.95);
  backdrop-filter: blur(10px);
}

.meeting-header.compact-mode .meeting-info-btn {
  padding: 4px 12px;
  font-size: 12px;
}

.meeting-header.compact-mode .window-btn {
  width: 28px;
  height: 28px;
}

/* 左上角会议详情按钮 */
.meeting-info-section {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.meeting-info-btn {
  background: none;
  border: 1px solid #666666;
  color: #dfdfdf;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.meeting-info-btn:hover {
  background-color: rgba(153, 153, 153, 0.2);
  border-color: #999999;
}

/* 顶部栏中的会议时长显示 */
.header-duration {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background-color: rgba(76, 175, 80, 0.2);
  border: 1px solid rgba(76, 175, 80, 0.4);
  border-radius: 6px;
  color: #4caf50;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  font-weight: 600;
}

.duration-icon {
  font-size: 14px;
}

.duration-text {
  min-width: 50px;
  text-align: center;
}

/* 右上角窗口控制按钮 */
.window-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.window-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  font-size: 16px;
  font-weight: bold;
}

.settings-btn {
  background-color: #555555;
  color: #dfdfdf;
}

.settings-btn:hover {
  background-color: #666666;
}

.minimize-btn {
  background-color: #555555;
  color: #dfdfdf;
}

.minimize-btn:hover {
  background-color: #666666;
}

.fullscreen-btn {
  background-color: #555555;
  color: #dfdfdf;
}

.fullscreen-btn:hover {
  background-color: #666666;
}

.close-btn {
  background-color: #e74c3c;
  color: #ffffff;
}

.close-btn:hover {
  background-color: #c0392b;
}

.window-icon {
  width: 16px;
  height: 16px;
  filter: brightness(0) saturate(100%) invert(100%);
}

.settings-btn .window-icon {
  filter: brightness(0) saturate(100%) invert(87%) sepia(0%) saturate(0%) hue-rotate(0deg) brightness(223%) contrast(87%);
}

/* 会议主体区域 */
.meeting-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow: hidden;
}

.video-area {
  width: 100%;
  max-width: 1200px;
  height: 100%;
  max-height: 700px;
  background-color: #2c2c2c;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #444444;
  padding: 20px;
  overflow: auto;
  position: relative;
}

/* 屏幕共享视图 */
.screen-share-view {
  width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #000000;
  border-radius: 8px;
  overflow: hidden;
}

.screen-share-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background-color: #000000;
}

/* 画中画摄像头窗口 */
.pip-camera {
  position: absolute;
  width: 240px;
  height: 180px;
  background-color: #1a1a1a;
  border: 2px solid #4CAF50;
  border-radius: 8px;
  overflow: hidden;
  cursor: move;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.6);
  z-index: 10;
  transition: box-shadow 0.2s ease;
}

.pip-camera:hover {
  box-shadow: 0 6px 24px rgba(76, 175, 80, 0.4);
  border-color: #66BB6A;
}

.pip-camera:active {
  cursor: grabbing;
}

.pip-camera-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background-color: #000000;
}

.pip-camera-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pip-camera-name {
  color: #ffffff;
  font-size: 12px;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

/* 观看者视频面板（可拖动悬浮窗口） */
.viewer-video-panel {
  position: fixed;
  width: 280px;
  max-height: 600px;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
  z-index: 100;
  display: flex;
  flex-direction: column;
}

.viewer-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  cursor: move;
  user-select: none;
  flex-shrink: 0;
}

.viewer-panel-header:active {
  cursor: grabbing;
}

.viewer-panel-title {
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
}

.viewer-panel-controls {
  display: flex;
  gap: 6px;
}

.viewer-panel-mode-btn,
.viewer-panel-toggle-btn {
  width: 28px;
  height: 28px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.viewer-panel-mode-btn:hover,
.viewer-panel-toggle-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.viewer-panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.viewer-panel-content::-webkit-scrollbar {
  width: 6px;
}

.viewer-panel-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.viewer-panel-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.viewer-panel-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.viewer-video-item {
  position: relative;
  width: 100%;
  aspect-ratio: 16/9;
  background: #1a1a1a;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 10px;
}

.viewer-video-item:last-child {
  margin-bottom: 0;
}

.viewer-video-item.my-video {
  border: 2px solid #4caf50;
}

.viewer-video-element,
.viewer-avatar-element {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.viewer-video-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.viewer-video-name {
  color: #ffffff;
  font-size: 12px;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.viewer-audio-btn {
  width: 28px;
  height: 28px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.viewer-audio-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.viewer-audio-btn.muted {
  background: rgba(244, 67, 54, 0.3);
  color: #f44336;
}

.viewer-audio-status {
  color: #4caf50;
  font-size: 14px;
}

.viewer-audio-status.muted {
  color: #f44336;
}

/* 参与者网格布局 */
.participants-grid {
  width: 100%;
  height: 100%;
  display: grid;
  gap: 16px;
  align-items: center;
  justify-content: center;
}

/* 1人：单个大视图 */
.participants-grid.grid-1 {
  grid-template-columns: 1fr;
  max-width: 600px;
  margin: 0 auto;
}

/* 2人：横向排列 */
.participants-grid.grid-2 {
  grid-template-columns: repeat(2, 1fr);
}

/* 3-4人：2x2网格 */
.participants-grid.grid-4 {
  grid-template-columns: repeat(2, 1fr);
}

/* 5-6人：2x3网格 */
.participants-grid.grid-6 {
  grid-template-columns: repeat(3, 1fr);
}

/* 7-9人：3x3网格 */
.participants-grid.grid-9 {
  grid-template-columns: repeat(3, 1fr);
}

/* 更多人：4列网格 */
.participants-grid.grid-many {
  grid-template-columns: repeat(4, 1fr);
}

/* 参与者视频项 */
.participant-video-item {
  position: relative;
  aspect-ratio: 4/3;
  min-height: 150px;
  max-height: 300px;
}

.video-frame {
  position: relative;
  width: 100%;
  height: 100%;
  background-color: #1a1a1a;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid #444444;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.video-frame:hover {
  border-color: #666666;
  transform: scale(1.02);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

/* 参与者头像 */
.participant-avatar-large {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #555555;
  transition: all 0.3s ease;
}

.video-frame:hover .participant-avatar-large {
  transform: scale(1.05);
  border-color: #777777;
}

/* 参与者视频流 */
.participant-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background-color: #000000;
}

.camera-fallback-card {
  width: 100%;
  height: 100%;
  padding: 28px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #f8fafc;
  background:
    radial-gradient(circle at top, rgba(59, 130, 246, 0.35), transparent 45%),
    linear-gradient(160deg, #1e293b 0%, #0f172a 100%);
}

.camera-state-permission {
  background:
    radial-gradient(circle at top, rgba(245, 158, 11, 0.34), transparent 45%),
    linear-gradient(160deg, #3b2f15 0%, #1f2937 100%);
}

.camera-state-busy {
  background:
    radial-gradient(circle at top, rgba(239, 68, 68, 0.34), transparent 45%),
    linear-gradient(160deg, #3f1d24 0%, #111827 100%);
}

.camera-state-missing {
  background:
    radial-gradient(circle at top, rgba(56, 189, 248, 0.32), transparent 45%),
    linear-gradient(160deg, #172554 0%, #0f172a 100%);
}

.participant-camera-fallback {
  gap: 12px;
}

.overlay-camera-fallback,
.viewer-camera-fallback {
  gap: 8px;
  padding: 16px;
}

.camera-fallback-badge {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(96, 165, 250, 0.16);
  border: 1px solid rgba(147, 197, 253, 0.35);
  font-size: 12px;
  color: #bfdbfe;
}

.camera-state-permission .camera-fallback-badge {
  background: rgba(245, 158, 11, 0.14);
  border-color: rgba(251, 191, 36, 0.34);
  color: #fde68a;
}

.camera-state-busy .camera-fallback-badge {
  background: rgba(239, 68, 68, 0.14);
  border-color: rgba(248, 113, 113, 0.32);
  color: #fecaca;
}

.camera-state-missing .camera-fallback-badge {
  background: rgba(56, 189, 248, 0.16);
  border-color: rgba(125, 211, 252, 0.34);
  color: #bae6fd;
}

.camera-fallback-icon {
  font-size: 36px;
  line-height: 1;
}

.camera-fallback-title {
  font-size: 20px;
  font-weight: 700;
  color: #ffffff;
}

.camera-fallback-text {
  max-width: 320px;
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(226, 232, 240, 0.92);
}

.camera-fallback-actions {
  display: flex;
  gap: 10px;
  margin-top: 6px;
}

.camera-fallback-btn {
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  color: #e2e8f0;
  border-radius: 999px;
  padding: 10px 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.camera-fallback-btn:hover {
  background: rgba(255, 255, 255, 0.14);
}

.camera-fallback-btn.primary {
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  border-color: transparent;
  color: #ffffff;
}

.camera-fallback-btn.primary:hover {
  filter: brightness(1.08);
}

/* 参与者信息覆盖层 */
.participant-info-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.participant-name {
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  gap: 6px;
}

.host-badge {
  display: inline-block;
  padding: 2px 8px;
  background-color: #f39c12;
  color: white;
  font-size: 11px;
  border-radius: 10px;
  font-weight: 500;
}

.participant-status {
  display: flex;
  gap: 6px;
  align-items: center;
}

.status-icon {
  font-size: 16px;
  opacity: 0.9;
}

.status-icon.muted {
  filter: drop-shadow(0 0 2px rgba(231, 76, 60, 0.8));
}

.status-icon.video-off {
  filter: drop-shadow(0 0 2px rgba(52, 152, 219, 0.8));
}

.status-icon.screen-sharing {
  filter: drop-shadow(0 0 2px rgba(76, 175, 80, 0.8));
}

/* 响应式布局 */
@media (max-width: 1024px) {
  .participants-grid.grid-many {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .participants-grid.grid-6,
  .participants-grid.grid-9,
  .participants-grid.grid-many {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .participant-avatar-large {
    width: 80px;
    height: 80px;
  }
}

@media (max-width: 480px) {
  .participants-grid {
    grid-template-columns: 1fr !important;
  }
}

/* 会议底部控制栏 */
.meeting-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background-color: #434343;
  border-top: 1px solid #555555;
  min-height: 80px;
}

.meeting-primary-toolbar {
  position: fixed;
  right: 18px;
  bottom: 110px;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  gap: 10px;
  width: 148px;
  padding: 10px;
  border-radius: 24px;
  background: rgba(15, 23, 42, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 50px rgba(2, 6, 23, 0.34);
  backdrop-filter: blur(18px);
}

.primary-toolbar-btn {
  width: 100%;
  min-width: 0;
  height: 58px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}

.primary-toolbar-btn:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(56, 189, 248, 0.24);
}

.primary-toolbar-btn.active {
  background: rgba(239, 68, 68, 0.18);
  border-color: rgba(248, 113, 113, 0.34);
}

.primary-toolbar-icon {
  font-size: 18px;
  line-height: 1;
}

.primary-toolbar-text {
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.control-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 16px;
  border: none;
  border-radius: 8px;
  background-color: #555555;
  color: #dfdfdf;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 70px;
  height: 60px;
  position: relative;
}

.control-button:hover {
  background-color: #666666;
  transform: translateY(-2px);
}

.control-button.active {
  background-color: #999999;
  color: #ffffff;
}

/* 未读消息徽章 */
.unread-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background-color: #f44336;
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  animation: badge-pulse 2s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

.leave-btn {
  background-color: #e74c3c;
  color: #ffffff;
}

.leave-btn:hover {
  background-color: #c0392b;
}

.end-meeting-btn {
  background-color: #9b59b6;
  color: #ffffff;
}

.end-meeting-btn:hover {
  background-color: #8e44ad;
}

.control-icon {
  width: 20px;
  height: 20px;
  filter: brightness(0) saturate(100%) invert(87%) sepia(0%) saturate(0%) hue-rotate(0deg) brightness(223%) contrast(87%);
}

.control-button.active .control-icon {
  filter: brightness(0) saturate(100%) invert(100%);
}

.leave-btn .control-icon,
.end-meeting-btn .control-icon {
  filter: brightness(0) saturate(100%) invert(100%);
}

.control-text {
  font-size: 11px;
  font-weight: 500;
  text-align: center;
}

/* 浮动模态框 */
.floating-modal {
  position: fixed;
  z-index: 1000;
  pointer-events: auto;
}

.meeting-info-modal {
  top: 70px;
  left: 20px;
  width: 320px;
}

.settings-modal {
  top: 70px;
  right: 20px;
  width: 400px;
}

.floating-modal .modal-content {
  background-color: #434343;
  border-radius: 12px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.5);
  border: 1px solid #555555;
  animation: modalSlideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #555555;
}

.modal-header h3 {
  margin: 0;
  color: #dfdfdf;
  font-size: 16px;
  font-weight: 600;
}

.modal-close {
  background: none;
  border: none;
  color: #dfdfdf;
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.modal-body {
  padding: 20px;
}

/* 会议详情模态框 */
.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  gap: 8px;
}

.info-item label {
  font-weight: 500;
  color: #dfdfdf;
  min-width: 80px;
  font-size: 14px;
}

.info-item span {
  color: #dfdfdf;
  font-size: 14px;
}

.copy-btn {
  background: none;
  border: 1px solid #666666;
  color: #dfdfdf;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s ease;
  margin-left: 8px;
}

.copy-btn:hover {
  background-color: rgba(153, 153, 153, 0.2);
  border-color: #999999;
}

/* 会议号容器和输入框样式 */
.meeting-no-container {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.meeting-no-input {
  background-color: #555555;
  border: 1px solid #666666;
  color: #dfdfdf;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
  font-family: 'Courier New', monospace;
  min-width: 120px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.meeting-no-input:focus {
  outline: none;
  border-color: #999999;
  background-color: #666666;
}

.meeting-no-input:hover {
  border-color: #777777;
}

/* 设置模态框 */
.settings-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 20px;
  border-bottom: 1px solid #555555;
  padding-bottom: 16px;
}

.settings-item {
  background: none;
  border: none;
  color: #dfdfdf;
  padding: 12px 16px;
  text-align: left;
  cursor: pointer;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.settings-item:hover {
  background-color: rgba(153, 153, 153, 0.1);
}

.settings-item.active {
  background-color: rgba(153, 153, 153, 0.2);
  color: #ffffff;
}

.settings-content {
  min-height: 200px;
}

.settings-panel h4 {
  margin: 0 0 16px 0;
  color: #dfdfdf;
  font-size: 16px;
  font-weight: 600;
}

.setting-option {
  margin-bottom: 12px;
}

.setting-option label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #dfdfdf;
  font-size: 14px;
  cursor: pointer;
}

.setting-option input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #999999;
}

.shortcut-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.shortcut-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #555555;
}

.shortcut-item:last-child {
  border-bottom: none;
}

.shortcut-desc {
  color: #dfdfdf;
  font-size: 14px;
}

.shortcut-key {
  background-color: #555555;
  color: #dfdfdf;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-family: 'Courier New', monospace;
}

.about-content {
  color: #dfdfdf;
  line-height: 1.6;
}

.about-content p {
  margin-bottom: 8px;
}

.about-content .copyright {
  margin-top: 20px;
  font-size: 12px;
  color: #999999;
}

/* 覆盖式模态框（成员列表、聊天） */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.modal-overlay .modal-content {
  background-color: #434343;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6);
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
  border: 1px solid #555555;
}

/* 参与者列表 */
.participant-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.participant-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background-color: #363636;
  border-radius: 8px;
  border: 1px solid #555555;
}

.participant-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.participant-info {
  flex: 1;
}

.participant-info h4 {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: #dfdfdf;
  display: flex;
  align-items: center;
  gap: 8px;
}

.participant-badge {
  background-color: #e67e22;
  color: #ffffff;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: bold;
}

.participant-status {
  margin: 0;
  font-size: 12px;
  color: #999999;
}

.participant-actions {
  display: flex;
  gap: 6px;
}

.action-button {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
}

.action-button.kick {
  background-color: #e74c3c;
  color: #ffffff;
}

.action-button.kick:hover {
  background-color: #c0392b;
}

.action-button.black {
  background-color: #95a5a6;
  color: #ffffff;
}

.action-button.black:hover {
  background-color: #7f8c8d;
}

/* 聊天 */
.chat-modal-content {
  width: 500px;
  max-width: 90vw;
}

.chat-modal-body {
  display: flex;
  flex-direction: column;
  height: 500px;
}

.chat-timeline-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background-color: #434343;
  border-radius: 6px;
  margin-bottom: 16px;
  border: 1px solid #555555;
}

.chat-timeline-selector label {
  font-size: 14px;
  color: #dfdfdf;
  white-space: nowrap;
  font-weight: 500;
}

.time-range-select {
  padding: 6px 10px;
  border: 1px solid #555555;
  border-radius: 4px;
  background-color: #363636;
  color: #dfdfdf;
  font-size: 13px;
  cursor: pointer;
  min-width: 120px;
}

.time-range-select:focus {
  outline: none;
  border-color: #999999;
}

.time-range-select option {
  background-color: #363636;
  color: #dfdfdf;
}

.refresh-button {
  padding: 6px 12px;
  border: 1px solid #555555;
  border-radius: 4px;
  background-color: #555555;
  color: #dfdfdf;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.refresh-button:hover {
  background-color: #666666;
  border-color: #777777;
}

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  overflow-y: auto;
  margin-bottom: 16px;
  padding-right: 8px;
}

/* 日期分隔符 */
.date-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 16px 0 12px 0;
  position: relative;
}

.date-divider::before,
.date-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, transparent, rgba(255, 255, 255, 0.1), transparent);
}

.date-text {
  padding: 4px 16px;
  background-color: rgba(67, 67, 67, 0.8);
  color: #999;
  font-size: 12px;
  border-radius: 12px;
  margin: 0 12px;
  white-space: nowrap;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.chat-message {
  display: flex;
  gap: 12px;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.chat-message:hover {
  background-color: rgba(255, 255, 255, 0.03);
}

/* 私聊消息样式 */
.chat-message.private-message {
  background-color: rgba(52, 152, 219, 0.1);
  border-left: 3px solid #3498db;
}

.chat-message.private-message:hover {
  background-color: rgba(52, 152, 219, 0.15);
}

/* 自己发送的消息 */
.chat-message.my-message .message-content {
  background-color: #2c5282;
  border-color: #3182ce;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid #555555;
}

.message-content {
  flex: 1;
  background-color: #363636;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #555555;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.message-content h4 {
  margin: 0;
  font-size: 14px;
  color: #dfdfdf;
  font-weight: 600;
}

.private-badge {
  display: inline-block;
  padding: 2px 8px;
  background-color: #3498db;
  color: white;
  font-size: 11px;
  border-radius: 10px;
  font-weight: 500;
}

.receive-info {
  font-size: 12px;
  color: #999999;
  font-style: italic;
}

.message-text {
  margin: 0 0 6px 0;
  font-size: 14px;
  color: #dfdfdf;
  line-height: 1.5;
  word-wrap: break-word;
}

.message-time {
  font-size: 11px;
  color: #999999;
}

.chat-input-area {
  display: flex;
  gap: 8px;
  position: relative;
}

.emoji-btn {
  width: 40px;
  height: 40px;
  border: 1px solid #555555;
  border-radius: 6px;
  background-color: #363636;
  color: #dfdfdf;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.emoji-btn:hover {
  background-color: #444444;
  border-color: #999999;
}

.chat-input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #555555;
  border-radius: 6px;
  background-color: #363636;
  color: #dfdfdf;
  font-size: 14px;
}

.chat-input:focus {
  outline: none;
  border-color: #999999;
}

.send-button {
  padding: 10px 16px;
  border: none;
  border-radius: 6px;
  background-color: #999999;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
  flex-shrink: 0;
}

.send-button:hover {
  background-color: #b3b3b3;
}

/* 表情选择器 */
.emoji-picker {
  position: absolute;
  bottom: 50px;
  left: 0;
  width: 320px;
  max-height: 300px;
  background: #2a2a2a;
  border: 1px solid #555555;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
  z-index: 1000;
  overflow: hidden;
}

.emoji-picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #333333;
  border-bottom: 1px solid #555555;
  color: #dfdfdf;
  font-size: 13px;
  font-weight: 600;
}

.emoji-close-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
  color: #dfdfdf;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.emoji-close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
  padding: 8px;
  max-height: 240px;
  overflow-y: auto;
}

.emoji-grid::-webkit-scrollbar {
  width: 6px;
}

.emoji-grid::-webkit-scrollbar-track {
  background: #2a2a2a;
}

.emoji-grid::-webkit-scrollbar-thumb {
  background: #555555;
  border-radius: 3px;
}

.emoji-grid::-webkit-scrollbar-thumb:hover {
  background: #666666;
}

.emoji-item {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 6px;
  background: transparent;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.emoji-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: scale(1.2);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .meeting-header {
    padding: 8px 16px;
    height: 50px;
  }
  
  .window-controls {
    gap: 4px;
  }
  
  .window-btn {
    width: 28px;
    height: 28px;
    font-size: 14px;
  }
  
  .meeting-footer {
    flex-wrap: wrap;
    gap: 8px;
    padding: 16px;
  }
  
  .control-button {
    min-width: 60px;
    height: 50px;
    padding: 8px 12px;
  }
  
  .control-icon {
    width: 16px;
    height: 16px;
  }
  
  .control-text {
    font-size: 10px;
  }
  
  .floating-modal {
    position: fixed;
    top: 60px;
    left: 10px;
    right: 10px;
    width: auto;
  }
  
  .settings-modal {
    right: 10px;
  }
  
  .participant-actions {
    flex-direction: column;
    gap: 4px;
  }
  
  .action-button {
    padding: 4px 8px;
    font-size: 10px;
  }
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: #363636;
}

::-webkit-scrollbar-thumb {
  background: #666666;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #777777;
}

/* 摄像头选择模态框 */
.camera-select-modal {
  width: 500px;
  max-width: 90vw;
}

.camera-select-hint {
  color: #dfdfdf;
  font-size: 14px;
  margin-bottom: 20px;
  line-height: 1.5;
}

.camera-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
  max-height: 400px;
  overflow-y: auto;
}

.camera-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background-color: #555555;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.camera-item:hover {
  background-color: #666666;
  transform: translateX(4px);
}

.camera-item.selected {
  background-color: #666666;
  border-color: #999999;
}

.camera-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.camera-info {
  flex: 1;
  min-width: 0;
}

.camera-name {
  color: #dfdfdf;
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
}

.camera-id {
  color: #999999;
  font-size: 12px;
  font-family: monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.camera-check {
  font-size: 24px;
  color: #4caf50;
  flex-shrink: 0;
}

.camera-select-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-cancel,
.btn-confirm {
  padding: 10px 24px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel {
  background-color: #555555;
  color: #dfdfdf;
}

.btn-cancel:hover {
  background-color: #666666;
}

.btn-confirm {
  background-color: #999999;
  color: #ffffff;
}

.btn-confirm:hover {
  background-color: #b3b3b3;
}

/* 屏幕共享选项对话框 */
.screen-share-options-modal {
  width: 420px;
}

.option-item {
  padding: 16px 0;
  border-bottom: 1px solid #555555;
}

.option-item:last-child {
  border-bottom: none;
}

.option-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.option-checkbox {
  width: 20px;
  height: 20px;
  margin-right: 12px;
  cursor: pointer;
  accent-color: #4caf50;
}

.option-text {
  color: #dfdfdf;
  font-size: 16px;
  font-weight: 500;
}

.option-hint {
  margin: 8px 0 0 32px;
  color: #999999;
  font-size: 13px;
  line-height: 1.4;
}

.option-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

/* 屏幕共享悬浮层 */
.screen-share-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10000;
  pointer-events: none;
}

.screen-share-overlay > * {
  pointer-events: auto;
}

/* 屏幕共享预览区域 */
.screen-share-preview-area {
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000000;
  pointer-events: auto;
}

.screen-share-preview-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.preview-label {
  position: absolute;
  top: 70px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  color: #ffffff;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  pointer-events: none;
  z-index: 1;
}

/* 顶部控制条 */
.overlay-control-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 50px;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(15px);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  z-index: 10001;
}

.bar-left,
.bar-center,
.bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sharing-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
}

.recording-dot {
  width: 10px;
  height: 10px;
  background-color: #f44336;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

.meeting-no-display {
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  margin: 0 8px;
}

.meeting-time {
  color: #4caf50;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  font-weight: 600;
  padding: 4px 12px;
  background: rgba(76, 175, 80, 0.2);
  border-radius: 6px;
}

.overlay-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  position: relative;
}

.overlay-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
}

.overlay-btn.active {
  background: rgba(244, 67, 54, 0.3);
  border-color: rgba(244, 67, 54, 0.6);
  color: #f44336;
}

/* 悬浮层未读消息徽章 */
.unread-badge-overlay {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background-color: #f44336;
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
  animation: badge-pulse 2s ease-in-out infinite;
}

.overlay-btn .btn-icon {
  font-size: 20px;
}

.overlay-btn .btn-text {
  font-size: 13px;
  font-weight: 500;
}

.stop-sharing-btn {
  background: rgba(244, 67, 54, 0.2);
  border-color: rgba(244, 67, 54, 0.5);
}

.stop-sharing-btn:hover {
  background: rgba(244, 67, 54, 0.4);
  border-color: rgba(244, 67, 54, 0.7);
}

/* 右上角视频面板 */
.video-overlay-panel {
  position: fixed;
  width: 280px;
  max-height: 600px;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
  overflow: hidden;
  z-index: 10001;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  cursor: move;
  user-select: none;
}

.panel-title {
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
}

.panel-toggle-btn {
  width: 28px;
  height: 28px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.panel-toggle-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.panel-content {
  padding: 12px;
  max-height: 540px;
  overflow-y: auto;
}

.panel-content::-webkit-scrollbar {
  width: 6px;
}

.panel-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.panel-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.panel-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.video-thumbnail {
  position: relative;
  width: 100%;
  aspect-ratio: 16/9;
  background: #1a1a1a;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 10px;
}

.video-thumbnail:last-child {
  margin-bottom: 0;
}

.video-thumbnail.my-video {
  border: 2px solid #4caf50;
}

.thumbnail-video,
.thumbnail-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.thumbnail-name {
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.thumbnail-audio-btn {
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  color: #ffffff;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.thumbnail-audio-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.thumbnail-audio-btn.muted {
  background: rgba(244, 67, 54, 0.3);
  color: #f44336;
}

.thumbnail-audio-status {
  color: #4caf50;
  font-size: 16px;
}

.thumbnail-audio-status.muted {
  color: #f44336;
}

/* 左下角聊天气泡面板 */
.chat-bubble-panel {
  position: fixed;
  width: 320px;
  max-height: 450px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  overflow: hidden;
  z-index: 10001;
}

.chat-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  cursor: move;
  user-select: none;
}

.chat-panel-title {
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
}

.chat-panel-toggle-btn {
  width: 28px;
  height: 28px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.chat-panel-toggle-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.chat-panel-content {
  display: flex;
  flex-direction: column;
  height: 380px;
}

.chat-panel-messages {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
}

.chat-panel-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-panel-messages::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.chat-panel-messages::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.chat-panel-messages::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.chat-panel-message {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.chat-panel-message:last-child {
  margin-bottom: 0;
}

.chat-message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.chat-message-content {
  flex: 1;
  min-width: 0;
}

.chat-message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.chat-message-sender {
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-message-time {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
  flex-shrink: 0;
  margin-left: 8px;
}

.chat-message-text {
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  line-height: 1.4;
  word-wrap: break-word;
  background: rgba(255, 255, 255, 0.05);
  padding: 8px 10px;
  border-radius: 8px;
}

/* 自己发送的消息 - 使用蓝色背景 */
.chat-panel-message.my-message .chat-message-text {
  background: rgba(33, 150, 243, 0.3);
  border: 1px solid rgba(33, 150, 243, 0.5);
}

.chat-panel-input-area {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
}

.emoji-btn-quick {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.emoji-btn-quick:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.25);
}

.chat-panel-input {
  flex: 1;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  color: #ffffff;
  font-size: 13px;
  outline: none;
  transition: all 0.2s ease;
}

.chat-panel-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.chat-panel-input:focus {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.25);
}

.chat-panel-send-btn {
  padding: 8px 16px;
  background: #2196f3;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.chat-panel-send-btn:hover {
  background: #1976d2;
}

.chat-panel-send-btn:active {
  transform: scale(0.95);
}

/* 快速聊天框的表情选择器 */
.emoji-picker-quick {
  position: absolute;
  bottom: 60px;
  left: 12px;
  width: 300px;
  max-height: 280px;
  background: rgba(0, 0, 0, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.6);
  z-index: 10002;
}

.thumbnail-audio-status.muted {
  color: #f44336;
}

.meeting-container {
  background:
    radial-gradient(circle at top left, rgba(45, 212, 191, 0.16), transparent 28%),
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.16), transparent 24%),
    linear-gradient(180deg, #020617 0%, #0f172a 52%, #111827 100%) !important;
}

.meeting-header {
  margin: 18px 18px 0;
  padding: 14px 18px;
  border-radius: 24px;
  background: rgba(15, 23, 42, 0.72) !important;
  border: 1px solid rgba(148, 163, 184, 0.14);
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 44px rgba(2, 6, 23, 0.26);
}

.meeting-info-btn,
.window-btn {
  border-radius: 16px !important;
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(148, 163, 184, 0.14) !important;
  color: #f8fafc !important;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.meeting-info-btn:hover,
.window-btn:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.12) !important;
  border-color: rgba(56, 189, 248, 0.24) !important;
}

.meeting-info-text,
.window-icon {
  color: #f8fafc !important;
}

.meeting-body {
  padding: 18px 18px 108px !important;
}

.video-area {
  border-radius: 30px;
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.66), rgba(2, 6, 23, 0.76)) !important;
  border: 1px solid rgba(148, 163, 184, 0.1);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04), 0 24px 60px rgba(2, 6, 23, 0.28);
  overflow: hidden;
}

.participants-grid {
  padding: 14px !important;
  gap: 14px !important;
}

.participant-video-item,
.viewer-video-item,
.video-thumbnail,
.screen-share-view,
.pip-camera,
.viewer-video-panel,
.video-overlay-panel,
.chat-bubble-panel {
  border-radius: 24px !important;
}

.video-frame,
.viewer-video-item,
.video-thumbnail,
.screen-share-video,
.pip-camera,
.screen-share-preview {
  background:
    radial-gradient(circle at top right, rgba(56, 189, 248, 0.12), transparent 32%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.8), rgba(15, 23, 42, 0.96)) !important;
  border: 1px solid rgba(148, 163, 184, 0.12);
  box-shadow: 0 18px 44px rgba(2, 6, 23, 0.26);
}

.participant-video-item:hover .video-frame,
.viewer-video-item:hover,
.video-thumbnail:hover {
  border-color: rgba(56, 189, 248, 0.24);
}

.participant-info-overlay,
.viewer-video-overlay,
.thumbnail-overlay,
.pip-camera-info {
  background: linear-gradient(to top, rgba(2, 6, 23, 0.9), rgba(2, 6, 23, 0.08)) !important;
}

.participant-name,
.viewer-video-name,
.thumbnail-name,
.pip-camera-name,
.panel-title,
.viewer-panel-title,
.chat-panel-title {
  color: #f8fafc !important;
  font-weight: 700 !important;
}

.host-badge,
.participant-badge,
.status-icon,
.thumbnail-audio-btn,
.viewer-audio-btn,
.viewer-audio-status,
.thumbnail-audio-status {
  border-radius: 999px !important;
}

.meeting-footer {
  left: 50% !important;
  bottom: 18px !important;
  transform: translateX(-50%);
  width: calc(100% - 36px);
  max-width: 920px;
  padding: 12px 14px !important;
  border-radius: 28px !important;
  background: rgba(15, 23, 42, 0.76) !important;
  border: 1px solid rgba(148, 163, 184, 0.14);
  backdrop-filter: blur(18px);
  box-shadow: 0 20px 54px rgba(2, 6, 23, 0.3);
}

.control-button {
  min-width: 92px;
  border-radius: 20px !important;
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(148, 163, 184, 0.14) !important;
  color: #f8fafc !important;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.control-button:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.12) !important;
  border-color: rgba(56, 189, 248, 0.24) !important;
}

.control-button.active {
  background: rgba(239, 68, 68, 0.16) !important;
  border-color: rgba(248, 113, 113, 0.3) !important;
}

.control-button.leave-btn,
.control-button.end-meeting-btn {
  color: #fff !important;
  border: none !important;
}

.control-button.leave-btn {
  background: linear-gradient(135deg, #f97316, #ea580c) !important;
}

.control-button.end-meeting-btn {
  background: linear-gradient(135deg, #ef4444, #dc2626) !important;
}

.floating-modal .modal-content,
.modal-overlay .modal-content,
.settings-modal .modal-content,
.meeting-info-modal .modal-content,
.chat-modal-content {
  border-radius: 28px !important;
  background:
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.14), transparent 34%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.96), rgba(15, 23, 42, 0.98)) !important;
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 30px 80px rgba(2, 6, 23, 0.34);
}

.modal-header,
.panel-header,
.viewer-panel-header,
.chat-panel-header {
  background: rgba(255, 255, 255, 0.04) !important;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12) !important;
}

.modal-header h3,
.settings-panel h4,
.shortcut-desc,
.shortcut-key,
.about-content strong,
.info-item label,
.info-item span,
.participant-info h4,
.participant-status,
.message-header h4,
.chat-message-sender,
.chat-message-text,
.notification-text {
  color: #f8fafc !important;
}

.settings-item,
.action-button,
.copy-btn,
.send-button,
.chat-panel-send-btn,
.emoji-btn,
.emoji-btn-quick,
.panel-toggle-btn,
.viewer-panel-toggle-btn,
.viewer-panel-mode-btn,
.chat-panel-toggle-btn,
.overlay-btn {
  border-radius: 16px !important;
}

.settings-item,
.action-button,
.copy-btn,
.emoji-btn,
.emoji-btn-quick,
.overlay-btn,
.panel-toggle-btn,
.viewer-panel-toggle-btn,
.viewer-panel-mode-btn,
.chat-panel-toggle-btn {
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(148, 163, 184, 0.14) !important;
  color: #f8fafc !important;
}

.settings-item.active,
.send-button,
.chat-panel-send-btn,
.overlay-btn.active {
  background: linear-gradient(135deg, #14b8a6, #0284c7) !important;
  border-color: transparent !important;
  color: #fff !important;
}

.chat-messages,
.chat-panel-messages,
.participant-list,
.panel-content,
.viewer-panel-content {
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.3) transparent;
}

.chat-message,
.chat-panel-message,
.participant-item,
.shortcut-item,
.info-item,
.meeting-detail-item {
  background: rgba(255, 255, 255, 0.04) !important;
  border: 1px solid rgba(148, 163, 184, 0.08);
  border-radius: 18px;
}

.chat-message,
.participant-item,
.shortcut-item,
.info-item {
  padding: 12px !important;
}

.chat-input,
.chat-panel-input,
.meeting-no-input,
.time-range-select {
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(148, 163, 184, 0.16) !important;
  border-radius: 16px !important;
  color: #f8fafc !important;
}

.chat-input::placeholder,
.chat-panel-input::placeholder {
  color: rgba(226, 232, 240, 0.42) !important;
}

.overlay-control-bar {
  top: 12px;
  left: 12px;
  right: 12px;
  height: 58px;
  border-radius: 22px;
  background: rgba(15, 23, 42, 0.8) !important;
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 18px 44px rgba(2, 6, 23, 0.28);
}

.meeting-no-display,
.meeting-time,
.sharing-indicator {
  border-radius: 999px;
}

.meeting-no-display {
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(148, 163, 184, 0.14) !important;
}

.meeting-time {
  color: #ccfbf1 !important;
  background: rgba(20, 184, 166, 0.16) !important;
}

@media (max-width: 768px) {
  .meeting-header {
    margin: 10px 10px 0;
    padding: 12px;
    border-radius: 20px;
  }

  .meeting-body {
    padding: 12px 10px 112px !important;
  }

  .meeting-footer {
    width: calc(100% - 20px);
    bottom: 10px !important;
    border-radius: 24px !important;
  }

  .meeting-primary-toolbar {
    right: 10px;
    bottom: 10px;
    width: 132px;
    gap: 8px;
    padding: 10px;
    border-radius: 20px;
  }

  .primary-toolbar-btn {
    height: 52px;
    border-radius: 16px;
    gap: 6px;
  }

  .primary-toolbar-text {
    font-size: 11px;
  }

  .control-button {
    min-width: 72px;
    border-radius: 18px !important;
  }

  .overlay-control-bar {
    top: 8px;
    left: 8px;
    right: 8px;
    height: auto;
    min-height: 54px;
    padding: 8px 12px;
  }
}
</style>
