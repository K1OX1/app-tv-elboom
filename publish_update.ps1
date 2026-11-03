param(
    [string]$Changelog = "Logos oficiales de apps y seleccion de apps en panel admin"
)

$ErrorActionPreference = "Stop"
$ProjectPath = "c:\Users\kioxo\Documents\APP TV"
$GradlePath = "$ProjectPath\gradlew.bat"
$BuildGradle = "$ProjectPath\app\build.gradle.kts"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "PUBLICADOR AUTOMATICO DE ACTUALIZACIONES (OTA BOOM TV)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Leer y aumentar versionCode en build.gradle.kts
$gradleContent = Get-Content $BuildGradle -Raw
if ($gradleContent -match 'versionCode\s*=\s*(\d+)') {
    $currentVersionCode = [int]$matches[1]
    $newVersionCode = $currentVersionCode + 1
    $gradleContent = $gradleContent -replace "versionCode\s*=\s*$currentVersionCode", "versionCode = $newVersionCode"
    
    if ($gradleContent -match 'versionName\s*=\s*"([^"]+)"') {
        $oldVerName = $matches[1]
        $parts = $oldVerName.Split('.')
        if ($parts.Length -eq 3) {
            $newPatch = [int]$parts[2] + 1
            $newVerName = "$($parts[0]).$($parts[1]).$newPatch"
            $gradleContent = $gradleContent -replace "versionName\s*=\s*`"$oldVerName`"", "versionName = `"$newVerName`""
        } else {
            $newVerName = "1.0.$newVersionCode"
        }
    } else {
        $newVerName = "1.0.$newVersionCode"
    }

    Set-Content -Path $BuildGradle -Value $gradleContent
    Write-Host "[OK] Version incrementada a: $newVerName (Code: $newVersionCode)" -ForegroundColor Green
} else {
    Write-Host "[ERROR] No se pudo encontrar versionCode" -ForegroundColor Red
    exit 1
}

# 2. Compilar APK Release firmado
Write-Host "[INFO] Limpiando APKs antiguos antes de compilar..." -ForegroundColor Yellow
Remove-Item -Path "$ProjectPath\app\build\outputs\apk\release\*.apk" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$ProjectPath\app-release.apk" -Force -ErrorAction SilentlyContinue

Write-Host "[INFO] Compilando APK Release firmado..." -ForegroundColor Yellow
$env:JAVA_HOME = "C:\Users\kioxo\.jdks\jbr-21.0.11"
& $GradlePath assembleRelease

$apkPath = "$ProjectPath\app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "[ERROR] No se encontro el APK en $apkPath" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] APK compilado exitosamente." -ForegroundColor Green

# 3. Subir APK a la nube permanente
Write-Host "[INFO] Subiendo APK a la nube permanente..." -ForegroundColor Yellow
$uploadResult = & curl.exe -s -F "reqtype=fileupload" -F "userhash=" -F "fileToUpload=@$apkPath" https://catbox.moe/user/api.php

if ($uploadResult -like "http*") {
    $apkUrl = $uploadResult.Trim()
    Write-Host "[OK] APK publicado permanentemente en Catbox: $apkUrl" -ForegroundColor Green
} else {
    Write-Host "[WARN] Catbox estándar no disponible ($uploadResult), intentando Litterbox..." -ForegroundColor Yellow
    $uploadResult = & curl.exe -s -F "reqtype=fileupload" -F "time=72h" -F "fileToUpload=@$apkPath" https://litterbox.catbox.moe/resources/internals/api.php
    if ($uploadResult -like "http*") {
        $apkUrl = $uploadResult.Trim()
        Write-Host "[OK] APK publicado en Litterbox: $apkUrl" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Falló la subida: $uploadResult" -ForegroundColor Red
        exit 1
    }
}

# 4. Generar version.json
$versionData = @{
    versionCode = $newVersionCode
    versionName = $newVerName
    apkUrl = $apkUrl
    changelog = $Changelog
} | ConvertTo-Json -Compress

$versionJsonFile = "$ProjectPath\version.json"
Set-Content -Path $versionJsonFile -Value $versionData
Write-Host "[OK] version.json generado:" -ForegroundColor Green
Write-Host $versionData -ForegroundColor White

# 5. Subir version.json a la nube pública permanente
Write-Host "[INFO] Publicando metadatos version.json en la nube permanente..." -ForegroundColor Yellow
$cloudUpdate = & curl.exe -s -X PUT -H "Content-Type: application/json" --data-binary "@$versionJsonFile" https://extendsclass.com/api/json-storage/bin/fbdcfbb
# Purgar cache CDN de inmediato
& curl.exe -s -H "Cache-Control: no-cache" -H "Pragma: no-cache" https://extendsclass.com/api/json-storage/bin/fbdcfbb | Out-Null
& curl.exe -s -H "Cache-Control: no-cache" -H "Pragma: no-cache" https://json.extendsclass.com/bin/fbdcfbb | Out-Null
Write-Host "[OK] Endpoint permanente actualizado y CDN purgado: https://extendsclass.com/api/json-storage/bin/fbdcfbb" -ForegroundColor Green

# Respaldo en Catbox
$jsonUpload = & curl.exe -s -F "reqtype=fileupload" -F "userhash=" -F "fileToUpload=@$versionJsonFile" https://catbox.moe/user/api.php
if ($jsonUpload -like "http*") {
    $remoteJsonUrl = $jsonUpload.Trim()
    Write-Host "[OK] Respaldo version.json en: $remoteJsonUrl" -ForegroundColor Green
}

# 6. Copiar APK a la raíz del servidor local para entrega ultrarrápida
Copy-Item -Path $apkPath -Destination "$ProjectPath\app-release.apk" -Force
Write-Host "[OK] APK disponible en servidor local: http://192.168.100.139:8080/app-release.apk" -ForegroundColor Green

Write-Host "`n[EXITO] ACTUALIZACION PUBLICADA EN LINEA (v$newVerName / Code $newVersionCode)" -ForegroundColor Cyan
Write-Host "Endpoint Cloud: https://extendsclass.com/api/json-storage/bin/fbdcfbb" -ForegroundColor White
Write-Host "Direct APK Cloud: $apkUrl" -ForegroundColor White
Write-Host "Las TVs conectadas detectarán la actualización automáticamente sin usar ADB ni Downloader." -ForegroundColor Green

Write-Host "`n[EXITO] ACTUALIZACION PUBLICADA CORRECTAMENTE (v$newVerName / Code $newVersionCode)" -ForegroundColor Cyan
Write-Host "Todas las TVs recibirán el aviso automáticamente en pantalla." -ForegroundColor White
