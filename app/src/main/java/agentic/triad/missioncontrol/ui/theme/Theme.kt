package agentic.triad.missioncontrol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The TRIAD palette — the exact tokens the web dashboard uses (v5.11), so the two clients read as
// one system. v5.11 facelift: the body is light paper with white cards (matching the web body), and
// the TRIAD brand strip stays dark pine on top.
val Paper = Color(0xFFF7F6F2)
val Card = Color(0xFFFFFFFF)
val Ink = Color(0xFF17251F)
val Ink2 = Color(0xFF51605A)
val Line = Color(0xFFE3E1D9)

val Pine = Color(0xFF0E211A)
val Pine2 = Color(0xFF16352A)
val PineLine = Color(0xFF2C4A3E)      // ghost-button border on the dark strip
val PineInk = Color(0xFFCFE0D8)       // ghost-button text on the dark strip
val PineVer = Color(0xFF7FA393)       // the mono "MISSION CONTROL ·" label

val Emerald = Color(0xFF0F8A5F)
val EmeraldBright = Color(0xFF59C99A)
val EmeraldSoft = Color(0xFFE7F4EE)
val Amber = Color(0xFFB45309)
val AmberSoft = Color(0xFFFDF3E5)
val Red = Color(0xFFB0403A)
val RedSoft = Color(0xFFF9EBEA)
val Blue = Color(0xFF1F5F8B)
val BlueSoft = Color(0xFFE8F0F6)
val Unk = Color(0xFF9AA5A0)
val UnkSoft = Color(0xFFECEAE4)
val Sev = Color(0xFF7D1D1A)

// Back-compat alias — earlier code referred to a single "Muted"; it maps to the web's --ink2.
val Muted = Ink2

// The cockpit body is a light desk surface (v5.11) — one scheme, not chased against the system
// theme, so the numbers read the same every time. The dark pine chrome is drawn explicitly where it
// belongs (the per-view brand strip).
private val TriadColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    secondary = Pine,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = UnkSoft,
    onSurfaceVariant = Ink2,
    outline = Line,
    error = Red,
    onError = Color.White,
)

private val TriadType = Typography()  // Material defaults; swap in IBM Plex/Archivo when bundled.

@Composable
fun TriadTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TriadColors, typography = TriadType, content = content)
}
