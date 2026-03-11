# 会议提醒发送到收件箱功能实现

## 功能概述

实现了会议提醒功能，在预约会议开始前 5 分钟自动向所有参与者的收件箱发送提醒通知。

## 用户需求

- 前端预约会议的倒计时提醒（60分钟、15分钟、5分钟）不仅弹窗提示，还要发送到用户的收件箱
- 只在倒计时 5 分钟时发送一次到收件箱（不是 60 分钟或 15 分钟）

## 实现方案

采用后端定时任务方式，每分钟检查一次即将开始的会议，在开始前 5 分钟为所有参与者创建提醒通知。

### 技术架构

1. **定时任务**：使用 Spring `@Scheduled` 注解，每分钟执行一次
2. **Redis 标记**：使用 Redis 存储已发送提醒的会议 ID，避免重复发送
3. **通知系统**：复用现有的统一收件箱系统
4. **WebSocket 推送**：实时推送通知给在线用户

## 代码修改

### 1. 新增通知类型

**文件**: `src/main/java/com/easymeeting/entity/enums/NotificationTypeEnum.java`

```java
// 会议类消息（5-11）
MEETING_INVITE_PENDING(5, "会议邀请待处理"),
MEETING_INVITE_ACCEPTED(6, "会议邀请已接受"),
MEETING_INVITE_REJECTED(7, "会议邀请已拒绝"),
MEETING_CANCELLED(8, "会议取消通知"),
MEETING_TIME_CHANGED(9, "会议时间变更通知"),
MEETING_INSTANT_INVITE(10, "即时会议邀请"),
MEETING_REMINDER(11, "会议提醒"),  // 新增

// 系统消息（12-13）
SYSTEM_NOTIFICATION(12, "系统通知"),
SYSTEM_MAINTENANCE(13, "维护通知");
```

### 2. 添加服务接口方法

**文件**: `src/main/java/com/easymeeting/service/UserNotificationService.java`

```java
/**
 * 创建会议提醒通知（会议开始前 5 分钟）
 * @param meetingId 会议ID
 * @param userId 用户ID
 * @param meetingName 会议名称
 * @param startTime 会议开始时间
 */
void createMeetingReminderNotification(String meetingId, String userId, 
                                      String meetingName, Date startTime);
```

### 3. 实现服务方法

**文件**: `src/main/java/com/easymeeting/service/impl/UserNotificationServiceImpl.java`

```java
@Override
public void createMeetingReminderNotification(String meetingId, String userId, 
                                              String meetingName, Date startTime) {
    logger.info("创建会议提醒通知: meetingId=" + meetingId + ", userId=" + userId);
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    // 创建通知
    UserNotification notification = new UserNotification();
    notification.setUserId(userId);
    notification.setNotificationType(NotificationTypeEnum.MEETING_REMINDER.getType());
    notification.setTitle("会议提醒");
    notification.setContent("您预约的会议「" + meetingName + "」将在 5 分钟后开始，开始时间：" + sdf.format(startTime));
    notification.setStatus(0); // 未读
    notification.setActionRequired(0); // 不需要操作
    notification.setActionStatus(0);
    notification.setReferenceId(meetingId);
    notification.setCreateTime(new Date());
    notification.setUpdateTime(new Date());
    
    this.userNotificationMapper.insert(notification);
    
    // 发送 WebSocket 通知
    try {
        MessageSendDto messageDto = new MessageSendDto();
        messageDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
        messageDto.setReceiveUserId(userId);
        messageDto.setMessageType(MessageTypeEnum.SYSTEM_NOTIFICATION.getType());
        messageDto.setMessageContent("您有一个会议即将开始");
        
        channelContextUtils.sendMessage(messageDto);
        logger.info("已发送会议提醒 WebSocket 通知给用户: " + userId);
    } catch (Exception e) {
        logger.error("发送会议提醒 WebSocket 通知失败", e);
    }
}
```

### 4. 添加定时任务

**文件**: `src/main/java/com/easymeeting/task/MeetingTimeoutTask.java`

新增 `checkMeetingReminder()` 方法：

```java
/**
 * 每分钟执行一次，检查即将开始的会议并发送提醒
 * cron表达式: 秒 分 时 日 月 周
 * 0 * * * * ? 表示每分钟的第0秒执行
 */
@Scheduled(cron = "0 * * * * ?")
public void checkMeetingReminder() {
    try {
        log.debug("开始检查会议提醒...");
        
        long currentTime = System.currentTimeMillis();
        long reminderTime = currentTime + REMINDER_MINUTES_BEFORE * 60 * 1000; // 5分钟后
        
        // 查询所有未开始的预约会议（status = 0）
        List<MeetingReserve> upcomingMeetings = meetingReserveMapper.selectUpcomingMeetings(
            new Date(currentTime), 
            new Date(reminderTime)
        );
        
        if (upcomingMeetings == null || upcomingMeetings.isEmpty()) {
            log.debug("当前没有即将开始的会议需要提醒");
            return;
        }
        
        int reminderCount = 0;
        
        for (MeetingReserve meeting : upcomingMeetings) {
            String meetingId = meeting.getMeetingId();
            
            // 检查是否已发送过提醒（使用 Redis 标记）
            String reminderKey = REMINDER_SENT_KEY_PREFIX + meetingId;
            String alreadySent = (String) redisUtils.get(reminderKey);
            
            if (alreadySent != null) {
                log.debug("会议 {} 已发送过提醒，跳过", meetingId);
                continue;
            }
            
            // 查询会议的所有参与者
            MeetingReserveMemberQuery memberQuery = new MeetingReserveMemberQuery();
            memberQuery.setMeetingId(meetingId);
            List<MeetingReserveMember> members = meetingReserveMemberMapper.selectList(memberQuery);
            
            if (members == null || members.isEmpty()) {
                log.debug("会议 {} 没有参与者，跳过提醒", meetingId);
                continue;
            }
            
            // 为每个参与者创建提醒通知
            for (MeetingReserveMember member : members) {
                try {
                    userNotificationService.createMeetingReminderNotification(
                        meetingId,
                        member.getInviteUserId(),
                        meeting.getMeetingName(),
                        meeting.getStartTime()
                    );
                    reminderCount++;
                } catch (Exception e) {
                    log.error("为用户 {} 创建会议提醒通知失败", member.getInviteUserId(), e);
                }
            }
            
            // 标记该会议已发送提醒（设置过期时间为 24 小时）
            redisUtils.setEx(reminderKey, "1", 24 * 60 * 60L);
            
            log.info("会议 {} 提醒通知已发送，参与者数量: {}", meetingId, members.size());
        }
        
        if (reminderCount > 0) {
            log.info("本次检查共发送 {} 条会议提醒通知", reminderCount);
        } else {
            log.debug("本次检查没有需要发送的会议提醒");
        }
        
    } catch (Exception e) {
        log.error("检查会议提醒时发生错误", e);
    }
}
```

### 5. 添加数据库查询方法

**文件**: `src/main/java/com/easymeeting/mappers/MeetingReserveMapper.java`

```java
/**
 * 查询即将开始的会议（用于发送提醒）
 * @param startTimeFrom 开始时间范围起点
 * @param startTimeTo 开始时间范围终点
 * @return 会议列表
 */
List<T> selectUpcomingMeetings(@Param("startTimeFrom") Date startTimeFrom, 
                                @Param("startTimeTo") Date startTimeTo);
```

**文件**: `src/main/resources/com/easymeeting/mappers/MeetingReserveMapper.xml`

```xml
<!-- 查询即将开始的会议（用于发送提醒）-->
<select id="selectUpcomingMeetings" resultMap="base_result_map">
    SELECT <include refid="base_column_list" />
    FROM meeting_reserve m
    WHERE m.status = 0
      AND m.start_time >= #{startTimeFrom}
      AND m.start_time &lt;= #{startTimeTo}
    ORDER BY m.start_time ASC
</select>
```

## 工作流程

1. **定时任务触发**：每分钟的第 0 秒执行一次
2. **查询即将开始的会议**：查询开始时间在当前时间到 5 分钟后之间的会议
3. **检查 Redis 标记**：判断该会议是否已发送过提醒
4. **查询参与者**：从 `meeting_reserve_member` 表获取所有参与者
5. **创建通知**：为每个参与者创建一条通知记录
6. **WebSocket 推送**：如果用户在线，实时推送通知
7. **标记已发送**：在 Redis 中标记该会议已发送提醒（24小时过期）

## 关键特性

### 1. 避免重复发送

使用 Redis 存储已发送提醒的会议 ID：
- Key: `meeting:reminder:sent:{meetingId}`
- Value: `"1"`
- 过期时间: 24 小时

### 2. 精确时间控制

- 查询条件：`start_time >= 当前时间 AND start_time <= 当前时间 + 5分钟`
- 只在会议开始前 5 分钟内发送一次

### 3. 通知内容

```
标题: 会议提醒
内容: 您预约的会议「{会议名称}」将在 5 分钟后开始，开始时间：{yyyy-MM-dd HH:mm}
```

### 4. 通知属性

- `notificationType`: 11 (MEETING_REMINDER)
- `status`: 0 (未读)
- `actionRequired`: 0 (不需要操作)
- `referenceId`: 会议ID

## 前端显示

前端 `Dashboard.vue` 中的收件箱会自动显示会议提醒通知：

- 在"会议"标签页中显示
- 显示会议名称和开始时间
- 点击可查看会议详情
- 支持标记为已读

## 测试建议

1. **创建测试会议**：创建一个开始时间在 5 分钟后的预约会议
2. **等待提醒**：等待定时任务执行（每分钟执行一次）
3. **检查收件箱**：查看用户收件箱是否收到提醒通知
4. **验证 WebSocket**：如果用户在线，应该实时收到通知
5. **验证不重复**：确认同一会议只发送一次提醒

## 日志输出

```
[INFO] 开始检查会议提醒...
[INFO] 创建会议提醒通知: meetingId=xxx, userId=xxx
[INFO] 已发送会议提醒 WebSocket 通知给用户: xxx
[INFO] 会议 xxx 提醒通知已发送，参与者数量: 3
[INFO] 本次检查共发送 3 条会议提醒通知
```

## 注意事项

1. **定时任务频率**：每分钟执行一次，确保不会错过提醒时间
2. **Redis 依赖**：需要 Redis 服务正常运行
3. **时区问题**：确保服务器时区与数据库时区一致
4. **会议状态**：只查询 `status = 0`（未开始）的会议
5. **过期清理**：Redis 标记 24 小时后自动过期

## 后续优化建议

1. **可配置提醒时间**：允许用户自定义提醒时间（如 10 分钟、15 分钟）
2. **多次提醒**：支持多个时间点的提醒（如 60 分钟、15 分钟、5 分钟都发送）
3. **提醒方式**：支持邮件、短信等多种提醒方式
4. **提醒偏好**：允许用户设置是否接收会议提醒

## 部署状态

- ✅ 后端代码已编译
- ✅ 后端服务已重启
- ✅ 定时任务已启用
- ✅ 通知类型已更新
- ✅ 数据库查询已添加

## 相关文件

- `src/main/java/com/easymeeting/entity/enums/NotificationTypeEnum.java`
- `src/main/java/com/easymeeting/service/UserNotificationService.java`
- `src/main/java/com/easymeeting/service/impl/UserNotificationServiceImpl.java`
- `src/main/java/com/easymeeting/task/MeetingTimeoutTask.java`
- `src/main/java/com/easymeeting/mappers/MeetingReserveMapper.java`
- `src/main/resources/com/easymeeting/mappers/MeetingReserveMapper.xml`
- `frontend/src/views/Dashboard.vue` (前端收件箱)
- `frontend/src/components/MeetingReminder.vue` (前端弹窗提醒)
