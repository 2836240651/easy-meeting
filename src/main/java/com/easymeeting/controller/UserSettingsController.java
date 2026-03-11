package com.easymeeting.controller;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.dto.ChangePasswordDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.dto.UserSettingsDto;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.service.UserInfoService;
import com.easymeeting.service.UserSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/settings")
public class UserSettingsController extends ABaseController {

    @Resource
    private UserSettingsService userSettingsService;

    @Resource
    private UserInfoService userInfoService;

    @GetMapping("/get")
    @globalInterceptor(checkLogin = true)
    public ResponseVO getUserSettings() {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
        try {
            UserSettingsDto settings = userSettingsService.getUserSettings(tokenUserInfo.getUserId());
            return getSuccessResponseVO(settings);
        } catch (Exception e) {
            return getFailResponseVO("获取设置失败: " + e.getMessage());
        }
    }

    @PostMapping("/save")
    @globalInterceptor(checkLogin = true)
    public ResponseVO saveSettings(@RequestBody UserSettingsDto settingsDto) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
        try {
            userSettingsService.saveOrUpdateSettings(tokenUserInfo.getUserId(), settingsDto);
            return getSuccessResponseVO("设置保存成功");
        } catch (Exception e) {
            return getFailResponseVO("保存设置失败: " + e.getMessage());
        }
    }

    @PostMapping("/changePassword")
    @globalInterceptor(checkLogin = true)
    public ResponseVO changePassword(@Valid @RequestBody ChangePasswordDto dto) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
        try {
            userInfoService.changePassword(tokenUserInfo.getUserId(), dto.getOldPassword(), dto.getNewPassword());
            return getSuccessResponseVO("密码修改成功");
        } catch (Exception e) {
            return getFailResponseVO(e.getMessage());
        }
    }
}
