package com.easymeeting.service.impl;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;

import com.easymeeting.entity.dto.*;
import com.easymeeting.entity.enums.*;
import com.easymeeting.entity.po.*;
import com.easymeeting.entity.query.*;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.mappers.*;
import com.easymeeting.redis.RedisComponent;

import com.easymeeting.utils.JsonUtils;
import com.easymeeting.websocket.ChannelContextUtils;
import com.easymeeting.websocket.message.MessageHandler;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.bridge.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.service.MeetingInfoService;
import com.easymeeting.utils.StringTools;
import org.springframework.util.StringUtils;


/**
 *  业务接口实现
 */
@Service("meetingInfoService")
public class MeetingInfoServiceImpl implements MeetingInfoService {
	private static final Log log = LogFactory.getLog(MeetingInfoServiceImpl.class);
	@Resource
	private ChannelContextUtils channelContextUtils;
	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	@Resource
	private MeetingInfoMapper<MeetingInfo, MeetingInfoQuery> meetingInfoMapper;

	@Resource
	private MeetingMemberMapper<MeetingMember,MeetingMemberQuery> meetingMemberMapper;
	@Resource
	private MeetingReserveMapper<MeetingReserve,MeetingReserveQuery> meetingReserveMapper;
	@Resource
	private MeetingReserveMemberMapper<MeetingReserveMember,MeetingReserveMemberQuery> meetingReserveMemberMapper;
    @Resource
	private UserContactMapper<UserContact,UserContactQuery> userContactMapper;
	@Autowired
    private RedisComponent redisComponent;
	@Resource
	private MessageHandler messageHandler;
	@Resource
	private UserNotificationServiceImpl userNotificationService;


	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<MeetingInfo> findListByParam(MeetingInfoQuery param) {
		return this.meetingInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(MeetingInfoQuery param) {
		return this.meetingInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<MeetingInfo> findListByPage(MeetingInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<MeetingInfo> list = this.findListByParam(param);
		PaginationResultVO<MeetingInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(MeetingInfo bean) {
		return this.meetingInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<MeetingInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.meetingInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<MeetingInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.meetingInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(MeetingInfo bean, MeetingInfoQuery param) {
		StringTools.checkParam(param);
		return this.meetingInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(MeetingInfoQuery param) {
		StringTools.checkParam(param);
		return this.meetingInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据MeetingId获取对象
	 */
	@Override
	public MeetingInfo getMeetingInfoByMeetingId(String meetingId) {
		return this.meetingInfoMapper.selectByMeetingId(meetingId);
	}

	/**
	 * 根据MeetingId修改
	 */
	@Override
	public Integer updateMeetingInfoByMeetingId(MeetingInfo bean, String meetingId) {
		return this.meetingInfoMapper.updateByMeetingId(bean, meetingId);
	}

	/**
	 * 根据MeetingId删除
	 */
	@Override
	public Integer deleteMeetingInfoByMeetingId(String meetingId) {
		return this.meetingInfoMapper.deleteByMeetingId(meetingId);
	}

	@Override
	public void quickMeeting(MeetingInfo meetingInfo, String nickName) {

	java.util.Date curDate = new Date();
	meetingInfo.setCreateTime(curDate);
	meetingInfo.setMeetingId(StringTools.getMeetingNoOrMettingId());
	meetingInfo.setStartTime(curDate);
	meetingInfo.setStatus(MeetingStatusEnum.RUNING.getStatus());
	this.meetingInfoMapper.insert(meetingInfo);
	}



	private void addMeetingMember(String meetingId,String userId,String nickName,Integer memberType){
		// 先检查用户是否已存在
		MeetingMember existingMember = meetingMemberMapper.selectByMeetingIdAndUserId(meetingId, userId);
		
		if (existingMember != null) {
			// 用户已存在，检查状态
			if (MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(existingMember.getStatus())) {
				// 用户已被拉黑，不允许加入
				log.info("用户尝试加入会议但已被拉黑: meetingId=" + meetingId + ", userId=" + userId);
				throw new BusinessException("你已经被拉黑无法加入会议");
			}
			
			if (MeetingMemberStatusEnum.KICK_OUT.getStatus().equals(existingMember.getStatus())) {
				// 用户被踢出，允许重新加入，更新状态为NORMAL
				log.info("被踢出用户重新加入会议: meetingId=" + meetingId + ", userId=" + userId);
				MeetingMember updateMember = new MeetingMember();
				updateMember.setStatus(MeetingMemberStatusEnum.NORMAL.getStatus());
				updateMember.setLastJoinTime(new Date());
				updateMember.setNickName(nickName);
				updateMember.setMemberType(memberType);
				updateMember.setMeetingStatus(MeetingStatusEnum.RUNING.getStatus());
				this.meetingMemberMapper.updateByMeetingIdAndUserId(updateMember, meetingId, userId);
				return;
			}
			
			// 用户状态正常，只更新加入时间等信息，不更新status
			log.info("用户重新加入会议: meetingId=" + meetingId + ", userId=" + userId);
			MeetingMember updateMember = new MeetingMember();
			updateMember.setLastJoinTime(new Date());
			updateMember.setNickName(nickName);
			updateMember.setMemberType(memberType);
			updateMember.setMeetingStatus(MeetingStatusEnum.RUNING.getStatus());
			this.meetingMemberMapper.updateByMeetingIdAndUserId(updateMember, meetingId, userId);
		} else {
			// 用户不存在，新增记录
			log.info("新用户加入会议: meetingId=" + meetingId + ", userId=" + userId);
			MeetingMember meetingMember = new MeetingMember();
			meetingMember.setMeetingId(meetingId);
			meetingMember.setUserId(userId);
			meetingMember.setNickName(nickName);
			meetingMember.setMemberType(memberType);
			meetingMember.setLastJoinTime(new Date());
			meetingMember.setMeetingStatus(MeetingStatusEnum.RUNING.getStatus());
			meetingMember.setStatus(MeetingMemberStatusEnum.NORMAL.getStatus());
			this.meetingMemberMapper.insert(meetingMember);
		}
	}
	private void add2Meeting(String meetingId,String userId,Integer sex,String nickName,Integer memberType,Boolean videoOpen){
		MeetingMemberDto meetingMemberDto = new MeetingMemberDto();
		meetingMemberDto.setSex(sex);
		meetingMemberDto.setNickName(nickName);
		meetingMemberDto.setUserId(userId);
		meetingMemberDto.setMemberType(memberType);
		meetingMemberDto.setOpenVideo(videoOpen);
		meetingMemberDto.setStatus(MeetingMemberStatusEnum.NORMAL.getStatus());
		meetingMemberDto.setJoinTime(System.currentTimeMillis()); // 设置加入时间
		
		// 从数据库获取用户头像
		UserInfo userInfo = userInfoMapper.selectByUserId(userId);
		if (userInfo != null && userInfo.getAvatar() != null) {
			meetingMemberDto.setAvatar(userInfo.getAvatar());
		}
		
		redisComponent.add2meeting(meetingId,meetingMemberDto);
	}
	private void checkMeetingJoin(String meetingId,String userId){
		try {
			// 检查Redis中的状态
			MeetingMemberDto meetingMember = redisComponent.getMeetingMember(meetingId, userId);
			if (meetingMember!=null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(meetingMember.getStatus())){
				throw new BusinessException("你已经被拉黑无法加入会议");
			}
			
			// 检查数据库中的状态（防止Redis数据丢失）
			MeetingMember dbMeetingMember = meetingMemberMapper.selectByMeetingIdAndUserId(meetingId, userId);
			if (dbMeetingMember != null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(dbMeetingMember.getStatus())) {
				throw new BusinessException("你已经被拉黑无法加入会议");
			}
		} catch (BusinessException e) {
			// 重新抛出业务异常
			throw e;
		} catch (Exception e) {
			log.error("checkMeetingJoin error: meetingId=" + meetingId + ", userId=" + userId, e);
			// 如果Redis出错，检查数据库
			try {
				MeetingMember dbMeetingMember = meetingMemberMapper.selectByMeetingIdAndUserId(meetingId, userId);
				if (dbMeetingMember != null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(dbMeetingMember.getStatus())) {
					throw new BusinessException("你已经被拉黑无法加入会议");
				}
			} catch (BusinessException be) {
				throw be;
			} catch (Exception ex) {
				log.error("checkMeetingJoin database check error: meetingId=" + meetingId + ", userId=" + userId, ex);
			}
		}
	}
	@Override
	public void joinMeeting(Boolean videoOpen, String meetingId, String userId, String nickName, Integer sex) {
		if (StringUtils.isEmpty(meetingId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		MeetingInfo meetingInfo = this.meetingInfoMapper.selectByMeetingId(meetingId);
		if (meetingInfo==null|| meetingInfo.getStatus().equals(MeetingStatusEnum.FINISHED)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//校验用户
		this.checkMeetingJoin(meetingId,userId);
		//加入成员
		MemberTypeEnum memeberType =meetingInfo.getCreateUserId().equals(userId)?MemberTypeEnum.COMPERE:MemberTypeEnum.NORAML;
		this.addMeetingMember(meetingId,userId,nickName,memeberType.getType());
		//加入会议
		this.add2Meeting(meetingId,userId,sex,nickName,memeberType.getType(),videoOpen);

		// 注意：不在这里发送WebSocket消息，因为此时用户的WebSocket连接可能还没建立
		// WebSocket消息会在用户连接建立后，由ChannelContextUtils.addContext()自动发送
		log.info("用户加入会议API调用成功: userId=" + userId + ", meetingId=" + meetingId + ", 等待WebSocket连接建立后发送通知");
	}

	@Override
	public String preJoinMeeting(String meetingNo, TokenUserInfoDto tokenUserInfo, String password) {
		MeetingInfoQuery meetingInfoQuery = new MeetingInfoQuery();
		meetingInfoQuery.setMeetingNo(meetingNo);
		meetingInfoQuery.setStatus(MeetingStatusEnum.RUNING.getStatus());
		List<MeetingInfo> meetingInfoList = this.meetingInfoMapper.selectList(meetingInfoQuery);
		System.out.println(meetingInfoList.toString());
		if (meetingInfoList==null||meetingInfoList.size()==0){
			// 检查是否存在该会议号但状态不是运行中
			MeetingInfoQuery allStatusQuery = new MeetingInfoQuery();
			allStatusQuery.setMeetingNo(meetingNo);
			List<MeetingInfo> allMeetings = this.meetingInfoMapper.selectList(allStatusQuery);
			if (allMeetings != null && allMeetings.size() > 0) {
				MeetingInfo meeting = allMeetings.get(0);
				if (MeetingStatusEnum.FINISHED.getStatus() == meeting.getStatus()) {
					throw new BusinessException("会议已结束");
				}
			}
			throw new BusinessException("会议号不存在或会议未开始");
		}
		MeetingInfo meetingInfo = meetingInfoList.get(0);
		log.info(meetingInfo.getStatus()+"------------------会议状态");
		MeetingStatusEnum meetingStatusEnum = MeetingStatusEnum.getEnum(meetingInfo.getStatus());
		System.out.println(meetingStatusEnum.getDesc());
		if (meetingStatusEnum.equals(MeetingStatusEnum.FINISHED)) {
			System.out.println("code finished");
			throw new BusinessException(ResponseCodeEnum.CODE_701);
		}
		if (!StringUtils.isEmpty(tokenUserInfo.getCurrentMeetingId()) && !tokenUserInfo.getCurrentMeetingId().equals(meetingInfo.getMeetingId())){
			System.out.println("code702");
			throw new BusinessException(ResponseCodeEnum.CODE_702);

		}
		checkMeetingJoin(meetingInfo.getMeetingId(),tokenUserInfo.getUserId());
		if (MeetingJoinTypeEnum.PASSWORD.getStatus().equals(meetingInfo.getJoinType()) && 
			!StringUtils.isEmpty(password) && 
			!Objects.equals(password, meetingInfo.getJoinPassword())){
			System.out.println("code703");
			throw new BusinessException(ResponseCodeEnum.CODE_703);
		}
		tokenUserInfo.setCurrentMeetingId(meetingInfo.getMeetingId());
		redisComponent.saveTokenUserInfoDto(tokenUserInfo);
		return meetingInfo.getMeetingId();
	}

	@Override
	public void exitMeetingRoom(TokenUserInfoDto tokenUserInfoDto, MeetingMemberStatusEnum statusEnum) {
		String meetingId = tokenUserInfoDto.getCurrentMeetingId();
		if (StringUtils.isEmpty(meetingId)) {
			log.warn("用户 " + tokenUserInfoDto.getUserId() + " 没有当前会议ID，无法退出");
			return;
		}
		String userId = tokenUserInfoDto.getUserId();
		
		log.info("=== 开始退出会议流程 ===");
		log.info("用户ID: " + userId + ", 会议ID: " + meetingId + ", 退出状态: " + statusEnum.getDesc());
		
		// 更新数据库状态（所有状态变更都需要持久化到数据库）
		MeetingMember meetingMember = new MeetingMember();
		meetingMember.setStatus(statusEnum.getStatus());
		meetingMember.setUserId(userId);
		log.info("更新成员状态到数据库: meetingId=" + meetingId + ", userId=" + userId + ", status=" + statusEnum.getStatus() + ", statusDesc=" + statusEnum.getDesc());
		meetingMemberMapper.updateByMeetingIdAndUserId(meetingMember, meetingId, userId);
		log.info("数据库状态更新完成");
		
		// 更新Redis状态
		Boolean exit = redisComponent.exitMeeting(meetingId, userId, statusEnum);
		if (!exit){
			log.warn("Redis中未找到成员，但数据库已更新: meetingId=" + meetingId + ", userId=" + userId);
			tokenUserInfoDto.setCurrentMeetingId(null);
			redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);
			return;
		}
		log.info("Redis状态更新完成");
		
		// 获取更新后的成员列表
		List<MeetingMemberDto> meetingMemberList = redisComponent.getMeetingMemberList(meetingId);
		log.info("当前会议成员总数: " + meetingMemberList.size());
		
		// 构建退出消息
		MeetingExitDto exitDto = new MeetingExitDto();
		exitDto.setMeetingMemberDtoList(meetingMemberList);
		exitDto.setExitStatus(statusEnum.getStatus());
		exitDto.setExitUserId(userId);
		
		String exitDtoJson = JsonUtils.convertObj2Json(exitDto);
		log.info("退出消息内容: " + exitDtoJson);
		
		// 发送退出消息给其他成员
		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setMessageType(MessageTypeEnum.EXIT_MEETING_ROOM.getType());
		messageSendDto.setMessageContent(exitDtoJson);
		messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.GROUP.getType());
		messageSendDto.setMeetingId(meetingId);  // 设置会议ID
		messageSendDto.setSendUserId(userId);  // 设置发送者ID
		messageSendDto.setSendUserNickName(tokenUserInfoDto.getNickName());  // 设置发送者昵称
		
		log.info("准备发送退出消息到会议房间: meetingId=" + meetingId + ", messageType=" + MessageTypeEnum.EXIT_MEETING_ROOM.getType() + ", sendUserId=" + userId);
		messageHandler.sendMessage(messageSendDto);
		log.info("退出消息已发送");
		
		// 清除用户的当前会议ID
		tokenUserInfoDto.setCurrentMeetingId(null);
		redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);
		log.info("用户token中的会议ID已清除");

		// 检查是否还有在线成员
		List<MeetingMemberDto> onlineMemeberList = meetingMemberList.stream().filter(item -> MeetingMemberStatusEnum.NORMAL.getStatus().equals(item.getStatus())).collect(Collectors.toList());
		log.info("当前在线成员数: " + onlineMemeberList.size());
		
		if (onlineMemeberList.isEmpty()){
			log.info("没有在线成员，检查是否需要结束会议");
			MeetingReserve meetingReserve = this.meetingReserveMapper.selectByMeetingId(meetingId);
			if (meetingReserve==null){
				log.info("非预约会议，自动结束");
				finishMeeting(meetingId, null);
				return;
			}
			if (System.currentTimeMillis()>meetingReserve.getStartTime().getTime()+meetingReserve.getDuration()*60*1000){
				log.info("预约会议已超时，自动结束");
				finishMeeting(meetingId,null);
				return;
			}
			log.info("预约会议未超时，保持会议状态");
		}
		log.info("=== 退出会议流程完成 ===");
	}

	@Override
	public void forceExitMeeting(TokenUserInfoDto tokenUserInfo, String userId, MeetingMemberStatusEnum meetingMemberStatusEnum) {
		MeetingInfo meetingInfo = meetingInfoMapper.selectByMeetingId(tokenUserInfo.getCurrentMeetingId());
		
		// 修复：检查操作者是否是主持人，而不是检查被踢者
		if (!meetingInfo.getCreateUserId().equals(tokenUserInfo.getUserId())) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		
		// 获取被踢用户的token信息
		TokenUserInfoDto tokenByUserId = this.redisComponent.getTokenByUserId(userId);
		
		// 发送强制下线通知给被踢用户
		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType()); // 单人消息
		messageSendDto.setMessageType(MessageTypeEnum.FORCE_OFF_LINE.getType());   // 强制下线
		messageSendDto.setMeetingId(tokenUserInfo.getCurrentMeetingId());
		messageSendDto.setReceiveUserId(userId);  // 接收者是被踢的用户
		messageSendDto.setSendUserId(tokenUserInfo.getUserId()); // 发送者是主持人
		messageSendDto.setMessageContent("您已被主持人移出会议");
		
		log.info("发送强制下线消息: userId=" + userId + ", messageType=" + messageSendDto.getMessageType() + ", messageSend2Type=" + messageSendDto.getMessageSend2Type());
		messageHandler.sendMessage(messageSendDto);
		log.info("发送强制下线消息完毕-----------------------------------------");
		
		// 执行退出会议操作
		exitMeetingRoom(tokenByUserId, meetingMemberStatusEnum);
		
		// 清除被踢用户的 currentMeetingId（被踢出或拉黑的用户不应该能重新加入）
		if (tokenByUserId != null) {
			tokenByUserId.setCurrentMeetingId(null);
			redisComponent.saveTokenUserInfoDto(tokenByUserId);
			log.info("已清除被踢用户的 currentMeetingId: userId=" + userId);
		}
	}

	@Override
	public void finishMeeting(String currentMeetingId, String userId) {
		MeetingInfo meetingInfo = this.meetingInfoMapper.selectByMeetingId(currentMeetingId);
		if (meetingInfo == null) {
			throw new BusinessException("会议不存在");
		}
		
		// 如果提供了userId，检查是否是会议创建者
		if (userId != null && !userId.isEmpty() && !meetingInfo.getCreateUserId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		
		MeetingInfo updateInfo = new MeetingInfo();
		updateInfo.setStatus(MeetingStatusEnum.FINISHED.getStatus());
		updateInfo.setEndTime(new Date());
		this.meetingInfoMapper.updateByMeetingId(updateInfo,currentMeetingId);

		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.GROUP.getType());
		messageSendDto.setMessageType(MessageTypeEnum.FINIS_MEETING.getType());
		messageSendDto.setMeetingId(currentMeetingId);
		messageHandler.sendMessage(messageSendDto);

		MeetingMember meetingMember = new MeetingMember();
		meetingMember.setMeetingStatus(MeetingStatusEnum.FINISHED.getStatus());
		MeetingMemberQuery meetingMemberQuery = new MeetingMemberQuery();
		meetingMemberQuery.setMeetingId(currentMeetingId);
		meetingMemberMapper.updateByParam(meetingMember,meetingMemberQuery);
		//todo 更新预约状态
		MeetingReserve updateMeetingReserve = new MeetingReserve();
		updateMeetingReserve.setStatus(MeetingReserveStatusEnum.ENDED.getStatus());
		this.meetingReserveMapper.updateByMeetingId(updateMeetingReserve,currentMeetingId);
		
		// 清除所有会议成员的currentMeetingId（使用getAllMeetingMembers获取所有成员）
		List<MeetingMemberDto> meetingMemberList = redisComponent.getAllMeetingMembers(currentMeetingId);
		log.info("结束会议，清除成员的currentMeetingId: meetingId=" + currentMeetingId + ", 成员数=" + meetingMemberList.size());
		for (MeetingMemberDto meetingMemberDto : meetingMemberList) {
			TokenUserInfoDto tokenByUserId = redisComponent.getTokenByUserId(meetingMemberDto.getUserId());
			if (tokenByUserId != null) {
				tokenByUserId.setCurrentMeetingId(null);
				redisComponent.saveTokenUserInfoDto(tokenByUserId);
				log.info("清除成员的currentMeetingId: userId=" + meetingMemberDto.getUserId());
			}
		}
		
		// 额外确保主持人的currentMeetingId也被清除（双重保险）
		if (userId != null && !userId.isEmpty()) {
			TokenUserInfoDto hostToken = redisComponent.getTokenByUserId(userId);
			if (hostToken != null && currentMeetingId.equals(hostToken.getCurrentMeetingId())) {
				hostToken.setCurrentMeetingId(null);
				redisComponent.saveTokenUserInfoDto(hostToken);
				log.info("清除主持人的currentMeetingId: userId=" + userId);
			}
		}
		
		redisComponent.removeAllMeetingMember(currentMeetingId);
	}

	@Override
	public void joinMeetingReserve(String meetingId, TokenUserInfoDto tokenUserInfo, String password) {
		String userId = tokenUserInfo.getUserId();
		if (!StringUtils.isEmpty(tokenUserInfo.getCurrentMeetingId())&&!meetingId.equals(tokenUserInfo.getCurrentMeetingId())) {
			throw new BusinessException("你有未结束的会议");
		}
		checkMeetingJoin(meetingId,userId);
		MeetingReserve meetingReserve = this.meetingReserveMapper.selectByMeetingId(meetingId);
		if (meetingReserve==null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		MeetingReserveMember reserveMember = this.meetingReserveMemberMapper.selectByMeetingIdAndInviteUserId(meetingId, userId);
		if (reserveMember==null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if (MeetingJoinTypeEnum.PASSWORD.getStatus().equals(meetingReserve.getJoinType())&&!meetingReserve.getJoinPassword().equals(password)) {
			throw new BusinessException(ResponseCodeEnum.CODE_703);
		}
		MeetingInfo meetingInfo = this.meetingInfoMapper.selectByMeetingId(meetingId);
		if (meetingInfo==null) {
			meetingInfo=new MeetingInfo();
			meetingInfo.setMeetingName(meetingReserve.getMeetingName());
			meetingInfo.setMeetingNo(StringTools.getMeetingNoOrMettingId());
			meetingInfo.setMeetingId(meetingId);
			meetingInfo.setCreateUserId(meetingReserve.getCreateUserId());
			Date curDate=new Date();
			meetingInfo.setCreateTime(curDate);
			meetingInfo.setJoinPassword(password);
			meetingInfo.setStartTime(curDate);
			meetingInfo.setStatus(MeetingStatusEnum.RUNING.getStatus());
			this.meetingInfoMapper.insert(meetingInfo);
		}
		tokenUserInfo.setCurrentMeetingId(meetingId);
		redisComponent.saveTokenUserInfoDto(tokenUserInfo);
	}

	@Override
	public void inviteContact(TokenUserInfoDto tokenUserInfoDto, String contactIds) {
	String[] ids=contactIds.split(",");
		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setUserId(tokenUserInfoDto.getUserId());
		userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		List<UserContact> userContacts = this.userContactMapper.selectList(userContactQuery);
		List<String> contactList = userContacts.stream().map(item -> item.getContactId()).collect(Collectors.toList());
		if (!contactList.containsAll(Arrays.asList(ids))) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		MeetingInfo meetingInfo = this.meetingInfoMapper.selectByMeetingId(tokenUserInfoDto.getCurrentMeetingId());
		for (String contactId : ids) {
			MeetingMemberDto meetingMember = redisComponent.getMeetingMember(meetingInfo.getMeetingId(), contactId);
			if (meetingMember!=null&&meetingMember.getStatus().equals(MeetingMemberStatusEnum.NORMAL.getStatus() )){
				continue;
			}
			redisComponent.inviteInfo(tokenUserInfoDto.getCurrentMeetingId(),contactId);
			MessageSendDto messageSendDto = new MessageSendDto();
			messageSendDto.setSendUserNickName(tokenUserInfoDto.getNickName());
			messageSendDto.setReceiveUserId(contactId);
			messageSendDto.setMessageType(MessageTypeEnum.INVITE_MEMBER_MEETING.getType());
			messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
			Date curDate=new Date();
			messageSendDto.setSendTime(curDate.getTime());
			MeetingInviteDto meetingInviteDto = new MeetingInviteDto();
			meetingInviteDto.setMeetingName(meetingInfo.getMeetingName());
			meetingInviteDto.setMeetingId(tokenUserInfoDto.getCurrentMeetingId());
			meetingInviteDto.setInviteUserName(tokenUserInfoDto.getNickName());
			messageSendDto.setMessageContent(JsonUtils.convertObj2Json(meetingInviteDto));
			messageHandler.sendMessage(messageSendDto);
		}

	}

	@Override
	public void acceptInvite(TokenUserInfoDto tokenUserInfoDto, String meetingId) {
		String inviteMeetingId = redisComponent.getInviteInfo(tokenUserInfoDto.getUserId(), meetingId);
		if (inviteMeetingId==null){
			throw new BusinessException("邀请信息过期");
		}
		tokenUserInfoDto.setCurrentMeetingId(inviteMeetingId);
		tokenUserInfoDto.setCurrentNickName(tokenUserInfoDto.getNickName());
		redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);
	}

	@Override
	public void sendVideoChange(String userId, String currentMeetingId, Boolean videoOpen) {
		MeetingMemberDto meetingMember = redisComponent.getMeetingMember(currentMeetingId, userId);
		meetingMember.setOpenVideo(videoOpen);
		redisComponent.add2meeting(currentMeetingId,meetingMember);
		
		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.GROUP.getType());
		messageSendDto.setMessageType(MessageTypeEnum.MEETING_USER_VIDEO_CHANGE.getType());
		messageSendDto.setSendUserId(userId);
		messageSendDto.setMeetingId(currentMeetingId);
		
		// 创建消息内容，包含用户ID和视频状态
		java.util.HashMap<String, Object> content = new java.util.HashMap<>();
		content.put("userId", userId);
		content.put("videoOpen", videoOpen);
		messageSendDto.setMessageContent(content);
		
		messageHandler.sendMessage(messageSendDto);
	}

	@Override
	public void updateMeetingStatus(String meetingId, Integer status,String userId) {
		MeetingStatusEnum anEnum = MeetingStatusEnum.getEnum(status);
		if (anEnum==null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		MeetingInfo selectedByMeetingId = this.meetingInfoMapper.selectByMeetingId(meetingId);
		if (!StringTools.isEmpty(selectedByMeetingId.getMeetingId())&&!selectedByMeetingId.getCreateUserId().equals(userId)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if (MeetingStatusEnum.FINISHED.equals(anEnum)){
			MeetingInfo meetingInfo = new MeetingInfo();
			meetingInfo.setMeetingId(meetingId);
			meetingInfo.setStatus(anEnum.getStatus());
			meetingInfo.setEndTime(new Date());
			this.meetingInfoMapper.updateByMeetingId(meetingInfo,meetingId);

			redisComponent.removeAllMeetingMember(meetingId);
			MessageSendDto messageSendDto = new MessageSendDto();
			messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.GROUP.getType());
			messageSendDto.setMessageType(MessageTypeEnum.FINIS_MEETING.getType());
			messageSendDto.setSendTime(new Date().getTime());
			messageHandler.sendMessage(messageSendDto);

			MeetingMember meetingMember = new MeetingMember();
			meetingMember.setStatus(anEnum.getStatus());
			MeetingMemberQuery meetingMemberQuery = new MeetingMemberQuery();
			meetingMemberQuery.setMeetingId(meetingId);
			this.meetingMemberMapper.updateByParam(meetingMember,meetingMemberQuery);
			MeetingReserve meetingReserve = new MeetingReserve();
			meetingReserve.setStatus(MeetingReserveStatusEnum.ENDED.getStatus());
			this.meetingReserveMapper.updateByMeetingId(meetingReserve,meetingId);
			List<MeetingMemberDto> meetingMemberList = redisComponent.getMeetingMemberList(meetingId);
			if (!meetingMemberList.isEmpty()){
				for (MeetingMemberDto item : meetingMemberList) {
					String itemUserId = item.getUserId();
					TokenUserInfoDto tokenByUserId = redisComponent.getTokenByUserId(itemUserId);
					tokenByUserId.setCurrentMeetingId(null);
					redisComponent.saveTokenUserInfoDto(tokenByUserId);
				}
			}

		}

	}

	@Override
	public List<MeetingMemberDto> getActiveMeetingMembers(String meetingId) {
		return redisComponent.getMeetingMemberList(meetingId);
	}

	@Override
	public void inviteUserToMeeting(TokenUserInfoDto tokenUserInfo, String inviteUserId) {
		log.info("邀请用户加入会议: inviter=" + tokenUserInfo.getUserId() + ", invitee=" + inviteUserId);
		
		String meetingId = tokenUserInfo.getCurrentMeetingId();
		
		// 获取会议信息
		MeetingInfo meetingInfo = this.meetingInfoMapper.selectByMeetingId(meetingId);
		if (meetingInfo == null) {
			throw new BusinessException("会议不存在");
		}
		
		// 检查会议状态
		if (MeetingStatusEnum.RUNING.getStatus() != meetingInfo.getStatus()) {
			throw new BusinessException("会议已结束");
		}
		
		// 检查被邀请用户是否存在
		UserInfo inviteUser = this.userInfoMapper.selectByUserId(inviteUserId);
		if (inviteUser == null) {
			throw new BusinessException("被邀请用户不存在");
		}
		
		// 检查被邀请用户是否已在会议中
		List<MeetingMemberDto> memberList = redisComponent.getMeetingMemberList(meetingId);
		boolean alreadyInMeeting = memberList.stream()
				.anyMatch(member -> member.getUserId().equals(inviteUserId));
		if (alreadyInMeeting) {
			throw new BusinessException("该用户已在会议中");
		}
		
		// 创建即时会议邀请通知
		userNotificationService.createInstantMeetingInviteNotification(
			meetingId,
			meetingInfo.getMeetingNo(),
			meetingInfo.getMeetingName(),
			meetingInfo.getJoinPassword(),
			inviteUserId,
			tokenUserInfo.getUserId(),
			tokenUserInfo.getNickName()
		);
		
		log.info("已发送会议邀请通知: meetingId=" + meetingId + ", inviteUserId=" + inviteUserId);
	}

}
