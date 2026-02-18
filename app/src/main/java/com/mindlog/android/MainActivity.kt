package com.mindlog.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.mindlog.android.navigation.VisitTransitionCoordinator
import com.mindlog.android.navigation.VisitTransitionCoordinatorOwner
import com.mindlog.android.navigation.VisitTransitionOverlayRenderer
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.navigator.NavigatorConfiguration
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

internal class HandoverTokenDeduplicator(
    private val duplicateWindowMillis: Long = 5_000
) {
    private var lastToken: String? = null
    private var lastHandledAtMillis: Long = 0

    fun shouldProcess(token: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val isDuplicate = token == lastToken && (nowMillis - lastHandledAtMillis) <= duplicateWindowMillis
        if (isDuplicate) {
            return false
        }

        lastToken = token
        lastHandledAtMillis = nowMillis
        return true
    }
}

class MainActivity : HotwireActivity(), VisitTransitionOverlayRenderer, VisitTransitionCoordinatorOwner {

    companion object {
        private const val TAG = "MainActivity"
        private val handoverTokenDeduplicator = HandoverTokenDeduplicator()
    }

    override val visitTransitionCoordinator: VisitTransitionCoordinator by lazy {
        VisitTransitionCoordinator(this)
    }

    private lateinit var transitionOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        transitionOverlay = findViewById(R.id.native_transition_overlay)
        Log.d(TAG, "앱 시작 BASE_URL=${BuildConfig.BASE_URL}")

        handleDeepLink(intent)
    }

    override fun navigatorConfigurations() = listOf(
        NavigatorConfiguration(
            name = "main",
            startLocation = "${BuildConfig.BASE_URL}/?source=app",
            navigatorHostId = R.id.main_nav_host
        )
    )

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    override fun onDestroy() {
        visitTransitionCoordinator.reset()
        super.onDestroy()
    }

    override fun showVisitTransitionOverlay() {
        if (!::transitionOverlay.isInitialized) {
            return
        }

        transitionOverlay.clearAnimation()
        transitionOverlay.alpha = 1f
        transitionOverlay.visibility = View.VISIBLE
    }

    override fun hideVisitTransitionOverlay() {
        if (!::transitionOverlay.isInitialized) {
            return
        }

        transitionOverlay.clearAnimation()
        transitionOverlay.visibility = View.GONE
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        Log.d(TAG, "딥링크 수신: $uri")

        if (!AuthRouteBuilder.isAuthCallback(uri.scheme, uri.host, uri.path)) {
            return
        }

        val token = uri.getQueryParameter("token")
        if (token.isNullOrBlank()) {
            Log.w(TAG, "딥링크에 유효한 토큰이 없습니다")
            return
        }

        if (!handoverTokenDeduplicator.shouldProcess(token)) {
            Log.w(TAG, "중복 handover 토큰 수신으로 교환 요청을 건너뜁니다")
            intent.data = null
            return
        }

        val exchangeUrl = AuthRouteBuilder.buildExchangeUrl(BuildConfig.BASE_URL, token)
        Log.d(TAG, "토큰 교환 URL로 이동: $exchangeUrl")
        delegate.currentNavigator?.route(exchangeUrl)
        intent.data = null
    }
}
