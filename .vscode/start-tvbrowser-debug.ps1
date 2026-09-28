param(
    [string]$TvBrowserJar = "C:\Program Files\TV-Browser\tvbrowser.jar",
    [int]$Port = 5005
)

if (-not (Test-Path -LiteralPath $TvBrowserJar)) {
    Write-Error "TV-Browser JAR nicht gefunden: $TvBrowserJar"
    exit 1
}

$installDir = Split-Path -Parent $TvBrowserJar
$classpath = "$TvBrowserJar;$installDir\lib\*"

Start-Process -FilePath "java" -WorkingDirectory $installDir -ArgumentList @(
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$Port",
    "-Dpropertiesfile=windows.properties",
    "-cp",
    "`"$classpath`"",
    "tvbrowser.TVBrowser"
)

$deadline = (Get-Date).AddSeconds(30)
while ((Get-Date) -lt $deadline) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $client.Connect("127.0.0.1", $Port)
        $client.Close()
        Write-Host "Debug-Port $Port ist bereit."
        exit 0
    } catch {
        Start-Sleep -Milliseconds 500
    } finally {
        $client.Dispose()
    }
}
Write-Error "Debug-Port $Port wurde nicht erreichbar."
exit 1
