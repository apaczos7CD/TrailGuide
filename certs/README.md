# TrailGuide development TLS certificate

Public files that may be committed:

- `trailguide-dev.crt`
- `trailguide-dev-openssl.cnf`

Private files that must stay local:

- `trailguide-dev.key`
- `trailguide-dev.p12`

Set the PKCS12 password in the local `.env` file:

```env
SERVER_SSL_KEY_STORE_PASSWORD=your-local-p12-password
```

Docker Compose mounts this directory read-only and uses `trailguide-dev.p12`
as the Spring Boot HTTPS keystore.
