#!/bin/bash
# WebView App Tester — Build Script
# Builds the APK using raw Android SDK tools (no Gradle)

set -e

ANDROID_JAR="/usr/share/java/com.android.android-23.jar"
DX="/usr/lib/android-sdk/build-tools/debian/dx"
PKG_PATH="com/krr/apptester"
KEYSTORE="debug.keystore"
ALIAS="debug"
PASS="android"

echo "══════════════════════════════════════════"
echo "  📦 WebView App Tester — Build"
echo "══════════════════════════════════════════"

# Check tools
for tool in aapt javac zipalign apksigner; do
    if ! command -v $tool &>/dev/null; then
        echo "❌ $tool not found. Run:"
        echo "   sudo apt install aapt android-sdk-build-tools android-sdk-platform-23 dalvik-exchange default-jdk-headless zipalign apksigner"
        exit 1
    fi
done
if [ ! -f "$DX" ]; then echo "❌ dx not found at $DX"; exit 1; fi
if [ ! -f "$ANDROID_JAR" ]; then echo "❌ android.jar not found at $ANDROID_JAR"; exit 1; fi
echo "  ✓ All tools found"

# Clean
rm -rf gen obj build
mkdir -p gen obj build

# Generate keystore if not exists
if [ ! -f "$KEYSTORE" ]; then
    echo "  Generating debug keystore..."
    keytool -genkeypair -keystore $KEYSTORE -storepass $PASS -keypass $PASS \
        -alias $ALIAS -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Debug,OU=Dev,O=Debug,L=Debug,ST=Debug,C=US" 2>/dev/null
fi

# 1. Package resources
echo "  [1/5] Packaging resources..."
aapt package -f -m -J gen/ -S res/ -M AndroidManifest.xml -I $ANDROID_JAR

# 2. Compile Java
echo "  [2/5] Compiling Java..."
javac -source 1.8 -target 1.8 -classpath $ANDROID_JAR \
    -sourcepath "src:gen" -d obj/ \
    src/$PKG_PATH/MainActivity.java gen/$PKG_PATH/R.java
CLASSES=$(find obj -name "*.class" | wc -l)
echo "         $CLASSES classes compiled"

# 3. Create DEX
echo "  [3/5] Creating DEX..."
$DX --dex --output=classes.dex obj/

# 4. Package APK
echo "  [4/5] Packaging APK..."
aapt package -f -M AndroidManifest.xml -S res/ -I $ANDROID_JAR -F build/unsigned.apk
aapt add build/unsigned.apk classes.dex >/dev/null

# 5. Sign
echo "  [5/5] Signing APK..."
zipalign -f 4 build/unsigned.apk build/aligned.apk
apksigner sign --ks $KEYSTORE --ks-pass pass:$PASS --key-pass pass:$PASS \
    --ks-key-alias $ALIAS --out build/AppTester.apk build/aligned.apk

# Cleanup
rm -f build/unsigned.apk build/aligned.apk classes.dex
rm -rf gen obj

SIZE=$(du -h build/AppTester.apk | cut -f1)
echo ""
echo "══════════════════════════════════════════"
echo "  ✅ Build successful!"
echo "  📦 build/AppTester.apk ($SIZE)"
echo "══════════════════════════════════════════"
echo ""
echo "  Install: adb install -r build/AppTester.apk"
echo "  Or transfer APK to phone and install manually"
