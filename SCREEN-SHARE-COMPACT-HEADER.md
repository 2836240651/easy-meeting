# 屏幕共享紧凑顶部栏设计

## 设计概述

根据用户需求，重新设计了屏幕共享时的界面布局：
- 发起屏幕共享后，会议顶部栏缩小到顶部
- 在顶部栏下方添加一个控制条，包含所有屏幕共享相关功能
- 移除了之前的自动隐藏工具栏设计

## 实现的功能

### 1. 紧凑模式顶部栏

当用户发起屏幕共享时，顶部栏会自动切换到紧凑模式：

**变化：**
- 高度从 60px 缩小到 40px
- 内边距减小
- 背景添加半透明效果和模糊
- 按钮尺寸缩小
- 显示会议时长（绿色高亮显示）

**包含元素：**
- 左侧：会议详情按钮 + 会议时长显示
- 右侧：设置、最小化、全屏、关闭按钮

### 2. 屏幕共享控制条

紧贴在紧凑顶部栏下方，提供所有屏幕共享控制功能：

**左侧：**
- 分享会议按钮

**中间：**
- 静音/解除静音（图标按钮）
- 开启/关闭视频（图标按钮）
- 成员（图标按钮）
- 聊天（图标按钮）
- 录制（图标按钮）
- 暂停/恢复共享（图标按钮）
- 结束共享（带文字的危险按钮）

## 技术实现

### HTML 结构

```vue
<!-- 会议顶部栏 -->
<div class="meeting-header" :class="{ 'compact-mode': isScreenSharing && currentScreenSharingUserId === currentUserId }">
  <div class="meeting-info-section">
    <button class="meeting-info-btn" @click="showMeetingInfo">会议详情</button>
    <!-- 屏幕共享时显示会议时长 -->
    <div v-if="isScreenSharing && currentScreenSharingUserId === currentUserId" class="header-duration">
      <span class="duration-icon">⏱️</span>
      <span class="duration-text">{{ formattedDuration }}</span>
    </div>
  </div>
  <!-- 窗口控制按钮 -->
</div>

<!-- 屏幕共享控制条 -->
<div v-if="isScreenSharing && currentScreenSharingUserId === currentUserId" class="screen-share-controls">
  <div class="controls-left">
    <button class="control-btn" @click="shareMeeting">
      <span class="btn-icon">📤</span>
      <span class="btn-text">分享会议</span>
    </button>
  </div>
  <div class="controls-center">
    <!-- 各种控制按钮 -->
  </div>
</div>
```

### CSS 样式

#### 紧凑模式顶部栏
```css
.meeting-header.compact-mode {
  height: 40px;
  padding: 6px 16px;
  background-color: rgba(67, 67, 67, 0.95);
  backdrop-filter: blur(10px);
}
```

#### 会议时长显示
```css
.header-duration {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background-color: rgba(76, 175, 80, 0.2);
  border: 1px solid rgba(76, 175, 80, 0.4);
  border-radius: 6px;
  color: #4caf50;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  font-weight: 600;
}
```

#### 控制条
```css
.screen-share-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 20px;
  background-color: rgba(67, 67, 67, 0.98);
  border-bottom: 1px solid #555555;
  z-index: 99;
}
```

#### 控制按钮
```css
.control-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 12px;
  background-color: rgba(85, 85, 85, 0.8);
  border: 1px solid rgba(153, 153, 153, 0.3);
  border-radius: 6px;
  color: #dfdfdf;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
}
```

### 移除的功能

1. **自动隐藏工具栏** - 不再需要鼠标移动触发显示/隐藏
2. **`showScreenShareToolbar`** - 移除状态变量
3. **`toolbarHideTimer`** - 移除计时器
4. **`hideToolbarWithDelay()`** - 移除函数

### 保留的功能

1. **会议时长计时器** - 继续在顶部栏显示
2. **所有控制功能** - 静音、视频、成员、聊天、录制、暂停、结束
3. **分享会议功能** - 复制会议信息到剪贴板
4. **暂停/恢复共享** - 通过禁用视频轨道实现

## 用户体验

### 优点

1. **始终可见** - 控制条始终显示，无需鼠标移动触发
2. **紧凑布局** - 顶部栏缩小，最大化屏幕共享区域
3. **清晰分层** - 顶部栏（会议信息）+ 控制条（操作按钮）
4. **实时信息** - 会议时长始终可见
5. **快速操作** - 所有功能一键可达

### 视觉效果

- 顶部栏：半透明深灰色，带模糊效果
- 会议时长：绿色高亮，易于识别
- 控制条：深灰色背景，与顶部栏协调
- 按钮：统一的圆角设计，悬停效果
- 危险操作：红色高亮（结束共享）

## 布局示例

```
┌─────────────────────────────────────────────────────────┐
│ [会议详情] [⏱️ 05:30]              [⚙️] [−] [⛶] [×]    │ ← 紧凑顶部栏 (40px)
├─────────────────────────────────────────────────────────┤
│ [📤 分享会议]  [🎤] [📹] [👥] [💬] [⏺️] [⏸️] [🛑 结束共享] │ ← 控制条
├─────────────────────────────────────────────────────────┤
│                                                         │
│                  屏幕共享内容区域                         │
│                                                         │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## 响应式行为

- 顶部栏在屏幕共享时自动切换到紧凑模式
- 控制条仅在用户发起屏幕共享时显示
- 停止共享后，顶部栏恢复正常大小，控制条消失

## 测试要点

1. ✅ 点击"共享屏幕"，选择选项，开始共享
2. ✅ 验证顶部栏缩小到 40px
3. ✅ 验证会议时长显示在顶部栏
4. ✅ 验证控制条显示在顶部栏下方
5. ✅ 测试所有控制按钮功能
6. ✅ 点击"结束共享"，验证恢复正常布局
7. ✅ 验证会议时长持续更新
8. ✅ 验证暂停/恢复共享功能

## 文件修改

- `frontend/src/views/Meeting.vue`
  - 修改顶部栏 HTML，添加紧凑模式 class
  - 添加会议时长显示
  - 替换工具栏为控制条
  - 移除自动隐藏相关代码
  - 更新 CSS 样式

## 总结

新设计更加简洁直观，符合用户"发起共享屏幕后，会议信息缩小到顶部"的需求。所有功能都保留，但布局更加紧凑，用户体验更好。
