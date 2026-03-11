# 实施任务：统一收件箱系统（Unified Inbox System）

## 概述

本任务列表将统一收件箱系统的设计和需求转化为可执行的开发任务。系统将通知类型从 3 种扩展到 11 种，支持会议邀请功能，并重构收件箱 UI 以提供消息分类和待办事项管理。

技术栈：
- 后端：Spring Boot + MyBatis + MySQL + Redis
- 前端：Vue 3 + Element Plus + Axios
- 实时通信：WebSocket

## 任务列表

- [ ] 1. 数据库迁移和表结构扩展
  - 创建数据备份表 user_notification_backup
  - 执行通知类型迁移（类型 2→4，类型 3→10）
  - 为 meeting_reserve_member 表添加 invite_status 和 response_time 字段
  - 创建新的数据库索引（idx_user_action, idx_user_type_time, idx_invite_status）
  - 验证迁移结果的正确性
  - _需求: 20.1, 20.2, 20.3, 20.4, 16.1-16.7_

- [ ] 2. 后端枚举和基础类型定义
  - [ ] 2.1 创建 NotificationTypeEnum 枚举类
    - 定义 11 种通知类型（1-11）
    - 实现 getByType() 方法
    - 实现 getCategory() 方法，返回通知类别
    - _需求: 1.1, 1.5_
  
  - [ ] 2.2 创建 NotificationCategory 枚举类
    - 定义三个类别：CONTACT, MEETING, SYSTEM
    - 提供描述性标签
    - _需求: 1.2, 1.3, 1.4_
  
  - [ ]* 2.3 编写枚举类的单元测试
    - 测试所有枚举值的类型和描述
    - 测试 getByType() 方法的正确性
    - 测试 getCategory() 方法的映射关系
    - 测试无效类型的处理

- [ ] 3. 扩展 UserNotificationService 和实现类
  - [ ] 3.1 在 UserNotificationService 接口中添加新方法签名
    - createMeetingInviteNotification()
    - createMeetingResponseNotification()
    - createMeetingCancelNotification()
    - createMeetingTimeChangeNotification()
    - getNotificationsByCategory()
    - getPendingActionNotifications()
    - handleMeetingInvite()
    - _需求: 2.1-2.8, 3.1-3.8, 4.1-4.5, 5.1-5.5, 6.1-6.7, 7.1-7.4_
  
  - [ ] 3.2 实现 createMeetingInviteNotification() 方法
    - 构建会议邀请通知对象（类型=5，action_required=1，action_status=0）
    - 保存通知到数据库
    - 如果用户在线，通过 WebSocket 推送通知
    - _需求: 2.1-2.8_
  
  - [ ] 3.3 实现 handleMeetingInvite() 方法
    - 验证通知存在且属于当前用户（权限验证）
    - 验证通知类型为 5 且 action_status 为 0
    - 验证关联的会议存在且未取消
    - 更新通知的 action_status（1=已同意，2=已拒绝）
    - 更新 meeting_reserve_member 表的 invite_status 和 response_time
    - 创建响应通知发送给会议创建者（类型 6 或 7）
    - 如果创建者在线，通过 WebSocket 推送响应通知
    - _需求: 3.1-3.8, 13.1-13.4, 14.1-14.4, 15.1-15.3_
  
  - [ ] 3.4 实现 createMeetingCancelNotification() 方法
    - 为所有被邀请用户创建会议取消通知（类型=8）
    - 保存通知到数据库
    - 通过 WebSocket 推送通知给在线用户
    - _需求: 4.1-4.5_
  
  - [ ] 3.5 实现 createMeetingTimeChangeNotification() 方法
    - 为所有被邀请用户创建会议时间变更通知（类型=9）
    - 在通知内容中包含新的开始时间
    - 保存通知到数据库
    - 通过 WebSocket 推送通知给在线用户
    - _需求: 5.1-5.5_
  
  - [ ] 3.6 实现 getNotificationsByCategory() 方法
    - 根据类别确定通知类型范围（CONTACT: 1-4, MEETING: 5-9, SYSTEM: 10-11）
    - 构建查询条件，按 create_time 降序排列
    - 执行分页查询
    - 返回分页结果对象
    - _需求: 6.1-6.7, 17.1-17.4_
  
  - [ ] 3.7 实现 getPendingActionNotifications() 方法
    - 查询 action_required=1 且 action_status=0 的通知
    - 仅包含类型 1（好友申请）和类型 5（会议邀请）
    - 按 create_time 降序排列
    - _需求: 7.1-7.4_
  
  - [ ]* 3.8 编写 UserNotificationService 的单元测试
    - 测试 createMeetingInviteNotification() 的通知创建完整性
    - 测试 handleMeetingInvite() 的接受/拒绝逻辑和状态更新
    - 测试 getNotificationsByCategory() 的分类查询和分页逻辑
    - 测试 getPendingActionNotifications() 的待办消息筛选
    - 测试边界条件（空列表、无效参数等）
    - Mock Mapper 层和 WebSocket 服务

- [ ] 4. 扩展 UserNotificationController
  - [ ] 4.1 添加 loadNotificationsByCategory 接口
    - 接收参数：category, pageNo, pageSize
    - 调用 UserNotificationService.getNotificationsByCategory()
    - 返回分页结果
    - 添加 @globalInterceptor(checkLogin = true) 注解
    - _需求: 6.1-6.7_
  
  - [ ] 4.2 添加 loadPendingActions 接口
    - 调用 UserNotificationService.getPendingActionNotifications()
    - 返回待办消息列表
    - 添加 @globalInterceptor(checkLogin = true) 注解
    - _需求: 7.1-7.4_
  
  - [ ] 4.3 添加 handleMeetingInvite 接口
    - 接收参数：notificationId, accepted
    - 调用 UserNotificationService.handleMeetingInvite()
    - 返回操作结果
    - 添加 @globalInterceptor(checkLogin = true) 注解
    - _需求: 3.1-3.8_
  
  - [ ]* 4.4 编写 Controller 的集成测试
    - 测试 loadNotificationsByCategory 接口的正确性
    - 测试 loadPendingActions 接口的正确性
    - 测试 handleMeetingInvite 接口的正确性
    - 测试权限验证和错误处理

- [ ] 5. 扩展 MeetingReserveService 集成通知功能
  - [ ] 5.1 修改 createReservation 方法
    - 在创建会议后，为每个被邀请用户调用 createMeetingInviteNotification()
    - 确保事务一致性（会议创建和通知创建在同一事务中）
    - _需求: 2.1-2.8_
  
  - [ ] 5.2 修改 cancelReservation 方法
    - 在取消会议后，调用 createMeetingCancelNotification()
    - 传入所有被邀请用户的 ID 列表
    - _需求: 4.1-4.5_
  
  - [ ] 5.3 添加 updateMeetingTime 方法
    - 更新会议的 start_time 字段
    - 调用 createMeetingTimeChangeNotification()
    - 传入所有被邀请用户的 ID 列表和新的开始时间
    - _需求: 5.1-5.5_
  
  - [ ]* 5.4 编写 MeetingReserveService 的集成测试
    - 测试创建会议时通知的发送
    - 测试取消会议时通知的发送
    - 测试更新会议时间时通知的发送
    - 验证数据库状态的一致性

- [ ] 6. 更新 MyBatis Mapper 和 XML
  - [ ] 6.1 在 UserNotificationMapper.xml 中添加按类别查询的 SQL
    - 支持通过通知类型列表查询
    - 支持分页查询
    - 按 create_time 降序排列
    - _需求: 6.1-6.7_
  
  - [ ] 6.2 在 UserNotificationMapper.xml 中添加待办消息查询的 SQL
    - 查询条件：action_required=1 AND action_status=0
    - 通知类型限制为 1 或 5
    - 按 create_time 降序排列
    - _需求: 7.1-7.4_
  
  - [ ] 6.3 在 UserNotificationMapper 接口中添加对应的方法签名
    - selectByCategory()
    - selectPendingActions()

- [ ] 7. Checkpoint - 后端功能验证
  - 运行所有单元测试和集成测试，确保通过
  - 使用 Postman 或测试 HTML 页面测试所有新增 API
  - 验证数据库数据的正确性
  - 验证 WebSocket 推送功能
  - 如有问题，请向用户反馈

- [ ] 8. 前端 API 服务层扩展
  - [ ] 8.1 在 notificationService 中添加新的 API 方法
    - loadNotificationsByCategory(params)
    - loadPendingActions()
    - handleMeetingInvite(params)
    - 使用 Axios 发送 HTTP 请求
    - _需求: 6.1-6.7, 7.1-7.4, 3.1-3.8_
  
  - [ ] 8.2 定义 TypeScript 类型
    - Notification 接口
    - NotificationTypeMap 常量
    - CategoryTitleMap 常量
    - NotificationCategory 类型

- [ ] 9. 重构收件箱页面（Dashboard.vue 或新建 InboxView.vue）
  - [ ] 9.1 实现标签页结构
    - 创建"全部消息"和"待办消息"两个标签页
    - 实现标签页切换逻辑
    - 在"待办消息"标签页显示数量徽章
    - _需求: 9.1-9.4_
  
  - [ ] 9.2 实现"全部消息"标签页
    - 添加消息类型筛选器（全部消息、联系人消息、会议消息、系统消息）
    - 实现筛选器变更时重新加载通知列表
    - 显示通知列表，支持分页
    - _需求: 10.1-10.4_
  
  - [ ] 9.3 实现"待办消息"标签页
    - 显示待处理的好友申请列表
    - 显示待处理的会议邀请列表
    - 为每个待办项添加"接受"和"拒绝"按钮
    - _需求: 7.1-7.4_
  
  - [ ] 9.4 实现通知项组件
    - 根据通知类型显示对应的图标
    - 显示通知标题、内容、时间
    - 显示未读标志
    - 实现类别封面标题（在每个类别的第一条消息前显示）
    - _需求: 11.1-11.6, 12.1-12.4_
  
  - [ ] 9.5 实现通知操作逻辑
    - 实现标记为已读功能
    - 实现处理会议邀请功能（接受/拒绝）
    - 实现处理好友申请功能（接受/拒绝）
    - 操作成功后刷新列表
    - _需求: 8.1-8.4, 3.1-3.8_
  
  - [ ] 9.6 实现 WebSocket 实时通知接收
    - 监听 WebSocket 消息
    - 收到新通知时更新列表和未读数量
    - 显示通知提示（可选）
    - _需求: 2.8, 3.8, 4.5, 5.5_
  
  - [ ]* 9.7 编写前端组件的单元测试
    - 测试标签页切换功能
    - 测试类别筛选功能
    - 测试通知列表渲染
    - 测试待办消息分组显示
    - Mock API 响应

- [ ] 10. 前端样式和 UI 优化
  - [ ] 10.1 设计和实现通知项样式
    - 未读消息的视觉区分（背景色或边框）
    - 不同类型通知的图标和颜色
    - 响应式布局
  
  - [ ] 10.2 设计和实现类别封面标题样式
    - 与通知项区分的背景色
    - 清晰的类别标识
  
  - [ ] 10.3 实现操作按钮样式
    - "接受"按钮使用主题色
    - "拒绝"按钮使用次要色
    - 按钮的 hover 和 active 状态
  
  - [ ] 10.4 实现加载状态和空状态
    - 加载中的骨架屏或 loading 动画
    - 无消息时的空状态提示
    - 错误状态的友好提示

- [ ] 11. 性能优化
  - [ ] 11.1 后端缓存实现
    - 使用 Redis 缓存未读通知数量（5分钟过期）
    - 使用 Redis 缓存待办消息列表（1分钟过期）
    - 在创建、更新通知时清除相关缓存
  
  - [ ] 11.2 前端性能优化
    - 实现虚拟滚动或懒加载（如果列表超过 50 条）
    - 实现下拉加载更多功能
    - 类别筛选器变更时使用防抖（300ms）
  
  - [ ]* 11.3 性能测试
    - 测试通知列表查询响应时间（目标 < 200ms P95）
    - 测试创建通知响应时间（目标 < 100ms P95）
    - 测试处理会议邀请响应时间（目标 < 300ms P95）
    - 测试 WebSocket 推送延迟（目标 < 500ms P95）

- [ ] 12. 错误处理和安全加固
  - [ ] 12.1 实现后端错误处理
    - 会议已取消错误（MEETING_CANCELLED）
    - 重复响应邀请错误（ALREADY_RESPONDED）
    - 无权限操作错误（PERMISSION_DENIED）
    - 通知不存在错误（NOTIFICATION_NOT_FOUND）
    - 数据库连接失败错误（DATABASE_ERROR）
    - _需求: 13.1-13.4, 14.1-14.4, 15.1-15.3_
  
  - [ ] 12.2 实现前端错误处理
    - 显示友好的错误提示
    - 提供重试按钮
    - 错误时刷新列表
  
  - [ ] 12.3 实现输入验证和 XSS 防护
    - 后端对通知标题和内容进行 HTML 转义
    - 前端使用文本插值而非 HTML 渲染
    - _需求: 19.1-19.3_
  
  - [ ] 12.4 实现权限验证
    - 验证用户只能访问和操作自己的通知
    - 在所有 API 中添加权限检查
    - _需求: 13.1-13.4_

- [ ] 13. 集成测试和端到端测试
  - [ ]* 13.1 编写完整的会议邀请流程测试
    - 用户A创建会议并邀请用户B和C
    - 验证B和C收到邀请通知
    - B接受邀请，验证A收到接受通知
    - C拒绝邀请，验证A收到拒绝通知
    - 验证数据库状态的一致性
  
  - [ ]* 13.2 编写会议取消流程测试
    - 用户A创建会议并邀请用户B
    - B接受邀请
    - A取消会议
    - 验证B收到取消通知
    - 验证B无法再次响应原邀请
  
  - [ ]* 13.3 编写收件箱UI交互测试
    - 用户登录并进入收件箱
    - 切换到待办消息标签页
    - 处理好友申请
    - 处理会议邀请
    - 切换到全部消息标签页
    - 使用类别筛选器
    - 验证消息列表更新

- [ ] 14. 文档和部署准备
  - [ ] 14.1 更新 API 文档
    - 记录所有新增的 API 接口
    - 提供请求和响应示例
  
  - [ ] 14.2 编写数据库迁移指南
    - 提供迁移脚本
    - 说明迁移步骤和注意事项
    - 提供回滚方案
  
  - [ ] 14.3 编写部署检查清单
    - 数据库迁移验证
    - 配置文件更新
    - 缓存清理
    - WebSocket 连接测试
  
  - [ ] 14.4 准备监控和告警
    - 配置 API 响应时间监控
    - 配置错误率告警
    - 配置 WebSocket 连接数监控

- [ ] 15. Checkpoint - 最终验证
  - 运行所有测试（单元测试、集成测试、E2E 测试）
  - 在测试环境进行完整的功能验证
  - 验证性能指标是否达标
  - 验证安全性（权限、XSS 防护、CSRF 防护）
  - 准备上线，如有问题请向用户反馈

## 注意事项

- 任务标记 `*` 的为可选任务，可根据项目进度和优先级决定是否执行
- 每个任务都引用了对应的需求编号，便于追溯
- Checkpoint 任务用于阶段性验证，确保增量开发的质量
- 建议按顺序执行任务，后续任务依赖前面任务的完成
- 所有涉及数据库修改的操作都应先在测试环境验证
- WebSocket 推送失败不应影响主流程，通知必须先保存到数据库
