-- 修正 user_contact_apply 表中的字段名拼写错误
-- 将 last_appply_time 改为 last_apply_time

USE easymeeting;

-- 修改字段名
ALTER TABLE user_contact_apply 
CHANGE COLUMN last_appply_time last_apply_time DATETIME;

-- 验证修改结果
DESCRIBE user_contact_apply;

-- 查看修改后的数据
SELECT apply_id, apply_user_id, receive_user_id, status, last_apply_time 
FROM user_contact_apply 
LIMIT 5;