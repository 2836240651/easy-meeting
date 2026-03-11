# 多窗口屏幕共享快速启动指南

## 快速测试步骤

### 1. 启动服务（3个终端）

**终端 1 - 后端服务**:
```bash
cd D:\JavaPartical\easymeeting-java
mvn spring-boot:run
```
等待看到: `Started EasymeetingApplication`

**终端 2 - 前端开发服务器**:
```bash
cd D:\JavaPartical\easymeeting-java\frontend
npm run dev
```
等待看到: `Local: http://localhost:3000/`

**终端 3 - Electron 应用**:
```bash
cd D:\JavaPartical\easymeeting-java\frontend
npm run electron:dev
```
等待 Electron 窗口打开

### 2. 登录并创建会议

1. 在 Electron 窗口中登录
2. 点击"快速会议"或创建新会议
3. 进入会议页面

### 3. 开始屏幕共享

1. 点击底部的"共享屏幕"按钮
2. 在弹出的选项对话框中：
   - 可选：勾选"同时共享电脑声音"
   - 可选：勾选"人像画中画"
   - 点击"开始共享"
3. 在屏幕源选择对话框中：
   - 选择"整个屏幕"或"窗口"标签页
   - 选择要共享的屏幕/窗口
   - 点击确认

### 4. 验证结果

应该看到：

✅ **主窗口消失**

✅ **顶部指示条出现**（屏幕最顶部）
- 红点 + "您正在共享屏幕"
- 会议时长
- 鼠标悬停时展开显示控制按钮

✅ **视频窗口出现**（右上角）
- 显示自己的头像（绿色边框）
- 可以拖动
- 可以折叠/展开

✅ **聊天窗口出现**（左下角）
- 显示聊天消息
- 可以发送消息
- 可以拖动
- 可以折叠/展开

✅ **边框窗口出现**（四个角）
- 绿色 L 形边框
- 发光动画

### 5. 测试交互

**测试拖动**:
- 拖动视频窗口的标题栏
- 拖动聊天窗口的标题栏

**测试控制**:
- 鼠标移到屏幕顶部
- 点击控制按钮（静音、视频等）

**测试聊天**:
- 在聊天窗口输入消息
- 点击发送或按 Enter

**测试折叠**:
- 点击窗口标题栏的 "−" 按钮
- 再点击 "+" 按钮展开

### 6. 结束共享

1. 鼠标移到屏幕顶部
2. 点击红色的"结束共享"按钮
3. 验证：
   - 所有悬浮窗口关闭
   - 主窗口重新出现
   - 回到正常会议界面

## 常见问题

### Q1: 悬浮窗口没有出现？

**检查**:
1. 打开 Electron 开发者工具（F12）
2. 查看控制台是否有错误
3. 检查是否有 "🚀 创建屏幕共享悬浮窗口" 日志

**解决**:
- 确保 Electron 应用正在运行
- 重启 Electron 应用

### Q2: 窗口位置不对？

**原因**: 可能是多显示器配置问题

**解决**: 
- 在主显示器上测试
- 检查 `main.js` 中的窗口位置计算

### Q3: 拖动不工作？

**检查**:
1. 确保在标题栏上拖动
2. 查看控制台是否有错误

**解决**:
- 重启 Electron 应用
- 检查 `moveWindow` IPC 处理器

### Q4: 消息不同步？

**检查**:
1. 查看控制台是否有 "📥 收到聊天消息更新" 日志
2. 检查 WebSocket 连接状态

**解决**:
- 确保 WebSocket 服务正常运行
- 检查网络连接

## 调试技巧

### 查看日志

**主窗口日志**:
- 按 F12 打开开发者工具
- 查看 Console 标签页

**Electron 主进程日志**:
- 查看启动 Electron 的终端输出

**关键日志标识**:
- 🚀 = 开始操作
- ✅ = 成功
- ❌ = 失败
- 📥 = 接收数据
- 📤 = 发送数据
- 🎯 = 组件生命周期

### 测试 Electron API

在浏览器控制台（F12）中运行：

```javascript
// 检查 Electron API 是否存在
console.log(window.electron)

// 检查屏幕共享 API
console.log(window.electron.startScreenShareOverlay)
console.log(window.electron.stopScreenShareOverlay)

// 检查窗口移动 API
console.log(window.electron.moveWindow)

// 检查通信 API
console.log(window.electron.updateParticipants)
console.log(window.electron.updateChatMessages)
console.log(window.electron.sendOverlayAction)
```

## 性能监控

### 查看资源使用

1. 打开任务管理器（Ctrl + Shift + Esc）
2. 找到 Electron 进程
3. 观察 CPU 和内存使用

**正常范围**:
- CPU: < 30%
- 内存: < 500MB

### 查看帧率

在浏览器控制台运行：

```javascript
// 显示 FPS
let lastTime = performance.now()
let frames = 0

function measureFPS() {
  frames++
  const now = performance.now()
  if (now >= lastTime + 1000) {
    console.log(`FPS: ${frames}`)
    frames = 0
    lastTime = now
  }
  requestAnimationFrame(measureFPS)
}

measureFPS()
```

## 下一步

测试完成后：

1. **如果一切正常**:
   - 继续测试多用户场景
   - 测试长时间运行稳定性
   - 参考 `MULTI-WINDOW-TEST-GUIDE.md` 进行完整测试

2. **如果有问题**:
   - 记录问题现象
   - 收集日志和截图
   - 参考故障排查指南

3. **性能优化**:
   - 如果 CPU 使用率过高，考虑减少同步频率
   - 如果内存持续增长，检查是否有内存泄漏
   - 如果拖动卡顿，优化拖动算法

## 完整文档

- `MULTI-WINDOW-IMPLEMENTATION-PLAN.md` - 实现计划
- `MULTI-WINDOW-COMPLETE.md` - 完成文档
- `MULTI-WINDOW-TEST-GUIDE.md` - 详细测试指南
- `IMPLEMENTATION-COMPLETE-SUMMARY.md` - 实现总结

## 联系支持

如果遇到问题，请提供：
1. 操作系统版本
2. Electron 版本
3. 错误日志
4. 复现步骤
5. 截图或录屏

祝测试顺利！🎉
