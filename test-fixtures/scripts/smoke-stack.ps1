#Requires -Version 5.1
param(
    [string]$BaseUrl = $env:BOX_SMOKE_BASE_URL
)

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = "http://127.0.0.1:8080"
}
$BaseUrl = $BaseUrl.TrimEnd("/")
$uri = "$BaseUrl/api/v1/system/health"

Write-Host "Smoke: GET $uri"

try {
    $resp = Invoke-RestMethod -Uri $uri -Method Get -TimeoutSec 15
} catch {
    Write-Error "Health request failed: $_"
}

if ($null -eq $resp -or $resp.code -ne 0) {
    Write-Error "Unexpected API envelope: $($resp | ConvertTo-Json -Compress)"
}

$data = $resp.data
$mysql = $data.mysql
$redis = $data.redis
$status = $data.status

Write-Host "status=$status mysql=$mysql redis=$redis elasticsearch=$($data.elasticsearch) minio=$($data.minio)"

if ($mysql -ne $true -or $redis -ne $true) {
    Write-Error "Health DEGRADED: mysql and redis must be true for release gate"
}

Write-Host "Stack smoke: PASSED" -ForegroundColor Green
