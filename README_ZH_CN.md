# Zalith Launcher 2
![Downloads](https://img.shields.io/github/downloads/ZalithLauncher/ZalithLauncher2/total)
[![QQ](https://img.shields.io/badge/QQ-blue)](https://qm.qq.com/q/2MVxS0B29y)
[![Sponsor](https://img.shields.io/badge/sponsor-30363D?logo=GitHub-Sponsors)](https://afdian.com/a/MovTery)

[English](README.md)  
[繁體中文](README_ZH_TW.md)

**Zalith Launcher 2** 是一个全新设计、面向 **Android 设备** 的 [Minecraft: Java Edition](https://www.minecraft.net/) 启动器。项目使用 [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/tree/v3_openjdk/app_pojavlauncher/src/main/jni) 作为启动核心，采用 **Jetpack Compose** 与 **Material Design 3** 构建现代化 UI 体验。  
我们目前正在搭建自己的官方网站 [zalithlauncher.cn](https://zalithlauncher.cn)  
此外，我们已注意到有第三方使用“Zalith Launcher”名称搭建了一个看似官方的网站。请注意：**该网站并非我们创建**，其通过冒用名义并植入广告牟利。我们对此类行为**不参与、不认可、不信任**。  
请务必提高警惕，**谨防个人隐私信息泄露**！  

> [!WARNING]
> 该项目与 [ZalithLauncher](https://github.com/ZalithLauncher/ZalithLauncher) 属于两个完全不同的项目  
> 项目目前仍处于早期开发阶段，功能持续添加中，欢迎关注更新！





## 📅 开发进度

以下是当前功能模块的开发计划及状态。

### ✅ 已完成功能

* [x] 启动器基础框架（主题、动画、设置等）
* [x] 启动并渲染 Minecraft 游戏
* [x] 游戏版本下载与安装
* [x] 控制方式：虚拟鼠标指针 / 实体鼠标 & 键盘控制 / 手势控制
* [x] 版本管理：版本列表、概览、配置功能
* [x] 自定义游戏安装目录
* [x] 账号系统：微软 OAuth 登录、离线账号、认证服务器支持
* [x] Java 环境管理
* [x] 内容管理器：存档管理界面

### 🛠️ 开发中 / 计划中功能

* [ ] 完整的控制系统（自定义控制布局，管理控制布局等）
* [ ] 游戏版本下载扩展内容：
    * [ ] OptiFine 搭配 OptiFabric 与 Fabric 进行安装
    * [ ] Fabric 搭配 Fabric API 模组进行安装
    * [ ] Quilt 搭配 Quilted Fabric API 模组进行安装
* [ ] 整合包下载与自动安装
* [ ] 模组下载与自动安装
* [ ] 资源包下载与自动安装
* [ ] 存档下载与安装功能
* [ ] 光影包下载与自动安装
* [ ] 内容管理器：模组 / 资源包 / 光影包 管理界面
* [ ] 手柄控制支持



## 🌐 语言与翻译支持

### Zalith Launcher 2 当前提供以下两种语言支持：

* **英语**（默认）
* **简体中文**

这两种语言是项目**官方维护并确保完整性**的语言。

### Zalith Launcher 2 社区提供的语言支持：

我们欢迎社区为其他语言贡献翻译！

* **土耳其语** - 作者：Star1xr
* **繁体中文** - 作者：HongyiHank

### 📌 为什么只保证英语与简体中文？

* 项目是**面向全球用户**的，因此需要提供默认的英文界面；然而，由于开发者并非母语为英语的人士，主要依靠 AI 辅助翻译完成英文内容，可能存在轻微误差。
   * 若对英文翻译有异议，请反馈翻译内容所在的行数，并给出您的意见~ [`values/strings.xml`](./ZalithLauncher/src/main/res/values/strings.xml)
* 开发者 [@MovTery](https://github.com/MovTery) 是中国开发者，能够保证**简体中文**翻译的质量和完整性。
* 出于人力限制，其他语言的完整性暂时无法保证，需依赖社区贡献。

### ✍️ 如何参与翻译？

如果您希望项目支持您的母语，欢迎通过 Pull Request 的形式提交翻译文件。请按照以下方式操作：

1. **复制默认语言文件**
   * 由于英文内容由 AI 辅助完成翻译，所以请尽量参考描述最准确的简体中文：
     [`values-zh-rCN/strings.xml`](./ZalithLauncher/src/main/res/values-zh-rCN/strings.xml)
2. **创建您的语言资源目录**
   * 例如：繁体中文为 `values-zh-rTW`，法语为 `values-fr`，日语为 `values-ja` 等。
3. **翻译内容**
   * 将 `strings.xml` 中的内容翻译为对应语言，并保留所有 `name` 属性不变。
4. **提交 Pull Request**
   * 请在 PR 描述中说明所添加语言，并注明翻译方式（如“人工翻译”）。

### ✅ 翻译建议与注意事项

* 请勿使用机器翻译（如 Google Translate、DeepL 等）直接生成翻译内容。
* 保持专业用词，遵循平台惯用表达（如 Minecraft 相关术语）
* 不要翻译标点或键位指令（如“Shift”、“Ctrl”等）
* 请保持字符串完整性（占位符 `%1$s`、`\n` 等格式务必保留）
* 请注意，XML 中需要转义特殊字符，如 `<` 需写为 `&lt;`，`&` 需写为`&amp;`。
   * 转义规则详见：[XML 官方转义规则](https://www.w3.org/TR/xml/#syntax)

感谢每一位语言贡献者的支持，让 Zalith Launcher 2 更加多语、更加全球化！




## 👨‍💻 开发者

该项目目前由 [@MovTery](https://github.com/MovTery) 独立开发，欢迎提出建议或反馈问题。由于个人精力有限，部分功能可能实现较慢，敬请谅解！




## 📦 构建方式（开发者）

> 以下内容适用于希望参与开发或自行构建应用的用户。

### 环境要求

* Android Studio Bumblebee 以上
* Android SDK：
    * **最低 API**：26（Android 8.0）
    * **目标 API**：35（Android 14）
* JDK 11

### 构建步骤

```bash
git clone git@github.com:ZalithLauncher/ZalithLauncher2.git
# 使用 Android Studio 打开项目并进行构建
```




## 📜 License

本项目代码遵循 **[GPL-3.0 license](LICENSE)** 开源协议。
