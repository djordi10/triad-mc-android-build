package agentic.triad.missioncontrol

import agentic.triad.missioncontrol.data.DemoFixtures
import agentic.triad.missioncontrol.mcp.MutatingDenylist
import agentic.triad.missioncontrol.ui.components.Tone
import agentic.triad.missioncontrol.ui.components.validityTone
import agentic.triad.missioncontrol.work.BroadcastPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** JVM unit tests — the wall, the fixtures, and the VALIDITY thresholds. */
class WallAndFixturesTest {

    @Test fun denylist_refuses_mutating_verbs() {
        for (verb in listOf("place_order", "cancel_all", "flatten", "enable_slot", "reset_breaker")) {
            assertFailsWith<IllegalArgumentException> { MutatingDenylist.assertReadOnly(verb) }
        }
    }

    @Test fun denylist_allows_read_tools() {
        MutatingDenylist.assertReadOnly("get_system_overview")
        MutatingDenylist.assertReadOnly("get_databank")
    }

    @Test fun demo_fixture_has_v4_databank_shape() {
        val env = DemoFixtures.envelope("get_databank")
        assertTrue(env.ok)
        assertTrue(env.data.toString().contains("triaddtbnk/1.4"))
    }

    @Test fun validity_thresholds() {
        assertEquals(Tone.GOOD, validityTone(96.0))
        assertEquals(Tone.WARN, validityTone(60.0))
        assertEquals(Tone.BAD, validityTone(28.5))
        assertEquals(Tone.NEUTRAL, validityTone(null))
    }

    @Test fun broadcast_policy_gates_the_digest() {
        assertFalse(BroadcastPolicy.OFF.shouldPost("RED", null))
        assertTrue(BroadcastPolicy.RED_ONLY.shouldPost("RED", "GREEN"))
        assertFalse(BroadcastPolicy.RED_ONLY.shouldPost("YELLOW", "GREEN"))
        assertTrue(BroadcastPolicy.ON_CHANGE.shouldPost("YELLOW", "GREEN"))
        assertFalse(BroadcastPolicy.ON_CHANGE.shouldPost("GREEN", "GREEN"))
        assertTrue(BroadcastPolicy.EVERY_RUN.shouldPost("GREEN", "GREEN"))
    }
}
