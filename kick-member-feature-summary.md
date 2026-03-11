# 主持人踢出成员功能实现说明

## 功能概述
主持人踢出任意成员后，被踢出的用户会收到 WebSocket 通知消息，显示提示信息，并自动退出当前会议返回主页。

---

## 实现步骤

### 1. 后端修复和增强

#### 1.1 修复权限检查逻辑
**文件：** `src/main/java/com/easymeeting/service/impl/MeetingInfoServiceImpl.java`

**问题：** 原代码检查的是被踢者是否是主持人，导致逻辑错误
```java
// ❌ 错误的逻辑
if (!meetingInfo.getCreateUserId().equals(userId)) {
    throw new BusinessException(ResponseCodeEnum.CODE_600);
}
```

**修复：** 检查操作者是否是主持人
```java
// ✅ 正确的逻辑
if (!meetingInfo.getCreateUserId().equals(tokenUserInfo.getUserId())) {
    throw new BusinessException(ResponseCodeEnum.CODE_600);
}
```

#### 1.2 添加 WebSocket 通知
在 `forceExitMeeting` 方法中添加强制下线通知：

```java
@Override
public void forceExitMeeting(TokenUserInfoDto tokenUserInfo, String userId, 
                             MeetingMemberStatusEnum meetingMemberStatusEnum) {
    MeetingInfo meetingInfo = meetingInfoMapper.selectByMeetingId(
        tokenUserInfo.getCurrentMeetingId()
    );
    
    // 检查操作者是否是主持人
    if (!meetingInfo.getCreateUserId().equals(tokenUserInfo.getUserId())) {
        throw new BusinessException(ResponseCodeEnum.CODE_600);
    }
    
    // 获取被踢用户的token信息
    TokenUserInfoDto tokenByUserId = this.redisComponent.getTokenByUserId(userId);
    
    // 🔥 发送强制下线通知给被踢用户
    MessageSendDto messageSendDto = new MessageSendDto();
    messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType()); // 单人消息
    messageSendDto.setMessageType(MessageTypeEnum.FORCE_OFF_LINE.getType()); // 强制下线
    messageSendDto.setMeetingId(tokenUserInfo.getCurrentMeetingId());
    messageSendDto.setReceiveUserId(userId);  // 接收者是被踢的用户
    messageSendDto.setMessageContent("您已被主持人移出会议");
    messageHandler.sendMessage(messageSendDto);
    
    // 执行退出会议操作
    exitMeetingRoom(tokenByUserId, meetingMemberStatusEnum);
}
```

**关键点：**
- 使用 `MessageSend2TypeEnum.USER` 发送单人消息（只发给被踢的用户）
- 使用 `MessageTypeEnum.FORCE_OFF_LINE` 消息类型（值为10）
- 设置 `receiveUserId` 为被踢用户的ID
- 通过 `messageHandler.sendMessage()` 发送 WebSocket 消息

---

### 2. 前端 WebSocket 消息处理

#### 2.1 添加消息类型处理器
**文件：** `frontend/src/api/meeting-websocket.js`

在 `setupMessageHandlers()` 方法中添加强制下线消息处理：

```javascript
setupMessageHandlers() {
    // ... 其他消息处理器 ...
    
    // 🔥 强制下线消息（被踢出）
    wsService.onMessage(MessageType.FORCE_OFF_LINE, (message) => {
      console.log('收到强制下线消息:', message)
      this.handleMessage('forceOffline', message)
    })
    
    // ... 其他消息处理器 ...
}
```

#### 2.2 在会议页面处理强制下线事件
**文件：** `frontend/src/views/Meeting.vue`

在 `setupWebSocketHandlers()` 中添加事件处理：

```javascript
const setupWebSocketHandlers = () => {
  // ... 其他处理器 ...
  
  // 🔥 处理强制下线（被踢出）
  meetingWsService.on('forceOffline', (message) => {
    console.log('收到强制下线消息:', message)
    const reason = message.messageContent || '您已被主持人移出会议'
    alert(reason)
    // 立即退出会议并返回主页
    exitMeeting()
  })
  
  // ... 其他处理器 ...
}
```

**处理流程：**
1. 接收到 `forceOffline` 消息
2. 显示提示信息（从 `messageContent` 获取）
3. 调用 `exitMeeting()` 退出会议
4. 自动返回主页（Dashboard）

---

## 完整流程图

```
主持人操作
    ↓
点击"踢出"按钮
    ↓
前端调用 kickOutMeeting API
    ↓
后端 MeetingInfoController.kickMeeting()
    ↓
MeetingInfoService.forceExitMeeting()
    ├─ 验证主持人权限
    ├─ 构建 WebSocket 消息
    │   ├─ messageType: FORCE_OFF_LINE (10)
    │   ├─ messageSend2Type: USER (0) - 单人消息
    │   ├─ receiveUserId: 被踢用户ID
    │   └─ messageContent: "您已被主持人移出会议"
    ├─ 发送 WebSocket 消息
    └─ 执行退出会议操作
        ↓
WebSocket 服务器推送消息
    ↓
被踢用户的前端接收消息
    ↓
meeting-websocket.js 处理消息
    ↓
触发 'forceOffline' 事件
    ↓
Meeting.vue 处理事件
    ├─ 显示提示："您已被主持人移出会议"
    ├─ 调用 exitMeeting()
    └─ 返回 Dashboard 页面
```

---

## 消息类型说明

### MessageTypeEnum.FORCE_OFF_LINE
- **值：** 10
- **描述：** 强制下线
- **用途：** 主持人踢出成员时使用
- **定义位置：** `src/main/java/com/easymeeting/entity/enums/MessageTypeEnum.java`

### MessageSend2TypeEnum
- **USER (0)：** 发送给特定用户（单人消息）
- **GROUP (1)：** 发送给群组（会议房间所有人）
- **定义位置：** `src/main/java/com/easymeeting/entity/enums/MessageSend2TypeEnum.java`

---

## 测试步骤

### 准备工作
1. 启动后端服务
2. 启动前端服务
3. 打开两个浏览器窗口（或使用两个 Electron 应用）

### 测试流程
1. **用户A（主持人）：**
   - 登录并创建会议
   - 进入会议房间

2. **用户B（普通成员）：**
   - 登录
   - 加入用户A创建的会议

3. **用户A 踢出用户B：**
   - 在参与者列表中找到用户B
   - 点击"踢出"按钮
   - 确认操作

4. **验证结果：**
   - ✅ 用户B 收到提示："您已被主持人移出会议"
   - ✅ 用户B 自动退出会议
   - ✅ 用户B 返回到 Dashboard 页面
   - ✅ 用户A 的参与者列表中不再显示用户B

---

## 关键代码位置

| 功能 | 文件路径 | 说明 |
|------|---------|------|
| 踢人接口 | `src/main/java/com/easymeeting/controller/MeetingInfoController.java` | `/kickOutMeeting` 接口 |
| 踢人逻辑 | `src/main/java/com/easymeeting/service/impl/MeetingInfoServiceImpl.java` | `forceExitMeeting()` 方法 |
| 消息类型 | `src/main/java/com/easymeeting/entity/enums/MessageTypeEnum.java` | `FORCE_OFF_LINE` 枚举 |
| WebSocket 处理 | `frontend/src/api/meeting-websocket.js` | `setupMessageHandlers()` 方法 |
| 前端事件处理 | `frontend/src/views/Meeting.vue` | `setupWebSocketHandlers()` 方法 |

---

## 注意事项

1. **权限验证：** 只有主持人可以踢人，后端会验证操作者是否是会议创建者

2. **消息发送类型：** 使用 `USER` 类型确保消息只发送给被踢的用户，不会广播给所有人

3. **用户体验：** 被踢用户会看到明确的提示信息，知道自己被移出会议的原因

4. **自动退出：** 前端收到强制下线消息后会自动调用 `exitMeeting()`，无需用户手动操作

5. **状态同步：** 后端会更新数据库中的成员状态，前端会从参与者列表中移除该成员

---

## 扩展功能建议

1. **拉黑功能：** 可以复用相同的逻辑，只需修改 `meetingMemberStatusEnum` 参数

2. **踢出原因：** 可以让主持人输入踢出原因，通过 `messageContent` 传递给被踢用户

3. **踢出记录：** 可以在数据库中记录踢出操作的日志，包括操作者、被踢者、时间、原因等

4. **重新加入限制：** 被踢出的用户可以设置一段时间内不能重新加入该会议

5. **通知其他成员：** 可以向会议中的其他成员广播某人被踢出的消息

---

## 修改总结

✅ 修复了权限检查逻辑错误
✅ 添加了 WebSocket 强制下线通知
✅ 前端添加了消息处理器
✅ 实现了自动退出会议功能
✅ 提供了用户友好的提示信息

现在主持人踢出成员后，被踢出的用户会立即收到通知并自动退出会议！
