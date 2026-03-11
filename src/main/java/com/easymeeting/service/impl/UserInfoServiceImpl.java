package com.easymeeting.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easymeeting.entity.config.AppConfig;
import com.easymeeting.entity.constants.Constants;
import com.easymeeting.entity.dto.MessageSendDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.*;
import com.easymeeting.entity.vo.UserInfoVo;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.redis.RedisComponent;
import com.easymeeting.utils.CopyTools;
import com.easymeeting.utils.FFmpegUtils;
import com.easymeeting.websocket.message.MessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easymeeting.entity.query.UserInfoQuery;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.query.SimplePage;
import com.easymeeting.mappers.UserInfoMapper;
import com.easymeeting.service.UserInfoService;
import com.easymeeting.utils.StringTools;
import org.springframework.web.multipart.MultipartFile;


/**
 * 业务接口实现
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {


	@Resource
	private AppConfig appConfig;

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
	@Autowired
	private RedisComponent redisComponent;
	@Resource
	private FFmpegUtils ffmpegUtils;
	@Resource
	private MessageHandler messageHandler;
	@Resource
	private com.easymeeting.service.UserSettingsService userSettingsService;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserInfo> findListByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(param);
		PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;

	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据UserId获取对象
	 */
	@Override
	public UserInfo getUserInfoByUserId(String userId) {
		return this.userInfoMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId修改
	 */
	@Override
	public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean, userId);
	}


	/**
	 * 根据UserId删除
	 */
	@Override
	public Integer deleteUserInfoByUserId(String userId) {
		// 先删除用户设置
		try {
			userSettingsService.deleteUserSettings(userId);
		} catch (Exception e) {
			// 如果删除设置失败，记录日志但继续删除用户
			System.err.println("删除用户设置失败: " + e.getMessage());
		}
		
		// 删除用户信息
		return this.userInfoMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email获取对象
	 */
	@Override
	public UserInfo getUserInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	/**
	 * 根据Email修改
	 */
	@Override
	public Integer updateUserInfoByEmail(UserInfo bean, String email) {
		return this.userInfoMapper.updateByEmail(bean, email);
	}

	/**
	 * 根据Email删除
	 */
	@Override
	public Integer deleteUserInfoByEmail(String email) {
		// 先获取用户ID
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
		if (userInfo != null) {
			// 删除用户设置
			try {
				userSettingsService.deleteUserSettings(userInfo.getUserId());
			} catch (Exception e) {
				// 如果删除设置失败，记录日志但继续删除用户
				System.err.println("删除用户设置失败: " + e.getMessage());
			}
		}
		
		// 删除用户信息
		return this.userInfoMapper.deleteByEmail(email);
	}

	@Override
	public void register(String email, String nickName, String password) {
		UserInfo user = this.userInfoMapper.selectByEmail(email);
		if (user != null) {
			throw new BusinessException("邮箱已存在");
		}
		Date curDate = new Date();
		String userId = StringTools.getRandomString(Constants.LENGTH_12);
		UserInfo userInfo = new UserInfo();
		userInfo.setUserId(userId);
		userInfo.setEmail(email);
		userInfo.setNickName(nickName);
		userInfo.setPassword(StringTools.encodebyMd5(password));
		userInfo.setCreateTime(curDate);
		userInfo.setLasgOffTime(curDate.getTime());
		userInfo.setMeetingNo(StringTools.getMeetingNoOrMettingId());
		userInfo.setStatus(UserStatusEnum.enable.getStatus());
		this.userInfoMapper.insert(userInfo);
	}

	@Override
	public UserInfoVo login(String email, String password) {
		password = StringTools.encodebyMd5(password);
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
		if (userInfo == null || !userInfo.getPassword().equals(password)) {
			throw new BusinessException("账户或密码不正确");
		}
		if (UserStatusEnum.disable.getStatus().equals(userInfo.getStatus())) {
			throw new BusinessException("账户被禁用");
		}

		// 确保用户有个人会议号，如果没有则生成一个
		if (StringTools.isEmpty(userInfo.getMeetingNo())) {
			userInfo.setMeetingNo(StringTools.getMeetingNoOrMettingId());
			this.userInfoMapper.updateByEmail(userInfo, email);
		}

	/*	if (userInfo.getLasgOffTime()==null || userInfo.getLasgOffTime()<=userInfo.getLastLoginTime()){
			throw new BusinessException("请勿重复登录");
		}*/
		TokenUserInfoDto tokenUserInfoDto = CopyTools.copy(userInfo, TokenUserInfoDto.class);
		String Token = StringTools.encodebyMd5(tokenUserInfoDto.getUserId() + StringTools.getRandomString(Constants.LENGTH_20));
		tokenUserInfoDto.setToken(Token);
		tokenUserInfoDto.setMyMeetingNo(userInfo.getMeetingNo());
		tokenUserInfoDto.setAdmin(appConfig.getAdminEmails().contains(email));
		redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);
		UserInfoVo userInfoVo = CopyTools.copy(userInfo, UserInfoVo.class);
		userInfoVo.setToken(Token);
		userInfoVo.setAdmin(tokenUserInfoDto.getAdmin());
		userInfoVo.setSuccess(true);
		Date curDate = new Date();
		userInfo.setLastLoginTime(curDate.getTime());
		this.userInfoMapper.updateByEmail(userInfo, email);
		return userInfoVo;
	}

	@Override
	public void changePassword(String userId, String oldPassword, String newPassword) throws Exception {
		UserInfo userInfo = this.userInfoMapper.selectByUserId(userId);
		if (userInfo==null){
			throw new BusinessException("用户不存在！！");
		}
		if (!StringTools.encodebyMd5(oldPassword).equals(userInfo.getPassword())) {
			throw new BusinessException("旧密码错误!!");
		}
		String encryptedNewPassword = StringTools.encodebyMd5(newPassword);
		userInfo.setPassword(encryptedNewPassword);
		userInfoMapper.updateByUserId(userInfo, userId);

	}

	@Override
	public void updateUserInfoByUserId(UserInfo bean, String userId, MultipartFile avatar) throws IOException {
		if (avatar != null) {
			String folder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
			File file = new File(folder);
			if (!file.exists()) {
				file.mkdirs();
			}
			String realFileName = userId + Constants.IMAGE_SUFFIX;
			String filePath = folder + realFileName;
			File tempFile = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP + StringTools.getRandomString(Constants.LENGTH_30));
			avatar.transferTo(tempFile);
			ffmpegUtils.createImageThumbnail(tempFile, filePath);
		}
		this.userInfoMapper.updateByUserId(bean, userId);
		TokenUserInfoDto updateToken = redisComponent.getTokenByUserId(userId);
		updateToken.setCurrentNickName(bean.getNickName());
		updateToken.setSex(bean.getSex());
		redisComponent.saveTokenUserInfoDto(updateToken);
		return;
	}

	@Override
	public void updatePassword(UserInfo bean, String userId) {
		this.userInfoMapper.updateByUserId(bean, userId);
		redisComponent.cleanTokenByUserId(userId);
	}

	@Override
	public void updateUserStatus(UserInfo userInfo) {
		Integer status = userInfo.getStatus();
		UserStatusEnum userStatusEnum = UserStatusEnum.getUserStatusEnum(status);
		if (userStatusEnum == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		this.userInfoMapper.updateByUserId(userInfo, userInfo.getUserId());
		if (userInfo.getStatus().equals(UserStatusEnum.disable.getStatus())) {
			forceOffLine(userInfo.getUserId());
		}
	}

	/*
	 * 管理员设置状态后如果用户已经为下线状态那么直接返回即可
	 * 如果用户为上线状态需要将其强制踢下线，并告知其被踢下线信息，并删除其token
	 * */
	@Override
	public void forceOffLine(String userId) {
		if (this.userInfoMapper.selectByUserId(userId).getOnlineType().equals(Constants.ZERO)) {
			return;
		}

		MessageSendDto sendDto = new MessageSendDto<>();
		sendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
		sendDto.setMessageType(MessageTypeEnum.FORCE_OFF_LINE.getType());
		sendDto.setReceiveUserId(userId);
		Date date = new Date();
		sendDto.setSendTime(date.getTime());
		messageHandler.sendMessage(sendDto);
		redisComponent.cleanTokenByUserId(userId);


	}


}