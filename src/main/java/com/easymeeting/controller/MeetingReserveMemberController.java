package com.easymeeting.controller;

import java.util.List;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.query.MeetingReserveMemberQuery;
import com.easymeeting.entity.po.MeetingReserveMember;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.service.MeetingReserveMemberService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 *  Controller
 */
@RestController("meetingReserveMemberController")
@RequestMapping("/meetingReserveMember")
public class MeetingReserveMemberController extends ABaseController{

	@Resource
	private MeetingReserveMemberService meetingReserveMemberService;
	/**
	 * 鏍规嵁鏉′欢鍒嗛〉鏌ヨ
	 */
	@RequestMapping("/loadDataList")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO loadDataList(MeetingReserveMemberQuery query){
		return getSuccessResponseVO(meetingReserveMemberService.findListByPage(query));
	}

	/**
	 * 鏂板
	 */
	@RequestMapping("/add")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO add(MeetingReserveMember bean) {
		meetingReserveMemberService.add(bean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鎵归噺鏂板
	 */
	@RequestMapping("/addBatch")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO addBatch(@RequestBody List<MeetingReserveMember> listBean) {
		meetingReserveMemberService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鎵归噺鏂板/淇敼
	 */
	@RequestMapping("/addOrUpdateBatch")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO addOrUpdateBatch(@RequestBody List<MeetingReserveMember> listBean) {
		meetingReserveMemberService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鏍规嵁MeetingIdAndInviteUserId鏌ヨ瀵硅薄
	 */
	@RequestMapping("/getMeetingReserveMemberByMeetingIdAndInviteUserId")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO getMeetingReserveMemberByMeetingIdAndInviteUserId(String meetingId,String inviteUserId) {
		return getSuccessResponseVO(meetingReserveMemberService.getMeetingReserveMemberByMeetingIdAndInviteUserId(meetingId,inviteUserId));
	}

	/**
	 * 鏍规嵁MeetingIdAndInviteUserId淇敼瀵硅薄
	 */
	@RequestMapping("/updateMeetingReserveMemberByMeetingIdAndInviteUserId")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO updateMeetingReserveMemberByMeetingIdAndInviteUserId(MeetingReserveMember bean,String meetingId,String inviteUserId) {
		meetingReserveMemberService.updateMeetingReserveMemberByMeetingIdAndInviteUserId(bean,meetingId,inviteUserId);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鏍规嵁MeetingIdAndInviteUserId鍒犻櫎
	 */
	@RequestMapping("/deleteMeetingReserveMemberByMeetingIdAndInviteUserId")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO deleteMeetingReserveMemberByMeetingIdAndInviteUserId(String meetingId,String inviteUserId) {
		meetingReserveMemberService.deleteMeetingReserveMemberByMeetingIdAndInviteUserId(meetingId,inviteUserId);
		return getSuccessResponseVO(null);
	}
}

