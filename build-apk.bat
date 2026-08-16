@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo        MyReminder - Automated Release APK Builder
echo ========================================================
echo.

if not exist "%~dp0app\release-keystore.jks" (
    echo [ERROR] Keystore not found at app\release-keystore.jks!
    pause
    exit /b 1
)

echo [1/3] Checking environment...
if defined ANDROID_HOME (
    echo  - Android SDK found at: %ANDROID_HOME%
) else (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
        echo  - Detected Android SDK at: %LOCALAPPDATA%\Android\Sdk
    ) else (
        echo [WARNING] ANDROID_HOME is not set and not found in default location.
        echo If the build fails, please set ANDROID_HOME or install Android Command Line Tools / Android Studio.
    )
)

echo [2/3] Building signed release APK with Gradle...
call "%~dp0gradlew.bat" assembleRelease --no-daemon

if %ERRORLEVEL% equ 0 (
    echo.
    echo [3/3] Build SUCCESSFUL!
    if exist "%~dp0app\build\outputs\apk\release\MyReminder.apk" (
        copy /y "%~dp0app\build\outputs\apk\release\MyReminder.apk" "%~dp0MyReminder.apk" >nul
        echo ========================================================
        echo  [SUCCESS] Signed APK generated at:
        echo  %~dp0MyReminder.apk
        echo ========================================================
    ) else (
        echo  [SUCCESS] APK located in: app\build\outputs\apk\release\
    )
) else (
    echo.
    echo [ERROR] Build failed. Please inspect the logs above.
)

echo.
pause
