# currentMeetingId 为 null 问题诊断

## 问题现象
调用 `/api/meetingInfo/getCurrentMeeting` 返回：
```json
{
  "data": null,
  "info": "请求成功"
}
```

## 根本原因
**Token 中的 `currentMeetingId` 为空！**

后端代码：
```java
if (StringUtils.isEmpty(tokenUserInfo.getCurrentMeetingId())){
    return getSuccessResponseVO(null);  // 返回 null
}
```

## 为什么 currentMeetingId 为空？

### 正常的加入会议流程

#### 方式1：快速会议
```
1. 用户点击"快速会议"
2. 调用 /api/meetingInfo/quickMeeting
3. 后端创建会议并设置 token.currentMeetingId
4. 前端跳转到 /meeting/:meetingId
5. Meeting.vue 调用 joinMeeting()
6. 此时 token 已包含 currentMeetingId ✅
```

#### 方式2：加入他人会议
```
1. 用户输入会议号
2. 调用 /api/meetingInfo/preJoinMeeting
3. 后端验证会议并设置 token.currentMeetingId
4. 前端跳转到 /meeting/:meetingId
5. Meeting.vue 调用 joinMeeting()
6. 此时 token 已包含 currentMeetingId ✅
```

### 可能导致 currentMeetingId 为空的情况

#### 情况1：直接访问 Meeting 页面
```
用户直接在浏览器输入 /meeting
  ↓
没有调用 quickMeeting 或 preJoinMeeting
  ↓
token.currentMeetingId 未设置
  ↓
❌ getCurrentMeeting 返回 null
```

#### 情况2：Token 过期或被清除
```
用户创建会议后
  ↓
Token 过期或被清除
  ↓
重新登录（新 token 没有 currentMeetingId）
  ↓
❌ getCurrentMeeting 返回 null
```

#### 情况3：刷新页面
```
用户在会议中刷新页面
  ↓
Meeting.vue 重新加载
  ↓
调用 joinMeeting()
  ↓
但 token.currentMeetingId 可能已被清除
  ↓
❌ getCurrentMeeting 返回 null
```

## 诊断步骤

### 步骤1：检查控制台日志
刷新页面后，查看控制台输出：
```
开始加入会议流程...
路由 meetingId: xxx  ← 检查这个值
当前用户ID: xxx
```

**如果 `路由 meetingId` 为空**：
- 说明路由参数没有传递
- 需要检查跳转逻辑

### 步骤2：检查后端日志
查看后端日志：
```
用户 xxx 加入会议，当前会议ID: xxx  ← 检查这个值
```

**如果 `当前会议ID` 为 null**：
- 说明 token 中没有 currentMeetingId
- 需要检查 quickMeeting 或 preJoinMeeting 是否正确执行

### 步骤3：检查 localStorage
在浏览器控制台执行：
```javascript
// 检查 token
console.log('Token:', localStorage.getItem('token'))

// 检查 userInfo
const userInfo = JSON.parse(localStorage.getItem('userInfo'))
console.log('UserInfo:', userInfo)
console.log('CurrentMeetingId:', userInfo.currentMeetingId)
```

**如果 `currentMeetingId` 为空**：
- 说明 token 没有正确设置
- 需要重新创建或加入会议

### 步骤4：检查路由参数
在浏览器控制台执行：
```javascript
// 检查当前路由
console.log('当前路由:', window.location.pathname)
console.log('路由参数:', window.location.search)
```

**预期结果**：
- 路径应该是 `/meeting` 或 `/meeting/:meetingId`
- 如果有查询参数，应该包含 `meetingId`

## 解决方案

### 方案1：确保正确的加入流程
**不要直接访问 Meeting 页面！**

正确流程：
1. 从 Dashboard 点击"快速会议"或"加入会议"
2. 系统会自动调用相应的 API
3. 然后跳转到 Meeting 页面

### 方案2：修复刷新页面问题
如果用户在会议中刷新页面，需要：

1. **从路由参数获取 meetingId**
2. **重新调用 preJoinMeeting 设置 token**
3. **然后调用 joinMeeting**

修改 Meeting.vue：
```javascript
const joinMeeting = async () => {
  try {
    // 如果 token 中没有 currentMeetingId，先调用 preJoinMeeting
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (!userInfo.currentMeetingId && meetingId.value) {
      console.log('Token 中没有 currentMeetingId，先调用 preJoinMeeting')
      await meetingService.preJoinMeeting(
        meetingId.value,
        userInfo.nickName || '用户',
        ''  // 密码
      )
    }
    
    // 然后调用 joinMeeting
    const response = await meetingService.joinMeeting(isVideoOn.value)
    // ...
  } catch (error) {
    console.error('加入会议失败:', error)
  }
}
```

### 方案3：后端返回更友好的错误信息
修改 `getCurrentMeeting` 方法：
```java
@RequestMapping("/getCurrentMeeting")
public ResponseVO getCurrentMeeting(){
    TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
    
    if (StringUtils.isEmpty(tokenUserInfo.getCurrentMeetingId())){
        log.warn("用户 {} 当前没有会议ID", tokenUserInfo.getUserId());
        return getFailResponseVO("当前没有进行中的会议");  // 返回错误而不是 null
    }
    
    // ...
}
```

## 测试场景

### 场景1：正常创建会议
```
1. 登录
2. 点击"快速会议"
3. 创建会议
4. 自动跳转到 Meeting 页面
5. 点击"会议详情"
6. 预期：显示正确的会议信息 ✅
```

### 场景2：正常加入会议
```
1. 登录
2. 输入会议号
3. 点击"加入会议"
4. 自动跳转到 Meeting 页面
5. 点击"会议详情"
6. 预期：显示正确的会议信息 ✅
```

### 场景3：刷新页面
```
1. 在会议中
2. 按 F5 刷新页面
3. 页面重新加载
4. 点击"会议详情"
5. 当前：显示"未知" ❌
6. 修复后：显示正确信息 ✅
```

## 快速修复建议

### 立即可以做的：
1. **不要直接访问 Meeting 页面**
2. **始终通过 Dashboard 的按钮进入会议**
3. **如果刷新页面导致问题，重新加入会议**

### 需要代码修改的：
1. **在 Meeting.vue 中检查 currentMeetingId**
2. **如果为空，根据路由参数重新设置**
3. **后端返回更友好的错误信息**

## 总结

- ✅ 后端逻辑正确：通过 token.currentMeetingId 查询
- ❌ 问题：token.currentMeetingId 为空
- 🔍 原因：没有正确调用 quickMeeting 或 preJoinMeeting
- 💡 解决：确保正确的加入流程，或在 Meeting.vue 中处理刷新情况

现在请：
1. 刷新页面
2. 查看控制台日志中的 "路由 meetingId" 和 "当前用户ID"
3. 查看后端日志中的 "当前会议ID"
4. 把这些信息发给我
