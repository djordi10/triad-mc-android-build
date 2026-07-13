package agentic.triad.missioncontrol.work

import android.content.Context

/** When the checkup posts a digest — mirrors the web client's Broadcast policy. */
enum class BroadcastPolicy {
    OFF,          // never post
    RED_ONLY,     // post only when the verdict is RED
    ON_CHANGE,    // post when the verdict differs from the last run
    EVERY_RUN;    // post every run

    /** Should a run with [verdict], following [previous], post under this policy? */
    fun shouldPost(verdict: String, previous: String?): Boolean = when (this) {
        OFF -> false
        RED_ONLY -> verdict.equals("RED", ignoreCase = true)
        ON_CHANGE -> !verdict.equals(previous, ignoreCase = true)
        EVERY_RUN -> true
    }
}

/**
 * Broadcast settings persisted locally (policy + interval). Kept tiny and plain — the last verdict
 * lives here too so ON_CHANGE has something to compare against across worker runs.
 */
class BroadcastSettings(context: Context) {
    private val prefs = context.getSharedPreferences("triad_broadcast", Context.MODE_PRIVATE)

    var policy: BroadcastPolicy
        get() = runCatching { BroadcastPolicy.valueOf(prefs.getString(KEY_POLICY, null) ?: "") }
            .getOrDefault(BroadcastPolicy.RED_ONLY)
        set(v) = prefs.edit().putString(KEY_POLICY, v.name).apply()

    /** Interval in minutes. WorkManager's periodic floor is 15 min; sub-15 needs a foreground
     *  service or self-rescheduling one-time work (see [BroadcastScheduler]). */
    var intervalMinutes: Long
        get() = prefs.getLong(KEY_INTERVAL, 15L)
        set(v) = prefs.edit().putLong(KEY_INTERVAL, v).apply()

    var lastVerdict: String?
        get() = prefs.getString(KEY_LAST, null)
        set(v) = prefs.edit().putString(KEY_LAST, v).apply()

    private companion object {
        const val KEY_POLICY = "policy"
        const val KEY_INTERVAL = "interval_min"
        const val KEY_LAST = "last_verdict"
    }
}
