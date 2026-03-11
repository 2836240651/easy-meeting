package com.easymeeting.controller;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.po.UserNotification;
import com.easymeeting.entity.query.UserNotificationQuery;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.service.UserNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户通知 Controller
 */
@RestController
@RequestMapping("/notification")
public class UserNotificationController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(UserNotificationController.class);

    @Resource
    private UserNotificationService userNotificationService;

    /**
     * 获取通知列表
     */
    @RequestMapping("/loadNotificationList")
    @globalInterceptor(checkLogin = true)
    public ResponseVO loadNotificationList(UserNotificationQuery query) {
        TokenUserInfoDto userDto = getTokenUserInfo();
        query.setUserId(userDto.getUserId());
        query.setOrderBy("create_time desc");
        PaginationResultVO<UserNotification> result = userNotificationService.getNotificationList(query);
        return getSuccessResponseVO(result);
    }

    /**
     * 获取未读通知数量
     */
    @RequestMapping("/getUnreadCount")
    @globalInterceptor(checkLogin = true)
    public ResponseVO getUnreadCount() {
        TokenUserInfoDto userDto = getTokenUserInfo();
        Integer count = userNotificationService.getUnreadCount(userDto.getUserId());
        return getSuccessResponseVO(count);
    }

    /**
     * 标记为已读
     */
    @RequestMapping("/markAsRead")
    @globalInterceptor(checkLogin = true)
    public ResponseVO markAsRead(Integer notificationId) {
        TokenUserInfoDto userDto = getTokenUserInfo();
        userNotificationService.markAsRead(notificationId, userDto.getUserId());
        return getSuccessResponseVO(null);
    }

    /**
     * 全部标记为已读
     */
    @RequestMapping("/markAllAsRead")
    @globalInterceptor(checkLogin = true)
    public ResponseVO markAllAsRead() {
        TokenUserInfoDto userDto = getTokenUserInfo();
        userNotificationService.markAllAsRead(userDto.getUserId());
        return getSuccessResponseVO(null);
    }

    // ========== 新增接口 ==========

    /**
     * 按类别获取通知列表
     */
    @RequestMapping("/loadNotificationsByCategory")
    @globalInterceptor(checkLogin = true)
    public ResponseVO loadNotificationsByCategory(String category, Integer pageNo, Integer pageSize) {
        TokenUserInfoDto userDto = getTokenUserInfo();
        
        // 解析类别
        com.easymeeting.entity.enums.NotificationCategory notificationCategory = null;
        if (category != null && !category.isEmpty() && !"all".equalsIgnoreCase(category)) {
            notificationCategory = com.easymeeting.entity.enums.NotificationCategory.fromString(category);
        }
        
        PaginationResultVO<UserNotification> result = userNotificationService.getNotificationsByCategory(
            userDto.getUserId(), notificationCategory, pageNo, pageSize);
        return getSuccessResponseVO(result);
    }

    /**
     * 获取待办消息列表
     */
    @RequestMapping("/loadPendingActions")
    @globalInterceptor(checkLogin = true)
    public ResponseVO loadPendingActions() {
        TokenUserInfoDto userDto = getTokenUserInfo();
        java.util.List<UserNotification> result = userNotificationService.getPendingActionNotifications(userDto.getUserId());
        return getSuccessResponseVO(result);
    }

    /**
     * 处理会议邀请
     */
    @RequestMapping("/handleMeetingInvite")
    @globalInterceptor(checkLogin = true)
    public ResponseVO handleMeetingInvite(Integer notificationId, Boolean accepted) {
        if (notificationId == null || accepted == null) {
            return getBusinessErrorResponseVO(new com.easymeeting.exception.BusinessException("参数不能为空"), null);
        }
        
        TokenUserInfoDto userDto = getTokenUserInfo();
        
        try {
            userNotificationService.handleMeetingInvite(notificationId, userDto.getUserId(), accepted);
            return getSuccessResponseVO(null);
        } catch (com.easymeeting.exception.BusinessException e) {
            return getBusinessErrorResponseVO(e, null);
        }
    }

    /**
     * 更新通知的操作状态（通过 notificationId）
     */
    @RequestMapping("/updateActionStatus")
    @globalInterceptor(checkLogin = true)
    public ResponseVO updateActionStatus(Integer notificationId, Integer actionStatus) {
        logger.info("=== 更新通知操作状态 ===");
        logger.info("notificationId: {}, actionStatus: {}", notificationId, actionStatus);
        
        if (notificationId == null || actionStatus == null) {
            logger.error("参数不能为空");
            return getBusinessErrorResponseVO(new com.easymeeting.exception.BusinessException("参数不能为空"), null);
        }
        
        TokenUserInfoDto userDto = getTokenUserInfo();
        logger.info("当前用户ID: {}", userDto.getUserId());
        
        // 先查询通知，验证权限
        UserNotification notification = userNotificationService.getByNotificationId(notificationId);
        if (notification == null) {
            logger.error("通知不存在: notificationId={}", notificationId);
            return getBusinessErrorResponseVO(new com.easymeeting.exception.BusinessException("通知不存在"), null);
        }
        
        logger.info("查询到通知: userId={}, notificationType={}, referenceId={}, currentActionStatus={}", 
            notification.getUserId(), notification.getNotificationType(), notification.getReferenceId(), notification.getActionStatus());
        
        if (!notification.getUserId().equals(userDto.getUserId())) {
            logger.error("无权限操作该通知: notificationUserId={}, currentUserId={}", 
                notification.getUserId(), userDto.getUserId());
            return getBusinessErrorResponseVO(new com.easymeeting.exception.BusinessException("无权限操作该通知"), null);
        }
        
        // 直接更新通知的 actionStatus
        logger.info("开始更新通知状态...");
        userNotificationService.updateActionStatusByNotificationId(notificationId, actionStatus);
        logger.info("✅ 通知状态更新成功");
        return getSuccessResponseVO(null);
    }
}
