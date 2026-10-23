package com.dan1eidtj.mayas.mesh

internal object MeshRetryPolicy {
    private val delaysMs = longArrayOf(2_000L, 5_000L, 10_000L, 20_000L, 40_000L, 60_000L)

    fun delayMs(attempt: Int): Long = delaysMs[attempt.coerceIn(0, delaysMs.lastIndex)]

    fun shouldRetry(now: Long, lastAttemptAt: Long, attemptCount: Int): Boolean {
        if (lastAttemptAt <= 0L) return true
        val delay = delayMs((attemptCount - 1).coerceAtLeast(0))
        return now >= lastAttemptAt && now - lastAttemptAt >= delay
    }

    fun nextAttempt(attempt: Int): Int {
        val safeAttempt = attempt.coerceAtLeast(0)
        return if (safeAttempt >= delaysMs.lastIndex) delaysMs.lastIndex else safeAttempt + 1
    }
}
