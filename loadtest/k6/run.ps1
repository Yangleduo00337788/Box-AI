param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('smoke', 'auth', 'workspace', 'mixed', 'chat', 'chat-sse', 'rag', 'published', 'register', 'workflow', 'embed', 'chat-tool', 'chat-mcp')]
    [string]$Scenario,
    [string]$EnvFile = (Join-Path $PSScriptRoot '.env'),
    [string]$Profile = '',
    [int]$Vus = 0,
    [string]$Duration = '',
    [string]$OutDir = (Join-Path $PSScriptRoot 'results')
)

$ErrorActionPreference = 'Stop'
if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
    Write-Error 'k6 is not on PATH. Install from https://k6.io/docs/get-started/installation/'
}

$k6Args = @('run')
if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) { return }
        $eq = $line.IndexOf('=')
        if ($eq -lt 1) { return }
        $key = $line.Substring(0, $eq).Trim()
        $value = $line.Substring($eq + 1).Trim()
        if ($value.StartsWith('"') -and $value.EndsWith('"')) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $k6Args += @('-e', "$key=$value")
    }
}

if ($Profile) {
    $k6Args += @('-e', "PROFILE=$Profile")
}
if ($Vus -gt 0) {
    $k6Args += @('-e', "VUS=$Vus")
}
if ($Duration) {
    $k6Args += @('-e', "DURATION=$Duration")
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$summary = Join-Path $OutDir "$Scenario-$stamp.json"
$k6Args += @(
    '--summary-trend-stats', 'min,avg,med,p(90),p(95),p(99),max',
    '--summary-export', $summary
)
$k6Args += (Join-Path $PSScriptRoot "scenarios\$Scenario.js")

Write-Host "k6 $($k6Args -join ' ')"
& k6 @k6Args
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
Write-Host "summary: $summary"
