-- 用户设置表（简化版，用于测试）
CREATE TABLE IF NOT EXISTS `user_settings` (
  `user_id` VARCHAR(12) NOT NULL COMMENT '用户ID',
  `default_video_on` TINYINT(1) DEFAULT 0 COMMENT '默认开启摄像头',
  `default_audio_on` TINYINT(1) DEFAULT 1 COMMENT '默认开启麦克风',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表';
