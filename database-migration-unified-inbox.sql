-- ============================================
-- 统一收件箱系统数据库迁移脚本
-- ============================================

-- 步骤 1: 创建备份表
CREATE TABLE IF NOT EXISTS `user_notification_backup` AS 
SELECT * FROM `user_notification`;

-- 步骤 2: 执行通知类型迁移
-- 将类型 3（系统通知）迁移到类型 10
UPDATE `user_notification` SET `notification_type` = 10 WHERE `notification_type` = 3;

-- 将类型 2（联系人删除）迁移到类型 4
UPDATE `user_notification` SET `notification_type` = 4 WHERE `notification_type` = 2;

-- 步骤 3: 为 meeting_reserve_member 表添加新字段
ALTER TABLE `meeting_reserve_member` 
ADD COLUMN `invite_status` INT DEFAULT 0 COMMENT '邀请状态：0=待响应，1=已接受，2=已拒绝' AFTER `invite_user_id`,
ADD COLUMN `response_time` DATETIME COMMENT '响应时间' AFTER `invite_status`;

-- 步骤 4: 创建新的数据库索引
-- 优化待办消息查询
CREATE INDEX `idx_user_action` ON `user_notification`(`user_id`, `action_required`, `action_status`);

-- 优化按类别分页查询
CREATE INDEX `idx_user_type_time` ON `user_notification`(`user_id`, `notification_type`, `create_time` DESC);

-- 优化会议成员邀请状态查询
CREATE INDEX `idx_invite_status` ON `meeting_reserve_member`(`invite_status`);

-- 步骤 5: 验证迁移结果
SELECT 
    notification_type,
    COUNT(*) as count,
    CASE 
        WHEN notification_type BETWEEN 1 AND 4 THEN 'CONTACT'
        WHEN notification_type BETWEEN 5 AND 9 THEN 'MEETING'
        WHEN notification_type BETWEEN 10 AND 11 THEN 'SYSTEM'
        ELSE 'UNKNOWN'
    END as category
FROM `user_notification`
GROUP BY notification_type
ORDER BY notification_type;

-- 验证索引创建
SHOW INDEX FROM `user_notification`;
SHOW INDEX FROM `meeting_reserve_member`;

-- ============================================
-- 回滚脚本（如果需要）
-- ============================================
-- DROP TABLE IF EXISTS `user_notification_backup`;
-- ALTER TABLE `meeting_reserve_member` DROP COLUMN `invite_status`, DROP COLUMN `response_time`;
-- DROP INDEX `idx_user_action` ON `user_notification`;
-- DROP INDEX `idx_user_type_time` ON `user_notification`;
-- DROP INDEX `idx_invite_status` ON `meeting_reserve_member`;
