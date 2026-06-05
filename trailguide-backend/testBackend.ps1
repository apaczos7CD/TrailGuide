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