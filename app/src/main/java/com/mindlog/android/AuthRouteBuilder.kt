package com.mindlog.android

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object AuthRouteBuilder {
    private const val CALLBACK_SCHEME = "mindlog"
    private const val CALLBACK_HOST = "auth"
    private const val CALLBACK_PATH = "/callback"

    fun isAuthCallback(scheme: String?, host: String?, path: String?): Boolean {
        return scheme == CALLBACK_SCHEME && host == CALLBACK_HOST && path == CALLBACK_PATH
    }

    fun buildExchangeUrl(baseUrl: String, token: String): String {
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8)
        return "$baseUrl/auth/exchange?token=$encodedToken"
    }
}
