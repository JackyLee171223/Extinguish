# Extinguish（Root / Shizuku 双方案版）

[English](README.md) | 简体中文

Extinguish 可以让 Android 应用继续运行，同时真正关闭屏幕，而不是显示一层黑色遮罩。该方案适用于 LCD、OLED 等不同屏幕类型。

本项目 Fork 自 [Moderpach/Extinguish](https://github.com/Moderpach/Extinguish)，在此基础上的主要变化是：**同时支持 Root 和 Shizuku 两种特权方式并可随时切换、适配 Android 17，并把两个快速设置磁贴合并为一个**。

> [!IMPORTANT]
> 本版本需要设备满足以下任一条件：已获取 Root 权限（如 Magisk），或已安装并授权 Shizuku。厂商定制系统可能修改屏幕与隐藏 API 的行为，因此部分设备上的实际效果可能不同。

## 与原版的差异

| 项目 | 上游版本 | 本分支 |
| --- | --- | --- |
| 特权启动 | 仅 Shizuku | **Root 与 Shizuku 共存，可随时切换** |
| 编译/目标 SDK | Android 15 / API 35 | Android 17 / API 37 |
| 快速设置磁贴 | 两个，需按顺序点击 | 一个磁贴完成启动和开关屏 |
| 特权状态恢复 | 不适用 | 冷启动自动检测，已授权直接进入主页 |
| 界面文案 | Shizuku 授权流程 | 按所选方式显示 Root 或 Shizuku 授权流程 |

## 功能

- 真正关闭屏幕电源，而不是覆盖黑色画面。
- 提供两种息屏方案：
  - **关闭屏幕电源**：关闭显示电源，屏幕关闭时无法响应触摸。
  - **调节屏幕亮度为 -1**：支持通过触摸屏幕重新点亮。
- 悬浮按钮，以及带定时关闭的倒计时弹窗。
- 音量键控制。
- 触摸事件控制。
- 通知控制。
- 一个快速设置磁贴。
- ADB / Intent 外部控制。

## 特权方式（Root / Shizuku）

在**设置 → 工作方案**或首次启动引导中，可以选择特权方式：

- **Root**：通过 [libsu](https://github.com/topjohnwu/libsu) 以 Root 身份运行特权服务，需要设备已 Root（推荐 Magisk）。
- **Shizuku**：通过 [Shizuku](https://github.com/RikkaApps/Shizuku) 运行特权服务，支持已 Root 设备，也支持通过无线调试授权而无需修改系统。

两种方式共用同一套 AIDL 接口和可隐藏 API 调用逻辑，因此功能完全一致；切换方式后按提示完成对应授权即可，无需重装。

### 单磁贴使用方式

1. 在快速设置中添加 **Extinguish**。
2. 首次点击时，应用会按当前特权方式确认授权；已授权或授权成功后自动启动服务并关闭屏幕。
3. 服务运行期间，用音量键控制熄屏/亮屏（音量键与电源键同时按下的截图手势会被自动忽略）。
4. 再次点击磁贴会**关闭服务**（屏幕会自动恢复点亮）。
5. 磁贴相当于服务的总开关，不再需要单独的启动磁贴。
4. 不再需要单独添加“启动服务”和“控制屏幕”两个磁贴。

如果启用了悬浮按钮，还需要授予“显示在其他应用上层”权限。

## 运行要求

- Android 8.0（API 26）至 Android 17（API 37）。
- 满足以下任一条件：设备已 Root 并允许本应用获取超级用户权限；或已安装 Shizuku 并向本应用授权。
- 安装 APK 时，如果设备上已有上游版本且签名不同，需要先卸载旧版。

## 构建

需要 JDK 17，以及包含以下组件的 Android SDK：

- `platforms;android-37.0`
- `build-tools;35.0.0` 或 `build-tools;37.0.0`
- `platform-tools`

在项目根目录创建 `local.properties` 并写入 SDK 路径：

```properties
sdk.dir=/path/to/android-sdk
```

然后执行：

```bash
./gradlew :app:assembleDebugSignedDebug
```

生成的 APK 位于：

```text
app/build/outputs/apk/debugSigned/debug/app-debugSigned-debug.apk
```

该产物使用 Android 调试密钥签名，适合直接安装验证。

## 实现原理

上游版本通过 Shizuku 在特权进程中运行 `DisplayControlService` 和 `EventsProviderService`。本分支保留了两套宿主实现并统一了 AIDL 接口：

- 主进程仍是普通 Android 应用进程。
- **Root 方式**：`root-service` 模块用 libsu 的 `RootService` 在 Root 进程中运行服务。
- **Shizuku 方式**：`shizuku-service` 模块用 Shizuku 用户服务在特权进程中运行服务。
- 两个模块实现同一套 AIDL（定义在 `root-service`），主进程只依赖这一种 binder 类型，按设置选择绑定目标。
- 隐藏 API 调用逻辑在两套实现中保持一致。
- 冷启动时应用会异步确认所选特权方式的状态；已授权时重新打开应用会直接进入主页。

## 隐私与权限

Extinguish 不上传个人数据。Root/Shizuku、悬浮窗和通知权限仅用于上述功能；其中 Root/Shizuku 属于高权限能力，请只从可信来源安装本应用。

## 已知限制

- 部分厂商系统可能限制息屏、亮度修改或 `getevent` 读取。
- Root 授权由设备上的 Root 管理器控制；Shizuku 需要其服务在运行并已授权。撤销授权后需重新进入应用授权。
- 调试签名版本不能在不卸载的情况下覆盖安装另一个签名的版本。

## 许可与致谢

本项目沿用上游项目的 [GPL-3.0 许可证](LICENSE)。

- 原始项目：[Moderpach/Extinguish](https://github.com/Moderpach/Extinguish)
- Root 能力由 [libsu](https://github.com/topjohnwu/libsu) 提供（Apache-2.0）。
- Shizuku 能力由 [Shizuku-API](https://github.com/RikkaApps/Shizuku-API) 提供（MIT）。
