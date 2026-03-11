package com.easymeeting.controller;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.dto.MeetingReserveDetailDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.po.MeetingReserve;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.service.MeetingReserveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 预约会议 Controller
 */
@RestController("meetingReserveController")
@RequestMapping("/meetingReserve")
@Slf4j
@Validated
public class MeetingReserveController extends ABaseController {

    @Resource
    private MeetingReserveService meetingReserveService;

    /**
     * 创建预约会议
     */
    @globalInterceptor(checkLogin = true)
    @RequestMapping("/createMeetingReserve")
    public ResponseVO createMeetingReserve(@RequestBody Map<String, Object> params) {
        String meetingName = (String) params.get("meetingName");
        Long startTimeMillis = params.get("startTime") != null ? 
            ((Number) params.get("startTime")).longValue() : null;
        Integer duration = params.get("duration") != null ? 
            ((Number) params.get("duration")).intValue() : null;
        Integer joinType = params.get("joinType") != null ? 
            ((Number) params.get("joinType")).intValue() : null;
        String joinPassword = (String) params.get("joinPassword");
        String inviteUserIds = (String) params.get("inviteUserIds");

        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();

        MeetingReserve meetingReserve = new MeetingReserve();
        meetingReserve.setMeetingName(meetingName);
        meetingReserve.setStartTime(startTimeMillis != null ? new Date(startTimeMillis) : null);
        meetingReserve.setDuration(duration);
        meetingReserve.setJoinType(joinType);
        meetingReserve.setJoinPassword(joinPassword);
        meetingReserve.setCreateUserId(tokenUserInfo.getUserId());
        meetingReserve.setInviteUserIds(inviteUserIds);

        meetingReserveService.createMeetingReserve(meetingReserve);

        return getSuccessResponseVO(meetingReserve.getMeetingId());
    }

    /**
     * 查询用户的预约会议列表
     */
    @globalInterceptor(checkLogin = true)
    @RequestMapping("/loadMeetingReserveList")
    public ResponseVO loadMeetingReserveList() {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
        List<MeetingReserve> list = meetingReserveService.loadMeetingReserveList(tokenUserInfo.getUserId());
        return getSuccessResponseVO(list);
    }

    /**
     * 取消预约会议（创建者）
     */
    @globalInterceptor(checkLogin = true)
    @RequestMapping("/cancelMeetingReserve")
    public ResponseVO cancelMeetingReserve(@NotEmpty String meetingId) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
        meetingReserveService.deleteMeetingReserveByMeetingId(meetingId, tokenUserInfo.getUserId());
        return getSuccessResponseVO(null);
    }

    /**
     * 退出预约会议（被邀请者）
     */
    @globalInterceptor(checkLogin = true)
    @RequestMapping("/leaveMeetingReserve")
    public ResponseVO leaveMeetingReserve(@NotEmpty String meetingId) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
        meetingReserveService.deleteMeetingReserveByUserId(meetingId, tokenUserInfo.getUserId());
        return getSuccessResponseVO(null);
    }

    /**
     * 修改预约会议（仅创建者）
     */
    @globalInterceptor(checkLogin = true)
    @RequestMapping("/updateMeetingReserve")
    public ResponseVO updateMeetingReserve(@RequestBody Map<String, Object> params) {
        String meetingId = (String) params.get("meetingId");
        String meetingName = (String) params.get("meetingName");
        Long startTimeMillis = params.get("startTime") != null ? 
            ((Number) params.get("startTime")).longValue() : null;
        Integer duration = params.get("duration") != null ? 
            ((Number) params.get("duration")).intValue() : null;
        Integer joinType = params.get("joinType") != null ? 
            ((Number) params.get("joinType")).intValue() : null;
        String joinPassword = (String) params.get("joinPassword");
        String inviteUserIds = (String) params.get("inviteUserIds");

        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();

        MeetingReserve meetingReserve = new MeetingReserve();
        meetingReserve.setMeetingId(meetingId);
        meetingReserve.setMeetingName(meetingName);
        meetingReserve.setStartTime(startTimeMillis != null ? new Date(startTimeMillis) : null);
        meetingReserve.setDuration(duration);
        meetingReserve.setJoinType(joinType);
        meetingReserve.setJoinPassword(joinPassword);
        meetingReserve.setInviteUserIds(inviteUserIds);

        meetingReserveService.updateMeetingReserve(meetingReserve, tokenUserInfo.getUserId());

        return getSuccessResponseVO(null);
    }

    /**
     * 查询预约会议详情
     */
    @globalInterceptor(checkLogin = true)
    @RequestMapping("/getMeetingReserveDetail")
    public ResponseVO getMeetingReserveDetail(@NotEmpty String meetingId) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
        MeetingReserveDetailDto detail = meetingReserveService.getMeetingReserveDetail(meetingId, tokenUserInfo.getUserId());
        return getSuccessResponseVO(detail);
    }

    /**
     * 获取即将开始的会议
     */
    @globalInterceptor(checkLogin = true)
    @RequestMapping("/getUpcomingMeetings")
    public ResponseVO getUpcomingMeetings() {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
        List<MeetingReserve> list = meetingReserveService.getUpcomingMeetings(tokenUserInfo.getUserId());
        return getSuccessResponseVO(list);
    }
}
