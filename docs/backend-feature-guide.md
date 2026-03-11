# EasyMeeting 后端功能说明

## 1. 技术栈与服务结构

- 核心框架：Spring Boot 2.7。
- 数据访问：MyBatis。
- 持久化：MySQL。
- 缓存与会话：Redis。
- 对象存储：MinIO（头像/媒体资源）。
- 实时层：Netty WebSocket（独立端口）。
- 可选消息分发：RabbitMQ（与 Redis topic 二选一）。

## 2. 服务入口与配置

- HTTP 服务：`http://localhost:6099/api`。
- WebSocket 服务：`ws://localhost:6098/ws?token=...`。
- 统一响应模型：`ResponseVO`。
- 全局异常处理：`AGlobalExceptionHandlerController`。

## 3. 鉴权与权限

- 主要鉴权方式：请求头 `token` + Redis 会话。
- 注解拦截：`@globalInterceptor(checkLogin/checkAdmin)`。
- 管理员接口由 `checkAdmin = true` 控制。
- 另有 session 风格设置接口：`/api/settings/*`。

## 4. 核心业务模块

### 4.1 账号与用户

- `AccountController`：验证码、登录、注册、资料更新、密码更新、系统设置加载。
- `UserInfoController`：获取用户资料、更新用户资料。
- `UserSettingsController`：获取/保存用户设置、修改密码。

### 4.2 会议与预约

- `MeetingInfoController`：
  - 快速会议、入会前校验、加入/退出、结束会议。
  - 当前会议查询、会议历史、成员列表。
  - 主持人管理（踢出、拉黑）。
  - 会议内邀请、邀请接收、视频状态同步。
- `MeetingReserveController`：
  - 预约创建/列表/详情/更新/取消/离开。
  - 即将开始会议查询。
- `MeetingMemberController`、`MeetingReserveMemberController`：通用关系表 CRUD。

### 4.3 聊天与文件

- `ChatController`：消息拉取、消息发送、文件上传、历史记录。
- `FileUploadController`：头像上传（文件、URL）。
- `FileController`：头像、资源下载、Range 分片支持。
- `FileAccessController`：旧版文件访问兼容路径。

### 4.4 联系人、申请、通知

- `UserContactController`：搜索、申请发送、申请处理、好友列表、黑名单管理。
- `UserContactApplyController`：申请记录通用 CRUD。
- `UserNotificationController`：通知列表、未读统计、已读操作、分类与待办处理。

### 4.5 AI、更新、后台管理

- `AIAssistantController`：AI 对话、摘要、建议、连接测试。
- `updateController`：客户端版本检查、安装包下载。
- `AppUpdateController`：更新记录维护与发布。
- `AdminController`：用户管理、状态更新、强制下线。
- `AdminSettingController`：系统设置和会议管理。

## 5. 实时通信架构

- Netty 握手阶段校验 token。
- 维护 `userId -> channel` 与 `meetingId -> channelGroup` 映射。
- 支持消息投递到用户、会议房间、广播主题。
- 消息类型覆盖：
  - 会议生命周期事件。
  - 聊天文本/媒体消息。
  - 联系人与通知变更。
  - WebRTC 与屏幕共享信令（offer/answer/ice/start/stop）。

## 6. 定时任务与系统行为

- 会议超时任务：自动关闭超时会议。
- 预约提醒任务：会前通知推送。
- 会议成员状态在数据库与缓存同步维护。

## 7. 数据与中间件职责

- MySQL：用户、会议、预约、联系人、通知等主数据。
- Redis：
  - token 会话。
  - 验证码。
  - 会议成员状态缓存。
  - 邀请与系统配置缓存。
- MinIO：头像等静态资源对象存储与访问。

## 8. 当前实现注意点

- 存在 token 鉴权与 session 鉴权并行情况。
- 部分 Controller 含通用 CRUD 接口，主要用于后台或通用管理场景。
- 应用更新链路结构完整，但部分实现仍有待补齐（如最新版本查询实现）。
