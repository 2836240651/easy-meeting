# 会议智能助手实现方案

## 功能概述

在会议聊天窗口中集成AI助手,提供以下功能:
1. 回答会议相关问题
2. 生成会议摘要
3. 提供会议建议
4. 执行会议控制命令

## 技术架构

```
前端(Vue 3)
    ↓
后端API(Spring Boot)
    ↓
AI服务层
    ↓
OpenAI API / 本地LLM(Ollama)
```

## 实现方案

### 方案选择

**推荐方案A: OpenAI API集成(快速实现)**
- 优点: 实现简单,效果好,适合演示
- 缺点: 需要API密钥,有调用成本
- 适用场景: 毕业设计演示、快速原型

**方案B: 本地Ollama集成(完全本地化)**
- 优点: 完全本地运行,无需API密钥,数据安全
- 缺点: 需要本地部署,性能依赖硬件
- 适用场景: 注重隐私,有本地GPU

**本文档采用方案A,同时提供方案B的实现代码**

## 核心功能设计

### 1. AI助手消息类型

```java
public enum AIMessageType {
    QUESTION_ANSWER,      // 问答
    MEETING_SUMMARY,      // 会议摘要
    MEETING_SUGGESTION,   // 会议建议
    COMMAND_EXECUTION     // 命令执行
}
```

### 2. 支持的命令

- `/summary` - 生成会议摘要
- `/suggest` - 获取会议建议
- `/help` - 显示帮助信息
- `/end` - 结束会议(仅主持人)
- `/mute @user` - 禁言用户(仅主持人)

### 3. AI上下文管理

AI助手需要了解:
- 会议基本信息(名称、时长、参与者)
- 聊天历史记录
- 当前会议状态
- 用户权限(是否主持人)

## 数据库设计

### AI对话记录表

```sql
CREATE TABLE ai_conversation (
    conversation_id VARCHAR(20) PRIMARY KEY,
    meeting_id VARCHAR(20) NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    user_message TEXT NOT NULL,
    ai_response TEXT NOT NULL,
    message_type VARCHAR(20),
    create_time DATETIME NOT NULL,
    INDEX idx_meeting_id (meeting_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## API接口设计

### 1. 发送消息给AI助手

```
POST /api/ai/chat
Request:
{
  "meetingId": "会议ID",
  "message": "用户消息"
}

Response:
{
  "code": 200,
  "data": {
    "response": "AI回复",
    "type": "QUESTION_ANSWER",
    "actions": []  // 可执行的操作
  }
}
```

### 2. 生成会议摘要

```
POST /api/ai/summary
Request:
{
  "meetingId": "会议ID"
}

Response:
{
  "code": 200,
  "data": {
    "summary": "会议摘要内容",
    "keyPoints": ["要点1", "要点2"],
    "participants": ["参与者统计"],
    "duration": "会议时长"
  }
}
```

### 3. 获取会议建议

```
POST /api/ai/suggest
Request:
{
  "meetingId": "会议ID"
}

Response:
{
  "code": 200,
  "data": {
    "suggestions": [
      "建议1: 控制发言时间",
      "建议2: 总结讨论要点"
    ]
  }
}
```

## 前端UI设计

### 聊天窗口增强

```
┌─────────────────────────────────┐
│  会议聊天                        │
├─────────────────────────────────┤
│  👤 张三: 大家好                 │
│  👤 李四: 今天讨论什么?          │
│  🤖 AI助手: 您好!我可以帮您...   │
│  👤 张三: /summary               │
│  🤖 AI助手: [会议摘要]           │
│     • 参与者: 3人                │
│     • 时长: 15分钟               │
│     • 讨论要点: ...              │
├─────────────────────────────────┤
│  [输入消息...]  [@AI] [发送]     │
└─────────────────────────────────┘
```

### AI助手标识

- 使用特殊图标(🤖)标识AI消息
- AI消息使用不同的背景色
- 支持@AI快速唤醒助手

## 实现步骤

### 阶段1: 后端基础(1-2天)
1. 添加OpenAI依赖
2. 创建AI服务层
3. 实现基础问答功能
4. 创建API接口

### 阶段2: 前端集成(1天)
1. 修改聊天组件
2. 添加AI消息显示
3. 实现@AI功能
4. 添加命令快捷按钮

### 阶段3: 高级功能(1-2天)
1. 实现会议摘要生成
2. 实现智能建议
3. 实现命令执行
4. 添加上下文管理

### 阶段4: 优化测试(1天)
1. 性能优化
2. 错误处理
3. 用户体验优化
4. 功能测试

## 成本估算

### OpenAI API成本
- GPT-3.5-turbo: $0.002/1K tokens
- 平均每次对话: ~500 tokens = $0.001
- 100次对话成本: ~$0.1

### 本地Ollama成本
- 硬件要求: 8GB+ RAM, 推荐GPU
- 完全免费,无调用限制

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
   • 会议名称: 项目讨论会
   • 参与者: 3人
   • 时长: 25分钟
   • 讨论要点:
     1. 确定了项目时间表
     2. 分配了各自的任务
     3. 约定下次会议时间
```

### 场景3: 智能建议
```
用户: /suggest
AI: 💡 会议建议
   • 建议记录本次会议的决策事项
   • 建议为每个任务设置负责人
   • 会议已进行30分钟,建议适当休息
```

### 场景4: 命令执行
```
用户: /end
AI: ✅ 会议即将结束,正在生成会议记录...
   [会议已结束]
```

## 论文撰写要点

### 技术创新点
1. 将AI技术与实时通信系统深度集成
2. 实现了智能会议助手,提升会议效率
3. 支持自然语言交互和命令执行

### 实现难点
1. AI上下文管理: 如何让AI理解会议状态
2. 实时性保证: AI响应速度优化
3. 命令解析: 自然语言到系统命令的转换

### 测试指标
1. AI响应时间: < 3秒
2. 摘要准确率: > 85%
3. 命令识别率: > 90%

## 下一步行动

1. 选择AI方案(OpenAI或Ollama)
2. 添加后端依赖和配置
3. 实现AI服务层
4. 创建API接口
5. 修改前端聊天组件
6. 测试和优化

准备好开始实现了吗?
