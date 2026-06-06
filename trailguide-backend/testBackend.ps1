Invoke-RestMethod -Method Post http://localhost:8080/api/auth/register `
  -ContentType "application/json" `
  -Body '{"username":"turysta1234","email":"turysta2@example.com","password":"StrongPassword123!"}'

$response = Invoke-RestMethod -Method Post http://localhost:8080/api/auth/login `
  -ContentType "application/json" `
  -Body '{"email":"turysta2@example.com","password":"StrongPassword123!"}'

$token2 = $response2.token

$trip = Invoke-RestMethod -Method Post http://localhost:8080/api/trips/start `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body '{"title":"Tatry morning walk","description":"Easy route near the valley"}'

$tripId = $trip.id

Invoke-RestMethod -Method Post "http://localhost:8080/api/trips/$tripId/finish" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body '{}'

Invoke-RestMethod -Method Post "http://localhost:8080/api/trips/$tripId/location-points" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body '{"latitude":49.2992,"longitude":19.9496,"timestamp":"2026-05-24T12:30:00Z"}'

Invoke-RestMethod -Method Get "http://localhost:8080/api/trips/$tripId" `
  -Headers @{ Authorization = "Bearer $token" }

Invoke-RestMethod -Method Get http://localhost:8080/api/trips `
  -Headers @{ Authorization = "Bearer $token" }

$env:JAVA_HOME = "C:\Users\apacz\.jdks\ms-21.0.11"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$adminResponse = Invoke-RestMethod -Method Post http://localhost:8080/api/auth/login `
  -ContentType "application/json" `
  -Body '{"email":"admin@example.com","password":"AdminPassword123!"}'

curl.exe -k -X POST "https://localhost:8443/api/auth/login" -H "Content-Type: application/json" -d '{"email":"admin@example.com","password":"AdminPassword123!"}'

$adminResponse = Invoke-RestMethod -SkipCertificateCheck -Method Post https://localhost:8443/api/auth/login `
  -ContentType "application/json" `
  -Body '{"email":"admin@example.com","password":"AdminPassword123!"}'

$adminToken = $adminResponse.token

Invoke-RestMethod -Method Get http://localhost:8080/api/admin/users `
  -Headers @{ Authorization = "Bearer $adminToken" }

Invoke-RestMethod -SkipCertificateCheck -Method Get https://localhost:8443/api/admin/users `
  -Headers @{ Authorization = "Bearer $adminToken" }

Invoke-RestMethod -Method Post http://localhost:8080/api/notifications `
  -Headers @{ Authorization = "Bearer $adminToken" } `
  -ContentType "application/json" `
  -Body '{"title":"Uwaga na trasie","body":"Szlak jest dzisiaj częściowo zamknięty."}'


Invoke-RestMethod -SkipCertificateCheck -Method Post https://localhost:8443/api/notifications `
  -Headers @{ Authorization = "Bearer $adminToken" } `
  -ContentType "application/json" `
  -Body '{"title":"Uwaga na trasie","body":"Szlak jest dzisiaj częściowo zamknięty."}'

