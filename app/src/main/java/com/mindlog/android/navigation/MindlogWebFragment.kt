package com.mindlog.android.navigation

import android.view.View
import android.widget.Space
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.fragments.HotwireWebFragment

@HotwireDestinationDeepLink(uri = "hotwire://fragment/web")
class MindlogWebFragment : HotwireWebFragment() {

    private val transitionCoordinator: VisitTransitionCoordinator?
        get() = (activity as? VisitTransitionCoordinatorOwner)?.visitTransitionCoordinator

    override fun createProgressView(location: String): View {
        // Progress is rendered by an Activity-level overlay managed by VisitTransitionCoordinator.
        return Space(requireContext()).apply {
            visibility = View.GONE
        }
    }

    override fun onVisitStarted(location: String) {
        super.onVisitStarted(location)
        transitionCoordinator?.onVisitStarted()
    }

    override fun onVisitCompleted(location: String, completedOffline: Boolean) {
        super.onVisitCompleted(location, completedOffline)
        transitionCoordinator?.onVisitTerminal()
    }

    override fun onVisitErrorReceived(location: String, error: VisitError) {
        super.onVisitErrorReceived(location, error)
        transitionCoordinator?.onVisitTerminal()
    }

    override fun onVisitErrorReceivedWithCachedSnapshotAvailable(location: String, error: VisitError) {
        super.onVisitErrorReceivedWithCachedSnapshotAvailable(location, error)
        transitionCoordinator?.onVisitTerminal()
    }
}
