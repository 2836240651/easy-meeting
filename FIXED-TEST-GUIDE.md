# WebRTC修复后测试指南

## 修复内容

已修复"glare"问题（双方同时发送offer导致连接失败）。

### 修复的问题：
1. ✅ 恢复了正确的连接协商逻辑（使用userId比较决定谁发起）
2. ✅ 添加了glare检测和处理（rollback机制）
3. ✅ 改进了日志输出，便于调试

### 连接逻辑：
- **userId较小的用户**：发送Offer
- **userId较大的用户**：等待Offer，然后发送Answer
- **如果双方同时发送Offer**：userId较大的用户回滚并接受对方的Offer

## 测试步骤

### 1. 刷新两个浏览器窗口

确保加载最新代码（前端会自动热重载）。

### 2. 清除旧的连接

在两个用户的控制台都运行：
```javascript
// 清除所有peer连接
webrtcManager.closeAllConnections()
```

### 3. 重新测试

**用户A（主持人，userId: 6cq7Pg48b4Rq）：**
1. 刷新页面
2. 进入会议
3. 开启视频
4. 打开控制台，应该看到：
   ```
   🎬 初始化WebRTC管理器
   🌐 与所有参与者建立连接
   准备与用户 2Lj7co9YQMps 建立连接
     当前用户ID: 6cq7Pg48b4Rq
     对方用户ID: 2Lj7co9YQMps
     是否发起连接: true  ← userId "6" > "2"，所以发起连接
   🔗 创建Peer连接: 2Lj7co9YQMps (发起方: true)
   📤 创建Offer发送给: 2Lj7co9YQMps
   ```

**用户B（参与者，userId: 2Lj7co9YQMps）：**
1. 刷新页面
2. 进入会议
3. 开启视频
4. 打开控制台，应该看到：
   ```
   🎬 初始化WebRTC管理器
   🌐 与所有参与者建立连接
   准备与用户 6cq7Pg48b4Rq 建立连接
     当前用户ID: 2Lj7co9YQMps
     对方用户ID: 6cq7Pg48b4Rq
     是否发起连接: false  ← userId "2" < "6"，所以等待
   🔗 创建Peer连接: 6cq7Pg48b4Rq (发起方: false)
   📨 收到WebRTC Offer
   📥 处理Offer来自: 6cq7Pg48b4Rq
   ✅ Answer已发送给: 6cq7Pg48b4Rq
   ```

**用户A应该收到Answer：**
```
📨 收到WebRTC Answer
📥 处理Answer来自: 2Lj7co9YQMps
✅ Answer已处理: 2Lj7co9YQMps
```

**双方应该收到ICE候选和远程流：**
```
🧊 发送ICE候选到: [对方userId]
📨 收到ICE候选
✅ ICE候选已添加: [对方userId]
📺 收到远程流: [对方userId]
✅ 远程视频流已设置到video元素
```

### 4. 验证连接状态

在两个用户的控制台都运行：
```javascript
debugWebRTC()
```

**预期结果：**
```
=== WebRTC调试信息 ===
当前用户ID: [userId]
会议ID: ifylfLoJ9X
WebSocket连接状态: true
Peer连接数量: 1
远程流数量: 1  ← 应该是1！
本地流存在: true

--- Peer连接详情 ---
用户 [对方userId]: {
  connectionState: 'connected',  ← 应该是connected！
  iceConnectionState: 'connected',  ← 应该是connected！
  signalingState: 'stable'  ← 应该是stable！
}
```

### 5. 验证视频显示

- ✅ 用户A能看到自己的视频
- ✅ 用户A能看到用户B的视频
- ✅ 用户B能看到自己的视频
- ✅ 用户B能看到用户A的视频

## 如果还是不行

### 检查1：查看完整的控制台日志

特别注意：
- 是否有红色错误
- 连接状态是否变为'connected'
- 是否收到了远程流

### 检查2：查看后端日志

搜索：
```
发送点对点消息: 类型=13
发送点对点消息: 类型=14
发送点对点消息: 类型=15
```

应该看到消息在两个用户之间来回传递。

### 检查3：网络问题

如果ICE连接一直是'checking'或'failed'：
1. 检查防火墙设置
2. 尝试关闭VPN
3. 确保STUN服务器可访问

### 检查4：浏览器兼容性

- Chrome/Edge: 完全支持
- Firefox: 完全支持
- Safari: 需要HTTPS（localhost除外）

## 调试命令

```javascript
// 查看详细状态
debugWebRTC()

// 查看peer连接
webrtcManager.peerConnections.forEach((pc, userId) => {
  console.log(`用户 ${userId}:`)
  console.log('  连接状态:', pc.connectionState)
  console.log('  ICE状态:', pc.iceConnectionState)
  console.log('  信令状态:', pc.signalingState)
  console.log('  发送者:', pc.getSenders().length)
  console.log('  接收者:', pc.getReceivers().length)
})

// 查看远程流
webrtcManager.remoteStreams.forEach((stream, userId) => {
  console.log(`用户 ${userId} 的流:`)
  console.log('  视频轨道:', stream.getVideoTracks())
  console.log('  音频轨道:', stream.getAudioTracks())
})
```

## 成功标志

当你看到以下日志时，说明连接成功：

1. **用户A（发起方）：**
   ```
   📤 创建Offer发送给: 2Lj7co9YQMps
   📨 收到WebRTC Answer
   🧊 发送ICE候选
   📺 收到远程流: 2Lj7co9YQMps
   🔌 连接状态变化 [2Lj7co9YQMps]: connected
   ```

2. **用户B（接收方）：**
   ```
   📨 收到WebRTC Offer
   ✅ Answer已发送给: 6cq7Pg48b4Rq
   🧊 发送ICE候选
   📺 收到远程流: 6cq7Pg48b4Rq
   🔌 连接状态变化 [6cq7Pg48b4Rq]: connected
   ```

## 下一步

连接成功后，可以：
1. 测试3人会议
2. 测试视频开关
3. 测试成员离开和重新加入
4. 添加音频支持
5. 添加屏幕共享

---
最后更新：2026-02-24
