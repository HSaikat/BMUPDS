# BMU PDS — Unofficial Android Client

A modern, mobile-first Jetpack Compose Android wrapper application optimized for the **Bangladesh Medical University (BMU) Personnel Data System (PDS)** portal. This client dynamically transforms a legacy desktop ERP layout into a responsive, fluid, touch-optimized experience for mobile screens.

---

## 📱 App Highlights & Interface Modernization

The application injects a customized JavaScript framework and structural CSS overrides directly into the native WebView rendering engine post-load. It targets legacy container scopes across the internal data pages to correct layout constraints and text clipping without altering any source system logic.

* **Adaptive Viewport Overhauls:** Strips hardcoded desktop metadata bounds and forces containers to align with native, adaptive mobile viewport configurations.
* **Fluid Horizontal Swipe Integration:** Fixes the critical right-side truncation issue found on internal payroll and report pages. Re-engineers layout groups such as `.oe_form_sheet_width` and table configurations to use explicit multi-axis rendering rules (`overflow-x: auto !important`) and table width expansions (`min-width: 900px !important`). This unlocks clean, natural horizontal touch-swiping to reach hidden details (**নীট মোট বেতন**, processing states, and interactive action buttons).
* **Dynamic Theme Remodeling:** Recasts original layouts into a modern design language matching the portal's branding scheme while cleaning up form wrappers, inputs, and data tables.
* **Mobile Touch Accordions:** Bypasses legacy desktop mouse-hover event scripts and implements snappy, touch-based gesture tracking parameters (`touchstart`) along with dynamic visual accordion arrow indicators.

---

## 🛠️ Architecture & System Framework

The codebase is built on top of a single-activity structural container that leverages modern Android development standards:

* **Jetpack Compose UI:** Employs a fully declarative layout model featuring material-grade component scaffolding (`PullToRefreshBox`, `Surface`, and `Box`).
* **Optimized WebView Engine:** Customizes core native `WebView` parameters, enabling `domStorageEnabled`, `databaseEnabled`, and secure `mixedContentMode` policies. It bypasses zoom restrictions to preserve the forced responsive styles and custom JavaScript logic perfectly.
* **Asynchronous Refresh Mechanics:** Integrates a material-grade pull-to-refresh state tied directly to asynchronous coroutine scopes (`rememberCoroutineScope`) to handle seamless system reloads.

---

## ⚙️ Quick Installation & Setup

### 1. Requirements
* Android Studio Jellyfish (or newer build paths)
* Android SDK Platform 34+ (Target SDK: 34, Minimum SDK: 26)
* Gradle build system configured for Kotlin DSL

### 2. Clones & Source Deployment
To set up a local build space on your machine, pull the private repository and open it inside Android Studio:

```bash
git clone [https://github.com/HSaikat/BMU_PDS.git](https://github.com/HSaikat/BMU_PDS.git)
