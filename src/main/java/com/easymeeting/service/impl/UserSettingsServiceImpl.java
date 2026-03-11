package com.easymeeting.service.impl;

import com.easymeeting.entity.dto.UserSettingsDto;
import com.easymeeting.entity.po.UserSettings;
import com.easymeeting.mappers.UserSettingsMapper;
import com.easymeeting.service.UserSettingsService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserSettingsServiceImpl implements UserSettingsService {
    
    @Resource
    private UserSettingsMapper userSettingsMapper;
    
    @Override
    public UserSettingsDto getUserSettings(String userId) {
        UserSettings settings = userSettingsMapper.selectByUserId(userId);
        
        // 如果用户没有设置，返回默认设置
        if (settings == null) {
            initDefaultSettings(userId);
            settings = userSettingsMapper.selectByUserId(userId);
        }
        
        UserSettingsDto dto = new UserSettingsDto();
        if (settings != null) {
            BeanUtils.copyProperties(settings, dto);
        }
        return dto;
    }
    
    @Override
    public void saveOrUpdateSettings(String userId, UserSettingsDto settingsDto) {
        UserSettings existing = userSettingsMapper.selectByUserId(userId);
        
        UserSettings settings = new UserSettings();
        BeanUtils.copyProperties(settingsDto, settings);
        settings.setUserId(userId);
        
        if (existing == null) {
            userSettingsMapper.insert(settings);
        } else {
            userSettingsMapper.update(settings);
        }
    }
    
    @Override
    public void deleteUserSettings(String userId) {
        userSettingsMapper.deleteByUserId(userId);
    }
    
    @Override
    public void initDefaultSettings(String userId) {
        UserSettings settings = new UserSettings();
        settings.setUserId(userId);
        
        // 会议设置默认值
        settings.setDefaultVideoOn(false);
        settings.setDefaultAudioOn(true);
        settings.setReminderTime(10);
        
        // 通知设置默认值
        settings.setDesktopNotification(true);
        settings.setSoundNotification(true);
        settings.setMeetingInviteNotification(true);
        settings.setFriendRequestNotification(true);
        
        // 隐私设置默认值
        settings.setShowOnlineStatus(true);
        settings.setAllowStrangerAdd(true);
        
        // 外观设置默认值
        settings.setDarkMode(false);
        settings.setLanguage("zh-CN");
        
        // 视频设置默认值
        settings.setVideoQuality("high");
        settings.setMirrorVideo(true);
        settings.setVirtualBackground(false);
        
        // 音频设置默认值
        settings.setEchoCancellation(true);
        settings.setNoiseSuppression(true);
        settings.setAutoGainControl(true);
        
        // 屏幕共享设置默认值
        settings.setShareSystemAudio(true);
        settings.setOptimizeVideoSharing(true);
        
        // 网络设置默认值
        settings.setAutoReconnect(true);
        settings.setShowNetworkStatus(true);
        
        userSettingsMapper.insert(settings);
    }
}
