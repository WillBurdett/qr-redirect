# qr-redirect (MVP)

Dynamic QR codes: companies print a code once, and you can change where it points at any time.

```
QR code  ->  https://qr.yourdomain.com/q/Ab3xK9z  ->  DB lookup  ->  302  ->  company's target URL
```

The QR image only ever encodes the short URL. "Reassigning" a code is a single database update, so
printed codes keep working forever.

Stack: Java 21, Spring Boot 3.5, Spring Data JPA, ZXing (QR generation), Caffeine (cache),
H2 for local development, Postgres in production.

## Run it

Needs Java 21 and Maven 3.9+.

```bash
mvn verify            # build + run the tests
mvn spring-boot:run   # start on http://localhost:8080 (H2 file database in ./data)
```

The `dev` profile is active by default and defines one demo company with the API key `dev-key-change-me`.

## Try it

```bash
KEY='X-API-Key: dev-key-change-me'

# 1. Create a code
curl -s -X POST localhost:8080/api/codes -H "$KEY" -H 'Content-Type: application/json' \
  -d '{"targetUrl":"https://example.com/spring-sale"}'
# -> {"id":1,"shortCode":"...","shortUrl":"http://localhost:8080/q/...","targetUrl":"https://example.com/spring-sale","active":true,...}

# 2. Download the printable image (use svg for print)
curl -s -H "$KEY" -o code.png "localhost:8080/api/codes/1/image?format=png&size=600"
curl -s -H "$KEY" -o code.svg "localhost:8080/api/codes/1/image?format=svg"

# 3. Scan it (what a phone does): expect 302 + Location header
curl -i localhost:8080/q/<shortCode>

# 4. Reassign it. The printed code is unchanged.
curl -s -X PUT localhost:8080/api/codes/1 -H "$KEY" -H 'Content-Type: application/json' \
  -d '{"targetUrl":"https://example.com/summer-sale"}'

# 5. Switch it off (scans show a friendly 404 page) or back on
curl -s -X PUT localhost:8080/api/codes/1 -H "$KEY" -H 'Content-Type: application/json' -d '{"active":false}'
```

## API

| Method | Path | Notes |
|---|---|---|
| `GET` | `/q/{code}` | Public. 302 to the target, or a 404 page if unknown/inactive. |
| `POST` | `/api/codes` | `{"targetUrl": "..."}` creates a code. |
| `GET` | `/api/codes` | Lists your company's codes. |
| `GET` | `/api/codes/{id}` | One code. |
| `PUT` | `/api/codes/{id}` | `{"targetUrl": "...", "active": true}`. Both fields optional. |
| `GET` | `/api/codes/{id}/image` | `format=png\|svg`, `size=64..4096` (PNG), `ec=L\|M\|Q\|H`. |

All `/api/**` calls need an `X-API-Key` header. The key identifies the company, and every query is scoped
to it, so one company can't see or change another's codes.

## Configuration

| Setting | Env var | Default | Purpose |
|---|---|---|---|
| `app.base-url` | `APP_BASE_URL` | `http://localhost:8080` | Public URL baked into every QR code. **Set it to your real domain before printing anything.** |
| `app.api-key` | `APP_APIKEY` | demo key in `dev` profile only | One key per company. Company ids from env vars come through lower-cased. |
| `app.allow-http` | `APP_ALLOWHTTP` | `false` | Accept `http://` targets (https only by default). |
| (port) | `PORT` | `8080` | |

## Deploy

```bash
mvn -DskipTests package
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:postgresql://host:5432/qrdb DB_USER=... DB_PASSWORD=... \
APP_BASE_URL=https://qr.yourdomain.com \
APP_APIKEYS_ACME=some-long-random-secret \
java -jar target/qr-redirect-0.0.1-SNAPSHOT.jar
```

`DB_URL` must be a JDBC URL (`jdbc:postgresql://...`). Many hosts hand you a `postgres://...` connection
string instead, which needs converting. Fly.io, Railway and Render all work: any host that runs a jar or
Dockerfile and offers managed Postgres.

## Design notes

- **302, not 301.** Browsers cache 301s permanently, which would break reassignment.
- **Short codes** are 7 random base62 characters (about 3.5 trillion combinations): not guessable or enumerable.
- **Hot path** (`/q/{code}`) is served from a Caffeine cache (60s TTL, max 10k entries). Updates evict the
  entry immediately on the instance that handled them. If you run several instances, a change can take up to
  the TTL to reach the others. Misses are never cached.
- **Target URL rules:** https only, host required, no embedded credentials, max 2048 chars.
- **Schema** is managed by Flyway (`ddl-auto=update`).

## Project layout

```
src/main/java/com/example/qrredirect/
  QrRedirectApplication.java
  config/AppProperties.java          settings (app.*)
  model/QrCode.java                  the one table: qr_codes
  repo/QrCodeRepository.java
  service/QrCodeService.java         create / resolve (cached) / update (evicts cache)
  service/QrImageService.java        ZXing PNG + SVG
  service/ShortCodeGenerator.java
  service/TargetUrlPolicy.java       what URLs are allowed
  web/RedirectController.java        GET /q/{code}
  web/QrCodeApiController.java       /api/codes/**
  web/ApiKeyFilter.java              per-company API keys
  web/CodeDtos.java
```

## Obvious next steps

1. **Scan analytics:** a `scan_events` table (code id, timestamp, user agent), written asynchronously so the redirect stays fast.
2. **Real auth and accounts** (Spring Security) to replace the API key filter, plus rate limiting on `/q/**`.
3. **Abuse protection:** domain blocklist and/or Google Safe Browsing check on target URLs.
4. **Flyway migrations** instead of `ddl-auto=update`.
5. **A small admin UI** (React) over the API: list, edit target, download image.
6. **Logos in the middle of a code:** generate with `ec=Q` or `H` and overlay the logo in the design tool.
7. **Custom domains per company** if they want codes on their own domain. The domain is baked into printed codes, so decide early.
8. **Spring Boot 4.x:** 3.5 left open-source support on 30 June 2026. The upgrade is mostly a version bump
   plus some relocated test annotations, and best done before you build much more on top.
