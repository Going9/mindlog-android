package com.mindlog.android

import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import dev.hotwire.navigation.routing.Router

/**
 * /auth/login으로 시작하는 경로를 Custom Tab(외부 브라우저)으로 열기 위한 라우터.
 * 
 * Hotwire WebView에서 Turbo가 주입되면 OAuth redirect가 CORS 오류를 발생시키므로,
 * 로그인 관련 경로는 Custom Tab에서 처리하여 Turbo 주입을 우회합니다.
 */
class AuthRouteDecisionHandler : Router.RouteDecisionHandler {
    
    companion object {
        private const val TAG = "AuthRouteDecisionHandler"
        private const val AUTH_LOGIN_PATH_PREFIX = "/auth/login"
        private const val DEBOUNCE_MS = 2000L  // 2초간 중복 클릭 방지
        
        // 마지막 Custom Tab 열기 시간 (중복 방지)
        @Volatile
        private var lastOpenTime = 0L
    }
    
    override val name: String = "auth-login-handler"
    
    override fun matches(location: String, configuration: NavigatorConfiguration): Boolean {
        val uri = Uri.parse(location)
        val matches = uri.path?.startsWith(AUTH_LOGIN_PATH_PREFIX) == true
        if (matches) {
            Log.d(TAG, "로그인 경로 감지: $location -> Custom Tab으로 열기")
        }
        return matches
    }
    
    override fun handle(
        location: String,
        configuration: NavigatorConfiguration,
        activity: HotwireActivity
    ): Router.Decision {
        // 중복 클릭 방지: 마지막 열기로부터 일정 시간 내 재클릭 무시
        val now = System.currentTimeMillis()
        if (now - lastOpenTime < DEBOUNCE_MS) {
            Log.d(TAG, "중복 클릭 무시: ${now - lastOpenTime}ms 전에 이미 열림")
            return Router.Decision.CANCEL
        }
        lastOpenTime = now
        
        // source=app 파라미터 추가 (서버가 앱 요청임을 인식하도록)
        val uri = Uri.parse(location)
        val modifiedUri = uri.buildUpon()
            .appendQueryParameter("source", "app")
            .build()
        
        Log.d(TAG, "Custom Tab에서 열기: $modifiedUri")
        
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        
        customTabsIntent.launchUrl(activity, modifiedUri)
        
        return Router.Decision.CANCEL
    }
}
