#Requires -Version 5.1
# 将 conversation-plugins 测试插件写入 MySQL（需 mysql 客户端在 PATH）
param(
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = $(if ($env:MYSQL_PORT) { [int]$env:MYSQL_PORT } else { 3306 }),
    [string]$User = "root",
    [string]$Password = $(if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { "root" }),
    [string]$Database = "box"
)

$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$Sql = Join-Path $Root "test-fixtures\conversation-plugins\dev-insert-test-plugins.sql"

if (-not (Test-Path $Sql)) {
    Write-Error "SQL not found: $Sql"
}

Write-Host "Loading plugins SQL into $Database @ ${DbHost}:$DbPort"
$env:MYSQL_PWD = $Password
Get-Content -Path $Sql -Raw -Encoding UTF8 | & mysql -h $DbHost -P $DbPort -u $User --default-character-set=utf8mb4 $Database
if ($LASTEXITCODE -ne 0) {
    Write-Error "mysql failed"
}
Write-Host "Done. See conversation-plugins/CASES.md"
