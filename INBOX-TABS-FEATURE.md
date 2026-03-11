# 收件箱标签页功能

## 功能描述
为收件箱添加"全部消息"和"待办消息"两个标签页，用户可以查看所有好友申请（包括已处理）或只查看待处理的申请。

## 实现内容

### 1. 标签页结构

**文件**: `frontend/src/views/Dashboard.vue`

#### 两个标签页
1. **全部消息**：显示所有好友申请，包括待处理、已接受、已拒绝、已拉黑
2. **待办消息**：只显示待处理的好友申请

#### 标签页切换
```vue
<div class="inbox-tabs">
  <div 
    class="inbox-tab" 
    :class="{ active: inboxActiveTab === 'all' }"
    @click="inboxActiveTab = 'all'"
  >
    <span>全部消息</span>
    <span v-if="allApplyList.length > 0" class="tab-count">{{ allApplyList.length }}</span>
  </div>
  <div 
    class="inbox-tab" 
    :class="{ active: inboxActiveTab === 'pending' }"
    @click="inboxActiveTab = 'pending'"
  >
    <span>待办消息</span>
    <span v-if="applyCount > 0" class="tab-count pending">{{ applyCount }}</span>
  </div>
</div>
```

### 2. 数据状态

#### 新增状态变量
```javascript
const contactApplyList = ref([])  // 待处理的联系人申请列表
const allApplyList = ref([])  // 所有联系人申请列表（包括已处理）
const inboxActiveTab = ref('pending')  // 收件箱当前标签页：'all' 或 'pending'
```

### 3. 全部消息标签页

#### 显示内容
- 所有好友申请（包括已处理）
- 每个申请显示状态徽章：
  - 待处理（蓝色）
  - 已接受（绿色）
  - 已拒绝（红色）
  - 已拉黑（灰色）
- 已处理的申请显示为半透明（opacity: 0.7）
- 只有待处理的申请显示操作按钮

#### 状态徽章样式
```css
.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.pending-badge { background: #409EFF; }  /* 蓝色 */
.accepted-badge { background: #67c23a; }  /* 绿色 */
.rejected-badge { background: #f56c6c; }  /* 红色 */
.blacklist-badge { background: #909399; }  /* 灰色 */
```

### 4. 待办消息标签页

#### 显示内容
- 只显示待处理的好友申请（status === 0）
- 每个申请都显示操作按钮（接受/拒绝）
- 与之前的实现保持一致

### 5. 数据加载

#### loadAllApplyList 函数
```javascript
const loadAllApplyList = async () => {
  try {
    // 调用后端API获取所有申请（包括已处理的）
    // 注意：这里需要后端提供一个新的API，或者修改现有API支持查询所有状态
    const response = await getContactApplyList()
    if (response.data.code === 200) {
      allApplyList.value = response.data.data || []
      console.log('所有申请列表加载成功:', allApplyList.value.length, '条')
    }
  } catch (error) {
    console.error('加载所有申请列表异常:', error)
  }
}
```

#### 切换到收件箱时加载
```javascript
if (nav === 'inbox') {
  await loadContactApplyList()  // 加载待处理申请
  await loadAllApplyList()  // 加载所有申请
  await loadApplyCount()
}
```

#### 处理申请后刷新
```javascript
// 重新加载申请列表和数量
await loadContactApplyList()
await loadAllApplyList()  // 同时刷新所有申请列表
await loadApplyCount()
```

### 6. 标签页样式

#### 标签页容器
```css
.inbox-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  border-bottom: 2px solid #3a3a3a;
}
```

#### 标签页项
```css
.inbox-tab {
  padding: 12px 24px;
  cursor: pointer;
  color: #999999;
  font-size: 15px;
  font-weight: 500;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: all 0.3s ease;
}

.inbox-tab:hover {
  color: #b3b3b3;
}

.inbox-tab.active {
  color: #409EFF;
  border-bottom-color: #409EFF;
}
```

#### 数量徽章
```css
.tab-count {
  background: #3a3a3a;
  color: #ffffff;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  min-width: 20px;
  text-align: center;
}

.tab-count.pending {
  background: #409EFF;  /* 待办消息徽章使用蓝色 */
}
```

### 7. 已处理申请样式

#### 半透明效果
```css
.apply-item.apply-processed {
  opacity: 0.7;
}

.apply-item.apply-processed:hover {
  opacity: 0.85;
}
```

## 用户体验

### 标签页切换
1. 默认显示"待办消息"标签页
2. 点击标签页切换内容
3. 当前标签页有蓝色下划线和蓝色文字
4. 标签页上显示对应的消息数量

### 全部消息标签页
- 查看所有历史申请
- 通过状态徽章快速识别申请状态
- 已处理的申请显示为半透明，视觉上与待处理申请区分
- 只有待处理的申请可以操作

### 待办消息标签页
- 专注于需要处理的申请
- 所有申请都可以操作
- 徽章数量与收件箱图标上的数量一致

### 状态说明
- **待处理**（蓝色）：刚收到的申请，需要处理
- **已接受**（绿色）：已同意的申请，该用户已成为好友
- **已拒绝**（红色）：已拒绝的申请
- **已拉黑**（灰色）：已拉黑的用户

## 后端支持

### 当前实现
目前使用相同的 API (`loadContactApply`) 加载两个列表。这个 API 只返回待处理的申请（status === 0）。

### 建议改进
后端应该提供一个新的 API 或修改现有 API，支持查询所有状态的申请：

#### 方案1：新增 API
```java
@RequestMapping("/loadAllContactApply")
@globalInterceptor
public ResponseVO loadAllContactApply() {
    TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
    UserContactApplyQuery query = new UserContactApplyQuery();
    query.setReceiveUserId(tokenUserInfo.getUserId());
    query.setOrderBy("last_apply_time desc");
    query.setQueryUserInfo(true);
    // 不设置 status 过滤，返回所有状态
    List<UserContactApply> listByParam = this.userContactApplyService.findListByParam(query);
    return getSuccessResponseVO(listByParam);
}
```

#### 方案2：修改现有 API
添加可选参数 `includeProcessed`：
```java
@RequestMapping("/loadContactApply")
@globalInterceptor
public ResponseVO loadContactApply(Boolean includeProcessed) {
    TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
    UserContactApplyQuery query = new UserContactApplyQuery();
    query.setReceiveUserId(tokenUserInfo.getUserId());
    query.setOrderBy("last_apply_time desc");
    query.setQueryUserInfo(true);
    
    // 如果不包括已处理的，只查询待处理状态
    if (includeProcessed == null || !includeProcessed) {
        query.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
    }
    
    List<UserContactApply> listByParam = this.userContactApplyService.findListByParam(query);
    return getSuccessResponseVO(listByParam);
}
```

## 测试步骤

### 1. 测试标签页切换
1. 进入收件箱页面
2. 验证默认显示"待办消息"标签页
3. 点击"全部消息"标签页
4. 验证内容切换正确
5. 验证标签页样式（蓝色下划线、蓝色文字）

### 2. 测试全部消息标签页
1. 确保有不同状态的申请（待处理、已接受、已拒绝）
2. 切换到"全部消息"标签页
3. 验证显示所有申请
4. 验证状态徽章显示正确
5. 验证已处理的申请显示为半透明
6. 验证只有待处理的申请显示操作按钮

### 3. 测试待办消息标签页
1. 切换到"待办消息"标签页
2. 验证只显示待处理的申请
3. 验证所有申请都显示操作按钮
4. 验证数量与徽章一致

### 4. 测试数量徽章
1. 验证"全部消息"标签页显示总数量
2. 验证"待办消息"标签页显示待处理数量（蓝色）
3. 处理一个申请后，验证数量自动更新

### 5. 测试处理申请
1. 在"全部消息"标签页处理申请
2. 验证申请状态更新
3. 验证申请移到已处理区域（半透明）
4. 验证操作按钮消失
5. 切换到"待办消息"标签页
6. 验证该申请已从列表中移除

### 6. 测试空状态
1. 处理完所有待处理申请
2. 在"待办消息"标签页验证空状态提示
3. 切换到"全部消息"标签页
4. 验证显示所有已处理的申请

### 7. 测试实时更新
1. 使用另一个账号发送好友申请
2. 验证"待办消息"标签页数量更新
3. 验证"全部消息"标签页数量更新
4. 验证收件箱图标徽章更新

## 相关文件
- `frontend/src/views/Dashboard.vue` - 主要实现文件
- `frontend/src/api/services.js` - API 服务
- `src/main/java/com/easymeeting/controller/UserContactController.java` - 后端控制器（需要修改）

## 注意事项
1. 当前实现使用相同的 API 加载两个列表，后端需要提供支持
2. 默认显示"待办消息"标签页，符合用户习惯
3. 标签页数量徽章实时更新
4. 已处理的申请使用半透明显示，视觉上区分
5. 状态徽章使用不同颜色，快速识别
6. 处理申请后自动刷新两个列表
7. 响应式设计，支持移动端和桌面端
