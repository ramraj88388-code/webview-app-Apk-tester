# 📦 WebView App Tester

**One APK. Unlimited web apps. Full native features.**

Test any website as a native Android app — with biometric lock, download bridge, dark mode sync, battery monitoring, and 20+ features. No Play Store needed.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-6.0%2B-green.svg)](https://developer.android.com)
[![APK Size](https://img.shields.io/badge/APK%20Size-57KB-blue.svg)]()
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

---

## 🎯 What Is This?

A single Android APK that lets you add unlimited web apps and launch each one in a fullscreen WebView with native Android features. Think of it as a **browser that makes websites feel like native apps** — with features browsers don't have.

**Use cases:**
- Test your PWA/web app as an Android app before publishing
- Demo web apps to clients on their phone
- Run internal business tools as native-feeling apps
- Test WebView compatibility of any website

---

## 📱 Screenshots

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ 📱 Apps         │  │ 📖 Guide        │  │ ⚙️ Settings      │
│                 │  │                 │  │                 │
│ ● My CRM App   │  │ 🔐 Google Login │  │ Biometric  [OFF]│
│   crm.app.com  │  │   BEFORE: ...   │  │ Refresh    [ON] │
│   [▶] [✏️] [🗑]  │  │   AFTER:  ...   │  │ Cache Clear[ON] │
│                 │  │                 │  │ Dark Sync  [ON] │
│ ● Dashboard    │  │ 📥 Downloads    │  │ GPS        [ON] │
│   dash.io      │  │   BEFORE: ...   │  │ Camera     [ON] │
│   [▶] [✏️] [🗑]  │  │   AFTER:  ...   │  │             ... │
│                 │  │                 │  │                 │
│           [+]   │  │ [📥 PDF] [📋 Copy]│  │ [🔄 Reset All]  │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

---

## ⚡ 24 Features

Every web app launched from this tester gets these features automatically:

### 🔒 Security
| Feature | Description | Toggleable |
|---------|-------------|------------|
| Biometric Lock | Fingerprint/PIN/face on app open | ✅ Settings |
| Keep Screen On | Prevent screen sleep | ✅ Settings |

### 🌐 WebView
| Feature | Description | Toggleable |
|---------|-------------|------------|
| Fullscreen Mode | No browser toolbar | Always on |
| Chrome User-Agent | Spoofs Chrome/125 | ✅ Settings |
| Refresh Button | Floating ↻ button | ✅ Settings |
| Cache Clear | Clears SW cache on load | ✅ Settings |
| Orientation Unlock | Portrait + landscape | ✅ Settings |
| Back Navigation | History → double-tap exit | Always on |
| Offline Screen | Styled retry page | Always on |

### 📱 Device Integration
| Feature | Description | Toggleable |
|---------|-------------|------------|
| Dark/Light Sync | Phone theme → web app | ✅ Settings |
| Battery Saver | Monitor + inject level | ✅ Settings |
| Notifications | Browser → Android status bar | ✅ Settings |
| GPS | Geolocation with permissions | ✅ Settings |
| Camera | File upload + capture | ✅ Settings |

### 📥 Downloads
| Feature | Description | Toggleable |
|---------|-------------|------------|
| Download Bridge | Blob/data URI → Downloads folder | ✅ Settings |
| HTTP Downloads | Standard URL downloads | Always on |
| Download Notification | Status bar notification on complete | ✅ Settings |
| Tap to Open | Tap notification → opens file | Always on |

### 🔗 External Links
| Feature | Description |
|---------|-------------|
| Phone | `tel:` → dialer |
| WhatsApp | `whatsapp://` → WhatsApp |
| Email | `mailto:` → Gmail |
| SMS | `sms:` → messaging |
| Maps | `geo:` → Google Maps |

### 📊 JS API (Auto-injected)
| Variable / Bridge | Value |
|-------------------|-------|
| `window.__IS_APK__` | `true` |
| `window.__APK_VERSION__` | `"1.0"` |
| `window.__THEME__` | `"dark"` or `"light"` |
| `window.__BATTERY__` | `{level: 85, low: false}` |
| `AndroidDownload.save(b64, name, mime)` | Save file to Downloads |
| `AndroidNotification.show(title, body)` | Show notification |

---

## 🛠️ Build from Source

### Prerequisites

```bash
# Ubuntu/Debian
sudo apt install -y aapt android-sdk-build-tools android-sdk-platform-23 \
  dalvik-exchange default-jdk-headless zipalign apksigner
```

### Build

```bash
git clone https://github.com/YOUR_USERNAME/webview-app-Apk-tester.git
cd webview-app-Apk-tester
chmod +x build.sh
./build.sh
```

APK will be at `build/AppTester.apk` (~57KB).

### Build manually

```bash
ANDROID_JAR=/usr/share/java/com.android.android-23.jar
DX=/usr/lib/android-sdk/build-tools/debian/dx

# 1. Package resources
aapt package -f -m -J gen/ -S res/ -M AndroidManifest.xml -I $ANDROID_JAR

# 2. Compile Java
javac -source 1.8 -target 1.8 -classpath $ANDROID_JAR \
  -sourcepath "src:gen" -d obj/ \
  src/com/krr/apptester/MainActivity.java gen/com/krr/apptester/R.java

# 3. Create DEX
$DX --dex --output=classes.dex obj/

# 4. Package APK
aapt package -f -M AndroidManifest.xml -S res/ -I $ANDROID_JAR -F unsigned.apk
aapt add unsigned.apk classes.dex

# 5. Sign
keytool -genkeypair -keystore debug.keystore -storepass android \
  -keypass android -alias debug -keyalg RSA -validity 10000 \
  -dname "CN=Debug" 2>/dev/null
zipalign -f 4 unsigned.apk aligned.apk
apksigner sign --ks debug.keystore --ks-pass pass:android \
  --key-pass pass:android --ks-key-alias debug --out AppTester.apk aligned.apk
```

---

## 📂 Project Structure

```
webview-app-Apk-tester/
├── src/
│   └── com/krr/apptester/
│       └── MainActivity.java      # All app logic (530 lines)
├── res/
│   ├── values/
│   │   └── strings.xml
│   └── drawable-*/
│       └── ic_launcher.png        # App icons (mdpi to xxxhdpi)
├── AndroidManifest.xml
├── build.sh                       # One-command build script
├── CONTRIBUTING.md
├── LICENSE
└── README.md
```

**Yes, the entire app is ONE Java file.** No Gradle, no Android Studio, no external dependencies. Pure Android SDK.

---

## 🤝 Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

### 🔥 Priority Issues (Help Wanted)

- [ ] **File Provider** — Tap-to-open notification on Android 7+ needs FileProvider
- [ ] **FCM Push Notifications** — Background push when app is closed (needs Gradle migration)
- [ ] **Bluetooth Printer** — ESC/POS thermal printer support for receipts
- [ ] **QR Scanner** — Launch camera as QR/barcode scanner from JS bridge
- [ ] **Per-app settings** — Different feature toggles for each saved app
- [ ] **Import/Export** — Share app list as JSON backup
- [ ] **Custom Icons** — Let users set per-app icons from gallery
- [ ] **Deep Links** — Handle `apptester://app/name` URLs
- [ ] **Auto-update checker** — Ping GitHub releases for new APK versions
- [ ] **Gradle build** — Migrate to Gradle for Play Store compatibility
- [ ] **Themed UI** — Material Design / dynamic colors on Android 12+

### Quick Wins (Good First Issues)

- [ ] Add app reordering (drag to sort)
- [ ] Add app search/filter bar
- [ ] Add long-press to copy URL
- [ ] Show favicon from website as app icon
- [ ] Add "share app" button (share URL via WhatsApp/etc)
- [ ] Dark/light mode for the app list screen itself
- [ ] Swipe to delete app card
- [ ] Add haptic feedback on button taps

---

## 🏗️ Architecture

```
MainActivity.java (530 lines)
│
├── Tab System
│   ├── Apps Tab — CRUD for saved web apps (SharedPreferences + JSON)
│   ├── Guide Tab — WebView compatibility reference with code examples
│   └── Settings Tab — 13 toggles + 2 sliders (SharedPreferences)
│
├── WebView Launcher
│   ├── Full-featured WebView with 24 capabilities
│   ├── JS bridges: AndroidDownload, AndroidNotification
│   ├── JS injection: cache clear, download intercept, refresh button
│   └── All features controlled by Settings toggles
│
└── Build System
    ├── javac 1.8 → dx → aapt → zipalign → apksigner
    ├── Compiles against API 23 (Android 6.0)
    ├── Targets API 34 (Android 14)
    └── Reflection for API 26+ features (NotificationChannel)
```

**Why one file?** This project compiles with raw Android SDK tools (aapt + javac + dx) — no Gradle, no build system. One file means one compilation unit, simple build script, easy to understand. Contributors can read the entire codebase in 10 minutes.

---

## 📋 Web App Compatibility

The Guide tab inside the app explains everything, but here's a summary:

### ✅ Works automatically
- Standard page navigation, forms, CSS, JS
- LocalStorage, SessionStorage, IndexedDB
- HTTP/HTTPS downloads
- Phone, email, SMS, WhatsApp links
- Camera file upload, GPS

### ⚠️ Needs code changes
| Feature | Problem | Fix |
|---------|---------|-----|
| Google Login | `signInWithPopup` blocked | Use `signInWithRedirect` |
| PDF download | `jsPDF .save()` uses blob | Use `AndroidDownload.save()` bridge |
| Excel download | `XLSX.writeFile()` uses blob | Use `XLSX.write()` + bridge |
| WhatsApp links | `https://wa.me/` may not open | Use `whatsapp://send?` |
| OAuth popup | `window.open()` blocked | Use `window.location.href` |

### ❌ Not supported
- Browser Push API / Web Push
- Service Worker push notifications
- Background sync

---

## 📊 Tech Specs

| Spec | Value |
|------|-------|
| APK Size | ~57KB |
| Min SDK | 23 (Android 6.0) |
| Target SDK | 34 (Android 14) |
| Java version | 1.8 |
| Build tools | aapt, dx, javac, apksigner |
| Dependencies | Zero — pure Android SDK |
| Classes | 27 |
| Source lines | 530 |
| Signing | v1 + v2 + v3 |

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

Free to use, modify, fork, sell, embed, or distribute. Attribution appreciated but not required.

---

## ⭐ Star History

If this project helps you, give it a star! It helps others find it.
