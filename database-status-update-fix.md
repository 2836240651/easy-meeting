# 拉黑用户数据库状态未更新问题修复

## 问题描述

主持人拉黑用户后：
- ✅ Redis中用户状态更新为4（BLACKLIST）
- ❌ 数据库中用户状态仍然是1（NORMAL）
- ❌ 被拉黑用户可以重新加入会议，没有提示"已被拉黑无法进入"

## 根本原因

在 `exitMeetingRoom` 方法中，数据库更新的代码放在方法的最后，但在此之前有多个 `return` 语句可能导致方法提前返回，从而跳过数据库更新。

### 原始代码问题

```java
@Override
public void exitMeetingRoom(TokenUserInfoDto tokenUserInfoDto, MeetingMemberStatusEnum statusEnum) {
    String meetingId = tokenUserInfoDto.getCurrentMeetingId();
    if (StringUtils.isEmpty(meetingId)) {
        return;  // ❌ 提前返回1
    }
    String userId = tokenUserInfoDto.getUserId();
    Boolean exit = redisComponent.exitMeeting(meetingId, userId, statusEnum);
    if (!exit){
        tokenUserInfoDto.setCurrentMeetingId(null);
        redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);
        return;  // ❌ 提前返回2：Redis中没有找到成员
    }
    
    // ... 发送消息 ...
    
    // 检查是否还有在线成员
    List<MeetingMemberDto> onlineMemeberList = ...;
    if (onlineMemeberList.isEmpty()){
        MeetingReserve meetingReserve = this.meetingReserveMapper.selectByMeetingIdmeetingId);
        if (meetingReserve==null){
            finishMeeting(meetingId, null);
            return;  // ❌ 提前返回3：没有预约会议
        }
        if (System.currentTimeMillis()>meetingReserve.getStartTime().getTime()+...){
            finishMeeting(meetingId,null);
            return;  // ❌ 提前返回4：会议超时
        }
    }

    // ❌ 数据库更新在最后，可能永远不会执行！
    if (ArrayUtils.contains(new Integer[]{...},statusEnum.getStatus())){
        MeetingMember meetingMember = new MeetingMember();
        meetingMember.setStatus(statusEnum.getStatus());
        meetingMember.setUserId(userId);
        log.info("用户被踢出或是被拉黑会议成员状态更新中-------------");
        meetingMemberMapper.updateByMeetingIdAndUserId(meetingMember,meetingId,userId);
    }
}
```

### 问题场景

**场景1：会议中只有两个人（主持人和被拉黑用户）**

1. 主持人拉黑用户A
2. 调用 `exitMeetingRoom(tokenByUserId, BLACKLIST)`
3. Redis状态更新为4 ✅
4. 发送退出消息
5. 检查在线成员：只剩主持人1人
6. 如果没有预约会议，调用 `finishMeeting()` 并 `return` ❌
7. **数据库更新代码被跳过！** ❌
8. 数据库中用户A的status仍然是1 ❌

**场景2：Redis中没有找到成员**

1. 主持人拉黑用户A
2. 调用 `exitMeetingRoom(tokenByUserId, BLACKLIST)`
3. `redisComponent.exitMeeting()` 返回 false（Redis中没有找到）
4. 方法提前 `return` ❌
5. **数据库更新代码被跳过！** ❌
6. 数据库中用户A的status仍然是1 ❌

## 解决方案

将数据库更新移到方法的最前面，确保在任何 `return` 之前就完成数据库状态的持久化。

### 修复后的代码

```java
@Override
public void exitMeetingRoom(TokenUserInfoDto tokenUserInfoDto, MeetingMemberStatusEnum statusEnum) {
    String meetingId = tokenUserInfoDto.getCurrentMeetingId();
    if (StringUtils.isEmpty(meetingId)) {
        return;
    }
    String userId = tokenUserInfoDto.getUserId();
    
    // ✅ 先更新数据库状态（对于踢出和拉黑操作，必须先持久化到数据库）
    if (ArrayUtils.contains(new Integer[]{
        MeetingMemberStatusEnum.KICK_OUT.getStatus(),
        MeetingMemberStatusEnum.BLACKLIST.getStatus()
    }, statusEnum.getStatus())){
        MeetingMember meetingMember = new MeetingMember();
        meetingMember.setStatus(statusEnum.getStatus());
        meetingMember.setUserId(userId);
        log.info("用户被踢出或被拉黑，更新数据库状态: meetingId=" + meetingId + 
                 ", userId=" + userId + ", status=" + statusEnum.getStatus());
        meetingMemberMapper.updateByMeetingIdAndUserId(meetingMember, meetingId, userId);
        log.info("数据库状态更新完成");
    }
    
    // ✅ 再更新Redis状态
    Boolean exit = redisComponent.exitMeeting(meetingId, userId, statusEnum);
    if (!exit){
        log.warn("Redis中未找到成员，但数据库已更新: meetingId=" + meetingId + 
                 ", userId=" + userId);
        tokenUserInfoDto.setCurrentMeetingId(null);
        redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);
        return;  // ✅ 即使提前返回，数据库也已经更新了
    }
    
    // 发送退出消息
    MessageSendDto messageSendDto = new MessageSendDto();
    messageSendDto.setMessageType(MessageTypeEnum.EXIT_MEETING_ROOM.getType());
    tokenUserInfoDto.setCurrentMeetingId(null);
    redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);
    List<MeetingMemberDto> meetingMemberList = redisComponent.getMeetingMemberList(meetingId);
    MeetingExitDto exitDto = new MeetingExitDto();
    exitDto.setMeetingMemberDtoList(meetingMemberList);
    exitDto.setExitStatus(statusEnum.getStatus());
    exitDto.setExitUserId(userId);
    messageSendDto.setMessageContent(JsonUtils.convertObj2Json(exitDto));
    messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.GROUP.getType());
    messageHandler.sendMessage(messageSendDto);

    // 检查是否还有在线成员
    List<MeetingMemberDto> onlineMemeberList = meetingMemberList.stream()
        .filter(item -> MeetingMemberStatusEnum.NORMAL.getStatus().equals(item.getStatus()))
        .collect(Collectors.toList());
    if (onlineMemeberList.isEmpty()){
        MeetingReserve meetingReserve = this.meetingReserveMapper.selectByMeetingId(meetingId);
        if (meetingReserve==null){
            finishMeeting(meetingId, null);
            return;  // ✅ 即使提前返回，数据库也已经更新了
        }
        if (System.currentTimeMillis()>meetingReserve.getStartTime().getTime()+
            meetingReserve.getDuration()*60*1000){
            finishMeeting(meetingId,null);
            return;  // ✅ 即使提前返回，数据库也已经更新了
        }
    }
}
```

## 修复效果

### 修复前

```
主持人拉黑用户A（会议中只有两人）
→ Redis：用户A的status = 4 ✅
→ 检查在线成员：只剩主持人
→ 调用finishMeeting()并return ❌
→ 数据库更新被跳过 ❌
→ 数据库：用户A的status = 1 ❌
→ 用户A尝试重新加入会议
→ checkMeetingJoin检查数据库：status = 1 ❌
→ 用户A成功加入会议 ❌
```

### 修复后

```
主持人拉黑用户A（会议中只有两人）
→ 数据库：用户A的status = 4 ✅（先更新）
→ 日志：用户被踢出或被拉黑，更新数据库状态 ✅
→ 日志：数据库状态更新完成 ✅
→ Redis：用户A的status = 4 ✅
→ 检查在线成员：只剩主持人
→ 调用finishMeeting()并return ✅
→ 用户A尝试重新加入会议
→ checkMeetingJoin检查数据库：status = 4 ✅
→ 抛出异常："你已经被拉黑无法加入会议" ✅
→ 用户A无法加入会议 ✅
```

## 关键改进

1. **数据库优先**：先更新数据库，再更新Redis，确保持久化数据的可靠性
2. **提前执行**：在任何可能的 `return` 之前就完成数据库更新
3. **详细日志**：添加日志记录数据库更新的开始和完成，便于调试
4. **容错处理**：即使Redis操作失败，数据库状态也已经正确更新

## 日志示例

### 拉黑用户时

```
INFO - 用户被踢出或被拉黑，更新数据库状态: meetingId=lcD7kRtSrU, userId=6cq7Pg48b4Rq, status=4
INFO - 数据库状态更新完成
INFO - 更新成员状态: meetingId=lcD7kRtSrU, userId=6cq7Pg48b4Rq, nickName=张三, oldStatus=1, newStatus=4
```

### Redis中没有找到成员时

```
INFO - 用户被踢出或被拉黑，更新数据库状态: meetingId=lcD7kRtSrU, userId=6cq7Pg48b4Rq, status=4
INFO - 数据库状态更新完成
WARN - Redis中未找到成员，但数据库已更新: meetingId=lcD7kRtSrU, userId=6cq7Pg48b4Rq
```

## 测试步骤

### 测试1：正常拉黑流程

1. 两个用户加入会议
2. 主持人拉黑用户B
3. 验证：
   - ✅ 后端日志显示"用户被踢出或被拉黑，更新数据库状态"
   - ✅ 后端日志显示"数据库状态更新完成"
   - ✅ 数据库中用户B的status = 4
   - ✅ Redis中用户B的status = 4
4. 用户B尝试重新加入会议
5. 验证：
   - ✅ 系统提示"你已经被拉黑无法加入会议"
   - ✅ 用户B无法进入会议

### 测试2：只有两人的会议

1. 主持人和用户B加入会议（只有两人）
2. 主持人拉黑用户B
3. 验证：
   - ✅ 后端日志显示"数据库状态更新完成"
   - ✅ 数据库中用户B的status = 4
   - ✅ 会议可能被结束（因为没有在线成员）
4. 用户B尝试重新加入会议
5. 验证：
   - ✅ 系统提示"你已经被拉黑无法加入会议"
   - ✅ 用户B无法进入会议

### 测试3：Redis数据丢失场景

1. 拉黑用户后，手动清除Redis中的会议数据
2. 被拉黑用户尝试重新加入会议
3. 验证：
   - ✅ checkMeetingJoin从数据库检查，发现status = 4
   - ✅ 系统提示"你已经被拉黑无法加入会议"
   - ✅ 用户无法进入会议

## 相关文件

- `src/main/java/com/easymeeting/service/impl/MeetingInfoServiceImpl.java` - exitMeetingRoom方法
- `src/main/resources/com/easymeeting/mappers/MeetingMemberMapper.xml` - updateByMeetingIdAndUserId SQL

## 相关修复

这是拉黑功能完整修复的第三部分：

1. **第一部分**：修改 `RedisComponent.getMeetingMemberList()` - 过滤Redis成员列表
2. **第二部分**：修改 `MeetingInfoController.loadMeetingMembers()` - 过滤数据库查询的成员列表
3. **第三部分**（本次修复）：修改 `MeetingInfoServiceImpl.exitMeetingRoom()` - 确保数据库状态正确更新

## 总结

通过将数据库更新移到方法的最前面，确保在任何可能的提前返回之前就完成数据库状态的持久化。这样即使方法因为各种原因提前返回，被拉黑用户的状态也已经正确保存到数据库中，从而阻止他们重新加入会议。

现在拉黑功能的完整流程已经修复：
1. ✅ 数据库状态正确更新为4
2. ✅ Redis状态正确更新为4
3. ✅ 成员列表不显示被拉黑用户
4. ✅ 被拉黑用户无法重新加入会议
