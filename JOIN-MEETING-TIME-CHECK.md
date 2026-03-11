# 预约会议加入时间检查功能

## 功能描述
为预约会议的"加入会议"按钮添加时间检查逻辑，只有在会议开始前5分钟到会议结束期间才能加入会议。如果用户在不可加入的时间点击按钮，会显示友好的提示信息。

## 实现内容

### 1. 时间检查逻辑

**文件**: `frontend/src/components/ReservationList.vue`

#### 可加入会议的条件
- 会议状态为 0（未开始/进行中）
- 会议未结束（当前时间 < 结束时间）
- 当前时间 >= 开始时间 - 5分钟（提前5分钟可以加入）

#### 实现的函数

1. **canJoinMeeting(meeting)** - 检查是否可以加入会议
```javascript
const canJoinMeeting = (meeting) => {
  const now = Date.now()
  const startTime = typeof meeting.startTime === 'string' 
    ? new Date(meeting.startTime).getTime() 
    : meeting.startTime
  const endTime = startTime + meeting.duration * 60 * 1000
  
  // 会议已取消或已结束
  if (meeting.status !== 0 || endTime < now) {
    return false
  }
  
  // 会议已开始或即将开始（提前5分钟可以加入）
  const fiveMinutesBefore = startTime - 5 * 60 * 1000
  return now >= fiveMinutesBefore
}
```

2. **getJoinButtonText(meeting)** - 获取按钮文字
- 可加入：显示"加入会议"
- 已取消：显示"会议已取消"
- 已结束：显示"会议已结束"
- 未开始：显示"等待开始"

3. **joinMeeting(meeting)** - 加入会议（带时间检查）
- 如果不可加入，显示友好提示
- 提示信息包含距离开始的剩余时间
- 例如："会议未开始，请等待预约时间。距离开始还有 1小时30分钟"

### 2. UI 改进

#### 按钮状态
```vue
<el-button 
  type="primary" 
  size="small" 
  @click="joinMeeting(meeting)"
  :disabled="!canJoinMeeting(meeting)"
>
  {{ getJoinButtonText(meeting) }}
</el-button>
```

#### 禁用状态样式
```css
.meeting-actions .el-button.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
```

## 用户体验

### 场景 1: 会议未开始（距离开始超过5分钟）
- 按钮显示：**等待开始**
- 按钮状态：禁用（灰色）
- 点击提示：会议未开始，请等待预约时间。距离开始还有 X小时X分钟

### 场景 2: 会议即将开始（距离开始少于5分钟）
- 按钮显示：**加入会议**
- 按钮状态：启用（蓝色）
- 点击行为：正常加入会议流程

### 场景 3: 会议进行中
- 按钮显示：**加入会议**
- 按钮状态：启用（蓝色）
- 点击行为：正常加入会议流程

### 场景 4: 会议已结束
- 按钮显示：**会议已结束**
- 按钮状态：禁用（灰色）
- 点击提示：会议已结束

### 场景 5: 会议已取消
- 按钮显示：**会议已取消**
- 按钮状态：禁用（灰色）
- 点击提示：会议已结束

## 时间计算逻辑

### 剩余时间显示
```javascript
const diff = startTime - now
const minutes = Math.floor(diff / 60000)
const hours = Math.floor(minutes / 60)
const remainMinutes = minutes % 60

// 显示格式
if (hours > 0) {
  timeText = `${hours}小时${remainMinutes}分钟`
} else {
  timeText = `${minutes}分钟`
}
```

### 示例
- 90分钟 → "1小时30分钟"
- 45分钟 → "45分钟"
- 5分钟 → "5分钟"

## 测试步骤

### 1. 测试未开始的会议
1. 创建一个开始时间为1小时后的预约会议
2. 在预约会议列表中查看该会议
3. 验证"加入会议"按钮显示为"等待开始"且禁用
4. 点击按钮，验证提示信息显示剩余时间

### 2. 测试即将开始的会议
1. 创建一个开始时间为3分钟后的预约会议
2. 在预约会议列表中查看该会议
3. 验证"加入会议"按钮显示为"加入会议"且启用
4. 点击按钮，验证可以正常加入会议

### 3. 测试进行中的会议
1. 创建一个开始时间为当前时间的预约会议
2. 在预约会议列表中查看该会议
3. 验证"加入会议"按钮显示为"加入会议"且启用
4. 点击按钮，验证可以正常加入会议

### 4. 测试已结束的会议
1. 查看一个已经结束的预约会议
2. 验证"加入会议"按钮显示为"会议已结束"且禁用
3. 点击按钮，验证提示信息

### 5. 测试需要密码的会议
1. 创建一个需要密码的预约会议（开始时间为3分钟后）
2. 点击"加入会议"按钮
3. 验证弹出密码输入对话框
4. 输入正确密码，验证可以加入会议

## 相关文件
- `frontend/src/components/ReservationList.vue` - 预约会议列表组件
- `frontend/src/api/services.js` - API 服务（joinMeetingReserve）
- `test-join-meeting-reserve.html` - API 测试页面

## 注意事项
1. 提前5分钟可以加入会议，给用户足够的准备时间
2. 时间计算使用时间戳，确保跨时区兼容性
3. 按钮文字和状态实时反映会议状态
4. 友好的错误提示，告知用户具体的等待时间
5. 禁用状态的按钮有明显的视觉反馈（灰色、半透明）
