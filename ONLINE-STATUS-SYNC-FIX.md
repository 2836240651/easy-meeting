# 联系人在线状态同步修复

## 问题描述

用户 A 因为网络波动断开 WebSocket 连接后重新连接，用户 B 看到用户 A 是离线状态，但用户 A 看到用户 B 是在线状态。这是因为缺少状态变更的实时通知机制。

## 问题根源

1. **状态更新不同步**：
   - 用户 A 断开时：后端更新 `lasgOffTime`（离线时间）
   - 用户 A 重连时：后端更新 `lastLoginTime`（登录时间）
   - 但是用户 B 的联系人列表没有收到任何通知

2. **依赖轮询机制**：
   - 前端每 60 秒轮询一次联系人列表
   - 在轮询间隔内，状态变更不会被感知
   - 导致状态显示延迟最多 60 秒

## 解决方案

### 1. 后端修改

#### 1.1 添加新的消息类型

**文件**: `src/main/java/com/easymeeting/entity/enums/MessageTypeEnum.java`

```java
USER_ONLINE_STATUS_CHANGE(21,"用户在线状态变更");
```

#### 1.2 创建在线状态 DTO

**文件**: `src/main/java/com/easymeeting/entity/dto/UserOnlineStatusDto.java`

```java
public class UserOnlineStatusDto implements Serializable {
    private String userId;           // 用户ID
    private Integer onlineStatus;    // 在线状态：1-在线，0-离线
    private Long lastLoginTime;      // 最后登录时间
    private Long lastOffTime;        // 最后离线时间
}
```

#### 1.3 修改 ChannelContextUtils

**文件**: `src/main/java/com/easymeeting/websocket/ChannelContextUtils.java`

**主要改动**：

1. **注入 UserContactService**：
```java
@Resource
private com.easymeeting.service.UserContactService userContactService;
```

2. **用户上线时广播状态**：
```java
public void addContext(String userId, Channel channel) {
    // ... 原有代码 ...
    
    // 更新登录时间
    UserInfo userInfo = new UserInfo();
    userInfo.setLastLoginTime(System.currentTimeMillis());
    userInfoMapper.updateByUserId(userInfo,userId);
    
    // 广播在线状态变更给所有联系人
    broadcastOnlineStatusChange(userId, 1, System.currentTimeMillis(), null);
    
    // ... 原有代码 ...
}
```

3. **添加用户离线处理方法**：
```java
public void handleUserOffline(String userId) {
    // 更新离线时间
    UserInfo userInfo = new UserInfo();
    Long offTime = System.currentTimeMillis();
    userInfo.setLasgOffTime(offTime);
    userInfoMapper.updateByUserId(userInfo, userId);
    
    // 广播离线状态变更给所有联系人
    broadcastOnlineStatusChange(userId, 0, null, offTime);
}
```

4. **添加广播状态变更方法**：
```java
private void broadcastOnlineStatusChange(String userId, Integer onlineStatus, 
                                         Long lastLoginTime, Long lastOffTime) {
    // 查询该用户的所有联系人
    UserContactQuery contactQuery = new UserContactQuery();
    contactQuery.setContactId(userId);
    contactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
    List<UserContact> contacts = userContactService.findListByParam(contactQuery);
    
    // 构造在线状态变更消息
    UserOnlineStatusDto statusDto = new UserOnlineStatusDto();
    statusDto.setUserId(userId);
    statusDto.setOnlineStatus(onlineStatus);
    statusDto.setLastLoginTime(lastLoginTime);
    statusDto.setLastOffTime(lastOffTime);
    
    // 向每个在线的联系人发送状态变更通知
    for (UserContact contact : contacts) {
        String contactUserId = contact.getUserId();
        Channel contactChannel = USER_CONTEXT_MAP.get(contactUserId);
        
        if (contactChannel != null) {
            MessageSendDto messageSendDto = new MessageSendDto();
            messageSendDto.setMessageType(MessageTypeEnum.USER_ONLINE_STATUS_CHANGE.getType());
            messageSendDto.setMessageContent(statusDto);
            messageSendDto.setReceiveUserId(contactUserId);
            messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
            
            sendMsg2User(messageSendDto);
        }
    }
}
```

#### 1.4 修改 HandlerWebSocket

**文件**: `src/main/java/com/easymeeting/websocket/netty/HandlerWebSocket.java`

**主要改动**：

1. **注入 ChannelContextUtils**：
```java
@Resource
private com.easymeeting.websocket.ChannelContextUtils channelContextUtils;
```

2. **修改 channelInactive 方法**：
```java
@Override
public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    String channelId = ctx.channel().id().toString();
    Attribute<String> attr = ctx.channel().attr(AttributeKey.valueOf(channelId));
    String userId = attr.get();
    
    if (userId != null) {
        // 调用 ChannelContextUtils 处理用户离线
        channelContextUtils.handleUserOffline(userId);
    }
}
```

### 2. 前端修改

#### 2.1 更新消息类型枚举

**文件**: `frontend/src/api/websocket.js`

```javascript
export const MessageType = {
  // ... 其他类型 ...
  USER_ONLINE_STATUS_CHANGE: 21
}
```

#### 2.2 添加状态变更处理

**文件**: `frontend/src/views/Dashboard.vue`

**主要改动**：

1. **添加消息处理函数**：
```javascript
const handleUserOnlineStatusChange = (message) => {
  console.log('收到用户在线状态变更消息:', message)
  
  const statusData = message.messageContent
  const userId = statusData.userId
  const lastLoginTime = statusData.lastLoginTime
  const lastOffTime = statusData.lastOffTime
  
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
    
    // 触发响应式更新
    contactList.value[contactIndex] = { ...contact }
  }
}
```

2. **注册消息监听器**：
```javascript
onMounted(async () => {
  // ... 其他代码 ...
  
  wsService.onMessage(MessageType.USER_CONTACT_APPLY, handleContactApplyMessage)
  wsService.onMessage(MessageType.USER_ONLINE_STATUS_CHANGE, handleUserOnlineStatusChange)
})
```

3. **清理消息监听器**：
```javascript
onUnmounted(() => {
  wsService.offMessage(MessageType.USER_CONTACT_APPLY, handleContactApplyMessage)
  wsService.offMessage(MessageType.USER_ONLINE_STATUS_CHANGE, handleUserOnlineStatusChange)
  wsService.disconnect()
})
```

## 工作流程

### 用户上线流程

1. 用户 A 建立 WebSocket 连接
2. `ChannelContextUtils.addContext()` 被调用
3. 更新用户 A 的 `lastLoginTime`
4. 查询用户 A 的所有联系人
5. 向每个在线的联系人发送 `USER_ONLINE_STATUS_CHANGE` 消息
6. 联系人（如用户 B）收到消息后更新本地联系人列表

### 用户离线流程

1. 用户 A 的 WebSocket 连接断开
2. `HandlerWebSocket.channelInactive()` 被调用
3. 调用 `ChannelContextUtils.handleUserOffline()`
4. 更新用户 A 的 `lasgOffTime`
5. 查询用户 A 的所有联系人
6. 向每个在线的联系人发送 `USER_ONLINE_STATUS_CHANGE` 消息
7. 联系人（如用户 B）收到消息后更新本地联系人列表

## 优势

1. **实时性**：状态变更立即通知，无需等待轮询
2. **准确性**：所有联系人看到的状态一致
3. **高效性**：只通知在线的联系人，减少不必要的消息
4. **可靠性**：即使网络波动导致断线重连，状态也能正确同步

## 测试建议

### 测试场景 1：正常上线/离线

1. 用户 A 和用户 B 互为联系人
2. 用户 A 登录，用户 B 应该立即看到用户 A 在线
3. 用户 A 退出，用户 B 应该立即看到用户 A 离线

### 测试场景 2：网络波动

1. 用户 A 和用户 B 互为联系人，都在线
2. 模拟用户 A 网络断开（关闭 WebSocket）
3. 用户 B 应该立即看到用户 A 离线
4. 用户 A 网络恢复（重新连接 WebSocket）
5. 用户 B 应该立即看到用户 A 在线

### 测试场景 3：多个联系人

1. 用户 A 有多个联系人（B、C、D）
2. 用户 A 上线/离线
3. 所有在线的联系人都应该收到状态变更通知

### 验证方法

1. 打开浏览器开发者工具的 Console
2. 观察 WebSocket 消息日志
3. 检查是否收到 `messageType: 21` 的消息
4. 验证联系人列表中的在线状态是否正确更新

## 注意事项

1. **性能考虑**：
   - 只向在线的联系人发送通知
   - 避免在高频率连接/断开时造成消息风暴

2. **数据一致性**：
   - 确保 `lastLoginTime` 和 `lasgOffTime` 的更新是原子操作
   - 前端收到消息后立即更新本地状态

3. **错误处理**：
   - 如果查询联系人列表失败，记录日志但不影响用户连接
   - 如果发送消息失败，不影响其他联系人的通知

## 文件修改清单

### 后端文件

- `src/main/java/com/easymeeting/entity/enums/MessageTypeEnum.java` - 添加消息类型
- `src/main/java/com/easymeeting/entity/dto/UserOnlineStatusDto.java` - 新建 DTO
- `src/main/java/com/easymeeting/websocket/ChannelContextUtils.java` - 添加广播逻辑
- `src/main/java/com/easymeeting/websocket/netty/HandlerWebSocket.java` - 修改离线处理

### 前端文件

- `frontend/src/api/websocket.js` - 添加消息类型
- `frontend/src/views/Dashboard.vue` - 添加消息处理
