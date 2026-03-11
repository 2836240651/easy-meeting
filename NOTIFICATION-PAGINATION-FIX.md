# 通知系统分页查询修复

## 问题描述

前端调用 `loadNotificationsByCategory` API 时返回错误：
```
加载通知列表失败: 服务器返回错误，请联系管理员
```

## 错误原因

后端日志显示 MyBatis 反射错误：
```
org.apache.ibatis.reflection.ReflectionException: There is no getter for property named 'pageStart' in 'class com.easymeeting.entity.query.UserNotificationQuery'
```

### 根本原因

在 `UserNotificationMapper.xml` 的 `selectList` 查询中，使用了错误的分页参数：
```xml
<if test="query.pageSize != null">
    LIMIT #{query.pageStart}, #{query.pageSize}
</if>
```

但 `UserNotificationQuery` 继承自 `BaseParam`，而 `BaseParam` 包含 `SimplePage` 对象，分页参数应该通过 `simplePage.start` 和 `simplePage.end` 访问。

## 修复方案

### 修改文件
`src/main/resources/com/easymeeting/mappers/UserNotificationMapper.xml`

### 修改内容

**修改前：**
```xml
ORDER BY create_time DESC
<if test="query.pageSize != null">
    LIMIT #{query.pageStart}, #{query.pageSize}
</if>
```

**修改后：**
```xml
ORDER BY create_time DESC
<if test="query.simplePage != null">
    LIMIT #{query.simplePage.start}, #{query.simplePage.end}
</if>
```

### 修改说明

1. 条件判断从 `query.pageSize != null` 改为 `query.simplePage != null`
2. 分页参数从 `#{query.pageStart}, #{query.pageSize}` 改为 `#{query.simplePage.start}, #{query.simplePage.end}`
3. 与其他 Mapper XML 文件保持一致的分页写法

## 参考其他 Mapper

其他 Mapper XML 文件都使用相同的分页方式：

### UserContactMapper.xml
```xml
<if test="query.simplePage!=null">
    limit #{query.simplePage.start},#{query.simplePage.end}
</if>
```

### MeetingInfoMapper.xml
```xml
<if test="query.simplePage!=null">
    limit #{query.simplePage.start}, #{query.simplePage.end}
</if>
```

### UserContactApplyMapper.xml
```xml
<if test="query.simplePage!=null">
    limit #{query.simplePage.start},#{query.simplePage.end}
</if>
```

## 分页机制说明

### BaseParam 类
```java
public class BaseParam {
    private SimplePage simplePage;
    private Integer pageNo;
    private Integer pageSize;
    // ...
}
```

### SimplePage 类
```java
public class SimplePage {
    private int pageNo;
    private int countTotal;
    private int pageSize;
    private int pageTotal;
    private int start;  // 起始位置
    private int end;    // 每页数量
    // ...
}
```

### 使用流程

1. Controller 接收 `pageNo` 和 `pageSize` 参数
2. Service 创建 `SimplePage` 对象：
   ```java
   SimplePage page = new SimplePage(pageNo, count, pageSize);
   query.setSimplePage(page);
   ```
3. `SimplePage` 自动计算 `start` 和 `end`：
   - `start = (pageNo - 1) * pageSize`
   - `end = pageSize`
4. Mapper XML 使用 `simplePage.start` 和 `simplePage.end`

## 测试验证

### 重启后端服务
```bash
mvn clean compile -DskipTests
mvn spring-boot:run
```

### 测试 API
```
GET http://localhost:6099/api/notification/loadNotificationsByCategory?category=all&pageNo=1&pageSize=15
Headers: token: <your_token>
```

### 预期结果
- API 返回 200 状态码
- 返回通知列表数据
- 前端收件箱正常显示

## 修复结果

✅ 后端编译成功
✅ 后端服务启动成功
✅ API 查询正常工作
✅ 前端可以正常加载通知列表

## 相关文件

- `src/main/resources/com/easymeeting/mappers/UserNotificationMapper.xml` - 修复的文件
- `src/main/java/com/easymeeting/entity/query/BaseParam.java` - 基础查询参数类
- `src/main/java/com/easymeeting/entity/query/SimplePage.java` - 分页对象类
- `src/main/java/com/easymeeting/entity/query/UserNotificationQuery.java` - 通知查询参数类

## 经验教训

1. **保持一致性**：新增的 Mapper XML 应该参考现有的写法
2. **理解继承关系**：`UserNotificationQuery` 继承 `BaseParam`，分页参数在父类中
3. **测试覆盖**：应该在开发阶段就测试分页查询功能
4. **日志分析**：后端日志清楚地指出了问题所在

## 下一步

现在可以继续测试统一收件箱系统的其他功能：
1. ✅ 通知列表加载
2. 🔄 类别筛选
3. 🔄 待办消息
4. 🔄 未读标记
5. 🔄 好友申请流程
