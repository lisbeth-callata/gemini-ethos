# Gemini Ethos - Environment Setup Script
# Ejecuta este script antes de iniciar la aplicación:
#   . .\setup-env.ps1

Write-Host "Configurando variables de entorno para Gemini Ethos..." -ForegroundColor Cyan

$env:GOOGLE_CLOUD_PROJECT = "turismo-486820"
$env:GOOGLE_CLOUD_LOCATION = "global"
$env:GOOGLE_APPLICATION_CREDENTIALS = "$PSScriptRoot\credentials\service-account.json"
$env:ETHOS_SERVER_PORT = "8080"

# Verificar que existe el archivo de credenciales
if (Test-Path $env:GOOGLE_APPLICATION_CREDENTIALS) {
    Write-Host "✓ Archivo de credenciales encontrado" -ForegroundColor Green
} else {
    Write-Host "✗ ADVERTENCIA: No se encontró el archivo de credenciales en:" -ForegroundColor Yellow
    Write-Host "  $env:GOOGLE_APPLICATION_CREDENTIALS" -ForegroundColor Yellow
    Write-Host "  Descárgalo desde Google Cloud Console" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Variables configuradas:" -ForegroundColor Green
Write-Host "  GOOGLE_CLOUD_PROJECT = $env:GOOGLE_CLOUD_PROJECT"
Write-Host "  GOOGLE_CLOUD_LOCATION = $env:GOOGLE_CLOUD_LOCATION"
Write-Host "  GOOGLE_APPLICATION_CREDENTIALS = $env:GOOGLE_APPLICATION_CREDENTIALS"
Write-Host "  ETHOS_SERVER_PORT = $env:ETHOS_SERVER_PORT"
Write-Host ""
Write-Host "Ahora puedes ejecutar: mvn clean package" -ForegroundColor Cyan
