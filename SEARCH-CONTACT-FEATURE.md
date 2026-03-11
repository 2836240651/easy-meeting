# 添加联系人搜索功能实现

## 问题描述

Dashboard 页面中的"添加联系人"模态框只有 UI，没有实现搜索功能和 API 调用。

## 解决方案

### 1. 前端修改

#### 1.1 修改搜索联系人模态框

**文件**: `frontend/src/views/Dashboard.vue`

**主要改动**：

1. **添加数据绑定和事件处理**：
```vue
<input 
  v-model="searchContactInput" 
  type="text" 
  placeholder="请输入用户ID或邮箱" 
  class="form-input"
  @keyup.enter="handleSearchContact"
>
<button class="btn-primary search-btn" @click="handleSearchContact">搜索</button>
```

2. **添加搜索结果显示**：
```vue
<div v-if="searchContactResult" class="search-result">
  <div class="result-item">
    <img :src="searchContactResult.avatar || getDefaultAvatar(searchContactResult.sex)" 
         alt="Avatar" 
         class="result-avatar">
    <div class="result-info">
      <h4>{{ searchContactResult.nickName }}</h4>
      <p>{{ searchContactResult.email }}</p>
      <p class="result-status">{{ getSearchResultStatus(searchContactResult) }}</p>
    </div>
    <button 
      v-if="searchContactResult.status === null"
      class="btn-primary" 
      @click="handleApplyContact(searchContactResult.userId)"
    >
      添加
    </button>
    <span v-else-if="searchContactResult.status === 0" class="status-text">已发送申请</span>
    <span v-else-if="searchContactResult.status === 1" class="status-text">已是好友</span>
  </div>
</div>
```

3. **添加搜索提示**：
```vue
<div v-if="searchContactMessage" class="search-message" :class="{ 'error': searchContactError }">
  {{ searchContactMessage }}
</div>
```

#### 1.2 添加状态变量

```javascript
const searchContactInput = ref('')        // 搜索输入
const searchContactResult = ref(null)     // 搜索结果
const searchContactMessage = ref('')      // 提示消息
const searchContactError = ref(false)     // 是否错误
```

#### 1.3 添加搜索功能函数

```javascript
// 搜索联系人
const handleSearchContact = async () => {
  if (!searchContactInput.value.trim()) {
    searchContactMessage.value = '请输入用户ID或邮箱'
    searchContactError.value = true
    return
  }
  
  try {
    searchContactMessage.value = '搜索中...'
    searchContactError.value = false
    searchContactResult.value = null
    
    // 判断输入的是邮箱还是用户ID
    const input = searchContactInput.value.trim()
    const isEmail = input.includes('@')
    
    const response = await searchContact(isEmail ? null : input, isEmail ? input : null)
    
    if (response.data.code === 200) {
      searchContactResult.value = response.data.data
      searchContactMessage.value = ''
    } else {
      searchContactMessage.value = response.data.info || '未找到该用户'
      searchContactError.value = true
      searchContactResult.value = null
    }
  } catch (error) {
    searchContactMessage.value = '搜索失败，请稍后重试'
    searchContactError.value = true
    searchContactResult.value = null
  }
}
```

#### 1.4 添加申请联系人函数

```javascript
// 申请添加联系人
const handleApplyContact = async (receiveUserId) => {
  try {
    const response = await applyContact(receiveUserId)
    
    if (response.data.code === 200) {
      const status = response.data.data
      
      if (status === 0) {
        searchContactMessage.value = '申请已发送，等待对方同意'
        searchContactError.value = false
        if (searchContactResult.value) {
          searchContactResult.value.status = 0
        }
      } else if (status === 1) {
        searchContactMessage.value = '已经是好友了'
        searchContactError.value = false
        if (searchContactResult.value) {
          searchContactResult.value.status = 1
        }
      }
    } else {
      searchContactMessage.value = response.data.info || '申请失败'
      searchContactError.value = true
    }
  } catch (error) {
    searchContactMessage.value = '申请失败，请稍后重试'
    searchContactError.value = true
  }
}
```

#### 1.5 添加辅助函数

```javascript
// 获取搜索结果状态文本
const getSearchResultStatus = (result) => {
  if (result.status === null) {
    return '可以添加'
  } else if (result.status === 0) {
    return '已发送申请'
  } else if (result.status === 1) {
    return '已是好友'
  }
  return ''
}

// 关闭搜索联系人模态框
const closeSearchContactModal = () => {
  showSearchContactModal.value = false
  searchContactInput.value = ''
  searchContactResult.value = null
  searchContactMessage.value = ''
  searchContactError.value = false
}
```

#### 1.6 添加样式

```css
/* 搜索结果 */
.search-result {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--secondary-bg);
  border-radius: 8px;
}

.result-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}

.result-info {
  flex: 1;
}

.result-info h4 {
  margin: 0 0 4px 0;
  font-size: 16px;
  color: var(--text-primary);
}

.result-info p {
  margin: 2px 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.result-status {
  font-size: 12px;
  color: var(--primary-color);
}

.status-text {
  font-size: 14px;
  color: var(--text-secondary);
  padding: 6px 12px;
  background: var(--secondary-bg);
  border-radius: 4px;
}

.search-message {
  margin-top: 12px;
  padding: 12px;
  border-radius: 4px;
  text-align: center;
  background: var(--secondary-bg);
  color: var(--text-secondary);
}

.search-message.error {
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
}
```

### 2. API 修改

#### 2.1 修改 searchContact API

**文件**: `frontend/src/api/services.js`

**修改前**：
```javascript
searchContact: (userId) => {
  return api.get('/userContact/searchContact', { params: { userId } })
}
```

**修改后**：
```javascript
searchContact: (userId, email) => {
  const params = {}
  if (userId) params.userId = userId
  if (email) params.email = email
  return api.get('/userContact/searchContact', { params })
}
```

## 功能说明

### 搜索流程

1. 用户在输入框中输入用户ID或邮箱
2. 点击"搜索"按钮或按回车键
3. 前端判断输入内容是邮箱还是用户ID（包含 @ 符号为邮箱）
4. 调用后端 API 进行搜索
5. 显示搜索结果

### 搜索结果状态

搜索结果会显示以下三种状态之一：

1. **可以添加** (`status === null`)：
   - 显示"添加"按钮
   - 点击后发送好友申请

2. **已发送申请** (`status === 0`)：
   - 显示"已发送申请"文本
   - 不可再次发送申请

3. **已是好友** (`status === 1`)：
   - 显示"已是好友"文本
   - 不可再次添加

### 申请流程

1. 用户点击"添加"按钮
2. 调用 `applyContact` API 发送好友申请
3. 后端返回状态：
   - `0`：申请已发送
   - `1`：已经是好友
4. 更新搜索结果中的状态
5. 显示相应的提示消息

## 后端 API

### 搜索联系人

**接口**: `GET /userContact/searchContact`

**参数**:
- `userId` (可选): 用户ID
- `email` (可选): 邮箱地址
- 注意：userId 和 email 至少要提供一个

**返回**: `UserInfoVo4Search` 对象
```json
{
  "code": 200,
  "data": {
    "userId": "U001",
    "nickName": "张三",
    "email": "zhangsan@example.com",
    "sex": 1,
    "avatar": "http://...",
    "status": null  // null: 可添加, 0: 已申请, 1: 已是好友
  }
}
```

### 发送好友申请

**接口**: `POST /userContact/contactApply`

**参数**:
- `receiveUserId`: 接收者用户ID

**返回**: 状态码
- `0`: 申请已发送
- `1`: 已经是好友

## 用户体验优化

1. **输入验证**：
   - 空输入时提示用户
   - 自动判断输入类型（邮箱/用户ID）

2. **实时反馈**：
   - 搜索中显示"搜索中..."
   - 成功/失败显示相应提示
   - 错误消息用红色高亮

3. **状态管理**：
   - 申请后立即更新按钮状态
   - 避免重复申请

4. **快捷操作**：
   - 支持回车键搜索
   - 关闭模态框时清空所有状态

## 测试建议

### 测试场景 1：搜索用户ID

1. 输入有效的用户ID
2. 点击搜索
3. 验证是否显示正确的用户信息

### 测试场景 2：搜索邮箱

1. 输入有效的邮箱地址
2. 点击搜索
3. 验证是否显示正确的用户信息

### 测试场景 3：搜索不存在的用户

1. 输入不存在的用户ID或邮箱
2. 点击搜索
3. 验证是否显示"未找到该用户"

### 测试场景 4：添加联系人

1. 搜索到一个可添加的用户
2. 点击"添加"按钮
3. 验证是否显示"申请已发送"
4. 验证按钮是否变为"已发送申请"状态

### 测试场景 5：重复添加

1. 搜索已经是好友的用户
2. 验证是否显示"已是好友"状态
3. 验证是否没有"添加"按钮

## 文件修改清单

- `frontend/src/views/Dashboard.vue` - 添加搜索功能和 UI
- `frontend/src/api/services.js` - 修改 searchContact API
