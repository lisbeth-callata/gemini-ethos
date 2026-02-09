# Gemini Ethos - Test Script
# Prueba los endpoints de la API

# Variables
$baseUrl = "http://localhost:8080"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Gemini Ethos - API Test Script" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "[TEST 1] Health Check..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/health" -Method GET
    Write-Host "  Status: $($response.status)" -ForegroundColor Green
    Write-Host "  Active Sessions: $($response.activeSessions)" -ForegroundColor Green
} catch {
    Write-Host "  ERROR: $_" -ForegroundColor Red
}
Write-Host ""

# Test 2: API Info
Write-Host "[TEST 2] API Info..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1" -Method GET
    Write-Host "  Name: $($response.name)" -ForegroundColor Green
    Write-Host "  Version: $($response.version)" -ForegroundColor Green
} catch {
    Write-Host "  ERROR: $_" -ForegroundColor Red
}
Write-Host ""

# Test 3: Frame Analysis (con imagen de prueba)
Write-Host "[TEST 3] Frame Analysis..." -ForegroundColor Yellow
Write-Host "  Nota: Para probar con imagen real, modifica este script" -ForegroundColor Gray

# Crear una imagen de prueba simple (1x1 pixel JPEG en base64)
$testImageBase64 = "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAn/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBEQCEQwD/AE1H/9k="

$body = @{
    imageData = $testImageBase64
    sessionId = "test-session-001"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/analyze/frame" -Method POST -Body $body -ContentType "application/json"
    Write-Host "  Session ID: $($response.sessionId)" -ForegroundColor Green
    Write-Host "  Risk Level: $($response.overallRiskLevel)" -ForegroundColor Green
    Write-Host "  Summary: $($response.summary)" -ForegroundColor Green
} catch {
    Write-Host "  ERROR: $_" -ForegroundColor Red
    Write-Host "  Asegurate de que el servidor este corriendo y las credenciales de GCP configuradas" -ForegroundColor Gray
}
Write-Host ""

# Test 4: Stream Analysis
Write-Host "[TEST 4] Stream Analysis with Location..." -ForegroundColor Yellow

$streamBody = @{
    sessionId = "test-stream-001"
    imageData = $testImageBase64
    mimeType = "image/jpeg"
    capturedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    location = @{
        latitude = -0.9538
        longitude = -90.9656
        locationName = "Isla Isabela"
        parkName = "Galapagos"
    }
    metadata = @{
        frameNumber = 1
        width = 1920
        height = 1080
        deviceId = "test-device"
        cameraType = "rear"
    }
} | ConvertTo-Json -Depth 3

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/analyze/stream" -Method POST -Body $streamBody -ContentType "application/json"
    Write-Host "  Session ID: $($response.sessionId)" -ForegroundColor Green
    Write-Host "  Risk Level: $($response.overallRiskLevel)" -ForegroundColor Green
    if ($response.regulationInfo) {
        Write-Host "  Park: $($response.regulationInfo.parkName)" -ForegroundColor Green
    }
} catch {
    Write-Host "  ERROR: $_" -ForegroundColor Red
}
Write-Host ""

# Test 5: Clear Session
Write-Host "[TEST 5] Clear Session..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/session/test-session-001" -Method DELETE
    Write-Host "  Result: $($response.message)" -ForegroundColor Green
} catch {
    Write-Host "  ERROR: $_" -ForegroundColor Red
}
Write-Host ""

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Tests Completed!" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
