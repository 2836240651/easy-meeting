# loadMeetingMembers接口过滤问题修复

## 问题发现

通过查看后端日志，发现主持人点击"刷新会议成员"时，调用的是 `/api/meetingInfo/loadMeetingMembers` 接口：

```
2026-02-22 17:24:39 [DEBUG] GET "/api/meetingInfo/loadMeetingMembers?meetingId=lcD7kRtSrU"
2026-02-22 17:24:39 [DEBUG] SELECT ... FROM meeting_member m LEFT JOIN user_info u ... WHERE m.meeting_id = ?
2026-02-22 17:24:39 [DEBUG] <==      Total: 2
```

## 根本原因

`loadMeetingMembers` 接口直接从数据库查询成员列表，返回所有成员（包括被拉黑和被踢出的），没有进行状态过滤。

### 原始代码

```java
@RequestMapping("/loadMeetingMembers")
public ResponseVO loadMeetingMembers(@NotEmpty String meetingId){
    MeetingMemberQuery meetingMemberQuery = new MeetingMemberQuery();
    meetingMemberQuery.setMeetingId(meetingId);
    List<MeetingMember> meetingMembers = meetingMemberService.findListByParam(meetingMemberQuery);
    Optional<MeetingMember> first = meetingMembers.stream()
        .filter(item -> item.getUserId().equals(getTokenUserInfo().getUserId()))
        .findFirst();
    if (!first.isPresent()){
        throw new BusinessException(ResponseCodeEnum.CODE_600);
    }
    return getSuccessResponseVO(meetingMembers);  // ❌ 返回所有成员
}
```

### 问题流程

1. 主持人拉黑用户A
   - Redis中用户A的status更新为4 ✅
   - 数据库中用户A的status更新为4 ✅

2. 主持人点击"刷新会议成员"
   - 前端调用 `/api/meetingInfo/loadMeetingMembers`
   - 后端从数据库查询所有成员（包括status=4的用户A）❌
   - 返回给前端所有成员 ❌
   - 前端显示所有成员（包括被拉黑的用户A）❌

## 修复方案

在 `loadMeetingMembers` 方法中添加状态过滤，只返回正常状态（status=1）的成员。

### 修复后的代码

```java
@RequestMapping("/loadMeetingMembers")
public ResponseVO loadMeetingMembers(@NotEmpty String meetingId){
    MeetingMemberQuery meetingMemberQuery = new MeetingMemberQuery();
    meetingMemberQuery.setMeetingId(meetingId);
    List<MeetingMember> meetingMembers = meetingMemberService.findListByParam(meetingMemberQuery);
    
    // 过滤掉被踢出和被拉黑的成员，只返回正常状态的成员
    List<MeetingMember> normalMembers = meetingMembers.stream()
        .filter(member -> MeetingMemberStatusEnum.NORMAL.getStatus().equals(member.getStatus()))
        .collect(java.util.stream.Collectors.toList());
    
    log.info("加载会议成员列表: meetingId={}, 总成员数={}, 正常成员数={}", 
             meetingId, meetingMembers.size(), normalMembers.size());
    
    // 记录被过滤掉的成员
    if (meetingMembers.size() > normalMembers.size()) {
        meetingMembers.stream()
            .filter(member -> !MeetingMemberStatusEnum.NORMAL.getStatus().equals(member.getStatus()))
            .forEach(member -> log.info("过滤掉非正常成员: userId={}, nickName={}, status={}", 
                     member.getUserId(), member.getNickName(), member.getStatus()));
    }
    
    Optional<MeetingMember> first = normalMembers.stream()
        .filter(item -> item.getUserId().equals(getTokenUserInfo().getUserId()))
        .findFirst();
    if (!first.isPresent()){
        throw new BusinessException(ResponseCodeEnum.CODE_600);
    }
    return getSuccessResponseVO(normalMembers);  // ✅ 只返回正常成员
}
```

## 修复效果

### 修复前

```
主持人拉黑用户A
→ 数据库：用户A的status = 4 ✅
→ 主持人点击"刷新会议成员"
→ 后端查询数据库，返回所有成员（包括用户A）❌
→ 前端显示用户A ❌
```

### 修复后

```
主持人拉黑用户A
→ 数据库：用户A的status = 4 ✅
→ 主持人点击"刷新会议成员"
→ 后端查询数据库，过滤掉status≠1的成员 ✅
→ 后端日志：过滤掉非正常成员: userId=A, status=4 ✅
→ 后端只返回正常成员（不包括用户A）✅
→ 前端不显示用户A ✅
```

## 日志示例

### 正常情况（无被拉黑成员）

```
INFO - 加载会议成员列表: meetingId=lcD7kRtSrU, 总成员数=5, 正常成员数=5
```

### 有被拉黑成员

```
INFO - 加载会议成员列表: meetingId=lcD7kRtSrU, 总成员数=5, 正常成员数=4
INFO - 过滤掉非正常成员: userId=6cq7Pg48b4Rq, nickName=张三, status=4
```

## 相关修复

这个修复是成员列表过滤问题的第二部分修复：

1. **第一部分**：修改 `RedisComponent.getMeetingMemberList()` - 过滤Redis中的成员列表
   - 文件：`src/main/java/com/easymeeting/redis/RedisComponent.java`
   - 影响：从Redis获取成员列表的场景（如用户加入会议、退出会议等）

2. **第二部分**（本次修复）：修改 `MeetingInfoController.loadMeetingMembers()` - 过滤数据库查询的成员列表
   - 文件：`src/main/java/com/easymeeting/controller/MeetingInfoController.java`
   - 影响：主持人点击"刷新会议成员"按钮的场景

## 测试步骤

1. 两个用户加入同一个会议
2. 主持人拉黑用户B
3. 验证：
   - ✅ 用户B收到提示并退出会议
   - ✅ 数据库中用户B的status = 4
4. 主持人点击"刷新会议成员"按钮
5. 验证：
   - ✅ 成员列表中不显示用户B
   - ✅ 后端日志显示"过滤掉非正常成员: userId=B, status=4"
6. 用户B尝试重新加入会议
7. 验证：
   - ✅ 系统提示"你已经被拉黑无法加入会议"
   - ✅ 用户B无法进入会议

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

## 相关文件

- `src/main/java/com/easymeeting/controller/MeetingInfoController.java` - 本次修复的文件
- `src/main/java/com/easymeeting/redis/RedisComponent.java` - 第一部分修复的文件
- `src/main/java/com/easymeeting/entity/enums/MeetingMemberStatusEnum.java` - 状态枚举定义

## 总结

通过在 `loadMeetingMembers` 接口中添加状态过滤逻辑，确保从数据库查询的成员列表也只返回正常状态的成员。结合之前对 `RedisComponent.getMeetingMemberList()` 的修复，现在无论是从Redis还是从数据库获取成员列表，都会自动过滤掉被拉黑和被踢出的成员。

这样就彻底解决了主持人拉黑或踢出用户后，刷新成员列表时被拉黑/踢出用户重新出现的问题。
