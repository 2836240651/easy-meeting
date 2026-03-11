# 预约会议密码功能

## 功能说明

为预约会议添加了加入方式选择功能，支持无需密码和需要密码两种模式。

## 功能特性

### 1. 加入方式选择

使用单选按钮组，提供两个选项：
- **无需密码**：默认选项，受邀成员可直接加入
- **需要密码**：需要输入5位数字密码才能加入

### 2. 密码输入

当选择"需要密码"时：
- 显示密码输入框
- 限制输入5位数字
- 显示字数统计
- 支持清除按钮
- 提示文字："密码为5位数字，用于验证参会者身份"

### 3. 表单验证

密码字段验证规则：
- 只有选择"需要密码"时才验证
- 必填验证：密码不能为空
- 格式验证：必须是5位数字（正则表达式：`^\d{5}$`）
- 错误提示：
  - 未输入：`请输入会议密码`
  - 格式错误：`密码必须是5位数字`

### 4. 数据提交

提交时根据加入方式处理密码：
```javascript
{
  joinType: form.value.joinType,  // 0-无需密码，1-需要密码
  joinPassword: form.value.joinType === 1 ? form.value.joinPassword : null
}
```

- `joinType = 0`：`joinPassword` 设为 `null`
- `joinType = 1`：`joinPassword` 为用户输入的5位数字

### 5. 编辑模式

编辑已有会议时：
- 正确回显加入方式（`joinType`）
- 正确回显密码（如果有）
- 支持修改加入方式和密码

## UI 设计

### 表单布局

```
会议名称: [输入框]
开始时间: [日期时间选择器]
会议时长: [60 分钟] (禁用)
加入方式: ○ 无需密码  ○ 需要密码
会议密码: [输入框] (仅当选择"需要密码"时显示)
邀请成员: [多选下拉框]
```

### 样式特点

- 单选按钮使用深色主题
- 选中状态使用主题色 `#999999`
- 密码输入框与其他输入框样式一致
- 提示文字使用灰色 `#999`

## 代码实现

### 模板部分

```vue
<el-form-item label="加入方式" prop="joinType">
  <el-radio-group v-model="form.joinType">
    <el-radio :label="0">无需密码</el-radio>
    <el-radio :label="1">需要密码</el-radio>
  </el-radio-group>
</el-form-item>

<el-form-item 
  v-if="form.joinType === 1" 
  label="会议密码" 
  prop="joinPassword"
>
  <el-input
    v-model="form.joinPassword"
    placeholder="请输入5位数字密码"
    maxlength="5"
    show-word-limit
    clearable
  />
  <div class="form-tip">密码为5位数字，用于验证参会者身份</div>
</el-form-item>
```

### 表单数据

```javascript
const form = ref({
  meetingName: '',
  startTime: null,
  duration: 60,
  joinType: 0,        // 0-无需密码，1-需要密码
  joinPassword: '',   // 5位数字密码
  inviteUserIds: []
})
```

### 验证规则

```javascript
const rules = {
  // ... 其他规则
  joinPassword: [
    {
      validator: (rule, value, callback) => {
        if (form.value.joinType === 1) {
          if (!value || value.trim() === '') {
            callback(new Error('请输入会议密码'))
          } else if (!/^\d{5}$/.test(value)) {
            callback(new Error('密码必须是5位数字'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}
```

### 提交逻辑

```javascript
const params = {
  meetingName: form.value.meetingName.trim(),
  startTime: form.value.startTime,
  duration: 60,
  joinType: form.value.joinType,
  joinPassword: form.value.joinType === 1 ? form.value.joinPassword : null,
  inviteUserIds: form.value.inviteUserIds.join(',')
}
```

## 使用场景

### 场景 1：公开会议
- 选择"无需密码"
- 所有受邀成员可直接加入
- 适合内部团队会议

### 场景 2：私密会议
- 选择"需要密码"
- 输入5位数字密码（如：12345）
- 只有知道密码的人才能加入
- 适合敏感话题讨论

### 场景 3：编辑会议
- 可以修改加入方式
- 可以添加或移除密码
- 可以修改密码

## 后端兼容性

后端已支持 `joinType` 和 `joinPassword` 字段：
- `meeting_reserve` 表包含这两个字段
- `MeetingReserveServiceImpl` 正确处理这些字段
- 创建和更新接口都支持

## 测试要点

1. **创建无密码会议**：
   - 选择"无需密码"
   - 提交成功
   - 验证 `joinType = 0`，`joinPassword = null`

2. **创建有密码会议**：
   - 选择"需要密码"
   - 输入5位数字（如：12345）
   - 提交成功
   - 验证 `joinType = 1`，`joinPassword = "12345"`

3. **密码验证**：
   - 选择"需要密码"但不输入密码 → 显示错误
   - 输入少于5位 → 显示错误
   - 输入非数字字符 → 显示错误
   - 输入正确的5位数字 → 验证通过

4. **编辑会议**：
   - 编辑无密码会议，改为有密码
   - 编辑有密码会议，改为无密码
   - 编辑有密码会议，修改密码

5. **UI 交互**：
   - 切换加入方式时，密码输入框正确显示/隐藏
   - 单选按钮样式正确
   - 深色主题下显示正常

## 相关文件

- `frontend/src/components/ScheduleMeetingModal.vue` - 预约会议模态框
- `src/main/java/com/easymeeting/entity/po/MeetingReserve.java` - 会议实体类
- `src/main/java/com/easymeeting/service/impl/MeetingReserveServiceImpl.java` - 会议服务实现

## 注意事项

1. 密码必须是5位数字，不支持其他格式
2. 密码明文存储在数据库中（考虑安全性，可以后续加密）
3. 切换加入方式时，密码输入框会自动显示/隐藏
4. 表单验证只在选择"需要密码"时才验证密码字段
