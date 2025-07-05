# Zalith Launcher 2
![Downloads](https://img.shields.io/github/downloads/ZalithLauncher/ZalithLauncher2/total)
[![QQ](https://img.shields.io/badge/QQ-blue)](https://qm.qq.com/q/2MVxS0B29y)
[![Sponsor](https://img.shields.io/badge/sponsor-30363D?logo=GitHub-Sponsors)](https://afdian.com/a/MovTery)

[简体中文](README_ZH_CN.md)  
[繁體中文](README_ZH_TW.md)

**Zalith Launcher 2** is a newly designed launcher for **Android devices** tailored for [Minecraft: Java Edition](https://www.minecraft.net/). The project uses [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/tree/v3_openjdk/app_pojavlauncher/src/main/jni) as its core launching engine and features a modern UI built with **Jetpack Compose** and **Material Design 3**.
We are currently building our official website [zalithlauncher.cn](https://zalithlauncher.cn)  
Additionally, we are aware that a third-party website has been set up using the name “Zalith Launcher,” appearing to be official. Please note: **this site was not created by us**. It exploits the name to display ads for profit. We **do not participate in, endorse, or trust** such content.  
Please stay vigilant and **protect your personal privacy**!  

> [!WARNING]
> This project is **completely separate** from [ZalithLauncher](https://github.com/ZalithLauncher/ZalithLauncher).  
> The project is in an early development stage. Many features are still under construction—stay tuned for updates!

## 📅 Development Progress

Here’s the current roadmap of features and development status:

### ✅ Completed Features

* [x] Core launcher framework (themes, animations, settings, etc.)
* [x] Game launching and rendering
* [x] Game version download and installation
* [x] Mod download and automatic installation
* [x] Resource pack download and automatic installation
* [x] World save download and installation
* [x] Shader pack download and automatic installation
* [x] Control support: virtual mouse pointer / physical mouse & keyboard / gesture control
* [x] Version management: list, overview, and configuration
* [x] Customizable game installation directory
* [x] Account system: Microsoft OAuth login, offline accounts, and authentication server support
* [x] Java runtime management
* [x] Content managers: UI for managing saves / resource packs

### 🛠️ In Development / Planned Features

* [ ] Full control system (custom layout editor, control profile manager, etc.)
* [ ] Game version download extensions:
    * [ ] Install OptiFine with OptiFabric and Fabric
    * [ ] Install Fabric with the Fabric API mod
    * [ ] Install Quilt with the Quilted Fabric API mod
* [ ] Modpack download and automatic installation
* [ ] Content managers: UI for managing mods / shaders
* [ ] Gamepad control support

## 🌐 Language and Translation Support

### Zalith Launcher 2 currently supports the following two languages:

* **English** (default)
* **Simplified Chinese**

These two languages are **officially maintained by the project and guaranteed for completeness**.

### Community-supported languages for Zalith Launcher 2:

We welcome the community to contribute translations for other languages!

* **Turkish** - by Star1xr
* **Traditional Chinese** - by HongyiHank
* **Russian** - by warrior-genius

### 📌 Why Only English and Simplified Chinese?

* The project is **internationally aimed**, so English is used as the default language; however, since the developer is not a native English speaker, the English content relies heavily on AI-assisted translation, which may contain minor inaccuracies.
   * If you have any objections to the English translation, please provide the line number of the translated content along with your feedback~ [`values/strings.xml`](./ZalithLauncher/src/main/res/values/strings.xml)
* The developer [@MovTery](https://github.com/MovTery) is based in China, and can guarantee the quality and completeness of the **Simplified Chinese** translation.
* Due to resource limitations, the completeness of other language translations cannot be guaranteed at this time and will depend on community contributions.

### ✍️ How to Contribute Translations?

We are using the Weblate platform to translate Zalith Launcher 2. You're welcome to join our [Weblate project](https://hosted.weblate.org/projects/zalithlauncher2) and contribute to the translations!  
Thank you to every language contributor for helping make Zalith Launcher 2 more multilingual and global!

## 👨‍💻 Developer

This project is currently being developed solely by [@MovTery](https://github.com/MovTery).
Feedback, suggestions, and issue reports are very welcome. As it's a personal project, development may take time—thank you for your patience!

## 📦 Build Instructions (For Developers)

> The following section is for developers who wish to contribute or build the project locally.

### Requirements

* Android Studio **Bumblebee** or newer
* Android SDK:
  * **Minimum API level**: 26 (Android 8.0)
  * **Target API level**: 35 (Android 14)
* JDK 11

### Build Steps

```bash
git clone git@github.com:ZalithLauncher/ZalithLauncher2.git
# Open the project in Android Studio and build
```

## 📜 License

This project is licensed under the **[GPL-3.0 license](LICENSE)**.

### Additional Terms (Pursuant to Section 7 of the GPLv3 License)

1. When distributing a modified version of this program, you must reasonably modify the program's name or version number to distinguish it from the original version. (According to [GPLv3, 7(c)](https://github.com/ZalithLauncher/ZalithLauncher2/blob/969827b/LICENSE#L372-L374))
    - Modified versions **must not include the original program name "ZalithLauncher" or its abbreviation "ZL" in their name, nor use any name that is similar enough to cause confusion with the official name**.
    - All modified versions **must clearly indicate that they are “Unofficial Modified Versions” on the program’s startup screen or main interface**.
    - The application name of the program can be modified in [gradle.properties](./ZalithLauncher/gradle.properties).

2. You must not remove the copyright notices displayed by the program. (According to [GPLv3, 7(b)](https://github.com/ZalithLauncher/ZalithLauncher2/blob/969827b/LICENSE#L368-L370))

