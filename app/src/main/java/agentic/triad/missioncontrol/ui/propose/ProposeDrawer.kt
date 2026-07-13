package agentic.triad.missioncontrol.ui.propose

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
import agentic.triad.missioncontrol.mcp.ProposeAction
import kotlinx.serialization.json.buildJsonObject

/**
 * The propose drawer — the ONLY write UI in the app. It files a record for a human at triadctl; it
 * does not act. `onFile` is wired to the MCP client's `propose()` (LIVE) or a local inbox (DEMO).
 */
@Composable
fun ProposeDrawer(onFile: (ProposeAction) -> Unit) {
    var kind by remember { mutableStateOf("config_change") }
    var rationale by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {
        Text("File a proposal (a human applies it at triadctl)")
        OutlinedTextField(kind, { kind = it }, label = { Text("kind") })
        OutlinedTextField(rationale, { rationale = it }, label = { Text("rationale + evidence") })
        Button(
            enabled = rationale.isNotBlank(),
            onClick = { onFile(ProposeAction(kind = kind, args = buildJsonObject { }, rationale = rationale)) },
        ) { Text("File proposal") }
    }
}
