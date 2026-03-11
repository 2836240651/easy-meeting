package com.easymeeting.controller;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户信息控制器
 */
@RestController("userProfileController")
@RequestMapping("/userInfo")
public class UserInfoController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoController.class);

    @Resource
    private UserInfoService userInfoService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/getUserInfo")
    @globalInterceptor
    public ResponseVO getUserInfo(HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
        UserInfo userInfo = userInfoService.getUserInfoByUserId(tokenUserInfoDto.getUserId());
        if (userInfo != null) {
            // 不返回密码信息
            userInfo.setPassword(null);
        }
        return getSuccessResponseVO(userInfo);
    }

    /**
     * 更新用户信息（只能修改性别、昵称、头像）
     */
    @PostMapping("/updateUserInfo")
    @globalInterceptor
    public ResponseVO updateUserInfo(HttpServletRequest request,
                                   @RequestParam(value = "sex", required = false) Integer sex,
                                   @RequestParam(value = "nickName", required = false) String nickName,
                                   @RequestParam(value = "avatar", required = false) String avatar) {
        try {
            TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
            String userId = tokenUserInfoDto.getUserId();

            // 创建更新对象
            UserInfo updateUserInfo = new UserInfo();
            updateUserInfo.setSex(sex);
            updateUserInfo.setNickName(nickName);
            updateUserInfo.setAvatar(avatar);

            // 执行更新
            userInfoService.updateUserInfoByUserId(updateUserInfo, userId);

            // 返回更新后的用户信息
            UserInfo updatedUserInfo = userInfoService.getUserInfoByUserId(userId);
            if (updatedUserInfo != null) {
                // 不返回密码信息
                updatedUserInfo.setPassword(null);
            }

            return getSuccessResponseVO(updatedUserInfo);
        } catch (Exception e) {
            logger.error("更新用户信息失败", e);
            return getFailResponseVO("更新用户信息失败");
        }
    }
}