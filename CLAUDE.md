# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application that displays daily Bible verses from jw.org. The app is primarily widget-based, showing daily Scripture text and Bible reading schedules directly on the home screen.

## Build & Development Commands

### Build
```bash
./gradlew assembleDebug    # Build debug APK
./gradlew assembleRelease  # Build release APK
```

### Install to Device
```bash
# USB debugging must be enabled on device
adb install -r app/build/outputs/apk/debug/daily_text.apk
```

### Run Tests
```bash
./gradlew test             # Run unit tests
./gradlew connectedAndroidTest  # Run instrumentation tests on connected device
```

## Android Emulator Setup

### Emulator Location & Commands
```bash
# Emulator executable location
~/Library/Android/sdk/emulator/emulator

# List available AVDs
~/Library/Android/sdk/emulator/emulator -list-avds

# Start emulator (run in background)
~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36 &

# Check connected devices (emulator shows as emulator-5554)
adb devices
```

### Available Emulators
- **Medium_Phone_API_36**: Main test emulator (API 36, production build - no root access)

### Installing APK to Emulator
```bash
# Install to specific emulator
adb -s emulator-5554 install -r app/build/outputs/apk/debug/daily_text-debug.apk

# View logs from emulator
adb -s emulator-5554 logcat | grep -E "DailyTextWidget|DateChangeReceiver"

# Clear logs
adb -s emulator-5554 logcat -c
```

### Testing Widget Updates
```bash
# Manually trigger midnight update broadcast (for testing)
adb -s emulator-5554 shell am broadcast -n com.example.daily_text/.DateChangeBroadcastReceiver -a com.example.daily_text.ACTION_UPDATE_DAILY

# Note: Must use explicit broadcast (-n flag) as implicit broadcasts don't work on Android 8.0+
```

### Test Mode for Alarm Testing
To test midnight alarms without waiting until midnight:
1. Set `TEST_MODE = true` in DailyTextWidgetProvider.kt companion object
2. Set `TEST_ALARM_MINUTES = 1` for 1-minute test alarms
3. Build and install APK
4. Add widget or trigger broadcast to schedule alarm
5. Wait 1 minute to see alarm trigger
6. **IMPORTANT**: Set `TEST_MODE = false` before production release

## Key Architecture

### Widget System
The app is centered around an Android home screen widget (`DailyTextWidgetProvider`):
- **Data Source**: Reads from `app/src/main/assets/daily_verses.json` and `bible_reading_schedule.json`
- **Date Navigation**: Users can navigate to previous/next dates via widget buttons
- **Automatic Updates**: Uses `AlarmManager` with `DateChangeBroadcastReceiver` to update widget at midnight
- **Shared Preferences**: Stores current date per widget instance using `PREFS_NAME = "DailyTextWidgetPrefs"`

### Core Components
1. **DailyTextWidgetProvider** (app/src/main/java/com/example/daily_text/DailyTextWidgetProvider.kt)
   - Main widget provider that renders daily verses and Bible reading schedules
   - Handles ACTION_PREV, ACTION_NEXT, ACTION_TODAY intents for navigation
   - Sets up midnight alarms for automatic date updates
   - Requires SCHEDULE_EXACT_ALARM permission on Android 12+

2. **DateChangeBroadcastReceiver** (app/src/main/java/com/example/daily_text/DateChangeBroadcastReceiver.kt)
   - Listens for date changes, timezone changes, boot events
   - Updates all widget instances to today's date
   - Re-schedules midnight alarm after receiving broadcasts

3. **DailyTextMainActivity** (app/src/main/java/com/example/daily_text/DailyTextMainActivity.kt)
   - Simple launcher activity for checking updates and managing alarm permissions
   - Directs users to GitHub releases page for manual APK updates

### Data Management
- **Daily Verses**: Stored in `app/src/main/assets/daily_verses.json` as array of objects with `date` (MM-DD), `title`, `reference`, `body`
- **Bible Reading Schedule**: Stored in `app/src/main/assets/bible_reading_schedule.json` with date keys mapping to `{day: int, reading: string}`
- **Data Update Process**: Use `parse_verses_to_json.py` to convert annual txt.zip files (e.g., `es25_KO.txt.zip`) into JSON format
  - Script automatically: unzips → parses txt → generates JSON → deletes txt files
  - Update ZIP_FILE variable in script for each year

### Permissions & Alarms
- Android 12+ requires explicit "Alarms & reminders" permission for exact alarms
- `scheduleMidnightUpdate()` checks `canScheduleExactAlarms()` and falls back to inexact alarms if permission denied
- Alarm is set at next midnight (00:00:00) using `setExactAndAllowWhileIdle()`

## Widget Interactions
- **Date label click**: Returns to today's date
- **Title click**: Opens jw.org daily text page for current date
- **Reading day (e.g., "4일차")**: Opens Bible reading schedule Google Sheet
- **Reading content (Bible chapters)**: Opens jw.org search with the reading range
- **< > buttons**: Navigate to previous/next dates

## CI/CD
GitHub Actions workflow (`.github/workflows/build-apk.yml`) automatically:
1. Builds debug and release APKs on push to main
2. Extracts version from `app/build.gradle`
3. Creates git tag and GitHub release if version is new
4. Uploads both APK variants to release

When bumping versions, update both `versionCode` and `versionName` in `app/build.gradle`:14-15.

## Development Notes
- Target SDK: 34, Min SDK: 21
- Kotlin 1.9.0, Gradle 8.2.0
- Uses Gson for JSON parsing, OkHttp for network requests, Kotlin Coroutines for async
- APK files are renamed to `daily_text-{buildType}.apk` via gradle configuration
- Widget layout supports HTML formatting for italic and colored text within verse body (especially for scripture references in parentheses)
