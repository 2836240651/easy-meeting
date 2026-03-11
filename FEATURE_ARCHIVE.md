# EasyMeeting 前后端功能归档文档

## 1. 项目概览

- 前端：Vue 3 + Vue Router + Element Plus + Axios + Electron。
- 后端：Spring Boot 2.7 + MyBatis + Redis + Netty WebSocket + MinIO + MySQL。
- HTTP 基础信息：
  - 后端服务地址：`http://localhost:6099`
  - 后端上下文路径：`/api`
  - 前端 Vite 代理：`/api -> http://localhost:6099`
- WebSocket：
  - Netty 服务地址：`ws://localhost:6098/ws?token=...`

## 2. 前端功能归档

### 2.1 路由/页面归档

| 路由 | 页面 | 主要能力 |
|---|---|---|
| `/login` | Login.vue | 图形验证码登录、token/userInfo 持久化 |
| `/register` | Register.vue | 图形验证码注册 |
| `/dashboard` | Dashboard.vue | 会议大厅、联系人、消息通知、个人资料、设置 |
| `/meeting/:meetingId?` | Meeting.vue | 会议内音视频/聊天/成员管理/屏幕共享 |
| `/ai-test` | AITest.vue | AI 助手调试页面 |
| `/screen-share-topbar` | ScreenShareTopBar.vue | 屏幕共享悬浮顶部控制条 |
| `/screen-share-video` | ScreenShareVideo.vue | 屏幕共享悬浮视频面板 |
| `/screen-share-chat` | ScreenShareChat.vue | 屏幕共享悬浮聊天面板 |
| `/border` | ScreenShareBorder.vue | 屏幕共享边框标记层 |

### 2.2 Dashboard 模块归档

- 会议主页：
  - 加入会议、快速会议、预约会议。
  - 当前会议卡片（重新加入/结束/离开）。
  - 历史会议与进行中会议弹窗。
  - 预约列表面板（`ReservationList`）。
- 个人资料：
  - 查看个人信息与会议号。
  - 修改昵称/性别/头像。
  - 支持文件上传头像或 URL 更新头像。
- 联系人：
  - 按 `userId/email` 搜索用户。
  - 发送申请、处理好友申请（同意/拒绝）。
  - 好友列表及在线状态展示。
  - 删除好友、拉黑、取消拉黑。
- 消息收件箱：
  - 全部通知/待处理通知。
  - 分类筛选：全部/联系人/会议/系统。
  - 处理好友申请和会议邀请。
  - 支持会议邀请的快速入会。
- 设置：
  - `SettingsPanel` 提供会议、音频、视频、网络、通知、隐私、主题等配置。
- 提醒：
  - `MeetingReminder` 每分钟检查一次即将开始的预约并触发桌面通知。
- 实时通信：
  - Dashboard 维持全局 WebSocket 连接。
  - 处理联系人申请、在线状态变化、联系人删除等推送。
  - 自动重连与连接状态提示。

### 2.3 会议室模块归档

- 会议生命周期：
  - 入会、入会前校验、离会、主持人结束会议。
- 媒体控制：
  - 麦克风开关、摄像头开关、全屏、会议信息展示。
- 成员管理：
  - 成员网格与成员列表弹窗。
  - 主持人能力：踢出、拉黑。
- 聊天：
  - 文本/媒体消息、未读计数、历史消息与时间线筛选。
- 邀请：
  - 会议内联系人邀请（`InviteMemberModal`）。
- WebRTC：
  - P2P 的 offer/answer/ice 信令交换。
  - 连接诊断与恢复能力。
- 屏幕共享：
  - 开始/停止共享。
  - 屏幕共享 WebRTC 信令。
  - 共享者与观看者布局（画中画、可拖拽面板）。
  - 与 Electron 悬浮窗联动。

### 2.4 Electron 能力归档

- 多窗口架构：
  - 主窗口 + 顶部控制窗口 + 视频窗口 + 聊天窗口 + 边框窗口。
- 桌面采集：
  - `ipcMain.handle('get-desktop-sources')`。
  - Preload 层补丁 `getDisplayMedia`，支持 Electron 源选择器。
- 悬浮层控制：
  - 启动/停止屏幕共享悬浮层。
  - 悬浮窗口移动控制。
  - 跨窗口 IPC 同步成员、聊天、操作状态。

### 2.5 前端 API 服务归档

- `authService`：验证码、登录、注册、账号信息更新、密码修改。
- `userService`：获取/更新用户资料。
- `meetingService`：快速会议、入会前校验、加入/退出、当前会议、结束会议、成员列表、踢出/拉黑、视频状态同步、邀请成员。
- `meetingReserveService`：创建/查询/更新/取消/离开预约，预约详情、即将开始会议、预约转入会。
- `chatService`：发送消息、加载消息、加载历史、上传媒体。
- `contactService`：搜索、申请、处理申请、列表、删除、黑名单。
- `notificationService`：通知列表、未读数、已读、分类、待处理、会议邀请处理。
- `settingsService`：获取/保存设置、修改密码（后端设置接口）。
- `systemService`：加载系统设置。

## 3. 后端功能归档

### 3.1 基础设施

- 全局响应封装：`ResponseVO`。
- 全局异常处理：`AGlobalExceptionHandlerController`。
- 登录/管理员拦截注解 + AOP：
  - `@globalInterceptor(checkLogin/checkAdmin)`。
  - token 来源：HTTP 请求头 `token`（Redis 会话）。
- 定时任务：
  - 会议超时自动结束。
  - 预约会议提醒通知。

### 3.2 Controller 模块归档

#### 3.2.1 账号/用户

- `AccountController`（`/account`）：
  - 验证码（`/checkCode`、`/captcha`）、登录、注册、登出、系统设置加载、资料更新、密码修改。
- `UserInfoController`（`/userInfo`）：
  - 获取用户信息、更新用户资料字段。
- `UserSettingsController`（`/api/settings`）：
  - 获取/保存设置、修改密码（基于 session 风格的 `userId`）。

#### 3.2.2 会议

- `MeetingInfoController`（`/meetingInfo`）：
  - 会议历史/列表、快速会议、入会前校验、加入、退出、结束。
  - 获取当前会议、加载成员。
  - 主持人管理：踢出/拉黑。
  - 邀请联系人、接受邀请、会议内邀请成员。
  - 视频状态同步。
- `MeetingReserveController`（`/meetingReserve`）：
  - 创建/查询/更新/取消/离开预约。
  - 预约详情、即将开始会议。
- `MeetingMemberController`、`MeetingReserveMemberController`：
  - 通用 CRUD 接口。

#### 3.2.3 聊天/文件

- `ChatController`（`/chat`）：
  - 消息加载、消息发送、媒体文件上传、历史记录加载。
- `FileUploadController`（`/upload`）：
  - 头像上传（文件/URL）、MinIO 存储。
- `FileController`（`/file`）：
  - 头像/资源/下载访问（含媒体 Range 分片支持）。
- `FileAccessController`（`/files`）：
  - 旧版本地头像文件访问。

#### 3.2.4 联系人/通知

- `UserContactController`（`/userContact`）：
  - 联系人搜索、申请发送与处理、好友/黑名单/申请列表加载。
  - 删除好友、拉黑、取消拉黑。
- `UserContactApplyController`（`/userContactApply`）：
  - 通用 CRUD 接口。
- `UserNotificationController`（`/notification`）：
  - 通知列表、未读、已读、分类筛选、待处理事项。
  - 会议邀请处理、操作状态更新。

#### 3.2.5 AI/更新/管理后台

- `AIAssistantController`（`/ai`）：
  - 对话、摘要、建议、AI 连接测试。
- `updateController`（`/update`）：
  - 客户端版本检查、安装包下载。
- `AdminController`（`/admin`）：
  - 用户列表查询、用户状态更新、强制下线。
- `AdminSettingController`（`/admin`）：
  - 系统设置保存/读取、管理员会议管理。
- `AppUpdateController`（`/admin`）：
  - 更新记录列表、保存、删除、发布。

### 3.3 核心业务规则归档

- 会议：
  - 支持快速会议与预约转会议。
  - 仅主持人可结束会议并执行成员管理操作。
  - 成员状态在 DB + Redis 双通道维护（正常/离开/踢出/拉黑）。
- 预约：
  - 对会议名称、时间、时长、密码进行参数校验。
  - 仅创建者可修改/取消预约。
  - 被邀请成员持久化到预约-成员关系表。
- 联系人：
  - 支持按 `userId` 或 `email` 搜索。
  - 前端阻止自加好友，后端执行关系状态校验。
  - 删除好友与拉黑流程会触发状态更新与通知。
- 通知：
  - 包含 13 种通知类型，覆盖联系人/会议/系统分类。
  - 支持待处理动作流转（同意/拒绝）。
- AI：
  - 支持普通问答与斜杠命令（`/summary`、`/suggest`、`/help`、`/end`）。
  - 模型提供方抽象支持 OpenAI 或 Ollama。

### 3.4 实时通信/WebSocket 归档

- Netty WebSocket 独立端口启动（`ws.port`）。
- 握手阶段通过 query 参数进行 token 校验。
- 管理用户 channel 映射与会议房间 channel group。
- 消息分发可按配置走 Redis Topic 或 RabbitMQ Fanout。
- 主要消息类型包括：
  - 会议生命周期、聊天文本/媒体更新、联系人事件；
  - 强制下线、视频状态变化；
  - WebRTC offer/answer/ice；
  - 屏幕共享 start/stop/offer/answer/ice；
  - 用户在线状态变化、系统通知。

### 3.5 存储与中间件归档

- MySQL：核心业务持久化（用户、会议、预约、联系人、通知等）。
- Redis：
  - token 会话、验证码、会议成员缓存、邀请标记、系统配置。
- MinIO：
  - 头像对象存储与公网 URL 访问。
- 可选 RabbitMQ：
  - 作为 Redis Topic 之外的消息分发通道。

## 4. 前后端功能映射

| 功能 | 前端入口 | 后端 API / Service |
|---|---|---|
| 登录/注册/验证码 | Login.vue/Register.vue | `/account/checkCode`, `/account/login`, `/account/register` |
| 资料更新 | Dashboard 资料弹窗 | `/userInfo/updateUserInfo`, `/upload/avatar`, `/upload/avatarByUrl` |
| 快速会议 | Dashboard + QuickMeetingModal | `/meetingInfo/quickMeeting`, `/meetingInfo/joinMeeting` |
| 会议号/ID 入会 | JoinMeetingModal + Meeting.vue | `/meetingInfo/preJoinMeeting`, `/meetingInfo/joinMeeting` |
| 预约会议流程 | ScheduleMeetingModal/ReservationList | `/meetingReserve/*`, `/meetingInfo/joinMeetingReserve` |
| 会议内生命周期 | Meeting.vue | `/meetingInfo/getCurrentMeeting`, `/meetingInfo/exitMeeting`, `/meetingInfo/finishMeeting` |
| 成员管理 | Meeting.vue 成员面板 | `/meetingInfo/kickOutMeeting`, `/meetingInfo/blackMeeting` |
| 聊天 | Meeting.vue 聊天区 | `/chat/sendMessage`, `/chat/loadMeesage`, `/chat/uploadFile`, `/chat/loadHistroy` |
| 联系人体系 | Dashboard 联系人区域 | `/userContact/*` |
| 收件箱/通知 | Dashboard 收件箱 | `/notification/*` |
| AI 助手 | AITest.vue / 会话中的 AI 流程 | `/ai/chat`, `/ai/summary`, `/ai/suggest` |
| 实时同步 | Dashboard/Meeting WebSocket 服务 | `ws://localhost:6098/ws?token=...` + 消息处理器 |
| 屏幕共享悬浮层 | Meeting.vue + Electron 窗口体系 | Electron IPC + 悬浮路由页面 |

## 5. 当前状态说明（基于代码观察）

- 目前同时存在基于 token 的接口鉴权与基于 session 的设置接口（`/api/settings`），属于两套鉴权风格并存。
- 前端当前主要使用本地 `settings-manager`；后端用户设置接口已存在，但不是 UI 主路径。
- 前端服务层存在少量历史遗留或未实际调用的 service/API 定义。
- 客户端更新链路结构完整，但 `AppUpdateServiceImpl.selectLatestUpdate` 当前返回 `null`（实现未完成）。

## 6. 建议的后续文档拆分

- 如需进一步规范，可拆分为：
  - `docs/frontend-feature-archive.md`
  - `docs/backend-feature-archive.md`
  - `docs/api-catalog.md`
  - `docs/realtime-message-protocol.md`
