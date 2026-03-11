package com.easymeeting.mappers;

import com.easymeeting.entity.po.UserSettings;
import org.apache.ibatis.annotations.Param;

/**
 * 用户设置Mapper
 */
public interface UserSettingsMapper {
    
    /**
     * 根据用户ID查询设置
     */
    UserSettings selectByUserId(@Param("userId") String userId);
    
    /**
     * 插入设置
     */
    Integer insert(UserSettings userSettings);
    
    /**
     * 更新设置
     */
    Integer update(UserSettings userSettings);
    
    /**
     * 根据用户ID删除设置
     */
    Integer deleteByUserId(@Param("userId") String userId);
}
