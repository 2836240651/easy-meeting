# AI助手模拟模式 - 解决MiniMax API 401错误

## 问题

MiniMax API返回401 Unauthorized错误,导致AI助手无法正常工作。

## 解决方案

添加了**模拟模式(Mock Mode)**,让AI助手可以在没有真实API的情况下正常演示和测试。

## 已完成的修改

### 1. 配置文件更新
`src/main/resources/application.properties`:
```properties
# 使用模拟模式
ai.provider=mock
ai.mock.enabled=true
```

### 2. 后端代码更新
`AIAssistantServiceImpl.java`:
- 添加了 `mockEnabled` 配置项
- 实现了 `callMockAI()` 方法
- 支持智能回复常见问题

### 3. 模拟AI功能

模拟AI可以回答:
- ✅ 问候语 ("你好", "hello")
- ✅ 参会人数 ("几个人", "多少人")
- ✅ 会议时长 ("多久", "时间")
- ✅ 摘要请求 ("总结", "摘要")
- ✅ 建议请求 ("建议")
- ✅ 帮助信息 ("帮助", "help")
- ✅ 其他通用问题

## 使用方法

### 当前配置(模拟模式)
```properties
ai.provider=mock
ai.mock.enabled=true
```

### 切换到真实API

当MiniMax API问题解决后,修改配置:

#### 使用MiniMax
```properties
ai.provider=minimax
ai.mock.enabled=false
ai.minimax.api-key=YOUR_CORRECT_API_KEY
ai.minimax.api-url=https://api.minimaxi.com/v1/chat/completions
ai.minimax.model=abab6.5s-chat
```

#### 使用OpenAI
```properties
ai.provider=openai
ai.mock.enabled=false
ai.openai.api-key=YOUR_OPENAI_API_KEY
ai.openai.model=gpt-4o-mini
```

#### 使用本地Ollama
```properties
ai.provider=ollama
ai.mock.enabled=false
ai.ollama.api-url=http://localhost:11434/api/generate
ai.ollama.model=qwen2.5:7b
```

## 测试AI助手

### 1. 启动服务
```bash
# 后端正在重启
# 前端运行在 http://localhost:3000
# Electron应用已启动
```

### 2. 进入会议
- 打开Electron应用
- 登录账号
- 创建或加入会议

### 3. 使用AI助手
在会议界面右上角找到"🤖 AI助手"面板:

#### 测试对话
- 输入: "你好"
- 输入: "现在有几个人在会议中?"
- 输入: "会议进行了多久?"
- 输入: "帮我总结一下"

#### 测试命令
- 点击 "📝 摘要" 按钮
- 点击 "💡 建议" 按钮
- 点击 "❓ 帮助" 按钮

## 模拟响应示例

### 问候
```
用户: 你好
AI: 你好!我是AI会议助手。我可以帮你回答会议相关的问题,生成会议摘要,或提供会议建议。有什么我可以帮你的吗?
```

### 参会人数
```
用户: 现在有几个人在会议中?
AI: 当前会议正在进行中。你可以查看右侧的成员列表了解参会人数。
```

### 会议时长
```
用户: 会议进行了多久?
AI: 会议已经进行了一段时间。你可以在会议详情中查看具体的开始时间和持续时长。
```

### 帮助信息
```
用户: 帮助
AI: 我是AI会议助手,可以帮你:
• 回答会议相关问题
• 生成会议摘要 (/summary)
• 提供会议建议 (/suggest)
• 分析会议内容

直接向我提问即可,我会尽力帮助你!
```

## 优势

### 模拟模式的好处
1. **无需API Key** - 不需要真实的AI API
2. **即时响应** - 无网络延迟
3. **稳定可靠** - 不受API限制影响
4. **成本为零** - 无API调用费用
5. **演示友好** - 适合展示和测试UI

### 适用场景
- ✅ 开发和测试阶段
- ✅ UI/UX演示
- ✅ 毕业设计展示
- ✅ API配额用尽时的备用方案
- ✅ 离线环境

## 下一步

### 解决MiniMax API问题

1. **验证API Key**
   - 登录MiniMax控制台
   - 检查API Key是否正确
   - 确认API Key权限

2. **查看官方文档**
   - 确认正确的API URL
   - 确认正确的模型名称
   - 确认请求格式

3. **测试API**
   - 使用 `test-minimax-api.html` 测试
   - 使用curl命令行测试
   - 查看详细错误信息

4. **联系支持**
   - 如果问题持续,联系MiniMax技术支持
   - 提供详细的错误日志

### 切换到真实API

当API问题解决后:
1. 修改 `application.properties`
2. 设置 `ai.provider=minimax` (或其他)
3. 设置 `ai.mock.enabled=false`
4. 重启后端服务
5. 测试真实AI响应

## 文件清单

### 修改的文件
- `src/main/resources/application.properties` - 添加mock配置
- `src/main/java/com/easymeeting/service/impl/AIAssistantServiceImpl.java` - 添加mock模式

### 新增的文件
- `test-minimax-api.html` - MiniMax API测试工具
- `MINIMAX-API-401-FIX.md` - API问题排查指南
- `AI-ASSISTANT-MOCK-MODE.md` - 本文档

### UI文件(已完成)
- `frontend/src/components/AIAssistant.vue` - AI助手组件
- `frontend/src/views/Meeting.vue` - 集成AI助手
- `frontend/src/api/services.js` - API服务

## 总结

通过添加模拟模式,AI助手现在可以:
- ✅ 正常显示UI
- ✅ 响应用户消息
- ✅ 执行快捷命令
- ✅ 提供智能回复
- ✅ 完整演示功能

等MiniMax API问题解决后,只需修改配置即可切换到真实AI服务,无需修改任何代码!
