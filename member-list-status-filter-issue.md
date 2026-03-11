# 成员列表显示被拉黑/踢出用户问题分析

## 问题描述

主持人拉黑或踢出用户后，重新点击成员列表时，那些被拉黑或被踢出的用户又重新出现在成员列表中。

## 问题根源分析

### 核心问题：Redis中的状态更新了，但成员没有被过滤

当前的实现流程：

1. **拉黑/踢出时**：
   ```java
   // RedisComponent.exitMeeting()
   memberDto.setStatus(statusEnum.getStatus());  // 设置为 BLACKLIST(4) 或 KICK_OUT(3)
   add2meeting(meetingId, memberDto);  // 更新Redis中的状态
   ```
   ✅ Redis中的状态已正确更新

2. **获取成员列表时**：
   ```java
   // RedisComponent.getMeetingMemberList()
   public List<MeetingMemberDto> getMeetingMemberList(String meetingId){
       List<MeetingMemberDto> meetingMemberDtoList =
        redisUtils.hvals(Constants.REDIS_KEY_MEETING_ROOM + meetingId);
        meetingMemberDtoList = meetingMemberDtoList.stream()
        .sorted(Comparator.comparing(MeetingMemberDto::getJoinTime))
        .collect(Collectors.toList());
       return meetingMemberDtoList;  // ❌ 返回所有成员，包括被拉黑/踢出的
   }
   ```
   ❌ 问题：返回了所有成员，没有过滤掉非正常状态的成员

3. **前端过滤**：
   ```javascript
   // Meeting.vue - updateParticipantsList()
   const normalMembers = memberList.filter(member => {
       return member.status === MemberStatus.NORMAL  // 只显示状态为1的成员
   })
   ```
   ✅ 前端有过滤逻辑，但依赖后端返回正确的状态

### 问题场景

**场景1：主持人点击成员列表**
```
前端 → 请求成员列表
后端 → getMeetingMemberList() → 返回所有成员（包括status=3,4的）
前端 → 过滤 status === 1 的成员 → 正确显示
```
✅ 这个场景应该是正常的

**场景2：被拉黑用户重新出现**

可能的原因：

#### 原因1：Redis状态未正确更新
```java
// exitMeetingRoom() 中
redisComponent.exitMeeting(meetingId, userId, statusEnum);
```

检查点：
- exitMeeting是否被正确调用？
- statusEnum是否是BLACKLIST或KICK_OUT？
- Redis中的数据是否真的更新了？

#### 原因2：用户重新加入会议
```java
// addMeetingMember() 中
MeetingMember existingMember = meetingMemberMapper.selectByMeetingIdAndUserId(meetingId, userId);
if (existingMember != null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(existingMember.getStatus())) {
    throw new BusinessException("你已经被拉黑无法加入会议");
}
```

如果这个检查失效，用户可能重新加入并覆盖了Redis中的状态。

#### 原因3：WebSocket消息同步问题

当用户被拉黑/踢出时，其他在线成员应该收到更新的成员列表：

```java
// exitMeetingRoom() 中
List<MeetingMemberDto> meetingMemberList = redisComponent.getMeetingMemberList(meetingId);
exitDto.setMeetingMemberDtoList(meetingMemberList);
// 发送给所有成员
messageHandler.sendMessage(messageSendDto);
```

问题：这里返回的列表包含了被踢出的用户（status=3或4），前端需要过滤。

## 解决方案

### 方案1：后端过滤（推荐）

在`RedisComponent.getMeetingMemberList()`中过滤掉非正常状态的成员：

```java
public List<MeetingMemberDto> getMeetingMemberList(String meetingId){
    List<MeetingMemberDto> meetingMemberDtoList =
     redisUtils.hvals(Constants.REDIS_KEY_MEETING_ROOM + meetingId);
     
    // 只返回状态为NORMAL的成员
    meetingMemberDtoList = meetingMemberDtoList.stream()
     .filter(member -> MeetingMemberStatusEnum.NORMAL.getStatus().equals(member.getStatus()))
     .sorted(Comparator.comparing(MeetingMemberDto::getJoinTime))
     .collect(Collectors.toList());
     
    return meetingMemberDtoList;
}
```

**优点**：
- 统一在后端过滤，所有调用getMeetingMemberList的地方都生效
- 减少网络传输的数据量
- 前端不需要修改

**缺点**：
- 如果某些场景需要获取所有成员（包括非正常状态），需要新增方法

### 方案2：新增方法，保持原方法不变

```java
// 获取所有成员（包括非正常状态）
public List<MeetingMemberDto> getAllMeetingMembers(String meetingId){
    List<MeetingMemberDto> meetingMemberDtoList =
     redisUtils.hvals(Constants.REDIS_KEY_MEETING_ROOM + meetingId);
     meetingMemberDtoList = meetingMemberDtoList.stream()
     .sorted(Comparator.comparing(MeetingMemberDto::getJoinTime))
     .collect(Collectors.toList());
    return meetingMemberDtoList;
}

// 只获取正常状态的成员
public List<MeetingMemberDto> getMeetingMemberList(String meetingId){
    return getAllMeetingMembers(meetingId).stream()
     .filter(member -> MeetingMemberStatusEnum.NORMAL.getStatus().equals(member.getStatus()))
     .collect(Collectors.toList());
}
```

**优点**：
- 向后兼容，不影响现有代码
- 灵活性高，可以根据需要选择方法

**缺点**：
- 需要检查所有调用getMeetingMemberList的地方，确认是否需要修改

### 方案3：前端增强过滤（临时方案）

确保前端的过滤逻辑正确：

```javascript
const updateParticipantsList = (memberList) => {
  console.log('收到成员列表数据:', memberList)
  
  if (!Array.isArray(memberList)) {
    console.error('成员列表不是数组:', typeof memberList, memberList)
    return
  }
  
  // 过滤出状态正常的成员（排除被踢出和被拉黑的）
  const normalMembers = memberList.filter(member => {
    const isNormal = member.status === MemberStatus.NORMAL
    if (!isNormal) {
      console.log('过滤掉非正常成员:', member.nickName, 'status:', member.status)
    }
    return isNormal
  })
  
  // ... 后续处理
}
```

## 调试步骤

### 1. 验证Redis中的状态

拉黑用户后，检查Redis中的数据：

```bash
# 连接Redis
redis-cli

# 查看会议成员
HGETALL meeting:room:{meetingId}

# 查看特定用户
HGET meeting:room:{meetingId} {userId}
```

预期结果：被拉黑用户的status应该是4

### 2. 添加后端日志

在关键位置添加日志：

```java
// RedisComponent.exitMeeting()
public Boolean exitMeeting(String meetingId, String userId, MeetingMemberStatusEnum statusEnum){
    MeetingMemberDto memberDto = getMeetingMember(meetingId, userId);
    if (memberDto==null){
        return false;
    }
    log.info("更新成员状态: meetingId={}, userId={}, oldStatus={}, newStatus={}", 
             meetingId, userId, memberDto.getStatus(), statusEnum.getStatus());
    memberDto.setStatus(statusEnum.getStatus());
    add2meeting(meetingId, memberDto);
    return true;
}

// RedisComponent.getMeetingMemberList()
public List<MeetingMemberDto> getMeetingMemberList(String meetingId){
    List<MeetingMemberDto> meetingMemberDtoList =
     redisUtils.hvals(Constants.REDIS_KEY_MEETING_ROOM + meetingId);
    
    log.info("获取成员列表: meetingId={}, 总数={}", meetingId, meetingMemberDtoList.size());
    for (MeetingMemberDto member : meetingMemberDtoList) {
        log.info("  成员: userId={}, nickName={}, status={}", 
                 member.getUserId(), member.getNickName(), member.getStatus());
    }
    
    meetingMemberDtoList = meetingMemberDtoList.stream()
     .sorted(Comparator.comparing(MeetingMemberDto::getJoinTime))
     .collect(Collectors.toList());
    return meetingMemberDtoList;
}
```

### 3. 前端调试

在浏览器控制台查看：

```javascript
// 查看收到的原始数据
console.log('原始成员列表:', memberList)

// 查看过滤后的数据
console.log('过滤后的成员:', normalMembers)

// 查看每个成员的状态
memberList.forEach(m => {
  console.log(`${m.nickName}: status=${m.status}`)
})
```

## 推荐实施方案

采用**方案2**（新增方法）+ 增强日志：

1. 新增`getAllMeetingMembers()`方法获取所有成员
2. 修改`getMeetingMemberList()`只返回正常状态的成员
3. 检查所有调用`getMeetingMemberList()`的地方，确认是否需要改用`getAllMeetingMembers()`
4. 添加详细日志便于调试

### 需要检查的调用点

根据搜索结果，以下地方调用了`getMeetingMemberList()`：

1. `MeetingInfoServiceImpl.joinMeeting()` - 用户加入会议时
   - 应该只返回正常成员 ✅

2. `MeetingInfoServiceImpl.exitMeetingRoom()` - 用户退出会议时
   - 应该只返回正常成员 ✅

3. `MeetingInfoServiceImpl.finishMeeting()` - 结束会议时
   - 可能需要所有成员（清理资源）❓

4. `ChannelContextUtils` - WebSocket消息处理
   - 应该只返回正常成员 ✅

## 总结

问题的根本原因是`getMeetingMemberList()`返回了所有成员，包括被拉黑和被踢出的成员。虽然前端有过滤逻辑，但最好在后端统一过滤，确保数据的一致性和正确性。

建议采用方案2，新增方法保持向后兼容，同时修改现有方法只返回正常状态的成员。
