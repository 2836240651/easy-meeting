
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `easymeeting` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

USE `easymeeting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_conversation` (
  `conversation_id` varchar(20) NOT NULL COMMENT '对话ID',
  `meeting_id` varchar(20) NOT NULL COMMENT '会议ID',
  `user_id` varchar(20) NOT NULL COMMENT '用户ID',
  `user_message` text NOT NULL COMMENT '用户消息',
  `ai_response` text NOT NULL COMMENT 'AI回复',
  `message_type` varchar(20) DEFAULT NULL COMMENT '消息类型',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`conversation_id`),
  KEY `idx_meeting_id` (`meeting_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_suggestion` (
  `suggestion_id` varchar(20) NOT NULL COMMENT '建议ID',
  `meeting_id` varchar(20) NOT NULL COMMENT '会议ID',
  `suggestion_type` varchar(20) DEFAULT NULL COMMENT '建议类型',
  `suggestions` text NOT NULL COMMENT '建议内容(JSON格式)',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`suggestion_id`),
  KEY `idx_meeting_id` (`meeting_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI建议记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_update` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `version` varchar(10) DEFAULT NULL,
  `update_desc` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `status` tinyint(1) DEFAULT NULL,
  `grayscale_id` varchar(1000) DEFAULT NULL,
  `file_type` tinyint(1) DEFAULT NULL,
  `outer_link` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_version` (`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `meeting_info` (
  `meeting_id` varchar(10) NOT NULL,
  `meeting_no` varchar(10) DEFAULT NULL,
  `meeting_name` varchar(100) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `create_user_id` varchar(12) DEFAULT NULL,
  `join_type` tinyint(1) DEFAULT NULL,
  `join_password` varchar(5) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `status` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`meeting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `meeting_member` (
  `meeting_id` varchar(10) NOT NULL,
  `user_id` varchar(12) NOT NULL,
  `nick_name` varchar(20) DEFAULT NULL,
  `last_join_time` datetime DEFAULT NULL,
  `status` tinyint(1) DEFAULT NULL,
  `member_type` tinyint(1) DEFAULT NULL,
  `meeting_status` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`meeting_id`,`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `meeting_record` (
  `record_id` varchar(20) NOT NULL COMMENT '记录ID',
  `meeting_id` varchar(20) NOT NULL COMMENT '会议ID',
  `meeting_name` varchar(100) DEFAULT NULL COMMENT '会议名称',
  `host_user_id` varchar(20) DEFAULT NULL COMMENT '主持人ID',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` int(11) DEFAULT NULL COMMENT '时长(分钟)',
  `participant_count` int(11) DEFAULT NULL COMMENT '参与人数',
  `summary_id` varchar(20) DEFAULT NULL COMMENT '关联的摘要ID',
  `chat_messages` text COMMENT '聊天记录(JSON格式)',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `idx_meeting_id` (`meeting_id`),
  KEY `idx_host_user_id` (`host_user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `meeting_reserve` (
  `meeting_id` varchar(10) NOT NULL,
  `meeting_name` varchar(100) DEFAULT NULL,
  `join_type` tinyint(1) DEFAULT NULL,
  `join_password` varchar(5) DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `create_user_id` varchar(12) DEFAULT NULL,
  `status` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`meeting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `meeting_reserve_member` (
  `meeting_id` varchar(10) NOT NULL,
  `invite_user_id` varchar(12) NOT NULL,
  `invite_status` int(11) DEFAULT '0' COMMENT '邀请状态：0=待响应，1=已接受，2=已拒绝',
  `response_time` datetime DEFAULT NULL COMMENT '响应时间',
  PRIMARY KEY (`meeting_id`,`invite_user_id`),
  KEY `idx_invite_status` (`invite_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `meeting_summary` (
  `summary_id` varchar(20) NOT NULL COMMENT '摘要ID',
  `meeting_id` varchar(20) NOT NULL COMMENT '会议ID',
  `meeting_name` varchar(100) DEFAULT NULL COMMENT '会议名称',
  `summary_content` text COMMENT '摘要内容',
  `key_points` text COMMENT '关键要点(JSON格式)',
  `participants` text COMMENT '参与者列表(JSON格式)',
  `duration` int(11) DEFAULT NULL COMMENT '会议时长(分钟)',
  `message_count` int(11) DEFAULT NULL COMMENT '消息数量',
  `generated_by` varchar(20) DEFAULT NULL COMMENT '生成者(AI/USER)',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`summary_id`),
  UNIQUE KEY `idx_meeting_id` (`meeting_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议摘要表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_contact` (
  `user_id` varchar(12) NOT NULL,
  `contact_id` varchar(12) NOT NULL COMMENT '联系人id',
  `status` tinyint(1) DEFAULT NULL,
  `last_update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`contact_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_contact_apply` (
  `apply_id` int(11) NOT NULL AUTO_INCREMENT,
  `apply_user_id` varchar(12) DEFAULT NULL,
  `receive_user_id` varchar(12) DEFAULT NULL,
  `status` tinyint(1) DEFAULT NULL,
  `last_apply_time` datetime DEFAULT NULL,
  PRIMARY KEY (`apply_id`),
  UNIQUE KEY `idx_key` (`apply_user_id`,`receive_user_id`) USING BTREE,
  KEY `idx_last_apply_time` (`last_apply_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_info` (
  `user_id` varchar(12) NOT NULL COMMENT 'uid',
  `sex` tinyint(1) DEFAULT NULL COMMENT '性别 0:女 1:男 2:保密',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `password` varchar(32) DEFAULT NULL COMMENT 'md5密码',
  `nick_name` varchar(20) DEFAULT NULL COMMENT '昵称',
  `status` tinyint(1) DEFAULT NULL COMMENT '0：禁用 1：启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `last_login_time` bigint(20) DEFAULT NULL COMMENT '最近登录时间',
  `lasg_off_time` bigint(20) DEFAULT NULL COMMENT '最近离线时间',
  `meeting_no` varchar(10) DEFAULT NULL COMMENT '个人会议号',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `idx_key_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_notification` (
  `notification_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` varchar(15) NOT NULL COMMENT '接收通知的用户ID',
  `notification_type` tinyint(2) NOT NULL COMMENT '通知类型：1=好友申请，2=联系人删除，3=系统通知',
  `related_user_id` varchar(15) DEFAULT NULL COMMENT '相关用户ID（发送申请的用户或删除你的用户）',
  `related_user_name` varchar(50) DEFAULT NULL COMMENT '相关用户昵称',
  `title` varchar(100) NOT NULL COMMENT '通知标题',
  `content` varchar(500) DEFAULT NULL COMMENT '通知内容',
  `status` tinyint(2) NOT NULL DEFAULT '0' COMMENT '通知状态：0=未读，1=已读',
  `action_required` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要操作：0=不需要，1=需要（如好友申请需要同意/拒绝）',
  `action_status` tinyint(2) DEFAULT NULL COMMENT '操作状态：0=待处理，1=已同意，2=已拒绝',
  `reference_id` varchar(50) DEFAULT NULL COMMENT '关联ID（如申请ID、会议ID等）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`notification_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_user_action` (`user_id`,`action_required`,`action_status`),
  KEY `idx_user_type_time` (`user_id`,`notification_type`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_notification_backup` (
  `notification_id` int(11) NOT NULL DEFAULT '0' COMMENT '通知ID',
  `user_id` varchar(15) CHARACTER SET utf8mb4 NOT NULL COMMENT '接收通知的用户ID',
  `notification_type` tinyint(2) NOT NULL COMMENT '通知类型：1=好友申请，2=联系人删除，3=系统通知',
  `related_user_id` varchar(15) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '相关用户ID（发送申请的用户或删除你的用户）',
  `related_user_name` varchar(50) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '相关用户昵称',
  `title` varchar(100) CHARACTER SET utf8mb4 NOT NULL COMMENT '通知标题',
  `content` varchar(500) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '通知内容',
  `status` tinyint(2) NOT NULL DEFAULT '0' COMMENT '通知状态：0=未读，1=已读',
  `action_required` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要操作：0=不需要，1=需要（如好友申请需要同意/拒绝）',
  `action_status` tinyint(2) DEFAULT NULL COMMENT '操作状态：0=待处理，1=已同意，2=已拒绝',
  `reference_id` varchar(50) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '关联ID（如申请ID、会议ID等）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_settings` (
  `user_id` varchar(12) NOT NULL COMMENT '用户ID',
  `default_video_on` tinyint(1) DEFAULT '0' COMMENT '默认开启摄像头',
  `default_audio_on` tinyint(1) DEFAULT '1' COMMENT '默认开启麦克风',
  `reminder_time` int(11) DEFAULT '10' COMMENT '会议提醒时间(分钟)',
  `desktop_notification` tinyint(1) DEFAULT '1' COMMENT '桌面通知',
  `sound_notification` tinyint(1) DEFAULT '1' COMMENT '声音提醒',
  `meeting_invite_notification` tinyint(1) DEFAULT '1' COMMENT '会议邀请通知',
  `friend_request_notification` tinyint(1) DEFAULT '1' COMMENT '好友申请通知',
  `show_online_status` tinyint(1) DEFAULT '1' COMMENT '显示在线状态',
  `allow_stranger_add` tinyint(1) DEFAULT '1' COMMENT '允许陌生人添加',
  `dark_mode` tinyint(1) DEFAULT '0' COMMENT '深色模式',
  `language` varchar(10) DEFAULT 'zh-CN' COMMENT '语言',
  `video_quality` varchar(20) DEFAULT 'high' COMMENT '视频质量(low/medium/high/ultra)',
  `mirror_video` tinyint(1) DEFAULT '1' COMMENT '镜像视频',
  `virtual_background` tinyint(1) DEFAULT '0' COMMENT '虚拟背景',
  `echo_cancellation` tinyint(1) DEFAULT '1' COMMENT '回声消除',
  `noise_suppression` tinyint(1) DEFAULT '1' COMMENT '噪音抑制',
  `auto_gain_control` tinyint(1) DEFAULT '1' COMMENT '自动增益',
  `share_system_audio` tinyint(1) DEFAULT '1' COMMENT '共享系统音频',
  `optimize_video_sharing` tinyint(1) DEFAULT '1' COMMENT '优化视频共享',
  `auto_reconnect` tinyint(1) DEFAULT '1' COMMENT '自动重连',
  `show_network_status` tinyint(1) DEFAULT '1' COMMENT '显示网络状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

