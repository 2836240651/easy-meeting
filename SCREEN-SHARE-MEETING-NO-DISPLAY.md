# 屏幕共享控制条显示会议号

## 更新内容

在屏幕共享悬浮层的顶部控制条中添加了会议号显示。

## 修改位置

**文件**: `frontend/src/views/Meeting.vue`

### HTML 结构

在 `bar-left` 部分添加了会议号显示：

```html
<div class="bar-left">
  <span class="sharing-indicator">
    <span class="recording-dot"></span>
    正在共享屏幕
  </span>
  <span class="meeting-no-display">会议号: {{ meetingNo }}</span>
  <span class="meeting-time">{{ formattedDuration }}</span>
</div>
```

### CSS 样式

添加了 `.meeting-no-display` 样式：

```css
.meeting-no-display {
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  margin: 0 8px;
}
```

## 显示效果

当用户开始屏幕共享时，顶部控制条从左到右显示：

1. **共享状态指示器**: 红点 + "正在共享屏幕"
2. **会议号**: "会议号: XXXXXXXX" （白色背景，半透明）
3. **会议时长**: "MM:SS" 或 "HH:MM:SS" （绿色背景）

## 样式特点

- **颜色**: 白色文字，半透明白色背景
- **边框**: 1px 半透明白色边框
- **圆角**: 6px 圆角
- **间距**: 左右各 8px 外边距
- **内边距**: 上下 4px，左右 12px

## 数据来源

会议号 `meetingNo` 来自：
- 在 `loadCurrentMeetingInfo()` 函数中从后端 API 获取
- 存储在 `meetingNo` ref 变量中
- 在会议详情浮框中也有显示

## 测试建议

1. 创建或加入会议
2. 点击"共享屏幕"按钮
3. 选择屏幕/窗口开始共享
4. 验证顶部控制条显示：
   - ✅ 红点动画
   - ✅ "正在共享屏幕"文字
   - ✅ 会议号（格式：会议号: XXXXXXXX）
   - ✅ 会议时长
5. 验证会议号显示正确（与会议详情中的会议号一致）

## 视觉效果

```
┌─────────────────────────────────────────────────────────────────┐
│ 🔴 正在共享屏幕  │ 会议号: ABC123XYZ │ ⏱️ 05:23  │ [控制按钮...] │
└─────────────────────────────────────────────────────────────────┘
```

## 相关文件

- `frontend/src/views/Meeting.vue` - 主要修改文件
- `ROLLBACK-TO-ORIGINAL-SCREEN-SHARE.md` - 屏幕共享回滚说明
- `SCREEN-SHARE-FEATURE.md` - 屏幕共享功能说明

## 总结

✅ 在屏幕共享控制条中添加了会议号显示
✅ 样式与整体设计保持一致
✅ 位置合理，不影响其他控制元素
✅ 方便用户在共享屏幕时查看会议号
