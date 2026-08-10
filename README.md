# 有效期打印（Android 重建版）

这是从旧 APK 功能和数据重建的标准 Android Studio 工程，用于通过蓝牙连接 Gprinter GP-M322，打印餐饮食材有效期标签。

## 当前能力

- Android 9–16（`minSdk 28` / `targetSdk 36`）
- 小米、荣耀、OPPO、vivo 等主流 Android 系统
- 161 条原有食材模板，支持分类筛选和搜索
- 冷藏、冷冻、常温有效期计算与模板修改
- 50 × 40 mm、203 DPI 标签预览与 TSPL 点阵打印
- Android 12+ 蓝牙权限和 Android 13+ 动态广播兼容
- 覆盖旧版时自动导入旧 `print.db` 数据
- 标签宽高、间隙、左右边距、操作人可在应用内设置

## 开始开发

1. 安装 Android Studio Meerkat 2024.3.1 或更高版本、JDK 17、Android SDK 36。
2. 用 Android Studio 打开本目录，等待 Gradle 同步。
3. 连接 Android 9–16 手机，运行 `app` 的 debug 变体。

命令行构建：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testReleaseUnitTest lintRelease
```

## 最常改的文件

- 内置模板：`app/src/main/assets/materials.json`
- 标签排版：`app/src/main/java/com/fjxm/print/printer/LabelRenderer.java`
- TSPL 编码：`app/src/main/java/com/fjxm/print/printer/TscCommandBuilder.java`
- 蓝牙连接：`app/src/main/java/com/fjxm/print/printer/BluetoothPrinterManager.java`
- 默认标签参数：`app/src/main/java/com/fjxm/print/model/LabelConfig.java`
- 主题与颜色：`app/src/main/res/values/`

修改 `materials.json` 只影响首次安装的新数据库；已经安装的手机应在应用内修改模板，或增加显式数据库迁移。

## Release 签名

复制 `signing.properties.example` 为 `signing.properties`，填写本机密钥路径和密码。`signing.properties`、`*.keystore`、`local.properties` 均已被 `.gitignore` 排除。

每次对外发布必须增加 `versionCode`。如果要覆盖已安装版本，必须使用与它相同的包名和签名证书。

## 硬件验收

CI 和本机构建能验证 APK、代码和点阵尺寸，但 GP-M322 的走纸方向、标签间隙、浓度仍需用真实打印机各打印 3–5 张确认。详细步骤见 `docs/测试与发布清单.md`。
