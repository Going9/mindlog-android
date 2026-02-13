package com.mindlog.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import dev.hotwire.core.config.Hotwire
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.config.registerRouteDecisionHandlers
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import dev.hotwire.navigation.routing.AppNavigationRouteDecisionHandler
import dev.hotwire.navigation.routing.BrowserTabRouteDecisionHandler
import dev.hotwire.navigation.routing.SystemNavigationRouteDecisionHandler

class MainActivity : HotwireActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private var isRouteHandlersRegistered = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!isRouteHandlersRegistered) {
            Hotwire.registerRouteDecisionHandlers(
                AppNavigationRouteDecisionHandler(),
                BrowserTabRouteDecisionHandler(),
                SystemNavigationRouteDecisionHandler()
            )
            isRouteHandlersRegistered = true
            Log.d(TAG, "RouteDecisionHandler 등록 완료")
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handleDeepLink(intent)
    }

    override fun navigatorConfigurations() = listOf(
        NavigatorConfiguration(
            name = "main",
            startLocation = "${BuildConfig.BASE_URL}/",
            navigatorHostId = R.id.main_nav_host
        )
    )

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            Log.d(TAG, "딥링크 수신: $uri")

            if (uri.scheme == "mindlog" && uri.host == "auth" && uri.path == "/callback") {
                val token = uri.getQueryParameter("token")
                if (token != null) {
                    val exchangeUrl = "${BuildConfig.BASE_URL}/auth/exchange?token=$token"
                    Log.d(TAG, "토큰 교환 URL로 이동: $exchangeUrl")
                    delegate.currentNavigator?.route(exchangeUrl)
                } else {
                    Log.w(TAG, "딥링크에 토큰이 없습니다")
                }
            }
        }
    }
}
