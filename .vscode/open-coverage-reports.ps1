$root = Split-Path -Parent $PSScriptRoot
$reports = @(
    (Join-Path $root "target\site\jacoco\index.html"),
    (Join-Path $root "frontend\coverage\index.html"),
    (Join-Path $root "ocr-service\htmlcov\index.html")
)

$missing = @($reports | Where-Object { -not (Test-Path $_) })
if ($missing.Count -gt 0) {
    throw "Some reports are missing. Run 'Run all coverage and open reports' first: $($missing -join ', ')"
}

foreach ($report in $reports) {
    Start-Process $report
}

Write-Host "Coverage reports opened in your default browser."
