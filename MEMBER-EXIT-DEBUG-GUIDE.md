# 成员退出问题调试指南

## 问题描述
成员退出会议后，其他成员的成员列表中仍然显示该成员，且该成员的摄像头画面卡住。

## 预期行为
1. 成员退出会议
2. 后端发送退出消息给其他成员
3. 其他成员收到消息后：
   - 从成员列表中移除该成员
   - 关闭与该成员的 WebRTC 连接
   - 停止显示该成员的视频流

## 调试步骤

### 1. 检查后端日志
启动后端服务后，当成员退出会议时，应该看到以下日志：

```
=== 开始退出会议流程 ===
用户ID: xxx, 会议ID: xxx, 退出状态: 退出会议
更新成员状态到数据库: meetingId=xxx, userId=xxx, status=2, statusDesc=退出会议
数据库状态更新完成
Redis状态更新完成
当前会议成员总数: X
退出消息内容: {"exitUserId":"xxx","meetingMemberDtoList":[...],"exitStatus":2}
准备发送退出消息到会议房间: meetingId=xxx, messageType=2
退出消息已发送
用户token中的会议ID已清除
当前在线成员数: X
=== 退出会议流程完成 ===
```

**关键检查点**：
- 退出消息内容是否包含 `exitUserId`
- `meetingMemberDtoList` 是否包含所有成员
- 消息是否成功发送

### 2. 检查前端 WebSocket 接收
打开浏览器控制台，当成员退出时，应该看到以下日志：

```
=== 收到成员离开消息 ===
完整消息对象: {...}
消息类型: 2
消息内容类型: string
消息内容原始值: {"exitUserId":"xxx","meetingMemberDtoList":[...],"exitStatus":2}
✅ JSON解析成功，退出数据: {...}
📤 准备移除退出的成员: xxx
退出状态: 2
当前参与者列表: [...]
当前所有参与者列表: [...]
```

**关键检查点**：
- 是否收到 `memberLeft` 消息
- `messageContent` 是否正确解析
- `exitUserId` 是否存在

### 3. 检查成员移除
继续查看控制台日志：

```
🗑️ removeParticipant 被调用，userId: xxx
关闭WebRTC连接...
✅ 从 allParticipants 移除: 用户名 (索引: X)
✅ 从 participants 移除: 用户名 (索引: X)
当前 allParticipants 数量: X
当前 participants 数量: X
✅ 成员移除完成
更新后参与者列表: [...]
更新后所有参与者列表: [...]
=== 成员离开消息处理完成 ===
```

**关键检查点**：
- `removeParticipant` 是否被调用
- 成员是否从两个数组中移除
- WebRTC 连接是否关闭

### 4. 检查 WebRTC 连接关闭
在 WebRTC 管理器中应该看到：

```
🔌 关闭Peer连接: xxx
```

**关键检查点**：
- PeerConnection 是否关闭
- 远程流是否停止
- 视频轨道是否停止

## 常见问题和解决方案

### 问题1：没有收到 memberLeft 消息
**可能原因**：
- WebSocket 连接断开
- 后端没有发送消息
- 消息类型不匹配

**解决方案**：
1. 检查 WebSocket 连接状态
2. 检查后端日志，确认消息已发送
3. 检查消息类型枚举值是否一致

### 问题2：消息内容解析失败
**可能原因**：
- `messageContent` 不是有效的 JSON 字符串
- JSON 格式错误

**解决方案**：
1. 检查后端发送的 JSON 格式
2. 在前端添加 try-catch 捕获解析错误
3. 打印原始消息内容

### 问题3：成员没有从列表中移除
**可能原因**：
- `exitUserId` 不匹配
- 数组查找失败
- Vue 响应式更新问题

**解决方案**：
1. 确认 `exitUserId` 与列表中的 `userId` 完全一致
2. 检查数组操作是否正确
3. 使用 `splice` 而不是直接赋值

### 问题4：视频流没有停止
**可能原因**：
- WebRTC 连接没有关闭
- 视频轨道没有停止
- 视频元素没有清空

**解决方案**：
1. 确保调用 `peerConnection.close()`
2. 停止所有视频轨道：`track.stop()`
3. 清空视频元素：`videoElement.srcObject = null`

## 测试场景

### 场景1：普通成员退出
1. 创建会议，主持人和成员加入
2. 成员点击"离开会议"
3. 主持人查看成员列表
4. **预期**：成员列表中不再显示该成员

### 场景2：主持人退出
1. 创建会议，主持人和成员加入
2. 主持人点击"离开会议"
3. 成员查看成员列表
4. **预期**：成员列表中不再显示主持人

### 场景3：多个成员依次退出
1. 创建会议，多个成员加入
2. 成员依次退出
3. 主持人查看成员列表
4. **预期**：每次退出后，成员列表正确更新

### 场景4：成员被踢出
1. 创建会议，主持人和成员加入
2. 主持人踢出成员
3. 其他成员查看成员列表
4. **预期**：成员列表中不再显示被踢出的成员

## 代码修改总结

### 前端修改
1. **Meeting.vue - memberLeft 事件处理器**
   - 添加详细日志
   - 正确解析 JSON 字符串
   - 调用 `removeParticipant` 移除成员
   - 使用后端提供的完整成员列表更新

2. **Meeting.vue - removeParticipant 函数**
   - 添加详细日志
   - 从两个数组中移除成员
   - 关闭 WebRTC 连接

3. **Meeting.vue - leaveMeeting 函数**
   - 调整执行顺序
   - 先调用后端 API
   - 等待消息发送完成
   - 再断开 WebSocket

### 后端修改
1. **MeetingInfoServiceImpl.java - exitMeetingRoom 方法**
   - 添加详细日志
   - 记录每个步骤的执行情况
   - 打印退出消息内容
   - 确认消息发送成功

## 验证清单

- [ ] 后端日志显示退出消息已发送
- [ ] 前端控制台显示收到 memberLeft 消息
- [ ] 消息内容正确解析，包含 exitUserId
- [ ] removeParticipant 函数被调用
- [ ] 成员从 allParticipants 数组中移除
- [ ] 成员从 participants 数组中移除
- [ ] WebRTC 连接已关闭
- [ ] 视频流已停止
- [ ] 成员列表 UI 正确更新
- [ ] 视频网格正确重新布局

## 完成时间
2026年2月25日
