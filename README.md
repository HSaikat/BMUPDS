# BMU PDS — Unofficial Android Client

A Kotlin and Jetpack Compose Android app for the **Bangladesh Medical University Personnel Data System (PDS)**. It brings the existing PDS and attendance portals into a mobile interface using Android WebView, with redesigned login, home, attendance, and selected salary pages.

The app uses the university’s existing forms, links, sessions, and server responses. The redesign changes how supported pages appear inside the app; it does not replace the university’s backend.

> This is an unofficial client and is not an official BMU application.

## What’s new

- **Redesigned login:** a clean blue-and-white layout, Bengali-friendly typography, clearer field labels, password visibility control, and visible login errors. The original login form is retained.
- **Rearranged home page:** employee profile information followed by grouped service options, quick services, and logout.
- **Simplified quick services:** `দ্রুত সেবা` contains only `পি ডি এস` and `ছুটি ও হাজিরা`. Salary status and salary report are no longer duplicated there.
- **Redesigned attendance dashboard:** five service cards replace the initial legacy dashboard view. The content area appears when a service is selected.
- **Attendance navigation fix:** subframe requests on the attendance dashboard can remain inside its iframe instead of replacing the entire WebView page.
- **Reminder bridge additions:** native methods now support muting, unmuting, and checking the current month’s reminder state.
- **Broader salary submission detection:** the reminder hook now also checks URLs containing `salary_form`, alongside `employee_salary_form-3`.

## Interface and supported pages

### Login and home

The entry-page redesign uses rounded cards, blue accents, responsive spacing, and Inter / Noto Sans Bengali font styling. Login keeps the original form action and input names, allowing the existing authentication flow to continue.

The home page reads available employee details and the profile image from the portal. Its layout follows this order:

1. Employee profile.
2. **বেতন ও ভাতাদি**.
3. **ব্যক্তিগত তথ্য ও সেটিংস**.
4. **রিপোর্ট ও অন্যান্য সেবা**.
5. **দ্রুত সেবা** — পি ডি এস and ছুটি ও হাজিরা.
6. Logout.

Navigation uses links discovered in the live page, including the attendance link’s existing token or query parameters.

### Attendance

On `attendance.bmu.ac.bd/dashboard.php`, the app presents these service cards:

| Service | Purpose |
| --- | --- |
| Attendance Log | View attendance records |
| Leave Apply | Open the leave application form |
| Leave Replace | Open the leave replacement service |
| Leave Report | View leave reports |
| Attendance Report | View attendance reports |

The initial overview iframe is hidden. Selecting a service opens its existing target inside the content area, with an active-card indicator and loading handling. A back-to-PDS action is also provided.

### Salary, allowances, and forms

The current source includes targeted transformations for salary and festival-allowance forms, monthly and festival salary lists, saved bills, future-fund information, salary status, returned bills, and salary reports.

Supported salary detail pages use summary sections for employee information, earnings, deductions, and net amounts. Supported lists use cards with available status, amounts, and original action links. General form styling improves spacing and presentation, while same-origin iframe adjustments help with wide tables.

**Implementation scope:** these are the transformations currently present in `MainActivity.kt`. The separate proposal to apply a new shared redesign to every service page has not yet been integrated into this source snapshot. Unsupported pages may retain parts of their original appearance.

## Android features

- **Pull to refresh** for reloading the current page.
- **Back navigation** through WebView history.
- **Loading overlay**, offline retry screen, and a page-error screen with navigation actions.
- **File chooser support** for web forms, including multiple selections when requested.
- **DownloadManager integration** with the current WebView cookies and user-agent attached to download requests.
- **Browser fallback** when a download fails or cannot be started.
- **Lifecycle handling** that pauses and resumes the WebView and its timers.
- **Update checks** using a version JSON file hosted on GitHub.
- **Calendar-based salary reminders** through Android’s calendar provider.

### Downloads

Downloads currently use Android `DownloadManager` and target the public Downloads directory. The app supplies the WebView’s cookies and user-agent to support authenticated download requests, and requests a completion notification.

If DownloadManager reports failure, or starting the download throws an exception, the app attempts to open the URL in an external browser. The browser has its own session, so protected files may still require authentication there.

The current implementation is **DownloadManager with browser fallback**, not a browser-only downloader.

### Salary reminders

`CalendarReminderHelper.kt` creates calendar events on every even-numbered day from **2 through 28**, at **10:00 AM in the device’s local time zone**, with an alert **10 minutes before** each event. Past times are skipped when scheduling the current month.

The helper requires calendar access and an available calendar. The device’s calendar app and notification settings determine how alerts are delivered.

| Action | Current behavior |
| --- | --- |
| Initial scheduling | Adds the remaining current-month events when permission is available and stored reminder state permits it |
| Salary submission hook | Removes stored reminder events and schedules the following month’s events |
| Mute | Removes stored events and records the current month as muted |
| Unmute | Clears the muted state and attempts to recreate remaining current-month events |

The JavaScript bridge exposes `onBillSubmitted()`, `muteReminder()`, `unmuteReminder()`, and `isReminderMuted()` under `Android`.

**Current limitations:** mute/unmute bridge methods are implemented, but the supplied page redesign does not yet include a visible reminder toggle. Submission detection is based on clicking a matching button and a short delay; it is not confirmation that the server accepted the bill. Events are created for specific dates rather than as an indefinite recurring rule, and reminder bookkeeping uses stored event IDs.

### App updates

The app reads version information from:

```text
https://raw.githubusercontent.com/HSaikat/BMUPDS/master/version.json
```

The JSON must provide these fields:

| Field | Type | Purpose |
| --- | --- | --- |
| `latestVersionCode` | Integer | Compared with the installed app’s version code |
| `latestVersionName` | String | Displayed in the update prompt |
| `updateUrl` | String | Opened when the user selects Update Now |

A higher version code shows an update dialog. **Update Now** opens the supplied URL externally; **Later** dismisses the prompt. The app does not automatically install an APK.

## Code structure

| File / component | Responsibility |
| --- | --- |
| `MainActivity.kt` | Activity setup, permissions, Compose UI, WebView configuration, navigation, downloads, file selection, update checks, and JavaScript injection |
| `INJECT_JS` | Portal DOM transformations, entry-page and attendance redesigns, salary styling, iframe handling, and submission detection |
| `PortalScreen` | WebView container, refresh state, loading and error UI, back handling, and lifecycle integration |
| `WebAppInterface` | JavaScript-to-Android reminder bridge |
| `CalendarReminderHelper.kt` | Calendar lookup, event creation/removal, and monthly reminder state in SharedPreferences |

The Kotlin package in the supplied files is `com.example.bmupds`.

The default portal entry point is:

```text
https://pds.bmu.ac.bd/pds/user_mod/pages/home/index.php
```

The activity also reads a `deep_url` intent extra during creation. This alone does not establish Android App Links; any external link handling depends on the project’s manifest configuration.

## Build and setup

Use the **complete Android project**, not only these two Kotlin files.

1. Open the project in an Android Studio version compatible with its Android Gradle Plugin.
2. Use the SDK and JDK versions required by the project’s Gradle configuration.
3. Sync Gradle and confirm the required libraries are present: Jetpack Compose / Material 3, AndroidX Activity and lifecycle support, Kotlin coroutines, OkHttp, and Gson.
4. Check the manifest permissions below and the activity declaration.
5. Run on an emulator or Android device, or build a debug APK:

   ```bash
   ./gradlew assembleDebug
   ```

The usual debug APK location is `app/build/outputs/apk/debug/`, depending on the project’s module and build configuration.

### Permissions

These permissions correspond to capabilities used in the supplied Kotlin source and should be declared as appropriate in the project’s `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.WRITE_CALENDAR" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Calendar permissions are requested at runtime. Notification permission is requested on Android 13 and later. Confirm any additional storage requirements against the project’s supported Android versions and download configuration.

The supplied files do not establish the minimum SDK, target SDK, dependency versions, or release version. Those values must come from the actual Gradle files and manifest.

## Implementation notes

- Page transformations depend on the university portal’s HTML structure. Portal changes can require selector and parsing updates.
- Cross-origin iframe content cannot be restyled by the same-origin DOM injection code.
- Cached WebView content may sometimes remain available without a connection, but the app is not a full offline PDS client.
- The normal interface no longer exposes a desktop-view toggle. Dormant desktop-mode state and user-agent code remain in `MainActivity.kt`.
- The current WebView configuration allows mixed content and proceeds past SSL certificate errors. These are existing implementation limitations and should be corrected before describing the app as enforcing secure HTTPS validation.
- This README describes the supplied Kotlin snapshot. A full-project build and on-device validation are required to verify release readiness.

## Project

[BMUPDS on GitHub](https://github.com/HSaikat/BMUPDS)

BMU portal availability, account access, and the accuracy of university records remain under the university’s systems. This app provides an alternative Android interface to those services.
