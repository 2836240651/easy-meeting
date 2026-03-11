package com.easymeeting.entity.po;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户设置实体类
 */
public class UserSettings implements Serializable {
    
    private String userId;
    
    // 会议设置
    private Boolean defaultVideoOn;
    private Boolean defaultAudioOn;
    private Integer reminderTime;
    
    // 通知设置
    private Boolean desktopNotification;
    private Boolean soundNotification;
    private Boolean meetingInviteNotification;
    private Boolean friendRequestNotification;
    
    // 隐私设置
    private Boolean showOnlineStatus;
    private Boolean allowStrangerAdd;
    
    // 外观设置
    private Boolean darkMode;
    private String language;
    
    // 视频设置
    private String videoQuality;
    private Boolean mirrorVideo;
    private Boolean virtualBackground;
    
    // 音频设置
    private Boolean echoCancellation;
    private Boolean noiseSuppression;
    private Boolean autoGainControl;
    
    // 屏幕共享设置
    private Boolean shareSystemAudio;
    private Boolean optimizeVideoSharing;
    
    // 网络设置
    private Boolean autoReconnect;
    private Boolean showNetworkStatus;
    
    private Date createTime;
    private Date updateTime;
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Boolean getDefaultVideoOn() {
        return defaultVideoOn;
    }
    
    public void setDefaultVideoOn(Boolean defaultVideoOn) {
        this.defaultVideoOn = defaultVideoOn;
    }
    
    public Boolean getDefaultAudioOn() {
        return defaultAudioOn;
    }
    
    public void setDefaultAudioOn(Boolean defaultAudioOn) {
        this.defaultAudioOn = defaultAudioOn;
    }
    
    public Integer getReminderTime() {
        return reminderTime;
    }
    
    public void setReminderTime(Integer reminderTime) {
        this.reminderTime = reminderTime;
    }
    
    public Boolean getDesktopNotification() {
        return desktopNotification;
    }
    
    public void setDesktopNotification(Boolean desktopNotification) {
        this.desktopNotification = desktopNotification;
    }
    
    public Boolean getSoundNotification() {
        return soundNotification;
    }
    
    public void setSoundNotification(Boolean soundNotification) {
        this.soundNotification = soundNotification;
    }
    
    public Boolean getMeetingInviteNotification() {
        return meetingInviteNotification;
    }
    
    public void setMeetingInviteNotification(Boolean meetingInviteNotification) {
        this.meetingInviteNotification = meetingInviteNotification;
    }
    
    public Boolean getFriendRequestNotification() {
        return friendRequestNotification;
    }
    
    public void setFriendRequestNotification(Boolean friendRequestNotification) {
        this.friendRequestNotification = friendRequestNotification;
    }
    
    public Boolean getShowOnlineStatus() {
        return showOnlineStatus;
    }
    
    public void setShowOnlineStatus(Boolean showOnlineStatus) {
        this.showOnlineStatus = showOnlineStatus;
    }
    
    public Boolean getAllowStrangerAdd() {
        return allowStrangerAdd;
    }
    
    public void setAllowStrangerAdd(Boolean allowStrangerAdd) {
        this.allowStrangerAdd = allowStrangerAdd;
    }
    
    public Boolean getDarkMode() {
        return darkMode;
    }
    
    public void setDarkMode(Boolean darkMode) {
        this.darkMode = darkMode;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getVideoQuality() {
        return videoQuality;
    }
    
    public void setVideoQuality(String videoQuality) {
        this.videoQuality = videoQuality;
    }
    
    public Boolean getMirrorVideo() {
        return mirrorVideo;
    }
    
    public void setMirrorVideo(Boolean mirrorVideo) {
        this.mirrorVideo = mirrorVideo;
    }
    
    public Boolean getVirtualBackground() {
        return virtualBackground;
    }
    
    public void setVirtualBackground(Boolean virtualBackground) {
        this.virtualBackground = virtualBackground;
    }
    
    public Boolean getEchoCancellation() {
        return echoCancellation;
    }
    
    public void setEchoCancellation(Boolean echoCancellation) {
        this.echoCancellation = echoCancellation;
    }
    
    public Boolean getNoiseSuppression() {
        return noiseSuppression;
    }
    
    public void setNoiseSuppression(Boolean noiseSuppression) {
        this.noiseSuppression = noiseSuppression;
    }
    
    public Boolean getAutoGainControl() {
        return autoGainControl;
    }
    
    public void setAutoGainControl(Boolean autoGainControl) {
        this.autoGainControl = autoGainControl;
    }
    
    public Boolean getShareSystemAudio() {
        return shareSystemAudio;
    }
    
    public void setShareSystemAudio(Boolean shareSystemAudio) {
        this.shareSystemAudio = shareSystemAudio;
    }
    
    public Boolean getOptimizeVideoSharing() {
        return optimizeVideoSharing;
    }
    
    public void setOptimizeVideoSharing(Boolean optimizeVideoSharing) {
        this.optimizeVideoSharing = optimizeVideoSharing;
    }
    
    public Boolean getAutoReconnect() {
        return autoReconnect;
    }
    
    public void setAutoReconnect(Boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }
    
    public Boolean getShowNetworkStatus() {
        return showNetworkStatus;
    }
    
    public void setShowNetworkStatus(Boolean showNetworkStatus) {
        this.showNetworkStatus = showNetworkStatus;
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
}
