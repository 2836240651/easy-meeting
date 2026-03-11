# 拉黑状态被重置Bug修复

## 问题描述

主持人拉黑用户后：
1. ✅ 用户收到消息并退出会议
2. ✅ 数据库中用户状态应该更新为4（BLACKLIST）
3. ❌ 但实际数据库中用户状态仍然是1（NORMAL）
4. ❌ 被拉黑用户可以重新加入会议

## 问题分析

### 根本原因

在`addMeetingMember`方法中使用了`insertOrUpdate`操作：

```java
private void addMeetingMember(String meetingId, String userId, String nickName, Integer memberType){
    MeetingMember meetingMember = new MeetingMember();
    meetingMember.setMeetingId(meetingId);
    meetingMember.setUserId(userId);
    meetingMember.setNickName(nickName);
    meetingMember.setMemberType(memberType);
    meetingMember.setLastJoinTime(new Date());
    meetingMember.setMeetingStatus(MeetingStatusEnum.RUNING.getStatus());
    meetingMember.setStatus(MeetingMemberStatusEnum.NORMAL.getStatus());  // ❌ 总是设置为NORMAL
    this.meetingMemberMapper.insertOrUpdate(meetingMember);  // ❌ 会覆盖拉黑状态
}
```

### 问题流程

1. **用户首次加入会议**
   ```
   addMeetingMember() → insertOrUpdate() → INSERT
   数据库：status = 1 (NORMAL)
   ```

2. **主持人拉黑用户**
   ```
   forceExitMeeting() → exitMeetingRoom() → updateByMeetingIdAndUserId()
   数据库：status = 4 (BLACKLIST)  ✅ 正确更新
   ```

3. **被拉黑用户尝试重新加入**
   ```
   joinMeeting() → checkMeetingJoin() → 检查通过（因为Redis可能已清除）
   → addMeetingMember() → insertOrUpdate()
   数据库：status = 1 (NORMAL)  ❌ 拉黑状态被覆盖！
   ```

### insertOrUpdate的行为

```sql
-- MyBatis的insertOrUpdate通常实现为：
INSERT INTO meeting_member (meeting_id, user_id, status, ...)
VALUES (?, ?, 1, ...)
ON DUPLICATE KEY UPDATE
    status = 1,  -- ❌ 这里会覆盖拉黑状态
    nick_name = ?,
    last_join_time = ?
```

## 解决方案

### 方案1：在addMeetingMember中检查拉黑状态（已采用）

```java
private void addMeetingMember(String meetingId, String userId, String nickName, Integer memberType){
    // 先检查用户是否已存在且被拉黑
    MeetingMember existingMember = meetingMemberMapper.selectByMeetingIdAndUserId(meetingId, userId);
    if (existingMember != null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(existingMember.getStatus())) {
        // 用户已被拉黑，不允许加入
        throw new BusinessException("你已经被拉黑无法加入会议");
    }
    
    MeetingMember meetingMember = new MeetingMember();
    meetingMember.setMeetingId(meetingId);
    meetingMember.setUserId(userId);
    meetingMember.setNickName(nickName);
    meetingMember.setMemberType(memberType);
    meetingMember.setLastJoinTime(new Date());
    meetingMember.setMeetingStatus(MeetingStatusEnum.RUNING.getStatus());
    meetingMember.setStatus(MeetingMemberStatusEnum.NORMAL.getStatus());
    this.meetingMemberMapper.insertOrUpdate(meetingMember);
}
```

**优点**：
- 在数据库操作前就阻止拉黑用户加入
- 双重保险（checkMeetingJoin + addMeetingMember）
- 即使checkMeetingJoin失效，这里也能拦截

### 方案2：修改insertOrUpdate逻辑（未采用）

修改Mapper XML，只在INSERT时设置status，UPDATE时不更新status：

```xml
<insert id="insertOrUpdate">
    INSERT INTO meeting_member (meeting_id, user_id, status, ...)
    VALUES (#{meetingId}, #{userId}, #{status}, ...)
    ON DUPLICATE KEY UPDATE
        nick_name = #{nickName},
        last_join_time = #{lastJoinTime}
        -- 不更新status字段
</insert>
```

**缺点**：
- 需要修改Mapper XML
- 可能影响其他使用insertOrUpdate的地方

## 完整的拉黑防护机制

现在有三层防护：

### 第1层：checkMeetingJoin（Redis + 数据库）

```java
private void checkMeetingJoin(String meetingId, String userId){
    // 检查Redis中的状态
    MeetingMemberDto meetingMember = redisComponent.getMeetingMember(meetingId, userId);
    if (meetingMember!=null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(meetingMember.getStatus())){
        throw new BusinessException("你已经被拉黑无法加入会议");
    }
    
    // 检查数据库中的状态
    MeetingMember dbMeetingMember = meetingMemberMapper.selectByMeetingIdAndUserId(meetingId, userId);
    if (dbMeetingMember != null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(dbMeetingMember.getStatus())) {
        throw new BusinessException("你已经被拉黑无法加入会议");
    }
}
```

### 第2层：addMeetingMember（数据库）

```java
private void addMeetingMember(String meetingId, String userId, String nickName, Integer memberType){
    // 再次检查数据库中的拉黑状态
    MeetingMember existingMember = meetingMemberMapper.selectByMeetingIdAndUserId(meetingId, userId);
    if (existingMember != null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(existingMember.getStatus())) {
        throw new BusinessException("你已经被拉黑无法加入会议");
    }
    // ...
}
```

### 第3层：exitMeetingRoom（持久化拉黑状态）

```java
if (ArrayUtils.contains(new Integer[]{
    MeetingMemberStatusEnum.KICK_OUT.getStatus(),
    MeetingMemberStatusEnum.BLACKLIST.getStatus()
}, statusEnum.getStatus())){
    MeetingMember meetingMember = new MeetingMember();
    meetingMember.setStatus(statusEnum.getStatus());
    meetingMember.setUserId(userId);
    log.info("用户被踢出或是被拉黑会议成员状态更新中-------------");
    meetingMemberMapper.updateByMeetingIdAndUserId(meetingMember, meetingId, userId);
}
```

## 测试场景

### 场景1：正常拉黑流程

1. 用户A和用户B加入会议
2. 主持人拉黑用户B
3. 验证：
   - ✅ 用户B收到强制下线消息
   - ✅ 用户B退出会议
   - ✅ 数据库中用户B的status = 4（BLACKLIST）
   - ✅ Redis中用户B的status = 4（BLACKLIST）

### 场景2：拉黑后尝试重新加入

1. 用户B被拉黑后尝试重新加入会议
2. 验证：
   - ✅ checkMeetingJoin检查Redis，发现被拉黑
   - ✅ 抛出异常："你已经被拉黑无法加入会议"
   - ✅ 用户B无法进入会议页面

### 场景3：Redis数据丢失后尝试加入

1. 用户B被拉黑
2. 手动清除Redis中的会议数据
3. 用户B尝试重新加入会议
4. 验证：
   - ✅ checkMeetingJoin检查Redis，未找到数据
   - ✅ checkMeetingJoin检查数据库，发现status = 4
   - ✅ 抛出异常："你已经被拉黑无法加入会议"
   - ✅ 用户B无法进入会议页面

### 场景4：checkMeetingJoin失效（极端情况）

1. 假设checkMeetingJoin方法被绕过或失效
2. 用户B尝试加入会议
3. 验证：
   - ✅ addMeetingMember检查数据库，发现status = 4
   - ✅ 抛出异常："你已经被拉黑无法加入会议"
   - ✅ 用户B无法进入会议页面

## 相关文件

- `src/main/java/com/easymeeting/service/impl/MeetingInfoServiceImpl.java`
  - `addMeetingMember()` - 添加了拉黑检查
  - `checkMeetingJoin()` - 修复了状态比较逻辑
  - `exitMeetingRoom()` - 更新数据库拉黑状态

## 关键点

1. **insertOrUpdate的陷阱**：会覆盖现有记录的所有字段，包括status
2. **多层防护**：不要依赖单一检查点，要有多层防护
3. **数据一致性**：Redis和数据库都要检查和更新
4. **异常处理**：业务异常要重新抛出，不能被吞掉
5. **日志记录**：关键操作要记录日志，便于追踪问题

## 总结

拉黑状态被重置的根本原因是`insertOrUpdate`操作会覆盖现有记录的status字段。通过在`addMeetingMember`方法中添加拉黑检查，在数据库操作前就阻止被拉黑用户加入会议，确保拉黑状态不会被覆盖。

现在系统有三层防护机制，即使某一层失效，其他层也能确保被拉黑用户无法重新加入会议。
