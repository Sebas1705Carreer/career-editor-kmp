package dev.sebas1705.careereditor.security

import dev.sebas1705.careereditor.BuildConfig

actual val embeddedApiToken: String = BuildConfig.API_TOKEN

actual val appVersionName: String = BuildConfig.VERSION_NAME
