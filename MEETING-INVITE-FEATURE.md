# 会议内邀请功能实现文档

## 功能概述

实现了会议内邀请功能，允许会议参与者邀请其他用户加入当前进行中的会议。被邀请用户会收到包含会议信息（包括密码）的通知，可以一键加入会议。

## 功能特点

### 1. 邀请方式
- **联系人列表**：显示所有联系人，自动过滤已在会议中的用户
- **用户搜索**：支持通过用户名或邮箱搜索并邀请非联系人用户
- **实时状态**：显示联系人的在线/离线状态

### 2. 通知内容
邀请通知包含完整的会议信息：
- 会议名称
- 会议号
- 会议密码（如果有）
- 邀请人信息

### 3. 安全设计
- 只有会议中的用户才能发送邀请
- 不能邀请自己
- 不能重复邀请已在会议中的用户
- 密码只在通知中显示，不会公开
- 通知只有接收者能看到

## 实现细节

### 后端实现

#### 1. 新增通知类型
```java
// NotificationTypeEnum.java
MEETING_INSTANT_INVITE(10, "即时会议邀请")
```

#### 2. Controller 接口
```java
// MeetingInfoController.java
@RequestMapping("/inviteUserToMeeting")
public ResponseVO inviteUserToMeeting(@NotEmpty String inviteUserId)
```

**功能**：
- 验证当前用户是否在会议中
- 验证被邀请用户是否存在
- 检查被邀请用户是否已在会议中
- 发送邀请通知

#### 3. Service 方法
```java
// UserNotificationService.java
void createInstantMeetingInviteNotification(
    String meetingId, String meetingNo, String meetingName, String password,
    String inviteUserId, String inviterUserId, String inviterName
)
```

**通知内容格式（JSON）**：
```json
{
  "meetingId": "abc123",
  "meetingNo": "123456789",
  "meetingName": "项目讨论会",
  "password": "abc123",
  "inviterName": "张三",
  "inviterUserId": "user123"
}
```

### 前端实现

#### 1. 邀请弹窗组件
**文件**：`frontend/src/components/InviteMemberModal.vue`

**功能**：
- 显示联系人列表（过滤已在会议中的用户）
- 支持用户搜索（用户名或邮箱）
- 显示用户在线状态
- 发送邀请请求

**Props**：
- `meetingId`: 当前会议ID
- `meetingName`: 会议名称
- `currentMembers`: 当前会议成员列表

**Events**：
- `close`: 关闭弹窗
- `invite`: 邀请用户（传递用户信息）

#### 2. Meeting.vue 集成
```vue
<!-- 邀请按钮（已存在） -->
<button class="control-button" @click="inviteParticipants">
  <img src="/svg/邀请.svg" alt="邀请" class="control-icon">
  <span class="control-text">邀请</span>
</button>

<!-- 邀请弹窗 -->
<InviteMemberModal
  v-if="showInviteModal"
  :meeting-id="meetingId"
  :meeting-name="meetingName"
  :current-members="allParticipants"
  @close="showInviteModal = false"
  @invite="handleInviteUser"
/>
```

#### 3. Dashboard.vue 通知处理
**通知显示**：
```vue
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
  <button class="btn-join-meeting" @click.stop="handleJoinInstantMeeting(notification)">
    立即加入
  </button>
</div>
```

**处理方法**：
```javascript
// 解析会议邀请内容
const parseMeetingInvite = (content) => {
  try {
    return JSON.parse(content)
  } catch (error) {
    return { meetingId: '', meetingNo: '', meetingName: '会议', password: '' }
  }
}

// 处理加入即时会议
const handleJoinInstantMeeting = async (notification) => {
  const meetingInfo = parseMeetingInvite(notification.content)
  await markNotificationAsRead(notification.notificationId)
  router.push({
    name: 'join-meeting',
    query: {
      meetingNo: meetingInfo.meetingNo,
      password: meetingInfo.password || ''
    }
  })
}
```

## API 接口

### 1. 邀请用户加入会议
```
POST /api/meetingInfo/inviteUserToMeeting?inviteUserId={userId}
```

**请求参数**：
- `inviteUserId`: 被邀请用户ID（必填）

**响应**：
```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

**错误情况**：
- 400: 您当前不在会议中
- 400: 不能邀请自己
- 400: 会议不存在
- 400: 会议已结束
- 400: 被邀请用户不存在
- 400: 该用户已在会议中

## 使用流程

### 邀请流程
1. 用户A在会议中点击"邀请"按钮
2. 打开邀请弹窗，显示联系人列表
3. 用户A选择联系人或搜索用户
4. 点击"邀请"按钮
5. 系统发送邀请通知给用户B
6. 用户B收到通知提示

### 接受邀请流程
1. 用户B打开收件箱
2. 查看即时会议邀请通知
3. 通知中显示：
   - 会议名称
   - 会议号
   - 会议密码（如果有）
4. 点击"立即加入"按钮
5. 自动跳转到加入会议页面，会议号和密码已自动填充
6. 点击"加入会议"即可进入

## 数据库变更

### 通知类型更新
```sql
-- 新增通知类型：即时会议邀请（类型 10）
-- 系统通知类型从 10-11 调整为 11-12
```

## 文件清单

### 后端文件
1. `src/main/java/com/easymeeting/entity/enums/NotificationTypeEnum.java` - 新增通知类型
2. `src/main/java/com/easymeeting/service/UserNotificationService.java` - 新增接口方法
3. `src/main/java/com/easymeeting/service/impl/UserNotificationServiceImpl.java` - 实现邀请通知创建
4. `src/main/java/com/easymeeting/service/MeetingInfoService.java` - 新增邀请接口
5. `src/main/java/com/easymeeting/service/impl/MeetingInfoServiceImpl.java` - 实现邀请逻辑
6. `src/main/java/com/easymeeting/controller/MeetingInfoController.java` - 新增邀请接口

### 前端文件
1. `frontend/src/components/InviteMemberModal.vue` - 邀请弹窗组件（新建）
2. `frontend/src/views/Meeting.vue` - 集成邀请功能
3. `frontend/src/views/Dashboard.vue` - 处理即时会议邀请通知
4. `frontend/src/api/services.js` - 新增邀请 API

## 测试建议

### 功能测试
1. **邀请联系人**
   - 在会议中点击邀请按钮
   - 验证联系人列表正确显示
   - 验证已在会议中的用户被过滤
   - 发送邀请并验证通知

2. **搜索邀请**
   - 搜索用户名
   - 搜索邮箱
   - 邀请搜索到的用户

3. **接受邀请**
   - 查看邀请通知
   - 验证会议信息显示正确
   - 点击"立即加入"
   - 验证自动填充会议号和密码

### 边界测试
1. 不在会议中时尝试邀请
2. 邀请自己
3. 重复邀请同一用户
4. 邀请不存在的用户
5. 会议结束后的邀请

### 安全测试
1. 验证密码只在通知中显示
2. 验证通知只有接收者能看到
3. 验证邀请权限检查

## 注意事项

1. **密码安全**：会议密码包含在通知内容中，但只有接收者能看到
2. **实时性**：邀请通过 WebSocket 实时推送
3. **状态同步**：联系人列表会自动过滤已在会议中的用户
4. **用户体验**：点击"立即加入"会自动填充所有信息，无需手动输入

## 后续优化建议

1. **批量邀请**：支持一次邀请多个用户
2. **邀请历史**：记录邀请历史和接受状态
3. **邀请链接**：生成可分享的邀请链接
4. **邀请提醒**：对未响应的邀请发送提醒
5. **权限控制**：允许主持人设置谁可以邀请他人

## 总结

会议内邀请功能已完整实现，包括：
- ✅ 邀请弹窗（联系人列表 + 用户搜索）
- ✅ 后端邀请接口和通知系统
- ✅ 前端通知显示和处理
- ✅ 一键加入会议（自动填充密码）
- ✅ 安全验证和权限检查

功能已编译并重启后端服务，可以开始测试使用。
