# BMU PDS — Unofficial Android Client

A modern, mobile-first Jetpack Compose Android wrapper application optimized for the **Bangladesh Medical University (BMU) Personnel Data System (PDS)** portal. This client dynamically transforms a legacy desktop ERP layout into a responsive, fluid, touch-optimized experience for mobile screens.

---

## 📱 App Highlights & Interface Modernization

The application injects a customized JavaScript framework and structural CSS overrides directly into the native WebView rendering engine post-load. It targets legacy container scopes across the internal data pages to correct layout constraints and text clipping without altering any source system logic.

* **Adaptive Viewport Overhauls:** Strips hardcoded desktop metadata bounds and forces containers to align with native, adaptive mobile viewport configurations (`width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no`).
* **Modern Floating Card Login:** Completely re-architects absolute desktop positions on the landing page into a sleek, centered, glassmorphic floating card UI with crisp text fields (`Enter your PDS ID`) and modern high-contrast button layouts.
* **Fluid Horizontal Swipe Integration:** Fixes critical right-side truncation issues found on internal payroll and report pages. Re-engineers layout groups (`.oe_view_manager_body`, `.oe_list_content`) to use explicit multi-axis rendering rules (`overflow-x: auto !important`) and list container optimizations. This unlocks clean, natural horizontal touch-swiping to reach hidden details (**নীট মোট বেতন**, processing states, and interactive action buttons).
* **Flat Menu Card Transformations:** Automatically refactors complicated sidebar elements into modern, touch-optimized flat layout links. Secondary tracking entries are grouped smoothly into a single custom mobile accordion dropdown (**আরও বিকল্প ▾**).
* **Dynamic Theme Remodeling:** Recasts original layouts into a high-contrast, premium dark navigation theme (`#0F172A` / `#111424`), replacing old layout formats with clean white-on-dark branding text assets and custom grid tiles.

---

## 🛠️ Architecture & System Framework

The codebase is built on top of a single-activity structural container that leverages modern Android development standards:

* **Jetpack Compose UI:** Employs a fully declarative layout model featuring material-grade component scaffolding (`PullToRefreshBox`, `Surface`, and `Box`).
* **Native Connection Interceptor Stack:** Features a custom native offline fallback screen. If network connectivity drops or a timeout occurs, the app gracefully intercepts the fault internally and shows a clean, styled layout prompt with manual retry handles rather than raw web browser timeout loops.
* **Optimized WebView Engine:** Customizes core native `WebView` parameters, enabling `javaScriptEnabled`, `domStorageEnabled`, `databaseEnabled`, and secure `mixedContentMode` policies. It bypasses desktop scaling limitations to keep the forced mobile styles intact.
* **Asynchronous Refresh Mechanics:** Integrates a material-grade pull-to-refresh state tied directly to asynchronous coroutine scopes (`rememberCoroutineScope`) to handle seamless portal reloads.

---

## 📂 Project Structure Map

```text
bmupds/
├── app/
│   ├── src/main/java/com/example/bmupds/
│   │   └── MainActivity.kt      # Unified Application Shell & JS Injection Engine
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

```

---




---

## 📄 License

This utility companion engine is distributed openly and safely under the legal parameters of the **MIT License** — view the included [LICENSE](https://www.google.com/search?q=LICENSE) tracking master file for clear copy-permission definitions.

```

```
