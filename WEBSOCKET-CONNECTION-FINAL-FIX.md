# WebSocket连接问题最终修复

## 已修复的问题

### 1. 导入错误
- ✅ 在Meeting.vue中添加了 `import { wsService } from '../api/websocket'`
- ✅ 修复了 "wsService is not defined" 错误

### 2. 增强的日志系统
添加了详细的连接日志：
- 🔌🔌🔌 连接开始
- ✅✅✅ 连接成功
- ❌❌❌ 连接关闭/错误
- 🫀 心跳日志
- 🔄 重连日志

## 现在需要测试

### 步骤1: 强制刷新浏览器
```
Ctrl + F5 (Windows)
Cmd + Shift + R (Mac)
```

### 步骤2: 打开浏览器控制台
在加入会议前就打开控制台，观察完整的连接过程

### 步骤3: 加入会议
观察控制台输出，应该看到：

```
🔌🔌🔌 开始建立WebSocket连接 🔌🔌🔌
URL: ws://localhost:6098/ws
Token: 存在(长度XXX)
用户ID: XXXXXX
WebSocket对象已创建，等待连接...
✅✅✅ WebSocket连接成功 ✅✅✅
连接URL: ws://localhost:6098/ws
Token长度: XXX
用户ID: XXXXXX
🫀 启动心跳机制，间隔: 20000 ms
WebSocket连接成功
```

### 步骤4: 如果看到连接关闭
如果看到：
```
❌❌❌ WebSocket连接关闭 ❌❌❌
关闭代码: XXXX
关闭原因: XXXX
```

请记录：
1. 关闭代码（code）
2. 关闭原因（reason）
3. 连接成功到关闭之间的时间间隔

## 常见WebSocket关闭代码

- 1000: 正常关闭
- 1001: 端点离开（如页面关闭）
- 1002: 协议错误
- 1003: 不支持的数据类型
- 1006: 异常关闭（没有收到关闭帧）
- 1007: 无效的帧数据
- 1008: 违反策略
- 1009: 消息太大
- 1010: 缺少扩展
- 1011: 内部错误
- 1015: TLS握手失败

## 可能的问题和解决方案

### 问题1: 连接立即关闭（code 1006）
**原因**: 后端拒绝连接或网络问题
**解决**: 
- 检查后端是否正常运行
- 检查端口6098是否被占用
- 检查防火墙设置

### 问题2: Token验证失败（code 1002或1008）
**原因**: Token无效或过期
**解决**:
- 重新登录获取新token
- 检查后端token验证逻辑

### 问题3: 连接超时后关闭（code 1006）
**原因**: 心跳失败或网络不稳定
**解决**:
- 检查网络连接
- 减少心跳间隔
- 检查后端超时设置

### 问题4: 连接成功但无法发送消息
**原因**: 连接状态不同步
**解决**:
- 检查 `wsService.ws.readyState` 是否为1
- 检查 `wsService.isConnected.value` 是否为true

## 调试命令

在浏览器控制台运行：

```javascript
// 检查连接状态
console.log('WebSocket对象:', wsService.ws)
console.log('readyState:', wsService.ws?.readyState)
console.log('isConnected:', wsService.isConnected?.value)
console.log('shouldReconnect:', wsService.shouldReconnect)
console.log('currentToken:', wsService.currentToken ? '存在' : '不存在')
console.log('currentUserId:', wsService.currentUserId)

// 手动重连
const token = localStorage