param(
    [ValidateSet('testDebugUnitTest', 'lintDebug', 'assembleDebug', 'assembleRelease', 'connectedDebugAndroidTest', 'verify')]
    [string]$Task = 'verify',
    [switch]$Install
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$projectRoot = Join-Path $repoRoot 'android\mneme'
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Users\Admin\AppData\Local\Programs\Java\jdk-21.0.13.1+2' }
$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"
$env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { 'C:\Android' }
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

$gradleArgs = @(
switch ($Task) {
    'verify' { @('testDebugUnitTest', 'lintDebug', 'assembleDebug') }
    default { @($Task) }
}
)
$gradleArgs += @('--no-daemon', '--console=plain')
$buildLog = Join-Path $env:TEMP "retenqo-$Task-build.log"

Push-Location $projectRoot
try {
    & .\gradlew.bat @gradleArgs *> $buildLog
    $gradleExitCode = $LASTEXITCODE
    if ($gradleExitCode -ne 0) {
        Write-Output "BUILD_FAILED task=$Task exit=$gradleExitCode log=$buildLog"
        Get-Content $buildLog -Tail 80
        exit $gradleExitCode
    }

    if ($Install -and $Task -eq 'assembleDebug') {
        $apk = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
        & adb install -r $apk
        if ($LASTEXITCODE -ne 0) { throw "ADB install failed with exit code $LASTEXITCODE" }
    }

    $jdkVersion = (& java -version 2>&1 | Select-Object -First 1).ToString().Trim()
    $buildSummary = Get-Content $buildLog | Where-Object { $_ -match '^BUILD SUCCESSFUL|^\d+ actionable tasks?' } | Select-Object -Last 2
    Write-Output "BUILD_OK task=$Task java=$jdkVersion"
    $buildSummary | ForEach-Object { Write-Output $_ }
    if ($Task -eq 'verify') {
        $testResults = Join-Path $projectRoot 'app\build\test-results\testDebugUnitTest'
        $lintReport = Join-Path $projectRoot 'app\build\reports\lint-results-debug.txt'
        $apk = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
        [xml[]]$suites = Get-ChildItem $testResults -Filter 'TEST-*.xml' | ForEach-Object { [xml](Get-Content $_.FullName) }
        $tests = ($suites | ForEach-Object { [int]$_.testsuite.tests } | Measure-Object -Sum).Sum
        $failures = ($suites | ForEach-Object { [int]$_.testsuite.failures + [int]$_.testsuite.errors } | Measure-Object -Sum).Sum
        $lintXml = Join-Path $projectRoot 'app\build\reports\lint-results-debug.xml'
        [xml]$lint = Get-Content $lintXml
        $issues = @($lint.issues.issue)
        $lintErrors = @($issues | Where-Object severity -eq 'Error').Count
        $lintWarnings = @($issues | Where-Object severity -eq 'Warning').Count
        $lintHints = @($issues | Where-Object severity -eq 'Hint').Count
        Write-Output "TESTS total=$tests failures=$failures"
        Write-Output "LINT errors=$lintErrors warnings=$lintWarnings hints=$lintHints"
        Write-Output "APK bytes=$((Get-Item $apk).Length)"
    }
} catch {
    Write-Error $_
    exit 1
} finally {
    Pop-Location
}
