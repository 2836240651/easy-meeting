# WebRTC视频通话实现总结

## 实现概述

已成功实现基于WebRTC的peer-to-peer视频通话功能，允许会议参与者之间实时传输视频流。

## 架构设计

### 1. 信令服务器（后端）

使用现有的WebSocket服务作为信令服务器，负责在peers之间交换连接信息。

**新增消息类型：**
- `WEBRTC_OFFER (13)` - WebRTC连接请求
- `WEBRTC_ANSWER (14)` - WebRTC连接响应
- `WEBRTC_ICE_CANDIDATE (15)` - ICE候选交换

### 2. WebRTC管理器（前端）

创建了`webrtc-manager.js`，封装所有WebRTC相关逻辑：

**核心功能：**
- 管理所有peer连接（RTCPeerConnection）
- 处理SDP offer/answer交换
- 处理ICE候选交换
- 管理本地和远程视频流
- 自动建立和关闭连接

**关键特性：**
- 使用Google STUN服务器进行NAT穿透
- 避免双向连接（只有userId较小的一方发起）
- 自动重连和错误处理
- 完整的生命周期管理

### 3. Meeting.vue集成

**视频显示：**
- 本地视频：显示自己的摄像头画面
- 远程视频：显示其他参与者的视频流
- 动态切换：视频开启时显示video元素，关闭时显示头像

**连接管理：**
- 加入会议时：与所有现有参与者建立连接
- 新成员加入：自动与新成员建立连接
- 成员离开：自动关闭对应的连接
- 离开会议：清理所有连接和资源

## 实现细节

### 连接建立流程

```
用户A加入会议
  ↓
加载现有成员列表
  ↓
与所有成员建立WebRTC连接（如果A的userId < 对方userId）
  ↓
用户B加入会议
  ↓
B收到成员列表（包括A）
  ↓
B与A建立连接（如果B的userId < A的userId）
  ↓
A收到"新成员加入"通知
  ↓
A与B建立连接（如果A的userId < B的userId）
```

### Offer/Answer交换流程

```
发起方（userId较小）
  ↓
创建RTCPeerConnection
  ↓
添加本地视频流
  ↓
创建Offer
  ↓
设置本地描述（setLocalDescription）
  ↓
通过WebSocket发送Offer给接收方
  ↓
接收方收到Offer
  ↓
创建RTCPeerConnection
  ↓
添加本地视频流
  ↓
设置远程描述（setRemoteDescription）
  ↓
创建Answer
  ↓
设置本地描述（setLocalDescription）
  ↓
通过WebSocket发送Answer给发起方
  ↓
发起方收到Answer
  ↓
设置远程描述（setRemoteDescription）
  ↓
连接建立成功
```

### ICE候选交换

```
RTCPeerConnection生成ICE候选
  ↓
onicecandidate事件触发
  ↓
通过WebSocket发送候选给对方
  ↓
对方收到候选
  ↓
addIceCandidate添加到连接
  ↓
ICE连接建立
  ↓
视频流开始传输
```

## 文件修改清单

### 后端文件

1. **MessageTypeEnum.java**
   - 添加了3个新的消息类型：WEBRTC_OFFER, WEBRTC_ANSWER, WEBRTC_ICE_CANDIDATE

### 前端文件

1. **webrtc-manager.js** (新建)
   - WebRTC管理器核心实现
   - 约500行代码
   - 完整的peer连接管理

2. **meeting-websocket.js**
   - 添加WebRTC消息处理器
   - 添加sendMessage通用方法

3. **Meeting.vue**
   - 导入webrtcManager
   - 添加远程视频流存储
   - 修改视频显示模板（支持远程视频）
   - 集成WebRTC初始化
   - 添加远程流处理函数
   - 修改参与者管理函数（建立/关闭连接）
   - 修改toggleVideo（设置本地流到WebRTC）
   - 修改onUnmounted（清理WebRTC资源）

## 使用的技术

### WebRTC API
- `RTCPeerConnection` - peer连接
- `getUserMedia` - 获取本地媒体流
- `addTrack` - 添加媒体轨道
- `createOffer/createAnswer` - 创建SDP
- `setLocalDescription/setRemoteDescription` - 设置SDP
- `addIceCandidate` - 添加ICE候选

### STUN服务器
使用Google公共STUN服务器：
- stun:stun.l.google.com:19302
- stun:stun1.l.google.com:19302
- stun:stun2.l.google.com:19302

## 测试步骤

1. **启动服务**
   - 后端已重启
   - 前端开发服务器运行中

2. **创建会议**
   - 用户A创建并进入会议
   - 用户A打开摄像头

3. **加入会议**
   - 用户B加入会议
   - 用户B打开摄像头

4. **验证**
   - ✅ 用户A能看到自己的视频
   - ✅ 用户B能看到自己的视频
   - ✅ 用户A能看到用户B的视频
   - ✅ 用户B能看到用户A的视频
   - ✅ 成员列表正确显示所有成员

5. **多人测试**
   - 用户C加入会议
   - 验证所有人都能看到彼此的视频

## 注意事项

### 浏览器兼容性
- Chrome/Edge: 完全支持
- Firefox: 完全支持
- Safari: 需要HTTPS或localhost

### 网络要求
- 需要STUN服务器进行NAT穿透
- 如果在复杂网络环境（如企业防火墙），可能需要TURN服务器

### 性能考虑
- 视频质量：默认1280x720
- 带宽消耗：每个视频流约1-2Mbps
- CPU使用：视频编解码会占用CPU资源

### 安全性
- 必须使用HTTPS（生产环境）
- 需要用户授权摄像头权限
- 视频流是加密的（DTLS-SRTP）

## 后续优化建议

1. **添加TURN服务器**
   - 用于复杂网络环境下的中继
   - 提高连接成功率

2. **视频质量控制**
   - 根据网络状况自动调整分辨率
   - 添加带宽检测

3. **音频支持**
   - 添加麦克风音频流
   - 实现静音/解除静音功能

4. **屏幕共享**
   - 使用getDisplayMedia API
   - 支持共享整个屏幕或特定窗口

5. **连接质量监控**
   - 显示网络延迟
   - 显示丢包率
   - 显示带宽使用

6. **UI优化**
   - 添加视频加载状态
   - 添加连接状态指示器
   - 优化视频布局

## 故障排查

### 视频不显示
1. 检查摄像头权限
2. 检查浏览器控制台错误
3. 检查WebSocket连接状态
4. 检查ICE连接状态

### 连接失败
1. 检查STUN服务器可访问性
2. 检查防火墙设置
3. 检查网络类型（可能需要TURN）

### 视频卡顿
1. 检查网络带宽
2. 降低视频分辨率
3. 检查CPU使用率

## 修改日期
2026-02-23
