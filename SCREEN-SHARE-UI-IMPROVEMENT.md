# 屏幕共享 UI 改进实现计划

## 需求概述

### 1. 屏幕共享选项对话框（点击共享屏幕按钮时显示）
- ✅ 同时共享电脑声音
- ✅ 人像画中画
- ✅ 按钮：共享

### 2. 屏幕共享工具栏（屏幕共享时显示，顶部自动隐藏）

**左侧区域：**
- ✅ 会议详情
- ✅ 会议时长（实时更新）
- ✅ 分享会议

**中间区域：**
- ✅ 静音/取消静音
- ✅ 开启/关闭视频
- ✅ 成员
- ✅ 聊天
- ⚠️ 录制（占位，提示暂未实现）
- ✅ 暂停共享
- ✅ 结束共享

## 实现进度

### ✅ 已完成
1. ✅ 添加响应式数据：
   - `showScreenShareOptions` - 显示选项对话框
   - `screenShareOptions` - 选项配置（shareAudio, showPip）
   - `showScreenShareToolbar` - 显示工具栏
   - `isScreenSharePaused` - 暂停状态
   - `isRecording` - 录制状态
   - `toolbarHideTimer` - 工具栏隐藏计时器
   - `meetingDuration` - 会议时长
   - `meetingStartTime` - 会议开始时间
   - `durationTimer` - 计时器

2. ✅ 添加计算属性：
   - `formattedDuration` - 格式化的会议时长（HH:MM:SS 或 MM:SS）

3. ✅ 创建屏幕共享选项对话框组件
   - 模态框 UI
   - 复选框：共享电脑声音、人像画中画
   - 按钮：取消、开始共享

4. ✅ 创建屏幕共享工具栏组件
   - 左侧：会议详情、会议时长、分享会议
   - 中间：静音、视频、成员、聊天、录制、暂停、结束
   - 自动隐藏/显示逻辑

5. ✅ 实现会议时长计时功能
   - 在 `onMounted` 时启动
   - 每秒更新
   - 在 `onUnmounted` 时清理

6. ✅ 实现暂停/恢复共享功能
   - `toggleScreenSharePause()` 函数
   - 通过禁用/启用视频轨道实现

7. ✅ 实现分享会议功能
   - `shareMeeting()` 函数
   - 使用 Electron 剪贴板 API
   - 复制会议信息

8. ✅ 实现工具栏自动隐藏
   - 鼠标移到顶部显示
   - 鼠标离开 3 秒后隐藏
   - CSS transform 动画

9. ✅ 添加 CSS 样式
   - 选项对话框样式
   - 工具栏样式
   - 按钮和图标样式

### ⚠️ 部分实现
- 录制功能（占位，提示暂未实现，需要后端支持）

### 📋 待实现
- 录制功能完整实现（需要后端 API 和 MediaRecorder 集成）
- 人像画中画选项与现有 PIP 逻辑集成
- 响应式布局优化

## 技术细节

### 会议时长计时
- ✅ 在 `onMounted` 时开始计时
- ✅ 每秒更新 `meetingDuration`
- ✅ 在 `onUnmounted` 时清除计时器
- ✅ 格式化显示（HH:MM:SS 或 MM:SS）

### 屏幕共享选项
- ✅ 共享电脑声音：修改 `getDisplayMedia` 的 audio 参数
- ⚠️ 人像画中画：控制是否显示共享者的摄像头小窗（需要与现有逻辑集成）

### 工具栏自动隐藏
- ✅ 监听鼠标移动事件（mouseenter/mouseleave）
- ✅ 鼠标移到顶部时显示工具栏
- ✅ 鼠标离开后 3 秒自动隐藏
- ✅ 使用 CSS transform 实现流畅动画

## 文件修改列表

### frontend/src/views/Meeting.vue
- ✅ 添加响应式数据
- ✅ 添加计算属性
- ✅ 添加屏幕共享选项对话框 UI
- ✅ 添加屏幕共享工具栏 UI
- ✅ 修改 `shareScreen` 函数逻辑
- ✅ 修改 `startScreenShare` 函数
- ✅ 修改 `stopScreenShare` 函数
- ✅ 添加 `toggleScreenSharePause` 函数
- ✅ 添加 `shareMeeting` 函数
- ✅ 添加 `toggleRecording` 函数
- ✅ 添加 `hideToolbarWithDelay` 函数
- ✅ 添加 `startMeetingDurationTimer` 函数
- ✅ 添加 `stopMeetingDurationTimer` 函数
- ✅ 更新 `onMounted` 钩子
- ✅ 更新 `onUnmounted` 钩子
- ✅ 添加 CSS 样式

## 实现状态

✅ **核心功能已全部完成！**

用户现在可以：
1. 在共享屏幕前选择是否共享音频
2. 使用自动隐藏的工具栏控制会议
3. 查看实时会议时长
4. 快速分享会议信息
5. 暂停/恢复屏幕共享
6. 通过工具栏访问所有常用功能

## 下一步（可选）

1. 实现完整的录制功能（需要后端支持）
2. 集成人像画中画选项与现有 PIP 逻辑
3. 优化响应式布局
4. 添加更多自定义选项（如分辨率、帧率等）

