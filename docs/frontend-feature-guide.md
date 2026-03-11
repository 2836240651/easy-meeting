# EasyMeeting 前端功能说明

## 1. 技术栈与运行形态

- Web 前端：Vue 3 + Vue Router + Element Plus + Axios。
- 桌面容器：Electron（主进程 + preload + 多窗口悬浮层）。
- 实时能力：WebSocket + WebRTC（会议内音视频与屏幕共享信令）。

## 2. 页面与路由

| 路由 | 页面 | 说明 |
|---|---|---|
| `/login` | `Login.vue` | 登录、验证码获取、token 持久化 |
| `/register` | `Register.vue` | 注册、验证码校验 |
| `/dashboard` | `Dashboard.vue` | 会议大厅、联系人、收件箱、设置中心 |
| `/meeting/:meetingId?` | `Meeting.vue` | 会议房间主页面 |
| `/ai-test` | `AITest.vue` | AI 助手调试入口 |
| `/screen-share-topbar` | `ScreenShareTopBar.vue` | 共享时顶部悬浮控制条 |
| `/screen-share-video` | `ScreenShareVideo.vue` | 共享时视频悬浮窗口 |
| `/screen-share-chat` | `ScreenShareChat.vue` | 共享时聊天悬浮窗口 |
| `/border` | `ScreenShareBorder.vue` | 共享边框标记层 |

## 3. 核心功能模块

### 3.1 账号与身份

- 图形验证码登录/注册。
- 用户信息保存与恢复（`token`、`userInfo`）。
- 个人资料编辑（昵称、性别、头像）。
- 密码修改（账号接口与设置接口均有支持）。

### 3.2 会议大厅（Dashboard）

- 快速会议创建。
- 会议号/会议 ID 入会。
- 预约会议创建、修改、取消、离开。
- 当前会议状态卡片（继续加入/离开/结束）。
- 历史会议列表与状态筛选。

### 3.3 会议室（Meeting）

- 入会前校验 + 正式入会流程。
- 麦克风/摄像头开关，视频状态同步。
- 成员列表、主持人踢人/拉黑。
- 会议文本聊天、媒体消息上传。
- 会议邀请（会议内邀请联系人或用户）。
- 主持人结束会议、成员退出会议。

### 3.4 联系人与通知

- 联系人搜索（`userId/email`）。
- 好友申请发送与处理（同意/拒绝）。
- 好友列表、黑名单、取消拉黑。
- 统一通知中心：分类、未读、已读、待处理动作。
- 会议邀请通知处理（接受/拒绝）。

### 3.5 设置与提醒

- 本地设置管理（会议/音频/视频/网络/通知/隐私/主题）。
- 后端用户设置接口（`/api/settings/*`）已接入。
- 预约会议提醒组件按分钟轮询并触发桌面通知。

### 3.6 实时通信

- Dashboard 维持全局 WebSocket 连接。
- 自动重连、心跳与连接状态显示。
- 实时接收联系人状态、通知、会议内信令消息。

### 3.7 屏幕共享与 Electron 多窗口

- Electron 主进程提供桌面源选择能力。
- preload 层桥接 `getDisplayMedia`。
- 共享时创建顶部栏/视频/聊天/边框多窗口。
- 跨窗口 IPC 同步共享状态、成员信息、聊天状态。

## 4. 前端 API 服务分层

- `authService`：登录注册、验证码、账号资料、密码。
- `userService`：用户信息获取与更新。
- `meetingService`：会议生命周期、成员管理、邀请、视频状态。
- `meetingReserveService`：预约会议全流程。
- `chatService`：消息发送、历史加载、文件上传。
- `contactService`：联系人与申请流。
- `notificationService`：通知中心与待办动作。
- `settingsService`：用户设置与密码修改。
- `systemService`：系统配置读取。

## 5. 前端目录建议关注点

- `frontend/src/views/`：页面级功能实现。
- `frontend/src/components/`：会议、预约、邀请、设置等复用组件。
- `frontend/src/api/`：HTTP 服务封装与 WebSocket 客户端。
- `frontend/electron/`：桌面端主进程、preload、窗口控制。

## 6. 已知实现特征

- 业务主路径以 Dashboard + Meeting 双核心页面组织。
- 会议能力同时依赖 HTTP（状态变更）与 WebSocket/WebRTC（实时信令）。
- 设置能力存在“本地管理 + 后端接口”并存模式。
