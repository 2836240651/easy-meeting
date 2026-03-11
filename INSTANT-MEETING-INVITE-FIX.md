# 即时会议邀请功能修复

## 问题描述

1. **路由错误**：点击"立即加入"时出现路由错误 `No match for {"name":"join-meeting"}`
2. **通知分类错误**：即时会议邀请通知一开始就显示在"全部消息"的"会议"标签页，而不是"待办消息"中
3. **缺少状态更新**：点击"立即加入"后，通知应该从"待办消息"移到"全部消息"，并允许用户再次加入

## 解决方案

### 1. 修复通知分类（后端）

**文件**: `src/main/java/com/easymeeting/service/impl/UserNotificationServiceImpl.java`

将即时会议邀请的 `actionRequired` 从 0 改为 1，使其显示在"待办消息"中：

```java
notification.setStatus(0); // 未读
notification.setActionRequired(1); // 需要操作（用户需要决定是否加入）
notification.setActionStatus(0); // 待处理
notification.setReferenceId(meetingId);
```

### 2. 添加更新通知状态接口（后端）

**文件**: `src/main/java/com/easymeeting/controller/UserNotificationController.java`

新增 `updateActionStatus` 接口：

```java
/**
 * 更新通知的操作状态
 */
@RequestMapping("/updateActionStatus")
@globalInterceptor(checkLogin = true)
public ResponseVO updateActionStatus(Integer notificationId, Integer actionStatus) {
    if (notificationId == null || actionStatus == null) {
        return getBusinessErrorResponseVO(new com.easymeeting.exception.BusinessException("参数不能为空"), null);
    }
    
    TokenUserInfoDto userDto = getTokenUserInfo();
    userNotificationService.updateActionStatus(userDto.getUserId(), String.valueOf(notificationId), actionStatus);
    return getSuccessResponseVO(null);
}
```

### 3. 添加前端 API 方法

**文件**: `frontend/src/api/services.js`

```javascript
// 更新通知的操作状态
updateActionStatus: (notificationId, actionStatus) => {
  return api.post(`/notification/updateActionStatus?notificationId=${notificationId}&actionStatus=${actionStatus}`)
}
```

### 4. 修复前端加入会议逻辑

**文件**: `frontend/src/views/Dashboard.vue`

修改 `handleJoinInstantMeeting` 函数：

```javascript
// 处理加入即时会议
const handleJoinInstantMeeting = async (notification) => {
  try {
    const meetingInfo = parseMeetingInvite(notification.content)
    
    // 更新通知的 actionStatus 为已处理（1）
    // 这样通知会从"待办消息"移到"全部消息"
    if (notification.actionStatus === 0) {
      try {
        await notificationService.updateActionStatus(notification.notificationId, 1)
      } catch (error) {
        console.error('更新通知状态失败:', error)
      }
    }
    
    // 标记通知为已读
    await markNotificationAsRead(notification.notificationId)
    
    // 直接跳转到会议页面，通过 query 传递会议号和密码
    router.push({
      path: '/meeting',
      query: {
        meetingNo: meetingInfo.meetingNo,
        password: meetingInfo.password || ''
      }
    })
  } catch (error) {
    console.error('加入会议失败:', error)
    ElMessage.error('加入会议失败，请重试')
  }
}
```

### 5. 修改 Meeting.vue 支持从 query 加入会议

**文件**: `frontend/src/views/Meeting.vue`

修改 `onMounted` 钩子，支持从 query 参数中获取 meetingNo 和 password：

```javascript
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
        userInfo.value?.nickName || '用户',
        queryPassword || null
      )
      
      if (preJoinResponse.data.code === 200) {
        // preJoinMeeting 成功后，会议ID会被设置到后端session中
        // 然后调用 joinMeeting
        await joinMeeting()
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
  
  // ... 其他初始化代码
})
```

## 工作流程

### 初始状态（待办消息）

1. 用户 A 在会议中邀请用户 B
2. 后端创建即时会议邀请通知：
   - `actionRequired = 1`（需要操作）
   - `actionStatus = 0`（待处理）
3. 通知显示在用户 B 的"待办消息"中
4. 显示"立即加入"按钮

### 用户点击"立即加入"

1. 前端调用 `updateActionStatus` 接口，将 `actionStatus` 更新为 1（已处理）
2. 标记通知为已读
3. 跳转到 Meeting 页面，传递 `meetingNo` 和 `password` 参数
4. Meeting 页面调用 `preJoinMeeting` 验证会议号和密码
5. 验证成功后调用 `joinMeeting` 加入会议

### 处理后状态（全部消息）

1. 通知从"待办消息"消失（因为 `actionStatus = 1`）
2. 通知显示在"全部消息"的"会议"标签页中
3. 用户仍然可以点击通知再次加入会议
4. 再次点击时，直接跳转到 Meeting 页面（不再更新 actionStatus）

## 通知状态说明

### actionRequired（是否需要操作）

- `0`: 不需要操作（仅通知）
- `1`: 需要操作（显示在待办消息中）

### actionStatus（操作状态）

- `0`: 待处理（显示在待办消息中）
- `1`: 已处理（从待办消息移除，显示在全部消息中）
- `2`: 已拒绝（从待办消息移除）

## 通知类型

即时会议邀请通知：
- `notificationType`: 10 (MEETING_INSTANT_INVITE)
- `actionRequired`: 1
- `actionStatus`: 0（初始）→ 1（点击后）

## 测试步骤

1. **创建即时会议**：用户 A 创建一个快速会议
2. **邀请用户**：用户 A 在会议中点击"邀请"，邀请用户 B
3. **检查待办消息**：用户 B 登录，查看"待办消息"，应该看到会议邀请
4. **点击立即加入**：用户 B 点击"立即加入"按钮
5. **验证跳转**：应该成功跳转到会议页面并加入会议
6. **检查全部消息**：返回 Dashboard，查看"全部消息"的"会议"标签页，应该看到该通知
7. **再次加入**：点击通知，应该能够再次加入会议

## 相关文件

### 后端
- `src/main/java/com/easymeeting/service/impl/UserNotificationServiceImpl.java`
- `src/main/java/com/easymeeting/controller/UserNotificationController.java`

### 前端
- `frontend/src/views/Dashboard.vue`
- `frontend/src/views/Meeting.vue`
- `frontend/src/api/services.js`

## 部署状态

- ✅ 后端代码已编译
- ✅ 后端服务已重启
- ✅ 前端代码已更新（自动热更新）
- ✅ 新增 API 接口已部署
- ✅ 通知状态逻辑已修复

## 注意事项

1. **路由问题**：不使用命名路由 `join-meeting`，而是直接使用 path `/meeting`
2. **参数传递**：通过 query 参数传递 `meetingNo` 和 `password`
3. **状态更新**：只在第一次点击时更新 `actionStatus`，避免重复更新
4. **错误处理**：如果 preJoinMeeting 失败，返回 Dashboard 页面
5. **用户体验**：在"全部消息"中仍然可以再次加入会议
