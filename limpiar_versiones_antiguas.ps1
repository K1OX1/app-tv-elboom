# Script para limpiar versiones y APKs antiguos
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "LIMPIEZA DE VERSIONES Y APKS ANTIGUOS - EL BOOM TV" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$projectRoot = $PSScriptRoot

# 1. Eliminar APKs debug antiguos
$debugApkPath = "$projectRoot\app\build\outputs\apk\debug"
if (Test-Path $debugApkPath) {
    Remove-Item -Path $debugApkPath -Recurse -Force
    Write-Host "[OK] Eliminada carpeta de APKs debug antiguos: $debugApkPath" -ForegroundColor Green
}

# 2. Eliminar intermediarios de compilación antiguos
$intermediatesPath = "$projectRoot\app\build\intermediates\apk"
if (Test-Path $intermediatesPath) {
    Remove-Item -Path $intermediatesPath -Recurse -Force
    Write-Host "[OK] Eliminados APKs intermedios: $intermediatesPath" -ForegroundColor Green
}

# 3. Limpieza de Gradle (opcional si se desea limpiar todo build/)
Write-Host "`nPara una limpieza total del caché de compilación de Android Studio/Gradle, ejecuta:" -ForegroundColor Yellow
Write-Host "  .\gradlew.bat clean" -ForegroundColor White
Write-Host "`n[OK] El proyecto contiene únicamente el pipeline de producción limpio." -ForegroundColor Green
