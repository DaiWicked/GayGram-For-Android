<p align="center"><img src="logo.png" alt="GayGram" width="112" /></p>

<h1 align="center">GayGram</h1>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-7.0%2B-3ddc84" />
  <a href="./LICENSE"><img alt="GPLv3" src="https://img.shields.io/badge/License-GPLv3-blue.svg" /></a>
</p>

<p align="center"><a href="README.md">中文</a> · <a href="README_EN.md">English</a> · <a href="README_RU.md">Русский</a> · <a href="README_TR.md">Türkçe</a> · <a href="README_KOR.md">한국어</a> · <a href="README_UR.md">اُردو</a> · <a href="README_ES.md">Español</a></p>

## About

**GayGram is an Android Telegram client from the 缺德组织 crew.**

It's built on [Monogram](https://github.com/monogram-android/monogram) — a native client in Kotlin and Compose, with a Rust protocol layer. On top of that base we added two things: a layer of glass on the controls, and a haptic feedback layer.

It's not "the official client with a pile of toggles bolted on." The UI is redone for a phone, and the features are being moved over one at a time from our own Windows desktop client.

## Features

| Category | What's in it |
| --- | --- |
| 🫧 Glass & haptics | Liquid glass on top bars, composer, pill, and behind menus; tactile feedback for taps, long presses, toggles, and list edge hits |
| 👻 Privacy | Anti-recall, ghost mode (hide online / read receipts / typing), message history |
| ✨ Extras | Local premium, auto-translate, message filters |
| 🧱 Under the hood | Native Kotlin + Compose, Rust MTProto, folders, QR / passkey sign-in |

Still being moved over from the desktop build: appearance tweaks, custom fonts, aurora gradients, wallpaper blur, biometric lock, dynamic icon, home screen widgets.

## What's different

### Glass

The top bars, the composer, the jump-to-latest pill, the area behind menus — these controls are frosted. The messages scrolling underneath blur through, and the rounded bits show a little refraction.

Messages themselves stay untouched. When you're reading, you want to read — not read through frosted glass.

### Haptics

A tap, a long press, a toggle, dragging a list to the end — each has its own feel. When the list is already at the bottom and you pull harder, it clicks once, like hitting a wall. Fling it and let momentum carry it there and it stays quiet, because that pull wasn't yours.

Too noisy? Turn it off in Settings → Appearance, or let it follow the system.

## Download

Grab the latest APK from [Releases](https://github.com/DaiWicked/GayGram-For-Android/releases) and allow installs from unknown sources. First sign-in needs your own Telegram account; if you want push, grant notification permission in system settings.

Runs on Android 7.0 and up.

> A personal project — not an official Telegram app, not on any store. If something feels off, open an issue and tell us.

## Building from source

We ship via GitHub Actions. To build locally you'll need:

- JDK 17+
- Android SDK (Platform 37)
- Rust + NDK (mtproto is Rust, via UniFFI bindings)
- Your own Telegram `API_ID` / `API_HASH`
- A `telers-mtproto-impl` checkout **next to** this repository (the Cargo manifests read `../telers-mtproto-impl`; the submodule under `vendor` is not enough)

Build notes are in `AGENTS.md` at the repo root — written for AI collaborators, but readable by humans too.

## 缺德组织

<p align="center"><img src="zuzhi.png" alt="缺德组织" width="168" /></p>

GayGram is a **缺德组织** production — a name that isn't very proper, for work that is.

## License

Released under [GNU GPL v3.0](LICENSE), inherited from Monogram.

---

<p align="center"><em>海内存知己，天涯若比邻。</em><br />"A friend kept in the heart leaves no distance: the far ends of the earth are next door."<br />— Wang Bo, <em>Sending Off Vice Magistrate Du to His Post in Shuzhou</em></p>
