# 预约会议功能测试指南

## 功能概述

预约会议功能允许用户创建未来的会议，邀请联系人参加，并在会议开始前收到提醒。

## 已实现的功能

### 后端 API
1. ✅ `POST /meetingReserve/createMeetingReserve` - 创建预约会议
2. ✅ `GET /meetingReserve/loadMeetingReserveList` - 查询用户的预约会议列表
3. ✅ `POST /meetingReserve/updateMeetingReserve` - 修改预约会议
4. ✅ `POST /meetingReserve/cancelMeetingReserve` - 取消预约会议（创建者）
5. ✅ `POST /meetingReserve/leaveMeetingReserve` - 退出预约会议（被邀请者）
6. ✅ `GET /meetingReserve/getMeetingReserveDetail` - 查询预约会议详情
7. ✅ `GET /meetingReserve/getUpcomingMeetings` - 获取即将开始的会议

### 前端组件
1. ✅ `ScheduleMeetingModal.vue` - 预约会议创建/编辑模态框
2. ✅ `ReservationList.vue` - 预约会议列表（可折叠）
3. ✅ `MeetingReminder.vue` - 会议提醒组件

### 简化设计
- 会议时长：固定 60 分钟
- 加入方式：固定无需密码
- 只需填写：会议名称、开始时间、邀请成员

## 测试步骤

### 1. 启动服务

#### 后端
```bash
mvn spring-boot:run
```
后端将在 http://localhost:6099 启动

**重要提示**: 如果后端服务已经在运行，需要重启才能加载新的 `MeetingReserveController`。请先停止现有服务，然后重新启动。

#### 前端
```bash
cd frontend
npm run dev
```
前端将在 http://localhost:3000 启动

### 1.1 快速 API 测试

在浏览器中打开 `test-meeting-reserve-api.html` 文件，可以快速测试所有预约会议 API：

1. 先登录获取 token
2. 测试 `getUpcomingMeetings` API（检查是否返回 404）
3. 创建预约会议
4. 查询会议列表
5. 查询会议详情
6. 取消会议

如果 `getUpcomingMeetings` 返回 404 错误，说明后端服务没有加载新的 Controller，需要重启后端服务。

### 2. 创建预约会议

1. 登录系统
2. 在 Dashboard 页面，点击"预约会议"按钮
3. 填写表单：
   - 会议名称：例如"项目讨论会"
   - 开始时间：选择未来的时间
   - 邀请成员：从联系人列表中选择（可选）
4. 点击"创建"按钮
5. 应该看到成功提示

### 3. 查看预约会议列表

1. 在 Dashboard 页面，找到"我的预约会议"区域
2. 点击标题展开列表
3. 应该看到两个标签页：
   - "即将开始"：显示未来的会议
   - "已结束"：显示过去的会议
4. 每个会议卡片显示：
   - 会议名称
   - 创建者
   - 开始时间
   - 会议时长
   - 状态标签

### 4. 测试会议操作

#### 作为创建者
- ✅ 修改会议：点击"修改"按钮，更改会议信息
- ✅ 取消会议：点击"取消"按钮，确认后会议被取消

#### 作为被邀请者
- ✅ 加入会议：点击"加入会议"按钮
- ✅ 退出会议：点击"退出"按钮，从会议中移除

### 5. 测试会议提醒

1. 创建一个开始时间在 1 小时内的会议
2. 等待 1 分钟（提醒组件每分钟检查一次）
3. 应该在右下角看到通知提醒
4. 通知内容包括：
   - 会议名称
   - 距离开始的时间
5. 点击通知可以快速加入会议

### 6. API 测试

可以使用 Postman 或 curl 测试 API：

#### 创建预约会议
```bash
curl -X POST http://localhost:6099/api/meetingReserve/createMeetingReserve \
  -H "Content-Type: application/json" \
  -H "token: YOUR_TOKEN" \
  -d '{
    "meetingName": "测试会议",
    "startTime": 1735660800000,
    "duration": 60,
    "joinType": 0,
    "inviteUserIds": "user1,user2"
  }'
```

#### 查询预约会议列表
```bash
curl -X GET http://localhost:6099/api/meetingReserve/loadMeetingReserveList \
  -H "token: YOUR_TOKEN"
```

#### 取消预约会议
```bash
curl -X POST "http://localhost:6099/api/meetingReserve/cancelMeetingReserve?meetingId=MEETING_ID" \
  -H "token: YOUR_TOKEN"
```

## 预期结果

### 成功场景
1. ✅ 创建会议后，列表中显示新会议
2. ✅ 修改会议后，信息更新
3. ✅ 取消会议后，会议从列表中移除
4. ✅ 被邀请者可以看到会议并加入
5. ✅ 即将开始的会议会收到提醒

### 错误处理
1. ✅ 会议名称为空：提示"请输入会议名称"
2. ✅ 会议名称超过50字符：提示"会议名称长度不能超过50个字符"
3. ✅ 未选择开始时间：提示"请选择开始时间"
4. ✅ 开始时间是过去时间：提示"开始时间必须是未来时间"
5. ✅ 非创建者尝试修改：后端返回权限错误
6. ✅ 非创建者尝试取消：后端返回权限错误

## 数据库验证

### 检查预约会议表
```sql
SELECT * FROM meeting_reserve ORDER BY create_time DESC LIMIT 10;
```

### 检查预约会议成员表
```sql
SELECT * FROM meeting_reserve_member WHERE meeting_id = 'YOUR_MEETING_ID';
```

## 注意事项

1. **时间格式**：前端使用毫秒时间戳，后端转换为 Date 对象
2. **权限控制**：只有创建者可以修改和取消会议
3. **状态管理**：
   - 0: 未开始
   - 1: 进行中
   - 2: 已取消
4. **会议提醒**：每分钟检查一次，提醒 1 小时内开始的会议
5. **默认值**：
   - duration: 60 分钟
   - joinType: 0 (无需密码)
   - joinPassword: null

## 已知限制

1. 会议时长固定为 60 分钟（可以后续扩展）
2. 不支持密码保护（可以后续扩展）
3. 提醒检查间隔为 1 分钟（不可配置）
4. 预约会议列表默认折叠（需要手动展开）

## 下一步优化建议

1. 添加会议时长选择
2. 添加密码保护选项
3. 添加重复会议功能
4. 添加会议日历视图
5. 添加邮件/短信提醒
6. 添加会议记录功能
