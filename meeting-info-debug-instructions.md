# 会议详情显示调试说明

## 问题描述
会议详情显示"未知"：
- 会议号：未知
- 发起人（主持人）：未知
- 会议名称：可能不正确

## 已完成的修改

### 后端修改
1. ✅ `MeetingInfo.java` - 添加了 `createUserNickName` 字段
2. ✅ `MeetingInfoMapper.xml` - 添加了 LEFT JOIN user_info 表
3. ✅ `MeetingInfoController.java` - 添加了详细的日志输出

### 前端修改
1. ✅ `Meeting.vue` - 修改了 `loadCurrentMeetingInfo` 函数
2. ✅ 添加了详细的控制台日志

## 调试步骤

### 步骤1：刷新浏览器
1. 按 `Ctrl + Shift + R` 强制刷新页面（清除缓存）
2. 或者关闭浏览器重新打开

### 步骤2：打开浏览器控制台
1. 按 `F12` 打开开发者工具
2. 切换到 `Console` 标签

### 步骤3：进入会议
1. 创建或加入一个会议
2. 观察控制台输出

### 步骤4：查看控制台日志
应该看到以下日志：

```
=== 开始加载会议信息 ===
API响应: {data: {...}, status: 200, ...}
响应数据: {code: 200, data: {...}, msg: "..."}
会议信息对象: {meetingId: "...", meetingNo: "...", meetingName: "...", createUserNickName: "..."}
设置后的值:
- meetingName: 快速会议
- meetingNo: 123456
- hostName: iron
- isHost: true
=== 会议信息加载成功 ===
```

### 步骤5：点击"会议详情"按钮
1. 点击左上角的"会议详情"按钮
2. 检查显示的信息是否正确

## 可能的问题和解决方案

### 问题1：控制台显示 "API返回数据为空"
**原因**：后端返回的 `data` 字段为 `null`

**检查**：
1. 查看后端日志，搜索 "获取当前会议信息"
2. 检查用户的 `currentMeetingId` 是否为空

**解决方案**：
- 确保用户已经成功加入会议
- 检查 `joinMeeting` API 是否正确设置了 `currentMeetingId`

### 问题2：控制台显示 "会议信息对象" 但字段为空
**原因**：数据库查询没有返回完整数据

**检查后端日志**：
```
返回会议信息 - 会议ID: xxx, 会议号: xxx, 会议名称: xxx, 创建者: xxx
```

**如果创建者为 null**：
- 检查数据库 `user_info` 表是否有对应的用户记录
- 检查 `MeetingInfoMapper.xml` 的 LEFT JOIN 是否正确

### 问题3：控制台没有任何日志
**原因**：`loadCurrentMeetingInfo` 函数没有被调用

**检查**：
1. 查看是否有 "开始加入会议流程..." 日志
2. 如果没有，说明 `joinMeeting` 函数没有执行

**解决方案**：
- 检查页面是否正确加载
- 检查 `onMounted` 是否正确调用 `joinMeeting`

### 问题4：API返回 401 或 403 错误
**原因**：token无效或过期

**解决方案**：
1. 重新登录
2. 检查 localStorage 中的 token 是否存在

## 后端日志检查

### 查看后端日志
在后端控制台搜索以下关键字：

1. **"获取当前会议信息"**
   ```
   获取当前会议信息 - 用户ID: xxx, 当前会议ID: xxx
   ```

2. **"返回会议信息"**
   ```
   返回会议信息 - 会议ID: xxx, 会议号: xxx, 会议名称: xxx, 创建者: xxx
   ```

3. **"用户 xxx 当前没有会议ID"**
   - 说明用户没有加入会议

4. **"会议不存在或已结束"**
   - 说明会议已经结束或被删除

## 数据库检查

### 检查 meeting_info 表
```sql
SELECT 
    m.meeting_id,
    m.meeting_no,
    m.meeting_name,
    m.create_user_id,
    u.nick_name as create_user_nick_name
FROM meeting_info m
LEFT JOIN user_info u ON m.create_user_id = u.user_id
WHERE m.meeting_id = 'xxx';  -- 替换为实际的会议ID
```

**预期结果**：
- `meeting_no` 不为空
- `meeting_name` 不为空
- `create_user_nick_name` 不为空

### 检查 user_info 表
```sql
SELECT user_id, nick_name 
FROM user_info 
WHERE user_id = 'xxx';  -- 替换为创建者的user_id
```

**预期结果**：
- 应该能找到对应的用户记录
- `nick_name` 不为空

## 测试用例

### 测试用例1：创建快速会议
```
1. 用户A登录
2. 点击"快速会议"
3. 创建会议
4. 点击"会议详情"
5. 预期：
   - 会议号：显示实际的会议号
   - 发起人：显示用户A的昵称
   - 会议名称：显示"快速会议"或自定义名称
```

### 测试用例2：加入他人会议
```
1. 用户A创建会议
2. 用户B加入会议
3. 用户B点击"会议详情"
4. 预期：
   - 会议号：显示实际的会议号
   - 发起人：显示用户A的昵称
   - 会议名称：显示会议名称
```

## 快速诊断命令

### 浏览器控制台执行
```javascript
// 检查localStorage
console.log('Token:', localStorage.getItem('token'))
console.log('UserInfo:', JSON.parse(localStorage.getItem('userInfo')))

// 手动调用API
fetch('http://localhost:6099/api/meetingInfo/getCurrentMeeting', {
    headers: {
        'token': localStorage.getItem('token')
    }
})
.then(r => r.json())
.then(data => {
    console.log('API返回:', data)
    if (data.data) {
        console.log('会议信息:', data.data)
        console.log('- 会议号:', data.data.meetingNo)
        console.log('- 会议名称:', data.data.meetingName)
        console.log('- 创建者昵称:', data.data.createUserNickName)
    }
})
```

## 总结

1. ✅ 后端已添加日志，可以追踪数据流
2. ✅ 前端已添加日志，可以看到API响应
3. ✅ 数据库查询已修改，会返回创建者昵称
4. 🔍 需要查看浏览器控制台和后端日志来定位具体问题

**下一步**：
1. 刷新浏览器页面
2. 打开控制台（F12）
3. 进入会议
4. 查看控制台日志
5. 如果还有问题，将控制台日志和后端日志发给我
