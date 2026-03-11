# Report 4: AI 在线联调

## 1. 执行结果（本机实测）

### 1.1 环境探测
- MySQL(3306): 在线
- Redis(6379): 在线
- Ollama(11434): 在线
- Spring Boot(6099): 已启动（本次由我拉起）

### 1.2 核心链路测试结果

#### A. 命令模式（不依赖模型推理）
- `POST /ai/chat` message=`/help`：通过（200）
- `POST /ai/chat` message=`/end`：通过（200，返回 `END_MEETING` action）

#### B. 模型模式（依赖模型推理）
- `POST /ai/chat` 普通问题：失败（600）
- `POST /ai/summary`：失败（600）
- `POST /ai/suggest`：失败（600）
- `GET /ai/test`：接口可达（200），但 `data.success=false`

失败原因（实测返回）：
- `AI 服务调用失败: 500 Internal Server Error: {"error":"memory layout cannot be allocated"}`

结论：
- 你的 AI 业务链路代码已通（鉴权、会议上下文、命令流、控制流都能跑）。
- 当前阻塞点在模型运行资源（Ollama 当前模型无法完成推理分配）。

## 2. 联调脚本

已生成脚本：
- `docs/ai-online-test.ps1`

运行方式：
```powershell
cd D:\JavaPartical\easymeeting-java
powershell -ExecutionPolicy Bypass -File .\docs\ai-online-test.ps1
```

## 3. 预期结果

### 3.1 当前资源条件下（qwen3-vl:8b）
- `/help`、`/end`：成功
- 文本生成类（chat/summary/suggest/test）：可能失败（内存不足）

### 3.2 模型资源正常时
- `POST /ai/chat`：200，`data.success=true`，返回自然语言答复
- `POST /ai/summary`：200，返回 `summary/keyPoints/participants`
- `POST /ai/suggest`：200，返回 `suggestions[]`
- `GET /ai/test`：200，`data.success=true`

## 4. 排障步骤（按优先级）

1. 换轻量文本模型（推荐）
- 现有 `qwen3-vl:8b` 是视觉模型，资源消耗高且不适合纯文本会议助手。
- 建议安装并切换到纯文本小模型（示例：`qwen2.5:3b-instruct` / `qwen2.5:1.5b`）。

2. 修改配置并重启后端
- 文件：`src/main/resources/application.properties`
- 配置项：
  - `ai.provider=ollama`
  - `ai.ollama.model=<你的轻量模型名>`

3. 验证模型可用性
```powershell
Invoke-RestMethod -Method Get -Uri http://127.0.0.1:11434/api/tags
```

4. 复跑联调脚本
```powershell
powershell -ExecutionPolicy Bypass -File .\docs\ai-online-test.ps1
```

5. 若仍失败
- 查 Ollama 日志是否 OOM
- 关闭其他占显存/内存进程
- 降低模型规格

## 5. 备注

本次联调为了通过验证码，脚本通过读取后端日志中的验证码（`code:xxxx`）完成自动化。这适用于开发环境；生产环境不应输出验证码明文日志。
