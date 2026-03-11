-- 用户通知表
CREATE TABLE IF NOT EXISTS `user_notification` (
  `notification_id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` VARCHAR(15) NOT NULL COMMENT '接收通知的用户ID',
  `notification_type` TINYINT(2) NOT NULL COMMENT '通知类型：1=好友申请，2=联系人删除，3=系统通知',
  `related_user_id` VARCHAR(15) DEFAULT NULL COMMENT '相关用户ID（发送申请的用户或删除你的用户）',
  `related_user_name` VARCHAR(50) DEFAULT NULL COMMENT '相关用户昵称',
  `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
  `content` VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '通知状态：0=未读，1=已读',
  `action_required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要操作：0=不需要，1=需要（如好友申请需要同意/拒绝）',
  `action_status` TINYINT(2) DEFAULT NULL COMMENT '操作状态：0=待处理，1=已同意，2=已拒绝',
  `reference_id` VARCHAR(50) DEFAULT NULL COMMENT '关联ID（如申请ID、会议ID等）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`notification_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';

-- 插入示例数据说明
-- 好友申请通知示例：
-- INSERT INTO user_notification (user_id, notification_type, related_user_id, related_user_name, title, content, action_required, action_status, reference_id)
-- VALUES ('user123', 1, 'user456', '张三', '好友申请', '张三 请求添加您为好友', 1, 0, 'apply_id_123');

-- 联系人删除通知示例：
-- INSERT INTO user_notification (user_id, notification_type, related_user_id, related_user_name, title, content, action_required, action_status)
-- VALUES ('user123', 2, 'user456', '张三', '联系人删除通知', '张三 已将您从好友列表中删除', 0, NULL);

-- 系统通知示例：
-- INSERT INTO user_notification (user_id, notification_type, title, content, action_required)
-- VALUES ('user123', 3, '系统维护通知', '系统将于今晚22:00进行维护，预计持续1小时', 0);
