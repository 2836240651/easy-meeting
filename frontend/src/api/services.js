import api from './axios'

// 认证相关 API
export const authService = {
  getCaptcha: () => api.get('/account/checkCode'),
  login: (data) => api.post('/account/login', data),
  register: (data) => api.post('/account/register', data),
  updateUserInfo: (data) => api.post('/account/updateUserInfo', data),
  updatePassword: (password) => api.post('/account/updatePassword', { password })
}

// 用户信息相关 API
export const userService = {
  getUserInfo: () => api.get('/userInfo/getUserInfo'),
  updateUserInfo: (data) => api.post('/userInfo/updateUserInfo', data)
}

// 会议相关 API
export const meetingService = {
  // 历史保留接口
  loadTodayMeeting: () => api.get('/meetingReserve/loadTodayMeeting'),
  loadMeetingReserve: () => api.get('/meetingReserve/loadMeetingReserve'),
  delMeetingReserve: (meetingId) => api.post('/meetingReserve/delMeetingReserve', { meetingId }),

  createMeetingReserve: (data) => api.post('/meetingReserve/createMeetingReserve', data),
  quickMeeting: (data) => api.post('/meetingInfo/quickMeeting', data),
  preJoinMeeting: (meetingNo, nickName, password) =>
    api.get('/meetingInfo/preJoinMeeting', { params: { meetingNo, nickName, password } }),
  joinMeetingReserve: (meetingId, nickName, password) =>
    api.post('/meetingInfo/joinMeetingReserve', { meetingId, nickName, password }),
  joinMeeting: (videoOpen, meetingId = null) => {
    const data = { videoOpen }
    if (meetingId) data.meetingId = meetingId
    return api.post('/meetingInfo/joinMeeting', data)
  },
  exitMeeting: () => api.post('/meetingInfo/exitMeeting'),
  getCurrentMeeting: () => api.get('/meetingInfo/getCurrentMeeting'),
  getMeetingInfoByMeetingId: (meetingId) =>
    api.get('/meetingInfo/getMeetingInfoByMeetingId', { params: { meetingId } }),
  getMeetingStatus: (meetingId) =>
    api.get('/meetingInfo/getMeetingStatus', { params: { meetingId } }),
  loadMeeting: (pageNo, status = null) => {
    const params = { pageNo }
    if (status !== null) params.status = status
    return api.get('/meetingInfo/loadMeeting', { params })
  },
  getMeetingMembers: (meetingId) =>
    api.get('/meetingInfo/loadMeetingMembers', { params: { meetingId } }),
  getMeetingMemberByMeetingIdAndUserId: (meetingId, userId) =>
    api.get('/meetingMember/getMeetingMemberByMeetingIdAndUserId', { params: { meetingId, userId } }),
  kickOutMeeting: (userId) => api.post('/meetingInfo/kickOutMeeting', { userId }),
  blackMeeting: (userId) => api.post('/meetingInfo/blackMeeting', { userId }),
  finishMeeting: (meetingId = null) => {
    const params = new URLSearchParams()
    if (meetingId) params.append('meetingId', meetingId)
    return api.post('/meetingInfo/finishMeeting', params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })
  },
  sendVideoChange: (videoOpen) => api.post('/meetingInfo/sendVideoChange', null, { params: { videoOpen } }),
  inviteUserToMeeting: (inviteUserId) =>
    api.post('/meetingInfo/inviteUserToMeeting', null, { params: { inviteUserId } })
}

// 预约会议相关 API
export const meetingReserveService = {
  createMeetingReserve: (data) => api.post('/meetingReserve/createMeetingReserve', data),
  loadMeetingReserveList: () => api.get('/meetingReserve/loadMeetingReserveList'),
  updateMeetingReserve: (data) => api.post('/meetingReserve/updateMeetingReserve', data),
  cancelMeetingReserve: (meetingId) =>
    api.post('/meetingReserve/cancelMeetingReserve', null, { params: { meetingId } }),
  leaveMeetingReserve: (meetingId) =>
    api.post('/meetingReserve/leaveMeetingReserve', null, { params: { meetingId } }),
  getMeetingReserveDetail: (meetingId) =>
    api.get('/meetingReserve/getMeetingReserveDetail', { params: { meetingId } }),
  getUpcomingMeetings: () => api.get('/meetingReserve/getUpcomingMeetings'),
  joinMeetingReserve: (params) =>
    api.post('/meetingInfo/joinMeetingReserve', null, {
      params: { meetingId: params.meetingId, password: params.password || '' }
    })
}

// 聊天相关 API
export const chatService = {
  sendMessage: (
    message,
    messageType,
    receiveUserId = '0',
    fileName = null,
    fileSize = null,
    fileType = null
  ) => {
    const params = new URLSearchParams()
    params.append('message', message)
    params.append('messageType', messageType)
    params.append('receiveUserId', receiveUserId)
    if (fileName) params.append('fileName', fileName)
    if (fileSize) params.append('fileSize', fileSize)
    if (fileType) params.append('fileType', fileType)

    return api.post('/chat/sendMessage', params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })
  },
  loadMessage: (maxMessageId = null, minMessageId = null, pageNo = 1) => {
    const params = { pageNo }
    if (maxMessageId) params.maxMessageId = maxMessageId
    if (minMessageId) params.minMessageId = minMessageId
    return api.get('/chat/loadMeesage', { params })
  },
  loadHistory: (meetingId, maxMessageId = null, pageNo = 1) => {
    const params = { meetingId, pageNo }
    if (maxMessageId) params.maxMessageId = maxMessageId
    return api.get('/chat/loadHistroy', { params })
  },
  uploadFile: (file, messageId, sendTime) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('messageId', messageId)
    formData.append('sendTime', sendTime)

    return api.post('/chat/uploadFile', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// AI 助手相关 API
export const aiService = {
  chat: (meetingId, message) => api.post('/ai/chat', { meetingId, message }),
  summary: (meetingId) => api.post('/ai/summary', { meetingId }),
  suggest: (meetingId) => api.post('/ai/suggest', { meetingId }),
  saveSpeechSegment: (meetingId, speakerName, content) =>
    api.post('/ai/speechSegment', { meetingId, speakerName, content }),
  generateSmartSummary: (meetingId) => api.post('/ai/smartSummary', { meetingId }),
  test: () => api.get('/ai/test')
}

// 联系人相关 API
export const contactService = {
  loadContactUser: () => api.get('/userContact/loadContactUser'),
  searchContact: (userId, email) => {
    const params = {}
    if (userId) params.userId = userId
    if (email) params.email = email
    return api.get('/userContact/searchContact', { params })
  },
  contactApply: (receiveUserId) =>
    api.post(`/userContact/contactApply?receiveUserId=${encodeURIComponent(receiveUserId)}`),
  dealWithApply: (applyUserId, status) =>
    api.post(`/userContact/dealWithApply?applyUserId=${encodeURIComponent(applyUserId)}&status=${status}`),
  loadContactApply: () => api.get('/userContact/loadContactApply'),
  loadAllContactApply: () => api.get('/userContact/loadAllContactApply'),
  loadMyApply: () => api.get('/userContact/loadMyApply'),
  loadContactApplyDealWithCount: () => api.get('/userContact/loadContactApplyDealWithCount'),
  delContact: (contactId, status) =>
    api.post(`/userContact/delContact?contactId=${encodeURIComponent(contactId)}&status=${status}`),
  loadBlackList: () => api.get('/userContact/loadBlackList'),
  unblackContact: (contactId) =>
    api.post(`/userContact/unblackContact?contactId=${encodeURIComponent(contactId)}`)
}

// 通知相关 API
export const notificationService = {
  loadNotificationList: (pageNo = 1, pageSize = 15, status = null, actionRequired = null) => {
    const params = { pageNo, pageSize }
    if (status !== null) params.status = status
    if (actionRequired !== null) params.actionRequired = actionRequired
    return api.get('/notification/loadNotificationList', { params })
  },
  getUnreadCount: () => api.get('/notification/getUnreadCount'),
  markAsRead: (notificationId) => api.post(`/notification/markAsRead?notificationId=${notificationId}`),
  markAllAsRead: () => api.post('/notification/markAllAsRead'),
  loadNotificationsByCategory: (category = 'all', pageNo = 1, pageSize = 15) =>
    api.get('/notification/loadNotificationsByCategory', { params: { category, pageNo, pageSize } }),
  loadPendingActions: () => api.get('/notification/loadPendingActions'),
  handleMeetingInvite: (notificationId, accepted) =>
    api.post(`/notification/handleMeetingInvite?notificationId=${notificationId}&accepted=${accepted}`),
  updateActionStatus: (notificationId, actionStatus) =>
    api.post(`/notification/updateActionStatus?notificationId=${notificationId}&actionStatus=${actionStatus}`)
}

// 系统设置 API
export const systemService = {
  loadSystemSetting: () => api.get('/account/loadSystemSetting')
}

// 用户设置 API
export const settingsService = {
  getUserSettings: () => api.get('/api/settings/get'),
  saveUserSettings: (settings) => api.post('/api/settings/save', settings),
  changePassword: (oldPassword, newPassword) =>
    api.post('/api/settings/changePassword', { oldPassword, newPassword })
}

// 便捷方法导出
export const getUserInfo = userService.getUserInfo
export const updateUserInfo = userService.updateUserInfo
export const getContactList = contactService.loadContactUser
export const getContactApplyList = contactService.loadContactApply
export const getContactApplyCount = contactService.loadContactApplyDealWithCount
export const getMeetingHistory = meetingService.loadMeeting
export const searchContact = contactService.searchContact
export const applyContact = contactService.contactApply
export const dealWithContactApply = contactService.dealWithApply
export const sendChatMessage = chatService.sendMessage
export const loadChatMessages = chatService.loadMessage
export const getUserSettings = settingsService.getUserSettings
export const saveUserSettings = settingsService.saveUserSettings
export const changePassword = settingsService.changePassword
export const aiChat = aiService.chat
export const aiGenerateSummary = aiService.summary
export const aiGetSuggestions = aiService.suggest
export const aiSmartSummary = aiService.generateSmartSummary
export const aiTest = aiService.test
