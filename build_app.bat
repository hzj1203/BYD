@echo off
echo 构建比亚迪自动控车APP
echo ======================

echo 1. 检查Android SDK...
if "%ANDROID_HOME%"=="" (
    echo 错误: 请设置ANDROID_HOME环境变量
    echo 下载Android SDK: https://developer.android.com/studio#downloads
    pause
    exit /b 1
)

echo 2. 检查Java...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 请安装Java JDK 11或更高版本
    echo 下载: https://adoptium.net/
    pause
    exit /b 1
)

echo 3. 安装依赖...
call gradlew.bat build

echo 4. 构建APK...
call gradlew.bat assembleDebug

echo.
echo 构建完成！
echo APK文件位置: app\build\outputs\apk\debug\app-debug.apk
echo.
echo 安装到手机:
echo adb install -r app\build\outputs\apk\debug\app-debug.apk
echo.
pause