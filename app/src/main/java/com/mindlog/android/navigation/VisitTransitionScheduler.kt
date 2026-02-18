package com.mindlog.android.navigation

import android.os.Handler
import android.os.Looper

fun interface CancellableTask {
    fun cancel()
}

interface VisitTransitionScheduler {
    fun isSchedulerThread(): Boolean
    fun dispatch(block: () -> Unit)
    fun schedule(delayMs: Long, block: () -> Unit): CancellableTask
}

class MainLooperVisitTransitionScheduler(
    private val handler: Handler = Handler(Looper.getMainLooper())
) : VisitTransitionScheduler {

    override fun isSchedulerThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

    override fun dispatch(block: () -> Unit) {
        if (isSchedulerThread()) {
            block()
            return
        }

        handler.post(block)
    }

    override fun schedule(delayMs: Long, block: () -> Unit): CancellableTask {
        val runnable = Runnable(block)
        handler.postDelayed(runnable, delayMs)
        return CancellableTask { handler.removeCallbacks(runnable) }
    }
}
