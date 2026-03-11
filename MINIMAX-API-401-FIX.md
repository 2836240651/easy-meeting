# MiniMax API 401 Unauthorized 错误修复

## 问题描述

调用MiniMax API时返回 `401 Unauthorized` 错误,说明API认证失败。

## 可能的原因

### 1. API Key 格式问题
- API Key 可能不正确
- API Key 可能已过期
- API Key 可能没有权限

### 2. API URL 不正确
MiniMax可能使用不同的API端点:
- `https://api.minimaxi.com/v1/chat/completions` (OpenAI兼容格式)
- `https://api.minimax.chat/v1/text/chatcompletion` (MiniMax原生格式)
- 其他可能的端点

### 3. 认证头格式问题
不同的API可能需要不同的认证头:
- `Authorization: Bearer API_KEY`
- `api-key: API_KEY`
- `X-API-Key: API_KEY`

### 4. 模型名称不正确
MiniMax支持的模型:
- `abab6.5s-chat`
- `abab6.5-chat`
- `abab5.5-chat`
- 其他模型

## 解决方案

### 方案1: 验证API Key

1. 登录MiniMax控制台
2. 检查API Key是否正确
3. 确认API Key是否有效
4. 检查API Key的权限设置

### 方案2: 使用正确的API格式

根据MiniMax官方文档,可能需要使用不同的请求格式:

#### OpenAI兼容格式
```bash
curl https://api.minimaxi.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -d '{
    "model": "abab6.5s-chat",
    "messages": [
      {"role": "system", "content": "You are a helpful assistant."},
      {"role": "user", "content": "Hello"}
    ]
  }'
```

#### MiniMax原生格式
```bash
curl https://api.minimax.chat/v1/text/chatcompletion \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -d '{
    "model": "abab6.5s-chat",
    "messages": [
      {"role": "USER", "content": "Hello"}
    ],
    "tokens_to_generate": 512
  }'
```

### 方案3: 修改后端代码

如果MiniMax使用特殊的API格式,需要修改 `AIAssistantServiceImpl.java`:

```java
// 为MiniMax创建专门的方法
private String callMiniMaxNative(String context, String message) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("model", minimaxModel);
    
    // MiniMax可能使用不同的消息格式
    List<Map<String, String>> messages = new ArrayList<>();
    Map<String, String> userMessage = new HashMap<>();
    userMessage.put("sender_type", "USER");
    userMessage.put("text", message);
    messages.add(userMessage);
    
    requestBody.put("messages", messages);
    requestBody.put("tokens_to_generate", 700);
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + minimaxApiKey);
    
    // ... 其余代码
}
```

### 方案4: 切换到OpenAI API (临时方案)

如果MiniMax API持续有问题,可以临时切换到OpenAI:

```properties
# 在 application.properties 中修改
ai.provider=openai
ai.openai.api-key=YOUR_OPENAI_API_KEY
ai.openai.model=gpt-4o-mini
```

### 方案5: 使用本地Ollama (离线方案)

如果有足够的内存,可以使用本地Ollama:

```properties
ai.provider=ollama
ai.ollama.api-url=http://localhost:11434/api/generate
ai.ollama.model=qwen2.5:7b
```

注意: 需要先安装Ollama并下载模型:
```bash
# 安装Ollama
# 下载模型 (使用更小的模型避免内存问题)
ollama pull qwen2.5:7b
```

## 测试步骤

### 1. 使用测试页面
打开 `test-minimax-api.html` 进行测试:
- 检查API Key格式
- 测试直接API调用
- 测试通过后端调用

### 2. 使用curl测试
```bash
curl -X POST https://api.minimaxi.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -d '{
    "model": "abab6.5s-chat",
    "messages": [{"role": "user", "content": "你好"}]
  }'
```

### 3. 检查后端日志
查看详细的错误信息:
```bash
# 查看后端日志中的完整错误堆栈
# 特别注意API响应的详细信息
```

## 当前配置

```properties
ai.provider=minimax
ai.minimax.api-key=sk-api-yIkPCxm-xPy3cFNXriExVexD0nYBivo6YyDk7kh1WZQNFNCtnnv4I45MUTH3T2ghZLO4hHzHByTC1L4pY95eEu8cyxM5RF6DmwdP9B_yyl1qQS2vgOCsXc4
ai.minimax.api-url=https://api.minimaxi.com/v1/chat/completions
ai.minimax.model=MiniMax-M2.5
```

## 建议的下一步

1. **验证API Key**: 确认API Key是否正确且有效
2. **查看官方文档**: 访问MiniMax官方文档确认正确的API格式
3. **测试API**: 使用curl或Postman直接测试API
4. **检查配额**: 确认API账户是否有足够的配额
5. **联系支持**: 如果问题持续,联系MiniMax技术支持

## 替代方案

如果MiniMax API无法使用,可以考虑:

1. **OpenAI API** - 稳定可靠,但需要付费
2. **本地Ollama** - 免费,但需要较大内存
3. **其他AI服务** - 如Claude, Gemini等

## 参考资源

- MiniMax官方文档: https://www.minimaxi.com/document
- OpenAI API文档: https://platform.openai.com/docs
- Ollama文档: https://ollama.ai/

## 注意事项

- API Key是敏感信息,不要泄露
- 测试时注意API调用次数限制
- 生产环境建议使用环境变量存储API Key
