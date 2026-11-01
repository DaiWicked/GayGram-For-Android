<p align="center"><img src="logo.png" alt="GayGram" width="112" /></p>

<h1 align="center">GayGram</h1>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-7.0%2B-3ddc84" />
  <a href="./LICENSE"><img alt="GPLv3" src="https://img.shields.io/badge/License-GPLv3-blue.svg" /></a>
</p>

<p align="center"><a href="README.md">中文</a> · <a href="README_EN.md">English</a> · <a href="README_RU.md">Русский</a> · <a href="README_TR.md">Türkçe</a> · <a href="README_KOR.md">한국어</a> · <a href="README_UR.md">اُردو</a> · <a href="README_ES.md">Español</a></p>

## 프로젝트 소개

**GayGram은 缺德组织에서 만든 Android Telegram 클라이언트입니다.**

<a href="https://github.com/monogram-android/monogram">Monogram</a>을 기반으로 합니다 — Kotlin과 Compose로 쓴 네이티브 클라이언트이고 프로토콜 계층은 Rust입니다. 그 위에 두 가지를 얹었습니다: 컨트롤 위에 얇은 유리 층, 그리고 햅틱 피드백.

"공식 클라이언트에 토글만 잔뜩 단 것"이 아닙니다. UI는 휴대폰에 맞게 다시 만들었고, 기능은 같은 팀의 Windows 데스크톱 클라이언트에서 하나씩 옮겨오고 있습니다.

## 기능

| 분류 | 내용 |
| --- | --- |
| 🫧 유리와 햅틱 | 상단 바, 입력창, 최신 메시지 점프 버튼, 메뉴 뒤에 액체 유리; 탭·길게 누르기·스위치·리스트 끝 걸림마다 각자 다른 촉각 |
| 👻 개인정보 | 메시지 회수 방지, 고스트 모드(접속/읽음/입력중 숨김), 메시지 기록 |
| ✨ 편의 | 로컬 프리미엄, 자동 번역, 메시지 필터 |
| 🧱 기반 | 네이티브 Kotlin + Compose, Rust MTProto, 폴더, QR / passkey 로그인 |

데스크톱에서 아직 옮겨오는 중: 외관 설정, 사용자 정의 폰트, 오로라 그라데이션, 배경화면 블러, 생체 인식 잠금, 동적 아이콘, 홈 화면 위젯.

## 남들과 다른 점

### 유리

상단 바, 입력창, 최신 메시지로 가는 버튼, 메뉴 뒤쪽 — 이 컨트롤들에 유리가 얹혀 있습니다. 뒤에서 스크롤되는 메시지가 흐려져 비치고, 둥근 모서리에서는 약간의 굴절이 보입니다.

메시지 자체에는 유리를 얹지 않습니다. 메시지를 읽을 때는 그냥 메시지를 보고 싶으니까요.

### 햅틱

탭, 길게 누르기, 스위치, 리스트를 끝까지 끌기 — 각자 자기만의 감각이 있습니다. 리스트가 이미 바닥에 닿아 있는데 더 끌면 벽에 부딪힌 듯 "딸깱" 한 번 울립니다. 던져서 관성으로 닿으면 조용합니다, 당신이 당긴 게 아니니까.

싫으면 설정 → 화면에서 끄거나 시스템을 따르게 두세요.

## 다운로드

[Releases](https://github.com/DaiWicked/GayGram-For-Android/releases)에서 최신 APK를 받고, 알 수 없는 출처 설치를 허용한 뒤 설치하세요. 처음 로그인할 때는 본인 Telegram 계정이 필요하고, 알림을 받으려면 시스템 설정에서 알림 권한을 주세요.

Android 7.0 이상에서 동작합니다.

> 개인 프로젝트입니다. Telegram 공식 앱이 아니며 어떤 스토어에도 올라가지 않습니다. 뭔가 이상하면 issue를 열어 알려 주세요.

## 소스에서 빌드

저희는 GitHub Actions로 빌드합니다. 로컬에서 빌드하려면:

- JDK 17 이상
- Android SDK (Platform 37)
- Rust + NDK (mtproto가 Rust로 작성됨, UniFFI 경유)
- 본인의 Telegram `API_ID` / `API_HASH`
- 이 저장소 **옆에** 둘 `telers-mtproto-impl` 체크아웃 (Cargo 매니페스트가 `../telers-mtproto-impl`를 읽으며, `vendor` 아래 submodule로는 쵩하지 않습니다)

자세한 내용은 루트의 `AGENTS.md`를 참고하세요. AI 협업자를 위해 썼지만 사람이 읽어도 됩니다.

## 缺德组织

<p align="center"><img src="zuzhi.png" alt="缺德组织" width="168" /></p>

GayGram은 **缺德组织**의 작품입니다. 이름은 가볍지만 하는 일은 진지합니다.

## 라이선스

[GNU GPL v3.0](LICENSE)으로 배포되며, Monogram에서 계승했습니다.

---

<p align="center"><em>海内存知己，天涯若比邻。</em><br />«마음에 친구가 있다면 거리는 사라진다. 세상 끝도 이웃이 된다.»<br />— 왕발(王勃), 「두소부의 촉주 부임을 보내며」</p>
