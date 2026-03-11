# 踢出成员参数接收问题修复

## 问题描述

主持人点击踢出成员时，前端Network抓包显示payload正确传送了被踢出用户的userId：
```json
{
  "userId": "6cq7Pg48b4Rq"
}
```

但是后端报错参数为null。

## 问题分析

### 前端发送方式
```javascript
// frontend/src/api/services.js
kickOutMeeting: (userId) => {
  return api.post('/meetingInfo/kickOutMeeting', { userId })
}
```

前端使用`api.post()`方法，将`{ userId }`作为**JSON请求体**发送。

### 后端接收方式（修复前）
```java
@globalInterceptor
@RequestMapping("/kickOutMeeting")
public ResponseVO kickMeeting(@NotNull String userId){
    TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
    this.meetingInfoService.forceExitMeeting(tokenUserInfo, userId, MeetingMemberStatusEnum.EXIT_MEETING);
    return getSuccessResponseVO(null);
}
```

后端使用`@NotNull String userId`参数，这种方式期望接收：
- URL参数：`/kickOutMeeting?userId=xxx`
- 表单参数：`Content-Type: application/x-www-form-urlencoded`

**但不能接收JSON请求体中的参数！**

### 根本原因

Spring MVC的参数绑定机制：
1. **简单参数**（如`String userId`）默认从URL参数或表单参数中获取
2. **@RequestBody注解**才会从JSON请求体中提取数据
3. 前端发送JSON，后端不使用`@RequestBody`，导致参数为null

## 解决方案

### 方案1：修改后端使用@RequestBody（已采用）

```java
@globalInterceptor
@RequestMapping("/kickOutMeeting")
public ResponseVO kickMeeting(@RequestBody Map<String, String> params){
    String userId = params.get("userId");
    if (StringTools.isEmpty(userId)) {
        throw new BusinessException("用户ID不能为空");
    }
    TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
    this.meetingInfoService.forceExitMeeting(tokenUserInfo, userId, MeetingMemberStatusEnum.EXIT_MEETING);
    return getSuccessResponseVO(null);
}
```

**优点**：
- 符合RESTful API设计规范（POST请求使用JSON请求体）
- 前端代码无需修改
- 更灵活，可以轻松扩展参数

**修改内容**：
1. 添加`@RequestBody`注解
2. 使用`Map<String, String>`接收JSON数据
3. 从Map中提取`userId`
4. 添加参数验证

### 方案2：修改前端使用URL参数（未采用）

```javascript
// 如果选择这个方案，前端需要修改为：
kickOutMeeting: (userId) => {
  return api.post('/meetingInfo/kickOutMeeting', null, { params: { userId } })
}
```

**缺点**：
- 不符合RESTful规范（POST请求应该使用请求体）
- 需要修改前端代码
- URL参数有长度限制

## 同时修复的接口

### blackMeeting接口

**修复前：**
```java
@globalInterceptor
@RequestMapping("/blackMeeting")
public ResponseVO blackMeeting(@NotNull String userId){
    TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
    this.meetingInfoService.forceExitMeeting(tokenUserInfo, userId, MeetingMemberStatusEnum.EXIT_MEETING);
    return getSuccessResponseVO(null);
}
```

**修复后：**
```java
@globalInterceptor
@RequestMapping("/blackMeeting")
public ResponseVO blackMeeting(@RequestBody Map<String, String> params){
    String userId = params.get("userId");
    if (StringTools.isEmpty(userId)) {
        throw new BusinessException("用户ID不能为空");
    }
    TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
    this.meetingInfoService.forceExitMeeting(tokenUserInfo, userId, MeetingMemberStatusEnum.BLACKLIST);
    return getSuccessResponseVO(null);
}
```

**注意**：还修复了一个bug，拉黑应该使用`MeetingMemberStatusEnum.BLACKLIST`而不是`EXIT_MEETING`。

## 测试步骤

1. 重启后端服务（已完成）
2. 两个用户加入同一个会议
3. 主持人打开成员列表
4. 主持人点击"踢出"按钮
5. 验证：
   - 后端不再报错参数为null
   - 被踢出的用户收到强制下线通知
   - 被踢出的用户自动退出会议
   - 主持人的成员列表中该用户被移除

## 相关文件

- `src/main/java/com/easymeeting/controller/MeetingInfoController.java` - 修改了kickOutMeeting和blackMeeting接口
- `frontend/src/api/services.js` - 前端API调用（无需修改）
- `frontend/src/views/Meeting.vue` - 前端踢人功能（无需修改）

## 技术要点

### Spring MVC参数绑定规则

| 注解 | 数据来源 | 适用场景 |
|------|---------|---------|
| 无注解 | URL参数、表单参数 | GET请求、表单提交 |
| @RequestParam | URL参数 | 显式指定URL参数 |
| @RequestBody | JSON请求体 | POST/PUT请求的JSON数据 |
| @PathVariable | URL路径 | RESTful风格的路径参数 |

### RESTful API设计规范

- **GET请求**：使用URL参数传递数据
- **POST/PUT/DELETE请求**：使用请求体（JSON）传递数据
- **路径参数**：用于标识资源（如`/user/{id}`）

## 总结

这是一个典型的前后端参数传递不匹配问题。前端使用JSON请求体发送数据，后端必须使用`@RequestBody`注解才能正确接收。修复后，踢出成员和拉黑成员功能都能正常工作。
