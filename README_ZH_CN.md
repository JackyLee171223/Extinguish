# Extinguish（Root 版）

[English](README.md) | 简体中文

Extinguish 可以让 Android 应用继续运行，同时真正关闭屏幕，而不是显示一层黑色遮罩。该方案适用于 LCD、OLED 等不同屏幕类型。

本项目 Fork 自 [Moderpach/Extinguish](https://github.com/Moderpach/Extinguish)，在此基础上的主要变化是：**用 Root 替代 Shizuku、适配 Android 17，并把两个快速设置磁贴合并为一个**。

> [!IMPORTANT]
> 本版本需要设备已获取 Root 权限。厂商定制系统可能修改屏幕与隐藏 API 的行为，因此部分设备上的实际效果可能不同。

## 与原版的差异

| 项目 | 上游版本 | 本分支 |
| --- | --- | --- |
| 特权启动 | Shizuku | libsu 6.0.0 RootService |
| 编译/目标 SDK | Android 15 / API 35 | Android 17 / API 37 |
| 快速设置磁贴 | 两个，需按顺序点击 | 一个磁贴完成启动和开关屏 |
| Root 状态恢复 | 不适用 | 冷启动自动检测，已授权直接进入主页 |
| 界面文案 | Shizuku 授权流程 | Root 授权流程 |

## 功能

- 真正关闭屏幕电源，而不是覆盖黑色画面。
- 提供两种息屏方案：
  - **Root | 关闭屏幕电源**：关闭显示电源，屏幕关闭时无法响应触摸。
  - **Root | 调节屏幕亮度为 -1**：支持通过触摸屏幕重新点亮。
- 悬浮按钮，以及带定时关闭的倒计时弹窗。
- 音量键控制。
- 触摸事件控制。
- 通知控制。
- 一个快速设置磁贴。
- ADB / Intent 外部控制。

### 单磁贴使用方式

1. 在快速设置中添加 **Extinguish**。
2. 首次点击时，应用会确认 Root 权限；已授权或授权成功后自动启动服务并关闭屏幕。
3. 再次点击磁贴，在开屏和关屏之间切换。
4. 不再需要单独添加“启动服务”和“控制屏幕”两个磁贴。

如果启用了悬浮按钮，还需要授予“显示在其他应用上层”权限。

## 运行要求

- Android 8.0（API 26）至 Android 17（API 37）。
- 已 Root，并允许本应用获取超级用户权限。推荐使用 Magisk。
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

## Root 启动原理

上游版本通过 Shizuku 在特权进程中运行 `DisplayControlService` 和 `EventsProviderService`。本分支将这两个服务改为 libsu 的 `RootService`：

- 主进程仍是普通 Android 应用进程。
- `RootService` 在 Root 进程中运行，通过 Binder / AIDL 与主进程通信。
- 隐藏 API 调用逻辑保留不变，只替换宿主方式。
- 冷启动时，libsu 可能暂时无法确定 Root 状态；应用会异步创建 Root Shell，确认结果后再决定进入主页还是授权页面。
- 已授权时重新打开应用会直接进入主页。

## 隐私与权限

Extinguish 不上传个人数据。Root、悬浮窗和通知权限仅用于上述功能；其中 Root 属于高权限能力，请只从可信来源安装本应用。

## 已知限制

- 部分厂商系统可能限制息屏、亮度修改或 `getevent` 读取。
- Root 权限由设备上的 Root 管理器控制；撤销授权后需要重新进入应用授权。
- 调试签名版本不能在不卸载的情况下覆盖安装另一个签名的版本。

## 许可与致谢

本项目沿用上游项目的 [GPL-3.0 许可证](LICENSE)。

- 原始项目：[Moderpach/Extinguish](https://github.com/Moderpach/Extinguish)
- Root 能力由 [libsu](https://github.com/topjohnwu/libsu) 提供（Apache-2.0）。
