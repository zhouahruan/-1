# 无限社区 (Wudian Community) Android App

无限社区 Android 原生客户端，基于 Jetpack Compose、Material Design 3 与 Kotlin 开发。

---

## 📱 手机安装提示“没有证书”排查与解决方案

如果您在手机上安装通过 GitHub Actions 构建的安装包时，提示 **“没有证书”**、**“未包含签名证书”** 或 **“解析安装包错误”**，请参考以下三种情况：

### 1. 最常见原因：下载的是 ZIP 压缩包，未解压直接安装
- **原因**：GitHub Actions 的 Artifacts（构建产物）会**自动将文件打包为 `.zip` 压缩包**（文件名通常为 `app-debug-apk.zip`）。如果直接点击安装或将其改名为 `.apk`，手机系统无法识别压缩包内部结构，便会提示“没有证书”或“解析错误”。
- **解决方法**：
  1. 使用手机自带的文件管理器打开下载的 `app-debug-apk.zip`。
  2. 点击**解压** / **解压到当前目录**。
  3. 点击解压出来的 **`app-debug.apk`** 文件进行安装即可。

---

### 2. 自动化构建已启用完整签名（V1 + V2 + V3）
- 现代 Android Gradle Plugin (AGP) 默认仅使用 V2 签名，而国内部分手机系统（如华为鸿蒙、小米 MIUI/HyperOS、OPPO、vivo 等）在安全检测时必须依赖传统的 V1 (JAR) 签名证书（`META-INF/*.RSA`）。
- **已修复**：CI 工作流 (`.github/workflows/build-debug.yml`) 已配置完整双重签名流水线，构建后自动执行：
  - `jarsigner` 写入 V1 传统证书
  - `apksigner` 写入 V2 + V3 签名块
  - 保证全面兼容各类安卓机型系统安装器。

---

### 3. 国内手机系统安全模式拦截（纯净模式 / 安全守护）
- **原因**：Debug 测试包使用的是开发自签名证书（`CN=Android Debug`），未经过各大手机厂商应用商店商业认证。国内手机系统会默认提示“缺少证书”、“未知风险”或“安全阻断”。
- **解决方法**：
  - **华为 / 荣耀（鸿蒙系统）**：
    - 安装弹窗中勾选“已了解此应用未在应用市场检测”，点击【继续安装】/【无视风险安装】。
    - 或在【设置】->【系统和更新】->【纯净模式】中选择退出或临时关闭增强防护。
  - **小米 / Redmi（MIUI / HyperOS）**：
    - 安装时点击右上角【设置】或忽略提示，选择【允许本次安装】/【继续安装】。
  - **OPPO / vivo**：
    - 输入锁屏密码后选择【继续安装】。

---

## 🎨 MD3 UI 扩展图形库

应用统一使用 **Material Design 3** 组件与 **Material Icons Extended 扩展图形包**，入口集中在：

```
app/src/main/java/com/example/ui/theme/Md3Graphics.kt
```

依赖已由 Compose BOM 统一管理（`gradle/libs.versions.toml` → `androidx-compose-material-icons-extended`）：

```kotlin
implementation(libs.androidx.compose.material.icons.extended)
```

### 扩展图形库能力

| 组成部分 | 说明 |
| --- | --- |
| `Md3Icons` | 语义化图标注册表，分 `Nav` / `File` / `Action` / `Status` / `Content` 五组，全应用不再各自 import 图标 |
| `md3FileBadge(fileName)` | 按文件后缀解析出「图标 + MD3 语义色 + 类型名」，支持 PDF、压缩包、代码、图片、视频、音频、文档、表格、演示文稿、安装包等 |
| `Md3IconTile` / `Md3FileIcon` | 图标底板与文件类型图标（直接传文件名即可） |
| `Md3InfoBanner` | 图标 + 文案的 MD3 提示条，替代纯 emoji 提示 |
| `Md3EmptyState` | 统一空状态（圆形图标底板 + 标题 + 描述 + 可选操作） |
| `Md3CoverPlaceholder` / `md3BrandGradient` | 品牌渐变封面占位图，缺图时不再留白 |
| `Md3TagChip` | 可带前置扩展图标的 MD3 标签胶囊 |

### 已接入的位置

- 底部主导航（首页 / 发现 / 云盘 / 搜索 / 我的）——`MainActivity.kt`
- 云盘文件夹与文件列表（按文件类型自动匹配扩展图标与配色）——`ui/components/CloudFileItem.kt`
- 发现页轻应用 / 群组提示条与空状态——`ui/screens/DiscoverScreen.kt`
- 首页帖子空状态——`ui/screens/HomeScreen.kt`

新增界面时请优先复用上述图形元素，保证整套 UI 视觉一致。

## 🛠 自动化构建 (GitHub Actions)
本项目**只构建正式包（Release APK）**：
- 推送（`git push`）到 `main` / `master`，或打 `v*` 标签时，`.github/workflows/build-release.yml` 会自动编译并上传已签名的 Release APK。
- 前往 GitHub 仓库的 **Actions** 标签页查看运行状态，在运行成功页面底部 **Artifacts** 区域下载 `app-release-apk`（下载的是 zip，请先解压再安装里面的 `.apk`）。
- 也可以手动触发：**Actions → Build Android Release APK → Run workflow**。
- Debug 工作流（`.github/workflows/build-debug.yml`）已改为**仅手动触发**，仅用于排查问题时临时构建，不再随推送自动运行。

## 🔐 Release 签名配置

如果配置了正式签名 Secrets，Release 构建会使用正式 upload keystore：

- `RELEASE_KEYSTORE_BASE64`：正式 upload keystore 的 Base64 内容
- `RELEASE_STORE_PASSWORD`：keystore 密码
- `RELEASE_KEY_ALIAS`：正式签名 key 的 alias
- `RELEASE_KEY_PASSWORD`：签名 key 密码

如果这些 Secrets 没有配置，CI 会生成一个仅用于测试的临时 Release 密钥，构建仍可成功，但会在构建日志与任务摘要中给出明确警告；该测试密钥不适用于应用商店正式发布，也不能保证后续版本签名一致。

CI 会将 keystore 仅写入 runner 的临时目录，构建完成后删除。请不要把 `.jks`、`.keystore`、Base64 密钥或密码提交到仓库。如果应用已经发布到应用商店，必须配置原有正式 upload key。
