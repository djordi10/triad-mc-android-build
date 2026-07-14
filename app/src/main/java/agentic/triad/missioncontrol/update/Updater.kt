package agentic.triad.missioncontrol.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * In-app auto-update. On launch the app asks GitHub for the newest published build; if its version
 * is higher than what's installed, it downloads that APK and hands it to the system installer (a
 * one-tap confirm — Android never installs a sideloaded APK fully silently). Push code → CI cuts a
 * release → the app self-updates next time it opens. All failures are silent (offline, rate-limited,
 * no newer build) — the app just runs the version it has.
 */
object Updater {

    // The public repo whose Releases hold each build's APK.
    private const val REPO = "djordi10/triad-mc-android-build"
    private const val LATEST = "https://api.github.com/repos/$REPO/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    /** Check for a newer release than [currentVersion] (BuildConfig.VERSION_CODE); install if found. */
    suspend fun checkAndInstall(ctx: Context, currentVersion: Int) {
        runCatching {
            val http = HttpClient()
            val meta = http.get(LATEST) {
                header("Accept", "application/vnd.github+json")
            }.bodyAsText()
            val obj = json.parseToJsonElement(meta).jsonObject

            // tag_name like "v37" → 37
            val tag = (obj["tag_name"] as? JsonPrimitive)?.content ?: return
            val latest = tag.filter { it.isDigit() }.toIntOrNull() ?: return
            if (latest <= currentVersion) return

            val asset = (obj["assets"] as? JsonArray)?.mapNotNull { it as? JsonObject }
                ?.firstOrNull { (it["name"] as? JsonPrimitive)?.content?.endsWith(".apk") == true }
                ?: return
            val url = (asset["browser_download_url"] as? JsonPrimitive)?.content ?: return

            val bytes: ByteArray = http.get(url).body()
            val file = File(ctx.cacheDir, "update.apk")
            file.writeBytes(bytes)

            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            withContext(Dispatchers.Main) { ctx.startActivity(intent) }
        }
    }
}
