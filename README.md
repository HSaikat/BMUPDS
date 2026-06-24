# BMU PDS — Unofficial Android Client

A modern, mobile-first Jetpack Compose Android wrapper application optimized for the **Bangladesh Medical University (BMU) Personnel Data System (PDS)** portal. This client dynamically transforms a legacy desktop ERP layout into a responsive, fluid, touch-optimized experience for mobile screens.

---

## 📱 App Highlights & Interface Modernization

The application injects a customized JavaScript framework and structural CSS overrides directly into the native WebView rendering engine post-load. It targets legacy container scopes across the internal data pages to correct layout constraints and text clipping without altering any source system logic.

* **Adaptive Viewport Overhauls:** Strips hardcoded desktop metadata bounds and forces containers to align with native, adaptive mobile viewport configurations (`width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no`).
* **Modern Floating Card Login:** Completely re-architects absolute desktop positions on the landing page into a sleek, centered, glassmorphic floating card UI with crisp text fields (`Enter your PDS ID`) and modern high-contrast button layouts.
* **Fluid Horizontal Swipe Integration:** Fixes critical right-side truncation issues found on internal payroll and report pages. Re-engineers layout groups (`.oe_view_manager_body`, `.oe_list_content`) to use explicit multi-axis rendering rules (`overflow-x: auto !important`) and list container optimizations.
* **Native Connection Interceptor Stack:** Features a custom native offline fallback screen. If network connectivity drops or a timeout occurs, the app gracefully intercepts the fault internally and shows a clean, styled layout prompt with manual retry handles rather than raw web browser timeout loops.
* **Optimized WebView Engine:** Customizes core native `WebView` parameters, enabling `javaScriptEnabled`, `domStorageEnabled`, `databaseEnabled`, and secure `mixedContentMode` policies.

---

## 🛡️ Robust Document Management & Resilient Architecture

The app includes advanced platform capabilities and defensive failsafes to guarantee high availability and usability even if server-side layouts or security rules evolve:

* **Secure File Upload Interception:** Overrides native web file chooser dialogs using a custom `WebChromeClient` tied to Compose state-driven `ActivityResultContracts`. Offloads file selection securely to the native system file manager, granting secure, single-instance read tokens without requiring total app storage access permissions.
* **Session-Authenticated Download Engine:** Integrates a secure background download handler that forces synchronization of production authentication state via `CookieManager.flush()`. This prevents anonymous or dropped session rejections when downloading salary slips or encrypted documentation.
* **Android 14 (API 34) Compliance:** Implements asynchronous background broadcast tracking safely utilizing `ContextCompat.registerReceiver` flagged explicitly with `RECEIVER_EXPORTED` status to handle download notifications securely without memory leaks or runtime exceptions.
* **Dual-Layer Browser Fallbacks:** Implements continuous telemetry error-catching. If a main-frame load failure is encountered or an asynchronous system file download gets blocked by extreme server configurations, it instantly routes the direct resource handle to the device's system browser.
* **Isolated JavaScript Sandbox Execution:** Wraps custom layout injections inside a self-executing sandbox containing strict browser `try-catch` exception blocks. If server elements change dynamically, scripts fail silently to prevent application freezing or blank screens, protecting the core client container.
* **Context-Aware Dynamic Desktop Mode:** Includes an intelligent, conditional toggle overlay that surfaces automatically if a structural portal modification breaks custom styles, or if toggled by the user. It instantly replaces User-Agent profiles, switches viewport constraints (`useWideViewPort` / `loadWithOverviewMode`), and bypasses mobile scripts to present the pristine, original desktop system.

---

## 📂 Project Structure Map

```text
bmupds/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/bmupds/
│   │   │   └── MainActivity.kt      # Unified Application Shell, JS Injection Engine & Native Receivers
│   │   └── AndroidManifest.xml      # Hardware, Network, and System Storage Access Manifest
│   └── build.gradle.kts          # App-level dependencies and SDK configuration
├── LICENSE                       # Project Open-Source Licensing Manifest
└── README.md                     # Technical Documentation File (This File)

```

---

## ⚙️ Quick Installation & Setup

### 1. System Requirements

* Android Studio Jellyfish (or newer build paths)
* Android SDK Platform 34+ (Target SDK: 34, Minimum SDK: 26)
* Gradle build system configured for Kotlin DSL

### 2. Core Manifest Configuration

To ensure seamless execution of background download management and network connectivity handovers, verify the following configurations exist inside your `AndroidManifest.xml`:

```xml
<manifest xmlns:android="[http://schemas.gestures](http://schemas.gestures)...">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    
    <application ...>
        </application>
</manifest>

```

```

```
