# PowerShell Script để test Risk Monitoring
# Author: Nguyễn Đình Nhật Huy - MSSV: 110122223

$BASE_URL = "http://localhost:8082"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RISK MONITORING TEST SCRIPT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check backend health
Write-Host "[1/4] Checking backend health..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$BASE_URL/actuator/health" -Method GET
    if ($health.status -eq "UP") {
        Write-Host "✓ Backend is UP and running!" -ForegroundColor Green
    } else {
        Write-Host "✗ Backend status: $($health.status)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ Backend is not responding. Please start it first!" -ForegroundColor Red
    Write-Host "  Run: mvn spring-boot:run" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Test 2: Check risk monitor status
Write-Host "[2/4] Checking risk monitor status..." -ForegroundColor Yellow
try {
    $status = Invoke-RestMethod -Uri "$BASE_URL/api/risk-monitor/status" -Method GET
    Write-Host "✓ Risk Monitor Service is active!" -ForegroundColor Green
    Write-Host "  - Scheduled: $($status.scheduledInterval)" -ForegroundColor Gray
    Write-Host "  - Red Grade Threshold: $($status.redGradeThreshold)" -ForegroundColor Gray
    Write-Host "  - Red Inactive Days: $($status.redInactiveDays)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed to get risk monitor status" -ForegroundColor Red
    Write-Host "  Error: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test 3: Trigger risk monitoring
Write-Host "[3/4] Triggering risk monitoring manually..." -ForegroundColor Yellow
Write-Host "  (This may take 10-30 seconds...)" -ForegroundColor Gray
try {
    $result = Invoke-RestMethod -Uri "$BASE_URL/api/risk-monitor/trigger" -Method POST
    if ($result.success) {
        Write-Host "✓ Risk monitoring completed successfully!" -ForegroundColor Green
        Write-Host "  - Duration: $($result.durationMs) ms" -ForegroundColor Gray
        Write-Host "  - Note: $($result.note)" -ForegroundColor Gray
    } else {
        Write-Host "✗ Risk monitoring failed" -ForegroundColor Red
        Write-Host "  Error: $($result.error)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ Failed to trigger risk monitoring" -ForegroundColor Red
    Write-Host "  Error: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test 4: Check warnings dashboard
Write-Host "[4/4] Checking warnings dashboard..." -ForegroundColor Yellow
try {
    $dashboard = Invoke-RestMethod -Uri "$BASE_URL/api/warnings/dashboard" -Method GET
    if ($dashboard.success) {
        $data = $dashboard.data
        Write-Host "✓ Dashboard data retrieved!" -ForegroundColor Green
        Write-Host "  - Green (Safe): $($data.greenCount)" -ForegroundColor Green
        Write-Host "  - Yellow (Warning): $($data.yellowCount)" -ForegroundColor Yellow
        Write-Host "  - Red (High Risk): $($data.redCount)" -ForegroundColor Red
        Write-Host "  - Total: $($data.totalCount)" -ForegroundColor Cyan
    } else {
        Write-Host "✗ Failed to get dashboard data" -ForegroundColor Red
    }
} catch {
    Write-Host "⚠ Dashboard endpoint not available (may be normal)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TEST COMPLETED!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Check backend logs for detailed results" -ForegroundColor Gray
Write-Host "2. Look for '🔴 RED ALERT' messages" -ForegroundColor Gray
Write-Host "3. Verify database: SELECT * FROM mdl_warning ORDER BY detected_at DESC LIMIT 10;" -ForegroundColor Gray
Write-Host ""
Write-Host "To view logs:" -ForegroundColor Yellow
Write-Host "  Get-Content chatbot-system\logs\chatbot-system.log -Tail 50" -ForegroundColor Gray
Write-Host ""
