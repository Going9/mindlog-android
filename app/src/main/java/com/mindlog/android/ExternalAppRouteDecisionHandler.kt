package com.mindlog.android

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import dev.hotwire.navigation.routing.Router

/**
 * 외부 앱 스킴(intent://, kakao://, 등)을 처리하기 위한 라우터.
 * 
 * 카카오 OAuth 로그인 중 "카카오톡으로 로그인" 버튼을 누르면 
 * intent:// 또는 kakao:// 스킴이 호출됩니다.
 * 이 핸들러는 해당 스킴을 감지하여 외부 앱으로 전달합니다.
 */
class ExternalAppRouteDecisionHandler : Router.RouteDecisionHandler {
    
    companion object {
        private const val TAG = "ExternalAppHandler"
        
        // 외부 앱으로 처리할 스킴 목록
        private val EXTERNAL_SCHEMES = listOf(
            "intent",           // Android Intent 스킴 (카카오톡 등)
            "kakaolink",        // 카카오링크
            "kakaokompassauth", // 카카오 인증
            "kakaotalk",        // 카카오톡
            "kakao"             // 일반 카카오 스킴
        )
        
        // 마켓 패키지 ID 매핑
        private val PACKAGE_MARKET_MAP = mapOf(
            "com.kakao.talk" to "market://details?id=com.kakao.talk"
        )
    }
    
    override val name: String = "external-app-handler"
    
    override fun matches(location: String, configuration: NavigatorConfiguration): Boolean {
        val uri = Uri.parse(location)
        val scheme = uri.scheme?.lowercase() ?: return false
        val matches = EXTERNAL_SCHEMES.any { scheme.startsWith(it) }
        
        if (matches) {
            Log.d(TAG, "외부 앱 스킴 감지: $scheme -> $location")
        }
        return matches
    }
    
    override fun handle(
        location: String,
        configuration: NavigatorConfiguration,
        activity: HotwireActivity
    ): Router.Decision {
        Log.d(TAG, "외부 앱 스킴 처리: $location")
        
        val uri = Uri.parse(location)
        
        return when (uri.scheme?.lowercase()) {
            "intent" -> handleIntentScheme(location, activity)
            else -> handleExternalApp(uri, activity)
        }
    }
    
    /**
     * intent:// 스킴 처리
     * 예: intent://authorize#Intent;scheme=kakaokompassauth;...;end
     */
    private fun handleIntentScheme(location: String, activity: HotwireActivity): Router.Decision {
        try {
            // intent:// URI를 Intent로 파싱
            val intent = Intent.parseUri(location, Intent.URI_INTENT_SCHEME)
            
            // 앱이 설치되어 있으면 실행
            if (intent.resolveActivity(activity.packageManager) != null) {
                Log.d(TAG, "외부 앱 실행: ${intent.`package`}")
                activity.startActivity(intent)
            } else {
                // 앱이 설치되어 있지 않으면 마켓으로 이동
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                val packageName = intent.`package`
                
                when {
                    packageName != null && PACKAGE_MARKET_MAP.containsKey(packageName) -> {
                        Log.d(TAG, "마켓으로 이동: $packageName")
                        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PACKAGE_MARKET_MAP[packageName]))
                        activity.startActivity(marketIntent)
                    }
                    fallbackUrl != null -> {
                        Log.d(TAG, "Fallback URL로 이동: $fallbackUrl")
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
                        activity.startActivity(browserIntent)
                    }
                    packageName != null -> {
                        Log.d(TAG, "Play Store로 이동: $packageName")
                        val playStoreIntent = Intent(Intent.ACTION_VIEW, 
                            Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                        activity.startActivity(playStoreIntent)
                    }
                    else -> {
                        Log.w(TAG, "처리할 수 없는 Intent: $location")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Intent 파싱 실패: $location", e)
        }
        
        return Router.Decision.CANCEL
    }
    
    /**
     * 일반 외부 앱 스킴 처리 (kakao://, kakaolink://, 등)
     */
    private fun handleExternalApp(uri: Uri, activity: HotwireActivity): Router.Decision {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            
            if (intent.resolveActivity(activity.packageManager) != null) {
                Log.d(TAG, "외부 앱 실행: $uri")
                activity.startActivity(intent)
            } else {
                Log.w(TAG, "처리할 수 있는 앱이 없음: $uri")
                // 카카오톡이 없으면 마켓으로
                if (uri.scheme?.startsWith("kakao") == true) {
                    val marketIntent = Intent(Intent.ACTION_VIEW, 
                        Uri.parse("https://play.google.com/store/apps/details?id=com.kakao.talk"))
                    activity.startActivity(marketIntent)
                }
            }
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "외부 앱 실행 실패: $uri", e)
        }
        
        return Router.Decision.CANCEL
    }
}
