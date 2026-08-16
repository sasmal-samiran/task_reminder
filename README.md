# MyReminder — Personal Interview Reminder App

A fully offline Android app to schedule and get reminded about your upcoming interviews, exams, and placement tasks.

**Built with:** Kotlin • Jetpack Compose • Room Database • Material 3 • AlarmManager • Exact Notifications

---

## Key Features

- ✅ **Add, Edit, Delete, Complete Tasks**: Track interviews, coding tests, aptitude tests, HR rounds, exams, and deadlines.
- ✅ **Morning Daily Summary (7:00 AM)**: Daily morning summary notification of the day's tasks (configurable in Settings).
- ✅ **Per-Task Reminders**: Exact alarms (At time / 5 min / 15 min / 30 min / 1 hour / 1 day before).
- ✅ **Meeting Link One-Tap Join**: Direct `[ Join Interview ]` button that opens Zoom/Google Meet/Teams links in browser.
- ✅ **Monthly Calendar**: Visual monthly calendar with dot markers on days containing tasks.
- ✅ **Search & Instant Filters**: Search by task or company name, filter by Interviews, Coding Tests, Deadlines, Completed.
- ✅ **Priority Visual Indicators**: Clean Material 3 indicators (🔴 High, 🟡 Medium, 🟢 Low, ✓ Completed).
- ✅ **Survives Reboots & Screen Locks**: `BootReceiver` automatically restores all alarms after phone restart.
- ✅ **100% Offline & Private**: Zero internet dependencies, no accounts, no cloud backend, stored strictly in local Room DB.

---

## Release APK Generation & Build Options

The project includes an official **Release Signing Configuration** with the pre-generated keystore (`app/release-keystore.jks`).

### Option 1: One-Click Build on Windows (Local)
If you have the Android SDK on your PC or `ANDROID_HOME` configured:
1. Double-click **[`build-apk.bat`](file:///d:/my_projects/myreminder/build-apk.bat)** in the root folder.
2. It automatically compiles and places the signed **`MyReminder.apk`** directly into the project root directory:
   ```
   d:\my_projects\myreminder\MyReminder.apk
   ```

### Option 2: Automated Cloud Build via GitHub Actions (Zero Local Setup)
If you do not have Android Studio or the Android SDK installed on your computer:
1. Push this project to your GitHub repository (or create a private repository).
2. The included workflow [`.github/workflows/build-apk.yml`](file:///d:/my_projects/myreminder/.github/workflows/build-apk.yml) will automatically trigger.
3. Go to the **Actions** tab on GitHub → Click the latest run → Download **`MyReminder-Signed-APK`**.
4. You will get the ready-to-install `MyReminder.apk`.

### Option 3: Terminal / CLI
Run the standard Gradle assemble command:
```bash
# Windows
gradlew.bat assembleRelease

# Mac / Linux
./gradlew assembleRelease
```
The output APK is generated at:
```
app/build/outputs/apk/release/MyReminder.apk
```

---

## Installation on Your Android Phone

1. **Transfer the APK**: Copy `MyReminder.apk` to your phone via USB cable, Google Drive, WhatsApp/Telegram saved messages, or Bluetooth.
2. **Open the APK**: Tap `MyReminder.apk` on your phone's file manager or downloads.
3. **Allow Installation**: If prompted, tap *Settings* and enable *Allow from this source* (standard for APKs installed outside Google Play).
4. **Install & Open**: Tap *Install*, then open **MyReminder**.

---

## Essential Permissions & Setup

### 1. Notification Permission (Android 13+)
* On the first launch, the app prompts for **Notification Permission**.
* Tap **Allow** so task reminders and the 7:00 AM morning brief can pop up.

### 2. Exact Alarms (Android 12+)
* Required for precise minute-by-minute reminders.
* Go to: **Settings → Apps → MyReminder → Alarms & reminders → Allow**.

### 3. Battery Optimization (Crucial for Reliable Alarms)
To prevent Android's aggressive battery savers from delaying alarms when the phone is asleep:
* **Stock Android / Pixel / Motorola**:
  * Settings → Apps → MyReminder → App battery usage → Select **Unrestricted**.
* **Samsung (One UI)**:
  * Settings → Device care → Battery → Background usage limits → Add MyReminder to **Never sleeping apps**.
* **Xiaomi / Redmi / POCO (MIUI / HyperOS)**:
  * Settings → Apps → Manage apps → MyReminder → Enable **Autostart** → Battery saver → **No restrictions**.
* **OnePlus / Realme / Oppo (OxygenOS / ColorOS)**:
  * Settings → Battery → More settings → App battery management → MyReminder → Allow background activity.
* **Vivo / iQOO (Funtouch OS)**:
  * Settings → Battery → High background power consumption → Enable for MyReminder.

---

## Project Structure & Architecture

```
d:\my_projects\myreminder\
├── build-apk.bat                      # One-click Windows release APK builder
├── .github/workflows/build-apk.yml    # Automated cloud build workflow
├── app/
│   ├── release-keystore.jks           # Release signing keystore
│   ├── build.gradle.kts               # Android & Compose configuration (SDK 26–35)
│   └── src/main/
│       ├── AndroidManifest.xml        # Permissions, receivers, launcher
│       └── java/com/myreminder/app/
│           ├── MainActivity.kt        # Edge-to-edge Compose entry & notification deep link
│           ├── MyReminderApp.kt       # Application class (Notification channels init)
│           ├── data/
│           │   ├── local/             # Room SQLite DB, TaskEntity, TaskDao, Converters, SettingsDataStore
│           │   └── model/             # TaskType, Priority, ReminderOption enums
│           ├── notification/          # AlarmScheduler, AlarmReceiver, MorningSummaryReceiver, BootReceiver
│           └── ui/
│               ├── theme/             # Material 3 typography, green palette, light/dark themes
│               ├── components/        # TaskCard, PriorityIndicator, FilterChips, EmptyState
│               ├── navigation/        # AppNavigation, Routes, backstack handling
│               └── screens/           # HomeScreen, AddEditScreen, TaskDetailScreen, CalendarScreen, SearchScreen, SettingsScreen
```

---

## Release Keystore Credentials (For Reference)

* **Keystore File**: `app/release-keystore.jks`
* **Keystore Password**: `reminder123`
* **Key Alias**: `myreminder`
* **Key Password**: `reminder123`
