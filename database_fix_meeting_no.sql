-- 为现有用户生成个人会议号的数据库脚本
-- 执行此脚本为所有没有个人会议号的用户生成一个

-- 1. 首先查看当前没有个人会议号的用户数量
SELECT COUNT(*) as users_without_meeting_no
FROM user_info 
WHERE meeting_no IS NULL OR meeting_no = '';

-- 2. 查看具体哪些用户没有个人会议号
SELECT user_id, email, nick_name, meeting_no, create_time
FROM user_info 
WHERE meeting_no IS NULL OR meeting_no = ''
ORDER BY create_time DESC;

-- 3. 为没有个人会议号的用户生成个人会议号
-- 使用随机10位字符串（字母+数字组合）
UPDATE user_info 
SET meeting_no = CONCAT(
    SUBSTRING('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', FLOOR(1 + RAND() * 36), 1),
    SUBSTRING('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', FLOOR(1 + RAND() * 36), 1),
    SUBSTRING('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', FLOOR(1 + RAND() * 36), 1),
    SUBSTRING('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', FLOOR(1 + RAND() * 36), 1),
    SUBSTRING('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', FLOOR(1 + RAND() * 36), 1),
    SUBSTRING('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', FLOOR(1 + RAND() * 36), 1),
    SUBSTRING('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', FLOOR(1 + RAND() * 36), 1),
    SUBSTRING('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', FLOOR(1 + RAND() * 36), 1),
    SUBSTRING('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', FLOOR(1 + RAND() * 36), 1),
    SUBSTRING('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', FLOOR(1 + RAND() * 36), 1)
)
WHERE meeting_no IS NULL OR meeting_no = '';

-- 4. 验证更新结果 - 检查是否还有用户没有会议号
SELECT COUNT(*) as remaining_users_without_meeting_no
FROM user_info 
WHERE meeting_no IS NULL OR meeting_no = '';

-- 5. 查看所有用户的会议号（验证生成结果）
SELECT user_id, email, nick_name, meeting_no, create_time
FROM user_info 
ORDER BY create_time DESC
LIMIT 10;

-- 6. 检查会议号是否有重复（确保唯一性）
SELECT meeting_no, COUNT(*) as count
FROM user_info 
WHERE meeting_no IS NOT NULL AND meeting_no != ''
GROUP BY meeting_no 
HAVING COUNT(*) > 1;