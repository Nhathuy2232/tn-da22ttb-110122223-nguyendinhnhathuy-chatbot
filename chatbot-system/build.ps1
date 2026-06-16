Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  BUILDING CHATBOT SYSTEM" -ForegroundColor Cyan
Write-Host "  (Custom NLP Engine - No External AI)" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Change to script directory
Set-Location $PSScriptRoot
Write-Host "Working directory: $(Get-Location)" -ForegroundColor Yellow
Write-Host ""

Write-Host "Step 1: Cleaning previous build..." -ForegroundColor Green
mvn clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Maven clean failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 2: Building project (skipping tests)..." -ForegroundColor Green
mvn package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Maven build failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "  BUILD SUCCESSFUL!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host ""
Write-Host "JAR file location:" -ForegroundColor Yellow
Write-Host "$(Get-Location)\target\chatbot-system-1.0.0-SNAPSHOT.jar" -ForegroundColor Cyan
Write-Host ""
