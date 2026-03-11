# 多用户测试环境启动完成

## 启动时间
2026-02-26 21:16

## 已启动的服务

### 1. 后端服务 ✅
- **状态**: 运行中
- **进程 ID**: Terminal 4
- **端口**: 6099
- **WebSocket 端口**: 6098
- **数据库**: 已连接
- **定时任务**: 正常运行

### 2. 前端 Vite 开发服务器 ✅
- **状态**: 运行中
- **进程 ID**: Terminal 2
- **端口**: 3000
- **启动时间**: 571ms
- **访问地址**: http://localhost:3000/

### 3. Electron 应用实例 ✅
- **实例 1**: 进程 ID 26760（用户A）
- **实例 2**: 进程 ID 30132（用户B）
- **状态**: 两个窗口都已启动

## 循环依赖问题修复 ✅

### 问题
后端启动时出现循环依赖错误：
```
channelContextUtils → userContactService → userNotificationService → channelContextUtils
```

### 解决方案
在 `UserNotificationServiceImpl.java` 中使用 `@Lazy` 注解：

```java
@Resource
@Lazy
private ChannelContextUtils channelContextUtils;
```

并添加导入：
```java
import org.springframework.context.annotation.Lazy;
```

### 结果
- 编译成功
- 后端服务正常启动
- 循环依赖已解决

## 测试统一收件箱系统

### 测试场景：好友申请流程

#### 步骤 1: 用户A添加用户B
1. 在 Electron 窗口 1（进程 26760）登录为用户A
2. 进入"联系人"页面
3. 点击"添加联系人"
4. 搜索用户B的邮箱或用户ID
5. 点击"添加"按钮

#### 步骤 2: 用户B查看收件箱
1. 在 Electron 窗口 2（进程 30132）登录为用户B
2. 点击左侧导航栏的"收件箱"图标
3. 应该看到新的收件箱界面：
   - 两个标签页：全部消息 / 待办消息
   - 类别筛选器（全部/联系人/会议/系统）
4. 切换到"待办消息"标签页
5. 应该看到用户A的好友申请通知

#### 步骤 3: 用户B处理申请
1. 在待办消息中找到用户A的申请
2. 点击"同意"或"拒绝"按钮
3. 验证操作成功提示
4. 切换到"全部消息"标签页
5. 验证申请状态已更新

#### 步骤 4: 用户A查看响应通知
1. 回到 Electron 窗口 1（用户A）
2. 进入"收件箱"页面
3. 应该看到用户B的响应通知：
   - 如果同意：显示"好友申请已同意"
   - 如果拒绝：显示"好友申请已拒绝"

### 测试功能点

#### 1. 收件箱 UI
- [ ] 两个标签页正确显示
- [ ] 类别筛选器正常工作
- [ ] 未读数量徽章显示正确
- [ ] 待办数量徽章显示正确

#### 2. 通知列表
- [ ] 通知正确显示（标题、内容、时间）
- [ ] 未读通知有视觉区分（背景色 + 蓝色圆点）
- [ ] 类别封面标题在类别变化时显示
- [ ] 相对时间显示正确（刚刚、X分钟前等）

#### 3. 操作功能
- [ ] 点击通知标记为已读
- [ ] 未读数量正确更新
- [ ] 同意/拒绝按钮正常工作
- [ ] 操作后状态正确更新

#### 4. 类别筛选
- [ ] 切换到"联系人消息"只显示类型 1-4
- [ ] 切换到"会议消息"只显示类型 5-9
- [ ] 切换到"系统消息"只显示类型 10-11
- [ ] 切换到"全部消息"显示所有通知

#### 5. 待办消息
- [ ] 只显示需要操作的通知（类型 1 和 5）
- [ ] 按类型分组显示
- [ ] 分组标题正确显示
- [ ] 操作按钮正常工作

## 浏览器开发者工具检查

### 打开开发者工具
在 Electron 窗口中按 `F12` 或 `Ctrl+Shift+I`

### 检查项目
1. **Console 标签页**：
   - 检查是否有 JavaScript 错误
   - 检查 API 请求日志

2. **Network 标签页**：
   - 检查 API 请求是否成功（200 状态码）
   - 检查请求参数和响应数据
   - 检查 WebSocket 连接状态

3. **Application 标签页**：
   - 检查 localStorage 中的 token 和 userInfo
   - 检查 WebSocket 连接

## API 端点测试

### 使用浏览器或 Postman 测试

#### 1. 获取通知列表（按类别）
```
GET http://localhost:6099/api/notification/loadNotificationsByCategory?category=all&pageNo=1&pageSize=15
Headers: token: <your_token>
```

#### 2. 获取待办消息
```
GET http://localhost:6099/api/notification/loadPendingActions
Headers: token: <your_token>
```

#### 3. 获取未读数量
```
GET http://localhost:6099/api/notification/getUnreadCount
Headers: token: <your_token>
```

#### 4. 标记为已读
```
POST http://localhost:6099/api/notification/markAsRead?notificationId=<id>
Headers: token: <your_token>
```

## 故障排查

### 如果收件箱显示空白
1. 打开浏览器控制台检查错误
2. 检查 Network 标签页的 API 请求
3. 刷新页面（Ctrl+R）
4. 检查后端日志

### 如果通知不显示
1. 检查数据库中是否有通知数据：
   ```sql
   SELECT * FROM user_notification ORDER BY create_time DESC LIMIT 10;
   ```
2. 检查 API 响应是否包含数据
3. 检查类别筛选器是否选择正确

### 如果操作按钮无效
1. 检查浏览器控制台错误
2. 检查 Network 标签页的 API 请求
3. 检查后端日志是否有错误

### 如果 WebSocket 连接失败
1. 检查后端 WebSocket 服务是否启动（端口 6098）
2. 检查浏览器控制台的 WebSocket 连接日志
3. 检查防火墙设置

## 查看日志

### 后端日志
```bash
# 使用 getProcessOutput 工具
terminalId: 4
lines: 50
```

### 前端 Vite 日志
```bash
# 使用 getProcessOutput 工具
terminalId: 2
lines: 30
```

### Electron 日志
```bash
# 使用 getProcessOutput 工具
terminalId: 5
lines: 30
```

## 停止服务

### 停止所有后台进程
```bash
# 使用 controlPwshProcess stop
terminalId: 2  # Vite
terminalId: 4  # 后端
terminalId: 5  # Electron
```

### 停止 Electron 窗口
直接关闭窗口或使用任务管理器结束进程

## 测试账号建议

### 账号A（用户1）
- 邮箱: user1@test.com
- 密码: 123456
- 用途: 发起好友申请

### 账号B（用户2）
- 邮箱: user2@test.com
- 密码: 123456
- 用途: 接收并处理好友申请

如果没有这些账号，请先注册。

## 下一步

1. ✅ 所有服务已启动
2. 🔄 **开始测试好友申请流程**
3. 🔄 测试类别筛选功能
4. 🔄 测试未读标记功能
5. 🔄 测试待办消息功能
6. 📝 记录测试结果和问题

## 相关文档

- `SERVICE-RESTART-UNIFIED-INBOX.md` - 服务重启文档
- `NOTIFICATION-FRONTEND-INTEGRATION.md` - 前端集成完成总结
- `UNIFIED-INBOX-IMPLEMENTATION-PROGRESS.md` - 实现进度文档
- `UNIFIED-INBOX-FRONTEND-GUIDE.md` - 前端集成详细指南

## 总结

多用户测试环境已成功搭建：
- 后端服务运行正常
- 前端 Vite 服务器运行正常
- 两个 Electron 应用实例已启动
- 循环依赖问题已修复

现在可以开始测试统一收件箱系统的完整功能。建议先测试基础 UI，然后测试完整的好友申请流程。
