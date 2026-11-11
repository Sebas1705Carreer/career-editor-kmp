# 0001. Replace the single embedded API secret with per-device write tokens

- **Status:** proposed (nothing in this note is implemented)
- **Date:** 2026-11-11
- **Area:** security, `career-api-worker` + Folio (`career-editor-kmp`)

## Context

Facts about today's setup:

- The API (`career-api-worker`) accepts every write (`POST`/`PUT`/`PATCH`/`DELETE`) with one shared bearer token, the Worker secret `API_SECRET`.
- The release build embeds the same value in the APK as `BuildConfig.API_TOKEN` (Doppler `folio` -> `API_TOKEN`, see `composeApp/build.gradle.kts`). The APK goes to testers through Firebase App Distribution, never to a public release, but it is still a file in testers' hands.
- Anyone who has the APK can extract the token (the README says so) and then has full write access to all 8 entities: `languages`, `personal`, `jobs`, `projects`, `skills`, `education`, `certifications`, `soft-skills`. The token cannot be told apart per tester or per device, cannot be revoked for one person, and rotating it means changing Cloudflare and Doppler and shipping a new APK to everyone.
- The API has no write log, so there is no way to see who changed what. A weekly export of the data (`/all`) is the only recovery path (career-api-worker PR "Harden the API").
- The Worker will rate-limit protected routes per IP, which slows guessing but does nothing about a legitimate token being shared.

Threat being addressed: a tester (or someone who obtains an APK or a tester's phone) overwriting or wiping portfolio data, with no way to cut that one access off without disrupting everybody, and no trace. Not addressed here: an attacker who owns the owner's Doppler/Cloudflare accounts.

## Options

**A. Keep the single secret (status quo).** Zero work. Every weakness above stays. Acceptable only while the tester list is just the owner.

**B. One static token per tester, embedded in a per-tester APK.** Simple on the Worker (a token table), but needs one build per tester, and the token is still extractable from that APK. Revocation is per tester, which is the gain. High release cost, so not recommended.

**C. Per-device tokens obtained at first launch with a one-time invite code.** The APK contains no write secret. The owner creates a short-lived single-use invite (admin call authenticated with the existing `API_SECRET`). The app exchanges the invite for a random 256-bit device token, stores it in the existing Android Keystore-backed `SecureStorage`, and sends it as the bearer token. The Worker stores only a SHA-256 hash per token (id, label, created, last used, optional scope) in KV and checks it with one KV read per write. Revoking a device deletes its record.

**D. Identity-based auth (Firebase Auth ID tokens or Cloudflare Access).** The Worker verifies a short-lived signed token and checks the user against an allowlist. Best long-term properties (no long-lived secret on the device, per-user audit, instant revocation), but more moving parts: JWT/JWKS verification in the Worker, a sign-in flow in the app, and a dependency on Google or Cloudflare being reachable. Worth it if the tester list grows beyond a handful of people.

**E. Play Integrity / App Check attestation on top of any of the above.** Raises the cost of using a stolen token from a script instead of the genuine app. Does not replace revocation or audit; add later if abuse appears.

## Recommendation

Adopt **C**, as the step that fixes the real problem (a secret baked into a distributed file) at the smallest cost, and keep **D** as the path if the tester group grows.

Sketch of C:

1. Worker: new endpoints `POST /admin/invites` (needs `API_SECRET`; returns a code valid 15 minutes, one use), `POST /enroll` (code + device name -> `{ id, token }`, the token shown once), `GET /admin/tokens` and `DELETE /admin/tokens/:id` (need `API_SECRET`). Token format `fol_<id>_<secret>` so the Worker finds the record by id in O(1) and compares hashes in constant time.
2. Worker: the write routes accept either a valid device token or `API_SECRET`. `API_SECRET` stays as the admin and CI credential (the `data-updates/apply.mjs` script), and is no longer put in the APK.
3. Optional scopes per token: `read` or `write`, and later a list of entities, so a tester who only edits skills cannot touch `personal`. Cheap to add once records exist.
4. Audit: record `last_used` (updated at most hourly to limit KV writes) and, if wanted, a short append-only log of writes by token id. Rate limit by token id instead of only by IP.
5. Folio: remove `API_TOKEN` from the build and from the release workflow; add an "enter invite code" step to the first-launch screen; keep the existing `GET /auth/check` probe to validate a stored token.
6. Migration: ship the Worker side first (both credentials accepted), then a Folio release without the embedded token, then rotate `API_SECRET` so every old APK stops working.

## Consequences

- Benefits: no write secret in any APK; per-device revoke and audit; rotating no longer means rebuilding for everyone; a leaked token exposes one device, and (with scopes) less than everything.
- Costs: new Worker code and tests, one extra KV read per write, an enrolment screen, and an owner step to create invites. Tokens live in KV, so KV also becomes part of what must be backed up.
- Limits, stated honestly: a device that is enrolled can still write what its scope allows, and a rooted device can read its own token from storage. This reduces blast radius and adds revocation and traceability; it does not make a malicious tester harmless. Backups (`/all` export) stay necessary.
- How it would be checked: Worker tests that every write route rejects a revoked, unknown, wrong-scope or expired-invite credential; a test that an invite works once; a CI grep that the release build no longer reads `API_TOKEN`.

## Alternatives considered

A, B, D and E above. Not recorded: any earlier discussion of these options.
