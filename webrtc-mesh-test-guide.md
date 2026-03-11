# WebRTC Mesh架构测试指南

## 架构说明

当前实现使用的是**Mesh（网状）架构**，这是WebRTC多人会议的标准实现方式之一。

### Mesh架构特点

```
用户A ←→ 用户B
  ↓  ×  ↑
用户C ←→ 用户D
```

- 每个用户与其他所有用户建立直接的peer-to-peer连接
- 用户A需要建立3个连接（到B、C、D）
- 用户B需要建立3个连接（到A、C、D）
- 总连接数 = n * (n-1) / 2（n为参与者数量）

**优点：**
- ✅ 低延迟（直接连接）
- ✅ 无需中央媒体服务器
- ✅ 服务器负载低（只做信令）

**缺点：**
- ⚠️ 客户端带宽消耗高（上传n-1路流）
- ⚠️ 客户端CPU消耗高（编码n-1路流）
- ⚠️ 适合小规模会议（建议2-6人）

## 测试步骤

### 准备工作

1. **确保服务已启动**
   - 后端：http://localhost:6099
   - 前端：http://localhost:3000
   - WebSocket：ws://localhost:6098

2. **打开浏览器控制台**
   - 按F12打开开发者工具
   - 切换到Console标签
   - 查看WebRTC相关日志

### 测试场景1：两人会议

1. **用户A（主持人）**
   - 打开浏览器窗口1
   - 登录账号A
   - 创建会议
   - 点击"开启视频"
   - 查看控制台日志：
     ```
     🎥 尝试获取摄像头权限...
     ✅ 摄像头权限获取成功
     💾 视频流已保存到 localStream.value
     🎬 本地流已设置到WebRTC管理器
     ```

2. **用户B（参与者）**
   - 打开浏览器窗口2（或使用隐私模式）
   - 登录账号B
   - 输入会议号加入会议
   - 点击"开启视频"
   - 查看控制台日志：
     ```
     🎉 有新成员加入会议
     🎬 初始化WebRTC管理器
     🔗 创建Peer连接: [用户A的ID] (发起方: true/false)
     📤 创建Offer发送给: [用户A的ID]
     ```

3. **验证连接建立**
   - 用户A控制台应该看到：
     ```
     📨 收到WebRTC Offer
     📥 处理Offer来自: [用户B的ID]
     📤 发送Answer到: [用户B的ID]
     🧊 发送ICE候选到: [用户B的ID]
     📺 收到远程流: [用户B的ID]
     ```
   
   - 用户B控制台应该看到：
     ```
     📨 收到WebRTC Answer
     📥 处理Answer来自: [用户A的ID]
     🧊 发送ICE候选到: [用户A的ID]
     📺 收到远程流: [用户A的ID]
     ```

4. **验证视频显示**
   - ✅ 用户A能看到自己的视频（本地）
   - ✅ 用户A能看到用户B的视频（远程）
   - ✅ 用户B能看到自己的视频（本地）
   - ✅ 用户B能看到用户A的视频（远程）

### 测试场景2：三人会议（Mesh架构）

1. **用户C加入**
   - 打开浏览器窗口3
   - 登录账号C
   - 加入会议
   - 开启视频

2. **验证连接拓扑**
   - 用户A应该有2个peer连接（到B和C）
   - 用户B应该有2个peer连接（到A和C）
   - 用户C应该有2个peer连接（到A和B）
   - 总共3个独立的peer连接

3. **验证视频显示**
   - 每个用户都能看到其他所有用户的视频
   - 用户A看到：自己 + B + C
   - 用户B看到：自己 + A + C
   - 用户C看到：自己 + A + B

## 调试技巧

### 1. 查看WebRTC连接状态

在浏览器控制台输入：
```javascript
// 查看所有peer连接
webrtcManager.peerConnections

// 查看远程视频流
webrtcManager.remoteStreams

// 查看本地视频流
webrtcManager.localStream
```

### 2. 查看连接状态

```javascript
// 遍历所有连接
webrtcManager.peerConnections.forEach((pc, userId) => {
  console.log(`用户 ${userId}:`, {
    connectionState: pc.connectionState,
    iceConnectionState: pc.iceConnectionState,
    signalingState: pc.signalingState
  })
})
```

### 3. 后端日志关键信息

查看后端日志中的关键信息：
```
用户 [userId] 加入会议房间 [meetingId]
WebSocket连接已建立，发送成员加入通知
发送点对点消息: 类型=13, 发送者=[userId], 接收者=[targetUserId]
✅ 消息已发送到用户: [targetUserId]
```

## 常见问题排查

### 问题1：看不到对方视频

**可能原因：**
1. WebSocket连接未建立
2. WebRTC信令交换失败
3. ICE连接失败
4. 摄像头权限未授予

**排查步骤：**
```javascript
// 1. 检查WebSocket连接
meetingWsService.isConnected

// 2. 检查peer连接数量
webrtcManager.peerConnections.size

// 3. 检查远程流数量
webrtcManager.remoteStreams.size

// 4. 检查本地流
webrtcManager.localStream
```

### 问题2：连接状态为failed

**可能原因：**
- NAT穿透失败
- STUN服务器不可达
- 防火墙阻止

**解决方案：**
1. 检查网络连接
2. 尝试使用不同的STUN服务器
3. 考虑添加TURN服务器

### 问题3：视频卡顿

**可能原因：**
- 带宽不足
- CPU占用过高
- 参与者过多（Mesh架构限制）

**解决方案：**
1. 降低视频分辨率
2. 限制参与者数量（建议≤6人）
3. 考虑使用SFU架构（需要媒体服务器）

## 性能监控

### 带宽消耗估算

假设视频质量为720p，每路流约1.5Mbps：

| 参与者数 | 上传带宽 | 下载带宽 | 总带宽 |
|---------|---------|---------|--------|
| 2人     | 1.5Mbps | 1.5Mbps | 3Mbps  |
| 3人     | 3Mbps   | 3Mbps   | 6Mbps  |
| 4人     | 4.5Mbps | 4.5Mbps | 9Mbps  |
| 6人     | 7.5Mbps | 7.5Mbps | 15Mbps |

### CPU消耗

- 视频编码：每路流约10-20% CPU
- 视频解码：每路流约5-10% CPU
- 4人会议：约60-90% CPU占用

## 架构对比

### Mesh vs SFU vs MCU

| 特性 | Mesh | SFU | MCU |
|-----|------|-----|-----|
| 延迟 | 低 | 中 | 高 |
| 客户端带宽 | 高 | 低 | 低 |
| 服务器负载 | 低 | 中 | 高 |
| 适用场景 | 2-6人 | 10-50人 | 50+人 |
| 实现复杂度 | 低 | 中 | 高 |

**当前实现：Mesh架构**
- 适合小型会议（2-6人）
- 无需额外的媒体服务器
- 实现简单，成本低

**如需支持更多人：**
- 考虑使用SFU（Selective Forwarding Unit）
- 推荐：Janus、Mediasoup、Jitsi
- 需要部署媒体服务器

## 下一步优化

1. **添加连接质量监控**
   - 显示网络延迟
   - 显示丢包率
   - 显示带宽使用

2. **添加自适应码率**
   - 根据网络状况调整分辨率
   - 动态调整帧率

3. **添加音频支持**
   - 获取麦克风音频
   - 实现静音/解除静音

4. **添加屏幕共享**
   - 使用getDisplayMedia API
   - 支持共享特定窗口

5. **考虑SFU架构**
   - 当参与者超过6人时
   - 部署媒体服务器
   - 降低客户端负载

## 测试检查清单

- [ ] 两人会议视频正常
- [ ] 三人会议视频正常
- [ ] 成员加入时自动建立连接
- [ ] 成员离开时自动关闭连接
- [ ] 视频开关功能正常
- [ ] 网络断线重连正常
- [ ] 浏览器控制台无错误
- [ ] 后端日志显示正常信令交换

## 修改日期
2026-02-24
