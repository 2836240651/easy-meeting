# 通知系统实现方案

## 数据库设计

### user_notification 表结构

| 字段名 | 类型 | 说明 |
|--------|------|------|
| notification_id | INT(11) | 通知ID（主键，自增） |
| user_id | VARCHAR(15) | 接收通知的用户ID |
| notification_type | TINYINT(2) | 通知类型：1=好友申请，2=联系人删除，3=系统通知 |
| related_user_id | VARCHAR(15) | 相关用户ID（可选） |
| related_user_name | VARCHAR(50) | 相关用户昵称（可选） |
| title | VARCHAR(100) | 通知标题 |
| content | VARCHAR(500) | 通知内容（可选） |
| status | TINYINT(2) | 通知状态：0=未读，1=已读 |
| action_required | TINYINT(1) | 是否需要操作：0=不需要，1=需要 |
| action_status | TINYINT(2) | 操作状态：0=待处理，1=已同意，2=已拒绝（可选） |
| reference_id | VARCHAR(50) | 关联ID（如申请ID）（可选） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

## 后端实现

### 1. 创建实体类

**UserNotification.java**
```java
package com.easymeeting.entity.po;

import java.io.Serializable;
import java.util.Date;

public class UserNotification implements Serializable {
    private Integer notificationId;
    private String userId;
    private Integer notificationType;
    private String relatedUserId;
    private String relatedUserName;
    private String title;
    private String content;
    private Integer status;
    private Integer actionRequired;
    private Integer actionStatus;
    private String referenceId;
    private Date createTime;
    private Date updateTime;
    
    // Getters and Setters
}
```

### 2. 创建 Mapper

**UserNotificationMapper.xml**
- selectList: 查询通知列表
- selectUnreadCount: 查询未读通知数量
- insert: 插入新通知
- updateStatus: 更新通知状态
- updateActionStatus: 更新操作状态

### 3. 创建 Service

**UserNotificationService.java**
- createNotification: 创建通知
- markAsRead: 标记为已读
- getUnreadCount: 获取未读数量
- getNotificationList: 获取通知列表
- updateActionStatus: 更新操作状态

### 4. 创建 Controller

**UserNotificationController.java**
- /loadNotifications: 加载通知列表
- /getUnreadCount: 获取未读数量
- /markAsRead: 标记为已读
- /markAllAsRead: 全部标记为已读

### 5. 修改现有逻辑

#### 好友申请时创建通知
在 `UserContactApplyServiceImpl.saveUserContactApply()` 中：
```java
// 创建好友申请通知
UserNotification notification = new UserNotification();
notification.setUserId(receiveUserId);
notification.setNotificationType(1); // 好友申请
notification.setRelatedUserId(applyUserId);
notification.setRelatedUserName(applyUserNickName);
notification.setTitle("好友申请");
notification.setContent(applyUserNickName + " 请求添加您为好友");
notification.setActionRequired(1);
notification.setActionStatus(0); // 待处理
notification.setReferenceId(applyId);
userNotificationService.createNotification(notification);
```

#### 处理好友申请时更新通知
在 `UserContactApplyServiceImpl.dealWithApply()` 中：
```java
// 更新通知的操作状态
userNotificationService.updateActionStatusByReference(
    receiveUserId, 
    referenceId, 
    status // 1=已同意，2=已拒绝
);
```

#### 删除好友时创建通知
在 `UserContactServiceImpl.delContact()` 中（删除时）：
```java
if (status.equals(UserContactStatusEnum.DEL.getStatus())) {
    // 创建联系人删除通知
    UserNotification notification = new UserNotification();
    notification.setUserId(contactId);
    notification.setNotificationType(2); // 联系人删除
    notification.setRelatedUserId(userId);
    notification.setRelatedUserName(userNickName);
    notification.setTitle("联系人删除通知");
    notification.setContent(userNickName + " 已将您从好友列表中删除");
    notification.setActionRequired(0); // 不需要操作
    userNotificationService.createNotification(notification);
}
```

## 前端实现

### 1. 修改收件箱显示逻辑

**Dashboard.vue 数据结构**
```javascript
const notificationList = ref([]) // 所有通知列表
const unreadCount = ref(0) // 未读通知数量

// 计算属性：待办通知（未读且需要操作的）
const pendingNotifications = computed(() => {
  return notificationList.value.filter(n => 
    n.status === 0 && n.actionRequired === 1 && n.actionStatus === 0
  )
})

// 计算属性：已读或已处理的通知
const processedNotifications = computed(() => {
  return notificationList.value.filter(n => 
    n.status === 1 || (n.actionRequired === 1 && n.actionStatus !== 0)
  )
})
```

### 2. 收件箱标签页

- **待办消息**：显示 `pendingNotifications`
  - 好友申请（待处理）
  - 其他需要操作的通知
  
- **全部消息**：显示 `notificationList`
  - 所有通知（包括已读、已处理）

### 3. 通知操作逻辑

#### 好友申请通知
- 点击"同意"或"拒绝"后：
  1. 调用 `dealWithApply` API
  2. 更新通知的 `actionStatus`
  3. 通知自动从"待办消息"移到"全部消息"

#### 联系人删除通知
- 打开查看后：
  1. 调用 `markAsRead` API
  2. 更新通知的 `status` 为已读
  3. 通知自动从"待办消息"移到"全部消息"

### 4. API 调用

```javascript
// services.js
export const notificationService = {
  // 加载通知列表
  loadNotifications: (pageNo = 1, pageSize = 20) => {
    return api.post('/notification/loadNotifications', { pageNo, pageSize })
  },
  
  // 获取未读数量
  getUnreadCount: () => {
    return api.get('/notification/getUnreadCount')
  },
  
  // 标记为已读
  markAsRead: (notificationId) => {
    return api.post(`/notification/markAsRead?notificationId=${notificationId}`)
  },
  
  // 全部标记为已读
  markAllAsRead: () => {
    return api.post('/notification/markAllAsRead')
  }
}
```

## 通知类型枚举

### NotificationTypeEnum.java
```java
public enum NotificationTypeEnum {
    CONTACT_APPLY(1, "好友申请"),
    CONTACT_DELETE(2, "联系人删除"),
    SYSTEM_NOTICE(3, "系统通知");
    
    private Integer type;
    private String desc;
    
    // Constructor, getters, etc.
}
```

## 实现步骤

1. ✅ 执行 SQL 创建 `user_notification` 表
2. ⬜ 创建后端实体类、Mapper、Service、Controller
3. ⬜ 修改好友申请逻辑，创建通知
4. ⬜ 修改删除好友逻辑，创建通知
5. ⬜ 修改前端收件箱显示逻辑
6. ⬜ 实现通知的已读/未读状态管理
7. ⬜ 测试完整流程

## 优势

1. **统一管理**：所有通知集中在一个表中
2. **可扩展**：可以轻松添加新的通知类型
3. **状态追踪**：清晰的已读/未读、已处理/未处理状态
4. **历史记录**：保留所有通知历史
5. **性能优化**：通过索引优化查询性能

## 注意事项

1. 定期清理旧通知（如30天前的已读通知）
2. 考虑通知的推送机制（WebSocket）
3. 通知数量过多时的分页处理
4. 通知的优先级排序
