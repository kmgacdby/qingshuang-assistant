# 清爽助手（QingShuang Assistant）

一个面向 Android 10（API 29）及以上设备、无需 Root 的实验性广告辅助工具。

## 当前原型包含
- 简单的状态说明页面。
- 可从应用内跳转到系统无障碍设置。
- 无障碍服务仅匹配明确的“跳过广告”“关闭广告”“skip ad”“close ad”等按钮文字，并有点击冷却时间。

## 尚未实现
- 本地 VPN / 广告域名过滤规则。
- 规则远程更新、白名单和过滤日志。
- 针对同域广告、图片广告或无法访问控件文字的广告的处理。

域名过滤通常需要本地 VPN，会与 hide.me、Proton VPN 等其他 VPN 应用争用 Android 的 VPN 槽位；同域广告也可能无法通过域名规则拦截。无障碍自动点击不保证能识别所有应用界面。

## 云端构建
推送到 `main` 后，GitHub Actions 会尝试构建 Debug APK。也可以在仓库的 **Actions** 页面手动运行 **Build Android APK** 工作流。成功后从该次运行的 Artifacts 下载 `qingshuang-assistant-debug-apk`。

## 安全提示
无障碍服务具有较强的界面读取和交互能力。请在确认用途和权限范围后再启用。当前版本是原型，不应宣传为完整广告拦截器。
