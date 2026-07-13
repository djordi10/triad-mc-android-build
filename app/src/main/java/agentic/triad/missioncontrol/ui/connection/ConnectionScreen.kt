package agentic.triad.missioncontrol.ui.connection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import agentic.triad.missioncontrol.TriadApp
import agentic.triad.missioncontrol.ui.theme.Muted

/**
 * Connection… — the app is **LIVE-only** (no DEMO). It boots connected to the baked endpoint; this
 * sheet just lets you re-point it or refresh the connection. The bearer, if given, goes to the
 * Keystore vault, never to logs. The `?token=` in the endpoint URL is the usual auth.
 */
@Composable
fun ConnectionScreen(app: TriadApp, onDone: () -> Unit) {
    var endpoint by remember { mutableStateOf(TriadApp.LIVE_ENDPOINT) }
    var bearer by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {
        Text("Connection — LIVE")
        OutlinedTextField(
            endpoint, { endpoint = it },
            label = { Text("MCP endpoint URL") },
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedTextField(
            bearer, { bearer = it },
            label = { Text("Bearer (optional; token can ride in the URL)") },
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = {
                if (bearer.isNotBlank()) app.vault.store(bearer)
                app.goLive(endpoint.trim())
                onDone()
            },
            modifier = Modifier.padding(top = 12.dp),
        ) { Text("Connect") }
        Text(
            "LIVE-only. The app reads, replays, and proposes — it can never place, cancel, flatten, " +
                "enable, release, or reset. Bearer held in memory + Keystore only.",
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
