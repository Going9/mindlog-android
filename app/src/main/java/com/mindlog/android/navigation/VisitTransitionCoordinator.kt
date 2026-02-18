package com.mindlog.android.navigation

import android.os.Handler
import android.os.Looper
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
    private val nowMs: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeVisitId = 0L
    private var startedAtMs = 0L
    private var terminalSignaled = false
    private var hideRunnable: Runnable? = null
    private var watchdogRunnable: Runnable? = null

    fun onVisitStarted() {
        runOnMain {
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
        runOnMain {
            val visitId = activeVisitId
            if (visitId == 0L || terminalSignaled) {
                return@runOnMain
            }

            terminalSignaled = true
            val elapsedMs = nowMs() - startedAtMs
            val remainingMs = minVisibleMs - elapsedMs

            if (remainingMs <= 0L) {
                finishVisitIfActive(visitId)
                return@runOnMain
            }

            cancelHideTimer()
            hideRunnable = Runnable {
                finishVisitIfActive(visitId)
            }.also { runnable ->
                mainHandler.postDelayed(runnable, remainingMs)
            }
        }
    }

    fun reset() {
        runOnMain {
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
        watchdogRunnable = Runnable {
            finishVisitIfActive(visitId)
        }.also { runnable ->
            mainHandler.postDelayed(runnable, maxVisibleMs)
        }
    }

    private fun cancelHideTimer() {
        hideRunnable?.let(mainHandler::removeCallbacks)
        hideRunnable = null
    }

    private fun cancelWatchdogTimer() {
        watchdogRunnable?.let(mainHandler::removeCallbacks)
        watchdogRunnable = null
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
            return
        }

        mainHandler.post(block)
    }

    private companion object {
        const val DEFAULT_MIN_VISIBLE_MS = 500L
        const val DEFAULT_MAX_VISIBLE_MS = 10_000L
    }
}
