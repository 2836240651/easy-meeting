package com.easymeeting.service.impl;

import com.easymeeting.entity.dto.MessageSendDto;
import com.easymeeting.entity.enums.MessageSend2TypeEnum;
import com.easymeeting.entity.enums.NotificationCategory;
import com.easymeeting.entity.enums.NotificationTypeEnum;
import com.easymeeting.entity.po.MeetingReserve;
import com.easymeeting.entity.po.UserNotification;
import com.easymeeting.entity.query.SimplePage;
import com.easymeeting.entity.query.UserNotificationQuery;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.mappers.MeetingReserveMapper;
import com.easymeeting.mappers.MeetingReserveMemberMapper;
import com.easymeeting.mappers.UserNotificationMapper;
import com.easymeeting.service.UserNotificationService;
import com.easymeeting.utils.StringTools;
import com.easymeeting.websocket.ChannelContextUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 用户通知 Service 实现
 */
@Service("userNotificationService")
public class UserNotificationServiceImpl implements UserNotificationService {

    private static final Log logger = LogFactory.getLog(UserNotificationServiceImpl.class);

    @Resource
    private UserNotificationMapper<UserNotification, UserNotificationQuery> userNotificationMapper;

    @Resource
    private MeetingReserveMapper meetingReserveMapper;

    @Resource
    private MeetingReserveMemberMapper meetingReserveMemberMapper;

    @Resource
    @Lazy
    private ChannelContextUtils channelContextUtils;

    @Override
    public void createNotification(UserNotification notification) {
        this.userNotificationMapper.insert(notification);
    }

    @Override
    public PaginationResultVO<UserNotification> getNotificationList(UserNotificationQuery query) {
        Integer count = this.userNotificationMapper.selectCount(query);
        Integer pageSize = query.getPageSize() == null ? 15 : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<UserNotification> list = this.userNotificationMapper.selectList(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    @Override
    public Integer getUnreadCount(String userId) {
        return this.userNotificationMapper.selectUnreadCount(userId);
    }

    @Override
    public void markAsRead(Integer notificationId, String userId) {
        this.userNotificationMapper.markAsRead(notificationId, userId);
    }

    @Override
    public void markAllAsRead(String userId) {
        this.userNotificationMapper.markAllAsRead(userId);
    }

    @Override
    public void updateActionStatus(String userId, String referenceId, Integer actionStatus) {
        this.userNotificationMapper.updateActionStatus(userId, referenceId, actionStatus);
    }

    @Override
    public void updateActionStatusByNotificationId(Integer notificationId, Integer actionStatus) {
        this.userNotificationMapper.updateActionStatusByNotificationId(notificationId, actionStatus);
    }

    @Override
    public UserNotification getByNotificationId(Integer notificationId) {
        return this.userNotificationMapper.selectByNotificationId(notificationId);
    }

    // ========== 新增方法实现 ==========

    @Override
    public void createMeetingInviteNotification(String meetingId, String inviteUserId, 
                                               String creatorName, String meetingName, Date startTime) {
        UserNotification notification = new UserNotification();
        notification.setUserId(inviteUserId);
        notification.setNotificationType(NotificationTypeEnum.MEETING_INVITE_PENDING.getType());
        
        // 获取会议创建者ID
        MeetingReserve meeting = (MeetingReserve) meetingReserveMapper.selectByMeetingId(meetingId);
        if (meeting != null) {
            notification.setRelatedUserId(meeting.getCreateUserId());
        }
        
        notification.setRelatedUserName(creatorName);
        notification.setTitle("会议邀请");
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        notification.setContent(creatorName + " 邀请您参加会议「" + meetingName + "」，开始时间：" + sdf.format(startTime));
        
        notification.setStatus(0); // 未读
        notification.setActionRequired(1); // 需要操作
        notification.setActionStatus(0); // 待处理
        notification.setReferenceId(meetingId);
        notification.setCreateTime(new Date());
        
        this.userNotificationMapper.insert(notification);
        logger.info("创建会议邀请通知成功，meetingId: " + meetingId + ", inviteUserId: " + inviteUserId);
        
        // 如果用户在线，通过 WebSocket 推送通知
        try {
            MessageSendDto messageSendDto = new MessageSendDto();
            messageSendDto.setMessageType(com.easymeeting.entity.enums.MessageTypeEnum.SYSTEM_NOTIFICATION.getType());
            messageSendDto.setMessageContent(notification);
            messageSendDto.setReceiveUserId(inviteUserId);
            messageSendDto.setMessageSend2Type(com.easymeeting.entity.enums.MessageSend2TypeEnum.USER.getType());
            channelContextUtils.sendMessage(messageSendDto);
        } catch (Exception e) {
            logger.error("推送会议邀请通知失败，但通知已保存到数据库", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleMeetingInvite(Integer notificationId, String userId, boolean accepted) {
        // 步骤 1: 查询并验证通知
        UserNotification notification = this.userNotificationMapper.selectByNotificationId(notificationId);
        
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException("无权限操作该通知");
        }
        
        if (!notification.getNotificationType().equals(NotificationTypeEnum.MEETING_INVITE_PENDING.getType())) {
            throw new BusinessException("该通知不是会议邀请");
        }
        
        if (notification.getActionStatus() != 0) {
            throw new BusinessException("该邀请已被处理");
        }
        
        String meetingId = notification.getReferenceId();
        MeetingReserve meeting = (MeetingReserve) meetingReserveMapper.selectByMeetingId(meetingId);
        
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }
        
        if (meeting.getStatus() == 3) {
            throw new BusinessException("该会议已被取消，无法响应邀请");
        }
        
        // 步骤 2: 更新通知状态
        notification.setActionStatus(accepted ? 1 : 2);
        notification.setUpdateTime(new Date());
        this.userNotificationMapper.updateByNotificationId(notification, notificationId);
        
        // 步骤 3: 处理会议成员记录
        try {
            if (accepted) {
                // 接受邀请：保留记录，更新 invite_status = 1
                meetingReserveMemberMapper.updateInviteStatus(meetingId, userId, 1, new Date());
                logger.info("用户接受会议邀请，更新 invite_status = 1，meetingId: " + meetingId + ", userId: " + userId);
            } else {
                // 拒绝邀请：从 meeting_reserve_member 表中删除该用户的记录
                meetingReserveMemberMapper.deleteByMeetingIdAndInviteUserId(meetingId, userId);
                logger.info("用户拒绝会议邀请，删除 meeting_reserve_member 记录，meetingId: " + meetingId + ", userId: " + userId);
            }
        } catch (Exception e) {
            logger.error("处理会议成员记录失败", e);
            throw new BusinessException("处理会议邀请失败");
        }
        
        // 步骤 4: 创建响应通知给会议创建者
        String creatorId = meeting.getCreateUserId();
        String responderName = notification.getRelatedUserName(); // 这里应该是响应者的昵称
        
        createMeetingResponseNotification(meetingId, creatorId, userId, meeting.getMeetingName(), accepted);
        
        logger.info("处理会议邀请成功，notificationId: " + notificationId + ", accepted: " + accepted);
    }

    @Override
    public void createMeetingResponseNotification(String meetingId, String creatorUserId,
                                                 String responderName, String meetingName, boolean accepted) {
        UserNotification notification = new UserNotification();
        notification.setUserId(creatorUserId);
        notification.setNotificationType(accepted ? 
            NotificationTypeEnum.MEETING_INVITE_ACCEPTED.getType() : 
            NotificationTypeEnum.MEETING_INVITE_REJECTED.getType());
        notification.setRelatedUserId(responderName); // 响应者ID
        notification.setRelatedUserName(responderName); // 响应者昵称
        notification.setTitle(accepted ? "会议邀请已接受" : "会议邀请已拒绝");
        notification.setContent(responderName + (accepted ? " 接受了您的会议邀请「" : " 拒绝了您的会议邀请「") + meetingName + "」");
        notification.setStatus(0); // 未读
        notification.setActionRequired(0); // 不需要操作
        notification.setReferenceId(meetingId);
        notification.setCreateTime(new Date());
        
        this.userNotificationMapper.insert(notification);
        logger.info("创建会议响应通知成功，meetingId: " + meetingId + ", accepted: " + accepted);
        
        // 如果创建者在线，通过 WebSocket 推送通知
        try {
            MessageSendDto messageSendDto = new MessageSendDto();
            messageSendDto.setMessageType(com.easymeeting.entity.enums.MessageTypeEnum.SYSTEM_NOTIFICATION.getType());
            messageSendDto.setMessageContent(notification);
            messageSendDto.setReceiveUserId(creatorUserId);
            messageSendDto.setMessageSend2Type(com.easymeeting.entity.enums.MessageSend2TypeEnum.USER.getType());
            channelContextUtils.sendMessage(messageSendDto);
        } catch (Exception e) {
            logger.error("推送会议响应通知失败，但通知已保存到数据库", e);
        }
    }

    @Override
    public void createMeetingCancelNotification(String meetingId, List<String> inviteUserIds,
                                               String creatorName, String meetingName) {
        if (inviteUserIds == null || inviteUserIds.isEmpty()) {
            return;
        }
        
        for (String inviteUserId : inviteUserIds) {
            UserNotification notification = new UserNotification();
            notification.setUserId(inviteUserId);
            notification.setNotificationType(NotificationTypeEnum.MEETING_CANCELLED.getType());
            notification.setRelatedUserName(creatorName);
            notification.setTitle("会议取消通知");
            notification.setContent(creatorName + " 取消了会议「" + meetingName + "」");
            notification.setStatus(0); // 未读
            notification.setActionRequired(0); // 不需要操作
            notification.setReferenceId(meetingId);
            notification.setCreateTime(new Date());
            
            this.userNotificationMapper.insert(notification);
            
            // 如果用户在线，通过 WebSocket 推送通知
            try {
                MessageSendDto messageSendDto = new MessageSendDto();
                messageSendDto.setMessageType(com.easymeeting.entity.enums.MessageTypeEnum.SYSTEM_NOTIFICATION.getType());
                messageSendDto.setMessageContent(notification);
                messageSendDto.setReceiveUserId(inviteUserId);
                messageSendDto.setMessageSend2Type(com.easymeeting.entity.enums.MessageSend2TypeEnum.USER.getType());
                channelContextUtils.sendMessage(messageSendDto);
            } catch (Exception e) {
                logger.error("推送会议取消通知失败，但通知已保存到数据库", e);
            }
        }
        
        logger.info("创建会议取消通知成功，meetingId: " + meetingId + ", 通知用户数: " + inviteUserIds.size());
    }

    @Override
    public void createMeetingTimeChangeNotification(String meetingId, List<String> inviteUserIds,
                                                   String creatorName, String meetingName, Date newStartTime) {
        if (inviteUserIds == null || inviteUserIds.isEmpty()) {
            return;
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        for (String inviteUserId : inviteUserIds) {
            UserNotification notification = new UserNotification();
            notification.setUserId(inviteUserId);
            notification.setNotificationType(NotificationTypeEnum.MEETING_TIME_CHANGED.getType());
            notification.setRelatedUserName(creatorName);
            notification.setTitle("会议时间变更通知");
            notification.setContent(creatorName + " 修改了会议「" + meetingName + "」的时间，新的开始时间：" + sdf.format(newStartTime));
            notification.setStatus(0); // 未读
            notification.setActionRequired(0); // 不需要操作
            notification.setReferenceId(meetingId);
            notification.setCreateTime(new Date());
            
            this.userNotificationMapper.insert(notification);
            
            // 如果用户在线，通过 WebSocket 推送通知
            try {
                MessageSendDto messageSendDto = new MessageSendDto();
                messageSendDto.setMessageType(com.easymeeting.entity.enums.MessageTypeEnum.SYSTEM_NOTIFICATION.getType());
                messageSendDto.setMessageContent(notification);
                messageSendDto.setReceiveUserId(inviteUserId);
                messageSendDto.setMessageSend2Type(com.easymeeting.entity.enums.MessageSend2TypeEnum.USER.getType());
                channelContextUtils.sendMessage(messageSendDto);
            } catch (Exception e) {
                logger.error("推送会议时间变更通知失败，但通知已保存到数据库", e);
            }
        }
        
        logger.info("创建会议时间变更通知成功，meetingId: " + meetingId + ", 通知用户数: " + inviteUserIds.size());
    }

    @Override
    public PaginationResultVO<UserNotification> getNotificationsByCategory(String userId, 
                                                                          NotificationCategory category,
                                                                          Integer pageNo, Integer pageSize) {
        // 参数验证
        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            pageSize = 15;
        }
        
        // 确定通知类型范围
        List<Integer> typeRange = new ArrayList<>();
        if (category == NotificationCategory.CONTACT) {
            typeRange = Arrays.asList(1, 2, 3, 4);
        } else if (category == NotificationCategory.MEETING) {
            typeRange = Arrays.asList(5, 6, 7, 8, 9, 10, 11);
        } else if (category == NotificationCategory.SYSTEM) {
            typeRange = Arrays.asList(12, 13);
        } else {
            // 如果 category 为 null，返回所有类型
            typeRange = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
        }
        
        // 构建查询条件
        UserNotificationQuery query = new UserNotificationQuery();
        query.setUserId(userId);
        query.setNotificationTypeList(typeRange);
        query.setOrderBy("create_time desc");
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        
        // 查询总数
        Integer count = this.userNotificationMapper.selectCount(query);
        
        // 查询分页数据
        SimplePage page = new SimplePage(pageNo, count, pageSize);
        query.setSimplePage(page);
        List<UserNotification> list = this.userNotificationMapper.selectList(query);
        
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    @Override
    public List<UserNotification> getPendingActionNotifications(String userId) {
        UserNotificationQuery query = new UserNotificationQuery();
        query.setUserId(userId);
        query.setActionRequired(1);
        query.setActionStatus(0);
        query.setOrderBy("create_time desc");
        
        return this.userNotificationMapper.selectList(query);
    }

    @Override
    public void createInstantMeetingInviteNotification(String meetingId, String meetingNo, 
                                                      String meetingName, String password,
                                                      String inviteUserId, String inviterUserId, 
                                                      String inviterName) {
        logger.info("创建即时会议邀请通知: meetingId=" + meetingId + ", inviteUserId=" + inviteUserId);
        
        // 构建通知内容（JSON 格式）
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("{");
        contentBuilder.append("\"meetingId\":\"").append(meetingId).append("\",");
        contentBuilder.append("\"meetingNo\":\"").append(meetingNo).append("\",");
        contentBuilder.append("\"meetingName\":\"").append(meetingName).append("\"");
        if (!StringTools.isEmpty(password)) {
            contentBuilder.append(",\"password\":\"").append(password).append("\"");
        }
        contentBuilder.append(",\"inviterName\":\"").append(inviterName).append("\",");
        contentBuilder.append("\"inviterUserId\":\"").append(inviterUserId).append("\"");
        contentBuilder.append("}");
        
        // 创建通知
        UserNotification notification = new UserNotification();
        notification.setUserId(inviteUserId);
        notification.setNotificationType(NotificationTypeEnum.MEETING_INSTANT_INVITE.getType());
        notification.setRelatedUserId(inviterUserId);
        notification.setRelatedUserName(inviterName);
        notification.setTitle("会议邀请");
        notification.setContent(contentBuilder.toString());
        notification.setStatus(0); // 未读
        notification.setActionRequired(1); // 需要操作（用户需要决定是否加入）
        notification.setActionStatus(0); // 待处理
        notification.setReferenceId(meetingId);
        notification.setCreateTime(new Date());
        notification.setUpdateTime(new Date());
        
        this.userNotificationMapper.insert(notification);
        
        // 发送 WebSocket 通知
        try {
            MessageSendDto messageDto = new MessageSendDto();
            messageDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
            messageDto.setSendUserId(inviterUserId);
            messageDto.setSendUserNickName(inviterName);
            messageDto.setReceiveUserId(inviteUserId);
            messageDto.setMessageType(com.easymeeting.entity.enums.MessageTypeEnum.SYSTEM_NOTIFICATION.getType());
            messageDto.setMessageContent("您收到了一条新的会议邀请");
            
            channelContextUtils.sendMessage(messageDto);
            logger.info("已发送即时会议邀请 WebSocket 通知给用户: " + inviteUserId);
        } catch (Exception e) {
            logger.error("发送即时会议邀请 WebSocket 通知失败", e);
        }
    }

    @Override
    public void createMeetingReminderNotification(String meetingId, String userId, 
                                                  String meetingName, Date startTime) {
        logger.info("创建会议提醒通知: meetingId=" + meetingId + ", userId=" + userId);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        // 创建通知
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setNotificationType(NotificationTypeEnum.MEETING_REMINDER.getType());
        notification.setTitle("会议提醒");
        notification.setContent("您预约的会议「" + meetingName + "」将在 5 分钟后开始，开始时间：" + sdf.format(startTime));
        notification.setStatus(0); // 未读
        notification.setActionRequired(0); // 不需要操作
        notification.setActionStatus(0);
        notification.setReferenceId(meetingId);
        notification.setCreateTime(new Date());
        notification.setUpdateTime(new Date());
        
        this.userNotificationMapper.insert(notification);
        
        // 发送 WebSocket 通知
        try {
            MessageSendDto messageDto = new MessageSendDto();
            messageDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
            messageDto.setReceiveUserId(userId);
            messageDto.setMessageType(com.easymeeting.entity.enums.MessageTypeEnum.SYSTEM_NOTIFICATION.getType());
            messageDto.setMessageContent("您有一个会议即将开始");
            
            channelContextUtils.sendMessage(messageDto);
            logger.info("已发送会议提醒 WebSocket 通知给用户: " + userId);
        } catch (Exception e) {
            logger.error("发送会议提醒 WebSocket 通知失败", e);
        }
    }
}
