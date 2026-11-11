param(
    [ValidateSet('build', 'run', 'test')]
    [string]$Task = 'build'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $java = 'java'
    $javac = 'javac'
    if ($env:JAVA_HOME) {
        $java = Join-Path $env:JAVA_HOME 'bin/java.exe'
        $javac = Join-Path $env:JAVA_HOME 'bin/javac.exe'
    }
    New-Item -ItemType Directory -Force 'build/classes' | Out-Null
    $sources = @(Get-ChildItem -LiteralPath 'src' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    & $javac -encoding UTF-8 --release 21 -d build/classes $sources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
    if ($Task -eq 'run') {
        & $java '-Dfile.encoding=UTF-8' -cp build/classes aeronpcd.driver.AirportDriver
        if ($LASTEXITCODE -ne 0) { throw 'Simulation failed.' }
    } elseif ($Task -eq 'test') {
        & $java '-Dfile.encoding=UTF-8' '-Djava.awt.headless=true' -cp build/classes aeronpcd.test.TestRunner
        if ($LASTEXITCODE -ne 0) { throw 'Tests failed.' }
    }
} finally {
    Pop-Location
}
