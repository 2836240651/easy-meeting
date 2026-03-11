package com.easymeeting.service;

import com.easymeeting.entity.enums.NotificationCategory;
import com.easymeeting.entity.po.UserNotification;
import com.easymeeting.entity.query.UserNotificationQuery;
import com.easymeeting.entity.vo.PaginationResultVO;

import java.util.Date;
import java.util.List;

/**
 * 用户通知 Service
 */
public interface UserNotificationService {

    /**
     * 创建通知
     */
    void createNotification(UserNotification notification);

    /**
     * 获取通知列表（分页）
     */
    PaginationResultVO<UserNotification> getNotificationList(UserNotificationQuery query);

    /**
     * 获取未读通知数量
     */
    Integer getUnreadCount(String userId);

    /**
     * 标记为已读
     */
    void markAsRead(Integer notificationId, String userId);

    /**
     * 全部标记为已读
     */
    void markAllAsRead(String userId);

    /**
     * 更新操作状态
     */
    void updateActionStatus(String userId, String referenceId, Integer actionStatus);

    /**
     * 通过通知ID更新操作状态
     */
    void updateActionStatusByNotificationId(Integer notificationId, Integer actionStatus);

    /**
     * 根据通知ID查询
     */
    UserNotification getByNotificationId(Integer notificationId);

    // ========== 新增方法 ==========

    /**
     * 创建会议邀请通知
     * @param meetingId 会议ID
     * @param inviteUserId 被邀请用户ID
     * @param creatorName 创建者昵称
     * @param meetingName 会议名称
     * @param startTime 会议开始时间
     */
    void createMeetingInviteNotification(String meetingId, String inviteUserId, 
                                        String creatorName, String meetingName, Date startTime);

    /**
     * 创建会议响应通知（接受/拒绝）
     * @param meetingId 会议ID
     * @param creatorUserId 会议创建者ID
     * @param responderName 响应者昵称
     * @param meetingName 会议名称
     * @param accepted 是否接受
     */
    void createMeetingResponseNotification(String meetingId, String creatorUserId,
                                          String responderName, String meetingName, boolean accepted);

    /**
     * 创建会议取消通知
     * @param meetingId 会议ID
     * @param inviteUserIds 被邀请用户ID列表
     * @param creatorName 创建者昵称
     * @param meetingName 会议名称
     */
    void createMeetingCancelNotification(String meetingId, List<String> inviteUserIds,
                                        String creatorName, String meetingName);

    /**
     * 创建会议时间变更通知
     * @param meetingId 会议ID
     * @param inviteUserIds 被邀请用户ID列表
     * @param creatorName 创建者昵称
     * @param meetingName 会议名称
     * @param newStartTime 新的开始时间
     */
    void createMeetingTimeChangeNotification(String meetingId, List<String> inviteUserIds,
                                            String creatorName, String meetingName, Date newStartTime);

    /**
     * 按类别查询通知列表
     * @param userId 用户ID
     * @param category 通知类别（CONTACT/MEETING/SYSTEM）
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PaginationResultVO<UserNotification> getNotificationsByCategory(String userId, 
                                                                    NotificationCategory category,
                                                                    Integer pageNo, Integer pageSize);

    /**
     * 获取待办消息列表（需要操作的未处理消息）
     * @param userId 用户ID
     * @return 待办消息列表
     */
    List<UserNotification> getPendingActionNotifications(String userId);

    /**
     * 处理会议邀请（接受/拒绝）
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @param accepted 是否接受
     */
    void handleMeetingInvite(Integer notificationId, String userId, boolean accepted);

    /**
     * 创建即时会议邀请通知
     * @param meetingId 会议ID
     * @param meetingNo 会议号
     * @param meetingName 会议名称
     * @param password 会议密码（可为空）
     * @param inviteUserId 被邀请用户ID
     * @param inviterUserId 邀请人用户ID
     * @param inviterName 邀请人昵称
     */
    void createInstantMeetingInviteNotification(String meetingId, String meetingNo, 
                                               String meetingName, String password,
                                               String inviteUserId, String inviterUserId, 
                                               String inviterName);

    /**
     * 创建会议提醒通知（会议开始前 5 分钟）
     * @param meetingId 会议ID
     * @param userId 用户ID
     * @param meetingName 会议名称
     * @param startTime 会议开始时间
     */
    void createMeetingReminderNotification(String meetingId, String userId, 
                                          String meetingName, Date startTime);
}
