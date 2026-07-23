package agentic.triad.missioncontrol.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Unit tests for the channel-aware release selection (the pure core of the in-app updater). */
class UpdaterTest {

    // A /releases payload: two dev tags (v39, v40), one prod tag with an apk (v41-prod), and a higher
    // prod tag with NO apk asset (v42-prod) — the selector must skip the assetless one.
    private val releases = """
        [
          {"tag_name":"v40","assets":[{"name":"app-debug.apk","browser_download_url":"https://x/v40.apk"}]},
          {"tag_name":"v39","assets":[{"name":"app-debug.apk","browser_download_url":"https://x/v39.apk"}]},
          {"tag_name":"v41-prod","assets":[{"name":"app-debug.apk","browser_download_url":"https://x/v41p.apk"}]},
          {"tag_name":"v42-prod","assets":[{"name":"notes.txt","browser_download_url":"https://x/notes.txt"}]}
        ]
    """.trimIndent()

    @Test fun dev_picks_highest_plain_tag_and_ignores_prod() {
        val t = selectUpdate(releases, "dev", currentVersion = 39)
        assertEquals(40, t?.version)
        assertEquals("https://x/v40.apk", t?.apkUrl)
    }

    @Test fun dev_returns_null_when_nothing_newer() {
        assertNull(selectUpdate(releases, "dev", currentVersion = 40))
    }

    @Test fun prod_picks_prod_tag_and_ignores_dev() {
        val t = selectUpdate(releases, "prod", currentVersion = 10)
        // v42-prod is higher but has no .apk asset → skipped; v41-prod wins.
        assertEquals(41, t?.version)
        assertEquals("https://x/v41p.apk", t?.apkUrl)
    }

    @Test fun prod_returns_null_when_nothing_newer_has_an_apk() {
        assertNull(selectUpdate(releases, "prod", currentVersion = 41))
    }

    @Test fun unparseable_json_is_null_not_a_throw() {
        assertNull(selectUpdate("not json", "dev", currentVersion = 1))
        assertNull(selectUpdate("{}", "prod", currentVersion = 1))
    }
}
