package com.easymeeting.entity.enums;

/**
 * 通知类别枚举
 */
public enum NotificationCategory {
    CONTACT("联系人消息"),
    MEETING("会议消息"),
    SYSTEM("系统消息");
    
    private String description;
    
    NotificationCategory(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据字符串获取枚举
     */
    public static NotificationCategory fromString(String category) {
        if (category == null) {
            return null;
        }
        try {
            return NotificationCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
