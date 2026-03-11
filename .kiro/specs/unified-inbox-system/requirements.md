# 需求文档：统一收件箱系统

## 引言

统一收件箱系统旨在整合和扩展现有的通知系统，支持多种消息类型的分类管理和待办事项处理。系统将通知类型从原有的 3 种扩展到 11 种，涵盖联系人消息、会议消息和系统消息三大类别，并提供直观的用户界面支持消息筛选和待办事项管理。

## 术语表

- **System**: 统一收件箱系统
- **User**: 使用系统的用户
- **Notification**: 通知消息，包含标题、内容、类型等信息
- **Notification_Type**: 通知类型，定义消息的具体类别（1-11）
- **Notification_Category**: 通知类别，包括联系人（CONTACT）、会议（MEETING）、系统（SYSTEM）
- **Pending_Notification**: 待办通知，需要用户操作的通知（action_required=1 且 action_status=0）
- **Meeting_Invite**: 会议邀请，类型为 5 的通知
- **Contact_Apply**: 好友申请，类型为 1 的通知
- **Action_Status**: 操作状态，0=待处理，1=已同意，2=已拒绝
- **Meeting_Creator**: 会议创建者
- **Invitee**: 被邀请参加会议的用户
- **WebSocket**: 实时消息推送协议
- **Database**: 数据库系统，包括 user_notification、meeting_reserve、meeting_reserve_member 表

## 需求

### 需求 1：通知类型扩展

**用户故事**：作为系统管理员，我希望系统支持 11 种不同的通知类型，以便能够准确分类和管理各种消息。

#### 验收标准

1. THE System SHALL 支持 11 种通知类型枚举值（1-11）
2. WHEN 通知类型为 1-4 时，THE System SHALL 将其归类为联系人类别（CONTACT）
3. WHEN 通知类型为 5-9 时，THE System SHALL 将其归类为会议类别（MEETING）
4. WHEN 通知类型为 10-11 时，THE System SHALL 将其归类为系统类别（SYSTEM）
5. THE System SHALL 为每种通知类型提供描述性标签

### 需求 2：会议邀请通知创建

**用户故事**：作为会议创建者，我希望系统在我创建预约会议时自动向被邀请用户发送邀请通知，以便他们能够及时收到会议信息。

#### 验收标准

1. WHEN 用户创建预约会议并指定被邀请用户时，THE System SHALL 为每个被邀请用户创建一条会议邀请通知
2. WHEN 创建会议邀请通知时，THE System SHALL 设置通知类型为 5（会议邀请待处理）
3. WHEN 创建会议邀请通知时，THE System SHALL 设置 action_required 为 1（需要操作）
4. WHEN 创建会议邀请通知时，THE System SHALL 设置 action_status 为 0（待处理）
5. WHEN 创建会议邀请通知时，THE System SHALL 设置 status 为 0（未读）
6. WHEN 创建会议邀请通知时，THE System SHALL 在 reference_id 字段中存储会议 ID
7. WHEN 创建会议邀请通知时，THE System SHALL 在通知内容中包含创建者昵称和会议名称
8. WHEN 被邀请用户在线时，THE System SHALL 通过 WebSocket 实时推送会议邀请通知

### 需求 3：会议邀请响应处理

**用户故事**：作为被邀请用户，我希望能够接受或拒绝会议邀请，以便会议创建者知道我的参会意向。

#### 验收标准

1. WHEN 用户接受会议邀请时，THE System SHALL 更新通知的 action_status 为 1（已同意）
2. WHEN 用户拒绝会议邀请时，THE System SHALL 更新通知的 action_status 为 2（已拒绝）
3. WHEN 用户响应会议邀请时，THE System SHALL 更新通知的 update_time 为当前时间
4. WHEN 用户响应会议邀请时，THE System SHALL 更新 meeting_reserve_member 表中对应记录的 invite_status
5. WHEN 用户响应会议邀请时，THE System SHALL 更新 meeting_reserve_member 表中对应记录的 response_time 为当前时间
6. WHEN 用户接受会议邀请时，THE System SHALL 创建类型为 6（会议邀请已接受）的响应通知发送给会议创建者
7. WHEN 用户拒绝会议邀请时，THE System SHALL 创建类型为 7（会议邀请已拒绝）的响应通知发送给会议创建者
8. WHEN 会议创建者在线时，THE System SHALL 通过 WebSocket 实时推送响应通知

### 需求 4：会议取消通知

**用户故事**：作为会议创建者，我希望在取消会议时系统自动通知所有被邀请用户，以便他们及时了解会议取消信息。

#### 验收标准

1. WHEN 会议创建者取消会议时，THE System SHALL 为所有被邀请用户创建会议取消通知
2. WHEN 创建会议取消通知时，THE System SHALL 设置通知类型为 8（会议取消通知）
3. WHEN 创建会议取消通知时，THE System SHALL 设置 action_required 为 0（不需要操作）
4. WHEN 创建会议取消通知时，THE System SHALL 在通知内容中包含创建者昵称和会议名称
5. WHEN 被邀请用户在线时，THE System SHALL 通过 WebSocket 实时推送会议取消通知

### 需求 5：会议时间变更通知

**用户故事**：作为会议创建者，我希望在修改会议时间时系统自动通知所有被邀请用户，以便他们了解新的会议时间。

#### 验收标准

1. WHEN 会议创建者修改会议开始时间时，THE System SHALL 为所有被邀请用户创建会议时间变更通知
2. WHEN 创建会议时间变更通知时，THE System SHALL 设置通知类型为 9（会议时间变更通知）
3. WHEN 创建会议时间变更通知时，THE System SHALL 设置 action_required 为 0（不需要操作）
4. WHEN 创建会议时间变更通知时，THE System SHALL 在通知内容中包含创建者昵称、会议名称和新的开始时间
5. WHEN 被邀请用户在线时，THE System SHALL 通过 WebSocket 实时推送会议时间变更通知

### 需求 6：按类别查询通知

**用户故事**：作为用户，我希望能够按消息类别（联系人、会议、系统）筛选通知列表，以便快速找到特定类型的消息。

#### 验收标准

1. WHEN 用户请求按类别查询通知时，THE System SHALL 返回指定类别的所有通知
2. WHEN 查询联系人类别时，THE System SHALL 返回通知类型为 1-4 的通知
3. WHEN 查询会议类别时，THE System SHALL 返回通知类型为 5-9 的通知
4. WHEN 查询系统类别时，THE System SHALL 返回通知类型为 10-11 的通知
5. WHEN 返回通知列表时，THE System SHALL 按创建时间降序排列
6. WHEN 返回通知列表时，THE System SHALL 支持分页查询
7. WHEN 返回分页结果时，THE System SHALL 包含页码、每页数量、总数量和总页数信息

### 需求 7：待办消息查询

**用户故事**：作为用户，我希望能够查看所有需要我操作的待办消息，以便及时处理好友申请和会议邀请。

#### 验收标准

1. WHEN 用户请求查询待办消息时，THE System SHALL 返回所有 action_required 为 1 且 action_status 为 0 的通知
2. WHEN 返回待办消息时，THE System SHALL 仅包含通知类型为 1（好友申请）或 5（会议邀请）的通知
3. WHEN 返回待办消息时，THE System SHALL 按创建时间降序排列
4. THE System SHALL 将待办消息分为好友申请和会议邀请两个子类别

### 需求 8：通知已读标记

**用户故事**：作为用户，我希望能够将通知标记为已读，以便区分已查看和未查看的消息。

#### 验收标准

1. WHEN 用户标记通知为已读时，THE System SHALL 更新通知的 status 为 1（已读）
2. WHEN 用户标记通知为已读时，THE System SHALL 更新通知的 update_time 为当前时间
3. WHEN 用户请求标记已读时，THE System SHALL 验证通知属于该用户
4. IF 通知不属于该用户，THEN THE System SHALL 返回权限错误

### 需求 9：收件箱 UI 标签页

**用户故事**：作为用户，我希望收件箱提供"全部消息"和"待办消息"两个标签页，以便分别查看所有消息和需要处理的消息。

#### 验收标准

1. THE System SHALL 在收件箱页面显示"全部消息"和"待办消息"两个标签页
2. WHEN 用户切换到"全部消息"标签页时，THE System SHALL 显示所有通知列表
3. WHEN 用户切换到"待办消息"标签页时，THE System SHALL 显示待办消息列表
4. WHEN 待办消息数量大于 0 时，THE System SHALL 在"待办消息"标签页显示数量徽章

### 需求 10：消息类型筛选器

**用户故事**：作为用户，我希望在"全部消息"标签页中使用类型筛选器，以便只查看特定类别的消息。

#### 验收标准

1. WHEN 用户在"全部消息"标签页时，THE System SHALL 显示消息类型筛选器
2. THE System SHALL 在筛选器中提供"全部消息"、"联系人消息"、"会议消息"、"系统消息"四个选项
3. WHEN 用户选择筛选类别时，THE System SHALL 重新加载对应类别的通知列表
4. WHEN 用户选择"全部消息"时，THE System SHALL 显示所有类别的通知

### 需求 11：通知项展示

**用户故事**：作为用户，我希望每条通知能够清晰显示类型图标、标题、内容、时间和未读状态，以便快速了解消息信息。

#### 验收标准

1. WHEN 显示通知项时，THE System SHALL 根据通知类型显示对应的图标
2. WHEN 显示通知项时，THE System SHALL 显示通知标题
3. WHEN 显示通知项时，THE System SHALL 显示通知内容
4. WHEN 显示通知项时，THE System SHALL 显示格式化的创建时间
5. WHEN 通知状态为未读时，THE System SHALL 显示未读标志
6. WHEN 通知需要操作时，THE System SHALL 显示操作按钮（接受/拒绝）

### 需求 12：类别封面标题

**用户故事**：作为用户，我希望在消息列表中看到类别封面标题，以便区分不同类别的消息组。

#### 验收标准

1. WHEN 显示通知列表时，THE System SHALL 在每个类别的第一条消息前显示类别封面标题
2. WHEN 通知类型为 1-4 时，THE System SHALL 显示"联系人申请类消息"标题
3. WHEN 通知类型为 5-9 时，THE System SHALL 显示"会议消息"标题
4. WHEN 通知类型为 10-11 时，THE System SHALL 显示"系统消息"标题

### 需求 13：权限验证

**用户故事**：作为系统，我需要验证用户只能访问和操作自己的通知，以保护用户隐私和数据安全。

#### 验收标准

1. WHEN 用户请求查询通知时，THE System SHALL 仅返回该用户的通知
2. WHEN 用户请求标记通知为已读时，THE System SHALL 验证通知属于该用户
3. WHEN 用户请求响应会议邀请时，THE System SHALL 验证通知属于该用户
4. IF 用户尝试操作不属于自己的通知，THEN THE System SHALL 返回权限错误并拒绝操作

### 需求 14：会议邀请响应限制

**用户故事**：作为系统，我需要确保每个会议邀请只能被响应一次，以防止重复操作导致的数据不一致。

#### 验收标准

1. WHEN 用户响应会议邀请时，THE System SHALL 验证通知的 action_status 为 0（待处理）
2. IF 通知的 action_status 不为 0，THEN THE System SHALL 返回"已响应"错误并拒绝操作
3. WHEN 用户响应会议邀请时，THE System SHALL 验证通知类型为 5（会议邀请待处理）
4. IF 通知类型不为 5，THEN THE System SHALL 返回"通知类型错误"错误并拒绝操作

### 需求 15：已取消会议的邀请处理

**用户故事**：作为系统，我需要防止用户响应已取消会议的邀请，以避免无效操作。

#### 验收标准

1. WHEN 用户响应会议邀请时，THE System SHALL 验证关联的会议存在
2. WHEN 用户响应会议邀请时，THE System SHALL 验证会议状态不为 3（已取消）
3. IF 会议不存在或已取消，THEN THE System SHALL 返回"会议已取消"错误并拒绝操作

### 需求 16：数据库索引优化

**用户故事**：作为系统管理员，我希望系统具有良好的查询性能，以便用户能够快速加载通知列表。

#### 验收标准

1. THE Database SHALL 在 user_notification 表的 user_id 字段上创建索引
2. THE Database SHALL 在 user_notification 表的 notification_type 字段上创建索引
3. THE Database SHALL 在 user_notification 表的 status 字段上创建索引
4. THE Database SHALL 在 user_notification 表的 action_required 字段上创建索引
5. THE Database SHALL 在 user_notification 表的 reference_id 字段上创建索引
6. THE Database SHALL 在 user_notification 表上创建复合索引 (user_id, action_required, action_status)
7. THE Database SHALL 在 user_notification 表上创建复合索引 (user_id, notification_type, create_time DESC)

### 需求 17：分页参数验证

**用户故事**：作为系统，我需要验证分页参数的有效性，以防止无效查询和潜在的性能问题。

#### 验收标准

1. WHEN 用户请求分页查询时，THE System SHALL 验证 pageNo 大于等于 1
2. IF pageNo 小于 1，THEN THE System SHALL 返回参数错误
3. WHEN 用户请求分页查询时，THE System SHALL 验证 pageSize 大于 0 且小于等于 100
4. IF pageSize 无效，THEN THE System SHALL 返回参数错误

### 需求 18：WebSocket 推送降级

**用户故事**：作为系统，我需要在 WebSocket 推送失败时保证通知仍然被保存，以确保消息不丢失。

#### 验收标准

1. WHEN 创建通知时，THE System SHALL 首先将通知保存到数据库
2. WHEN 用户在线时，THE System SHALL 尝试通过 WebSocket 推送通知
3. IF WebSocket 推送失败，THEN THE System SHALL 记录错误日志但不影响主流程
4. WHEN WebSocket 推送失败时，THE System SHALL 确保通知已成功保存到数据库

### 需求 19：输入内容转义

**用户故事**：作为系统，我需要对用户输入的内容进行转义，以防止 XSS 攻击。

#### 验收标准

1. WHEN 创建通知时，THE System SHALL 对通知标题进行 HTML 转义
2. WHEN 创建通知时，THE System SHALL 对通知内容进行 HTML 转义
3. WHEN 前端显示通知内容时，THE System SHALL 使用文本插值而非 HTML 渲染

### 需求 20：数据迁移兼容性

**用户故事**：作为系统管理员，我希望系统能够平滑迁移现有通知数据，以保持向后兼容性。

#### 验收标准

1. WHEN 执行数据迁移时，THE System SHALL 将现有通知类型 3（系统通知）迁移到类型 10
2. WHEN 执行数据迁移时，THE System SHALL 将现有通知类型 2（联系人删除）迁移到类型 4
3. WHEN 执行数据迁移时，THE System SHALL 保持通知类型 1（好友申请）不变
4. WHEN 执行数据迁移时，THE System SHALL 在迁移前创建数据备份表
