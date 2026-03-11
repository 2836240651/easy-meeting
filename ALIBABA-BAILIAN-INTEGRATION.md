# 阿里百炼(Alibaba Bailian)AI集成

## 配置完成

已成功集成阿里百炼AI服务到会议系统。

## 配置信息

### API配置
```properties
ai.provider=alibaba
ai.alibaba.api-key=sk-380e127757cb4031b4cf97962e74057b
ai.alibaba.api-url=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
ai.alibaba.model=qwen-plus
```

### 支持的模型
阿里百炼支持多个通义千问模型:
- `qwen-turbo` - 快速响应,适合简单对话
- `qwen-plus` - 平衡性能和质量(当前使用)
- `qwen-max` - 最强性能,适合复杂任务
- `qwen-max-longcontext` - 支持长文本

## API特点

### OpenAI兼容
阿里百炼提供OpenAI兼容的API接口:
- 使用标准的 `/v1/chat/completions` 端点
- 支持 `messages` 格式
- 使用 `Bearer Token` 认证
- 响应格式与OpenAI一致

### 优势
1. **国内访问快** - 服务器在国内,延迟低
2. **价格实惠** - 比OpenAI更便宜
3. **中文优化** - 通义千问对中文理解更好
4. **稳定可靠** - 阿里云基础设施支持

## 代码实现

### 后端配置
`AIAssistantServiceImpl.java`:
```java
@Value("${ai.alibaba.api-key:}")
private String alibabaApiKey;

@Value("${ai.alibaba.api-url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
private String alibabaApiUrl;

@Value("${ai.alibaba.model:qwen-plus}")
private String alibabaModel;

private String callAlibaba(String context, String message) {
    if (StringTools.isEmpty(alibabaApiKey)) {
        throw new RuntimeException("Alibaba Bailian API key is not configured");
    }
    return callOpenAICompatible(alibabaApiUrl, alibabaApiKey, alibabaModel, context, message);
}
```

### API调用流程
1. 用户在会议中向AI助手提问
2. 系统收集会议上下文(参会人员、聊天记录等)
3. 构建请求发送到阿里百炼API
4. 接收AI响应并返回给用户
5. 保存对话记录到数据库

## 使用方法

### 1. 启动服务
```bash
# 后端正在重启,配置阿里百炼API
# 前端运行在 http://localhost:3000
# Electron应用已启动
```

### 2. 测试AI助手
在会议界面中:
- 打开右上角的"🤖 AI助手"面板
- 输入问题,例如:
  - "你好"
  - "现在有几个人在会议中?"
  - "帮我总结一下会议内容"
- 点击快捷命令:
  - 📝 摘要 - 生成会议摘要
  - 💡 建议 - 获取会议建议
  - ❓ 帮助 - 查看帮助信息

### 3. 验证API工作
查看后端日志:
```
[INFO] Calling Alibaba Bailian API with model: qwen-plus
```

如果看到此日志,说明正在使用阿里百炼API。

## API请求示例

### 请求格式
```bash
curl -X POST https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sk-380e127757cb4031b4cf97962e74057b" \
  -d '{
    "model": "qwen-plus",
    "messages": [
      {
        "role": "system",
        "content": "You are a professional meeting assistant. Reply in concise Chinese."
      },
      {
        "role": "user",
        "content": "你好"
      }
    ],
    "temperature": 0.4,
    "max_tokens": 700
  }'
```

### 响应格式
```json
{
  "id": "chatcmpl-xxx",
  "object": "chat.completion",
  "created": 1234567890,
  "model": "qwen-plus",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "你好!我是AI会议助手,有什么可以帮助你的吗?"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 20,
    "completion_tokens": 15,
    "total_tokens": 35
  }
}
```

## 功能特性

### 会议上下文感知
AI助手会自动获取会议信息:
- 参会人员列表
- 最近的聊天消息
- 会议时长
- 会议状态

### 智能对话
支持多种对话场景:
- 问答 - 回答会议相关问题
- 摘要 - 生成会议纪要
- 建议 - 提供会议改进建议
- 分析 - 分析会议内容

### 命令系统
支持快捷命令:
- `/summary` - 生成会议摘要
- `/suggest` - 获取会议建议
- `/help` - 显示帮助信息

## 切换AI服务

### 切换到其他服务
修改 `application.properties`:

#### 使用OpenAI
```properties
ai.provider=openai
ai.openai.api-key=YOUR_OPENAI_API_KEY
```

#### 使用本地Ollama
```properties
ai.provider=ollama
ai.ollama.model=qwen2.5:7b
```

#### 使用模拟模式
```properties
ai.provider=mock
ai.mock.enabled=true
```

## 故障排查

### API调用失败
1. **检查API Key**
   - 确认API Key正确
   - 检查是否有足够的配额

2. **检查网络**
   - 确认可以访问 dashscope.aliyuncs.com
   - 检查防火墙设置

3. **查看日志**
   - 后端日志中查看详细错误
   - 确认请求和响应内容

### 常见错误

#### 401 Unauthorized
- API Key不正确
- API Key已过期
- 没有权限

#### 429 Too Many Requests
- 超过API调用频率限制
- 等待一段时间后重试

#### 500 Internal Server Error
- API服务暂时不可用
- 请求格式不正确

## 性能优化

### 响应时间
- qwen-turbo: ~1-2秒
- qwen-plus: ~2-3秒
- qwen-max: ~3-5秒

### 成本控制
- 使用合适的模型(qwen-plus平衡性价比)
- 限制max_tokens避免过长响应
- 缓存常见问题的答案

### 并发控制
- 限制同时API调用数量
- 使用队列处理请求
- 实现请求重试机制

## 参考资源

- [阿里云百炼平台](https://bailian.console.aliyun.com/)
- [通义千问API文档](https://help.aliyun.com/zh/dashscope/)
- [OpenAI兼容接口文档](https://help.aliyun.com/zh/dashscope/developer-reference/compatibility-of-openai-with-dashscope/)

## 总结

✅ 已成功集成阿里百炼AI服务
✅ 使用通义千问qwen-plus模型
✅ OpenAI兼容接口,易于使用
✅ 支持会议上下文感知
✅ 完整的对话和命令系统

现在可以在会议中使用真实的AI助手功能了!
