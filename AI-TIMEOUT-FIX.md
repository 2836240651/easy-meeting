# AI API超时问题修复

## 问题描述

AI助手调用阿里百炼API时,前端显示"AI助手暂时无法响应",后端日志显示:
```
[INFO] Calling Alibaba Bailian API with model: qwen-plus
```
但之后没有任何响应或错误日志。

## 问题原因

RestTemplate没有配置超时时间,导致API调用可能无限等待,最终导致请求超时但没有明确的错误信息。

## 解决方案

### 修改AIConfig.java

添加超时配置:

```java
@Bean
public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(10000); // 连接超时10秒
    factory.setReadTimeout(60000);    // 读取超时60秒
    return new RestTemplate(factory);
}
```

### 超时配置说明

- **连接超时 (Connect Timeout)**: 10秒
  - 建立TCP连接的最大等待时间
  - 如果10秒内无法连接到服务器,抛出异常

- **读取超时 (Read Timeout)**: 60秒
  - 等待服务器响应的最大时间
  - AI生成可能需要较长时间,设置为60秒

## 可能的其他问题

### 1. 网络连接问题
如果无法访问阿里云服务器:
- 检查防火墙设置
- 检查网络代理配置
- 确认可以访问 dashscope.aliyuncs.com

### 2. API Key问题
- 确认API Key正确
- 检查API Key是否有效
- 确认API配额是否充足

### 3. API URL问题
当前配置:
```
https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
```

如果不工作,可以尝试:
- `https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation`
- 查看阿里云百炼官方文档确认正确的端点

## 测试步骤

### 1. 等待后端重启
```bash
# 后端正在重启
# 等待看到 "Started EasymeetingApplication" 日志
```

### 2. 刷新Electron应用
按 Ctrl+R 刷新页面

### 3. 测试AI助手
在会议中:
- 输入"你好"
- 等待响应(最多60秒)
- 查看是否有明确的错误或成功响应

### 4. 查看后端日志
如果仍然失败,查看日志中的错误信息:
```
[ERROR] Call Alibaba Bailian failed
```

## 调试工具

### 使用测试页面
打开 `test-alibaba-api-direct.html`:
1. 点击"通过后端测试"
2. 查看响应
3. 如果有错误,会显示详细信息

### 查看详细日志
在后端日志中查找:
- `Calling Alibaba Bailian API` - API调用开始
- `Call Alibaba Bailian failed` - API调用失败
- 异常堆栈信息

## 备用方案

如果阿里百炼API持续无法使用:

### 方案1: 使用模拟模式
```properties
ai.provider=mock
ai.mock.enabled=true
```

### 方案2: 使用其他AI服务
```properties
# OpenAI
ai.provider=openai
ai.openai.api-key=YOUR_KEY

# 本地Ollama
ai.provider=ollama
```

## 预期结果

修复后,应该看到:
1. API调用在60秒内完成
2. 成功时返回AI响应
3. 失败时显示明确的错误信息
4. 不再出现无响应的情况

## 文件修改

- `src/main/java/com/easymeeting/config/AIConfig.java` - 添加超时配置
- `test-alibaba-api-direct.html` - 新增测试工具

## 下一步

1. 等待后端启动完成
2. 刷新Electron应用
3. 测试AI助手功能
4. 如果仍有问题,查看详细错误日志
