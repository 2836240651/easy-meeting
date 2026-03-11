# 成员列表过滤问题修复总结

## 问题描述

主持人拉黑或踢出用户后，重新点击成员列表时，那些被拉黑或被踢出的用户又重新出现在成员列表中。

## 根本原因

`RedisComponent.getMeetingMemberList()` 方法返回了所有成员，包括被拉黑（status=4）和被踢出（status=3）的用户，没有进行状态过滤。

## 修复方案

### 1. 新增 `getAllMeetingMembers()` 方法

用于获取会议中所有成员（包括所有状态），适用于需要完整成员信息的场景，如清理资源、统计等。

```java
/**
 * 获取会议中所有成员（包括所有状态：正常、退出、踢出、拉黑）
 * 用于需要获取完整成员信息的场景，如清理资源、统计等
 * 
 * @param meetingId 会议ID
 * @return 所有成员列表（按加入时间排序）
 */
public List<MeetingMemberDto> getAllMeetingMembers(String meetingId){
    List<MeetingMemberDto> meetingMemberDtoList =
     redisUtils.hvals(Constants.REDIS_KEY_MEETING_ROOM + meetingId);
     
    log.debug("获取会议所有成员: meetingId={}, 总数={}", meetingId, meetingMemberDtoList.size());
    
    meetingMemberDtoList = meetingMemberDtoList.stream()
     .sorted(Comparator.comparing(MeetingMemberDto::getJoinTime))
     .collect(Collectors.toList());
     
    return meetingMemberDtoList;
}
```

### 2. 修改 `getMeetingMemberList()` 方法

只返回状态为 NORMAL（status=1）的成员，过滤掉被踢出和被拉黑的成员。

```java
/**
 * 获取会议中正常状态的成员列表（过滤掉被踢出和被拉黑的成员）
 * 这是默认的获取成员列表方法，只返回status=1（NORMAL）的成员
 * 
 * @param meetingId 会议ID
 * @return 正常状态的成员列表（按加入时间排序）
 */
public List<MeetingMemberDto> getMeetingMemberList(String meetingId){
    List<MeetingMemberDto> allMembers = getAllMeetingMembers(meetingId);
    
    // 只返回状态为NORMAL的成员，过滤掉被踢出(3)和被拉黑(4)的成员
    List<MeetingMemberDto> normalMembers = allMembers.stream()
     .filter(member -> MeetingMemberStatusEnum.NORMAL.getStatus().equals(member.getStatus()))
     .collect(Collectors.toList());
     
    log.info("获取会议正常成员: meetingId={}, 总成员数={}, 正常成员数={}", 
             meetingId, allMembers.size(), normalMembers.size());
    
    // 记录被过滤掉的成员（调试用）
    if (allMembers.size() > normalMembers.size()) {
        allMembers.stream()
            .filter(member -> !MeetingMemberStatusEnum.NORMAL.getStatus().equals(member.getStatus()))
            .forEach(member -> log.debug("过滤掉非正常成员: userId={}, nickName={}, status={}", 
                     member.getUserId(), member.getNickName(), member.getStatus()));
    }
    
    return normalMembers;
}
```

### 3. 增强 `exitMeeting()` 方法日志

添加详细日志记录状态变更，便于调试和追踪问题。

```java
public Boolean exitMeeting(String meetingId, String userId, MeetingMemberStatusEnum statusEnum){
    MeetingMemberDto memberDto = getMeetingMember(meetingId, userId);
    if (memberDto==null){
        log.warn("退出会议失败，成员不存在: meetingId={}, userId={}", meetingId, userId);
        return false;
    }
    
    Integer oldStatus = memberDto.getStatus();
    memberDto.setStatus(statusEnum.getStatus());
    add2meeting(meetingId, memberDto);
    
    log.info("更新成员状态: meetingId={}, userId={}, nickName={}, oldStatus={}, newStatus={}", 
             meetingId, userId, memberDto.getNickName(), oldStatus, statusEnum.getStatus());
    
    return true;
}
```

### 4. 修改 `removeAllMeetingMember()` 方法

使用 `getAllMeetingMembers()` 获取所有成员（包括非正常状态），确保清理时不遗漏。

```java
public void removeAllMeetingMember(String meetingId) {
    // 这里需要获取所有成员（包括非正常状态），因为要清理所有数据
    List<MeetingMemberDto> meetingMemberList = getAllMeetingMembers(meetingId);
    List<String> userIdList  = meetingMemberList.stream().map(MeetingMemberDto::getUserId).
    collect(Collectors.toList());
    if (userIdList.isEmpty()){
        log.info("会议无成员，无需清理: meetingId={}", meetingId);
        return;
    }
    log.info("清理会议所有成员: meetingId={}, 成员数={}", meetingId, userIdList.size());
    redisUtils.hdel(Constants.REDIS_KEY_MEETING_ROOM+meetingId,userIdList.toArray(new String[userIdList.size()]));
}
```

### 5. 添加日志支持

在类上添加 `@Slf4j` 注解，启用日志功能。

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RedisComponent {
    // ...
}
```

## 修复效果

### 修复前

```
主持人拉黑用户A
→ Redis中用户A的status更新为4 ✅
→ 主持人点击成员列表
→ 后端返回所有成员（包括用户A，status=4）
→ 前端过滤status=1的成员
→ 如果前端过滤失效或Redis状态未正确更新，用户A会重新出现 ❌
```

### 修复后

```
主持人拉黑用户A
→ Redis中用户A的status更新为4 ✅
→ 日志：更新成员状态: userId=xxx, oldStatus=1, newStatus=4 ✅
→ 主持人点击成员列表
→ 后端只返回status=1的成员（用户A被过滤掉）✅
→ 日志：获取会议正常成员: 总成员数=5, 正常成员数=4 ✅
→ 日志：过滤掉非正常成员: userId=xxx, status=4 ✅
→ 前端显示正常成员列表（不包括用户A）✅
```

## 优点

1. **统一过滤**：在后端统一过滤，所有调用 `getMeetingMemberList()` 的地方都生效
2. **减少传输**：减少网络传输的数据量，不传输被拉黑/踢出的成员
3. **向后兼容**：新增方法保持向后兼容，不影响现有代码
4. **灵活性高**：可以根据需要选择获取所有成员或只获取正常成员
5. **便于调试**：详细的日志记录，便于追踪问题和调试

## 影响范围

### 直接影响的调用点

根据代码搜索，以下地方调用了 `getMeetingMemberList()`：

1. **MeetingInfoServiceImpl.joinMeeting()** - 用户加入会议时
   - 影响：只返回正常成员 ✅
   - 结果：新用户加入时，看不到被拉黑/踢出的成员

2. **MeetingInfoServiceImpl.exitMeetingRoom()** - 用户退出会议时
   - 影响：只返回正常成员 ✅
   - 结果：退出消息中不包含被拉黑/踢出的成员

3. **MeetingInfoServiceImpl.finishMeeting()** - 结束会议时
   - 影响：只返回正常成员 ✅
   - 结果：结束会议通知只发给正常成员（被拉黑的不需要通知）

4. **ChannelContextUtils** - WebSocket消息处理
   - 影响：只返回正常成员 ✅
   - 结果：WebSocket推送的成员列表不包含被拉黑/踢出的成员

5. **RedisComponent.removeAllMeetingMember()** - 清理所有成员
   - 影响：已修改为使用 `getAllMeetingMembers()` ✅
   - 结果：清理时包含所有成员，不遗漏

### 无影响的场景

- 数据库操作：不受影响，数据库中的状态仍然正确保存
- 拉黑检查：`checkMeetingJoin()` 仍然从数据库检查拉黑状态
- 单个成员查询：`getMeetingMember()` 不受影响

## 测试建议

### 测试场景1：拉黑后成员列表

1. 用户A和用户B加入会议
2. 主持人拉黑用户B
3. 主持人点击成员列表
4. 验证：
   - ✅ 成员列表中不显示用户B
   - ✅ 后端日志显示"过滤掉非正常成员: userId=B, status=4"
   - ✅ 用户B无法重新加入会议

### 测试场景2：踢出后成员列表

1. 用户A和用户B加入会议
2. 主持人踢出用户B
3. 主持人点击成员列表
4. 验证：
   - ✅ 成员列表中不显示用户B
   - ✅ 后端日志显示"过滤掉非正常成员: userId=B, status=3"
   - ✅ 用户B可以重新加入会议（踢出不是拉黑）

### 测试场景3：新用户加入

1. 用户A被拉黑
2. 用户B加入会议
3. 验证：
   - ✅ 用户B看到的成员列表中不包括用户A
   - ✅ 后端日志显示正确的成员数量

### 测试场景4：结束会议

1. 会议中有正常成员和被拉黑成员
2. 主持人结束会议
3. 验证：
   - ✅ 所有正常成员收到结束通知
   - ✅ 被拉黑成员不收到通知（已经不在会议中）
   - ✅ Redis中的所有成员数据被清理

## 日志示例

### 拉黑用户时

```
INFO  - 更新成员状态: meetingId=abc123, userId=user001, nickName=张三, oldStatus=1, newStatus=4
```

### 获取成员列表时

```
DEBUG - 获取会议所有成员: meetingId=abc123, 总数=5
INFO  - 获取会议正常成员: meetingId=abc123, 总成员数=5, 正常成员数=4
DEBUG - 过滤掉非正常成员: userId=user001, nickName=张三, status=4
```

### 清理会议成员时

```
INFO  - 清理会议所有成员: meetingId=abc123, 成员数=5
```

## 相关文件

- `src/main/java/com/easymeeting/redis/RedisComponent.java` - 修改的核心文件
- `src/main/java/com/easymeeting/entity/enums/MeetingMemberStatusEnum.java` - 状态枚举定义

## 状态枚举说明

```java
public enum MeetingMemberStatusEnum {
    DEL_MEETING(0, "删除会议"),
    NORMAL(1, "正常"),           // ✅ 只有这个状态会在成员列表中显示
    EXIT_MEETING(2, "退出会议"),
    KICK_OUT(3, "被踢出会议"),    // ❌ 被过滤
    BLACKLIST(4, "被拉黑");       // ❌ 被过滤
}
```

## 总结

通过在 `RedisComponent.getMeetingMemberList()` 方法中添加状态过滤逻辑，确保只返回正常状态的成员，从根本上解决了被拉黑/踢出用户重新出现在成员列表中的问题。同时新增 `getAllMeetingMembers()` 方法保持灵活性，并添加详细日志便于调试和追踪问题。

修复后，主持人点击成员列表时，只会看到正常状态的成员，被拉黑和被踢出的用户不会再出现。
