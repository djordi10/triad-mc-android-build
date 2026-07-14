package agentic.triad.missioncontrol.ui.connection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

/**
 * Connection… — the DEMO/LIVE switch. LIVE takes the gateway MCP endpoint + bearer; the bearer goes
 * to the Keystore vault, never to logs. A biometric prompt belongs in front of the LIVE commit.
 */
@Composable
fun ConnectionScreen(app: TriadApp, onDone: () -> Unit) {
    var endpoint by remember { mutableStateOf("https://gateway-host:8765/mcp") }
    var bearer by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {
        Text("Connection")
        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("MCP endpoint URL") })
        OutlinedTextField(bearer, { bearer = it }, label = { Text("Bearer (stored in Keystore)") })
        Button(onClick = {
            if (bearer.isNotBlank()) app.vault.store(bearer)
            app.goLive(endpoint)
            onDone()
        }) { Text("Go LIVE") }
        Button(onClick = { app.goDemo(); onDone() }) { Text("Use DEMO") }
        Text(
            "The app reads, replays, and proposes — it can never place, cancel, flatten, enable, " +
                "release, or reset. Bearer held in memory + Keystore only.",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = agentic.triad.missioncontrol.ui.theme.Muted,
        )
    }
}
