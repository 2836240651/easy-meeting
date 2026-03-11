package com.easymeeting.entity.query;

import java.util.List;

/**
 * 用户通知查询条件类
 */
public class UserNotificationQuery extends BaseParam {
    
    private Integer notificationId;
    private String userId;
    private Integer notificationType;
    private List<Integer> notificationTypeList; // 新增：支持多个通知类型查询
    private String relatedUserId;
    private Integer status;
    private Integer actionRequired;
    private Integer actionStatus;
    private String referenceId;

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(Integer notificationType) {
        this.notificationType = notificationType;
    }

    public List<Integer> getNotificationTypeList() {
        return notificationTypeList;
    }

    public void setNotificationTypeList(List<Integer> notificationTypeList) {
        this.notificationTypeList = notificationTypeList;
    }

    public String getRelatedUserId() {
        return relatedUserId;
    }

    public void setRelatedUserId(String relatedUserId) {
        this.relatedUserId = relatedUserId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getActionRequired() {
        return actionRequired;
    }

    public void setActionRequired(Integer actionRequired) {
        this.actionRequired = actionRequired;
    }

    public Integer getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(Integer actionStatus) {
        this.actionStatus = actionStatus;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
}
