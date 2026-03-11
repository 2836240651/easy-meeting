# 收件箱功能实现

## 功能描述
实现收件箱功能，显示好友申请列表，并在收件箱图标上显示未读申请数量徽章。用户可以在收件箱中接受或拒绝好友申请。

## 实现内容

### 1. 收件箱图标徽章

**文件**: `frontend/src/views/Dashboard.vue`

#### 移动端导航
```vue
<li :class="{ active: activeNav === 'inbox' }" @click="handleMobileNavClick('inbox')">
  <div class="mobile-nav-item">
    <div class="mobile-nav-icon-wrapper">
      <img src="/svg/组件-邮箱.svg" alt="Inbox" class="mobile-nav-icon">
      <span v-if="applyCount > 0" class="mobile-nav-badge">{{ applyCount }}</span>
    </div>
    <span class="mobile-nav-label">收件箱</span>
  </div>
</li>
```

#### 桌面端导航
```vue
<div :class="{ active: activeNav === 'inbox' }" class="bottom-nav-item">
  <div class="nav-item" :title="'收件箱'" @click="activeNav = 'inbox'">
    <div class="nav-icon-wrapper">
      <img src="/svg/组件-邮箱.svg" alt="Inbox" class="nav-icon">
      <span v-if="applyCount > 0" class="nav-badge">{{ applyCount }}</span>
    </div>
    <span class="nav-tooltip">收件箱</span>
  </div>
</div>
```

### 2. 收件箱页面内容

#### 页面结构
- 页面标题："收件箱"
- 好友申请区域：
  - 区域标题："好友申请"
  - 未处理数量徽章：显示待处理申请数量
  - 申请列表：显示所有待处理的好友申请
  - 空状态：无申请时显示友好提示

#### 申请列表项
每个申请项包含：
- 申请者头像（50x50，圆形）
- 申请者信息：
  - 昵称
  - 邮箱
  - 申请时间（相对时间）
- 操作按钮：
  - 接受按钮（绿色）
  - 拒绝按钮（红色）

### 3. 时间格式化

#### formatApplyTime 函数
显示相对时间，更友好的用户体验：
- 1分钟内：显示"刚刚"
- 1-60分钟：显示"X分钟前"
- 1-24小时：显示"X小时前"
- 1-7天：显示"X天前"
- 超过7天：显示具体日期（月/日 时:分）

```javascript
const formatApplyTime = (timestamp) => {
  if (!timestamp) return '未知时间'
  try {
    const now = Date.now()
    const applyTime = typeof timestamp === 'string' ? new Date(timestamp).getTime() : timestamp
    const diff = now - applyTime
    
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)
    
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days < 7) return `${days}天前`
    
    // 超过7天显示具体日期
    const date = new Date(applyTime)
    return date.toLocaleDateString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (error) {
    console.error('时间格式化失败:', error)
    return '时间错误'
  }
}
```

### 4. 自动加载逻辑

#### 切换到收件箱时自动加载
```javascript
const handleNavChange = async (nav) => {
  // ... 其他逻辑
  
  // 如果切换到收件箱，加载申请列表
  if (nav === 'inbox') {
    await loadContactApplyList()
    await loadApplyCount()
  }
  
  // ...
}
```

### 5. 样式设计

#### 深色主题配色
- 主背景：`#2a2a2a`
- 卡片背景：`#1a1a1a`
- 悬停背景：`#252525`
- 边框颜色：`#3a3a3a`
- 主文字：`#ffffff`
- 次要文字：`#999999`
- 辅助文字：`#666666`

#### 按钮配色
- 接受按钮：`#67c23a`（绿色）
- 拒绝按钮：`#f56c6c`（红色）
- 徽章背景：`#409EFF`（蓝色）

#### 交互效果
- 卡片悬停：向右平移5px，背景变亮
- 按钮悬停：向上平移2px，添加阴影
- 所有过渡动画：0.3s ease

#### 响应式设计
移动端（<768px）：
- 申请项改为垂直布局
- 头像缩小到40x40
- 操作按钮占满宽度
- 减少内边距

## 数据流程

### 1. 加载申请列表
```
用户点击收件箱 
→ handleNavChange('inbox')
→ loadContactApplyList()
→ 调用 API: /userContact/loadContactApply
→ 更新 contactApplyList
→ 渲染申请列表
```

### 2. 处理申请
```
用户点击接受/拒绝
→ handleContactApply(applyUserId, status)
→ 调用 API: /userContact/dealWithApply
→ 显示成功消息
→ 重新加载申请列表
→ 更新申请数量
→ 如果接受，重新加载联系人列表
```

### 3. 实时更新
```
收到好友申请消息（WebSocket）
→ handleContactApplyMessage()
→ loadApplyCount()
→ 更新徽章数量
→ 如果在收件箱页面，自动刷新列表
```

## 用户体验

### 视觉反馈
1. 未读徽章：红色圆形徽章显示未处理申请数量
2. 悬停效果：卡片和按钮有明显的悬停反馈
3. 空状态：友好的空状态提示和图标

### 操作流程
1. 用户看到收件箱图标上的徽章数量
2. 点击收件箱进入页面
3. 查看申请者信息（头像、昵称、邮箱、申请时间）
4. 点击"接受"或"拒绝"按钮
5. 系统显示操作结果
6. 列表自动刷新，徽章数量更新

### 时间显示
- 刚收到的申请：显示"刚刚"
- 最近的申请：显示相对时间（如"5分钟前"）
- 较早的申请：显示具体日期

## API 接口

### 1. 加载申请列表
- 接口：`GET /api/userContact/loadContactApply`
- 返回：待处理的好友申请列表
- 字段：
  - `applyId`: 申请ID
  - `applyUserId`: 申请者用户ID
  - `nickName`: 申请者昵称
  - `email`: 申请者邮箱
  - `avatar`: 申请者头像
  - `lastApplyTime`: 申请时间

### 2. 处理申请
- 接口：`POST /api/userContact/dealWithApply`
- 参数：
  - `applyUserId`: 申请者用户ID
  - `status`: 处理状态（1-接受，2-拒绝）
- 返回：操作结果

### 3. 获取申请数量
- 接口：`GET /api/userContact/loadContactApplyDealWithCount`
- 返回：待处理申请数量

## 测试步骤

### 1. 测试徽章显示
1. 确保有待处理的好友申请
2. 查看收件箱图标
3. 验证徽章显示正确的数量
4. 验证徽章样式（红色圆形，白色文字）

### 2. 测试收件箱页面
1. 点击收件箱图标
2. 验证页面标题显示"收件箱"
3. 验证好友申请区域显示
4. 验证未处理数量显示正确
5. 验证申请列表正确渲染

### 3. 测试申请列表项
1. 验证头像正确显示
2. 验证昵称、邮箱显示正确
3. 验证申请时间格式正确（相对时间）
4. 验证接受和拒绝按钮显示

### 4. 测试接受申请
1. 点击某个申请的"接受"按钮
2. 验证显示成功消息
3. 验证该申请从列表中移除
4. 验证徽章数量减1
5. 验证该用户出现在联系人列表中

### 5. 测试拒绝申请
1. 点击某个申请的"拒绝"按钮
2. 验证显示成功消息
3. 验证该申请从列表中移除
4. 验证徽章数量减1

### 6. 测试空状态
1. 处理完所有申请
2. 验证显示空状态图标和提示文字
3. 验证徽章消失

### 7. 测试实时更新
1. 使用另一个账号发送好友申请
2. 验证徽章数量自动更新
3. 如果在收件箱页面，验证列表自动刷新

### 8. 测试响应式设计
1. 在桌面端测试（>768px）
2. 在移动端测试（<768px）
3. 验证布局自动调整
4. 验证所有功能正常工作

## 相关文件
- `frontend/src/views/Dashboard.vue` - 主要实现文件
- `frontend/src/api/services.js` - API 服务
- `frontend/src/api/websocket.js` - WebSocket 实时更新

## 注意事项
1. 徽章数量实时更新，通过 WebSocket 消息触发
2. 切换到收件箱页面时自动加载最新数据
3. 处理申请后自动刷新列表和徽章
4. 接受申请后会自动更新联系人列表
5. 时间显示使用相对时间，更友好
6. 深色主题配色，与整体风格一致
7. 响应式设计，支持移动端和桌面端
