package com.easymeeting.controller;

import com.easymeeting.entity.constants.Constants;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.ChatMessageReceiveEnum;
import com.easymeeting.entity.enums.ResponseCodeEnum;
import com.easymeeting.entity.po.MeetingChatMessage;
import com.easymeeting.entity.po.MeetingMember;
import com.easymeeting.entity.query.MeetingChatMessageQuery;
import com.easymeeting.entity.query.MeetingMemberQuery;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.mappers.MeetingMemberMapper;
import com.easymeeting.service.MeetingChatMessageService;
import com.easymeeting.utils.TableSplitUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.easymeeting.annotation.globalInterceptor;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
@Validated
@RequestMapping("/chat")
public class ChatController extends ABaseController{
    @Resource
    private MeetingChatMessageService meetingChatMessageService;
    @Resource
    private MeetingMemberMapper<MeetingMember, MeetingMemberQuery> meetingMemberMapper;
@RequestMapping("/loadMeesage")
@globalInterceptor
public ResponseVO loadMessage(Long maxMessageId, Long minMessageId, Integer pageNo){
    TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
    log.info("加载消息 - 会议ID: {}, maxMessageId: {}, minMessageId: {}, pageNo: {}", 
        tokenUserInfo.getCurrentMeetingId(), maxMessageId, minMessageId, pageNo);
    MeetingChatMessageQuery meetingChatMessageQuery = new MeetingChatMessageQuery();
    meetingChatMessageQuery.setMeetingId(tokenUserInfo.getCurrentMeetingId());
    meetingChatMessageQuery.setPageNo(pageNo);
    meetingChatMessageQuery.setOrderBy("m.message_id desc");
    meetingChatMessageQuery.setMaxMessage(maxMessageId);
    meetingChatMessageQuery.setMinMessage(minMessageId);
    meetingChatMessageQuery.setUserId(tokenUserInfo.getUserId());
    meetingChatMessageQuery.setQueryUserInfo(true); // 查询用户头像和昵称
    log.info("查询参数 - queryUserInfo: {}", meetingChatMessageQuery.getQueryUserInfo());
    String tableName = TableSplitUtils.getMeetingChatMessageTable(tokenUserInfo.getCurrentMeetingId());
    PaginationResultVO resultVO = meetingChatMessageService.findListByPage(tableName, meetingChatMessageQuery);
    log.info("查询结果 - 消息数量: {}", resultVO.getList() != null ? resultVO.getList().size() : 0);
    return getSuccessResponseVO(resultVO);
}
    @RequestMapping("/sendMessage")
    @globalInterceptor
    public ResponseVO sendMessage(String message,@NotNull Integer messageType, 
    @NotEmpty String receiveUserId,String fileName,Long fileSize,Integer fileType){
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
        MeetingChatMessage meetingChatMessage = new MeetingChatMessage();
        meetingChatMessage.setMeetingId(tokenUserInfo.getCurrentMeetingId());
        meetingChatMessage.setSendUserId(tokenUserInfo.getUserId());
        meetingChatMessage.setSendUserNickName(tokenUserInfo.getNickName()); // 🔥 设置发送者昵称
        meetingChatMessage.setFileName(fileName);
        meetingChatMessage.setFileSize(fileSize);
        meetingChatMessage.setFileType(fileType);
        meetingChatMessage.setMessageType(messageType);
        meetingChatMessage.setReceiveUserId(receiveUserId);
        if (Constants.ZERO_STR.equals(receiveUserId)){
            meetingChatMessage.setReceiveType(ChatMessageReceiveEnum.ALL.getType());
        }else{
            meetingChatMessage.setReceiveType(ChatMessageReceiveEnum.USER.getType());
        }
        meetingChatMessage.setMessageContent(message);
        this.meetingChatMessageService.saveMessage(meetingChatMessage);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/uploadFile")
    @globalInterceptor
    public ResponseVO uploadFile(@NotNull MultipartFile file,@NotNull Long messageId,@NotNull Long sendTime)
    throws IOException {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
        this.meetingChatMessageService.uploadFile(tokenUserInfo,file,messageId,sendTime);

        return getSuccessResponseVO(null);
    }
    @RequestMapping("/loadHistroy")
    @globalInterceptor
    public ResponseVO loadHistory(String meetingId,Long maxMessageId,Integer pageNo)throws IOException {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
        checkMember(meetingId,tokenUserInfo.getUserId());
        MeetingChatMessageQuery meetingChatMessageQuery = new MeetingChatMessageQuery();
        meetingChatMessageQuery.setMeetingId(tokenUserInfo.getCurrentMeetingId());
        meetingChatMessageQuery.setPageNo(pageNo);
        meetingChatMessageQuery.setOrderBy("m.message_id desc");
        meetingChatMessageQuery.setMaxMessage(maxMessageId);
        meetingChatMessageQuery.setUserId(tokenUserInfo.getUserId());
        meetingChatMessageQuery.setQueryUserInfo(true); // 查询用户头像和昵称
        String tableName = TableSplitUtils.getMeetingChatMessageTable(tokenUserInfo.getCurrentMeetingId());
        PaginationResultVO resultVO = meetingChatMessageService.findListByPage(tableName, meetingChatMessageQuery);

        return getSuccessResponseVO(resultVO);
    }

    private void checkMember(String meetingId, String userId) {
        MeetingMemberQuery meetingMemberQuery = new MeetingMemberQuery();
        meetingMemberQuery.setUserId(userId);
        meetingMemberQuery.setMeetingId(meetingId);
        Integer i = meetingMemberMapper.selectCount(meetingMemberQuery);
        if (i<0){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }


}
