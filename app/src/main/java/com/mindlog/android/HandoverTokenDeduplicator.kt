package com.mindlog.android

internal class HandoverTokenDeduplicator(
    private val duplicateWindowMillis: Long = 5_000
) {
    private var lastToken: String? = null
    private var lastHandledAtMillis: Long = 0

    fun shouldProcess(token: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val isDuplicate = token == lastToken && (nowMillis - lastHandledAtMillis) <= duplicateWindowMillis
        if (isDuplicate) {
            return false
        }

        lastToken = token
        lastHandledAtMillis = nowMillis
        return true
    }
}
