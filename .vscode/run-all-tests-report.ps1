$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot

function Invoke-TestStep([string]$Name, [string]$WorkingDirectory, [string]$Command, [string[]]$Arguments) {
    Write-Host "`n>>> $Name" -ForegroundColor Cyan
    $started = Get-Date
    Push-Location $WorkingDirectory
    try {
        $output = (& $Command @Arguments 2>&1 | Out-String)
        $exitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }

    [PSCustomObject]@{
        Name = $Name
        Passed = ($exitCode -eq 0)
        Duration = ((Get-Date) - $started).ToString("mm\:ss")
        Output = $output
    }
}

$results = @(
    Invoke-TestStep "Backend tests" $root "mvn" @("test")
    Invoke-TestStep "Frontend tests" (Join-Path $root "frontend") "npm" @("test")
    Invoke-TestStep "OCR tests" (Join-Path $root "ocr-service") "python" @("-m", "pytest", "-q")
)

$reportDirectory = Join-Path $root "test-results"
New-Item -ItemType Directory -Force -Path $reportDirectory | Out-Null
$reportPath = Join-Path $reportDirectory "index.html"

$cards = foreach ($result in $results) {
    $status = if ($result.Passed) { "PASSED" } else { "FAILED" }
    $class = if ($result.Passed) { "passed" } else { "failed" }
    $output = [System.Net.WebUtility]::HtmlEncode($result.Output)
    @"
<article class="card $class">
  <h2>$([System.Net.WebUtility]::HtmlEncode($result.Name))</h2>
  <p class="status">$status <span>Duration: $($result.Duration)</span></p>
  <details>
    <summary>Show test output</summary>
    <pre>$output</pre>
  </details>
</article>
"@
}

$coverageLinks = @(
    @{ Name = "Backend coverage"; Path = "..\target\site\jacoco\index.html" },
    @{ Name = "Frontend coverage"; Path = "..\frontend\coverage\index.html" },
    @{ Name = "OCR coverage"; Path = "..\ocr-service\htmlcov\index.html" }
) | Where-Object { Test-Path (Join-Path $reportDirectory $_.Path) } |
    ForEach-Object { "<li><a href=""$($_.Path)"">$($_.Name)</a></li>" }

$overallStatus = if (@($results | Where-Object { -not $_.Passed }).Count -eq 0) {
    "All test suites passed"
} else {
    "One or more test suites failed"
}

$html = @"
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>SGRWallet test results</title>
  <style>
    body { font-family: Segoe UI, sans-serif; max-width: 1000px; margin: 2rem auto; padding: 0 1rem; color: #17352b; background: #f3f8f5; }
    h1 { margin-bottom: .4rem; }
    .summary { color: #5d746b; }
    .card { background: white; border-left: .45rem solid #999; border-radius: .6rem; padding: 1rem 1.25rem; margin: 1rem 0; box-shadow: 0 .25rem 1rem #164c3020; }
    .passed { border-color: #16834b; }
    .failed { border-color: #c53030; }
    .status { font-weight: 700; color: #16834b; }
    .failed .status { color: #c53030; }
    .status span { color: #5d746b; font-weight: 400; margin-left: 1rem; }
    pre { max-height: 28rem; overflow: auto; padding: 1rem; background: #17231e; color: #e7f5eb; border-radius: .4rem; white-space: pre-wrap; }
    a { color: #087f5b; }
  </style>
</head>
<body>
  <h1>SGRWallet test results</h1>
  <p class="summary">$overallStatus</p>
  $($cards -join "`n")
  <h2>Coverage reports</h2>
  <ul>
    $(if ($coverageLinks.Count -gt 0) { $coverageLinks -join "`n" } else { "<li>Run coverage first to generate reports.</li>" })
  </ul>
</body>
</html>
"@

Set-Content -Path $reportPath -Value $html -Encoding UTF8
Start-Process $reportPath
Write-Host "`nTest report opened: $reportPath" -ForegroundColor Green

if (@($results | Where-Object { -not $_.Passed }).Count -gt 0) {
    exit 1
}
