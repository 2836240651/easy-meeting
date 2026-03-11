package com.easymeeting.entity.dto;

import java.io.Serializable;

/**
 * 用户在线状态变更DTO
 */
public class UserOnlineStatusDto implements Serializable {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 在线状态：1-在线，0-离线
     */
    private Integer onlineStatus;
    
    /**
     * 最后登录时间
     */
    private Long lastLoginTime;
    
    /**
     * 最后离线时间
     */
    private Long lastOffTime;
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Integer getOnlineStatus() {
        return onlineStatus;
    }
    
    public void setOnlineStatus(Integer onlineStatus) {
        this.onlineStatus = onlineStatus;
    }
    
    public Long getLastLoginTime() {
        return lastLoginTime;
    }
    
    public void setLastLoginTime(Long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
    
    public Long getLastOffTime() {
        return lastOffTime;
    }
    
    public void setLastOffTime(Long lastOffTime) {
        this.lastOffTime = lastOffTime;
    }
}
