# 实现计划：会议预约功能 (meeting-reservation)

## 概述

本实现计划将会议预约功能分解为可执行的开发任务。该功能扩展现有的即时会议系统，实现完整的预约会议生命周期管理，包括创建、查询、加入、修改和取消预约会议。

后端使用 Java (Spring Boot + MyBatis + Redis)，前端使用 JavaScript (Vue 3 + Element Plus)。

## 任务列表

### 1. 后端基础设施

- [x] 1.1 扩展 MeetingReserveMapper 接口
  - 添加批量查询方法 `selectByMeetingIds(List<String> meetingIds)`
  - 添加条件查询方法支持状态和时间范围过滤
  - 添加更新方法 `updateByMeetingId(MeetingReserve bean, String meetingId)`
  - _需求: 2.1, 2.2, 4.1, 8.1_

- [x] 1.2 扩展 MeetingReserveMemberMapper 接口
  - 添加批量插入方法 `insertBatch(List<MeetingReserveMember> members)`
  - 添加条件删除方法 `deleteByParam(MeetingReserveMemberQuery query)`
  - 添加按用户ID查询方法 `selectByInviteUserId(String userId)`
  - _需求: 1.4, 4.6, 5.4, 6.4_

- [ ]* 1.3 编写 Mapper 单元测试
  - 测试批量查询和插入功能
  - 测试条件查询和删除功能
  - _需求: 10.1, 10.2_

### 2. 后端服务层 - MeetingReserveService

- [x] 2.1 实现 loadMeetingReserveList 方法
  - 查询用户参与的所有预约会议（创建的和被邀请的）
  - 按开始时间降序排序
  - 过滤已取消的会议
  - 填充创建者昵称信息
  - _需求: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ]* 2.2 编写 loadMeetingReserveList 属性测试
  - **属性 10: 查询结果完整性**
  - **验证需求: 2.1, 2.2, 2.4**

- [x] 2.3 实现 updateMeetingReserve 方法
  - 验证用户是创建者
  - 验证会议状态为未开始
  - 验证修改的时间是未来时间
  - 更新预约会议信息
  - 更新成员列表（如果修改了邀请列表）
  - 使用事务保证原子性
  - _需求: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9_

- [ ]* 2.4 编写 updateMeetingReserve 属性测试
  - **属性 2: 创建者必在成员列表（不变式）**
  - **属性 18: 修改权限验证**
  - **属性 19: 成员列表更新正确性**
  - **验证需求: 4.1, 4.2, 4.8_

- [x] 2.5 实现 getMeetingReserveDetail 方法
  - 验证用户访问权限
  - 查询预约会议基本信息
  - 查询所有参与者列表
  - 标识当前用户是否为创建者
  - 生成会议状态描述
  - _需求: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

- [ ]* 2.6 编写 getMeetingReserveDetail 单元测试
  - 测试权限验证
  - 测试数据完整性
  - _需求: 7.1, 7.2, 7.3_

- [x] 2.7 实现 getUpcomingMeetings 方法
  - 计算未来1小时的时间范围
  - 查询用户参与的预约会议
  - 过滤状态为未开始的会议
  - 按开始时间升序排序
  - _需求: 8.1, 8.2, 8.3_

- [ ]* 2.8 编写 getUpcomingMeetings 属性测试
  - **属性 26: 即将开始会议过滤**
  - **验证需求: 8.1, 8.2, 8.3**

- [x] 2.9 实现 checkMeetingReserveAccess 方法
  - 验证用户是否在成员列表中
  - 返回布尔值表示访问权限
  - _需求: 11.5, 11.6_

### 3. 后端服务层 - 验证和辅助方法

- [x] 3.1 实现 validateMeetingReserveInput 方法
  - 验证会议名称长度（1-50）
  - 验证开始时间是未来时间
  - 验证会议时长范围（15-480分钟）
  - 验证密码长度（5位，当需要密码时）
  - _需求: 1.5, 1.6, 1.7, 1.8, 10.4, 10.5, 10.6_

- [ ]* 3.2 编写输入验证属性测试
  - **属性 5: 会议名称验证**
  - **属性 6: 开始时间验证**
  - **属性 7: 时长范围验证**
  - **属性 8: 密码长度验证**
  - **验证需求: 1.5, 1.6, 1.7, 1.8**

### 4. 后端控制器层 - MeetingReserveController

- [x] 4.1 创建 MeetingReserveController 类
  - 添加 @RestController 和 @RequestMapping 注解
  - 注入 MeetingReserveService 和 ContactService
  - 设置基础路径为 `/meetingReserve`
  - _需求: 11.1_

- [x] 4.2 实现 createMeetingReserve 接口
  - 添加 @PostMapping 和 @GlobalInterceptor 注解
  - 参数验证（@NotEmpty, @NotNull）
  - 调用 service.createMeetingReserve
  - 返回统一格式响应
  - _需求: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9_

- [x] 4.3 实现 loadMeetingReserveList 接口
  - 添加 @GetMapping 和 @GlobalInterceptor 注解
  - 从 token 获取当前用户ID
  - 调用 service.loadMeetingReserveList
  - 返回预约会议列表
  - _需求: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 4.4 实现 updateMeetingReserve 接口
  - 添加 @PostMapping 和 @GlobalInterceptor 注解
  - 参数验证
  - 从 token 获取当前用户ID
  - 调用 service.updateMeetingReserve
  - 返回成功响应
  - _需求: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 4.5 实现 cancelMeetingReserve 接口
  - 添加 @PostMapping 和 @GlobalInterceptor 注解
  - 参数验证（@NotEmpty meetingId）
  - 从 token 获取当前用户ID
  - 调用 service.deleteMeetingReserveByMeetingId
  - 返回成功响应
  - _需求: 5.1, 5.2, 5.3, 5.4_

- [x] 4.6 实现 leaveMeetingReserve 接口
  - 添加 @PostMapping 和 @GlobalInterceptor 注解
  - 参数验证（@NotEmpty meetingId）
  - 从 token 获取当前用户ID
  - 调用 service.deleteMeetingReserveByUserId
  - 返回成功响应
  - _需求: 6.1, 6.2, 6.3, 6.4_

- [x] 4.7 实现 getMeetingReserveDetail 接口
  - 添加 @GetMapping 和 @GlobalInterceptor 注解
  - 参数验证（@NotEmpty meetingId）
  - 从 token 获取当前用户ID
  - 调用 service.getMeetingReserveDetail
  - 返回会议详情
  - _需求: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

- [x] 4.8 实现 getUpcomingMeetings 接口
  - 添加 @GetMapping 和 @GlobalInterceptor 注解
  - 从 token 获取当前用户ID
  - 调用 service.getUpcomingMeetings
  - 返回即将开始的会议列表
  - _需求: 8.1, 8.2, 8.3_

- [ ]* 4.9 编写 Controller 集成测试
  - 测试所有接口的正常流程
  - 测试权限验证
  - 测试错误处理
  - _需求: 11.1, 13.1, 13.2, 13.3, 13.4_

### 5. 后端错误处理和异常

- [x] 5.1 添加错误处理逻辑
  - 会议不存在：返回 ResponseCodeEnum.CODE_600
  - 密码错误：返回 ResponseCodeEnum.CODE_703
  - 无权限访问：抛出 BusinessException
  - 参数验证失败：抛出 BusinessException
  - 时间冲突：抛出 BusinessException
  - _需求: 13.1, 13.2, 13.3, 13.4, 13.5_

- [ ]* 5.2 编写错误处理单元测试
  - 测试各种错误场景
  - 验证错误消息正确性
  - _需求: 13.1, 13.2, 13.3_

### 6. 检查点 - 后端功能完成

- [x] 6.1 后端检查点
  - 确保所有后端测试通过
  - 验证 API 接口可正常调用
  - 检查数据库操作正确性
  - 如有问题，请向用户询问

### 7. 前端组件 - ScheduleMeetingModal（预约会议模态框）

- [x] 7.1 创建 ScheduleMeetingModal.vue 组件
  - 创建文件 `frontend/src/components/ScheduleMeetingModal.vue`
  - 定义 props（visible）和 emits（update:visible, created）
  - 设置基础模板结构（使用 el-dialog）
  - _需求: 15.1_

- [x] 7.2 实现预约表单数据结构
  - 定义 form 对象（meetingName, startTime, duration, joinType, joinPassword, inviteUserIds）
  - 设置默认值（duration: 60, joinType: 0）
  - 定义 contactList 和 selectedContacts 数组
  - _需求: 15.2_

- [x] 7.3 实现联系人加载和选择
  - 实现 loadContacts 方法（调用 /contact/loadContact API）
  - 使用 el-select 组件实现多选联系人
  - 支持搜索和过滤联系人
  - _需求: 15.3_

- [x] 7.4 实现时间和密码输入
  - 使用 el-date-picker 选择开始时间（type="datetime"）
  - 使用 el-input-number 设置会议时长
  - 使用 el-radio-group 选择加入方式
  - 条件显示密码输入框（当 joinType === 1）
  - _需求: 15.2_

- [x] 7.5 实现表单验证
  - 实现 validateForm 方法
  - 验证会议名称非空且长度 ≤ 50
  - 验证开始时间是未来时间
  - 验证会议时长在 15-480 范围内
  - 验证密码长度为 5 位（当需要密码时）
  - _需求: 15.2_

- [x] 7.6 实现表单提交
  - 实现 handleSubmit 方法
  - 调用 validateForm 验证
  - 调用 /meetingReserve/createMeetingReserve API
  - 处理成功和失败响应
  - 成功后触发 created 事件并关闭模态框
  - _需求: 15.1, 15.2_

- [ ]* 7.7 编写 ScheduleMeetingModal 组件测试
  - 测试表单验证逻辑
  - 测试 API 调用
  - _需求: 15.2_

### 8. 前端组件 - ReservationList（预约会议列表）

- [x] 8.1 创建 ReservationList.vue 组件
  - 创建文件 `frontend/src/components/ReservationList.vue`
  - 定义数据结构（upcomingMeetings, endedMeetings, activeTab）
  - 设置基础模板结构（使用 el-tabs）
  - _需求: 15.4_

- [x] 8.2 实现预约会议列表加载
  - 实现 loadMeetingReserveList 方法
  - 调用 /meetingReserve/loadMeetingReserveList API
  - 分类会议：即将开始（未来时间且状态为未开始）vs 已结束
  - 在 mounted 钩子中调用加载方法
  - _需求: 15.4_

- [x] 8.3 实现会议列表展示
  - 使用 el-card 展示每个会议
  - 显示会议名称、创建者、开始时间、时长
  - 实现 formatMeetingTime 方法格式化时间
  - 实现 getMeetingStatus 方法获取状态标识
  - 使用 el-tag 显示会议状态
  - _需求: 15.4, 15.5_

- [x] 8.4 实现加入会议功能
  - 实现 joinMeeting 方法
  - 如果需要密码，弹出密码输入框
  - 调用 /joinMeetingReserve API
  - 成功后跳转到会议页面（router.push）
  - _需求: 15.6_

- [x] 8.5 实现修改会议功能（创建者）
  - 实现 editMeeting 方法
  - 打开 ScheduleMeetingModal 并传入会议数据
  - 调用 /meetingReserve/updateMeetingReserve API
  - 成功后刷新列表
  - 只对创建者显示修改按钮
  - _需求: 15.7_

- [x] 8.6 实现取消会议功能（创建者）
  - 实现 cancelMeeting 方法
  - 使用 el-message-box 确认取消操作
  - 调用 /meetingReserve/cancelMeetingReserve API
  - 成功后刷新列表
  - 只对创建者显示取消按钮
  - _需求: 15.7_

- [x] 8.7 实现退出会议功能（被邀请者）
  - 实现 leaveMeeting 方法
  - 使用 el-message-box 确认退出操作
  - 调用 /meetingReserve/leaveMeetingReserve API
  - 成功后刷新列表
  - 只对被邀请者显示退出按钮
  - _需求: 15.8_

- [ ]* 8.8 编写 ReservationList 组件测试
  - 测试会议分类逻辑
  - 测试按钮显示逻辑
  - _需求: 15.4, 15.5_

### 9. 前端组件 - MeetingReminder（会议提醒）

- [x] 9.1 创建 MeetingReminder.vue 组件
  - 创建文件 `frontend/src/components/MeetingReminder.vue`
  - 定义数据结构（upcomingMeetings, reminderInterval）
  - 设置基础模板结构
  - _需求: 15.9_

- [x] 9.2 实现定时检查逻辑
  - 实现 startReminderCheck 方法（每分钟检查一次）
  - 实现 stopReminderCheck 方法
  - 在 mounted 钩子中启动定时检查
  - 在 beforeUnmount 钩子中停止定时检查
  - _需求: 15.9_

- [x] 9.3 实现即将开始会议检查
  - 实现 checkUpcomingMeetings 方法
  - 调用 /meetingReserve/getUpcomingMeetings API
  - 如果有即将开始的会议，显示通知
  - 使用 el-notification 显示提醒（duration: 0 表示不自动关闭）
  - _需求: 15.9_

- [x] 9.4 实现快速加入功能
  - 实现 quickJoin 方法
  - 在通知中添加"立即加入"按钮
  - 点击后调用加入会议逻辑
  - 跳转到会议页面
  - _需求: 15.10_

- [ ]* 9.5 编写 MeetingReminder 组件测试
  - 测试定时检查逻辑
  - 测试通知显示
  - _需求: 15.9_

### 10. 前端集成 - Dashboard 整合

- [x] 10.1 在 Dashboard.vue 中集成预约会议功能
  - 导入 ScheduleMeetingModal、ReservationList、MeetingReminder 组件
  - 添加"预约会议"按钮
  - 添加预约会议列表展示区域
  - 添加会议提醒组件
  - _需求: 15.1, 15.4, 15.9_

- [x] 10.2 实现组件间通信
  - 处理 ScheduleMeetingModal 的 created 事件
  - 创建成功后刷新 ReservationList
  - 处理 MeetingReminder 的快速加入事件
  - _需求: 15.1, 15.10_

- [ ]* 10.3 编写前端集成测试
  - 测试组件间通信
  - 测试完整用户流程
  - _需求: 15.1, 15.4_

### 11. 检查点 - 前端功能完成

- [x] 11.1 前端检查点
  - 确保所有前端组件正常渲染
  - 验证 API 调用正确
  - 测试用户交互流程
  - 如有问题，请向用户询问

### 12. 性能优化

- [x] 12.1 添加数据库索引
  - 验证 meeting_reserve 表索引（idx_create_user_id, idx_start_time, idx_status, idx_status_start_time）
  - 验证 meeting_reserve_member 表索引（idx_invite_user_id, idx_meeting_id）
  - _需求: 14.1, 14.2, 14.3, 14.4_

- [x] 12.2 实现 Redis 缓存
  - 缓存即将开始的会议列表（TTL: 5分钟）
  - 缓存用户的预约会议数量（TTL: 10分钟）
  - 在创建、修改、取消时清除相关缓存
  - _需求: 14.1, 14.2_

- [x] 12.3 实现分布式锁
  - 在 MeetingInfoService.joinMeetingReserve 中使用 Redis 分布式锁
  - 防止多个用户同时加入时重复创建 MeetingInfo
  - 使用 RedisComponent.tryLock 和 unlock 方法
  - _需求: 12.1, 12.2_

- [ ]* 12.4 编写并发控制测试
  - 测试多用户同时加入预约会议
  - 验证只创建一个 MeetingInfo
  - _需求: 12.1_

### 13. 前端优化

- [x] 13.1 添加加载状态
  - 在所有 API 调用时显示 loading 状态
  - 使用 el-loading 指令或组件
  - _需求: 15.2_

- [x] 13.2 优化错误提示
  - 统一错误处理逻辑
  - 使用 el-message 显示友好的错误提示
  - 根据错误码显示不同的提示信息
  - _需求: 13.1, 13.2, 13.3, 13.4_

- [x] 13.3 优化移动端适配
  - 调整模态框在移动端的显示
  - 优化列表在小屏幕上的布局
  - 使用响应式设计
  - _需求: 15.1, 15.4_

### 14. 最终集成和测试

- [x] 14.1 端到端测试
  - 测试完整的创建预约会议流程
  - 测试多用户加入预约会议流程
  - 测试修改和取消预约会议流程
  - 测试权限控制和错误处理
  - _需求: 1.1-15.10_

- [ ]* 14.2 性能测试
  - 测试 API 响应时间
  - 验证是否满足性能要求（创建<200ms, 查询<100ms, 加入<300ms）
  - _需求: 14.1, 14.2, 14.3, 14.4_

- [x] 14.3 最终检查点
  - 确保所有功能正常工作
  - 验证所有需求已实现
  - 检查代码质量和文档完整性
  - 如有问题，请向用户询问

## 注意事项

- 标记 `*` 的任务为可选任务，可以跳过以加快 MVP 开发
- 每个任务都引用了相关的需求编号，便于追溯
- 检查点任务确保增量验证，及时发现问题
- 属性测试验证通用正确性属性
- 单元测试验证特定示例和边界情况
- 集成测试验证端到端流程

## 实现顺序建议

1. 先完成后端基础设施和服务层（任务 1-5）
2. 然后完成后端控制器层（任务 4）
3. 进行后端检查点验证（任务 6）
4. 完成前端组件开发（任务 7-10）
5. 进行前端检查点验证（任务 11）
6. 最后进行性能优化和最终测试（任务 12-14）
