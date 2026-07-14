package agentic.triad.missioncontrol

import android.app.Application
import agentic.triad.missioncontrol.data.DemoRepository
import agentic.triad.missioncontrol.data.LiveRepository
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.mcp.LiveMcpClient
import agentic.triad.missioncontrol.mcp.ProposeAction
import agentic.triad.missioncontrol.mcp.TriadMcpClient
import agentic.triad.missioncontrol.secure.BearerVault
import agentic.triad.missioncontrol.work.BroadcastScheduler

/**
 * The whole DI container — manual and explicit (the estate's taste: few dependencies, no magic).
 * Boots in DEMO; [goLive] is the one place mode-swap happens. The bearer never leaves [vault] +
 * memory.
 */
class TriadApp : Application() {

    lateinit var vault: BearerVault
        private set

    var repository: MissionRepository = DemoRepository()
        private set

    /** The live MCP client when in LIVE (for the checkup write-back); null in DEMO. */
    var client: TriadMcpClient? = null
        private set

    override fun onCreate() {
        super.onCreate()
        vault = BearerVault(this)
        // Boot straight to LIVE against the baked triad-mcp endpoint (token in the ?token= query).
        // The Connection sheet can still re-point it or drop to DEMO.
        goLive(LIVE_ENDPOINT)
        // Schedule the checkup broadcast at boot (respects the stored policy/interval; OFF is a no-op
        // beyond keeping the periodic work registered).
        BroadcastScheduler.schedule(this)
    }

    /** Flip to LIVE against a gateway endpoint; the bearer is read from the vault per request. */
    fun goLive(endpoint: String) {
        val c = LiveMcpClient(endpoint = endpoint, bearerProvider = vault::peek)
        client = c
        repository = LiveRepository(c)
    }

    fun goDemo() {
        client = null
        repository = DemoRepository()
    }

    /**
     * File a proposal — the only write the UI can trigger. In LIVE it goes to the MCP proposals
     * inbox for a human at triadctl; in DEMO there is no window, so it is acknowledged locally and
     * never sent. Returns a human-readable outcome for the drawer to show.
     */
    suspend fun propose(action: ProposeAction): String {
        val c = client ?: return "DEMO — proposal acknowledged locally, not sent (connect LIVE to file)"
        val env = c.propose(action)
        return if (env.ok) "Filed to the proposals inbox — a human applies it at triadctl"
        else "Refused: ${env.error ?: "unknown"}"
    }

    companion object {
        /** The baked LIVE MCP endpoint (auth token in the `?token=` query). */
        const val LIVE_ENDPOINT =
            "https://triad-mc.bgzr.io/mcp?token=tmc_4f1183d581f36abcf9c1f28da0dd"
    }
}
