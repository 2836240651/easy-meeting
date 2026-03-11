# 成员退出问题最终修复

## 问题现象
成员退出会议后，其他成员的成员列表中仍然显示该成员，且该成员的摄像头画面卡住。只有当其他成员重新进入会议时，才能看到正确的成员列表。

## 根本原因
WebSocket 退出消息没有被其他成员接收到，导致前端无法更新成员列表和关闭 WebRTC 连接。

## 修复内容

### 后端修复

#### 1. MeetingInfoServiceImpl.java - exitMeetingRoom 方法
添加了详细的日志和完整的消息字段：

```java
// 发送退出消息给其他成员
MessageSendDto messageSendDto = new MessageSendDto();
messageSendDto.setMessageType(MessageTypeEnum.EXIT_MEETING_ROOM.getType());
messageSendDto.setMessageContent(exitDtoJson);
messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.GROUP.getType());
messageSendDto.setMeetingId(meetingId);  // ✅ 设置会议ID
messageSendDto.setSendUserId(userId);  // ✅ 设置发送者ID
messageSendDto.setSendUserNickName(tokenUserInfoDto.getNickName());  // ✅ 设置发送者昵称
```

**关键改进**：
- 确保 `meetingId` 字段被设置
- 添加 `sendUserId` 和 `sendUserNickName`
- 添加详细的日志记录每个步骤

#### 2. ChannelContextUtils.java - sendMsg2Group 方法
增强了日志输出，帮助诊断消息发送问题：

```java
log.info("🔵🔵🔵 向会议房间 {} 发送消息，消息类型: {}, 房间成员数: {}", 
         messageSendDto.getMeetingId(), 
         messageSendDto.getMessageType(),
         group.size());

String messageJson = JSON.toJSONString(messageSendDto);
log.info("🔵🔵🔵 消息JSON: {}", messageJson);

// 发送消息到所有成员
group.writeAndFlush(new TextWebSocketFrame(messageJson));
log.info("🔵🔵🔵 ✅ 消息已发送到会议房间的所有成员");
```

**关键改进**：
- 打印完整的消息 JSON
- 记录房间成员数
- 确认消息已发送

### 前端修复

#### 1. Meeting.vue - memberLeft 事件处理器
添加了详细的日志和正确的 JSON 解析：

```javascript
meetingWsService.on('memberLeft', (message) => {
  console.log('=== 收到成员离开消息 ===')
  console.log('完整消息对象:', message)
  
  // 解析消息内容（可能是JSON字符串）
  let exitData = message.messageContent
  if (typeof exitData === 'string') {
    try {
      exitData = JSON.parse(exitData)
      console.log('✅ JSON解析成功，退出数据:', exitData)
    } catch (e) {
      console.error('❌ JSON解析失败:', e)
      return
    }
  }
  
  if (exitData && exitData.exitUserId) {
    removeParticipant(exitData.exitUserId)
    
    // 如果有更新的成员列表，也更新一下
    if (exitData.meetingMemberDtoList) {
      updateParticipantsList(exitData.meetingMemberDtoList)
    }
  }
})
```

#### 2. Meeting.vue - removeParticipant 函数
增强了日志，确保成员从两个数组中移除：

```javascript
const removeParticipant = (userId) => {
  console.log('🗑️ removeParticipant 被调用，userId:', userId)
  
  // 关闭WebRTC连接
  webrtcManager.closePeerConnection(userId)
  
  // 从 allParticipants 移除
  const allIndex = allParticipants.value.findIndex(p => p.userId === userId)
  if (allIndex > -1) {
    allParticipants.value.splice(allIndex, 1)
    console.log('✅ 从 allParticipants 移除')
  }
  
  // 从 participants 移除
  const index = participants.value.findIndex(p => p.userId === userId)
  if (index > -1) {
    participants.value.splice(index, 1)
    console.log('✅ 从 participants 移除')
  }
}
```

#### 3. Meeting.vue - leaveMeeting 函数
调整了执行顺序，确保消息发送完成：

```javascript
const leaveMeeting = async () => {
  if (confirm('确定要离开会议吗？')) {
    try {
      // 先调用后端API离开会议（这会触发后端发送退出消息给其他人）
      const response = await meetingService.exitMeeting()
      
      // 等待一小段时间，确保后端消息已发送
      await new Promise(resolve => setTimeout(resolve, 500))
      
      // 断开WebSocket连接
      meetingWsService.disconnect()
      
      // 跳转到仪表板
      router.push('/dashboard')
    } catch (error) {
      console.error('❌ 离开会议失败:', error)
    }
  }
}
```

## 测试步骤

### 1. 启动服务
```bash
# 启动后端
cd backend
mvn spring-boot:run

# 启动前端
cd frontend
npm run dev
```

### 2. 测试场景
1. 打开两个浏览器窗口（或使用无痕模式）
2. 两个用户登录不同账号
3. 用户A创建会议
4. 用户B加入会议
5. 用户B点击"离开会议"

### 3. 观察后端日志
应该看到以下日志序列：

```
=== 开始退出会议流程 ===
用户ID: xxx, 会议ID: xxx, 退出状态: 退出会议
更新成员状态到数据库: ...
数据库状态更新完成
Redis状态更新完成
当前会议成员总数: 2
退出消息内容: {"exitUserId":"xxx",...}
准备发送退出消息到会议房间: meetingId=xxx, messageType=2, sendUserId=xxx
🔴🔴🔴 MessageHandler4Redis.sendMessage 被调用
🔴🔴🔴 消息已发布到Redis Topic
退出消息已发送
用户token中的会议ID已清除
当前在线成员数: 1
=== 退出会议流程完成 ===

🟡🟡🟡 从Redis Topic收到消息
🟡🟡🟡 消息类型: 2, ...
🔵🔵🔵 ChannelContextUtils.sendMessage 被调用
🔵🔵🔵 路由到 sendMsg2Group
🔵🔵🔵 向会议房间 xxx 发送消息，消息类型: 2, 房间成员数: 2
🔵🔵🔵 消息JSON: {...}
🔵🔵🔵 ✅ 消息已发送到会议房间的所有成员
🔵🔵🔵 处理退出会议消息
🔵🔵🔵 退出用户ID: xxx
🔵🔵🔵 已从会议房间移除用户: xxx
🔵🔵🔵 当前在线成员数: 1
```

### 4. 观察前端日志（用户A的浏览器）
应该看到以下日志序列：

```
=== 收到成员离开消息 ===
完整消息对象: {...}
消息类型: 2
消息内容类型: string
消息内容原始值: {"exitUserId":"xxx",...}
✅ JSON解析成功，退出数据: {...}
📤 准备移除退出的成员: xxx
退出状态: 2
当前参与者列表: [...]
🗑️ removeParticipant 被调用，userId: xxx
关闭WebRTC连接...
🔌 关闭Peer连接: xxx
✅ 从 allParticipants 移除: 用户名 (索引: 0)
✅ 从 participants 移除: 用户名 (索引: 0)
当前 allParticipants 数量: 1
当前 participants 数量: 0
✅ 成员移除完成
=== 成员离开消息处理完成 ===
```

### 5. 验证结果
- [ ] 用户A的成员列表中不再显示用户B
- [ ] 用户B的视频画面消失
- [ ] 视频网格重新布局
- [ ] 没有卡顿或错误

## 常见问题排查

### 问题1：后端日志显示消息已发送，但前端没有收到
**可能原因**：
- WebSocket 连接断开
- 消息类型不匹配
- 前端没有注册 `memberLeft` 事件处理器

**排查步骤**：
1. 检查前端 WebSocket 连接状态
2. 检查 `meeting-websocket.js` 中是否注册了 `MessageType.EXIT_MEETING_ROOM` 的处理器
3. 检查消息类型枚举值是否一致（后端和前端都是 2）

### 问题2：前端收到消息但解析失败
**可能原因**：
- `messageContent` 不是有效的 JSON 字符串
- JSON 格式错误

**排查步骤**：
1. 打印 `message.messageContent` 的原始值
2. 检查是否是字符串类型
3. 尝试手动解析 JSON

### 问题3：成员没有从列表中移除
**可能原因**：
- `exitUserId` 不匹配
- 数组查找失败

**排查步骤**：
1. 打印 `exitData.exitUserId` 和列表中的 `userId`
2. 检查是否完全一致（包括类型）
3. 检查数组操作是否正确执行

## 文件修改清单

### 后端
- [x] `src/main/java/com/easymeeting/service/impl/MeetingInfoServiceImpl.java`
  - 增强 `exitMeetingRoom` 方法的日志
  - 确保消息字段完整

- [x] `src/main/java/com/easymeeting/websocket/ChannelContextUtils.java`
  - 增强 `sendMsg2Group` 方法的日志
  - 打印完整消息内容

### 前端
- [x] `frontend/src/views/Meeting.vue`
  - 增强 `memberLeft` 事件处理器
  - 增强 `removeParticipant` 函数
  - 调整 `leaveMeeting` 函数执行顺序

## 完成时间
2026年2月25日
