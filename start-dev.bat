@echo off
title VCogniRule Development Environment

REM ============================================================
REM VCogniRule - Development Environment Setup
REM ============================================================
REM This script auto-detects JDK 21 and Apache Maven from the
REM folder NEXT TO this project (one level up from this script).
REM
REM Expected folder structure:
REM   <some-folder>/
REM   ├── jdk21/              <- JDK 21 (ZIP version, extracted)
REM   ├── apache-maven/       <- Apache Maven (ZIP version, extracted)
REM   └── VCogniRule/         <- This project (cloned)
REM       ├── start-dev.bat   <- You are here
REM       └── pom.xml
REM
REM If the folders aren't found, falls back to system PATH.
REM ============================================================

REM --- Resolve parent directory (one level up from this script) ---
for %%I in ("%~dp0..") do set "PARENT_DIR=%%~fI"

echo [Setup] Looking for JDK and Maven in: %PARENT_DIR%
echo.

REM --- Find JDK (checks "jdk21" first, then any "jdk*" folder) ---
set "JAVA_HOME="
if exist "%PARENT_DIR%\jdk21\bin\java.exe" (
    set "JAVA_HOME=%PARENT_DIR%\jdk21"
) else (
    for /d %%D in ("%PARENT_DIR%\jdk*") do (
        if exist "%%D\bin\java.exe" set "JAVA_HOME=%%D"
    )
)

if defined JAVA_HOME (
    echo [Setup] Found JDK: %JAVA_HOME%
    set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
    echo [Setup] JDK not found in parent folder - trying system PATH...
)

REM --- Find Maven (checks "apache-maven" first, then any "apache-maven*") ---
set "MAVEN_HOME="
if exist "%PARENT_DIR%\apache-maven\bin\mvn.cmd" (
    set "MAVEN_HOME=%PARENT_DIR%\apache-maven"
) else (
    for /d %%D in ("%PARENT_DIR%\apache-maven*") do (
        if exist "%%D\bin\mvn.cmd" set "MAVEN_HOME=%%D"
    )
)

if defined MAVEN_HOME (
    echo [Setup] Found Maven: %MAVEN_HOME%
    set "PATH=%MAVEN_HOME%\bin;%PATH%"
) else (
    echo [Setup] Maven not found in parent folder - trying system PATH...
)

echo.
echo ========================================
echo   VCogniRule Development Environment
echo ========================================
echo.

echo [1/2] Checking Java...
java -version
if errorlevel 1 goto error

echo.
echo [2/2] Checking Maven...
call mvn -version
if errorlevel 1 goto error

echo.
echo Environment ready.
echo Project: %CD%
echo.

REM --- Open VS Code if available ---
where code >nul 2>&1
if not errorlevel 1 (
    echo Opening VS Code...
    start code .
) else (
    echo VS Code not found in PATH - skipping.
)

echo.
echo ========================================
echo  Terminal is ready. Type commands here.
echo  Example:  mvn spring-boot:run
echo  Then open: http://localhost:8080
echo ========================================
echo.
cmd /k
exit /b

:error
echo.
echo A setup command failed. Check the error above.
echo.
echo If Java or Maven is not found:
echo   1. Download JDK 21 ZIP from https://adoptium.net/
echo   2. Download Apache Maven ZIP from https://maven.apache.org/download.cgi
echo   3. Extract both ZIPs in the folder NEXT TO this project
echo   4. Name the JDK folder "jdk21" and the Maven folder "apache-maven"
echo   5. Run this script again
echo.
pause