# 屏幕共享单用户限制功能

## 实现方案

限制同一时间只能有一个用户共享屏幕，这是最常见的会议软件做法。

## 实现内容

### 1. 后端消息类型
在 `MessageTypeEnum.java` 中添加：
- `SCREEN_SHARE_START(16)` - 开始屏幕共享
- `SCREEN_SHARE_STOP(17)` - 停止屏幕共享

### 2. 后端消息处理
在 `HandlerWebSocket.java` 中：
- 添加对消息类型 16 和 17 的处理
- 直接转发屏幕共享消息给所有会议成员

### 3. 前端消息处理
在 `meeting-websocket.js` 中：
- 添加 `screenShareStart` 事件处理器
- 添加 `screenShareStop` 事件处理器

### 4. 前端状态管理
在 `Meeting.vue` 中：
- 添加 `currentScreenSharingUserId` 状态，跟踪当前正在共享屏幕的用户
- 在 `setupWebSocketHandlers` 中监听屏幕共享消息
- 更新全局状态

### 5. 屏幕共享逻辑
在 `shareScreen()` 函数中：
```javascript
// 检查是否已有其他人在共享
if (currentScreenSharingUserId.value && 
    currentScreenSharingUserId.value !== currentUserId.value) {
  const sharingUser = allParticipants.value.find(
    p => p.userId === currentScreenSharingUserId.value
  )
  const sharingUserName = sharingUser ? sharingUser.name : '其他用户'
  alert(`${sharingUserName} 正在共享屏幕，同一时间只能有一个人共享屏幕。`)
  return
}

// 开始共享后，通知所有人
meetingWsService.sendMessage({
  messageType: 16, // SCREEN_SHARE_START
  messageSend2Type: 1, // GROUP
  sendUserId: currentUserId.value,
  sendUserNickName: userName.value,
  meetingId: meetingId.value,
  messageContent: {
    userId: currentUserId.value,
    userName: userName.value
  },
  sendTime: Date.now()
})
```

在 `stopScreenShare()` 函数中：
```javascript
// 停止共享后，通知所有人
meetingWsService.sendMessage({
  messageType: 17, // SCREEN_SHARE_STOP
  messageSend2Type: 1, // GROUP
  sendUserId: currentUserId.value,
  sendUserNickName: userName.value,
  meetingId: meetingId.value,
  messageContent: {
    userId: currentUserId.value,
    userName: userName.value
  },
  sendTime: Date.now()
})
```

## 工作流程

### 用户A开始共享屏幕
1. 用户A点击"共享屏幕"按钮
2. 检查 `currentScreenSharingUserId` 是否为空
3. 如果为空，允许共享
4. 发送 `SCREEN_SHARE_START` 消息给所有人
5. 所有用户（包括用户B、C）收到消息，更新 `currentScreenSharingUserId = A`

### 用户B尝试共享屏幕
1. 用户B点击"共享屏幕"按钮
2. 检查 `currentScreenSharingUserId` = A（不为空）
3. 显示提示："用户A 正在共享屏幕，同一时间只能有一个人共享屏幕。"
4. 阻止用户B共享

### 用户A停止共享屏幕
1. 用户A点击"停止共享"或浏览器停止按钮
2. 发送 `SCREEN_SHARE_STOP` 消息给所有人
3. 所有用户收到消息，清空 `currentScreenSharingUserId = null`
4. 现在其他用户可以开始共享了

## 优势

1. **简单可靠** - 不需要复杂的多流管理
2. **带宽友好** - 每个用户只发送一个视频流
3. **用户体验好** - 清晰的提示信息，避免冲突
4. **符合常规** - 大多数会议软件都是这样实现的

## 测试步骤

1. 用户A和用户B加入会议
2. 用户A点击"共享屏幕"，选择屏幕
3. 用户B应该能看到用户A的屏幕
4. 用户B点击"共享屏幕"
5. 用户B应该看到提示："用户A 正在共享屏幕，同一时间只能有一个人共享屏幕。"
6. 用户A点击"停止共享"
7. 用户B再次点击"共享屏幕"，这次应该成功
8. 用户A应该能看到用户B的屏幕

## 文件修改

### 后端
- `src/main/java/com/easymeeting/entity/enums/MessageTypeEnum.java` - 添加消息类型
- `src/main/java/com/easymeeting/websocket/netty/HandlerWebSocket.java` - 添加消息处理

### 前端
- `frontend/src/api/meeting-websocket.js` - 添加事件处理器
- `frontend/src/views/Meeting.vue` - 添加状态管理和检查逻辑

## 状态
✅ 完成
