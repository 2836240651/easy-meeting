package com.easymeeting.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.MeetingMemberStatusEnum;
import com.easymeeting.entity.enums.MeetingStatusEnum;
import com.easymeeting.entity.enums.ResponseCodeEnum;
import com.easymeeting.entity.po.MeetingMember;
import com.easymeeting.entity.query.MeetingInfoQuery;
import com.easymeeting.entity.po.MeetingInfo;
import com.easymeeting.entity.query.MeetingMemberQuery;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.mappers.MeetingMemberMapper;
import com.easymeeting.service.MeetingInfoService;
import com.easymeeting.service.impl.MeetingInfoServiceImpl;
import com.easymeeting.service.impl.MeetingMemberServiceImpl;
import com.easymeeting.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForReadableInstant;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 *  Controller
 */
@RestController("meetingInfoController")
@RequestMapping("/meetingInfo")
@Slf4j
@Validated
public class MeetingInfoController extends ABaseController{

	@Resource
	private MeetingInfoService meetingInfoService;
	@Resource
	private MeetingInfoServiceImpl meetingInfoServiceimpl;
	private FutureOrPresentValidatorForReadableInstant futureOrPresentValidatorForReadableInstant;
    @Autowired
    private MeetingMemberMapper meetingMemberMapper;
    @Autowired
    private MeetingMemberServiceImpl meetingMemberService;

	/*
	* 鍘嗗彶浼氳
	/**
	 * 鍔犺浇浼氳鍒楄〃
	 * @param pageNo 椤电爜
	 * @param status 浼氳鐘舵€侊細0-杩涜涓紝1-宸茬粨鏉燂紝null-鍏ㄩ儴
	 * @return 浼氳鍒楄〃
	 */
	@globalInterceptor(checkLogin = true)
	@RequestMapping("/loadMeeting")
	public ResponseVO loadMeeting(Integer pageNo, Integer status) {
		TokenUserInfoDto tokenUserInfo = this.getTokenUserInfo();
		MeetingInfoQuery meetingInfoQuery = new MeetingInfoQuery();
		meetingInfoQuery.setUserId(tokenUserInfo.getUserId());
		meetingInfoQuery.setPageNo(pageNo);
		
		// 濡傛灉浼犲叆浜唖tatus鍙傛暟锛屽垯鎸夌姸鎬佽繃婊?
		if (status != null) {
			meetingInfoQuery.setStatus(status);
		}
		
		meetingInfoQuery.setOrderBy("m.create_time desc");
		meetingInfoQuery.setQueryMemberCount(true);
		PaginationResultVO resultVO = meetingInfoServiceimpl.findListByPage(meetingInfoQuery);
		return getSuccessResponseVO(resultVO);
	}


	@globalInterceptor(checkLogin = true)
	@RequestMapping("/quickMeeting")
	public ResponseVO quickMeeting(@RequestBody Map<String, Object> params) {
		Integer meetingNoType = (Integer) params.get("meetingNoType");
		String MeetingName = (String) params.get("MeetingName");
		Integer joinType = (Integer) params.get("joinType");
		String joinPassword = (String) params.get("joinPassword");
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		log.info(tokenUserInfo.toString()+"--------------------------------------------------");
		if (tokenUserInfo.getCurrentMeetingId()!=null){
		throw new BusinessException("请先退出当前会议");
		}
		MeetingInfo meetingInfo = new MeetingInfo();
		meetingInfo.setMeetingName(MeetingName);
		meetingInfo.setJoinType(joinType);
		meetingInfo.setJoinPassword(joinPassword);
		meetingInfo.setCreateUserId(tokenUserInfo.getUserId());
		meetingInfo.setMeetingNo(meetingNoType==0? tokenUserInfo.getMyMeetingNo() : StringTools.getMeetingNoOrMettingId());
		meetingInfoServiceimpl.quickMeeting(meetingInfo,tokenUserInfo.getNickName());
		tokenUserInfo.setCurrentMeetingId(meetingInfo.getMeetingId());
		tokenUserInfo.setCurrentNickName(tokenUserInfo.getNickName());
		resetTokenUserInfo(tokenUserInfo);
		return getSuccessResponseVO(meetingInfo.getMeetingId());
	}



	@RequestMapping("/joinMeeting")
	@globalInterceptor
	public ResponseVO joinMeeting(@RequestBody Map<String, Object> params) {
		Boolean videoOpen = (Boolean) params.get("videoOpen");
		String meetingId = (String) params.get("meetingId"); // 娣诲姞meetingId鍙傛暟
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		
		String targetMeetingId = tokenUserInfo.getCurrentMeetingId();
		
		// 濡傛灉currentMeetingId涓簄ull锛屼絾鎻愪緵浜唌eetingId锛屽皾璇曟仮澶嶄細璁姸鎬?
		if (StringUtils.isEmpty(targetMeetingId) && !StringUtils.isEmpty(meetingId)) {
			// 楠岃瘉浼氳鏄惁瀛樺湪涓旀鍦ㄨ繘琛?
			MeetingInfo meetingInfo = this.meetingInfoService.getMeetingInfoByMeetingId(meetingId);
			if (meetingInfo == null) {
				throw new BusinessException("会议不存在");
			}
			if (meetingInfo.getStatus().equals(MeetingStatusEnum.FINISHED.getStatus())) {
				throw new BusinessException("会议已结束");
			}
			
			// 鎭㈠鐢ㄦ埛鐨勪細璁姸鎬?
			tokenUserInfo.setCurrentMeetingId(meetingId);
			resetTokenUserInfo(tokenUserInfo);
			targetMeetingId = meetingId;
		}
		
		if (StringUtils.isEmpty(targetMeetingId)) {
			throw new BusinessException("鏃犳硶纭畾瑕佸姞鍏ョ殑浼氳");
		}
		
		// 璋冪敤service鍔犲叆浼氳
		meetingInfoServiceimpl.joinMeeting(videoOpen, targetMeetingId,
				tokenUserInfo.getUserId(), tokenUserInfo.getNickName(), tokenUserInfo.getSex());
		
		return getSuccessResponseVO("鍔犲叆浼氳鎴愬姛");
	}

	@globalInterceptor(checkLogin = true)
	@RequestMapping("/preJoinMeeting")
	public ResponseVO preJoinMeeting( String meetingNo, String nickName,String password) {
		log.info("杩涘叆prejoinMeeting");
		System.out.println(password);
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		meetingNo = meetingNo.replace(" ", "");
		tokenUserInfo.setCurrentNickName(nickName);
		String meetingId=meetingInfoServiceimpl.preJoinMeeting(meetingNo,tokenUserInfo,password);
		log.info("楠岃瘉瀹屾瘯!!");
		return getSuccessResponseVO(meetingId);
	}
	@RequestMapping("/exitMeeting")
	@globalInterceptor
	public ResponseVO exitMeeting(){
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		this.meetingInfoService.exitMeetingRoom(tokenUserInfo, MeetingMemberStatusEnum.EXIT_MEETING);
		
		// 娉ㄦ剰锛氫笉娓呴櫎 currentMeetingId锛屽厑璁哥敤鎴烽噸鏂板姞鍏ヤ細璁?
		// tokenUserInfo.setCurrentMeetingId(null);
		// resetTokenUserInfo(tokenUserInfo);
		
		return getSuccessResponseVO(null);
	}

	@globalInterceptor
	@RequestMapping("/kickOutMeeting")
	public ResponseVO kickMeeting(@RequestBody Map<String, String> params){
		String userId = params.get("userId");
		if (StringTools.isEmpty(userId)) {
			throw new BusinessException("鐢ㄦ埛ID涓嶈兘涓虹┖");
		}
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		this.meetingInfoService.forceExitMeeting(tokenUserInfo, userId, MeetingMemberStatusEnum.EXIT_MEETING);
		return getSuccessResponseVO(null);
	}
	@globalInterceptor
	@RequestMapping("/blackMeeting")
	public ResponseVO blackMeeting(@RequestBody Map<String, String> params){
		String userId = params.get("userId");
		if (StringTools.isEmpty(userId)) {
			throw new BusinessException("鐢ㄦ埛ID涓嶈兘涓虹┖");
		}
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		this.meetingInfoService.forceExitMeeting(tokenUserInfo, userId, MeetingMemberStatusEnum.BLACKLIST);
		return getSuccessResponseVO(null);
	}
	@globalInterceptor
	@RequestMapping("/getCurrentMeeting")
	public ResponseVO getCurrentMeeting(){
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		log.info("鑾峰彇褰撳墠浼氳淇℃伅 - 鐢ㄦ埛ID: {}", tokenUserInfo.getUserId());
		
		// 涓嶅啀渚濊禆 currentMeetingId锛岃€屾槸鏌ヨ鐢ㄦ埛鍙備笌鐨勬墍鏈夎繘琛屼腑鐨勪細璁?
		// 鏌ヨ鏉′欢锛氫細璁姸鎬佷负杩涜涓?status=0)锛屼笖鐢ㄦ埛鍦?meeting_member 琛ㄤ腑涓?status in (1, 2)
		MeetingInfoQuery query = new MeetingInfoQuery();
		query.setStatus(MeetingStatusEnum.RUNING.getStatus());
		query.setUserId(tokenUserInfo.getUserId());
		query.setOrderBy("create_time desc");
		query.setPageNo(1);
		query.setPageSize(1); // 鍙彇鏈€鏂扮殑涓€涓?
		
		PaginationResultVO<MeetingInfo> result = this.meetingInfoService.findListByPage(query);
		
		if (result == null || result.getList() == null || result.getList().isEmpty()) {
			log.info("鐢ㄦ埛 {} 褰撳墠娌℃湁杩涜涓殑浼氳", tokenUserInfo.getUserId());
			return getSuccessResponseVO(null);
		}
		
		MeetingInfo meetingInfo = result.getList().get(0);
		log.info("杩斿洖浼氳淇℃伅 - 浼氳ID: {}, 浼氳鍙? {}, 浼氳鍚嶇О: {}, 鍒涘缓鑰? {}", 
			meetingInfo.getMeetingId(), 
			meetingInfo.getMeetingNo(), 
			meetingInfo.getMeetingName(),
			meetingInfo.getCreateUserNickName());
		
		return getSuccessResponseVO(meetingInfo);
	}

	@globalInterceptor(checkLogin = true)
	@RequestMapping("/getMeetingStatus")
	public ResponseVO getMeetingStatus(@NotEmpty String meetingId) {
		MeetingInfo meetingInfo = this.meetingInfoService.getMeetingInfoByMeetingId(meetingId);
		Map<String, Object> result = new HashMap<>();
		result.put("meetingId", meetingId);
		if (meetingInfo == null) {
			result.put("status", MeetingStatusEnum.FINISHED.getStatus());
			result.put("ended", true);
			return getSuccessResponseVO(result);
		}
		result.put("status", meetingInfo.getStatus());
		result.put("ended", MeetingStatusEnum.FINISHED.getStatus() == meetingInfo.getStatus());
		return getSuccessResponseVO(result);
	}


	@globalInterceptor
	@RequestMapping("/finishMeeting")
	public ResponseVO finishMeeting(String meetingId){
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		
		// 濡傛灉娌℃湁浼犲叆meetingId锛屼娇鐢ㄥ綋鍓嶄細璁甀D
		String targetMeetingId = meetingId;
		if (StringUtils.isEmpty(targetMeetingId)) {
			targetMeetingId = tokenUserInfo.getCurrentMeetingId();
		}
		
		if (StringUtils.isEmpty(targetMeetingId)) {
			return getFailResponseVO("褰撳墠娌℃湁杩涜涓殑浼氳");
		}
		
		// 妫€鏌ョ敤鎴锋槸鍚︽槸浼氳鍒涘缓鑰?
		MeetingInfo meetingInfo = this.meetingInfoService.getMeetingInfoByMeetingId(targetMeetingId);
		if (meetingInfo == null) {
			return getFailResponseVO("会议不存在");
		}
		
		if (!meetingInfo.getCreateUserId().equals(tokenUserInfo.getUserId())) {
			return getFailResponseVO("只有会议创建者可以结束会议");
		}
		
		// 妫€鏌ヤ細璁姸鎬?
		if (meetingInfo.getStatus().equals(MeetingStatusEnum.FINISHED.getStatus())) {
			return getFailResponseVO("浼氳宸茬粡缁撴潫");
		}
		
		this.meetingInfoService.finishMeeting(targetMeetingId, tokenUserInfo.getUserId());
		
		// 濡傛灉缁撴潫鐨勬槸褰撳墠浼氳锛屾竻闄oken涓殑浼氳ID
		if (targetMeetingId.equals(tokenUserInfo.getCurrentMeetingId())) {
			tokenUserInfo.setCurrentMeetingId(null);
			resetTokenUserInfo(tokenUserInfo);
		}
		
		return getSuccessResponseVO(null);
	}

	@globalInterceptor
	@RequestMapping("/delMeetingRecord")
	public ResponseVO delMeetingRecord(@NotEmpty String meetingId){
		MeetingMember meetingMember = new MeetingMember();
		meetingMember.setStatus(MeetingMemberStatusEnum.DEL_MEETING.getStatus());
		MeetingMemberQuery meetingMemberQuery = new MeetingMemberQuery();
		meetingMemberQuery.setMeetingId(meetingId);
		meetingMemberQuery.setUserId(getTokenUserInfo().getUserId());
		meetingMemberMapper.updateByParam(meetingMemberQuery,meetingMember);
		return	getSuccessResponseVO(null);
	}
	@globalInterceptor
	@RequestMapping("/loadMeetingMembers")
	public ResponseVO loadMeetingMembers(@NotEmpty String meetingId){
		// 浠?Redis 鑾峰彇姝ｅ父鐘舵€佺殑鎴愬憳鍒楄〃锛堣嚜鍔ㄨ繃婊ゆ帀閫€鍑恒€佽韪€佽鎷夐粦鐨勬垚鍛橈級
		List<com.easymeeting.entity.dto.MeetingMemberDto> meetingMemberDtos = meetingInfoService.getActiveMeetingMembers(meetingId);
		
		// 妫€鏌ュ綋鍓嶇敤鎴锋槸鍚﹀湪浼氳涓?
		String currentUserId = getTokenUserInfo().getUserId();
		boolean isUserInMeeting = meetingMemberDtos.stream()
			.anyMatch(member -> member.getUserId().equals(currentUserId));
		
		if (!isUserInMeeting){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		
		return getSuccessResponseVO(meetingMemberDtos);
	}

	@globalInterceptor
	@RequestMapping("/joinMeetingReserve")
	public ResponseVO joinMeetingReserve(@NotEmpty String meetingId,String nickName,String password){
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		tokenUserInfo.setCurrentNickName(nickName);
		this.meetingInfoService.joinMeetingReserve(meetingId,tokenUserInfo,password);
		return getSuccessResponseVO(null);
	}
	@globalInterceptor
	@RequestMapping("/inviteContact")
	public ResponseVO inviteContact(@NotEmpty String contactIds){

		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
		this.meetingInfoService.inviteContact(tokenUserInfoDto,contactIds);
		return getSuccessResponseVO(null);
	}
	@globalInterceptor
	@RequestMapping("/acceptInvite")
	public ResponseVO acceptInvite(@NotEmpty String meetingId){
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		this.meetingInfoService.acceptInvite(tokenUserInfo,meetingId);
		return getSuccessResponseVO(null);
	}
	@globalInterceptor
	@RequestMapping("/sendVideoChange")
	public ResponseVO acceptInvite(@NotNull Boolean videoOpen){
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		this.meetingInfoService.sendVideoChange(tokenUserInfo.getUserId(),tokenUserInfo.getCurrentMeetingId(),videoOpen);
		return getSuccessResponseVO(null);
	}
	/**
	 * 鏍规嵁鏉′欢鍒嗛〉鏌ヨ
	 *
	 *
	 */
	@RequestMapping("/loadDataList")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO loadDataList(MeetingInfoQuery query){
		return getSuccessResponseVO(meetingInfoService.findListByPage(query));
	}

	/**
	 * 鏂板
	 */
	@RequestMapping("/add")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO add(MeetingInfo bean) {
		meetingInfoService.add(bean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鎵归噺鏂板
	 */
	@RequestMapping("/addBatch")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO addBatch(@RequestBody List<MeetingInfo> listBean) {
		meetingInfoService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鎵归噺鏂板/淇敼
	 */
	@RequestMapping("/addOrUpdateBatch")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO addOrUpdateBatch(@RequestBody List<MeetingInfo> listBean) {
		meetingInfoService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鏍规嵁MeetingId鏌ヨ瀵硅薄
	 */
	@RequestMapping("/getMeetingInfoByMeetingId")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO getMeetingInfoByMeetingId(String meetingId) {
		return getSuccessResponseVO(meetingInfoService.getMeetingInfoByMeetingId(meetingId));
	}

	/**
	 * 鏍规嵁MeetingId淇敼瀵硅薄
	 */
	@RequestMapping("/updateMeetingInfoByMeetingId")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO updateMeetingInfoByMeetingId(MeetingInfo bean,String meetingId) {
		meetingInfoService.updateMeetingInfoByMeetingId(bean,meetingId);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鏍规嵁MeetingId鍒犻櫎
	 */
	@RequestMapping("/deleteMeetingInfoByMeetingId")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO deleteMeetingInfoByMeetingId(String meetingId) {
		meetingInfoService.deleteMeetingInfoByMeetingId(meetingId);
		return getSuccessResponseVO(null);
	}

	/**
	 * 閭€璇风敤鎴峰姞鍏ュ綋鍓嶄細璁?
	 * @param inviteUserId 琚個璇风敤鎴稩D
	 * @return 鍝嶅簲缁撴灉
	 */
	@globalInterceptor(checkLogin = true)
	@RequestMapping("/inviteUserToMeeting")
	public ResponseVO inviteUserToMeeting(@NotEmpty String inviteUserId) {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		
		// 妫€鏌ュ綋鍓嶇敤鎴锋槸鍚﹀湪浼氳涓?
		if (StringTools.isEmpty(tokenUserInfo.getCurrentMeetingId())) {
			throw new BusinessException("鎮ㄥ綋鍓嶄笉鍦ㄤ細璁腑");
		}
		
		// 妫€鏌ユ槸鍚﹂個璇疯嚜宸?
		if (inviteUserId.equals(tokenUserInfo.getUserId())) {
			throw new BusinessException("不能邀请自己");
		}
		
		// 璋冪敤 service 鍙戦€侀個璇?
		meetingInfoService.inviteUserToMeeting(tokenUserInfo, inviteUserId);
		
		return getSuccessResponseVO(null);
	}
}


