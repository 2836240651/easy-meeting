# AI助手数据持久化设计

## 数据库表设计

### 1. ai_conversation - AI对话记录表

存储用户与AI助手的所有对话记录。

**字段说明:**
- `conversation_id`: 对话ID(主键)
- `meeting_id`: 会议ID
- `user_id`: 用户ID
- `user_message`: 用户消息
- `ai_response`: AI回复
- `message_type`: 消息类型(QUESTION_ANSWER/COMMAND_EXECUTION等)
- `create_time`: 创建时间

**用途:**
- 记录所有AI对话历史
- 用于分析用户提问模式
- 优化AI响应质量
- 审计和追溯

### 2. meeting_summary - 会议摘要表

存储AI生成的会议摘要。

**字段说明:**
- `summary_id`: 摘要ID(主键)
- `meeting_id`: 会议ID(唯一索引)
- `meeting_name`: 会议名称
- `summary_content`: 摘要内容
- `key_points`: 关键要点(JSON格式)
- `participants`: 参与者列表(JSON格式)
- `duration`: 会议时长(分钟)
- `message_count`: 消息数量
- `generated_by`: 生成者(AI/USER)
- `create_time`: 创建时间
- `update_time`: 更新时间

**用途:**
- 保存会议摘要供后续查看
- 会议记录归档
- 数据分析和报表
- 会议效率评估

### 3. ai_suggestion - AI建议记录表

存储AI提供的会议建议。

**字段说明:**
- `suggestion_id`: 建议ID(主键)
- `meeting_id`: 会议ID
- `suggestion_type`: 建议类型
- `suggestions`: 建议内容(JSON格式)
- `create_time`: 创建时间

**用途:**
- 记录AI建议历史
- 分析建议采纳率
- 优化建议算法

### 4. meeting_record - 会议记录表

存储完整的会议记录,包含摘要和聊天记录。

**字段说明:**
- `record_id`: 记录ID(主键)
- `meeting_id`: 会议ID(唯一索引)
- `meeting_name`: 会议名称
- `host_user_id`: 主持人ID
- `start_time`: 开始时间
- `end_time`: 结束时间
- `duration`: 时长(分钟)
- `participant_count`: 参与人数
- `summary_id`: 关联的摘要ID
- `chat_messages`: 聊天记录(JSON格式)
- `create_time`: 创建时间

**用途:**
- 完整的会议归档
- 会议回放和查询
- 合规性要求
- 数据分析

## 实体类

### AIConversation.java
```java
@Data
public class AIConversation {
    private String conversationId;
    private String meetingId;
    private String userId;
    private String userMessage;
    private String aiResponse;
    private String messageType;
    private Date createTime;
}
```

### MeetingSummary.java
```java
@Data
public class MeetingSummary {
    private String summaryId;
    private String meetingId;
    private String meetingName;
    private String summaryContent;
    private String keyPoints;        // JSON
    private String participants;     // JSON
    private Integer duration;
    private Integer messageCount;
    private String generatedBy;
    private Date createTime;
    private Date updateTime;
}
```

### AISuggestion.java
```java
@Data
public class AISuggestion {
    private String suggestionId;
    private String meetingId;
    private String suggestionType;
    private String suggestions;      // JSON
    private Date createTime;
}
```

### MeetingRecord.java
```java
@Data
public class MeetingRecord {
    private String recordId;
    private String meetingId;
    private String meetingName;
    private String hostUserId;
    private Date startTime;
    private Date endTime;
    private Integer duration;
    private Integer participantCount;
    private String summaryId;
    private String chatMessages;     // JSON
    private Date createTime;
}
```

## 数据流程

### 1. AI对话流程

```
用户发送消息
    ↓
AI处理并响应
    ↓
保存到ai_conversation表
    ↓
返回响应给用户
```

### 2. 会议摘要生成流程

```
用户请求生成摘要
    ↓
收集会议数据(成员、消息、时长)
    ↓
调用AI生成摘要
    ↓
保存到meeting_summary表
    ↓
返回摘要给用户
```

### 3. 会议结束流程

```
会议结束
    ↓
自动生成摘要
    ↓
保存到meeting_summary表
    ↓
创建meeting_record记录
    ↓
关联summary_id
    ↓
归档完成
```

## 数据查询API

### 1. 查询对话历史

```java
GET /api/ai/conversations/{meetingId}
```

返回指定会议的所有AI对话记录。

### 2. 查询会议摘要

```java
GET /api/ai/summary/{meetingId}
```

返回指定会议的摘要。

### 3. 查询会议记录

```java
GET /api/ai/record/{meetingId}
```

返回完整的会议记录。

### 4. 查询用户的所有会议记录

```java
GET /api/ai/records/user/{userId}
```

返回用户参与的所有会议记录列表。

## 数据统计分析

### 1. AI使用统计

```sql
-- 统计AI对话次数
SELECT 
    DATE(create_time) as date,
    COUNT(*) as conversation_count
FROM ai_conversation
GROUP BY DATE(create_time)
ORDER BY date DESC;

-- 统计最常见的问题类型
SELECT 
    message_type,
    COUNT(*) as count
FROM ai_conversation
GROUP BY message_type
ORDER BY count DESC;
```

### 2. 会议统计

```sql
-- 统计会议平均时长
SELECT 
    AVG(duration) as avg_duration,
    AVG(participant_count) as avg_participants,
    AVG(message_count) as avg_messages
FROM meeting_summary;

-- 统计最活跃的会议
SELECT 
    meeting_name,
    duration,
    participant_count,
    message_count
FROM meeting_summary
ORDER BY message_count DESC
LIMIT 10;
```

## 数据清理策略

### 1. 对话记录清理

保留最近3个月的对话记录,超过3个月的可以归档或删除。

```sql
-- 删除3个月前的对话记录
DELETE FROM ai_conversation
WHERE create_time < DATE_SUB(NOW(), INTERVAL 3 MONTH);
```

### 2. 会议记录归档

会议结束后30天,将详细记录归档,只保留摘要。

```sql
-- 清理30天前的详细聊天记录
UPDATE meeting_record
SET chat_messages = NULL
WHERE end_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

## 隐私和安全

### 1. 数据加密

敏感的会议内容可以加密存储:

```java
// 加密聊天记录
String encryptedMessages = AESUtil.encrypt(chatMessages);
record.setChatMessages(encryptedMessages);
```

### 2. 访问控制

只有会议参与者可以查看会议记录:

```java
// 验证用户权限
if (!isMeetingParticipant(userId, meetingId)) {
    throw new BusinessException("无权访问此会议记录");
}
```

### 3. 数据脱敏

导出数据时对敏感信息脱敏:

```java
// 脱敏用户信息
summary.setParticipants(
    participants.stream()
        .map(name -> name.substring(0, 1) + "**")
        .collect(Collectors.toList())
);
```

## 部署步骤

### 1. 执行SQL脚本

```bash
mysql -u root -p easymeeting < create-ai-tables.sql
```

### 2. 验证表创建

```sql
SHOW TABLES LIKE 'ai_%';
SHOW TABLES LIKE 'meeting_%';
```

### 3. 重启应用

```bash
mvn spring-boot:run
```

### 4. 测试数据持久化

使用test-ai-assistant.html测试,然后查询数据库:

```sql
SELECT * FROM ai_conversation ORDER BY create_time DESC LIMIT 10;
SELECT * FROM meeting_summary ORDER BY create_time DESC LIMIT 10;
```

## 论文撰写要点

### 数据持久化设计

1. **设计合理性**: 
   - 分离对话记录和摘要数据
   - 使用JSON存储复杂数据结构
   - 建立合适的索引提高查询效率

2. **数据完整性**:
   - 记录所有AI交互历史
   - 保存会议完整记录
   - 支持数据追溯和审计

3. **性能优化**:
   - 使用索引加速查询
   - JSON字段存储灵活数据
   - 定期清理历史数据

4. **安全性考虑**:
   - 访问权限控制
   - 敏感数据加密
   - 数据脱敏导出

完成!现在AI助手具有完整的数据持久化能力。
