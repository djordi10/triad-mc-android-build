package agentic.triad.missioncontrol.mcp

/**
 * The wall, made mechanical. `call()` may never carry a mutating verb — a defense-in-depth mirror
 * of the server-side window (which is also read/replay/propose only). If a tool name here ever
 * reaches [TriadMcpClient.call], it throws before a byte hits the network. The only writes in the
 * whole app are `propose()` and `recordCheckup()`, each its own method with its own UI.
 */
object MutatingDenylist {
    private val FORBIDDEN = setOf(
        "place", "cancel", "flatten", "enable", "disable", "release", "reset", "widen",
        "kill", "arm", "disarm", "set_", "update_config", "apply",
    )

    fun assertReadOnly(tool: String) {
        val low = tool.lowercase()
        if (low.startsWith("get_") || low.startsWith("list_") ||
            low.startsWith("run_") || low.startsWith("search_")
        ) {
            return // a read/replay tool (e.g. get_kill_state READS the switch, it doesn't kill)
        }
        require(FORBIDDEN.none { low.startsWith(it) || low.contains(it) }) {
            "MutatingDenylist: '$tool' is not a read tool — the app reads, replays, and proposes; " +
                "re-opening always takes a human at triadctl"
        }
    }
}
