package com.mindlog.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRouteBuilderTest {

    @Test
    fun isAuthCallback_OnlyReturnsTrueForExpectedSchemeHostPath() {
        assertTrue(AuthRouteBuilder.isAuthCallback("mindlog", "auth", "/callback"))
        assertFalse(AuthRouteBuilder.isAuthCallback("https", "auth", "/callback"))
        assertFalse(AuthRouteBuilder.isAuthCallback("mindlog", "other", "/callback"))
        assertFalse(AuthRouteBuilder.isAuthCallback("mindlog", "auth", "/wrong"))
    }

    @Test
    fun buildExchangeUrl_EncodesTokenValue() {
        val url = AuthRouteBuilder.buildExchangeUrl(
            "https://www.mindlog.blog",
            "a+b/c=d?e"
        )

        assertEquals(
            "https://www.mindlog.blog/auth/exchange?token=a%2Bb%2Fc%3Dd%3Fe",
            url
        )
    }
}
