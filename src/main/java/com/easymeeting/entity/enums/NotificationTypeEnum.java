package com.easymeeting.entity.enums;

/**
 * 通知类型枚举
 * 支持 11 种通知类型，分为联系人、会议、系统三大类
 */
public enum NotificationTypeEnum {
    // 联系人类消息（1-4）
    CONTACT_APPLY_PENDING(1, "好友申请待处理"),
    CONTACT_APPLY_ACCEPTED(2, "好友申请已同意"),
    CONTACT_APPLY_REJECTED(3, "好友申请已拒绝"),
    CONTACT_DELETED(4, "联系人删除通知"),
    
    // 会议类消息（5-11）
    MEETING_INVITE_PENDING(5, "会议邀请待处理"),
    MEETING_INVITE_ACCEPTED(6, "会议邀请已接受"),
    MEETING_INVITE_REJECTED(7, "会议邀请已拒绝"),
    MEETING_CANCELLED(8, "会议取消通知"),
    MEETING_TIME_CHANGED(9, "会议时间变更通知"),
    MEETING_INSTANT_INVITE(10, "即时会议邀请"),
    MEETING_REMINDER(11, "会议提醒"),
    
    // 系统消息（12-13）
    SYSTEM_NOTIFICATION(12, "系统通知"),
    SYSTEM_MAINTENANCE(13, "维护通知");
    
    private Integer type;
    private String desc;
    
    NotificationTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
    
    /**
     * 根据类型值获取枚举
     */
    public static NotificationTypeEnum getByType(Integer type) {
        if (type == null) {
            return null;
        }
        for (NotificationTypeEnum item : NotificationTypeEnum.values()) {
            if (item.type.equals(type)) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * 获取通知类别
     */
    public NotificationCategory getCategory() {
        if (type >= 1 && type <= 4) {
            return NotificationCategory.CONTACT;
        }
        if (type >= 5 && type <= 11) {
            return NotificationCategory.MEETING;
        }
        if (type >= 12 && type <= 13) {
            return NotificationCategory.SYSTEM;
        }
        return NotificationCategory.SYSTEM;
    }
    
    public Integer getType() {
        return type;
    }
    
    public String getDesc() {
        return desc;
    }
}
