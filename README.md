# FALAQ - The Focused & Secure Browser

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-purple?style=for-the-badge&logo=kotlin" alt="Kotlin & Jetpack Compose">
  <img src="https://img.shields.io/badge/Android-Native-green?style=for-the-badge&logo=android" alt="Android Native">
</p>

**FALAQ** is a fast, safe, and modern Android browser designed for users who value focus, purity, and a distraction-free web experience. Built with native Android technologies (Kotlin and Jetpack Compose), it provides a smooth, responsive, and secure browsing environment.

*Note: This project was developed with the assistance of AI.*

## ✨ Features

- **🛡️ Built-in Content Blocking**: Automatically blocks known adult and malicious domains. It uses a robust offline domain matching engine based on [StevenBlack's hosts](https://github.com/StevenBlack/hosts) to keep your browsing experience clean.
- **🔍 Forced Safe-Search**: FALAQ automatically enforces safe-search strict modes on major search engines (Google, Bing, Yahoo, DuckDuckGo).
- **🌙 True AMOLED Dark Mode**: Injects deep black backgrounds into web pages using `FORCE_DARK_ON` for WebView. Saves battery on OLED screens and reduces eye strain at night.
- **📖 OALD 10th Integration**: Native integration with the Oxford Advanced Learner's Dictionary (OALD). Select any word on a webpage, and a custom context menu action lets you look it up instantly in the OALD app.
- **⚡ Fast & Lightweight UI**: Built entirely with Jetpack Compose and Material 3 for a fluid, accessible, and intuitive user interface. 
- **🌐 Custom Domain Blocking**: Easily block specific distracting sites (like social media or video streaming platforms) via the `UrlHandler`.

## 🚀 Getting Started

FALAQ is fully open-source and easy to build. We encourage developers to clone, tweak, and experiment with the source code!

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Koala or newer recommended).
- JDK 17+.

### Installation
1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/falaq-browser.git
   cd falaq-browser
   ```
2. **Open the project in Android Studio.**
3. **Sync Gradle files** and allow the dependencies to download.
4. **Run the app** on an Android emulator or a physical device via ADB.

## 🛠️ Modifying Custom Blocklists

FALAQ allows you to easily customize which domains are blocked. You can modify this directly in the source code.

### 1. Blocking Specific Custom Domains
To add or remove specific domains (e.g., blocking distracting social media sites like YouTube or Instagram), edit the `UrlHandler.kt` file:

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

### 2. Updating the Core Blocklist Source
The core adult-content blocklist is fetched remotely and cached. If you want to use a different hosts file (for example, to block ads, tracking, or fake news), edit `BlocklistManager.kt`:

**File Location:** `app/src/main/java/com/example/logic/BlocklistManager.kt`

```kotlin
// Change this URL to point to your preferred hosts file
private const val HOSTS_URL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn-only/hosts"
```

## 🛡️ Recommendations & Adblocking

While FALAQ comes with powerful built-in domain blocking for specific categories (like adult content and custom distractions), **it is highly recommended to use a system-wide or DNS-level adblocker alongside FALAQ** for a completely ad-free and tracking-free experience. 

Great options include:
- **AdGuard DNS** (Configurable in Android Settings -> Private DNS)
- **NextDNS**
- **Blokada**

Using these tools in conjunction with FALAQ ensures maximum privacy and performance.

## 🤝 Contributing

Contributions are welcome! If you have ideas for new features, bug fixes, or performance improvements:
1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

## 📄 License

This project is released under a **Custom Non-Commercial License**. 

You are highly encouraged to clone, tweak, experiment, and use this code for personal or educational purposes! However, **commercial use, distribution, or monetization** of this software or its derivatives requires explicit written permission from the repository owner. 

See the `LICENSE` file for more details.
