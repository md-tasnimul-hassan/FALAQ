# FALAQ - The Focused & Halal Browser

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-purple?style=for-the-badge&logo=kotlin" alt="Kotlin & Jetpack Compose">
  <img src="https://img.shields.io/badge/Android-Native-green?style=for-the-badge&logo=android" alt="Android Native">
</p>

**FALAQ** is a fast, safe, modern and halal Android browser designed for users who value focus, purity, and a distraction-free web experience. Built with native Android technologies (Kotlin and Jetpack Compose), it provides a smooth, responsive, and secure browsing environment.

*Note: This project was developed with the assistance of AI.*

## ✨ Core Features

### 🎨 Modern Material 3 & Functional UI
- **No-BS, Clean Interface**: A strictly functional, minimal, and distraction-free UI. No bloat, no unnecessary feeds, and no visual clutter.
- **Material You Theming**: Fully embraces Android's Material 3 design guidelines with dynamic theming, smooth intuitive animations, and an immersive edge-to-edge layout.
- **Advanced UI Controls**: Features built-in text zooming/scaling, intuitive navigation shortcuts, easy refresh actions, and a thoughtfully designed bottom menu optimized for one-handed operation.
- **🌙 True AMOLED Dark Mode**: Injects deep black backgrounds into web pages using `FORCE_DARK_ON` for WebView. Saves battery on OLED screens and significantly reduces eye strain at night.

### 🛡️ Dual-Layer Domain & APK Blocking System
FALAQ incorporates distinct, powerful blocking mechanisms to keep your browsing clean and focused:
1. **Core Content Blocklist**: Automatically fetches and caches [StevenBlack's hosts file](https://github.com/StevenBlack/hosts) via an efficient background manager. It parses and evaluates thousands of domains locally and offline, ensuring zero-latency protection against adult content and unsafe sites.
2. **Custom Distraction Blocker**: A hardcoded, easily configurable local blocklist. Use this to permanently block personal productivity sinks (like YouTube, Facebook, Instagram, or TikTok) and reclaim your focus. YouTube, Reddit and Quora are blocked by default.
3. **Strict APK Download Blocking**: FALAQ explicitly blocks the download of `.apk` files directly from the web, preventing accidental or malicious side-loading of untrusted apps onto your device.

### 🔍 Secure Browsing Enhancements
- **Forced Safe-Search**: Automatically enforces strict safe-search. By default only google is allowed. 
- **📖 OALD 10th Integration**: Native integration with the Oxford Advanced Learner's Dictionary (OALD). Select any word on a webpage, and a custom context menu action lets you look it up instantly in the OALD app.

## 🚀 Getting Started

FALAQ is fully open-source and easy to build. We encourage developers to clone, tweak, and experiment with the source code!

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Koala or newer recommended).
- JDK 17+.

### Installation
1. **Clone the repository:**
   ```bash
   git clone https://github.com/md-tasnimul-hassan/FALAQ.git
   cd FALAQ
   ```
2. **Open the project in Android Studio.**
3. **Sync Gradle files** and allow the dependencies to download.
4. **Run the app** on an Android emulator or a physical device.

## 🛠️ Modifying Custom Blocklists

FALAQ allows you to easily customize which domains are blocked directly in the source code.

To add or remove specific domains (e.g., blocking distracting social media sites), edit the `UrlHandler.kt` file:

**File Location:** `app/src/main/java/com/example/logic/UrlHandler.kt`

```kotlin
// Inside UrlHandler object
private val customBlockedUrls = setOf(
    "youtube.com",
    "m.youtube.com",
    "instagram.com",
    "facebook.com",
    "tiktok.com"
    // Add your own custom domains here
)
```
Any domain added to this list will be intercepted and blocked by the browser.

## 🛡️ Recommendations & Adblocking

While FALAQ comes with powerful built-in domain blocking for specific categories, **it is highly recommended to use a system-wide or DNS-level adblocker alongside FALAQ** for a completely ad-free and tracking-free experience. 

Great options include:
- **NextDNS** (Highly recommended)
- **AdGuard DNS** (Configurable in Android Settings -> Network & Internet -> Private DNS)

Using these tools in conjunction with FALAQ ensures maximum privacy and performance.

## 🤝 Contributing

Contributions are welcome! If you have ideas for new features, bug fixes, or performance improvements:
1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

## 📄 License

This project is released under a **MIT License**. 

You are highly encouraged to clone, tweak, experiment, and use this code for personal or educational purposes!
See the `LICENSE` file for more details.

*Note: This app is solo developed. Apologies for any small bugs or issues.*