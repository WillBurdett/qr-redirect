# Requirements traceability matrix

Requirement IDs are also JUnit tags in the E2E suite (`@Tag("REQ-QR-02")`), so `mvn verify -Dgroups=REQ-QR-02` runs everything that traces to a requirement.
Acceptance criteria below were derived from the current behaviour of the app; review them against intent when baselining.

| ID | Requirement / acceptance criteria | Backend tests | E2E tests | Status |
|---|---|---|---|---|
| REQ-AUTH-01 | **Given** valid admin credentials **when** logging in **then** the dashboard opens; invalid credentials show an error; logout ends the session | `CompanyControllerIntegrationTest` (rejectsLoginForUnknownEmail, rejectsLoginWithWrongPassword) | `AuthE2ETest` validLoginShowsDashboard, wrongPasswordIsRejected, logoutEndsSession | Automated |
| REQ-AUTH-02 | Management API (`/api/**`) rejects callers with no session / API key; public `/q/**` stays open | `QrRedirectFlowTest.apiRequiresTheOperatorKeyButRedirectsArePublic` | `AuthE2ETest.managementApiRequiresLogin` | Automated |
| REQ-CO-01 | An admin can create a company with a plan; empty names, unknown plans and case-insensitive duplicates are rejected; pilot plans get an expiry | `CompanyServiceTest`, `CompanyControllerIntegrationTest`, `QrRedirectFlowTest.companyRulesAreEnforced` | `QrLifecycleE2ETest.createCompanyAndQrCode` | Automated |
| REQ-QR-01 | An admin can create a QR code for a company with an https target; http, `javascript:`, credentials-in-URL, malformed or missing targets are rejected | `UnitTests` (urlPolicy*), `QrRedirectFlowTest` (createRejectsUnsafeOrMissingInput, updateRejectsUnsafeTarget) | `createCompanyAndQrCode`, `insecureUrlIsRejected` | Automated |
| REQ-QR-02 | Changing the destination takes effect on the next scan; the short code stays the same | `QrRedirectFlowTest.createScanReassignAndDeactivate` | `QrLifecycleE2ETest.editDestinationRedirectsToNewTargetAndResetsHits` | Automated |
| REQ-QR-03 | A deactivated code shows a friendly 404 to scanners; reactivating restores the redirect | `createScanReassignAndDeactivate` | `QrLifecycleE2ETest.deactivateAndReactivate` | Automated |
| REQ-QR-04 | Deleting needs confirmation; once deleted the code no longer redirects | `QrRedirectFlowTest.deletedCodeNoLongerRedirects` | `QrLifecycleE2ETest.deleteNeedsConfirmation` | Automated |
| REQ-QR-05 | Download returns one zip containing the PNG and the SVG; the QR decodes to the short URL | `QrRedirectFlowTest` (pngImageDecodesBackToTheShortUrl, svgImageIsServedAndBadImageParamsAreRejected), `UnitTests.svgPathMatchesTheRealQrMatrix` | `QrLifecycleE2ETest.downloadZipContainsPngAndSvg` | Automated (zip endpoint API test: gap) |
| REQ-QR-06 | List can be filtered by company, QR id and status, and is paginated (10 per page) | `QrRedirectFlowTest.codesBelongToACompanyAndCanBeFiltered` | `filtersByCompanyAndStatus`, `paginationSplitsLongLists` | Automated (QR-id filter: gap) |
| REQ-RED-01 | Scanning an active code returns HTTP 302 to the target with `Cache-Control: no-store` | `createScanReassignAndDeactivate` | `PublicRedirectE2ETest.activeCodeRedirects` | Automated |
| REQ-RED-02 | Unknown or malformed codes return a friendly HTML 404 | `QrRedirectFlowTest.unknownAndMalformedCodesGetThe404Page` | `PublicRedirectE2ETest.unknownCodesGetFriendly404` | Automated |
| REQ-SCAN-01 | Human scans increment the hit count; bots and inactive codes do not; the count resets only when the target URL changes | `QrRedirectFlowTest` (scansAreCountedBotsAndInactiveCodesAreNot, scanCountResetsOnlyWhenTheTargetActuallyChanges) | `QrLifecycleE2ETest` (scansAreCountedButBotsAreNot, editDestinationRedirectsToNewTargetAndResetsHits) | Automated |
