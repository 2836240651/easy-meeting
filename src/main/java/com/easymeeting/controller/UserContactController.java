package com.easymeeting.controller;

import java.util.List;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.UserContactApplyStatusEnum;
import com.easymeeting.entity.enums.UserContactStatusEnum;
import com.easymeeting.entity.po.UserContactApply;
import com.easymeeting.entity.query.UserContactApplyQuery;
import com.easymeeting.entity.query.UserContactQuery;
import com.easymeeting.entity.po.UserContact;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.entity.vo.UserInfoVo4Search;
import com.easymeeting.service.UserContactApplyService;
import com.easymeeting.service.UserContactService;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 *  Controller
 */
@RestController("userContactController")
@RequestMapping("/userContact")
public class UserContactController extends ABaseController{

	@Resource
	private UserContactService userContactService;
	@Resource
	private UserContactApplyService userContactApplyService;

	@RequestMapping("/searchContact")
	@globalInterceptor
	public ResponseVO searchContact(String userId, String email) {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		
		// 验证参数：userId和email至少要有一个
		if ((userId == null || userId.trim().isEmpty()) && (email == null || email.trim().isEmpty())) {
			return getFailResponseVO("userId和email参数至少需要提供一个");
		}
		
		UserInfoVo4Search userInfoVo4Search = userContactService.searchContact(userId, email, tokenUserInfo.getUserId());
		return getSuccessResponseVO(userInfoVo4Search);
	}

	@RequestMapping("/contactApply")
	@globalInterceptor
	public ResponseVO contactApply(@NotEmpty String receiveUserId) {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		UserContactApply userContactApply = new UserContactApply();
		userContactApply.setApplyUserId(tokenUserInfo.getUserId());
		userContactApply.setReceiveUserId(receiveUserId);
		Integer status =userContactApplyService.saveUserContactApply(userContactApply);
		return getSuccessResponseVO(status);
	}

	@RequestMapping("/dealWithApply")
	@globalInterceptor
	public ResponseVO dealWithApply(@NotEmpty String applyUserId ,@NotEmpty Integer status) {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		this.userContactApplyService.dealWithApply(applyUserId,tokenUserInfo.getUserId(),status,tokenUserInfo.getNickName());
		return getSuccessResponseVO(null);
	}

	@RequestMapping("/loadContactUser")
	@globalInterceptor
	public ResponseVO loadContact() {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setUserId(tokenUserInfo.getUserId());
		userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		userContactQuery.setQueryUserInfo(true);
		userContactQuery.setOrderBy("last_update_time desc");
		List<UserContact> listByParam = this.userContactService.findListByParam(userContactQuery);
		return getSuccessResponseVO(listByParam);
	}


	@RequestMapping("/loadContactApply")
	@globalInterceptor
	public ResponseVO loadContactApply() {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		UserContactApplyQuery userContactApplyQuery = new UserContactApplyQuery();
		userContactApplyQuery.setReceiveUserId(tokenUserInfo.getUserId());
		userContactApplyQuery.setOrderBy("last_apply_time desc");
		userContactApplyQuery.setQueryUserInfo(true);
		userContactApplyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
		List<UserContactApply> listByParam = this.userContactApplyService.findListByParam(userContactApplyQuery);
		return getSuccessResponseVO(listByParam);
	}

	/**
	 * 加载所有联系人申请（包括已处理）
	 */
	@RequestMapping("/loadAllContactApply")
	@globalInterceptor
	public ResponseVO loadAllContactApply() {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		UserContactApplyQuery userContactApplyQuery = new UserContactApplyQuery();
		userContactApplyQuery.setReceiveUserId(tokenUserInfo.getUserId());
		userContactApplyQuery.setOrderBy("last_apply_time desc");
		userContactApplyQuery.setQueryUserInfo(true);
		// 不设置 status 过滤，返回所有状态的申请
		List<UserContactApply> listByParam = this.userContactApplyService.findListByParam(userContactApplyQuery);
		return getSuccessResponseVO(listByParam);
	}

	/**
	 * 加载当前用户发送的待处理申请
	 */
	@RequestMapping("/loadMyApply")
	@globalInterceptor
	public ResponseVO loadMyApply() {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		UserContactApplyQuery userContactApplyQuery = new UserContactApplyQuery();
		userContactApplyQuery.setApplyUserId(tokenUserInfo.getUserId());
		userContactApplyQuery.setOrderBy("last_apply_time desc");
		userContactApplyQuery.setQueryReceiveUserInfo(true);  // 只查询接收用户信息
		userContactApplyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
		List<UserContactApply> listByParam = this.userContactApplyService.findListByParam(userContactApplyQuery);
		return getSuccessResponseVO(listByParam);
	}



	@RequestMapping("/loadContactApplyDealWithCount")
	@globalInterceptor
	public ResponseVO loadContactApplyDealWithCount(){
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		UserContactApplyQuery userContactApplyQuery = new UserContactApplyQuery();
		userContactApplyQuery.setReceiveUserId(tokenUserInfo.getUserId());
		userContactApplyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
		Integer countByParam = this.userContactApplyService.findCountByParam(userContactApplyQuery);
		return getSuccessResponseVO(countByParam);
	}
	@RequestMapping("/delContact")
	@globalInterceptor
	public ResponseVO delContact(@NotEmpty String contactId, @NotNull Integer status) {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		// 使用当前登录用户的ID，防止恶意操作
		this.userContactService.delContact(tokenUserInfo.getUserId(), contactId, status);
		return getSuccessResponseVO(null);
	}

	/**
	 * 加载拉黑列表
	 */
	@RequestMapping("/loadBlackList")
	@globalInterceptor
	public ResponseVO loadBlackList() {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setUserId(tokenUserInfo.getUserId());
		userContactQuery.setStatus(UserContactStatusEnum.BLACKLIST.getStatus());
		userContactQuery.setQueryUserInfo(true);
		userContactQuery.setOrderBy("last_update_time desc");
		List<UserContact> listByParam = this.userContactService.findListByParam(userContactQuery);
		return getSuccessResponseVO(listByParam);
	}

	/**
	 * 取消拉黑
	 */
	@RequestMapping("/unblackContact")
	@globalInterceptor
	public ResponseVO unblackContact(@NotEmpty String contactId) {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		this.userContactService.unblackContact(tokenUserInfo.getUserId(), contactId);
		return getSuccessResponseVO(null);
	}


	/**
	 * 根据条件分页查询
	 */
	@RequestMapping("/loadDataList")
	public ResponseVO loadDataList(UserContactQuery query){
		return getSuccessResponseVO(userContactService.findListByPage(query));
	}

	/**
	 * 新增
	 */
	@RequestMapping("/add")
	public ResponseVO add(UserContact bean) {
		userContactService.add(bean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 批量新增
	 */
	@RequestMapping("/addBatch")
	public ResponseVO addBatch(@RequestBody List<UserContact> listBean) {
		userContactService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 批量新增/修改
	 */
	@RequestMapping("/addOrUpdateBatch")
	public ResponseVO addOrUpdateBatch(@RequestBody List<UserContact> listBean) {
		userContactService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 根据UserIdAndContactId查询对象
	 */
	@RequestMapping("/getUserContactByUserIdAndContactId")
	public ResponseVO getUserContactByUserIdAndContactId(String userId,String contactId) {
		return getSuccessResponseVO(userContactService.getUserContactByUserIdAndContactId(userId,contactId));
	}

	/**
	 * 根据UserIdAndContactId修改对象
	 */
	@RequestMapping("/updateUserContactByUserIdAndContactId")
	public ResponseVO updateUserContactByUserIdAndContactId(UserContact bean,String userId,String contactId) {
		userContactService.updateUserContactByUserIdAndContactId(bean,userId,contactId);
		return getSuccessResponseVO(null);
	}

	/**
	 * 根据UserIdAndContactId删除
	 */
	@RequestMapping("/deleteUserContactByUserIdAndContactId")
	public ResponseVO deleteUserContactByUserIdAndContactId(String userId,String contactId) {
		userContactService.deleteUserContactByUserIdAndContactId(userId,contactId);
		return getSuccessResponseVO(null);
	}
}