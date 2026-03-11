# 屏幕共享预览功能

## 功能说明

为发起屏幕共享的用户添加了屏幕共享预览功能，让共享者也能在应用中间看到自己正在共享的内容，就像观看者看到的那样。

## 实现内容

### 1. UI 布局

**屏幕共享悬浮层结构**：

```
┌─────────────────────────────────────────────────────────┐
│ 顶部控制条（50px 高）                                    │
│ 🔴 正在共享屏幕 │ 会议号: XXX │ ⏱️ 时长 │ [控制按钮]    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│                                                         │
│              屏幕共享预览区域（全屏）                     │
│                                                         │
│           [显示正在共享的屏幕内容]                        │
│                                                         │
│                                                         │
└─────────────────────────────────────────────────────────┘
                                    ┌──────────────┐
                                    │ 视频面板     │
                                    │ (右上角悬浮) │
                                    └──────────────┘
```

### 2. 新增元素

#### HTML 结构

在屏幕共享悬浮层中添加了预览区域：

```html
<!-- 中间屏幕共享预览区域 -->
<div class="screen-share-preview-area">
  <video 
    ref="localScreenSharePreview"
    autoplay 
    muted
    playsinline
    class="screen-share-preview-video">
  </video>
  <div class="preview-label">屏幕共享预览</div>
</div>
```

#### JavaScript

添加了新的 ref 变量：

```javascript
const localScreenSharePreview = ref(null)  // 屏幕共享预览视频元素
```

在 `startScreenShare` 函数中设置预览流：

```javascript
// 设置屏幕共享预览
if (localScreenSharePreview.value && screenStream.value) {
  localScreenSharePreview.value.srcObject = screenStream.value
  console.log('✅ 屏幕共享预览已设置')
}
```

#### CSS 样式

```css
/* 屏幕共享预览区域 */
.screen-share-preview-area {
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000000;
  pointer-events: auto;
}

.screen-share-preview-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.preview-label {
  position: absolute;
  top: 70px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  color: #ffffff;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  pointer-events: none;
  z-index: 1;
}
```

## 功能特点

### 1. 全屏预览
- 预览区域占据整个应用窗口（除了顶部控制条）
- 使用黑色背景
- 视频内容居中显示，保持宽高比（`object-fit: contain`）

### 2. 预览标签
- 在预览区域顶部显示"屏幕共享预览"标签
- 半透明黑色背景，白色文字
- 不响应鼠标事件（`pointer-events: none`）

### 3. 视频流设置
- 使用与发送给其他用户相同的 `screenStream`
- 自动播放（`autoplay`）
- 静音（`muted`）- 避免回声
- 内联播放（`playsinline`）

### 4. 层级关系
- 预览区域在最底层
- 顶部控制条在预览区域之上
- 右上角视频面板在最上层（可拖动）

## 用户体验

### 共享者视角

当用户开始屏幕共享时：

1. **顶部控制条**（固定在顶部）
   - 显示共享状态、会议号、时长
   - 提供控制按钮（静音、视频、成员、聊天、暂停、结束）

2. **中间预览区域**（全屏）
   - 实时显示正在共享的屏幕内容
   - 让共享者清楚知道其他人看到的内容
   - 顶部有"屏幕共享预览"标签提示

3. **右上角视频面板**（悬浮）
   - 显示所有会议成员的视频/头像
   - 可以拖动位置
   - 可以折叠/展开

### 观看者视角

观看者看到的内容与共享者预览的内容完全一致：
- 全屏显示共享内容
- 如果共享者开启了摄像头，左上角显示画中画

## 技术实现

### 视频流复用

```javascript
// 获取屏幕共享流
const stream = await navigator.mediaDevices.getDisplayMedia({...})

// 保存到 screenStream
screenStream.value = stream

// 1. 通过 WebRTC 发送给其他用户
await webrtcManager.startScreenShare(stream)

// 2. 在本地预览元素中显示
if (localScreenSharePreview.value && screenStream.value) {
  localScreenSharePreview.value.srcObject = screenStream.value
}
```

### 布局计算

- 顶部控制条：`top: 0, height: 50px`
- 预览区域：`top: 60px, bottom: 0`（留出 10px 间距）
- 视频面板：`position: fixed`，可拖动

## 优势

1. **一致性**: 共享者看到的内容与观看者完全一致
2. **实时反馈**: 共享者可以实时看到共享效果
3. **便于控制**: 可以及时发现并调整共享内容
4. **用户友好**: 符合常见视频会议软件的使用习惯

## 测试建议

### 基础测试

1. **开始屏幕共享**
   - 点击"共享屏幕"按钮
   - 选择屏幕/窗口
   - 验证预览区域显示共享内容
   - 验证顶部控制条正常显示
   - 验证右上角视频面板正常显示

2. **预览内容验证**
   - 在共享的屏幕/窗口中移动鼠标
   - 验证预览中能看到鼠标移动
   - 打开/关闭窗口
   - 验证预览实时更新

3. **控制功能**
   - 测试所有控制按钮
   - 验证功能正常工作
   - 验证视频面板可以拖动

4. **停止共享**
   - 点击"结束共享"
   - 验证预览区域消失
   - 验证恢复正常会议界面

### 多用户测试

1. 用户 A 开始屏幕共享
2. 验证用户 A 看到预览
3. 用户 B 加入会议
4. 验证用户 B 看到共享内容
5. 对比用户 A 的预览和用户 B 看到的内容是否一致

### 性能测试

1. 监控 CPU 和内存使用
2. 验证预览不会显著增加资源消耗
3. 测试长时间共享的稳定性

## 相关文件

- `frontend/src/views/Meeting.vue` - 主要实现文件
- `SCREEN-SHARE-MEETING-NO-DISPLAY.md` - 会议号显示功能
- `ROLLBACK-TO-ORIGINAL-SCREEN-SHARE.md` - 屏幕共享回滚说明
- `SCREEN-SHARE-FEATURE.md` - 屏幕共享功能说明

## 总结

✅ 添加了屏幕共享预览区域
✅ 共享者可以实时看到共享内容
✅ 布局合理，不影响控制功能
✅ 视频流复用，性能开销小
✅ 用户体验与主流视频会议软件一致
