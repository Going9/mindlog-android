package com.mindlog.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import dev.hotwire.core.config.Hotwire
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.config.registerRouteDecisionHandlers
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import dev.hotwire.navigation.routing.AppNavigationRouteDecisionHandler
import dev.hotwire.navigation.routing.BrowserTabRouteDecisionHandler
import dev.hotwire.navigation.routing.SystemNavigationRouteDecisionHandler
import dev.hotwire.navigation.util.applyDefaultImeWindowInsets

class MainActivity : HotwireActivity() {

    companion object {
        private const val TAG = "MainActivity"
        // 실제 서버 URL로 변경하세요. 에뮬레이터에서는 10.0.2.2, 실기기에서는 실제 IP 사용


        //private const val BASE_URL = "https://www.mindlog.blog"
        private const val BASE_URL = "https://localhost:8443"


        // RouteDecisionHandler 등록은 앱 시작 시 한 번만 수행
        private var isRouteHandlersRegistered = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // RouteDecisionHandler 등록 (앱 시작 시 한 번만)
        if (!isRouteHandlersRegistered) {
            Hotwire.registerRouteDecisionHandlers(
                ExternalAppRouteDecisionHandler(),  // 외부 앱 스킴 (카카오톡 등) 처리
                AuthRouteDecisionHandler(),  // 로그인 경로를 Custom Tab으로 열기
                AppNavigationRouteDecisionHandler(),
                BrowserTabRouteDecisionHandler(),
                SystemNavigationRouteDecisionHandler()
            )
            isRouteHandlersRegistered = true
            Log.d(TAG, "RouteDecisionHandler 등록 완료")
        }
        
        // Edge-to-Edge 비활성화: WebView에서 키보드가 올라올 때 adjustResize가 정상 동작하도록 함
        // enableEdgeToEdge()  // WebView + Edge-to-Edge 조합에서 키보드 인셋 문제 발생
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // findViewById<View>(R.id.main_nav_host).applyDefaultImeWindowInsets()  // Edge-to-Edge 비활성화 시 불필요

        // 앱 실행 시 딥링크로 시작된 경우 처리
        handleDeepLink(intent)
    }

    override fun navigatorConfigurations() = listOf(
        NavigatorConfiguration(
            name = "main",
            startLocation = "$BASE_URL/",
            navigatorHostId = R.id.main_nav_host
        )
    )

    // 앱이 이미 실행 중일 때 딥링크를 받은 경우 처리
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    /**
     * 딥링크를 처리합니다.
     * 
     * mindlog://auth/callback?token=xxx 형태의 URL을 받으면,
     * WebView가 /auth/exchange?token=xxx에 접속하여 세션을 생성합니다.
     */
    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            Log.d(TAG, "딥링크 수신: $uri")

            if (uri.scheme == "mindlog" && uri.host == "auth" && uri.path == "/callback") {
                val token = uri.getQueryParameter("token")
                if (token != null) {
                    // 백엔드 토큰 교환 URL
                    val exchangeUrl = "$BASE_URL/auth/exchange?token=$token"
                    Log.d(TAG, "토큰 교환 URL로 이동: $exchangeUrl")

                    // 현재 Navigator를 사용하여 해당 URL로 이동
                    // WebView가 이 URL에 접속하면 백엔드에서 세션(쿠키)을 생성합니다
                    delegate.currentNavigator?.route(exchangeUrl)
                } else {
                    Log.w(TAG, "딥링크에 토큰이 없습니다")
                }
            }
        }
    }
}