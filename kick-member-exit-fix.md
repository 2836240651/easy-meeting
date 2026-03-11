# 踢出成员后用户未退出会议问题修复

## 问题描述

主持人踢出成员时：
1. 前端正确发送了userId到后端
2. 后端成功接收参数并发送了强制下线消息
3. 被踢出的用户收到提示"您已被主持人移出会议"
4. **但是用户没有退出会议，仍然停留在会议页面**

## 问题分析

### 后端日志分析

```
Read "application/json;charset=UTF-8" to [{userId=6cq7Pg48b4Rq}]
收到信息
```

后端成功：
- ✅ 接收到userId参数
- ✅ 发送了WebSocket消息到Redis
- ✅ 消息被Redis监听器接收

### 前端问题

查看前端代码发现两个问题：

#### 问题1：调用了不存在的函数

```javascript
// frontend/src/views/Meeting.vue
meetingWsService.on('forceOffline', (message) => {
  console.log('收到强制下线消息:', message)
  const reason = message.messageContent || '您已被主持人移出会议'
  alert(reason)
  // 立即退出会议并返回主页
  exitMeeting()  // ❌ 这个函数不存在！
})
```

`exitMeeting()`函数在Meeting.vue中没有定义，导致JavaScript报错，用户无法退出会议。

#### 问题2：后端消息缺少sendUserId字段

```java
// 修复前
MessageSendDto messageSendDto = new MessageSendDto();
messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
messageSendDto.setMessageType(MessageTypeEnum.FORCE_OFF_LINE.getType());
messageSendDto.setMeetingId(tokenUserInfo.getCurrentMeetingId());
messageSendDto.setReceiveUserId(userId);
messageSendDto.setMessageContent("您已被主持人移出会议");
// ❌ 缺少 sendUserId 字段
```

## 解决方案

### 修复1：添加forceExitMeeting函数

在Meeting.vue中添加专门的强制退出函数，不需要用户确认：

```javascript
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
```

### 修复2：更新forceOffline事件处理器

```javascript
// 处理强制下线（被踢出）
meetingWsService.on('forceOffline', (message) => {
  console.log('收到强制下线消息:', message)
  const reason = message.messageContent || '您已被主持人移出会议'
  alert(reason)
  // 立即退出会议并返回主页（不需要确认）
  forceExitMeeting()  // ✅ 调用正确的函数
})
```

### 修复3：后端添加sendUserId字段和日志

```java
@Override
public void forceExitMeeting(TokenUserInfoDto tokenUserInfo, String userId, MeetingMemberStatusEnum meetingMemberStatusEnum) {
    MeetingInfo meetingInfo = meetingInfoMapper.selectByMeetingId(tokenUserInfo.getCurrentMeetingId());
    
    // 检查操作者是否是主持人
    if (!meetingInfo.getCreateUserId().equals(tokenUserInfo.getUserId())) {
        throw new BusinessException(ResponseCodeEnum.CODE_600);
    }
    
    // 获取被踢用户的token信息
    TokenUserInfoDto tokenByUserId = this.redisComponent.getTokenByUserId(userId);
    
    // 发送强制下线通知给被踢用户
    MessageSendDto messageSendDto = new MessageSendDto();
    messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
    messageSendDto.setMessageType(MessageTypeEnum.FORCE_OFF_LINE.getType());
    messageSendDto.setMeetingId(tokenUserInfo.getCurrentMeetingId());
    messageSendDto.setReceiveUserId(userId);
    messageSendDto.setSendUserId(tokenUserInfo.getUserId()); // ✅ 添加发送者ID
    messageSendDto.setMessageContent("您已被主持人移出会议");
    
    // ✅ 添加日志便于调试
    log.info("发送强制下线消息: userId=" + userId + 
             ", messageType=" + messageSendDto.getMessageType() + 
             ", messageSend2Type=" + messageSendDto.getMessageSend2Type());
    messageHandler.sendMessage(messageSendDto);
    
    // 执行退出会议操作
    exitMeetingRoom(tokenByUserId, meetingMemberStatusEnum);
}
```

## 工作流程

### 完整的踢出流程

1. **主持人点击踢出按钮**
   - 前端调用`meetingService.kickOutMeeting(userId)`
   - 发送POST请求：`{ userId: "6cq7Pg48b4Rq" }`

2. **后端处理踢出请求**
   - Controller接收JSON参数
   - 验证主持人权限
   - 调用`forceExitMeeting()`方法

3. **发送强制下线消息**
   - 创建`MessageSendDto`对象
   - 设置消息类型为`FORCE_OFF_LINE`（10）
   - 设置发送类型为`USER`（单人消息）
   - 通过Redis发布消息

4. **WebSocket推送消息**
   - Redis监听器接收消息
   - `ChannelContextUtils.sendMsg2User()`发送到指定用户
   - WebSocket推送到被踢用户的客户端

5. **前端接收并处理**
   - `websocket.js`接收原始消息
   - `meeting-websocket.js`分发到`forceOffline`事件
   - `Meeting.vue`的事件处理器被触发
   - 显示提示信息
   - 调用`forceExitMeeting()`

6. **强制退出会议**
   - 调用后端API退出会议
   - 断开WebSocket连接
   - 跳转到Dashboard页面

## 测试步骤

1. 启动前后端服务和两个Electron应用
2. 两个用户加入同一个会议
3. 主持人打开成员列表
4. 主持人点击"踢出"按钮
5. 验证：
   - ✅ 被踢用户收到提示"您已被主持人移出会议"
   - ✅ 被踢用户自动退出会议
   - ✅ 被踢用户跳转到Dashboard页面
   - ✅ 主持人的成员列表中该用户被移除
   - ✅ 后端日志显示"发送强制下线消息"

## 相关文件

- `src/main/java/com/easymeeting/service/impl/MeetingInfoServiceImpl.java` - 后端forceExitMeeting方法
- `frontend/src/views/Meeting.vue` - 前端forceExitMeeting函数和事件处理器
- `frontend/src/api/meeting-websocket.js` - WebSocket消息分发
- `frontend/src/api/websocket.js` - 底层WebSocket服务

## 关键点

1. **函数命名一致性**：确保调用的函数确实存在
2. **错误处理**：即使API调用失败，也要确保用户能退出会议
3. **消息完整性**：WebSocket消息应包含所有必要字段
4. **日志记录**：添加详细日志便于调试
5. **用户体验**：被踢出时不需要用户确认，直接退出

## 总结

这个问题的根本原因是前端调用了一个不存在的函数`exitMeeting()`，导致JavaScript报错，用户无法退出会议。通过添加`forceExitMeeting()`函数并更新事件处理器，问题得到解决。同时在后端添加了sendUserId字段和日志，使整个流程更加完善和可追踪。
