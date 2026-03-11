# WebSocket断开问题修复

## 问题

WebSocket在发送WebRTC Offer后立即断开（code: 1006），导致：
- Offer可能没有成功发送到后端
- ICE候选无法发送
- 视频连接无法建立

## 原因分析

Code 1006表示"异常关闭"，可能的原因：
1. **后端处理WebRTC消息时出错**
2. **后端主动关闭了连接**
3. **网络问题**
4. **消息格式不正确**

## 临时解决方案

### 方案1：等待WebSocket重连后再建立WebRTC连接

在用户B的控制台运行：

```javascript
// 等待WebSocket重连
setTimeout(async () => {
  console.log('WebSocket状态:', meetingWsService.isConnected)
  
  if (meetingWsService.isConnected) {
    console.log('WebSocket已重连，重新建立WebRTC连接')
    
    // 关闭旧连接
    webrtcManager.closeAllConnections()
    
    // 重新建立连接
    const userAId = '6cq7Pg48b4Rq'
    await webrtcManager.connectToParticipant(userAId)
    
    console.log('✅ 已重新建立连接')
  } else {
    console.log('❌ WebSocket未重连')
  }
}, 6000)  // 等待6秒（重连间隔是5秒）
```

### 方案2：检查后端日志

查看后端日志中是否有：
- 错误信息
- 异常堆栈
- WebSocket关闭的原因

搜索关键词：
```
error
exception
close
disconnect
类型=13
```

### 方案3：简化WebRTC消息

问题可能是SDP太大（7044字节）导致WebSocket断开。

尝试分段发送或压缩SDP。

## 根本解决方案

需要修复后端的WebSocket处理逻辑，确保：
1. 正确处理WebRTC消息（类型13、14、15）
2. 不会因为消息大小而断开连接
3. 正确转发点对点消息

## 调试步骤

1. **查看后端日志**
   ```
   搜索：类型=13
   搜索：error
   搜索：exception
   ```

2. **测试简单消息**
   在用户B的控制台：
   ```javascript
   // 发送一个简单的测试消息
   meetingWsService.wsService.ws.send(JSON.stringify({
     messageType: 13,
     messageSend2Type: 1,
     sendUserId: '2Lj7co9YQMps',
     receiveUserId: '6cq7Pg48b4Rq',
     meetingId: '5jGRP9bLho',
     messageContent: {
       type: 'test',
       sdp: 'test'
     }
   }))
   
   // 观察WebSocket是否断开
   ```

3. **检查WebSocket连接状态**
   ```javascript
   // 持续监控
   setInterval(() => {
     console.log('WebSocket状态:', meetingWsService.wsService.ws.readyState)
   }, 1000)
   ```

## 下一步

请提供：
1. **后端日志**（特别是WebSocket断开时的日志）
2. **是否有错误或异常**
3. **WebSocket重连后是否能正常工作**

---
最后更新：2026-02-24
