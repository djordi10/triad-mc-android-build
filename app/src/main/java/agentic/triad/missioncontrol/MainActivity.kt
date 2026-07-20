package agentic.triad.missioncontrol

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import agentic.triad.missioncontrol.ui.nav.MissionNav
import agentic.triad.missioncontrol.ui.theme.TriadTheme
import agentic.triad.missioncontrol.update.Updater
import agentic.triad.missioncontrol.work.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TriadApp
        // Auto-update: DISABLED for local dev (kept reinstalling the v39 release over our from-source
        // build + spamming the unknown-apps/notif dialogs). Re-enable by uncommenting.
        // CoroutineScope(Dispatchers.IO).launch {
        //     Updater.checkAndInstall(this@MainActivity, BuildConfig.VERSION_CODE)
        // }
        // The checkup digest needs a channel + (API 33+) the runtime notification permission.
        Notifications.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            TriadTheme {
                Surface(Modifier.fillMaxSize()) {
                    val windowSize = calculateWindowSizeClass(this)
                    MissionNav(app = app, widthClass = windowSize.widthSizeClass)
                }
            }
        }
    }
}
