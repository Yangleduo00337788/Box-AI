#Requires -Version 5.1
<#
.SYNOPSIS
  Phase 1 基础设施 + 可脚本化的存储/插件准备（不含浏览器与 LLM 对话验收）。

.EXAMPLE
  $env:BOX_SMOKE_BASE_URL = "http://127.0.0.1:8080"
  .\test-fixtures\scripts\run-automated-infra.ps1
#>
param(
    [string]$BaseUrl = $env:BOX_SMOKE_BASE_URL,
    [string]$DbHost = $(if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { "127.0.0.1" }),
    [int]$DbPort = $(if ($env:MYSQL_PORT) { [int]$env:MYSQL_PORT } else { 3306 }),
    [string]$DbUser = $(if ($env:MYSQL_USER) { $env:MYSQL_USER } else { "root" }),
    [string]$DbPassword = $(if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { "root" }),
    [string]$Database = $(if ($env:MYSQL_DATABASE) { $env:MYSQL_DATABASE } else { "box" }),
    [string]$RedisHost = "127.0.0.1",
    [int]$RedisPort = 6379,
    [string]$EsUrl = "http://127.0.0.1:9200",
    [string]$MinioHealthUrl = "http://127.0.0.1:9000/minio/health/live",
    [string]$WebUrl = "http://127.0.0.1:5173",
    [string]$AdminWebUrl = "http://127.0.0.1:5174",
    [switch]$SkipPluginSql
)

$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$passed = @()
$failed = @()

function Pass($id, $msg) {
    Write-Host "[PASS] $id $msg" -ForegroundColor Green
    $script:passed += $id
}
function Fail($id, $msg) {
    Write-Host "[FAIL] $id $msg" -ForegroundColor Red
    $script:failed += $id
}

function Invoke-MysqlQuery([string]$sql) {
    $env:MYSQL_PWD = $DbPassword
    $out = & mysql -h $DbHost -P $DbPort -u $DbUser -N -B -e $sql $Database 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($out -join "`n") }
    return $out
}

Write-Host "Box AI automated infra checks" -ForegroundColor Cyan

if ([string]::IsNullOrWhiteSpace($BaseUrl)) { $BaseUrl = "http://127.0.0.1:8080" }
$BaseUrl = $BaseUrl.TrimEnd("/")
$health = $null
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/api/v1/system/health" -TimeoutSec 15
} catch {
    Write-Host "WARN: could not fetch health from $BaseUrl" -ForegroundColor Yellow
}

# 1.1 MySQL + Flyway
try {
    $ping = Invoke-MysqlQuery "SELECT 1"
    if ($ping -ne "1") { throw "unexpected ping: $ping" }
    $failedMigrations = Invoke-MysqlQuery "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0"
    if ([int]$failedMigrations -gt 0) { throw "flyway_schema_history has $failedMigrations failed row(s)" }
    $latest = Invoke-MysqlQuery "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1"
    Pass "1.1" "MySQL database '$Database' OK; latest Flyway=$latest"
} catch {
    Fail "1.1" $_.Exception.Message
}

# 1.2 Redis
try {
    $redisOk = $false
    if (Get-Command redis-cli -ErrorAction SilentlyContinue) {
        $pong = & redis-cli -h $RedisHost -p $RedisPort ping 2>&1
        $redisOk = ($pong -eq "PONG")
    } else {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.Connect($RedisHost, $RedisPort)
        $redisOk = $tcp.Connected
        $tcp.Close()
    }
    if (-not $redisOk) { throw "Redis not reachable on ${RedisHost}:$RedisPort" }
    Pass "1.2" "Redis reachable"
} catch {
    Fail "1.2" $_.Exception.Message
}

# 1.3 Elasticsearch (prefer API health; plain HTTP only for compose-style ES)
try {
    if ($null -eq $health) {
        $health = Invoke-RestMethod -Uri "$BaseUrl/api/v1/system/health" -TimeoutSec 15
    }
    if ($health.data.elasticsearch -eq $true) {
        Pass "1.3" "Elasticsearch OK (health.elasticsearch=true)"
    } else {
        $es = Invoke-RestMethod -Uri $EsUrl -Method Get -TimeoutSec 10
        if ($null -eq $es.version.number) { throw "no ES version in response" }
        Pass "1.3" "Elasticsearch $($es.version.number) at $EsUrl"
    }
} catch {
    Fail "1.3" $_.Exception.Message
}

# 1.4 Object storage (health + optional local MinIO live)
try {
    if ($null -eq $health) { throw "health unavailable" }
    $minioFlag = $health.data.minio
    $minioLive = $false
    try {
        $r = Invoke-WebRequest -Uri $MinioHealthUrl -UseBasicParsing -TimeoutSec 5
        $minioLive = ($r.StatusCode -eq 200)
    } catch { $minioLive = $false }
    if ($minioFlag -ne $true -and -not $minioLive) {
        throw "health.minio=$minioFlag and MinIO health URL failed"
    }
    Pass "1.4" "Object storage OK (health.minio=$minioFlag, minioLive=$minioLive)"
} catch {
    Fail "1.4" $_.Exception.Message
}

# 1.5 Health mysql/redis
try {
    if ($null -eq $health) {
        $health = Invoke-RestMethod -Uri "$BaseUrl/api/v1/system/health" -TimeoutSec 15
    }
    if ($health.data.mysql -ne $true -or $health.data.redis -ne $true) {
        throw "mysql=$($health.data.mysql) redis=$($health.data.redis)"
    }
    Pass "1.5" "health mysql/redis true; status=$($health.data.status)"
} catch {
    Fail "1.5" $_.Exception.Message
}

# 1.6 HTTP endpoints
function Test-HttpOk($id, $url, $label) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 8 -MaximumRedirection 5
        if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 400) {
            Pass $id "$label HTTP $($r.StatusCode)"
        } else {
            Fail $id "$label HTTP $($r.StatusCode)"
        }
    } catch {
        Fail $id "$label $_"
    }
}
Test-HttpOk "1.6a" "$BaseUrl/api/v1/system/health" "box-server API"
Test-HttpOk "1.6b" $WebUrl "box-web"
Test-HttpOk "1.6c" $AdminWebUrl "box-admin-web"

# Storage schema (automated only)
try {
    $embCols = Invoke-MysqlQuery @"
SELECT COUNT(*) FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = '$Database' AND TABLE_NAME = 'knowledge_chunk'
  AND COLUMN_NAME LIKE '%embed%'
"@
    if ([int]$embCols -gt 0) { throw "knowledge_chunk has embedding-like columns: $embCols" }
    Pass "S-05" "knowledge_chunk has no embedding column in MySQL"
} catch {
    Fail "S-05" $_.Exception.Message
}

try {
    $cfg = Invoke-MysqlQuery "SELECT COUNT(*) FROM system_config WHERE config_key = 'platform.object_storage.settings'"
    if ([int]$cfg -lt 1) {
        Fail "S-06" "missing system_config platform.object_storage.settings (configure in admin if using R2)"
    } else {
        Pass "S-06" "system_config has platform.object_storage.settings"
    }
} catch {
    Fail "S-06" $_.Exception.Message
}

if ($health.data.redis -eq $true) {
    Pass "S-30" "health redis=true"
} else {
    Fail "S-30" "health redis not true"
}

# 5.6 partial: health + request id header on API
try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/api/v1/system/health" -UseBasicParsing -TimeoutSec 10
    $rid = $resp.Headers["X-Request-Id"]
    if ([string]::IsNullOrWhiteSpace($rid)) { $rid = $resp.Headers["x-request-id"] }
    if ([string]::IsNullOrWhiteSpace($rid)) {
        Fail "5.6" "health OK but no X-Request-Id response header (check filter config)"
    } else {
        Pass "5.6" "health OK; X-Request-Id present"
    }
} catch {
    Fail "5.6" $_.Exception.Message
}

# 4B prep: load plugin SQL
if (-not $SkipPluginSql) {
    try {
        if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
            Fail "4B-sql" "mysql client not in PATH; skip SQL load"
        } else {
            & (Join-Path $PSScriptRoot "load-conversation-plugins.ps1") -DbHost $DbHost -DbPort $DbPort -User $DbUser -Password $DbPassword -Database $Database
            Pass "4B-sql" "dev-insert-test-plugins.sql loaded"
        }
    } catch {
        Fail "4B-sql" $_.Exception.Message
    }
}

Write-Host ""
Write-Host "Summary: $($passed.Count) passed, $($failed.Count) failed" -ForegroundColor $(if ($failed.Count -eq 0) { "Green" } else { "Yellow" })
if ($failed.Count -gt 0) {
    Write-Host "Failed IDs: $($failed -join ', ')"
    exit 1
}
exit 0
