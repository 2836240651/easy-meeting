# 搜索联系人头像显示修复

## 问题描述

搜索联系人后，返回的数据中没有 `avatar`、`email` 和 `sex` 字段，导致前端无法显示用户头像，只能显示默认头像。

**后端返回的数据**：
```json
{
  "userId": "6cq7Pg48b4Rq",
  "nickName": "iron",
  "status": 1
}
```

**期望返回的数据**：
```json
{
  "userId": "6cq7Pg48b4Rq",
  "nickName": "iron",
  "email": "iron@example.com",
  "sex": 1,
  "avatar": "http://localhost:9000/easymeeting/avatar/xxx.jpg",
  "status": 1
}
```

## 解决方案

### 1. 修改 UserInfoVo4Search 类

**文件**: `src/main/java/com/easymeeting/entity/vo/UserInfoVo4Search.java`

添加缺失的字段：

```java
public class UserInfoVo4Search {
    private String userId;
    private String nickName;
    private String email;      // 新增
    private Integer sex;       // 新增
    private String avatar;     // 新增
    private Integer status;
    
    // ... getter 和 setter 方法
}
```

### 2. 修改 searchContact 方法

**文件**: `src/main/java/com/easymeeting/service/impl/UserContactServiceImpl.java`

在创建 `UserInfoVo4Search` 对象时设置新增的字段：

```java
@Override
public UserInfoVo4Search searchContact(String userId, String email, String myUserId) {
    UserInfo userInfo = null;
    
    // 根据参数选择查询方式
    if (userId != null && !userId.trim().isEmpty()) {
        userInfo = userInfoMapper.selectByUserId(userId);
    } else if (email != null && !email.trim().isEmpty()) {
        userInfo = userInfoMapper.selectByEmail(email);
    }
    
    if (userInfo == null) {
        return null;
    }
    
    UserInfoVo4Search userInfoVo4Search = new UserInfoVo4Search();
    userInfoVo4Search.setUserId(userInfo.getUserId());
    userInfoVo4Search.setNickName(userInfo.getNickName());
    userInfoVo4Search.setEmail(userInfo.getEmail());       // 新增
    userInfoVo4Search.setSex(userInfo.getSex());           // 新增
    userInfoVo4Search.setAvatar(userInfo.getAvatar());     // 新增
    
    // ... 其他逻辑
    
    return userInfoVo4Search;
}
```

### 3. 前端添加头像处理函数

**文件**: `frontend/src/views/Dashboard.vue`

添加专门处理搜索结果头像的函数：

```javascript
// 获取搜索结果用户头像
const getSearchContactAvatar = (user) => {
  // 如果有avatar字段，直接使用
  if (user.avatar) {
    return user.avatar
  }
  // 否则使用默认头像
  return getDefaultAvatar(user.sex)
}
```

修改搜索结果模板：

```vue
<img :src="getSearchContactAvatar(searchContactResult)" alt="Avatar" class="result-avatar">
```

## 重新编译和测试

### 1. 重新编译后端

```bash
# 在项目根目录执行
mvn clean compile

# 或者使用 IDEA 的 Build -> Rebuild Project
```

### 2. 重启后端服务

停止当前运行的后端服务，然后重新启动。

### 3. 测试 API

打开 `test-search-contact-api.html` 文件进行测试：

1. 在浏览器中打开该文件
2. 确保已登录（token 会自动从 localStorage 读取）
3. 选择搜索方式（用户ID 或 邮箱）
4. 输入搜索内容
5. 点击"搜索联系人"按钮
6. 查看返回的数据是否包含所有字段

**预期结果**：

- ✅ 返回数据包含 `email` 字段
- ✅ 返回数据包含 `sex` 字段
- ✅ 返回数据包含 `avatar` 字段（如果用户有头像）
- ✅ 前端正确显示用户头像

### 4. 前端测试

1. 登录系统
2. 点击"添加联系人"
3. 输入用户ID或邮箱进行搜索
4. 验证是否显示正确的用户头像

## 字段说明

### email (String)
- 用户的邮箱地址
- 用于显示用户的联系方式

### sex (Integer)
- 用户性别
- 0: 女
- 1: 男
- 2: 保密
- 用于在没有头像时显示默认头像

### avatar (String)
- 用户头像的完整 URL
- 格式：`http://localhost:9000/easymeeting/avatar/xxx.jpg`
- 如果为 null 或空字符串，则使用默认头像

## 常见问题

### Q1: 修改后仍然没有返回新字段

**原因**: 后端代码没有重新编译

**解决方法**:
1. 使用 Maven 重新编译：`mvn clean compile`
2. 或在 IDEA 中：Build -> Rebuild Project
3. 重启后端服务

### Q2: 返回的 avatar 字段为 null

**原因**: 用户可能没有上传头像

**解决方法**:
- 这是正常情况
- 前端会自动使用默认头像（根据性别）
- 确保 `sex` 字段有值

### Q3: 头像 URL 无法访问

**原因**: MinIO 服务未启动或配置错误

**解决方法**:
1. 检查 MinIO 服务是否运行
2. 检查 MinIO 配置（端口、bucket 等）
3. 确保头像文件已正确上传到 MinIO

### Q4: 前端显示默认头像而不是用户头像

**原因**: 
- 后端返回的 `avatar` 字段为 null
- 或者头像 URL 无法访问

**解决方法**:
1. 使用测试页面检查后端返回的数据
2. 检查 `avatar` 字段是否有值
3. 在浏览器中直接访问头像 URL，确认是否可访问
4. 检查浏览器控制台是否有跨域错误

## 文件修改清单

### 后端文件
- `src/main/java/com/easymeeting/entity/vo/UserInfoVo4Search.java` - 添加字段
- `src/main/java/com/easymeeting/service/impl/UserContactServiceImpl.java` - 设置字段值

### 前端文件
- `frontend/src/views/Dashboard.vue` - 添加头像处理函数

### 测试文件
- `test-search-contact-api.html` - API 测试页面

## 验证步骤

1. ✅ 修改 `UserInfoVo4Search.java`，添加 `email`、`sex`、`avatar` 字段
2. ✅ 修改 `UserContactServiceImpl.java`，设置这些字段的值
3. ⏳ 重新编译后端代码
4. ⏳ 重启后端服务
5. ⏳ 使用测试页面验证 API 返回数据
6. ⏳ 在前端搜索联系人，验证头像显示

## 注意事项

1. **必须重新编译**：修改 Java 代码后必须重新编译才能生效
2. **必须重启服务**：编译后必须重启后端服务
3. **清除缓存**：如果前端仍显示旧数据，清除浏览器缓存
4. **检查 MinIO**：确保 MinIO 服务正常运行，头像 URL 可访问
