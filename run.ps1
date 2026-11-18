# Spendly Server Launcher
Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "                  STARTING SPENDLY APPLICATION                    " -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

if (-not (Test-Path "WebContent\WEB-INF\classes\com\spendly\runner\SpendlyServer.class")) {
    Write-Host "Compiling application first..." -ForegroundColor Yellow
    & .\build.ps1
}

$classpath = "WebContent\WEB-INF\classes;" + ((Get-ChildItem -Path "WebContent\WEB-INF\lib" -Filter "*.jar" | ForEach-Object { $_.FullName }) -join ";")

Write-Host "Starting Spendly Server on http://localhost:8085/ ..." -ForegroundColor Green
& java -cp $classpath com.spendly.runner.SpendlyServer 8085
