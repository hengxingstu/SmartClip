param(
    [string]$JarPath = "",
    [string]$OutputDir = ".\app\runtime\win-jre",
    [string]$JdkHome = $env:JAVA_HOME
)

$ErrorActionPreference = "Stop"

if (-not $JdkHome) {
    throw "JAVA_HOME is not set. Please point it to JDK 17."
}

if (-not $JarPath) {
    $targetDir = Resolve-Path "..\target" -ErrorAction Stop
    $jarCandidate = Get-ChildItem $targetDir -Filter "*.jar" |
        Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*.original" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $jarCandidate) {
        throw "No backend jar was found in ..\target. Run mvn package first or pass -JarPath."
    }

    $JarPath = $jarCandidate.FullName
}

$jarPathResolved = Resolve-Path $JarPath -ErrorAction Stop
$outputDirResolved = Join-Path (Get-Location) $OutputDir
$workDir = Join-Path $env:TEMP ("smartclip-jre-" + [guid]::NewGuid().ToString("N"))

New-Item -ItemType Directory -Force -Path $workDir | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path $outputDirResolved -Parent) | Out-Null

$jarExe = Join-Path $JdkHome "bin\jar.exe"
$jdepsExe = Join-Path $JdkHome "bin\jdeps.exe"
$jlinkExe = Join-Path $JdkHome "bin\jlink.exe"

if (-not (Test-Path $jarExe)) {
    throw "jar.exe was not found: $jarExe"
}
if (-not (Test-Path $jdepsExe)) {
    throw "jdeps.exe was not found: $jdepsExe"
}
if (-not (Test-Path $jlinkExe)) {
    throw "jlink.exe was not found: $jlinkExe"
}

try {
    Push-Location $workDir
    & $jarExe xf $jarPathResolved
    Pop-Location

    $libDir = Join-Path $workDir "BOOT-INF\lib"
    $classesDir = Join-Path $workDir "BOOT-INF\classes"

    if (-not (Test-Path $classesDir)) {
        throw "BOOT-INF\classes was not found. This script expects an executable Spring Boot jar."
    }

    $classpath = ""
    if (Test-Path $libDir) {
        $classpath = ((Get-ChildItem $libDir -Filter *.jar | ForEach-Object { $_.FullName }) -join ';')
    }

    $jdepsArgs = @(
        "--ignore-missing-deps",
        "--recursive",
        "--multi-release", "17",
        "--print-module-deps"
    )

    if ($classpath) {
        $jdepsArgs += @("--class-path", $classpath)
    }

    $jdepsArgs += $classesDir
    $moduleDeps = (& $jdepsExe @jdepsArgs | Out-String).Trim()

    if (-not $moduleDeps) {
        throw "jdeps could not determine the required modules."
    }

    $modules = $moduleDeps
    if ($modules -notmatch "(^|,)java.desktop(,|$)") {
        $modules = "$modules,java.desktop"
    }
    if ($modules -notmatch "(^|,)jdk.unsupported(,|$)") {
        $modules = "$modules,jdk.unsupported"
    }

    if (Test-Path $outputDirResolved) {
        Remove-Item -LiteralPath $outputDirResolved -Recurse -Force
    }

    & $jlinkExe `
        --add-modules $modules `
        --strip-debug `
        --no-header-files `
        --no-man-pages `
        --compress=2 `
        --output $outputDirResolved

    Write-Host "Slim JRE created at: $outputDirResolved"
    Write-Host "Modules: $modules"
}
finally {
    if (Get-Location) {
        Set-Location (Split-Path -Parent $PSScriptRoot)
    }
    if (Test-Path $workDir) {
        Remove-Item -LiteralPath $workDir -Recurse -Force
    }
}
