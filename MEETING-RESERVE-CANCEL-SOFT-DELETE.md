# 预约会议取消改为逻辑删除

## 需求描述

用户希望取消预约会议后，不是物理删除记录，而是将预约会议的状态改为 3（已取消）。

## 修改内容

### 1. 修改取消预约会议的逻辑

**文件：`src/main/java/com/easymeeting/service/impl/MeetingReserveServiceImpl.java`**

#### 修改前（物理删除）

```java
@Override
@Transactional(rollbackFor =Exception.class)
public void deleteMeetingReserveByMeetingId(String meetingId, String userId) {
    MeetingReserveQuery meetingReserveQuery = new MeetingReserveQuery();
    meetingReserveQuery.setMeetingId(meetingId);
    meetingReserveQuery.setCreateUserId(userId);
    Integer i = this.meetingReserveMapper.deleteByParam(meetingReserveQuery);
    if (i>0){
        MeetingReserveMemberQuery meetingReserveMemberQuery = new MeetingReserveMemberQuery();
        meetingReserveMemberQuery.setMeetingId(meetingId);
        this.meetingReserveMemberMapper.deleteByParam(meetingReserveMemberQuery);
    }
}
```

#### 修改后（逻辑删除）

```java
@Override
@Transactional(rollbackFor =Exception.class)
public void deleteMeetingReserveByMeetingId(String meetingId, String userId) {
    // 逻辑删除：将状态改为已取消(3)，而不是物理删除
    MeetingReserve meetingReserve = this.meetingReserveMapper.selectByMeetingId(meetingId);
    if (meetingReserve == null) {
        throw new BusinessException(ResponseCodeEnum.CODE_600);
    }
    
    // 验证是否是创建者
    if (!meetingReserve.getCreateUserId().equals(userId)) {
        throw new BusinessException("只有创建者可以取消会议");
    }
    
    // 更新预约会议状态为已取消
    MeetingReserve updateBean = new MeetingReserve();
    updateBean.setStatus(MeetingReserveStatusEnum.CANCELLED.getStatus());
    this.meetingReserveMapper.updateByMeetingId(updateBean, meetingId);
    
    log.info("预约会议已取消: meetingId=" + meetingId + ", userId=" + userId);
}
```

### 2. 添加日志支持

添加了 Apache Commons Logging 支持：

```java
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Service("meetingReserveService")
public class MeetingReserveServiceImpl implements MeetingReserveService {
    private static final Log log = LogFactory.getLog(MeetingReserveServiceImpl.class);
    // ...
}
```

### 3. 改进被邀请者离开会议的逻辑

```java
//会议预约被邀者离开
@Override
public void deleteMeetingReserveByUserId(String meetingId, String userId) {
    MeetingReserve meetingReserve = this.meetingReserveMapper.selectByMeetingId(meetingId);
    if (meetingReserve==null){
        throw new BusinessException(ResponseCodeEnum.CODE_600);
    }
    
    // 如果是创建者，则取消整个会议
    if (meetingReserve.getCreateUserId().equals(userId)){
        deleteMeetingReserveByMeetingId(meetingReserve.getMeetingId(), userId);
    } else {
        // 如果是被邀请者，则从成员列表中删除
        MeetingReserveMemberQuery meetingReserveMemberQuery = new MeetingReserveMemberQuery();
        meetingReserveMemberQuery.setMeetingId(meetingId);
        meetingReserveMemberQuery.setInviteUserId(userId);
        this.meetingReserveMemberMapper.deleteByParam(meetingReserveMemberQuery);
        log.info("用户离开预约会议: meetingId=" + meetingId + ", userId=" + userId);
    }
}
```

## 功能说明

### 取消会议的行为

1. **创建者取消会议**
   - 调用 `/api/meetingReserve/cancelMeetingReserve`
   - 将 `meeting_reserve` 表中的 `status` 字段更新为 3（已取消）
   - 不删除 `meeting_reserve` 记录
   - 不删除 `meeting_reserve_member` 记录
   - 记录日志

2. **被邀请者离开会议**
   - 调用 `/api/meetingReserve/leaveMeetingReserve`
   - 如果是创建者：执行取消会议逻辑
   - 如果是被邀请者：从 `meeting_reserve_member` 表中删除该成员记录
   - 记录日志

### 已取消会议的处理

1. **加载预约会议列表**
   - `loadMeetingReserveList` 方法已经过滤了已取消的会议
   - 使用 `statusNotEqual(3)` 排除已取消的会议

2. **获取即将开始的会议**
   - `getUpcomingMeetings` 方法只查询状态为 0（未开始）的会议
   - 已取消的会议不会出现在提醒列表中

3. **前端显示**
   - 已取消的会议不会显示在预约列表中
   - 如果需要显示已取消的会议，可以添加一个"已取消"标签页

## 数据保留的好处

1. **数据完整性**
   - 保留会议历史记录
   - 便于审计和统计

2. **可恢复性**
   - 如果需要，可以恢复已取消的会议
   - 可以查看取消原因和时间

3. **统计分析**
   - 可以统计取消率
   - 分析取消原因
   - 优化会议管理

## 状态说明

`meeting_reserve` 表的 `status` 字段：
- 0 = 未开始 (NO_START)
- 1 = 进行中 (RUNNING)
- 2 = 已结束 (ENDED)
- 3 = 已取消 (CANCELLED)

## 测试建议

### 1. 创建者取消会议

1. 创建一个预约会议
2. 作为创建者点击"取消会议"
3. 检查数据库：
   - `meeting_reserve` 表中该会议的 `status` 应该为 3
   - 记录应该仍然存在
   - `meeting_reserve_member` 表中的成员记录应该仍然存在
4. 检查前端：
   - 该会议不应该出现在预约列表中
   - 该会议不应该出现在即将开始的提醒中

### 2. 被邀请者离开会议

1. 创建一个预约会议并邀请其他用户
2. 作为被邀请者点击"离开会议"
3. 检查数据库：
   - `meeting_reserve` 表中该会议的 `status` 应该仍然为 0
   - `meeting_reserve_member` 表中该用户的记录应该被删除
4. 检查前端：
   - 该会议不应该出现在被邀请者的预约列表中
   - 该会议应该仍然出现在创建者的预约列表中

### 3. 已取消会议的过滤

1. 取消一个预约会议
2. 刷新预约列表
3. 确认已取消的会议不显示
4. 检查后端日志，确认有"预约会议已取消"的日志

## 相关文件

### 修改的文件
- `src/main/java/com/easymeeting/service/impl/MeetingReserveServiceImpl.java` - 取消会议逻辑改为逻辑删除

### 相关文件
- `src/main/java/com/easymeeting/controller/MeetingReserveController.java` - 取消会议 API
- `src/main/java/com/easymeeting/entity/enums/MeetingReserveStatusEnum.java` - 状态枚举
- `frontend/src/components/ReservationList.vue` - 前端预约列表

## 后续优化建议

1. **添加取消原因**
   - 在 `meeting_reserve` 表中添加 `cancel_reason` 字段
   - 取消时记录取消原因
   - 便于分析和改进

2. **添加取消时间**
   - 在 `meeting_reserve` 表中添加 `cancel_time` 字段
   - 记录取消的具体时间
   - 便于统计和分析

3. **通知被邀请者**
   - 会议被取消时，通知所有被邀请者
   - 可以通过 WebSocket 推送通知
   - 或者发送邮件通知

4. **显示已取消的会议**
   - 在前端添加"已取消"标签页
   - 显示已取消的会议列表
   - 提供查看取消原因的功能

5. **恢复已取消的会议**
   - 提供恢复功能
   - 将状态从 3 改回 0
   - 重新激活会议

6. **自动清理**
   - 定期清理过期的已取消会议
   - 例如：取消后 30 天自动删除
   - 或者归档到历史表
