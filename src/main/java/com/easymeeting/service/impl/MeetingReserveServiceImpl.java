package com.easymeeting.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.ws.Response;

import com.easymeeting.entity.enums.MeetingReserveStatusEnum;
import com.easymeeting.entity.enums.ResponseCodeEnum;
import com.easymeeting.entity.po.MeetingMember;
import com.easymeeting.entity.po.MeetingReserveMember;
import com.easymeeting.entity.query.MeetingMemberQuery;
import com.easymeeting.entity.query.MeetingReserveMemberQuery;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.mappers.MeetingReserveMemberMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import com.easymeeting.entity.enums.PageSize;
import com.easymeeting.entity.query.MeetingReserveQuery;
import com.easymeeting.entity.po.MeetingReserve;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.query.SimplePage;
import com.easymeeting.mappers.MeetingReserveMapper;
import com.easymeeting.service.MeetingReserveService;
import com.easymeeting.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


/**
 *  业务接口实现
 */
@Service("meetingReserveService")
public class MeetingReserveServiceImpl implements MeetingReserveService {
	private static final Log log = LogFactory.getLog(MeetingReserveServiceImpl.class);

	@Resource
	private MeetingReserveMapper<MeetingReserve, MeetingReserveQuery> meetingReserveMapper;
	@Resource
	private MeetingReserveMemberMapper<MeetingReserveMember, MeetingReserveMemberQuery> meetingReserveMemberMapper;
	@Resource
	private com.easymeeting.mappers.UserInfoMapper<com.easymeeting.entity.po.UserInfo, com.easymeeting.entity.query.UserInfoQuery> userInfoMapper;
	@Resource
	private com.easymeeting.service.UserNotificationService userNotificationService;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<MeetingReserve> findListByParam(MeetingReserveQuery param) {
		return this.meetingReserveMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(MeetingReserveQuery param) {
		return this.meetingReserveMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<MeetingReserve> findListByPage(MeetingReserveQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<MeetingReserve> list = this.findListByParam(param);
		PaginationResultVO<MeetingReserve> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(MeetingReserve bean) {
		return this.meetingReserveMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<MeetingReserve> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.meetingReserveMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<MeetingReserve> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.meetingReserveMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(MeetingReserve bean, MeetingReserveQuery param) {
		StringTools.checkParam(param);
		return this.meetingReserveMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(MeetingReserveQuery param) {
		StringTools.checkParam(param);
		return this.meetingReserveMapper.deleteByParam(param);
	}

	/**
	 * 根据MeetingId获取对象
	 */
	@Override
	public MeetingReserve getMeetingReserveByMeetingId(String meetingId) {
		return this.meetingReserveMapper.selectByMeetingId(meetingId);
	}

	/**
	 * 根据MeetingId修改
	 */
	@Override
	public Integer updateMeetingReserveByMeetingId(MeetingReserve bean, String meetingId) {
		return this.meetingReserveMapper.updateByMeetingId(bean, meetingId);
	}

	//会议预约邀请者
	@Override
	@Transactional(rollbackFor =Exception.class)
	public void deleteMeetingReserveByMeetingId(String meetingId, String userId) {
		// 逻辑删除：将状态改为已取消(3)，而不是物理删除
		MeetingReserve meetingReserve = this.meetingReserveMapper.selectByMeetingId(meetingId);
		if (meetingReserve == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		
		// 验证是否是创建者
		if (!meetingReserve.getCreateUserId().equals(userId)) {
			throw new BusinessException("只有创建者可以取消会议");
		}
		
		// 更新预约会议状态为已取消
		MeetingReserve updateBean = new MeetingReserve();
		updateBean.setStatus(MeetingReserveStatusEnum.CANCELLED.getStatus());
		this.meetingReserveMapper.updateByMeetingId(updateBean, meetingId);
		
		log.info("预约会议已取消: meetingId=" + meetingId + ", userId=" + userId);
	}


	//会议预约被邀者离开
	@Override
	public void deleteMeetingReserveByUserId(String meetingId, String userId) {
		MeetingReserve meetingReserve = this.meetingReserveMapper.selectByMeetingId(meetingId);
		if (meetingReserve==null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		
		// 如果是创建者，则取消整个会议
		if (meetingReserve.getCreateUserId().equals(userId)){
			deleteMeetingReserveByMeetingId(meetingReserve.getMeetingId(), userId);
		} else {
			// 如果是被邀请者，则从成员列表中删除
			MeetingReserveMemberQuery meetingReserveMemberQuery = new MeetingReserveMemberQuery();
			meetingReserveMemberQuery.setMeetingId(meetingId);
			meetingReserveMemberQuery.setInviteUserId(userId);
			this.meetingReserveMemberMapper.deleteByParam(meetingReserveMemberQuery);
			log.info("用户离开预约会议: meetingId=" + meetingId + ", userId=" + userId);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void createMeetingReserve(MeetingReserve bean) {
		// 验证输入
		validateMeetingReserveInput(bean);
		
		bean.setMeetingId(StringTools.getMeetingNoOrMettingId());
		bean.setCreateTime(new Date());
		bean.setStatus(MeetingReserveStatusEnum.NO_START.getStatus());
		this.meetingReserveMapper.insert(bean);
		
		ArrayList<MeetingReserveMember> reserveMembers = new ArrayList<>();
		
		// 先添加创建者
		MeetingReserveMember hostMember = new MeetingReserveMember();
		hostMember.setMeetingId(bean.getMeetingId());
		hostMember.setInviteUserId(bean.getCreateUserId());
		reserveMembers.add(hostMember);
		
		// 获取创建者信息
		com.easymeeting.entity.po.UserInfo creatorInfo = userInfoMapper.selectByUserId(bean.getCreateUserId());
		String creatorName = creatorInfo != null ? creatorInfo.getNickName() : "未知用户";
		
		// 添加被邀请者（排除创建者自己）
		if (!StringUtils.isEmpty(bean.getInviteUserIds())){
			String[] inviteUserIdArray = bean.getInviteUserIds().split(",");
			for (String id : inviteUserIdArray) {
				// 跳过创建者自己
				if (!id.trim().equals(bean.getCreateUserId())) {
					MeetingReserveMember meetingReserveMember = new MeetingReserveMember();
					meetingReserveMember.setInviteUserId(id.trim());
					meetingReserveMember.setMeetingId(bean.getMeetingId());
					reserveMembers.add(meetingReserveMember);
					
					// 发送会议邀请通知
					try {
						userNotificationService.createMeetingInviteNotification(
							bean.getMeetingId(),
							id.trim(),
							creatorName,
							bean.getMeetingName(),
							bean.getStartTime()
						);
						log.info("已为用户 " + id.trim() + " 创建会议邀请通知");
					} catch (Exception e) {
						log.error("创建会议邀请通知失败，但不影响会议创建", e);
					}
				}
			}
		}
		
		this.meetingReserveMemberMapper.insertBatch(reserveMembers);
	}

	@Override
	public List<MeetingReserve> loadTodayMeeting(String userId) {
		MeetingReserveMemberQuery meetingReserveMemberQuery = new MeetingReserveMemberQuery();
		meetingReserveMemberQuery.setInviteUserId(userId);
		List<MeetingReserveMember> meetingReserveMembers = this.meetingReserveMemberMapper.selectList(meetingReserveMemberQuery);
		List<MeetingReserve> meetingReserveList = new ArrayList<>();
		if (meetingReserveMembers.isEmpty()){
			return null;
		}else {
			for (MeetingReserveMember reserveMember : meetingReserveMembers) {
				String meetingId = reserveMember.getMeetingId();
				MeetingReserve meetingReserve = this.meetingReserveMapper.selectByMeetingId(meetingId);
				if (meetingReserve==null){
					return null;
				}else{
					meetingReserveList.add(meetingReserve);
				}
			}
			return meetingReserveList;
		}
	}

	@Override
	public List<MeetingReserve> loadMeetingReserveList(String userId) {
		// 查询用户参与的所有预约会议ID
		MeetingReserveMemberQuery memberQuery = new MeetingReserveMemberQuery();
		memberQuery.setInviteUserId(userId);
		List<MeetingReserveMember> members = this.meetingReserveMemberMapper.selectList(memberQuery);
		
		if (members == null || members.isEmpty()) {
			return new ArrayList<>();
		}
		
		// 提取所有 meetingId
		List<String> meetingIds = new ArrayList<>();
		for (MeetingReserveMember member : members) {
			meetingIds.add(member.getMeetingId());
		}
		
		// 批量查询预约会议详情
		MeetingReserveQuery reserveQuery = new MeetingReserveQuery();
		reserveQuery.setMeetingIds(meetingIds);
		reserveQuery.setStatusNotEqual(MeetingReserveStatusEnum.CANCELLED.getStatus());
		reserveQuery.setOrderBy("start_time DESC");
		reserveQuery.setQueryUserInfo(true);
		
		return this.meetingReserveMapper.selectList(reserveQuery);
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateMeetingReserve(MeetingReserve bean, String userId) {
		// 查询现有预约会议
		MeetingReserve existing = this.meetingReserveMapper.selectByMeetingId(bean.getMeetingId());
		if (existing == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		
		// 验证权限
		if (!existing.getCreateUserId().equals(userId)) {
			throw new BusinessException("只有创建者可以修改预约会议");
		}
		
		// 验证状态
		if (!MeetingReserveStatusEnum.NO_START.getStatus().equals(existing.getStatus())) {
			throw new BusinessException("只能修改未开始的预约会议");
		}
		
		// 验证修改内容
		if (bean.getStartTime() != null && bean.getStartTime().before(new Date())) {
			throw new BusinessException("开始时间必须是未来时间");
		}
		
		// 更新预约会议信息
		this.meetingReserveMapper.updateByMeetingId(bean, bean.getMeetingId());
		
		// 如果修改了邀请列表，更新成员表
		if (bean.getInviteUserIds() != null) {
			// 删除现有成员（除了创建者）
			MeetingReserveMemberQuery deleteQuery = new MeetingReserveMemberQuery();
			deleteQuery.setMeetingId(bean.getMeetingId());
			deleteQuery.setInviteUserIdNotEqual(userId);
			this.meetingReserveMemberMapper.deleteByParam(deleteQuery);
			
			// 插入新成员
			if (!StringUtils.isEmpty(bean.getInviteUserIds())) {
				List<MeetingReserveMember> newMembers = new ArrayList<>();
				String[] inviteUserIdArray = bean.getInviteUserIds().split(",");
				for (String inviteUserId : inviteUserIdArray) {
					if (!inviteUserId.trim().equals(userId)) {
						MeetingReserveMember member = new MeetingReserveMember();
						member.setMeetingId(bean.getMeetingId());
						member.setInviteUserId(inviteUserId.trim());
						newMembers.add(member);
					}
				}
				if (!newMembers.isEmpty()) {
					this.meetingReserveMemberMapper.insertBatch(newMembers);
				}
			}
		}
	}
	
	@Override
	public com.easymeeting.entity.dto.MeetingReserveDetailDto getMeetingReserveDetail(String meetingId, String userId) {
		// 验证用户访问权限
		if (!checkMeetingReserveAccess(meetingId, userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		
		// 查询预约会议基本信息
		MeetingReserve meetingReserve = this.meetingReserveMapper.selectByMeetingId(meetingId);
		if (meetingReserve == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		
		// 查询所有参与者
		MeetingReserveMemberQuery memberQuery = new MeetingReserveMemberQuery();
		memberQuery.setMeetingId(meetingId);
		List<MeetingReserveMember> members = this.meetingReserveMemberMapper.selectList(memberQuery);
		
		List<com.easymeeting.entity.po.UserInfo> inviteMembers = new ArrayList<>();
		for (MeetingReserveMember member : members) {
			com.easymeeting.entity.po.UserInfo userInfo = userInfoMapper.selectByUserId(member.getInviteUserId());
			if (userInfo != null) {
				inviteMembers.add(userInfo);
			}
		}
		
		// 构建返回对象
		com.easymeeting.entity.dto.MeetingReserveDetailDto detailDto = new com.easymeeting.entity.dto.MeetingReserveDetailDto();
		detailDto.setMeetingReserve(meetingReserve);
		detailDto.setInviteMembers(inviteMembers);
		detailDto.setIsCreator(meetingReserve.getCreateUserId().equals(userId));
		detailDto.setMeetingStatus(MeetingReserveStatusEnum.getByStatus(meetingReserve.getStatus()).getDesc());
		
		return detailDto;
	}
	
	@Override
	public boolean checkMeetingReserveAccess(String meetingId, String userId) {
		MeetingReserveMember member = this.meetingReserveMemberMapper.selectByMeetingIdAndInviteUserId(meetingId, userId);
		return member != null;
	}
	
	@Override
	public List<MeetingReserve> getUpcomingMeetings(String userId) {
		// 计算时间范围
		Date now = new Date();
		Date oneHourLater = new Date(now.getTime() + 60 * 60 * 1000);
		
		// 查询用户参与的预约会议
		MeetingReserveMemberQuery memberQuery = new MeetingReserveMemberQuery();
		memberQuery.setInviteUserId(userId);
		List<MeetingReserveMember> members = this.meetingReserveMemberMapper.selectList(memberQuery);
		
		if (members == null || members.isEmpty()) {
			return new ArrayList<>();
		}
		
		// 提取 meetingId 列表
		List<String> meetingIds = new ArrayList<>();
		for (MeetingReserveMember member : members) {
			meetingIds.add(member.getMeetingId());
		}
		
		// 查询即将开始的会议
		MeetingReserveQuery reserveQuery = new MeetingReserveQuery();
		reserveQuery.setMeetingIds(meetingIds);
		reserveQuery.setStatus(MeetingReserveStatusEnum.NO_START.getStatus());
		reserveQuery.setStartTimeStart(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now));
		reserveQuery.setStartTimeEnd(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(oneHourLater));
		reserveQuery.setOrderBy("start_time ASC");
		reserveQuery.setQueryUserInfo(true);
		
		return this.meetingReserveMapper.selectList(reserveQuery);
	}

	/**
	 * 验证预约会议输入
	 */
	private void validateMeetingReserveInput(MeetingReserve bean) {
		if (StringUtils.isEmpty(bean.getMeetingName()) || bean.getMeetingName().length() > 50) {
			throw new BusinessException("会议名称长度必须在1-50之间");
		}
		
		if (bean.getStartTime() == null || bean.getStartTime().before(new Date())) {
			throw new BusinessException("开始时间必须是未来时间");
		}
		
		if (bean.getDuration() == null || bean.getDuration() < 15 || bean.getDuration() > 480) {
			throw new BusinessException("会议时长必须在15-480分钟之间");
		}
		
		if (com.easymeeting.entity.enums.MeetingJoinTypeEnum.PASSWORD.getStatus().equals(bean.getJoinType())) {
			if (StringUtils.isEmpty(bean.getJoinPassword()) || bean.getJoinPassword().length() != 5) {
				throw new BusinessException("密码必须是5位字符");
			}
		}
	}
}
