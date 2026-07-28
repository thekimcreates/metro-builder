@echo off
setlocal
set GRADLE_VERSION=8.6
set APP_HOME=%~dp0
set CACHE_DIR=%APP_HOME%.gradle-bootstrap
set DIST_DIR=%CACHE_DIR%\gradle-%GRADLE_VERSION%
set ZIP_FILE=%CACHE_DIR%\gradle-%GRADLE_VERSION%-bin.zip
set REQUIRED_JAR=%DIST_DIR%\lib\plugins\gradle-diagnostics-%GRADLE_VERSION%.jar

if not exist "%DIST_DIR%\bin\gradle.bat" goto install
if not exist "%REQUIRED_JAR%" goto install
goto run

:install
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
if not exist "%CACHE_DIR%" mkdir "%CACHE_DIR%"
echo Downloading Gradle %GRADLE_VERSION%...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP_FILE%'"
if errorlevel 1 exit /b 1
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP_FILE%' '%CACHE_DIR%'"
if not exist "%REQUIRED_JAR%" (
  echo Error: Gradle extraction was incomplete. Delete .gradle-bootstrap and retry.
  exit /b 1
)

:run
call "%DIST_DIR%\bin\gradle.bat" --no-daemon %*
