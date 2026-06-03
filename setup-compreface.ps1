Write-Host "=== Setup CompreFace ===" -ForegroundColor Cyan

# Check Docker
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "Docker not found. Install Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Start CompreFace
Write-Host "Starting CompreFace container..." -ForegroundColor Yellow
docker rm -f compreface 2>$null
docker run -d -p 8000:8000 -e ADMIN_PASSWORD=admin --name compreface exadel/compreface

Write-Host "Waiting for CompreFace to start (up to 120s)..." -ForegroundColor Yellow
$maxWait = 120
$waited = 0
while ($waited -lt $maxWait) {
    try {
        $r = curl.exe -s -o $null -w "%{http_code}" http://localhost:8000/api/v1/health 2>$null
        if ($r -eq 200) { break }
    } catch {}
    Start-Sleep 2
    $waited += 2
    Write-Host "." -NoNewline
}

if ($waited -ge $maxWait) {
    Write-Host "`nCompreFace failed to start. Check 'docker logs compreface'" -ForegroundColor Red
    exit 1
}
Write-Host "`nCompreFace is ready!" -ForegroundColor Green

# Login
Write-Host "Creating application and getting API key..." -ForegroundColor Yellow
try {
    $login = curl.exe -s -X POST http://localhost:8000/api/v1/login `
        -H "Content-Type: application/json" `
        -d '{"email":"admin@admin.com","password":"admin"}' | ConvertFrom-Json
    $token = $login.access_token

    curl.exe -s -X POST http://localhost:8000/api/v1/management/applications `
        -H "Content-Type: application/json" `
        -H "Authorization: Bearer $token" `
        -d '{"name":"money-transfer"}' | Out-Null

    $appInfo = curl.exe -s http://localhost:8000/api/v1/management/applications/money-transfer `
        -H "Authorization: Bearer $token" | ConvertFrom-Json

    $apiKey = $appInfo.api_key
    Write-Host "API Key: $apiKey" -ForegroundColor Green

    # Update application.yml
    $ymlPath = "src\main\resources\application.yml"
    $yml = Get-Content $ymlPath -Raw
    $yml = $yml -replace "api-key:.*", "api-key: $apiKey"
    Set-Content $ymlPath $yml
    Write-Host "Updated application.yml with API key" -ForegroundColor Green
} catch {
    Write-Host "Failed to get API key: $_" -ForegroundColor Red
    Write-Host "Manually set COMPREFACE_API_KEY env var or update application.yml" -ForegroundColor Yellow
}

Write-Host "`n=== Done! Restart the app and try face features ===" -ForegroundColor Cyan
