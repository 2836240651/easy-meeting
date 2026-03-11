# 会议成员获取聊天消息的完整流程

## 一、前端发起请求

### 1. 用户操作触发
```javascript
// 前端 Meeting.vue
const showChat = async () => {
  await loadChatHistory()  // 打开聊天窗口时加载历史消息
  showChatModal.value = true
}

const loadChatHistory = async () => {
  const minMessageId = calculateMinMessageId(selectedTimeRange.value)
  const response = await chatService.loadMessage(null, minMessageId, 1)
  // 处理返回的消息数据...
}
```

### 2. API 调用
```javascript
// frontend/src/api/services.js
loadMessage: (maxMessageId = null, minMessageId = null, pageNo = 1) => {
  const params = { pageNo }
  if (maxMessageId) params.maxMessageId = maxMessageId
  if (minMessageId) params.minMessageId = minMessageId
  return api.get('/chat/loadMeesage', { params })
}
```

**请求参数：**
- `maxMessageId`: 最大消息ID（用于加载更早的消息）
- `minMessageId`: 最小消息ID（用于时间范围过滤）
- `pageNo`: 页码（默认1）

---

## 二、后端处理流程

### 步骤1：Controller 接收请求
**文件：** `ChatController.java`

```java
@RequestMapping("/loadMeesage")
@globalInterceptor  // 全局拦截器，验证token和权限
public ResponseVO loadMessage(Long maxMessageId, Long minMessageId, Integer pageNo){
    // 1. 从token中获取当前用户信息
    TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
    
    // 2. 构建查询条件
    MeetingChatMessageQuery query = new MeetingChatMessageQuery();
    query.setMeetingId(tokenUserInfo.getCurrentMeetingId());  // 当前会议ID
    query.setPageNo(pageNo);                                   // 页码
    query.setOrderBy("m.message_id desc");                     // 按消息ID降序
    query.setMaxMessage(maxMessageId);                         // 最大消息ID
    query.setMinMessage(minMessageId);                         // 最小消息ID
    query.setUserId(tokenUserInfo.getUserId());                // 当前用户ID（用于权限过滤）
    query.setQueryUserInfo(true);                              // 🔥 关键：查询用户头像和昵称
    
    // 3. 计算分表名称（根据会议ID进行分表）
    String tableName = TableSplitUtils.getMeetingChatMessageTable(
        tokenUserInfo.getCurrentMeetingId()
    );
    // 例如：meeting_chat_message_fqIVaXzx8L
    
    // 4. 调用Service层查询
    PaginationResultVO resultVO = meetingChatMessageService.findListByPage(
        tableName, 
        query
    );
    
    // 5. 返回结果
    return getSuccessResponseVO(resultVO);
}
```

**关键点：**
- `@globalInterceptor`：验证用户身份和权限
- `getTokenUserInfo()`：从请求头的token中解析用户信息
- `setQueryUserInfo(true)`：启用JOIN查询用户信息
- 分表策略：每个会议有独立的消息表

---

### 步骤2：Service 层处理
**文件：** `MeetingChatMessageServiceImpl.java`

```java
@Override
public PaginationResultVO<MeetingChatMessage> findListByPage(
    String tableName, 
    MeetingChatMessageQuery param
) {
    // 1. 查询总数
    int count = this.findCountByParam(tableName, param);
    
    // 2. 计算分页参数
    int pageSize = param.getPageSize() == null ? 15 : param.getPageSize();
    SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
    param.setSimplePage(page);
    
    // 3. 查询数据列表
    List<MeetingChatMessage> list = this.findListByParam(tableName, param);
    
    // 4. 封装分页结果
    PaginationResultVO<MeetingChatMessage> result = new PaginationResultVO(
        count,              // 总记录数
        page.getPageSize(), // 每页大小
        page.getPageNo(),   // 当前页码
        page.getPageTotal(),// 总页数
        list                // 数据列表
    );
    
    return result;
}

@Override
public List<MeetingChatMessage> findListByParam(
    String tableName, 
    MeetingChatMessageQuery param
) {
    // 确保表存在（如果不存在则自动创建）
    ensureTableExists(tableName);
    
    // 调用Mapper查询
    return this.meetingChatMessageMapper.selectList(tableName, param);
}
```

**关键点：**
- 自动分页处理
- 表存在性检查（动态创建分表）
- 返回分页结果对象

---

### 步骤3：Mapper 层执行 SQL
**文件：** `MeetingChatMessageMapper.xml`

```xml
<select id="selectList" resultMap="base_result_map">
    SELECT 
        m.message_id,
        m.meeting_id,
        m.message_type,
        m.message_content,
        m.send_user_id,
        
        <!-- 🔥 关键：优先使用user_info表的昵称 -->
        <if test="query.queryUserInfo != null and query.queryUserInfo">
            IFNULL(ui.nick_name, m.send_user_nick_name) as send_user_nick_name,
        </if>
        <if test="query.queryUserInfo == null or !query.queryUserInfo">
            m.send_user_nick_name,
        </if>
        
        m.send_time,
        m.receive_type,
        m.receive_user_id,
        m.file_size,
        m.file_name,
        m.file_type,
        m.file_suffix,
        m.status
        
        <!-- 🔥 关键：查询用户头像 -->
        <if test="query.queryUserInfo != null and query.queryUserInfo">
            ,ui.avatar
        </if>
        
    FROM ${tableName} m
    
    <!-- 🔥 关键：LEFT JOIN user_info 表 -->
    <if test="query.queryUserInfo != null and query.queryUserInfo">
        LEFT JOIN user_info ui ON ui.user_id = m.send_user_id
    </if>
    
    <!-- WHERE 条件 -->
    <where>
        <!-- 会议ID过滤 -->
        <if test="query.meetingId != null and query.meetingId!=''">
            and m.meeting_id = #{query.meetingId}
        </if>
        
        <!-- 权限过滤：只能看到发给所有人的消息 或 发给自己的私聊消息 -->
        <if test="query.userId!=null and query.userId!=''">
            and (m.receive_user_id = #{query.userId} and m.receive_type = 1 
                 or m.receive_type = 0)
        </if>
        
        <!-- 消息ID范围过滤 -->
        <if test="query.maxMessage != null">
            <![CDATA[ and m.message_id < #{query.maxMessage} ]]>
        </if>
        <if test="query.minMessage != null">
            <![CDATA[ and m.message_id >= #{query.minMessage} ]]>
        </if>
    </where>
    
    <!-- 排序 -->
    <if test="query.orderBy!=null">
        order by ${query.orderBy}
    </if>
    
    <!-- 分页 -->
    <if test="query.simplePage!=null">
        limit #{query.simplePage.start}, #{query.simplePage.end}
    </if>
</select>
```

**SQL 执行示例：**
```sql
SELECT 
    m.message_id,
    m.meeting_id,
    m.message_type,
    m.message_content,
    m.send_user_id,
    IFNULL(ui.nick_name, m.send_user_nick_name) as send_user_nick_name,
    m.send_time,
    m.receive_type,
    m.receive_user_id,
    m.file_size,
    m.file_name,
    m.file_type,
    m.file_suffix,
    m.status,
    ui.avatar
FROM meeting_chat_message_fqIVaXzx8L m
LEFT JOIN user_info ui ON ui.user_id = m.send_user_id
WHERE m.meeting_id = 'fqIVaXzx8L'
  AND (m.receive_user_id = '6cq7Pg48b4Rq' and m.receive_type = 1 
       or m.receive_type = 0)
ORDER BY m.message_id desc
LIMIT 0, 15
```

**关键点：**
- `IFNULL(ui.nick_name, m.send_user_nick_name)`：优先使用最新昵称，如果没有则使用消息表中保存的昵称
- `LEFT JOIN user_info`：关联用户表获取头像和昵称
- 权限过滤：只返回用户有权限看到的消息
- 动态表名：`${tableName}` 支持分表

---

### 步骤4：ResultMap 映射
**文件：** `MeetingChatMessageMapper.xml`

```xml
<resultMap id="base_result_map" type="com.easymeeting.entity.po.MeetingChatMessage">
    <result column="message_id" property="messageId" />
    <result column="meeting_id" property="meetingId" />
    <result column="message_type" property="messageType" />
    <result column="message_content" property="messageContent" />
    <result column="send_user_id" property="sendUserId" />
    <result column="send_user_nick_name" property="sendUserNickName" />
    <result column="send_time" property="sendTime" />
    <result column="receive_type" property="receiveType" />
    <result column="receive_user_id" property="receiveUserId" />
    <result column="file_size" property="fileSize" />
    <result column="file_name" property="fileName" />
    <result column="file_type" property="fileType" />
    <result column="file_suffix" property="fileSuffix" />
    <result column="status" property="status" />
    
    <!-- 🔥 关键：映射头像字段 -->
    <result column="avatar" property="avatar" />
</resultMap>
```

**映射结果：**
```java
MeetingChatMessage {
    messageId: 1234567890,
    meetingId: "fqIVaXzx8L",
    messageType: 5,
    messageContent: "大家好",
    sendUserId: "6cq7Pg48b4Rq",
    sendUserNickName: "aaa火药批发部",  // 从user_info表获取的最新昵称
    sendTime: 1708502400000,
    receiveType: 0,
    receiveUserId: "0",
    avatar: "/avatar/6cq7Pg48b4Rq.jpg"  // 从user_info表获取的头像
}
```

---

## 三、数据返回流程

### 步骤5：封装响应
**Controller 返回：**
```java
return getSuccessResponseVO(resultVO);
```

**响应结构：**
```json
{
  "code": 200,
  "info": "请求成功",
  "data": {
    "totalCount": 25,      // 总消息数
    "pageSize": 15,        // 每页大小
    "pageNo": 1,           // 当前页
    "pageTotal": 2,        // 总页数
    "list": [              // 消息列表
      {
        "messageId": 1234567890,
        "meetingId": "fqIVaXzx8L",
        "messageType": 5,
        "messageContent": "大家好",
        "sendUserId": "6cq7Pg48b4Rq",
        "sendUserNickName": "aaa火药批发部",  // ✅ 最新昵称
        "sendTime": 1708502400000,
        "receiveType": 0,
        "receiveUserId": "0",
        "avatar": "/avatar/6cq7Pg48b4Rq.jpg"  // ✅ 用户头像
      },
      // ... 更多消息
    ]
  }
}
```

---

## 四、前端处理响应

### 步骤6：前端接收并渲染
```javascript
// Meeting.vue
const loadChatHistory = async () => {
  const response = await chatService.loadMessage(null, minMessageId, 1)
  
  if (response.data && response.data.code === 200) {
    const messages = response.data.data.list
    
    // 清空现有消息
    chatMessages.value = []
    
    // 处理每条消息
    messages.reverse().forEach(msg => {
      const senderName = msg.sendUserNickName || '未知用户'  // ✅ 使用最新昵称
      const senderAvatar = msg.avatar || getMemberAvatar(msg.sendUserId)  // ✅ 使用头像
      
      addChatMessage({
        id: msg.messageId,
        sender: senderName,
        avatar: senderAvatar,
        text: msg.messageContent,
        time: formatTime(msg.sendTime),
        sendUserId: msg.sendUserId,
        isPrivate: msg.receiveType === 1,
        receiveUserId: msg.receiveUserId
      })
    })
  }
}
```

---

## 五、核心技术点总结

### 1. 分表策略
- **目的**：每个会议独立存储消息，避免单表数据过大
- **实现**：`meeting_chat_message_{meetingId}`
- **优点**：提高查询性能，便于数据管理

### 2. 权限控制
```sql
-- 只能看到：
-- 1. 发给所有人的消息 (receive_type = 0)
-- 2. 发给自己的私聊消息 (receive_type = 1 AND receive_user_id = 当前用户ID)
WHERE (m.receive_user_id = #{query.userId} and m.receive_type = 1 
       or m.receive_type = 0)
```

### 3. 用户信息关联
- **问题**：消息表中保存的昵称可能过期
- **解决**：LEFT JOIN user_info 表获取最新昵称和头像
- **优化**：使用 `IFNULL(ui.nick_name, m.send_user_nick_name)` 确保总有昵称显示

### 4. 分页查询
- **默认每页**：15条消息
- **排序**：按消息ID降序（最新消息在前）
- **时间范围**：通过 minMessageId 和 maxMessageId 控制

### 5. 性能优化
- **索引**：message_id, meeting_id, send_user_id
- **分表**：减少单表数据量
- **LEFT JOIN**：只在需要时才关联用户表
- **分页**：避免一次加载过多数据

---

## 六、完整流程图

```
前端用户操作
    ↓
点击聊天按钮 / 发送消息后刷新
    ↓
调用 chatService.loadMessage()
    ↓
发送 GET /api/chat/loadMeesage
    ↓
后端 ChatController.loadMessage()
    ├─ 验证token (@globalInterceptor)
    ├─ 获取用户信息 (getTokenUserInfo)
    ├─ 构建查询条件 (MeetingChatMessageQuery)
    ├─ 计算分表名称 (TableSplitUtils)
    └─ 调用 Service 层
        ↓
MeetingChatMessageServiceImpl.findListByPage()
    ├─ 查询总数 (findCountByParam)
    ├─ 计算分页 (SimplePage)
    ├─ 查询列表 (findListByParam)
    └─ 封装结果 (PaginationResultVO)
        ↓
MeetingChatMessageMapper.selectList()
    ├─ 动态SQL构建
    ├─ LEFT JOIN user_info (获取头像和昵称)
    ├─ WHERE 条件过滤 (会议ID、权限、时间范围)
    ├─ ORDER BY 排序
    ├─ LIMIT 分页
    └─ 执行SQL查询
        ↓
数据库返回结果
    ↓
ResultMap 映射为 Java 对象
    ↓
返回 JSON 响应
    ↓
前端接收数据
    ↓
渲染聊天消息列表
    └─ 显示头像、昵称、消息内容、时间
```

---

## 七、关键代码位置

| 层级 | 文件路径 | 主要功能 |
|------|---------|---------|
| 前端API | `frontend/src/api/services.js` | 定义HTTP请求 |
| 前端页面 | `frontend/src/views/Meeting.vue` | 消息加载和渲染 |
| Controller | `src/main/java/com/easymeeting/controller/ChatController.java` | 接收请求，参数处理 |
| Service | `src/main/java/com/easymeeting/service/impl/MeetingChatMessageServiceImpl.java` | 业务逻辑，分页处理 |
| Mapper | `src/main/resources/com/easymeeting/mappers/MeetingChatMessageMapper.xml` | SQL查询，数据映射 |
| Entity | `src/main/java/com/easymeeting/entity/po/MeetingChatMessage.java` | 消息实体类 |
| Query | `src/main/java/com/easymeeting/entity/query/MeetingChatMessageQuery.java` | 查询条件类 |

---

## 八、最新优化（本次修改）

### 优化内容
1. ✅ 添加 `queryUserInfo` 标志位，控制是否JOIN用户表
2. ✅ 使用 `IFNULL(ui.nick_name, m.send_user_nick_name)` 优先使用最新昵称
3. ✅ 查询用户头像 `ui.avatar`
4. ✅ 发送消息后立即刷新消息列表

### 优化效果
- 消息显示最新的用户昵称（即使用户修改了昵称）
- 消息显示用户头像
- 发送消息后立即看到效果
- 性能可控（只在需要时才JOIN）
