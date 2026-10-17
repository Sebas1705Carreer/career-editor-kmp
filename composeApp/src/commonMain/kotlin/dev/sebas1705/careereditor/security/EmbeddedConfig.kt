package dev.sebas1705.careereditor.security

/**
 * API token compiled into the app at build time (Doppler `folio` → `API_TOKEN`), or "" when
 * the build did not receive one. With it the app opens already signed in, so the token is never
 * typed on a phone. Only the Android build embeds it: the desktop installers are published on a
 * public release, where anyone could extract it.
 */
expect val embeddedApiToken: String
