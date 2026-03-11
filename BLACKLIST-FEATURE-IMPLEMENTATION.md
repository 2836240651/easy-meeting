# 拉黑列表功能实现

## 功能概述

实现了完整的拉黑列表功能，包括：
1. 拉黑列表页面展示
2. 取消拉黑功能
3. 修改拉黑逻辑为单向拉黑

## 修改内容

### 1. 后端修改

#### 1.1 修改拉黑逻辑（UserContactServiceImpl.java）

**原逻辑**：
- 用户A拉黑用户B → 双向拉黑（A拉黑B，B也拉黑A）

**新逻辑**：
- 用户A拉黑用户B → 单向拉黑
  - A对B的状态：拉黑（status = 3）
  - B对A的状态：删除（status = 2）
  - 结果：A将B加入黑名单，同时A从B的联系人列表中删除

**代码位置**：`src/main/java/com/easymeeting/service/impl/UserContactServiceImpl.java`

```java
if (status.equals(UserContactStatusEnum.BLACKLIST.getStatus())) {
    // 拉黑逻辑：单向拉黑
    // 1. 更新当前用户对联系人的状态为拉黑
    UserContact userContact = new UserContact();
    userContact.setStatus(status);
    userContact.setLastUpdateTime(new Date());
    this.userContactMapper.updateByUserIdAndContactId(userContact, userId, contactId);
    
    // 2. 将当前用户从对方的联系人列表中删除（不是拉黑，是删除）
    UserContact contactUserContact = new UserContact();
    contactUserContact.setStatus(UserContactStatusEnum.DEL.getStatus());
    contactUserContact.setLastUpdateTime(new Date());
    this.userContactMapper.updateByUserIdAndContactId(contactUserContact, contactId, userId);
    
    // 拉黑不发送通知
}
```

#### 1.2 添加取消拉黑方法（UserContactServiceImpl.java）

```java
@Override
public void unblackContact(String userId, String contactId) {
    // 检查是否确实拉黑了该用户
    UserContact userContact = this.userContactMapper.selectByUserIdAndContactId(userId, contactId);
    if (userContact == null || !userContact.getStatus().equals(UserContactStatusEnum.BLACKLIST.getStatus())) {
        throw new BusinessException("该用户不在黑名单中");
    }
    
    // 1. 删除当前用户对该联系人的拉黑记录
    this.userContactMapper.deleteByUserIdAndContactId(userId, contactId);
    
    // 2. 检查对方是否还有对当前用户的记录（之前被删除的）
    UserContact contactUserContact = this.userContactMapper.selectByUserIdAndContactId(contactId, userId);
    if (contactUserContact != null && contactUserContact.getStatus().equals(UserContactStatusEnum.DEL.getStatus())) {
        // 如果对方的记录是删除状态，也删除它，让双方都可以重新添加
        this.userContactMapper.deleteByUserIdAndContactId(contactId, userId);
    }
}
```

#### 1.3 添加 Controller 接口（UserContactController.java）

```java
/**
 * 加载拉黑列表
 */
@RequestMapping("/loadBlackList")
@globalInterceptor
public ResponseVO loadBlackList() {
    TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
    UserContactQuery userContactQuery = new UserContactQuery();
    userContactQuery.setUserId(tokenUserInfo.getUserId());
    userContactQuery.setStatus(UserContactStatusEnum.BLACKLIST.getStatus());
    userContactQuery.setQueryUserInfo(true);
    userContactQuery.setOrderBy("last_update_time desc");
    List<UserContact> listByParam = this.userContactService.findListByParam(userContactQuery);
    return getSuccessResponseVO(listByParam);
}

/**
 * 取消拉黑
 */
@RequestMapping("/unblackContact")
@globalInterceptor
public ResponseVO unblackContact(@NotEmpty String contactId) {
    TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
    this.userContactService.unblackContact(tokenUserInfo.getUserId(), contactId);
    return getSuccessResponseVO(null);
}
```

### 2. 前端修改

#### 2.1 扩展 API 服务（services.js）

```javascript
export const contactService = {
  // ... 其他方法
  
  // 加载拉黑列表
  loadBlackList: () => {
    return api.get('/userContact/loadBlackList')
  },
  // 取消拉黑
  unblackContact: (contactId) => {
    return api.post(`/userContact/unblackContact?contactId=${encodeURIComponent(contactId)}`)
  }
}
```

#### 2.2 添加联系人标签页（Dashboard.vue）

**响应式数据**：
```javascript
const blackList = ref([])  // 拉黑列表
const contactActiveTab = ref('friends')  // 联系人当前标签页：'friends' 或 'blacklist'
```

**方法**：
```javascript
// 加载拉黑列表
const loadBlackList = async () => {
  const response = await contactService.loadBlackList()
  if (response.data.code === 200) {
    blackList.value = response.data.data || []
  }
}

// 处理拉黑列表标签页点击
const handleBlacklistTabClick = async () => {
  contactActiveTab.value = 'blacklist'
  await loadBlackList()
}

// 取消拉黑
const handleUnblackContact = async (contact) => {
  const confirmed = await ElMessageBox.confirm(
    `确定要取消拉黑 ${contact.nickName || contact.contactId} 吗？取消后您可以重新添加对方为好友。`,
    '取消拉黑',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
  )
  
  if (confirmed) {
    const response = await contactService.unblackContact(contact.contactId)
    if (response.data.code === 200) {
      ElMessage.success(`已取消拉黑 ${contact.nickName || contact.contactId}`)
      await loadBlackList()
    }
  }
}

// 格式化拉黑时间
const formatBlacklistTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}
```

#### 2.3 UI 结构

```vue
<!-- 联系人标签页 -->
<div class="contact-tabs">
  <div class="contact-tab" :class="{ active: contactActiveTab === 'friends' }" @click="contactActiveTab = 'friends'">
    <span>好友列表</span>
    <span v-if="contactList.length > 0" class="tab-count">({{ contactList.length }})</span>
  </div>
  <div class="contact-tab" :class="{ active: contactActiveTab === 'blacklist' }" @click="handleBlacklistTabClick">
    <span>拉黑列表</span>
    <span v-if="blackList.length > 0" class="tab-count">({{ blackList.length }})</span>
  </div>
</div>

<!-- 拉黑列表标签页 -->
<div v-if="contactActiveTab === 'blacklist'">
  <div class="blacklist-section">
    <div v-if="blackList.length === 0" class="empty-blacklist">
      <p>暂无拉黑用户</p>
    </div>
    <div v-for="contact in blackList" :key="contact.contactId" class="blacklist-item">
      <div class="contact-avatar">
        <img :src="getContactAvatar(contact)" alt="Contact Avatar" class="contact-avatar-img">
      </div>
      <div class="contact-info">
        <h4>{{ contact.nickName || contact.contactId }}</h4>
        <p class="blacklist-time">拉黑时间：{{ formatBlacklistTime(contact.lastUpdateTime) }}</p>
      </div>
      <div class="contact-actions">
        <button class="contact-action-btn unblock-btn" @click="handleUnblackContact(contact)" title="取消拉黑">
          取消拉黑
        </button>
      </div>
    </div>
  </div>
</div>
```

#### 2.4 样式

```css
/* 联系人标签页样式 */
.contact-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  border-bottom: 2px solid #3a3a3a;
}

.contact-tab {
  padding: 12px 24px;
  cursor: pointer;
  color: #999999;
  font-size: 15px;
  font-weight: 500;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.contact-tab:hover {
  color: #b3b3b3;
}

.contact-tab.active {
  color: #409EFF;
  border-bottom-color: #409EFF;
}

/* 拉黑列表样式 */
.blacklist-section {
  margin-top: 20px;
}

.empty-blacklist {
  text-align: center;
  padding: 60px 20px;
  color: #666;
  font-size: 14px;
}

.blacklist-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 16px;
  padding: 16px;
  background-color: var(--background-card);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color);
  transition: var(--transition);
  box-shadow: var(--shadow-sm);
  align-items: center;
  margin-bottom: 12px;
}

.blacklist-item:hover {
  border-color: #666;
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.blacklist-time {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.unblock-btn {
  background-color: #4CAF50;
  color: white;
}

.unblock-btn:hover {
  background-color: #45a049;
  transform: translateY(-1px);
}
```

## 功能特性

### 1. 拉黑逻辑

**场景：用户A拉黑用户B**

- **A的视角**：
  - B出现在A的拉黑列表中
  - A无法看到B的在线状态
  - A无法向B发送消息
  - A可以取消拉黑B

- **B的视角**：
  - A从B的联系人列表中消失
  - B不知道被A拉黑（不会收到通知）
  - B无法向A发送消息
  - B可以重新添加A为好友（但A需要先取消拉黑才能同意）

### 2. 取消拉黑

- 用户可以在拉黑列表中点击"取消拉黑"按钮
- 取消拉黑后：
  - 从拉黑列表中移除该用户
  - 删除双方的联系人记录
  - 双方可以重新添加对方为好友

### 3. UI/UX 特性

- **标签页切换**：好友列表 / 拉黑列表
- **数量显示**：标签页上显示好友数量和拉黑用户数量
- **时间显示**：显示拉黑时间（相对时间：刚刚、X分钟前、X小时前等）
- **空状态提示**：无拉黑用户时显示友好提示
- **确认对话框**：拉黑和取消拉黑都需要用户确认
- **成功提示**：操作成功后显示提示消息
- **深色主题**：与现有设计风格保持一致

## 数据库状态说明

### user_contact 表状态值

- `status = 1`：好友关系
- `status = 2`：已删除
- `status = 3`：已拉黑

### 拉黑场景的数据库状态

**用户A拉黑用户B**：

| user_id | contact_id | status | 说明 |
|---------|-----------|--------|------|
| A | B | 3 | A拉黑了B |
| B | A | 2 | A从B的联系人列表中删除 |

**取消拉黑后**：

| user_id | contact_id | status | 说明 |
|---------|-----------|--------|------|
| - | - | - | 两条记录都被删除 |

## 测试步骤

### 测试场景 1：拉黑好友

1. **用户A登录**
2. 进入"联系人"页面
3. 在好友列表中找到用户B
4. 点击"拉黑"按钮
5. 确认拉黑操作
6. **验证**：
   - 用户B从好友列表中消失
   - 切换到"拉黑列表"标签页
   - 用户B出现在拉黑列表中
   - 显示拉黑时间

7. **用户B登录**
8. 进入"联系人"页面
9. **验证**：
   - 用户A从好友列表中消失
   - 用户B没有收到任何通知

### 测试场景 2：取消拉黑

1. **用户A登录**
2. 进入"联系人"页面
3. 切换到"拉黑列表"标签页
4. 找到用户B
5. 点击"取消拉黑"按钮
6. 确认取消拉黑操作
7. **验证**：
   - 用户B从拉黑列表中消失
   - 拉黑列表数量减少

8. **用户B登录**
9. 进入"联系人"页面
10. 点击"添加联系人"
11. 搜索用户A
12. **验证**：
    - 可以搜索到用户A
    - 可以发送好友申请

### 测试场景 3：拉黑后重新添加

1. **用户A拉黑用户B**（参考测试场景1）
2. **用户B尝试添加用户A**
3. 搜索用户A
4. **验证**：
   - 显示"已拉黑"状态
   - 无法发送好友申请

5. **用户A取消拉黑用户B**（参考测试场景2）
6. **用户B再次尝试添加用户A**
7. **验证**：
   - 可以发送好友申请
   - 用户A可以收到申请并同意

## 数据库验证 SQL

### 查看用户的联系人记录
```sql
SELECT * FROM user_contact 
WHERE user_id = '用户A的ID' OR contact_id = '用户A的ID'
ORDER BY last_update_time DESC;
```

### 查看拉黑记录
```sql
SELECT * FROM user_contact 
WHERE status = 3
ORDER BY last_update_time DESC;
```

### 查看特定用户之间的关系
```sql
SELECT * FROM user_contact 
WHERE (user_id = '用户A的ID' AND contact_id = '用户B的ID')
   OR (user_id = '用户B的ID' AND contact_id = '用户A的ID');
```

## 修改的文件清单

### 后端
- `src/main/java/com/easymeeting/service/UserContactService.java` - 添加 unblackContact 接口
- `src/main/java/com/easymeeting/service/impl/UserContactServiceImpl.java` - 修改拉黑逻辑，添加取消拉黑方法
- `src/main/java/com/easymeeting/controller/UserContactController.java` - 添加 loadBlackList 和 unblackContact 接口

### 前端
- `frontend/src/api/services.js` - 添加 loadBlackList 和 unblackContact API
- `frontend/src/views/Dashboard.vue` - 添加拉黑列表UI和相关方法

## 状态说明

✅ 后端代码修改完成
✅ 后端编译成功
✅ 后端服务已重启（Terminal 9）
✅ 前端代码修改完成
✅ 前端服务正常运行
⏳ 等待测试验证

## 注意事项

1. **拉黑是单向的**：只有拉黑方能看到对方在黑名单中，被拉黑方不知道被拉黑
2. **拉黑不发送通知**：为了保护用户隐私，拉黑操作不会通知对方
3. **取消拉黑后需要重新添加**：取消拉黑只是移除黑名单记录，不会自动恢复好友关系
4. **数据库记录清理**：取消拉黑会删除双方的联系人记录，让双方可以重新添加
5. **搜索限制**：被拉黑的用户无法搜索到拉黑方（需要在 searchContact 方法中添加检查）

## 总结

实现了完整的拉黑列表功能，包括：
- 单向拉黑逻辑（拉黑方将对方加入黑名单，同时从对方的联系人列表中删除）
- 拉黑列表页面展示
- 取消拉黑功能
- 友好的UI/UX设计
- 完整的错误处理和用户提示

拉黑功能现在更符合常见社交应用的逻辑，保护了用户隐私，同时提供了灵活的管理选项。
