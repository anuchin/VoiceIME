# VoiceIME - Build Instructions

## Quick Start (Recommended)

### Option 1: GitHub Actions Build (Easiest)
1. Push this project to GitHub
2. Go to Actions tab → Select "Build Android APK" → Run workflow
3. Download the built APK from the artifacts section

### Option 2: Build Locally with Docker
```bash
# Build using Docker (no Android SDK installation needed)
docker build -t voiceime-builder -f Dockerfile.build .
docker run --rm -v $(pwd):/workspace voiceime-builder
```

### Option 3: Manual Setup on Linux/Mac

#### Prerequisites
```bash
# Install JDK 17
sudo apt install openjdk-17-jdk  # Ubuntu/Debian
brew install openjdk@17          # macOS

# Install Android SDK command line tools
mkdir -p ~/android-sdk
cd ~/android-sdk
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip cmdline-tools.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/
```

#### Environment Variables
Add to your `~/.bashrc` or `~/.zshrc`:
```bash
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64  # adjust for macOS
```

#### Install Required SDK Components
```bash
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
yes | sdkmanager --licenses
```

#### Build the APK
```bash
cd VoiceIME
./gradlew assembleDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## Install on Device

```bash
# Enable USB debugging on your device
# Connect via USB and authorize

./gradlew installDebug

# Or manually install the APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Enable the Keyboard

1. Go to Settings → System → Languages & input → On-screen keyboard
2. Enable "VoiceIME"
3. Switch to VoiceIME when typing
