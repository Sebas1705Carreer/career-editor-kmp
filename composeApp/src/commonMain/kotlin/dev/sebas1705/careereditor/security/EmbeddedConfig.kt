package dev.sebas1705.careereditor.security

/**
 * API token compiled into the app at build time (Doppler `folio` → `API_TOKEN`), or "" when
 * the build did not receive one. With it the app opens already signed in, so the token is never
 * typed on a phone. Anyone holding the APK can extract it, so the APK only goes to Firebase App
 * Distribution, never to a public release.
 */
expect val embeddedApiToken: String

/** versionName of this build (shown in Settings instead of a hardcoded string). */
expect val appVersionName: String
