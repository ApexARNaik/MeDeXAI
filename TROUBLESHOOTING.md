# Health Chatbot - Troubleshooting Guide

## Issue: App crashes after clicking "Download Model" button during "Finalizing..." phase

### Root Causes Fixed:

1. **Missing Native Libraries**: The RunAnywhere SDK requires native (.so) libraries that weren't
   being properly packaged
2. **Race Conditions**: SDK initialization and download operations were happening too quickly
   without proper synchronization
3. **Network Configuration**: Missing network security configuration for HTTPS/HTTP downloads
4. **Resource Conflicts**: Conflicting META-INF files from different dependencies

### Fixes Applied:

#### 1. Updated Gradle Configuration (`app/build.gradle.kts`)

- Fixed deprecated `packagingOptions` → `packaging`
- Improved native library packaging with `pickFirsts` rules
- Added debug build type with proper debugging enabled

#### 2. Enhanced Network Configuration

- Added `network_security_config.xml` to allow cleartext traffic
- Added `ACCESS_NETWORK_STATE` permission
- Updated AndroidManifest.xml to reference network config

#### 3. Improved MainActivity Error Handling

- Added network connectivity checks before download
- Implemented proper SDK initialization sequence with delays
- Added timeout handling to prevent infinite hanging
- Enhanced logging for better debugging
- Added progress tracking with timeout detection

#### 4. Better SDK Initialization

- Added concurrent initialization prevention
- Implemented step-by-step initialization with delays
- Enhanced error reporting and logging

### How to Test the Fixes:

1. **Clean Build**:
   ```bash
   ./gradlew clean assembleDebug
   ```

2. **Install and Run**: Install the APK on your device/emulator

3. **Check Logs**: Use Android Studio's Logcat to monitor:
   ```
   Filter: "HealthChatbot"
   ```

4. **Test Steps**:
    - Open the app
    - Click "Download Model"
    - Monitor the status text changes:
        - "Preparing download..."
        - "Initializing..."
        - "Downloading model..."
        - "Downloading... X%"
        - "✅ Model downloaded successfully!"

### If Issues Persist:

#### Check Logcat for Specific Errors:

```bash
adb logcat | grep -E "(HealthChatbot|AndroidRuntime|FATAL)"
```

#### Common Solutions:

1. **Network Issues**:
    - Ensure device has internet connection
    - Check if corporate firewall blocks HuggingFace downloads
    - Try on different network (mobile data vs WiFi)

2. **Memory Issues**:
    - Close other apps to free memory
    - Restart the device
    - Use a device with more RAM (4GB+ recommended)

3. **Storage Issues**:
    - Ensure device has at least 1GB free space
    - Clear app data and try again

4. **SDK Issues**:
    - Check if the AAR files are properly included in `app/libs/`
    - Verify file sizes:
        - `RunAnywhereKotlinSDK-release-v0.1.3.aar` (~4MB)
        - `runanywhere-llm-llamacpp-release-v0.1.3.aar` (~2MB)

### Debug Commands:

```bash
# Clear logs and run app
adb logcat -c
adb logcat | grep HealthChatbot

# Check app info
adb shell dumpsys package com.example.healthchatbot

# Check available storage
adb shell df /data/data/com.example.healthchatbot
```

### Expected Log Output (Success):

```
D/HealthChatbot: Starting download process...
D/HealthChatbot: SDK already initialized, skipping
D/HealthChatbot: Fetching available models...
D/HealthChatbot: Found 1 models
D/HealthChatbot: Starting download for model: [model-id]
D/HealthChatbot: Download progress: 10%
...
D/HealthChatbot: Download progress: 100%
D/HealthChatbot: Download completed successfully
```

### Additional Notes:

- The app now has better error handling and won't crash silently
- Network connectivity is checked before attempting downloads
- SDK initialization has built-in delays to prevent race conditions
- Download progress includes timeout detection to prevent hanging
- All error messages are now displayed to the user with actionable information