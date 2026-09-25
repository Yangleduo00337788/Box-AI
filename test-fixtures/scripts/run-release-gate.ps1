#Requires -Version 5.1
<#
.SYNOPSIS
  Box AI Phase 0 发布门禁：后端单测 + 前端 typecheck/test/build + 可选栈健康检查。

.EXAMPLE
  .\test-fixtures\scripts\run-release-gate.ps1
  $env:BOX_SMOKE_BASE_URL = "http://127.0.0.1:8080"
  .\test-fixtures\scripts\run-release-gate.ps1 -SkipBackend -SkipFrontend
#>
param(
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [switch]$SkipSmoke
)

$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")

function Write-Step($msg) {
    Write-Host ""
    Write-Host "==> $msg" -ForegroundColor Cyan
}

function Invoke-Step($name, [scriptblock]$block) {
    Write-Step $name
    & $block
    if ($LASTEXITCODE -ne 0 -and $null -ne $LASTEXITCODE) {
        throw "$name failed with exit code $LASTEXITCODE"
    }
}

Write-Host "Box AI Release Gate (Phase 0)" -ForegroundColor Green
Write-Host "Root: $Root"

if (-not $SkipBackend) {
    Invoke-Step "box-server: mvn test" {
        Push-Location (Join-Path $Root "box-server")
        try { mvn -B test } finally { Pop-Location }
    }
}

if (-not $SkipFrontend) {
    Invoke-Step "box-ui: npm ci + typecheck" {
        Push-Location (Join-Path $Root "box-ui")
        try {
            npm ci
            npm run typecheck
        } finally { Pop-Location }
    }
    Invoke-Step "box-web: npm ci + test + build" {
        Push-Location (Join-Path $Root "box-web")
        try {
            npm ci
            npm run test
            npm run build
        } finally { Pop-Location }
    }
    Invoke-Step "box-admin-web: npm ci + build" {
        Push-Location (Join-Path $Root "box-admin-web")
        try {
            npm ci
            npm run build
        } finally { Pop-Location }
    }
}

if (-not $SkipSmoke) {
    $baseUrl = $env:BOX_SMOKE_BASE_URL
    if ([string]::IsNullOrWhiteSpace($baseUrl)) {
        Write-Host ""
        Write-Host "Skip smoke: set BOX_SMOKE_BASE_URL to run stack health check." -ForegroundColor Yellow
    } else {
        & (Join-Path $PSScriptRoot "smoke-stack.ps1") -BaseUrl $baseUrl
    }
}

Write-Host ""
Write-Host "Phase 0 automated gate: PASSED" -ForegroundColor Green
Write-Host "Next: complete test-fixtures/RELEASE-GATE.md Phase 1-5 (manual)." -ForegroundColor Yellow
