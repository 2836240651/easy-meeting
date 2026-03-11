-- 用户设置表
-- 如果表已存在，先删除
DROP TABLE IF EXISTS `user_settings`;

CREATE TABLE `user_settings` (
  `user_id` VARCHAR(12) NOT NULL COMMENT '用户ID',
  
  -- 会议设置
  `default_video_on` TINYINT(1) DEFAULT 0 COMMENT '默认开启摄像头',
  `default_audio_on` TINYINT(1) DEFAULT 1 COMMENT '默认开启麦克风',
  `reminder_time` INT DEFAULT 10 COMMENT '会议提醒时间(分钟)',
  
  -- 通知设置
  `desktop_notification` TINYINT(1) DEFAULT 1 COMMENT '桌面通知',
  `sound_notification` TINYINT(1) DEFAULT 1 COMMENT '声音提醒',
  `meeting_invite_notification` TINYINT(1) DEFAULT 1 COMMENT '会议邀请通知',
  `friend_request_notification` TINYINT(1) DEFAULT 1 COMMENT '好友申请通知',
  
  -- 隐私设置
  `show_online_status` TINYINT(1) DEFAULT 1 COMMENT '显示在线状态',
  `allow_stranger_add` TINYINT(1) DEFAULT 1 COMMENT '允许陌生人添加',
  
  -- 外观设置
  `dark_mode` TINYINT(1) DEFAULT 0 COMMENT '深色模式',
  `language` VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言',
  
  -- 视频设置
  `video_quality` VARCHAR(20) DEFAULT 'high' COMMENT '视频质量(low/medium/high/ultra)',
  `mirror_video` TINYINT(1) DEFAULT 1 COMMENT '镜像视频',
  `virtual_background` TINYINT(1) DEFAULT 0 COMMENT '虚拟背景',
  
  -- 音频设置
  `echo_cancellation` TINYINT(1) DEFAULT 1 COMMENT '回声消除',
  `noise_suppression` TINYINT(1) DEFAULT 1 COMMENT '噪音抑制',
  `auto_gain_control` TINYINT(1) DEFAULT 1 COMMENT '自动增益',
  
  -- 屏幕共享设置
  `share_system_audio` TINYINT(1) DEFAULT 1 COMMENT '共享系统音频',
  `optimize_video_sharing` TINYINT(1) DEFAULT 1 COMMENT '优化视频共享',
  
  -- 网络设置
  `auto_reconnect` TINYINT(1) DEFAULT 1 COMMENT '自动重连',
  `show_network_status` TINYINT(1) DEFAULT 1 COMMENT '显示网络状态',
  
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表';

-- 注意：外键约束已移除，应用层需要保证数据一致性
-- 如果需要外键，可以手动执行以下语句（需要确保user_info表的user_id字段类型完全匹配）：
-- ALTER TABLE `user_settings` 
-- ADD CONSTRAINT `fk_user_settings_user_id` 
-- FOREIGN KEY (`user_id`) 
-- REFERENCES `user_info` (`user_id`) 
-- ON DELETE CASCADE;
