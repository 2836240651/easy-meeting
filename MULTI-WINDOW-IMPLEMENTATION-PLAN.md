# 多窗口屏幕共享实现计划

## 已完成

### ✅ Electron Main.js
- 添加了多窗口创建函数
- 添加了 IPC 处理器
- 添加了窗口间通信机制

### ✅ Preload.js
- 添加了新的 IPC 方法
- 添加了窗口间通信方法

## 待实现

### 1. 创建 Vue 组件（4个）

#### A. ScreenShareTopBar.vue
**路径**: `frontend/src/views/ScreenShareTopBar.vue`

**功能**:
- 显示"您正在共享屏幕"指示器
- 鼠标悬停时展开显示控制按钮
- 控制按钮：静音、视频、暂停、结束共享

**关键点**:
- 透明背景
- 自动展开/收起
- 通过 IPC 与主窗口通信

#### B. ScreenShareVideo.vue
**路径**: `frontend/src/views/ScreenShareVideo.vue`

**功能**:
- 显示所有会议成员的视频
- 可拖动窗口
- 可折叠/展开
- 显示音频状态

**关键点**:
- 接收主窗口的参与者数据
- 显示视频流（需要通过某种方式传递 MediaStream）
- 拖动功能

#### C. ScreenShareChat.vue
**路径**: `frontend/src/views/ScreenShareChat.vue`

**功能**:
- 显示聊天消息
- 发送消息
- 可拖动窗口
- 可折叠/展开

**关键点**:
- 接收主窗口的聊天消息
- 发送消息到主窗口
- 拖动功能

#### D. ScreenShareBorder.vue
**路径**: `frontend/src/views/ScreenShareBorder.vue`

**功能**:
- 显示绿色边框角标
- 根据 corner 参数显示不同位置的角标

**关键点**:
- 完全透明背景
- 只显示绿色边框
- 不响应鼠标事件

### 2. 修改路由配置

**文件**: `frontend/src/router/index.js`

添加 4 个新路由：
```javascript
{
  path: '/screen-share-topbar',
  name: 'ScreenShareTopBar',
  component: () => import('../views/ScreenShareTopBar.vue')
},
{
  path: '/screen-share-video',
  name: 'ScreenShareVideo',
  component: () => import('../views/ScreenShareVideo.vue')
},
{
  path: '/screen-share-chat',
  name: 'ScreenShareChat',
  component: () => import('../views/ScreenShareChat.vue')
},
{
  path: '/border',
  name: 'ScreenShareBorder',
  component: () => import('../views/ScreenShareBorder.vue')
}
```

### 3. 修改 Meeting.vue

在 `startScreenShare` 函数中调用：
```javascript
// 获取共享区域边界（如果可能）
const shareBounds = {
  x: 0,
  y: 0,
  width: screen.width,
  height: screen.height
}

// 创建悬浮窗口
if (window.electron) {
  await window.electron.startScreenShareOverlay(shareBounds)
}
```

在 `stopScreenShare` 函数中调用：
```javascript
// 关闭悬浮窗口
if (window.electron) {
  await window.electron.stopScreenShareOverlay()
}
```

### 4. 窗口间数据同步

**挑战**: MediaStream 无法直接通过 IPC 传递

**解决方案**:
1. **方案 A**: 在每个窗口中独立建立 WebRTC 连接
   - 优点：完全独立，数据隔离
   - 缺点：复杂，资源消耗大

2. **方案 B**: 使用 SharedWorker 或 BroadcastChannel
   - 优点：可以共享数据
   - 缺点：MediaStream 仍然无法传递

3. **方案 C**: 使用 Canvas 捕获视频帧，通过 IPC 传递
   - 优点：可以传递视频数据
   - 缺点：性能开销大，延迟高

4. **方案 D（推荐）**: 在悬浮窗口中重新获取视频流
   - 在视频窗口中，通过 WebRTC 重新连接获取视频流
   - 通过 IPC 传递连接信息（userId, offer/answer）

## 实现优先级

### Phase 1: 基础框架（MVP）
1. ✅ Electron main.js 修改
2. ✅ Preload.js 修改
3. ⏳ 创建 ScreenShareTopBar.vue（简化版）
4. ⏳ 创建 ScreenShareBorder.vue
5. ⏳ 修改路由
6. ⏳ 修改 Meeting.vue 调用

### Phase 2: 视频窗口
1. 创建 ScreenShareVideo.vue
2. 实现窗口间 WebRTC 连接
3. 实现拖动功能

### Phase 3: 聊天窗口
1. 创建 ScreenShareChat.vue
2. 实现消息同步
3. 实现拖动功能

### Phase 4: 完善和优化
1. 添加动画效果
2. 优化性能
3. 处理边界情况
4. 添加错误处理

## 技术难点

### 1. MediaStream 传递
**问题**: MediaStream 对象无法通过 IPC 序列化传递

**解决方案**:
- 在视频窗口中重新建立 WebRTC 连接
- 主窗口作为信令服务器，转发 offer/answer/ICE

### 2. 窗口拖动
**问题**: 透明窗口的拖动区域难以控制

**解决方案**:
- 使用 `-webkit-app-region: drag` CSS 属性
- 在可拖动区域设置该属性

### 3. 窗口层级
**问题**: 确保所有悬浮窗口始终在最上层

**解决方案**:
- 使用 `alwaysOnTop: true`
- 定期检查窗口层级

### 4. 性能优化
**问题**: 多个窗口同时渲染视频可能导致性能问题

**解决方案**:
- 使用硬件加速
- 限制视频分辨率
- 使用 requestAnimationFrame 优化渲染

## 下一步

由于这是一个非常复杂的实现，建议：

1. **先实现 Phase 1（MVP）**
   - 只创建顶部条和边框
   - 验证多窗口机制是否工作
   - 不包含视频和聊天功能

2. **测试 MVP**
   - 确保窗口能正确创建和销毁
   - 确保主窗口能正确隐藏和显示
   - 确保边框显示在正确位置

3. **逐步添加功能**
   - 先添加视频窗口（最复杂）
   - 再添加聊天窗口
   - 最后完善细节

是否继续实现 Phase 1（MVP）？
