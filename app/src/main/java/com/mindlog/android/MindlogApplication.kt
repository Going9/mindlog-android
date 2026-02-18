package com.mindlog.android

import android.app.Application
import com.mindlog.android.navigation.MindlogWebFragment
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.config.PathConfiguration
import dev.hotwire.navigation.config.defaultFragmentDestination
import dev.hotwire.navigation.config.registerFragmentDestinations
import dev.hotwire.navigation.config.registerRouteDecisionHandlers
import dev.hotwire.navigation.fragments.HotwireWebBottomSheetFragment
import dev.hotwire.navigation.routing.AppNavigationRouteDecisionHandler
import dev.hotwire.navigation.routing.BrowserTabRouteDecisionHandler
import dev.hotwire.navigation.routing.SystemNavigationRouteDecisionHandler

class MindlogApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Hotwire.config.debugLoggingEnabled = BuildConfig.DEBUG
        Hotwire.config.webViewDebuggingEnabled = BuildConfig.DEBUG
        Hotwire.config.applicationUserAgentPrefix = "mindlog-android"

        Hotwire.loadPathConfiguration(
            context = this,
            location = PathConfiguration.Location(assetFilePath = "json/path-configuration.json")
        )

        Hotwire.registerRouteDecisionHandlers(
            AppNavigationRouteDecisionHandler(),
            BrowserTabRouteDecisionHandler(),
            SystemNavigationRouteDecisionHandler()
        )

        Hotwire.defaultFragmentDestination = MindlogWebFragment::class
        Hotwire.registerFragmentDestinations(
            MindlogWebFragment::class,
            HotwireWebBottomSheetFragment::class
        )
    }
}
