# 观看者视频面板功能（可拖动悬浮窗口）

## 功能说明

为观看屏幕共享的用户添加了右上角可拖动的成员视频面板，样式和功能与共享者的视频面板一致。提供两种显示模式：
1. **只显示自己** - 只显示自己的视频/头像
2. **显示全部成员** - 显示所有会议成员的视频/头像

## 实现内容

### 1. UI 布局

**观看者视角布局**：

```
┌─────────────────────────────────────────────────────┐
│  ┌──────────┐                    ┌──────────────┐  │
│  │ 画中画    │                    │ 会议成员 (N) │  │
│  │ (共享者)  │                    │ [👤][−]      │  │
│  └──────────┘                    ├──────────────┤  │
│                                  │  ┌────────┐  │  │
│              屏幕共享画面          │  │ 自己   │  │  │
│                                  │  └────────┘  │  │
│         [显示共享者的屏幕内容]     │              │  │
│                                  └──────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 2. 面板特性

#### 可拖动悬浮窗口
- **位置**：默认在右上角（距离右边 20px，距离顶部 60px）
- **尺寸**：280px 宽，最大高度 600px
- **拖动**：鼠标按住标题栏可以拖动到任意位置
- **样式**：圆角、半透明黑色背景、模糊效果、阴影

#### 两种显示模式

**模式 1：只显示自己（默认）**
- 图标：👤
- 只显示当前用户的视频/头像
- 可以控制自己的音频（静音/解除静音）
- 节省空间，专注观看共享内容

**模式 2：显示全部成员**
- 图标：👥
- 显示所有会议成员的视频/头像
- 包括自己（绿色边框标识）和其他参与者
- 可以看到所有人的音频状态
- 可以滚动查看（成员较多时）

#### 面板控制

- **拖动**：按住标题栏拖动
- **切换模式**：点击 👤/👥 按钮
- **折叠/展开**：点击 −/+ 按钮
- **音频控制**：在自己的视频上点击音频按钮

### 3. 技术实现

#### HTML 结构

```html
<!-- 右上角成员视频面板（可拖动） -->
<div class="viewer-video-panel" :style="{ left: viewerPanelPosition.x + 'px', top: viewerPanelPosition.y + 'px' }">
  <div class="viewer-panel-header" @mousedown="startDragViewerPanel">
    <span class="viewer-panel-title">会议成员 ({{ allParticipants.length }})</span>
    <div class="viewer-panel-controls">
      <button class="viewer-panel-mode-btn" @click="toggleViewerDisplayMode">
        {{ viewerDisplayMode === 'self' ? '👤' : '👥' }}
      </button>
      <button class="viewer-panel-toggle-btn" @click="toggleViewerPanel">
        {{ viewerPanelExpanded ? '−' : '+' }}
      </button>
    </div>
  </div>
  
  <div v-if="viewerPanelExpanded" class="viewer-panel-content">
    <!-- 视频内容 -->
  </div>
</div>
```

#### JavaScript

```javascript
// 状态变量
const viewerPanelPosition = ref({ x: 0, y: 60 })  // 面板位置
const isDraggingViewerPanel = ref(false)  // 是否正在拖动
const viewerPanelExpanded = ref(true)  // 是否展开
const viewerDisplayMode = ref('self')  // 显示模式

// 初始化位置
const initViewerPanelPosition = () => {
  const videoArea = document.querySelector('.video-area')
  if (videoArea) {
    const rect = videoArea.getBoundingClientRect()
    viewerPanelPosition.value = {
      x: rect.width - 300,  // 右上角
      y: 60
    }
  }
}

// 拖动功能
const startDragViewerPanel = (event) => {
  isDraggingViewerPanel.value = true
  // ... 拖动逻辑
}
```

#### CSS 样式

```css
.viewer-video-panel {
  position: fixed;
  width: 280px;
  max-height: 600px;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
  z-index: 100;
}

.viewer-panel-header {
  cursor: move;
  user-select: none;
}

.viewer-panel-header:active {
  cursor: grabbing;
}
```

## 与共享者面板的一致性

观看者的视频面板与共享者的视频面板保持一致：

| 特性 | 共享者面板 | 观看者面板 |
|------|-----------|-----------|
| 位置 | 可拖动悬浮 | 可拖动悬浮 ✅ |
| 尺寸 | 280px 宽 | 280px 宽 ✅ |
| 样式 | 圆角、半透明、模糊 | 圆角、半透明、模糊 ✅ |
| 拖动 | 标题栏拖动 | 标题栏拖动 ✅ |
| 折叠 | 支持 | 支持 ✅ |
| 滚动 | 支持 | 支持 ✅ |
| 视频显示 | 视频/头像 | 视频/头像 ✅ |
| 额外功能 | - | 切换显示模式 ⭐ |

## 用户体验

### 拖动面板

1. 鼠标移到标题栏
2. 光标变为 `move`（移动图标）
3. 按住鼠标左键
4. 光标变为 `grabbing`（抓取图标）
5. 拖动到任意位置
6. 释放鼠标
7. 面板固定在新位置

### 切换显示模式

**从"只显示自己"到"显示全部"**：
1. 点击 👤 按钮
2. 按钮变为 👥
3. 面板显示所有成员
4. 视频流自动更新

**从"显示全部"到"只显示自己"**：
1. 点击 👥 按钮
2. 按钮变为 👤
3. 面板只显示自己
4. 视频流自动更新

### 折叠/展开

1. 点击 "−" 按钮折叠
2. 面板只显示标题栏
3. 点击 "+" 按钮展开
4. 面板显示完整内容

## 优势

1. **一致性**：与共享者面板样式完全一致
2. **灵活性**：可以拖动到任意位置，不遮挡重要内容
3. **节省空间**：默认只显示自己，不占用太多空间
4. **易于操作**：拖动、折叠、切换模式都很直观
5. **视觉美观**：半透明、模糊效果，现代化设计

## 测试建议

### 基础测试

1. **观看屏幕共享**
   - 用户 A 开始屏幕共享
   - 用户 B 加入会议
   - 验证用户 B 看到：
     - ✅ 屏幕共享内容
     - ✅ 右上角视频面板
     - ✅ 默认显示自己的视频

2. **拖动面板**
   - 按住标题栏
   - 拖动到不同位置
   - 验证面板跟随鼠标移动
   - 验证不会拖出视频区域

3. **切换显示模式**
   - 点击 👤 按钮
   - 验证显示所有成员
   - 验证视频流正确显示
   - 点击 👥 按钮
   - 验证只显示自己

4. **折叠/展开**
   - 点击 "−" 按钮
   - 验证面板折叠
   - 点击 "+" 按钮
   - 验证面板展开

### 多用户测试

1. 3-5 个用户加入会议
2. 用户 A 开始屏幕共享
3. 其他用户验证：
   - 视频面板显示正确
   - 拖动功能正常
   - 切换模式正常
   - 视频流显示正确

### 边界测试

1. 拖动到边界
2. 验证不会超出视频区域
3. 测试快速拖动
4. 测试多次切换模式

## 相关文件

- `frontend/src/views/Meeting.vue` - 主要实现文件
- `SCREEN-SHARE-PREVIEW-FOR-SHARER.md` - 共享者预览功能
- `SCREEN-SHARE-MEETING-NO-DISPLAY.md` - 会议号显示功能

## 总结

✅ 观看者视频面板改为可拖动悬浮窗口
✅ 与共享者面板样式完全一致
✅ 支持拖动到任意位置
✅ 提供两种显示模式
✅ 支持折叠/展开
✅ 视频流自动更新
✅ 用户体验友好，操作直观

## 实现内容

### 1. UI 布局

**观看者视角布局**：

```
┌─────────────────────────────────────────────────────┬──────────┐
│                                                     │ 会议成员  │
│                                                     │ (N)      │
│                                                     │ [👤][−]  │
│              屏幕共享画面（全屏）                     ├──────────┤
│                                                     │          │
│         [显示共享者的屏幕内容]                        │  [自己]  │
│                                                     │          │
│                                                     │          │
└─────────────────────────────────────────────────────┴──────────┘
  ┌──────────┐
  │ 画中画    │ (左上角，共享者摄像头)
  └──────────┘
```

### 2. 新增元素

#### HTML 结构

在屏幕共享观看视图中添加了右侧视频面板：

```html
<!-- 右侧成员视频面板 -->
<div class="viewer-video-panel" :class="{ collapsed: !viewerPanelExpanded }">
  <div class="viewer-panel-header">
    <span class="viewer-panel-title">会议成员 ({{ allParticipants.length }})</span>
    <div class="viewer-panel-controls">
      <!-- 切换显示模式按钮 -->
      <button class="viewer-panel-mode-btn" @click="toggleViewerDisplayMode">
        {{ viewerDisplayMode === 'self' ? '👤' : '👥' }}
      </button>
      <!-- 折叠/展开按钮 -->
      <button class="viewer-panel-toggle-btn" @click="toggleViewerPanel">
        {{ viewerPanelExpanded ? '−' : '+' }}
      </button>
    </div>
  </div>
  
  <div v-if="viewerPanelExpanded" class="viewer-panel-content">
    <!-- 根据显示模式显示不同内容 -->
  </div>
</div>
```

#### JavaScript

添加了新的状态变量：

```javascript
const viewerPanelExpanded = ref(true)  // 面板是否展开
const viewerDisplayMode = ref('self')  // 显示模式：'self' 或 'all'
const viewerLocalVideo = ref(null)  // 本地视频元素（只显示自己模式）
const viewerLocalVideoAll = ref(null)  // 本地视频元素（显示全部模式）
const viewerParticipantVideoRefs = ref(new Map())  // 参与者视频元素
```

添加了控制函数：

```javascript
// 切换面板展开/折叠
const toggleViewerPanel = () => {
  viewerPanelExpanded.value = !viewerPanelExpanded.value
}

// 切换显示模式
const toggleViewerDisplayMode = () => {
  viewerDisplayMode.value = viewerDisplayMode.value === 'self' ? 'all' : 'self'
  nextTick(() => {
    updateViewerVideoStreams()
  })
}

// 更新视频流
const updateViewerVideoStreams = () => {
  // 设置本地视频流
  // 设置参与者视频流
}
```

#### CSS 样式

```css
/* 观看者视频面板 */
.viewer-video-panel {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  width: 320px;
  background: rgba(0, 0, 0, 0.95);
  backdrop-filter: blur(20px);
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  z-index: 100;
  transition: transform 0.3s ease;
}

.viewer-video-panel.collapsed {
  transform: translateX(280px);  /* 折叠时只露出 40px */
}
```

## 功能特点

### 1. 两种显示模式

#### 只显示自己模式（默认）
- 图标：👤
- 只显示当前用户的视频/头像
- 节省屏幕空间
- 适合专注观看共享内容

#### 显示全部成员模式
- 图标：👥
- 显示所有会议成员的视频/头像
- 包括自己和其他参与者
- 适合需要看到所有人的场景

### 2. 面板控制

- **折叠/展开**：点击 "−" 或 "+" 按钮
- **切换模式**：点击 👤 或 👥 按钮
- **音频控制**：在自己的视频上可以切换静音

### 3. 视频流显示

- 如果成员开启了摄像头，显示视频流
- 如果成员关闭了摄像头，显示头像
- 自己的视频有绿色边框标识
- 显示音频状态（静音/正常）

### 4. 面板特性

- **固定位置**：右侧固定，不影响屏幕共享内容
- **半透明背景**：黑色背景 + 模糊效果
- **可滚动**：成员较多时可以滚动查看
- **自定义滚动条**：细窄的滚动条，不占用太多空间

## 布局说明

### 观看者视角完整布局

```
┌─────────────────────────────────────────────────────┬──────────┐
│                                                     │ 标题栏    │
│  ┌──────────┐                                      │ [👤][−]  │
│  │ 画中画    │                                      ├──────────┤
│  │ (共享者)  │                                      │          │
│  └──────────┘                                      │  ┌────┐  │
│                                                     │  │自己│  │
│              屏幕共享画面                            │  └────┘  │
│                                                     │          │
│         [显示共享者的屏幕内容]                        │  ┌────┐  │
│                                                     │  │成员│  │
│                                                     │  └────┘  │
│                                                     │          │
│                                                     │   ...    │
└─────────────────────────────────────────────────────┴──────────┘
```

### 尺寸规格

- **面板宽度**：320px
- **面板位置**：右侧固定
- **视频项宽高比**：16:9
- **视频项间距**：12px
- **折叠后露出**：40px

## 用户交互

### 切换显示模式

1. **从"只显示自己"切换到"显示全部"**
   - 点击 👤 按钮
   - 按钮变为 👥
   - 面板显示所有成员

2. **从"显示全部"切换到"只显示自己"**
   - 点击 👥 按钮
   - 按钮变为 👤
   - 面板只显示自己

### 折叠/展开面板

1. **折叠面板**
   - 点击 "−" 按钮
   - 面板向右滑动，只露出 40px
   - 按钮变为 "+"

2. **展开面板**
   - 点击 "+" 按钮
   - 面板向左滑动，完全展开
   - 按钮变为 "−"

### 音频控制

- 在自己的视频上点击音频按钮
- 可以切换静音/解除静音
- 按钮颜色变化：正常（白色）/ 静音（红色）

## 技术实现

### 视频流管理

```javascript
// 当切换显示模式时
const toggleViewerDisplayMode = () => {
  viewerDisplayMode.value = viewerDisplayMode.value === 'self' ? 'all' : 'self'
  
  // 等待 DOM 更新后设置视频流
  nextTick(() => {
    updateViewerVideoStreams()
  })
}

// 更新视频流
const updateViewerVideoStreams = () => {
  // 1. 设置本地视频流
  if (viewerDisplayMode.value === 'self' && viewerLocalVideo.value) {
    viewerLocalVideo.value.srcObject = localStream.value
  } else if (viewerDisplayMode.value === 'all' && viewerLocalVideoAll.value) {
    viewerLocalVideoAll.value.srcObject = localStream.value
  }
  
  // 2. 设置参与者视频流
  if (viewerDisplayMode.value === 'all') {
    participants.value.forEach(participant => {
      if (participant.videoOpen && participant.videoRef) {
        const viewerVideoEl = viewerParticipantVideoRefs.value.get(participant.userId)
        if (viewerVideoEl && participant.videoRef.srcObject) {
          viewerVideoEl.srcObject = participant.videoRef.srcObject
        }
      }
    })
  }
}
```

### 自动更新

当收到屏幕共享开始消息时，自动更新视频流：

```javascript
meetingWsService.on('screenShareStart', async (message) => {
  // ... 设置状态
  
  // 更新观看者视频面板的视频流
  await nextTick()
  updateViewerVideoStreams()
})
```

## 优势

1. **灵活性**：两种显示模式满足不同需求
2. **节省空间**：默认只显示自己，不占用太多屏幕空间
3. **易于切换**：一键切换显示模式
4. **视觉一致**：与共享者的视频面板风格一致
5. **用户友好**：符合常见视频会议软件的使用习惯

## 测试建议

### 基础测试

1. **观看屏幕共享**
   - 用户 A 开始屏幕共享
   - 用户 B 加入会议
   - 验证用户 B 看到：
     - ✅ 屏幕共享内容
     - ✅ 右侧视频面板
     - ✅ 默认显示自己的视频

2. **切换显示模式**
   - 点击 👤 按钮
   - 验证显示所有成员
   - 点击 👥 按钮
   - 验证只显示自己

3. **折叠/展开面板**
   - 点击 "−" 按钮
   - 验证面板折叠
   - 点击 "+" 按钮
   - 验证面板展开

4. **音频控制**
   - 在自己的视频上点击音频按钮
   - 验证静音状态切换

### 多用户测试

1. 3-5 个用户加入会议
2. 用户 A 开始屏幕共享
3. 其他用户验证：
   - 视频面板显示正确
   - 切换模式正常工作
   - 视频流显示正确
   - 音频状态同步

### 性能测试

1. 测试多成员场景（10+ 用户）
2. 验证视频面板滚动流畅
3. 验证切换模式无卡顿
4. 监控 CPU 和内存使用

## 相关文件

- `frontend/src/views/Meeting.vue` - 主要实现文件
- `SCREEN-SHARE-PREVIEW-FOR-SHARER.md` - 共享者预览功能
- `SCREEN-SHARE-MEETING-NO-DISPLAY.md` - 会议号显示功能
- `ROLLBACK-TO-ORIGINAL-SCREEN-SHARE.md` - 屏幕共享回滚说明

## 总结

✅ 添加了观看者右侧视频面板
✅ 提供两种显示模式（只显示自己/显示全部）
✅ 支持折叠/展开面板
✅ 支持音频控制
✅ 视频流自动更新
✅ 视觉效果与共享者面板一致
✅ 用户体验友好，操作简单
