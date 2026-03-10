# GitHub Actions 在线编译指南

## 步骤一：创建GitHub账号

1. 访问 https://github.com
2. 点击 `Sign up` 注册账号
3. 验证邮箱

## 步骤二：创建新仓库

1. 登录GitHub后，点击右上角 `+` → `New repository`
2. 填写仓库信息：
   - Repository name: `AutoScript`
   - 选择 `Private`（私有）或 `Public`（公开）
   - **不要**勾选 "Add a README file"
   - **不要**勾选 "Add .gitignore"
3. 点击 `Create repository`

## 步骤三：上传代码到GitHub

### 方法一：使用GitHub网页上传（最简单）

1. 打开你创建的仓库页面
2. 点击 `uploading an existing file`
3. 将 `AutoScript` 文件夹中的所有文件拖拽上传
4. 点击 `Commit changes`

### 方法二：使用Git命令行

```powershell
# 进入项目目录
cd C:\Users\yunduan\Desktop\项目\AutoScript

# 初始化Git仓库
git init

# 添加所有文件
git add .

# 提交
git commit -m "Initial commit"

# 添加远程仓库（替换 YOUR_USERNAME 为你的GitHub用户名）
git remote add origin https://github.com/YOUR_USERNAME/AutoScript.git

# 推送到GitHub
git branch -M main
git push -u origin main
```

## 步骤四：触发编译

上传代码后，GitHub Actions 会自动开始编译：

1. 进入你的仓库页面
2. 点击顶部的 `Actions` 标签
3. 可以看到正在运行的 `Build APK` 工作流
4. 点击进入查看编译进度

### 手动触发编译

1. 进入 `Actions` 标签
2. 选择 `Build APK` 工作流
3. 点击 `Run workflow` 按钮
4. 选择分支后点击绿色的 `Run workflow` 按钮

## 步骤五：下载APK

编译完成后，有两种方式下载APK：

### 方式一：从Artifacts下载

1. 进入 `Actions` 标签
2. 点击已完成的编译任务
3. 滚动到页面底部的 `Artifacts` 区域
4. 下载 `app-debug` 或 `app-release`

### 方式二：从Releases下载

如果编译成功，会自动创建一个Release：

1. 点击仓库右侧的 `Releases`
2. 找到最新的Release
3. 下载APK文件

## 常见问题

### Q: 编译失败怎么办？

1. 点击失败的编译任务查看错误日志
2. 常见错误：
   - 代码语法错误
   - 依赖下载失败（重新运行即可）
   - 配置文件格式错误

### Q: 如何更新代码？

1. 修改本地代码
2. 使用Git提交并推送：
   ```powershell
   git add .
   git commit -m "更新说明"
   git push
   ```
3. GitHub Actions 会自动重新编译

### Q: 编译需要多长时间？

首次编译约需 5-10 分钟，后续编译会更快（有缓存）。

## 文件说明

| 文件 | 说明 |
|------|------|
| `.github/workflows/build.yml` | GitHub Actions 配置文件 |
| `gradlew` | Linux/Mac 构建脚本 |
| `gradlew.bat` | Windows 构建脚本 |
| `.gitignore` | Git 忽略文件配置 |

---

**注意**：确保上传时包含 `.github` 文件夹（以点开头的文件夹可能被隐藏）
