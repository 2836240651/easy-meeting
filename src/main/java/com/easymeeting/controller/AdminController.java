package com.easymeeting.controller;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.entity.query.UserInfoQuery;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.entity.vo.UserInfoVo;
import com.easymeeting.mappers.UserInfoMapper;
import com.easymeeting.service.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

@RequestMapping("/admin")
@RestController
@Slf4j
@Validated
public class AdminController extends ABaseController{
    @Resource
    private UserInfoService userInfoService;
    @RequestMapping("/loadUserList")
    @globalInterceptor(checkAdmin = true)
    public ResponseVO loadUserList(String nickNameFuzzy, @NotNull Integer pageNo, @NotNull Integer pageSize) {
        UserInfoQuery userInfoQuery = new UserInfoQuery();
        userInfoQuery.setNickNameFuzzy(nickNameFuzzy);
        userInfoQuery.setPageNo(pageNo);
        userInfoQuery.setPageSize(pageSize);
        PaginationResultVO<UserInfo> listByPage = userInfoService.findListByPage(userInfoQuery);
        return getSuccessResponseVO(listByPage  );
    }
    @RequestMapping("/updateUserStatus")
    @globalInterceptor(checkAdmin = true)
    public ResponseVO updateUserStatus(@NotEmpty String userId, @NotNull Integer status) {
        UserInfo userInfo = new UserInfo();
        userInfo.setStatus(status);
        userInfo.setUserId(userId);
        this.userInfoService.updateUserStatus(userInfo);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/forceOffLine")
    @globalInterceptor(checkAdmin = true)
    public ResponseVO forceOffLine(@NotEmpty String userId) {

        this.userInfoService.forceOffLine(userId);
        return getSuccessResponseVO(null);
    }

}


