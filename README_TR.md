<p align="center"><img src="logo.png" alt="GayGram" width="112" /></p>

<h1 align="center">GayGram</h1>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-7.0%2B-3ddc84" />
  <a href="./LICENSE"><img alt="GPLv3" src="https://img.shields.io/badge/License-GPLv3-blue.svg" /></a>
</p>

<p align="center"><a href="README.md">中文</a> · <a href="README_EN.md">English</a> · <a href="README_RU.md">Русский</a> · <a href="README_TR.md">Türkçe</a> · <a href="README_KOR.md">한국어</a> · <a href="README_UR.md">اُردو</a> · <a href="README_ES.md">Español</a></p>

## Hakkında

**GayGram, 缺德组织'ün Android Telegram istemcisidir.**

<a href="https://github.com/monogram-android/monogram">Monogram</a> üzerine inşa edilmiştir — Kotlin ve Compose ile yazılmış yerel bir istemci, protokol katmanı Rust. Bu temelin üstüne iki şey ekledik: kontrollerin üstünde bir cam katmanı ve bir dokunsal geri bildirim katmanı.

"Resmî istemcinin üstüne bir sürü anahtar takılmış hali" değil bu. Arayüz telefona göre yeniden yapıldı, özellikler ise aynı ekibin Windows masaüstü istemcisinden tek tek taşınıyor.

## Özellikler

| Kategori | İçerik |
| --- | --- |
| 🫧 Cam ve dokunsal | Üst çubuklar, yazı alanı, atlama düğmesi ve menü arkasında sıvı cam; dokunma, uzun basma, anahtar ve liste kenar dayanmasında kendi geri bildirimi |
| 👻 Gizlilik | Mesaj silinmesine karşı koruma, hayalet mod (çevrimiçi / okundu / yazıyoru gizle), mesaj geçmişi |
| ✨ Ekstralar | Yerel Premium, otomatik çeviri, mesaj filtreleri |
| 🧱 Alt yapı | Yerel Kotlin + Compose, Rust MTProto, klasörler, QR / passkey ile giriş |

Masaüstünden hâlâ taşınanlar: görünüm ayarları, özel fontlar, aurora gradyanı, duvar kâğıdı bulanıklığı, biyometrik kilit, dinamik ikon, ana ekran widget'ları.

## Neyi farklı

### Cam

Üst çubuklar, yazı alanı, en son mesaja atlama düğmesi ve menü arkası — bu kontroller camın altında. Arkada kayan mesajlar bulanıklaşır, yuvarlak köşelerde hafif kırılma görülür.

Mesajların kendisinde cam yoktur. Okurken sadece mesajı istersin, buzlu camın arkasından değil.

### Dokunsal geri bildirim

Dokunma, uzun basma, anahtar, listeyi sonuna kadar sürükleme — her birinin kendine has hissi vardır. Liste zaten en dipte duruyorsa ve daha çekersen bir kez "tık" eder, duvara değmiş gibi. Bırakıp ataletle oraya varırsa ses çıkmaz, çünkü o çeken sen değildin.

İstemiyor musun? Ayarlar → Görünüm'den kapat, ya da sistemi izlemesine bırak.

## İndir

En güncel APK'yı [Releases](https://github.com/DaiWicked/GayGram-For-Android/releases) sayfasından indir, bilinmeyen kaynaklardan kuruluma izin ver ve kur. İlk girişte kendi Telegram hesabın gerekir; bildirim istiyorsan sistem ayarlarından izin vermeyi unutma.

Android 7.0 ve üzerinde çalışır.

> Kişisel proje: resmî bir Telegram uygulaması değil ve hiçbir mağazada yayınlanmıyor. Bir şey tuhaf gelirse issue aç.

## Kaynaktan derleme

Biz GitHub Actions üzerinden yayın yapıyoruz. Yerelde derlemek için gerekenler:

- JDK 17+
- Android SDK (Platform 37)
- Rust + NDK (mtproto Rust'ta, UniFFI üzerinden)
- kendi `API_ID` / `API_HASH` Telegram bilgilerin
- deponun **yanında** bir `telers-mtproto-impl` kopyası (Cargo manifestleri `../telers-mtproto-impl` okur; `vendor` altındaki submodule yetmez)

Ayrıntılar kök dizindeki `AGENTS.md`'de; AI yardımcıları için yazıldı ama insanlar da okuyabilir.

## 缺德组织

<p align="center"><img src="zuzhi.png" alt="缺德组织" width="168" /></p>

GayGram bir **缺德组织** işidir: adı pek ciddi değil, işi ciddi.

## Lisans

[GNU GPL v3.0](LICENSE) ile dağıtılır, lisans Monogram'dan mirastır.

---

<p align="center"><em>海内存知己，天涯若比邻。</em><br />«Gönülde dost varsa mesafe kalmaz: dünyanın öbür ucu komşu olur.»<br />— Wang Bo, <em>Du Shaofu'yu Shuzhou'daki görevine uğurlarken</em></p>
