package agentic.triad.missioncontrol.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import agentic.triad.missioncontrol.BuildConfig
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

/** A resolved update: the target [version] (the tag's number) and the APK [apkUrl] to install. */
data class UpdateTarget(val version: Int, val apkUrl: String)

/**
 * In-app auto-update. On launch the app lists the repo's GitHub Releases, picks the newest one on
 * its OWN channel (dev = plain `vN` tags / prod = `vN-prod` tags) whose version beats what's
 * installed, downloads that APK and hands it to the system installer (a one-tap confirm — Android
 * never installs a sideloaded APK fully silently). Push code → CI cuts a release → the app
 * self-updates next time it opens. All failures are silent (offline, rate-limited, no newer build).
 *
 * Two guards keep dev iteration sane:
 *  - `versionCode <= 1` → a from-source/local build (VERSION_CODE env unset) never self-updates, so
 *    it can't keep reinstalling the published release over your working build.
 *  - channel (`BuildConfig.UPDATE_CHANNEL`) → dev testers and prod users follow separate release
 *    streams, both versioned by CI run_number.
 */
object Updater {

    // The public repo whose Releases hold each build's APK.
    private const val REPO = "djordi10/triad-mc-android-build"
    // ALL releases (not /releases/latest — that's a single flag-marked release and can't serve two
    // channels; we filter the list by our own channel's tag scheme).
    private const val RELEASES = "https://api.github.com/repos/$REPO/releases?per_page=100"

    /** Check for a newer release than [currentVersion] (BuildConfig.VERSION_CODE); install if found. */
    suspend fun checkAndInstall(ctx: Context, currentVersion: Int) {
        // Sentinel: a from-source local build defaults to versionCode 1 (VERSION_CODE env unset). It is
        // always "older" than the latest published tag, so self-updating would just reinstall the
        // release over the working build on every launch. CI builds carry run_number (> 1).
        if (currentVersion <= 1) return
        runCatching {
            val http = HttpClient()
            val body = http.get(RELEASES) {
                header("Accept", "application/vnd.github+json")
            }.bodyAsText()

            val target = selectUpdate(body, BuildConfig.UPDATE_CHANNEL, currentVersion) ?: return

            val bytes: ByteArray = http.get(target.apkUrl).body()
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

/**
 * Pure, network-free release selection (so it is unit-testable). From a `/releases` JSON array, pick
 * the highest-versioned release ON [channel] whose version beats [currentVersion], returning its APK
 * asset. Tag schemes: dev = `^v(\d+)$` (e.g. `v39`), prod = `^v(\d+)-prod$` (e.g. `v39-prod`). Returns
 * null when nothing newer is on the channel (or the JSON is unparseable). Never throws.
 */
fun selectUpdate(releasesJson: String, channel: String, currentVersion: Int): UpdateTarget? {
    val json = Json { ignoreUnknownKeys = true }
    val arr = runCatching { json.parseToJsonElement(releasesJson) as? JsonArray }.getOrNull() ?: return null
    val tagRegex = if (channel == "prod") Regex("""^v(\d+)-prod$""") else Regex("""^v(\d+)$""")

    var best: UpdateTarget? = null
    for (el in arr) {
        val obj = el as? JsonObject ?: continue
        val tag = (obj["tag_name"] as? JsonPrimitive)?.content ?: continue
        val ver = tagRegex.find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: continue
        if (ver <= currentVersion) continue
        val apk = (obj["assets"] as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.firstOrNull { (it["name"] as? JsonPrimitive)?.content?.endsWith(".apk") == true }
            ?.let { (it["browser_download_url"] as? JsonPrimitive)?.content }
            ?: continue
        if (best == null || ver > best.version) best = UpdateTarget(ver, apk)
    }
    return best
}
