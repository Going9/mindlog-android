package com.mindlog.android.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisitTransitionCoordinatorTest {

    @Test
    fun onVisitStarted_ShowsOverlayImmediately() {
        val scheduler = FakeScheduler()
        val renderer = FakeRenderer()
        val coordinator = newCoordinator(renderer, scheduler)

        coordinator.onVisitStarted()

        assertEquals(1, renderer.showCount)
        assertTrue(renderer.isVisible)
    }

    @Test
    fun onVisitTerminal_DelaysHideUntilMinVisibleTime() {
        val scheduler = FakeScheduler()
        val renderer = FakeRenderer()
        val coordinator = newCoordinator(renderer, scheduler)

        coordinator.onVisitStarted()
        scheduler.advanceBy(120)
        coordinator.onVisitTerminal()

        assertEquals(0, renderer.hideCount)

        scheduler.advanceBy(379)
        assertEquals(0, renderer.hideCount)

        scheduler.advanceBy(1)
        assertEquals(1, renderer.hideCount)
        assertFalse(renderer.isVisible)
    }

    @Test
    fun onVisitTerminal_HidesImmediatelyAfterMinVisibleTime() {
        val scheduler = FakeScheduler()
        val renderer = FakeRenderer()
        val coordinator = newCoordinator(renderer, scheduler)

        coordinator.onVisitStarted()
        scheduler.advanceBy(600)

        coordinator.onVisitTerminal()

        assertEquals(1, renderer.hideCount)
        assertFalse(renderer.isVisible)
    }

    @Test
    fun onVisitStarted_CancelsPreviousPendingHideAndTracksLatestVisit() {
        val scheduler = FakeScheduler()
        val renderer = FakeRenderer()
        val coordinator = newCoordinator(renderer, scheduler)

        coordinator.onVisitStarted()
        scheduler.advanceBy(100)
        coordinator.onVisitTerminal()

        scheduler.advanceBy(200)
        coordinator.onVisitStarted()

        scheduler.advanceBy(300)
        assertEquals(0, renderer.hideCount)

        coordinator.onVisitTerminal()
        scheduler.advanceBy(199)
        assertEquals(0, renderer.hideCount)

        scheduler.advanceBy(1)
        assertEquals(1, renderer.hideCount)
        assertFalse(renderer.isVisible)
    }

    @Test
    fun watchdog_HidesOverlayWhenTerminalSignalIsMissing() {
        val scheduler = FakeScheduler()
        val renderer = FakeRenderer()
        val coordinator = newCoordinator(renderer, scheduler, maxVisibleMs = 1_000)

        coordinator.onVisitStarted()
        scheduler.advanceBy(999)
        assertTrue(renderer.isVisible)

        scheduler.advanceBy(1)
        assertEquals(1, renderer.hideCount)
        assertFalse(renderer.isVisible)
    }

    @Test
    fun reset_HidesOverlayAndClearsActiveVisit() {
        val scheduler = FakeScheduler()
        val renderer = FakeRenderer()
        val coordinator = newCoordinator(renderer, scheduler)

        coordinator.onVisitStarted()
        coordinator.reset()

        assertEquals(1, renderer.hideCount)
        assertFalse(renderer.isVisible)

        coordinator.onVisitTerminal()
        assertEquals(1, renderer.hideCount)
    }

    private fun newCoordinator(
        renderer: FakeRenderer,
        scheduler: FakeScheduler,
        minVisibleMs: Long = 500,
        maxVisibleMs: Long = 10_000
    ): VisitTransitionCoordinator {
        return VisitTransitionCoordinator(
            renderer = renderer,
            minVisibleMs = minVisibleMs,
            maxVisibleMs = maxVisibleMs,
            scheduler = scheduler,
            nowMs = { scheduler.currentTimeMs }
        )
    }

    private class FakeRenderer : VisitTransitionOverlayRenderer {
        var showCount: Int = 0
        var hideCount: Int = 0
        var isVisible: Boolean = false

        override fun showVisitTransitionOverlay() {
            showCount += 1
            isVisible = true
        }

        override fun hideVisitTransitionOverlay() {
            hideCount += 1
            isVisible = false
        }
    }

    private class FakeScheduler : VisitTransitionScheduler {
        var currentTimeMs: Long = 0
            private set

        private data class ScheduledItem(
            val runAtMs: Long,
            val block: () -> Unit,
            var cancelled: Boolean = false
        )

        private val queue = mutableListOf<ScheduledItem>()

        override fun isSchedulerThread(): Boolean = true

        override fun dispatch(block: () -> Unit) {
            block()
        }

        override fun schedule(delayMs: Long, block: () -> Unit): CancellableTask {
            val item = ScheduledItem(
                runAtMs = currentTimeMs + delayMs.coerceAtLeast(0),
                block = block
            )
            queue.add(item)
            queue.sortBy { it.runAtMs }
            return CancellableTask { item.cancelled = true }
        }

        fun advanceBy(deltaMs: Long) {
            currentTimeMs += deltaMs.coerceAtLeast(0)
            runDueTasks()
        }

        private fun runDueTasks() {
            while (true) {
                val nextIndex = queue.indexOfFirst { it.runAtMs <= currentTimeMs }
                if (nextIndex < 0) {
                    return
                }

                val item = queue.removeAt(nextIndex)
                if (!item.cancelled) {
                    item.block.invoke()
                }
            }
        }
    }
}
