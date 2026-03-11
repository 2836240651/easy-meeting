# 成员列表模态框显示修复总结

## 问题描述
用户反馈：点击"成员"按钮打开成员列表模态框时，只显示其他成员，没有显示自己。这导致主持人无法看到完整的成员列表，也无法对所有成员进行管理。

## 需求分析
1. **成员列表模态框**应该显示**所有成员**（包括自己）
2. **主持人**需要能够对其他成员进行**踢出**和**拉黑**操作
3. **不能对自己**进行踢出或拉黑操作
4. **不能对主持人**进行踢出或拉黑操作（如果有多个主持人）

## 设计方案

### 两个成员数组

#### 1. `participants` - 用于视频区域
- **用途**：显示在会议主界面的视频网格中
- **内容**：只包含其他成员，**不包括自己**
- **原因**：当前用户单独显示在顶部，不需要在网格中重复显示

#### 2. `allParticipants` - 用于成员列表模态框
- **用途**：显示在"成员"按钮打开的模态框中
- **内容**：包含**所有成员**（包括自己）
- **原因**：主持人需要看到完整的成员列表进行管理

### 数据结构

```javascript
// 成员对象结构
{
  userId: "6cq7Pg48b4Rq",
  name: "iron",
  avatar: "http://...",
  isMuted: false,
  isHost: true,
  videoOpen: true,
  status: 1,
  memberType: 1,
  isCurrentUser: true  // 新增：标记是否是当前用户
}
```

## 实现细节

### 1. 添加 `allParticipants` 数组

```javascript
const participants = ref([])  // 用于视频区域显示（不包括自己）
const allParticipants = ref([])  // 用于成员列表模态框（包括所有人）
```

### 2. 修改 `updateParticipantsList` 函数

```javascript
const updateParticipantsList = (memberList) => {
  // 过滤出状态正常的成员
  const normalMembers = memberList.filter(member => {
    return member.status === MemberStatus.NORMAL
  })
  
  // allParticipants: 包括所有正常状态的成员
  allParticipants.value = normalMembers.map(member => {
    const formatted = formatParticipant(member)
    formatted.isCurrentUser = member.userId === currentUserId.value
    return formatted
  })
  
  // participants: 只包括其他成员，不包括自己
  participants.value = normalMembers
    .filter(member => member.userId !== currentUserId.value)
    .map(member => formatParticipant(member))
}
```

### 3. 修改成员列表模态框 HTML

```vue
<div class="modal-header">
  <h3>会议成员 ({{ allParticipants.length }})</h3>
</div>

<div v-for="participant in allParticipants" :key="participant.userId">
  <h4>{{ participant.name }}
    <span v-if="participant.isHost">主持人</span>
    <span v-if="participant.isCurrentUser">我</span>
  </h4>
  
  <!-- 只有主持人可以操作，且不能操作主持人和自己 -->
  <div v-if="isHost && !participant.isHost && !participant.isCurrentUser">
    <button @click="kickOutParticipant(participant.userId)">踢出</button>
    <button @click="blackParticipant(participant.userId)">拉黑</button>
  </div>
</div>
```

### 4. 更新 `addParticipant` 函数

```javascript
const addParticipant = (memberData) => {
  const formatted = formatParticipant(memberData)
  formatted.isCurrentUser = memberData.userId === currentUserId.value
  
  // 添加到 allParticipants
  if (!allParticipants.value.find(p => p.userId === memberData.userId)) {
    allParticipants.value.push(formatted)
  }
  
  // 如果不是当前用户，也添加到 participants
  if (memberData.userId !== currentUserId.value) {
    if (!participants.value.find(p => p.userId === memberData.userId)) {
      participants.value.push(formatted)
    }
  }
}
```

### 5. 更新 `removeParticipant` 函数

```javascript
const removeParticipant = (userId) => {
  // 从两个数组中移除
  allParticipants.value = allParticipants.value.filter(p => p.userId !== userId)
  participants.value = participants.value.filter(p => p.userId !== userId)
}
```

### 6. 更新踢人和拉黑函数

```javascript
const kickOutParticipant = async (userId) => {
  if (!isHost.value) return
  
  if (confirm('确定要将该成员踢出会议吗？')) {
    try {
      await meetingService.kickOutMeeting(userId)
      
      // 从两个数组中移除
      participants.value = participants.value.filter(p => p.userId !== userId)
      allParticipants.value = allParticipants.value.filter(p => p.userId !== userId)
      
      alert('踢出成功')
    } catch (error) {
      alert('踢出失败，请稍后重试')
    }
  }
}
```

## 权限控制

### 显示操作按钮的条件
```vue
v-if="isHost && !participant.isHost && !participant.isCurrentUser"
```

**条件说明**：
1. `isHost` - 当前用户必须是主持人
2. `!participant.isHost` - 目标成员不能是主持人
3. `!participant.isCurrentUser` - 目标成员不能是自己

### 权限矩阵

| 当前用户 | 目标成员 | 是否显示操作按钮 |
|---------|---------|----------------|
| 主持人   | 自己     | ❌ 否          |
| 主持人   | 主持人   | ❌ 否          |
| 主持人   | 普通成员 | ✅ 是          |
| 普通成员 | 任何人   | ❌ 否          |

## 后端API

### 踢出成员
```
POST /api/meetingInfo/kickOutMeeting
参数: userId (被踢出的用户ID)
```

### 拉黑成员
```
POST /api/meetingInfo/blackMeeting
参数: userId (被拉黑的用户ID)
```

## UI 展示

### 成员列表模态框示例

```
┌─────────────────────────────────┐
│ 会议成员 (2)                 ×  │
├─────────────────────────────────┤
│ 👤 iron [主持人] [我]           │
│    正常                          │
│                                  │
│ 👤 jemmy                         │
│    正常                          │
│    [踢出] [拉黑]                │
└─────────────────────────────────┘
```

### 视频区域显示

```
┌─────────────────────────────────┐
│ 👤 iron (我)                    │  ← 当前用户单独显示
├─────────────────────────────────┤
│ 👤 jemmy                        │  ← 其他成员
└─────────────────────────────────┘
```

## 测试场景

### 场景1：主持人查看成员列表
```
1. iron (主持人) 创建会议
2. jemmy 加入会议
3. iron 点击"成员"按钮
4. 应该看到：
   - iron [主持人] [我] - 无操作按钮
   - jemmy - 有 [踢出] [拉黑] 按钮
```

### 场景2：普通成员查看成员列表
```
1. iron (主持人) 创建会议
2. jemmy (普通成员) 加入会议
3. jemmy 点击"成员"按钮
4. 应该看到：
   - iron [主持人] - 无操作按钮
   - jemmy [我] - 无操作按钮
```

### 场景3：主持人踢出成员
```
1. iron 点击 jemmy 的 [踢出] 按钮
2. 确认对话框
3. 调用后端 API
4. 成功后：
   - jemmy 从成员列表中消失
   - jemmy 从视频区域消失
   - jemmy 收到被踢出通知（通过WebSocket）
```

### 场景4：主持人拉黑成员
```
1. iron 点击 jemmy 的 [拉黑] 按钮
2. 确认对话框
3. 调用后端 API
4. 成功后：
   - jemmy 从成员列表中消失
   - jemmy 从视频区域消失
   - jemmy 被加入黑名单，无法再次加入
```

## 修改文件

### 前端文件
- `frontend/src/views/Meeting.vue`
  - 添加 `allParticipants` 数组
  - 修改 `updateParticipantsList` 函数
  - 修改 `addParticipant` 函数
  - 修改 `removeParticipant` 函数
  - 修改 `kickOutParticipant` 函数
  - 修改 `blackParticipant` 函数
  - 修改成员列表模态框 HTML

### 后端文件
- 无需修改（API已存在）

## 总结

- ✅ 成员列表模态框现在显示所有成员（包括自己）
- ✅ 主持人可以对其他普通成员进行踢出和拉黑操作
- ✅ 不能对自己或其他主持人进行操作
- ✅ 视频区域继续只显示其他成员（不包括自己）
- ✅ 两个数组独立维护，互不影响

修改完成后，刷新页面即可看到效果！
