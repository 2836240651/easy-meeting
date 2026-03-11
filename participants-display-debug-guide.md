# 会议成员头像显示问题调试指南

## 问题描述
会议页面只显示自己的头像，不显示其他成员的头像。

## 可能的原因

### 1. 会议中只有一个人
这是最常见的情况。如果会议中只有你一个人，那么自然不会显示其他成员。

**解决方法：**
- 使用另一个浏览器或隐身模式，用不同的账号加入同一个会议
- 或者让另一个人加入你的会议

### 2. 其他成员的状态不是 NORMAL (1)
如果其他成员已经退出会议或被踢出，他们的状态会变成 EXIT_MEETING (2) 或 KICK_OUT (3)，前端会过滤掉这些成员。

**检查方法：**
使用调试工具 `debug-participants-display-v2.html` 查看成员的 status 字段。

### 3. WebSocket 未正确接收成员加入消息
当新成员加入时，WebSocket 应该推送 `ADD_MEETING_ROOM` 消息，前端接收后更新成员列表。

**检查方法：**
打开浏览器控制台，查看是否有以下日志：
- "有新成员加入:"
- "收到成员列表数据:"
- "过滤后成员数量（排除自己）: X"

### 4. currentUserId 未正确设置
如果 currentUserId 为空或错误，过滤逻辑可能会出问题。

**检查方法：**
在浏览器控制台执行：
```javascript
JSON.parse(localStorage.getItem('userInfo')).userId
```

## 调试步骤

### 步骤 1: 使用调试工具
1. 打开 `debug-participants-display-v2.html`
2. 点击"加载成员列表"按钮
3. 查看以下信息：
   - 总成员数
   - 过滤后成员数
   - 每个成员的 status 和 userId

### 步骤 2: 检查浏览器控制台
1. 打开会议页面
2. 按 F12 打开开发者工具
3. 切换到 Console 标签
4. 查找以下关键日志：
   ```
   加载参与者列表成功: X 人
   过滤后成员数量（排除自己）: X
   ```

### 步骤 3: 多用户测试
1. 用户A创建会议
2. 用户B加入会议
3. 在用户A的浏览器控制台查看是否有 "有新成员加入:" 日志
4. 在用户B的浏览器控制台查看是否有成员列表更新日志

### 步骤 4: 检查 API 响应
在浏览器控制台执行以下代码：
```javascript
// 获取当前会议ID
fetch('http://localhost:6099/api/meetingInfo/getCurrentMeeting', {
    headers: { 'token': localStorage.getItem('token') }
})
.then(r => r.json())
.then(data => {
    console.log('会议信息:', data)
    const meetingId = data.data.meetingId
    
    // 获取成员列表
    return fetch(`http://localhost:6099/api/meetingInfo/loadMeetingMembers?meetingId=${meetingId}`, {
        headers: { 'token': localStorage.getItem('token') }
    })
})
.then(r => r.json())
.then(data => {
    console.log('成员列表:', data)
    console.log('成员数量:', data.data.length)
    data.data.forEach(m => {
        console.log(`- ${m.nickName}: status=${m.status}, userId=${m.userId}`)
    })
})
```

## 代码逻辑说明

### 前端过滤逻辑
```javascript
const filteredMembers = memberList.filter(member => {
    // 只显示状态为 NORMAL (1) 且不是当前用户的成员
    return member.status === MemberStatus.NORMAL && member.userId !== currentUserId.value
})
```

### 成员状态枚举
```javascript
MemberStatus = {
    DEL_MEETING: 0,   // 删除会议
    NORMAL: 1,        // 正常（在会议中）
    EXIT_MEETING: 2,  // 退出会议
    KICK_OUT: 3,      // 被踢出
    BLACKLIST: 4      // 被拉黑
}
```

### 头像优先级
1. 数据库中的 avatar 字段（如果有）
2. 根据性别显示默认头像（sex=1 男，sex=0 女）
3. 使用昵称首字母生成头像

## 常见问题

### Q: 为什么刷新页面后其他成员消失了？
A: 刷新页面会重新加载成员列表。如果其他成员在你刷新前已经退出，他们的 status 会变成 EXIT_MEETING (2)，会被过滤掉。

### Q: 为什么 WebSocket 没有收到成员加入消息？
A: 可能的原因：
1. WebSocket 连接未建立成功
2. 后端未正确推送消息
3. 消息类型不匹配

检查方法：在控制台查看 "WebSocket连接成功" 日志

### Q: 如何确认后端返回了正确的头像？
A: 使用调试工具或在控制台执行 API 检查代码，查看返回的 avatar 字段是否有值。

## 解决方案总结

如果确认会议中有多个人，但仍然只显示自己的头像：

1. **检查成员状态**：确保其他成员的 status = 1
2. **检查 WebSocket**：确保 WebSocket 连接正常，能接收到消息
3. **检查过滤逻辑**：确保 currentUserId 正确，过滤条件正常
4. **检查后端数据**：确保后端 LEFT JOIN 正确，返回了 avatar 和 sex 字段

## 测试建议

为了彻底测试多人会议功能，建议：
1. 准备两个不同的浏览器（如 Chrome 和 Firefox）
2. 或使用一个普通窗口 + 一个隐身窗口
3. 用两个不同的账号分别登录
4. 一个创建会议，另一个加入
5. 观察双方是否都能看到对方的头像
