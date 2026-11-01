<p align="center"><img src="logo.png" alt="GayGram" width="112" /></p>

<h1 align="center">GayGram</h1>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-7.0%2B-3ddc84" />
  <a href="./LICENSE"><img alt="GPLv3" src="https://img.shields.io/badge/License-GPLv3-blue.svg" /></a>
</p>

<p align="center"><a href="README.md">中文</a> · <a href="README_EN.md">English</a> · <a href="README_RU.md">Русский</a> · <a href="README_TR.md">Türkçe</a> · <a href="README_KOR.md">한국어</a> · <a href="README_UR.md">اُردو</a> · <a href="README_ES.md">Español</a></p>

## 简介

**GayGram 是缺德组织做的 Android Telegram 客户端。**

基于 [Monogram](https://github.com/monogram-android/monogram) 改的——Monogram 是用 Kotlin + Compose 写的原生客户端，协议层是 Rust。我们在这个底子上加了两层东西：一层糊在控制层上的玻璃，和一套触感反馈。

它不是在官方客户端上叠一堆开关那种路子。UI 是按手机重新做的，功能是从同一个组织的 Windows 桌面版一件件搬过来的。

## 功能

| 分类 | 有什么 |
| --- | --- |
| 🫧 玻璃与触感 | 顶栏/输入栏/胶囊/菜单背后的液态玻璃；点按、长按、拨开关、列表拖到底各有触感反馈 |
| 👻 隐私增强 | 反撤回、幽灵模式（隐藏在线/已读/输入中）、消息历史 |
| ✨ 体验增强 | 本地 Premium、自动翻译、消息过滤器 |
| 🧱 底子 | 原生 Kotlin + Compose、Rust MTProto、文件夹、QR / passkey 登录 |

正在从桌面版继续搬：外观增强、字体自定义、极光渐变、壁纸模糊、生物识别锁、动态图标、桌面小组件。

## 跟别人不一样在哪

### 玻璃

顶栏、输入框、回到底部的胶囊、菜单背后，这些控制层糊了一层玻璃。背后滚的消息会透过它虚化，圆角的地方能看到一点点折射。

消息本身不加。看消息的时候你只想看消息，别让一层毛玻璃挡字。

### 触感

点按、长按、拨开关、列表拖到底，各有各的一下。列表已经顶到底了你还硬拽，会「咔」一声，像撞墙；靠惯性滑到底不响，因为那一下不是你拽的。

嫌吵在设置 → 外观里关，或者跟着系统走。

## 下载

从 [Releases](https://github.com/DaiWicked/GayGram-For-Android/releases) 下最新 APK，允许未知来源安装。第一次登录用自己的 Telegram 账号；想收推送去系统设置给通知权限。

Android 7.0 以上都能跑。

> 这是个人项目，不是 Telegram 官方应用，也不上架任何商店。用着哪里不对劲，开个 issue 说一声。

## 从源码构建

我们自己出包走 GitHub Actions。你想本地编译，需要：

- JDK 17 以上
- Android SDK（Platform 37）
- Rust + NDK（mtproto 是 Rust 写的，走 UniFFI 绑定）
- 自己的 Telegram `API_ID` / `API_HASH`
- 与仓库**同级**检出的 `telers-mtproto-impl`（Cargo 清单读的是 `../telers-mtproto-impl`，只有 `vendor` 下的 submodule 不够）

构建细节看根目录 `AGENTS.md`，本来是写给 AI 协作者的，人也能看懂。

## 缺德组织

<p align="center"><img src="zuzhi.png" alt="缺德组织" width="168" /></p>

GayGram 是**缺德组织**的作品。名字不太正经，做的事很正经。

## 许可

按 [GNU GPL v3.0](LICENSE) 发布，许可证继承自 Monogram。

---

<p align="center"><em>海内存知己，天涯若比邻。</em><br />—— 王勃《送杜少府之任蜀州》</p>
