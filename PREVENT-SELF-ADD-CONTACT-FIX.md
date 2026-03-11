# 防止用户添加自己为好友修复

## 问题描述

用户在搜索联系人时，可以搜索到自己并添加自己为好友，导致数据库中出现 `user_id` 和 `contact_id` 相同的记录。

例如：
```
user_id: 6cq7Pg48b4Rq
contact_id: 6cq7Pg48b4Rq
```

## 问题分析

### 后端逻辑

后端 `searchContact` 方法已经正确处理了这种情况：

```java
// 如果搜索的是自己
if (userInfo.getUserId().equals(myUserId)){
    userInfoVo4Search.setStatus(-1); // 特殊状态表示是自己
    return userInfoVo4Search;
}
```

当用户搜索到自己时，返回 `status = -1`。

### 前端问题

前端没有处理 `status = -1` 的情况，仍然显示"添加"按钮，导致用户可以点击添加自己。

## 修复方案

### 1. 修改搜索结果显示

**文件：`frontend/src/views/Dashboard.vue`**

在搜索结果中添加对 `status = -1` 的处理：

```vue
<button 
  v-if="searchContactResult.status === null"
  class="btn-primary" 
  @click="handleApplyContact(searchContactResult.userId)"
>
  添加
</button>
<span v-else-if="searchContactResult.status === -1" class="status-text">这是你自己</span>
<span v-else-if="searchContactResult.status === 0" class="status-text">已发送申请</span>
<span v-else-if="searchContactResult.status === 1" class="status-text">已是好友</span>
<span v-else-if="searchContactResult.status === 3" class="status-text">已拉黑</span>
```

### 2. 修改状态文本函数

修改 `getSearchResultStatus` 函数，添加对所有状态的处理：

```javascript
const getSearchResultStatus = (result) => {
  if (result.status === -1) {
    return '这是你自己'
  } else if (result.status === null) {
    return '可以添加'
  } else if (result.status === 0) {
    return '已发送申请'
  } else if (result.status === 1) {
    return '已是好友'
  } else if (result.status === 3) {
    return '已拉黑'
  }
  return ''
}
```

## 状态说明

### 搜索联系人返回的状态

- `-1` = 搜索到的是自己
- `null` = 可以添加（没有任何关系）
- `0` = 已发送申请（待处理）
- `1` = 已是好友
- `3` = 已拉黑

### 前端显示

| 状态 | 显示文本 | 操作按钮 |
|------|---------|---------|
| -1   | 这是你自己 | 无 |
| null | 可以添加 | 添加按钮 |
| 0    | 已发送申请 | 无 |
| 1    | 已是好友 | 无 |
| 3    | 已拉黑 | 无 |

## 修复效果

### 修复前

1. 用户搜索自己的邮箱或用户ID
2. 显示搜索结果，包含"添加"按钮
3. 用户点击"添加"
4. 发送好友申请给自己
5. 数据库中创建 `user_id = contact_id` 的记录

### 修复后

1. 用户搜索自己的邮箱或用户ID
2. 显示搜索结果，显示"这是你自己"
3. 没有"添加"按钮
4. 用户无法添加自己为好友

## 测试建议

### 1. 搜索自己

1. 登录系统
2. 点击"添加联系人"
3. 输入自己的邮箱或用户ID
4. 点击"搜索"
5. 检查：
   - 是否显示"这是你自己"
   - 是否没有"添加"按钮

### 2. 搜索其他用户

1. 输入其他用户的邮箱或用户ID
2. 点击"搜索"
3. 检查：
   - 如果没有关系，显示"添加"按钮
   - 如果已发送申请，显示"已发送申请"
   - 如果已是好友，显示"已是好友"
   - 如果已拉黑，显示"已拉黑"

### 3. 数据库检查

检查 `user_contact` 表，确保没有 `user_id = contact_id` 的记录：

```sql
SELECT * FROM user_contact WHERE user_id = contact_id;
```

应该返回空结果。

## 清理现有错误数据

如果数据库中已经存在 `user_id = contact_id` 的记录，需要清理：

```sql
-- 查看错误数据
SELECT * FROM user_contact WHERE user_id = contact_id;

-- 删除错误数据
DELETE FROM user_contact WHERE user_id = contact_id;

-- 同时检查申请表
SELECT * FROM user_contact_apply WHERE apply_user_id = receive_user_id;

-- 删除错误申请
DELETE FROM user_contact_apply WHERE apply_user_id = receive_user_id;
```

## 相关文件

### 修改的文件
- `frontend/src/views/Dashboard.vue` - 添加对 status=-1 的处理

### 相关文件
- `src/main/java/com/easymeeting/service/impl/UserContactServiceImpl.java` - 搜索联系人逻辑
- `src/main/java/com/easymeeting/controller/UserContactController.java` - 联系人控制器
- `src/main/java/com/easymeeting/entity/vo/UserInfoVo4Search.java` - 搜索结果VO

## 后端额外保护

虽然前端已经阻止了用户添加自己，但建议在后端也添加验证：

**文件：`src/main/java/com/easymeeting/service/impl/UserContactApplyServiceImpl.java`**

在 `saveUserContactApply` 方法中添加验证：

```java
@Override
public Integer saveUserContactApply(UserContactApply userContactApply) {
    // 验证：不能添加自己为好友
    if (userContactApply.getApplyUserId().equals(userContactApply.getReceiveUserId())) {
        throw new BusinessException("不能添加自己为好友");
    }
    
    // ... 其他逻辑 ...
}
```

这样即使前端被绕过，后端也能阻止这种操作。

## 注意事项

1. **前后端双重验证**
   - 前端验证提供更好的用户体验
   - 后端验证确保数据安全

2. **状态码一致性**
   - 确保前后端对状态码的理解一致
   - 添加新状态时同步更新前后端

3. **错误数据清理**
   - 定期检查并清理错误数据
   - 可以添加数据库约束防止此类错误

4. **用户提示**
   - 提供清晰的状态提示
   - 避免用户困惑

## 后续优化建议

1. **添加数据库约束**
   ```sql
   ALTER TABLE user_contact 
   ADD CONSTRAINT chk_not_self 
   CHECK (user_id != contact_id);
   ```

2. **添加后端验证**
   - 在 `saveUserContactApply` 方法中验证
   - 抛出友好的错误提示

3. **改进搜索逻辑**
   - 搜索时自动排除自己
   - 或者在搜索结果中不显示自己

4. **添加日志**
   - 记录尝试添加自己的行为
   - 便于监控和分析
