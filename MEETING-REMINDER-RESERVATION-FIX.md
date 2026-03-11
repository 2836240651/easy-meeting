# 会议提醒和预约列表功能修复

## 修复内容

### 1. 会议提醒多次提醒功能

修改了 `MeetingReminder.vue` 组件，实现在会议开始前 60 分钟、15 分钟、5 分钟各提醒一次。

#### 主要改动：

1. **提醒记录结构改变**
   - 从 `Set<meetingId>` 改为 `Map<meetingId, { 60: boolean, 15: boolean, 5: boolean }>`
   - 记录每个会议在不同时间点的提醒状态

2. **提醒逻辑优化**
   - 新增 `checkAndNotifyMeeting` 函数，根据剩余时间判断应该触发哪个阶段的提醒
   - 60 分钟提醒：剩余时间 > 15 分钟且 ≤ 60 分钟
   - 15 分钟提醒：剩余时间 > 5 分钟且 ≤ 15 分钟
   - 5 分钟提醒：剩余时间 ≥ 0 且 ≤ 5 分钟

3. **通知样式区分**
   - 60 分钟：`info` 类型（蓝色）
   - 15 分钟：`warning` 类型（橙色）
   - 5 分钟：`error` 类型（红色），提示"请做好准备"

#### 代码示例：

```javascript
// 检查并通知会议
const checkAndNotifyMeeting = (meeting) => {
  const now = Date.now()
  let startTime = meeting.startTime
  
  // 时间格式转换
  if (typeof startTime === 'string') {
    startTime = new Date(startTime).getTime()
  }
  
  const diff = startTime - now
  const minutes = Math.floor(diff / 60000)
  
  // 获取或初始化提醒记录
  if (!notifiedMeetings.value.has(meeting.meetingId)) {
    notifiedMeetings.value.set(meeting.meetingId, {
      60: false,
      15: false,
      5: false
    })
  }
  
  const notifyRecord = notifiedMeetings.value.get(meeting.meetingId)
  
  // 判断应该触发哪个阶段的提醒
  if (minutes <= 60 && minutes > 15 && !notifyRecord[60]) {
    showMeetingNotification(meeting, 60)
    notifyRecord[60] = true
  } else if (minutes <= 15 && minutes > 5 && !notifyRecord[15]) {
    showMeetingNotification(meeting, 15)
    notifyRecord[15] = true
  } else if (minutes <= 5 && minutes >= 0 && !notifyRecord[5]) {
    showMeetingNotification(meeting, 5)
    notifyRecord[5] = true
  }
}
```

### 2. ReservationList 组件后端 API 集成

修复了 `ReservationList.vue` 组件与后端 API 的数据格式兼容问题。

#### 主要问题：

1. **时间格式不一致**
   - 后端返回：字符串格式 `"yyyy-MM-dd HH:mm"`（通过 `@JsonFormat` 注解）
   - 前端期望：时间戳（毫秒）

2. **创建者名称字段**
   - 后端返回：`nickName` 字段
   - 前端使用：`createUserName` 字段（错误）

#### 修复方案：

1. **时间格式处理**
   - 在所有需要比较时间的地方添加类型检查和转换
   - 支持字符串和时间戳两种格式

```javascript
// 计算即将开始的会议
const upcomingMeetings = computed(() => {
  const now = Date.now()
  return allMeetings.value.filter(meeting => {
    // 将字符串时间转换为时间戳
    const startTime = typeof meeting.startTime === 'string' 
      ? new Date(meeting.startTime).getTime() 
      : meeting.startTime
    return startTime >= now && meeting.status === 0
  }).sort((a, b) => {
    const timeA = typeof a.startTime === 'string' ? new Date(a.startTime).getTime() : a.startTime
    const timeB = typeof b.startTime === 'string' ? new Date(b.startTime).getTime() : b.startTime
    return timeA - timeB
  })
})
```

2. **创建者名称字段修正**
   - 将 `meeting.createUserName` 改为 `meeting.nickName`

3. **时间显示优化**
   - `formatMeetingTime` 函数支持字符串格式直接返回
   - 后端已经格式化好的时间字符串无需再次转换

4. **排序优化**
   - 即将开始的会议：按开始时间升序排序（最近的在前）
   - 已结束的会议：按开始时间降序排序（最新的在前）

## 后端数据格式

### MeetingReserve 实体类关键字段：

```java
public class MeetingReserve {
    private String meetingId;
    private String meetingName;
    private Integer joinType;
    private String joinPassword;
    private Integer duration;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date startTime;  // 返回格式化的字符串
    
    private String createUserId;
    private Integer status;
    private String nickName;  // 创建者昵称（通过 JOIN 查询获取）
    private String inviteUserIds;
}
```

### API 返回示例：

```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "meetingId": "M123456",
      "meetingName": "项目讨论会",
      "joinType": 0,
      "duration": 60,
      "startTime": "2026-02-25 15:30",
      "createUserId": "U001",
      "nickName": "张三",
      "status": 0,
      "inviteUserIds": "U001,U002,U003"
    }
  ]
}
```

## 测试建议

### 1. 会议提醒测试

1. 创建一个 61 分钟后开始的会议
2. 等待 1 分钟，应该收到 60 分钟提醒（蓝色）
3. 快进到 15 分钟前，应该收到 15 分钟提醒（橙色）
4. 快进到 5 分钟前，应该收到 5 分钟提醒（红色）
5. 每个阶段只应该提醒一次

### 2. 预约列表测试

1. 创建多个不同时间的预约会议
2. 检查"即将开始"标签页：
   - 只显示未开始且未取消的会议
   - 按开始时间升序排序
   - 显示正确的创建者名称
   - 状态标签颜色正确（15分钟内红色，60分钟内橙色，其他绿色）
3. 检查"已结束"标签页：
   - 显示已结束或已取消的会议
   - 按开始时间降序排序
4. 测试操作按钮：
   - 创建者可以看到"修改"和"取消"按钮
   - 被邀请者可以看到"退出"按钮
   - 所有人都可以看到"加入会议"按钮

## 文件修改清单

- `frontend/src/components/MeetingReminder.vue` - 多次提醒功能
- `frontend/src/components/ReservationList.vue` - 后端 API 集成修复

## 注意事项

1. 会议提醒每分钟检查一次，确保不会错过提醒时间点
2. 后端返回的时间是字符串格式，前端需要转换为时间戳进行比较
3. 创建者名称字段是 `nickName`，不是 `createUserName`
4. 后端已经通过 `queryUserInfo(true)` 参数查询了用户信息
