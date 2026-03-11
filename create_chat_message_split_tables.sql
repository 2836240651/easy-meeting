-- 创建会议聊天消息分表（32张表）
-- 基于 meeting_chat_message 模板表创建

-- 首先确保模板表存在
CREATE TABLE IF NOT EXISTS `meeting_chat_message` (
  `message_id` bigint(20) NOT NULL COMMENT '消息ID',
  `meeting_id` varchar(20) NOT NULL COMMENT '会议ID',
  `message_type` tinyint(1) DEFAULT NULL COMMENT '消息类型 5:文本消息 6:媒体消息',
  `message_content` varchar(500) DEFAULT NULL COMMENT '消息内容',
  `send_user_id` varchar(20) DEFAULT NULL COMMENT '发送人ID',
  `send_time` bigint(20) DEFAULT NULL COMMENT '发送时间',
  `receive_type` tinyint(1) DEFAULT NULL COMMENT '接收类型 0:所有人 1:单人',
  `receive_user_id` varchar(20) DEFAULT NULL COMMENT '接收人ID',
  `file_name` varchar(200) DEFAULT NULL COMMENT '文件名',
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小',
  `file_type` tinyint(1) DEFAULT NULL COMMENT '文件类型',
  `file_suffix` varchar(10) DEFAULT NULL COMMENT '文件后缀',
  `status` tinyint(1) DEFAULT NULL COMMENT '状态 0:发送中 1:已发送',
  PRIMARY KEY (`message_id`),
  KEY `idx_meeting_id` (`meeting_id`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议聊天消息表';

-- 创建32张分表
CREATE TABLE IF NOT EXISTS meeting_chat_message_01 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_02 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_03 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_04 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_05 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_06 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_07 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_08 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_09 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_10 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_11 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_12 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_13 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_14 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_15 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_16 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_17 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_18 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_19 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_20 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_21 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_22 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_23 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_24 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_25 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_26 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_27 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_28 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_29 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_30 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_31 LIKE meeting_chat_message;
CREATE TABLE IF NOT EXISTS meeting_chat_message_32 LIKE meeting_chat_message;
