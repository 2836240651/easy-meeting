# EasyMeeting API 目录

## 1. 调用约定

- 服务地址默认 `http://localhost:6099`，前端请求前缀由 Axios 统一代理为 `/api`。
- WebSocket 地址：`ws://localhost:6098/ws?token=...`。
- 返回体：统一为 `ResponseVO`（少量文件下载接口直接写入 `response`）。
- 鉴权：
  - `token` 鉴权：通过请求头 `token`（`@globalInterceptor`）。
  - session 鉴权：`/api/settings/*` 通过 `HttpSession` 中 `userId`。

## 2. 账号与用户接口

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/account/checkCode` | 否 | 获取验证码（别名：`/account/captcha`） |
| POST | `/account/login` | 否 | 登录 |
| POST | `/account/register` | 否 | 注册 |
| POST | `/account/logout` | 是 | 登出（当前实现占位） |
| GET | `/account/loadSystemSetting` | 否 | 获取系统设置 |
| POST | `/account/updateUserInfo` | 是 | 更新基础资料 |
| POST | `/account/updatePassword` | 是 | 修改密码 |
| GET | `/userInfo/getUserInfo` | 是 | 获取当前用户资料 |
| POST | `/userInfo/updateUserInfo` | 是 | 更新用户资料（昵称/性别/头像） |
| GET | `/api/settings/get` | session | 获取用户设置 |
| POST | `/api/settings/save` | session | 保存用户设置 |
| POST | `/api/settings/changePassword` | session | 修改密码 |

## 3. 会议与预约接口

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/meetingInfo/loadMeeting` | 是 | 历史/进行中会议列表 |
| POST | `/meetingInfo/quickMeeting` | 是 | 快速会议 |
| GET | `/meetingInfo/preJoinMeeting` | 是 | 入会前校验 |
| POST | `/meetingInfo/joinMeeting` | 是 | 正式入会 |
| POST | `/meetingInfo/exitMeeting` | 是 | 退出会议 |
| POST | `/meetingInfo/finishMeeting` | 是 | 结束会议（主持人） |
| GET | `/meetingInfo/getCurrentMeeting` | 是 | 获取当前会议 |
| GET | `/meetingInfo/loadMeetingMembers` | 是 | 获取会议成员 |
| POST | `/meetingInfo/kickOutMeeting` | 是 | 踢出成员 |
| POST | `/meetingInfo/blackMeeting` | 是 | 拉黑成员 |
| POST | `/meetingInfo/joinMeetingReserve` | 是 | 预约转入会 |
| POST | `/meetingInfo/inviteContact` | 是 | 邀请联系人入会 |
| POST | `/meetingInfo/inviteUserToMeeting` | 是 | 会议内邀请指定用户 |
| POST | `/meetingInfo/acceptInvite` | 是 | 接受会议邀请 |
| POST | `/meetingInfo/sendVideoChange` | 是 | 广播视频状态变化 |
| GET | `/meetingInfo/getMeetingInfoByMeetingId` | 否 | 按 meetingId 获取会议信息 |
| POST | `/meetingInfo/delMeetingRecord` | 是 | 删除个人会议记录 |
| POST | `/meetingReserve/createMeetingReserve` | 是 | 创建预约 |
| GET | `/meetingReserve/loadMeetingReserveList` | 是 | 预约列表 |
| POST | `/meetingReserve/updateMeetingReserve` | 是 | 更新预约 |
| POST | `/meetingReserve/cancelMeetingReserve` | 是 | 取消预约（创建者） |
| POST | `/meetingReserve/leaveMeetingReserve` | 是 | 离开预约（被邀请者） |
| GET | `/meetingReserve/getMeetingReserveDetail` | 是 | 预约详情 |
| GET | `/meetingReserve/getUpcomingMeetings` | 是 | 即将开始的预约 |

## 4. 聊天与文件接口

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/chat/loadMeesage` | 是 | 加载会议消息 |
| POST | `/chat/sendMessage` | 是 | 发送消息 |
| POST | `/chat/uploadFile` | 是 | 上传聊天文件 |
| GET | `/chat/loadHistroy` | 是 | 加载历史消息 |
| POST | `/upload/avatar` | 是 | 上传头像文件 |
| POST | `/upload/avatarByUrl` | 是 | 通过 URL 上传头像 |
| GET | `/file/getAvatar` | token 参数 | 获取头像（旧接口） |
| GET | `/file/getResource` | token 参数 | 获取资源（支持 range） |
| GET | `/file/downloadFile` | token 参数 | 下载聊天文件 |
| GET | `/files/avatar/{filename}` | 否 | 本地头像访问兼容接口 |

## 5. 联系人与通知接口

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/userContact/searchContact` | 是 | 搜索联系人 |
| POST | `/userContact/contactApply` | 是 | 发送好友申请 |
| POST | `/userContact/dealWithApply` | 是 | 处理好友申请 |
| GET | `/userContact/loadContactUser` | 是 | 好友列表 |
| GET | `/userContact/loadContactApply` | 是 | 待处理申请 |
| GET | `/userContact/loadAllContactApply` | 是 | 全部申请 |
| GET | `/userContact/loadMyApply` | 是 | 我发起的申请 |
| GET | `/userContact/loadContactApplyDealWithCount` | 是 | 待处理申请计数 |
| POST | `/userContact/delContact` | 是 | 删除好友/拉黑 |
| GET | `/userContact/loadBlackList` | 是 | 黑名单 |
| POST | `/userContact/unblackContact` | 是 | 取消拉黑 |
| GET | `/notification/loadNotificationList` | 是 | 通知列表 |
| GET | `/notification/getUnreadCount` | 是 | 未读数量 |
| POST | `/notification/markAsRead` | 是 | 标记单条已读 |
| POST | `/notification/markAllAsRead` | 是 | 全部已读 |
| GET | `/notification/loadNotificationsByCategory` | 是 | 分类通知 |
| GET | `/notification/loadPendingActions` | 是 | 待处理通知 |
| POST | `/notification/handleMeetingInvite` | 是 | 处理会议邀请 |
| POST | `/notification/updateActionStatus` | 是 | 更新通知动作状态 |

## 6. AI、更新与管理接口

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/ai/chat` | token | AI 对话 |
| POST | `/ai/summary` | token | 会议摘要 |
| POST | `/ai/suggest` | token | 会议建议 |
| GET | `/ai/test` | 否 | AI 连通性测试 |
| GET | `/update/checkVersion` | 否 | 客户端版本检查 |
| GET | `/update/downloadApp` | 否 | 下载客户端安装包 |
| GET | `/admin/loadUserList` | 建议管理员 | 用户列表 |
| POST | `/admin/updateUserStatus` | 管理员 | 更新用户状态 |
| POST | `/admin/forceOffLine` | 管理员 | 强制用户下线 |
| POST | `/admin/saveSysSetting` | 管理员 | 保存系统设置 |
| GET | `/admin/getSysSetting` | 登录 | 获取系统设置 |
| GET | `/admin/loadMeeting` | 管理员 | 管理端会议列表 |
| POST | `/admin/updateMeetingStatus` | 管理员 | 管理端更新会议状态 |
| GET | `/admin/loadUpdateList` | 登录 | 更新包列表 |
| POST | `/admin/saveUpdate` | 管理员 | 新增/更新更新包 |
| POST | `/admin/delUpdate` | 管理员 | 删除更新包 |
| POST | `/admin/postUpdate` | 管理员 | 发布更新包 |

## 7. 通用 CRUD 接口（管理/内部）

以下接口由代码生成风格保留，主要用于通用管理或内部调试：

- `/meetingMember/*`
- `/meetingReserveMember/*`
- `/userContactApply/*`
- `/userContact/*` 中的 `add/loadDataList/get/update/delete*`
- `/meetingInfo/*` 中的 `add/loadDataList/get/update/delete*`

## 8. WebSocket 目录

- 握手地址：`ws://localhost:6098/ws?token=...`
- 用途：会议内信令、聊天实时推送、联系人与通知事件、在线状态同步。
- 消息类别（按代码语义）：
  - 会议生命周期类。
  - 聊天文本/媒体类。
  - WebRTC 与屏幕共享信令类。
  - 联系人状态与系统通知类。

## 9. 前后端接口对齐备注

前端 `frontend/src/api/services.js` 中有少量历史接口名与后端当前实现不一致：

- `GET /meetingReserve/delMeetingReserve`（后端现为 `POST /cancelMeetingReserve`）
- `GET /meetingReserve/loadMeetingReserve`（后端现为 `GET /loadMeetingReserveList`）
- `GET /meetingReserve/loadTodayMeeting`（后端未提供同名接口）

建议后续统一前端服务层命名，避免历史别名导致误调用。

