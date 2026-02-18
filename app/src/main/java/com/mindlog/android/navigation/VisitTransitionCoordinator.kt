package com.mindlog.android.navigation

import android.os.SystemClock

interface VisitTransitionOverlayRenderer {
    fun showVisitTransitionOverlay()
    fun hideVisitTransitionOverlay()
}

interface VisitTransitionCoordinatorOwner {
    val visitTransitionCoordinator: VisitTransitionCoordinator
}

class VisitTransitionCoordinator(
    private val renderer: VisitTransitionOverlayRenderer,
    private val minVisibleMs: Long = DEFAULT_MIN_VISIBLE_MS,
    private val maxVisibleMs: Long = DEFAULT_MAX_VISIBLE_MS,
    private val scheduler: VisitTransitionScheduler = MainLooperVisitTransitionScheduler(),
    private val nowMs: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private var activeVisitId = 0L
    private var startedAtMs = 0L
    private var terminalSignaled = false
    private var hideTask: CancellableTask? = null
    private var watchdogTask: CancellableTask? = null

    fun onVisitStarted() {
        runOnScheduler {
            val visitId = activeVisitId + 1L
            activeVisitId = visitId
            startedAtMs = nowMs()
            terminalSignaled = false
            cancelHideTimer()
            startWatchdog(visitId)
            renderer.showVisitTransitionOverlay()
        }
    }

    fun onVisitTerminal() {
        runOnScheduler {
            val visitId = activeVisitId
            if (visitId == 0L || terminalSignaled) {
                return@runOnScheduler
            }

            terminalSignaled = true
            val elapsedMs = nowMs() - startedAtMs
            val remainingMs = minVisibleMs - elapsedMs

            if (remainingMs <= 0L) {
                finishVisitIfActive(visitId)
                return@runOnScheduler
            }

            cancelHideTimer()
            hideTask = scheduler.schedule(remainingMs) {
                finishVisitIfActive(visitId)
            }
        }
    }

    fun reset() {
        runOnScheduler {
            cancelHideTimer()
            cancelWatchdogTimer()
            activeVisitId = 0L
            startedAtMs = 0L
            terminalSignaled = false
            renderer.hideVisitTransitionOverlay()
        }
    }

    private fun finishVisitIfActive(visitId: Long) {
        if (activeVisitId != visitId) {
            return
        }

        cancelHideTimer()
        cancelWatchdogTimer()
        activeVisitId = 0L
        startedAtMs = 0L
        terminalSignaled = false
        renderer.hideVisitTransitionOverlay()
    }

    private fun startWatchdog(visitId: Long) {
        cancelWatchdogTimer()
        watchdogTask = scheduler.schedule(maxVisibleMs) {
            finishVisitIfActive(visitId)
        }
    }

    private fun cancelHideTimer() {
        hideTask?.cancel()
        hideTask = null
    }

    private fun cancelWatchdogTimer() {
        watchdogTask?.cancel()
        watchdogTask = null
    }

    private fun runOnScheduler(block: () -> Unit) {
        if (scheduler.isSchedulerThread()) {
            block()
            return
        }

        scheduler.dispatch(block)
    }

    private companion object {
        const val DEFAULT_MIN_VISIBLE_MS = 500L
        const val DEFAULT_MAX_VISIBLE_MS = 10_000L
    }
}
