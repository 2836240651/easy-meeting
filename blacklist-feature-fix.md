# 拉黑功能问题修复

## 问题描述

拉黑功能存在问题，被拉黑的用户可能仍然能够重新加入会议。

## 问题分析

### 原始代码问题

在`MeetingInfoServiceImpl.checkMeetingJoin()`方法中，检查拉黑状态的逻辑有误：

```java
// ❌ 错误的比较方式
if (meetingMember!=null && MeetingMemberStatusEnum.BLACKLIST.equals(meetingMember.getStatus())){
    throw new BusinessException("你已经被拉黑无法加入会议");
}
```

**问题**：
1. `MeetingMemberStatusEnum.BLACKLIST`是一个枚举对象
2. `meetingMember.getStatus()`返回的是Integer类型的状态值
3. 使用`equals`比较枚举对象和Integer值永远返回false
4. 导致拉黑检查失效

### 其他潜在问题

1. **只检查Redis，不检查数据库**
   - 如果Redis数据丢失或过期，拉黑状态会失效
   - 被拉黑的用户可以重新加入会议

2. **异常处理不当**
   - 原代码捕获所有异常后只记录日志，允许用户加入
   - 应该区分业务异常和系统异常

## 解决方案

### 修复1：正确比较状态值

```java
// ✅ 正确的比较方式
if (meetingMember!=null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(meetingMember.getStatus())){
    throw new BusinessException("你已经被拉黑无法加入会议");
}
```

### 修复2：同时检查Redis和数据库

```java
private void checkMeetingJoin(String meetingId,String userId){
    try {
        // 检查Redis中的状态
        MeetingMemberDto meetingMember = redisComponent.getMeetingMember(meetingId, userId);
        if (meetingMember!=null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(meetingMember.getStatus())){
            throw new BusinessException("你已经被拉黑无法加入会议");
        }
        
        // 检查数据库中的状态（防止Redis数据丢失）
        MeetingMember dbMeetingMember = meetingMemberMapper.selectByMeetingIdAndUserId(meetingId, userId);
        if (dbMeetingMember != null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(dbMeetingMember.getStatus())) {
            throw new BusinessException("你已经被拉黑无法加入会议");
        }
    } catch (BusinessException e) {
        // 重新抛出业务异常
        throw e;
    } catch (Exception e) {
        log.error("checkMeetingJoin error: meetingId=" + meetingId + ", userId=" + userId, e);
        // 如果Redis出错，检查数据库
        try {
            MeetingMember dbMeetingMember = meetingMemberMapper.selectByMeetingIdAndUserId(meetingId, userId);
            if (dbMeetingMember != null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(dbMeetingMember.getStatus())) {
                throw new BusinessException("你已经被拉黑无法加入会议");
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.error("checkMeetingJoin database check error: meetingId=" + meetingId + ", userId=" + userId, ex);
        }
    }
}
```

## 拉黑功能完整流程

### 1. 主持人拉黑成员

```
前端 → POST /api/meetingInfo/blackMeeting
     → { userId: "被拉黑用户ID" }
```

### 2. 后端处理拉黑请求

```java
@RequestMapping("/blackMeeting")
public ResponseVO blackMeeting(@RequestBody Map<String, String> params){
    String userId = params.get("userId");
    if (StringTools.isEmpty(userId)) {
        throw new BusinessException("用户ID不能为空");
    }
    TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
    // 调用forceExitMeeting，传入BLACKLIST状态
    this.meetingInfoService.forceExitMeeting(tokenUserInfo, userId, MeetingMemberStatusEnum.BLACKLIST);
    return getSuccessResponseVO(null);
}
```

### 3. 强制退出并设置拉黑状态

```java
public void forceExitMeeting(TokenUserInfoDto tokenUserInfo, String userId, MeetingMemberStatusEnum meetingMemberStatusEnum) {
    // 1. 验证主持人权限
    // 2. 发送强制下线消息
    // 3. 调用exitMeetingRoom，传入BLACKLIST状态
    exitMeetingRoom(tokenByUserId, meetingMemberStatusEnum);
}
```

### 4. 退出会议并更新状态

```java
public void exitMeetingRoom(TokenUserInfoDto tokenUserInfoDto, MeetingMemberStatusEnum statusEnum) {
    // 1. 从Redis中更新成员状态为BLACKLIST
    redisComponent.exitMeeting(meetingId, userId, statusEnum);
    
    // 2. 发送退出会议消息
    // ...
    
    // 3. 更新数据库中的状态
    if (ArrayUtils.contains(new Integer[]{
        MeetingMemberStatusEnum.KICK_OUT.getStatus(),
        MeetingMemberStatusEnum.BLACKLIST.getStatus()
    }, statusEnum.getStatus())){
        MeetingMember meetingMember = new MeetingMember();
        meetingMember.setStatus(statusEnum.getStatus());
        meetingMember.setUserId(userId);
        meetingMemberMapper.updateByMeetingIdAndUserId(meetingMember, meetingId, userId);
    }
}
```

### 5. 被拉黑用户尝试重新加入

```java
public void joinMeeting(Boolean videoOpen, String meetingId, String userId, String nickName, Integer sex) {
    // 1. 验证会议存在
    // 2. 检查用户是否被拉黑
    this.checkMeetingJoin(meetingId, userId);  // ✅ 这里会抛出异常
    // 3. 如果通过检查，加入会议
}
```

### 6. 检查拉黑状态

```java
private void checkMeetingJoin(String meetingId, String userId){
    // 1. 检查Redis中的状态
    MeetingMemberDto meetingMember = redisComponent.getMeetingMember(meetingId, userId);
    if (meetingMember!=null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(meetingMember.getStatus())){
        throw new BusinessException("你已经被拉黑无法加入会议");  // ✅ 阻止加入
    }
    
    // 2. 检查数据库中的状态（双重保险）
    MeetingMember dbMeetingMember = meetingMemberMapper.selectByMeetingIdAndUserId(meetingId, userId);
    if (dbMeetingMember != null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(dbMeetingMember.getStatus())) {
        throw new BusinessException("你已经被拉黑无法加入会议");  // ✅ 阻止加入
    }
}
```

## 状态枚举值

```java
public enum MeetingMemberStatusEnum {
    DEL_MEETING(0, "删除会议"),
    NORMAL(1, "正常"),
    EXIT_MEETING(2, "退出会议"),
    KICK_OUT(3, "被踢出会议"),
    BLACKLIST(4, "被拉黑");
    
    private Integer status;
    private String desc;
}
```

## 测试步骤

### 测试1：拉黑功能

1. 两个用户加入同一个会议
2. 主持人打开成员列表
3. 主持人点击"拉黑"按钮
4. 验证：
   - ✅ 被拉黑用户收到提示并退出会议
   - ✅ 被拉黑用户的状态在Redis中更新为4（BLACKLIST）
   - ✅ 被拉黑用户的状态在数据库中更新为4（BLACKLIST）

### 测试2：拉黑后无法重新加入

1. 被拉黑的用户尝试重新加入会议
2. 输入会议号
3. 点击"加入会议"
4. 验证：
   - ✅ 系统提示"你已经被拉黑无法加入会议"
   - ✅ 用户无法进入会议页面

### 测试3：Redis数据丢失场景

1. 拉黑用户后，手动清除Redis中的会议数据
2. 被拉黑用户尝试重新加入会议
3. 验证：
   - ✅ 系统从数据库检查拉黑状态
   - ✅ 仍然提示"你已经被拉黑无法加入会议"
   - ✅ 用户无法进入会议

## 相关文件

- `src/main/java/com/easymeeting/controller/MeetingInfoController.java` - blackMeeting接口
- `src/main/java/com/easymeeting/service/impl/MeetingInfoServiceImpl.java` - forceExitMeeting和checkMeetingJoin方法
- `src/main/java/com/easymeeting/redis/RedisComponent.java` - Redis状态更新
- `src/main/java/com/easymeeting/entity/enums/MeetingMemberStatusEnum.java` - 状态枚举

## 关键点

1. **类型匹配**：比较状态时要使用`.getStatus()`获取Integer值
2. **双重检查**：同时检查Redis和数据库，确保拉黑状态不会丢失
3. **异常处理**：区分业务异常和系统异常，业务异常要重新抛出
4. **数据一致性**：Redis和数据库都要更新拉黑状态
5. **用户体验**：被拉黑后显示明确的提示信息

## 总结

拉黑功能的核心问题是状态比较逻辑错误，导致拉黑检查失效。通过修复比较逻辑并添加数据库检查，确保了拉黑功能的可靠性。即使Redis数据丢失，系统仍然能够从数据库中检查拉黑状态，防止被拉黑用户重新加入会议。
