# WebSocket轮询问题修复

## 问题描述

在会议页面（Meeting.vue）中，控制台一直输出"开始加载当前会议信息"，说明：
1. Dashboard.vue 的定时器仍在运行（每30秒轮询一次）
2. Dashboard.vue 组件可能没有被正确卸载
3. 这不是WebSocket长连接应有的行为

## 问题根源

### Dashboard.vue 中的轮询代码

```javascript
// 设置定时器定期检查当前会议状态
setInterval(async () => {
  if (currentMeeting.value) {
    await loadCurrentMeeting()
  }
}, 30000) // 每30秒检查一次
```

**问题：**
1. 使用了 `setInterval` 但没有保存定时器ID
2. `onUnmounted` 中没有清理这个定时器
3. 当路由切换到 Meeting.vue 时，Dashboard.vue 可能没有被卸载（取决于路由配置）

## 解决方案

### 方案1：修复 Dashboard.vue 的定时器清理

```javascript
// 在 Dashboard.vue 的 setup 中添加
const meetingPollingInterval = ref(null)

onMounted(async () => {
  // ... 其他初始化代码 ...
  
  // 设置定时器定期检查当前会议状态
  meetingPollingInterval.value = setInterval(async () => {
    if (currentMeeting.value) {
      await loadCurrentMeeting()
    }
  }, 30000) // 每30秒检查一次
})

onUnmounted(() => {
  // 清理会议轮询定时器
  if (meetingPollingInterval.value) {
    clearInterval(meetingPollingInterval.value)
    meetingPollingInterval.value = null
  }
  
  // 停止联系人列表轮询
  stopContactPolling()
  
  wsService.offMessage(MessageType.USER_CONTACT_APPLY, handleContactApplyMessage)
  wsService.disconnect()
})
```

### 方案2：使用 WebSocket 替代轮询

**更好的做法是完全移除轮询，使用WebSocket实时更新：**

```javascript
// 在 Dashboard.vue 中
onMounted(async () => {
  // ... 其他初始化代码 ...
  
  // ❌ 删除这个轮询定时器
  // setInterval(async () => {
  //   if (currentMeeting.value) {
  //     await loadCurrentMeeting()
  //   }
  // }, 30000)
  
  // ✅ 使用 WebSocket 监听会议状态变化
  wsService.onMessage(MessageType.MEETING_STATUS_CHANGE, (message) => {
    console.log('会议状态变化:', message)
    loadCurrentMeeting()
  })
})
```

### 方案3：在进入会议页面时停止轮询

```javascript
// 在 Meeting.vue 的 onMounted 中
onMounted(() => {
  // 停止 Dashboard 的轮询（如果存在）
  if (window.dashboardPollingInterval) {
    clearInterval(window.dashboardPollingInterval)
    window.dashboardPollingInterval = null
  }
  
  loadUserInfo()
  joinMeeting()
  // ... 其他初始化代码 ...
})
```

## 推荐实施步骤

### 步骤1：立即修复 - 添加定时器清理

修改 `frontend/src/views/Dashboard.vue`：

1. 在 `setup()` 函数开始处添加：
```javascript
const meetingPollingInterval = ref(null)
```

2. 在 `onMounted` 中修改定时器创建：
```javascript
// 设置定时器定期检查当前会议状态
meetingPollingInterval.value = setInterval(async () => {
  if (currentMeeting.value) {
    await loadCurrentMeeting()
  }
}, 30000)
```

3. 在 `onUnmounted` 中添加清理：
```javascript
// 清理会议轮询定时器
if (meetingPollingInterval.value) {
  clearInterval(meetingPollingInterval.value)
  meetingPollingInterval.value = null
}
```

### 步骤2：长期优化 - 移除轮询

会议状态应该通过WebSocket实时推送，而不是轮询：

1. 后端发送会议状态变化消息
2. 前端通过WebSocket接收并更新UI
3. 完全移除定时器轮询

## 验证修复

### 测试步骤

1. 打开浏览器控制台
2. 进入Dashboard页面
3. 加入会议，进入Meeting页面
4. 观察控制台输出

**修复前：**
```
开始加载当前会议信息...  (每30秒输出一次)
开始加载当前会议信息...
开始加载当前会议信息...
```

**修复后：**
```
(不再有重复的轮询日志)
```

### 检查定时器

在控制台执行：
```javascript
// 检查是否有活跃的定时器
console.log('活跃定时器数量:', window.setInterval.length)
```

## 为什么这很重要

1. **性能问题**：不必要的HTTP请求浪费资源
2. **WebSocket优势**：实时性更好，延迟更低
3. **资源泄漏**：未清理的定时器会导致内存泄漏
4. **用户体验**：减少不必要的网络请求

## 相关文件

- `frontend/src/views/Dashboard.vue` - 需要修复定时器清理
- `frontend/src/views/Meeting.vue` - 使用WebSocket长连接
- `frontend/src/api/meeting-websocket.js` - WebSocket服务

## 下一步

1. 先实施步骤1的快速修复
2. 测试验证定时器被正确清理
3. 考虑步骤2的长期优化方案
