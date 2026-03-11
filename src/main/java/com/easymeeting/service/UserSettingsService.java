package com.easymeeting.service;

import com.easymeeting.entity.dto.UserSettingsDto;

/**
 * 用户设置Service
 */
public interface UserSettingsService {
    
    /**
     * 获取用户设置
     */
    UserSettingsDto getUserSettings(String userId);
    
    /**
     * 保存或更新用户设置
     */
    void saveOrUpdateSettings(String userId, UserSettingsDto settingsDto);
    
    /**
     * 删除用户设置
     */
    void deleteUserSettings(String userId);
    
    /**
     * 初始化用户默认设置
     */
    void initDefaultSettings(String userId);
}
