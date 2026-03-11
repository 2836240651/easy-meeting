# 会议邀请接受/拒绝逻辑修复

## 修改内容

### 问题描述
之前的实现中，无论用户接受还是拒绝会议邀请，都只是更新 `meeting_reserve_member` 表中的 `invite_status` 字段。这导致拒绝邀请的用户仍然会在"我的预约会议"列表中看到该会议。

### 修复方案
修改 `UserNotificationServiceImpl.handleMeetingInvite()` 方法的逻辑：

- **接受邀请**：保留 `meeting_reserve_member` 记录，更新 `invite_status = 1`
- **拒绝邀请**：从 `meeting_reserve_member` 表中删除该用户的记录

### 修改的文件
- `src/main/java/com/easymeeting/service/impl/UserNotificationServiceImpl.java`

### 修改的代码（第 136-150 行）

```java
// 步骤 3: 处理会议成员记录
try {
    if (accepted) {
        // 接受邀请：保留记录，更新 invite_status = 1
        meetingReserveMemberMapper.updateInviteStatus(meetingId, userId, 1, new Date());
        logger.info("用户接受会议邀请，更新 invite_status = 1，meetingId: " + meetingId + ", userId: " + userId);
    } else {
        // 拒绝邀请：从 meeting_reserve_member 表中删除该用户的记录
        meetingReserveMemberMapper.deleteByMeetingIdAndInviteUserId(meetingId, userId);
        logger.info("用户拒绝会议邀请，删除 meeting_reserve_member 记录，meetingId: " + meetingId + ", userId: " + userId);
    }
} catch (Exception e) {
    logger.error("处理会议成员记录失败", e);
    throw new BusinessException("处理会议邀请失败");
}
```

## 实现效果

### 接受邀请
1. 用户在收件箱中点击"同意"按钮
2. 通知状态更新为"已同意"（`action_status = 1`）
3. `meeting_reserve_member` 表中的记录保留，`invite_status` 更新为 1
4. 用户可以在"我的预约会议"列表中看到该会议
5. 会议创建者收到"XXX 接受了您的会议邀请"通知

### 拒绝邀请
1. 用户在收件箱中点击"拒绝"按钮
2. 通知状态更新为"已拒绝"（`action_status = 2`）
3. `meeting_reserve_member` 表中的该用户记录被删除
4. 用户不会在"我的预约会议"列表中看到该会议
5. 会议创建者收到"XXX 拒绝了您的会议邀请"通知

## 测试步骤

### 前置条件
- 后端服务已重启（已完成）
- 前端服务正常运行
- 至少有两个测试账号（例如：用户A 和 用户B）

### 测试场景 1：接受会议邀请

1. **用户A 创建预约会议并邀请用户B**
   - 使用用户A登录
   - 进入"会议"页面
   - 点击"预约会议"
   - 填写会议信息：
     - 会议名称：测试会议-接受邀请
     - 开始时间：选择未来时间
     - 会议时长：60分钟
     - 邀请成员：选择用户B
   - 点击"创建"

2. **用户B 查看并接受邀请**
   - 使用用户B登录
   - 进入"收件箱"
   - 切换到"待办消息"标签页
   - 应该看到用户A的会议邀请通知
   - 点击"同意"按钮
   - 通知状态应该变为"已同意"

3. **验证用户B可以看到会议**
   - 用户B进入"会议"页面
   - 切换到"我的预约会议"标签页
   - 应该能看到"测试会议-接受邀请"

4. **验证用户A收到响应通知**
   - 使用用户A登录
   - 进入"收件箱"
   - 应该看到"用户B 接受了您的会议邀请「测试会议-接受邀请」"通知

5. **验证数据库记录**
   - 查询 `meeting_reserve_member` 表
   - 应该存在记录：`meeting_id = XXX, invite_user_id = 用户B的ID, invite_status = 1`

### 测试场景 2：拒绝会议邀请

1. **用户A 创建预约会议并邀请用户B**
   - 使用用户A登录
   - 进入"会议"页面
   - 点击"预约会议"
   - 填写会议信息：
     - 会议名称：测试会议-拒绝邀请
     - 开始时间：选择未来时间
     - 会议时长：60分钟
     - 邀请成员：选择用户B
   - 点击"创建"

2. **用户B 查看并拒绝邀请**
   - 使用用户B登录
   - 进入"收件箱"
   - 切换到"待办消息"标签页
   - 应该看到用户A的会议邀请通知
   - 点击"拒绝"按钮
   - 通知状态应该变为"已拒绝"

3. **验证用户B看不到会议**
   - 用户B进入"会议"页面
   - 切换到"我的预约会议"标签页
   - 不应该看到"测试会议-拒绝邀请"

4. **验证用户A收到响应通知**
   - 使用用户A登录
   - 进入"收件箱"
   - 应该看到"用户B 拒绝了您的会议邀请「测试会议-拒绝邀请」"通知

5. **验证数据库记录**
   - 查询 `meeting_reserve_member` 表
   - 不应该存在记录：`meeting_id = XXX, invite_user_id = 用户B的ID`

### 测试场景 3：多用户邀请

1. **用户A 创建预约会议并邀请用户B和用户C**
   - 使用用户A登录
   - 创建会议并邀请用户B和用户C

2. **用户B 接受邀请，用户C 拒绝邀请**
   - 用户B登录并接受邀请
   - 用户C登录并拒绝邀请

3. **验证结果**
   - 用户B可以在"我的预约会议"中看到该会议
   - 用户C不能在"我的预约会议"中看到该会议
   - 用户A收到两条响应通知（一条接受，一条拒绝）

## 数据库验证 SQL

### 查看会议成员记录
```sql
SELECT * FROM meeting_reserve_member 
WHERE meeting_id = '会议ID';
```

### 查看通知记录
```sql
SELECT * FROM user_notification 
WHERE reference_id = '会议ID' 
ORDER BY create_time DESC;
```

### 查看会议详情
```sql
SELECT * FROM meeting_reserve 
WHERE meeting_id = '会议ID';
```

## 注意事项

1. **事务处理**：`handleMeetingInvite` 方法使用了 `@Transactional` 注解，确保通知更新和成员记录处理在同一事务中
2. **错误处理**：如果删除或更新失败，会抛出 `BusinessException`，事务会回滚
3. **日志记录**：每次操作都会记录详细的日志，便于排查问题
4. **WebSocket 推送**：响应通知会通过 WebSocket 实时推送给会议创建者

## 相关文件

- `src/main/java/com/easymeeting/service/impl/UserNotificationServiceImpl.java` - 通知服务实现
- `src/main/java/com/easymeeting/mappers/MeetingReserveMemberMapper.java` - 会议成员 Mapper 接口
- `src/main/resources/com/easymeeting/mappers/MeetingReserveMemberMapper.xml` - 会议成员 Mapper XML
- `frontend/src/views/Dashboard.vue` - 收件箱前端页面
- `frontend/src/api/services.js` - API 服务

## 状态说明

✅ 代码修改完成
✅ 后端编译成功
✅ 后端服务已重启
⏳ 等待测试验证

## 总结

修复了会议邀请的接受/拒绝逻辑，现在：
- 接受邀请的用户会保留在 `meeting_reserve_member` 表中，可以看到预约会议
- 拒绝邀请的用户会从 `meeting_reserve_member` 表中删除，不会看到预约会议
- 会议创建者会收到相应的响应通知

这样确保了只有接受邀请的用户才能在"我的预约会议"中看到该会议。
