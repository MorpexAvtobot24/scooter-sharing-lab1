function Test-Api($sb) {
    try { return (& $sb).StatusCode }
    catch { return [int]$_.Exception.Response.StatusCode }
}

# 1. Запит без токена (очікуємо 401)
Write-Host "1. Без токена (має бути 401):" (Test-Api { Invoke-WebRequest -Uri "http://localhost:8081/api/scooters" -UseBasicParsing })

# 2. Отримуємо JWT-токени для alice та bob із Keycloak
$aliceToken = (Invoke-RestMethod -Method Post -Uri "http://localhost:8080/realms/course-realm/protocol/openid-connect/token" -Body @{client_id="backend-app"; grant_type="password"; username="alice"; password="alice123"}).access_token
$bobToken = (Invoke-RestMethod -Method Post -Uri "http://localhost:8080/realms/course-realm/protocol/openid-connect/token" -Body @{client_id="backend-app"; grant_type="password"; username="bob"; password="bob123"}).access_token
Write-Host "2. Токени alice і bob отримано!"

# 3. Перевірка прав bob (GET -> 200, POST/PUT/DELETE -> 403)
$bobH = @{ Authorization = "Bearer $bobToken"; "Content-Type" = "application/json" }
$body = '{"serialNumber":"ES-9999","model":"Test","batteryLevel":90,"pricePerMinute":0.25,"status":"AVAILABLE"}'
Write-Host "3. Bob GET (має бути 200):" (Test-Api { Invoke-WebRequest -Uri "http://localhost:8081/api/scooters" -Headers $bobH -UseBasicParsing })
Write-Host "4. Bob POST (має бути 403):" (Test-Api { Invoke-WebRequest -Method Post -Uri "http://localhost:8081/api/scooters" -Headers $bobH -Body $body -UseBasicParsing })
Write-Host "5. Bob DELETE (має бути 403):" (Test-Api { Invoke-WebRequest -Method Delete -Uri "http://localhost:8081/api/scooters/1" -Headers $bobH -UseBasicParsing })

# 6. Перевірка alice (створення -> 201, дублікат -> 409, неіснуючий ID -> 404)
$aliceH = @{ Authorization = "Bearer $aliceToken"; "Content-Type" = "application/json" }
$dupBody = '{"serialNumber":"ES-1001","model":"Dup","batteryLevel":50,"pricePerMinute":0.20,"status":"AVAILABLE"}'
Write-Host "6. Alice дублікат (має бути 409):" (Test-Api { Invoke-WebRequest -Method Post -Uri "http://localhost:8081/api/scooters" -Headers $aliceH -Body $dupBody -UseBasicParsing })
Write-Host "7. Alice неіснуючий ID (має бути 404):" (Test-Api { Invoke-WebRequest -Uri "http://localhost:8081/api/scooters/99999" -Headers $aliceH -UseBasicParsing })