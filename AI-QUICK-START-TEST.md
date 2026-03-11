# AI助手快速测试指南

## ✅ 准备工作检查

- [x] Ollama已安装并运行
- [x] qwen3-vl:8b模型已下载
- [x] 数据库表已创建
- [x] application.properties已配置

## 🚀 启动步骤

### 1. 确认Ollama运行状态

```bash
# 检查Ollama服务
ollama list

# 应该看到:
# NAME                  ID              SIZE      MODIFIED
# qwen3-vl:8b           901cae732162    6.1 GB    30 hours ago
```

### 2. 启动后端服务

在项目根目录运行:

```bash
mvn spring-boot:run
```

或者在IDEA中直接运行主类。

等待启动完成,看到类似信息:
```
Started EasymeetingApplication in X.XXX seconds
```

### 3. 测试AI连接

打开浏览器访问:
```
http://localhost:6099/api/ai/test
```

**预期响应:**
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

如果看到这个响应,说明AI功能正常!

### 4. 使用测试页面

打开 `test-ai-assistant.html` 文件(双击或用浏览器打开)

**测试步骤:**

1. **配置区域**
   - API地址: `http://localhost:6099/api/ai`
   - 会议ID: 输入任意测试ID,如 `test123`
   - Token: 暂时可以留空(如果需要认证,需要先登录获取)

2. **测试1: 检查AI连接**
   - 点击"测试连接"按钮
   - 应该看到AI的响应

3. **测试2: AI聊天**
   - 在消息框输入: "你好"
   - 点击"发送消息"
   - 等待AI响应(首次可能需要10-15秒)

4. **测试3: 命令测试**
   - 点击"测试 /help"按钮
   - 应该看到帮助信息

## 🔍 故障排查

### 问题1: 连接被拒绝

**错误信息:** `Connection refused` 或 `Failed to fetch`

**解决方案:**
1. 确认后端已启动: `http://localhost:6099/api/ai/test`
2. 检查端口是否正确(6099)
3. 查看后端日志是否有错误

### 问题2: Ollama连接失败

**错误信息:** `AI服务调用失败: Connection refused`

**解决方案:**
```bash
# 检查Ollama是否运行
ollama list

# 如果没运行,启动Ollama
# Windows: Ollama会自动启动,检查系统托盘

# 测试Ollama API
curl http://localhost:11434/api/tags
```

### 问题3: 首次响应很慢

**原因:** 首次调用需要加载模型到内存

**解决方案:**
- 这是正常的,首次需要10-15秒
- 后续调用会快很多(2-5秒)
- 可以预热模型:
  ```bash
  ollama run qwen3-vl:8b
  # 输入: 你好
  # 等待响应后输入: /bye
  ```

### 问题4: 数据库错误

**错误信息:** `Table 'easymeeting.ai_conversation' doesn't exist`

**解决方案:**
```bash
# 重新执行SQL脚本
mysql -u root -p123456 easymeeting < create-ai-tables.sql

# 验证表是否创建
mysql -u root -p123456 -e "USE easymeeting; SHOW TABLES LIKE 'ai_%';"
```

## 📊 验证数据持久化

### 1. 发送几条测试消息

在test-ai-assistant.html中发送几条消息

### 2. 查询数据库

```sql
-- 查看对话记录
SELECT * FROM ai_conversation ORDER BY create_time DESC LIMIT 5;

-- 应该看到你刚才的对话记录
```

### 3. 测试会议摘要(需要真实会议)

如果你有一个真实的会议ID,可以测试摘要功能:

```bash
# 使用curl测试
curl -X POST http://localhost:6099/api/ai/summary \
  -H "Content-Type: application/json" \
  -d '{"meetingId":"your-meeting-id"}'
```

## 🎯 完整测试流程

### 场景1: 简单问答

1. 打开test-ai-assistant.html
2. 输入消息: "你好,请介绍一下你自己"
3. 点击发送
4. 等待AI响应
5. 查看数据库: `SELECT * FROM ai_conversation;`

### 场景2: 命令测试

1. 点击"测试 /help"按钮
2. 查看帮助信息
3. 点击"测试 /suggest"按钮
4. 查看会议建议

### 场景3: 会议摘要(需要真实会议)

1. 创建一个测试会议
2. 在会议中发送几条聊天消息
3. 使用会议ID测试摘要功能
4. 查看数据库: `SELECT * FROM meeting_summary;`

## 📝 测试检查清单

- [ ] Ollama正常运行
- [ ] 后端服务启动成功
- [ ] /api/ai/test 接口返回正常
- [ ] test-ai-assistant.html 可以打开
- [ ] AI聊天功能正常
- [ ] /help 命令正常
- [ ] 对话记录保存到数据库
- [ ] 响应时间可接受(首次10-15秒,后续2-5秒)

## 🎉 成功标志

如果你看到:
1. ✅ AI能正常响应你的问题
2. ✅ 数据库中有对话记录
3. ✅ 命令功能正常工作
4. ✅ 响应时间在可接受范围内

恭喜!AI助手功能已经成功部署! 🎊

## 📞 需要帮助?

如果遇到问题:
1. 查看后端日志
2. 查看浏览器控制台
3. 检查Ollama日志
4. 参考 AI-ASSISTANT-SETUP-GUIDE.md

## 下一步

- [ ] 集成到前端会议界面
- [ ] 添加更多AI功能
- [ ] 优化响应速度
- [ ] 完善错误处理

祝测试顺利! 🚀
