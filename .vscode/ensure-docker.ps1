$docker = Get-Command docker -ErrorAction SilentlyContinue
if ($null -eq $docker) {
    throw "Docker CLI was not found. Install Docker Desktop and restart VS Code."
}

docker info *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Host "Docker Desktop is already running."
    exit 0
}

$desktopPaths = @(
    (Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"),
    (Join-Path $env:LOCALAPPDATA "Docker\Docker Desktop.exe")
)
$desktopPath = $desktopPaths | Where-Object { Test-Path $_ } | Select-Object -First 1

if ($null -eq $desktopPath) {
    throw "Docker Desktop was not found. Start it manually or install it before running the project."
}

Write-Host "Starting Docker Desktop..."
Start-Process -FilePath $desktopPath

$deadline = (Get-Date).AddSeconds(120)
do {
    Start-Sleep -Seconds 2
    docker info *> $null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Docker Desktop is ready."
        exit 0
    }
} while ((Get-Date) -lt $deadline)

throw "Docker Desktop did not become ready within 120 seconds."
