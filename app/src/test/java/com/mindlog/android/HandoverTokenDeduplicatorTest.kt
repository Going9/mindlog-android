package com.mindlog.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandoverTokenDeduplicatorTest {

    @Test
    fun shouldProcess_RejectsSameTokenWithinDuplicateWindow() {
        val deduplicator = HandoverTokenDeduplicator(duplicateWindowMillis = 5_000)

        assertTrue(deduplicator.shouldProcess("token-1", nowMillis = 1_000))
        assertFalse(deduplicator.shouldProcess("token-1", nowMillis = 2_000))
    }

    @Test
    fun shouldProcess_AllowsSameTokenAfterDuplicateWindow() {
        val deduplicator = HandoverTokenDeduplicator(duplicateWindowMillis = 5_000)

        assertTrue(deduplicator.shouldProcess("token-1", nowMillis = 1_000))
        assertTrue(deduplicator.shouldProcess("token-1", nowMillis = 7_000))
    }

    @Test
    fun shouldProcess_AllowsDifferentToken() {
        val deduplicator = HandoverTokenDeduplicator(duplicateWindowMillis = 5_000)

        assertTrue(deduplicator.shouldProcess("token-1", nowMillis = 1_000))
        assertTrue(deduplicator.shouldProcess("token-2", nowMillis = 2_000))
    }
}
