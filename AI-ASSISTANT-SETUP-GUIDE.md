# AI助手部署和测试指南

## 第一步: 安装Ollama

### Windows系统
1. 访问 https://ollama.com/download
2. 下载Windows安装包
3. 运行安装程序
4. 安装完成后,Ollama会自动在后台运行

### 验证安装
打开命令行,运行:
```bash
ollama --version
```

## 第二步: 下载qwen3-vl:8b模型

在命令行中运行:
```bash
ollama pull qwen3-vl:8b
```

这会下载约4.7GB的模型文件,请耐心等待。

### 验证模型
```bash
ollama list
```
应该能看到qwen3-vl:8b模型

### 测试模型
```bash
ollama run qwen3-vl:8b
```
然后输入"你好",看是否能正常回复。输入`/bye`退出。

## 第三步: 配置Spring Boot应用

### 1. 修改application.yml

在`src/main/resources/application.yml`文件中添加:

```yaml
ai:
  provider: ollama
  ollama:
    api-url: http://localhost:11434/api/generate
    model: qwen3-vl:8b
```

### 2. 添加RestTemplate Bean

在你的配置类中添加(如果还没有):

```java
@Configuration
public class AppConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### 3. 添加缺失的Service方法

需要在以下Service中添加方法:

**ChatMessageService.java**
```java
List<ChatMessage> getMessagesByMeetingId(String meetingId);
List<ChatMessage> getRecentMessages(String meetingId, int limit);
```

**MeetingMemberService.java**
```java
List<MeetingMember> getMembersByMeetingId(String meetingId);
MeetingMember getMember(String meetingId, String userId);
```

## 第四步: 启动后端服务

1. 确保MySQL和Redis正在运行
2. 确保Ollama正在运行(Windows会自动启动)
3. 启动Spring Boot应用

```bash
mvn spring-boot:run
```

## 第五步: 测试AI功能

### 测试1: 检查Ollama连接

访问: http://localhost:8080/api/ai/test

应该返回:
```json
{
  "code": 200,
  "data": {
    "response": "你好!我是会议助手...",
    "type": "QUESTION_ANSWER",
    "success": true
  }
}
```

### 测试2: 使用Postman测试聊天

**请求:**
```
POST http://localhost:8080/api/ai/chat
Headers:
  Authorization: Bearer <your-jwt-token>
  Content-Type: application/json

Body:
{
  "meetingId": "your-meeting-id",
  "message": "现在有几个人在会议中?"
}
```

**预期响应:**
```json
{
  "code": 200,
  "data": {
    "response": "当前会议有X位参与者...",
    "type": "QUESTION_ANSWER",
    "success": true
  }
}
```

### 测试3: 生成会议摘要

**请求:**
```
POST http://localhost:8080/api/ai/summary
Headers:
  Authorization: Bearer <your-jwt-token>
  Content-Type: application/json

Body:
{
  "meetingId": "your-meeting-id"
}
```

### 测试4: 获取会议建议

**请求:**
```
POST http://localhost:8080/api/ai/suggest
Headers:
  Authorization: Bearer <your-jwt-token>
  Content-Type: application/json

Body:
{
  "meetingId": "your-meeting-id"
}
```

## 第六步: 前端集成(下一步)

前端需要:
1. 在聊天组件中添加AI消息显示
2. 添加@AI功能
3. 添加命令快捷按钮(/summary, /suggest等)
4. 显示AI响应的特殊样式

## 常见问题排查

### 问题1: Ollama连接失败

**错误信息:** "AI服务调用失败: Connection refused"

**解决方案:**
1. 检查Ollama是否运行: `ollama list`
2. 检查端口11434是否被占用
3. 尝试重启Ollama服务

### 问题2: 模型响应慢

**原因:** qwen3-vl:8b是一个较大的模型

**解决方案:**
1. 首次调用会加载模型到内存,需要等待
2. 后续调用会快很多
3. 如果太慢,可以换用更小的模型如`qwen:7b`

### 问题3: 中文乱码

**解决方案:**
确保application.yml中设置了UTF-8编码:
```yaml
spring:
  http:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

### 问题4: AI响应不准确

**解决方案:**
1. 优化提示词(prompt)
2. 增加上下文信息
3. 调整temperature参数(在callOllama方法中)

## 性能优化建议

### 1. 添加响应缓存

对于相同的问题,可以缓存AI响应:
```java
@Cacheable(value = "aiResponses", key = "#meetingId + '_' + #message")
public AIMessageDto chat(String meetingId, String userId, String message) {
    // ...
}
```

### 2. 异步处理

对于耗时的操作(如生成摘要),使用异步:
```java
@Async
public CompletableFuture<AISummaryDto> generateSummaryAsync(String meetingId) {
    // ...
}
```

### 3. 限流

防止频繁调用AI:
```java
@RateLimiter(name = "aiService", fallbackMethod = "rateLimitFallback")
public AIMessageDto chat(...) {
    // ...
}
```

## 下一步开发计划

1. ✅ 后端AI服务实现
2. ⏳ 前端聊天组件集成
3. ⏳ WebSocket实时推送AI消息
4. ⏳ 会议记录自动保存
5. ⏳ 语音转文字(可选)

## 演示场景

### 场景1: 会议问答
```
用户: @AI 现在有几个人在会议中?
AI: 当前会议有3位参与者:张三、李四、王五
```

### 场景2: 生成摘要
```
用户: /summary
AI: 📊 会议摘要
   会议名称: 项目讨论会
   参与者: 张三, 李四, 王五
   时长: 25分钟
   消息数: 15条
   
   摘要:
   本次会议主要讨论了项目进度和任务分配...
   
   关键要点:
   1. 确定了项目时间表
   2. 分配了各自的任务
   3. 约定下次会议时间
```

### 场景3: 智能建议
```
用户: /suggest
AI: 💡 会议建议
   1. 建议记录本次会议的决策事项
   2. 建议为每个任务设置负责人和截止日期
   3. 会议已进行30分钟,建议适当休息
   4. 建议使用屏幕共享功能展示文档
```

## 论文撰写要点

### 创新点
1. 将大语言模型集成到实时会议系统
2. 实现智能会议助手,提升会议效率
3. 支持本地部署,保证数据安全

### 技术难点
1. AI上下文管理: 如何让AI理解会议状态
2. 实时性保证: AI响应速度优化
3. 命令解析: 自然语言到系统命令的转换

### 测试数据
- AI响应时间: 2-5秒(首次加载10-15秒)
- 摘要准确率: 通过人工评估
- 命令识别率: 测试各种命令格式

准备好测试了吗?
