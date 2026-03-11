-- AI助手相关数据库表
-- 创建时间: 2026-03-05

USE easymeeting;

-- 1. AI对话记录表
CREATE TABLE IF NOT EXISTS ai_conversation (
    conversation_id VARCHAR(20) PRIMARY KEY COMMENT '对话ID',
    meeting_id VARCHAR(20) NOT NULL COMMENT '会议ID',
    user_id VARCHAR(20) NOT NULL COMMENT '用户ID',
    user_message TEXT NOT NULL COMMENT '用户消息',
    ai_response TEXT NOT NULL COMMENT 'AI回复',
    message_type VARCHAR(50) NOT NULL COMMENT '消息类型(QUESTION_ANSWER/COMMAND/SUMMARY/SUGGESTION)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_meeting_id (meeting_id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';

-- 2. 会议摘要表
CREATE TABLE IF NOT EXISTS meeting_summary (
    summary_id VARCHAR(20) PRIMARY KEY COMMENT '摘要ID',
    meeting_id VARCHAR(20) NOT NULL COMMENT '会议ID',
    meeting_name VARCHAR(100) COMMENT '会议名称',
    summary_content TEXT NOT NULL COMMENT '摘要内容',
    key_points TEXT COMMENT '关键要点(JSON数组)',
    participants TEXT COMMENT '参与者列表(JSON数组)',
    duration INT COMMENT '会议时长(分钟)',
    message_count INT COMMENT '消息数量',
    generated_by VARCHAR(50) NOT NULL DEFAULT 'AI' COMMENT '生成方式(AI/MANUAL)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_meeting_id (meeting_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议摘要表';

-- 3. AI建议表
CREATE TABLE IF NOT EXISTS ai_suggestion (
    suggestion_id VARCHAR(20) PRIMARY KEY COMMENT '建议ID',
    meeting_id VARCHAR(20) NOT NULL COMMENT '会议ID',
    suggestion_type VARCHAR(50) NOT NULL COMMENT '建议类型',
    suggestion_content TEXT NOT NULL COMMENT '建议内容',
    priority INT DEFAULT 0 COMMENT '优先级(0-10)',
    status TINYINT DEFAULT 0 COMMENT '状态(0-待处理 1-已采纳 2-已忽略)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_meeting_id (meeting_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI建议表';

-- 4. 会议记录表(用于AI分析)
CREATE TABLE IF NOT EXISTS meeting_record (
    record_id VARCHAR(20) PRIMARY KEY COMMENT '记录ID',
    meeting_id VARCHAR(20) NOT NULL COMMENT '会议ID',
    record_type VARCHAR(50) NOT NULL COMMENT '记录类型(JOIN/LEAVE/CHAT/SCREEN_SHARE等)',
    user_id VARCHAR(20) COMMENT '用户ID',
    content TEXT COMMENT '记录内容',
    metadata TEXT COMMENT '元数据(JSON)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_meeting_id (meeting_id),
    INDEX idx_record_type (record_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议记录表';

-- 查看创建的表
SHOW TABLES LIKE '%ai%';
SHOW TABLES LIKE 'meeting_summary';
SHOW TABLES LIKE 'meeting_record';
