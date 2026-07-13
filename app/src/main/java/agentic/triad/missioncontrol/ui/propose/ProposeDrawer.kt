package agentic.triad.missioncontrol.ui.propose

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
import agentic.triad.missioncontrol.mcp.ProposeAction
import agentic.triad.missioncontrol.ui.theme.Muted
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The propose drawer — the ONLY write UI in the app. It files a record for a human at triadctl; it
 * does not act. For a `config_change` it captures the exact lever + new value so the proposal says
 * *what* to change (the human then runs compile → triadctl apply). `onFile` is wired to the MCP
 * client's `propose()`.
 */
@Composable
fun ProposeDrawer(onFile: (ProposeAction) -> Unit) {
    var kind by remember { mutableStateOf("config_change") }
    var lever by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var rationale by remember { mutableStateOf("") }

    val isConfig = kind.trim() == "config_change"
    val ready = rationale.isNotBlank() && (!isConfig || (lever.isNotBlank() && newValue.isNotBlank()))

    Column(Modifier.padding(16.dp)) {
        Text("File a proposal — a human applies it at triadctl")
        OutlinedTextField(kind, { kind = it }, label = { Text("kind") }, modifier = Modifier.padding(top = 8.dp))
        if (isConfig) {
            OutlinedTextField(
                lever, { lever = it },
                label = { Text("lever (e.g. risk.conviction_threshold)") },
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedTextField(
                newValue, { newValue = it },
                label = { Text("new value") },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        OutlinedTextField(
            rationale, { rationale = it },
            label = { Text("rationale + evidence") },
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            enabled = ready,
            onClick = {
                val args = buildJsonObject {
                    if (isConfig) {
                        put("lever", lever.trim())
                        put("to", newValue.trim())
                    }
                }
                onFile(ProposeAction(kind = kind.trim(), args = args, rationale = rationale.trim()))
            },
            modifier = Modifier.padding(top = 12.dp),
        ) { Text("File proposal") }
        Text(
            "Propose-only. This files a request; it changes nothing on its own — never places, " +
                "cancels, or applies. A human applies config via triadctl.",
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
