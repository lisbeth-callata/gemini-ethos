@echo off
REM Gemini Ethos - Run Script for Windows
REM Este script configura las variables de entorno y ejecuta la aplicacion

echo ============================================
echo   Gemini Ethos - Startup Script
echo ============================================
echo.

REM Verificar si GOOGLE_CLOUD_PROJECT esta configurado
if "%GOOGLE_CLOUD_PROJECT%"=="" (
    echo ERROR: GOOGLE_CLOUD_PROJECT no esta configurado
    echo.
    echo Por favor, configura la variable de entorno:
    echo   set GOOGLE_CLOUD_PROJECT=tu-proyecto-id
    echo.
    echo O edita este script y descomenta la linea siguiente:
    REM set GOOGLE_CLOUD_PROJECT=tu-proyecto-id
    echo.
    pause
    exit /b 1
)

echo Proyecto GCP: %GOOGLE_CLOUD_PROJECT%
echo.

REM Verificar si el JAR existe
if not exist "target\gemini-ethos-1.0.0-SNAPSHOT.jar" (
    echo Compilando el proyecto...
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo ERROR: Fallo la compilacion
        pause
        exit /b 1
    )
)

echo.
echo Iniciando Gemini Ethos...
echo.

java -jar target\gemini-ethos-1.0.0-SNAPSHOT.jar

pause
