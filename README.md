# TrailGuide

TrailGuide is an Android field app for hikers. It records GPS-based trips and uses a Spring Boot REST API with JWT authentication, PostgreSQL, Firebase Cloud Messaging support, and HTTPS/TLS for backend communication.

## Project Structure

```text
TrailGuide/
  android-app/          Android client
  trailguide-backend/   Spring Boot REST API
  certs/                Development TLS certificate files
  docker-compose.yml    Backend + PostgreSQL runtime
```

## Backend Features

- User registration and login with JWT.
- User profile editing.
- Roles: `USER` and `ADMIN`.
- Default admin account seeded at backend startup.
- Trip tracking API:
  - start trip,
  - save GPS points,
  - finish trip,
  - calculate distance on the backend,
  - list user trips,
  - view trip details.
- Users can access only their own trips.
- FCM token registration.
- Admin-only notification endpoint.
- PostgreSQL runtime through Docker Compose.
- H2 in-memory database for automated tests.
- HTTPS-only backend in Docker.

## Android Features

- Login and registration against the backend API.
- JWT storage with `EncryptedSharedPreferences`.
- User profile view and edit.
- Current trip and historical trip views.
- Foreground service for GPS tracking while a trip is active.
- OpenStreetMap/osmdroid trip preview.
- Firebase Cloud Messaging token registration after login.
- System notifications for received FCM push messages.
- Light and dark Material 3 themes.
- Polish and English language resources.
- HTTPS communication with the development backend through Android Network Security Config.

## Requirements

- JDK 21
- Docker Desktop
- PowerShell
- OpenSSL, if you need to regenerate development certificates
- Android Studio with an emulator image that includes Google Play Services, if you want to test FCM

## Local Environment

Create a local `.env` file in the repository root. It is ignored by Git.

Example:

```env
TRAILGUIDE_FIREBASE_ENABLED=true
TRAILGUIDE_FIREBASE_SERVICE_ACCOUNT_FILE=./firebase-service-account.json
SERVER_SSL_KEY_STORE_PASSWORD=your-local-p12-password
```

Do not commit:

- `.env`
- Firebase service account JSON files
- `android-app/app/google-services.json`
- `certs/*.key`
- `certs/*.p12`

Keep `.env.example` committed as a template only.

## Firebase Setup

### Android App

Create an Android app in Firebase Console with this package name:

```text
pl.trailguide.app
```

Download `google-services.json` and place it here:

```text
android-app/app/google-services.json
```

This file is ignored by Git. When it exists, the Android build applies the Google Services plugin automatically.

### Backend Admin SDK

Generate a Firebase service account private key in Firebase Console and place it outside Git-tracked files, for example:

```text
firebase-service-account.json
```

Point `.env` to that file:

```env
TRAILGUIDE_FIREBASE_ENABLED=true
TRAILGUIDE_FIREBASE_SERVICE_ACCOUNT_FILE=./firebase-service-account.json
```

If `TRAILGUIDE_FIREBASE_ENABLED=false`, `/api/notifications` stores notification records and counts recipients, but does not send push messages to Firebase.

## Development TLS Certificate

Public certificate files may be committed:

```text
certs/trailguide-dev.crt
certs/trailguide-dev-openssl.cnf
certs/README.md
```

Private certificate material must stay local:

```text
certs/trailguide-dev.key
certs/trailguide-dev.p12
```

The Docker backend expects:

```text
certs/trailguide-dev.p12
```

and reads its password from:

```env
SERVER_SSL_KEY_STORE_PASSWORD
```

## Running Automated Backend Tests

Tests use H2 and do not require PostgreSQL, Docker, or TLS certificates.

```powershell
cd C:\Users\apacz\projekty\TrailGuide\TrailGuide\trailguide-backend
.\gradlew.bat test
```

## Running Backend With Docker

The Docker setup starts two containers:

- `trailguide-backend`
- `trailguide-postgres`

Run:

```powershell
cd C:\Users\apacz\projekty\TrailGuide\TrailGuide
docker compose up --build
```

Backend URL:

```text
https://localhost:8443
```

PostgreSQL is exposed on the host as:

```text
localhost:5433
```

For the Android emulator, the same backend is available as:

```text
https://10.0.2.2:8443
```

Stop containers:

```powershell
docker compose down
```

Stop containers and remove database data:

```powershell
docker compose down -v
```

## Default Admin

The backend creates a default admin account at startup:

```text
email: admin@example.com
password: AdminPassword123!
role: ADMIN
```

These values can be changed through environment variables in `docker-compose.yml`.

## PowerShell API Examples

Because the development certificate is self-signed, use `-SkipCertificateCheck` in PowerShell 7 for manual testing.

### Login As Admin

```powershell
$adminResponse = Invoke-RestMethod -SkipCertificateCheck -Method Post https://localhost:8443/api/auth/login `
  -ContentType "application/json" `
  -Body '{"email":"admin@example.com","password":"AdminPassword123!"}'

$adminToken = $adminResponse.token
```

### List Users As Admin

```powershell
Invoke-RestMethod -SkipCertificateCheck -Method Get https://localhost:8443/api/admin/users `
  -Headers @{ Authorization = "Bearer $adminToken" }
```

### Register User

```powershell
Invoke-RestMethod -SkipCertificateCheck -Method Post https://localhost:8443/api/auth/register `
  -ContentType "application/json" `
  -Body '{"username":"turysta123","email":"turysta@example.com","password":"StrongPassword123!"}'
```

### Login User

```powershell
$userResponse = Invoke-RestMethod -SkipCertificateCheck -Method Post https://localhost:8443/api/auth/login `
  -ContentType "application/json" `
  -Body '{"email":"turysta@example.com","password":"StrongPassword123!"}'

$userToken = $userResponse.token
```

### Update User Profile

```powershell
Invoke-RestMethod -SkipCertificateCheck -Method Put https://localhost:8443/api/users/me `
  -Headers @{ Authorization = "Bearer $userToken" } `
  -ContentType "application/json" `
  -Body '{"firstName":"Adam","city":"Krakow","height":170,"weight":75,"hikingLevel":"BEGINNER"}'
```

### Start Trip

```powershell
$trip = Invoke-RestMethod -SkipCertificateCheck -Method Post https://localhost:8443/api/trips/start `
  -Headers @{ Authorization = "Bearer $userToken" } `
  -ContentType "application/json" `
  -Body '{"title":"Tatry morning walk","description":"Easy route near the valley"}'

$tripId = $trip.id
```

### Add GPS Point

```powershell
Invoke-RestMethod -SkipCertificateCheck -Method Post "https://localhost:8443/api/trips/$tripId/location-points" `
  -Headers @{ Authorization = "Bearer $userToken" } `
  -ContentType "application/json" `
  -Body '{"latitude":49.2992,"longitude":19.9496,"altitude":960.0,"accuracy":6.5,"timestamp":"2026-05-24T12:30:00Z"}'
```

### Finish Trip

The backend sets `endTime` and calculates `distanceMeters` from saved GPS points.

```powershell
Invoke-RestMethod -SkipCertificateCheck -Method Post "https://localhost:8443/api/trips/$tripId/finish" `
  -Headers @{ Authorization = "Bearer $userToken" } `
  -ContentType "application/json" `
  -Body '{}'
```

### Register FCM Token

```powershell
Invoke-RestMethod -SkipCertificateCheck -Method Post https://localhost:8443/api/fcm-token `
  -Headers @{ Authorization = "Bearer $userToken" } `
  -ContentType "application/json" `
  -Body '{"token":"test-fcm-token"}'
```

### Send Notification As Admin

```powershell
Invoke-RestMethod -SkipCertificateCheck -Method Post https://localhost:8443/api/notifications `
  -Headers @{ Authorization = "Bearer $adminToken" } `
  -ContentType "application/json" `
  -Body '{"title":"Trail warning","body":"The trail is partially closed today."}'
```

## Android HTTPS Notes

The Docker backend uses:

```text
https://localhost:8443
```

From an Android emulator, the host machine is usually available as:

```text
https://10.0.2.2:8443
```

The Android app should:

- call `https://10.0.2.2:8443`,
- trust the bundled development certificate in `android-app/app/src/main/res/raw/trailguide_dev.crt`,
- use `android-app/app/src/main/res/xml/network_security_config.xml`,
- reject cleartext HTTP traffic with `android:usesCleartextTraffic="false"`.

The development certificate contains SAN entries for:

```text
localhost
127.0.0.1
10.0.2.2
```

If you regenerate `certs/trailguide-dev.crt`, also update the bundled Android certificate resource.

## Android Build

```powershell
cd C:\Users\apacz\projekty\TrailGuide\TrailGuide\android-app
.\gradlew.bat :app:assembleDebug
```

If Android Studio reports unresolved Firebase, Google Play Services, or osmdroid symbols, run Gradle Sync after adding the Firebase configuration file.
