<p align="center"><img src="logo.png" alt="GayGram" width="112" /></p>

<h1 align="center">GayGram</h1>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-7.0%2B-3ddc84" />
  <a href="./LICENSE"><img alt="GPLv3" src="https://img.shields.io/badge/License-GPLv3-blue.svg" /></a>
</p>

<p align="center"><a href="README.md">中文</a> · <a href="README_EN.md">English</a> · <a href="README_RU.md">Русский</a> · <a href="README_TR.md">Türkçe</a> · <a href="README_KOR.md">한국어</a> · <a href="README_UR.md">اُردو</a> · <a href="README_ES.md">Español</a></p>

## Sobre

**GayGram es un cliente de Telegram para Android de 缺德组织.**

Está basado en <a href="https://github.com/monogram-android/monogram">Monogram</a> — un cliente nativo en Kotlin y Compose, con el protocolo en Rust. Sobre esa base añadimos dos cosas: una capa de cristal sobre los controles, y una capa de respuesta háptica.

No es "el cliente oficial con un montón de interruptores". La interfaz está rehecha para el móvil, y las funciones se van trasladando una a una desde nuestro propio cliente de escritorio para Windows.

## Funciones

| Categoría | Qué incluye |
| --- | --- |
| 🫧 Cristal y háptica | Cristal líquido en barras superiores, campo de escritura, botón de salto y detrás de menús; respuesta táctil en toques, pulsaciones largas, interruptores y al llegar la lista al borde |
| 👻 Privacidad | Protección contra borrado, modo fantasma (ocultar conexión / lectura / escritura), historial de mensajes |
| ✨ Extras | Premium local, traducción automática, filtros de mensajes |
| 🧱 Base | Kotlin + Compose nativo, MTProto en Rust, carpetas, inicio por QR / passkey |

Todavía se está trasladando desde el escritorio: ajustes de apariencia, fuentes propias, degradado aurora, desenfoque de fondo, bloqueo biométrico, icono dinámico, widgets de pantalla de inicio.

## Qué lo diferencia

### Cristal

Las barras superiores, el campo de escritura, el botón de salto a lo último y el área detrás de los menús llevan cristal. Los mensajes que se despliegan por detrás se difuminan, y en las esquinas redondeadas se ve un poco de refracción.

Los mensajes en sí no llevan cristal. Cuando lees, quieres leer — no mirar a través de un cristal esmerilado.

### Respuesta háptica

Un toque, una pulsación larga, un interruptor, una lista que llega al final — cada cosa tiene su propia sensación. Si la lista ya está abajo del todo y sigues tirando, suena un chasquido, como tocar una pared. Si la sueltas y llega por inercia, se queda callada: ese tirón no fue tuyo.

¿No lo quieres? Se apaga en Ajustes → Apariencia, o lo dejas seguir al sistema.

## Descargar

El último APK está en [Releases](https://github.com/DaiWicked/GayGram-For-Android/releases). Permite instalar de orígenes desconocidos e instala. Para la primera sesión necesitas tu propia cuenta de Telegram; si quieres notificaciones, dale permiso en los ajustes del sistema.

Funciona en Android 7.0 y versiones posteriores.

> Proyecto personal: no es una aplicación oficial de Telegram ni se publica en ninguna tienda. Si algo no encaja, abre un issue.

## Compilar desde el código

Nosotros publicamos vía GitHub Actions. Para compilar localmente necesitas:

- JDK 17+
- Android SDK (Platform 37)
- Rust + NDK (mtproto está en Rust, vía UniFFI)
- tus propias `API_ID` / `API_HASH` de Telegram
- una copia de `telers-mtproto-impl` **junto** al repositorio (los manifiestos de Cargo leen `../telers-mtproto-impl`; el submódulo en `vendor` no basta)

Los detalles están en `AGENTS.md` en la raíz del repo; escrito para asistentes de IA, pero los humanos también lo entienden.

## 缺德组织

<p align="center"><img src="zuzhi.png" alt="缺德组织" width="168" /></p>

GayGram es obra de **缺德组织**: un nombre poco serio para un trabajo serio.

## Licencia

Distribuido bajo [GNU GPL v3.0](LICENSE), licencia heredada de Monogram.

---

<p align="center"><em>海内存知己，天涯若比邻。</em><br />«Un amigo guardado en el corazón acorta el mundo: los confines de la tierra quedan al lado.»<br />— Wang Bo, <em>Despidiendo al subprefecto Du hacia su destino en Shuzhou</em></p>
