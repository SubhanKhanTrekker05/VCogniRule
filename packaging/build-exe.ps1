# ============================================================
# VCogniRule - build a double-click Windows app (VCogniRule.exe)
# ============================================================
# Output: dist\VCogniRule\VCogniRule.exe plus its own Java runtime,
# so the target PC does not need Java installed.
#
# Double-clicking VCogniRule.exe opens a console window, starts the app
# and opens http://localhost:8080 in the default browser.
# Closing the console window stops the app.
# The database (vcognirule.db) is created inside dist\VCogniRule\app.
#
# Needs JDK 21 (for jpackage) and Maven. Run from the project folder:
#   powershell -ExecutionPolicy Bypass -File packaging\build-exe.ps1
# Add -SkipTests to skip the JUnit tests.
# ============================================================
param([switch]$SkipTests)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# Use JDK 21 / Maven from the parent folder when they are there (portable setup),
# otherwise whatever is on PATH.
$parent = Split-Path -Parent $root
if (Test-Path "$parent\jdk21\bin\jpackage.exe") { $env:JAVA_HOME = "$parent\jdk21" }
if ($env:JAVA_HOME) { $env:Path = "$env:JAVA_HOME\bin;$env:Path" }
if (Test-Path "$parent\apache-maven\bin\mvn.cmd") { $env:Path = "$parent\apache-maven\bin;$env:Path" }

Write-Host '1/3  Building the application jar with Maven...'
$mvnArgs = @('-q', 'clean', 'package')
if ($SkipTests) { $mvnArgs += '-DskipTests' }
& mvn @mvnArgs
if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }

$jar = Get-ChildItem target\vcognirule-*.jar | Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
$jpkgInput = Join-Path $root 'target\jpackage-input'
Remove-Item -Recurse -Force $jpkgInput -ErrorAction SilentlyContinue
New-Item -ItemType Directory $jpkgInput | Out-Null
Copy-Item $jar.FullName $jpkgInput

Write-Host '2/3  Creating dist\VCogniRule with jpackage...'
$dist = Join-Path $root 'dist'
Remove-Item -Recurse -Force (Join-Path $dist 'VCogniRule') -ErrorAction SilentlyContinue
& jpackage --type app-image --name VCogniRule --app-version 1.0.0 `
    --vendor 'VCogniRule team (NED University)' `
    --input $jpkgInput --main-jar $jar.Name --dest $dist `
    --win-console `
    --java-options '-Dvcognirule.open-browser=true' `
    --java-options '-Dspring.profiles.active=prod' `
    --java-options '-Dspring.datasource.url=jdbc:sqlite:$APPDIR/vcognirule.db'
if ($LASTEXITCODE -ne 0) { throw 'jpackage failed.' }

Write-Host '3/3  Zipping dist\VCogniRule...'
$zip = Join-Path $dist 'VCogniRule-windows.zip'
Remove-Item $zip -ErrorAction SilentlyContinue
# Windows' own tar.exe (System32, Windows 10+) writes the zip; retry briefly in case
# antivirus is still scanning the freshly created runtime files.
# (Run from inside dist: tar reads "D:" in a full path as a remote host name.)
for ($try = 1; $try -le 5; $try++) {
    Push-Location $dist
    & (Join-Path $env:SystemRoot 'System32\tar.exe') -a -c -f 'VCogniRule-windows.zip' 'VCogniRule'
    $ok = ($LASTEXITCODE -eq 0)
    Pop-Location
    if ($ok) { break }
    Remove-Item $zip -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
}
if (-not (Test-Path $zip)) { throw 'Could not create the zip.' }

Write-Host ''
Write-Host "Done: $dist\VCogniRule\VCogniRule.exe"
Write-Host "      $zip (copy this to another PC and unzip)"
