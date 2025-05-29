# Zalith Launcher 2
![Downloads](https://img.shields.io/github/downloads/ZalithLauncher/ZalithLauncher2/total)
[![QQ](https://img.shields.io/badge/QQ-blue)](https://qm.qq.com/q/2MVxS0B29y)
[![Sponsor](https://img.shields.io/badge/sponsor-30363D?logo=GitHub-Sponsors)](https://afdian.com/a/MovTery)

[English](README.md)  
[简体中文](README_ZH_CN.md)

**Zalith Launcher 2** 是一個全新設計、面向 **Android 裝置** 的 [Minecraft: Java Edition](https://www.minecraft.net/) 啟動器。專案使用 [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/tree/v3_openjdk/app_pojavlauncher/src/main/jni) 作為啟動核心，採用 **Jetpack Compose** 與 **Material Design 3** 構建現代化 UI 體驗。  
我們目前正在搭建自己的官方網站 [zalithlauncher.cn](https://zalithlauncher.cn)  
此外，我們已注意到有第三方使用「Zalith Launcher」名稱搭建了一個看似官方的網站。請注意：**該網站並非我們創建**，其透過冒用名義並植入廣告牟利。我們對此類行為**不參與、不認可、不信任**。  
請務必提高警覺，**謹防個人隱私資訊洩露**！  

> [!WARNING]
> 該專案與 [ZalithLauncher](https://github.com/ZalithLauncher/ZalithLauncher) 屬於兩個完全不同的專案  
> 專案目前仍處於早期開發階段，功能持續新增中，歡迎關注更新！





## 📅 開發進度

以下是當前功能模組的開發計畫及狀態。

### ✅ 已完成功能

* [x] 啟動器基礎框架（主題、動畫、設定等）
* [x] 啟動並渲染 Minecraft 遊戲
* [x] 遊戲版本下載與安裝
* [x] 控制方式：虛擬滑鼠指標 / 實體滑鼠 & 鍵盤控制 / 手勢控制
* [x] 版本管理：版本列表、概覽、配置功能
* [x] 自訂遊戲安裝目錄
* [x] 帳號系統：微軟 OAuth 登入、離線帳號、認證伺服器支援
* [x] Java 環境管理
* [x] 內容管理器：存檔 / 資源包 管理介面

### 🛠️ 開發中 / 計畫中功能

* [ ] 完整的控制系統（自訂控制佈局，管理控制佈局等）
* [ ] 遊戲版本下載擴展內容：
    * [ ] OptiFine 搭配 OptiFabric 與 Fabric 進行安裝
    * [ ] Fabric 搭配 Fabric API 模組進行安裝
    * [ ] Quilt 搭配 Quilted Fabric API 模組進行安裝
* [ ] 整合包下載與自動安裝
* [ ] 模組下載與自動安裝
* [ ] 資源包下載與自動安裝
* [ ] 存檔下載與安裝功能
* [ ] 光影包下載與自動安裝
* [ ] 內容管理器：模組 / 光影包 管理介面
* [ ] 手把控制支援



## 🌐 語言與翻譯支援

### Zalith Launcher 2 當前提供以下兩種語言支援：

* **英語**（預設）
* **簡體中文**

這兩種語言是專案**官方維護並確保完整性**的語言。

### Zalith Launcher 2 社群提供的語言支援：

我們歡迎社群為其他語言貢獻翻譯！

* **土耳其語** - 作者：Star1xr
* **繁體中文** - 作者：HongyiHank
* **俄語** - 作者：warrior-genius

### 📌 為什麼只保證英語與簡體中文？

* 專案是**面向全球使用者**的，因此需要提供預設的英文介面；然而，由於開發者並非母語為英語的人士，主要依靠 AI 輔助翻譯完成英文內容，可能存在輕微誤差。
   * 若對英文翻譯有異議，請反饋翻譯內容所在的行數，並給出您的意見~ [`values/strings.xml`](./ZalithLauncher/src/main/res/values/strings.xml)
* 開發者 [@MovTery](https://github.com/MovTery) 是中國開發者，能夠保證**簡體中文**翻譯的品質和完整性。
* 出於人力限制，其他語言的完整性暫時無法保證，需依賴社群貢獻。

### ✍️ 如何參與翻譯？

如果您希望專案支援您的母語，歡迎透過 Pull Request 的形式提交翻譯檔案。請按照以下方式操作：

1. **複製預設語言檔案**
   * 由於英文內容由 AI 輔助完成翻譯，所以請盡量參考描述最準確的簡體中文：
     [`values-zh-rCN/strings.xml`](./ZalithLauncher/src/main/res/values-zh-rCN/strings.xml)
2. **建立您的語言資源目錄**
   * 例如：繁體中文為 `values-zh-rTW`，法語為 `values-fr`，日語為 `values-ja` 等。
3. **翻譯內容**
   * 將 `strings.xml` 中的內容翻譯為對應語言，並保留所有 `name` 屬性不變。
4. **提交 Pull Request**
   * 請在 PR 描述中說明所新增語言，並註明翻譯方式（如「人工翻譯」）。

### ✅ 翻譯建議與注意事項

* 請勿使用機器翻譯（如 Google Translate、DeepL 等）直接生成翻譯內容。
* 保持專業用詞，遵循平台慣用表達（如 Minecraft 相關術語）
* 不要翻譯標點或鍵位指令（如「Shift」、「Ctrl」等）
* 請保持字串完整性（佔位符 `%1$s`、`\n` 等格式務必保留）
* 請注意，XML 中需要轉義特殊字元，如 `<` 需寫為 `&lt;`，`&` 需寫為`&amp;`。
   * 轉義規則詳見：[XML 官方轉義規則](https://www.w3.org/TR/xml/#syntax)

感謝每一位語言貢獻者的支持，讓 Zalith Launcher 2 更加多語、更加全球化！




## 👨‍💻 開發者

該專案目前由 [@MovTery](https://github.com/MovTery) 獨立開發，歡迎提出建議或反饋問題。由於個人精力有限，部分功能可能實現較慢，敬請諒解！




## 📦 構建方式（開發者）

> 以下內容適用於希望參與開發或自行構建應用的使用者。

### 環境要求

* Android Studio Bumblebee 以上
* Android SDK：
    * **最低 API**：26（Android 8.0）
    * **目標 API**：35（Android 14）
* JDK 11

### 構建步驟

```bash
git clone git@github.com:ZalithLauncher/ZalithLauncher2.git
# 使用 Android Studio 開啟專案並進行構建
```




## 📜 License

本專案程式碼遵循 **[GPL-3.0 license](LICENSE)** 開源協議。

### 附加條款（依據 GPLv3 開源授權條款第七條）

1. 當你分發本程式的修改版本時，必須以合理方式修改該程式的名稱或版本號，以區別於原始版本。（依據 [GPLv3, 7(c)](https://github.com/ZalithLauncher/ZalithLauncher2/blob/969827b/LICENSE#L372-L374)）
    - 修改版本 **不得在名稱中包含原程式名稱「ZalithLauncher」或其縮寫「ZL」，亦不得使用與官方名稱相近、可能造成混淆的名稱**。
    - 所有修改版本 **必須在程式啟動畫面或主介面中以明顯方式標示其為「非官方修改版」**。
    - 程式的應用名稱可於 [gradle.properties](./ZalithLauncher/gradle.properties) 中進行修改。

2. 你不得移除本程式所顯示的版權聲明。（依據 [GPLv3, 7(b)](https://github.com/ZalithLauncher/ZalithLauncher2/blob/969827b/LICENSE#L368-L370)）

