# 离线好友申请通知修复

## 问题描述

用户A向离线的用户B发送好友申请后，用户B上线时收件箱没有显示待处理的好友申请通知。

## 问题分析

### 原因

1. **初始化时未加载申请数量**
   - Dashboard 在 `onMounted` 时只在 `activeNav === 'contact'` 时才加载申请数量
   - 如果用户登录后不在联系人页面，收件箱徽章不会显示未读数量

2. **轮询机制不完善**
   - 联系人列表轮询只在联系人页面时运行
   - 如果用户在其他页面，不会定期检查新的好友申请

3. **依赖 WebSocket 推送**
   - 如果用户离线时收到申请，上线后只能通过 WebSocket 推送获知
   - 但 WebSocket 消息可能丢失或延迟

## 修复方案

### 1. 始终加载申请数量

**文件：`frontend/src/views/Dashboard.vue`**

在 `onMounted` 时，无论在哪个页面都加载申请数量：

```javascript
onMounted(async () => {
  // ... 其他初始化代码 ...
  
  // 始终加载申请数量（用于显示收件箱徽章）
  await loadApplyCount()
  console.log('已加载好友申请数量')
  
  // 启动联系人和申请的轮询（始终运行）
  startContactPolling()
  
  // 加载联系人相关数据
  if (activeNav.value === 'contact') {
    await loadContactList()
  }
  
  // 如果当前在收件箱页面，加载申请列表
  if (activeNav.value === 'inbox') {
    await loadContactApplyList()
    await loadAllApplyList()
  }
  
  // ... 其他初始化代码 ...
})
```

### 2. 改进轮询机制

修改 `startContactPolling` 函数，使其始终轮询申请数量：

```javascript
const startContactPolling = () => {
  // 如果已经有轮询在运行，先停止
  if (contactPollingInterval.value) {
    clearInterval(contactPollingInterval.value)
  }
  
  console.log('启动联系人列表轮询，间隔:', CONTACT_POLLING_INTERVAL / 1000, '秒')
  
  // 设置定时器，每分钟轮询一次
  contactPollingInterval.value = setInterval(async () => {
    // 始终轮询申请数量（用于收件箱徽章）
    await loadApplyCount()
    
    // 只有在联系人页面时才轮询联系人列表
    if (activeNav.value === 'contact') {
      console.log('轮询更新联系人列表...')
      await loadContactList()
    }
    
    // 如果在收件箱页面，也轮询申请列表
    if (activeNav.value === 'inbox') {
      console.log('轮询更新收件箱申请列表...')
      await loadContactApplyList()
      await loadAllApplyList()
    }
  }, CONTACT_POLLING_INTERVAL)
}
```

### 3. 简化导航切换逻辑

修改 `handleNavChange` 函数，移除启动/停止轮询的逻辑：

```javascript
const handleNavChange = async (nav) => {
  activeNav.value = nav
  
  if (nav === 'contact') {
    await loadContactList()
    await loadApplyCount()
  }
  
  // 如果切换到收件箱，加载申请列表
  if (nav === 'inbox') {
    await loadContactApplyList()  // 加载待处理申请
    await loadAllApplyList()  // 加载所有申请
    await loadApplyCount()
  }
  
  // 页面切换后确保焦点正确
  await ensureFocus()
}
```

## 修复效果

### 1. 用户上线立即显示申请

- 用户登录后，立即加载好友申请数量
- 收件箱徽章显示未读申请数量
- 无论用户在哪个页面，都能看到徽章

### 2. 定期检查新申请

- 每60秒自动检查一次新的好友申请
- 即使 WebSocket 消息丢失，也能通过轮询获知
- 在收件箱页面时，同时更新申请列表

### 3. 实时更新

- 收到 WebSocket 消息时立即更新
- 切换到收件箱页面时立即加载最新数据
- 处理申请后立即刷新数量

## 工作流程

### 场景1：用户A向离线的用户B发送申请

1. 用户A发送好友申请
2. 后端保存申请到数据库
3. 尝试通过 WebSocket 推送给用户B（失败，因为离线）
4. 用户B上线
5. Dashboard 初始化时调用 `loadApplyCount()`
6. 收件箱徽章显示未读数量
7. 用户B点击收件箱，看到待处理的申请

### 场景2：用户B在线但在其他页面

1. 用户A发送好友申请
2. 后端保存申请并通过 WebSocket 推送
3. 用户B收到 WebSocket 消息
4. `handleContactApplyMessage` 被调用
5. 调用 `loadApplyCount()` 更新徽章
6. 收件箱徽章显示未读数量

### 场景3：WebSocket 消息丢失

1. 用户A发送好友申请
2. WebSocket 消息因网络问题丢失
3. 60秒后，轮询机制触发
4. 调用 `loadApplyCount()` 检查新申请
5. 发现有新申请，更新徽章
6. 收件箱徽章显示未读数量

## 测试建议

### 1. 离线申请测试

1. 用户B退出登录（离线）
2. 用户A向用户B发送好友申请
3. 用户B重新登录
4. 检查：
   - 收件箱徽章是否显示未读数量
   - 点击收件箱是否能看到申请

### 2. 在线申请测试

1. 用户B在线但在会议页面
2. 用户A向用户B发送好友申请
3. 检查：
   - 收件箱徽章是否立即更新
   - 切换到收件箱是否能看到申请

### 3. 轮询测试

1. 用户B在线
2. 手动在数据库中插入一条好友申请记录
3. 等待60秒
4. 检查：
   - 收件箱徽章是否自动更新
   - 申请数量是否正确

### 4. 多申请测试

1. 多个用户向用户B发送好友申请
2. 检查：
   - 徽章数量是否正确累加
   - 收件箱是否显示所有申请

## 相关API

### 加载申请数量

```
GET /api/userContact/loadContactApplyDealWithCount
```

返回待处理的好友申请数量。

### 加载待处理申请列表

```
GET /api/userContact/loadContactApply
```

返回所有待处理的好友申请列表。

### 加载所有申请列表

```
GET /api/userContact/loadAllContactApply
```

返回所有状态的好友申请列表（包括已处理）。

## 性能优化建议

### 1. 调整轮询间隔

当前轮询间隔为60秒，可以根据实际需求调整：

```javascript
const CONTACT_POLLING_INTERVAL = 60000 // 1分钟
```

建议：
- 高频场景：30秒
- 正常场景：60秒
- 低频场景：120秒

### 2. 智能轮询

可以根据用户活跃度动态调整轮询间隔：

```javascript
// 用户活跃时：30秒
// 用户不活跃时：120秒
```

### 3. 使用 WebSocket 心跳

确保 WebSocket 连接稳定，减少消息丢失：

```javascript
// 已实现心跳机制
heartbeatTimer: 20000 // 20秒心跳
```

## 注意事项

1. **轮询始终运行**
   - 轮询在 Dashboard 初始化时启动
   - 在 Dashboard 卸载时停止
   - 不会因为切换页面而停止

2. **避免重复请求**
   - 切换页面时会立即加载数据
   - 轮询会在60秒后再次加载
   - 不会造成过多的请求

3. **WebSocket 优先**
   - 优先使用 WebSocket 推送
   - 轮询作为备用机制
   - 确保消息不丢失

4. **徽章显示**
   - 徽章显示在移动端和桌面端
   - 数量为0时不显示徽章
   - 数量大于99时显示"99+"

## 相关文件

### 修改的文件
- `frontend/src/views/Dashboard.vue` - 修复申请加载和轮询逻辑

### 相关文件
- `frontend/src/api/services.js` - API 服务定义
- `frontend/src/api/websocket.js` - WebSocket 服务
- `src/main/java/com/easymeeting/controller/UserContactController.java` - 好友申请控制器

## 后续优化建议

1. **添加桌面通知**
   - 收到新申请时显示桌面通知
   - 需要用户授权通知权限

2. **声音提示**
   - 收到新申请时播放提示音
   - 可以在设置中开关

3. **申请详情预览**
   - 鼠标悬停在徽章上显示最新申请
   - 无需点击进入收件箱

4. **批量处理**
   - 支持批量接受/拒绝申请
   - 提高处理效率
