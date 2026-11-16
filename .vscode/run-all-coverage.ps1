$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function Invoke-Step([string]$WorkingDirectory, [string]$Command, [string[]]$Arguments) {
    Write-Host "`n>>> $Command $($Arguments -join ' ')" -ForegroundColor Cyan
    Push-Location $WorkingDirectory
    try {
        & $Command @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Command failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

Invoke-Step $root "mvn" @("verify")
Invoke-Step (Join-Path $root "frontend") "npm" @("run", "test:coverage")
Invoke-Step (Join-Path $root "ocr-service") "python" @(
    "-m", "pytest", "--cov=app", "--cov-report=term-missing", "--cov-report=html"
)

$reports = @(
    (Join-Path $root "target\site\jacoco\index.html"),
    (Join-Path $root "frontend\coverage\index.html"),
    (Join-Path $root "ocr-service\htmlcov\index.html")
)

foreach ($report in $reports) {
    if (-not (Test-Path $report)) {
        throw "Coverage report was not generated: $report"
    }
    Start-Process $report
}

Write-Host "`nCoverage reports opened in your default browser." -ForegroundColor Green
