# 通知系统完整实现指南

## 前置条件

1. ✅ 执行 `create-notification-table.sql` 创建数据库表
2. ✅ 创建 `UserNotification.java` 实体类

## 需要创建的文件清单

### 后端文件（按顺序创建）

1. ✅ `src/main/java/com/easymeeting/entity/po/UserNotification.java` - 实体类
2. ⬜ `src/main/java/com/easymeeting/entity/query/UserNotificationQuery.java` - 查询条件类
3. ⬜ `src/main/java/com/easymeeting/entity/enums/NotificationTypeEnum.java` - 通知类型枚举
4. ⬜ `src/main/java/com/easymeeting/mappers/UserNotificationMapper.java` - Mapper接口
5. ⬜ `src/main/resources/com/easymeeting/mappers/UserNotificationMapper.xml` - Mapper XML
6. ⬜ `src/main/java/com/easymeeting/service/UserNotificationService.java` - Service接口
7. ⬜ `src/main/java/com/easymeeting/service/impl/UserNotificationServiceImpl.java` - Service实现
8. ⬜ `src/main/java/com/easymeeting/controller/UserNotificationController.java` - Controller
9. ⬜ 修改 `UserContactApplyServiceImpl.java` - 添加创建通知逻辑
10. ⬜ 修改 `UserContactServiceImpl.java` - 添加删除通知逻辑

### 前端文件

11. ⬜ 修改 `frontend/src/api/services.js` - 添加通知API
12. ⬜ 修改 `frontend/src/views/Dashboard.vue` - 集成通知系统

## 详细实现步骤

由于代码量较大，我建议使用以下方式之一：

### 方式一：使用代码生成器（推荐）

如果你的项目有代码生成器，可以基于 `user_notification` 表自动生成：
- Entity
- Query
- Mapper接口
- Mapper XML
- Service接口
- Service实现

然后只需要：
1. 创建 Controller
2. 修改现有的 Service 添加通知创建逻辑
3. 修改前端集成

### 方式二：手动创建（我来帮你）

我可以帮你创建所有必要的文件。由于文件较多，我会：
1. 先创建核心的后端文件（Mapper、Service）
2. 然后创建 Controller
3. 修改现有逻辑集成通知
4. 最后修改前端

你希望我继续创建剩余的文件吗？我会分批次创建，每次创建2-3个文件。

## 快速开始（如果你想自己实现）

### 1. UserNotificationQuery.java

```java
package com.easymeeting.entity.query;

public class UserNotificationQuery extends BaseParam {
    private Integer notificationId;
    private String userId;
    private Integer notificationType;
    private Integer status;
    private Integer actionRequired;
    private Integer actionStatus;
    private String referenceId;
    
    // Getters and Setters
}
```

### 2. NotificationTypeEnum.java

```java
package com.easymeeting.entity.enums;

public enum NotificationTypeEnum {
    CONTACT_APPLY(1, "好友申请"),
    CONTACT_DELETE(2, "联系人删除"),
    SYSTEM_NOTICE(3, "系统通知");
    
    private Integer type;
    private String desc;
    
    NotificationTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
    
    public Integer getType() {
        return type;
    }
    
    public String getDesc() {
        return desc;
    }
}
```

### 3. 关键Service方法

```java
// UserNotificationService.java
public interface UserNotificationService {
    // 创建通知
    void createNotification(UserNotification notification);
    
    // 获取通知列表
    List<UserNotification> getNotificationList(String userId, Integer pageNo, Integer pageSize);
    
    // 获取未读数量
    Integer getUnreadCount(String userId);
    
    // 标记为已读
    void markAsRead(Integer notificationId, String userId);
    
    // 全部标记为已读
    void markAllAsRead(String userId);
    
    // 更新操作状态
    void updateActionStatus(String userId, String referenceId, Integer actionStatus);
}
```

### 4. 集成到现有逻辑

#### 好友申请时创建通知

在 `UserContactApplyServiceImpl.saveUserContactApply()` 方法中添加：

```java
// 创建好友申请通知
UserNotification notification = new UserNotification();
notification.setUserId(bean.getReceiveUserId());
notification.setNotificationType(NotificationTypeEnum.CONTACT_APPLY.getType());
notification.setRelatedUserId(bean.getApplyUserId());
// 获取申请用户昵称
UserInfo applyUser = userInfoMapper.selectByUserId(bean.getApplyUserId());
notification.setRelatedUserName(applyUser != null ? applyUser.getNickName() : bean.getApplyUserId());
notification.setTitle("好友申请");
notification.setContent(notification.getRelatedUserName() + " 请求添加您为好友");
notification.setStatus(0); // 未读
notification.setActionRequired(1); // 需要操作
notification.setActionStatus(0); // 待处理
notification.setReferenceId("apply_" + bean.getApplyUserId() + "_" + bean.getReceiveUserId());
userNotificationService.createNotification(notification);
```

#### 处理好友申请时更新通知

在 `UserContactApplyServiceImpl.dealWithApply()` 方法中添加：

```java
// 更新通知的操作状态
String referenceId = "apply_" + applyUserId + "_" + userId;
userNotificationService.updateActionStatus(userId, referenceId, status);
```

#### 删除好友时创建通知

在 `UserContactServiceImpl.delContact()` 方法中添加：

```java
if (status.equals(UserContactStatusEnum.DEL.getStatus())) {
    // 获取当前用户信息
    UserInfo userInfo = this.userInfoMapper.selectByUserId(userId);
    String nickName = userInfo != null ? userInfo.getNickName() : userId;
    
    // 创建联系人删除通知
    UserNotification notification = new UserNotification();
    notification.setUserId(contactId);
    notification.setNotificationType(NotificationTypeEnum.CONTACT_DELETE.getType());
    notification.setRelatedUserId(userId);
    notification.setRelatedUserName(nickName);
    notification.setTitle("联系人删除通知");
    notification.setContent(nickName + " 已将您从好友列表中删除");
    notification.setStatus(0); // 未读
    notification.setActionRequired(0); // 不需要操作
    userNotificationService.createNotification(notification);
    
    // 发送WebSocket通知...
}
```

## 下一步

请告诉我你希望：
1. 我继续创建剩余的后端文件
2. 你自己根据指南实现
3. 其他方式

我会根据你的选择继续协助。
