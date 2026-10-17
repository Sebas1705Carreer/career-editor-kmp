# career-editor-kmp

KMP desktop and Android editor for managing data in [career-api](https://api.sebas1705.dev) — the personal career portfolio API.

**Platforms:** Android · Desktop (JVM)

---

## Stack

- **Kotlin Multiplatform** — shared business logic across platforms
- **Compose Multiplatform** — shared UI for Android and Desktop
- **Ktor Client** — HTTP communication with career-api
- **Clean Architecture** — domain / data / presentation separation

---

## Structure

```
composeApp/
├── androidMain/     # Android entry point
├── desktopMain/     # Desktop entry point
└── commonMain/      # Shared UI, logic and network layer
```

---

## Getting started

```bash
git clone https://github.com/Sebas1705Carreer/career-editor-kmp.git
cd career-editor-kmp
```

**Android:** open in Android Studio and run the `composeApp` configuration.

**Desktop:**
```bash
./gradlew desktopRun
```

> Requires Java 17+ and Android Studio Hedgehog or later.

---

## Related

- [career-api-worker](https://github.com/Sebas1705Carreer/career-api-worker) — backend API (Cloudflare Workers + KV)
## The API token

The token that writes to the career API lives in **Doppler**, project `folio` (`API_TOKEN`), and is compiled into the Android app, so it opens already signed in and nobody types it on a phone. Build with it from the repo root (`doppler.yaml` binds `folio/dev`):

```bash
doppler run -- ./gradlew :composeApp:installDebug
```

Without Doppler the app still builds and asks for the token on first launch, as before. The desktop app never embeds it (its installers are published on a public release) and keeps the login screen. Rotating the token: change `API_SECRET` in Cloudflare and `API_TOKEN` in Doppler (`dev` and `prd`), then tag a new version.

> **Anyone who has the APK can extract the token.** That is why the APK is never attached to a GitHub release (this repository is public): it only goes to Firebase App Distribution, to the testers invited there.

## Publishing a release

One tag `vX.Y.Z` triggers two workflows:

| Workflow | Builds | Goes to |
|---|---|---|
| `release.yml` | Windows, macOS and Linux installers (no token inside) | the GitHub release |
| `release-apk.yml` | signed Android APK with the token | **Firebase App Distribution** (group `testers`) |

```bash
git tag v1.2.0
git push origin v1.2.0
```

`versionName` is the tag and `versionCode` is the workflow run number. The only GitHub secret is `DOPPLER_TOKEN` (read-only service token of `folio/prd`); everything else is read from Doppler:

| Doppler secret (`folio/prd`) | Used for |
|---|---|
| `API_TOKEN` | compiled into the APK |
| `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` | signing the APK |
| `FIREBASE_APP_ID`, `FIREBASE_SERVICE_ACCOUNT_JSON` | App Distribution (service account with the *Firebase App Distribution Admin* role) |

The workflow checks that every one exists and fails naming the missing one.

**Back up the keystore** (`folio-release.jks`) somewhere you will still have it in five years. If it is lost, an installed app can only be updated by uninstalling it first. Neither the keystore nor any password belongs in this repository.
