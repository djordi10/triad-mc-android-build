package agentic.triad.missioncontrol.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import agentic.triad.missioncontrol.TriadApp
import agentic.triad.missioncontrol.mcp.CheckupRun
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The native analog of the web client's Telegram digest. On its interval it runs the checkup via
 * `get_checkup`, applies the [BroadcastPolicy] (Off / RED-only / on-change / every-run), and posts a
 * local-notification digest — verdict 🟢🟡🔴, the non-green components, and a headline stat line.
 * In LIVE it also `record_checkup`s the run (append-only, graceful if the tool is absent).
 *
 * The repository + client are resolved from the [TriadApp] container at run time, so WorkManager can
 * instantiate the worker with the default constructor (no custom WorkerFactory needed).
 */
class BroadcastWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TriadApp
        val settings = BroadcastSettings(applicationContext)

        val env = app.repository.tool("get_checkup").envelope
        if (!env.ok) return Result.success()  // honest unavailable — nothing to broadcast
        val data = env.data?.jsonObject ?: return Result.success()

        val verdict = data["verdict"]?.jsonPrimitive?.content?.uppercase() ?: "UNKNOWN"
        val reds = nonGreenNames(data, "reds", data["components"])
        val previous = settings.lastVerdict
        settings.lastVerdict = verdict

        // In LIVE, write the run back to the system (append-only; a DEMO run has no client).
        runCatching { app.client?.recordCheckup(CheckupRun(verdict = verdict)) }

        if (settings.policy.shouldPost(verdict, previous)) {
            val headline = headlineStats(app)
            Notifications.postDigest(
                applicationContext,
                title = "${emoji(verdict)} TRIAD checkup — $verdict",
                body = buildString {
                    append(reds.size).append(" non-green")
                    if (reds.isNotEmpty()) append(": ").append(reds.take(6).joinToString(", "))
                    append("\n").append(headline)
                },
            )
        }
        return Result.success()
    }

    private fun emoji(verdict: String) = when (verdict) {
        "GREEN" -> "🟢"; "YELLOW" -> "🟡"; "RED" -> "🔴"; else -> "⚪"
    }

    /** Names of the non-green components — from an explicit `reds` array if present, else derived
     *  from `components[*].status != green`. Handles a count-only `reds` (returns empty names). */
    private fun nonGreenNames(
        data: JsonObject, redsKey: String, components: kotlinx.serialization.json.JsonElement?,
    ): List<String> {
        (data[redsKey] as? JsonArray)?.let { arr ->
            return arr.mapNotNull { el ->
                (el as? JsonObject)?.get("name")?.jsonPrimitive?.content
                    ?: (el as? JsonPrimitive)?.content
            }
        }
        return (components as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val status = o["status"]?.jsonPrimitive?.content?.lowercase()
            if (status != null && status != "green") o["name"]?.jsonPrimitive?.content else null
        }
    }

    /** open / validity headline pulled from get_system_overview (best-effort; honest dashes). */
    private suspend fun headlineStats(app: TriadApp): String {
        val o = app.repository.tool("get_system_overview").envelope.data?.jsonObject
        val open = o?.get("open_positions")?.jsonPrimitive?.content ?: "—"
        val validity = o?.get("validity_pct")?.jsonPrimitive?.content ?: "—"
        return "open $open · validity $validity%"
    }

    private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())
}
