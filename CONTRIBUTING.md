# Contributing to WebView App Tester

Thanks for your interest in contributing! This project is intentionally simple — one Java file, no Gradle, no dependencies. Let's keep it that way where possible.

## 🚀 Getting Started

### 1. Fork and clone

```bash
git clone https://github.com/YOUR_USERNAME/webview-app-Apk-tester.git
cd webview-app-Apk-tester
```

### 2. Set up build tools

```bash
# Ubuntu/Debian
sudo apt install -y aapt android-sdk-build-tools android-sdk-platform-23 \
  dalvik-exchange default-jdk-headless zipalign apksigner

# Verify
which aapt javac apksigner  # all should return paths
```

### 3. Build

```bash
chmod +x build.sh
./build.sh
```

### 4. Install on device

```bash
adb install -r build/AppTester.apk
```

Or transfer the APK to your phone and install manually.

## 📝 How to Contribute

### Bug fixes
1. Open an issue describing the bug
2. Fork → fix → test on a real device
3. Submit PR with before/after description

### New features
1. Check existing issues — it might already be planned
2. Open an issue to discuss the feature BEFORE coding
3. Keep it in one file if possible (MainActivity.java)
4. All features must be toggleable from Settings tab
5. Submit PR with screenshots/video

### Documentation
- Fix typos, improve explanations
- Add examples for JS bridge usage
- Translate README to other languages

## 🏗️ Code Guidelines

### The one-file rule
Everything lives in `MainActivity.java`. If your feature can be added here, add it here. Only create new files if absolutely necessary (like a FileProvider XML).

### Naming
- Methods: `camelCase`
- Constants: `UPPER_SNAKE`
- Settings keys: lowercase like `"biometric"`, `"refresh"`, `"darksync"`

### Settings
Every new feature that can be toggled:
1. Add a boolean field: `private boolean sMyFeature = true;`
2. Add to `loadSettings()`: `sMyFeature = sp.getBoolean("myfeature", true);`
3. Add toggle in `buildSettingsTab()`: `addToggle(c, "My Feature", "Description", sMyFeature, "myfeature");`
4. Check the setting before executing: `if (sMyFeature) { ... }`

### JS Injection
Keep injected JS minimal and minified in the Java string. Always:
- Guard with `if(window._flag) return;` to prevent double-injection
- Use `evaluateJavascript()` not `loadUrl("javascript:...")`

### Build compatibility
- Compile with `-source 1.8 -target 1.8`
- Must compile against API 23 JAR
- Use reflection for API 26+ features
- Never use lambdas or method references (Java 8 source, not Java 8 language features in Android)

## 🧪 Testing

Test on:
- A real Android device (emulators may not test WebView correctly)
- Multiple Android versions if possible (6.0, 10, 13, 14)
- Test with at least 3 different web apps

### Test checklist
- [ ] App compiles with `build.sh`
- [ ] App installs and opens
- [ ] Can add a web app
- [ ] Can launch a web app
- [ ] Can edit a web app
- [ ] Can delete a web app
- [ ] Settings toggles work
- [ ] Download bridge works (test with a site that has file downloads)
- [ ] Back button returns to app list
- [ ] Notification permission popup appears (Android 13+)

## 📋 PR Template

```
## What does this PR do?
Brief description

## Type
- [ ] Bug fix
- [ ] New feature
- [ ] Documentation
- [ ] Refactor

## Testing
- Tested on: [device name, Android version]
- Screenshots: [attach if UI change]

## Checklist
- [ ] Builds with ./build.sh
- [ ] No new files added (or justified why)
- [ ] Feature is toggleable in Settings
- [ ] Tested on real device
```

## 💬 Questions?

Open an issue with the "question" label. We're happy to help!
