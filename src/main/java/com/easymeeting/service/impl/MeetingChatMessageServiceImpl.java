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
import com.easymeeting.exception.BusinessException;
import com.easymeeting.utils.*;
import com.easymeeting.websocket.message.MessageHandler;
import jodd.util.ArraysUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easymeeting.entity.query.MeetingChatMessageQuery;
import com.easymeeting.entity.po.MeetingChatMessage;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.query.SimplePage;
import com.easymeeting.mappers.MeetingChatMessageMapper;
import com.easymeeting.service.MeetingChatMessageService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.tags.MessageTag;


/**
 *  业务接口实现
 */
@Service("meetingChatMessageService")
public class MeetingChatMessageServiceImpl implements MeetingChatMessageService {
	@Resource
	private FFmpegUtils ffmpegUtils;
	@Resource
	private MessageHandler messageHandler;
	@Resource
	private MeetingChatMessageMapper<MeetingChatMessage, MeetingChatMessageQuery> meetingChatMessageMapper;
    @Autowired
    private AppConfig appConfig;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<MeetingChatMessage> findListByParam(String tableName,MeetingChatMessageQuery param) {
		ensureTableExists(tableName);
		return this.meetingChatMessageMapper.selectList(tableName,param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(String tableName,MeetingChatMessageQuery param) {
		ensureTableExists(tableName);
		return this.meetingChatMessageMapper.selectCount(tableName,param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<MeetingChatMessage> findListByPage(String tableName,MeetingChatMessageQuery param) {
		int count = this.findCountByParam(tableName,param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<MeetingChatMessage> list = this.findListByParam(tableName,param);
		PaginationResultVO<MeetingChatMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(String tableName,MeetingChatMessage bean) {
		ensureTableExists(tableName);
		return this.meetingChatMessageMapper.insert(tableName,bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(String tableName,List<MeetingChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		ensureTableExists(tableName);
		return this.meetingChatMessageMapper.insertBatch(tableName,listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(String tableName,List<MeetingChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		ensureTableExists(tableName);
		return this.meetingChatMessageMapper.insertOrUpdateBatch(tableName,listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(String tableName,MeetingChatMessage bean, MeetingChatMessageQuery param) {
		StringTools.checkParam(param);
		ensureTableExists(tableName);
		return this.meetingChatMessageMapper.updateByParam(tableName,bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(String tableName,MeetingChatMessageQuery param) {
		StringTools.checkParam(param);
		ensureTableExists(tableName);
		return this.meetingChatMessageMapper.deleteByParam(tableName,param);
	}

	/**
	 * 根据MessageId获取对象
	 */
	@Override
	public MeetingChatMessage getMeetingChatMessageByMessageId(String tableName,Long messageId) {
		ensureTableExists(tableName);
		return this.meetingChatMessageMapper.selectByMessageId(tableName,messageId);
	}

	/**
	 * 根据MessageId修改
	 */
	@Override
	public Integer updateMeetingChatMessageByMessageId(String tableName,MeetingChatMessage bean, Long messageId) {
		ensureTableExists(tableName);
		return this.meetingChatMessageMapper.updateByMessageId(tableName,bean, messageId);
	}

	/**
	 * 根据MessageId删除
	 */
	@Override
	public Integer deleteMeetingChatMessageByMessageId(String tableName,Long messageId) {
		ensureTableExists(tableName);
		return this.meetingChatMessageMapper.deleteByMessageId(tableName,messageId);
	}

	@Override
	public void saveMessage(MeetingChatMessage meetingChatMessage) {
	if (!ArraysUtil.contains(new Integer[]{MessageTypeEnum.CHAT_TEXT_MESSAGE.getType(),MessageTypeEnum.CHAT_MEDIA_MESSAGE.getType()},meetingChatMessage.getMessageType())){
		throw new BusinessException(ResponseCodeEnum.CODE_600);
	}
	if (meetingChatMessage.getReceiveType()==null){
		throw new BusinessException(ResponseCodeEnum.CODE_600);
	}
	if (ChatMessageReceiveEnum.USER.getType().equals(meetingChatMessage.getReceiveType())&& StringTools.isEmpty(meetingChatMessage.getReceiveUserId())){
		throw new BusinessException(ResponseCodeEnum.CODE_600);
	}
		MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(meetingChatMessage.getMessageType());
		if (messageTypeEnum.equals(MessageTypeEnum.CHAT_TEXT_MESSAGE)){
			if (StringTools.isEmpty(meetingChatMessage.getMessageContent())) {
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}

		meetingChatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
		}
		else if (meetingChatMessage.equals(MessageTypeEnum.CHAT_MEDIA_MESSAGE)){
		if (StringTools.isEmpty(meetingChatMessage.getFileName())||meetingChatMessage.getFileSize()==null||meetingChatMessage.getFileType()==null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		meetingChatMessage.setFileSuffix(StringTools.getFileSuffix(meetingChatMessage.getFileName()));
		meetingChatMessage.setStatus(MessageStatusEnum.SENDING.getStatus());
		}
		meetingChatMessage.setSendTime(System.currentTimeMillis());
		meetingChatMessage.setMessageId(SnowFlakeUtils.nextId());
		String tableName = TableSplitUtils.getMeetingChatMessageTable(meetingChatMessage.getMeetingId());
		
		// 确保分表存在
		ensureTableExists(tableName);
		
		this.meetingChatMessageMapper.insert(tableName,meetingChatMessage);
		MessageSendDto sendDto = CopyTools.copy(meetingChatMessage, MessageSendDto.class);
		
		// 🔥 确保设置必要的字段
		sendDto.setMessageType(meetingChatMessage.getMessageType());
		sendDto.setMeetingId(meetingChatMessage.getMeetingId());
		sendDto.setSendUserId(meetingChatMessage.getSendUserId());
		sendDto.setSendUserNickName(meetingChatMessage.getSendUserNickName());
		
		if (meetingChatMessage.getReceiveType().equals(ChatMessageReceiveEnum.USER.getType())){
			sendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
			messageHandler.sendMessage(sendDto);
		}else {
			sendDto.setMessageSend2Type(MessageSend2TypeEnum.GROUP.getType());
			messageHandler.sendMessage(sendDto);
		}

	}

	/**
	 * 确保分表存在，如果不存在则自动创建
	 */
	private void ensureTableExists(String tableName) {
		Integer exists = meetingChatMessageMapper.checkTableExists(tableName);
		if (exists == null || exists == 0) {
			// 表不存在，创建表
			meetingChatMessageMapper.createTable(tableName, "meeting_chat_message");
			// 立即转换字符集为 utf8mb4 以支持 Emoji
			meetingChatMessageMapper.convertTableCharset(tableName);
		}
	}

	@Override
	public void uploadFile(TokenUserInfoDto tokenUserInfo, MultipartFile file, Long messageId, Long sendTime)throws IOException {
		String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYY_MM.getPattern());
		String folder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + month;
		File file1 = new File(folder);
		if (!file1.exists()) {
			file1.mkdirs();
		}
		String filePath = folder + "/" + messageId;
		String filename = file.getOriginalFilename();
		String fileSuffix = StringTools.getFileSuffix(filename);
		if (FileTypeEnum.getBySuffix(fileSuffix)==FileTypeEnum.IMAGE){
			File tempFile = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP + StringTools.getRandomString(Constants.LENGTH_30));
			file.transferTo(tempFile);
			filePath = filePath+Constants.IMAGE_SUFFIX;
			 filePath= ffmpegUtils.transferImageType(tempFile, filePath);
			ffmpegUtils.createImageThumbnail(tempFile,filePath);
		}else if (FileTypeEnum.getBySuffix(fileSuffix)==FileTypeEnum.VIDEO){
			File videoFile = new File(appConfig.getProjectFolder() + Constants.VIDEO_SUFFIX + StringTools.getRandomString(Constants.LENGTH_30));
			file.transferTo(videoFile);
			filePath = filePath+Constants.VIDEO_SUFFIX;
			ffmpegUtils.transferVideorype(videoFile,filePath,fileSuffix);
			ffmpegUtils.createImageThumbnail(videoFile,filePath);
		}else{
			filePath = filePath+fileSuffix;
			file.transferTo(new File(filePath));
		}
		String tableName = TableSplitUtils.getMeetingChatMessageTable(tokenUserInfo.getCurrentMeetingId());
		
		// 确保分表存在
		ensureTableExists(tableName);
		
		MeetingChatMessage meetingChatMessage = new MeetingChatMessage();
		meetingChatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
		meetingChatMessage.setMessageId(messageId);
		this.meetingChatMessageMapper.updateByMessageId(tableName,meetingChatMessage,messageId);
		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setMessageId(messageId);
		messageSendDto.setMeetingId(tokenUserInfo.getUserId());
		messageSendDto.setSendUserId(tokenUserInfo.getUserId());
		messageSendDto.setStatus(MessageStatusEnum.SENDED.getStatus());
		messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.GROUP.getType());
		messageSendDto.setMessageType(MessageTypeEnum.CHAT_MEDIA_MESSAGE_UPDATE.getType());
		messageHandler.sendMessage(messageSendDto);
	}
}