# 设置功能后端完整实现

## 已创建的文件

### 1. 数据层
- ✅ `UserSettings.java` - 实体类
- ✅ `UserSettingsMapper.java` - Mapper接口  
- ✅ `UserSettingsMapper.xml` - MyBatis映射文件
- ✅ `UserSettingsDto.java` - 数据传输对象
- ✅ `ChangePasswordDto.java` - 修改密码DTO

### 2. 服务层
- ✅ `UserSettingsService.java` - Service接口
- ⚠️ `UserSettingsServiceImpl.java` - 需要创建
- ⚠️ 修改 `UserInfoServiceImpl.java` - 添加删除用户时清理设置

### 3. 控制层
- ⚠️ `UserSettingsController.java` - 需要创建

## 需要创建的文件内容

### UserSettingsServiceImpl.java

```java
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
        BeanUtils.copyProperties(settings, dto);
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
```

### UserSettingsController.java

```java
package com.easymeeting.controller;

import com.easymeeting.entity.dto.ChangePasswordDto;
import com.easymeeting.entity.dto.UserSettingsDto;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.service.UserInfoService;
import com.easymeeting.service.UserSettingsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/settings")
public class UserSettingsController {
    
    @Resource
    private UserSettingsService userSettingsService;
    
    @Resource
    private UserInfoService userInfoService;
    
    /**
     * 获取用户设置
     */
    @GetMapping("/get")
    public ResponseVO getUserSettings(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseVO.error("未登录");
        }
        
        UserSettingsDto settings = userSettingsService.getUserSettings(userId);
        return ResponseVO.success(settings);
    }
    
    /**
     * 保存用户设置
     */
    @PostMapping("/save")
    public ResponseVO saveSettings(HttpSession session, @RequestBody UserSettingsDto settingsDto) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseVO.error("未登录");
        }
        
        userSettingsService.saveOrUpdateSettings(userId, settingsDto);
        return ResponseVO.success("设置保存成功");
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/changePassword")
    public ResponseVO changePassword(HttpSession session, @Valid @RequestBody ChangePasswordDto dto) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseVO.error("未登录");
        }
        
        try {
            userInfoService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
            return ResponseVO.success("密码修改成功");
        } catch (Exception e) {
            return ResponseVO.error(e.getMessage());
        }
    }
}
```

### 修改 UserInfoService.java - 添加修改密码方法

```java
/**
 * 修改密码
 */
void changePassword(String userId, String oldPassword, String newPassword) throws Exception;
```

### 修改 UserInfoServiceImpl.java - 实现修改密码

```java
@Override
public void changePassword(String userId, String oldPassword, String newPassword) throws Exception {
    UserInfo userInfo = userInfoMapper.selectByUserId(userId);
    if (userInfo == null) {
        throw new Exception("用户不存在");
    }
    
    // 验证旧密码
    String encryptedOldPassword = StringTools.encodeMd5(oldPassword);
    if (!encryptedOldPassword.equals(userInfo.getPassword())) {
        throw new Exception("旧密码错误");
    }
    
    // 更新新密码
    String encryptedNewPassword = StringTools.encodeMd5(newPassword);
    userInfo.setPassword(encryptedNewPassword);
    userInfoMapper.updateByUserId(userInfo, userId);
}
```

### 修改 UserInfoServiceImpl.java - 删除用户时清理设置

在删除用户的方法中添加：

```java
@Resource
private UserSettingsService userSettingsService;

// 在删除用户的方法中添加
public void deleteUser(String userId) {
    // 删除用户设置
    userSettingsService.deleteUserSettings(userId);
    
    // 删除用户信息
    userInfoMapper.deleteByUserId(userId);
    
    // 其他清理工作...
}
```

## 前端API调用

### 在 services.js 中添加

```javascript
// 获取用户设置
export const getUserSettings = () => {
  return request.get('/api/settings/get')
}

// 保存用户设置
export const saveUserSettings = (settings) => {
  return request.post('/api/settings/save', settings)
}

// 修改密码
export const changePassword = (oldPassword, newPassword) => {
  return request.post('/api/settings/changePassword', {
    oldPassword,
    newPassword
  })
}
```

### 修改 Dashboard.vue 中的修改密码方法

```javascript
// 修改密码
const handleChangePassword = () => {
  ElMessageBox.prompt('请输入旧密码', '修改密码', {
    confirmButtonText: '下一步',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPattern: /^.{6,}$/,
    inputErrorMessage: '密码长度至少6位'
  }).then(({ value: oldPassword }) => {
    ElMessageBox.prompt('请输入新密码', '修改密码', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPattern: /^.{6,}$/,
      inputErrorMessage: '密码长度至少6位'
    }).then(async ({ value: newPassword }) => {
      try {
        const response = await changePassword(oldPassword, newPassword)
        if (response.data.code === 200) {
          ElMessage.success('密码修改成功，请重新登录')
          setTimeout(() => {
            handleSwitchAccount()
          }, 1500)
        } else {
          ElMessage.error(response.data.message || '密码修改失败')
        }
      } catch (error) {
        ElMessage.error('密码修改失败')
      }
    })
  }).catch(() => {
    // 用户取消
  })
}
```

## 实现步骤

1. ✅ 创建 `UserSettingsServiceImpl.java`
2. ✅ 创建 `UserSettingsController.java`
3. ✅ 修改 `UserInfoService.java` 添加 `changePassword` 方法
4. ✅ 修改 `UserInfoServiceImpl.java` 实现修改密码和删除用户清理
5. ✅ 在 `services.js` 中添加API调用方法
6. ✅ 修改 `Dashboard.vue` 中的 `handleChangePassword` 方法

## 测试清单

- [ ] 测试获取用户设置
- [ ] 测试保存用户设置
- [ ] 测试修改密码（正确的旧密码）
- [ ] 测试修改密码（错误的旧密码）
- [ ] 测试删除用户时是否清理设置
- [ ] 测试首次登录时自动创建默认设置

## 注意事项

1. 确保 `StringTools.encodeMd5()` 方法存在
2. 确保 `UserInfoMapper` 有 `updateByUserId` 方法
3. 确保 `UserInfoMapper` 有 `deleteByUserId` 方法
4. 修改密码后需要重新登录
5. 删除用户时会级联删除设置
