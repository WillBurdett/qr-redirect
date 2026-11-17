# Test strategy – qr-redirect

## 1. Scope
In scope: public redirect (`/q/{code}`), admin login, company and QR-code management, scan counting, QR image/zip download, dashboard UI.
Out of scope (for now): load/performance testing, accessibility audit, cross-browser (Chromium only), VPS/Caddy/TLS configuration.

## 2. Test levels
| Level | What | Tooling | Where | Runs |
|---|---|---|---|---|
| Unit | URL policy, short-ID generator, SVG matrix, company rules | JUnit 5, AssertJ, Mockito | `backend/src/test/.../service` | every push |
| API / integration | Controllers + Spring + real Postgres | MockMvc, Testcontainers (postgres:17) | `backend/src/test/.../web`, `QrRedirectFlowTest` | every push |
| End-to-end | Real browser against the real production image + Postgres | Playwright for Java, JUnit 5 | `e2e/` | PRs and `main` |

Principle: test each rule at the lowest level that can prove it; E2E covers only user journeys and integration seams (e.g. frontend ↔ backend routes).

## 3. Risk-based priorities
| Risk | Impact | Coverage |
|---|---|---|
| Printed code stops redirecting / goes to wrong URL | Critical (cannot be re-printed) | REQ-RED-01, REQ-QR-02, REQ-QR-03 at API and E2E level |
| Open redirect / unsafe target (`javascript:`, `http:`, credentials in URL) | High (security) | Unit + API tests on `TargetUrlPolicy`, E2E negative test |
| Unauthenticated access to management API | High (security) | REQ-AUTH-02 at API and E2E level |
| Wrong scan statistics (sales evidence for clients) | Medium | REQ-SCAN-01 at API and E2E level |
| Frontend and backend drift (renamed routes) | Medium | E2E journeys (caught `/api/codes` vs `/api/qrCodes`) |

## 4. Software Testing Life Cycle mapping
| STLC phase | How it is applied here |
|---|---|
| Requirements analysis | Requirements IDs and acceptance criteria in `REQUIREMENTS_TRACEABILITY.md`; testability review (e.g. "hit" semantics: bots and inactive codes excluded) |
| Test planning | This document: scope, levels, tools, risks, entry/exit criteria |
| Test case design | Given/When/Then criteria per requirement; positive, negative and boundary cases; tests tagged with requirement IDs |
| Environment set-up | `docker-compose.e2e.yml` (prod image + disposable Postgres), Testcontainers for backend tests, same Postgres major version as production |
| Test execution | GitHub Actions: `backend-tests.yml` (unit + integration + coverage), `e2e-tests.yml` (browser journeys) |
| Defect management | Failing run uploads surefire reports, Playwright trace and screenshot; defects logged as GitHub issues with severity/priority, linked to the REQ ID |
| Test closure | Traceability matrix reviewed, known gaps listed (section 8), coverage report archived |

## 5. Entry / exit criteria
Entry: build compiles, migrations apply, app healthy at `/actuator/health`.
Exit (merge to `main`): all backend and E2E tests green; no open Critical/High defect; every Must requirement has at least one automated test in the matrix.

## 6. Test data and isolation
Every test creates its own company/URL with a random suffix and never depends on another test. Data is arranged through the API (fast), the behaviour under test is exercised through the UI, and results are asserted in the UI and on the public `/q/{code}` endpoint. The E2E database is `tmpfs` and discarded after the run.

## 7. Running
```bash
# Backend (unit + integration, needs Docker for Testcontainers) + coverage
cd backend && mvn verify            # coverage: backend/target/site/jacoco/index.html

# End-to-end
docker compose -f docker-compose.e2e.yml up -d --build
until curl -sf http://localhost:8080/actuator/health; do sleep 3; done
cd e2e
mvn exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"   # first time only
mvn verify                          # all journeys
mvn verify -Dgroups=smoke           # smoke subset
mvn verify -Dgroups=REQ-QR-02       # everything tracing to one requirement
mvn verify -De2e.headed=true        # watch the browser
docker compose -f docker-compose.e2e.yml down -v
```
Failures write a screenshot and trace to `e2e/target/playwright`; open a trace with `npx playwright show-trace <file>.zip`.

## 8. Known gaps
- Chromium only; no mobile viewport or Firefox/WebKit runs.
- No load test of `/q/{code}` and no accessibility checks.
- Login rate limiting / lockout is not implemented, so not tested.
- Company-plan expiry behaviour is covered at API level only.
