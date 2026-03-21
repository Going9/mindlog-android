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
        transitionOverlay.animate()
            .alpha(0f)
            .setDuration(120)
            .withEndAction { transitionOverlay.visibility = View.GONE }
            .start()
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
