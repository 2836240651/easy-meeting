package com.easymeeting.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户通知实体类
 */
public class UserNotification implements Serializable {

    /**
     * 通知ID
     */
    private Integer notificationId;

    /**
     * 接收通知的用户ID
     */
    private String userId;

    /**
     * 通知类型：1=好友申请，2=联系人删除，3=系统通知
     */
    private Integer notificationType;

    /**
     * 相关用户ID
     */
    private String relatedUserId;

    /**
     * 相关用户昵称
     */
    private String relatedUserName;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知状态：0=未读，1=已读
     */
    private Integer status;

    /**
     * 是否需要操作：0=不需要，1=需要
     */
    private Integer actionRequired;

    /**
     * 操作状态：0=待处理，1=已同意，2=已拒绝
     */
    private Integer actionStatus;

    /**
     * 关联ID（如申请ID）
     */
    private String referenceId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    // Getters and Setters
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

    public String getRelatedUserId() {
        return relatedUserId;
    }

    public void setRelatedUserId(String relatedUserId) {
        this.relatedUserId = relatedUserId;
    }

    public String getRelatedUserName() {
        return relatedUserName;
    }

    public void setRelatedUserName(String relatedUserName) {
        this.relatedUserName = relatedUserName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "UserNotification{" +
                "notificationId=" + notificationId +
                ", userId='" + userId + '\'' +
                ", notificationType=" + notificationType +
                ", relatedUserId='" + relatedUserId + '\'' +
                ", relatedUserName='" + relatedUserName + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", status=" + status +
                ", actionRequired=" + actionRequired +
                ", actionStatus=" + actionStatus +
                ", referenceId='" + referenceId + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
