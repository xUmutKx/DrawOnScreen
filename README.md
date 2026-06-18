# Draw On Screen

A free, open-source screen drawing overlay for Android. No ads, no premium gates.

## Features
- ✏️ Pen, Marker, Eraser, Line, Rectangle, Circle, Arrow tools
- 🎨 10-colour palette + stroke size slider
- ↩️ Unlimited undo / redo
- 👆 Stylus support — side button instantly switches to eraser
- 👁️ Pass-through mode — interact with apps behind your drawing
- 🪟 Draggable floating toolbar — stays out of your way
- 🌑 True AMOLED black Material 3 UI

---

## Build from Termux

### 1. Install prerequisites
```bash
pkg update && pkg upgrade -y
pkg install git openjdk-17 -y
```

### 2. Clone & enter project
```bash
git clone https://github.com/YOUR_USERNAME/DrawOnScreen.git
cd DrawOnScreen
```

### 3. Set ANDROID_HOME (Termux)
```bash
export ANDROID_HOME=$PREFIX/share/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

Or if you have Android SDK elsewhere:
```bash
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk
```

### 4. Build debug APK
```bash
chmod +x gradlew
./gradlew assembleDebug
```
APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### 5. Install directly
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
# or if on the same device:
am start -a android.intent.action.VIEW \
  -d file://$(pwd)/app/build/outputs/apk/debug/app-debug.apk \
  -t application/vnd.android.package-archive
```

---

## Push to GitHub (triggers Actions build)

```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/DrawOnScreen.git
git push -u origin main
```

GitHub Actions will automatically build both debug and release APKs.  
Download them from the **Actions** tab → your workflow run → **Artifacts**.

### Create a versioned release
```bash
git tag v1.0.0
git push origin v1.0.0
```
This triggers a GitHub Release with APKs attached.

---

## Permissions
| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw over other apps |
| `FOREGROUND_SERVICE` | Keep overlay alive |
| `VIBRATE` | Optional haptic feedback |

## License
MIT
