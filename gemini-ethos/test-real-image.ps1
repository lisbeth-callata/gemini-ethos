# Test Gemini Ethos con imagen real
# Este script inicia el servidor, espera, y hace una prueba

$projectDir = "C:\Users\xp77915\Documents\turismo\gemini-ethos"
$imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Iguana_iguana_Portoviejo_02.jpg/320px-Iguana_iguana_Portoviejo_02.jpg"
$testImagePath = "$projectDir\test-iguana.jpg"

# Configurar variables de entorno
$env:GOOGLE_CLOUD_PROJECT = "turismo-486820"
$env:GOOGLE_APPLICATION_CREDENTIALS = "$projectDir\credentials\service-account.json"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Gemini Ethos - Test con Imagen Real" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Descargar imagen de prueba si no existe
if (-not (Test-Path $testImagePath)) {
    Write-Host "Descargando imagen de prueba..." -ForegroundColor Yellow
    try {
        Invoke-WebRequest -Uri $imageUrl -OutFile $testImagePath -UseBasicParsing
        Write-Host "Imagen descargada: $testImagePath" -ForegroundColor Green
    } catch {
        Write-Host "Error descargando imagen: $_" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "Convirtiendo imagen a Base64..." -ForegroundColor Yellow
$imageBytes = [System.IO.File]::ReadAllBytes($testImagePath)
$base64Image = [Convert]::ToBase64String($imageBytes)
Write-Host "Imagen convertida: $($imageBytes.Length) bytes" -ForegroundColor Green

Write-Host ""
Write-Host "Enviando imagen a Gemini Ethos para analisis..." -ForegroundColor Yellow
Write-Host "(Simulando un turista en Galapagos cerca de una iguana)" -ForegroundColor Gray
Write-Host ""

# Crear el body con contexto de ubicacion
$body = @{
    sessionId = "test-galapagos-001"
    imageData = $base64Image
    mimeType = "image/jpeg"
    location = @{
        latitude = -0.9538
        longitude = -90.9656
        locationName = "Isla Isabela"
        parkName = "Galapagos"
    }
} | ConvertTo-Json -Depth 3

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/analyze/stream" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 60
    
    Write-Host "============================================" -ForegroundColor Green
    Write-Host "  RESULTADO DEL ANALISIS" -ForegroundColor Green
    Write-Host "============================================" -ForegroundColor Green
    Write-Host ""
    
    Write-Host "Session ID: $($response.sessionId)" -ForegroundColor White
    Write-Host "Timestamp: $($response.timestamp)" -ForegroundColor White
    Write-Host ""
    
    # Mostrar nivel de riesgo con color
    $riskColor = switch ($response.overallRiskLevel) {
        "LOW" { "Green" }
        "MEDIUM" { "Yellow" }
        "HIGH" { "DarkYellow" }
        "CRITICAL" { "Red" }
        default { "White" }
    }
    Write-Host "NIVEL DE RIESGO: $($response.overallRiskLevel)" -ForegroundColor $riskColor
    Write-Host ""
    
    # Mostrar comportamientos detectados
    if ($response.detectedBehaviors -and $response.detectedBehaviors.Count -gt 0) {
        Write-Host "COMPORTAMIENTOS DETECTADOS:" -ForegroundColor Cyan
        foreach ($behavior in $response.detectedBehaviors) {
            Write-Host "  - [$($behavior.riskLevel)] $($behavior.behaviorType)" -ForegroundColor Yellow
            Write-Host "    $($behavior.description)" -ForegroundColor White
            Write-Host "    Confianza: $([math]::Round($behavior.confidence * 100, 1))%" -ForegroundColor Gray
        }
        Write-Host ""
    }
    
    # Mostrar guias eticas
    if ($response.guidelines -and $response.guidelines.Count -gt 0) {
        Write-Host "GUIAS ETICAS:" -ForegroundColor Cyan
        foreach ($guide in $response.guidelines) {
            Write-Host "  [$($guide.category)]" -ForegroundColor Yellow
            Write-Host "    $($guide.guideline)" -ForegroundColor White
            if ($guide.culturalContext) {
                Write-Host "    Contexto: $($guide.culturalContext)" -ForegroundColor Gray
            }
        }
        Write-Host ""
    }
    
    # Mostrar acciones inmediatas
    if ($response.immediateActions -and $response.immediateActions.Count -gt 0) {
        Write-Host "ACCIONES RECOMENDADAS:" -ForegroundColor Cyan
        foreach ($action in $response.immediateActions) {
            Write-Host "  * $action" -ForegroundColor White
        }
        Write-Host ""
    }
    
    # Mostrar info de regulaciones si existe
    if ($response.regulationInfo) {
        Write-Host "REGULACIONES APLICABLES:" -ForegroundColor Cyan
        Write-Host "  Parque: $($response.regulationInfo.parkName)" -ForegroundColor White
        Write-Host "  Region: $($response.regulationInfo.region)" -ForegroundColor White
        if ($response.regulationInfo.applicableRules) {
            foreach ($rule in $response.regulationInfo.applicableRules) {
                Write-Host "  - $rule" -ForegroundColor Gray
            }
        }
        Write-Host ""
    }
    
    Write-Host "RESUMEN:" -ForegroundColor Cyan
    Write-Host $response.summary -ForegroundColor White
    Write-Host ""
    
} catch {
    Write-Host "ERROR en la solicitud: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Asegurate de que el servidor este corriendo:" -ForegroundColor Yellow
    Write-Host "  cd $projectDir" -ForegroundColor Gray
    Write-Host '  $env:GOOGLE_CLOUD_PROJECT = "turismo-486820"' -ForegroundColor Gray
    Write-Host '  $env:GOOGLE_APPLICATION_CREDENTIALS = "...\credentials\service-account.json"' -ForegroundColor Gray
    Write-Host '  java -jar target\gemini-ethos-1.0.0-SNAPSHOT.jar' -ForegroundColor Gray
}

Write-Host "============================================" -ForegroundColor Cyan
