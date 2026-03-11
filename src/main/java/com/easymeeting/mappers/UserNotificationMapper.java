package com.easymeeting.mappers;

import com.easymeeting.entity.po.UserNotification;
import com.easymeeting.entity.query.UserNotificationQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户通知 Mapper
 */
public interface UserNotificationMapper<T, P> extends BaseMapper<T, P> {

    /**
     * 根据通知ID查询
     */
    T selectByNotificationId(@Param("notificationId") Integer notificationId);

    /**
     * 根据通知ID更新
     */
    Integer updateByNotificationId(@Param("bean") T t, @Param("notificationId") Integer notificationId);

    /**
     * 根据通知ID删除
     */
    Integer deleteByNotificationId(@Param("notificationId") Integer notificationId);

    /**
     * 获取未读通知数量
     */
    Integer selectUnreadCount(@Param("userId") String userId);

    /**
     * 标记为已读
     */
    Integer markAsRead(@Param("notificationId") Integer notificationId, @Param("userId") String userId);

    /**
     * 全部标记为已读
     */
    Integer markAllAsRead(@Param("userId") String userId);

    /**
     * 更新操作状态
     */
    Integer updateActionStatus(@Param("userId") String userId, @Param("referenceId") String referenceId, @Param("actionStatus") Integer actionStatus);

    /**
     * 通过通知ID更新操作状态
     */
    Integer updateActionStatusByNotificationId(@Param("notificationId") Integer notificationId, @Param("actionStatus") Integer actionStatus);
}
