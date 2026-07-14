package agentic.triad.missioncontrol

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

/**
 * TRIAD Mission Control (Android) — a thin native shell around the LIVE deployed dashboard at
 * triad-mc.bgzr.io. The app IS the site: dashboard, config-store editor, analytics, and every page
 * the design pipeline ships next — always current, no re-porting. It boots the page with the token,
 * then flips the dashboard to LIVE against the same-origin MCP (token in the `?token=` query, the
 * documented query-param auth). Read/replay/propose only — the wall lives server-side.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            with(CookieManager.getInstance()) {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(this@apply, true)
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    // Auto-connect the deployed dashboard to LIVE (same-origin MCP, token in query).
                    // Silent no-op if the dashboard's internals change — the user can still connect
                    // via its own Connection sheet.
                    view.evaluateJavascript(LIVE_JS, null)
                }
            }
            loadUrl(HOME)
        }
        setContentView(web)

        // Back navigates the WebView history; only exits the app when there's nowhere back to go.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) {
                    web.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    companion object {
        private const val TOKEN = "tmc_4f1183d581f36abcf9c1f28da0dd"
        private const val HOME = "https://triad-mc.bgzr.io/?token=$TOKEN"

        /** Flip the deployed dashboard to LIVE against the same-origin MCP, token in the query. */
        private val LIVE_JS = """
            (function(){
              try {
                if (typeof CONN !== 'undefined' && typeof McpAdapter === 'function') {
                  CONN.url = 'https://triad-mc.bgzr.io/mcp?token=$TOKEN';
                  adapter = McpAdapter(CONN);
                  if (typeof refresh === 'function') refresh();
                  if (typeof startPolling === 'function') startPolling();
                }
              } catch (e) {}
            })();
        """.trimIndent()
    }
}
