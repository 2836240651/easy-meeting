package com.easymeeting.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easymeeting.entity.dto.MessageSendDto;
import com.easymeeting.entity.enums.*;
import com.easymeeting.entity.po.UserContact;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.entity.po.UserNotification;
import com.easymeeting.entity.query.UserContactQuery;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.mappers.UserContactMapper;
import com.easymeeting.mappers.UserInfoMapper;
import com.easymeeting.service.UserNotificationService;
import com.easymeeting.websocket.message.MessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.easymeeting.entity.query.UserContactApplyQuery;
import com.easymeeting.entity.po.UserContactApply;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.query.SimplePage;
import com.easymeeting.mappers.UserContactApplyMapper;
import com.easymeeting.service.UserContactApplyService;
import com.easymeeting.utils.StringTools;


/**
 *  业务接口实现
 */
@Service("userContactApplyService")
public class UserContactApplyServiceImpl implements UserContactApplyService {

	@Resource
	private UserContactApplyMapper<UserContactApply, UserContactApplyQuery> userContactApplyMapper;
	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;
	@Resource
	private UserInfoMapper<UserInfo, UserContactQuery> userInfoMapper;
	@Resource
	private UserNotificationService userNotificationService;
    @Qualifier("messageHandler")
    @Resource
    private MessageHandler messageHandler;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserContactApply> findListByParam(UserContactApplyQuery param) {
		return this.userContactApplyMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserContactApplyQuery param) {
		return this.userContactApplyMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserContactApply> findListByPage(UserContactApplyQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserContactApply> list = this.findListByParam(param);
		PaginationResultVO<UserContactApply> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserContactApply bean) {
		return this.userContactApplyMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserContactApply> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactApplyMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserContactApply> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactApplyMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserContactApply bean, UserContactApplyQuery param) {
		StringTools.checkParam(param);
		return this.userContactApplyMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserContactApplyQuery param) {
		StringTools.checkParam(param);
		return this.userContactApplyMapper.deleteByParam(param);
	}

	/**
	 * 根据ApplyId获取对象
	 */
	@Override
	public UserContactApply getUserContactApplyByApplyId(Integer applyId) {
		return this.userContactApplyMapper.selectByApplyId(applyId);
	}

	/**
	 * 根据ApplyId修改
	 */
	@Override
	public Integer updateUserContactApplyByApplyId(UserContactApply bean, Integer applyId) {
		return this.userContactApplyMapper.updateByApplyId(bean, applyId);
	}

	/**
	 * 根据ApplyId删除
	 */
	@Override
	public Integer deleteUserContactApplyByApplyId(Integer applyId) {
		return this.userContactApplyMapper.deleteByApplyId(applyId);
	}

	/**
	 * 根据ApplyUserIdAndReceiveUserId获取对象
	 */
	@Override
	public UserContactApply getUserContactApplyByApplyUserIdAndReceiveUserId(String applyUserId, String receiveUserId) {
		return this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserId(applyUserId, receiveUserId);
	}

	/**
	 * 根据ApplyUserIdAndReceiveUserId修改
	 */
	@Override
	public Integer updateUserContactApplyByApplyUserIdAndReceiveUserId(UserContactApply bean, String applyUserId, String receiveUserId) {
		return this.userContactApplyMapper.updateByApplyUserIdAndReceiveUserId(bean, applyUserId, receiveUserId);
	}

	/**
	 * 根据ApplyUserIdAndReceiveUserId删除
	 */
	@Override
	public Integer deleteUserContactApplyByApplyUserIdAndReceiveUserId(String applyUserId, String receiveUserId) {
		return this.userContactApplyMapper.deleteByApplyUserIdAndReceiveUserId(applyUserId, receiveUserId);
	}


	@Override
	public Integer saveUserContactApply(UserContactApply bean) {
		org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(UserContactApplyServiceImpl.class);
		log.info("saveUserContactApply - 接收到的参数: applyUserId=" + bean.getApplyUserId() + ", receiveUserId=" + bean.getReceiveUserId());
		
		UserContact myUserContact = this.userContactMapper.selectByUserIdAndContactId(bean.getApplyUserId(), bean.getReceiveUserId());
		UserContact himUserContact = this.userContactMapper.selectByUserIdAndContactId(bean.getReceiveUserId(), bean.getApplyUserId());
		if (myUserContact!=null&&himUserContact.getStatus().equals(UserContactStatusEnum.BLACKLIST.getStatus())){
			throw new BusinessException("对方已将您拉黑!");
		}
		if (myUserContact !=null &&himUserContact.getStatus().equals(UserContactStatusEnum.FRIEND.getStatus())) {
		 myUserContact = new UserContact();
		 myUserContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		 myUserContact.setLastUpdateTime(new Date());
		 this.userContactMapper.updateByUserIdAndContactId(myUserContact,bean.getApplyUserId(), bean.getReceiveUserId());
		 return UserContactStatusEnum.FRIEND.getStatus();
		}

		UserContactApply userContactApply = this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserId(bean.getApplyUserId(), bean.getReceiveUserId());
		if (userContactApply==null) {
			userContactApply = new UserContactApply();
			userContactApply.setApplyUserId(bean.getApplyUserId());
			userContactApply.setReceiveUserId(bean.getReceiveUserId());
			userContactApply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
			userContactApply.setLastApplyTime(new Date());  // 设置申请时间
			log.info("saveUserContactApply - 准备插入新记录: " + userContactApply.toString());
			this.userContactApplyMapper.insert(userContactApply);
		}else {
			UserContactApply updateContactApply = new UserContactApply();
			updateContactApply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
			updateContactApply.setLastApplyTime(new Date());
			log.info("saveUserContactApply - 准备更新已有记录: applyUserId=" + bean.getApplyUserId() + ", receiveUserId=" + bean.getReceiveUserId());
			this.userContactApplyMapper.updateByApplyUserIdAndReceiveUserId(updateContactApply, bean.getApplyUserId(), bean.getReceiveUserId());
		}
		// 创建好友申请通知
		UserInfo applyUser = this.userInfoMapper.selectByUserId(bean.getApplyUserId());
		UserNotification notification = new UserNotification();
		notification.setUserId(bean.getReceiveUserId());
		notification.setNotificationType(NotificationTypeEnum.CONTACT_APPLY_PENDING.getType());
		notification.setRelatedUserId(bean.getApplyUserId());
		notification.setRelatedUserName(applyUser != null ? applyUser.getNickName() : bean.getApplyUserId());
		notification.setTitle("好友申请");
		notification.setContent(notification.getRelatedUserName() + " 请求添加您为好友");
		notification.setStatus(0); // 未读
		notification.setActionRequired(1); // 需要操作
		notification.setActionStatus(0); // 待处理
		notification.setReferenceId("apply_" + bean.getApplyUserId() + "_" + bean.getReceiveUserId());
		userNotificationService.createNotification(notification);
		
		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
		messageSendDto.setMessageType(MessageTypeEnum.USER_CONTACT_APPLY.getType());
		messageSendDto.setReceiveUserId(bean.getReceiveUserId());
		messageHandler.sendMessage(messageSendDto);
		return UserContactApplyStatusEnum.INIT.getStatus();
	}


	@Override
	public void dealWithApply(String applyUserId, String userId, Integer status, String nickName) {
		UserContactApplyStatusEnum statusEnum = UserContactApplyStatusEnum.getByStatus(status);
		if (statusEnum.getStatus().equals(UserContactApplyStatusEnum.INIT.getStatus())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		UserContactApply userContactApply = this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserId(applyUserId, userId);
		if (userContactApply==null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if (statusEnum.getStatus().equals(UserContactApplyStatusEnum.PASS.getStatus())) {
			UserContact userContact = new UserContact();
			userContact.setUserId(applyUserId);
			userContact.setContactId(userId);
			userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			userContact.setLastUpdateTime(new Date());
			this.userContactMapper.insertOrUpdate(userContact);
			userContact.setUserId(userId);
			userContact.setContactId(applyUserId);
			this.userContactMapper.insertOrUpdate(userContact);
		}
		UserContactApply updateApply = new UserContactApply();
		updateApply.setStatus(status);
		this.userContactApplyMapper.updateByApplyId(updateApply,userContactApply.getApplyId());
		
		// 更新接收者的通知操作状态
		String referenceId = "apply_" + applyUserId + "_" + userId;
		userNotificationService.updateActionStatus(userId, referenceId, status);
		
		// 给申请人创建通知（同意或拒绝）
		UserNotification applyUserNotification = new UserNotification();
		applyUserNotification.setUserId(applyUserId);
		applyUserNotification.setNotificationType(NotificationTypeEnum.CONTACT_APPLY_PENDING.getType());
		applyUserNotification.setRelatedUserId(userId);
		applyUserNotification.setRelatedUserName(nickName);
		if (statusEnum.getStatus().equals(UserContactApplyStatusEnum.PASS.getStatus())) {
			applyUserNotification.setTitle("好友申请已同意");
			applyUserNotification.setContent(nickName + " 同意了您的联系人申请");
		} else {
			applyUserNotification.setTitle("好友申请已拒绝");
			applyUserNotification.setContent(nickName + " 拒绝了您的联系人申请");
		}
		applyUserNotification.setStatus(0); // 未读
		applyUserNotification.setActionRequired(0); // 不需要操作
		applyUserNotification.setReferenceId("apply_result_" + applyUserId + "_" + userId);
		userNotificationService.createNotification(applyUserNotification);
		
		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
		messageSendDto.setMessageType(MessageTypeEnum.USER_CONTACT_DEAL_WITH.getType());
		messageSendDto.setMessageContent(status);
		messageSendDto.setReceiveUserId(applyUserId);
		messageSendDto.setSendUserNickName(nickName);
		messageHandler.sendMessage(messageSendDto);
	}
}