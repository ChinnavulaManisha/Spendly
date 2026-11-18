# Spendly Build & Packaging Script
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   Compiling Spendly Java Application    " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

$srcDir = "src\main\java"
$classesDir = "WebContent\WEB-INF\classes"
$libDir = "WebContent\WEB-INF\lib"
$distDir = "dist"

New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
New-Item -ItemType Directory -Force -Path $distDir | Out-Null

# Copy resource files (db.properties) to classes directory
Copy-Item "src\main\resources\*" -Destination $classesDir -Force -ErrorAction SilentlyContinue

# Gather all java source files
$javaFiles = Get-ChildItem -Path $srcDir -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
$classpath = (Get-ChildItem -Path $libDir -Filter "*.jar" | ForEach-Object { $_.FullName }) -join ";"

Write-Host "Found $($javaFiles.Count) Java source files. Compiling..." -ForegroundColor Yellow

$javacArgs = @(
    "-cp", $classpath,
    "-d", $classesDir,
    "-encoding", "UTF-8"
) + $javaFiles

& javac $javacArgs

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation Successful!" -ForegroundColor Green

    # Package into Spendly.war
    Write-Host "Packaging Spendly.war..." -ForegroundColor Yellow
    $warFile = "$distDir\Spendly.war"
    if (Test-Path $warFile) { Remove-Item $warFile -Force }

    # Use jar command if available, or zip compression
    Set-Location WebContent
    & jar -cvf "..\$warFile" *
    Set-Location ..

    Write-Host "=========================================" -ForegroundColor Green
    Write-Host " Build Complete: dist/Spendly.war created " -ForegroundColor Green
    Write-Host "=========================================" -ForegroundColor Green
} else {
    Write-Host "Compilation FAILED with error code $LASTEXITCODE" -ForegroundColor Red
    exit 1
}
