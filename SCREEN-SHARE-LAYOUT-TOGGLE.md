# 屏幕共享布局切换功能

## 功能概述
在屏幕共享时，用户可以通过控制栏的布局切换按钮，在不同的显示模式之间切换。

## 实现内容

### 1. 新增响应式变量
```javascript
const screenShareLayout = ref('fullscreen') // 屏幕共享布局：fullscreen, left, right, pip
```

### 2. 新增切换函数
```javascript
const toggleScreenShareLayout = () => {
  const layouts = ['fullscreen', 'left', 'right', 'pip']
  const currentIndex = layouts.indexOf(screenShareLayout.value)
  const nextIndex = (currentIndex + 1) % layouts.length
  screenShareLayout.value = layouts[nextIndex]
  
  const layoutNames = {
    fullscreen: '全屏',
    left: '左侧',
    right: '右侧',
    pip: '画中画'
  }
  console.log(`🖥️ 切换屏幕共享布局: ${layoutNames[screenShareLayout.value]}`)
}
```

### 3. 控制栏按钮
在屏幕共享控制栏中添加了布局切换按钮，位于"聊天"和"暂停共享"按钮之间：
- 图标根据当前布局动态变化：
  - 全屏：🖥️
  - 左侧：◀️
  - 右侧：▶️
  - 画中画：📱

### 4. 四种布局模式

#### 全屏模式（fullscreen）
- 屏幕共享占据整个窗口（除顶部控制栏）
- 默认模式

#### 左侧模式（left）
- 屏幕共享占据左半边窗口
- 右侧可以显示其他内容

#### 右侧模式（right）
- 屏幕共享占据右半边窗口
- 左侧可以显示其他内容

#### 画中画模式（pip）
- 屏幕共享以小窗口形式显示在右上角
- 尺寸：480x320px
- 带有蓝色边框和阴影
- 可以看到更多会议内容

### 5. 样式特性
- 所有布局切换都有平滑过渡动画（0.3s）
- 画中画模式有特殊的边框和阴影效果
- 预览标签会根据布局模式调整位置和大小

## 使用方法
1. 开始屏幕共享
2. 点击控制栏中的布局切换按钮（图标会根据当前模式变化）
3. 布局会按照：全屏 → 左侧 → 右侧 → 画中画 → 全屏 的顺序循环切换

## 技术细节
- 使用 CSS class 动态绑定实现不同布局
- 使用 CSS transition 实现平滑切换动画
- 布局状态保存在响应式变量中
- 支持实时切换，无需重新开始共享

## 文件修改