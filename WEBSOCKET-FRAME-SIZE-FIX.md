# WebSocket帧大小问题已修复

## 根本原因

后端日志显示：
```
io.netty.handler.codec.http.websocketx.CorruptedWebSocketFrameException: 
Max frame length of 6553 has been exceeded.
```

**WebSocket最大帧长度限制太小！**

- 原始配置: 6553 字节 (约 6.4 KB)
- WebRTC的SDP消息通常需要更大空间（包含完整的媒体描述、编解码器信息、ICE候选等）
- 当发送Offer/Answer时，消息超过限制，导致连接被后端强制关闭

## 修复方案

修改 `src/main/java/com/easymeeting/websocket/netty/NettyWebSocketStarter.java`:

```java
// 之前
pipeline.addLast(new WebSocketServerProtocolHandler("/ws",
        null,true,6553,  // ❌ 太小
        true,true,10000L));

// 修改后
pipeline.addLast(new WebSocketServerProtocolHandler("/ws",
        null,true,65536,  // ✅ 64KB，足够WebRTC使用
        true,true,10000L));
```

## 测试步骤

1. **后端已重启** - 新的配置已生效
2. **刷新两个用户的浏览器** (Ctrl+F5)
3. **重新加入会议**
4. **两个用户都点击视频按钮**
5. **观察WebSocket连接状态和视频流**

## 预期结果

### 之前的问题
- ❌ WebSocket连接建立后立即断开
- ❌ 后端日志: `Max frame length of 6553 has been exceeded`
- ❌ 前端日志: `WebSocket状态: 3 (CLOSED)`
- ❌ 无法发送WebRTC消息

### 现在应该
- ✅ WebSocket连接保持稳定
- ✅ 没有帧大小超限错误
- ✅ 前端日志: `WebSocket状态: 1 (OPEN)`
- ✅ WebRTC消息正常发送
- ✅ 视频流正常显示

## 检查日志

### 前端日志（浏览器控制台）
应该看到：
```
🔌🔌🔌 开始建立WebSocket连接
✅✅✅ WebSocket连接成功
🫀 启动心跳机制
📤📤📤 WebSocket状态: 1  ✅
📺📺📺 处理远程视频流添加
✅✅✅ 远程视频流已设置到video元素
```

### 后端日志
应该看到：
```
有新的连接加入....
WebSocket连接已建立
用户 xxx 加入会议房间
🎯🎯🎯 收到WebRTC消息: 类型=13 (Offer)
🔵🔵🔵 路由到 sendMsg2User
🟢🟢🟢 ✅ 消息已发送到用户
```

**不应该再看到**: `Max frame length ... has been exceeded`

## 如果仍有问题

1. 确认后端已重启
2. 确认浏览器已强制刷新
3. 检查后端日志是否还有帧大小错误
4. 检查前端WebSocket状态是否为1 (OPEN)

## 技术说明

### WebSocket帧大小限制的作用
- 防止恶意客户端发送超大消息导致内存溢出
- 需要根据实际应用场景调整

### 为什么选择65536 (64KB)
- WebRTC的SDP消息通常在10-30KB
- 64KB提供了足够的余量
- 不会太大导致内存问题
- 符合WebSocket标准建议

### 其他可能需要调整的场景
- 如果使用高质量视频或多路视频，可能需要更大
- 如果传输文件或大数据，需要进一步增加
- 建议根据实际监控调整
