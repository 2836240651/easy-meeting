package com.easymeeting.controller;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.dto.SystemSettingDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.po.MeetingInfo;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.entity.query.MeetingInfoQuery;
import com.easymeeting.entity.query.UserInfoQuery;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.redis.RedisComponent;
import com.easymeeting.service.MeetingInfoService;
import com.easymeeting.service.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RequestMapping("/admin")
@RestController
@Slf4j
@Validated
public class AdminSettingController extends ABaseController{
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private MeetingInfoService meetingInfoService;
    @RequestMapping("/saveSysSetting")
    @globalInterceptor(checkAdmin = true)
    public ResponseVO saveSysSetting(SystemSettingDto systemSettingDto){
        redisComponent.saveSystemSetting(systemSettingDto);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/getSysSetting")
    @globalInterceptor
    public ResponseVO getSysSetting(){
        SystemSettingDto settingDto = redisComponent.getSystemSetting();
        return getSuccessResponseVO(settingDto);
    }
    @RequestMapping("/loadMeeting")
    @globalInterceptor(checkAdmin = true)
    public ResponseVO loadMeeting(String meetingIdFuzzy,@NotNull Integer pageNo,@NotNull Integer pageSize){
        MeetingInfoQuery meetingInfoQuery = new MeetingInfoQuery();
        meetingInfoQuery.setMeetingId(meetingIdFuzzy);
        meetingInfoQuery.setPageNo(pageNo);
        meetingInfoQuery.setPageSize(pageSize);
        meetingInfoQuery.setOrderBy("m.create_time desc");
        meetingInfoQuery.setQueryMemberCount(true);
        PaginationResultVO<MeetingInfo> listByPage = meetingInfoService.findListByPage(meetingInfoQuery);
        return getSuccessResponseVO(listByPage);
    }

    @RequestMapping("/updateMeetingStatus")
    @globalInterceptor(checkAdmin = true)
    public ResponseVO updateMeetingStatus(@NotEmpty String meetingId,@NotNull Integer status){
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
        this.meetingInfoService.updateMeetingStatus(meetingId,status,tokenUserInfo.getUserId());
        return getSuccessResponseVO(null);
    }


}
