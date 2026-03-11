# 联系人列表集成说明

## 问题描述

预约会议模态框中的"邀请成员"下拉框需要联调后端联系人列表 API。

## 后端 API

### 接口信息
- **URL**: `/api/userContact/loadContactUser`
- **方法**: GET
- **需要认证**: 是（需要 token）

### 返回数据结构

```json
{
  "code": 200,
  "info": "请求成功",
  "data": [
    {
      "userId": "当前用户ID",
      "contactId": "联系人的用户ID",
      "nickName": "联系人昵称",
      "avatar": "联系人头像",
      "status": 1,
      "lastLoginTime": 1234567890000,
      "lastOffTime": 1234567890000,
      "lastUpdateTime": "2024-01-01 12:00:00"
    }
  ]
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | String | 当前用户的ID |
| contactId | String | 联系人的真实用户ID（用于邀请） |
| nickName | String | 联系人昵称 |
| avatar | String | 联系人头像URL |
| status | Integer | 联系人状态（1-好友） |
| lastLoginTime | Long | 最后登录时间戳 |
| lastOffTime | Long | 最后离线时间戳 |

## 前端实现

### 数据映射

后端返回的 `UserContact` 对象需要映射为前端使用的格式：

```javascript
const contacts = result.data.data || []
contactList.value = contacts.map(contact => ({
  userId: contact.contactId,  // 重要：使用 contactId 作为 userId
  nickName: contact.nickName,
  email: contact.email || '',
  avatar: contact.avatar,
  lastLoginTime: contact.lastLoginTime,
  lastOffTime: contact.lastOffTime
}))
```

### 关键点

1. **字段映射**：
   - 后端的 `contactId` → 前端的 `userId`
   - 这是因为 `contactId` 才是联系人的真实用户ID
   - `userId` 是当前用户的ID

2. **加载时机**：
   - 对话框打开时自动加载
   - 使用 `watch` 监听 `props.visible`

3. **错误处理**：
   - 显示详细的错误信息
   - 使用 `ElMessage` 提示用户

### 完整代码

```javascript
// 加载联系人列表
const loadContacts = async () => {
  try {
    console.log('开始加载联系人列表...')
    const result = await contactService.loadContactUser()
    console.log('联系人列表 API 响应:', result)
    
    if (result && result.data) {
      if (result.data.code === 200) {
        // 后端返回的是 UserContact 对象，需要映射字段
        const contacts = result.data.data || []
        contactList.value = contacts.map(contact => ({
          userId: contact.contactId,  // 联系人的真实 userId
          nickName: contact.nickName,
          email: contact.email || '',
          avatar: contact.avatar,
          lastLoginTime: contact.lastLoginTime,
          lastOffTime: contact.lastOffTime
        }))
        console.log('联系人列表加载成功，数量:', contactList.value.length)
      } else {
        console.error('加载联系人失败，错误码:', result.data.code)
        ElMessage.error(result.data.info || '加载联系人失败')
      }
    }
  } catch (error) {
    console.error('加载联系人失败:', error)
    ElMessage.error('加载联系人失败: ' + (error.message || '未知错误'))
  }
}

// 监听对话框打开
watch(() => props.visible, (newVal) => {
  if (newVal) {
    loadContacts()  // 对话框打开时加载联系人
    // ... 其他初始化逻辑
  }
})
```

## 下拉框显示

```vue
<el-select
  v-model="form.inviteUserIds"
  multiple
  filterable
  placeholder="选择要邀请的成员（可选）"
  style="width: 100%"
  collapse-tags
  collapse-tags-tooltip
  :max-collapse-tags="3"
>
  <el-option
    v-for="contact in contactList"
    :key="contact.userId"
    :label="contact.nickName"
    :value="contact.userId"
  >
    <div class="contact-option">
      <span>{{ contact.nickName }}</span>
      <span class="contact-email">{{ contact.email }}</span>
    </div>
  </el-option>
</el-select>
```

## 测试步骤

### 1. 使用测试页面

打开 `test-contact-list-api.html`：
1. 输入有效的 token
2. 点击"测试 loadContactUser"
3. 查看返回的联系人列表
4. 确认字段结构

### 2. 在应用中测试

1. 登录应用
2. 打开预约会议对话框
3. 点击"邀请成员"下拉框
4. 查看控制台日志：
   ```
   开始加载联系人列表...
   联系人列表 API 响应: {...}
   联系人列表加载成功，数量: X
   ```
5. 确认下拉框显示联系人列表

### 3. 验证数据

检查控制台输出的联系人数据：
```javascript
[
  {
    userId: "联系人ID",  // 这是 contactId 映射过来的
    nickName: "联系人昵称",
    email: "",
    avatar: "头像URL",
    lastLoginTime: 时间戳,
    lastOffTime: 时间戳
  }
]
```

## 常见问题

### 1. 下拉框为空

**原因**：
- Token 无效或过期
- 用户没有联系人
- API 返回错误

**解决**：
- 检查控制台日志
- 确认 token 有效
- 使用测试页面验证 API

### 2. 显示的是当前用户ID而不是联系人ID

**原因**：字段映射错误

**解决**：确保使用 `contact.contactId` 而不是 `contact.userId`

### 3. 邀请失败

**原因**：
- 提交的 userId 不正确
- 后端验证失败

**解决**：
- 检查提交的 `inviteUserIds` 数组
- 确认使用的是 `contactId`

## 数据流

```
用户打开对话框
  ↓
触发 watch
  ↓
调用 loadContacts()
  ↓
发送 GET /api/userContact/loadContactUser
  ↓
后端返回 UserContact 列表
  ↓
前端映射字段 (contactId → userId)
  ↓
更新 contactList
  ↓
下拉框显示联系人
  ↓
用户选择联系人
  ↓
form.inviteUserIds 更新
  ↓
提交时发送 inviteUserIds
  ↓
后端创建会议并添加成员
```

## 相关文件

- `frontend/src/components/ScheduleMeetingModal.vue` - 预约会议模态框
- `frontend/src/api/services.js` - API 服务定义
- `src/main/java/com/easymeeting/controller/UserContactController.java` - 后端控制器
- `src/main/java/com/easymeeting/entity/po/UserContact.java` - 联系人实体类
- `test-contact-list-api.html` - API 测试页面

## 注意事项

1. **字段映射很重要**：必须使用 `contactId` 作为邀请的用户ID
2. **错误处理**：添加详细的日志和用户提示
3. **性能优化**：只在对话框打开时加载，避免重复请求
4. **数据验证**：确保返回的数据格式正确
