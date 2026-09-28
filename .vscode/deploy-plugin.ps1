param(
    [string]$PluginDir = "$env:APPDATA\TV-Browser\4.2.7\plugins"
)

$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)

if (Get-Process -Name "tvbrowser*", "javaw", "java" -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowTitle -like "TV-Browser*" }) {
    Write-Error "TV-Browser laeuft noch. Bitte beenden, sonst ist die Plugin-JAR gesperrt."
}

mvn -B -q -DskipTests package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not (Test-Path -LiteralPath $PluginDir)) {
    Write-Error "Plugin-Ordner nicht gefunden: $PluginDir"
}

Copy-Item -LiteralPath "target/TvHBrowser.jar" -Destination (Join-Path $PluginDir "TvHBrowser.jar") -Force
Write-Host "Deployed nach $PluginDir"
