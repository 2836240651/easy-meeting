# 联系人列表字段映射修复

## 问题描述
在预约会议功能中，点击"邀请成员"时需要加载联系人列表。前端已经实现了加载逻辑，但后端 MyBatis 映射文件中存在字段拼写错误，导致 `lastOffTime` 字段无法正确映射。

## 修复内容

### 1. 修复 UserContactMapper.xml 中的字段拼写错误

**文件**: `src/main/resources/com/easymeeting/mappers/UserContactMapper.xml`

**问题**:
- `lasgOffTime` 拼写错误（应该是 `lastOffTime`）
- 数据库字段 `last_off_time` 也拼写错误（应该是 `last_off_time`）

**修复**:
```xml
<!-- 修复前 -->
<result column="lasgOffTime" property="lastOffTime"/>
,ui.lasg_off_time lasgOffTime

<!-- 修复后 -->
<result column="lastOffTime" property="lastOffTime"/>
,ui.last_off_time lastOffTime
```

## 数据流程说明

### 后端数据结构
1. **UserContact 表结构**:
   - `userId`: 当前用户ID
   - `contactId`: 联系人的真实 userId
   - `status`: 联系人状态
   - `lastUpdateTime`: 最后更新时间

2. **查询逻辑**:
   - 当 `queryUserInfo=true` 时，会 LEFT JOIN `user_info` 表
   - 通过 `u.contact_id = ui.user_id` 关联获取联系人详细信息
   - 返回字段：`nickName`, `lastLoginTime`, `lastOffTime`, `avatar`

### 前端数据映射
前端在 `ScheduleMeetingModal.vue` 中正确地将后端数据映射：
```javascript
contactList.value = contacts.map(contact => ({
  userId: contact.contactId,  // 联系人的真实 userId（用于邀请）
  nickName: contact.nickName,
  email: contact.email || '',
  avatar: contact.avatar,
  lastLoginTime: contact.lastLoginTime,
  lastOffTime: contact.lastOffTime
}))
```

## 测试步骤

### 1. 重启后端服务
```bash
# 编译已完成，需要重启后端服务
# 停止当前运行的后端服务
# 重新启动后端服务
```

### 2. 测试联系人列表加载
1. 打开 `test-contact-list-api.html`
2. 输入有效的 token
3. 点击"测试 loadContactUser"按钮
4. 验证返回的数据包含：
   - `contactId`: 联系人的真实 userId
   - `nickName`: 联系人昵称
   - `avatar`: 联系人头像
   - `lastLoginTime`: 最后登录时间
   - `lastOffTime`: 最后离线时间（修复后应该正确显示）

### 3. 测试预约会议邀请功能
1. 登录应用
2. 点击"预约会议"按钮
3. 在预约会议对话框中，点击"邀请成员"下拉框
4. 验证：
   - 联系人列表正确加载
   - 显示联系人昵称和邮箱
   - 可以选择多个联系人
   - 选择后显示已选择人数

### 4. 测试创建预约会议
1. 填写会议信息：
   - 会议名称
   - 开始时间（至少1小时后）
   - 会议时长（30/45/60分钟）
   - 加入方式（无需密码/需要密码）
   - 邀请成员（选择联系人）
2. 点击"创建会议"
3. 验证：
   - 会议创建成功
   - 被邀请的成员正确写入 `meeting_reserve_member` 表
   - 包括创建者自己和被邀请的联系人

## 相关文件
- `src/main/resources/com/easymeeting/mappers/UserContactMapper.xml` - 修复字段映射
- `frontend/src/components/ScheduleMeetingModal.vue` - 前端邀请成员功能
- `src/main/java/com/easymeeting/controller/UserContactController.java` - 联系人API
- `src/main/java/com/easymeeting/entity/po/UserContact.java` - 联系人实体类
- `test-contact-list-api.html` - API测试页面

## 注意事项
1. 修改 XML 映射文件后必须重新编译
2. 编译后必须重启后端服务才能生效
3. `contactId` 是联系人的真实 `userId`，用于邀请时传递给后端
4. 前端正确地将 `contactId` 映射为 `userId` 用于下拉选择
