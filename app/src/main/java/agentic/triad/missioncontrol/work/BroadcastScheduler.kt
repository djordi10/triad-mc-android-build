package agentic.triad.missioncontrol.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the checkup broadcast. WorkManager's periodic floor is 15 minutes, so the stored
 * interval is clamped up to that; the web client's 5-minute cadence would need a foreground service
 * or self-rescheduling one-time work (a documented next step). The work is unique and KEEPs the
 * existing schedule across boots so we never stack duplicates.
 */
object BroadcastScheduler {
    private const val WORK_NAME = "triad_checkup_broadcast"
    private const val MIN_PERIOD_MIN = 15L

    fun schedule(context: Context) {
        val minutes = BroadcastSettings(context).intervalMinutes.coerceAtLeast(MIN_PERIOD_MIN)
        val request = PeriodicWorkRequestBuilder<BroadcastWorker>(minutes, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /** Re-arm after a settings change (interval/policy edited in the Broadcast sheet). */
    fun reschedule(context: Context) {
        val minutes = BroadcastSettings(context).intervalMinutes.coerceAtLeast(MIN_PERIOD_MIN)
        val request = PeriodicWorkRequestBuilder<BroadcastWorker>(minutes, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
