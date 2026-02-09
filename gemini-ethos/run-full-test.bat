@echo off
REM Script para ejecutar el test completo de Gemini Ethos
REM Ejecutar desde: C:\Users\xp77915\Documents\turismo\gemini-ethos

echo ============================================
echo   Gemini Ethos - Test Completo
echo ============================================
echo.

set GOOGLE_CLOUD_PROJECT=turismo-486820
set GOOGLE_APPLICATION_CREDENTIALS=C:\Users\xp77915\Documents\turismo\gemini-ethos\credentials\service-account.json

echo Iniciando servidor en segundo plano...
start /B java -jar C:\Users\xp77915\Documents\turismo\gemini-ethos\target\gemini-ethos-1.0.0-SNAPSHOT.jar

echo Esperando 10 segundos para que el servidor inicie...
timeout /t 10 /nobreak > nul

echo.
echo Verificando health del servidor...
curl -s http://localhost:8080/health

echo.
echo.
echo Servidor listo! Presiona cualquier tecla para ejecutar el test con imagen...
pause > nul

echo.
echo Ejecutando test con PowerShell...
powershell -ExecutionPolicy Bypass -File "C:\Users\xp77915\Documents\turismo\gemini-ethos\test-real-image.ps1"

echo.
echo Presiona cualquier tecla para detener el servidor...
pause > nul

echo Deteniendo servidor...
taskkill /F /IM java.exe 2>nul

echo Listo!
