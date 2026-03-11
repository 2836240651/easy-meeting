package com.easymeeting.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easymeeting.entity.dto.MessageSendDto;
import com.easymeeting.entity.enums.MessageSend2TypeEnum;
import com.easymeeting.entity.enums.MessageTypeEnum;
import com.easymeeting.entity.enums.NotificationTypeEnum;
import com.easymeeting.entity.enums.ResponseCodeEnum;
import com.easymeeting.entity.enums.UserContactApplyStatusEnum;
import com.easymeeting.entity.enums.UserContactStatusEnum;
import com.easymeeting.entity.po.UserContactApply;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.entity.po.UserNotification;
import com.easymeeting.entity.query.UserContactApplyQuery;
import com.easymeeting.entity.query.UserInfoQuery;
import com.easymeeting.entity.vo.UserInfoVo4Search;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.mappers.UserContactApplyMapper;
import com.easymeeting.mappers.UserInfoMapper;
import com.easymeeting.service.UserNotificationService;
import com.easymeeting.websocket.message.MessageHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.easymeeting.entity.enums.PageSize;
import com.easymeeting.entity.query.UserContactQuery;
import com.easymeeting.entity.po.UserContact;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.query.SimplePage;
import com.easymeeting.mappers.UserContactMapper;
import com.easymeeting.service.UserContactService;
import com.easymeeting.utils.StringTools;


/**
 *  业务接口实现
 */
@Service("userContactService")
public class UserContactServiceImpl implements UserContactService {

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;
	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
	@Resource
	private UserContactApplyMapper<UserContactApply, UserContactApplyQuery> userContactApplyMapper;
	@Resource
	private UserNotificationService userNotificationService;
	
	@Autowired
	private ApplicationContext applicationContext;
	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserContact> findListByParam(UserContactQuery param) {
		return this.userContactMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserContactQuery param) {
		return this.userContactMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserContact> findListByPage(UserContactQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserContact> list = this.findListByParam(param);
		PaginationResultVO<UserContact> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserContact bean) {
		return this.userContactMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserContact> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserContact> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserContact bean, UserContactQuery param) {
		StringTools.checkParam(param);
		return this.userContactMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserContactQuery param) {
		StringTools.checkParam(param);
		return this.userContactMapper.deleteByParam(param);
	}

	/**
	 * 根据UserIdAndContactId获取对象
	 */
	@Override
	public UserContact getUserContactByUserIdAndContactId(String userId, String contactId) {
		return this.userContactMapper.selectByUserIdAndContactId(userId, contactId);
	}

	/**
	 * 根据UserIdAndContactId修改
	 */
	@Override
	public Integer updateUserContactByUserIdAndContactId(UserContact bean, String userId, String contactId) {
		return this.userContactMapper.updateByUserIdAndContactId(bean, userId, contactId);
	}

	/**
	 * 根据UserIdAndContactId删除
	 */
	@Override
	public Integer deleteUserContactByUserIdAndContactId(String userId, String contactId) {
		return this.userContactMapper.deleteByUserIdAndContactId(userId, contactId);
	}

	@Override
	public UserInfoVo4Search searchContact(String userId, String email, String myUserId) {
		UserInfo userInfo = null;
		
		// 根据参数选择查询方式
		if (userId != null && !userId.trim().isEmpty()) {
			userInfo = userInfoMapper.selectByUserId(userId);
		} else if (email != null && !email.trim().isEmpty()) {
			userInfo = userInfoMapper.selectByEmail(email);
		}
		
		if (userInfo == null) {
			return null;
		}
		UserInfoVo4Search userInfoVo4Search = new UserInfoVo4Search();
		userInfoVo4Search.setUserId(userInfo.getUserId());
		userInfoVo4Search.setNickName(userInfo.getNickName());
		userInfoVo4Search.setEmail(userInfo.getEmail());
		userInfoVo4Search.setSex(userInfo.getSex());
		userInfoVo4Search.setAvatar(userInfo.getAvatar());
		
		// 如果搜索的是自己
		if (userInfo.getUserId().equals(myUserId)){
			userInfoVo4Search.setStatus(-1); // 特殊状态表示是自己
			return userInfoVo4Search;
		}
		
		// 检查我是否拉黑了对方（只检查 user_contact 表）
		UserContact myContact = this.userContactMapper.selectByUserIdAndContactId(myUserId, userInfo.getUserId());
		if (myContact != null && UserContactStatusEnum.BLACKLIST.getStatus().equals(myContact.getStatus())) {
			userInfoVo4Search.setStatus(UserContactApplyStatusEnum.BLACKLIST.getStatus());
			return userInfoVo4Search;
		}
		
		// 检查是否有待处理的申请
		UserContactApply userContactApply = this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserId(myUserId, userInfo.getUserId());
		if (userContactApply!=null&&UserContactApplyStatusEnum.INIT.getStatus().equals(userContactApply.getStatus())) {
			userInfoVo4Search.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
			return userInfoVo4Search;
		}
		
		// 检查是否已经是好友
		UserContact contactUserContact = this.userContactMapper.selectByUserIdAndContactId(userInfo.getUserId(), myUserId);
		if (myContact!=null&& UserContactStatusEnum.FRIEND.getStatus().equals(myContact.getStatus())||
		contactUserContact!=null&&UserContactStatusEnum.FRIEND.getStatus().equals(contactUserContact.getStatus())) {
			userInfoVo4Search.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			return userInfoVo4Search;
		}

		// 如果没有任何关系，返回null状态（可以发送申请）
		userInfoVo4Search.setStatus(null);
		return userInfoVo4Search;
	}

	public void delContact(String userId, String contactId, Integer status) {
		org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(UserContactServiceImpl.class);
		
		if(!ArrayUtils.contains(new Integer[]{UserContactStatusEnum.DEL.getStatus(),UserContactStatusEnum.BLACKLIST.getStatus()},status)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		
		if (status.equals(UserContactStatusEnum.BLACKLIST.getStatus())) {
			// 拉黑逻辑：单向拉黑
			// 1. 更新当前用户对联系人的状态为拉黑
			UserContact userContact = new UserContact();
			userContact.setStatus(status);
			userContact.setLastUpdateTime(new Date());
			this.userContactMapper.updateByUserIdAndContactId(userContact, userId, contactId);
			log.info("拉黑联系人: userId=" + userId + ", contactId=" + contactId);
			
			// 2. 将当前用户从对方的联系人列表中删除（不是拉黑，是删除）
			UserContact contactUserContact = new UserContact();
			contactUserContact.setStatus(UserContactStatusEnum.DEL.getStatus());
			contactUserContact.setLastUpdateTime(new Date());
			this.userContactMapper.updateByUserIdAndContactId(contactUserContact, contactId, userId);
			log.info("从对方联系人列表中删除: userId=" + contactId + ", contactId=" + userId);
			
			// 拉黑不发送通知
		} else {
			// 删除好友逻辑：双向删除
			// 更新当前用户对联系人的状态
			UserContact userContact = new UserContact();
			userContact.setStatus(status);
			userContact.setLastUpdateTime(new Date());
			this.userContactMapper.updateByUserIdAndContactId(userContact, userId, contactId);
			log.info("更新联系人状态: userId=" + userId + ", contactId=" + contactId + ", status=" + status);
			
			// 更新对方对当前用户的状态（双向删除）
			UserContact contactUserContact = new UserContact();
			contactUserContact.setStatus(status);
			contactUserContact.setLastUpdateTime(new Date());
			this.userContactMapper.updateByUserIdAndContactId(contactUserContact, contactId, userId);
			log.info("更新对方联系人状态: userId=" + contactId + ", contactId=" + userId + ", status=" + status);
			
			// 创建通知并发送WebSocket通知给对方
			try {
				// 获取当前用户信息
				UserInfo userInfo = this.userInfoMapper.selectByUserId(userId);
				String nickName = userInfo != null ? userInfo.getNickName() : userId;
				
				// 创建联系人删除通知
				UserNotification notification = new UserNotification();
				notification.setUserId(contactId);
				notification.setNotificationType(NotificationTypeEnum.CONTACT_DELETED.getType());
				notification.setRelatedUserId(userId);
				notification.setRelatedUserName(nickName);
				notification.setTitle("联系人删除通知");
				notification.setContent(nickName + " 已将您从好友列表中删除");
				notification.setStatus(0); // 未读
				notification.setActionRequired(0); // 不需要操作
				userNotificationService.createNotification(notification);
				log.info("创建联系人删除通知: 从 " + userId + " 到 " + contactId);
				
				// 使用 ApplicationContext 延迟获取 MessageHandler，避免循环依赖
				MessageHandler messageHandler = applicationContext.getBean(MessageHandler.class);
				
				MessageSendDto messageSendDto = new MessageSendDto();
				messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
				messageSendDto.setMessageType(MessageTypeEnum.USER_CONTACT_DELETE.getType());
				messageSendDto.setReceiveUserId(contactId);
				messageSendDto.setSendUserNickName(nickName);
				messageSendDto.setMessageContent(status);
				messageHandler.sendMessage(messageSendDto);
				log.info("发送好友删除通知: 从 " + userId + " 到 " + contactId);
			} catch (Exception e) {
				log.error("发送好友删除通知失败: " + e.getMessage(), e);
				// 不抛出异常，删除操作已经完成
			}
		}
	}


	@Override
	public void unblackContact(String userId, String contactId) {
		org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(UserContactServiceImpl.class);

		// 检查是否确实拉黑了该用户
		UserContact userContact = this.userContactMapper.selectByUserIdAndContactId(userId, contactId);
		if (userContact == null || !userContact.getStatus().equals(UserContactStatusEnum.BLACKLIST.getStatus())) {
			throw new BusinessException("该用户不在黑名单中");
		}

		// 1. 删除当前用户对该联系人的拉黑记录
		this.userContactMapper.deleteByUserIdAndContactId(userId, contactId);
		log.info("取消拉黑，删除拉黑记录: userId=" + userId + ", contactId=" + contactId);

		// 2. 检查对方是否还有对当前用户的记录（之前被删除的）
		UserContact contactUserContact = this.userContactMapper.selectByUserIdAndContactId(contactId, userId);
		if (contactUserContact != null && contactUserContact.getStatus().equals(UserContactStatusEnum.DEL.getStatus())) {
			// 如果对方的记录是删除状态，也删除它，让双方都可以重新添加
			this.userContactMapper.deleteByUserIdAndContactId(contactId, userId);
			log.info("取消拉黑，删除对方的删除记录: userId=" + contactId + ", contactId=" + userId);
		}

		log.info("取消拉黑成功: userId=" + userId + ", contactId=" + contactId);
	}



}