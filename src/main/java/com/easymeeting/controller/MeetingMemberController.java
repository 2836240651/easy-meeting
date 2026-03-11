package com.easymeeting.controller;

import java.util.List;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.query.MeetingMemberQuery;
import com.easymeeting.entity.po.MeetingMember;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.service.MeetingMemberService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 *  Controller
 */
@RestController("meetingMemberController")
@RequestMapping("/meetingMember")
public class MeetingMemberController extends ABaseController{

	@Resource
	private MeetingMemberService meetingMemberService;
	/**
	 * 鏍规嵁鏉′欢鍒嗛〉鏌ヨ
	 */
	@RequestMapping("/loadDataList")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO loadDataList(MeetingMemberQuery query){
		return getSuccessResponseVO(meetingMemberService.findListByPage(query));
	}

	/**
	 * 鏂板
	 */
	@RequestMapping("/add")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO add(MeetingMember bean) {
		meetingMemberService.add(bean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鎵归噺鏂板
	 */
	@RequestMapping("/addBatch")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO addBatch(@RequestBody List<MeetingMember> listBean) {
		meetingMemberService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鎵归噺鏂板/淇敼
	 */
	@RequestMapping("/addOrUpdateBatch")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO addOrUpdateBatch(@RequestBody List<MeetingMember> listBean) {
		meetingMemberService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鏍规嵁MeetingIdAndUserId鏌ヨ瀵硅薄
	 */
	@RequestMapping("/getMeetingMemberByMeetingIdAndUserId")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO getMeetingMemberByMeetingIdAndUserId(String meetingId,String userId) {
		return getSuccessResponseVO(meetingMemberService.getMeetingMemberByMeetingIdAndUserId(meetingId,userId));
	}

	/**
	 * 鏍规嵁MeetingIdAndUserId淇敼瀵硅薄
	 */
	@RequestMapping("/updateMeetingMemberByMeetingIdAndUserId")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO updateMeetingMemberByMeetingIdAndUserId(MeetingMember bean,String meetingId,String userId) {
		meetingMemberService.updateMeetingMemberByMeetingIdAndUserId(bean,meetingId,userId);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鏍规嵁MeetingIdAndUserId鍒犻櫎
	 */
	@RequestMapping("/deleteMeetingMemberByMeetingIdAndUserId")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO deleteMeetingMemberByMeetingIdAndUserId(String meetingId,String userId) {
		meetingMemberService.deleteMeetingMemberByMeetingIdAndUserId(meetingId,userId);
		return getSuccessResponseVO(null);
	}
}

